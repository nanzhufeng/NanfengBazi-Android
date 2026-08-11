package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportFailure
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageFailure
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

sealed interface RecognitionRunResult {
    data class NeedsReview(val session: ImportSession) : RecognitionRunResult
    data class Failed(val session: ImportSession) : RecognitionRunResult
    data object NotFound : RecognitionRunResult
    data class Rejected(val status: ImportStatus) : RecognitionRunResult
    data object RevisionConflict : RecognitionRunResult
}

class ImportRecognitionCoordinator(
    private val repository: ImportSessionRepository,
    private val contentReader: ImportImageContentReader,
    private val ocrEngine: OcrEngine,
    private val pageClassifier: WenzhenPageClassifier,
    private val documentRefiner: OcrDocumentRefiner = NoOpOcrDocumentRefiner,
    private val fingerprintEngine: ImageFingerprintEngine? = null,
    private val imageGrouper: WenzhenImageGrouper = WenzhenImageGrouper(),
    private val p0Parser: WenzhenP0Parser = WenzhenP0Parser(),
    private val parseResultRefiner: WenzhenParseResultRefiner = NoOpWenzhenParseResultRefiner,
    private val clock: Clock = Clock.systemUTC(),
    private val diagnosticIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    init {
        // Recognition engines are adapters only. Whether an image may leave the device is
        // decided by the app-level consent gate before this coordinator is scheduled; this
        // coordinator still owns the same recoverable session, validation and review path.
        require(ocrEngine.engineId.isNotBlank()) { "图片识别引擎身份不能为空" }
        require(ocrEngine.engineVersion.isNotBlank()) { "图片识别引擎版本不能为空" }
    }

    suspend fun recognize(sessionId: String): RecognitionRunResult {
        var session = repository.findById(sessionId) ?: return RecognitionRunResult.NotFound
        if (session.status == ImportStatus.GROUPING_CASES && session.ocrDocuments.isNotEmpty()) {
            val needsReview = session.copy(
                status = ImportStatus.NEEDS_REVIEW,
                updatedAt = nowNotBefore(session.updatedAt),
            )
            session = persist(needsReview, session.revision)
                ?: return RecognitionRunResult.RevisionConflict
            return RecognitionRunResult.NeedsReview(session)
        }
        if (
            session.status !in setOf(
                ImportStatus.CLASSIFYING,
                ImportStatus.RECOGNIZING,
                ImportStatus.FAILED,
                ImportStatus.NEEDS_REVIEW,
            )
        ) {
            return RecognitionRunResult.Rejected(session.status)
        }
        if (session.status == ImportStatus.FAILED && session.failure?.retryable != true) {
            return RecognitionRunResult.Rejected(session.status)
        }
        if (
            session.status == ImportStatus.NEEDS_REVIEW &&
            session.imageFailures.none(ImportImageFailure::retryable)
        ) {
            return RecognitionRunResult.Rejected(session.status)
        }

        val targetImageIds = when (session.status) {
            ImportStatus.NEEDS_REVIEW -> session.imageFailures
                .filter(ImportImageFailure::retryable)
                .mapTo(mutableSetOf(), ImportImageFailure::imageId)
            else -> session.images.mapTo(mutableSetOf(), ImportImageRef::id)
        }
        val started = session.copy(
            status = ImportStatus.RECOGNIZING,
            failure = null,
            attemptCount = session.attemptCount + 1,
            updatedAt = nowNotBefore(session.updatedAt),
        )
        session = persist(started, session.revision)
            ?: return RecognitionRunResult.RevisionConflict

        try {
            require(session.images.isNotEmpty()) { "导入会话没有可识别图片" }
            val processedImages = mutableMapOf<String, ImportImageRef>()
            val recognizedDocuments = mutableListOf<OcrDocument>()
            val imageFailures = mutableListOf<ImportImageFailure>()
            session.images.filter { it.id in targetImageIds }.forEach { originalImage ->
                var image = originalImage
                try {
                    val bytes = contentReader.readBytes(image)
                    fingerprintEngine?.fingerprint(bytes)?.let { fingerprint ->
                        image = image.copy(
                            perceptualHash = fingerprint.perceptualHash,
                            widthPx = fingerprint.widthPx,
                            heightPx = fingerprint.heightPx,
                        )
                    }
                    val input = OcrImageInput(image, bytes)
                    val initialDocument = ocrEngine.recognize(input)
                    require(initialDocument.imageId == image.id) { "OCR 文档与输入图片身份不一致" }
                    require(initialDocument.engineId == ocrEngine.engineId) { "OCR 引擎身份不一致" }
                    require(initialDocument.engineVersion == ocrEngine.engineVersion) {
                        "OCR 引擎版本不一致"
                    }
                    val classification = pageClassifier.classify(initialDocument)
                    val document = documentRefiner.refine(
                        input = input,
                        pageType = classification.pageType,
                        initialDocument = initialDocument,
                    )
                    require(document.imageId == image.id) { "精识别文档与输入图片身份不一致" }
                    require(document.engineId == ocrEngine.engineId) { "精识别引擎身份不一致" }
                    require(document.engineVersion == ocrEngine.engineVersion) {
                        "精识别引擎版本不一致"
                    }
                    processedImages[image.id] = image.copy(
                        pageType = classification.pageType,
                        pageConfidence = classification.confidence,
                        classifierVersion = classification.classifierVersion,
                    )
                    recognizedDocuments += document
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    processedImages[image.id] = image
                    imageFailures += ImportImageFailure(
                        imageId = image.id,
                        code = "IMAGE_RECOGNITION_FAILED",
                        userMessage = failure.message
                            ?.takeIf { message -> message.length <= 180 }
                            ?: "这张图片未能识别，可单独重试。",
                        retryable = true,
                        diagnosticId = diagnosticIdFactory(),
                    )
                }
            }
            val mergedImages = session.images.map { processedImages[it.id] ?: it }
            val mergedDocuments = session.ocrDocuments
                .filterNot { it.imageId in targetImageIds } + recognizedDocuments
            val mergedFailures = session.imageFailures
                .filterNot { it.imageId in targetImageIds } + imageFailures
            if (mergedDocuments.isEmpty()) {
                val firstFailure = mergedFailures.firstOrNull()?.userMessage
                val failed = session.copy(
                    status = ImportStatus.FAILED,
                    images = mergedImages,
                    ocrDocuments = emptyList(),
                    imageFailures = mergedFailures,
                    failure = ImportFailure(
                        code = "ALL_IMAGES_RECOGNITION_FAILED",
                        userMessage = firstFailure
                            ?.let { "所选图片均未能识别：$it" }
                            ?: "所选图片均未能识别，可稍后复用原图重试。",
                        retryable = true,
                        failedStage = ImportStatus.RECOGNIZING,
                        diagnosticId = diagnosticIdFactory(),
                    ),
                    updatedAt = nowNotBefore(session.updatedAt),
                )
                val persisted = persist(failed, session.revision)
                    ?: return RecognitionRunResult.RevisionConflict
                return RecognitionRunResult.Failed(persisted)
            }
            val groupedCandidates = imageGrouper.group(mergedImages, mergedDocuments)
            val parsed = parseResultRefiner.refine(
                p0Parser.parse(
                    images = mergedImages,
                    documents = mergedDocuments,
                    groupedCandidates = groupedCandidates,
                ),
            )
            val grouped = session.copy(
                status = ImportStatus.GROUPING_CASES,
                images = mergedImages,
                ocrDocuments = mergedDocuments,
                imageFailures = mergedFailures,
                extractedFields = parsed.fields,
                extractedLongTexts = parsed.longTexts,
                caseCandidates = parsed.candidates,
                updatedAt = nowNotBefore(session.updatedAt),
            )
            session = persist(grouped, session.revision)
                ?: return RecognitionRunResult.RevisionConflict
            val needsReview = session.copy(
                status = ImportStatus.NEEDS_REVIEW,
                updatedAt = nowNotBefore(session.updatedAt),
            )
            session = persist(needsReview, session.revision)
                ?: return RecognitionRunResult.RevisionConflict
            return RecognitionRunResult.NeedsReview(session)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            val failed = session.copy(
                status = ImportStatus.FAILED,
                failure = ImportFailure(
                    code = "IMAGE_RECOGNITION_FAILED",
                    userMessage = "图片识别未完成，可稍后重试并保留已导入的原图。",
                    retryable = true,
                    failedStage = ImportStatus.RECOGNIZING,
                    diagnosticId = diagnosticIdFactory(),
                ),
                updatedAt = nowNotBefore(session.updatedAt),
            )
            val persisted = persist(failed, session.revision)
                ?: return RecognitionRunResult.RevisionConflict
            return RecognitionRunResult.Failed(persisted)
        }
    }

    private suspend fun persist(
        session: ImportSession,
        expectedRevision: Long,
    ): ImportSession? = when (repository.save(session, expectedRevision)) {
        is ImportSessionWriteResult.Updated -> repository.findById(session.id)
        else -> null
    }

    private fun nowNotBefore(previous: Instant): Instant =
        clock.instant().let { if (it < previous) previous else it }
}
