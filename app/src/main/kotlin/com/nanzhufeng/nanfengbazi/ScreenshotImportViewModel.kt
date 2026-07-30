package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
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
)

data class ScreenshotLongTextReviewUi(
    val id: String,
    val label: String,
    val rawText: String,
    val adopted: Boolean,
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
    val needsReview: Boolean = false,
    val canRetry: Boolean = false,
    val recoverableSessionCount: Int = 0,
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

    fun commitCandidate(candidateId: String) {
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
                )
            }
            when (val result = committer.commitCandidate(sessionId, candidateId)) {
                is ScreenshotCandidateCommitResult.Committed -> {
                    if (result.sessionCompleted) {
                        mutableState.update {
                            it.copy(
                                activeSessionId = null,
                                needsReview = false,
                                reviewCandidates = emptyList(),
                                committingCandidateId = null,
                                committedCaseCount = it.committedCaseCount + 1,
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
                                message = "当前候选已写入正式命例，其余候选仍待核对。",
                            )
                        }
                    }
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
                    completedImageCount = result.session.images.size,
                    classifiedPageTypes = result.session.images.map { image -> image.pageType },
                    exactDuplicatePairCount = duplicateCounts.first,
                    similarDuplicatePairCount = duplicateCounts.second,
                    failedImageCount = result.session.imageFailures.size,
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
                    completedImageCount = result.session.images.size,
                    failedImageCount = result.session.imageFailures.size,
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
                        completedImageCount = recent.images.size,
                        classifiedPageTypes = recent.images.map { it.pageType },
                        exactDuplicatePairCount = duplicateCounts.first,
                        similarDuplicatePairCount = duplicateCounts.second,
                        failedImageCount = recent.imageFailures.size,
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
        return caseCandidates.map { candidate ->
            candidate.toReviewUi(fieldsById, longTextsById)
        }
    }

    private fun ImportCaseCandidate.toReviewUi(
        fieldsById: Map<String, CaseFieldEvidence>,
        longTextsById: Map<String, ImportedLongTextEvidence>,
    ) = ScreenshotCandidateReviewUi(
        id = id,
        alias = suggestedAlias ?: "未命名候选",
        imageCount = imageIds.size,
        fields = fieldEvidenceIds.mapNotNull(fieldsById::get).map { field ->
            ScreenshotFieldReviewUi(
                id = field.id,
                label = field.fieldKey.displayLabel(),
                sourceValue = field.rawText,
                normalizedValue = field.normalizedValue?.displayValue(),
                calculationValue = if (field.fieldKey == "chart.four_pillars") {
                    "提交前按采用的出生资料复算"
                } else {
                    "不参与命盘计算"
                },
                adoptedValue = field.adoptedValue?.displayValue(),
                confidencePercent = listOfNotNull(
                    field.ocrConfidence,
                    field.parserConfidence,
                    field.consistencyConfidence,
                ).minOrNull()?.times(100)?.toInt(),
            )
        },
        longTexts = longTextEvidenceIds.mapNotNull(longTextsById::get).map { text ->
            ScreenshotLongTextReviewUi(
                id = text.id,
                label = when (text.type) {
                    ImportedLongTextType.OWNER_FEEDBACK -> "命主反馈"
                    ImportedLongTextType.MASTER_COMMENTARY -> "师傅点评"
                    ImportedLongTextType.UNKNOWN -> "其他原文"
                },
                rawText = text.rawText,
                adopted = text.adopted,
            )
        },
        fullyAdopted = fieldEvidenceIds
            .mapNotNull(fieldsById::get)
            .takeIf { it.isNotEmpty() }
            ?.all { it.adoptedValue != null } == true &&
            longTextEvidenceIds
                .mapNotNull(longTextsById::get)
                .all(ImportedLongTextEvidence::adopted),
        readyToCommit = REQUIRED_COMMIT_FIELD_KEYS.all { requiredKey ->
            fieldEvidenceIds
                .mapNotNull(fieldsById::get)
                .any { field ->
                    field.fieldKey == requiredKey && field.adoptedValue != null
                }
        },
        missingRequiredFields = REQUIRED_COMMIT_FIELD_KEYS.mapNotNull { requiredKey ->
            requiredKey.displayLabel().takeUnless {
                fieldEvidenceIds
                    .mapNotNull(fieldsById::get)
                    .any { field ->
                        field.fieldKey == requiredKey && field.adoptedValue != null
                    }
            }
        },
        targetCaseId = targetCaseId,
    )

    private suspend fun importSessionRepositorySession(sessionId: String): ImportSession? =
        repository.findById(sessionId)

    private fun String.displayLabel(): String = when (this) {
        "identity.alias" -> "命例名称"
        "identity.sex" -> "性别"
        "birth.solar_date" -> "公历生日"
        "chart.four_pillars" -> "四柱"
        else -> this
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
        const val PARSER_VERSION = "wenzhen-p0-v1"
        val REQUIRED_COMMIT_FIELD_KEYS = listOf(
            "identity.alias",
            "identity.sex",
            "birth.solar_date",
            "chart.four_pillars",
        )
    }
}
