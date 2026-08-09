package com.nanzhufeng.nanfengbazi

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.nanfengbazi.domain.BasicShenShaRules
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidate
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.FortunePosition
import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupContract
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidate
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneLayer
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneSelection
import com.nanzhufeng.nanfengbazi.domain.ProfessionalPillarColumn
import com.nanzhufeng.nanfengbazi.domain.ProfessionalTextGroup
import com.nanzhufeng.nanfengbazi.domain.ProfessionalTimelineItem
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
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
import com.nanzhufeng.nanfengbazi.domain.model.toTraditionalChineseText
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenSourceFidelityContract
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
import com.nanzhufeng.nanfengbazi.data.backup.BackupDatabasePreflight
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlan
import com.nanzhufeng.nanfengbazi.data.backup.RestorePreview
import com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummary
import com.nanzhufeng.nanfengbazi.domain.displayName
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
    onCreateCaseImageDocument: (String) -> Unit = {},
    onSharePreparedCaseImage: () -> Unit = {},
    onCreateFullBackupDocument: (String) -> Unit = {},
    onCreateEncryptedFullBackupDocument: (String) -> Unit = {},
    onOpenFullBackupDocument: () -> Unit = {},
    onRetryPasswordFullBackupDocument: (CharArray) -> Unit = {},
    onExecuteFullBackupDocument: (CharArray?) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            AppDestination.CreateCase,
        ),
    ) {
        viewModel.navigateBack()
    }
    NanfengBaziTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
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
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showRootNavigation && !useNavigationRail) {
                            RootNavigationBar(
                                destination = state.destination,
                                onOpenChart = openChart,
                                onOpenRecords = viewModel::backToList,
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
                                onOpenRecords = viewModel::backToList,
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
                        onOpenCaseComparison = viewModel::openCaseComparison,
                        onOpenCase = viewModel::openDetail,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.CaseComparison -> CaseComparisonScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onSelectLeft = viewModel::selectComparisonLeft,
                        onSelectRight = viewModel::selectComparisonRight,
                        onRetry = viewModel::retryCaseComparison,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.FourPillarsLookup -> FourPillarsLookupScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onConfirmSelection = viewModel::confirmFourPillarsLookup,
                        onUseCandidate = viewModel::useFourPillarsLookupCandidate,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.Almanac -> AlmanacScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onPreviousMonth = { viewModel.moveAlmanacMonth(-1) },
                        onNextMonth = { viewModel.moveAlmanacMonth(1) },
                        onToday = viewModel::showTodayInAlmanac,
                        onSelectDate = viewModel::selectAlmanacDate,
                        onSelectDateTime = viewModel::selectAlmanacDateTime,
                        onSelectDoubleHour = viewModel::selectAlmanacDoubleHour,
                        onAdjustFourPillars = viewModel::openFourPillarsLookup,
                        onUseForChart = viewModel::useAlmanacDateForChart,
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
                        onRatHourRuleChange = viewModel::updateDefaultRatHourRule,
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
                        onConfirmFourPillarsLookup = viewModel::confirmFourPillarsLookup,
                        onPrepareBirthPickerToday = viewModel::prepareBirthPickerToday,
                        onOpenAlmanac = viewModel::openAlmanac,
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
                                onOpenCommentaryCandidates =
                                    viewModel::openMasterCommentaryCandidates,
                                onOpenFeedbackThemeCandidates =
                                    viewModel::openFeedbackThemeCandidates,
                                onAddEvent = { viewModel.openEvent() },
                                onEditEvent = viewModel::openEvent,
                                onOwnerFeedbackChange = viewModel::updateOwnerFeedback,
                                onMasterCommentaryChange = viewModel::updateMasterCommentary,
                                onAddNotesTimeline = viewModel::addCaseNotesTimeline,
                                onNotesTimelineContentChange =
                                    viewModel::updateCaseNotesTimelineContent,
                                onSaveCaseNotes = viewModel::saveCaseNotes,
                                onDuplicate = viewModel::duplicateCase,
                                onExportSingleCase = viewModel::requestSingleCaseExport,
                                onOpenObjectiveSummary = viewModel::openObjectiveSummary,
                                onOpenExternalAnalysis = viewModel::openExternalAnalysisBridge,
                                onExportCaseImage = {
                                    viewModel.requestCaseImageDelivery(
                                        CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE,
                                    )
                                },
                                onShareCaseImage = {
                                    viewModel.requestCaseImageDelivery(
                                        CaseImageDeliveryMode.SHARE_LONG_IMAGE,
                                    )
                                },
                                onMoveToTrash = viewModel::requestMoveToTrash,
                                onRestore = viewModel::restoreCase,
                                onSelectSection = viewModel::selectDetailSection,
                                onFortuneObservationDateChange =
                                    viewModel::updateFortuneObservationDate,
                                onFortuneObservationTimeChange =
                                    viewModel::updateFortuneObservationTime,
                                onFortuneObservationSelect =
                                    viewModel::selectProfessionalFortuneObservation,
                                onFortuneToday = viewModel::locateFortuneToday,
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
                    is AppDestination.CaseObjectiveSummary -> CaseObjectiveSummaryScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onRetry = viewModel::retryObjectiveSummary,
                        onCopy = {
                            viewModel.copyObjectiveSummary { text ->
                                val clipboard = context
                                    .getSystemService(ClipboardManager::class.java)
                                if (clipboard == null) {
                                    false
                                } else {
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("南枫八字客观命盘摘要", text),
                                    )
                                    true
                                }
                            }
                        },
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.ExternalAnalysisBridge -> ExternalAnalysisBridgeScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onSetGroupSelected = viewModel::setExternalAnalysisGroupSelected,
                        onSetRedaction = viewModel::setExternalAnalysisRedaction,
                        onSetExportConfirmed =
                            viewModel::setExternalAnalysisExportConfirmed,
                        onCopy = {
                            viewModel.copyExternalAnalysisPayload { text ->
                                val clipboard = context
                                    .getSystemService(ClipboardManager::class.java)
                                    ?: return@copyExternalAnalysisPayload false
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("南枫八字外部分析材料", text),
                                )
                                clipboard.primaryClip
                                    ?.getItemAt(0)
                                    ?.coerceToText(context)
                                    ?.toString() == text
                            }
                        },
                        onProviderChange = viewModel::updateExternalAnalysisProvider,
                        onModelChange = viewModel::updateExternalAnalysisModel,
                        onResultChange = viewModel::updateExternalAnalysisResult,
                        onSetImportConfirmed =
                            viewModel::setExternalAnalysisImportConfirmed,
                        onSave = viewModel::saveExternalAnalysisResult,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.MasterCommentaryCandidates ->
                        MasterCommentaryCandidateScreen(
                            state = state,
                            onBack = viewModel::navigateBack,
                            onContentChange =
                                viewModel::updateMasterCommentaryCandidateContent,
                            onCategoryChange =
                                viewModel::updateMasterCommentaryCandidateCategory,
                            onReject = viewModel::rejectMasterCommentaryCandidate,
                            onRestoreRejected =
                                viewModel::restoreRejectedMasterCommentaryCandidate,
                            onAdopt = viewModel::adoptMasterCommentaryCandidate,
                            modifier = Modifier.padding(padding),
                        )
                    is AppDestination.FeedbackThemeCandidates ->
                        FeedbackThemeCandidateScreen(
                            state = state,
                            onBack = viewModel::navigateBack,
                            onTagChange = viewModel::updateFeedbackThemeCandidateTag,
                            onReject = viewModel::rejectFeedbackThemeCandidate,
                            onRestoreRejected =
                                viewModel::restoreRejectedFeedbackThemeCandidate,
                            onAdopt = viewModel::adoptFeedbackThemeCandidate,
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
            state.caseImageConfirmationMode?.let { mode ->
                AlertDialog(
                    onDismissRequest = viewModel::cancelCaseImageConfirmation,
                    title = {
                        Text(
                            if (mode == CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE) {
                                "导出命盘长图？"
                            } else {
                                "分享命盘长图？"
                            },
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "图片会使用当前已采用的本机计算快照和正式记录，" +
                                    "不会重新排盘，也不会把问真来源值或旧快照当作当前结果。",
                            )
                            Text(
                                "图片包含姓名、出生资料、命盘和研究记录。" +
                                    if (mode == CaseImageDeliveryMode.SHARE_LONG_IMAGE) {
                                        "继续后会把同一 PNG 交给你选择的外部应用；" +
                                            "是否实际发送由目标应用决定。"
                                    } else {
                                        "请只保存到可信位置。"
                                    },
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.confirmCaseImageDelivery { preparedMode, fileName ->
                                    when (preparedMode) {
                                        CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE ->
                                            onCreateCaseImageDocument(fileName)
                                        CaseImageDeliveryMode.SHARE_LONG_IMAGE ->
                                            onSharePreparedCaseImage()
                                    }
                                }
                            },
                            modifier = Modifier.testTag(
                                if (mode == CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE) {
                                    "confirm_case_image_export"
                                } else {
                                    "confirm_case_image_share"
                                },
                            ),
                        ) {
                            Text(
                                if (mode == CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE) {
                                    "生成并选择位置"
                                } else {
                                    "生成并打开分享"
                                },
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::cancelCaseImageConfirmation) {
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
            state.caseImageError?.let { error ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissCaseImageError,
                    title = { Text("命盘图片处理失败") },
                    text = {
                        Text(
                            error,
                            modifier = Modifier.testTag("case_image_error"),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissCaseImageError) {
                            Text("知道了")
                        }
                    },
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
                        Text(
                            if (
                                preview.databasePreflight ==
                                BackupDatabasePreflight.INDEPENDENT_ROOM_ROUND_TRIP_VERIFIED
                            ) {
                                "独立临时数据库预演：通过"
                            } else {
                                "独立临时数据库预演：未执行"
                            },
                            modifier = Modifier.testTag("full_backup_database_preflight"),
                        )
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
private val EXPANDED_DETAIL_MIN_WIDTH = 360.dp

private data class RootNavigationAction(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val tag: String,
    val onClick: () -> Unit,
)

private fun rootNavigationActions(
    destination: AppDestination,
    onOpenChart: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
): List<RootNavigationAction> = listOf(
    RootNavigationAction(
        "排盘",
        Icons.Filled.Home,
        destination == AppDestination.CreateCase,
        "nav_chart",
        onOpenChart,
    ),
    RootNavigationAction(
        "记录",
        Icons.Filled.List,
        destination == AppDestination.CaseList ||
            destination == AppDestination.RecordHub ||
            destination is AppDestination.CaseDetail,
        "nav_records",
        onOpenRecords,
    ),
    RootNavigationAction(
        "设置",
        Icons.Filled.Settings,
        destination == AppDestination.Settings,
        "nav_settings",
        onOpenSettings,
    ),
)

@Composable
private fun RootNavigationBar(
    destination: AppDestination,
    onOpenChart: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val items = rootNavigationActions(
        destination = destination,
        onOpenChart = onOpenChart,
        onOpenRecords = onOpenRecords,
        onOpenSettings = onOpenSettings,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("root_navigation"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NanfengGreen,
                        selectedTextColor = NanfengGreen,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = NanfengNavigationMuted,
                        unselectedTextColor = NanfengNavigationMuted,
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .widthIn(min = 48.dp)
                        .semantics { contentDescription = item.label }
                        .testTag(item.tag),
                )
            }
        }
    }
}

@Composable
private fun RootNavigationRail(
    destination: AppDestination,
    onOpenChart: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = rootNavigationActions(
        destination = destination,
        onOpenChart = onOpenChart,
        onOpenRecords = onOpenRecords,
        onOpenSettings = onOpenSettings,
    )
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .padding(vertical = 12.dp)
            .testTag("root_navigation_rail"),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NanfengGreen,
                    selectedTextColor = NanfengGreen,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = NanfengNavigationMuted,
                    unselectedTextColor = NanfengNavigationMuted,
                ),
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
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("记录", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${cases.size} 个本地保存案例",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("刷新")
                }
            }
        }
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 10.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cases, key = CaseSummary::id) { summary ->
                    RecordCaseRow(summary = summary, onClick = { onOpenCase(summary.id) })
                }
            }
        }
    }
}

