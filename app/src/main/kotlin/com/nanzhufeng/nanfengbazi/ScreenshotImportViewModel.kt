package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.ImportFailure
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextEvidence
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextType
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportSourceApp
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import com.nanzhufeng.nanfengbazi.imageparser.RecognitionRunResult
import com.nanzhufeng.nanfengbazi.imageparser.DuplicateImageKind
import com.nanzhufeng.nanfengbazi.imageparser.ImportImageDuplicateDetector
import java.io.InputStream
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingImportImage(
    val originalFileName: String,
    val mimeType: String,
    val openInput: () -> InputStream?,
)

data class ScreenshotFieldReviewUi(
    val id: String,
    val label: String,
    val sourceValue: String,
    val normalizedValue: String?,
    val calculationValue: String,
    val adoptedValue: String?,
    val confidencePercent: Int?,
    val sourceImageName: String,
    val sourceImageRelativePath: String,
    val sourceImageWidthPx: Int?,
    val sourceImageHeightPx: Int?,
    val evidenceBox: EvidenceBoundingBox?,
    val evidenceRegion: String?,
    val userEdited: Boolean,
)

data class ScreenshotLongTextReviewUi(
    val id: String,
    val label: String,
    val rawText: String,
    val adopted: Boolean,
    val sourceImageName: String,
)

data class ScreenshotCandidateReviewUi(
    val id: String,
    val alias: String,
    val imageCount: Int,
    val fields: List<ScreenshotFieldReviewUi>,
    val longTexts: List<ScreenshotLongTextReviewUi>,
    val fullyAdopted: Boolean,
    val readyToCommit: Boolean,
    val missingRequiredFields: List<String>,
    val blockingIssues: List<String>,
    val reviewWarnings: List<String>,
    val targetCaseId: String?,
)

data class ScreenshotImportUiState(
    val busy: Boolean = false,
    val progressText: String? = null,
    val activeSessionId: String? = null,
    val completedImageCount: Int = 0,
    val classifiedPageTypes: List<WenzhenPageType> = emptyList(),
    val exactDuplicatePairCount: Int = 0,
    val similarDuplicatePairCount: Int = 0,
    val failedImageCount: Int = 0,
    val caseCandidateCount: Int = 0,
    val multiImageCandidateCount: Int = 0,
    val extractedFieldCount: Int = 0,
    val extractedLongTextCount: Int = 0,
    val reviewCandidates: List<ScreenshotCandidateReviewUi> = emptyList(),
    val committingCandidateId: String? = null,
    val committedCaseCount: Int = 0,
    val pendingDuplicateCandidateId: String? = null,
    val duplicateCaseCandidates: List<DuplicateCaseCandidate> = emptyList(),
    val needsReview: Boolean = false,
    val canRetry: Boolean = false,
    val recoverableSessionCount: Int = 0,
    val sessionStatus: ImportStatus? = null,
    val parserVersion: String? = null,
    val failureCodes: List<String> = emptyList(),
    val failureDiagnosticIds: List<String> = emptyList(),
    val message: String? = null,
)

