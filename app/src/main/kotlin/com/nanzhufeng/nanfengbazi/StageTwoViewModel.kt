package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.nanfengbazi.data.backup.BackupExportResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupPreviewResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupProtection
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreAction
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreDecision
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseMergePreparation
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseMergePreparationResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlan
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreExecutionResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlanResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreRecoveryResult
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupOperations
import com.nanzhufeng.nanfengbazi.data.backup.RestorePreview
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExchangeService
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseBundleOperations
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocumentProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportDecision
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePlan
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePreparation
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePreparationResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreview
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreviewResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.file.Path
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AppDestination {
    data object CaseList : AppDestination
    data object CreateCase : AppDestination
    data class CaseDetail(val caseId: String) : AppDestination
    data class EditCase(val caseId: String) : AppDestination
    data class EditMetadata(val caseId: String) : AppDestination
    data class EditTextRecord(
        val caseId: String,
        val recordId: String?,
    ) : AppDestination
    data class EditEvent(
        val caseId: String,
        val eventId: String?,
    ) : AppDestination
}

class StageTwoNavigator {
    private val stack = mutableListOf<AppDestination>(AppDestination.CaseList)
    val current: AppDestination
        get() = stack.last()

    fun openCreate(): AppDestination {
        return push(AppDestination.CreateCase)
    }

    fun openDetail(caseId: String): AppDestination {
        return push(AppDestination.CaseDetail(caseId))
    }

    fun openEditCase(caseId: String): AppDestination {
        return push(AppDestination.EditCase(caseId))
    }

    fun openMetadata(caseId: String): AppDestination {
        return push(AppDestination.EditMetadata(caseId))
    }

    fun openTextRecord(caseId: String, recordId: String?): AppDestination {
        return push(AppDestination.EditTextRecord(caseId, recordId))
    }

    fun openEvent(caseId: String, eventId: String?): AppDestination {
        return push(AppDestination.EditEvent(caseId, eventId))
    }

    fun back(): AppDestination {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        return current
    }

    fun backToList(): AppDestination {
        stack.clear()
        stack += AppDestination.CaseList
        return current
    }

    private fun push(destination: AppDestination): AppDestination {
        stack += destination
        return current
    }
}

data class StageTwoUiState(
    val destination: AppDestination = AppDestination.CaseList,
    val query: String = "",
    val selectedGroupId: String? = null,
    val selectedTagId: String? = null,
    val sortOrder: CaseSortOrder = CaseSortOrder.UPDATED_DESC,
    val visibility: CaseVisibility = CaseVisibility.ACTIVE,
    val availableGroups: List<CaseGroup> = emptyList(),
    val availableTags: List<CaseTag> = emptyList(),
    val cases: List<CaseSummary> = emptyList(),
    val listLoading: Boolean = false,
    val listError: String? = null,
    val form: CaseFormState = CaseFormState(),
    val formError: String? = null,
    val saving: Boolean = false,
    val previewing: Boolean = false,
    val instantCalculation: CalculationResult? = null,
    val duplicateCandidates: List<DuplicateCaseCandidate> = emptyList(),
    val detail: BaziCase? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val editForm: CaseFormState = CaseFormState(),
    val metadataDraft: CaseMetadataDraft = CaseMetadataDraft(),
    val recordDraft: TextRecordDraft = TextRecordDraft(),
    val eventDraft: EventDraft = EventDraft(),
    val mutationSaving: Boolean = false,
    val mutationError: String? = null,
    val deleteConfirmationVisible: Boolean = false,
    val singleCaseExportConfirmationVisible: Boolean = false,
    val singleCasePasswordExportVisible: Boolean = false,
    val singleCasePasswordImportVisible: Boolean = false,
    val singleCasePasswordCommitVisible: Boolean = false,
    val singleCasePasswordError: String? = null,
    val singleCaseExportIncludesAttachments: Boolean = false,
    val singleCaseExchangeBusy: Boolean = false,
    val singleCasePreview: SingleCasePreview? = null,
    val singleCaseMergePreparation: SingleCaseMergePreparation? = null,
    val singleCaseMergeModules: Set<SingleCaseMergeModule> = emptySet(),
    val singleCaseFieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice> = emptyMap(),
    val singleCaseExchangeError: String? = null,
    val fullBackupExportConfirmationVisible: Boolean = false,
    val fullBackupPasswordExportVisible: Boolean = false,
    val fullBackupPasswordImportVisible: Boolean = false,
    val fullBackupPasswordError: String? = null,
    val fullBackupBusy: Boolean = false,
    val fullBackupPreview: RestorePreview? = null,
    val fullBackupDecisions: Map<String, BackupCaseRestoreDecision> = emptyMap(),
    val fullBackupMergePreparation: BackupCaseMergePreparation? = null,
    val fullBackupMergeModules: Set<SingleCaseMergeModule> = emptySet(),
    val fullBackupMergeFieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice> = emptyMap(),
    val fullBackupRestorePlan: BackupRestorePlan? = null,
    val fullBackupRestoreConfirmationVisible: Boolean = false,
    val fullBackupRestorePasswordVisible: Boolean = false,
    val fullBackupError: String? = null,
    val message: String? = null,
)

private sealed interface PendingSingleCaseBundleCommit {
    data class Import(
        val decision: SingleCaseImportDecision,
    ) : PendingSingleCaseBundleCommit

    data class Merge(
        val plan: SingleCaseMergePlan,
    ) : PendingSingleCaseBundleCommit
}

