package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportFailure
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportSourceApp
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
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

data class ScreenshotImportUiState(
    val busy: Boolean = false,
    val progressText: String? = null,
    val activeSessionId: String? = null,
    val completedImageCount: Int = 0,
    val classifiedPageTypes: List<WenzhenPageType> = emptyList(),
    val exactDuplicatePairCount: Int = 0,
    val similarDuplicatePairCount: Int = 0,
    val failedImageCount: Int = 0,
    val needsReview: Boolean = false,
    val canRetry: Boolean = false,
    val recoverableSessionCount: Int = 0,
    val message: String? = null,
)

class ScreenshotImportViewModel(
    private val repository: ImportSessionRepository,
    private val imageStore: PrivateImportImageStore,
    private val recognitionScheduler: ScreenshotRecognitionScheduler,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    constructor(container: AppContainer) : this(
        repository = container.importSessionRepository,
        imageStore = container.importImageStore,
        recognitionScheduler = container.screenshotRecognitionScheduler,
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
            val sessions = repository.list(
                ImportStatus.entries.toSet() - setOf(
                    ImportStatus.COMPLETED,
                    ImportStatus.CANCELLED,
                ),
            )
            mutableState.update { current ->
                val recent = sessions.firstOrNull()
                if (recent == null || current.busy || current.activeSessionId != null) {
                    current.copy(recoverableSessionCount = sessions.size)
                } else {
                    val duplicateCounts = duplicateCounts(recent)
                    current.copy(
                        activeSessionId = recent.id,
                        completedImageCount = recent.images.size,
                        classifiedPageTypes = recent.images.map { it.pageType },
                        exactDuplicatePairCount = duplicateCounts.first,
                        similarDuplicatePairCount = duplicateCounts.second,
                        failedImageCount = recent.imageFailures.size,
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
    }
}