class ScreenshotImportViewModel(
    private val repository: ImportSessionRepository,
    private val imageStore: PrivateImportImageStore,
    private val recognitionScheduler: ScreenshotRecognitionScheduler,
    private val importCommitter: ScreenshotImportCommitter? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    constructor(container: AppContainer) : this(
        repository = container.importSessionRepository,
        imageStore = container.importImageStore,
        recognitionScheduler = container.screenshotRecognitionScheduler,
        importCommitter = container.screenshotImportCommitter,
    )

    private val mutableState = MutableStateFlow(ScreenshotImportUiState())
    val state: StateFlow<ScreenshotImportUiState> = mutableState.asStateFlow()

    init {
        refreshRecoverableSessions()
    }

    fun importImages(images: List<PendingImportImage>) {
        if (images.isEmpty()) return
        if (mutableState.value.busy) {
            mutableState.update { it.copy(message = "已有截图导入正在处理，请稍候。") }
            return
        }
        viewModelScope.launch {
            val sessionId = idFactory()
            mutableState.value = ScreenshotImportUiState(
                busy = true,
                progressText = "正在建立私有导入会话…",
                activeSessionId = sessionId,
            )
            var session: ImportSession? = null
            try {
                val now = clock.instant()
                val initial = ImportSession(
                    id = sessionId,
                    sourceApp = ImportSourceApp.WENZHEN_BAZI,
                    status = ImportStatus.WAITING,
                    parserVersion = PARSER_VERSION,
                    createdAt = now,
                    updatedAt = now,
                )
                val created = repository.save(initial, expectedRevision = 0)
                check(created is ImportSessionWriteResult.Created) { "导入会话创建失败" }
                session = requireNotNull(repository.findById(sessionId))
                session = persist(
                    session.copy(
                        status = ImportStatus.COPYING_IMAGES,
                        updatedAt = clock.instant(),
                    ),
                )

                images.forEachIndexed { index, source ->
                    mutableState.update {
                        it.copy(progressText = "正在私有复制第 ${index + 1}/${images.size} 张图片…")
                    }
                    val image = imageStore.copyImage(
                        sessionId = sessionId,
                        imageId = idFactory(),
                        originalFileName = source.originalFileName,
                        mimeType = source.mimeType,
                        createdAt = clock.instant(),
                        openInput = source.openInput,
                    )
                    session = persist(
                        requireNotNull(session).copy(
                            images = requireNotNull(session).images + image,
                            updatedAt = clock.instant(),
                        ),
                    )
                }

                session = persist(
                    requireNotNull(session).copy(
                        status = ImportStatus.CLASSIFYING,
                        updatedAt = clock.instant(),
                    ),
                )
                mutableState.update {
                    it.copy(
                        progressText = "正在使用内置中文模型离线识别…",
                        completedImageCount = requireNotNull(session).images.size,
                    )
                }
                applyRecognitionResult(
                    recognitionScheduler.recognize(sessionId),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                val failed = session?.let { current ->
                    runCatching {
                        persist(
                            current.copy(
                                status = ImportStatus.FAILED,
                                failure = ImportFailure(
                                    code = "IMAGE_COPY_FAILED",
                                    userMessage = "部分图片未能复制到私有空间，请重新选择后再试。",
                                    retryable = false,
                                    failedStage = ImportStatus.COPYING_IMAGES,
                                    diagnosticId = idFactory(),
                                ),
                                updatedAt = clock.instant(),
                            ),
                        )
                    }.getOrNull()
                }
                mutableState.update {
                    it.copy(
                        busy = false,
                        progressText = null,
                        completedImageCount = failed?.images?.size ?: 0,
                        canRetry = false,
                        message = "截图导入未完成；已经私有复制的原图仍保留在可恢复会话中。",
                    )
                }
                refreshRecoverableSessions()
            }
        }
    }

    fun retryRecognition() {
        val sessionId = mutableState.value.activeSessionId ?: return
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    busy = true,
                    progressText = "正在复用已保存原图重新识别…",
                    message = null,
                )
            }
            applyRecognitionResult(
                recognitionScheduler.recognize(sessionId),
            )
        }
    }

    fun setFieldAdopted(
        candidateId: String,
        fieldId: String,
        adopted: Boolean,
    ) {
        updateReviewSession { session ->
            val candidate = session.caseCandidates
                .singleOrNull { it.id == candidateId }
                ?: return@updateReviewSession null
            if (fieldId !in candidate.fieldEvidenceIds) return@updateReviewSession null
            session.copy(
                extractedFields = session.extractedFields.map { field ->
                    if (field.id == fieldId) {
                        field.copy(adoptedValue = field.normalizedValue.takeIf { adopted })
                    } else {
                        field
                    }
                },
                updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
            )
        }
    }

    fun setLongTextAdopted(
        candidateId: String,
        longTextId: String,
        adopted: Boolean,
    ) {
        updateReviewSession { session ->
            val candidate = session.caseCandidates
                .singleOrNull { it.id == candidateId }
                ?: return@updateReviewSession null
            if (longTextId !in candidate.longTextEvidenceIds) return@updateReviewSession null
            session.copy(
                extractedLongTexts = session.extractedLongTexts.map { longText ->
                    if (longText.id == longTextId) {
                        longText.copy(adopted = adopted)
                    } else {
                        longText
                    }
                },
                updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
            )
        }
    }

    fun setCandidateAdopted(
        candidateId: String,
        adopted: Boolean,
    ) {
        updateReviewSession { session ->
            val candidate = session.caseCandidates
                .singleOrNull { it.id == candidateId }
                ?: return@updateReviewSession null
            session.copy(
                extractedFields = session.extractedFields.map { field ->
                    if (field.id in candidate.fieldEvidenceIds) {
                        field.copy(adoptedValue = field.normalizedValue.takeIf { adopted })
                    } else {
                        field
                    }
                },
                extractedLongTexts = session.extractedLongTexts.map { longText ->
                    if (longText.id in candidate.longTextEvidenceIds) {
                        longText.copy(adopted = adopted)
                    } else {
                        longText
                    }
                },
                updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
            )
        }
    }

    fun updateFieldNormalizedValue(
        candidateId: String,
        fieldId: String,
        editedValue: String,
    ) {
        updateReviewSession { session ->
            val candidate = session.caseCandidates
                .singleOrNull { it.id == candidateId }
                ?: return@updateReviewSession null
            if (fieldId !in candidate.fieldEvidenceIds) return@updateReviewSession null
            val field = session.extractedFields.singleOrNull { it.id == fieldId }
                ?: return@updateReviewSession null
            val normalized = field.fieldKey.parseEditedValue(editedValue)
            if (normalized == null) {
                mutableState.update {
                    it.copy(message = field.fieldKey.editValidationMessage())
                }
                return@updateReviewSession null
            }
            session.copy(
                extractedFields = session.extractedFields.map { existing ->
                    if (existing.id == fieldId) {
                        existing.copy(
                            normalizedValue = normalized,
                            adoptedValue = null,
                            userEdited = true,
                        )
                    } else {
                        existing
                    }
                },
                updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
            )
        }
    }

    fun commitCandidate(
        candidateId: String,
        allowDuplicate: Boolean = false,
    ) {
        val sessionId = mutableState.value.activeSessionId ?: return
        val committer = importCommitter
        if (committer == null) {
            mutableState.update { it.copy(message = "当前环境未接入正式命例提交器。") }
            return
        }
        if (mutableState.value.committingCandidateId != null) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    committingCandidateId = candidateId,
                    message = null,
                    pendingDuplicateCandidateId = null,
                    duplicateCaseCandidates = emptyList(),
                )
            }
            when (
                val result = committer.commitCandidate(
                    sessionId,
                    candidateId,
                    allowDuplicate,
                )
            ) {
                is ScreenshotCandidateCommitResult.Committed -> {
                    if (result.sessionCompleted) {
                        mutableState.update {
                            it.copy(
                                activeSessionId = null,
                                needsReview = false,
                                reviewCandidates = emptyList(),
                                committingCandidateId = null,
                                committedCaseCount = it.committedCaseCount + 1,
                                pendingDuplicateCandidateId = null,
                                duplicateCaseCandidates = emptyList(),
                                message = "全部候选已核对并写入正式命例。",
                            )
                        }
                    } else {
                        val persisted = importSessionRepositorySession(sessionId)
                        mutableState.update {
                            it.copy(
                                reviewCandidates = persisted?.toReviewCandidates().orEmpty(),
                                committingCandidateId = null,
                                committedCaseCount = it.committedCaseCount + 1,
                                pendingDuplicateCandidateId = null,
                                duplicateCaseCandidates = emptyList(),
                                message = "当前候选已写入正式命例，其余候选仍待核对。",
                            )
                        }
                    }
                }

                is ScreenshotCandidateCommitResult.DuplicateFound -> mutableState.update {
                    it.copy(
                        committingCandidateId = null,
                        pendingDuplicateCandidateId = candidateId,
                        duplicateCaseCandidates = result.candidates,
                        message = "发现疑似重复命例，请人工核对后决定是否仍保留两份。",
                    )
                }

                is ScreenshotCandidateCommitResult.Rejected -> mutableState.update {
                    it.copy(
                        committingCandidateId = null,
                        message = result.message,
                    )
                }

                is ScreenshotCandidateCommitResult.Failed -> mutableState.update {
                    it.copy(
                        committingCandidateId = null,
                        message = result.message,
                    )
                }
            }
            refreshRecoverableSessions()
        }
    }

    fun deleteActiveImport() {
        val sessionId = mutableState.value.activeSessionId ?: return
        if (mutableState.value.busy) return
        viewModelScope.launch {
            val session = repository.findById(sessionId)
            if (session == null) {
                mutableState.value = ScreenshotImportUiState(
                    message = "导入会话已经不存在。",
                )
                refreshRecoverableSessions()
                return@launch
            }
            when (repository.delete(session.id, session.revision)) {
                ImportSessionDeleteResult.Deleted -> {
                    val undeletedCount = session.images.count { image ->
                        runCatching {
                            imageStore.deleteImage(image)
                            true
                        }.getOrDefault(false).not()
                    }
                    mutableState.value = ScreenshotImportUiState(
                        message = if (undeletedCount == 0) {
                            "本次截图导入会话及私有原图已删除。"
                        } else {
                            "导入会话已删除，$undeletedCount 张原图未能清理。"
                        },
                    )
                    refreshRecoverableSessions()
                }

                is ImportSessionDeleteResult.RevisionConflict -> {
                    mutableState.update {
                        it.copy(message = "导入会话刚刚发生更新，请确认后重试删除。")
                    }
                }

                ImportSessionDeleteResult.NotFound -> {
                    mutableState.value = ScreenshotImportUiState(
                        message = "导入会话已经不存在。",
                    )
                    refreshRecoverableSessions()
                }
            }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    private suspend fun applyRecognitionResult(result: RecognitionRunResult) {
        when (result) {
            is RecognitionRunResult.NeedsReview -> mutableState.update {
                val duplicateCounts = duplicateCounts(result.session)
                it.copy(
                    busy = false,
                    progressText = null,
                    activeSessionId = result.session.id,
                    sessionStatus = result.session.status,
                    parserVersion = result.session.parserVersion,
                    completedImageCount = result.session.images.size,
                    classifiedPageTypes = result.session.images.map { image -> image.pageType },
                    exactDuplicatePairCount = duplicateCounts.first,
                    similarDuplicatePairCount = duplicateCounts.second,
                    failedImageCount = result.session.imageFailures.size,
                    failureCodes = buildList {
                        result.session.failure?.code?.let(::add)
                        addAll(result.session.imageFailures.map { failure -> failure.code })
                    },
                    failureDiagnosticIds = buildList {
                        result.session.failure?.diagnosticId?.let(::add)
                        addAll(result.session.imageFailures.map { failure -> failure.diagnosticId })
                    },
                    caseCandidateCount = result.session.caseCandidates.size,
                    multiImageCandidateCount = result.session.caseCandidates.count {
                        candidate -> candidate.imageIds.size > 1
                    },
                    extractedFieldCount = result.session.extractedFields.size,
                    extractedLongTextCount = result.session.extractedLongTexts.size,
                    reviewCandidates = result.session.toReviewCandidates(),
                    needsReview = true,
                    canRetry = result.session.imageFailures.any { failure -> failure.retryable },
                    message = if (result.session.imageFailures.isEmpty()) {
                        "离线识别已完成，结果保存在待核对会话中，尚未写入正式命例。"
                    } else {
                        "其余图片已完成识别，失败图片可单独重试；尚未写入正式命例。"
                    },
                )
            }

            is RecognitionRunResult.Failed -> mutableState.update {
                it.copy(
                    busy = false,
                    progressText = null,
                    activeSessionId = result.session.id,
                    sessionStatus = result.session.status,
                    parserVersion = result.session.parserVersion,
                    completedImageCount = result.session.images.size,
                    failedImageCount = result.session.imageFailures.size,
                    failureCodes = buildList {
                        result.session.failure?.code?.let(::add)
                        addAll(result.session.imageFailures.map { failure -> failure.code })
                    },
                    failureDiagnosticIds = buildList {
                        result.session.failure?.diagnosticId?.let(::add)
                        addAll(result.session.imageFailures.map { failure -> failure.diagnosticId })
                    },
                    needsReview = false,
                    canRetry = result.session.failure?.retryable == true,
                    message = result.session.failure?.userMessage,
                )
            }

            RecognitionRunResult.NotFound -> mutableState.update {
                it.copy(
                    busy = false,
                    progressText = null,
                    canRetry = false,
                    message = "找不到对应导入会话，请重新选择图片。",
                )
            }

            is RecognitionRunResult.Rejected -> mutableState.update {
                it.copy(
                    busy = false,
                    progressText = null,
                    canRetry = false,
                    message = "当前会话状态不允许执行识别：${result.status.name}",
                )
            }

            RecognitionRunResult.RevisionConflict -> mutableState.update {
                it.copy(
                    busy = false,
                    progressText = null,
                    canRetry = true,
                    message = "导入会话已在其他流程中更新，请稍后重试。",
                )
            }
        }
        refreshRecoverableSessions()
    }

    private suspend fun persist(session: ImportSession): ImportSession {
        val result = repository.save(
            session = session,
            expectedRevision = session.revision,
        )
        check(result is ImportSessionWriteResult.Updated) { "导入会话持久化失败" }
        return requireNotNull(repository.findById(session.id))
    }

    private fun refreshRecoverableSessions() {
        viewModelScope.launch {
            var sessions = repository.list(
                ImportStatus.entries.toSet() - setOf(
                    ImportStatus.COMPLETED,
                    ImportStatus.CANCELLED,
                ),
            )
            val recoverableCompletion = sessions.firstOrNull { session ->
                session.status in setOf(
                    ImportStatus.NEEDS_REVIEW,
                    ImportStatus.READY_TO_COMMIT,
                    ImportStatus.COMMITTING,
                ) &&
                    session.caseCandidates.isNotEmpty() &&
                    session.caseCandidates.all { it.targetCaseId != null }
            }
            var recoveryMessage: String? = null
            if (recoverableCompletion != null && importCommitter != null) {
                recoveryMessage = when (
                    val recovery = importCommitter.resumeCompletedSession(recoverableCompletion.id)
                ) {
                    is ScreenshotCandidateCommitResult.Committed -> null
                    is ScreenshotCandidateCommitResult.DuplicateFound ->
                        "恢复完成检查遇到疑似重复命例。"
                    is ScreenshotCandidateCommitResult.Rejected -> recovery.message
                    is ScreenshotCandidateCommitResult.Failed -> recovery.message
                }
                sessions = repository.list(
                    ImportStatus.entries.toSet() - setOf(
                        ImportStatus.COMPLETED,
                        ImportStatus.CANCELLED,
                    ),
                )
            }
            mutableState.update { current ->
                val recent = sessions.firstOrNull()
                if (recent == null || current.busy || current.activeSessionId != null) {
                    current.copy(
                        recoverableSessionCount = sessions.size,
                        message = current.message ?: recoveryMessage,
                    )
                } else {
                    val duplicateCounts = duplicateCounts(recent)
                    current.copy(
                        activeSessionId = recent.id,
                        sessionStatus = recent.status,
                        parserVersion = recent.parserVersion,
                        completedImageCount = recent.images.size,
                        classifiedPageTypes = recent.images.map { it.pageType },
                        exactDuplicatePairCount = duplicateCounts.first,
                        similarDuplicatePairCount = duplicateCounts.second,
                        failedImageCount = recent.imageFailures.size,
                        failureCodes = buildList {
                            recent.failure?.code?.let(::add)
                            addAll(recent.imageFailures.map { failure -> failure.code })
                        },
                        failureDiagnosticIds = buildList {
                            recent.failure?.diagnosticId?.let(::add)
                            addAll(recent.imageFailures.map { failure -> failure.diagnosticId })
                        },
                        caseCandidateCount = recent.caseCandidates.size,
                        multiImageCandidateCount = recent.caseCandidates.count {
                            candidate -> candidate.imageIds.size > 1
                        },
                        extractedFieldCount = recent.extractedFields.size,
                        extractedLongTextCount = recent.extractedLongTexts.size,
                        reviewCandidates = recent.toReviewCandidates(),
                        needsReview = recent.status == ImportStatus.NEEDS_REVIEW,
                        canRetry = when (recent.status) {
                            ImportStatus.CLASSIFYING,
                            ImportStatus.RECOGNIZING,
                            ImportStatus.GROUPING_CASES,
                            -> true

                            ImportStatus.FAILED -> recent.failure?.retryable == true
                            ImportStatus.NEEDS_REVIEW ->
                                recent.imageFailures.any { failure -> failure.retryable }
                            else -> false
                        },
                        recoverableSessionCount = sessions.size,
                        message = current.message ?: recoveryMessage,
                    )
                }
            }
        }
    }

    private fun duplicateCounts(session: ImportSession): Pair<Int, Int> {
        val matches = ImportImageDuplicateDetector().findMatches(session.images)
        return matches.count { it.kind == DuplicateImageKind.EXACT } to
            matches.count { it.kind == DuplicateImageKind.VISUALLY_SIMILAR }
    }

    private fun updateReviewSession(
        transform: (ImportSession) -> ImportSession?,
    ) {
        val sessionId = mutableState.value.activeSessionId ?: return
        if (mutableState.value.busy) return
        viewModelScope.launch {
            val current = repository.findById(sessionId)
            if (current == null || current.status != ImportStatus.NEEDS_REVIEW) {
                mutableState.update {
                    it.copy(message = "当前导入会话已经变化，请返回后重新打开。")
                }
                return@launch
            }
            val updated = transform(current) ?: return@launch
            when (repository.save(updated, current.revision)) {
                is ImportSessionWriteResult.Updated -> {
                    val persisted = requireNotNull(repository.findById(sessionId))
                    mutableState.update {
                        it.copy(
                            reviewCandidates = persisted.toReviewCandidates(),
                            extractedFieldCount = persisted.extractedFields.size,
                            extractedLongTextCount = persisted.extractedLongTexts.size,
                        )
                    }
                }

                is ImportSessionWriteResult.RevisionConflict -> mutableState.update {
                    it.copy(message = "核对结果已在其他流程更新，请重新打开后再试。")
                }

                else -> mutableState.update {
                    it.copy(message = "本次核对未保存，原始识别结果没有改变。")
                }
            }
        }
    }

    private fun ImportSession.toReviewCandidates(): List<ScreenshotCandidateReviewUi> {
        val fieldsById = extractedFields.associateBy(CaseFieldEvidence::id)
        val longTextsById = extractedLongTexts.associateBy(ImportedLongTextEvidence::id)
        val imagesById = images.associateBy { it.id }
        val imageFailuresById = imageFailures.associateBy { it.imageId }
        return caseCandidates.map { candidate ->
            candidate.toReviewUi(
                fieldsById = fieldsById,
                longTextsById = longTextsById,
                imagesById = imagesById,
                imageFailuresById = imageFailuresById,
            )
        }
    }

    private fun ImportCaseCandidate.toReviewUi(
        fieldsById: Map<String, CaseFieldEvidence>,
        longTextsById: Map<String, ImportedLongTextEvidence>,
        imagesById: Map<String, com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef>,
        imageFailuresById:
            Map<String, com.nanzhufeng.nanfengbazi.domain.model.ImportImageFailure>,
    ): ScreenshotCandidateReviewUi {
        val candidateFields = fieldEvidenceIds.mapNotNull(fieldsById::get)
        val candidateLongTexts = longTextEvidenceIds.mapNotNull(longTextsById::get)
        val missingRequiredFieldKeys = REQUIRED_COMMIT_FIELD_KEYS.filter { requiredKey ->
            candidateFields.none { field ->
                field.fieldKey == requiredKey && field.adoptedValue != null
            }
        }
        val blockingIssues = buildList {
            imageIds.mapNotNull(imageFailuresById::get).forEach { failure ->
                val imageName = imagesById[failure.imageId]?.originalFileName ?: "未知图片"
                add("$imageName：${failure.userMessage}")
            }
            imageIds.mapNotNull(imagesById::get)
                .filter { it.pageType == WenzhenPageType.UNKNOWN }
                .forEach { image ->
                    add("${image.originalFileName}：未识别出问真页面类型")
                }
            REQUIRED_COMMIT_FIELD_KEYS.forEach { requiredKey ->
                val matchingFields = candidateFields.filter { it.fieldKey == requiredKey }
                when {
                    matchingFields.isEmpty() ->
                        add("未识别出${requiredKey.displayLabel()}")
                    matchingFields.all { it.normalizedValue == null } ->
                        add("${requiredKey.displayLabel()}无法规范化，请人工修正")
                }
            }
            if (candidateFields.isEmpty() && candidateLongTexts.isEmpty() && isEmpty()) {
                add("关联图片没有提取出可核对内容")
            }
        }.distinct()
        val reviewWarnings = buildList {
            val lowConfidenceCount = candidateFields.count { field ->
                listOfNotNull(
                    field.ocrConfidence,
                    field.parserConfidence,
                    field.consistencyConfidence,
                ).minOrNull()?.let { it < LOW_CONFIDENCE_THRESHOLD } == true
            }
            if (lowConfidenceCount > 0) {
                add("$lowConfidenceCount 项字段识别置信度较低，请重点核对")
            }
            if (imageIds.size > 1 &&
                groupingConfidence?.let { it < LOW_GROUPING_CONFIDENCE_THRESHOLD } == true
            ) {
                add("多图归组置信度较低，请确认这些图片属于同一命例")
            }
        }
        return ScreenshotCandidateReviewUi(
            id = id,
            alias = suggestedAlias ?: "未命名候选",
            imageCount = imageIds.size,
            fields = candidateFields.map { field ->
            ScreenshotFieldReviewUi(
                id = field.id,
                label = field.fieldKey.displayLabel(),
                sourceValue = field.rawText,
                normalizedValue = field.normalizedValue?.displayValue(),
                calculationValue = field.calculatedValue?.displayValue() ?: when {
                    field.fieldKey == "chart.four_pillars" ->
                        "提交前按采用的出生资料复算"
                    field.fieldKey.startsWith("chart.") &&
                        field.fieldKey.endsWith(".spirits") ->
                        "神煞仅保留来源证据；当前不自动复算"
                    field.fieldKey == "identity.constellation" ||
                        field.fieldKey == "identity.zodiac" ->
                        "提交后与本机基础排盘自动对照；不会覆盖本地排盘"
                    field.fieldKey.startsWith("chart.") ->
                        "提交后与本机基础排盘自动对照；不会覆盖本地排盘"
                    field.fieldKey.startsWith("professional.") ->
                        "提交后按观察时刻自动复算；不会覆盖本地排盘"
                    else -> "不参与命盘计算"
                },
                adoptedValue = field.adoptedValue?.displayValue(),
                confidencePercent = listOfNotNull(
                    field.ocrConfidence,
                    field.parserConfidence,
                    field.consistencyConfidence,
                ).minOrNull()?.times(100)?.toInt(),
                sourceImageName =
                    imagesById[field.attachmentId]?.originalFileName ?: "未知来源图片",
                sourceImageRelativePath =
                    imagesById[field.attachmentId]?.relativePath.orEmpty(),
                sourceImageWidthPx = imagesById[field.attachmentId]?.widthPx,
                sourceImageHeightPx = imagesById[field.attachmentId]?.heightPx,
                evidenceBox = field.boundingBox,
                evidenceRegion = field.boundingBox?.run {
                    "($left,$top)-($right,$bottom)"
                },
                userEdited = field.userEdited,
            )
        },
        longTexts = candidateLongTexts.map { text ->
            ScreenshotLongTextReviewUi(
                id = text.id,
                label = when (text.type) {
                    ImportedLongTextType.OWNER_FEEDBACK -> "命主反馈"
                    ImportedLongTextType.MASTER_COMMENTARY -> "师傅点评"
                    ImportedLongTextType.UNKNOWN -> "其他原文"
                },
                rawText = text.rawText,
                adopted = text.adopted,
                sourceImageName = imagesById[text.imageId]
                    ?.originalFileName
                    ?: "未知来源图片",
            )
        },
        fullyAdopted = candidateFields
            .takeIf { it.isNotEmpty() }
            ?.all { it.adoptedValue != null } == true &&
            candidateLongTexts.all(ImportedLongTextEvidence::adopted),
        readyToCommit = REQUIRED_COMMIT_FIELD_KEYS.all { requiredKey ->
            candidateFields.any { field ->
                field.fieldKey == requiredKey && field.adoptedValue != null
            }
        },
        missingRequiredFields = missingRequiredFieldKeys.map { it.displayLabel() },
        blockingIssues = blockingIssues,
        reviewWarnings = reviewWarnings,
        targetCaseId = targetCaseId,
    )
    }

    private suspend fun importSessionRepositorySession(sessionId: String): ImportSession? =
        repository.findById(sessionId)

    private fun String.displayLabel(): String = when (this) {
        "identity.alias" -> "命例名称"
        "identity.name" -> "姓名"
        "identity.sex" -> "性别"
        "identity.constellation" -> "星座"
        "identity.zodiac" -> "属相"
        "birth.solar_date" -> "公历生日"
        "birth.solar_datetime" -> "公历出生时间"
        "birth.lunar_text" -> "农历原文"
        "birth.true_solar_datetime" -> "问真真太阳时"
        "birth.location" -> "出生地区"
        "birth.latitude" -> "纬度"
        "birth.longitude" -> "经度"
        "chart.four_pillars" -> "四柱"
        "professional.observed_at" -> "专业细盘 · 观察时刻"
        "professional.flow_year" -> "专业细盘 · 流年柱"
        "professional.flow_month" -> "专业细盘 · 流月柱"
        "professional.flow_day" -> "专业细盘 · 流日柱"
        "professional.flow_hour" -> "专业细盘 · 流时柱"
        "professional.decade" -> "专业细盘 · 当前大运"
        "professional.natal_year" -> "专业细盘 · 年柱"
        "professional.natal_month" -> "专业细盘 · 月柱"
        "professional.natal_day" -> "专业细盘 · 日柱"
        "professional.natal_hour" -> "专业细盘 · 时柱"
        else -> CHART_FIELD_PATTERN.matchEntire(this)
            ?.let { match ->
                "${CHART_COLUMN_LABELS.getValue(match.groupValues[1])} · " +
                    CHART_ROW_LABELS.getValue(match.groupValues[2])
            }
            ?: EVENT_FIELD_PATTERN.matchEntire(this)
                ?.groupValues
                ?.get(1)
                ?.let { "关键事件候选 · ${it}年" }
            ?: this
    }

    private fun TypedFieldValue.displayValue(): String = when (this) {
        is TypedFieldValue.Text -> value
        is TypedFieldValue.IntegerNumber -> value.toString()
        is TypedFieldValue.DecimalNumber -> canonicalValue
        is TypedFieldValue.BooleanValue -> if (value) "是" else "否"
        is TypedFieldValue.DateTimeValue -> value.run {
            "%04d-%02d-%02d %02d:%02d:%02d".format(
                year,
                month,
                day,
                hour,
                minute,
                second,
            )
        }

        is TypedFieldValue.FourPillarsValue -> value.run {
            "$year $month $day $hour"
        }
    }

    private fun String.parseEditedValue(rawValue: String): TypedFieldValue? {
        val value = rawValue.trim()
        return when (this) {
            "identity.alias" -> value
                .takeIf { it.isNotEmpty() && it.length <= 60 }
                ?.let(TypedFieldValue::Text)

            "identity.sex" -> value
                .takeIf { it == "男" || it == "女" }
                ?.let(TypedFieldValue::Text)

            "birth.solar_date" -> runCatching { LocalDate.parse(value) }
                .getOrNull()
                ?.toString()
                ?.let(TypedFieldValue::Text)

            "birth.solar_datetime",
            "birth.true_solar_datetime",
            "professional.observed_at",
            -> runCatching { LocalDateTime.parse(value.replace(' ', 'T')) }
                .getOrNull()
                ?.let { dateTime ->
                    TypedFieldValue.DateTimeValue(
                        CivilDateTime(
                            year = dateTime.year,
                            month = dateTime.monthValue,
                            day = dateTime.dayOfMonth,
                            hour = dateTime.hour,
                            minute = dateTime.minute,
                            second = dateTime.second,
                        ),
                    )
                }

            "birth.latitude" -> value.toDoubleOrNull()
                ?.takeIf { it in -90.0..90.0 }
                ?.let { TypedFieldValue.DecimalNumber(it.toString()) }

            "birth.longitude" -> value.toDoubleOrNull()
                ?.takeIf { it in -180.0..180.0 }
                ?.let { TypedFieldValue.DecimalNumber(it.toString()) }

            "chart.four_pillars" -> value
                .split(Regex("\\s+"))
                .takeIf { pillars ->
                    pillars.size == 4 && pillars.all(FOUR_PILLAR_PATTERN::matches)
                }
                ?.let { pillars ->
                    TypedFieldValue.FourPillarsValue(
                        FourPillars(
                            year = pillars[0],
                            month = pillars[1],
                            day = pillars[2],
                            hour = pillars[3],
                        ),
                    )
                }

            "professional.flow_year",
            "professional.flow_month",
            "professional.flow_day",
            "professional.flow_hour",
            "professional.decade",
            "professional.natal_year",
            "professional.natal_month",
            "professional.natal_day",
            "professional.natal_hour",
            -> value
                .takeIf(FOUR_PILLAR_PATTERN::matches)
                ?.let(TypedFieldValue::Text)

            else -> value.takeIf(String::isNotEmpty)?.let(TypedFieldValue::Text)
        }
    }

    private fun String.editValidationMessage(): String = when (this) {
        "identity.alias" -> "命例名称不能为空且不能超过 60 个字符。"
        "identity.sex" -> "性别只能填写“男”或“女”。"
        "birth.solar_date" -> "公历生日必须使用 YYYY-MM-DD 格式并且是真实日期。"
        "birth.solar_datetime",
        "birth.true_solar_datetime",
        "professional.observed_at",
        -> "时间必须使用 YYYY-MM-DD HH:MM:SS 格式并且是真实时间。"
        "birth.latitude" -> "纬度必须是 -90 到 90 之间的数字。"
        "birth.longitude" -> "经度必须是 -180 到 180 之间的数字。"
        "chart.four_pillars" -> "四柱必须按“年柱 月柱 日柱 时柱”填写，例如：壬申 戊申 壬申 丙午。"
        "professional.flow_year",
        "professional.flow_month",
        "professional.flow_day",
        "professional.flow_hour",
        "professional.decade",
        "professional.natal_year",
        "professional.natal_month",
        "professional.natal_day",
        "professional.natal_hour",
        -> "干支必须填写一组有效天干地支，例如：丙午。"
        else -> "修正值不能为空。"
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ScreenshotImportViewModel::class.java))
            return ScreenshotImportViewModel(container) as T
        }
    }

    private companion object {
        const val PARSER_VERSION = "wenzhen-p0-v5"
        val REQUIRED_COMMIT_FIELD_KEYS = listOf(
            "identity.alias",
            "identity.sex",
            "birth.solar_date",
            "chart.four_pillars",
        )
        val FOUR_PILLAR_PATTERN = Regex(
            "[甲乙丙丁戊己庚辛壬癸][子丑寅卯辰巳午未申酉戌亥]",
        )
        val EVENT_FIELD_PATTERN = Regex(
            "event\\.candidate\\.((?:19|20)\\d{2})\\.\\d+",
        )
        val CHART_FIELD_PATTERN = Regex(
            "chart\\.(year|month|day|hour)\\." +
                "(main_star|hidden_stems|secondary_stars|fortune_stage|" +
                "self_stage|void|nayin|spirits)",
        )
        val CHART_COLUMN_LABELS = mapOf(
            "year" to "年柱",
            "month" to "月柱",
            "day" to "日柱",
            "hour" to "时柱",
        )
        val CHART_ROW_LABELS = mapOf(
            "main_star" to "主星",
            "hidden_stems" to "藏干",
            "secondary_stars" to "副星",
            "fortune_stage" to "星运",
            "self_stage" to "自坐",
            "void" to "空亡",
            "nayin" to "纳音",
            "spirits" to "神煞",
        )
        const val LOW_CONFIDENCE_THRESHOLD = 0.7f
        const val LOW_GROUPING_CONFIDENCE_THRESHOLD = 0.75f
    }
}