class StageTwoViewModel(
    private val caseRepository: CaseRepository,
    private val createCase: CreateCaseUseCase,
    private val editCase: EditCaseUseCase,
    private val caseMetadata: CaseMetadataUseCase = CaseMetadataUseCase(caseRepository),
    private val textRecords: TextRecordUseCase,
    private val caseEvents: CaseEventUseCase,
    private val caseLifecycle: CaseLifecycleUseCase = CaseLifecycleUseCase(caseRepository),
    private val navigator: StageTwoNavigator = StageTwoNavigator(),
    private val clock: Clock = Clock.systemUTC(),
    private val singleCaseExchange: SingleCaseExchangeService =
        SingleCaseExchangeService(caseRepository, clock),
    private val singleCaseBundleService: SingleCaseBundleOperations? = null,
    private val caseBackupService: CaseBackupOperations? = null,
    private val backupAttachmentRoot: Path? = null,
    private val backupWorkRoot: Path? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(StageTwoUiState())
    val state: StateFlow<StageTwoUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var pendingExportPassword: CharArray? = null
    private var pendingSingleCaseBundleExport: Boolean = false
    private var pendingSingleCaseBundleCommit: PendingSingleCaseBundleCommit? = null
    private var pendingFullBackupPassword: CharArray? = null

    init {
        recoverInterruptedRestores()
        refreshCases()
    }

    private fun recoverInterruptedRestores() {
        val service = caseBackupService ?: return
        val attachmentRoot = backupAttachmentRoot ?: return
        viewModelScope.launch {
            val result = try {
                withContext(ioDispatcher) {
                    service.recoverInterruptedRestores(attachmentRoot)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BackupRestoreRecoveryResult.RequiresAttention(
                    code = "RESTORE_RECOVERY_FAILED",
                    message = "启动时无法检查未完成恢复事务，已停止自动处理。",
                )
            }
            mutableState.update {
                when (result) {
                    is BackupRestoreRecoveryResult.Success -> {
                        val recovered = result.rolledBackTransactions +
                            result.finalizedTransactions
                        if (recovered == 0) {
                            it
                        } else {
                            it.copy(
                                message =
                                    "已检查未完成恢复：回滚 ${result.rolledBackTransactions} 个，" +
                                        "完成收尾 ${result.finalizedTransactions} 个。",
                            )
                        }
                    }
                    is BackupRestoreRecoveryResult.RequiresAttention -> it.copy(
                        fullBackupError = "${result.message}（${result.code}）",
                    )
                }
            }
        }
    }

    fun refreshCases() {
        searchJob?.cancel()
        val current = mutableState.value
        val request = CaseSearchRequest(
            query = current.query,
            groupId = current.selectedGroupId,
            tagId = current.selectedTagId,
            sortOrder = current.sortOrder,
            visibility = current.visibility,
        )
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(listLoading = true, listError = null) }
            try {
                val catalogRequest = CaseSearchRequest(visibility = current.visibility)
                val allCases = caseRepository.search(catalogRequest)
                val cases = if (request == catalogRequest) {
                    allCases
                } else {
                    caseRepository.search(request)
                }
                mutableState.update {
                    it.copy(
                        cases = cases,
                        availableGroups = allCases.flatMap { item -> item.groups }
                            .distinctBy { group -> group.id }
                            .sortedBy { group -> group.name },
                        availableTags = allCases.flatMap { item -> item.tags }
                            .distinctBy { tag -> tag.id }
                            .sortedBy { tag -> tag.name },
                        listLoading = false,
                        listError = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        listLoading = false,
                        listError = "命例列表读取失败，请点击重试。",
                    )
                }
            }
        }
    }

    fun requestFullBackupExport() {
        if (mutableState.value.fullBackupBusy || mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(
                fullBackupExportConfirmationVisible = true,
                fullBackupError = null,
            )
        }
    }

    fun cancelFullBackupExport() {
        if (mutableState.value.fullBackupBusy) return
        pendingFullBackupPassword?.fill('\u0000')
        pendingFullBackupPassword = null
        mutableState.update { it.copy(fullBackupExportConfirmationVisible = false) }
    }

    fun confirmFullBackupExport(): String? {
        val service = caseBackupService
        if (service == null || backupAttachmentRoot == null) {
            mutableState.update {
                it.copy(
                    fullBackupExportConfirmationVisible = false,
                    fullBackupError = "完整备份服务尚未就绪（BACKUP_SERVICE_UNAVAILABLE）。",
                )
            }
            return null
        }
        pendingFullBackupPassword?.fill('\u0000')
        pendingFullBackupPassword = null
        mutableState.update {
            it.copy(
                fullBackupExportConfirmationVisible = false,
                fullBackupError = null,
            )
        }
        return service.suggestedFileName()
    }

    fun requestPasswordFullBackupExport() {
        if (mutableState.value.fullBackupBusy || caseBackupService == null) return
        mutableState.update {
            it.copy(
                fullBackupExportConfirmationVisible = false,
                fullBackupPasswordExportVisible = true,
                fullBackupPasswordError = null,
            )
        }
    }

    fun cancelPasswordFullBackupExport() {
        pendingFullBackupPassword?.fill('\u0000')
        pendingFullBackupPassword = null
        mutableState.update {
            it.copy(
                fullBackupPasswordExportVisible = false,
                fullBackupPasswordError = null,
            )
        }
    }

    fun confirmPasswordFullBackupExport(
        password: CharArray,
        confirmation: CharArray,
    ): String? {
        val service = caseBackupService
        if (service == null || backupAttachmentRoot == null) {
            password.fill('\u0000')
            confirmation.fill('\u0000')
            mutableState.update {
                it.copy(fullBackupError = "完整备份服务尚未就绪（BACKUP_SERVICE_UNAVAILABLE）。")
            }
            return null
        }
        val error = validateExportPassword(password, confirmation)
        confirmation.fill('\u0000')
        if (error != null) {
            password.fill('\u0000')
            mutableState.update { it.copy(fullBackupPasswordError = error) }
            return null
        }
        pendingFullBackupPassword?.fill('\u0000')
        pendingFullBackupPassword = password.copyOf()
        password.fill('\u0000')
        mutableState.update {
            it.copy(
                fullBackupPasswordExportVisible = false,
                fullBackupPasswordError = null,
                fullBackupError = null,
            )
        }
        return service.suggestedEncryptedFileName()
    }

    fun clearPendingFullBackupExport() {
        pendingFullBackupPassword?.fill('\u0000')
        pendingFullBackupPassword = null
    }

    fun exportFullBackup(openOutput: () -> OutputStream?) {
        if (mutableState.value.fullBackupBusy || mutableState.value.singleCaseExchangeBusy) {
            clearPendingFullBackupExport()
            return
        }
        val service = caseBackupService
        val attachmentRoot = backupAttachmentRoot
        if (service == null || attachmentRoot == null) {
            clearPendingFullBackupExport()
            mutableState.update {
                it.copy(fullBackupError = "完整备份服务尚未就绪（BACKUP_SERVICE_UNAVAILABLE）。")
            }
            return
        }
        val exportPassword = pendingFullBackupPassword
        pendingFullBackupPassword = null
        val passwordProtected = exportPassword != null
        viewModelScope.launch {
            mutableState.update { it.copy(fullBackupBusy = true, fullBackupError = null) }
            val result = try {
                withContext(ioDispatcher) {
                    try {
                        val output = openOutput()
                            ?: return@withContext BackupExportResult.Rejected(
                                code = "OUTPUT_OPEN_FAILED",
                                message = "无法创建完整备份文件。",
                            )
                        service.export(
                            output = output,
                            attachmentRoot = attachmentRoot,
                            appVersion = BuildConfig.VERSION_NAME,
                            protection = if (exportPassword == null) {
                                BackupProtection.UnencryptedSensitiveDataConfirmed
                            } else {
                                BackupProtection.PasswordProtected(exportPassword)
                            },
                        )
                    } finally {
                        exportPassword?.fill('\u0000')
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BackupExportResult.Rejected(
                    code = "OUTPUT_OPEN_FAILED",
                    message = "无法创建或写入完整备份文件。",
                )
            }
            mutableState.update {
                when (result) {
                    is BackupExportResult.Success -> it.copy(
                        fullBackupBusy = false,
                        message = if (passwordProtected) {
                            "密码加密完整备份已导出：${result.counts.cases} 个命例、" +
                                "${result.counts.attachments} 个附件；请另行保管密码。"
                        } else {
                            "完整未加密备份已导出：${result.counts.cases} 个命例、" +
                                "${result.counts.attachments} 个附件。"
                        },
                    )
                    is BackupExportResult.Rejected -> it.copy(
                        fullBackupBusy = false,
                        fullBackupError = "${result.message}（${result.code}）",
                    )
                }
            }
        }
    }

    fun previewFullBackup(openInput: () -> InputStream?) {
        previewFullBackup(openInput, password = null)
    }

    fun previewFullBackupWithPassword(
        password: CharArray,
        openInput: () -> InputStream?,
    ) {
        if (password.isEmpty()) {
            password.fill('\u0000')
            mutableState.update { it.copy(fullBackupPasswordError = "请输入解密密码。") }
            return
        }
        previewFullBackup(openInput, password)
    }

    private fun previewFullBackup(
        openInput: () -> InputStream?,
        password: CharArray?,
    ) {
        if (mutableState.value.fullBackupBusy || mutableState.value.singleCaseExchangeBusy) {
            password?.fill('\u0000')
            return
        }
        val service = caseBackupService
        val workRoot = backupWorkRoot
        if (service == null || workRoot == null) {
            password?.fill('\u0000')
            mutableState.update {
                it.copy(fullBackupError = "完整备份服务尚未就绪（BACKUP_SERVICE_UNAVAILABLE）。")
            }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    fullBackupBusy = true,
                    fullBackupPreview = null,
                    fullBackupError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    val input = openInput()
                        ?: return@withContext BackupPreviewResult.Rejected(
                            code = "INPUT_OPEN_FAILED",
                            message = "无法打开所选完整备份文件。",
                        )
                    input.use { service.preview(it, workRoot, password) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BackupPreviewResult.Rejected(
                    code = "INPUT_OPEN_FAILED",
                    message = "无法读取所选完整备份文件。",
                )
            } finally {
                password?.fill('\u0000')
            }
            mutableState.update {
                when (result) {
                    is BackupPreviewResult.Success -> it.copy(
                        fullBackupBusy = false,
                        fullBackupPreview = result.preview,
                        fullBackupDecisions = emptyMap(),
                        fullBackupMergePreparation = null,
                        fullBackupMergeModules = emptySet(),
                        fullBackupMergeFieldChoices = emptyMap(),
                        fullBackupRestorePlan = null,
                        fullBackupPasswordImportVisible = false,
                        fullBackupPasswordError = null,
                    )
                    is BackupPreviewResult.Rejected -> {
                        if (
                            result.code == "PASSWORD_REQUIRED" ||
                            result.code == "DECRYPTION_FAILED"
                        ) {
                            it.copy(
                                fullBackupBusy = false,
                                fullBackupPasswordImportVisible = true,
                                fullBackupPasswordError =
                                    if (result.code == "PASSWORD_REQUIRED") {
                                        null
                                    } else {
                                        "${result.message}（${result.code}）"
                                    },
                            )
                        } else {
                            it.copy(
                                fullBackupBusy = false,
                                fullBackupPasswordImportVisible = false,
                                fullBackupPasswordError = null,
                                fullBackupError = "${result.message}（${result.code}）",
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelPasswordFullBackupImport() {
        if (mutableState.value.fullBackupBusy) return
        mutableState.update {
            it.copy(
                fullBackupPasswordImportVisible = false,
                fullBackupPasswordError = null,
            )
        }
    }

    fun dismissFullBackupPreview() {
        if (mutableState.value.fullBackupBusy) return
        mutableState.update {
            it.copy(
                fullBackupPreview = null,
                fullBackupDecisions = emptyMap(),
                fullBackupMergePreparation = null,
                fullBackupMergeModules = emptySet(),
                fullBackupMergeFieldChoices = emptyMap(),
                fullBackupRestorePlan = null,
            )
        }
    }

    fun chooseFullBackupDecision(
        sourceCaseId: String,
        action: BackupCaseRestoreAction,
    ) {
        if (mutableState.value.fullBackupBusy) return
        val source = mutableState.value.fullBackupPreview
            ?.cases
            ?.singleOrNull { it.sourceCaseId == sourceCaseId }
            ?: return
        val allowed = when (action) {
            BackupCaseRestoreAction.IMPORT_AS_IS -> source.conflicts.isEmpty()
            BackupCaseRestoreAction.SKIP -> true
            BackupCaseRestoreAction.KEEP_BOTH -> source.conflicts.isNotEmpty()
            BackupCaseRestoreAction.MERGE -> false
        }
        if (!allowed) return
        val decision = BackupCaseRestoreDecision(sourceCaseId, action)
        mutableState.update {
            it.copy(
                fullBackupDecisions = it.fullBackupDecisions + (sourceCaseId to decision),
                fullBackupRestorePlan = null,
                fullBackupError = null,
            )
        }
    }

    fun skipAllFullBackupCases() {
        if (mutableState.value.fullBackupBusy) return
        val preview = mutableState.value.fullBackupPreview ?: return
        val decisions = preview.cases.associate { source ->
            source.sourceCaseId to BackupCaseRestoreDecision(
                sourceCaseId = source.sourceCaseId,
                action = BackupCaseRestoreAction.SKIP,
            )
        }
        mutableState.update {
            it.copy(
                fullBackupDecisions = decisions,
                fullBackupMergePreparation = null,
                fullBackupMergeModules = emptySet(),
                fullBackupMergeFieldChoices = emptyMap(),
                fullBackupRestorePlan = null,
                fullBackupError = null,
            )
        }
    }

    fun prepareFullBackupRestorePlan() {
        val service = caseBackupService ?: return
        val preview = mutableState.value.fullBackupPreview ?: return
        if (mutableState.value.fullBackupBusy) return
        val decisions = preview.cases.mapNotNull {
            mutableState.value.fullBackupDecisions[it.sourceCaseId]
        }
        viewModelScope.launch {
            mutableState.update { it.copy(fullBackupBusy = true, fullBackupError = null) }
            val result = try {
                withContext(ioDispatcher) { service.prepareRestorePlan(preview, decisions) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BackupRestorePlanResult.Rejected(
                    code = "PLAN_PREPARATION_FAILED",
                    message = "无法生成完整备份恢复方案。",
                )
            }
            mutableState.update {
                when (result) {
                    is BackupRestorePlanResult.Success -> it.copy(
                        fullBackupBusy = false,
                        fullBackupRestorePlan = result.plan,
                        message = "恢复方案已通过过期与范围检查；尚未执行写入。",
                    )
                    is BackupRestorePlanResult.Rejected -> it.copy(
                        fullBackupBusy = false,
                        fullBackupRestorePlan = null,
                        fullBackupError = "${result.message}（${result.code}）",
                    )
                }
            }
        }
    }

    fun prepareFullBackupCaseMerge(sourceCaseId: String, targetCaseId: String) {
        val service = caseBackupService ?: return
        val preview = mutableState.value.fullBackupPreview ?: return
        if (mutableState.value.fullBackupBusy) return
        viewModelScope.launch {
            mutableState.update { it.copy(fullBackupBusy = true, fullBackupError = null) }
            val result = try {
                withContext(ioDispatcher) {
                    service.prepareCaseMerge(preview, sourceCaseId, targetCaseId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BackupCaseMergePreparationResult.Rejected(
                    "MERGE_PREPARATION_FAILED",
                    "无法生成该命例的合并差异。",
                )
            }
            mutableState.update {
                when (result) {
                    is BackupCaseMergePreparationResult.Success -> it.copy(
                        fullBackupBusy = false,
                        fullBackupMergePreparation = result.preparation,
                        fullBackupMergeModules = emptySet(),
                        fullBackupMergeFieldChoices = emptyMap(),
                        fullBackupRestorePlan = null,
                    )
                    is BackupCaseMergePreparationResult.Rejected -> it.copy(
                        fullBackupBusy = false,
                        fullBackupError = "${result.message}（${result.code}）",
                    )
                }
            }
        }
    }

    fun toggleFullBackupMergeModule(module: SingleCaseMergeModule) {
        if (mutableState.value.fullBackupBusy) return
        mutableState.update {
            val selected = it.fullBackupMergeModules
            it.copy(
                fullBackupMergeModules = if (module in selected) selected - module else selected + module,
                fullBackupRestorePlan = null,
            )
        }
    }

    fun chooseFullBackupMergeField(key: SingleCaseFieldKey, choice: SingleCaseValueChoice) {
        if (mutableState.value.fullBackupBusy) return
        mutableState.update {
            it.copy(
                fullBackupMergeFieldChoices = it.fullBackupMergeFieldChoices + (key to choice),
                fullBackupRestorePlan = null,
            )
        }
    }

    fun confirmFullBackupMergeDecision() {
        val preparation = mutableState.value.fullBackupMergePreparation ?: return
        if (mutableState.value.fullBackupBusy) return
        val decision = BackupCaseRestoreDecision(
            sourceCaseId = preparation.sourceCaseId,
            action = BackupCaseRestoreAction.MERGE,
            targetCaseId = preparation.targetCaseId,
            modules = mutableState.value.fullBackupMergeModules,
            fieldChoices = mutableState.value.fullBackupMergeFieldChoices,
        )
        mutableState.update {
            it.copy(
                fullBackupDecisions = it.fullBackupDecisions +
                    (preparation.sourceCaseId to decision),
                fullBackupMergePreparation = null,
                fullBackupMergeModules = emptySet(),
                fullBackupMergeFieldChoices = emptyMap(),
                fullBackupRestorePlan = null,
            )
        }
    }

    fun cancelFullBackupCaseMerge() {
        if (mutableState.value.fullBackupBusy) return
        mutableState.update {
            it.copy(
                fullBackupMergePreparation = null,
                fullBackupMergeModules = emptySet(),
                fullBackupMergeFieldChoices = emptyMap(),
            )
        }
    }

    fun requestFullBackupRestore() {
        if (mutableState.value.fullBackupBusy) return
        if (mutableState.value.fullBackupRestorePlan == null) return
        mutableState.update {
            it.copy(
                fullBackupRestoreConfirmationVisible = true,
                fullBackupPasswordError = null,
                fullBackupError = null,
            )
        }
    }

    fun cancelFullBackupRestore() {
        if (mutableState.value.fullBackupBusy) return
        mutableState.update {
            it.copy(
                fullBackupRestoreConfirmationVisible = false,
                fullBackupRestorePasswordVisible = false,
                fullBackupPasswordError = null,
            )
        }
    }

    fun confirmFullBackupRestore(): Boolean {
        if (mutableState.value.fullBackupBusy) return false
        val plan = mutableState.value.fullBackupRestorePlan ?: return false
        return if (plan.preview.manifest.encrypted) {
            mutableState.update {
                it.copy(
                    fullBackupRestoreConfirmationVisible = false,
                    fullBackupRestorePasswordVisible = true,
                    fullBackupPasswordError = null,
                )
            }
            false
        } else {
            mutableState.update {
                it.copy(fullBackupRestoreConfirmationVisible = false)
            }
            true
        }
    }

    fun executeFullBackupRestore(
        password: CharArray? = null,
        openInput: () -> InputStream?,
    ) {
        if (mutableState.value.fullBackupBusy) {
            password?.fill('\u0000')
            return
        }
        val service = caseBackupService
        val workRoot = backupWorkRoot
        val attachmentRoot = backupAttachmentRoot
        val plan = mutableState.value.fullBackupRestorePlan
        if (service == null || workRoot == null || attachmentRoot == null || plan == null) {
            password?.fill('\u0000')
            mutableState.update {
                it.copy(
                    fullBackupRestoreConfirmationVisible = false,
                    fullBackupRestorePasswordVisible = false,
                    fullBackupError = "完整备份恢复服务尚未就绪（RESTORE_SERVICE_UNAVAILABLE）。",
                )
            }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    fullBackupBusy = true,
                    fullBackupRestoreConfirmationVisible = false,
                    fullBackupRestorePasswordVisible = false,
                    fullBackupPasswordError = null,
                    fullBackupError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    val input = openInput()
                        ?: return@withContext BackupRestoreExecutionResult.Rejected(
                            code = "INPUT_OPEN_FAILED",
                            message = "无法重新打开生成恢复方案的备份文件。",
                        )
                    input.use {
                        service.executeRestorePlan(
                            plan = plan,
                            input = it,
                            workRoot = workRoot,
                            attachmentRoot = attachmentRoot,
                            password = password,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BackupRestoreExecutionResult.Rejected(
                    code = "RESTORE_EXECUTION_FAILED",
                    message = "完整备份恢复失败，未确认任何写入成功。",
                )
            } finally {
                password?.fill('\u0000')
            }
            mutableState.update {
                when (result) {
                    is BackupRestoreExecutionResult.Success -> it.copy(
                        fullBackupBusy = false,
                        fullBackupPreview = null,
                        fullBackupDecisions = emptyMap(),
                        fullBackupMergePreparation = null,
                        fullBackupMergeModules = emptySet(),
                        fullBackupMergeFieldChoices = emptyMap(),
                        fullBackupRestorePlan = null,
                        message =
                            "完整备份恢复完成：导入 ${result.summary.importedCases}，" +
                                "保留两份 ${result.summary.keptBothCases}，" +
                                "合并 ${result.summary.mergedCases}，" +
                                "跳过 ${result.summary.skippedCases}，" +
                                "附件 ${result.summary.restoredAttachments}。",
                    )
                    is BackupRestoreExecutionResult.Rejected -> {
                        if (
                            result.code == "PASSWORD_REQUIRED" ||
                            result.code == "DECRYPTION_FAILED"
                        ) {
                            it.copy(
                                fullBackupBusy = false,
                                fullBackupRestorePasswordVisible = true,
                                fullBackupPasswordError =
                                    "${result.message}（${result.code}）",
                            )
                        } else {
                            it.copy(
                                fullBackupBusy = false,
                                fullBackupError = "${result.message}（${result.code}）",
                            )
                        }
                    }
                }
            }
            if (result is BackupRestoreExecutionResult.Success) {
                refreshCases()
            }
        }
    }

    fun dismissFullBackupError() {
        mutableState.update { it.copy(fullBackupError = null) }
    }

    fun requestSingleCaseExport() {
        val detail = mutableState.value.detail
        if (detail == null || mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(
                singleCaseExportConfirmationVisible = true,
                singleCaseExportIncludesAttachments = detail.attachments.isNotEmpty(),
                singleCaseExchangeError = null,
            )
        }
    }

    fun chooseSingleCaseExportAttachments(include: Boolean) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(
                singleCaseExportIncludesAttachments =
                    include && detail.attachments.isNotEmpty(),
            )
        }
    }

    fun cancelSingleCaseExport() {
        pendingSingleCaseBundleExport = false
        mutableState.update {
            it.copy(
                singleCaseExportConfirmationVisible = false,
                singleCaseExportIncludesAttachments = false,
            )
        }
    }

    fun confirmSingleCaseExport(): String? {
        val detail = mutableState.value.detail ?: return null
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
        pendingSingleCaseBundleExport =
            mutableState.value.singleCaseExportIncludesAttachments &&
                detail.attachments.isNotEmpty()
        mutableState.update {
            it.copy(
                singleCaseExportConfirmationVisible = false,
                singleCasePasswordExportVisible = false,
                singleCasePasswordError = null,
                singleCaseExchangeError = null,
            )
        }
        return if (pendingSingleCaseBundleExport) {
            singleCaseBundleService?.suggestedFileName(detail)
        } else {
            singleCaseExchange.suggestedFileName(detail)
        }
    }

    fun requestPasswordSingleCaseExport() {
        if (mutableState.value.detail == null || mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(
                singleCaseExportConfirmationVisible = false,
                singleCasePasswordExportVisible = true,
                singleCasePasswordError = null,
            )
        }
    }

    fun cancelPasswordSingleCaseExport() {
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
        mutableState.update {
            it.copy(
                singleCasePasswordExportVisible = false,
                singleCasePasswordError = null,
            )
        }
    }

    fun confirmPasswordSingleCaseExport(
        password: CharArray,
        confirmation: CharArray,
    ): String? {
        val detail = mutableState.value.detail
        if (detail == null) {
            password.fill('\u0000')
            confirmation.fill('\u0000')
            return null
        }
        val error = when {
            password.size < MIN_EXPORT_PASSWORD_LENGTH ->
                "密码至少需要 $MIN_EXPORT_PASSWORD_LENGTH 个字符。"
            password.size > MAX_EXPORT_PASSWORD_LENGTH ->
                "密码不能超过 $MAX_EXPORT_PASSWORD_LENGTH 个字符。"
            !password.contentEquals(confirmation) -> "两次输入的密码不一致。"
            else -> null
        }
        confirmation.fill('\u0000')
        if (error != null) {
            password.fill('\u0000')
            mutableState.update { it.copy(singleCasePasswordError = error) }
            return null
        }
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = password.copyOf()
        pendingSingleCaseBundleExport =
            mutableState.value.singleCaseExportIncludesAttachments &&
                detail.attachments.isNotEmpty()
        password.fill('\u0000')
        mutableState.update {
            it.copy(
                singleCasePasswordExportVisible = false,
                singleCasePasswordError = null,
                singleCaseExchangeError = null,
            )
        }
        return if (pendingSingleCaseBundleExport) {
            singleCaseBundleService?.suggestedEncryptedFileName(detail)
        } else {
            singleCaseExchange.suggestedEncryptedFileName(detail)
        }
    }

    fun clearPendingSingleCaseExport() {
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
        pendingSingleCaseBundleExport = false
    }

    fun exportCurrentCase(openOutput: () -> OutputStream?) {
        val caseId = mutableState.value.detail?.id
        if (caseId == null || mutableState.value.singleCaseExchangeBusy) {
            clearPendingSingleCaseExport()
            return
        }
        val exportPassword = pendingExportPassword
        pendingExportPassword = null
        val exportBundle = pendingSingleCaseBundleExport
        pendingSingleCaseBundleExport = false
        val passwordProtected = exportPassword != null
        viewModelScope.launch {
            mutableState.update {
                it.copy(singleCaseExchangeBusy = true, singleCaseExchangeError = null)
            }
            val result = try {
                withContext(ioDispatcher) {
                    val output = openOutput()
                        ?: return@withContext SingleCaseExportResult.Rejected(
                            code = "OUTPUT_OPEN_FAILED",
                            message = "无法创建目标文件。",
                        )
                    output.use { stream ->
                        val protection = if (exportPassword == null) {
                            SingleCaseProtection.UnencryptedSensitiveDataConfirmed
                        } else {
                            SingleCaseProtection.PasswordProtected(exportPassword)
                        }
                        if (exportBundle) {
                            val service = singleCaseBundleService
                            val attachmentRoot = backupAttachmentRoot
                            if (service == null || attachmentRoot == null) {
                                SingleCaseExportResult.Rejected(
                                    code = "BUNDLE_EXPORT_UNAVAILABLE",
                                    message = "当前环境未配置单命例附件包导出。",
                                )
                            } else {
                                service.export(
                                    caseId = caseId,
                                    output = stream,
                                    attachmentRoot = attachmentRoot,
                                    appVersion = BuildConfig.VERSION_NAME,
                                    protection = protection,
                                )
                            }
                        } else {
                            singleCaseExchange.export(
                                caseId = caseId,
                                output = stream,
                                appVersion = BuildConfig.VERSION_NAME,
                                protection = protection,
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SingleCaseExportResult.Rejected(
                    code = "OUTPUT_OPEN_FAILED",
                    message = "无法创建或写入目标文件。",
                )
            } finally {
                exportPassword?.fill('\u0000')
            }
            mutableState.update {
                when (result) {
                    is SingleCaseExportResult.Success -> it.copy(
                        singleCaseExchangeBusy = false,
                        message = if (exportBundle && passwordProtected) {
                            "密码加密单命例附件包已导出；图片二进制已包含，请另行保存密码。"
                        } else if (exportBundle) {
                            "单命例附件包已导出，图片二进制和引用均已校验。"
                        } else if (passwordProtected) {
                            "密码加密单命例已导出；请另行安全保存密码。"
                        } else {
                            "单命例 JSON 已导出，图片仅保留引用信息。"
                        },
                    )
                    is SingleCaseExportResult.Rejected -> it.copy(
                        singleCaseExchangeBusy = false,
                        singleCaseExchangeError = "${result.message}（${result.code}）",
                    )
                }
            }
        }
    }

    fun previewSingleCase(openInput: () -> InputStream?) {
        previewSingleCase(openInput, password = null)
    }

    fun previewSingleCaseWithPassword(
        password: CharArray,
        openInput: () -> InputStream?,
    ) {
        if (password.isEmpty()) {
            password.fill('\u0000')
            mutableState.update { it.copy(singleCasePasswordError = "请输入解密密码。") }
            return
        }
        previewSingleCase(openInput, password)
    }

    private fun previewSingleCase(
        openInput: () -> InputStream?,
        password: CharArray?,
    ) {
        if (mutableState.value.singleCaseExchangeBusy) {
            password?.fill('\u0000')
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    singleCaseExchangeBusy = true,
                    singleCasePreview = null,
                    singleCasePasswordError = null,
                    singleCaseExchangeError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    val input = openInput()
                        ?: return@withContext SingleCasePreviewResult.Rejected(
                            code = "INPUT_OPEN_FAILED",
                            message = "无法打开所选文件。",
                        )
                    input.use { previewSingleCaseInput(it, password) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SingleCasePreviewResult.Rejected(
                    code = "INPUT_OPEN_FAILED",
                    message = "无法读取所选文件。",
                )
            } finally {
                password?.fill('\u0000')
            }
            mutableState.update {
                when (result) {
                    is SingleCasePreviewResult.Success -> it.copy(
                        singleCaseExchangeBusy = false,
                        singleCasePreview = result.preview,
                        singleCasePasswordImportVisible = false,
                        singleCasePasswordError = null,
                        singleCaseMergePreparation = null,
                        singleCaseMergeModules = emptySet(),
                        singleCaseFieldChoices = emptyMap(),
                    )
                    is SingleCasePreviewResult.Rejected -> {
                        if (
                            result.code == "PASSWORD_REQUIRED" ||
                            result.code == "DECRYPTION_FAILED"
                        ) {
                            it.copy(
                                singleCaseExchangeBusy = false,
                                singleCasePasswordImportVisible = true,
                                singleCasePasswordError =
                                    if (result.code == "PASSWORD_REQUIRED") {
                                        null
                                    } else {
                                        "${result.message}（${result.code}）"
                                    },
                            )
                        } else {
                            it.copy(
                                singleCaseExchangeBusy = false,
                                singleCasePasswordImportVisible = false,
                                singleCasePasswordError = null,
                                singleCaseExchangeError =
                                    "${result.message}（${result.code}）",
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun previewSingleCaseInput(
        input: InputStream,
        password: CharArray?,
    ): SingleCasePreviewResult {
        val pushback = PushbackInputStream(input, SINGLE_CASE_FORMAT_PROBE_BYTES)
        val prefix = ByteArray(SINGLE_CASE_FORMAT_PROBE_BYTES)
        var count = 0
        while (count < prefix.size) {
            val read = pushback.read(prefix, count, prefix.size - count)
            if (read < 0) break
            count += read
        }
        if (count > 0) {
            pushback.unread(prefix, 0, count)
        }
        val firstContentByte = prefix
            .take(count)
            .firstOrNull { byte ->
                byte.toInt().toChar() !in setOf(' ', '\t', '\r', '\n')
            }
        return if (firstContentByte?.toInt()?.toChar() == '{') {
            singleCaseExchange.preview(pushback, password)
        } else {
            val service = singleCaseBundleService
                ?: return SingleCasePreviewResult.Rejected(
                    "BUNDLE_IMPORT_UNAVAILABLE",
                    "当前环境未配置单命例附件包导入。",
                )
            val workRoot = backupWorkRoot
                ?: return SingleCasePreviewResult.Rejected(
                    "BUNDLE_IMPORT_UNAVAILABLE",
                    "当前环境没有可用的命例包校验临时目录。",
                )
            service.preview(pushback, workRoot, password)
        }
    }

    fun cancelPasswordSingleCaseImport() {
        if (mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(
                singleCasePasswordImportVisible = false,
                singleCasePasswordError = null,
            )
        }
    }

    fun dismissSingleCasePreview() {
        if (mutableState.value.singleCaseExchangeBusy) return
        pendingSingleCaseBundleCommit = null
        mutableState.update {
            it.copy(
                singleCasePreview = null,
                singleCasePasswordCommitVisible = false,
                singleCasePasswordError = null,
            )
        }
    }

    fun commitSingleCaseImport(
        decision: SingleCaseImportDecision,
        openInput: (() -> InputStream?)? = null,
        password: CharArray? = null,
    ) {
        val preview = mutableState.value.singleCasePreview ?: return
        if (mutableState.value.singleCaseExchangeBusy) {
            password?.fill('\u0000')
            return
        }
        val bundledCommit =
            preview.containsAttachmentBinaries &&
                decision != SingleCaseImportDecision.SKIP
        if (
            bundledCommit &&
            preview.protection == SingleCaseDocumentProtection.PASSWORD_PROTECTED &&
            password == null
        ) {
            pendingSingleCaseBundleCommit = PendingSingleCaseBundleCommit.Import(decision)
            mutableState.update {
                it.copy(
                    singleCasePasswordCommitVisible = true,
                    singleCasePasswordError = null,
                )
            }
            return
        }
        if (bundledCommit && openInput == null) {
            password?.fill('\u0000')
            mutableState.update {
                it.copy(
                    singleCaseExchangeError =
                        "无法重新打开命例附件包，未写入数据。（INPUT_OPEN_FAILED）",
                )
            }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    singleCaseExchangeBusy = true,
                    singleCasePasswordCommitVisible = false,
                    singleCasePasswordError = null,
                    singleCaseExchangeError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    if (bundledCommit) {
                        val service = singleCaseBundleService
                        val workRoot = backupWorkRoot
                        val attachmentRoot = backupAttachmentRoot
                        val input = openInput?.invoke()
                        if (
                            service == null ||
                            workRoot == null ||
                            attachmentRoot == null ||
                            input == null
                        ) {
                            SingleCaseImportResult.Rejected(
                                code = "INPUT_OPEN_FAILED",
                                message = "无法重新打开命例附件包。",
                            )
                        } else {
                            input.use {
                                service.commitImport(
                                    preview = preview,
                                    decision = decision,
                                    input = it,
                                    workRoot = workRoot,
                                    attachmentRoot = attachmentRoot,
                                    password = password,
                                )
                            }
                        }
                    } else {
                        singleCaseExchange.commitImport(preview, decision)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SingleCaseImportResult.Rejected(
                    code = "IMPORT_FAILED",
                    message = "单命例导入失败，未写入数据。",
                )
            } finally {
                password?.fill('\u0000')
            }
            when (result) {
                is SingleCaseImportResult.Imported -> {
                    pendingSingleCaseBundleCommit = null
                    mutableState.update {
                        it.copy(
                            singleCaseExchangeBusy = false,
                            singleCasePasswordCommitVisible = false,
                            singleCasePreview = null,
                            visibility = CaseVisibility.ACTIVE,
                            message = "单命例已作为新命例导入，原有本地命例未被覆盖。",
                        )
                    }
                    refreshCases()
                }
                is SingleCaseImportResult.Skipped -> {
                    pendingSingleCaseBundleCommit = null
                    mutableState.update {
                        it.copy(
                            singleCaseExchangeBusy = false,
                            singleCasePasswordCommitVisible = false,
                            singleCasePreview = null,
                            message = "已跳过该单命例，未写入数据。",
                        )
                    }
                }
                is SingleCaseImportResult.Merged -> {
                    pendingSingleCaseBundleCommit = null
                    mutableState.update {
                        it.copy(
                            singleCaseExchangeBusy = false,
                            singleCasePasswordCommitVisible = false,
                            singleCasePreview = null,
                            message = "单命例差异已合并到本地目标。",
                        )
                    }
                }
                is SingleCaseImportResult.Rejected -> {
                    val retryPassword =
                        bundledCommit &&
                            (
                                result.code == "PASSWORD_REQUIRED" ||
                                    result.code == "DECRYPTION_FAILED"
                                )
                    if (!retryPassword) {
                        pendingSingleCaseBundleCommit = null
                    }
                    mutableState.update {
                        if (retryPassword) {
                            it.copy(
                                singleCaseExchangeBusy = false,
                                singleCasePasswordCommitVisible = true,
                                singleCasePasswordError =
                                    "${result.message}（${result.code}）",
                            )
                        } else {
                            it.copy(
                                singleCaseExchangeBusy = false,
                                singleCasePasswordCommitVisible = false,
                                singleCaseExchangeError =
                                    "${result.message}（${result.code}）",
                            )
                        }
                    }
                }
            }
        }
    }

    fun prepareSingleCaseMerge(targetCaseId: String) {
        val preview = mutableState.value.singleCasePreview ?: return
        if (mutableState.value.singleCaseExchangeBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(singleCaseExchangeBusy = true, singleCaseExchangeError = null)
            }
            val result = try {
                withContext(ioDispatcher) {
                    singleCaseExchange.prepareMerge(preview, targetCaseId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SingleCaseMergePreparationResult.Rejected(
                    code = "MERGE_PREPARATION_FAILED",
                    message = "无法生成合并差异，未写入数据。",
                )
            }
            mutableState.update {
                when (result) {
                    is SingleCaseMergePreparationResult.Success -> it.copy(
                        singleCaseExchangeBusy = false,
                        singleCasePreview = null,
                        singleCaseMergePreparation = result.preparation,
                        singleCaseMergeModules = emptySet(),
                        singleCaseFieldChoices = result.preparation.fieldDifferences
                            .associate { difference ->
                                difference.key to SingleCaseValueChoice.LOCAL
                            },
                    )
                    is SingleCaseMergePreparationResult.Rejected -> it.copy(
                        singleCaseExchangeBusy = false,
                        singleCaseExchangeError = "${result.message}（${result.code}）",
                    )
                }
            }
        }
    }

    fun toggleSingleCaseMergeModule(module: SingleCaseMergeModule) {
        if (mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            val modules = it.singleCaseMergeModules.toMutableSet()
            if (!modules.add(module)) modules.remove(module)
            it.copy(singleCaseMergeModules = modules)
        }
    }

    fun chooseSingleCaseMergeField(
        key: SingleCaseFieldKey,
        choice: SingleCaseValueChoice,
    ) {
        if (mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(singleCaseFieldChoices = it.singleCaseFieldChoices + (key to choice))
        }
    }

    fun cancelSingleCaseMerge() {
        if (mutableState.value.singleCaseExchangeBusy) return
        pendingSingleCaseBundleCommit = null
        mutableState.update {
            it.copy(
                singleCasePreview = it.singleCaseMergePreparation?.sourcePreview,
                singleCaseMergePreparation = null,
                singleCaseMergeModules = emptySet(),
                singleCaseFieldChoices = emptyMap(),
                singleCasePasswordCommitVisible = false,
                singleCasePasswordError = null,
            )
        }
    }

    fun commitSingleCaseMerge(
        openInput: (() -> InputStream?)? = null,
        password: CharArray? = null,
    ) {
        val preparation = mutableState.value.singleCaseMergePreparation ?: return
        if (mutableState.value.singleCaseExchangeBusy) {
            password?.fill('\u0000')
            return
        }
        val plan = SingleCaseMergePlan(
            preparation = preparation,
            modules = mutableState.value.singleCaseMergeModules,
            fieldChoices = mutableState.value.singleCaseFieldChoices,
        )
        commitSingleCaseMergePlan(plan, openInput, password)
    }

    private fun commitSingleCaseMergePlan(
        plan: SingleCaseMergePlan,
        openInput: (() -> InputStream?)?,
        password: CharArray?,
    ) {
        val preparation = plan.preparation
        val bundledCommit = preparation.sourcePreview.containsAttachmentBinaries
        if (
            bundledCommit &&
            preparation.sourcePreview.protection ==
            SingleCaseDocumentProtection.PASSWORD_PROTECTED &&
            password == null
        ) {
            pendingSingleCaseBundleCommit = PendingSingleCaseBundleCommit.Merge(plan)
            mutableState.update {
                it.copy(
                    singleCasePasswordCommitVisible = true,
                    singleCasePasswordError = null,
                )
            }
            return
        }
        if (bundledCommit && openInput == null) {
            password?.fill('\u0000')
            mutableState.update {
                it.copy(
                    singleCaseExchangeError =
                        "无法重新打开命例附件包，未写入数据。（INPUT_OPEN_FAILED）",
                )
            }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    singleCaseExchangeBusy = true,
                    singleCasePasswordCommitVisible = false,
                    singleCasePasswordError = null,
                    singleCaseExchangeError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    if (bundledCommit) {
                        val service = singleCaseBundleService
                        val workRoot = backupWorkRoot
                        val attachmentRoot = backupAttachmentRoot
                        val input = openInput?.invoke()
                        if (
                            service == null ||
                            workRoot == null ||
                            attachmentRoot == null ||
                            input == null
                        ) {
                            SingleCaseImportResult.Rejected(
                                code = "INPUT_OPEN_FAILED",
                                message = "无法重新打开命例附件包。",
                            )
                        } else {
                            input.use {
                                service.commitMerge(
                                    plan = plan,
                                    input = it,
                                    workRoot = workRoot,
                                    attachmentRoot = attachmentRoot,
                                    password = password,
                                )
                            }
                        }
                    } else {
                        singleCaseExchange.commitMerge(plan)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SingleCaseImportResult.Rejected(
                    code = "MERGE_FAILED",
                    message = "单命例合并失败，未写入数据。",
                )
            } finally {
                password?.fill('\u0000')
            }
            when (result) {
                is SingleCaseImportResult.Merged -> {
                    pendingSingleCaseBundleCommit = null
                    mutableState.update {
                        it.copy(
                            singleCaseExchangeBusy = false,
                            singleCasePasswordCommitVisible = false,
                            singleCaseMergePreparation = null,
                            singleCaseMergeModules = emptySet(),
                            singleCaseFieldChoices = emptyMap(),
                            visibility = CaseVisibility.ACTIVE,
                            message = "单命例差异已合并到“${preparation.targetAlias}”。",
                        )
                    }
                    refreshCases()
                }
                is SingleCaseImportResult.Rejected -> {
                    val retryPassword =
                        bundledCommit &&
                            (
                                result.code == "PASSWORD_REQUIRED" ||
                                    result.code == "DECRYPTION_FAILED"
                                )
                    if (!retryPassword) {
                        pendingSingleCaseBundleCommit = null
                    }
                    mutableState.update {
                        if (retryPassword) {
                            it.copy(
                                singleCaseExchangeBusy = false,
                                singleCasePasswordCommitVisible = true,
                                singleCasePasswordError =
                                    "${result.message}（${result.code}）",
                            )
                        } else {
                            it.copy(
                                singleCaseExchangeBusy = false,
                                singleCasePasswordCommitVisible = false,
                                singleCaseExchangeError =
                                    "${result.message}（${result.code}）",
                            )
                        }
                    }
                }
                is SingleCaseImportResult.Imported,
                is SingleCaseImportResult.Skipped,
                -> {
                    pendingSingleCaseBundleCommit = null
                    mutableState.update {
                        it.copy(
                            singleCaseExchangeBusy = false,
                            singleCasePasswordCommitVisible = false,
                            singleCaseExchangeError =
                                "合并返回了不匹配的结果，未确认成功。",
                        )
                    }
                }
            }
        }
    }

    fun commitPendingSingleCaseBundle(
        password: CharArray,
        openInput: () -> InputStream?,
    ) {
        if (password.isEmpty()) {
            password.fill('\u0000')
            mutableState.update {
                it.copy(singleCasePasswordError = "请输入解密密码。")
            }
            return
        }
        when (val pending = pendingSingleCaseBundleCommit) {
            is PendingSingleCaseBundleCommit.Import -> commitSingleCaseImport(
                decision = pending.decision,
                openInput = openInput,
                password = password,
            )
            is PendingSingleCaseBundleCommit.Merge -> commitSingleCaseMergePlan(
                plan = pending.plan,
                openInput = openInput,
                password = password,
            )
            null -> {
                password.fill('\u0000')
                mutableState.update {
                    it.copy(
                        singleCasePasswordCommitVisible = false,
                        singleCasePasswordError = null,
                        singleCaseExchangeError =
                            "没有待认证的单命例附件提交。（NO_PENDING_COMMIT）",
                    )
                }
            }
        }
    }

    fun cancelPasswordSingleCaseCommit() {
        if (mutableState.value.singleCaseExchangeBusy) return
        pendingSingleCaseBundleCommit = null
        mutableState.update {
            it.copy(
                singleCasePasswordCommitVisible = false,
                singleCasePasswordError = null,
            )
        }
    }

    fun dismissSingleCaseExchangeError() {
        mutableState.update { it.copy(singleCaseExchangeError = null) }
    }

    fun updateQuery(query: String) {
        mutableState.update { it.copy(query = query) }
        refreshCases()
    }

    fun selectGroup(groupId: String?) {
        mutableState.update {
            it.copy(selectedGroupId = groupId, listError = null)
        }
        refreshCases()
    }

    fun selectTag(tagId: String?) {
        mutableState.update {
            it.copy(selectedTagId = tagId, listError = null)
        }
        refreshCases()
    }

    fun selectSortOrder(sortOrder: CaseSortOrder) {
        mutableState.update {
            it.copy(sortOrder = sortOrder, listError = null)
        }
        refreshCases()
    }

    fun selectVisibility(visibility: CaseVisibility) {
        mutableState.update {
            it.copy(
                visibility = visibility,
                selectedGroupId = null,
                selectedTagId = null,
                listError = null,
            )
        }
        refreshCases()
    }

    fun openCreate() {
        mutableState.update {
            it.copy(
                destination = navigator.openCreate(),
                formError = null,
                duplicateCandidates = emptyList(),
                previewing = false,
                instantCalculation = null,
                message = null,
            )
        }
    }

    fun updateForm(transform: (CaseFormState) -> CaseFormState) {
        mutableState.update {
            it.copy(
                form = transform(it.form),
                formError = null,
                duplicateCandidates = emptyList(),
                instantCalculation = null,
            )
        }
    }

    fun previewCase() {
        val current = mutableState.value
        if (current.saving || current.previewing) return
        val form = current.form
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    previewing = true,
                    formError = null,
                    duplicateCandidates = emptyList(),
                    instantCalculation = null,
                )
            }
            when (val result = createCase.preview(form)) {
                is PreviewCaseResult.Calculated -> mutableState.update {
                    if (it.form == form && it.destination == AppDestination.CreateCase) {
                        it.copy(
                            previewing = false,
                            instantCalculation = result.calculation,
                        )
                    } else {
                        it.copy(previewing = false)
                    }
                }
                is PreviewCaseResult.ValidationFailed -> mutableState.update {
                    if (it.form == form && it.destination == AppDestination.CreateCase) {
                        it.copy(previewing = false, formError = result.message)
                    } else {
                        it.copy(previewing = false)
                    }
                }
                is PreviewCaseResult.CalculationFailed -> mutableState.update {
                    if (it.form == form && it.destination == AppDestination.CreateCase) {
                        it.copy(
                            previewing = false,
                            formError =
                                "排盘失败：${result.message} 输入内容已保留，可修改后重试。",
                        )
                    } else {
                        it.copy(previewing = false)
                    }
                }
                is PreviewCaseResult.TimeZoneChoiceRequired -> mutableState.update {
                    if (it.form == form && it.destination == AppDestination.CreateCase) {
                        it.copy(
                            form = it.form.copy(
                                resolvedUtcOffsetSeconds = null,
                                availableUtcOffsetSeconds = result.validUtcOffsetSeconds,
                            ),
                            previewing = false,
                            formError = "该出生时间在 ${result.timeZoneId} 出现两次。" +
                                "请选择实际 UTC offset 后再次即时排盘或保存。",
                        )
                    } else {
                        it.copy(previewing = false)
                    }
                }
            }
        }
    }

    fun submitCase(allowDuplicate: Boolean = false) {
        if (mutableState.value.saving || mutableState.value.previewing) return
        val form = mutableState.value.form
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, formError = null) }
            when (val result = createCase(form, allowDuplicate)) {
                is CreateCaseResult.Created -> {
                    mutableState.update {
                        it.copy(
                            destination = navigator.backToList(),
                            query = "",
                            form = CaseFormState(),
                            saving = false,
                            instantCalculation = null,
                            duplicateCandidates = emptyList(),
                            message = "命例已完成排盘并保存。",
                        )
                    }
                    refreshCases()
                }
                is CreateCaseResult.ValidationFailed -> mutableState.update {
                    it.copy(saving = false, formError = result.message)
                }
                is CreateCaseResult.TimeZoneChoiceRequired -> mutableState.update {
                    it.copy(
                        form = it.form.copy(
                            resolvedUtcOffsetSeconds = null,
                            availableUtcOffsetSeconds = result.validUtcOffsetSeconds,
                        ),
                        saving = false,
                        formError = "该出生时间在 ${result.timeZoneId} 出现两次。" +
                            "请选择实际 UTC offset 后再次保存。",
                    )
                }
                is CreateCaseResult.CalculationFailed -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "排盘失败：${result.message} 输入内容已保留，可修改后重试。",
                    )
                }
                is CreateCaseResult.DuplicateCandidates -> mutableState.update {
                    it.copy(
                        saving = false,
                        duplicateCandidates = result.candidates,
                        formError = "发现疑似重复命例。请先核对；确认仍需保留两份时可继续保存。",
                    )
                }
                is CreateCaseResult.AlreadyExists -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "保存冲突：该命例已经存在，未覆盖原记录。",
                    )
                }
                is CreateCaseResult.RevisionConflict -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "保存冲突：命例已被更新，未覆盖较新的记录。",
                    )
                }
                is CreateCaseResult.StorageFailed -> mutableState.update {
                    it.copy(saving = false, formError = result.message)
                }
            }
        }
    }

    fun openDetail(caseId: String) {
        mutableState.update {
            it.copy(
                destination = navigator.openDetail(caseId),
                detail = null,
                detailLoading = true,
                detailError = null,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                var detail = caseRepository.findById(caseId)
                val viewWarning = if (detail?.deletedAt == null) {
                    try {
                        if (caseRepository.markViewed(caseId, clock.instant())) {
                            detail = caseRepository.findById(caseId)
                            null
                        } else {
                            "详情已打开，但最近查看时间未能记录。"
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        "详情已打开，但数据库未能记录最近查看时间。"
                    }
                } else {
                    null
                }
                mutableState.update {
                    if (detail == null) {
                        it.copy(
                            detailLoading = false,
                            detailError = "未找到该命例，记录可能已被移除。",
                        )
                    } else {
                        it.copy(
                            detail = detail,
                            detailLoading = false,
                            message = viewWarning,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        detailLoading = false,
                        detailError = "命例详情读取失败，请返回列表后重试。",
                    )
                }
            }
        }
    }

    fun openEditCase() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val form = detail.toEditableForm()
        mutableState.update {
            it.copy(
                destination = navigator.openEditCase(detail.id),
                editForm = form,
                mutationError = null,
                duplicateCandidates = emptyList(),
            )
        }
    }

    fun openMetadata() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        mutableState.update {
            it.copy(
                destination = navigator.openMetadata(detail.id),
                metadataDraft = CaseMetadataDraft(
                    groupNames = detail.groups.joinToString("，") { group -> group.name },
                    tagNames = detail.tags.joinToString("，") { tag -> tag.name },
                    isFavorite = detail.isFavorite,
                    isPinned = detail.isPinned,
                ),
                mutationError = null,
            )
        }
    }

    fun updateMetadataDraft(
        transform: (CaseMetadataDraft) -> CaseMetadataDraft,
    ) {
        mutableState.update {
            it.copy(
                metadataDraft = transform(it.metadataDraft),
                mutationError = null,
            )
        }
    }

    fun saveMetadata() {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val draft = mutableState.value.metadataDraft
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = caseMetadata.save(detail.id, detail.revision, draft)
            finishMutation(result, detail.id, "命例分组、标签与标记已保存。")
        }
    }

    fun updateEditForm(transform: (CaseFormState) -> CaseFormState) {
        mutableState.update {
            it.copy(
                editForm = transform(it.editForm),
                mutationError = null,
                duplicateCandidates = emptyList(),
            )
        }
    }

    fun saveEditedCase(allowDuplicate: Boolean = false) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val form = mutableState.value.editForm
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = editCase(detail.id, detail.revision, form, allowDuplicate)
            finishMutation(
                result = result,
                caseId = detail.id,
                successMessage = "命例资料已重新排盘并保存；旧计算快照仍保留。",
            )
        }
    }

    fun openTextRecord(recordId: String? = null) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val record = recordId?.let { id ->
            detail.textRecords.firstOrNull { it.id == id } ?: return
        }
        mutableState.update {
            it.copy(
                destination = navigator.openTextRecord(detail.id, recordId),
                recordDraft = if (record == null) {
                    TextRecordDraft()
                } else {
                    TextRecordDraft(
                        type = record.type,
                        content = record.content,
                        analysisCategory = record.analysisCategory
                            ?: AnalysisCategory.GENERAL,
                    )
                },
                mutationError = null,
            )
        }
    }

    fun updateRecordDraft(transform: (TextRecordDraft) -> TextRecordDraft) {
        mutableState.update {
            it.copy(recordDraft = transform(it.recordDraft), mutationError = null)
        }
    }

    fun saveTextRecord(recordId: String?) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val draft = mutableState.value.recordDraft
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = textRecords.save(detail.id, detail.revision, recordId, draft)
            finishMutation(result, detail.id, "记录已保存。")
        }
    }

    fun deleteTextRecord(recordId: String) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = textRecords.delete(detail.id, detail.revision, recordId)
            finishMutation(result, detail.id, "记录已删除。")
        }
    }

    fun openEvent(eventId: String? = null) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val event = eventId?.let { id ->
            detail.events.firstOrNull { it.id == id } ?: return
        }
        mutableState.update {
            it.copy(
                destination = navigator.openEvent(detail.id, eventId),
                eventDraft = if (event == null) {
                    EventDraft()
                } else {
                    EventDraft(
                        year = event.year?.toString().orEmpty(),
                        month = event.month?.toString().orEmpty(),
                        day = event.day?.toString().orEmpty(),
                        status = event.status.orEmpty(),
                        rawText = event.rawText,
                        title = event.title.orEmpty(),
                        category = event.category,
                    )
                },
                mutationError = null,
            )
        }
    }

    fun updateEventDraft(transform: (EventDraft) -> EventDraft) {
        mutableState.update {
            it.copy(eventDraft = transform(it.eventDraft), mutationError = null)
        }
    }

    fun saveEvent(eventId: String?) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val draft = mutableState.value.eventDraft
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = caseEvents.save(detail.id, detail.revision, eventId, draft)
            finishMutation(result, detail.id, "关键事件已保存。")
        }
    }

    fun deleteEvent(eventId: String) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = caseEvents.delete(detail.id, detail.revision, eventId)
            finishMutation(result, detail.id, "关键事件已删除。")
        }
    }

    fun requestMoveToTrash() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        mutableState.update {
            it.copy(deleteConfirmationVisible = true, mutationError = null)
        }
    }

    fun cancelMoveToTrash() {
        mutableState.update { it.copy(deleteConfirmationVisible = false) }
    }

    fun confirmMoveToTrash() {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    mutationSaving = true,
                    mutationError = null,
                    deleteConfirmationVisible = false,
                )
            }
            when (val result = caseLifecycle.moveToTrash(detail.id, detail.revision)) {
                is CaseMutationResult.Saved -> {
                    mutableState.update {
                        it.copy(
                            destination = navigator.backToList(),
                            detail = null,
                            mutationSaving = false,
                            message = "命例已移入回收站，附件和历史仍完整保留。",
                        )
                    }
                    refreshCases()
                }
                else -> finishMutation(result, detail.id, "")
            }
        }
    }

    fun restoreCase() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt == null || mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            when (val result = caseLifecycle.restore(detail.id, detail.revision)) {
                is CaseMutationResult.Saved -> {
                    mutableState.update {
                        it.copy(
                            destination = navigator.backToList(),
                            visibility = CaseVisibility.ACTIVE,
                            detail = null,
                            mutationSaving = false,
                            message = "命例已从回收站恢复。",
                        )
                    }
                    refreshCases()
                }
                else -> finishMutation(result, detail.id, "")
            }
        }
    }

    fun duplicateCase() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null || mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            when (val result = caseLifecycle.duplicate(detail.id, detail.revision)) {
                is CaseMutationResult.Saved -> {
                    mutableState.update {
                        it.copy(
                            mutationSaving = false,
                            visibility = CaseVisibility.ACTIVE,
                        )
                    }
                    navigator.backToList()
                    openDetail(result.caseId)
                    mutableState.update {
                        it.copy(message = "副本已创建；记录、事件和来源附件未复制。")
                    }
                    refreshCases()
                }
                else -> finishMutation(result, detail.id, "")
            }
        }
    }

    fun navigateBack() {
        mutableState.update {
            it.copy(
                destination = navigator.back(),
                mutationError = null,
                formError = null,
            )
        }
    }

    fun backToList() {
        mutableState.update {
            it.copy(
                destination = navigator.backToList(),
                detail = null,
                detailError = null,
                formError = null,
                previewing = false,
                instantCalculation = null,
            )
        }
    }

    private suspend fun finishMutation(
        result: CaseMutationResult,
        caseId: String,
        successMessage: String,
    ) {
        when (result) {
            is CaseMutationResult.Saved -> {
                val refreshed = try {
                    caseRepository.findById(caseId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (refreshed == null) {
                    mutableState.update {
                        it.copy(
                            mutationSaving = false,
                            mutationError = "更改已保存，但详情刷新失败。请返回列表后重新打开。",
                        )
                    }
                } else {
                    mutableState.update {
                        it.copy(
                            destination = navigator.back(),
                            detail = refreshed,
                            mutationSaving = false,
                            mutationError = null,
                            message = successMessage,
                        )
                    }
                    refreshCases()
                }
            }
            is CaseMutationResult.ValidationFailed -> mutableState.update {
                it.copy(mutationSaving = false, mutationError = result.message)
            }
            is CaseMutationResult.DuplicateCandidates -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    duplicateCandidates = result.candidates,
                    mutationError = "发现疑似重复命例。请核对后决定是否仍保存。",
                )
            }
            is CaseMutationResult.TimeZoneChoiceRequired -> mutableState.update {
                it.copy(
                    editForm = it.editForm.copy(
                        resolvedUtcOffsetSeconds = null,
                        availableUtcOffsetSeconds = result.validUtcOffsetSeconds,
                    ),
                    mutationSaving = false,
                    mutationError = "该出生时间在 ${result.timeZoneId} 出现两次。" +
                        "请选择实际 UTC offset 后再次保存。",
                )
            }
            CaseMutationResult.NotFound -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "目标记录不存在，未保存任何更改。",
                )
            }
            is CaseMutationResult.RevisionConflict -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "保存冲突：命例已有较新修订（${result.actualRevision}），" +
                        "当前输入仍保留。请返回详情重新打开后再编辑。",
                )
            }
            is CaseMutationResult.CalculationFailed -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "重新排盘失败：${result.message} 当前输入仍保留。",
                )
            }
            is CaseMutationResult.StorageFailed -> mutableState.update {
                it.copy(mutationSaving = false, mutationError = result.message)
            }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val useCase = CreateCaseUseCase(
                baziEngine = container.baziEngine,
                caseRepository = container.caseRepository,
            )
            return StageTwoViewModel(
                caseRepository = container.caseRepository,
                createCase = useCase,
                editCase = EditCaseUseCase(
                    baziEngine = container.baziEngine,
                    caseRepository = container.caseRepository,
                ),
                caseMetadata = CaseMetadataUseCase(container.caseRepository),
                textRecords = TextRecordUseCase(container.caseRepository),
                caseEvents = CaseEventUseCase(container.caseRepository),
                caseLifecycle = CaseLifecycleUseCase(container.caseRepository),
                singleCaseBundleService = container.singleCaseBundleService,
                caseBackupService = container.caseBackupService,
                backupAttachmentRoot = container.backupAttachmentRoot,
                backupWorkRoot = container.backupWorkRoot,
            ) as T
        }
    }

    private companion object {
        const val MIN_EXPORT_PASSWORD_LENGTH = 8
        const val MAX_EXPORT_PASSWORD_LENGTH = 256
        const val SINGLE_CASE_FORMAT_PROBE_BYTES = 64
    }

    private fun validateExportPassword(
        password: CharArray,
        confirmation: CharArray,
    ): String? = when {
        password.size < MIN_EXPORT_PASSWORD_LENGTH ->
            "密码至少需要 $MIN_EXPORT_PASSWORD_LENGTH 个字符。"
        password.size > MAX_EXPORT_PASSWORD_LENGTH ->
            "密码不能超过 $MAX_EXPORT_PASSWORD_LENGTH 个字符。"
        !password.contentEquals(confirmation) -> "两次输入的密码不一致。"
        else -> null
    }
}