@Composable
private fun RecordCaseRow(summary: CaseSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("record_case_${summary.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConstellationBadge(summary.westernZodiac)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.name.value ?: summary.alias,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    summary.birthInput.displayDateOnly(),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    summary.fourPillars?.display() ?: "待排盘",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (summary.fourPillars == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        NanfengGreen
                    },
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "查看案例",
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanfengGold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHomeScreen(
    state: StageTwoUiState,
    screenshotImportState: ScreenshotImportUiState,
    onRatHourRuleChange: (RatHourRule) -> Unit,
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
            .testTag("settings_home_screen"),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                "设置",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Card(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("本地优先", color = NanfengGreen, fontWeight = FontWeight.SemiBold)
                Text(
                    "命例、截图识别与排盘只在本机处理，不连接外部 AI。",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SettingsGroupTitle("排盘偏好")
        SettingsActionGroup {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("子时口径", style = MaterialTheme.typography.titleMedium)
                Text(
                    "作为新排盘与四柱反查的默认规则；既有命例仍按快照规则复算。",
                    modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatHourRuleButton(
                        label = "23:00 换日",
                        selected = state.defaultRatHourRule == RatHourRule.TYME_DEFAULT,
                        tag = "settings_rat_default",
                        onClick = { onRatHourRuleChange(RatHourRule.TYME_DEFAULT) },
                        modifier = Modifier.weight(1f),
                    )
                    RatHourRuleButton(
                        label = "晚子时算当天",
                        selected = state.defaultRatHourRule == RatHourRule.LATE_RAT_SAME_DAY,
                        tag = "settings_rat_late",
                        onClick = { onRatHourRuleChange(RatHourRule.LATE_RAT_SAME_DAY) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        SettingsGroupTitle("导入与建档")
        SettingsActionGroup {
            SettingsActionRow(
                title = "导入问真截图",
                description = "本机识别后逐项确认",
                icon = Icons.Filled.Search,
                onClick = onImportScreenshots,
                enabled = !screenshotImportState.busy,
                tag = "settings_import_screenshots",
                accent = NanfengOrange,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            SettingsActionRow(
                title = "导入单案例文件",
                description = "先查冲突，不覆盖现有案例",
                icon = Icons.Filled.List,
                onClick = onImportSingleCase,
                tag = "settings_import_case",
            )
        }
        SettingsGroupTitle("备份与恢复")
        SettingsActionGroup {
            SettingsActionRow(
                title = "导出完整备份",
                description = "命例、记录、快照与附件",
                icon = Icons.Filled.Home,
                onClick = onExportFullBackup,
                tag = "settings_export_backup",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            SettingsActionRow(
                title = "预览并恢复完整备份",
                description = "先核验清单再决定写入",
                icon = Icons.Filled.KeyboardArrowRight,
                onClick = onRestoreFullBackup,
                tag = "settings_restore_backup",
                accent = NanfengOrange,
            )
        }
        SettingsGroupTitle("诊断与版本")
        SettingsActionGroup {
            SettingsActionRow(
                title = "复制脱敏诊断包",
                description = "不包含个人资料与附件内容",
                icon = Icons.Filled.Settings,
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
                tag = "settings_copy_diagnostics",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            SettingsStaticRow("当前版本", BuildConfig.VERSION_NAME)
        }
        Text(
            "正式发布仍需签名、真实问真样本与真机数据保留验收。",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsActionGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true,
    accent: Color = NanfengGreen,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = title }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(36.dp),
            color = accent.copy(alpha = 0.10f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = accent,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsStaticRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(42.dp))
        Text(title, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onOpenCaseComparison: () -> Unit,
    onOpenCase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_list_screen"),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = NanfengPageBackground,
                ) {
                    Row {
                        RecordTopTab(
                            text = "用户列表",
                            selected = state.visibility != CaseVisibility.TRASHED,
                            onClick = { onSelectVisibility(CaseVisibility.ACTIVE) },
                            modifier = Modifier.weight(1f),
                            tag = "visibility_active",
                        )
                        RecordTopTab(
                            text = "回收站",
                            selected = state.visibility == CaseVisibility.TRASHED,
                            onClick = { onSelectVisibility(CaseVisibility.TRASHED) },
                            modifier = Modifier.weight(1f),
                            tag = "visibility_trashed",
                        )
                    }
                }
                Box {
                    TextButton(
                        onClick = { moreExpanded = true },
                        modifier = Modifier
                            .width(52.dp)
                            .heightIn(min = 48.dp)
                            .testTag("record_more"),
                    ) {
                        Text("•••", style = MaterialTheme.typography.titleMedium)
                    }
                    DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("新排盘") },
                            onClick = {
                                moreExpanded = false
                                onCreate()
                            },
                            modifier = Modifier.testTag("new_case_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("导入问真截图") },
                            onClick = {
                                moreExpanded = false
                                onImportScreenshots()
                            },
                            enabled = !screenshotImportState.busy,
                            modifier = Modifier.testTag("import_screenshots_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("导入单案例") },
                            onClick = {
                                moreExpanded = false
                                onImportSingleCase()
                            },
                            modifier = Modifier.testTag("import_single_case_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("命例对比") },
                            onClick = {
                                moreExpanded = false
                                onOpenCaseComparison()
                            },
                            enabled = state.visibility == CaseVisibility.ACTIVE,
                            modifier = Modifier.testTag("open_case_comparison"),
                        )
                        DropdownMenuItem(
                            text = { Text("导出完整备份") },
                            onClick = {
                                moreExpanded = false
                                onExportFullBackup()
                            },
                            modifier = Modifier.testTag("export_full_backup_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("恢复完整备份") },
                            onClick = {
                                moreExpanded = false
                                onPreviewFullBackup()
                            },
                            modifier = Modifier.testTag("preview_full_backup_button"),
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("case_search"),
            placeholder = { Text("请输入姓名、别名或四柱") },
            trailingIcon = {
                Text(
                    "筛选",
                    modifier = Modifier
                        .padding(end = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        ScreenshotImportSummary(
            state = screenshotImportState,
            onRetry = onRetryScreenshotImport,
            onDelete = onDeleteScreenshotImport,
            onReview = onReviewScreenshotImport,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordCategoryTab(
                text = "全部",
                selected = state.selectedGroupId == null && state.selectedTagId == null,
                onClick = { onSelectGroup(null); onSelectTag(null) },
            )
            state.availableGroups.take(5).forEach { group ->
                RecordCategoryTab(
                    text = group.name,
                    selected = state.selectedGroupId == group.id,
                    onClick = { onSelectGroup(group.id) },
                )
            }
            state.availableTags.take(3).forEach { tag ->
                RecordCategoryTab(
                    text = tag.name,
                    selected = state.selectedTagId == tag.id,
                    onClick = { onSelectTag(tag.id) },
                )
            }
            TextButton(
                onClick = {
                    val next = CaseSortOrder.entries[
                        (CaseSortOrder.entries.indexOf(state.sortOrder) + 1) %
                            CaseSortOrder.entries.size
                    ]
                    onSelectSort(next)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("排序") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            state.listLoading -> LoadingBox("正在读取命例…")
            state.listError != null -> ErrorBox(
                message = state.listError,
                actionLabel = "重试",
                onAction = onRefresh,
            )
            state.cases.isEmpty() -> EmptyCaseList(state.visibility, onCreate)
            else -> Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        end = 28.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    itemsIndexed(state.cases, key = { _, item -> item.id }) { index, summary ->
                        val section = summary.recordSection()
                        val previousSection = state.cases.getOrNull(index - 1)?.recordSection()
                        if (section != previousSection) {
                            Text(
                                section,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        CaseSummaryRow(summary, onClick = { onOpenCase(summary.id) })
                    }
                }
                RecordAlphabetIndex(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordTopTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    tag: String,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(26.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun RecordCategoryTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(
            text,
            color = if (selected) NanfengGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RecordAlphabetIndex(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".forEach { initial ->
            Text(
                initial.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun CaseSummary.recordSection(): String {
    if (isPinned) return "星标置顶"
    val first = (name.value ?: alias).trim().firstOrNull() ?: return "#"
    return if (first.isLetter() && first.code < 128) first.uppercase() else "#"
}

private fun String?.constellationIconRes(): Int? = when (this?.removeSuffix("座")) {
    "白羊" -> R.drawable.zodiac_aries
    "金牛" -> R.drawable.zodiac_taurus
    "双子" -> R.drawable.zodiac_gemini
    "巨蟹" -> R.drawable.zodiac_cancer
    "狮子" -> R.drawable.zodiac_leo
    "处女" -> R.drawable.zodiac_virgo
    "天秤" -> R.drawable.zodiac_libra
    "天蝎" -> R.drawable.zodiac_scorpio
    "射手" -> R.drawable.zodiac_sagittarius
    "摩羯" -> R.drawable.zodiac_capricorn
    "水瓶" -> R.drawable.zodiac_aquarius
    "双鱼" -> R.drawable.zodiac_pisces
    else -> null
}

@Composable
private fun ConstellationBadge(
    westernZodiac: String?,
    modifier: Modifier = Modifier,
) {
    val display = westernZodiac?.removeSuffix("座")?.let { "${it}座" } ?: "待计算"
    Surface(
        modifier = modifier
            .size(46.dp)
            .semantics { contentDescription = "星座：$display" },
        shape = CircleShape,
        color = NanfengNavigation,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val iconRes = westernZodiac.constellationIconRes()
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = NanfengGold,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = NanfengGold,
                )
            }
            Text(
                display,
                modifier = Modifier.padding(top = 1.dp),
                color = NanfengGold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CaseSummaryRow(
    summary: CaseSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "打开命例：${summary.alias}" }
            .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
            .testTag("case_${summary.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    summary.name.value ?: summary.alias,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "  ${summary.sexForFortuneDirection.displayName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                summary.birthInput.displayDateOnly(),
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "别名：${summary.alias}",
                modifier = Modifier
                    .width(1.dp)
                    .height(1.dp),
                color = Color.Transparent,
            )
        }
        summary.fourPillars?.let { pillars ->
            Row(
                modifier = Modifier.width(104.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(pillars.year, pillars.month, pillars.day, pillars.hour).forEach { pillar ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        pillar.forEach { char ->
                            Text(
                                char.toString(),
                                color = baziElementColor(char),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        ConstellationBadge(
            westernZodiac = summary.westernZodiac,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseComparisonScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onSelectLeft: (String) -> Unit,
    onSelectRight: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_comparison_screen"),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("命例对比", fontWeight = FontWeight.SemiBold)
                    Text(
                        "只比较客观资料与版本化计算结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("back_from_case_comparison"),
                ) {
                    Text("返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("选择甲盘", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.comparisonCandidates.forEach { candidate ->
                    SelectionButton(
                        text = candidate.alias,
                        selected = candidate.id == state.comparisonLeftCaseId,
                        onClick = { onSelectLeft(candidate.id) },
                        tag = "comparison_left_${candidate.id}",
                    )
                }
            }
            Text("选择乙盘", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.comparisonCandidates.forEach { candidate ->
                    SelectionButton(
                        text = candidate.alias,
                        selected = candidate.id == state.comparisonRightCaseId,
                        onClick = { onSelectRight(candidate.id) },
                        tag = "comparison_right_${candidate.id}",
                    )
                }
            }
        }
        when {
            state.comparisonLoading -> LoadingBox("正在重建两个命例的对比档案…")
            state.comparisonError != null -> ErrorBox(
                message = state.comparisonError,
                actionLabel = "重新读取",
                onAction = onRetry,
            )
            state.comparisonReport != null -> CaseComparisonReportContent(
                report = state.comparisonReport,
            )
            else -> ErrorBox(
                message = "请选择两个不同的活动命例。",
                actionLabel = "重新读取",
                onAction = onRetry,
            )
        }
    }
}

@Composable
private fun CaseComparisonReportContent(
    report: CaseComparisonReport,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("case_comparison_report"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${report.leftAlias} ↔ ${report.rightAlias}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "相同 ${report.sameCount} · 不同 ${report.differentCount} · " +
                            "待补 ${report.missingCount}",
                        modifier = Modifier.testTag("case_comparison_summary"),
                    )
                    Text(
                        "结果仅描述字段异同，不生成吉凶、合婚或关系结论。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(
            items = report.sections,
            key = { it.title },
        ) { section ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(section.title, fontWeight = FontWeight.SemiBold)
                    section.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("comparison_row_${section.title}_${row.label}"),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(row.label, fontWeight = FontWeight.Medium)
                                Text(
                                    row.outcome.displayName(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (row.outcome) {
                                        CaseComparisonOutcome.SAME ->
                                            MaterialTheme.colorScheme.primary
                                        CaseComparisonOutcome.DIFFERENT ->
                                            MaterialTheme.colorScheme.error
                                        CaseComparisonOutcome.MISSING ->
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        report.leftAlias,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(row.leftValue)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        report.rightAlias,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(row.rightValue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun CaseComparisonOutcome.displayName(): String = when (this) {
    CaseComparisonOutcome.SAME -> "相同"
    CaseComparisonOutcome.DIFFERENT -> "不同"
    CaseComparisonOutcome.MISSING -> "待补"
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
    val previewableFields = state.reviewCandidates
        .flatMap(ScreenshotCandidateReviewUi::fields)
        .filter { field ->
            field.sourceImageRelativePath.isNotBlank() && field.evidenceBox != null
        }
    val activePreviewField = previewableFields.firstOrNull { it.id == previewFieldId }
        ?: previewableFields.firstOrNull()
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
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val expanded = LocalConfiguration.current.screenWidthDp >= 840 &&
                maxWidth >= EXPANDED_DETAIL_MIN_WIDTH
            Row(modifier = Modifier.fillMaxSize()) {
                if (expanded) {
                    ScreenshotImportEvidencePane(
                        field = activePreviewField,
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .testTag("expanded_screenshot_evidence_pane"),
                    )
                    VerticalDivider()
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("screenshot_review_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("screenshot_import_time_notice"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        FourPillarsLookupContract.CANDIDATE_NOTICE,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
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
    }
}

@Composable
private fun ScreenshotImportEvidencePane(
    field: ScreenshotFieldReviewUi?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("原始截图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (field == null) {
                Text(
                    "当前没有可定位的字段原图；右侧字段仍保留来源名称与证据状态。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "定位字段：${field.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ScreenshotEvidencePreview(field = field)
                Text(
                    "橙框只标示该字段的来源区域，不等同于算法真值。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    summary.westernZodiac
                        ?.removeSuffix("座")
                        ?.let { " · 星座${it}座" }
                        .orEmpty(),
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
    onConfirmFourPillarsLookup: (FourPillarsLookupSelection) -> Unit,
    onPrepareBirthPickerToday: () -> Unit,
    onOpenAlmanac: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WenzhenCreateCaseScreen(
        state = state,
        onFormChange = onFormChange,
        onPreview = onPreview,
        onSubmit = onSubmit,
        onConfirmDuplicate = onConfirmDuplicate,
        onConfirmFourPillarsLookup = onConfirmFourPillarsLookup,
        onPrepareBirthPickerToday = onPrepareBirthPickerToday,
        onOpenAlmanac = onOpenAlmanac,
        modifier = modifier,
    )
}

@Composable
private fun WenzhenCreateCaseScreen(
    state: StageTwoUiState,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onPreview: () -> Unit,
    onSubmit: () -> Unit,
    onConfirmDuplicate: () -> Unit,
    onConfirmFourPillarsLookup: (FourPillarsLookupSelection) -> Unit,
    onPrepareBirthPickerToday: () -> Unit,
    onOpenAlmanac: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var saveCase by rememberSaveable { mutableStateOf(true) }
    var showBirthPicker by rememberSaveable { mutableStateOf(false) }
    var birthPickerEntryMode by rememberSaveable { mutableStateOf(BirthPickerMode.SOLAR) }
    var showBirthplacePicker by rememberSaveable { mutableStateOf(false) }
    val form = state.form
    LaunchedEffect(showBirthPicker) {
        if (showBirthPicker) onPrepareBirthPickerToday()
    }
    LaunchedEffect(Unit) {
        if (form.sex == null && form.year.isBlank() && form.locationName.isBlank()) {
            val defaultBirthplace = BirthplaceCatalog.defaultBirthplace
            onFormChange {
                it.copy(
                    sex = SexForFortuneDirection.MAN,
                    year = "1990",
                    month = "1",
                    day = "1",
                    hour = "0",
                    minute = "0",
                    second = "0",
                    locationName = defaultBirthplace.displayName,
                    timeZoneId = defaultBirthplace.timeZoneId,
                    latitude = defaultBirthplace.latitude.toString(),
                    longitude = defaultBirthplace.longitude.toString(),
                ).clearTimeZoneResolution()
            }
        }
    }
    if (showBirthPicker) {
        BirthDateTimePickerSheet(
            form = form,
            onDismiss = { showBirthPicker = false },
            onConfirm = { selection ->
                onFormChange {
                    it.copy(
                        calendarSystem = if (selection.mode == BirthPickerMode.LUNAR) {
                            CalendarSystem.LUNAR
                        } else {
                            CalendarSystem.SOLAR
                        },
                        year = selection.year.toString(),
                        month = selection.month.toString(),
                        day = selection.day.toString(),
                        hour = selection.hour.toString(),
                        minute = selection.minute.toString(),
                        second = "0",
                        isLeapMonth = selection.isLeapMonth,
                    ).clearTimeZoneResolution()
                }
                showBirthPicker = false
            },
            onConfirmFourPillars = { selection ->
                showBirthPicker = false
                onConfirmFourPillarsLookup(selection)
            },
            fourPillarsCurrent = listOf(
                state.fourPillarsLookupForm.yearPillar,
                state.fourPillarsLookupForm.monthPillar,
                state.fourPillarsLookupForm.dayPillar,
                state.fourPillarsLookupForm.hourPillar,
            ),
            fourPillarsStartYear = state.fourPillarsLookupForm.startYear.toIntOrNull()
                ?: FourPillarsLookupContract.MIN_YEAR,
            fourPillarsEndYear = state.fourPillarsLookupForm.endYear.toIntOrNull()
                ?: FourPillarsLookupContract.MAX_YEAR,
            initialMode = birthPickerEntryMode,
            todaySnapshot = state.birthPickerTodaySnapshot,
        )
    }
    if (showBirthplacePicker) {
        BirthplacePickerSheet(
            form = form,
            onDismiss = { showBirthplacePicker = false },
            onConfirm = { place ->
                onFormChange {
                    it.copy(
                        locationName = place.displayName,
                        timeZoneId = place.timeZoneId,
                        latitude = place.latitude?.toString().orEmpty(),
                        longitude = place.longitude?.toString().orEmpty(),
                    ).clearTimeZoneResolution()
                }
                showBirthplacePicker = false
            },
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_case_screen"),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "首页排盘",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_input_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "姓名",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = NanfengInk,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (form.alias.isBlank()) {
                                Text(
                                    "请输入姓名或命例名",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                            BasicTextField(
                                value = form.alias,
                                onValueChange = { value -> onFormChange { it.copy(alias = value) } },
                                enabled = !state.saving,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = NanfengInk,
                                    textAlign = TextAlign.End,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("case_alias"),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeChoiceGroup(
                            options = listOf(
                                "男" to (form.sex == SexForFortuneDirection.MAN),
                                "女" to (form.sex == SexForFortuneDirection.WOMAN),
                            ),
                            onSelect = { label ->
                                onFormChange {
                                    it.copy(
                                        sex = if (label == "男") {
                                            SexForFortuneDirection.MAN
                                        } else {
                                            SexForFortuneDirection.WOMAN
                                        },
                                    )
                                }
                            },
                            tags = listOf("sex_man", "sex_woman"),
                        )
                        HomeChoiceGroup(
                            options = listOf(
                                "公历" to (form.calendarSystem == CalendarSystem.SOLAR),
                                "农历" to (form.calendarSystem == CalendarSystem.LUNAR),
                                "四柱" to false,
                            ),
                            onSelect = { label ->
                                when (label) {
                                    "四柱" -> {
                                        birthPickerEntryMode = BirthPickerMode.FOUR_PILLARS
                                        showBirthPicker = true
                                    }
                                    "公历" -> {
                                        birthPickerEntryMode = BirthPickerMode.SOLAR
                                        showBirthPicker = true
                                    }
                                    else -> {
                                        birthPickerEntryMode = BirthPickerMode.LUNAR
                                        showBirthPicker = true
                                    }
                                }
                            },
                            tags = listOf(
                                "birth_calendar_solar",
                                "birth_calendar_lunar",
                                "open_four_pillars_lookup",
                            ),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                    HomePickerRow(
                        title = "出生时间",
                        value = form.birthDateTimeDisplay(),
                        supporting = "",
                        onClick = {
                            birthPickerEntryMode = if (form.calendarSystem == CalendarSystem.LUNAR) {
                                BirthPickerMode.LUNAR
                            } else {
                                BirthPickerMode.SOLAR
                            }
                            showBirthPicker = true
                        },
                        tag = "open_birth_datetime_picker",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HomePickerRow(
                        title = "出生地区",
                        value = form.locationName.ifBlank { "请选择地区" },
                        supporting = "",
                        onClick = { showBirthplacePicker = true },
                        tag = "open_birthplace_picker",
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "保存命例",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = NanfengInk,
                        )
                        Switch(
                            checked = saveCase,
                            onCheckedChange = { saveCase = it },
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = 0.78f
                                    scaleY = 0.78f
                                }
                                .semantics { contentDescription = "保存命例" },
                        )
                    }
                    Button(
                        onClick = if (saveCase) onSubmit else onPreview,
                        enabled = !state.saving && !state.previewing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(50.dp)
                            .testTag(if (saveCase) "save_case" else "preview_case"),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = NanfengNavigation,
                            contentColor = Color(0xFFF2D8A5),
                        ),
                        shape = RoundedCornerShape(25.dp),
                    ) {
                        Text(
                            when {
                                state.saving -> "正在排盘并保存…"
                                state.previewing -> "正在即时排盘…"
                                else -> "开始排盘"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            AlmanacHomeEntry(onClick = onOpenAlmanac)
            state.formError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
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
            if (state.instantCalculation != null) {
                InstantCalculationPreviewCard(
                    calculation = state.instantCalculation,
                    sex = form.sex ?: state.instantCalculation.normalizedInput.sexForFortuneDirection,
                )
            }
            if (state.duplicateCandidates.isNotEmpty()) {
                DuplicateCandidatesCard(
                    candidates = state.duplicateCandidates,
                    saving = state.saving || state.previewing,
                    onConfirm = onConfirmDuplicate,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeChoiceGroup(
    options: List<Pair<String, Boolean>>,
    onSelect: (String) -> Unit,
    tags: List<String>,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NanfengPageBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row {
            options.forEachIndexed { index, (label, selected) ->
                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .widthIn(min = 44.dp)
                        .clickable { onSelect(label) }
                        .testTag(tags[index]),
                    shape = RoundedCornerShape(19.dp),
                    color = if (selected) NanfengGreen else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomePickerRow(
    title: String,
    value: String,
    supporting: String,
    onClick: () -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$title，$value" }
            .padding(horizontal = 2.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            modifier = Modifier.width(68.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = NanfengInk,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = NanfengInk,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting.isNotBlank()) {
                Text(
                    supporting,
                    modifier = Modifier.padding(top = 1.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun CaseFormState.birthDateTimeDisplay(): String {
    val date = listOf(year, month.padStart(2, '0'), day.padStart(2, '0')).joinToString("-")
    val time = "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}"
    return if (year.isBlank() || month.isBlank() || day.isBlank()) {
        "请选择出生时间"
    } else {
        "$date  $time"
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
    showBack: Boolean = true,
) {
    var showBirthPicker by rememberSaveable { mutableStateOf(false) }
    var showBirthplacePicker by rememberSaveable { mutableStateOf(false) }
    if (showBirthPicker) {
        BirthDateTimePickerSheet(
            form = form,
            onDismiss = { showBirthPicker = false },
            onConfirm = { selection ->
                onFormChange {
                    it.copy(
                        calendarSystem = if (selection.mode == BirthPickerMode.LUNAR) {
                            CalendarSystem.LUNAR
                        } else {
                            CalendarSystem.SOLAR
                        },
                        year = selection.year.toString(),
                        month = selection.month.toString(),
                        day = selection.day.toString(),
                        hour = selection.hour.toString(),
                        minute = selection.minute.toString(),
                        second = "0",
                        isLeapMonth = selection.isLeapMonth,
                    ).clearTimeZoneResolution()
                }
                showBirthPicker = false
            },
            showFourPillarsOption = false,
        )
    }
    if (showBirthplacePicker) {
        BirthplacePickerSheet(
            form = form,
            onDismiss = { showBirthplacePicker = false },
            onConfirm = { place ->
                onFormChange {
                    it.copy(
                        locationName = place.displayName,
                        timeZoneId = place.timeZoneId,
                        latitude = place.latitude?.toString().orEmpty(),
                        longitude = place.longitude?.toString().orEmpty(),
                    ).clearTimeZoneResolution()
                }
                showBirthplacePicker = false
            },
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(screenTag),
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (showBack) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("返回")
                    }
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
                "",
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                HomePickerRow(
                    title = if (form.calendarSystem == CalendarSystem.LUNAR) "农历出生时间" else "公历出生时间",
                    value = form.birthDateTimeDisplay(),
                    supporting = "",
                    onClick = { if (!saving) showBirthPicker = true },
                    tag = "open_birth_datetime_picker",
                )
            }
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
                "",
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                HomePickerRow(
                    title = "出生地区",
                    value = form.locationName.ifBlank { "请选择地区" },
                    supporting = "",
                    onClick = { if (!saving) showBirthplacePicker = true },
                    tag = "open_birthplace_picker",
                )
            }
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

private fun String.evidenceFieldLabel(): String {
    WenzhenSourceFidelityContract.definitionFor(this)?.let { return it.displayLabel }
    return when (this) {
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
private fun CaseObjectiveSummaryScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("objective_summary_screen"),
    ) {
        TopAppBar(
            title = { Text(state.objectiveSummary?.title ?: "客观命盘摘要") },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("返回")
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.objectiveSummaryLoading ->
                    LoadingBox("正在读取已采用快照并生成客观摘要…")
                state.objectiveSummary == null && state.objectiveSummaryFailure != null ->
                    ErrorBox(
                        message = "${state.objectiveSummaryFailure.message}" +
                            "（${state.objectiveSummaryFailure.code}）",
                        actionLabel = "重试",
                        onAction = onRetry,
                    )
                state.objectiveSummary != null -> ObjectiveSummaryContent(
                    summary = state.objectiveSummary,
                    copied = state.objectiveSummaryCopied,
                    copyFailure = state.objectiveSummaryFailure,
                    onCopy = onCopy,
                )
                else -> ErrorBox(
                    message = "客观摘要尚未生成（SUMMARY_UNAVAILABLE）。",
                    actionLabel = "重试",
                    onAction = onRetry,
                )
            }
        }
    }
}

@Composable
private fun ObjectiveSummaryContent(
    summary: CaseObjectiveSummary,
    copied: Boolean,
    copyFailure: com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryFailure?,
    onCopy: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("objective_summary_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("objective_summary_notice"),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "客观摘要 v${summary.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        summary.provenanceNotice,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        summary.interpretationNotice,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Button(
                onClick = onCopy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("copy_objective_summary_button"),
            ) {
                Text(if (copied) "已复制客观摘要" else "复制客观摘要")
            }
        }
        if (copyFailure != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("objective_summary_copy_error"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        "${copyFailure.message}（${copyFailure.code}）",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        summary.sections.forEach { section ->
            item(key = section.id) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("objective_summary_section_${section.id}"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        section.fields.forEachIndexed { index, field ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            }
                            Text(
                                field.label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                field.value,
                                modifier = Modifier.padding(top = 3.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "来源：${field.source.displayName()}",
                                modifier = Modifier.padding(top = 3.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MasterCommentaryCandidateScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onContentChange: (String, String) -> Unit,
    onCategoryChange: (String, AnalysisCategory) -> Unit,
    onReject: (String) -> Unit,
    onRestoreRejected: (String) -> Unit,
    onAdopt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("master_commentary_candidates_screen"),
    ) {
        TopAppBar(
            title = { Text("师傅点评观点候选") },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("返回")
                }
            },
        )
        val candidateSet = state.commentaryCandidateSet
        when {
            candidateSet == null && state.commentaryCandidateFailure != null -> ErrorBox(
                message = "${state.commentaryCandidateFailure.message}" +
                    "（${state.commentaryCandidateFailure.code}）",
                actionLabel = "返回详情",
                onAction = onBack,
            )
            candidateSet == null && state.commentaryCandidateAdoptionFailure != null -> ErrorBox(
                message = "${state.commentaryCandidateAdoptionFailure.message}" +
                    "（${state.commentaryCandidateAdoptionFailure.code}）",
                actionLabel = "返回详情",
                onAction = onBack,
            )
            candidateSet == null -> LoadingBox("正在提取可定位的点评句段…")
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("master_commentary_candidates_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_commentary_candidates_notice"),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "本地确定性候选 · 规则 v${candidateSet.ruleVersion}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "共 ${candidateSet.candidates.size} 条，来源点评版本 " +
                                    "v${candidateSet.sourceRevision}。候选只用于人工整理，" +
                                    "不代表观点正确，也不是本机排盘算法结论。",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "只有逐条采用才会新增正式分析记录；编辑、拒绝和采用都不会" +
                                    "覆盖完整师傅点评原文。",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                state.commentaryCandidateAdoptionFailure?.let { failure ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("commentary_candidate_error"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Text(
                                "${failure.message}（${failure.code}）",
                                modifier = Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                items(
                    items = candidateSet.candidates,
                    key = { it.id },
                ) { candidate ->
                    MasterCommentaryCandidateCard(
                        candidate = candidate,
                        saving = state.commentaryCandidateSavingId == candidate.id,
                        anySaving = state.commentaryCandidateSavingId != null ||
                            state.detailLoading ||
                            state.detail == null,
                        onContentChange = { onContentChange(candidate.id, it) },
                        onCategoryChange = { onCategoryChange(candidate.id, it) },
                        onReject = { onReject(candidate.id) },
                        onRestoreRejected = { onRestoreRejected(candidate.id) },
                        onAdopt = { onAdopt(candidate.id) },
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MasterCommentaryCandidateCard(
    candidate: MasterCommentaryCandidate,
    saving: Boolean,
    anySaving: Boolean,
    onContentChange: (String) -> Unit,
    onCategoryChange: (AnalysisCategory) -> Unit,
    onReject: () -> Unit,
    onRestoreRejected: () -> Unit,
    onAdopt: () -> Unit,
) {
    val pending = candidate.status == MasterCommentaryCandidateStatus.PENDING
    val statusText = when (candidate.status) {
        MasterCommentaryCandidateStatus.PENDING -> "待确认"
        MasterCommentaryCandidateStatus.ADOPTED -> "已采用为正式分析"
        MasterCommentaryCandidateStatus.REJECTED -> "已拒绝"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = statusText }
            .testTag("commentary_candidate_card"),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                statusText,
                modifier = Modifier.testTag(
                    "commentary_candidate_status_${candidate.status.name}",
                ),
                style = MaterialTheme.typography.labelLarge,
                color = when (candidate.status) {
                    MasterCommentaryCandidateStatus.PENDING ->
                        MaterialTheme.colorScheme.primary
                    MasterCommentaryCandidateStatus.ADOPTED -> Color(0xFF2E7D32)
                    MasterCommentaryCandidateStatus.REJECTED ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                "原文片段 [${candidate.sourceRange.startInclusive}, " +
                    "${candidate.sourceRange.endExclusive})",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                candidate.sourceExcerpt,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("commentary_candidate_source"),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = candidate.proposedContent,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("commentary_candidate_content"),
                label = { Text("采用内容") },
                enabled = pending && !anySaving,
                minLines = 2,
            )
            Text(
                "分类建议（可修改）",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnalysisCategory.entries.forEach { category ->
                    val categoryModifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("commentary_category_${category.name}")
                    if (candidate.proposedCategory == category) {
                        Button(
                            onClick = { onCategoryChange(category) },
                            enabled = pending && !anySaving,
                            modifier = categoryModifier,
                        ) {
                            Text(category.displayName())
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onCategoryChange(category) },
                            enabled = pending && !anySaving,
                            modifier = categoryModifier,
                        ) {
                            Text(category.displayName())
                        }
                    }
                }
            }
            Text(
                candidate.ruleEvidence.joinToString("；") { evidence ->
                    buildString {
                        append(evidence.explanation)
                        if (evidence.matchedTerms.isNotEmpty()) {
                            append(" 命中：")
                            append(evidence.matchedTerms.joinToString("、"))
                        }
                    }
                },
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (candidate.status) {
                MasterCommentaryCandidateStatus.PENDING -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !anySaving,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reject_commentary_candidate"),
                    ) {
                        Text("拒绝")
                    }
                    Button(
                        onClick = onAdopt,
                        enabled = !anySaving && candidate.proposedContent.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("adopt_commentary_candidate"),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("采用")
                        }
                    }
                }
                MasterCommentaryCandidateStatus.REJECTED -> OutlinedButton(
                    onClick = onRestoreRejected,
                    enabled = !anySaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .heightIn(min = 48.dp)
                        .testTag("restore_commentary_candidate"),
                ) {
                    Text("恢复为待确认")
                }
                MasterCommentaryCandidateStatus.ADOPTED -> Text(
                    "正式分析已写入；如需修改，请在详情的分析记录中编辑。",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackThemeCandidateScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onTagChange: (String, String) -> Unit,
    onReject: (String) -> Unit,
    onRestoreRejected: (String) -> Unit,
    onAdopt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("feedback_theme_candidates_screen"),
    ) {
        TopAppBar(
            title = { Text("命主反馈主题候选") },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("返回")
                }
            },
        )
        val candidateSet = state.feedbackThemeCandidateSet
        when {
            candidateSet == null && state.feedbackThemeCandidateFailure != null -> ErrorBox(
                message = "${state.feedbackThemeCandidateFailure.message}" +
                    "（${state.feedbackThemeCandidateFailure.code}）",
                actionLabel = "返回详情",
                onAction = onBack,
            )
            candidateSet == null && state.feedbackThemeAdoptionFailure != null -> ErrorBox(
                message = "${state.feedbackThemeAdoptionFailure.message}" +
                    "（${state.feedbackThemeAdoptionFailure.code}）",
                actionLabel = "返回详情",
                onAction = onBack,
            )
            candidateSet == null -> LoadingBox("正在提取可定位的反馈主题…")
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("feedback_theme_candidates_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_theme_candidates_notice"),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "本地确定性候选 · 规则 v${candidateSet.ruleVersion}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "共 ${candidateSet.candidates.size} 个主题，来源反馈版本 " +
                                    "v${candidateSet.sourceRevision}。候选只是标签建议，" +
                                    "不代表用户确认，也不是本机排盘算法真值。",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "只有逐条采用才会给命例新增正式标签；编辑、拒绝和采用都不会" +
                                    "覆盖完整命主反馈、历史版本或既有事件。",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                state.feedbackThemeAdoptionFailure?.let { failure ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_theme_candidate_error"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Text(
                                "${failure.message}（${failure.code}）",
                                modifier = Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                items(
                    items = candidateSet.candidates,
                    key = { it.id },
                ) { candidate ->
                    FeedbackThemeCandidateCard(
                        candidate = candidate,
                        saving = state.feedbackThemeSavingId == candidate.id,
                        anySaving = state.feedbackThemeSavingId != null ||
                            state.detailLoading ||
                            state.detail == null,
                        onTagChange = { onTagChange(candidate.id, it) },
                        onReject = { onReject(candidate.id) },
                        onRestoreRejected = { onRestoreRejected(candidate.id) },
                        onAdopt = { onAdopt(candidate.id) },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FeedbackThemeCandidateCard(
    candidate: FeedbackThemeCandidate,
    saving: Boolean,
    anySaving: Boolean,
    onTagChange: (String) -> Unit,
    onReject: () -> Unit,
    onRestoreRejected: () -> Unit,
    onAdopt: () -> Unit,
) {
    val pending = candidate.status == FeedbackThemeCandidateStatus.PENDING
    val statusText = when (candidate.status) {
        FeedbackThemeCandidateStatus.PENDING -> "待确认"
        FeedbackThemeCandidateStatus.ADOPTED -> "已采用为正式标签"
        FeedbackThemeCandidateStatus.REJECTED -> "已拒绝"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = statusText }
            .testTag("feedback_theme_candidate_card"),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                statusText,
                modifier = Modifier.testTag(
                    "feedback_theme_status_${candidate.status.name}",
                ),
                style = MaterialTheme.typography.labelLarge,
                color = when (candidate.status) {
                    FeedbackThemeCandidateStatus.PENDING -> MaterialTheme.colorScheme.primary
                    FeedbackThemeCandidateStatus.ADOPTED -> Color(0xFF2E7D32)
                    FeedbackThemeCandidateStatus.REJECTED ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                "规范主题：${candidate.canonicalTagName} · 事件分类建议：" +
                    candidate.suggestedEventCategory.displayName(),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = candidate.proposedTagName,
                onValueChange = onTagChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("feedback_theme_tag_input"),
                label = { Text("采用标签（可修改）") },
                enabled = pending && !anySaving,
                singleLine = true,
            )
            candidate.sourceEvidence.forEach { evidence ->
                Text(
                    "来源片段 [${evidence.range.startInclusive}, " +
                        "${evidence.range.endExclusive})",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    evidence.excerpt,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .testTag("feedback_theme_source_evidence"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "命中：${evidence.matchedTerms.joinToString("、")}",
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${candidate.ruleExplanation}（${candidate.ruleId}）",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (candidate.status) {
                FeedbackThemeCandidateStatus.PENDING -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !anySaving,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reject_feedback_theme_candidate"),
                    ) {
                        Text("拒绝")
                    }
                    Button(
                        onClick = onAdopt,
                        enabled = !anySaving && candidate.proposedTagName.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("adopt_feedback_theme_candidate"),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("采用")
                        }
                    }
                }
                FeedbackThemeCandidateStatus.REJECTED -> OutlinedButton(
                    onClick = onRestoreRejected,
                    enabled = !anySaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .heightIn(min = 48.dp)
                        .testTag("restore_feedback_theme_candidate"),
                ) {
                    Text("恢复为待确认")
                }
                FeedbackThemeCandidateStatus.ADOPTED -> Text(
                    "正式标签已写入；完整反馈原文和事件均保持不变。",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    onOpenCommentaryCandidates: (String) -> Unit,
    onOpenFeedbackThemeCandidates: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onOwnerFeedbackChange: (String) -> Unit,
    onMasterCommentaryChange: (String) -> Unit,
    onAddNotesTimeline: (CaseEventTimelineLevel, Int, String) -> Unit,
    onNotesTimelineContentChange: (String, String) -> Unit,
    onSaveCaseNotes: () -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onOpenObjectiveSummary: () -> Unit,
    onOpenExternalAnalysis: () -> Unit,
    onExportCaseImage: () -> Unit,
    onShareCaseImage: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    onSelectSection: (CaseDetailSection) -> Unit,
    onFortuneObservationDateChange: (String) -> Unit,
    onFortuneObservationTimeChange: (String) -> Unit,
    onFortuneObservationSelect: (ProfessionalFortuneSelection) -> Unit,
    onFortuneToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var managementMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_detail_screen"),
    ) {
        TopAppBar(
            title = {
                Text(
                    "南枫八字",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            navigationIcon = {
                androidx.compose.material3.IconButton(
                    onClick = onBack,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                Box {
                    androidx.compose.material3.IconButton(
                        onClick = { managementMenuExpanded = true },
                        modifier = Modifier.testTag("toggle_case_management"),
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "管理命例")
                    }
                    DropdownMenu(
                        expanded = managementMenuExpanded,
                        onDismissRequest = { managementMenuExpanded = false },
                    ) {
                        fun closeThen(action: () -> Unit) {
                            managementMenuExpanded = false
                            action()
                        }
                        DropdownMenuItem(
                            text = { Text("编辑基本资料") },
                            onClick = { closeThen(onEditCase) },
                            modifier = Modifier.testTag("edit_case_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("分组与标签") },
                            onClick = { closeThen(onEditMetadata) },
                            modifier = Modifier.testTag("edit_metadata_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("管理出生时间候选") },
                            onClick = { closeThen(onAddBirthTimeCandidate) },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("客观命盘摘要") },
                            onClick = { closeThen(onOpenObjectiveSummary) },
                        )
                        DropdownMenuItem(
                            text = { Text("外部分析桥接") },
                            onClick = { closeThen(onOpenExternalAnalysis) },
                        )
                        DropdownMenuItem(
                            text = { Text("导出单命例") },
                            onClick = { closeThen(onExportSingleCase) },
                            enabled = !state.singleCaseExchangeBusy,
                            modifier = Modifier.testTag("export_single_case_button"),
                        )
                        DropdownMenuItem(
                            text = { Text("导出命盘长图") },
                            onClick = { closeThen(onExportCaseImage) },
                            enabled = !state.caseImageBusy,
                        )
                        DropdownMenuItem(
                            text = { Text("分享命盘长图") },
                            onClick = { closeThen(onShareCaseImage) },
                            enabled = !state.caseImageBusy,
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("复制命例") },
                            onClick = { closeThen(onDuplicate) },
                        )
                        if (state.detail?.deletedAt == null) {
                            DropdownMenuItem(
                                text = { Text("移入回收站", color = MaterialTheme.colorScheme.error) },
                                onClick = { closeThen(onMoveToTrash) },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("恢复命例") },
                                onClick = { closeThen(onRestore) },
                            )
                        }
                    }
                }
            },
        )
        if (state.detail != null && !state.detailLoading && state.detailError == null) {
            CaseDetailTabs(
                selectedSection = state.detailSection,
                onSelectSection = onSelectSection,
            )
            WenzhenCaseIdentityHeader(
                case = state.detail,
                adopted = state.detail.calculationSnapshots.asReversed()
                    .firstOrNull { it.adopted },
                section = state.detailSection,
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
                state.detail != null -> ReferenceCaseDetailContent(
                    case = state.detail,
                    onEditCase = onEditCase,
                    onAddBirthTimeCandidate = onAddBirthTimeCandidate,
                    onAdoptBirthTimeCandidate = onAdoptBirthTimeCandidate,
                    onAddRecord = onAddRecord,
                    onEditRecord = onEditRecord,
                    onOpenCommentaryCandidates = onOpenCommentaryCandidates,
                    onOpenFeedbackThemeCandidates = onOpenFeedbackThemeCandidates,
                    onAddEvent = onAddEvent,
                    onEditEvent = onEditEvent,
                    caseNotesDraft = state.caseNotesDraft,
                    caseNotesSaving = state.caseNotesSaving,
                    caseNotesSaveError = state.caseNotesSaveError,
                    caseNotesSaved = state.caseNotesDraft == state.caseNotesSavedDraft,
                    onOwnerFeedbackChange = onOwnerFeedbackChange,
                    onMasterCommentaryChange = onMasterCommentaryChange,
                    onAddNotesTimeline = onAddNotesTimeline,
                    onNotesTimelineContentChange = onNotesTimelineContentChange,
                    onSaveCaseNotes = onSaveCaseNotes,
                    selectedSection = state.detailSection,
                    mutationSaving = state.mutationSaving,
                    mutationError = state.mutationError,
                    fortuneObservationDate = state.fortuneObservationDate,
                    fortuneObservationTime = state.fortuneObservationTime,
                    fortunePosition = state.fortunePosition,
                    professionalFortunePosition = state.professionalFortunePosition,
                    fortunePositionError = state.fortunePositionError,
                    onFortuneObservationDateChange = onFortuneObservationDateChange,
                    onFortuneObservationTimeChange = onFortuneObservationTimeChange,
                    onFortuneObservationSelect = onFortuneObservationSelect,
                    onFortuneToday = onFortuneToday,
                )
            }
        }
    }
}

private fun CaseDetailSection.displayName(): String = when (this) {
    CaseDetailSection.BASIC_INFO -> "基本信息"
    CaseDetailSection.BASIC_CHART -> "基本排盘"
    CaseDetailSection.FORTUNE -> "专业细盘"
    CaseDetailSection.RECORDS -> "断事笔记"
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
    TabRow(
        selectedTabIndex = CaseDetailSection.entries.indexOf(selectedSection),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("case_detail_tabs"),
        containerColor = NanfengNavigation,
        contentColor = NanfengGold,
        divider = {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        },
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(
                    tabPositions[CaseDetailSection.entries.indexOf(selectedSection)],
                ),
                color = NanfengGold,
            )
        },
    ) {
        CaseDetailSection.entries.forEach { section ->
            Tab(
                selected = selectedSection == section,
                onClick = { onSelectSection(section) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(section.testTag()),
                text = {
                    Text(
                        section.displayName(),
                        color = if (selectedSection == section) {
                            NanfengGoldLight
                        } else {
                            Color.White.copy(alpha = 0.82f)
                        },
                        fontWeight = if (selectedSection == section) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun WenzhenCaseIdentityHeader(
    case: BaziCase,
    adopted: CaseCalculationSnapshot?,
    section: CaseDetailSection,
) {
    val result = adopted?.result
    val westernZodiac = result?.basicChartDetails?.westernZodiac
    val iconRes = westernZodiac.constellationIconRes()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("case_identity_header"),
        color = NanfengNavigation,
        shape = RoundedCornerShape(0.dp),
    ) {
        when (section) {
            CaseDetailSection.BASIC_INFO -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ZodiacIdentityBadge(
                        westernZodiac = westernZodiac,
                        iconRes = iconRes,
                        size = 66.dp,
                        iconSize = 25.dp,
                    )
                    Text(
                        case.name.value ?: case.alias,
                        modifier = Modifier.padding(top = 7.dp),
                        color = NanfengGoldLight,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            CaseDetailSection.BASIC_CHART,
            CaseDetailSection.FORTUNE,
            -> {
                val solar = result?.calendarConversion?.solarDateTime
                val lunar = result?.calendarConversion?.lunarDateTime
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ZodiacIdentityBadge(
                        westernZodiac = westernZodiac,
                        iconRes = iconRes,
                        size = 46.dp,
                        iconSize = 17.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            case.name.value ?: case.alias,
                            color = NanfengGoldLight,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            solar?.let {
                                "公历 %04d年%02d月%02d日 %02d:%02d:%02d".format(
                                    it.year,
                                    it.month,
                                    it.day,
                                    it.hour,
                                    it.minute,
                                    it.second,
                                )
                            } ?: case.birthInput.displayDateTime(),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .testTag("shared_identity_solar_time"),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            lunar?.let {
                                "农历 ${it.toTraditionalChineseText()}"
                            } ?: "农历 暂无换算结果",
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .testTag("shared_identity_lunar_time"),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        case.sexForFortuneDirection.chartTypeName(),
                        modifier = Modifier
                            .width(48.dp)
                            .padding(end = 18.dp)
                            .testTag("shared_identity_chart_type"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            CaseDetailSection.RECORDS -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("notes_identity_header"),
                ) {
                    val pillars = result?.fourPillars?.let {
                        listOf(it.year, it.month, it.day, it.hour)
                    }.orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            case.sexForFortuneDirection.chartTypeName(),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = (-122).dp)
                                .testTag("notes_identity_chart_type"),
                            color = NanfengGoldLight,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (pillars.isEmpty()) {
                            Text(
                                "暂无已采用排盘",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Row(
                                modifier = Modifier.testTag("notes_identity_four_pillars"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                pillars.forEachIndexed { index, pillar ->
                                    Column(
                                        modifier = Modifier.width(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            pillar.take(1),
                                            modifier = Modifier.testTag("notes_identity_stem_$index"),
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            pillar.drop(1).take(1),
                                            modifier = Modifier.testTag("notes_identity_branch_$index"),
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 2.dp, bottom = 7.dp),
                        color = Color.White.copy(alpha = 0.12f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "大运",
                            modifier = Modifier.width(34.dp),
                            color = NanfengGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        result?.decadeFortunes?.take(10)?.forEachIndexed { index, decade ->
                            Text(
                                decade.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("notes_decade_$index"),
                                color = Color.White,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZodiacIdentityBadge(
    westernZodiac: String?,
    iconRes: Int?,
    size: Dp,
    iconSize: Dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(2.dp, NanfengGold),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = NanfengGold,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = NanfengGold,
                )
            }
            Text(
                westernZodiac?.let { if (it.endsWith("座")) it else "${it}座" } ?: "待计算",
                color = NanfengGold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

private enum class CaseNotesMode {
    OWNER_FEEDBACK,
    MASTER_COMMENTARY,
}

@Composable
private fun ReferenceCaseDetailContent(
    case: BaziCase,
    onEditCase: () -> Unit,
    onAddBirthTimeCandidate: () -> Unit,
    onAdoptBirthTimeCandidate: (String) -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onOpenCommentaryCandidates: (String) -> Unit,
    onOpenFeedbackThemeCandidates: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    caseNotesDraft: CaseNotesDraft,
    caseNotesSaving: Boolean,
    caseNotesSaveError: String?,
    caseNotesSaved: Boolean,
    onOwnerFeedbackChange: (String) -> Unit,
    onMasterCommentaryChange: (String) -> Unit,
    onAddNotesTimeline: (CaseEventTimelineLevel, Int, String) -> Unit,
    onNotesTimelineContentChange: (String, String) -> Unit,
    onSaveCaseNotes: () -> Unit,
    selectedSection: CaseDetailSection,
    mutationSaving: Boolean,
    mutationError: String?,
    fortuneObservationDate: String,
    fortuneObservationTime: String,
    fortunePosition: FortunePosition?,
    professionalFortunePosition: ProfessionalFortunePosition?,
    fortunePositionError: String?,
    onFortuneObservationDateChange: (String) -> Unit,
    onFortuneObservationTimeChange: (String) -> Unit,
    onFortuneObservationSelect: (ProfessionalFortuneSelection) -> Unit,
    onFortuneToday: () -> Unit,
) {
    val adopted = case.calculationSnapshots.asReversed().firstOrNull { it.adopted }
    var showObservationPicker by rememberSaveable(case.id) { mutableStateOf(false) }
    var notesMode by rememberSaveable(case.id) {
        mutableStateOf(
            if (
                case.textRecords.any { it.type == CaseTextRecordType.MASTER_COMMENTARY } &&
                case.textRecords.none { it.type == CaseTextRecordType.OWNER_FEEDBACK }
            ) {
                CaseNotesMode.MASTER_COMMENTARY
            } else {
                CaseNotesMode.OWNER_FEEDBACK
            },
        )
    }
    if (showObservationPicker) {
        ObservationDateTimePickerSheet(
            currentDate = fortuneObservationDate,
            currentTime = fortuneObservationTime,
            onDismiss = { showObservationPicker = false },
            onConfirm = { date, time ->
                onFortuneObservationDateChange(date)
                onFortuneObservationTimeChange(time)
                showObservationPicker = false
            },
        )
    }
    val wide = LocalConfiguration.current.screenWidthDp >= 840
    if (selectedSection == CaseDetailSection.RECORDS) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .testTag("case_notes_layout")
                .padding(
                    start = if (wide) 28.dp else 10.dp,
                    top = 6.dp,
                    end = if (wide) 28.dp else 10.dp,
                    bottom = 6.dp,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
            ) {
                ReferenceCaseNotes(
                    case = case,
                    adopted = adopted,
                    mode = notesMode,
                    onModeChange = { notesMode = it },
                    onEditRecord = onEditRecord,
                    draft = caseNotesDraft,
                    onOwnerFeedbackChange = onOwnerFeedbackChange,
                    onMasterCommentaryChange = onMasterCommentaryChange,
                    onAddTimeline = onAddNotesTimeline,
                    onTimelineContentChange = onNotesTimelineContentChange,
                )
            }
            CaseNotesSaveFooter(
                case = case,
                saving = caseNotesSaving,
                saveError = caseNotesSaveError,
                saved = caseNotesSaved,
                onSave = onSaveCaseNotes,
            )
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                color = if (selectedSection == CaseDetailSection.FORTUNE) {
                    NanfengPageBackground
                } else {
                    Color.White
                },
                shape = if (selectedSection == CaseDetailSection.BASIC_INFO) {
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                } else {
                    RoundedCornerShape(0.dp)
                },
            )
            .padding(
                horizontal = when {
                    selectedSection == CaseDetailSection.FORTUNE -> 0.dp
                    wide -> 28.dp
                    else -> 10.dp
                },
                vertical = when (selectedSection) {
                    CaseDetailSection.BASIC_INFO -> 8.dp
                    CaseDetailSection.FORTUNE -> 0.dp
                    else -> 6.dp
                },
            ),
    ) {
        when (selectedSection) {
            CaseDetailSection.BASIC_INFO -> ReferenceBasicInfo(
                case = case,
                adopted = adopted,
                onEditCase = onEditCase,
                onAddBirthTimeCandidate = onAddBirthTimeCandidate,
                onAdoptBirthTimeCandidate = onAdoptBirthTimeCandidate,
                mutationSaving = mutationSaving,
                mutationError = mutationError,
            )

            CaseDetailSection.BASIC_CHART -> ReferenceBasicChart(
                case = case,
                adopted = adopted,
            )

            CaseDetailSection.FORTUNE -> {
                if (adopted == null) {
                    ReferenceEmptyText("当前命例没有已采用的计算快照。")
                } else {
                    FortuneDetailsView(
                        calculation = adopted.result,
                        fortuneObservationDate = fortuneObservationDate,
                        fortuneObservationTime = fortuneObservationTime,
                        fortunePosition = fortunePosition,
                        professionalFortunePosition = professionalFortunePosition,
                        fortunePositionError = fortunePositionError,
                        onOpenObservationPicker = { showObservationPicker = true },
                        onObservationSelect = onFortuneObservationSelect,
                        onToday = onFortuneToday,
                    )
                }
            }

            CaseDetailSection.RECORDS -> Unit
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ReferenceBasicInfo(
    case: BaziCase,
    adopted: CaseCalculationSnapshot?,
    onEditCase: () -> Unit,
    onAddBirthTimeCandidate: () -> Unit,
    onAdoptBirthTimeCandidate: (String) -> Unit,
    mutationSaving: Boolean,
    mutationError: String?,
) {
    var alternateRow = false
    fun nextAlternate(): Boolean = alternateRow.also { alternateRow = !alternateRow }

    val result = adopted?.result
    val conversion = result?.calendarConversion
    val solarText = conversion?.solarDateTime?.display() ?: case.birthInput.displayDateTime()
    val lunarText = conversion?.lunarDateTime?.let { lunar ->
        "${lunar.year}年${if (lunar.isLeapMonth) "闰" else ""}${lunar.month}月${lunar.day}日 " +
            "%02d:%02d:%02d".format(lunar.hour, lunar.minute, lunar.second)
    } ?: "暂无换算结果"
    WenzhenDualFactRow(
        leftLabel = "姓名",
        leftValue = case.name.value ?: case.alias,
        rightLabel = "性别",
        rightValue = case.sexForFortuneDirection.displayName(),
        alternate = nextAlternate(),
        rightTag = "basic_info_sex_fact",
    )
    WenzhenFactRow("农历", lunarText, alternate = nextAlternate())
    WenzhenFactRow("阳历", solarText.removePrefix("公历 "), alternate = nextAlternate())
    result?.trueSolarTimeEvidence?.let { evidence ->
        WenzhenFactRow(
            "真太阳时",
            evidence.trueSolarDateTime.display(),
            alternate = nextAlternate(),
        )
    }
    WenzhenFactRow(
        "出生地区",
        case.birthInput.locationName ?: "未提供",
        alternate = nextAlternate(),
        tag = "basic_info_birthplace_row",
    )
    result?.basicChartDetails?.let { basic ->
        WenzhenFactRow(
            "前一节气",
            "${basic.previousSolarTerm.name} ${basic.previousSolarTerm.at.display()}",
            alternate = nextAlternate(),
            tag = "basic_info_previous_term_row",
        )
        WenzhenFactRow(
            "后一节气",
            "${basic.nextSolarTerm.name} ${basic.nextSolarTerm.at.display()}",
            alternate = nextAlternate(),
            tag = "basic_info_next_term_row",
        )
        WenzhenDualFactRow(
            leftLabel = "生肖",
            leftValue = basic.zodiac,
            rightLabel = "星座",
            rightValue = if (basic.westernZodiac.endsWith("座")) {
                basic.westernZodiac
            } else {
                "${basic.westernZodiac}座"
            },
            alternate = nextAlternate(),
            rightTag = "basic_info_zodiac_fact",
        )
    }
    if (result != null) {
        WenzhenDualFactRow(
            "胎元",
            result.fetalOrigin,
            "胎息",
            result.fetalBreath,
            nextAlternate(),
            rightTag = "basic_info_fetal_breath_fact",
        )
        WenzhenDualFactRow(
            "命宫",
            result.ownSign,
            "身宫",
            result.bodySign,
            nextAlternate(),
        )
        WenzhenSectionHeader(
            "命盘摘要",
            modifier = Modifier.padding(top = 14.dp),
        )
        WenzhenFactRow("四柱", result.fourPillars.display(), alternate = nextAlternate())
        WenzhenDualFactRow(
            "日主",
            result.basicChartDetails?.dayMaster ?: "暂无",
            "起运方向",
            if (result.fortuneStart.direction.name == "FORWARD") "顺排" else "逆排",
            alternate = nextAlternate(),
        )
        WenzhenDualFactRow(
            "起运年龄",
            "${result.fortuneStart.years}年${result.fortuneStart.months}月" +
                "${result.fortuneStart.days}日",
            "交运年份",
            result.fortuneStart.endAt.year.toString(),
            alternate = nextAlternate(),
        )
    }
    if (case.groups.isNotEmpty() || case.tags.isNotEmpty()) {
        WenzhenFactRow(
            "分组标签",
            buildList {
                addAll(case.groups.map { it.name })
                addAll(case.tags.map { it.name })
            }.joinToString(" · "),
            alternate = nextAlternate(),
        )
    }
    if (case.birthTimeCandidates.size > 1) {
        WenzhenSectionHeader(
            title = "出生时间候选",
            actionLabel = "添加",
            onAction = onAddBirthTimeCandidate,
            modifier = Modifier.padding(top = 18.dp),
        )
        case.birthTimeCandidates.forEach { candidate ->
            val snapshot = case.calculationSnapshots.firstOrNull {
                it.id == candidate.calculationSnapshotId
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (candidate.adopted) "${candidate.label} · 当前采用" else candidate.label,
                        fontWeight = if (candidate.adopted) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        "${candidate.birthInput.displayDateTime()}　${snapshot?.result?.fourPillars?.display().orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!candidate.adopted && case.deletedAt == null) {
                    TextButton(
                        onClick = { onAdoptBirthTimeCandidate(candidate.id) },
                        enabled = !mutationSaving && snapshot != null,
                        modifier = Modifier.testTag("adopt_birth_time_candidate_${candidate.id}"),
                    ) {
                        Text("采用")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        mutationError?.let { error ->
            Text(
                error,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReferenceBasicChart(
    case: BaziCase,
    adopted: CaseCalculationSnapshot?,
) {
    if (adopted == null) {
        ReferenceEmptyText("当前命例没有已采用的计算快照。")
        return
    }
    val result = adopted.result
    result.basicChartDetails?.let { basic ->
        val solar = result.calendarConversion?.solarDateTime
        BasicChartDetailsView(
            details = basic,
            sex = case.sexForFortuneDirection,
            dateValues = solar?.let {
                listOf("${it.year}年", "${it.month}月", "${it.day}日", "%02d时".format(it.hour))
            },
        )
    } ?: ReferenceEmptyText("当前计算快照缺少基础排盘明细。")
    result.warnings.forEach { warning ->
        Text(
            warning.message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun WenzhenSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTag: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(NanfengGold),
        )
        Text(
            title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = if (actionTag == null) Modifier else Modifier.testTag(actionTag),
                colors = ButtonDefaults.textButtonColors(contentColor = NanfengGold),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
private fun WenzhenFactRow(
    label: String,
    value: String,
    alternate: Boolean,
    tag: String? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag == null) Modifier else Modifier.testTag(tag)),
        color = if (alternate) NanfengControlSurface else Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                label,
                modifier = Modifier.width(78.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun WenzhenDualFactRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    alternate: Boolean,
    leftTag: String? = null,
    rightTag: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (alternate) NanfengControlSurface else Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            WenzhenInlineFact(
                leftLabel,
                leftValue,
                Modifier.weight(1f),
                tag = leftTag,
            )
            Spacer(Modifier.width(12.dp))
            WenzhenInlineFact(
                rightLabel,
                rightValue,
                Modifier.weight(1f),
                tag = rightTag,
            )
        }
    }
}

@Composable
private fun WenzhenInlineFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    Row(
        modifier = modifier.then(if (tag == null) Modifier else Modifier.testTag(tag)),
    ) {
        Text(
            "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReferenceEmptyText(message: String) {
    Text(
        message,
        modifier = Modifier.padding(vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceCaseNotes(
    case: BaziCase,
    adopted: CaseCalculationSnapshot?,
    mode: CaseNotesMode,
    onModeChange: (CaseNotesMode) -> Unit,
    onEditRecord: (String) -> Unit,
    draft: CaseNotesDraft,
    onOwnerFeedbackChange: (String) -> Unit,
    onMasterCommentaryChange: (String) -> Unit,
    onAddTimeline: (CaseEventTimelineLevel, Int, String) -> Unit,
    onTimelineContentChange: (String, String) -> Unit,
) {
    var pickerVisible by rememberSaveable(case.id) { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val switcherWidth = minOf(maxWidth * 0.54f, 220.dp)
        Surface(
            modifier = Modifier
                .width(switcherWidth)
                .testTag("notes_mode_switcher"),
            color = Color.White,
            shape = RoundedCornerShape(13.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NanfengGold.copy(alpha = 0.55f),
            ),
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                NotesModeTab(
                    text = "命主反馈",
                    selected = mode == CaseNotesMode.OWNER_FEEDBACK,
                    onClick = { onModeChange(CaseNotesMode.OWNER_FEEDBACK) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_mode_owner"),
                )
                NotesModeTab(
                    text = "师傅点评",
                    selected = mode == CaseNotesMode.MASTER_COMMENTARY,
                    onClick = { onModeChange(CaseNotesMode.MASTER_COMMENTARY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_mode_master"),
                )
            }
        }
    }
    if (mode == CaseNotesMode.OWNER_FEEDBACK) {
        ReferenceOwnerProfileSheet(case)
        WenzhenSectionHeader(
            title = "命主反馈",
            modifier = Modifier.padding(top = 18.dp),
        )
        CaseNotesTextEditor(
            value = draft.ownerFeedback,
            onValueChange = onOwnerFeedbackChange,
            placeholder = "直接记录命主的反馈信息",
            enabled = case.deletedAt == null,
            modifier = Modifier.testTag("owner_feedback_input"),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "关键事件反馈记录",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            androidx.compose.material3.IconButton(
                onClick = { pickerVisible = true },
                enabled = case.deletedAt == null && adopted != null,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("add_event_button"),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加大运或流年",
                    tint = NanfengGold,
                )
            }
        }
        CaseNotesTimeline(
            draft = draft,
            calculation = adopted?.result,
            enabled = case.deletedAt == null,
            onContentChange = onTimelineContentChange,
        )
    } else {
        WenzhenSectionHeader(
            title = "师傅点评",
            modifier = Modifier.padding(top = 18.dp),
        )
        CaseNotesTextEditor(
            value = draft.masterCommentary,
            onValueChange = onMasterCommentaryChange,
            placeholder = "直接记录师傅的判断与点评",
            enabled = case.deletedAt == null,
            minLines = 8,
            modifier = Modifier.testTag("master_commentary_input"),
        )
    }

    Spacer(Modifier.height(10.dp))

    if (pickerVisible && adopted != null) {
        CaseNotesTimePicker(
            calculation = adopted.result,
            onDismiss = { pickerVisible = false },
            onConfirm = { level, year, stemBranch ->
                onAddTimeline(level, year, stemBranch)
                pickerVisible = false
            },
        )
    }
}

@Composable
private fun CaseNotesSaveFooter(
    case: BaziCase,
    saving: Boolean,
    saveError: String?,
    saved: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("case_notes_save_footer"),
    ) {
        Text(
            when {
                saveError != null -> saveError
                saving -> "正在保存…"
                saved -> "已自动保存"
                else -> "编辑中，将自动保存"
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (saveError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            },
        )
        Button(
            onClick = onSave,
            enabled = case.deletedAt == null && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_case_notes_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NanfengGold),
        ) {
            Text(if (saving) "保存中" else "保存", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CaseNotesTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    minLines: Int = 4,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        placeholder = {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
    )
}

@Composable
private fun CaseNotesTimeline(
    draft: CaseNotesDraft,
    calculation: CalculationResult?,
    enabled: Boolean,
    onContentChange: (String, String) -> Unit,
) {
    if (draft.timeline.isEmpty()) {
        Text(
            if (calculation == null) "暂无排盘时间信息。" else "点击右侧 + 添加大运或流年。",
            modifier = Modifier.padding(vertical = 18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    val decades = calculation?.decadeFortunes.orEmpty().sortedByDescending { it.startYear }
    val displayedIds = mutableSetOf<String>()
    decades.forEach { decade ->
        val decadeEntry = draft.timeline.firstOrNull {
            it.level == CaseEventTimelineLevel.DECADE && it.year == decade.startYear
        }
        val annualEntries = draft.timeline.filter {
            it.level == CaseEventTimelineLevel.ANNUAL && it.year in decade.startYear..decade.endYear
        }.sortedByDescending { it.year }
        if (decadeEntry == null && annualEntries.isEmpty()) return@forEach
        decadeEntry?.let { displayedIds += it.id }
        displayedIds += annualEntries.map { it.id }
        CaseNotesDecadeGroup(
            decade = decade,
            decadeEntry = decadeEntry,
            annualEntries = annualEntries,
            enabled = enabled,
            onContentChange = onContentChange,
        )
    }
    draft.timeline.filterNot { it.id in displayedIds }
        .sortedByDescending { it.year }
        .forEach { entry ->
            CaseNotesStandaloneTimelineEntry(entry, enabled, onContentChange)
        }
}

@Composable
private fun CaseNotesDecadeGroup(
    decade: DecadeFortune,
    decadeEntry: CaseNotesTimelineDraft?,
    annualEntries: List<CaseNotesTimelineDraft>,
    enabled: Boolean,
    onContentChange: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .drawBehind {
                val x = 7.dp.toPx()
                drawLine(
                    color = NanfengGold.copy(alpha = 0.25f),
                    start = Offset(x, 12.dp.toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        CaseNotesTimelineLabel(
            text = "${decade.startYear}年  ${decade.name}大运",
            parent = true,
        )
        decadeEntry?.let { entry ->
            CaseNotesTimelineInput(
                entry = entry,
                enabled = enabled,
                onContentChange = onContentChange,
                modifier = Modifier.padding(start = 24.dp, top = 7.dp),
            )
        }
        annualEntries.forEachIndexed { index, entry ->
            val connectsToNext = index < annualEntries.lastIndex
            Column(
                modifier = Modifier
                    .padding(start = 18.dp, top = 12.dp)
                    .then(
                        if (connectsToNext) {
                            Modifier
                                .testTag("notes_annual_connector")
                                .drawBehind {
                                    val x = 5.dp.toPx()
                                    drawLine(
                                        color = NanfengGold.copy(alpha = 0.38f),
                                        start = Offset(x, 12.dp.toPx()),
                                        end = Offset(x, size.height + 24.dp.toPx()),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                                        ),
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                CaseNotesTimelineLabel(
                    text = "${entry.year}年  ${entry.stemBranch}",
                    parent = false,
                )
                CaseNotesTimelineInput(
                    entry = entry,
                    enabled = enabled,
                    onContentChange = onContentChange,
                    modifier = Modifier.padding(start = 18.dp, top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CaseNotesStandaloneTimelineEntry(
    entry: CaseNotesTimelineDraft,
    enabled: Boolean,
    onContentChange: (String, String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        CaseNotesTimelineLabel(
            text = "${entry.year}年  ${entry.stemBranch}" +
                if (entry.level == CaseEventTimelineLevel.DECADE) "大运" else "",
            parent = entry.level == CaseEventTimelineLevel.DECADE,
        )
        CaseNotesTimelineInput(
            entry = entry,
            enabled = enabled,
            onContentChange = onContentChange,
            modifier = Modifier.padding(start = 24.dp, top = 6.dp),
        )
    }
}

@Composable
private fun CaseNotesTimelineLabel(text: String, parent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(if (parent) 14.dp else 10.dp),
            shape = CircleShape,
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(
                if (parent) 4.dp else 3.dp,
                NanfengGold.copy(alpha = if (parent) 0.72f else 0.48f),
            ),
        ) {}
        Text(
            text,
            modifier = Modifier.padding(start = 9.dp),
            color = if (parent) NanfengGold else MaterialTheme.colorScheme.onSurface,
            style = if (parent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (parent) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CaseNotesTimelineInput(
    entry: CaseNotesTimelineDraft,
    enabled: Boolean,
    onContentChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = entry.content,
        onValueChange = { onContentChange(entry.id, it) },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_event_input")
            .semantics { contentDescription = "时间线事件输入 ${entry.id}" },
        placeholder = { Text("输入这一阶段的关键事件") },
        minLines = 2,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun CaseNotesTimePicker(
    calculation: CalculationResult,
    onDismiss: () -> Unit,
    onConfirm: (CaseEventTimelineLevel, Int, String) -> Unit,
) {
    var level by rememberSaveable { mutableStateOf(CaseEventTimelineLevel.DECADE) }
    val currentYear = java.time.LocalDate.now().year
    val decades = calculation.decadeFortunes
        .distinctBy { it.startYear }
        .sortedByDescending { it.startYear }
    val annuals = calculation.annualFortunes
        .distinctBy { it.calendarYear }
        .sortedByDescending { it.calendarYear }
    val currentDecadeYear = decades
        .firstOrNull { currentYear in it.startYear..it.endYear }
        ?.startYear
    val currentAnnualYear = annuals.firstOrNull { it.calendarYear == currentYear }?.calendarYear
    var selectedYear by rememberSaveable(level) {
        mutableStateOf(
            if (level == CaseEventTimelineLevel.DECADE) {
                currentDecadeYear ?: decades.firstOrNull()?.startYear
            } else {
                currentAnnualYear ?: annuals.firstOrNull()?.calendarYear
            },
        )
    }
    val selectedDecade = decades.firstOrNull { it.startYear == selectedYear }
    val selectedAnnual = annuals.firstOrNull { it.calendarYear == selectedYear }
    val wheelValues = if (level == CaseEventTimelineLevel.DECADE) {
        decades.map { it.startYear }
    } else {
        annuals.map { it.calendarYear }
    }
    FixedPickerSheet(
        onDismiss = onDismiss,
        targetHeight = 540.dp,
        surfaceColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .testTag("notes_time_picker")
                .navigationBarsPadding()
                .padding(start = 22.dp, top = 20.dp, end = 22.dp, bottom = 44.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val width = minOf(maxWidth * 0.66f, 280.dp)
                Surface(
                    modifier = Modifier
                        .width(width)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        NotesModeTab(
                            text = "大运",
                            selected = level == CaseEventTimelineLevel.DECADE,
                            onClick = { level = CaseEventTimelineLevel.DECADE },
                            modifier = Modifier.weight(1f),
                        )
                        NotesModeTab(
                            text = "流年",
                            selected = level == CaseEventTimelineLevel.ANNUAL,
                            onClick = { level = CaseEventTimelineLevel.ANNUAL },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text(
                if (level == CaseEventTimelineLevel.DECADE) {
                    "选择大运（未来到过去）"
                } else {
                    "选择流年（未来到过去）"
                },
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selectedYear != null && wheelValues.isNotEmpty()) {
                WheelSelectionPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(264.dp)
                        .testTag("notes_timeline_wheel_panel"),
                ) {
                    ValueWheel(
                        label = if (level == CaseEventTimelineLevel.DECADE) "大运" else "流年",
                        values = wheelValues,
                        selectedValue = selectedYear!!,
                        display = { year ->
                            if (level == CaseEventTimelineLevel.DECADE) {
                                val name = decades.firstOrNull { it.startYear == year }?.name.orEmpty()
                                "${year}年  ${name}大运"
                            } else {
                                val name = annuals.firstOrNull { it.calendarYear == year }?.name.orEmpty()
                                "${year}年  $name"
                            }
                        },
                        onSelected = { selectedYear = it },
                        modifier = Modifier.weight(1f),
                        tag = "notes_timeline_wheel",
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(264.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无可选时间", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val year = selectedYear ?: return@Button
                    val name = if (level == CaseEventTimelineLevel.DECADE) {
                        selectedDecade?.name
                    } else {
                        selectedAnnual?.name
                    } ?: return@Button
                    onConfirm(level, year, name)
                },
                enabled = selectedYear != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(48.dp)
                    .testTag("notes_time_picker_confirm"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NanfengGold),
            ) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReferenceOwnerProfileSheet(case: BaziCase) {
    val occupation = case.profile.occupation.value
    val education = case.profile.education.value
    val finance = case.profile.finance.value
    val marriage = case.profile.marriage.value
    val health = case.profile.health.value
    if (listOf(occupation, education, finance, marriage, health).all { it == null }) return
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)) {
        if (occupation != null || education != null) {
            WenzhenDualFactRow(
                "职业",
                occupation ?: "未填写",
                "学历",
                education ?: "未填写",
                alternate = false,
            )
        }
        if (finance != null || marriage != null) {
            WenzhenDualFactRow(
                "财富",
                finance ?: "未填写",
                "婚姻",
                marriage ?: "未填写",
                alternate = true,
            )
        }
        health?.let {
            WenzhenFactRow("健康状态", it, alternate = false)
        }
    }
}

@Composable
private fun ReferenceOtherNotes(
    case: BaziCase,
    onEditRecord: (String) -> Unit,
) {
    val otherNotes = case.textRecords.filter {
        it.type == CaseTextRecordType.NOTE || it.type == CaseTextRecordType.ANALYSIS
    }
    if (otherNotes.isEmpty()) return
    WenzhenSectionHeader("其他笔记", modifier = Modifier.padding(top = 14.dp))
    otherNotes.forEach { record ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("record_card")
                .clickable(enabled = case.deletedAt == null) { onEditRecord(record.id) }
                .padding(vertical = 9.dp),
        ) {
            Text(
                buildString {
                    append(record.type.displayName())
                    if (record.type == CaseTextRecordType.ANALYSIS) {
                        append(" · ")
                        append((record.analysisCategory ?: AnalysisCategory.GENERAL).displayName())
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = NanfengGold,
            )
            Text(record.content, modifier = Modifier.padding(top = 3.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    }
}

@Composable
private fun NotesModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clickable(onClick = onClick),
        color = if (selected) NanfengGold else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ReferenceEventTimelineItem(
    event: CaseEvent,
    last: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = NanfengGold,
            ) {}
            if (!last) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(74.dp)
                        .background(NanfengGold.copy(alpha = 0.36f)),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, bottom = 16.dp),
        ) {
            Text(
                buildString {
                    append(event.displayDate())
                    event.stemBranch?.let { append("　$it") }
                },
                color = NanfengGold,
                fontWeight = FontWeight.Medium,
            )
            event.title?.let { title ->
                Text(title, modifier = Modifier.padding(top = 3.dp), fontWeight = FontWeight.SemiBold)
            }
            Text(event.rawText, modifier = Modifier.padding(top = 3.dp))
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
    onOpenCommentaryCandidates: (String) -> Unit,
    onOpenFeedbackThemeCandidates: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onOpenObjectiveSummary: () -> Unit,
    onOpenExternalAnalysis: () -> Unit,
    onExportCaseImage: () -> Unit,
    onShareCaseImage: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    selectedSection: CaseDetailSection,
    singleCaseExchangeBusy: Boolean,
    caseImageBusy: Boolean,
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
    var managementExpanded by rememberSaveable(case.id) { mutableStateOf(false) }
    var showObservationPicker by rememberSaveable(case.id) { mutableStateOf(false) }
    if (showObservationPicker) {
        ObservationDateTimePickerSheet(
            currentDate = fortuneObservationDate,
            currentTime = fortuneObservationTime,
            onDismiss = { showObservationPicker = false },
            onConfirm = { date, time ->
                onFortuneObservationDateChange(date)
                onFortuneObservationTimeChange(time)
                showObservationPicker = false
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(NanfengPageBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (selectedSection == CaseDetailSection.BASIC_INFO) {
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
            TextButton(
                onClick = { managementExpanded = !managementExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("toggle_case_management"),
            ) {
                Text(if (managementExpanded) "收起管理操作" else "更多管理操作")
            }
            if (managementExpanded) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onExportCaseImage,
                    enabled = !singleCaseExchangeBusy && !mutationSaving && !caseImageBusy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_case_image_button"),
                ) {
                    Text(if (caseImageBusy) "正在生成…" else "导出图片")
                }
                OutlinedButton(
                    onClick = onShareCaseImage,
                    enabled = !singleCaseExchangeBusy && !mutationSaving && !caseImageBusy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("share_case_image_button"),
                ) {
                    Text(if (caseImageBusy) "正在生成…" else "分享长图")
                }
            }
            OutlinedButton(
                onClick = onOpenObjectiveSummary,
                enabled = !singleCaseExchangeBusy && !mutationSaving && !caseImageBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .heightIn(min = 48.dp)
                    .testTag("open_objective_summary_button"),
            ) {
                Text("客观命盘摘要")
            }
            OutlinedButton(
                onClick = onOpenExternalAnalysis,
                enabled = !singleCaseExchangeBusy && !mutationSaving && !caseImageBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .heightIn(min = 48.dp)
                    .testTag("open_external_analysis_button"),
            ) {
                Text("外部分析桥接")
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
                    CalculationArchiveComparisonView(
                        snapshots = case.calculationSnapshots,
                        current = adopted,
                    )
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
                    FortuneDetailsView(
                        calculation = adopted.result,
                        fortuneObservationDate = fortuneObservationDate,
                        fortuneObservationTime = fortuneObservationTime,
                        fortunePosition = fortunePosition,
                        professionalFortunePosition = professionalFortunePosition,
                        fortunePositionError = fortunePositionError,
                        onOpenObservationPicker = { showObservationPicker = true },
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
                                WenzhenSourceFidelityContract.isSourceOnly(
                                    evidence.fieldKey,
                                )
                            ) {
                                WenzhenSourceFidelityContract.CALCULATION_MESSAGE
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
                            Text(
                                "来源：${record.sourceType.displayName()}",
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (
                                record.type == CaseTextRecordType.MASTER_COMMENTARY &&
                                case.deletedAt == null
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onOpenCommentaryCandidates(record.id)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .heightIn(min = 48.dp)
                                        .testTag("open_commentary_candidates_button"),
                                ) {
                                    Text("提取观点候选")
                                }
                            }
                            if (
                                record.type == CaseTextRecordType.OWNER_FEEDBACK &&
                                case.deletedAt == null
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onOpenFeedbackThemeCandidates(record.id)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .heightIn(min = 48.dp)
                                        .testTag("open_feedback_theme_candidates_button"),
                                ) {
                                    Text("提取主题标签候选")
                                }
                            }
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
                                "来源：${revision.snapshot.sourceType.displayName()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
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
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
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
private fun FortuneDetailsView(
    calculation: CalculationResult,
    fortuneObservationDate: String,
    fortuneObservationTime: String,
    fortunePosition: FortunePosition?,
    professionalFortunePosition: ProfessionalFortunePosition?,
    fortunePositionError: String?,
    onOpenObservationPicker: () -> Unit,
    onObservationSelect: (ProfessionalFortuneSelection) -> Unit = {},
    onToday: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NanfengPageBackground)
            .testTag("professional_page_surface")
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        if (professionalFortunePosition == null) {
            ReferenceEmptyText(fortunePositionError ?: "正在定位专业岁运…")
            return@Column
        }
        ProfessionalPillarMatrix(professionalFortunePosition.pillarColumns)
        ProfessionalSelectedDateBar(
            calculation = calculation,
            completedAge = professionalFortunePosition.completedAge,
            date = fortuneObservationDate,
            time = fortuneObservationTime,
            detail = professionalFortunePosition.selectedDateDetail,
            error = fortunePositionError,
            onOpenPicker = onOpenObservationPicker,
            onToday = onToday,
        )
        ProfessionalTimelineRow(
            title = "大运",
            items = professionalFortunePosition.decadeTimeline,
            tag = "decade_fortune_details",
            layer = ProfessionalFortuneLayer.DECADE,
            onSelect = onObservationSelect,
        )
        ProfessionalTimelineRow(
            title = "流年",
            items = professionalFortunePosition.annualTimeline,
            tag = "annual_fortune_details",
            layer = ProfessionalFortuneLayer.ANNUAL,
            onSelect = onObservationSelect,
        )
        ProfessionalTimelineRow(
            title = "流月",
            items = professionalFortunePosition.monthlyTimeline,
            tag = "monthly_fortune_details",
            layer = ProfessionalFortuneLayer.MONTHLY,
            onSelect = onObservationSelect,
        )
        ProfessionalTimelineRow(
            title = "流日",
            items = professionalFortunePosition.dailyTimeline,
            tag = "daily_fortune_details",
            layer = ProfessionalFortuneLayer.DAILY,
            onSelect = onObservationSelect,
        )
        ProfessionalTimelineRow(
            title = "流时",
            items = professionalFortunePosition.hourlyTimeline,
            tag = "hourly_fortune_details",
            layer = ProfessionalFortuneLayer.HOURLY,
            onSelect = onObservationSelect,
        )
        ProfessionalTextSections(
            title = "合冲刑害",
            groups = professionalFortunePosition.interactionGroups,
            tag = "fortune_interactions",
        )
        ProfessionalTextSections(
            title = "神煞",
            groups = professionalFortunePosition.shenShaGroups,
            tag = "fortune_shensha",
            stackLines = true,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    tag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag == null) Modifier else Modifier.testTag(tag))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ProfessionalPillarMatrix(columns: List<ProfessionalPillarColumn>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .testTag("professional_fortune_position"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.background(Color.White)) {
            val groupDividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("professional_transit_natal_divider")
                    .drawWithContent {
                        drawContent()
                        val x = size.width * 5f / 9f
                        drawLine(
                            color = groupDividerColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .testTag("professional_time_row"),
                ) {
                    columns.forEach { column ->
                        ProfessionalPillarCell(column, Modifier.weight(1f))
                    }
                }
                ProfessionalHiddenStemGrid(columns)
            }
        }
    }
}

@Composable
private fun ProfessionalPillarCell(
    column: ProfessionalPillarColumn,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .then(
                if (column.key.startsWith("flow_")) Modifier.testTag("${column.key}_pillar")
                else Modifier,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color.White)
                .testTag("${column.key}_time_label"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                column.label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val stem = column.pillar.getOrNull(0)
        val branch = column.pillar.getOrNull(1)
        if (stem == null || branch == null) {
            Text("—", modifier = Modifier.padding(top = 28.dp))
        } else {
            ProfessionalTenGodLabel(
                tenGod = column.stemTenGod,
                segmented = true,
                tag = "${column.key}_stem_ten_god",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .testTag("${column.key}_stem_surface")
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stem.toString(),
                    modifier = Modifier.testTag("${column.key}_stem_text"),
                    color = baziElementColor(stem),
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                branch.toString(),
                modifier = Modifier
                    .background(Color.White)
                    .padding(top = 5.dp)
                    .testTag("${column.key}_branch_text"),
                color = baziElementColor(branch),
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfessionalHiddenStemGrid(columns: List<ProfessionalPillarColumn>) {
    val rowCount = columns.maxOfOrNull { it.hiddenStems.size } ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProfessionalPillarGridSurface)
            .testTag("professional_hidden_stem_surface")
            .padding(top = 6.dp, bottom = 4.dp),
    ) {
        repeat(rowCount) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProfessionalPillarGridSurface)
                    .testTag("professional_hidden_stem_row_$rowIndex")
                    .padding(vertical = 1.5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                columns.forEach { column ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        column.hiddenStems.getOrNull(rowIndex)?.let { hidden ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    hidden.heavenStem,
                                    modifier = Modifier.testTag(
                                        "${column.key}_hidden_stem_$rowIndex",
                                    ),
                                    color = hidden.heavenStem.firstOrNull()
                                        ?.let(::baziElementColor) ?: NanfengInk,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    hidden.tenGod,
                                    modifier = Modifier.testTag(
                                        "${column.key}_hidden_ten_god_$rowIndex",
                                    ),
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
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

@Composable
private fun ProfessionalTenGodLabel(
    tenGod: String,
    compact: Boolean = false,
    segmented: Boolean = false,
    tag: String? = null,
) {
    Text(
        tenGod,
        modifier = (if (segmented) {
            Modifier
                .fillMaxWidth()
                .background(ProfessionalPillarGridSurface)
                .padding(vertical = 1.dp)
        } else {
            Modifier.padding(vertical = if (compact) 0.dp else 1.dp)
        }).then(if (tag == null) Modifier else Modifier.testTag(tag)),
        fontSize = when {
            segmented -> 11.sp
            compact -> 8.sp
            else -> 10.sp
        },
        lineHeight = when {
            segmented -> 15.sp
            compact -> 9.sp
            else -> 13.sp
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private val ProfessionalPillarGridSurface = Color(0xFFFBFBFA)

@Composable
private fun ProfessionalTimelineRow(
    title: String,
    items: List<ProfessionalTimelineItem>,
    tag: String,
    layer: ProfessionalFortuneLayer,
    onSelect: (ProfessionalFortuneSelection) -> Unit,
) {
    if (items.isEmpty()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = professionalTimelineCardColor(title)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(26.dp)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                title.forEach { character ->
                    Text(
                        text = character.toString(),
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val timelineColumnWidth = maxWidth / PROFESSIONAL_TIMELINE_VISIBLE_COLUMNS
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tag}_list"),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                        Box(
                            modifier = Modifier
                                .width(timelineColumnWidth)
                                .testTag("${tag}_column"),
                        ) {
                            ProfessionalTimelineCell(
                                item = item,
                                modifier = Modifier.fillMaxWidth(),
                                compact = true,
                                showTrailingDivider = index < items.lastIndex,
                                onClick = {
                                    onSelect(ProfessionalFortuneSelection(layer, item.observedAt))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val PROFESSIONAL_TIMELINE_VISIBLE_COLUMNS = 10

private val ProfessionalTimelinePrimarySurface = Color.White
private val ProfessionalTimelineAlternateSurface = Color(0xFFFCFCFB)

private fun professionalTimelineCardColor(title: String): Color = when (title) {
    "流年", "流日" -> ProfessionalTimelineAlternateSurface
    else -> ProfessionalTimelinePrimarySurface
}

@Composable
private fun ProfessionalTimelineCell(
    item: ProfessionalTimelineItem,
    modifier: Modifier = Modifier,
    compact: Boolean,
    showTrailingDivider: Boolean = false,
    onClick: () -> Unit,
) {
    val trailingDivider = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("timeline_${item.key}")
            .then(if (item.selected) Modifier.testTag("selected_${item.key}") else Modifier)
            .then(
                if (showTrailingDivider) {
                    Modifier.drawBehind {
                        val dividerX = size.width - 0.5.dp.toPx()
                        drawLine(
                            color = trailingDivider,
                            start = Offset(dividerX, 0f),
                            end = Offset(dividerX, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                },
            ),
        color = if (item.selected) NanfengGold.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 0.dp else 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                item.label,
                modifier = Modifier.testTag("timeline_${item.key}_label"),
                fontSize = if (compact) 8.sp else 9.sp,
                lineHeight = if (compact) 9.sp else 11.sp,
                color = if (item.selected) NanfengGold else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            val stem = item.pillar.getOrNull(0)
            val branch = item.pillar.getOrNull(1)
            val stageLabel = item.stageLabel
            if (stageLabel != null) {
                Text(
                    stageLabel.take(1),
                    modifier = Modifier.testTag("timeline_${item.key}_upper"),
                    fontSize = if (compact) 14.sp else 15.sp,
                    lineHeight = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ProfessionalTenGodLabel(" ", compact)
                Text(
                    stageLabel.drop(1),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .testTag("timeline_${item.key}_lower"),
                    fontSize = if (compact) 14.sp else 15.sp,
                    lineHeight = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ProfessionalTimelineBranchDetail(item, compact)
            } else if (stem != null) {
                Text(
                    stem.toString(),
                    modifier = Modifier.testTag("timeline_${item.key}_stem"),
                    color = baziElementColor(stem),
                    fontSize = if (compact) 14.sp else 15.sp,
                    lineHeight = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(modifier = Modifier.testTag("timeline_${item.key}_stem_detail")) {
                    ProfessionalTenGodLabel(item.stemTenGod, compact)
                }
            }
            if (stageLabel == null && branch != null) {
                Text(
                    branch.toString(),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .testTag("timeline_${item.key}_branch"),
                    color = baziElementColor(branch),
                    fontSize = if (compact) 14.sp else 15.sp,
                    lineHeight = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                ProfessionalTimelineBranchDetail(item, compact)
            }
            Text(
                item.subtitle,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .testTag("timeline_${item.key}_subtitle"),
                textAlign = TextAlign.Center,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ProfessionalTimelineBranchDetail(
    item: ProfessionalTimelineItem,
    compact: Boolean,
) {
    Text(
        item.hiddenStems.joinToString(separator = "") { tenGodAbbreviation(it.tenGod) },
        modifier = Modifier
            .testTag("timeline_${item.key}_branch_detail"),
        fontSize = if (compact) 8.sp else 9.sp,
        lineHeight = if (compact) 9.sp else 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProfessionalSelectedDateBar(
    calculation: CalculationResult,
    completedAge: Int,
    date: String,
    time: String,
    detail: String,
    error: String?,
    onOpenPicker: () -> Unit,
    onToday: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fortune_selected_datetime"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, bottom = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .clickable(onClick = onOpenPicker)
                    .testTag("fortune_observation_picker")
                    .semantics { contentDescription = "修改观察时间" },
                color = Color.White.copy(alpha = 0.76f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    NanfengGold.copy(alpha = 0.28f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = NanfengGold.copy(alpha = 0.72f),
                    )
                    Text(
                        "阳历 $date $time　${detail.ifBlank { "农历未记录" }}",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("fortune_start_info"),
            ) {
                Text(
                    "起运  ${calculation.fortuneStart.direction.displayName()} · " +
                        calculation.fortuneStart.ageDurationDisplay(),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "交运  ${calculation.fortuneStart.endAt.display()}",
                    modifier = Modifier.padding(top = 1.dp),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .testTag("fortune_age_today_group"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$completedAge 岁",
                    modifier = Modifier.testTag("fortune_completed_age"),
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NanfengInk,
                )
                Spacer(modifier = Modifier.width(7.dp))
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable(onClick = onToday)
                        .testTag("fortune_today")
                        .semantics { contentDescription = "定位今天" },
                    color = NanfengGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_today),
                            contentDescription = null,
                            tint = NanfengGold,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "今",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NanfengGold,
                        )
                    }
                }
            }
        }
        error?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection.displayName(): String =
    if (this == com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection.FORWARD) "顺排" else "逆排"

private fun com.nanzhufeng.nanfengbazi.domain.model.FortuneStart.ageDurationDisplay(): String =
    "${years}年${months}月${days}日${hours}时${minutes}分"

@Composable
private fun ProfessionalTextSections(
    title: String,
    groups: List<ProfessionalTextGroup>,
    tag: String,
    stackLines: Boolean = false,
) {
    if (groups.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(tag)) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth().background(NanfengControlSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        groups.forEachIndexed { groupIndex, group ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    group.title,
                    modifier = Modifier.width(66.dp),
                    fontSize = 10.sp,
                    color = NanfengGold,
                )
                if (stackLines) {
                    Column(modifier = Modifier.weight(1f)) {
                        group.lines.ifEmpty { listOf("无") }.forEachIndexed { lineIndex, line ->
                            Text(
                                buildAnnotatedString {
                                    line.take(2).forEach { character ->
                                        withStyle(SpanStyle(color = baziElementColor(character))) {
                                            append(character)
                                        }
                                    }
                                    append(line.drop(2))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("${tag}_line_${groupIndex}_$lineIndex")
                                    .padding(bottom = if (lineIndex == group.lines.lastIndex) 0.dp else 3.dp),
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                } else {
                    Text(
                        group.lines.ifEmpty { listOf("无") }.joinToString("；"),
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun DecadeFortuneDetailsView(decades: List<DecadeFortune>) {
    Column(modifier = Modifier.fillMaxWidth().testTag("decade_fortune_details")) {
        decades.take(8).forEachIndexed { index, decade ->
            DetailRow(
                "第${index + 1}运",
                "${decade.name}　${decade.startAge}–${decade.endAge}岁　" +
                    "${decade.startYear}–${decade.endYear}",
            )
        }
    }
}

@Composable
private fun CalculationArchiveComparisonView(
    snapshots: List<CaseCalculationSnapshot>,
    current: CaseCalculationSnapshot,
) {
    val previous = snapshots.asReversed().firstOrNull { it.id != current.id }
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calculation_archive_comparison"),
    ) {
        Text(
            "计算档案差异",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
        )
        if (previous == null) {
            Text(
                "暂无历史计算快照；当前档案会继续保留，后续重算后可在这里核对差异。",
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val comparison = remember(previous, current) {
            compareCalculationSnapshots(previous, current)
        }
        Text(
            comparison.attributionSummary,
            modifier = Modifier
                .padding(top = 6.dp)
                .testTag("calculation_archive_attribution"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        DetailRow(
            "档案",
            "${previous.result.profile.id} → ${current.result.profile.id}",
        )
        DetailRow(
            "引擎版本变化",
            "${previous.result.evidence.engineVersion} → " +
                current.result.evidence.engineVersion,
        )
        DetailRow(
            "规则版本变化",
            "${previous.result.evidence.ruleVersion} → " +
                current.result.evidence.ruleVersion,
        )
        if (comparison.outcomeConsistent) {
            Text(
                "核心排盘结果一致。",
                modifier = Modifier
                    .padding(top = 6.dp)
                    .testTag("calculation_archive_consistent"),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                "发现 ${comparison.outcomeChanges.size} 项结果变化：",
                modifier = Modifier
                    .padding(top = 6.dp)
                    .testTag("calculation_archive_changed"),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            comparison.outcomeChanges.forEachIndexed { index, change ->
                Text(
                    "${change.label}：${change.previousValue} → ${change.currentValue}",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("calculation_archive_change_$index"),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        val olderCount = (snapshots.size - 2).coerceAtLeast(0)
        if (olderCount > 0) {
            Text(
                "另保留 $olderCount 条更早快照；当前仅与最近一条历史快照比较。",
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BasicChartDetailsView(
    details: BasicChartDetails,
    sex: SexForFortuneDirection,
    dateValues: List<String>? = null,
) {
    val pillars = details.pillars.associateBy { it.position }
    val ordered = PillarPosition.entries.map { requireNotNull(pillars[it]) }
    val natalShenSha = BasicShenShaRules.resolve(details.pillars).map { shenSha ->
        shenSha.names.take(BASIC_CHART_SHEN_SHA_LIMIT)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("basic_chart_details"),
    ) {
        BasicChartTableRow(
            label = "",
            values = listOf("年柱", "月柱", "日柱", "时柱"),
            shaded = true,
        )
        dateValues?.let {
            BasicChartTableRow(
                label = "日期",
                values = it,
            )
        }
        BasicChartTableRow(
            label = "十神",
            values = ordered.map { pillar ->
                if (pillar.position == PillarPosition.DAY) {
                    if (sex == SexForFortuneDirection.MAN) "元男" else "元女"
                } else {
                    pillar.primaryTenGod
                }
            },
            tag = "basic_chart_primary",
            shaded = true,
        )
        BasicChartTableRow(
            "天干",
            ordered.map(PillarDetail::heavenStem),
            valueColors = ordered.map { baziElementColor(it.heavenStemElement) },
            emphasis = true,
        )
        BasicChartTableRow(
            "地支",
            ordered.map(PillarDetail::earthBranch),
            valueColors = ordered.map { baziElementColor(it.earthBranchElement) },
            emphasis = true,
        )
        BasicChartHiddenStemRow(ordered, shaded = true)
        BasicChartTableRow("自坐", ordered.map(PillarDetail::selfSittingTerrain))
        BasicChartTableRow(
            "空亡",
            ordered.map { it.voidEarthBranches.joinToString("") },
            shaded = true,
        )
        BasicChartTableRow("纳音", ordered.map(PillarDetail::naYin))
        BasicChartTableRow(
            label = "神煞",
            values = List(PillarPosition.entries.size) { index ->
                natalShenSha.getOrNull(index)
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString("\n")
                    ?: "—"
            },
            tag = "basic_chart_shensha",
            valueTagPrefix = "basic_chart_shensha_value",
            shaded = true,
        )
    }
}

@Composable
private fun BasicChartHiddenStemRow(
    pillars: List<PillarDetail>,
    shaded: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (shaded) NanfengControlSurface else Color.White)
            .padding(horizontal = 6.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "藏干",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        pillars.forEach { pillar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                pillar.hiddenStems.forEach { hidden ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            hidden.heavenStem,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            color = baziElementColor(hidden.element),
                        )
                        Text(
                            hidden.tenGod,
                            modifier = Modifier.padding(start = 2.dp),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicChartTableRow(
    label: String,
    values: List<String>,
    tag: String? = null,
    valueTagPrefix: String? = null,
    valueColors: List<Color> = emptyList(),
    emphasis: Boolean = false,
    shaded: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag == null) Modifier else Modifier.testTag(tag))
            .background(if (shaded) NanfengControlSurface else Color.White)
            .padding(horizontal = 8.dp, vertical = if (emphasis) 12.dp else 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        values.forEachIndexed { index, value ->
            Text(
                value,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (valueTagPrefix == null) {
                            Modifier
                        } else {
                            Modifier.testTag("${valueTagPrefix}_$index")
                        },
                    ),
                textAlign = TextAlign.Center,
                style = if (emphasis) {
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                lineHeight = if (emphasis) {
                    MaterialTheme.typography.headlineSmall.lineHeight
                } else {
                    MaterialTheme.typography.bodyMedium.lineHeight
                },
                color = valueColors.getOrNull(index) ?: MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private const val BASIC_CHART_SHEN_SHA_LIMIT = 5

@Composable
private fun SectionHeading(title: String, description: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    if (description.isNotBlank()) {
        Text(
            description,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Spacer(modifier = Modifier.height(8.dp))
    }
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

private fun SexForFortuneDirection.chartTypeName(): String = when (this) {
    SexForFortuneDirection.MAN -> "乾造"
    SexForFortuneDirection.WOMAN -> "坤造"
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
    AnalysisCategory.KEY_YEARS -> "关键年份"
    AnalysisCategory.OPEN_QUESTIONS -> "待验证问题"
    AnalysisCategory.OTHER -> "其他"
}

internal fun TextRecordSourceType.displayName(): String = when (this) {
    TextRecordSourceType.USER -> "用户记录"
    TextRecordSourceType.RULE_TEMPLATE -> "规则模板"
    TextRecordSourceType.EXTERNAL_AI -> "外部 AI（手动回填）"
    TextRecordSourceType.IMPORTED_IMAGE -> "图片导入"
    TextRecordSourceType.LEGACY_UNSPECIFIED -> "历史未标记"
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

private fun com.nanzhufeng.nanfengbazi.domain.model.BirthInput.displayDateOnly(): String =
    when (val calendar = calendarInput) {
        is BirthCalendarInput.Solar -> with(calendar.dateTime) {
            "阳历%04d年%d月%d日".format(year, month, day)
        }
        is BirthCalendarInput.Lunar -> with(calendar.dateTime) {
            "农历%04d年%s%d月%d日".format(
                year,
                if (isLeapMonth) "闰" else "",
                month,
                day,
            )
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
