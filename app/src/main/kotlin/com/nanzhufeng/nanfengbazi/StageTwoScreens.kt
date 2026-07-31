package com.nanzhufeng.nanfengbazi

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.FortunePosition
import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
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
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NanfengBaziApp(
    viewModel: StageTwoViewModel,
    onImportScreenshots: () -> Unit = {},
    screenshotImportState: ScreenshotImportUiState = ScreenshotImportUiState(),
    onRetryScreenshotImport: () -> Unit = {},
    onDeleteScreenshotImport: () -> Unit = {},
    onSetScreenshotFieldAdopted: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onUpdateScreenshotFieldValue: (String, String, String) -> Unit = { _, _, _ -> },
    onSetScreenshotLongTextAdopted: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSetScreenshotCandidateAdopted: (String, Boolean) -> Unit = { _, _ -> },
    onCommitScreenshotCandidate: (String, Boolean) -> Unit = { _, _ -> },
    onConsumeScreenshotImportMessage: () -> Unit = {},
    onCreateSingleCaseDocument: (String) -> Unit = {},
    onOpenSingleCaseDocument: () -> Unit = {},
    onRetryPasswordSingleCaseDocument: (CharArray) -> Unit = {},
    onCommitSingleCaseImport: ((SingleCaseImportDecision) -> Unit)? = null,
    onCommitSingleCaseMerge: (() -> Unit)? = null,
    onCommitPasswordSingleCaseDocument: (CharArray) -> Unit = {},
    onCreateFullBackupDocument: (String) -> Unit = {},
    onCreateEncryptedFullBackupDocument: (String) -> Unit = {},
    onOpenFullBackupDocument: () -> Unit = {},
    onRetryPasswordFullBackupDocument: (CharArray) -> Unit = {},
    onExecuteFullBackupDocument: (CharArray?) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    val commitSingleCaseImport = onCommitSingleCaseImport
        ?: { decision -> viewModel.commitSingleCaseImport(decision) }
    val commitSingleCaseMerge = onCommitSingleCaseMerge
        ?: { viewModel.commitSingleCaseMerge() }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(screenshotImportState.message) {
        screenshotImportState.message?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeScreenshotImportMessage()
        }
    }
    LaunchedEffect(screenshotImportState.committedCaseCount) {
        if (screenshotImportState.committedCaseCount > 0) {
            viewModel.refreshCases()
            if (
                screenshotImportState.activeSessionId == null &&
                state.destination == AppDestination.ScreenshotImportReview
            ) {
                viewModel.navigateBack()
            }
        }
    }
    BackHandler(
        enabled = state.destination !in setOf(
            AppDestination.CaseList,
            AppDestination.RecordHub,
            AppDestination.Settings,
        ),
    ) {
        viewModel.navigateBack()
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val useNavigationRail = maxWidth >= EXPANDED_NAVIGATION_MIN_WIDTH
                val showRootNavigation = state.destination in setOf(
                    AppDestination.CaseList,
                    AppDestination.CreateCase,
                    AppDestination.RecordHub,
                    AppDestination.Settings,
                ) || (useNavigationRail && state.destination is AppDestination.CaseDetail)
                val openChart = {
                    if (state.destination != AppDestination.CreateCase) {
                        viewModel.openCreate()
                    }
                }
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showRootNavigation && !useNavigationRail) {
                            RootNavigationBar(
                                destination = state.destination,
                                onOpenChart = openChart,
                                onOpenCases = viewModel::backToList,
                                onOpenRecords = viewModel::openRecordHub,
                                onOpenSettings = viewModel::openSettings,
                            )
                        }
                    },
                ) { padding ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (showRootNavigation && useNavigationRail) {
                            RootNavigationRail(
                                destination = state.destination,
                                onOpenChart = openChart,
                                onOpenCases = viewModel::backToList,
                                onOpenRecords = viewModel::openRecordHub,
                                onOpenSettings = viewModel::openSettings,
                                modifier = Modifier.padding(padding),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        ) {
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
                        onImportScreenshots = onImportScreenshots,
                        screenshotImportState = screenshotImportState,
                        onRetryScreenshotImport = onRetryScreenshotImport,
                        onDeleteScreenshotImport = onDeleteScreenshotImport,
                        onReviewScreenshotImport = viewModel::openScreenshotImportReview,
                        onImportSingleCase = onOpenSingleCaseDocument,
                        onExportFullBackup = viewModel::requestFullBackupExport,
                        onPreviewFullBackup = onOpenFullBackupDocument,
                        onOpenCase = viewModel::openDetail,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.RecordHub -> RecordHubScreen(
                        cases = state.cases,
                        loading = state.listLoading,
                        error = state.listError,
                        onRefresh = viewModel::refreshCases,
                        onOpenCase = viewModel::openDetail,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.Settings -> SettingsHomeScreen(
                        state = state,
                        screenshotImportState = screenshotImportState,
                        onImportScreenshots = onImportScreenshots,
                        onImportSingleCase = onOpenSingleCaseDocument,
                        onExportFullBackup = viewModel::requestFullBackupExport,
                        onRestoreFullBackup = onOpenFullBackupDocument,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.ScreenshotImportReview -> ScreenshotImportReviewScreen(
                        state = screenshotImportState,
                        onBack = viewModel::navigateBack,
                        onSetFieldAdopted = onSetScreenshotFieldAdopted,
                        onUpdateFieldValue = onUpdateScreenshotFieldValue,
                        onSetLongTextAdopted = onSetScreenshotLongTextAdopted,
                        onSetCandidateAdopted = onSetScreenshotCandidateAdopted,
                        onCommitCandidate = onCommitScreenshotCandidate,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.CreateCase -> CreateCaseScreen(
                        state = state,
                        onBack = viewModel::backToList,
                        onOpenCase = viewModel::openDetail,
                        onFormChange = viewModel::updateForm,
                        onPreview = viewModel::previewCase,
                        onSubmit = { viewModel.submitCase() },
                        onConfirmDuplicate = { viewModel.submitCase(allowDuplicate = true) },
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.CaseDetail -> {
                        val detailContent: @Composable (Modifier) -> Unit = { modifier ->
                            CaseDetailScreen(
                                state = state,
                                onBack = viewModel::backToList,
                                onEditCase = viewModel::openEditCase,
                                onAddBirthTimeCandidate = viewModel::openBirthTimeCandidate,
                                onAdoptBirthTimeCandidate = viewModel::adoptBirthTimeCandidate,
                                onEditMetadata = viewModel::openMetadata,
                                onAddRecord = { viewModel.openTextRecord() },
                                onEditRecord = viewModel::openTextRecord,
                                onAddEvent = { viewModel.openEvent() },
                                onEditEvent = viewModel::openEvent,
                                onDuplicate = viewModel::duplicateCase,
                                onExportSingleCase = viewModel::requestSingleCaseExport,
                                onMoveToTrash = viewModel::requestMoveToTrash,
                                onRestore = viewModel::restoreCase,
                                onSelectSection = viewModel::selectDetailSection,
                                onFortuneObservationDateChange =
                                    viewModel::updateFortuneObservationDate,
                                onFortuneObservationTimeChange =
                                    viewModel::updateFortuneObservationTime,
                                modifier = modifier,
                            )
                        }
                        if (useNavigationRail) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                            ) {
                                ExpandedCaseIndexPane(
                                    cases = state.cases,
                                    selectedCaseId = state.detail?.id,
                                    loading = state.listLoading,
                                    error = state.listError,
                                    onRefresh = viewModel::refreshCases,
                                    onOpenCase = viewModel::openDetail,
                                    modifier = Modifier
                                        .width(340.dp)
                                        .fillMaxHeight(),
                                )
                                VerticalDivider()
                                detailContent(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        } else {
                            detailContent(Modifier.padding(padding))
                        }
                    }
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
                    is AppDestination.AddBirthTimeCandidate -> CaseFormScreen(
                        title = "新增出生时间候选",
                        screenTag = "add_birth_time_candidate_screen",
                        form = state.candidateForm,
                        error = state.mutationError,
                        saving = state.mutationSaving,
                        submitLabel = "计算并添加候选",
                        showIdentityFields = false,
                        candidateLabel = state.candidateLabel,
                        onCandidateLabelChange = viewModel::updateCandidateLabel,
                        sexEditable = false,
                        onBack = viewModel::navigateBack,
                        onFormChange = viewModel::updateCandidateForm,
                        onSubmit = viewModel::saveBirthTimeCandidate,
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
                    }
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
            if (state.fullBackupRestoreConfirmationVisible) {
                val plan = state.fullBackupRestorePlan
                AlertDialog(
                    onDismissRequest = viewModel::cancelFullBackupRestore,
                    title = { Text("执行完整备份恢复？") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "将按已检查的逐例方案写入当前资料库，并复制被导入命例的" +
                                    "真实附件。不会提供无范围覆盖。",
                            )
                            plan?.let {
                                Text(
                                    "按原 ID ${it.decisions.count { decision ->
                                        decision.action ==
                                            BackupCaseRestoreAction.IMPORT_AS_IS
                                    }}，保留两份 ${it.decisions.count { decision ->
                                        decision.action ==
                                            BackupCaseRestoreAction.KEEP_BOTH
                                    }}，范围合并 ${it.decisions.count { decision ->
                                        decision.action == BackupCaseRestoreAction.MERGE
                                    }}，跳过 ${it.decisions.count { decision ->
                                        decision.action == BackupCaseRestoreAction.SKIP
                                    }}。",
                                )
                            }
                            Text(
                                "提交前会重新读取同一文件、复核冲突和目标修订；任一步失败会" +
                                    "回滚数据库并移除本次新增附件。",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (viewModel.confirmFullBackupRestore()) {
                                    onExecuteFullBackupDocument(null)
                                }
                            },
                            modifier = Modifier.testTag("confirm_full_backup_restore"),
                        ) {
                            Text(
                                if (plan?.preview?.manifest?.encrypted == true) {
                                    "继续并输入密码"
                                } else {
                                    "确认执行恢复"
                                },
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = viewModel::cancelFullBackupRestore,
                            modifier = Modifier.testTag("cancel_full_backup_restore"),
                        ) {
                            Text("返回方案")
                        }
                    },
                )
            }
            if (state.fullBackupRestorePasswordVisible) {
                SingleCasePasswordDialog(
                    title = "再次输入完整备份密码",
                    description = "密码只用于本次提交前重新认证同一备份，不会保存。",
                    error = state.fullBackupPasswordError,
                    requireConfirmation = false,
                    confirmLabel = "认证并执行恢复",
                    confirmTag = "confirm_password_full_backup_restore",
                    passwordTag = "full_backup_restore_password",
                    onConfirm = { password, _ ->
                        onExecuteFullBackupDocument(password)
                    },
                    onDismiss = viewModel::cancelFullBackupRestore,
                )
            }
            if (state.singleCaseExportConfirmationVisible) {
                AlertDialog(
                    onDismissRequest = viewModel::cancelSingleCaseExport,
                    title = { Text("选择单命例导出内容") },
                    text = {
                        val attachmentCount = state.detail?.attachments?.size ?: 0
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "文件可能包含出生资料、健康、婚姻、财务、反馈和分析，" +
                                    "未加密导出请妥善保管。",
                            )
                            SelectionButton(
                                text = "JSON（只保存图片引用，兼容轻量交换）",
                                selected = !state.singleCaseExportIncludesAttachments,
                                onClick = {
                                    viewModel.chooseSingleCaseExportAttachments(false)
                                },
                                tag = "single_case_export_references_only",
                            )
                            if (attachmentCount > 0) {
                                SelectionButton(
                                    text = "命例包（包含 $attachmentCount 个图片附件）",
                                    selected =
                                        state.singleCaseExportIncludesAttachments,
                                    onClick = {
                                        viewModel.chooseSingleCaseExportAttachments(true)
                                    },
                                    tag = "single_case_export_with_attachments",
                                )
                                Text(
                                    "命例包会逐个校验附件大小与 SHA-256，适合完整迁移问真截图证据。",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Text(
                                    "该命例当前没有图片附件，只需导出 JSON。",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
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
            if (state.singleCasePasswordCommitVisible) {
                SingleCasePasswordDialog(
                    title = "再次输入命例包密码",
                    description = "密码只用于提交前重新认证同一命例包，不会保存。",
                    error = state.singleCasePasswordError,
                    requireConfirmation = false,
                    confirmLabel = "认证并提交",
                    confirmTag = "confirm_password_single_case_commit",
                    passwordTag = "single_case_commit_password",
                    onConfirm = { password, _ ->
                        onCommitPasswordSingleCaseDocument(password)
                    },
                    onDismiss = viewModel::cancelPasswordSingleCaseCommit,
                )
            }
            state.singleCasePreview?.let { preview ->
                SingleCasePreviewDialog(
                    preview = preview,
                    busy = state.singleCaseExchangeBusy,
                    onKeepBoth = {
                        commitSingleCaseImport(SingleCaseImportDecision.KEEP_BOTH)
                    },
                    onSkip = {
                        commitSingleCaseImport(SingleCaseImportDecision.SKIP)
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
                    onConfirm = commitSingleCaseMerge,
                    onDismiss = viewModel::cancelSingleCaseMerge,
                )
            }
            state.fullBackupPreview
                ?.takeIf {
                    state.fullBackupMergePreparation == null &&
                        !state.fullBackupRestoreConfirmationVisible &&
                        !state.fullBackupRestorePasswordVisible
                }
                ?.let { preview ->
                FullBackupRestoreWorkspace(
                    preview = preview,
                    decisions = state.fullBackupDecisions,
                    preparedPlan = state.fullBackupRestorePlan,
                    busy = state.fullBackupBusy,
                    onChooseDecision = viewModel::chooseFullBackupDecision,
                    onSkipAll = viewModel::skipAllFullBackupCases,
                    onPrepareMerge = viewModel::prepareFullBackupCaseMerge,
                    onPreparePlan = viewModel::prepareFullBackupRestorePlan,
                    onRequestRestore = viewModel::requestFullBackupRestore,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullBackupRestoreWorkspace(
    preview: RestorePreview,
    decisions: Map<String, BackupCaseRestoreDecision>,
    preparedPlan: BackupRestorePlan?,
    busy: Boolean,
    onChooseDecision: (String, BackupCaseRestoreAction) -> Unit,
    onSkipAll: () -> Unit,
    onPrepareMerge: (String, String) -> Unit,
    onPreparePlan: () -> Unit,
    onRequestRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val manifest = preview.manifest
    val conflictedCases = preview.cases.filter { it.conflicts.isNotEmpty() }
    val conflictCandidateCount = conflictedCases.sumOf { it.conflicts.size }
    val selectedCount = decisions.size
    BackHandler(enabled = !busy, onBack = onDismiss)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_backup_preview"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("完整备份恢复工作台")
                        Text(
                            "已决策 $selectedCount / ${preview.cases.size}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !busy,
                        modifier = Modifier.testTag("close_full_backup_preview"),
                    ) {
                        Text("关闭")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    preparedPlan?.let {
                        Text(
                            "方案已通过复核，尚未写入数据。",
                            modifier = Modifier.testTag("full_backup_plan_ready"),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onPreparePlan,
                            enabled = !busy && selectedCount == preview.cases.size,
                            modifier = Modifier.testTag("prepare_full_backup_plan"),
                        ) {
                            Text(if (preparedPlan == null) "检查恢复方案" else "重新检查方案")
                        }
                        if (preparedPlan != null) {
                            Button(
                                onClick = onRequestRestore,
                                enabled = !busy,
                                modifier = Modifier.testTag("request_full_backup_restore"),
                            ) {
                                Text("核对后执行恢复")
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("full_backup_case_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("备份概览", fontWeight = FontWeight.SemiBold)
                        Text("来源 App：${manifest.appVersion}")
                        Text("格式 / Schema：${manifest.formatVersion} / " +
                            manifest.databaseSchemaVersion)
                        Text("创建时间：${manifest.createdAt}")
                        Text("文件保护：${if (manifest.encrypted) "密码加密" else "未加密"}")
                        Text(
                            "命例 ${manifest.counts.cases} · 快照 ${manifest.counts.snapshots} · " +
                                "记录 ${manifest.counts.textRecords} · 事件 " +
                                "${manifest.counts.events} · 附件 ${manifest.counts.attachments}",
                        )
                        Text("已校验文件：${preview.sourceFileCount}")
                        if (conflictedCases.isEmpty()) {
                            Text("当前库未发现稳定 ID、出生输入或四柱冲突。")
                        } else {
                            Text(
                                "发现 ${conflictedCases.size} 个来源命例、" +
                                    "$conflictCandidateCount 个本地冲突候选。",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("full_backup_conflict_summary"),
                            )
                        }
                        OutlinedButton(
                            onClick = onSkipAll,
                            enabled = !busy && preview.cases.isNotEmpty(),
                            modifier = Modifier.testTag("skip_all_full_backup_cases"),
                        ) {
                            Text("明确将全部命例设为跳过")
                        }
                        Text(
                            "批量跳过只设置逐例决策，不会立即执行恢复。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Text("逐例恢复决策", fontWeight = FontWeight.SemiBold)
            }
            items(
                items = preview.cases,
                key = { it.sourceCaseId },
            ) { sourceCase ->
                val selected = decisions[sourceCase.sourceCaseId]?.action
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_backup_case_${sourceCase.sourceCaseId}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            sourceCase.sourceAlias +
                                (if (sourceCase.isTrashed) "（来源已删除）" else ""),
                            fontWeight = FontWeight.SemiBold,
                            modifier = if (sourceCase.conflicts.isNotEmpty()) {
                                Modifier.testTag(
                                    "full_backup_conflict_${sourceCase.sourceCaseId}",
                                )
                            } else {
                                Modifier
                            },
                        )
                        Text(
                            "来源 ID：${sourceCase.sourceCaseId}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "当前决策：${selected?.label ?: "尚未选择"}",
                            color = if (selected == null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        sourceCase.conflicts.forEach { conflict ->
                            Text(
                                "本地：${conflict.localAlias}" +
                                    (if (conflict.isTrashed) "（回收站）" else "") +
                                    "；${conflict.reasons.joinToString("、") { it.label }}" +
                                    "；修订 ${conflict.localRevision}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
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
                                modifier = Modifier
                                    .testTag("full_backup_skip_${sourceCase.sourceCaseId}")
                                    .semantics {
                                        contentDescription =
                                            "跳过完整备份来源 ${sourceCase.sourceCaseId}"
                                    },
                            ) { Text("跳过") }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "已验证文件保护、ZIP 路径、大小、哈希、数据引用、附件一致性和当前库" +
                            "冲突。只有底部“核对后执行恢复”会进入最终确认。",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
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
                    if (counts.attachmentReferences > 0) {
                        Text(
                            "记录或事件最多关联 ${counts.attachmentReferences} 个来源附件；" +
                                "提交时只复制实际新增内容引用的附件，并在多个模块间去重。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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
    val missingAttachmentBinaries =
        hasAttachmentReferences && !preview.containsAttachmentBinaries
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
                    if (missingAttachmentBinaries) {
                        "此 JSON 不包含图片二进制。该命例存在图片或字段证据，" +
                            "请改用命例附件包导入，以免证据引用失效。"
                    } else if (preview.containsAttachmentBinaries) {
                        "附件包已校验 ${preview.counts.attachmentReferences} 个图片附件的" +
                            "大小与 SHA-256；提交时会重新认证文件并使用附件事务。"
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
                                    !busy && !missingAttachmentBinaries && !conflict.isTrashed,
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
                enabled = !busy && !missingAttachmentBinaries,
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

private val EXPANDED_NAVIGATION_MIN_WIDTH = 840.dp

private data class RootNavigationAction(
    val label: String,
    val glyph: String,
    val selected: Boolean,
    val tag: String,
    val onClick: () -> Unit,
)

private fun rootNavigationActions(
    destination: AppDestination,
    onOpenChart: () -> Unit,
    onOpenCases: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
): List<RootNavigationAction> = listOf(
    RootNavigationAction(
        "排盘",
        "盘",
        destination == AppDestination.CreateCase,
        "nav_chart",
        onOpenChart,
    ),
    RootNavigationAction(
        "命例",
        "例",
        destination == AppDestination.CaseList ||
            destination is AppDestination.CaseDetail,
        "nav_cases",
        onOpenCases,
    ),
    RootNavigationAction(
        "记录",
        "记",
        destination == AppDestination.RecordHub,
        "nav_records",
        onOpenRecords,
    ),
    RootNavigationAction(
        "设置",
        "设",
        destination == AppDestination.Settings,
        "nav_settings",
        onOpenSettings,
    ),
)

@Composable
private fun RootNavigationBar(
    destination: AppDestination,
    onOpenChart: () -> Unit,
    onOpenCases: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val items = rootNavigationActions(
        destination = destination,
        onOpenChart = onOpenChart,
        onOpenCases = onOpenCases,
        onOpenRecords = onOpenRecords,
        onOpenSettings = onOpenSettings,
    )
    NavigationBar(modifier = Modifier.testTag("root_navigation")) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Text(item.glyph) },
                label = { Text(item.label) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 48.dp)
                    .semantics { contentDescription = item.label }
                    .testTag(item.tag),
            )
        }
    }
}

@Composable
private fun RootNavigationRail(
    destination: AppDestination,
    onOpenChart: () -> Unit,
    onOpenCases: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = rootNavigationActions(
        destination = destination,
        onOpenChart = onOpenChart,
        onOpenCases = onOpenCases,
        onOpenRecords = onOpenRecords,
        onOpenSettings = onOpenSettings,
    )
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .testTag("root_navigation_rail"),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Text(item.glyph) },
                label = { Text(item.label) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 48.dp)
                    .semantics { contentDescription = item.label }
                    .testTag(item.tag),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedCaseIndexPane(
    cases: List<CaseSummary>,
    selectedCaseId: String?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenCase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("expanded_case_index_pane"),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("命例索引", fontWeight = FontWeight.SemiBold)
                    Text(
                        "保持详情上下文，快速切换命例",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("刷新")
                }
            },
        )
        when {
            loading -> LoadingBox("正在读取命例…")
            error != null -> ErrorBox(error, "重试", onRefresh)
            cases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无命例")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cases, key = CaseSummary::id) { summary ->
                    Card(
                        colors = if (summary.id == selectedCaseId) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        } else {
                            CardDefaults.cardColors()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCase(summary.id) }
                            .testTag("expanded_case_${summary.id}"),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(summary.alias, fontWeight = FontWeight.SemiBold)
                            Text(
                                summary.fourPillars?.display() ?: "暂无已采用排盘",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordHubScreen(
    cases: List<CaseSummary>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenCase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("record_hub_screen"),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("记录", fontWeight = FontWeight.SemiBold)
                    Text(
                        "按命例进入反馈、点评、事件与版本历史",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("刷新")
                }
            },
        )
        when {
            loading -> LoadingBox("正在读取记录索引…")
            error != null -> ErrorBox(error, "重试", onRefresh)
            cases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无命例记录，请先从“排盘”创建或从“命例”导入。")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(cases, key = CaseSummary::id) { summary ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCase(summary.id) }
                            .testTag("record_case_${summary.id}"),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(summary.alias, fontWeight = FontWeight.SemiBold)
                            Text(
                                summary.fourPillars?.display() ?: "暂无已采用排盘",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "打开命例查看完整记录与事件",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHomeScreen(
    state: StageTwoUiState,
    screenshotImportState: ScreenshotImportUiState,
    onImportScreenshots: () -> Unit,
    onImportSingleCase: () -> Unit,
    onExportFullBackup: () -> Unit,
    onRestoreFullBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_home_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("数据只保存在本机；问真截图识别不需要联网。")
        Button(
            onClick = onImportScreenshots,
            enabled = !screenshotImportState.busy,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("settings_import_screenshots"),
        ) {
            Text("导入问真截图")
        }
        OutlinedButton(
            onClick = onImportSingleCase,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("settings_import_case"),
        ) {
            Text("导入单命例文件")
        }
        OutlinedButton(
            onClick = onExportFullBackup,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("settings_export_backup"),
        ) {
            Text("导出完整备份")
        }
        OutlinedButton(
            onClick = onRestoreFullBackup,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("settings_restore_backup"),
        ) {
            Text("预览并恢复完整备份")
        }
        OutlinedButton(
            onClick = {
                val diagnostics = buildAppDiagnosticText(
                    state = state,
                    screenshot = screenshotImportState,
                    appVersion = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                )
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("南枫八字诊断包", diagnostics))
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("settings_copy_diagnostics"),
        ) {
            Text("复制脱敏诊断包")
        }
        Text(
            "诊断包不包含姓名、出生资料、截图文字、文件路径、密码或附件内容。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text("版本：${BuildConfig.VERSION_NAME}")
        Text(
            "发布前仍需正式签名、真实问真样本验收和用户授权的真机数据保留安装。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    onImportScreenshots: () -> Unit,
    screenshotImportState: ScreenshotImportUiState,
    onRetryScreenshotImport: () -> Unit,
    onDeleteScreenshotImport: () -> Unit,
    onReviewScreenshotImport: () -> Unit,
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
                            .heightIn(min = 48.dp)
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
                onClick = onImportScreenshots,
                enabled = !screenshotImportState.busy,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("import_screenshots_button"),
            ) {
                Text(if (screenshotImportState.busy) "识别中…" else "截图建档")
            }
            OutlinedButton(
                onClick = onImportSingleCase,
                enabled = !state.singleCaseExchangeBusy && !state.fullBackupBusy,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("import_single_case_button"),
            ) {
                Text(if (state.singleCaseExchangeBusy) "读取中…" else "导入单命例")
            }
            OutlinedButton(
                onClick = onExportFullBackup,
                enabled = !state.fullBackupBusy && !state.singleCaseExchangeBusy,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("export_full_backup_button"),
            ) {
                Text(if (state.fullBackupBusy) "处理中…" else "导出完整备份")
            }
            OutlinedButton(
                onClick = onPreviewFullBackup,
                enabled = !state.fullBackupBusy && !state.singleCaseExchangeBusy,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("preview_full_backup_button"),
            ) {
                Text("检查完整备份")
            }
        }
        ScreenshotImportSummary(
            state = screenshotImportState,
            onRetry = onRetryScreenshotImport,
            onDelete = onDeleteScreenshotImport,
            onReview = onReviewScreenshotImport,
        )
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
private fun ScreenshotImportSummary(
    state: ScreenshotImportUiState,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onReview: () -> Unit,
) {
    if (
        !state.busy &&
        !state.needsReview &&
        !state.canRetry &&
        state.recoverableSessionCount == 0
    ) {
        return
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("screenshot_import_summary"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = when {
                    state.busy -> "截图正在本机处理"
                    state.needsReview -> "截图识别结果待核对"
                    state.canRetry -> "截图识别可重试"
                    else -> "存在未完成的截图导入"
                },
                fontWeight = FontWeight.SemiBold,
            )
            state.progressText?.let { Text(it) }
            val facts = buildList {
                if (state.completedImageCount > 0) {
                    add("原图 ${state.completedImageCount} 张")
                }
                if (state.classifiedPageTypes.isNotEmpty()) {
                    add(
                        state.classifiedPageTypes
                            .groupingBy(WenzhenPageType::displayName)
                            .eachCount()
                            .entries
                            .joinToString("、") { (name, count) -> "$name $count 张" },
                    )
                }
                if (state.exactDuplicatePairCount > 0) {
                    add("完全重复 ${state.exactDuplicatePairCount} 对")
                }
                if (state.similarDuplicatePairCount > 0) {
                    add("疑似相似 ${state.similarDuplicatePairCount} 对")
                }
                if (state.failedImageCount > 0) {
                    add("待重试 ${state.failedImageCount} 张")
                }
                if (state.caseCandidateCount > 0) {
                    add("待核对候选 ${state.caseCandidateCount} 个")
                }
                if (state.multiImageCandidateCount > 0) {
                    add("多图归组 ${state.multiImageCandidateCount} 个")
                }
                if (state.extractedFieldCount > 0) {
                    add("待核对字段 ${state.extractedFieldCount} 项")
                }
                if (state.extractedLongTextCount > 0) {
                    add("完整原文 ${state.extractedLongTextCount} 段")
                }
                if (state.recoverableSessionCount > 0) {
                    add("可恢复 ${state.recoverableSessionCount} 个")
                }
            }
            if (facts.isNotEmpty()) Text(facts.joinToString(" · "))
            if (state.needsReview) {
                Text("当前结果只保存在导入会话中，尚未写入正式命例。")
                if (state.reviewCandidates.isNotEmpty()) {
                    Button(
                        onClick = onReview,
                        modifier = Modifier.testTag("review_screenshot_import_button"),
                    ) {
                        Text("逐项核对")
                    }
                }
            }
            if (state.canRetry && !state.busy) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_screenshot_import_button"),
                ) {
                    Text("复用原图重试")
                }
            }
            if (state.activeSessionId != null && !state.busy) {
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.testTag("delete_screenshot_import_button"),
                ) {
                    Text("删除本次导入")
                }
            }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除本次截图导入？") },
            text = { Text("导入会话、识别结果和已私有复制的原图都会删除，正式命例不会受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_screenshot_import"),
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenshotImportReviewScreen(
    state: ScreenshotImportUiState,
    onBack: () -> Unit,
    onSetFieldAdopted: (String, String, Boolean) -> Unit,
    onUpdateFieldValue: (String, String, String) -> Unit,
    onSetLongTextAdopted: (String, String, Boolean) -> Unit,
    onSetCandidateAdopted: (String, Boolean) -> Unit,
    onCommitCandidate: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var correctionDrafts by rememberSaveable {
        mutableStateOf(emptyMap<String, String>())
    }
    var previewFieldId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screenshot_import_review_screen"),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("核对问真导入", fontWeight = FontWeight.SemiBold)
                    Text(
                        "确认前不会写入正式命例",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("返回")
                }
            },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("screenshot_review_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.reviewCandidates, key = ScreenshotCandidateReviewUi::id) { candidate ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("screenshot_candidate_${candidate.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(candidate.alias, fontWeight = FontWeight.SemiBold)
                        Text(
                            "来源图片 ${candidate.imageCount} 张 · " +
                                "字段 ${candidate.fields.size} 项 · " +
                                "原文 ${candidate.longTexts.size} 段",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (candidate.blockingIssues.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("candidate_blocking_issues_${candidate.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        "候选问题摘要",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    candidate.blockingIssues.forEach { issue ->
                                        Text(
                                            "• $issue",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                        }
                        if (candidate.reviewWarnings.isNotEmpty()) {
                            Text(
                                candidate.reviewWarnings.joinToString(
                                    separator = "\n",
                                    transform = { "提醒：$it" },
                                ),
                                modifier = Modifier.testTag(
                                    "candidate_review_warnings_${candidate.id}",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onSetCandidateAdopted(candidate.id, !candidate.fullyAdopted)
                            },
                            modifier = Modifier.testTag(
                                "adopt_screenshot_candidate_${candidate.id}",
                            ),
                        ) {
                            Text(if (candidate.fullyAdopted) "撤销本候选采用" else "采用本候选全部内容")
                        }
                        Button(
                            onClick = { onCommitCandidate(candidate.id, false) },
                            enabled = candidate.readyToCommit &&
                                candidate.targetCaseId == null &&
                                state.committingCandidateId == null,
                            modifier = Modifier.testTag(
                                "commit_screenshot_candidate_${candidate.id}",
                            ),
                        ) {
                            Text(
                                when {
                                    candidate.targetCaseId != null -> "已写入正式命例"
                                    state.committingCandidateId == candidate.id -> "正在复算并写入…"
                                    !candidate.readyToCommit -> "必填字段尚未齐全"
                                    else -> "复算一致后写入正式命例"
                                },
                            )
                        }
                        if (state.pendingDuplicateCandidateId == candidate.id) {
                            DuplicateCandidatesCard(
                                candidates = state.duplicateCaseCandidates,
                                saving = state.committingCandidateId != null,
                                onConfirm = {
                                    onCommitCandidate(candidate.id, true)
                                },
                            )
                        }
                        if (candidate.missingRequiredFields.isNotEmpty()) {
                            Text(
                                "还需确认：${candidate.missingRequiredFields.joinToString("、")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            "提交时会用采用的生日和时柱复算；四柱不一致将自动拦截。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        candidate.fields.forEach { field ->
                            val correctedValue =
                                correctionDrafts[field.id] ?: field.normalizedValue.orEmpty()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(field.label, fontWeight = FontWeight.Medium)
                                        Switch(
                                            checked = field.adoptedValue != null,
                                            onCheckedChange = { adopted ->
                                                onSetFieldAdopted(
                                                    candidate.id,
                                                    field.id,
                                                    adopted,
                                                )
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .semantics {
                                                    contentDescription =
                                                        "采用截图字段：${field.label}"
                                                }
                                                .testTag(
                                                    "adopt_screenshot_field_${field.id}",
                                                ),
                                        )
                                    }
                                    Text("来源值：${field.sourceValue}")
                                    Text("规范值：${field.normalizedValue ?: "未识别"}")
                                    Text(
                                        "证据：${field.sourceImageName}" +
                                            (field.evidenceRegion?.let { " · 区域 $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (
                                        field.sourceImageRelativePath.isNotBlank() &&
                                        field.evidenceBox != null
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                previewFieldId = if (previewFieldId == field.id) {
                                                    null
                                                } else {
                                                    field.id
                                                }
                                            },
                                            modifier = Modifier.testTag(
                                                "preview_screenshot_field_${field.id}",
                                            ),
                                        ) {
                                            Text(
                                                if (previewFieldId == field.id) {
                                                    "收起原图定位"
                                                } else {
                                                    "在原图中定位"
                                                },
                                            )
                                        }
                                        if (previewFieldId == field.id) {
                                            ScreenshotEvidencePreview(
                                                field = field,
                                                modifier = Modifier.testTag(
                                                    "screenshot_field_preview_${field.id}",
                                                ),
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = correctedValue,
                                        onValueChange = { updatedValue ->
                                            correctionDrafts =
                                                correctionDrafts + (field.id to updatedValue)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("edit_screenshot_field_${field.id}"),
                                        label = {
                                            Text(if (field.userEdited) "人工修正值（已修改）" else "人工修正值")
                                        },
                                        supportingText = {
                                            Text("保存修正后会撤销该字段的采用状态，需重新确认。")
                                        },
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateFieldValue(
                                                candidate.id,
                                                field.id,
                                                correctedValue,
                                            )
                                        },
                                        enabled = correctedValue.trim() != field.normalizedValue,
                                        modifier = Modifier.testTag(
                                            "save_screenshot_field_${field.id}",
                                        ),
                                    ) {
                                        Text("保存修正")
                                    }
                                    Text("计算值：${field.calculationValue}")
                                    Text("采用值：${field.adoptedValue ?: "未采用"}")
                                    field.confidencePercent?.let {
                                        Text(
                                            "最低置信度：$it%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        candidate.longTexts.forEach { longText ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(longText.label, fontWeight = FontWeight.Medium)
                                        Switch(
                                            checked = longText.adopted,
                                            onCheckedChange = { adopted ->
                                                onSetLongTextAdopted(
                                                    candidate.id,
                                                    longText.id,
                                                    adopted,
                                                )
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .semantics {
                                                    contentDescription =
                                                        "采用截图原文：${longText.label}"
                                                }
                                                .testTag(
                                                    "adopt_screenshot_text_${longText.id}",
                                                ),
                                        )
                                    }
                                    Text(longText.rawText)
                                    Text(
                                        "来源：${longText.sourceImageName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        if (longText.adopted) "采用状态：已确认" else "采用状态：未采用",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotEvidencePreview(
    field: ScreenshotFieldReviewUi,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(field.sourceImageRelativePath) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(field.sourceImageRelativePath) {
        bitmap = withContext(Dispatchers.IO) {
            decodeImportPreview(
                filesDir = context.filesDir,
                relativePath = field.sourceImageRelativePath,
            )
        }
    }
    val preview = bitmap
    if (preview == null) {
        Text(
            "原图预览暂不可用，文件名与边界框坐标仍已保留。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        Image(
            bitmap = preview.asImageBitmap(),
            contentDescription = "${field.sourceImageName} 的字段原图定位",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        val evidence = field.evidenceBox
        val originalWidth = field.sourceImageWidthPx
        val originalHeight = field.sourceImageHeightPx
        if (evidence != null && originalWidth != null && originalHeight != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = minOf(
                    size.width / originalWidth.toFloat(),
                    size.height / originalHeight.toFloat(),
                )
                val offsetX = (size.width - originalWidth * scale) / 2f
                val offsetY = (size.height - originalHeight * scale) / 2f
                drawRect(
                    color = Color(0xFFFF7A00),
                    topLeft = Offset(
                        offsetX + evidence.left * scale,
                        offsetY + evidence.top * scale,
                    ),
                    size = Size(
                        (evidence.right - evidence.left) * scale,
                        (evidence.bottom - evidence.top) * scale,
                    ),
                    style = Stroke(width = 5f),
                )
            }
        }
    }
}

private fun decodeImportPreview(
    filesDir: File,
    relativePath: String,
): Bitmap? {
    if (relativePath.isBlank()) return null
    val root = File(filesDir, "import-images").canonicalFile
    val source = File(root, relativePath).canonicalFile
    if (!source.path.startsWith(root.path + File.separator) || !source.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1200) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        source.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private fun WenzhenPageType.displayName(): String = when (this) {
    WenzhenPageType.HOME_INPUT -> "首页排盘"
    WenzhenPageType.USER_LIST -> "用户列表"
    WenzhenPageType.BASIC_INFO -> "基本信息"
    WenzhenPageType.BASIC_CHART -> "基本排盘"
    WenzhenPageType.PROFESSIONAL_CHART -> "专业细盘"
    WenzhenPageType.COMMENTARY -> "师傅点评"
    WenzhenPageType.FEEDBACK -> "命主反馈"
    WenzhenPageType.UNKNOWN -> "待识别"
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
    val modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag(tag)
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
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
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .heightIn(min = 48.dp),
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
                    summary.birthInput.displayDateTime() +
                    summary.zodiac?.let { " · 生肖$it" }.orEmpty(),
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
    onOpenCase: (String) -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onPreview: () -> Unit,
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
        previewing = state.previewing,
        instantCalculation = state.instantCalculation,
        submitLabel = "排盘并保存",
        onBack = onBack,
        onFormChange = onFormChange,
        onPreview = onPreview,
        onSubmit = onSubmit,
        duplicateCandidates = state.duplicateCandidates,
        onConfirmDuplicate = onConfirmDuplicate,
        topContent = {
            RecentCasesSection(
                cases = state.recentCases,
                onOpenCase = onOpenCase,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun RecentCasesSection(
    cases: List<CaseSummary>,
    onOpenCase: (String) -> Unit,
) {
    SectionHeading(
        "最近命例",
        if (cases.isEmpty()) {
            "打开过的命例会出现在这里，方便继续研究。"
        } else {
            "按最近查看时间显示，可直接返回命盘详情。"
        },
    )
    if (cases.isEmpty()) {
        Text(
            "暂无最近查看记录",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("recent_cases_empty"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        cases.forEach { summary ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onOpenCase(summary.id) }
                    .semantics {
                        contentDescription = "打开最近命例：${summary.alias}"
                    }
                    .testTag("recent_case_${summary.id}"),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        summary.name.value ?: summary.alias,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${summary.sexForFortuneDirection.displayName()} · " +
                            summary.birthInput.displayDateTime(),
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "四柱：${summary.fourPillars?.display() ?: "暂无计算结果"}",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseFormScreen(
    title: String,
    screenTag: String,
    form: CaseFormState,
    error: String?,
    saving: Boolean,
    modifier: Modifier = Modifier,
    previewing: Boolean = false,
    instantCalculation: CalculationResult? = null,
    submitLabel: String,
    onBack: () -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onPreview: (() -> Unit)? = null,
    onSubmit: () -> Unit,
    showIdentityFields: Boolean = true,
    candidateLabel: String = "",
    onCandidateLabelChange: (String) -> Unit = {},
    sexEditable: Boolean = true,
    duplicateCandidates: List<DuplicateCaseCandidate> = emptyList(),
    onConfirmDuplicate: (() -> Unit)? = null,
    topContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(screenTag),
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
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
            topContent?.invoke()
            if (showIdentityFields) {
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
            } else {
                SectionHeading(
                    "候选说明",
                    "同一命例可保存多个出生时间；新增候选不会自动改变当前采用盘。",
                )
                OutlinedTextField(
                    value = candidateLabel,
                    onValueChange = onCandidateLabelChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("birth_time_candidate_label"),
                    label = { Text("候选名称 *") },
                    supportingText = { Text("例如：问真原记录、家人回忆 11:50") },
                    singleLine = true,
                    enabled = !saving,
                )
            }
            Text(
                "性别 *",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SexButton(
                    text = "男",
                    selected = form.sex == SexForFortuneDirection.MAN,
                    enabled = !saving && sexEditable,
                    tag = "sex_man",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.MAN) }
                    },
                )
                SexButton(
                    text = "女",
                    selected = form.sex == SexForFortuneDirection.WOMAN,
                    enabled = !saving && sexEditable,
                    tag = "sex_woman",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.WOMAN) }
                    },
                )
            }

            SectionHeading(
                "出生时间",
                "支持公历与农历（含闰月），按所选 IANA 时区解析原始民用时。",
            )
            Text(
                "历法 *",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SexButton(
                    text = "公历",
                    selected = form.calendarSystem == CalendarSystem.SOLAR,
                    enabled = !saving,
                    tag = "birth_calendar_solar",
                    onClick = {
                        onFormChange {
                            it.copy(
                                calendarSystem = CalendarSystem.SOLAR,
                                isLeapMonth = false,
                            ).clearTimeZoneResolution()
                        }
                    },
                )
                SexButton(
                    text = "农历",
                    selected = form.calendarSystem == CalendarSystem.LUNAR,
                    enabled = !saving,
                    tag = "birth_calendar_lunar",
                    onClick = {
                        onFormChange {
                            it.copy(calendarSystem = CalendarSystem.LUNAR)
                                .clearTimeZoneResolution()
                        }
                    },
                )
                if (form.calendarSystem == CalendarSystem.LUNAR) {
                    SexButton(
                        text = "闰月",
                        selected = form.isLeapMonth,
                        enabled = !saving,
                        tag = "birth_lunar_leap_month",
                        onClick = {
                            onFormChange {
                                it.copy(isLeapMonth = !it.isLeapMonth)
                                    .clearTimeZoneResolution()
                            }
                        },
                    )
                }
            }
            NumericFieldRow(
                values = listOf(
                    NumericField("年", form.year, "birth_year") {
                        onFormChange { form ->
                            form.copy(year = it).clearTimeZoneResolution()
                        }
                    },
                    NumericField("月", form.month, "birth_month") {
                        onFormChange { form ->
                            form.copy(month = it).clearTimeZoneResolution()
                        }
                    },
                    NumericField("日", form.day, "birth_day") {
                        onFormChange { form ->
                            form.copy(day = it).clearTimeZoneResolution()
                        }
                    },
                ),
                enabled = !saving,
            )
            NumericFieldRow(
                values = listOf(
                    NumericField("时", form.hour, "birth_hour") {
                        onFormChange { form ->
                            form.copy(hour = it).clearTimeZoneResolution()
                        }
                    },
                    NumericField("分", form.minute, "birth_minute") {
                        onFormChange { form ->
                            form.copy(minute = it).clearTimeZoneResolution()
                        }
                    },
                    NumericField("秒", form.second, "birth_second") {
                        onFormChange { form ->
                            form.copy(second = it).clearTimeZoneResolution()
                        }
                    },
                ),
                enabled = !saving,
            )
            Text(
                "时间精度 *",
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimePrecision.entries.forEach { precision ->
                    SexButton(
                        text = precision.displayName(),
                        selected = form.timePrecision == precision,
                        enabled = !saving,
                        tag = "birth_time_precision_${precision.name}",
                        onClick = {
                            onFormChange { current ->
                                current.copy(
                                    timePrecision = precision,
                                    minute = when (precision) {
                                        TimePrecision.HOUR_ONLY,
                                        TimePrecision.DOUBLE_HOUR_ONLY,
                                        TimePrecision.UNKNOWN,
                                        -> "0"
                                        else -> current.minute
                                    },
                                    second = when (precision) {
                                        TimePrecision.EXACT_TO_SECOND -> current.second
                                        else -> "0"
                                    },
                                ).clearTimeZoneResolution()
                            }
                        },
                    )
                }
            }
            Text(
                "子时换日规则 *",
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RatHourRule.entries.forEach { rule ->
                    SexButton(
                        text = rule.displayName(),
                        selected = form.ratHourRule == rule,
                        enabled = !saving,
                        tag = "birth_rat_hour_rule_${rule.name}",
                        onClick = {
                            onFormChange { it.copy(ratHourRule = rule) }
                        },
                    )
                }
            }
            Text(
                "仅 23:00–23:59 的日柱会因口径不同而变化；所选规则随计算快照留存。",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "时间来源",
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeSourceType.entries.forEach { source ->
                    SexButton(
                        text = source.displayName(),
                        selected = form.timeSourceType == source,
                        enabled = !saving,
                        tag = "birth_time_source_${source.name}",
                        onClick = {
                            onFormChange { it.copy(timeSourceType = source) }
                        },
                    )
                }
            }
            OutlinedTextField(
                value = form.sourceNote,
                onValueChange = { value ->
                    onFormChange { it.copy(sourceNote = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("birth_time_source_note"),
                label = { Text("时间来源说明（可选）") },
                supportingText = { Text("可记录出生证、家人回忆或截图出处，不要填写账号密码。") },
                minLines = 2,
                enabled = !saving,
            )
            SectionHeading(
                "出生地区与时区",
                "地区必填；经纬度可稍后补录。时区使用 IANA 标识，例如 Asia/Shanghai。",
            )
            OutlinedTextField(
                value = form.locationName,
                onValueChange = { value ->
                    onFormChange { it.copy(locationName = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("birth_location"),
                label = { Text("出生地区 *") },
                singleLine = true,
                enabled = !saving,
            )
            OutlinedTextField(
                value = form.timeZoneId,
                onValueChange = { value ->
                    onFormChange {
                        it.copy(timeZoneId = value).clearTimeZoneResolution()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("birth_time_zone"),
                label = { Text("IANA 时区 *") },
                supportingText = { Text("中国大陆通常为 Asia/Shanghai") },
                singleLine = true,
                enabled = !saving,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = form.longitude,
                    onValueChange = { value ->
                        onFormChange { it.copy(longitude = value) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("birth_longitude"),
                    label = {
                        Text(if (form.useTrueSolarTime) "经度 *" else "经度（可选）")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !saving,
                )
                OutlinedTextField(
                    value = form.latitude,
                    onValueChange = { value ->
                        onFormChange { it.copy(latitude = value) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("birth_latitude"),
                    label = {
                        Text(if (form.useTrueSolarTime) "纬度 *" else "纬度（可选）")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !saving,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "启用真太阳时",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "使用经纬度、历史时区与天文均时差校正；年/月暂按原始民用时，" +
                            "日/时按校正后当地时间，边界口径待问真样本验收。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = form.useTrueSolarTime,
                    onCheckedChange = { checked ->
                        onFormChange { it.copy(useTrueSolarTime = checked) }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "启用真太阳时" }
                        .testTag("birth_true_solar_time"),
                    enabled = !saving,
                )
            }
            if (form.availableUtcOffsetSeconds.isNotEmpty()) {
                Text(
                    "该当地时间出现两次，请根据原始记录选择 UTC offset：",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    form.availableUtcOffsetSeconds.forEach { offsetSeconds ->
                        SexButton(
                            text = formatUtcOffset(offsetSeconds),
                            selected = form.resolvedUtcOffsetSeconds == offsetSeconds,
                            enabled = !saving,
                            tag = "birth_utc_offset_$offsetSeconds",
                            onClick = {
                                onFormChange {
                                    it.copy(resolvedUtcOffsetSeconds = offsetSeconds)
                                }
                            },
                        )
                    }
                }
            }
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
            if (instantCalculation != null) {
                InstantCalculationPreviewCard(
                    calculation = instantCalculation,
                    sex = form.sex ?: instantCalculation.normalizedInput.sexForFortuneDirection,
                )
            }
            if (duplicateCandidates.isNotEmpty() && onConfirmDuplicate != null) {
                DuplicateCandidatesCard(
                    candidates = duplicateCandidates,
                    saving = saving || previewing,
                    onConfirm = onConfirmDuplicate,
                )
            }
            if (onPreview != null) {
                OutlinedButton(
                    onClick = onPreview,
                    enabled = !saving && !previewing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(52.dp)
                        .testTag("preview_case"),
                ) {
                    if (previewing) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(22.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("正在即时排盘…")
                    } else {
                        Text("即时排盘（不保存）")
                    }
                }
            }
            Button(
                onClick = onSubmit,
                enabled = !saving && !previewing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (onPreview == null) 20.dp else 10.dp,
                        bottom = 28.dp,
                    )
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
private fun InstantCalculationPreviewCard(
    calculation: CalculationResult,
    sex: SexForFortuneDirection,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("instant_calculation_preview"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "即时排盘结果（未保存）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "修改任一输入会清除此结果；只有点击“排盘并保存”才会写入本机命例库。",
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DetailRow("四柱", calculation.fourPillars.display())
            calculation.basicChartDetails?.let {
                BasicChartDetailsView(details = it, sex = sex)
            }
            calculation.calendarConversion?.let { conversion ->
                DetailRow("换算公历", conversion.solarDateTime.display())
                val lunar = conversion.lunarDateTime
                DetailRow(
                    "换算农历",
                    "${lunar.year}年${if (lunar.isLeapMonth) "闰" else ""}" +
                        "${lunar.month}月${lunar.day}日 " +
                        "%02d:%02d:%02d".format(
                            lunar.hour,
                            lunar.minute,
                            lunar.second,
                        ),
                )
            }
            calculation.trueSolarTimeEvidence?.let {
                DetailRow("真太阳时", it.trueSolarDateTime.display())
            }
            DetailRow(
                "时间精度",
                calculation.normalizedInput.timePrecision.displayName(),
            )
            DetailRow(
                "时间来源",
                calculation.normalizedInput.timeSourceType.displayName(),
            )
            DetailRow(
                "子时规则",
                calculation.profile.ratHourRule.displayName(),
                tag = "instant_rat_hour_rule_${calculation.profile.ratHourRule.name}",
            )
            DetailRow(
                "计算配置",
                calculation.profile.id,
                tag = "instant_calculation_profile_${calculation.profile.id}",
            )
            calculation.normalizedInput.sourceNote?.let {
                DetailRow("时间来源说明", it)
            }
            DetailRow("胎元", calculation.fetalOrigin)
            DetailRow("胎息", calculation.fetalBreath)
            DetailRow("命宫", calculation.ownSign)
            DetailRow("身宫", calculation.bodySign)
            DetailRow(
                "起运方向",
                if (calculation.fortuneStart.direction.name == "FORWARD") "顺排" else "逆排",
            )
            DetailRow(
                "起运年龄",
                "${calculation.fortuneStart.years} 年 " +
                    "${calculation.fortuneStart.months} 月 " +
                    "${calculation.fortuneStart.days} 日 " +
                    "${calculation.fortuneStart.hours} 时 " +
                    "${calculation.fortuneStart.minutes} 分",
            )
            DetailRow("精确交运时间", calculation.fortuneStart.endAt.display())
            DecadeFortuneDetailsView(calculation.decadeFortunes)
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

private val EVENT_EVIDENCE_FIELD_PATTERN = Regex(
    "event\\.candidate\\.((?:19|20)\\d{2})\\.\\d+",
)
private val CHART_EVIDENCE_FIELD_PATTERN = Regex(
    "chart\\.(year|month|day|hour)\\." +
        "(main_star|hidden_stems|secondary_stars|fortune_stage|" +
        "self_stage|void|nayin|spirits)",
)
private val CHART_EVIDENCE_COLUMN_LABELS = mapOf(
    "year" to "年柱",
    "month" to "月柱",
    "day" to "日柱",
    "hour" to "时柱",
)
private val CHART_EVIDENCE_ROW_LABELS = mapOf(
    "main_star" to "主星",
    "hidden_stems" to "藏干",
    "secondary_stars" to "副星",
    "fortune_stage" to "星运",
    "self_stage" to "自坐",
    "void" to "空亡",
    "nayin" to "纳音",
    "spirits" to "神煞",
)

private fun String.evidenceFieldLabel(): String = when (this) {
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
    "birth.previous_jie" -> "前一节"
    "birth.next_jie" -> "后一节"
    "chart.four_pillars" -> "四柱"
    "chart.fetal_origin" -> "胎元"
    "chart.fetal_breath" -> "胎息"
    "chart.own_sign" -> "命宫"
    "chart.body_sign" -> "身宫"
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
    else -> CHART_EVIDENCE_FIELD_PATTERN.matchEntire(this)
        ?.let { match ->
            "${CHART_EVIDENCE_COLUMN_LABELS.getValue(match.groupValues[1])} · " +
                CHART_EVIDENCE_ROW_LABELS.getValue(match.groupValues[2])
        }
        ?: EVENT_EVIDENCE_FIELD_PATTERN.matchEntire(this)
            ?.groupValues
            ?.get(1)
            ?.let { "关键事件候选 · ${it}年" }
        ?: this
}

private fun TypedFieldValue.evidenceDisplayValue(): String = when (this) {
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

    is TypedFieldValue.FourPillarsValue ->
        "${value.year} ${value.month} ${value.day} ${value.hour}"
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
    val modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag(tag)
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
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
    onAddBirthTimeCandidate: () -> Unit,
    onAdoptBirthTimeCandidate: (String) -> Unit,
    onEditMetadata: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    onSelectSection: (CaseDetailSection) -> Unit,
    onFortuneObservationDateChange: (String) -> Unit,
    onFortuneObservationTimeChange: (String) -> Unit,
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
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("返回")
                }
            },
        )
        if (state.detail != null && !state.detailLoading && state.detailError == null) {
            CaseDetailTabs(
                selectedSection = state.detailSection,
                onSelectSection = onSelectSection,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
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
                    onAddBirthTimeCandidate = onAddBirthTimeCandidate,
                    onAdoptBirthTimeCandidate = onAdoptBirthTimeCandidate,
                    onEditMetadata = onEditMetadata,
                    onAddRecord = onAddRecord,
                    onEditRecord = onEditRecord,
                    onAddEvent = onAddEvent,
                    onEditEvent = onEditEvent,
                    onDuplicate = onDuplicate,
                    onExportSingleCase = onExportSingleCase,
                    onMoveToTrash = onMoveToTrash,
                    onRestore = onRestore,
                    selectedSection = state.detailSection,
                    singleCaseExchangeBusy = state.singleCaseExchangeBusy,
                    mutationSaving = state.mutationSaving,
                    mutationError = state.mutationError,
                    fortuneObservationDate = state.fortuneObservationDate,
                    fortuneObservationTime = state.fortuneObservationTime,
                    fortunePosition = state.fortunePosition,
                    professionalFortunePosition = state.professionalFortunePosition,
                    fortunePositionError = state.fortunePositionError,
                    onFortuneObservationDateChange = onFortuneObservationDateChange,
                    onFortuneObservationTimeChange = onFortuneObservationTimeChange,
                )
            }
        }
    }
}

private fun CaseDetailSection.displayName(): String = when (this) {
    CaseDetailSection.BASIC_INFO -> "基本信息"
    CaseDetailSection.BASIC_CHART -> "基本排盘"
    CaseDetailSection.FORTUNE -> "岁运"
    CaseDetailSection.RECORDS -> "分析记录"
}

private fun CaseDetailSection.testTag(): String = when (this) {
    CaseDetailSection.BASIC_INFO -> "detail_tab_basic_info"
    CaseDetailSection.BASIC_CHART -> "detail_tab_basic_chart"
    CaseDetailSection.FORTUNE -> "detail_tab_fortune"
    CaseDetailSection.RECORDS -> "detail_tab_records"
}

@Composable
private fun CaseDetailTabs(
    selectedSection: CaseDetailSection,
    onSelectSection: (CaseDetailSection) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = CaseDetailSection.entries.indexOf(selectedSection),
        edgePadding = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("case_detail_tabs"),
    ) {
        CaseDetailSection.entries.forEach { section ->
            Tab(
                selected = selectedSection == section,
                onClick = { onSelectSection(section) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(section.testTag()),
                text = { Text(section.displayName()) },
            )
        }
    }
}

@Composable
private fun CaseDetailContent(
    case: BaziCase,
    onEditCase: () -> Unit,
    onAddBirthTimeCandidate: () -> Unit,
    onAdoptBirthTimeCandidate: (String) -> Unit,
    onEditMetadata: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    selectedSection: CaseDetailSection,
    singleCaseExchangeBusy: Boolean,
    mutationSaving: Boolean,
    mutationError: String?,
    fortuneObservationDate: String,
    fortuneObservationTime: String,
    fortunePosition: FortunePosition?,
    professionalFortunePosition: ProfessionalFortunePosition?,
    fortunePositionError: String?,
    onFortuneObservationDateChange: (String) -> Unit,
    onFortuneObservationTimeChange: (String) -> Unit,
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
                        .heightIn(min = 48.dp)
                        .testTag("edit_case_button"),
                ) {
                    Text("编辑资料")
                }
                OutlinedButton(
                    onClick = onEditMetadata,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
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
                        .heightIn(min = 48.dp)
                        .testTag("duplicate_case_button"),
                ) {
                    Text("复制命例")
                }
                OutlinedButton(
                    onClick = onMoveToTrash,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
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
                    .heightIn(min = 48.dp)
                    .testTag("export_single_case_button"),
            ) {
                Text(if (singleCaseExchangeBusy) "正在导出…" else "导出单命例")
            }
        } else {
            Button(
                onClick = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .heightIn(min = 48.dp)
                    .testTag("restore_case_button"),
            ) {
                Text("恢复命例")
            }
        }
        if (selectedSection == CaseDetailSection.BASIC_INFO) {
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
            DetailSection("出生时间候选") {
            if (case.deletedAt == null) {
                OutlinedButton(
                    onClick = onAddBirthTimeCandidate,
                    enabled = !mutationSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_birth_time_candidate_button"),
                ) {
                    Text("新增时间候选")
                }
            }
            if (case.birthTimeCandidates.isEmpty()) {
                Text(
                    "旧版命例暂无候选记录；下次重新排盘或添加候选时会建立证据链。",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                case.birthTimeCandidates.forEach { candidate ->
                    val snapshot = case.calculationSnapshots.firstOrNull {
                        it.id == candidate.calculationSnapshotId
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag(
                                if (candidate.adopted) {
                                    "adopted_birth_time_candidate_${candidate.label}"
                                } else {
                                    "alternate_birth_time_candidate_${candidate.label}"
                                },
                            )
                            .semantics {
                                contentDescription =
                                    "出生时间候选：${candidate.label}；" +
                                        if (candidate.adopted) "当前采用" else "备选"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (candidate.adopted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (candidate.adopted) {
                                        "当前采用：${candidate.label}"
                                    } else {
                                        candidate.label
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (candidate.adopted) "已采用" else "备选",
                                    color = if (candidate.adopted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Text(
                                "候选时间：${candidate.birthInput.displayDateTime()}",
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                "${candidate.birthInput.timePrecision.displayName()} · " +
                                    candidate.birthInput.timeSourceType.displayName(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "四柱：${snapshot?.result?.fourPillars?.display() ?: "计算快照缺失"}",
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (
                                case.deletedAt == null &&
                                !candidate.adopted
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onAdoptBirthTimeCandidate(candidate.id)
                                    },
                                    enabled = !mutationSaving && snapshot != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .testTag(
                                            "adopt_birth_time_candidate_${candidate.id}",
                                        ),
                                ) {
                                    Text(if (mutationSaving) "正在切换…" else "采用此时间")
                                }
                            }
                        }
                    }
                }
            }
            mutationError?.let { error ->
                Text(
                    error,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .testTag("candidate_mutation_error"),
                    color = MaterialTheme.colorScheme.error,
                )
            }
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
            DetailRow("出生地区", case.birthInput.locationName ?: "未提供")
            DetailRow(
                "经纬度",
                if (case.birthInput.longitude != null && case.birthInput.latitude != null) {
                    "${case.birthInput.longitude}, ${case.birthInput.latitude}"
                } else {
                    "未提供"
                },
            )
            if (case.birthInput.coordinateSource != null) {
                DetailRow("坐标来源", "用户录入")
            }
            DetailRow("时区", case.birthInput.timeZoneId)
            DetailRow(
                "UTC offset",
                case.birthInput.resolvedUtcOffsetSeconds?.let(::formatUtcOffset)
                    ?: "旧数据未解析",
            )
            DetailRow(
                "时区数据版本",
                case.birthInput.timeZoneDataVersion ?: "旧数据未记录",
            )
            DetailRow("时间精度", case.birthInput.timePrecision.displayName())
            DetailRow("时间来源", case.birthInput.timeSourceType.displayName())
            case.birthInput.sourceNote?.let { DetailRow("时间来源说明", it) }
            DetailRow(
                "真太阳时",
                if (case.birthInput.useTrueSolarTime) "已启用" else "未启用",
            )
            DetailRow("来源", case.sourceType.displayName())
            case.copiedFromCaseId?.let { sourceId ->
                DetailRow("复制来源", sourceId)
            }
        }
        }
        if (
            selectedSection == CaseDetailSection.BASIC_CHART ||
            selectedSection == CaseDetailSection.FORTUNE
        ) {
            DetailSection(
                if (selectedSection == CaseDetailSection.BASIC_CHART) {
                    "基本排盘"
                } else {
                    "起运与岁运"
                },
            ) {
            if (adopted == null) {
                Text("当前命例没有已采用的计算快照。")
            } else {
                if (selectedSection == CaseDetailSection.BASIC_CHART) {
                    DetailRow("四柱", adopted.result.fourPillars.display())
                    adopted.result.basicChartDetails?.let { basic ->
                        BasicChartDetailsView(
                            details = basic,
                            sex = case.sexForFortuneDirection,
                        )
                    }
                    DetailRow("胎元", adopted.result.fetalOrigin)
                    DetailRow("胎息", adopted.result.fetalBreath)
                    DetailRow("命宫", adopted.result.ownSign)
                    DetailRow("身宫", adopted.result.bodySign)
                    DetailRow("计算配置", adopted.result.profile.id)
                    DetailRow("子时规则", adopted.result.profile.ratHourRule.displayName())
                    DetailRow("引擎", adopted.result.evidence.engineName)
                    DetailRow("引擎版本", adopted.result.evidence.engineVersion)
                    DetailRow("规则版本", adopted.result.evidence.ruleVersion)
                    adopted.result.calendarConversion?.let { conversion ->
                        DetailRow("换算公历", conversion.solarDateTime.display())
                        val lunar = conversion.lunarDateTime
                        DetailRow(
                            "换算农历",
                            "${lunar.year}年${if (lunar.isLeapMonth) "闰" else ""}" +
                                "${lunar.month}月${lunar.day}日 " +
                                "%02d:%02d:%02d".format(
                                    lunar.hour,
                                    lunar.minute,
                                    lunar.second,
                                ),
                        )
                    }
                    adopted.result.trueSolarTimeEvidence?.let { evidence ->
                        DetailRow("原始民用时间", evidence.originalCivilDateTime.display())
                        DetailRow("真太阳时", evidence.trueSolarDateTime.display())
                        DetailRow(
                            "经度平太阳时校正",
                            formatSignedDuration(evidence.meanSolarCorrectionSeconds),
                        )
                        DetailRow(
                            "均时差校正",
                            formatSignedDuration(evidence.equationOfTimeCorrectionSeconds),
                        )
                        DetailRow(
                            "总校正量",
                            formatSignedDuration(evidence.totalCorrectionSeconds),
                        )
                        DetailRow(
                            "边界变化",
                            buildList {
                                if (evidence.crossesDate) add("跨日")
                                if (evidence.crossesDoubleHour) add("跨时辰")
                            }.joinToString("、").ifEmpty { "未跨日、未跨时辰" },
                        )
                        DetailRow(
                            "真太阳时作用规则",
                            "暂定：年/月按民用时，日/时按真太阳时",
                        )
                        DetailRow("真太阳时算法", evidence.algorithmVersion)
                    }
                    adopted.result.warnings.forEach { warning ->
                        DetailRow("计算提醒", warning.message)
                    }
                } else {
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
                            "${adopted.result.fortuneStart.days} 日 " +
                            "${adopted.result.fortuneStart.hours} 时 " +
                            "${adopted.result.fortuneStart.minutes} 分",
                    )
                    DetailRow(
                        "精确交运时间",
                        adopted.result.fortuneStart.endAt.display(),
                        tag = "fortune_transfer_time",
                    )
                    OutlinedTextField(
                        value = fortuneObservationDate,
                        onValueChange = onFortuneObservationDateChange,
                        label = { Text("观察日期（YYYY-MM-DD）") },
                        singleLine = true,
                        isError = fortunePositionError != null,
                        supportingText = fortunePositionError?.let { message ->
                            { Text(message) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                            .testTag("fortune_observation_date"),
                    )
                    OutlinedTextField(
                        value = fortuneObservationTime,
                        onValueChange = onFortuneObservationTimeChange,
                        label = { Text("观察时间（HH:mm）") },
                        singleLine = true,
                        isError = fortunePositionError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("fortune_observation_time"),
                    )
                    fortunePosition?.let { position ->
                        CurrentFortunePositionView(position)
                    }
                    professionalFortunePosition?.let { position ->
                        ProfessionalFortunePositionView(position)
                    }
                    Text(
                        "定位规则：流年以精确立春切换；流月以交节瞬间切换；" +
                            "流日按命例子时规则；大运以精确交运时刻切换。",
                        modifier = Modifier.padding(bottom = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DecadeFortuneDetailsView(adopted.result.decadeFortunes)
                    AnnualFortuneDetailsView(
                        annuals = adopted.result.annualFortunes,
                        current = fortunePosition,
                    )
                }
            }
        }
        }
        if (
            selectedSection == CaseDetailSection.BASIC_INFO &&
            case.fieldEvidence.isNotEmpty()
        ) {
            DetailSection("导入证据对照") {
                Text(
                    "来源原文不会被人工修正覆盖；规范值、采用值与本机计算结果分别留存。",
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                case.fieldEvidence.forEach { evidence ->
                    Text(
                        evidence.fieldKey.evidenceFieldLabel(),
                        modifier = Modifier.padding(bottom = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                    DetailRow("来源值", evidence.rawText)
                    DetailRow(
                        "规范值",
                        evidence.normalizedValue?.evidenceDisplayValue() ?: "未识别",
                    )
                    DetailRow(
                        "采用值",
                        evidence.adoptedValue?.evidenceDisplayValue() ?: "未采用",
                    )
                    DetailRow(
                        "计算值",
                        evidence.calculatedValue?.evidenceDisplayValue()
                            ?: if (evidence.fieldKey == "chart.four_pillars") {
                                adopted?.result?.fourPillars?.display() ?: "无已采用计算快照"
                            } else if (
                                evidence.fieldKey.startsWith("chart.") &&
                                evidence.fieldKey.endsWith(".spirits")
                            ) {
                                "神煞仅保留来源证据；当前不自动复算"
                            } else if (
                                evidence.fieldKey == "identity.constellation" ||
                                evidence.fieldKey == "identity.zodiac"
                            ) {
                                "未完成基础排盘自动对照"
                            } else if (evidence.fieldKey.startsWith("chart.")) {
                                "未完成基础排盘自动对照"
                            } else if (evidence.fieldKey.startsWith("professional.")) {
                                "未完成专业流运自动对照"
                            } else {
                                "不参与命盘计算"
                            },
                    )
                    evidence.consistencyConfidence?.let { confidence ->
                        DetailRow(
                            "自动对照",
                            if (confidence == 1f) "一致" else "不一致，保留来源待核对",
                        )
                    }
                    DetailRow("人工修正", if (evidence.userEdited) "是" else "否")
                    HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
                }
            }
        }
        if (selectedSection == CaseDetailSection.RECORDS) {
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
                            .heightIn(min = 48.dp)
                            .testTag("add_record_button"),
                    ) {
                        Text("新增记录")
                    }
                    OutlinedButton(
                        onClick = onAddEvent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
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
private fun DetailRow(
    label: String,
    value: String,
    tag: String? = null,
) {
    Column(
        modifier = Modifier
            .then(if (tag == null) Modifier else Modifier.testTag(tag))
            .padding(bottom = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun DecadeFortuneDetailsView(
    decades: List<DecadeFortune>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .testTag("decade_fortune_details"),
    ) {
        Text(
            "前八步大运",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
            Text("大运", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Text(
                "起止年龄",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "起止年份",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        decades.forEachIndexed { index, decade ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                    "${index + 1}. ${decade.name}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${decade.startAge}–${decade.endAge} 岁",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${decade.startYear}–${decade.endYear}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CurrentFortunePositionView(
    position: FortunePosition,
) {
    val decadeText = when (position.status) {
        FortunePositionStatus.BEFORE_FIRST_DECADE -> "尚未交入第一步大运"
        FortunePositionStatus.WITHIN_DECADE ->
            position.decadeFortune?.name ?: "当前大运未定位"
        FortunePositionStatus.AFTER_TIMELINE -> "已超出前八步大运范围"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("current_fortune_position"),
    ) {
        DetailRow(
            "当前流年",
            "${position.annualFortune.calendarYear} ${position.annualFortune.name} · " +
                "虚岁 ${position.annualFortune.nominalAge}",
            tag = "current_annual_fortune",
        )
        DetailRow("当前大运", decadeText, tag = "current_decade_fortune")
    }
}

@Composable
private fun ProfessionalFortunePositionView(
    position: ProfessionalFortunePosition,
) {
    val context = LocalContext.current
    var copied by remember(position) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("professional_fortune_position"),
    ) {
        Text(
            "专业流运",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        DetailRow("流年柱", position.flowPillars.year, tag = "flow_year_pillar")
        DetailRow("流月柱", position.flowPillars.month, tag = "flow_month_pillar")
        DetailRow("流日柱", position.flowPillars.day, tag = "flow_day_pillar")
        DetailRow("流时柱", position.flowPillars.hour, tag = "flow_hour_pillar")
        DetailRow(
            "前一节气",
            "${position.previousSolarTerm.name}（${position.previousSolarTerm.type.displayName()}） " +
                position.previousSolarTerm.at.display(),
            tag = "previous_solar_term",
        )
        DetailRow(
            "后一节气",
            "${position.nextSolarTerm.name}（${position.nextSolarTerm.type.displayName()}） " +
                position.nextSolarTerm.at.display(),
            tag = "next_solar_term",
        )
        DetailRow("计算档案", position.profileId, tag = "fortune_profile_id")
        DetailRow("规则版本", position.ruleVersion, tag = "fortune_rule_version")
        DetailRow(
            "观察时刻口径",
            "民用时（不额外校正观察地点真太阳时）",
            tag = "fortune_observation_time_mode",
        )
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(
                    ClipData.newPlainText(
                        "南枫八字专业流运诊断",
                        position.toDiagnosticText(),
                    ),
                )
                copied = true
            },
            modifier = Modifier.testTag("copy_fortune_diagnostics"),
        ) {
            Text(if (copied) "诊断已复制" else "复制流运诊断")
        }
    }
}

private fun com.nanzhufeng.nanfengbazi.domain.model.SolarTermType.displayName(): String =
    when (this) {
        com.nanzhufeng.nanfengbazi.domain.model.SolarTermType.JIE -> "节"
        com.nanzhufeng.nanfengbazi.domain.model.SolarTermType.QI -> "气"
    }

private fun ProfessionalFortunePosition.toDiagnosticText(): String = buildString {
    appendLine("观察时刻：${position.observedAt.display()}")
    appendLine(
        "当前流年：${position.annualFortune.calendarYear} " +
            "${position.annualFortune.name}（虚岁 ${position.annualFortune.nominalAge}）",
    )
    appendLine("当前大运：${position.decadeFortune?.name ?: position.status.name}")
    appendLine(
        "流柱：年 ${flowPillars.year} 月 ${flowPillars.month} " +
            "日 ${flowPillars.day} 时 ${flowPillars.hour}",
    )
    appendLine(
        "前一节气：${previousSolarTerm.name}（${previousSolarTerm.type.displayName()}）" +
            " ${previousSolarTerm.at.display()}",
    )
    appendLine(
        "后一节气：${nextSolarTerm.name}（${nextSolarTerm.type.displayName()}）" +
            " ${nextSolarTerm.at.display()}",
    )
    appendLine("计算档案：$profileId")
    appendLine("观察时刻口径：民用时（不额外校正观察地点真太阳时）")
    append("规则版本：$ruleVersion")
}

@Composable
private fun AnnualFortuneDetailsView(
    annuals: List<AnnualFortune>,
    current: FortunePosition?,
) {
    if (annuals.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .testTag("annual_fortune_details"),
    ) {
        Text(
            "流年表",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "出生年至前八步大运终点；交运年份的当前归属以上方精确时刻定位为准。",
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text("年份", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall)
            Text("流年", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall)
            Text(
                "虚岁",
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "大运",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        annuals.forEach { annual ->
            val isCurrent = current?.annualFortune?.calendarYear == annual.calendarYear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .then(
                        if (isCurrent) {
                            Modifier.testTag("current_annual_fortune_row")
                        } else {
                            Modifier
                        },
                    ),
            ) {
                val style = if (isCurrent) {
                    MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodySmall
                }
                Text(
                    annual.calendarYear.toString(),
                    modifier = Modifier.weight(0.8f),
                    style = style,
                )
                Text(annual.name, modifier = Modifier.weight(0.8f), style = style)
                Text(
                    annual.nominalAge.toString(),
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.Center,
                    style = style,
                )
                Text(
                    annual.decadeName ?: "未交运",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = style,
                )
            }
        }
    }
}

@Composable
private fun BasicChartDetailsView(
    details: BasicChartDetails,
    sex: SexForFortuneDirection,
) {
    val pillars = details.pillars.associateBy { it.position }
    val ordered = PillarPosition.entries.map { requireNotNull(pillars[it]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("basic_chart_details"),
    ) {
        Text(
            "基础排盘明细",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "生肖 ${details.zodiac} · ${details.westernZodiac}座 · 日主 ${details.dayMaster}",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "前节气 ${details.previousSolarTerm.name} " +
                details.previousSolarTerm.at.display(),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "后节气 ${details.nextSolarTerm.name} ${details.nextSolarTerm.at.display()}",
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val previousJie = details.previousJie
        val nextJie = details.nextJie
        if (previousJie != null && nextJie != null) {
            Text(
                "问真同口径前一节 ${previousJie.name} " +
                    previousJie.at.display(),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "问真同口径后一节 ${nextJie.name} ${nextJie.at.display()}",
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicChartTableRow(
            label = "",
            values = listOf("年柱", "月柱", "日柱", "时柱"),
        )
        BasicChartTableRow(
            label = "主星",
            values = ordered.map { pillar ->
                if (pillar.position == PillarPosition.DAY) {
                    if (sex == SexForFortuneDirection.MAN) "元男" else "元女"
                } else {
                    pillar.primaryTenGod
                }
            },
            tag = "basic_chart_primary",
        )
        BasicChartTableRow("天干", ordered.map(PillarDetail::heavenStem))
        BasicChartTableRow("地支", ordered.map(PillarDetail::earthBranch))
        BasicChartTableRow(
            "藏干",
            ordered.map { it.hiddenStems.joinToString("\n") { hidden -> hidden.heavenStem } },
        )
        BasicChartTableRow(
            "副星",
            ordered.map { it.hiddenStems.joinToString("\n") { hidden -> hidden.tenGod } },
            tag = "basic_chart_secondary",
        )
        BasicChartTableRow("星运", ordered.map(PillarDetail::terrain))
        BasicChartTableRow("自坐", ordered.map(PillarDetail::selfSittingTerrain))
        BasicChartTableRow(
            "空亡",
            ordered.map { it.voidEarthBranches.joinToString("") },
        )
        BasicChartTableRow("纳音", ordered.map(PillarDetail::naYin))
    }
}

@Composable
private fun BasicChartTableRow(
    label: String,
    values: List<String>,
    tag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag == null) Modifier else Modifier.testTag(tag))
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        values.forEach { value ->
            Text(
                value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
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

private fun TimePrecision.displayName(): String = when (this) {
    TimePrecision.EXACT_TO_SECOND -> "精确到秒"
    TimePrecision.EXACT_TO_MINUTE -> "精确到分钟"
    TimePrecision.APPROXIMATE -> "大约时间"
    TimePrecision.HOUR_ONLY -> "只知小时"
    TimePrecision.DOUBLE_HOUR_ONLY -> "只知时辰"
    TimePrecision.UNKNOWN -> "时辰未知"
}

private fun TimeSourceType.displayName(): String = when (this) {
    TimeSourceType.SELF_REPORTED -> "本人提供"
    TimeSourceType.FAMILY_REPORTED -> "家人提供"
    TimeSourceType.OFFICIAL_RECORD -> "出生证明"
    TimeSourceType.WENZHEN_SCREENSHOT -> "问真截图"
    TimeSourceType.OTHER_RECORD -> "其他资料"
    TimeSourceType.UNKNOWN -> "未说明"
}

private fun RatHourRule.displayName(): String = when (this) {
    RatHourRule.TYME_DEFAULT -> "23:00 换日（Tyme 默认）"
    RatHourRule.LATE_RAT_SAME_DAY -> "晚子时日柱算当天"
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
            "农历 ${date.year}年${if (date.isLeapMonth) "闰" else ""}" +
                "${date.month}月${date.day}日 " +
                "%02d:%02d:%02d".format(date.hour, date.minute, date.second)
        }
    }

private fun CivilDateTime.display(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun formatUtcOffset(totalSeconds: Int): String {
    val sign = if (totalSeconds >= 0) "+" else "-"
    val absolute = kotlin.math.abs(totalSeconds)
    val hours = absolute / 3_600
    val minutes = absolute % 3_600 / 60
    val seconds = absolute % 60
    return if (seconds == 0) {
        "UTC$sign%02d:%02d".format(hours, minutes)
    } else {
        "UTC$sign%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}

private fun formatSignedDuration(totalSeconds: Int): String {
    val sign = if (totalSeconds >= 0) "+" else "-"
    val absolute = kotlin.math.abs(totalSeconds)
    return "$sign${absolute / 60}分${absolute % 60}秒"
}

private fun FourPillars.display(): String = "$year $month $day $hour"

private fun CaseSortOrder.displayName(): String = when (this) {
    CaseSortOrder.LAST_VIEWED_DESC -> "最近查看"
    CaseSortOrder.UPDATED_DESC -> "最近更新"
    CaseSortOrder.CREATED_DESC -> "最近创建"
    CaseSortOrder.BIRTH_ASC -> "出生时间"
}
