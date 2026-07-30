package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportFailure
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
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
    private val clock: Clock = Clock.systemUTC(),
    private val diagnosticIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    init {
        require(ocrEngine.executionMode == OcrExecutionMode.OFFLINE) {
            "图片导入只允许离线 OCR 引擎"
        }
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
            )
        ) {
            return RecognitionRunResult.Rejected(session.status)
        }
        if (session.status == ImportStatus.FAILED && session.failure?.retryable != true) {
            return RecognitionRunResult.Rejected(session.status)
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
            val documents = session.images.map { image ->
                val input = OcrImageInput(
                    image = image,
                    bytes = contentReader.readBytes(image),
                )
                val document = ocrEngine.recognize(input)
                require(document.imageId == image.id) { "OCR 文档与输入图片身份不一致" }
                require(document.engineId == ocrEngine.engineId) { "OCR 引擎身份不一致" }
                require(document.engineVersion == ocrEngine.engineVersion) { "OCR 引擎版本不一致" }
                document
            }
            val classifiedImages = session.images.map { image ->
                val classification = pageClassifier.classify(
                    documents.single { it.imageId == image.id },
                )
                image.copy(
                    pageType = classification.pageType,
                    pageConfidence = classification.confidence,
                    classifierVersion = classification.classifierVersion,
                )
            }
            val grouped = session.copy(
                status = ImportStatus.GROUPING_CASES,
                images = classifiedImages,
                ocrDocuments = documents,
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