private fun BaziCase.toEditableForm(): CaseFormState {
    val calendar = birthInput.calendarInput
    val dateTimeParts = when (calendar) {
        is BirthCalendarInput.Solar -> with(calendar.dateTime) {
            listOf(year, month, day, hour, minute, second)
        }
        is BirthCalendarInput.Lunar -> with(calendar.dateTime) {
            listOf(year, month, day, hour, minute, second)
        }
    }
    return CaseFormState(
        alias = alias,
        name = name.value.orEmpty(),
        sex = sexForFortuneDirection,
        year = dateTimeParts[0].toString(),
        month = dateTimeParts[1].toString(),
        day = dateTimeParts[2].toString(),
        hour = dateTimeParts[3].toString(),
        minute = dateTimeParts[4].toString(),
        second = dateTimeParts[5].toString(),
        calendarSystem = when (calendar) {
            is BirthCalendarInput.Solar -> CalendarSystem.SOLAR
            is BirthCalendarInput.Lunar -> CalendarSystem.LUNAR
        },
        isLeapMonth = (calendar as? BirthCalendarInput.Lunar)?.dateTime?.isLeapMonth == true,
        locationName = birthInput.locationName.orEmpty(),
        longitude = birthInput.longitude?.toString().orEmpty(),
        latitude = birthInput.latitude?.toString().orEmpty(),
        timeZoneId = birthInput.timeZoneId,
        resolvedUtcOffsetSeconds = birthInput.resolvedUtcOffsetSeconds,
        useTrueSolarTime = birthInput.useTrueSolarTime,
        timePrecision = birthInput.timePrecision,
        timeSourceType = birthInput.timeSourceType,
        sourceNote = birthInput.sourceNote.orEmpty(),
    )
}
