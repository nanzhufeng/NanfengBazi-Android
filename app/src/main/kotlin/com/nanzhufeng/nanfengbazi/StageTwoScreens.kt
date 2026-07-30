package com.nanzhufeng.nanfengbazi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocumentProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportDecision
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePreparation
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreview
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseMergePreparation
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreAction
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreDecision
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlan
import com.nanzhufeng.nanfengbazi.data.backup.RestorePreview

@Composable
fun NanfengBaziApp(
    viewModel: StageTwoViewModel,
    onCreateSingleCaseDocument: (String) -> Unit = {},
    onOpenSingleCaseDocument: () -> Unit = {},
    onRetryPasswordSingleCaseDocument: (CharArray) -> Unit = {},
    onCreateFullBackupDocument: (String) -> Unit = {},
    onCreateEncryptedFullBackupDocument: (String) -> Unit = {},
    onOpenFullBackupDocument: () -> Unit = {},
    onRetryPasswordFullBackupDocument: (CharArray) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }
    BackHandler(enabled = state.destination != AppDestination.CaseList) {
        viewModel.navigateBack()
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                when (state.destination) {
                    AppDestination.CaseList -> CaseListScreen(
                        state = state,
                        onQueryChange = viewModel::updateQuery,
                        onSelectGroup = viewModel::selectGroup,
                        onSelectTag = viewModel::selectTag,
                        onSelectSort = viewModel::selectSortOrder,
                        onSelectVisibility = viewModel::selectVisibility,
                        onRefresh = viewModel::refreshCases,
                        onCreate = viewModel::openCreate,
                        onImportSingleCase = onOpenSingleCaseDocument,
                        onExportFullBackup = viewModel::requestFullBackupExport,
                        onPreviewFullBackup = onOpenFullBackupDocument,
                        onOpenCase = viewModel::openDetail,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.CreateCase -> CreateCaseScreen(
                        state = state,
                        onBack = viewModel::backToList,
                        onFormChange = viewModel::updateForm,
                        onSubmit = { viewModel.submitCase() },
                        onConfirmDuplicate = { viewModel.submitCase(allowDuplicate = true) },
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.CaseDetail -> CaseDetailScreen(
                        state = state,
                        onBack = viewModel::backToList,
                        onEditCase = viewModel::openEditCase,
                        onEditMetadata = viewModel::openMetadata,
                        onAddRecord = { viewModel.openTextRecord() },
                        onEditRecord = viewModel::openTextRecord,
                        onAddEvent = { viewModel.openEvent() },
                        onEditEvent = viewModel::openEvent,
                        onDuplicate = viewModel::duplicateCase,
                        onExportSingleCase = viewModel::requestSingleCaseExport,
                        onMoveToTrash = viewModel::requestMoveToTrash,
                        onRestore = viewModel::restoreCase,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditCase -> CaseFormScreen(
                        title = "编辑命例",
                        screenTag = "edit_case_screen",
                        form = state.editForm,
                        error = state.mutationError,
                        saving = state.mutationSaving,
                        submitLabel = "重新排盘并保存",
                        onBack = viewModel::navigateBack,
                        onFormChange = viewModel::updateEditForm,
                        onSubmit = { viewModel.saveEditedCase() },
                        duplicateCandidates = state.duplicateCandidates,
                        onConfirmDuplicate = {
                            viewModel.saveEditedCase(allowDuplicate = true)
                        },
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditMetadata -> CaseMetadataEditorScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onDraftChange = viewModel::updateMetadataDraft,
                        onSave = viewModel::saveMetadata,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditTextRecord -> TextRecordEditorScreen(
                        state = state,
                        destination = state.destination as AppDestination.EditTextRecord,
                        onBack = viewModel::navigateBack,
                        onDraftChange = viewModel::updateRecordDraft,
                        onSave = viewModel::saveTextRecord,
                        onDelete = viewModel::deleteTextRecord,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditEvent -> EventEditorScreen(
                        state = state,
                        destination = state.destination as AppDestination.EditEvent,
                        onBack = viewModel::navigateBack,
                        onDraftChange = viewModel::updateEventDraft,
                        onSave = viewModel::saveEvent,
                        onDelete = viewModel::deleteEvent,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
            if (state.deleteConfirmationVisible) {
                AlertDialog(
                    onDismissRequest = viewModel::cancelMoveToTrash,
                    title = { Text("移入回收站？") },
                    text = {
                        Text(
                            "命例会从主列表隐藏，但附件、记录、事件和计算历史都会保留，" +
                                "可随时从回收站恢复。",
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = viewModel::confirmMoveToTrash,
                            modifier = Modifier.testTag("confirm_trash_button"),
                        ) {
                            Text("移入回收站")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::cancelMoveToTrash) {
                            Text("取消")
                        }
                    },
                )
            }
            if (state.fullBackupExportConfirmationVisible) {
                AlertDialog(
                    onDismissRequest = viewModel::cancelFullBackupExport,
                    title = { Text("导出完整未加密备份？") },
                    text = {
                        Text(
                            "ZIP 会包含全部命例、出生资料、健康/婚姻/财务记录、" +
                                "版本历史和来源图片附件。如不想保存明文，请选择密码加密；" +
                                "否则请只保存到可信位置。",
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.confirmFullBackupExport()?.let(
                                    onCreateFullBackupDocument,
                                )
                            },
                            modifier = Modifier.testTag("confirm_full_backup_export"),
                        ) {
                            Text("确认并选择位置")
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = viewModel::requestPasswordFullBackupExport,
                                modifier = Modifier.testTag("choose_password_full_backup_export"),
                            ) {
                                Text("改用密码加密")
                            }
                            TextButton(onClick = viewModel::cancelFullBackupExport) {
                                Text("取消")
                            }
                        }
                    },
                )
            }
            if (state.fullBackupPasswordExportVisible) {
                SingleCasePasswordDialog(
                    title = "设置完整备份加密密码",
                    description = "密码不会写入备份，也无法找回。请至少输入 8 个字符并另行保管。",
                    error = state.fullBackupPasswordError,
                    requireConfirmation = true,
                    confirmLabel = "选择保存位置",
                    confirmTag = "confirm_password_full_backup_export",
                    passwordTag = "full_backup_password",
                    confirmationTag = "full_backup_password_confirmation",
                    onConfirm = { password, confirmation ->
                        viewModel.confirmPasswordFullBackupExport(
                            password,
                            checkNotNull(confirmation),
                        )?.let(onCreateEncryptedFullBackupDocument)
                    },
                    onDismiss = viewModel::cancelPasswordFullBackupExport,
                )
            }
            if (state.fullBackupPasswordImportVisible) {
                SingleCasePasswordDialog(
                    title = "输入完整备份解密密码",
                    description = "密码只用于本次只读校验，不会保存。错误密码与损坏文件使用相同错误。",
                    error = state.fullBackupPasswordError,
                    requireConfirmation = false,
                    confirmLabel = "解密并检查",
                    confirmTag = "confirm_password_full_backup_import",
                    passwordTag = "full_backup_password",
                    onConfirm = { password, _ ->
                        onRetryPasswordFullBackupDocument(password)
                    },
                    onDismiss = viewModel::cancelPasswordFullBackupImport,
                )
            }
            if (state.singleCaseExportConfirmationVisible) {
                AlertDialog(
                    onDismissRequest = viewModel::cancelSingleCaseExport,
                    title = { Text("导出未加密单命例？") },
                    text = {
                        Text(
                            "JSON 可能包含出生资料、健康、婚姻、财务、反馈和分析。" +
                                "本文件只保存图片引用信息，不包含图片二进制。请妥善保管。",
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.confirmSingleCaseExport()?.let(
                                    onCreateSingleCaseDocument,
                                )
                            },
                            modifier = Modifier.testTag("confirm_single_case_export"),
                        ) {
                            Text("选择保存位置")
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = viewModel::requestPasswordSingleCaseExport,
                                modifier = Modifier.testTag(
                                    "choose_password_single_case_export",
                                ),
                            ) {
                                Text("改用密码加密")
                            }
                            TextButton(onClick = viewModel::cancelSingleCaseExport) {
                                Text("取消")
                            }
                        }
                    },
                )
            }
            if (state.singleCasePasswordExportVisible) {
                SingleCasePasswordDialog(
                    title = "设置单命例加密密码",
                    description = "密码不会写入文件，也无法找回。请至少输入 8 个字符并另行保管。",
                    error = state.singleCasePasswordError,
                    requireConfirmation = true,
                    confirmLabel = "选择保存位置",
                    confirmTag = "confirm_password_single_case_export",
                    onConfirm = { password, confirmation ->
                        viewModel.confirmPasswordSingleCaseExport(
                            password,
                            checkNotNull(confirmation),
                        )?.let(onCreateSingleCaseDocument)
                    },
                    onDismiss = viewModel::cancelPasswordSingleCaseExport,
                )
            }
            if (state.singleCasePasswordImportVisible) {
                SingleCasePasswordDialog(
                    title = "输入单命例解密密码",
                    description = "密码只用于本次解密，不会保存。错误密码与损坏文件使用相同错误。",
                    error = state.singleCasePasswordError,
                    requireConfirmation = false,
                    confirmLabel = "解密并预览",
                    confirmTag = "confirm_password_single_case_import",
                    onConfirm = { password, _ ->
                        onRetryPasswordSingleCaseDocument(password)
                    },
                    onDismiss = viewModel::cancelPasswordSingleCaseImport,
                )
            }
            state.singleCasePreview?.let { preview ->
                SingleCasePreviewDialog(
                    preview = preview,
                    busy = state.singleCaseExchangeBusy,
                    onKeepBoth = {
                        viewModel.commitSingleCaseImport(SingleCaseImportDecision.KEEP_BOTH)
                    },
                    onSkip = {
                        viewModel.commitSingleCaseImport(SingleCaseImportDecision.SKIP)
                    },
                    onMergeTarget = viewModel::prepareSingleCaseMerge,
                    onDismiss = viewModel::dismissSingleCasePreview,
                )
            }
            state.singleCaseMergePreparation?.let { preparation ->
                SingleCaseMergeDialog(
                    preparation = preparation,
                    selectedModules = state.singleCaseMergeModules,
                    fieldChoices = state.singleCaseFieldChoices,
                    busy = state.singleCaseExchangeBusy,
                    onToggleModule = viewModel::toggleSingleCaseMergeModule,
                    onChooseField = viewModel::chooseSingleCaseMergeField,
                    onConfirm = viewModel::commitSingleCaseMerge,
                    onDismiss = viewModel::cancelSingleCaseMerge,
                )
            }
            state.fullBackupPreview
                ?.takeIf { state.fullBackupMergePreparation == null }
                ?.let { preview ->
                FullBackupPreviewDialog(
                    preview = preview,
                    decisions = state.fullBackupDecisions,
                    preparedPlan = state.fullBackupRestorePlan,
                    busy = state.fullBackupBusy,
                    onChooseDecision = viewModel::chooseFullBackupDecision,
                    onPrepareMerge = viewModel::prepareFullBackupCaseMerge,
                    onPreparePlan = viewModel::prepareFullBackupRestorePlan,
                    onDismiss = viewModel::dismissFullBackupPreview,
                )
            }
            state.fullBackupMergePreparation?.let { preparation ->
                FullBackupMergeDialog(
                    preparation = preparation,
                    selectedModules = state.fullBackupMergeModules,
                    fieldChoices = state.fullBackupMergeFieldChoices,
                    busy = state.fullBackupBusy,
                    onToggleModule = viewModel::toggleFullBackupMergeModule,
                    onChooseField = viewModel::chooseFullBackupMergeField,
                    onConfirm = viewModel::confirmFullBackupMergeDecision,
                    onDismiss = viewModel::cancelFullBackupCaseMerge,
                )
            }
            state.singleCaseExchangeError?.let { error ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissSingleCaseExchangeError,
                    title = { Text("文件处理失败") },
                    text = { Text(error, modifier = Modifier.testTag("single_case_error")) },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissSingleCaseExchangeError) {
                            Text("知道了")
                        }
                    },
                )
            }
            state.fullBackupError?.let { error ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissFullBackupError,
                    title = { Text("完整备份处理失败") },
                    text = { Text(error, modifier = Modifier.testTag("full_backup_error")) },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissFullBackupError) {
                            Text("知道了")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FullBackupPreviewDialog(
    preview: RestorePreview,
    decisions: Map<String, BackupCaseRestoreDecision>,
    preparedPlan: BackupRestorePlan?,
    busy: Boolean,
    onChooseDecision: (String, BackupCaseRestoreAction) -> Unit,
    onPrepareMerge: (String, String) -> Unit,
    onPreparePlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val manifest = preview.manifest
    val conflictedCases = preview.cases.filter { it.conflicts.isNotEmpty() }
    val conflictCandidateCount = conflictedCases.sumOf { it.conflicts.size }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("完整备份只读预览") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("full_backup_preview"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("来源 App：${manifest.appVersion}")
                Text("格式版本：${manifest.formatVersion}")
                Text("数据库 Schema：${manifest.databaseSchemaVersion}")
                Text("创建时间：${manifest.createdAt}")
                Text("文件保护：${if (manifest.encrypted) "密码加密" else "未加密"}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("命例：${manifest.counts.cases}")
                Text("计算快照：${manifest.counts.snapshots}")
                Text(
                    "文本记录：${manifest.counts.textRecords}，" +
                        "历史：${manifest.counts.textRecordRevisions}",
                )
                Text(
                    "关键事件：${manifest.counts.events}，" +
                        "历史：${manifest.counts.eventRevisions}",
                )
                Text("附件：${manifest.counts.attachments}")
                Text("已校验文件：${preview.sourceFileCount}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("逐命例检查：${preview.cases.size} 个")
                if (conflictedCases.isEmpty()) {
                    Text("当前库未发现稳定 ID、出生输入或四柱冲突。")
                } else {
                    Text(
                        "发现 ${conflictedCases.size} 个来源命例、" +
                            "$conflictCandidateCount 个本地冲突候选。",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("full_backup_conflict_summary"),
                    )
                    conflictedCases.take(MAX_FULL_BACKUP_CONFLICT_CASES).forEach { sourceCase ->
                        Text(
                            "来源：${sourceCase.sourceAlias}" +
                                (if (sourceCase.isTrashed) "（回收站）" else ""),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag(
                                "full_backup_conflict_${sourceCase.sourceCaseId}",
                            ),
                        )
                        sourceCase.conflicts.forEach { conflict ->
                            Text(
                                "本地：${conflict.localAlias}" +
                                    (if (conflict.isTrashed) "（回收站）" else "") +
                                    "；${conflict.reasons.joinToString("、") { it.label }}" +
                                    "；修订 ${conflict.localRevision}",
                            )
                        }
                    }
                    if (conflictedCases.size > MAX_FULL_BACKUP_CONFLICT_CASES) {
                        Text(
                            "另有 ${conflictedCases.size - MAX_FULL_BACKUP_CONFLICT_CASES} 个" +
                                "冲突命例未在此摘要展开。",
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("逐例恢复决策", fontWeight = FontWeight.SemiBold)
                preview.cases.forEach { sourceCase ->
                    val selected = decisions[sourceCase.sourceCaseId]?.action
                    Text("${sourceCase.sourceAlias}：${selected?.label ?: "尚未选择"}")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (sourceCase.conflicts.isEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    onChooseDecision(
                                        sourceCase.sourceCaseId,
                                        BackupCaseRestoreAction.IMPORT_AS_IS,
                                    )
                                },
                                enabled = !busy,
                                modifier = Modifier.testTag(
                                    "full_backup_import_${sourceCase.sourceCaseId}",
                                ),
                            ) { Text("按原 ID 导入") }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onChooseDecision(
                                        sourceCase.sourceCaseId,
                                        BackupCaseRestoreAction.KEEP_BOTH,
                                    )
                                },
                                enabled = !busy,
                                modifier = Modifier.testTag(
                                    "full_backup_keep_${sourceCase.sourceCaseId}",
                                ),
                            ) { Text("保留两份") }
                            sourceCase.conflicts
                                .filterNot { it.isTrashed }
                                .forEach { conflict ->
                                    OutlinedButton(
                                        onClick = {
                                            onPrepareMerge(
                                                sourceCase.sourceCaseId,
                                                conflict.localCaseId,
                                            )
                                        },
                                        enabled = !busy,
                                        modifier = Modifier
                                            .testTag("full_backup_merge_candidate")
                                            .semantics {
                                                contentDescription =
                                                    "来源 ${sourceCase.sourceCaseId} 合并到" +
                                                        " ${conflict.localCaseId}"
                                            },
                                    ) {
                                        Text("范围合并到 ${conflict.localAlias}")
                                    }
                                }
                        }
                        OutlinedButton(
                            onClick = {
                                onChooseDecision(
                                    sourceCase.sourceCaseId,
                                    BackupCaseRestoreAction.SKIP,
                                )
                            },
                            enabled = !busy,
                            modifier = Modifier.testTag(
                                "full_backup_skip_${sourceCase.sourceCaseId}",
                            ),
                        ) { Text("跳过") }
                    }
                }
                preparedPlan?.let {
                    Text(
                        "恢复方案已通过过期与范围检查，尚未写入数据。",
                        modifier = Modifier.testTag("full_backup_plan_ready"),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "本页面已验证文件保护、ZIP 路径、大小、哈希、数据引用、附件一致性和" +
                        "当前库冲突候选；本页只生成恢复方案，不会写入或覆盖数据库。",
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPreparePlan,
                enabled = !busy && decisions.size == preview.cases.size,
                modifier = Modifier.testTag("prepare_full_backup_plan"),
            ) {
                Text(if (preparedPlan == null) "检查恢复方案" else "重新检查方案")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.testTag("close_full_backup_preview"),
            ) { Text("关闭") }
        },
    )
}

private val BackupCaseRestoreAction.label: String
    get() = when (this) {
        BackupCaseRestoreAction.IMPORT_AS_IS -> "按原 ID 导入"
        BackupCaseRestoreAction.SKIP -> "跳过"
        BackupCaseRestoreAction.KEEP_BOTH -> "保留两份"
        BackupCaseRestoreAction.MERGE -> "范围合并"
    }

@Composable
private fun FullBackupMergeDialog(
    preparation: BackupCaseMergePreparation,
    selectedModules: Set<SingleCaseMergeModule>,
    fieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice>,
    busy: Boolean,
    onToggleModule: (SingleCaseMergeModule) -> Unit,
    onChooseField: (SingleCaseFieldKey, SingleCaseValueChoice) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val counts = preparation.analysis.addableCounts
    val availableModules = buildList {
        if (counts.calculationSnapshots > 0) {
            add(
                SingleCaseMergeModule.CALCULATION_SNAPSHOTS to
                    "追加计算快照 ${counts.calculationSnapshots} 条（不改变当前采用盘）",
            )
        }
        if (counts.textRecords + counts.textRecordRevisions > 0) {
            add(
                SingleCaseMergeModule.TEXT_RECORDS to
                    "追加文本记录 ${counts.textRecords} 条、历史 ${counts.textRecordRevisions} 条",
            )
        }
        if (counts.events + counts.eventRevisions > 0) {
            add(
                SingleCaseMergeModule.EVENTS to
                    "追加事件 ${counts.events} 条、历史 ${counts.eventRevisions} 条",
            )
        }
        if (counts.groups + counts.tags > 0) {
            add(
                SingleCaseMergeModule.ORGANIZATION to
                    "追加分组 ${counts.groups} 个、标签 ${counts.tags} 个",
            )
        }
    }
    val hasSelection = selectedModules.any { module ->
        availableModules.any { it.first == module }
    } || fieldChoices.values.any { it == SingleCaseValueChoice.IMPORTED }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("选择完整备份合并范围") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("full_backup_merge_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "目标：${preparation.targetAlias}（修订 ${preparation.targetRevision}）",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "只有明确选中的内容会进入该命例的恢复方案；未选本地字段不会被覆盖。",
                    color = MaterialTheme.colorScheme.error,
                )
                if (availableModules.isNotEmpty()) {
                    Text("按模块追加", fontWeight = FontWeight.SemiBold)
                    availableModules.forEach { (module, label) ->
                        if (module in selectedModules) {
                            Button(
                                onClick = { onToggleModule(module) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("已选择 · $label")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onToggleModule(module) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                if (preparation.analysis.fieldDifferences.isNotEmpty()) {
                    HorizontalDivider()
                    Text("逐字段采用", fontWeight = FontWeight.SemiBold)
                    preparation.analysis.fieldDifferences.forEach { difference ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(difference.label, fontWeight = FontWeight.SemiBold)
                            Text("本地：${difference.localValue}")
                            Text("来源：${difference.importedValue}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectionButton(
                                    text = "保留本地",
                                    selected =
                                        fieldChoices[difference.key] !=
                                            SingleCaseValueChoice.IMPORTED,
                                    onClick = {
                                        onChooseField(
                                            difference.key,
                                            SingleCaseValueChoice.LOCAL,
                                        )
                                    },
                                    tag = "full_merge_local_${difference.key.name}",
                                )
                                SelectionButton(
                                    text = "采用来源",
                                    selected =
                                        fieldChoices[difference.key] ==
                                            SingleCaseValueChoice.IMPORTED,
                                    onClick = {
                                        onChooseField(
                                            difference.key,
                                            SingleCaseValueChoice.IMPORTED,
                                        )
                                    },
                                    tag = "full_merge_imported_${difference.key.name}",
                                )
                            }
                        }
                    }
                }
                if (availableModules.isEmpty() &&
                    preparation.analysis.fieldDifferences.isEmpty()
                ) {
                    Text("该来源命例没有可追加模块或可替换字段。")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy && hasSelection,
                modifier = Modifier.testTag("confirm_full_backup_merge"),
            ) {
                Text("确认所选范围")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.testTag("cancel_full_backup_merge"),
            ) {
                Text("返回批量预览")
            }
        },
    )
}

private val BackupCaseConflictReason.label: String
    get() = when (this) {
        BackupCaseConflictReason.STABLE_ID_EXISTS -> "稳定 ID 已存在"
        BackupCaseConflictReason.SAME_BIRTH_INPUT -> "出生输入相同"
        BackupCaseConflictReason.SAME_FOUR_PILLARS -> "四柱相同"
    }

private const val MAX_FULL_BACKUP_CONFLICT_CASES = 20

@Composable
private fun SingleCasePreviewDialog(
    preview: SingleCasePreview,
    busy: Boolean,
    onKeepBoth: () -> Unit,
    onSkip: () -> Unit,
    onMergeTarget: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sourceCase = preview.document.caseData
    val hasAttachmentReferences = preview.counts.attachmentReferences > 0
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("单命例导入预览") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("single_case_preview"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("别名：${sourceCase.alias}", fontWeight = FontWeight.SemiBold)
                Text("稳定 ID：${sourceCase.id}")
                Text(
                    "来源：App ${preview.document.appVersion} / " +
                        "Schema ${preview.document.databaseSchemaVersion}",
                )
                Text("导出时间：${preview.document.exportedAt}")
                Text(
                    "文件保护：" +
                        when (preview.protection) {
                            SingleCaseDocumentProtection.UNENCRYPTED -> "未加密"
                            SingleCaseDocumentProtection.PASSWORD_PROTECTED -> "密码加密"
                        },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("计算快照：${preview.counts.calculationSnapshots}")
                Text(
                    "文本记录：${preview.counts.textRecords}，" +
                        "历史：${preview.counts.textRecordRevisions}",
                )
                Text(
                    "关键事件：${preview.counts.events}，" +
                        "历史：${preview.counts.eventRevisions}",
                )
                Text(
                    "图片引用：${preview.counts.attachmentReferences}，" +
                        "字段证据：${preview.counts.fieldEvidence}",
                )
                Text(
                    if (hasAttachmentReferences) {
                        "此 JSON 不包含图片二进制。该命例存在图片或字段证据，" +
                            "当前禁止提交导入，以免证据引用失效。"
                    } else {
                        "当前仍是零写入预览；确认导入时会再次核对本地冲突，" +
                            "并创建全新身份，不覆盖现有命例。"
                    },
                    color = MaterialTheme.colorScheme.error,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                if (preview.conflicts.isEmpty()) {
                    Text("本地未发现稳定 ID、出生输入或四柱冲突。")
                } else {
                    Text("本地冲突候选", fontWeight = FontWeight.SemiBold)
                    preview.conflicts.forEach { conflict ->
                        val location = if (conflict.isTrashed) "回收站" else "活动命例"
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "• ${conflict.alias}（$location）：" +
                                    conflict.reasons.joinToString("、") { it.displayName() },
                            )
                            TextButton(
                                onClick = { onMergeTarget(conflict.caseId) },
                                enabled =
                                    !busy && !hasAttachmentReferences && !conflict.isTrashed,
                                modifier = Modifier.testTag(
                                    "merge_single_case_${conflict.caseId}",
                                ),
                            ) {
                                Text(
                                    if (conflict.isTrashed) {
                                        "请先从回收站恢复再合并"
                                    } else {
                                        "与此命例生成合并差异"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onKeepBoth,
                enabled = !busy && !hasAttachmentReferences,
                modifier = Modifier.testTag("keep_both_single_case"),
            ) {
                Text(
                    when {
                        busy -> "正在提交…"
                        preview.conflicts.isEmpty() -> "导入为新命例"
                        else -> "保留两份并导入"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !busy,
                modifier = Modifier.testTag("skip_single_case_import"),
            ) {
                Text("跳过导入")
            }
        },
    )
}

@Composable
private fun SingleCasePasswordDialog(
    title: String,
    description: String,
    error: String?,
    requireConfirmation: Boolean,
    confirmLabel: String,
    confirmTag: String,
    passwordTag: String = "single_case_password",
    confirmationTag: String = "single_case_password_confirmation",
    onConfirm: (CharArray, CharArray?) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(passwordTag),
                )
                if (requireConfirmation) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text("再次输入密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = error != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(confirmationTag),
                    )
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("single_case_password_error"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        password.toCharArray(),
                        if (requireConfirmation) confirmation.toCharArray() else null,
                    )
                },
                modifier = Modifier.testTag(confirmTag),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun SingleCaseMergeDialog(
    preparation: SingleCaseMergePreparation,
    selectedModules: Set<SingleCaseMergeModule>,
    fieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice>,
    busy: Boolean,
    onToggleModule: (SingleCaseMergeModule) -> Unit,
    onChooseField: (SingleCaseFieldKey, SingleCaseValueChoice) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val counts = preparation.addableCounts
    val availableModules = buildList {
        if (counts.calculationSnapshots > 0) {
            add(
                SingleCaseMergeModule.CALCULATION_SNAPSHOTS to
                    "追加计算快照 ${counts.calculationSnapshots} 条（不改变当前采用盘）",
            )
        }
        if (counts.textRecords + counts.textRecordRevisions > 0) {
            add(
                SingleCaseMergeModule.TEXT_RECORDS to
                    "追加文本记录 ${counts.textRecords} 条、历史 ${counts.textRecordRevisions} 条",
            )
        }
        if (counts.events + counts.eventRevisions > 0) {
            add(
                SingleCaseMergeModule.EVENTS to
                    "追加事件 ${counts.events} 条、历史 ${counts.eventRevisions} 条",
            )
        }
        if (counts.groups + counts.tags > 0) {
            add(
                SingleCaseMergeModule.ORGANIZATION to
                    "追加分组 ${counts.groups} 个、标签 ${counts.tags} 个",
            )
        }
    }
    val hasSelection = selectedModules.any { module ->
        availableModules.any { it.first == module }
    } || fieldChoices.values.any { it == SingleCaseValueChoice.IMPORTED }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("选择合并范围") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("single_case_merge_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "目标：${preparation.targetAlias}（修订 ${preparation.targetRevision}）",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "只有下方明确选中的内容会写入；本地未选字段不会被覆盖。",
                    color = MaterialTheme.colorScheme.error,
                )
                if (availableModules.isNotEmpty()) {
                    Text("按模块追加", fontWeight = FontWeight.SemiBold)
                    availableModules.forEach { (module, label) ->
                        if (module in selectedModules) {
                            Button(
                                onClick = { onToggleModule(module) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("已选择 · $label")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onToggleModule(module) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                if (preparation.fieldDifferences.isNotEmpty()) {
                    HorizontalDivider()
                    Text("逐字段采用", fontWeight = FontWeight.SemiBold)
                    preparation.fieldDifferences.forEach { difference ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(difference.label, fontWeight = FontWeight.SemiBold)
                            Text("本地：${difference.localValue}")
                            Text("来源：${difference.importedValue}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectionButton(
                                    text = "保留本地",
                                    selected =
                                        fieldChoices[difference.key] !=
                                            SingleCaseValueChoice.IMPORTED,
                                    onClick = {
                                        onChooseField(
                                            difference.key,
                                            SingleCaseValueChoice.LOCAL,
                                        )
                                    },
                                    tag = "merge_local_${difference.key.name}",
                                )
                                SelectionButton(
                                    text = "采用来源",
                                    selected =
                                        fieldChoices[difference.key] ==
                                            SingleCaseValueChoice.IMPORTED,
                                    onClick = {
                                        onChooseField(
                                            difference.key,
                                            SingleCaseValueChoice.IMPORTED,
                                        )
                                    },
                                    tag = "merge_imported_${difference.key.name}",
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy && hasSelection,
                modifier = Modifier.testTag("confirm_single_case_merge"),
            ) {
                Text(if (busy) "正在合并…" else "确认所选范围")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.testTag("cancel_single_case_merge"),
            ) {
                Text("返回预览")
            }
        },
    )
}

private fun SingleCaseConflictReason.displayName(): String = when (this) {
    SingleCaseConflictReason.STABLE_ID_EXISTS -> "稳定 ID 已存在"
    SingleCaseConflictReason.SAME_BIRTH_INPUT -> "出生输入相同"
    SingleCaseConflictReason.SAME_FOUR_PILLARS -> "采用四柱相同"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseListScreen(
    state: StageTwoUiState,
    onQueryChange: (String) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSelectSort: (CaseSortOrder) -> Unit,
    onSelectVisibility: (CaseVisibility) -> Unit,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onImportSingleCase: () -> Unit,
    onExportFullBackup: () -> Unit,
    onPreviewFullBackup: () -> Unit,
    onOpenCase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_list_screen"),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("南枫八字", fontWeight = FontWeight.SemiBold)
                    Text(
                        "本地命例",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                if (state.visibility == CaseVisibility.ACTIVE) {
                    Button(
                        onClick = onCreate,
                        enabled = !state.singleCaseExchangeBusy,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("new_case_button"),
                    ) {
                        Text("新建命例")
                    }
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onImportSingleCase,
                enabled = !state.singleCaseExchangeBusy && !state.fullBackupBusy,
                modifier = Modifier.testTag("import_single_case_button"),
            ) {
                Text(if (state.singleCaseExchangeBusy) "读取中…" else "导入单命例")
            }
            OutlinedButton(
                onClick = onExportFullBackup,
                enabled = !state.fullBackupBusy && !state.singleCaseExchangeBusy,
                modifier = Modifier.testTag("export_full_backup_button"),
            ) {
                Text(if (state.fullBackupBusy) "处理中…" else "导出完整备份")
            }
            OutlinedButton(
                onClick = onPreviewFullBackup,
                enabled = !state.fullBackupBusy && !state.singleCaseExchangeBusy,
                modifier = Modifier.testTag("preview_full_backup_button"),
            ) {
                Text("检查完整备份")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectionButton(
                text = "命例",
                selected = state.visibility == CaseVisibility.ACTIVE,
                onClick = { onSelectVisibility(CaseVisibility.ACTIVE) },
                tag = "visibility_active",
            )
            SelectionButton(
                text = "回收站",
                selected = state.visibility == CaseVisibility.TRASHED,
                onClick = { onSelectVisibility(CaseVisibility.TRASHED) },
                tag = "visibility_trashed",
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("case_search"),
            label = { Text("搜索姓名、别名或四柱") },
            singleLine = true,
        )
        CaseListControls(
            state = state,
            onSelectGroup = onSelectGroup,
            onSelectTag = onSelectTag,
            onSelectSort = onSelectSort,
        )
        when {
            state.listLoading -> LoadingBox("正在读取命例…")
            state.listError != null -> ErrorBox(
                message = state.listError,
                actionLabel = "重试",
                onAction = onRefresh,
            )
            state.cases.isEmpty() -> EmptyCaseList(state.visibility, onCreate)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.cases, key = { it.id }) { summary ->
                    CaseSummaryCard(summary, onClick = { onOpenCase(summary.id) })
                }
            }
        }
    }
}

@Composable
private fun CaseListControls(
    state: StageTwoUiState,
    onSelectGroup: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSelectSort: (CaseSortOrder) -> Unit,
) {
    FilterRow(
        title = "分组",
        allSelected = state.selectedGroupId == null,
        values = state.availableGroups.map { it.id to it.name },
        selectedId = state.selectedGroupId,
        onSelected = onSelectGroup,
        testTagPrefix = "group_filter",
    )
    FilterRow(
        title = "标签",
        allSelected = state.selectedTagId == null,
        values = state.availableTags.map { it.id to it.name },
        selectedId = state.selectedTagId,
        onSelected = onSelectTag,
        testTagPrefix = "tag_filter",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("排序", style = MaterialTheme.typography.labelLarge)
        CaseSortOrder.entries.forEach { sort ->
            SelectionButton(
                text = sort.displayName(),
                selected = state.sortOrder == sort,
                onClick = { onSelectSort(sort) },
                tag = "sort_${sort.name.lowercase()}",
            )
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    allSelected: Boolean,
    values: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    testTagPrefix: String,
) {
    if (values.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        SelectionButton(
            text = "全部",
            selected = allSelected,
            onClick = { onSelected(null) },
            tag = "${testTagPrefix}_all",
        )
        values.forEach { (id, name) ->
            SelectionButton(
                text = name,
                selected = selectedId == id,
                onClick = { onSelected(id) },
                tag = "${testTagPrefix}_$id",
            )
        }
    }
}

@Composable
private fun SelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.testTag(tag)) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
            Text(text)
        }
    }
}

@Composable
private fun EmptyCaseList(
    visibility: CaseVisibility,
    onCreate: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                if (visibility == CaseVisibility.TRASHED) "回收站为空" else "还没有命例",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (visibility == CaseVisibility.TRASHED) {
                    "移入回收站的命例会保留完整数据，并可从这里恢复。"
                } else {
                    "先手动录入出生资料，应用会完成排盘并保存到本机。"
                },
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (visibility == CaseVisibility.ACTIVE) {
                Button(
                    onClick = onCreate,
                    modifier = Modifier.padding(top = 20.dp),
                ) {
                    Text("新建第一个命例")
                }
            }
        }
    }
}

@Composable
private fun CaseSummaryCard(
    summary: CaseSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "打开命例：${summary.alias}" }
            .testTag("case_${summary.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                buildString {
                    if (summary.isPinned) append("📌 ")
                    if (summary.isFavorite) append("★ ")
                    append(summary.name.value ?: summary.alias)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "别名：${summary.alias}",
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${summary.sexForFortuneDirection.displayName()} · " +
                    summary.birthInput.displayDateTime(),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "四柱：${summary.fourPillars?.display() ?: "暂无计算结果"}",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.groups.isNotEmpty()) {
                Text(
                    "分组：${summary.groups.joinToString("、") { it.name }}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.tags.isNotEmpty()) {
                Text(
                    "标签：${summary.tags.joinToString("、") { it.name }}",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCaseScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onSubmit: () -> Unit,
    onConfirmDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CaseFormScreen(
        title = "新建命例",
        screenTag = "create_case_screen",
        form = state.form,
        error = state.formError,
        saving = state.saving,
        submitLabel = "排盘并保存",
        onBack = onBack,
        onFormChange = onFormChange,
        onSubmit = onSubmit,
        duplicateCandidates = state.duplicateCandidates,
        onConfirmDuplicate = onConfirmDuplicate,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseFormScreen(
    title: String,
    screenTag: String,
    form: CaseFormState,
    error: String?,
    saving: Boolean,
    submitLabel: String,
    onBack: () -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    duplicateCandidates: List<DuplicateCaseCandidate> = emptyList(),
    onConfirmDuplicate: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(screenTag),
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionHeading("身份信息", "别名用于本地识别；姓名可以留空。")
            OutlinedTextField(
                value = form.alias,
                onValueChange = { value -> onFormChange { it.copy(alias = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("case_alias"),
                label = { Text("命例别名 *") },
                singleLine = true,
                enabled = !saving,
            )
            OutlinedTextField(
                value = form.name,
                onValueChange = { value -> onFormChange { it.copy(name = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("case_name"),
                label = { Text("姓名（可选）") },
                singleLine = true,
                enabled = !saving,
            )
            Text(
                "性别 *",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SexButton(
                    text = "男",
                    selected = form.sex == SexForFortuneDirection.MAN,
                    enabled = !saving,
                    tag = "sex_man",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.MAN) }
                    },
                )
                SexButton(
                    text = "女",
                    selected = form.sex == SexForFortuneDirection.WOMAN,
                    enabled = !saving,
                    tag = "sex_woman",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.WOMAN) }
                    },
                )
            }

            SectionHeading(
                "出生时间",
                "当前阶段支持公历、北京时间民用时；不静默转换农历或真太阳时。",
            )
            NumericFieldRow(
                values = listOf(
                    NumericField("年", form.year, "birth_year") {
                        onFormChange { form -> form.copy(year = it) }
                    },
                    NumericField("月", form.month, "birth_month") {
                        onFormChange { form -> form.copy(month = it) }
                    },
                    NumericField("日", form.day, "birth_day") {
                        onFormChange { form -> form.copy(day = it) }
                    },
                ),
                enabled = !saving,
            )
            NumericFieldRow(
                values = listOf(
                    NumericField("时", form.hour, "birth_hour") {
                        onFormChange { form -> form.copy(hour = it) }
                    },
                    NumericField("分", form.minute, "birth_minute") {
                        onFormChange { form -> form.copy(minute = it) }
                    },
                    NumericField("秒", form.second, "birth_second") {
                        onFormChange { form -> form.copy(second = it) }
                    },
                ),
                enabled = !saving,
            )
            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("form_error"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            if (duplicateCandidates.isNotEmpty() && onConfirmDuplicate != null) {
                DuplicateCandidatesCard(
                    candidates = duplicateCandidates,
                    saving = saving,
                    onConfirm = onConfirmDuplicate,
                )
            }
            Button(
                onClick = onSubmit,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 28.dp)
                    .height(52.dp)
                    .testTag("save_case"),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(22.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("正在排盘并保存…")
                } else {
                    Text(submitLabel)
                }
            }
        }
    }
}

@Composable
private fun DuplicateCandidatesCard(
    candidates: List<DuplicateCaseCandidate>,
    saving: Boolean,
    onConfirm: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("duplicate_candidates"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "发现 ${candidates.size} 个疑似重复命例",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            candidates.take(5).forEach { candidate ->
                val location = if (candidate.summary.deletedAt == null) {
                    "命例列表"
                } else {
                    "回收站"
                }
                val reasons = candidate.reasons.joinToString("、") {
                    when (it) {
                        DuplicateReason.SAME_BIRTH_INPUT -> "出生时间与性别相同"
                        DuplicateReason.SAME_FOUR_PILLARS -> "四柱相同"
                    }
                }
                Text(
                    "• ${candidate.summary.alias}（$location；$reasons）",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "系统不会自动合并或覆盖。只有确认确需保留两份时才继续。",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onConfirm,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("confirm_duplicate_save"),
            ) {
                Text("确认仍然保存")
            }
        }
    }
}

private data class NumericField(
    val label: String,
    val value: String,
    val tag: String,
    val onChange: (String) -> Unit,
)

@Composable
private fun NumericFieldRow(
    values: List<NumericField>,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        values.forEach { field ->
            OutlinedTextField(
                value = field.value,
                onValueChange = field.onChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(field.tag),
                label = { Text(field.label) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
private fun SexButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(tag),
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(tag),
        ) {
            Text(text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseDetailScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onEditCase: () -> Unit,
    onEditMetadata: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_detail_screen"),
    ) {
        TopAppBar(
            title = { Text(state.detail?.name?.value ?: state.detail?.alias ?: "命例详情") },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
        )
        when {
            state.detailLoading -> LoadingBox("正在读取命例详情…")
            state.detailError != null -> ErrorBox(
                message = state.detailError,
                actionLabel = "返回列表",
                onAction = onBack,
            )
            state.detail != null -> CaseDetailContent(
                case = state.detail,
                onEditCase = onEditCase,
                onEditMetadata = onEditMetadata,
                onAddRecord = onAddRecord,
                onEditRecord = onEditRecord,
                onAddEvent = onAddEvent,
                onEditEvent = onEditEvent,
                onDuplicate = onDuplicate,
                onExportSingleCase = onExportSingleCase,
                onMoveToTrash = onMoveToTrash,
                onRestore = onRestore,
                singleCaseExchangeBusy = state.singleCaseExchangeBusy,
            )
        }
    }
}

@Composable
private fun CaseDetailContent(
    case: BaziCase,
    onEditCase: () -> Unit,
    onEditMetadata: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    singleCaseExchangeBusy: Boolean,
) {
    val adopted = case.calculationSnapshots.asReversed().firstOrNull { it.adopted }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (case.deletedAt == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onEditCase,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_case_button"),
                ) {
                    Text("编辑资料")
                }
                OutlinedButton(
                    onClick = onEditMetadata,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_metadata_button"),
                ) {
                    Text("管理分类")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("duplicate_case_button"),
                ) {
                    Text("复制命例")
                }
                OutlinedButton(
                    onClick = onMoveToTrash,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trash_case_button"),
                ) {
                    Text("移入回收站")
                }
            }
            OutlinedButton(
                onClick = onExportSingleCase,
                enabled = !singleCaseExchangeBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("export_single_case_button"),
            ) {
                Text(if (singleCaseExchangeBusy) "正在导出…" else "导出单命例 JSON")
            }
        } else {
            Button(
                onClick = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("restore_case_button"),
            ) {
                Text("恢复命例")
            }
        }
        DetailSection("命例管理") {
            DetailRow("收藏", if (case.isFavorite) "是" else "否")
            DetailRow("置顶", if (case.isPinned) "是" else "否")
            DetailRow("状态", if (case.deletedAt == null) "正常" else "回收站")
            DetailRow(
                "分组",
                case.groups.joinToString("、") { it.name }.ifEmpty { "未设置" },
            )
            DetailRow(
                "标签",
                case.tags.joinToString("、") { it.name }.ifEmpty { "未设置" },
            )
        }
        DetailSection("原始录入信息") {
            DetailRow("命例别名", case.alias)
            DetailRow(
                "姓名",
                when (case.name.state) {
                    FieldValueState.PRESENT -> case.name.value.orEmpty()
                    FieldValueState.ABSENT -> "未提供"
                    FieldValueState.CLEARED -> "已清空"
                },
            )
            DetailRow("性别", case.sexForFortuneDirection.displayName())
            DetailRow("历法与时间", case.birthInput.displayDateTime())
            DetailRow("时区", case.birthInput.timeZoneId)
            DetailRow("时间精度", case.birthInput.timePrecision.name)
            DetailRow("来源", case.sourceType.displayName())
            case.copiedFromCaseId?.let { sourceId ->
                DetailRow("复制来源", sourceId)
            }
        }
        DetailSection("计算结果") {
            if (adopted == null) {
                Text("当前命例没有已采用的计算快照。")
            } else {
                DetailRow("四柱", adopted.result.fourPillars.display())
                DetailRow("计算配置", adopted.result.profile.id)
                DetailRow("引擎", adopted.result.evidence.engineName)
                DetailRow("引擎版本", adopted.result.evidence.engineVersion)
                DetailRow("规则版本", adopted.result.evidence.ruleVersion)
                DetailRow(
                    "起运方向",
                    if (adopted.result.fortuneStart.direction.name == "FORWARD") {
                        "顺排"
                    } else {
                        "逆排"
                    },
                )
                DetailRow(
                    "起运年龄",
                    "${adopted.result.fortuneStart.years} 年 " +
                        "${adopted.result.fortuneStart.months} 月 " +
                        "${adopted.result.fortuneStart.days} 日",
                )
                DetailRow(
                    "大运",
                    adopted.result.decadeFortunes.joinToString("、") { it.name },
                )
            }
        }
        DetailSection("分析与记录") {
            if (case.deletedAt == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddRecord,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_record_button"),
                    ) {
                        Text("新增记录")
                    }
                    OutlinedButton(
                        onClick = onAddEvent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_event_button"),
                    ) {
                        Text("新增事件")
                    }
                }
            } else {
                Text(
                    "回收站中的记录为只读；恢复命例后可继续编辑。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (case.textRecords.isEmpty()) {
                Text(
                    "暂无笔记、反馈或点评。",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                case.textRecords.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("record_card")
                            .clickable(enabled = case.deletedAt == null) {
                                onEditRecord(record.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                buildString {
                                    append(record.type.displayName())
                                    if (record.type == CaseTextRecordType.ANALYSIS) {
                                        append(" · ")
                                        append(
                                            (
                                                record.analysisCategory
                                                    ?: AnalysisCategory.GENERAL
                                                ).displayName(),
                                        )
                                    }
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                record.content,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 4,
                            )
                        }
                    }
                }
            }
            if (case.events.isEmpty()) {
                Text(
                    "暂无关键事件。",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                case.events.forEach { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clickable(enabled = case.deletedAt == null) {
                                onEditEvent(event.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${event.displayDate()} · ${event.category.displayName()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            event.title?.let { title ->
                                Text(
                                    title,
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Text(
                                event.rawText,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 4,
                            )
                            if (event.status != null) {
                                Text(
                                    "状态：${event.status}",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            if (
                case.textRecordRevisions.isNotEmpty() ||
                case.eventRevisions.isNotEmpty()
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                Text(
                    "版本历史（记录 ${case.textRecordRevisions.size} / " +
                        "事件 ${case.eventRevisions.size}）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                case.textRecordRevisions.asReversed().forEach { revision ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "记录 · ${revision.changeType.displayName()} · " +
                                    "v${revision.version} · " +
                                    revision.snapshot.type.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (revision.snapshot.type == CaseTextRecordType.ANALYSIS) {
                                Text(
                                    "分类：${
                                        (
                                            revision.snapshot.analysisCategory
                                                ?: AnalysisCategory.GENERAL
                                            ).displayName()
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                revision.snapshot.content,
                                modifier = Modifier.padding(top = 3.dp),
                                maxLines = 6,
                            )
                        }
                    }
                }
                case.eventRevisions.asReversed().forEach { revision ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "事件 · ${revision.changeType.displayName()} · " +
                                    "v${revision.version} · " +
                                    revision.snapshot.displayDate() + " · " +
                                    revision.snapshot.category.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            revision.snapshot.title?.let { title ->
                                Text(
                                    title,
                                    modifier = Modifier.padding(top = 3.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Text(
                                revision.snapshot.rawText,
                                modifier = Modifier.padding(top = 3.dp),
                                maxLines = 6,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionHeading(title: String, description: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        description,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LoadingBox(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(message, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun ErrorBox(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = MaterialTheme.colorScheme.error)
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun SexForFortuneDirection.displayName(): String = when (this) {
    SexForFortuneDirection.MAN -> "男"
    SexForFortuneDirection.WOMAN -> "女"
}

private fun CaseSourceType.displayName(): String = when (this) {
    CaseSourceType.MANUAL -> "手动录入"
    CaseSourceType.CASE_COPY -> "命例复制"
    CaseSourceType.WENZHEN_SCREENSHOT -> "问真截图迁移"
    CaseSourceType.BACKUP_RESTORE -> "备份恢复"
}

internal fun CaseTextRecordType.displayName(): String = when (this) {
    CaseTextRecordType.NOTE -> "普通笔记"
    CaseTextRecordType.OWNER_FEEDBACK -> "命主反馈"
    CaseTextRecordType.MASTER_COMMENTARY -> "师傅点评"
    CaseTextRecordType.ANALYSIS -> "分析记录"
}

internal fun AnalysisCategory.displayName(): String = when (this) {
    AnalysisCategory.GENERAL -> "综合"
    AnalysisCategory.PERSONALITY -> "性格"
    AnalysisCategory.CAREER -> "事业"
    AnalysisCategory.WEALTH -> "财运"
    AnalysisCategory.RELATIONSHIP -> "感情"
    AnalysisCategory.HEALTH -> "健康"
    AnalysisCategory.EDUCATION -> "学业"
    AnalysisCategory.FAMILY -> "家庭"
    AnalysisCategory.OTHER -> "其他"
}

internal fun CaseEventCategory.displayName(): String = when (this) {
    CaseEventCategory.GENERAL -> "综合"
    CaseEventCategory.EDUCATION -> "学业"
    CaseEventCategory.CAREER -> "事业"
    CaseEventCategory.WEALTH -> "财运"
    CaseEventCategory.RELATIONSHIP -> "感情"
    CaseEventCategory.FAMILY -> "家庭"
    CaseEventCategory.HEALTH -> "健康"
    CaseEventCategory.OTHER -> "其他"
}

private fun RecordChangeType.displayName(): String = when (this) {
    RecordChangeType.CREATED -> "新增"
    RecordChangeType.UPDATED -> "修改"
    RecordChangeType.DELETED -> "删除"
}

private fun CaseEvent.displayDate(): String = when {
    year == null -> "日期待核对"
    month == null -> "${year}年"
    day == null -> "${year}年${month}月"
    else -> "${year}年${month}月${day}日"
}

private fun com.nanzhufeng.nanfengbazi.domain.model.BirthInput.displayDateTime(): String =
    when (val calendar = calendarInput) {
        is BirthCalendarInput.Solar -> "公历 ${calendar.dateTime.display()}"
        is BirthCalendarInput.Lunar -> {
            val date = calendar.dateTime
            "农历 ${date.year}年${date.month}月${date.day}日 " +
                "%02d:%02d:%02d".format(date.hour, date.minute, date.second)
        }
    }

private fun CivilDateTime.display(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun FourPillars.display(): String = "$year $month $day $hour"

private fun CaseSortOrder.displayName(): String = when (this) {
    CaseSortOrder.LAST_VIEWED_DESC -> "最近查看"
    CaseSortOrder.UPDATED_DESC -> "最近更新"
    CaseSortOrder.CREATED_DESC -> "最近创建"
    CaseSortOrder.BIRTH_ASC -> "出生时间"
}
