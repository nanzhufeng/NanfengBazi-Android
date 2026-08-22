package com.nanzhufeng.nanfengbazi

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.BreakIterator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.nanfengbazi.domain.BasicShenShaRules
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityReport
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityRecord
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySignal
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySignalKind
import com.nanzhufeng.nanfengbazi.domain.BaziStructuralProfile
import com.nanzhufeng.nanfengbazi.domain.CaseAdvancedFilter
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.caseNameInitial
import com.nanzhufeng.nanfengbazi.domain.FourPillarsSearchFilter
import com.nanzhufeng.nanfengbazi.domain.PillarCharacterFilter
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
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.completedAgeRangeDisplay
import com.nanzhufeng.nanfengbazi.domain.model.solarBirthDateTimeForFortuneDisplay
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
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncCoordinator
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncState
import com.nanzhufeng.nanfengbazi.cloud.BaziGoogleSignInClient
import com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummary
import com.nanzhufeng.nanfengbazi.domain.displayName
import com.nanzhufeng.nanfengbazi.domain.structuralProfileOrAnalyze
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// 历史页顶栏的搜索与库切换共用此轮廓，保证边框、按压面和阴影都是同一枚胶囊。
private val RecordToolbarPillShape = RoundedCornerShape(24.dp)
internal val HomeQuickEntryPillShape = RoundedCornerShape(percent = 50)

@Composable
fun NanfengBaziApp(
    viewModel: StageTwoViewModel,
    onImportScreenshots: () -> Unit = {},
    screenshotImportState: ScreenshotImportUiState = ScreenshotImportUiState(),
    onRetryScreenshotImport: () -> Unit = {},
    onConfirmScreenshotAiRecognition: () -> Unit = {},
    onCancelScreenshotAiRecognition: () -> Unit = {},
    onDeleteScreenshotImport: () -> Unit = {},
    onSetScreenshotFieldAdopted: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onUpdateScreenshotFieldValue: (String, String, String) -> Unit = { _, _, _ -> },
    onSetScreenshotLongTextAdopted: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSetScreenshotCandidateAdopted: (String, Boolean) -> Unit = { _, _ -> },
    onCommitScreenshotCandidate: (String, Boolean) -> Unit = { _, _ -> },
    onConsumeScreenshotImportMessage: () -> Unit = {},
    onCreateSingleCaseDocument: (SingleCaseExportDocumentRequest) -> Unit = {},
    onOpenSingleCaseDocument: () -> Unit = {},
    onOpenWenzhenImportDocument: () -> Unit = {},
    onImportCuratedCelebrityCatalog: () -> Unit = {},
    onRetryPasswordSingleCaseDocument: (CharArray) -> Unit = {},
    onCommitSingleCaseImport: ((SingleCaseImportDecision) -> Unit)? = null,
    onCommitSingleCaseMerge: (() -> Unit)? = null,
    onCommitPasswordSingleCaseDocument: (CharArray) -> Unit = {},
    onSaveCaseImagesToGallery: (List<String>) -> Unit = {},
    onSharePreparedCaseImages: () -> Unit = {},
    onCreateFullBackupDocument: (String) -> Unit = {},
    onCreateEncryptedFullBackupDocument: (String) -> Unit = {},
    onOpenFullBackupDocument: () -> Unit = {},
    onRetryPasswordFullBackupDocument: (CharArray) -> Unit = {},
    onExecuteFullBackupDocument: (CharArray?) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cloudContainer = (context.applicationContext as? NanfengBaziApplication)?.container
    val skinPreferenceStore = remember(cloudContainer) { cloudContainer?.baziSkinPreferenceStore }
    var selectedSkin by remember(skinPreferenceStore) {
        mutableStateOf(skinPreferenceStore?.read() ?: BaziSkin.INK_STAR_CHART)
    }
    val rootView = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val caseDetailCaptureRegistry = remember { CaseDetailPageCaptureRegistry() }
    // 列表在详情页显示期间会暂时离开组合；滚动位置必须由根页面持有，返回后才能回到原处。
    val caseListState = rememberLazyListState()
    val latestUiState by rememberUpdatedState(state)
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    var transientMessage by remember { mutableStateOf<String?>(null) }
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
    LaunchedEffect(transientMessage) {
        transientMessage?.let {
            snackbarHostState.showSnackbar(it)
            transientMessage = null
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
    val returnsToCompatibilityCreate =
        state.destination == AppDestination.CreateCase &&
            state.compatibilityParticipantRole != null
    val hasBackStackDestination = state.destination !in setOf(
            AppDestination.CaseList,
            AppDestination.RecordHub,
            AppDestination.Settings,
            AppDestination.CreateCase,
        )
    BackHandler(
        enabled = hasBackStackDestination ||
            state.compatibilityParticipantSelectionRole != null ||
            returnsToCompatibilityCreate,
    ) {
        if (returnsToCompatibilityCreate) {
            viewModel.cancelCompatibilityParticipantCreate()
        } else {
            viewModel.navigateBack()
        }
    }
    NanfengBaziTheme(skin = selectedSkin) {
        state.wenzhenImportPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = viewModel::cancelWenzhenWebImport,
                title = { Text("导入网页案例资料？") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("用户案例 ${preview.userCaseCount} 个，名人案例 ${preview.celebrityCaseCount} 个。")
                        Text(
                            "用户分组 ${preview.userGroupCount} 个，名人分类 " +
                                "${preview.celebrityGroupCount} 个，名人标签 " +
                                "${preview.celebrityTagCount} 个。",
                        )
                        Text(
                            "导入会逐例校验并使用稳定 ID；其中名人案例会自动纳入" +
                                "名人案例统一资料库。重复执行只跳过已存在案例，中途中断后可续传。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::executeWenzhenWebImport,
                        modifier = Modifier.testTag("wenzhen_import_confirm"),
                    ) { Text("开始导入") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelWenzhenWebImport) { Text("取消") }
                },
            )
        }
        if (state.wenzhenImportBusy && state.wenzhenImportProgress != null) {
            val progress = requireNotNull(state.wenzhenImportProgress)
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在导入网页案例资料") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text("${progress.stage}  ${progress.completed}/${progress.total}")
                    }
                },
                confirmButton = {},
            )
        }
        state.wenzhenImportResult?.let { result ->
            AlertDialog(
                onDismissRequest = viewModel::dismissWenzhenImportResult,
                title = { Text("网页案例资料导入完成") },
                text = {
                    Text(
                        "新增 ${result.created} 个（用户 ${result.userCreated}、名人案例 " +
                            "${result.celebrityCreated} 个已纳入统一资料库），已存在 ${result.skipped} 个，" +
                            "异常 ${result.invalid} 个。",
                    )
                },
                confirmButton = {
                    Button(onClick = viewModel::dismissWenzhenImportResult) { Text("完成") }
                },
            )
        }
        state.wenzhenImportError?.let { error ->
            AlertDialog(
                onDismissRequest = viewModel::dismissWenzhenImportResult,
                title = { Text("网页案例资料导入未完成") },
                text = { Text(error) },
                confirmButton = {
                    Button(onClick = viewModel::dismissWenzhenImportResult) { Text("知道了") }
                },
            )
        }
        state.curatedCelebrityImportPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = viewModel::cancelCuratedCelebrityImport,
                title = { Text("更新名人案例统一资料库？") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("本次更新 ${preview.caseCount} 例，统一归入五个展示分组。")
                        Text("资料等级：${preview.ratedCount.entries.sortedBy { it.key }.joinToString("、") { "${it.key} ${it.value}" }}。")
                        Text("含其他时刻候选的命例 ${preview.alternativeTimeCaseCount} 例。")
                        Text(
                            "每例会保留来源网址、资料等级、默认与备选时刻；与历史导入资料统一去重，且不会覆盖已有命例。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::executeCuratedCelebrityImport,
                        modifier = Modifier.testTag("curated_celebrity_import_confirm"),
                    ) { Text("开始导入") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelCuratedCelebrityImport) { Text("取消") }
                },
            )
        }
        if (state.curatedCelebrityImportBusy && state.curatedCelebrityImportProgress != null) {
            val progress = requireNotNull(state.curatedCelebrityImportProgress)
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在更新名人案例统一资料库") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text("${progress.completed}/${progress.total}")
                    }
                },
                confirmButton = {},
            )
        }
        state.curatedCelebrityImportResult?.let { result ->
            AlertDialog(
                onDismissRequest = viewModel::dismissCuratedCelebrityImportResult,
                title = { Text("名人案例统一资料库已更新") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("新增 ${result.created} 个，更新 ${result.updated} 个，已存在 ${result.skipped} 个，异常 ${result.invalid} 个。")
                        if (result.errors.isNotEmpty()) {
                            Text(
                                result.errors.joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::dismissCuratedCelebrityImportResult) { Text("完成") }
                },
            )
        }
        state.curatedCelebrityImportError?.let { error ->
            AlertDialog(
                onDismissRequest = viewModel::dismissCuratedCelebrityImportResult,
                title = { Text("名人案例统一资料库更新未完成") },
                text = { Text(error) },
                confirmButton = {
                    Button(onClick = viewModel::dismissCuratedCelebrityImportResult) { Text("知道了") }
                },
            )
        }
        if (state.builtInCelebrityCatalogSyncBusy) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在同步名人案例统一资料库") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(
                            "正在校验并分批更新 ${state.builtInCelebrityCatalogSyncCaseCount} 条资料。" +
                                "用户案例、笔记和关键事件不会被改动。",
                        )
                    }
                },
                confirmButton = {},
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BoxWithConstraints {
                val useNavigationRail = maxWidth >= EXPANDED_NAVIGATION_MIN_WIDTH
                val isCompatibilityParticipantCreate = state.compatibilityParticipantRole != null
                val isCompatibilityParticipantSelection =
                    state.compatibilityParticipantSelectionRole != null
                val showRootNavigation = !isCompatibilityParticipantCreate &&
                    !isCompatibilityParticipantSelection && state.destination in setOf(
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
                val shouldFloatRootNavigation = showRootNavigation && !useNavigationRail
                val rootNavigationScrollEndInset = if (shouldFloatRootNavigation) {
                    ROOT_NAVIGATION_BAR_HEIGHT +
                        ROOT_NAVIGATION_BOTTOM_MARGIN +
                        ROOT_NAVIGATION_CONTENT_GAP
                } else {
                    0.dp
                }
                // Only the A–Z/# index needs the usable list viewport.  Keeping this separate
                // from page content prevents a white navigation backing strip from returning.
                val recordAlphabetIndexBottomInset = if (
                    shouldFloatRootNavigation && state.destination == AppDestination.CaseList
                ) {
                    RECORD_ALPHABET_FLOATING_NAVIGATION_INSET
                } else {
                    0.dp
                }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { padding ->
                    // The phone navigation is a true overlay. It owns no layout height or
                    // content-safe spacer; pages continue naturally behind its own surface.
                    val rootScreenModifier = Modifier.padding(padding)
                    Box(modifier = Modifier.fillMaxSize()) {
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
                        caseListState = caseListState,
                        onQueryChange = viewModel::updateQuery,
                        onSelectGroup = viewModel::selectGroup,
                        onSelectTag = viewModel::selectTag,
                        onSelectSort = viewModel::selectSortOrder,
                        onSelectVisibility = viewModel::selectVisibility,
                        onSelectLibrary = viewModel::selectCaseLibrary,
                        onApplyAdvancedFilter = viewModel::applyAdvancedFilter,
                        onClearFilters = viewModel::clearCaseFilters,
                        onRefresh = viewModel::refreshCases,
                        onCreate = viewModel::openCreate,
                        onCreateCelebrity = viewModel::openCreateCelebrityCase,
                        onImportScreenshots = onImportScreenshots,
                        screenshotImportState = screenshotImportState,
                        onRetryScreenshotImport = onRetryScreenshotImport,
                        onConfirmScreenshotAiRecognition = onConfirmScreenshotAiRecognition,
                        onCancelScreenshotAiRecognition = onCancelScreenshotAiRecognition,
                        onDeleteScreenshotImport = onDeleteScreenshotImport,
                        onReviewScreenshotImport = viewModel::openScreenshotImportReview,
                        onCreateGroup = viewModel::createCaseGroup,
                        onRenameGroup = viewModel::renameCaseGroup,
                        onReorderGroups = viewModel::reorderCaseGroups,
                        onDeleteGroup = viewModel::deleteCaseGroup,
                        onEnterBatchMode = {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        },
                        onUpdatePinnedCases = viewModel::updatePinnedCases,
                        onBatchDeleteCases = viewModel::batchDeleteCases,
                        onEditCase = viewModel::openEditCase,
                        onTogglePinnedCase = viewModel::togglePinnedCase,
                        onPrefetchCase = viewModel::prefetchCaseDetail,
                        onOpenCase = viewModel::openDetailFromList,
                        compatibilitySelectionRole = state.compatibilityParticipantSelectionRole,
                        onSelectCompatibilityCase = viewModel::selectCompatibilityParticipantFromList,
                        onCancelCompatibilitySelection = viewModel::cancelCompatibilityParticipantList,
                        bottomContentInset = rootNavigationScrollEndInset,
                        alphabetIndexBottomInset = recordAlphabetIndexBottomInset,
                        modifier = rootScreenModifier,
                    )
                    AppDestination.CaseComparison -> CaseComparisonScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onSelectLeft = viewModel::selectComparisonLeft,
                        onSelectRight = viewModel::selectComparisonRight,
                        onRetry = viewModel::retryCaseComparison,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.BaziCompatibility -> BaziCompatibilityScreen(
                        state = state,
                        onBack = viewModel::navigateBack,
                        onOpenParticipantList = viewModel::openCompatibilityParticipantList,
                        onOpenHistoryRecord = viewModel::openCompatibilityHistoryRecord,
                        onCloseHistoryRecord = viewModel::closeCompatibilityHistoryRecord,
                        onDeleteHistoryRecords = viewModel::deleteCompatibilityHistoryRecords,
                        onRetryHistory = viewModel::retryCompatibilityHistory,
                        onAnalyze = viewModel::analyzeBaziCompatibility,
                        onRetry = viewModel::retryBaziCompatibility,
                        // Compatibility TopAppBar owns the status-bar inset. Applying Scaffold's
                        // inset here as well leaves a blank band above all compatibility pages.
                        modifier = Modifier,
                    )
                    AppDestination.BaziCompatibilityReport -> BaziCompatibilityReportScreen(
                        report = state.compatibilityReport,
                        loading = state.compatibilityLoading,
                        error = state.compatibilityError,
                        onBack = viewModel::navigateBack,
                        onReplaceParticipant = viewModel::openCompatibilityParticipantList,
                        onRetry = viewModel::retryBaziCompatibility,
                        // Keep the report aligned with setup/history: one top inset, from its bar.
                        modifier = Modifier,
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
                        onOpenCase = viewModel::openDetailFromList,
                        bottomContentInset = rootNavigationScrollEndInset,
                        modifier = rootScreenModifier,
                    )
                    AppDestination.Settings -> SettingsHomeScreen(
                        state = state,
                        aiCommentaryState = state.aiCommentary,
                        selectedSkin = selectedSkin,
                        onSkinPreview = { selectedSkin = it },
                        onSkinCommitted = { skin ->
                            selectedSkin = skin
                            skinPreferenceStore?.write(skin)
                        },
                        screenshotImportState = screenshotImportState,
                        onRatHourRuleChange = viewModel::updateDefaultRatHourRule,
                        onImportScreenshots = onImportScreenshots,
                        onImportSingleCase = onOpenSingleCaseDocument,
                        onImportWenzhen = onOpenWenzhenImportDocument,
                        onImportCuratedCelebrityCatalog = onImportCuratedCelebrityCatalog,
                        onExportFullBackup = viewModel::requestFullBackupExport,
                        onRestoreFullBackup = onOpenFullBackupDocument,
                        onOpenAiServicePage = viewModel::openAiServicePage,
                        onOpenAiSettings = viewModel::openAiCommentarySettings,
                        onOpenAiHistory = viewModel::openAiCallHistory,
                        cloudSyncCoordinator = cloudContainer?.cloudSyncCoordinator,
                        googleSignInClient = cloudContainer?.googleSignInClient,
                        activityContext = context,
                        bottomContentInset = rootNavigationScrollEndInset,
                        modifier = rootScreenModifier,
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
                        onBack = if (state.compatibilityParticipantRole != null) {
                            viewModel::cancelCompatibilityParticipantCreate
                        } else {
                            viewModel::backToList
                        },
                        onOpenCase = viewModel::openDetailFromList,
                        onCreateGroup = viewModel::createAndSelectCaseGroup,
                        onFormChange = viewModel::updateForm,
                        onPreview = viewModel::previewCase,
                        onSubmit = { viewModel.submitCase() },
                        onConfirmDuplicate = { viewModel.submitCase(allowDuplicate = true) },
                        onConfirmFourPillarsLookup = viewModel::confirmFourPillarsLookup,
                        onPrepareBirthPickerToday = viewModel::prepareBirthPickerToday,
                        onOpenAlmanac = viewModel::openAlmanac,
                        onOpenBaziCompatibility = viewModel::openBaziCompatibility,
                        compatibilityParticipantRole = state.compatibilityParticipantRole,
                        bottomContentInset = 0.dp,
                        modifier = rootScreenModifier,
                    )
                    is AppDestination.CaseDetail -> {
                        val detailContent: @Composable (Modifier) -> Unit = { modifier ->
                            CaseDetailScreen(
                                state = state,
                                captureRegistry = caseDetailCaptureRegistry,
                                onBack = {
                                    if (state.detailIsTransient) {
                                        viewModel.closeTransientDetail()
                                    } else {
                                        viewModel.navigateBack()
                                    }
                                },
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
                                onAiCommentaryChange = viewModel::updateManualAiCommentary,
                                onSelectAiCommentaryVersion = viewModel::selectAiCommentaryVersion,
                                onAddNotesTimeline = viewModel::addCaseNotesTimeline,
                                onNotesTimelineContentChange =
                                    viewModel::updateCaseNotesTimelineContent,
                                onSaveCaseNotes = viewModel::saveCaseNotes,
                                onEnsureCaseNotesHydrated = viewModel::ensureCaseNotesHydrated,
                                onRetryPreparedSave = viewModel::retryPreparedCaseSave,
                                onReturnToPreparedSaveForm = viewModel::returnToPreparedCaseForm,
                                onDismissPreparedSaveDialog = viewModel::dismissPreparedSaveDialog,
                                onShowPreparedSaveDialog = viewModel::showPreparedSaveDialog,
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
                                onFortuneObservationChange =
                                    viewModel::updateFortuneObservation,
                                onFortuneObservationSelect =
                                    viewModel::selectProfessionalFortuneObservation,
                                onFortuneToday = viewModel::locateFortuneToday,
                                onAiPromptCopied = {
                                    transientMessage = "AI 指令已复制到剪贴板。"
                                },
                                onOpenAiCommentary = viewModel::openAiCommentary,
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
                                    onOpenCase = viewModel::openDetailFromList,
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
                    is AppDestination.EditCase -> EditCaseScreen(
                        form = state.editForm,
                        groupNames = state.metadataDraft.groupNames,
                        availableGroups = state.availableFormGroups,
                        error = state.mutationError,
                        saving = state.mutationSaving,
                        onBack = viewModel::navigateBack,
                        onFormChange = viewModel::updateEditForm,
                        onGroupNamesChange = { value ->
                            viewModel.updateMetadataDraft { it.copy(groupNames = value) }
                        },
                        onCreateGroup = viewModel::createAndSelectEditCaseGroup,
                        onSave = { viewModel.saveEditedCase() },
                        onCreateCopy = viewModel::createEditedCaseCopy,
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
                    if (shouldFloatRootNavigation) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 18.dp)
                                .padding(bottom = ROOT_NAVIGATION_BOTTOM_MARGIN),
                        ) {
                            RootNavigationBar(
                                destination = state.destination,
                                onOpenChart = openChart,
                                onOpenRecords = viewModel::backToList,
                                onOpenSettings = viewModel::openSettings,
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
            AiCommentaryDialogs(
                state = state.aiCommentary,
                onDismissCommentary = viewModel::dismissAiCommentary,
                onDismissServiceMenu = viewModel::closeAiServiceMenu,
                onOpenSettings = viewModel::openAiCommentarySettings,
                onDismissSettings = viewModel::closeAiCommentarySettings,
                onOpenHistory = viewModel::openAiCallHistory,
                onDismissHistory = viewModel::closeAiCallHistory,
                onSaveProvider = viewModel::saveAiCommentaryProvider,
                onSelectProvider = viewModel::selectAiCommentaryProvider,
                onPrivacyConfirmed = viewModel::setAiCommentaryPrivacyConfirmed,
                onGenerate = viewModel::generateAiCommentary,
                onContentChange = viewModel::updateAiCommentaryContent,
                onSaveCommentary = viewModel::saveAiCommentary,
            )
            state.caseImageConfirmationMode?.let { mode ->
                AlertDialog(
                    onDismissRequest = viewModel::cancelCaseImageConfirmation,
                    title = {
                        Text(
                            if (mode == CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE) {
                                "保存两张命盘长图"
                            } else {
                                "分享两张命盘长图"
                            },
                        )
                    },
                    text = {
                        Text(
                            if (mode == CaseImageDeliveryMode.SHARE_LONG_IMAGE) {
                                "将基本资料、基本排盘、专业细盘合并为一张长图；断事笔记（含命主反馈、师傅点评、AI 点评）生成另一张后打开分享。"
                            } else {
                                "将基本资料、基本排盘、专业细盘合并为一张长图；断事笔记（含命主反馈、师傅点评、AI 点评）生成另一张并保存到系统图库。"
                            },
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.confirmCaseImageDelivery { preparedMode, facts, fileNames ->
                                    val originalSection = state.detailSection
                                    coroutineScope.launch {
                                        when (
                                            val result = captureCaseDetailLongImage(
                                                rootView = rootView,
                                                registry = caseDetailCaptureRegistry,
                                                originalSection = originalSection,
                                                selectSection = viewModel::selectDetailSection,
                                                isSectionContentReady = { section ->
                                                    section != CaseDetailSection.FORTUNE ||
                                                        latestUiState.professionalFortunePosition != null ||
                                                        latestUiState.fortunePositionError != null
                                                },
                                            )
                                        ) {
                                            is CaseDetailLongImageCaptureResult.Success ->
                                                viewModel.completeCaseDetailPageCapture(
                                                    mode = preparedMode,
                                                    facts = facts,
                                                    fileNames = fileNames,
                                                    captured = result.images,
                                                ) { completedMode, completedFileNames ->
                                                    when (completedMode) {
                                                        CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE ->
                                                            onSaveCaseImagesToGallery(completedFileNames)
                                                        CaseImageDeliveryMode.SHARE_LONG_IMAGE ->
                                                            onSharePreparedCaseImages()
                                                    }
                                                }

                                            is CaseDetailLongImageCaptureResult.Rejected ->
                                                viewModel.reportCaseDetailPageCaptureFailed(
                                                    result.message,
                                                )
                                        }
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
                                    "保存"
                                } else {
                                    "分享"
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
                    title = { Text("导出完整备份") },
                    text = {
                        Text(
                            "默认导出 ZIP；文件含敏感命例资料，请妥善保存。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Text("导出")
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
                    title = "导出密码",
                    description = "输入至少 6 位密码，请妥善保管。",
                    error = state.fullBackupPasswordError,
                    requireConfirmation = false,
                    confirmLabel = "保存",
                    confirmTag = "confirm_password_full_backup_export",
                    passwordTag = "full_backup_password",
                    dismissLabel = "返回",
                    onConfirm = { password, _ ->
                        viewModel.confirmPasswordFullBackupExport(password)
                            ?.let(onCreateEncryptedFullBackupDocument)
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
                    title = { Text("导出当前命例") },
                    text = {
                        val attachmentCount = state.detail?.attachments?.size ?: 0
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "默认导出 JSON；文件含敏感命例资料，请妥善保存。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (attachmentCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "包含 $attachmentCount 张图片附件",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Switch(
                                        checked = state.singleCaseExportIncludesAttachments,
                                        onCheckedChange =
                                            viewModel::chooseSingleCaseExportAttachments,
                                        modifier = Modifier.testTag(
                                            "single_case_export_with_attachments",
                                        ),
                                    )
                                }
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
                            Text("保存")
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
                    title = "导出密码",
                    description = "输入至少 6 位密码，请妥善保管。",
                    error = state.singleCasePasswordError,
                    requireConfirmation = false,
                    confirmLabel = "保存",
                    confirmTag = "confirm_password_single_case_export",
                    dismissLabel = "返回",
                    onConfirm = { password, _ ->
                        viewModel.confirmPasswordSingleCaseExport(password)
                            ?.let(onCreateSingleCaseDocument)
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
                Text("姓名：${sourceCase.name.value ?: sourceCase.alias}", fontWeight = FontWeight.SemiBold)
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
    dismissLabel: String = "取消",
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
                Text(dismissLabel)
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
private val INNER_DISPLAY_HOME_MIN_WIDTH = 600.dp
private val ROOT_NAVIGATION_BAR_HEIGHT = 68.dp
private val ROOT_NAVIGATION_BOTTOM_MARGIN = 8.dp
private val ROOT_NAVIGATION_CONTENT_GAP = 10.dp
// Root navigation: 68dp surface + 8dp bottom gap + OPPO gesture area/shadow.
// Used only by the A–Z/# overlay; it must not create a content backing strip.
private val RECORD_ALPHABET_FLOATING_NAVIGATION_INSET = 112.dp

@Composable
private fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    MaterialAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        properties = properties,
    )
}

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
    modifier: Modifier = Modifier,
) {
    var optimisticSelectedTag by remember { mutableStateOf<String?>(null) }
    var pendingNavigationTag by remember { mutableStateOf<String?>(null) }
    val navigationShape = RoundedCornerShape(26.dp)
    val items = rootNavigationActions(
        destination = destination,
        onOpenChart = onOpenChart,
        onOpenRecords = onOpenRecords,
        onOpenSettings = onOpenSettings,
    )
    val latestItems by rememberUpdatedState(items)
    LaunchedEffect(destination) {
        if (pendingNavigationTag == null) {
            optimisticSelectedTag = null
        }
    }
    LaunchedEffect(pendingNavigationTag) {
        val targetTag = pendingNavigationTag ?: return@LaunchedEffect
        withFrameNanos { }
        latestItems.firstOrNull { it.tag == targetTag }?.onClick?.invoke()
        if (pendingNavigationTag == targetTag) {
            pendingNavigationTag = null
            optimisticSelectedTag = null
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ROOT_NAVIGATION_BAR_HEIGHT)
            // The root bar floats over page content: only its downward spot shadow should be visible.
            .graphicsLayer {
                shadowElevation = 12.dp.toPx()
                shape = navigationShape
                clip = false
                ambientShadowColor = Color.Transparent
                spotShadowColor = NanfengInk.copy(alpha = 0.30f)
            }
            .testTag("root_navigation"),
        shape = navigationShape,
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            items.forEach { item ->
                val selected = optimisticSelectedTag?.let { it == item.tag } ?: item.selected
                Surface(
                    onClick = {
                        if (!selected) {
                            optimisticSelectedTag = item.tag
                            // The keyed effect cancels an older target automatically.
                            pendingNavigationTag = item.tag
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(58.dp)
                        .semantics { contentDescription = item.label }
                        .testTag(item.tag),
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else NanfengNavigationMuted,
                        )
                        Text(
                            item.label,
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else NanfengNavigationMuted,
                        )
                    }
                }
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
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
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
                        onClick = { onOpenCase(summary.id) },
                        colors = if (summary.id == selectedCaseId) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        } else {
                            CardDefaults.cardColors()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
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
    bottomContentInset: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
                    bottom = bottomContentInset,
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
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
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
                    maxLines = 2,
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
    aiCommentaryState: AiCommentaryUiState,
    selectedSkin: BaziSkin,
    onSkinPreview: (BaziSkin) -> Unit,
    onSkinCommitted: (BaziSkin) -> Unit,
    screenshotImportState: ScreenshotImportUiState,
    onRatHourRuleChange: (RatHourRule) -> Unit,
    onImportScreenshots: () -> Unit,
    onImportSingleCase: () -> Unit,
    onImportWenzhen: () -> Unit,
    onImportCuratedCelebrityCatalog: () -> Unit,
    onExportFullBackup: () -> Unit,
    onRestoreFullBackup: () -> Unit,
    onOpenAiServicePage: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onOpenAiHistory: () -> Unit,
    cloudSyncCoordinator: BaziCloudSyncCoordinator?,
    googleSignInClient: BaziGoogleSignInClient?,
    activityContext: android.content.Context,
    bottomContentInset: Dp,
    modifier: Modifier = Modifier,
) {
    var showSkinPicker by rememberSaveable { mutableStateOf(false) }
    var showRatHourRulePicker by rememberSaveable { mutableStateOf(false) }
    var showAiServicePage by rememberSaveable { mutableStateOf(false) }
    var showCloudSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showAiServicePage) { showAiServicePage = false }
    if (showAiServicePage) {
        AiServiceSettingsPage(
            state = aiCommentaryState,
            onBack = { showAiServicePage = false },
            onOpenSettings = onOpenAiSettings,
            onOpenHistory = onOpenAiHistory,
            bottomContentInset = bottomContentInset,
            modifier = modifier,
        )
        return
    }
    BackHandler(enabled = showCloudSettings) { showCloudSettings = false }
    if (showCloudSettings && cloudSyncCoordinator != null && googleSignInClient != null) {
        BaziCloudSettingsPage(
            coordinator = cloudSyncCoordinator,
            googleSignInClient = googleSignInClient,
            activityContext = activityContext,
            onBack = { showCloudSettings = false },
            modifier = modifier,
        )
        return
    }
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
        SettingsGroupTitle("皮肤")
        BaziSkinSettingRow(
            selectedSkin = selectedSkin,
            onClick = { showSkinPicker = true },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SettingsGroupTitle("南枫云")
        SettingsActionGroup {
            val cloudState by (cloudSyncCoordinator?.state
                ?.collectAsStateWithLifecycle(initialValue = BaziCloudSyncState.Unconfigured)
                ?: remember { mutableStateOf(BaziCloudSyncState.Unconfigured) })
            SettingsActionRow(
                title = "Google 账号与同步",
                description = cloudDescription(cloudState),
                icon = Icons.Filled.Settings,
                onClick = { showCloudSettings = true },
                tag = "settings_cloud_sync",
                accent = NanfengGreen,
            )
        }
        SettingsGroupTitle("AI 点评")
        SettingsActionGroup {
            SettingsActionRow(
                title = "AI 模型服务",
                description = "模型设置与调用记录",
                icon = Icons.Filled.Settings,
                onClick = {
                    onOpenAiServicePage()
                    showAiServicePage = true
                },
                tag = "settings_ai_commentary",
                accent = NanfengGold,
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
                title = "恢复完整备份",
                description = "先核验清单再决定写入",
                icon = Icons.Filled.KeyboardArrowRight,
                onClick = onRestoreFullBackup,
                tag = "settings_restore_backup",
                accent = NanfengOrange,
            )
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
                title = "导入网页案例资料",
                description = "先核对数量；名人案例会自动纳入统一资料库",
                icon = Icons.Filled.List,
                onClick = onImportWenzhen,
                enabled = !state.wenzhenImportBusy,
                tag = "settings_import_wenzhen_web",
                accent = NanfengGold,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            SettingsActionRow(
                title = "名人案例统一资料库",
                description = "安装即自带；应用更新后自动同步。点此可重新同步",
                icon = Icons.Filled.List,
                onClick = onImportCuratedCelebrityCatalog,
                enabled = !state.curatedCelebrityImportBusy && !state.builtInCelebrityCatalogSyncBusy,
                tag = "settings_import_curated_celebrities",
                accent = NanfengGold,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            SettingsActionRow(
                title = "导入南枫命例包",
                description = "跨设备转移单个命例；先预览冲突再决定合并",
                icon = Icons.Filled.List,
                onClick = onImportSingleCase,
                tag = "settings_import_case",
            )
        }
        SettingsGroupTitle("排盘偏好")
        SettingsActionGroup {
            SettingsActionRow(
                title = "子时口径",
                description = if (state.defaultRatHourRule == RatHourRule.TYME_DEFAULT) {
                    "23:00 换日"
                } else {
                    "晚子时算当天"
                },
                icon = Icons.Filled.Settings,
                onClick = { showRatHourRulePicker = true },
                tag = "settings_rat_hour_rule",
                accent = NanfengGreen,
            )
        }
        Spacer(modifier = Modifier.height(bottomContentInset))
    }
    if (showSkinPicker) {
        BaziSkinPickerDialog(
            selectedSkin = selectedSkin,
            onSkinPreview = onSkinPreview,
            onSkinCommitted = onSkinCommitted,
            onDismissRequest = { showSkinPicker = false },
        )
    }
    if (showRatHourRulePicker) {
        AlertDialog(
            onDismissRequest = { showRatHourRulePicker = false },
            title = { Text("子时口径") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        RatHourRule.TYME_DEFAULT to "23:00 换日",
                        RatHourRule.LATE_RAT_SAME_DAY to "晚子时算当天",
                    ).forEach { (rule, label) ->
                        Surface(
                            onClick = {
                                onRatHourRuleChange(rule)
                                showRatHourRulePicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = if (state.defaultRatHourRule == rule) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = state.defaultRatHourRule == rule,
                                    onClick = null,
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRatHourRulePicker = false }) { Text("取消") }
            },
        )
    }
}

private fun cloudDescription(state: BaziCloudSyncState): String = when (state) {
    BaziCloudSyncState.Unconfigured -> "当前构建尚未配置云端"
    BaziCloudSyncState.SignedOut -> "登录后加密同步结构化命例"
    is BaziCloudSyncState.Ready -> state.message
    is BaziCloudSyncState.Working -> state.message
    is BaziCloudSyncState.RecoveryCodeReady -> "请安全保存一次性恢复码"
    BaziCloudSyncState.RecoveryCodeRequired -> "需要恢复码解锁云端数据"
    is BaziCloudSyncState.AccountEntryChoice -> "请确认本机与云端数据的处理方式"
    is BaziCloudSyncState.Failure -> state.message
}

@Composable
private fun AiServiceSettingsPage(
    state: AiCommentaryUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    bottomContentInset: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ai_service_settings_page"),
    ) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("ai_service_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回设置")
                }
                Text(
                    "AI 模型服务",
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomContentInset),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AiServiceDestinationCard(
                    title = "模型设置",
                    description = "已配置 ${state.configs.values.count { it.enabled && it.hasApiKey }} / " +
                        "${AiCommentaryProviderPresets.providerIds.size} 个服务",
                    icon = Icons.Filled.Settings,
                    onClick = onOpenSettings,
                    tag = "ai_service_open_settings",
                    accent = NanfengGold,
                )
                AiServiceDestinationCard(
                    title = "调用记录",
                    description = if (state.callRecords.isEmpty()) "暂无记录" else "最近 ${state.callRecords.size} 条",
                    icon = Icons.Filled.List,
                    onClick = onOpenHistory,
                    tag = "ai_service_open_history",
                    accent = NanfengGreen,
                )
            }
        }
    }
}

@Composable
private fun AiServiceDestinationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
    accent: Color,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            // Keep the requested compact 68 dp at normal text, while allowing the two text
            // lines to grow rather than top-clipping at an accessibility font scale.
            .heightIn(min = 68.dp)
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.13f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = accent,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CaseListScreen(
    state: StageTwoUiState,
    caseListState: LazyListState,
    onQueryChange: (String) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSelectSort: (CaseSortOrder) -> Unit,
    onSelectVisibility: (CaseVisibility) -> Unit,
    onSelectLibrary: (CaseLibraryType) -> Unit,
    onApplyAdvancedFilter: (CaseAdvancedFilter) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onCreateCelebrity: () -> Unit,
    onImportScreenshots: () -> Unit,
    screenshotImportState: ScreenshotImportUiState,
    onRetryScreenshotImport: () -> Unit,
    onConfirmScreenshotAiRecognition: () -> Unit,
    onCancelScreenshotAiRecognition: () -> Unit,
    onDeleteScreenshotImport: () -> Unit,
    onReviewScreenshotImport: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onReorderGroups: (List<String>) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onEnterBatchMode: () -> Unit,
    onUpdatePinnedCases: (Set<String>) -> Unit,
    onBatchDeleteCases: (Set<String>) -> Unit,
    onEditCase: (String) -> Unit,
    onTogglePinnedCase: (String) -> Unit,
    onPrefetchCase: (String) -> Unit,
    onOpenCase: (String) -> Unit,
    compatibilitySelectionRole: SexForFortuneDirection? = null,
    onSelectCompatibilityCase: ((String) -> Unit)? = null,
    onCancelCompatibilitySelection: (() -> Unit)? = null,
    bottomContentInset: Dp,
    alphabetIndexBottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val isCompatibilitySelection = compatibilitySelectionRole != null
    val compatibilityRoleLabel = if (compatibilitySelectionRole == SexForFortuneDirection.MAN) {
        "男方"
    } else {
        "女方"
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var showSortOptions by rememberSaveable { mutableStateOf(false) }
    var showGroupEditor by rememberSaveable { mutableStateOf(false) }
    var pinnedEditMode by rememberSaveable { mutableStateOf(false) }
    var pinnedSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteEditMode by rememberSaveable { mutableStateOf(false) }
    var deleteSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingSwipeDeleteCaseId by rememberSaveable { mutableStateOf<String?>(null) }
    var alphabetJumpMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val alphabetJumpScope = rememberCoroutineScope()
    var alphabetJumpJob by remember { mutableStateOf<Job?>(null) }
    val alphabetHaptics = rememberAppHapticFeedback()
    val firstCaseIndexByInitial = remember(state.cases) {
        state.cases.indexOfFirstBy { summary -> summary.displayCaseName().caseNameInitial() }
    }
    val firstPinnedCaseIndex = remember(state.cases) {
        state.cases.indexOfFirst { it.isPinned }.takeIf { it >= 0 }
    }
    LaunchedEffect(alphabetJumpMessage) {
        if (alphabetJumpMessage != null) {
            delay(1_600L)
            alphabetJumpMessage = null
        }
    }
    val alphabetActivationDistancePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val activeAlphabetInitial by remember(state.cases, alphabetActivationDistancePx) {
        derivedStateOf {
            val layoutInfo = caseListState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val upcomingSection = layoutInfo.visibleItemsInfo
                .asSequence()
                .filter { item ->
                    item.offset >= viewportStart &&
                        item.offset <= viewportStart + alphabetActivationDistancePx
                }
                .mapNotNull { item ->
                    val summary = state.cases.getOrNull(item.index) ?: return@mapNotNull null
                    val section = summary.recordSection()
                    val previousSection = state.cases.getOrNull(item.index - 1)?.recordSection()
                    section.takeIf { it != previousSection }
                }
                .lastOrNull()
            upcomingSection?.firstOrNull()
                ?: state.cases.getOrNull(caseListState.firstVisibleItemIndex)
                    ?.displayCaseName()
                    ?.caseNameInitial()
                ?: '#'
        }
    }
    val activeFilterCount = listOfNotNull(state.selectedGroupId, state.selectedTagId).size +
        state.advancedFilter.activeCategoryCount
    val emptyResultCriteria = buildList {
        state.query.trim().takeIf(String::isNotEmpty)?.let { query ->
            add("关键词“$query”")
        }
        state.selectedGroupId?.let { groupId ->
            state.availableGroups.firstOrNull { it.id == groupId }?.name?.let { groupName ->
                add("分组“$groupName”")
            } ?: add("所选分组")
        }
        state.selectedTagId?.let { tagId ->
            state.availableTags.firstOrNull { it.id == tagId }?.name?.let { tagName ->
                add("标签“$tagName”")
            } ?: add("所选标签")
        }
        if (state.advancedFilter.activeCategoryCount > 0) {
            add("其他筛选条件")
        }
    }
    val visibleDeleteCaseIds = state.cases.mapTo(linkedSetOf()) { it.id }
    val allVisibleDeleteCasesSelected = deleteEditMode &&
        visibleDeleteCaseIds.isNotEmpty() &&
        visibleDeleteCaseIds.all { it in deleteSelection }
    BackHandler(enabled = pinnedEditMode || deleteEditMode) {
        pinnedEditMode = false
        pinnedSelection = emptySet()
        deleteEditMode = false
        deleteSelection = emptySet()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("case_list_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
                .testTag("record_toolbar"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isCompatibilitySelection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = requireNotNull(onCancelCompatibilitySelection),
                        modifier = Modifier.width(64.dp),
                    ) {
                        Text("返回")
                    }
                    Text(
                        text = "选择${compatibilityRoleLabel}八字",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_visibility_switcher"),
                    shape = RecordToolbarPillShape,
                    color = Color(0xFFF9F9F8),
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        RecordTopTab(
                            text = "用户列表 ${state.libraryCaseCounts[CaseLibraryType.USER] ?: 0}",
                            selected = state.visibility == CaseVisibility.ACTIVE &&
                                state.libraryType == CaseLibraryType.USER,
                            onClick = { onSelectLibrary(CaseLibraryType.USER) },
                            modifier = Modifier.weight(1f),
                            tag = "visibility_active",
                            accent = NanfengGreen,
                        )
                        RecordTopTab(
                            text = "名人案例 ${state.libraryCaseCounts[CaseLibraryType.CELEBRITY] ?: 0}",
                            selected = state.visibility == CaseVisibility.ACTIVE &&
                                state.libraryType == CaseLibraryType.CELEBRITY,
                            onClick = { onSelectLibrary(CaseLibraryType.CELEBRITY) },
                            modifier = Modifier.weight(1f),
                            tag = "visibility_celebrity",
                            accent = NanfengGoldText,
                        )
                        RecordTopTab(
                            text = "回收站 ${state.trashedCaseCount}",
                            selected = state.visibility == CaseVisibility.TRASHED,
                            onClick = { onSelectVisibility(CaseVisibility.TRASHED) },
                            modifier = Modifier.weight(1f),
                            tag = "visibility_trashed",
                            accent = NanfengSolarTermRed,
                        )
                    }
                }
                Box {
                    Surface(
                        onClick = { moreExpanded = true },
                        modifier = Modifier
                            .size(width = 68.dp, height = 44.dp)
                            .semantics { contentDescription = "更多命例操作" }
                            .testTag("record_more"),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = NanfengGreen,
                            )
                        }
                    }
                    NanfengWhiteDropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false },
                    ) {
                        NanfengOverflowMenuItem(
                            label = "新增名人案例",
                            icon = Icons.Filled.Add,
                            accent = NanfengGoldText,
                            onClick = {
                                moreExpanded = false
                                onCreateCelebrity()
                            },
                            modifier = Modifier.testTag("record_more_create_celebrity"),
                        )
                        NanfengOverflowMenuItem(
                            label = "导入问真截图",
                            icon = Icons.Filled.Search,
                            accent = NanfengOrange,
                            onClick = {
                                moreExpanded = false
                                onImportScreenshots()
                            },
                            enabled = !screenshotImportState.busy,
                            modifier = Modifier.testTag("import_screenshots_button"),
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        NanfengOverflowMenuItem(
                            label = "列表排序",
                            icon = Icons.Filled.List,
                            accent = NanfengGreen,
                            onClick = {
                                moreExpanded = false
                                showSortOptions = true
                            },
                            modifier = Modifier.testTag("record_more_sort"),
                        )
                        if (state.visibility != CaseVisibility.TRASHED) {
                            NanfengOverflowMenuItem(
                                label = "分组管理",
                                icon = Icons.Filled.Edit,
                                accent = NanfengGreen,
                                onClick = {
                                    moreExpanded = false
                                    showGroupEditor = true
                                },
                                modifier = Modifier.testTag("record_more_groups"),
                            )
                        }
                        NanfengOverflowMenuItem(
                            label = "置顶八字",
                            icon = Icons.Filled.Star,
                            accent = NanfengGoldText,
                            onClick = {
                                moreExpanded = false
                                onEnterBatchMode()
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                pinnedSelection = state.batchCases
                                    .filter { it.isPinned }
                                    .map { it.id }
                                    .toSet()
                                pinnedEditMode = true
                            },
                            modifier = Modifier.testTag("record_more_pinned"),
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        NanfengOverflowMenuItem(
                            label = "批量删除",
                            icon = Icons.Filled.Delete,
                            accent = NanfengSolarTermRed,
                            onClick = {
                                moreExpanded = false
                                onEnterBatchMode()
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                deleteSelection = emptySet()
                                deleteEditMode = true
                            },
                            modifier = Modifier.testTag("record_more_delete"),
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
                    .testTag("case_search"),
                placeholder = {
                    Text(
                        "搜索姓名或四柱",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    Row(
                        modifier = Modifier
                            .width(if (isCompatibilitySelection) 48.dp else 128.dp)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            4.dp,
                            Alignment.End,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    enabled = state.query.isNotEmpty(),
                                    onClickLabel = "删除一个搜索字",
                                    onLongClickLabel = "清空搜索文字",
                                    onLongClick = { onQueryChange("") },
                                    onClick = {
                                        onQueryChange(state.query.dropLastTextElement())
                                    },
                                )
                                .semantics {
                                    contentDescription = "删除搜索文字，长按清空"
                                }
                                .testTag("record_search_delete"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (state.query.isNotEmpty()) 0.82f else 0.30f,
                                ),
                            )
                        }
                        if (!isCompatibilitySelection) Surface(
                            onClick = { showAdvancedFilters = true },
                            modifier = Modifier
                                .height(34.dp)
                                .width(72.dp)
                                .semantics {
                                    contentDescription = "打开记录筛选"
                                }
                                .testTag("record_filter_toggle"),
                            shape = RoundedCornerShape(17.dp),
                            color = if (activeFilterCount > 0) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                NanfengControlSurface
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    4.dp,
                                    Alignment.CenterHorizontally,
                                ),
                            ) {
                                Icon(
                                    Icons.Filled.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (activeFilterCount > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Text(
                                    if (activeFilterCount > 0) "筛选 $activeFilterCount" else "筛选",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (activeFilterCount > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RecordToolbarPillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (!isCompatibilitySelection) ScreenshotImportSummary(
            state = screenshotImportState,
            onRetry = onRetryScreenshotImport,
            onConfirmAiRecognition = onConfirmScreenshotAiRecognition,
            onCancelAiRecognition = onCancelScreenshotAiRecognition,
            onDelete = onDeleteScreenshotImport,
            onReview = onReviewScreenshotImport,
        )
        if (!isCompatibilitySelection && state.visibility != CaseVisibility.TRASHED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("record_filter_strip"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecordCategoryTab(
                    text = "全部 ${state.libraryCaseCounts[state.libraryType] ?: 0}",
                    selected = state.selectedGroupId == null && state.selectedTagId == null,
                    onClick = { onSelectGroup(null); onSelectTag(null) },
                    tag = "record_filter_all",
                )
                state.availableGroups.forEach { group ->
                    RecordCategoryTab(
                        text = "${group.name} ${state.groupCaseCounts[group.id] ?: 0}",
                        selected = state.selectedGroupId == group.id,
                        onClick = { onSelectGroup(group.id) },
                        tag = "record_group_${group.id}",
                    )
                }
            }
        } else if (!isCompatibilitySelection) {
            // 回收站没有分组筛选，但必须保留活动列表同等高度，避免切换时内容上跳。
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("record_filter_strip_placeholder"),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        when {
            state.listLoading && state.cases.isEmpty() -> LoadingBox("正在读取命例…")
            state.listError != null -> ErrorBox(
                message = state.listError,
                actionLabel = "重试",
                onAction = onRefresh,
            )
            state.cases.isEmpty() -> EmptyCaseList(
                visibility = state.visibility,
                libraryType = state.libraryType,
                libraryCaseCount = state.libraryCaseCounts[state.libraryType] ?: 0,
                activeCriteria = emptyResultCriteria,
                onCreate = if (state.libraryType == CaseLibraryType.CELEBRITY) {
                    onCreateCelebrity
                } else {
                    onCreate
                },
                onClearConditions = {
                    onQueryChange("")
                    onClearFilters()
                },
            )
            else -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White),
            ) {
                LazyColumn(
                    state = caseListState,
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = maxOf(
                            if (pinnedEditMode || deleteEditMode) 92.dp else 16.dp,
                            bottomContentInset + 16.dp,
                        ),
                    ),
                ) {
                    itemsIndexed(state.cases, key = { _, item -> item.id }) { index, summary ->
                        LaunchedEffect(summary.id) {
                            onPrefetchCase(summary.id)
                        }
                        val section = summary.recordSection()
                        val previousSection = state.cases.getOrNull(index - 1)?.recordSection()
                        if (section != previousSection) {
                            Text(
                                section,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isCompatibilitySelection) {
                            CaseSummaryRow(
                                summary = summary,
                                onClick = { requireNotNull(onSelectCompatibilityCase)(summary.id) },
                            )
                        } else {
                            Box(modifier = Modifier.padding(end = 28.dp)) {
                                SwipeableCaseSummaryRow(
                                    summary = summary,
                                    selectionMode = pinnedEditMode || deleteEditMode,
                                    selected = if (pinnedEditMode) {
                                        summary.id in pinnedSelection
                                    } else {
                                        summary.id in deleteSelection
                                    },
                                    onClick = {
                                        if (pinnedEditMode) {
                                            pinnedSelection = if (summary.id in pinnedSelection) {
                                                pinnedSelection - summary.id
                                            } else {
                                                pinnedSelection + summary.id
                                            }
                                        } else if (deleteEditMode) {
                                            if (summary.id in deleteSelection) {
                                                deleteSelection = deleteSelection - summary.id
                                            } else {
                                                deleteSelection = deleteSelection + summary.id
                                            }
                                        } else {
                                            onOpenCase(summary.id)
                                        }
                                    },
                                    onEdit = { onEditCase(summary.id) },
                                    onTogglePinned = { onTogglePinnedCase(summary.id) },
                                    onDelete = { pendingSwipeDeleteCaseId = summary.id },
                                )
                            }
                        }
                    }
                }
                // 索引的几何参照是可见案例区而不是整个根内容：浮动主导航不参与垂直居中。
                // 让上下留白相等后略放大索引本身，避免过度收紧。
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(bottom = alphabetIndexBottomInset),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    RecordAlphabetIndex(
                        activeInitial = activeAlphabetInitial,
                        pinnedActive = activeAlphabetInitial == '星',
                        onPinnedClick = {
                            val targetIndex = firstPinnedCaseIndex
                            if (targetIndex == null) {
                                alphabetJumpMessage = "当前列表没有置顶命例"
                            } else {
                                alphabetJumpJob?.cancel()
                                alphabetHaptics.perform(AppHapticEvent.SELECTION)
                                alphabetJumpJob = alphabetJumpScope.launch {
                                    // 置顶锚点就是列表首项；不能套用字母索引的“目标附近预定位”，
                                    // 否则会先闪到 A 分组再回到置顶分组。
                                    caseListState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        onInitialClick = { initial ->
                            val targetIndex = firstCaseIndexByInitial[initial]
                            if (targetIndex == null) {
                                alphabetJumpMessage = "当前列表没有以「$initial」开头的案例"
                            } else {
                                alphabetJumpJob?.cancel()
                                alphabetHaptics.perform(AppHapticEvent.SELECTION)
                                alphabetJumpJob = alphabetJumpScope.launch {
                                    caseListState.smoothAlphabetScrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .offset(y = 10.dp)
                            .padding(end = 3.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
                alphabetJumpMessage?.let { message ->
                    Surface(
                        modifier = Modifier.align(Alignment.Center).testTag("record_alphabet_empty_hint"),
                        shape = RoundedCornerShape(14.dp),
                        color = NanfengInk.copy(alpha = 0.86f),
                    ) {
                        Text(message, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
                if (pinnedEditMode || deleteEditMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = bottomContentInset)
                            .testTag("record_batch_action_bar"),
                        color = Color.White,
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (deleteEditMode) {
                                OutlinedButton(
                                    onClick = {
                                        if (allVisibleDeleteCasesSelected) {
                                            deleteSelection = deleteSelection - visibleDeleteCaseIds
                                        } else {
                                            deleteSelection = deleteSelection + visibleDeleteCaseIds
                                        }
                                    },
                                    enabled = visibleDeleteCaseIds.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("record_batch_select_all"),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(
                                        if (allVisibleDeleteCasesSelected) "取消全选" else "全选",
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (pinnedEditMode) {
                                        onUpdatePinnedCases(pinnedSelection)
                                    } else {
                                        onBatchDeleteCases(deleteSelection.toSet())
                                    }
                                    pinnedEditMode = false
                                    pinnedSelection = emptySet()
                                    deleteEditMode = false
                                    deleteSelection = emptySet()
                                },
                                enabled = pinnedEditMode || deleteSelection.isNotEmpty(),
                                modifier = Modifier.weight(1f).height(52.dp)
                                    .testTag("record_batch_confirm"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (deleteEditMode) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        NanfengGold
                                    },
                                ),
                            ) {
                                Text(
                                    if (deleteEditMode) {
                                        "删除 ${deleteSelection.size}"
                                    } else {
                                        "置顶"
                                    },
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    pinnedEditMode = false
                                    pinnedSelection = emptySet()
                                    deleteEditMode = false
                                    deleteSelection = emptySet()
                                },
                                modifier = Modifier.weight(1f).height(52.dp)
                                    .testTag("record_batch_cancel"),
                            ) { Text("取消") }
                        }
                    }
                }
            }
        }
        if (showAdvancedFilters) {
            RecordAdvancedFilterDialog(
                applied = state.advancedFilter,
                availableSeasonalStates = state.availableSeasonalWuxingStates,
                availableShenSha = state.availableShenSha,
                onDismiss = { showAdvancedFilters = false },
                onReset = {
                    onClearFilters()
                    showAdvancedFilters = false
                },
                onApply = {
                    onApplyAdvancedFilter(it)
                    showAdvancedFilters = false
                },
            )
        }
        if (showSortOptions) {
            RecordSortDialog(
                selected = state.sortOrder,
                onDismiss = { showSortOptions = false },
                onConfirm = {
                    onSelectSort(it)
                    showSortOptions = false
                },
            )
        }
        if (showGroupEditor) {
            RecordGroupEditorDialog(
                groups = state.availableGroups,
                cases = state.batchCases,
                readOnly = state.visibility == CaseVisibility.ACTIVE &&
                    state.libraryType == CaseLibraryType.CELEBRITY,
                saving = state.mutationSaving,
                onDismiss = { showGroupEditor = false },
                onCreate = onCreateGroup,
                onRename = onRenameGroup,
                onReorder = onReorderGroups,
                onDelete = onDeleteGroup,
            )
        }
        pendingSwipeDeleteCaseId?.let { caseId ->
            AlertDialog(
                onDismissRequest = { pendingSwipeDeleteCaseId = null },
                title = { Text("移入回收站？") },
                text = { Text("该命例会从主列表隐藏，原有记录和计算历史仍保留。") },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingSwipeDeleteCaseId = null
                            onBatchDeleteCases(setOf(caseId))
                        },
                        modifier = Modifier.testTag("confirm_swipe_case_delete"),
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSwipeDeleteCaseId = null }) {
                        Text("取消")
                    }
                },
            )
        }
    }
}

@Composable
private fun RecordAdvancedFilterDialog(
    applied: CaseAdvancedFilter,
    availableSeasonalStates: List<String>,
    availableShenSha: List<String>,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (CaseAdvancedFilter) -> Unit,
) {
    var draft by remember(applied) { mutableStateOf(applied) }
    var activePillarIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var activePillarTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var seasonalExpanded by rememberSaveable { mutableStateOf(false) }
    var shenShaExpanded by rememberSaveable { mutableStateOf(false) }
    var showBirthplacePicker by rememberSaveable { mutableStateOf(false) }
    val stems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val tenGods = listOf("比肩", "劫财", "食神", "伤官", "偏财", "正财", "七杀", "正官", "偏印", "正印")
    val seasonalOptions = (
        availableSeasonalStates + listOf(
            "木旺", "火相", "水休", "金囚", "土死",
            "火旺", "土相", "木休", "水囚", "金死",
            "金旺", "水相", "土休", "火囚", "木死",
            "水旺", "木相", "金休", "土囚", "火死",
        )
    ).distinct()
    val shenShaOptions = (
        availableShenSha + listOf(
            "天乙贵人", "太极贵人", "文昌贵人", "国印贵人",
            "金舆", "禄神", "羊刃", "驿马", "桃花", "华盖", "将星", "红鸾",
        )
    ).distinct()
    Dialog(
        onDismissRequest = {
            if (activePillarIndex != null) {
                activePillarIndex = null
                activePillarTarget = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.34f))
                .testTag("record_filter_panel"),
        ) {
            val drawerWidth = minOf(maxWidth * 0.90f, 430.dp)
            Row(Modifier.fillMaxSize()) {
                Spacer(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onDismiss)
                        .testTag("filter_outside_scrim"),
                )
                Surface(
                    modifier = Modifier.fillMaxHeight().width(drawerWidth),
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                ) {
                    Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("记录筛选", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("filter_close")) {
                            Text("关闭")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        RecordFilterSection("性别") {
                            HomeChoiceGroup(
                                options = listOf(
                                    "男" to (draft.sex == SexForFortuneDirection.MAN),
                                    "女" to (draft.sex == SexForFortuneDirection.WOMAN),
                                ),
                                onSelect = { value ->
                                    val sex = if (value == "男") {
                                        SexForFortuneDirection.MAN
                                    } else {
                                        SexForFortuneDirection.WOMAN
                                    }
                                    draft = draft.copy(sex = sex.takeUnless { it == draft.sex })
                                },
                                tags = listOf("filter_gender_男", "filter_gender_女"),
                                itemWidth = 156.dp,
                                itemHeight = 40.dp,
                            )
                        }
                        RecordFilterSection("干支") {
                            ElementFilterChoiceGrid(
                                options = stems + branches,
                                selected = draft.ganZhi.map(Char::toString).toSet(),
                                columns = 10,
                                tagPrefix = "filter_ganzhi",
                            ) { value ->
                                val selected = value.single()
                                draft = draft.copy(
                                    ganZhi = draft.ganZhi.toggle(selected),
                                )
                            }
                        }
                        RecordFilterSection("四柱") {
                            val pillarFilters = listOf(
                                draft.fourPillars.year,
                                draft.fourPillars.month,
                                draft.fourPillars.day,
                                draft.fourPillars.hour,
                            )
                            val pillarLabels = listOf("年柱", "月柱", "日柱", "时柱")
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                pillarLabels.forEach { label ->
                                    Text(
                                        label,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                pillarFilters.forEachIndexed { index, filter ->
                                    PillarFilterCell(
                                        character = filter.stem,
                                        tenGod = filter.stemTenGod,
                                        onClick = {
                                            activePillarIndex = index
                                            activePillarTarget = "stem"
                                        },
                                        modifier = Modifier.weight(1f).testTag("filter_pillar_stem_$index"),
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                pillarFilters.forEachIndexed { index, filter ->
                                    PillarFilterCell(
                                        character = filter.branch,
                                        tenGod = filter.branchTenGod,
                                        onClick = {
                                            activePillarIndex = index
                                            activePillarTarget = "branch"
                                        },
                                        modifier = Modifier.weight(1f).testTag("filter_pillar_branch_$index"),
                                    )
                                }
                            }
                        }
                        RecordFilterSection("出生地区") {
                            HomePickerRow(
                                title = "选择",
                                value = draft.birthRegion.ifBlank { "请选择地区" },
                                supporting = "",
                                onClick = { showBirthplacePicker = true },
                                tag = "filter_birth_region",
                            )
                        }
                        RecordExpandableFilterSection(
                            title = "旺相休囚死",
                            expanded = seasonalExpanded,
                            onToggle = { seasonalExpanded = !seasonalExpanded },
                            tag = "filter_seasonal_toggle",
                        ) {
                            FilterChoiceGrid(
                                options = seasonalOptions,
                                selected = draft.seasonalWuxingStates,
                                columns = 3,
                                tagPrefix = "filter_seasonal",
                            ) { draft = draft.copy(seasonalWuxingStates = draft.seasonalWuxingStates.toggle(it)) }
                        }
                        RecordExpandableFilterSection(
                            title = "神煞",
                            expanded = shenShaExpanded,
                            onToggle = { shenShaExpanded = !shenShaExpanded },
                            tag = "filter_shensha_toggle",
                        ) {
                            FilterChoiceGrid(
                                options = shenShaOptions,
                                selected = draft.shenSha,
                                columns = 3,
                                tagPrefix = "filter_shensha",
                            ) { draft = draft.copy(shenSha = draft.shenSha.toggle(it)) }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("filter_reset"),
                        ) { Text("重置") }
                        Button(
                            onClick = { onApply(draft) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("filter_apply"),
                            colors = ButtonDefaults.buttonColors(containerColor = NanfengGold),
                        ) { Text("确定") }
                    }
                    Spacer(Modifier.height(34.dp))
                    }
                    val pillarIndex = activePillarIndex
                    val pillarTarget = activePillarTarget
                    if (pillarIndex != null && pillarTarget != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.30f)),
                        )
                        Column(Modifier.fillMaxSize()) {
                            Spacer(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clickable {
                                        activePillarIndex = null
                                        activePillarTarget = null
                                    }
                                    .testTag("pillar_picker_scrim"),
                            )
                            PillarFilterSelectionSheet(
                                pillarLabel = listOf("年柱", "月柱", "日柱", "时柱")[pillarIndex],
                                target = pillarTarget,
                                filter = listOf(
                                    draft.fourPillars.year,
                                    draft.fourPillars.month,
                                    draft.fourPillars.day,
                                    draft.fourPillars.hour,
                                )[pillarIndex],
                                stems = stems,
                                branches = branches,
                                tenGods = tenGods,
                                onCharacterSelected = { selected ->
                                    val current = listOf(
                                        draft.fourPillars.year,
                                        draft.fourPillars.month,
                                        draft.fourPillars.day,
                                        draft.fourPillars.hour,
                                    )[pillarIndex]
                                    val updated = if (pillarTarget == "stem") {
                                        current.copy(stem = selected)
                                    } else {
                                        current.copy(branch = selected)
                                    }
                                    draft = draft.copy(
                                        fourPillars = draft.fourPillars.updated(
                                            pillarIndex,
                                            updated,
                                        ),
                                    )
                                },
                                onTenGodSelected = { selected ->
                                    val current = listOf(
                                        draft.fourPillars.year,
                                        draft.fourPillars.month,
                                        draft.fourPillars.day,
                                        draft.fourPillars.hour,
                                    )[pillarIndex]
                                    val updated = if (pillarTarget == "stem") {
                                        current.copy(stemTenGod = selected)
                                    } else {
                                        current.copy(branchTenGod = selected)
                                    }
                                    draft = draft.copy(
                                        fourPillars = draft.fourPillars.updated(
                                            pillarIndex,
                                            updated,
                                        ),
                                    )
                                },
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color.White),
                            )
                        }
                    }
                    }
                }
            }
        }
    }
    if (showBirthplacePicker) {
        BirthplacePickerSheet(
            form = CaseFormState(locationName = draft.birthRegion),
            onDismiss = { showBirthplacePicker = false },
            onConfirm = { place ->
                draft = draft.copy(birthRegion = place.displayName)
                showBirthplacePicker = false
            },
        )
    }
}

@Composable
private fun PillarFilterSelectionSheet(
    pillarLabel: String,
    target: String,
    filter: PillarCharacterFilter,
    stems: List<String>,
    branches: List<String>,
    tenGods: List<String>,
    onCharacterSelected: (Char?) -> Unit,
    onTenGodSelected: (String?) -> Unit,
) {
    val selectingStem = target == "stem"
    val characterOptions = if (selectingStem) stems else branches
    val selectedCharacter = if (selectingStem) filter.stem else filter.branch
    val selectedTenGod = if (selectingStem) filter.stemTenGod else filter.branchTenGod
    val targetLabel = if (selectingStem) "干" else "支"
    val targetTag = if (selectingStem) "stem" else "branch"
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("pillar_filter_sheet"),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "请选择 $pillarLabel-$targetLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ElementFilterChoiceGrid(
                options = characterOptions + "-",
                selected = setOf(selectedCharacter?.toString() ?: "-"),
                columns = 5,
                tagPrefix = "pillar_picker_$targetTag",
                minHeight = 36.dp,
            ) { value ->
                onCharacterSelected(value.singleOrNull()?.takeUnless { it == '-' })
            }
            Text(
                "请选择 $pillarLabel-十神",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PillarPickerTextGrid(
                options = tenGods + "-",
                selected = setOf(selectedTenGod ?: "-"),
                columns = 5,
                tagPrefix = "pillar_picker_${targetTag}_tengod",
            ) { value ->
                onTenGodSelected(value.takeUnless { it == "-" })
            }
        }
    }
}

@Composable
private fun RecordSortDialog(
    selected: CaseSortOrder,
    onDismiss: () -> Unit,
    onConfirm: (CaseSortOrder) -> Unit,
) {
    var draft by remember(selected) { mutableStateOf(selected.toPublicRecordSort()) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f))
                .testTag("record_sort_sheet"),
        ) {
            Column(Modifier.fillMaxSize()) {
                Spacer(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss)
                        .testTag("record_sort_scrim"),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = Color.White,
                ) {
                    Column(
                    modifier = Modifier.navigationBarsPadding()
                        .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 42.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Text(
                            "列表排序方式",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = { onConfirm(draft) },
                            colors = ButtonDefaults.buttonColors(containerColor = NanfengGold),
                            modifier = Modifier.testTag("record_sort_confirm"),
                        ) { Text("确定") }
                    }
                    listOf(
                        CaseSortOrder.NAME_ASC to "姓名排序",
                        CaseSortOrder.UPDATED_DESC to "最近编辑",
                        CaseSortOrder.BIRTH_ASC to "出生时间",
                    ).forEach { (value, label) ->
                        FilterChoiceChip(
                            text = label,
                            selected = draft == value,
                            onClick = { draft = value },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                .testTag("record_sort_${value.name.lowercase()}"),
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordGroupEditorDialog(
    groups: List<CaseGroup>,
    cases: List<CaseSummary>,
    readOnly: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onDelete: (String) -> Unit,
) {
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<CaseGroup?>(null) }
    var displayedGroups by remember(groups) { mutableStateOf(groups) }
    var draggedGroupId by remember { mutableStateOf<String?>(null) }
    var draggedOffsetY by remember { mutableStateOf(0f) }
    val displayedGroupsState by rememberUpdatedState(displayedGroups)
    val groupListState = rememberLazyListState()

    LaunchedEffect(groups) {
        if (draggedGroupId == null) displayedGroups = groups
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .testTag("record_group_editor"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
                    .testTag("record_group_editor_scrim"),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 18.dp, top = 18.dp, end = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "全部分组",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onDismiss) { Text("完成") }
                    }
                    Text(
                        if (readOnly) "内置职业分组（${groups.size}）" else "所有分组（${groups.size}）",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (readOnly) {
                        Text(
                            "名人案例的职业分组由内置统一资料库维护，列表、筛选和数量使用同一目录。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                        color = NanfengControlSurface,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("全部（${cases.size}）", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = groupListState,
                    ) {
                        if (displayedGroups.isEmpty()) {
                            item {
                                Text(
                                    if (readOnly) "内置职业分组正在同步，请稍后重试。" else "暂无分组，点击底部“添加”创建第一个分组。",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 22.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        itemsIndexed(displayedGroups, key = { _, group -> group.id }) { _, group ->
                            val count = cases.count { summary ->
                                summary.groups.any { it.id == group.id }
                            }
                            if (editingGroupId == group.id) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        modifier = Modifier.weight(1f)
                                            .testTag("record_group_name_${group.id}"),
                                        singleLine = true,
                                    )
                                    TextButton(
                                        onClick = {
                                            onRename(group.id, editName)
                                            editingGroupId = null
                                        },
                                        enabled = editName.isNotBlank() &&
                                            editName != group.name && !saving,
                                    ) { Text("保存") }
                                    TextButton(onClick = { editingGroupId = null }) { Text("取消") }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 68.dp)
                                        .padding(horizontal = 4.dp)
                                        .graphicsLayer {
                                            translationY = if (draggedGroupId == group.id) {
                                                draggedOffsetY
                                            } else {
                                                0f
                                            }
                                        }
                                        .zIndex(if (draggedGroupId == group.id) 1f else 0f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "${group.name}（$count）",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    IconButton(
                                        onClick = {
                                            editingGroupId = group.id
                                            editName = group.name
                                        },
                                        enabled = !saving && !readOnly,
                                        modifier = Modifier.testTag("record_group_rename_${group.id}"),
                                    ) { Icon(Icons.Filled.Edit, "重命名${group.name}") }
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("record_group_reorder_${group.id}")
                                            .semantics {
                                                contentDescription = "拖动排序${group.name}"
                                            }
                                            .pointerInput(group.id, saving, readOnly) {
                                            if (!saving && !readOnly) {
                                                    detectVerticalDragGestures(
                                                        onDragStart = {
                                                            draggedGroupId = group.id
                                                            draggedOffsetY = 0f
                                                        },
                                                        onVerticalDrag = { change, dragAmount ->
                                                            change.consume()
                                                            val currentGroups = displayedGroupsState
                                                            val currentIndex = currentGroups.indexOfFirst {
                                                                it.id == group.id
                                                            }
                                                            if (currentIndex < 0) return@detectVerticalDragGestures
                                                            draggedOffsetY += dragAmount
                                                            val draggedItem = groupListState.layoutInfo
                                                                .visibleItemsInfo
                                                                .firstOrNull { it.key == group.id }
                                                                ?: return@detectVerticalDragGestures
                                                            val draggedCenter = draggedItem.offset +
                                                                draggedOffsetY + draggedItem.size / 2f
                                                            val targetItem = groupListState.layoutInfo
                                                                .visibleItemsInfo
                                                                .firstOrNull { item ->
                                                                    item.key != group.id &&
                                                                        draggedCenter in item.offset.toFloat()..
                                                                            (item.offset + item.size).toFloat()
                                                                }
                                                            val targetIndex = targetItem?.let { item ->
                                                                currentGroups.indexOfFirst { it.id == item.key }
                                                            } ?: -1
                                                            if (targetIndex >= 0 && targetIndex != currentIndex) {
                                                                displayedGroups = currentGroups.toMutableList().apply {
                                                                    add(targetIndex, removeAt(currentIndex))
                                                                }
                                                                draggedOffsetY = 0f
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            val orderedIds = displayedGroupsState.map { it.id }
                                                            if (orderedIds != groups.map { it.id }) onReorder(orderedIds)
                                                            draggedGroupId = null
                                                            draggedOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggedGroupId = null
                                                            draggedOffsetY = 0f
                                                            displayedGroups = groups
                                                        },
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.List, contentDescription = null)
                                    }
                                    IconButton(
                                        onClick = { pendingDelete = group },
                                        enabled = !saving && !readOnly,
                                        modifier = Modifier.testTag("record_group_delete_${group.id}"),
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "删除${group.name}",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                    if (adding && !readOnly) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = NanfengControlSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = newGroupName,
                                    onValueChange = { newGroupName = it },
                                    modifier = Modifier.weight(1f).testTag("record_group_new_name"),
                                    placeholder = { Text("输入新分组名称") },
                                    singleLine = true,
                                )
                                Button(
                                    onClick = {
                                        onCreate(newGroupName)
                                        newGroupName = ""
                                        adding = false
                                    },
                                    enabled = newGroupName.isNotBlank() && !saving,
                                    modifier = Modifier.testTag("record_group_add_confirm"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NanfengNavigation,
                                    ),
                                ) { Text("创建") }
                                IconButton(onClick = { adding = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "取消新增分组")
                                }
                            }
                        }
                    } else if (!readOnly) {
                        Button(
                            onClick = { adding = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("record_group_add"),
                            colors = ButtonDefaults.buttonColors(containerColor = NanfengNavigation),
                        ) { Text("添加") }
                    }
                    Spacer(
                        Modifier
                            .height(GroupDialogBottomSafetySpace)
                            .testTag("record_group_bottom_safety_space"),
                    )
                }
            }
        }
    }
    pendingDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除分组") },
            text = { Text("确定删除“${group.name}”吗？命例本身不会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(group.id)
                        pendingDelete = null
                    },
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RecordCaseSelectionDialog(
    title: String,
    description: String,
    cases: List<CaseSummary>,
    initialSelection: Set<String>,
    confirmLabel: String,
    saving: Boolean,
    testTagPrefix: String,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    destructive: Boolean = false,
) {
    var selected by remember(cases, initialSelection) { mutableStateOf(initialSelection) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .testTag("${testTagPrefix}_dialog"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
                    .testTag("${testTagPrefix}_scrim"),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Text(
                            title,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = { onConfirm(selected) },
                            enabled = !saving && (!destructive || selected.isNotEmpty()),
                            colors = if (destructive) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                )
                            } else {
                                ButtonDefaults.buttonColors(containerColor = NanfengGold)
                            },
                            modifier = Modifier.testTag("${testTagPrefix}_confirm"),
                        ) { Text(confirmLabel) }
                    }
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (cases.isEmpty()) {
                        Text("暂无可选择的命例。")
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            items(cases, key = { it.id }) { summary ->
                                val checked = summary.id in selected
                                Surface(
                                    onClick = {
                                        selected = if (checked) {
                                            selected - summary.id
                                        } else {
                                            selected + summary.id
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 54.dp)
                                        .testTag("${testTagPrefix}_case_${summary.id}"),
                                    color = Color.Transparent,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = null,
                                        )
                                        Text(
                                            summary.name.value?.ifBlank { null } ?: summary.alias,
                                            modifier = Modifier.weight(1f),
                                        )
                                        summary.fourPillars?.let {
                                            Text(
                                                listOf(it.year, it.month, it.day, it.hour)
                                                    .joinToString(" "),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
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
private fun RecordFilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun RecordExpandableFilterSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    tag: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起$title" else "展开$title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (expanded) content()
    }
}

@Composable
private fun FilterChoiceGrid(
    options: List<String>,
    selected: Set<String>,
    columns: Int,
    tagPrefix: String,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.chunked(columns).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowOptions.forEach { option ->
                    FilterChoiceChip(
                        text = option,
                        selected = option in selected,
                        onClick = { onToggle(option) },
                        modifier = Modifier.weight(1f).testTag("${tagPrefix}_$option"),
                    )
                }
                repeat(columns - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ElementFilterChoiceGrid(
    options: List<String>,
    selected: Set<String>,
    columns: Int,
    tagPrefix: String,
    minHeight: Dp = 40.dp,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(columns).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowOptions.forEach { option ->
                    val isSelected = option in selected
                    val character = option.singleOrNull()?.takeUnless { it == '-' }
                    Surface(
                        onClick = { onToggle(option) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = minHeight)
                            .testTag("${tagPrefix}_$option"),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected && character != null) {
                            baziElementSelectedContainerColor(character)
                        } else {
                            baziElementContainerColor(character)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) {
                                character?.let(::baziElementColor)?.copy(alpha = 0.55f)
                                    ?: NanfengGold.copy(alpha = 0.65f)
                            } else {
                                Color.Transparent
                            },
                        ),
                    ) {
                        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                option,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                color = character?.let(::baziElementColor)
                                    ?: MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                repeat(columns - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PillarPickerTextGrid(
    options: List<String>,
    selected: Set<String>,
    columns: Int,
    tagPrefix: String,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(columns).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowOptions.forEach { option ->
                    val isSelected = option in selected
                    Surface(
                        onClick = { onToggle(option) },
                        modifier = Modifier.weight(1f).heightIn(min = 36.dp)
                            .testTag("${tagPrefix}_$option"),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) NanfengGold.copy(alpha = 0.15f) else NanfengControlSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NanfengGold.copy(alpha = 0.65f) else Color.Transparent,
                        ),
                    ) {
                        Box(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(
                                option,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) NanfengGold else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                repeat(columns - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PillarFilterCell(
    character: Char?,
    tenGod: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        character != null && tenGod != null -> "$character·$tenGod"
        character != null -> character.toString()
        tenGod != null -> tenGod
        else -> "-"
    }
    val selected = character != null || tenGod != null
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected && character != null) {
            baziElementSelectedContainerColor(character)
        } else {
            baziElementContainerColor(character)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) {
                character?.let(::baziElementColor)?.copy(alpha = 0.55f)
                    ?: NanfengGold.copy(alpha = 0.65f)
            } else {
                Color.Transparent
            },
        ),
    ) {
        Box(Modifier.padding(horizontal = 4.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = character?.let(::baziElementColor) ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun FilterChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) NanfengGold.copy(alpha = 0.15f) else NanfengControlSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) NanfengGold.copy(alpha = 0.65f) else Color.Transparent,
        ),
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) NanfengGold else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

private fun FourPillarsSearchFilter.updated(
    index: Int,
    value: PillarCharacterFilter,
): FourPillarsSearchFilter = when (index) {
    0 -> copy(year = value)
    1 -> copy(month = value)
    2 -> copy(day = value)
    else -> copy(hour = value)
}

private fun CaseSortOrder.toPublicRecordSort(): CaseSortOrder = when (this) {
    CaseSortOrder.NAME_ASC, CaseSortOrder.UPDATED_DESC, CaseSortOrder.BIRTH_ASC -> this
    CaseSortOrder.LAST_VIEWED_DESC, CaseSortOrder.CREATED_DESC -> CaseSortOrder.UPDATED_DESC
}

@Composable
private fun RecordTopTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    tag: String,
    accent: Color,
) {
    val (label, count) = text.recordLabelAndCount()
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .testTag(tag),
        shape = RecordToolbarPillShape,
        color = if (selected) Color.White else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.22f) else Color.Transparent,
        ),
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    accent
                } else {
                    accent.copy(alpha = 0.78f)
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            count?.let {
                Text(
                    it,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .widthIn(min = 18.dp),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) accent else accent.copy(alpha = 0.78f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecordCategoryTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String? = null,
) {
    val (label, count) = text.recordLabelAndCount()
    // 分组胶囊保持固定几何；名称和计数合计较长时收紧文字，不能撑宽或挤断计数。
    val compactText = label.length + (count?.length ?: 0) >= 6
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(92.dp)
                .height(36.dp)
                .then(if (tag == null) Modifier else Modifier.testTag(tag)),
            shape = RoundedCornerShape(18.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape,
                        ),
                )
                Text(
                    label,
                    fontSize = if (compactText) 9.sp else 11.sp,
                    lineHeight = if (compactText) 12.sp else 15.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium,
                )
                count?.let {
                    Text(
                        it,
                        fontSize = if (compactText) 8.sp else 9.sp,
                        lineHeight = if (compactText) 10.sp else 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun String.recordLabelAndCount(): Pair<String, String?> {
    val separator = lastIndexOf(' ')
    if (separator <= 0) return this to null
    val possibleCount = substring(separator + 1)
    return if (possibleCount.toIntOrNull() != null) substring(0, separator) to possibleCount else this to null
}

@Composable
private fun RecordSortChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "切换排序，当前$text" }
            .testTag("record_sort_control"),
        shape = RoundedCornerShape(15.dp),
        color = NanfengGold.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            NanfengGold.copy(alpha = 0.26f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = NanfengGold,
            )
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * 字母索引跨越很长列表时不直接做全距离逐项动画：先无动画地预定位到目标前后极短距离，
 * 再完成可见的收尾动画。这样不会在大量案例卡片上持续测量／合成，也能随时被下一次点按取消。
 */
private suspend fun LazyListState.smoothAlphabetScrollToItem(targetIndex: Int) {
    val itemCount = layoutInfo.totalItemsCount
    if (itemCount == 0) return
    val safeTargetIndex = targetIndex.coerceIn(0, itemCount - 1)
    val isVisible = layoutInfo.visibleItemsInfo.any { it.index == safeTargetIndex }
    if (!isVisible) {
        val direction = if (safeTargetIndex >= firstVisibleItemIndex) 1 else -1
        val approachIndex = (safeTargetIndex - direction * 3).coerceIn(0, itemCount - 1)
        scrollToItem(approachIndex)
    }
    animateScrollToItem(safeTargetIndex)
}

@Composable
private fun RecordAlphabetIndex(
    activeInitial: Char,
    pinnedActive: Boolean,
    onPinnedClick: () -> Unit,
    onInitialClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        val pinnedInteractionSource = remember { MutableInteractionSource() }
        val pinnedPressed by pinnedInteractionSource.collectIsPressedAsState()
        // 置顶定位与字母项共用同一透明默认态与圆形选中反馈；默认只露出主体图标。
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(28.dp)
                .clickable(
                    interactionSource = pinnedInteractionSource,
                    indication = null,
                    onClick = onPinnedClick,
                )
                .testTag("record_alphabet_pinned_locator"),
            contentAlignment = Alignment.Center,
        ) {
            val pinnedVisualActive = pinnedActive || pinnedPressed
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (pinnedVisualActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.VerticalAlignTop,
                    contentDescription = "定位至置顶命例",
                    modifier = Modifier.size(17.dp),
                    tint = if (pinnedVisualActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                }
            }
        }
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".forEach { initial ->
            val active = initial == activeInitial
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            // 每一行保留较大的触控区域；可见的选中、按压反馈只由内层圆形承载，
            // 避免触控层的矩形波纹破坏字母索引的圆形语言。
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onInitialClick(initial) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val visualActive = active || pressed
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = if (visualActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            initial.toString(),
                            fontSize = if (active) 12.sp else 10.sp,
                            lineHeight = if (active) 14.sp else 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (visualActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private inline fun <T> List<T>.indexOfFirstBy(initial: (T) -> Char): Map<Char, Int> =
    buildMap {
        this@indexOfFirstBy.forEachIndexed { index, item -> putIfAbsent(initial(item), index) }
    }

private fun CaseSummary.recordSection(): String {
    if (isPinned) return "星标置顶"
    return displayCaseName().caseNameInitial().toString()
}

private fun CaseSummary.displayCaseName(): String = name.value?.ifBlank { null } ?: alias

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
private fun SwipeableCaseSummaryRow(
    summary: CaseSummary,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit,
) {
    val actionWidth = 72.dp
    val revealedWidth = actionWidth * 3
    val revealedWidthPx = with(LocalDensity.current) { revealedWidth.toPx() }
    val offset = remember(summary.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectionMode) {
        if (selectionMode) offset.animateTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("case_swipe_${summary.id}"),
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .testTag("case_swipe_actions_${summary.id}"),
            horizontalArrangement = Arrangement.End,
        ) {
            SwipeCaseAction(
                text = "编辑",
                background = Color(0xFF6E7474),
                width = actionWidth,
                tag = "case_swipe_edit_${summary.id}",
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onEdit()
                },
            )
            SwipeCaseAction(
                text = if (summary.isPinned) "取消置顶" else "置顶",
                background = NanfengNavigation,
                width = actionWidth,
                tag = "case_swipe_pin_${summary.id}",
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onTogglePinned()
                },
            )
            SwipeCaseAction(
                text = "删除",
                background = MaterialTheme.colorScheme.error,
                width = actionWidth,
                tag = "case_swipe_delete_${summary.id}",
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onDelete()
                },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(Color.White)
                .then(
                    if (selectionMode) {
                        Modifier
                    } else {
                        Modifier.pointerInput(summary.id, revealedWidthPx) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val next = (offset.value + dragAmount)
                                        .coerceIn(-revealedWidthPx, 0f)
                                    scope.launch { offset.snapTo(next) }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        offset.animateTo(
                                            if (offset.value <= -revealedWidthPx * 0.32f) {
                                                -revealedWidthPx
                                            } else {
                                                0f
                                            },
                                        )
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { offset.animateTo(0f) }
                                },
                            )
                        }
                    },
                ),
        ) {
            CaseSummaryRow(
                summary = summary,
                selectionMode = selectionMode,
                selected = selected,
                onClick = {
                    if (offset.value < -1f) {
                        scope.launch { offset.animateTo(0f) }
                    } else {
                        onClick()
                    }
                },
            )
        }
    }
}

@Composable
private fun SwipeCaseAction(
    text: String,
    background: Color,
    width: Dp,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .semantics { contentDescription = "左滑操作：$text" }
            .testTag(tag),
        shape = RectangleShape,
        color = background,
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CaseSummaryRow(
    summary: CaseSummary,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "打开命例：${summary.name.value ?: summary.alias}"
            }
            .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
            .testTag("case_${summary.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.padding(end = 8.dp)
                    .testTag("case_select_${summary.id}"),
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaziCompatibilityScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onOpenParticipantList: (SexForFortuneDirection) -> Unit,
    onOpenHistoryRecord: (String) -> Unit,
    onCloseHistoryRecord: () -> Unit,
    onDeleteHistoryRecords: (Set<String>) -> Unit,
    onRetryHistory: () -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCompatibilityHistory by remember { mutableStateOf(false) }
    val historyRecord = state.compatibilityHistory.firstOrNull {
        it.id == state.compatibilityHistoryRecordId
    }
    if (historyRecord != null) {
        BackHandler(onBack = onCloseHistoryRecord)
        Column(modifier = modifier.fillMaxSize().testTag("bazi_compatibility_history_record")) {
            TopAppBar(
                title = { Text("合盘记录", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onCloseHistoryRecord) { Text("返回合盘") }
                },
            )
            BaziCompatibilityReportContent(
                report = historyRecord.report,
                onReplaceParticipant = onOpenParticipantList,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }
    if (showCompatibilityHistory) {
        CompatibilityHistoryScreen(
            records = state.compatibilityHistory,
            loading = state.compatibilityHistoryLoading,
            error = state.compatibilityHistoryError,
            onBack = { showCompatibilityHistory = false },
            onOpenRecord = onOpenHistoryRecord,
            onDeleteRecords = onDeleteHistoryRecords,
            onRetry = onRetryHistory,
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier.fillMaxSize().testTag("bazi_compatibility_screen"),
    ) {
        TopAppBar(
            title = { Text("八字合盘", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("back_from_bazi_compatibility"),
                ) { Text("返回") }
            },
        )
        CompatibilitySetupPanel(
            state = state,
            onOpenParticipantPicker = onOpenParticipantList,
            onOpenHistory = { showCompatibilityHistory = true },
            onAnalyze = onAnalyze,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaziCompatibilityReportScreen(
    report: BaziCompatibilityReport?,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onReplaceParticipant: (SexForFortuneDirection) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("bazi_compatibility_report_screen"),
    ) {
        TopAppBar(
            title = { Text("合盘结果", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("返回") }
            },
        )
        when {
            loading -> LoadingBox("正在生成双方合盘对照…")
            report != null -> BaziCompatibilityReportContent(
                report = report,
                onReplaceParticipant = onReplaceParticipant,
                modifier = Modifier.weight(1f),
            )
            else -> ErrorBox(
                message = error ?: "合盘结果暂不可用，请返回重新选择双方命例。",
                actionLabel = "重试",
                onAction = onRetry,
            )
        }
    }
}

@Composable
private fun CompatibilitySetupPanel(
    state: StageTwoUiState,
    onOpenParticipantPicker: (SexForFortuneDirection) -> Unit,
    onOpenHistory: () -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canAnalyze = state.compatibilityLeftCaseId != null &&
        state.compatibilityRightCaseId != null
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = onOpenHistory,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "查看合盘记录" }
                .testTag("bazi_compatibility_history_entry"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = NanfengGreen.copy(alpha = 0.10f),
                    contentColor = NanfengGreen,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bazi_compatibility_history),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(26.dp),
                    )
                }
                Text(
                    "合盘记录",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompatibilityParticipantCard(
                roleLabel = "男方",
                candidates = state.compatibilityCandidates.filter {
                    it.sexForFortuneDirection == SexForFortuneDirection.MAN
                },
                selectedId = state.compatibilityLeftCaseId,
                onAdd = { onOpenParticipantPicker(SexForFortuneDirection.MAN) },
                tagPrefix = "compatibility_male",
                modifier = Modifier.weight(1f),
            )
            CompatibilityParticipantCard(
                roleLabel = "女方",
                candidates = state.compatibilityCandidates.filter {
                    it.sexForFortuneDirection == SexForFortuneDirection.WOMAN
                },
                selectedId = state.compatibilityRightCaseId,
                onAdd = { onOpenParticipantPicker(SexForFortuneDirection.WOMAN) },
                tagPrefix = "compatibility_female",
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = onAnalyze,
            enabled = canAnalyze && !state.compatibilityLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("bazi_compatibility_analyze"),
        ) {
            Text(if (state.compatibilityLoading) "正在核对命盘…" else "开始合盘")
        }
        when {
            state.compatibilityError != null -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(state.compatibilityError, style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("bazi_compatibility_retry"),
                    ) { Text("重新读取命例") }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun CompatibilityParticipantCard(
    roleLabel: String,
    candidates: List<CaseSummary>,
    selectedId: String?,
    onAdd: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    val selected = candidates.firstOrNull { it.id == selectedId }
    val roleAccent = if (roleLabel == "男方") CompatibilityMaleAccent else CompatibilityFemaleAccent
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 248.dp)
            .testTag("${tagPrefix}_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = roleAccent.copy(alpha = 0.10f),
                    contentColor = roleAccent,
                ) {
                    Text(
                        roleLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (selected != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(roleAccent, CircleShape),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    selected?.let { candidate ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = roleAccent.copy(alpha = 0.10f),
                            contentColor = roleAccent,
                        ) {
                            Text(
                                candidate.alias,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Surface(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(62.dp)
                            .semantics { contentDescription = "添加${roleLabel}八字" }
                            .testTag("${tagPrefix}_add"),
                        shape = CircleShape,
                        color = roleAccent.copy(alpha = 0.10f),
                        contentColor = roleAccent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CompatibilityHistoryScreen(
    records: List<BaziCompatibilityRecord>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOpenRecord: (String) -> Unit,
    onDeleteRecords: (Set<String>) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedRecordIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteRecordIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val allSelected = records.isNotEmpty() && records.all { it.id in selectedRecordIds }
    BackHandler(onBack = onBack)
    Column(modifier = modifier.fillMaxSize().testTag("bazi_compatibility_history")) {
        TopAppBar(
            title = { Text("合盘记录", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回合盘") } },
            actions = {
                if (selectionMode) {
                    TextButton(
                        onClick = {
                            selectedRecordIds = if (allSelected) emptySet() else records.mapTo(linkedSetOf()) { it.id }
                        },
                        enabled = records.isNotEmpty(),
                    ) { Text(if (allSelected) "取消全选" else "全选") }
                    TextButton(
                        onClick = { pendingDeleteRecordIds = selectedRecordIds },
                        enabled = selectedRecordIds.isNotEmpty(),
                    ) { Text("删除 ${selectedRecordIds.size}", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = {
                        selectionMode = false
                        selectedRecordIds = emptySet()
                    }) { Text("完成") }
                } else if (records.isNotEmpty()) {
                    TextButton(onClick = { selectionMode = true }) { Text("管理") }
                }
            },
        )
        if (loading && records.isEmpty()) {
            LoadingBox("正在读取合盘记录…")
        } else if (error != null && records.isEmpty()) {
            ErrorBox(
                message = error,
                actionLabel = "重新读取",
                onAction = onRetry,
            )
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 34.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bazi_compatibility_history),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = NanfengGreen,
                        )
                        Text("暂无合盘记录", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (selectionMode) 92.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(records, key = { it.id }) { record ->
                        CompatibilityHistoryRecordCard(
                            record = record,
                            selectionMode = selectionMode,
                            selected = record.id in selectedRecordIds,
                            onClick = {
                                if (selectionMode) {
                                    selectedRecordIds = if (record.id in selectedRecordIds) {
                                        selectedRecordIds - record.id
                                    } else {
                                        selectedRecordIds + record.id
                                    }
                                } else {
                                    onOpenRecord(record.id)
                                }
                            },
                            onLongClick = { pendingDeleteRecordIds = setOf(record.id) },
                        )
                    }
                }
                if (selectionMode) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp,
                    ) {
                        Button(
                            onClick = { pendingDeleteRecordIds = selectedRecordIds },
                            enabled = selectedRecordIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp)
                                .testTag("compatibility_history_batch_delete"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text("删除已选 ${selectedRecordIds.size} 条") }
                    }
                }
            }
        }
    }
    if (pendingDeleteRecordIds.isNotEmpty()) {
        val deletingCount = pendingDeleteRecordIds.size
        AlertDialog(
            onDismissRequest = { pendingDeleteRecordIds = emptySet() },
            title = { Text(if (deletingCount == 1) "删除这条合盘记录？" else "删除 $deletingCount 条合盘记录？") },
            text = { Text("仅删除本机保存的合盘报告，不会删除男方、女方的任何命例或笔记。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecords(pendingDeleteRecordIds)
                        selectedRecordIds = selectedRecordIds - pendingDeleteRecordIds
                        pendingDeleteRecordIds = emptySet()
                        if (records.size == deletingCount) selectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_compatibility_history_delete"),
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecordIds = emptySet() }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompatibilityHistoryRecordCard(
    record: BaziCompatibilityRecord,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("compatibility_history_${record.id}"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, NanfengGreen) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("compatibility_history_select_${record.id}"),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = NanfengGreen.copy(alpha = 0.10f),
                    contentColor = NanfengGreen,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bazi_compatibility_history),
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(18.dp),
                    )
                }
                Text(
                    "合盘记录",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NanfengGreen,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    formatCompatibilityHistoryCreatedAt(record.createdAtEpochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompatibilityHistoryParticipantSummary(
                    roleLabel = "男方",
                    participant = record.report.left,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = NanfengGold.copy(alpha = 0.14f),
                    contentColor = NanfengGoldText,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("合", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                CompatibilityHistoryParticipantSummary(
                    roleLabel = "女方",
                    participant = record.report.right,
                    modifier = Modifier.weight(1f),
                )
            }
            CompatibilityHistoryRelationshipFocus(record.report)
        }
    }
}

@Composable
private fun CompatibilityHistoryRelationshipFocus(report: BaziCompatibilityReport) {
    val intimate = report.compatibilityRelationSnapshot(
        leftPosition = PillarPosition.DAY,
        rightPosition = PillarPosition.DAY,
        scope = "亲密关系与相处模式",
    )
    val family = report.compatibilityRelationSnapshot(
        leftPosition = PillarPosition.YEAR,
        rightPosition = PillarPosition.YEAR,
        scope = "成长家庭与长辈互动",
    )
    Text(
        "亲密：${intimate.headline}  ·  家庭：${family.headline}",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CompatibilityHistoryParticipantSummary(
    roleLabel: String,
    participant: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityParticipant,
    modifier: Modifier = Modifier,
) {
    val roleAccent = if (roleLabel == "男方") CompatibilityMaleAccent else CompatibilityFemaleAccent
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = roleAccent.copy(alpha = 0.10f),
                contentColor = roleAccent,
            ) {
                Text(
                    roleLabel,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                participant.alias,
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NanfengInk,
            )
        }
        Text(
            "${participant.solarDateTimeText.ifBlank { "出生日期未保存" }} · 生肖 ${participant.zodiac.ifBlank { "未保存" }}",
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "日主 ${participant.dayMaster} · ${listOf(participant.pillars.year, participant.pillars.month, participant.pillars.day, participant.pillars.hour).joinToString(" ")}",
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatCompatibilityHistoryCreatedAt(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))

@Composable
private fun BaziCompatibilityReportContent(
    report: BaziCompatibilityReport,
    onReplaceParticipant: ((SexForFortuneDirection) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("bazi_compatibility_report"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
    ) {
        item {
            CompatibilitySideBySideChart(report, onReplaceParticipant)
        }
        item {
            CompatibilityRelationshipSummary(report)
        }
        item {
            CompatibilityElementVisualization(report)
        }
        item {
            CompatibilityImportantParameterTable(report)
        }
        if (report.warnings.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text("资料与口径提示", fontWeight = FontWeight.SemiBold)
                    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        report.warnings.forEach { warning ->
                            Text("• ${warning.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            CompatibilityRelationshipTable(
                title = "相处优势",
                leftAlias = report.left.alias,
                rightAlias = report.right.alias,
                signals = report.coordinationSignals,
                emptyText = "当前规则集中没有额外的跨盘呼应信号。",
            )
        }
        item {
            CompatibilityRelationshipTable(
                title = "相处提醒",
                leftAlias = report.left.alias,
                rightAlias = report.right.alias,
                signals = report.tensionSignals,
                emptyText = "当前规则集中没有需要额外提示的跨盘张力信号。",
            )
        }
        item {
            CompatibilityAiPromptSection(report = report, modifier = Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp))
        }
    }
}

@Composable
private fun CompatibilitySideBySideChart(
    report: BaziCompatibilityReport,
    onReplaceParticipant: ((SexForFortuneDirection) -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("compatibility_side_by_side_chart"),
        color = Color.White,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E0DB)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFF1F1D19)),
            ) {
                CompatibilityParticipantHeader(
                    modifier = Modifier.weight(1f),
                    role = "男方",
                    participant = report.left,
                    roleAccent = CompatibilityMaleAccent,
                    onReplace = onReplaceParticipant?.let { { it(SexForFortuneDirection.MAN) } },
                )
                VerticalDivider(color = Color(0xFF45413A))
                CompatibilityParticipantHeader(
                    modifier = Modifier.weight(1f),
                    role = "女方",
                    participant = report.right,
                    roleAccent = CompatibilityFemaleAccent,
                    onReplace = onReplaceParticipant?.let { { it(SexForFortuneDirection.WOMAN) } },
                )
            }
            HorizontalDivider(color = Color(0xFF45413A))
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                CompatibilityBasicChartGrid(
                    modifier = Modifier.weight(1f),
                    participant = report.left,
                    role = "男方",
                )
                VerticalDivider(color = Color(0xFFE2E0DB))
                CompatibilityBasicChartGrid(
                    modifier = Modifier.weight(1f),
                    participant = report.right,
                    role = "女方",
                )
            }
            HorizontalDivider(color = Color(0xFFE2E0DB))
            CompatibilityPairedDecadeTimeline(report.left, report.right)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompatibilityParticipantHeader(
    modifier: Modifier,
    role: String,
    participant: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityParticipant,
    roleAccent: Color,
    onReplace: (() -> Unit)?,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF1F1D19))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    participant.alias,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    "（${if (role == "男方") "男" else "女"}）",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = roleAccent,
                )
            }
            if (onReplace != null) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                    IconButton(
                        onClick = onReplace,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("replace_${role}_compatibility_participant"),
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "更换${role}八字",
                            modifier = Modifier.size(20.dp),
                            tint = NanfengGoldText,
                        )
                    }
                }
            }
        }
        Text(
            "阳历：${participant.solarDateTimeText.ifBlank { "未保存" }}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 2,
        )
        Text(
            "农历：${participant.lunarDateTimeText.ifBlank { "未保存" }}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 2,
        )
    }
}

@Composable
private fun CompatibilityBasicChartGrid(
    modifier: Modifier,
    participant: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityParticipant,
    role: String,
) {
    val names = listOf("年柱", "月柱", "日柱", "时柱")
    val values = listOf(participant.pillars.year, participant.pillars.month, participant.pillars.day, participant.pillars.hour)
    val details = participant.pillarPresentation.takeIf { it.size == 4 }
    Column(modifier = modifier) {
        CompatibilityBasicGridRow(label = "", values = names, shaded = true, strong = true)
        CompatibilityBasicGridRow(
            label = "日期",
            values = participant.solarDateValues.takeIf { it.size == 4 } ?: List(4) { "未保存" },
        )
        CompatibilityBasicGridRow(
            label = "十神",
            values = values.mapIndexed { index, _ ->
                if (index == PillarPosition.DAY.ordinal) {
                    if (role == "男方") "元男" else "元女"
                } else {
                    details?.get(index)?.primaryTenGod ?: "未保存"
                }
            },
            valueColors = values.map { value ->
                value.firstOrNull()?.let(::compatibilityTenGodColor) ?: NanfengInk
            },
            shaded = true,
        )
        CompatibilityBasicGridRow(
            label = "天干",
            values = values.map { it.firstOrNull()?.toString() ?: "未保存" },
            valueColors = values.map { value -> value.firstOrNull()?.let(::baziElementColor) ?: NanfengInk },
            strong = true,
        )
        CompatibilityBasicGridRow(
            label = "地支",
            values = values.map { it.lastOrNull()?.toString() ?: "未保存" },
            valueColors = values.map { value -> value.lastOrNull()?.let(::baziElementColor) ?: NanfengInk },
            strong = true,
        )
        CompatibilityBasicHiddenStemGridRow(
            label = "藏干",
            values = details?.map { detail ->
                detail.hiddenStemSummary.compatibilityHiddenStemEntries()
            } ?: List(4) { listOf("未保存") },
            shaded = true,
        )
    }
}

private fun String.compatibilityHiddenStemEntries(): List<String> =
    split('·', '\n')
        .map(String::trim)
        .filter(String::isNotBlank)
        .ifEmpty { listOf("未保存") }

private val COMPATIBILITY_STEM_CHARACTERS = setOf('甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸')

/** 十神的色彩跟随其对应天干的统一五行色，和基础排盘的藏干保持同一口径。 */
private fun compatibilityTenGodColor(stem: Char): Color = baziElementColor(stem)

@Composable
private fun CompatibilityBasicHiddenStemGridRow(
    label: String,
    values: List<List<String>>,
    shaded: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (shaded) Color(0xFFF5F5F3) else Color.White)
            .testTag("compatibility_basic_chart_hidden_stems"),
        verticalAlignment = Alignment.Top,
    ) {
        CompatibilityBasicGridCell(
            value = label,
            modifier = Modifier.width(30.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strong = false,
        )
        values.forEach { entries ->
            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Color(0xFFE5E4E0), thickness = 0.5.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    entries.forEach { entry ->
                        val stem = entry.firstOrNull()?.takeIf { it in COMPATIBILITY_STEM_CHARACTERS }
                        if (stem == null) {
                            Text(
                                text = entry,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = NanfengInk,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stem.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = compatibilityTenGodColor(stem),
                                )
                                Text(
                                    text = entry.drop(1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = compatibilityTenGodColor(stem),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFE5E4E0))
}

@Composable
private fun CompatibilityBasicGridRow(
    label: String,
    values: List<String>,
    valueColors: List<Color> = emptyList(),
    shaded: Boolean = false,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (shaded) Color(0xFFF5F5F3) else Color.White),
        verticalAlignment = Alignment.Top,
    ) {
        CompatibilityBasicGridCell(
            value = label,
            modifier = Modifier.width(30.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strong = false,
        )
        values.forEachIndexed { index, value ->
            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Color(0xFFE5E4E0), thickness = 0.5.dp)
            CompatibilityBasicGridCell(
                value = value,
                modifier = Modifier.weight(1f),
                color = valueColors.getOrNull(index) ?: NanfengInk,
                strong = strong,
            )
        }
    }
    HorizontalDivider(color = Color(0xFFE5E4E0))
}

@Composable
private fun CompatibilityBasicGridCell(
    value: String,
    modifier: Modifier,
    color: Color,
    strong: Boolean,
) {
    Box(
        modifier = modifier.heightIn(min = if (strong) 48.dp else 34.dp).padding(horizontal = 2.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            value,
            textAlign = TextAlign.Center,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
        )
    }
}

@Composable
private fun CompatibilityPairedDecadeTimeline(
    left: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityParticipant,
    right: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityParticipant,
) {
    if (left.decadeFortunes.isEmpty() && right.decadeFortunes.isEmpty()) return
    val pairedSteps = (0 until maxOf(left.decadeFortunes.size, right.decadeFortunes.size)).map { index ->
        left.decadeFortunes.getOrNull(index) to right.decadeFortunes.getOrNull(index)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFBFAF7))
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("compatibility_paired_decades"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "大运并行",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = NanfengInk,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(30.dp)) {
                CompatibilityDecadeRoleLabel("男方", CompatibilityMaleAccent)
                HorizontalDivider(color = Color(0xFFE4E2DE), thickness = 0.5.dp)
                CompatibilityDecadeRoleLabel("女方", CompatibilityFemaleAccent)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                pairedSteps.forEach { (leftDecade, rightDecade) ->
                    CompatibilityPairedDecadeColumn(
                        left = leftDecade,
                        right = rightDecade,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompatibilityDecadeRoleLabel(
    role: String,
    roleAccent: Color,
) {
    Box(
        modifier = Modifier.height(66.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            role,
            style = MaterialTheme.typography.labelSmall,
            color = roleAccent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CompatibilityPairedDecadeColumn(
    left: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityDecadePresentation?,
    right: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityDecadePresentation?,
) {
    Column(modifier = Modifier.width(64.dp)) {
        CompatibilityDecadeCell(left)
        HorizontalDivider(color = Color(0xFFE4E2DE), thickness = 0.5.dp)
        CompatibilityDecadeCell(right)
    }
}

@Composable
private fun CompatibilityDecadeCell(
    decade: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityDecadePresentation?,
) {
    val tenGod = decade?.stemTenGod.orEmpty().ifBlank { "—" }
    val name = decade?.name.orEmpty().ifBlank { "—" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            decade?.ageRange.orEmpty().ifBlank { "—" },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            tenGod,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = decade?.name?.firstOrNull()?.let(::compatibilityTenGodColor)
                ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            buildAnnotatedString {
                name.forEach { character ->
                    withStyle(SpanStyle(color = baziElementColor(character))) { append(character) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CompatibilityImportantParameterTable(report: BaziCompatibilityReport) {
    val leftDayBranch = report.left.pillars.day.lastOrNull()?.toString() ?: "未保存"
    val rightDayBranch = report.right.pillars.day.lastOrNull()?.toString() ?: "未保存"
    val leftYearBranch = report.left.pillars.year.lastOrNull()?.toString() ?: "未保存"
    val rightYearBranch = report.right.pillars.year.lastOrNull()?.toString() ?: "未保存"
    val leftStructure = report.left.structuralProfileOrAnalyze()
    val rightStructure = report.right.structuralProfileOrAnalyze()
    CompatibilityComparisonTable(
        title = "核心关系",
        headers = listOf("对比项", report.left.alias, report.right.alias, "解析"),
        rows = listOf(
            listOf(
                "日主",
                "${report.left.dayMaster}${report.dayMasterRelation.leftElement}",
                "${report.right.dayMaster}${report.dayMasterRelation.rightElement}",
                "${report.dayMasterRelation.description}：${report.dayMasterRelation.explanation}",
            ),
        ),
        columnWeights = CompatibilityReportColumnWeights,
    )
    CompatibilityComparisonTable(
        title = "家庭互动",
        headers = listOf("对比项", report.left.alias, report.right.alias, "解析"),
        rows = listOf(
            listOf(
                "双方年支（生肖）", "${report.left.zodiac.ifBlank { "未保存" }}（$leftYearBranch）", "${report.right.zodiac.ifBlank { "未保存" }}（$rightYearBranch）",
                report.compatibilityPairImpact(PillarPosition.YEAR, PillarPosition.YEAR, "成长家庭与长辈互动"),
            ),
            listOf(
                "双方夫妻宫（日支）", leftDayBranch, rightDayBranch,
                report.compatibilityPairImpact(PillarPosition.DAY, PillarPosition.DAY, "伴侣相处与亲密边界"),
            ),
            listOf(
                "${report.left.alias}夫妻宫 ↔ ${report.right.alias}年支", leftDayBranch, rightYearBranch,
                report.compatibilityPairImpact(PillarPosition.DAY, PillarPosition.YEAR, "${report.left.alias}进入${report.right.alias}成长家庭的相处节奏"),
            ),
            listOf(
                "${report.right.alias}夫妻宫 ↔ ${report.left.alias}年支", leftYearBranch, rightDayBranch,
                report.compatibilityPairImpact(PillarPosition.YEAR, PillarPosition.DAY, "${report.right.alias}进入${report.left.alias}成长家庭的相处节奏"),
            ),
        ),
        columnWeights = CompatibilityReportColumnWeights,
    )
    CompatibilityComparisonTable(
        title = "命盘结构",
        headers = listOf("对比项", report.left.alias, report.right.alias, "解析"),
        rows = listOf(
            listOf(
                "表层五行缺失",
                report.left.pillars.compatibilityMissingElements(),
                report.right.pillars.compatibilityMissingElements(),
                "按各自采用快照四柱的显性干支展示未出现项；未出现不等同喜忌，也不能单独解释为互补。",
            ),
            listOf(
                "日主旺衰（候选）",
                leftStructure.compatibilityStrengthLabel(),
                rightStructure.compatibilityStrengthLabel(),
                "${report.left.alias}：${leftStructure.compatibilityStrengthEvidence()}；${report.right.alias}：${rightStructure.compatibilityStrengthEvidence()}。依据月令、通根及天干生扶克泄耗，不以字符数量直接判旺衰。",
            ),
            listOf(
                "格局（候选）",
                leftStructure.selectedPattern.name,
                rightStructure.selectedPattern.name,
                "${report.left.alias}：${leftStructure.compatibilityPatternEvidence()}；${report.right.alias}：${rightStructure.compatibilityPatternEvidence()}。先取月令藏干，再看透干；从格、专旺与合化只列复核项，不作定格。",
            ),
        ),
        columnWeights = CompatibilityReportColumnWeights,
    )
}

private fun BaziStructuralProfile.compatibilityStrengthLabel(): String =
    "${strength.displayName}（${strengthConfidence.displayName}置信）"

private fun BaziStructuralProfile.compatibilityStrengthEvidence(): String =
    strengthEvidence.take(2).joinToString("；") { it.detail }

private fun BaziStructuralProfile.compatibilityPatternEvidence(): String =
    selectedPattern.evidence.joinToString("；") { it.detail }

private fun BaziCompatibilityReport.compatibilityPairImpact(
    leftPosition: PillarPosition,
    rightPosition: PillarPosition,
    scope: String,
): String {
    val elemental = compatibilityBranchElementRelation(leftPosition, rightPosition, scope)
    val impacts = coordinationSignals.plus(tensionSignals)
        .filter { signal ->
            signal.pillars.size == 2 &&
                signal.pillars.any { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.LEFT && it.position == leftPosition } &&
                signal.pillars.any { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.RIGHT && it.position == rightPosition }
        }
        .distinctBy { signal -> signal.kind to signal.values }
        .map { signal -> "${signal.kind.title} · ${signal.values}：${signal.explanation}" }
    return listOf(elemental.detail).plus(impacts).joinToString("\n")
}

@Composable
private fun CompatibilityElementVisualization(report: BaziCompatibilityReport) {
    val leftElements = report.left.pillars.compatibilityElementCounts()
    val rightElements = report.right.pillars.compatibilityElementCounts()
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 18.dp, end = 16.dp).testTag("compatibility_element_balance"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("五行结构", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CompatibilityParticipantLegend("男方 · ${report.left.alias}", CompatibilityMaleAccent, Modifier.weight(1f))
            CompatibilityParticipantLegend("女方 · ${report.right.alias}", CompatibilityFemaleAccent, Modifier.weight(1f))
        }
        Text("以双方四柱八个表层干支字符计数。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf("木", "火", "土", "金", "水").forEach { element ->
            CompatibilityElementBalanceRow(
                element = element,
                leftCount = leftElements.getValue(element),
                rightCount = rightElements.getValue(element),
            )
        }
    }
}

@Composable
private fun CompatibilityElementBalanceRow(
    element: String,
    leftCount: Int,
    rightCount: Int,
) {
    val elementColor = baziElementColor(element)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(element, modifier = Modifier.width(26.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = elementColor)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CompatibilityElementBar("男", leftCount, elementColor, CompatibilityMaleAccent)
            CompatibilityElementBar("女", rightCount, elementColor, CompatibilityFemaleAccent)
        }
    }
}

@Composable
private fun CompatibilityElementBar(
    role: String,
    count: Int,
    elementColor: Color,
    roleAccent: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(role, modifier = Modifier.width(14.dp), style = MaterialTheme.typography.labelSmall, color = roleAccent, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.weight(1f).height(7.dp).background(elementColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((count / 8f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(elementColor, RoundedCornerShape(4.dp)),
            )
        }
        Text("$count/8", modifier = Modifier.width(27.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompatibilityParticipantLegend(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private data class CompatibilityRelationSnapshot(
    val headline: String,
    val detail: String,
    val color: Color,
)

private data class CompatibilityBranchElementRelation(
    val headline: String,
    val detail: String,
    val color: Color,
    val requiresAttention: Boolean = false,
)

private fun BaziCompatibilityReport.compatibilityBranchElementRelation(
    leftPosition: PillarPosition,
    rightPosition: PillarPosition,
    scope: String,
): CompatibilityBranchElementRelation {
    val leftBranch = left.pillars.compatibilityBranchAt(leftPosition)
    val rightBranch = right.pillars.compatibilityBranchAt(rightPosition)
    val leftElement = leftBranch.compatibilityBranchElement()
    val rightElement = rightBranch.compatibilityBranchElement()
    if (leftElement == null || rightElement == null) {
        return CompatibilityBranchElementRelation(
            headline = "五行关系待核",
            detail = "${scope}的地支信息不完整，暂不判断双方在支持、付出或主导上的方向。",
            color = Color(0xFF73716C),
        )
    }
    return when {
        leftElement == rightElement -> CompatibilityBranchElementRelation(
            headline = "同五行（$leftElement）",
            detail = "${scope}同属$leftElement，双方较容易站在相近的需求和表达方式上；有默契时很顺，僵持时也要避免各自坚持自己的标准。",
            color = NanfengGreen,
        )
        leftElement.compatibilityGenerates() == rightElement -> CompatibilityBranchElementRelation(
            headline = "男方生女方（$leftElement→$rightElement）",
            detail = "${scope}呈男方生女方：传统上是资生与输出的方向，现实中男方更容易主动承担、照顾或投入资源；需要确认女方能接住，也避免付出长期失衡。",
            color = NanfengGreen,
        )
        rightElement.compatibilityGenerates() == leftElement -> CompatibilityBranchElementRelation(
            headline = "女方生男方（$rightElement→$leftElement）",
            detail = "${scope}呈女方生男方：传统上是资生与输出的方向，现实中女方更容易主动承担、照顾或投入资源；需要确认男方能接住，也避免付出长期失衡。",
            color = NanfengGreen,
        )
        leftElement.compatibilityControls() == rightElement -> CompatibilityBranchElementRelation(
            headline = "男方克女方（$leftElement→$rightElement）",
            detail = "${scope}呈男方克女方：传统上是制约方向，现实中男方更容易在规则、资源、节奏或决策上占据主导。适度能帮助落实，过强会让女方感到被管束、被消耗。",
            color = NanfengSolarTermRed,
            requiresAttention = true,
        )
        rightElement.compatibilityControls() == leftElement -> CompatibilityBranchElementRelation(
            headline = "女方克男方（$rightElement→$leftElement）",
            detail = "${scope}呈女方克男方：传统上是制约方向，现实中女方更容易在规则、资源、节奏或决策上占据主导。适度能帮助落实，过强会让男方感到被管束、被消耗。",
            color = NanfengSolarTermRed,
            requiresAttention = true,
        )
        else -> CompatibilityBranchElementRelation(
            headline = "五行关系待核",
            detail = "${scope}暂未能取得稳定的五行方向，建议先核对双方已采用的四柱资料。",
            color = Color(0xFF73716C),
        )
    }
}

private fun FourPillars.compatibilityBranchAt(position: PillarPosition): String = when (position) {
    PillarPosition.YEAR -> year.lastOrNull()?.toString().orEmpty()
    PillarPosition.MONTH -> month.lastOrNull()?.toString().orEmpty()
    PillarPosition.DAY -> day.lastOrNull()?.toString().orEmpty()
    PillarPosition.HOUR -> hour.lastOrNull()?.toString().orEmpty()
}

private fun String.compatibilityBranchElement(): String? = when (this) {
    "寅", "卯" -> "木"
    "巳", "午" -> "火"
    "辰", "戌", "丑", "未" -> "土"
    "申", "酉" -> "金"
    "亥", "子" -> "水"
    else -> null
}

private fun String.compatibilityGenerates(): String? = when (this) {
    "木" -> "火"
    "火" -> "土"
    "土" -> "金"
    "金" -> "水"
    "水" -> "木"
    else -> null
}

private fun String.compatibilityControls(): String? = when (this) {
    "木" -> "土"
    "土" -> "水"
    "水" -> "火"
    "火" -> "金"
    "金" -> "木"
    else -> null
}

private fun BaziCompatibilityReport.compatibilityRelationSnapshot(
    leftPosition: PillarPosition,
    rightPosition: PillarPosition,
    scope: String,
): CompatibilityRelationSnapshot {
    val elemental = compatibilityBranchElementRelation(leftPosition, rightPosition, scope)
    val signals = coordinationSignals.plus(tensionSignals)
        .filter { signal ->
            signal.pillars.size == 2 &&
                signal.pillars.any { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.LEFT && it.position == leftPosition } &&
                signal.pillars.any { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.RIGHT && it.position == rightPosition }
        }
        .distinctBy { signal -> signal.kind to signal.values }
    if (signals.isEmpty()) return CompatibilityRelationSnapshot(elemental.headline, elemental.detail, elemental.color)
    val hasTension = signals.any { !it.kind.isCoordination }
    return CompatibilityRelationSnapshot(
        headline = listOf(elemental.headline).plus(
            signals.map { signal -> "${signal.kind.title} ${signal.values}" },
        ).joinToString(" · "),
        detail = listOf(elemental.detail).plus(
            signals.map { signal -> signal.explanation.substringAfter('，', signal.explanation) },
        ).joinToString("；"),
        color = if (hasTension || elemental.requiresAttention) NanfengSolarTermRed else NanfengGreen,
    )
}

@Composable
private fun CompatibilityRelationshipSummary(report: BaziCompatibilityReport) {
    val summary = report.relationshipSummary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .border(0.5.dp, Color(0xFFE1DFDA), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("compatibility_relationship_summary"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("双方关系总结", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        CompatibilitySummaryRow(
            label = "关系判断",
            accent = NanfengGoldText,
            text = summary.relationshipJudgement,
        )
        CompatibilitySummaryRow(
            label = "优势",
            accent = NanfengGreen,
            text = summary.advantage,
        )
        CompatibilitySummaryRow(
            label = "需要留意",
            accent = NanfengSolarTermRed,
            text = summary.caution,
        )
        CompatibilitySummaryRow(
            label = "相处建议",
            accent = NanfengGold,
            text = summary.suggestion,
        )
        CompatibilitySummaryRow(
            label = "资料提醒",
            accent = MaterialTheme.colorScheme.onSurfaceVariant,
            text = summary.dataReminder,
        )
    }
}

@Composable
private fun CompatibilitySummaryRow(label: String, accent: Color, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(62.dp), color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompatibilityComparisonTable(
    title: String,
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    firstColumnColor: ((List<String>) -> Color)? = null,
    columnWeights: List<Float> = List(headers.size) { 1f },
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .compatibilityTableSideBorders(),
    ) {
        Text(
            title,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        CompatibilityTableHeader(headers, columnWeights)
        rows.forEach { row ->
            CompatibilityTableRow(
                values = row,
                firstColumnColor = firstColumnColor?.invoke(row),
                columnWeights = columnWeights,
            )
        }
    }
}

private val CompatibilityReportColumnWeights = listOf(0.92f, 0.72f, 0.72f, 1.64f)

private fun Modifier.compatibilityTableSideBorders(): Modifier = drawWithContent {
    drawContent()
    val strokeWidth = 0.5.dp.toPx()
    val inset = strokeWidth / 2f
    drawLine(
        color = Color(0xFFDCDCD8),
        start = Offset(inset, 0f),
        end = Offset(inset, size.height),
        strokeWidth = strokeWidth,
    )
    drawLine(
        color = Color(0xFFDCDCD8),
        start = Offset(size.width - inset, 0f),
        end = Offset(size.width - inset, size.height),
        strokeWidth = strokeWidth,
    )
}

@Composable
private fun CompatibilityTableHeader(headers: List<String>, columnWeights: List<Float>) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Color.White)) {
        headers.forEachIndexed { index, header ->
            if (index > 0) VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Color(0xFFDCDCD8), thickness = 0.5.dp)
            Box(
                modifier = Modifier
                    .weight(columnWeights.getOrElse(index) { 1f })
                    .fillMaxHeight()
                    .background(Color(0xFFF3F3F1))
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    header,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
    HorizontalDivider(color = Color(0xFFDCDCD8), thickness = 0.5.dp)
}

@Composable
private fun CompatibilityTableRow(
    values: List<String>,
    firstColumnColor: Color? = null,
    columnWeights: List<Float> = List(values.size) { 1f },
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Color.White)) {
        values.forEachIndexed { index, value ->
            if (index > 0) VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Color(0xFFE5E4E0), thickness = 0.5.dp)
            Box(
                modifier = Modifier
                    .weight(columnWeights.getOrElse(index) { 1f })
                    .fillMaxHeight()
                    .background(if (index == 0) Color(0xFFF7F7F5) else Color.White)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index == 0) firstColumnColor ?: NanfengInk else NanfengInk,
                )
            }
        }
    }
    HorizontalDivider(color = Color(0xFFE5E4E0), thickness = 0.5.dp)
}

@Composable
private fun CompatibilityRelationshipTable(
    title: String,
    leftAlias: String,
    rightAlias: String,
    signals: List<BaziCompatibilitySignal>,
    emptyText: String,
) {
    val groups = signals
        .groupBy { it.kind to it.values }
        .map { (key, groupedSignals) ->
            CompatibilityRelationshipGroup(
                kind = key.first,
                values = key.second,
                left = groupedSignals.map(BaziCompatibilitySignal::compatibilityEvidence).map { it.left }.filter { it.isNotBlank() }.distinct().joinToString("；"),
                right = groupedSignals.map(BaziCompatibilitySignal::compatibilityEvidence).map { it.right }.filter { it.isNotBlank() }.distinct().joinToString("；"),
                explanation = groupedSignals
                    .map(BaziCompatibilitySignal::explanation)
                    .distinct()
                    .joinToString("\n"),
            )
        }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .compatibilityTableSideBorders(),
    ) {
        Text(
            title,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (groups.isEmpty()) {
            Text(emptyText, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            CompatibilityTableHeader(
                headers = listOf("对比项", leftAlias, rightAlias, "解析"),
                columnWeights = CompatibilityReportColumnWeights,
            )
            groups.forEach { group ->
                val accent = if (group.kind.isCoordination) NanfengGreen else NanfengSolarTermRed
                CompatibilityTableRow(
                    values = listOf(
                        group.kind.title,
                        group.left.ifBlank { "未参与" },
                        group.right.ifBlank { "未参与" },
                        "${group.values}\n${group.explanation}",
                    ),
                    firstColumnColor = accent,
                    columnWeights = CompatibilityReportColumnWeights,
                )
            }
        }
    }
}

private data class CompatibilityRelationshipGroup(
    val kind: BaziCompatibilitySignalKind,
    val values: String,
    val left: String,
    val right: String,
    val explanation: String,
)

private fun FourPillars.compatibilityElementCounts(): Map<String, Int> =
    listOf(year, month, day, hour)
        .flatMap { pillar -> listOf(pillar.firstOrNull(), pillar.lastOrNull()) }
        .mapNotNull { character -> character?.compatibilityElement() }
        .groupingBy { it }
        .eachCount()
        .let { counts -> listOf("木", "火", "土", "金", "水").associateWith { counts[it] ?: 0 } }

private fun FourPillars.compatibilityMissingElements(): String =
    compatibilityElementCounts()
        .filterValues { count -> count == 0 }
        .keys
        .joinToString("、")
        .ifBlank { "无" }

private fun Char.compatibilityElement(): String? = when (this) {
    '甲', '乙', '寅', '卯' -> "木"
    '丙', '丁', '巳', '午' -> "火"
    '戊', '己', '辰', '戌', '丑', '未' -> "土"
    '庚', '辛', '申', '酉' -> "金"
    '壬', '癸', '亥', '子' -> "水"
    else -> null
}

private val CompatibilityMaleAccent = Color(0xFF527A84)
private val CompatibilityFemaleAccent = Color(0xFFD78335)

private data class CompatibilitySignalEvidence(val left: String, val right: String)

private fun BaziCompatibilitySignal.compatibilityEvidence(): CompatibilitySignalEvidence {
    val left = pillars
        .filter { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.LEFT }
        .joinToString("、") { "${it.position.compatibilityDisplay()} ${it.value}" }
    val right = pillars
        .filter { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.RIGHT }
        .joinToString("、") { "${it.position.compatibilityDisplay()} ${it.value}" }
    return CompatibilitySignalEvidence(left = left, right = right)
}

private fun com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.compatibilityDisplay(): String =
    if (this == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.LEFT) "男方" else "女方"

private fun PillarPosition.compatibilityDisplay(): String = when (this) {
    PillarPosition.YEAR -> "年柱"
    PillarPosition.MONTH -> "月柱"
    PillarPosition.DAY -> "日柱"
    PillarPosition.HOUR -> "时柱"
}

private fun TimePrecision.compatibilityDisplay(): String = when (this) {
    TimePrecision.EXACT_TO_SECOND -> "精确到秒"
    TimePrecision.EXACT_TO_MINUTE -> "精确到分"
    TimePrecision.APPROXIMATE -> "约略时间"
    TimePrecision.HOUR_ONLY -> "仅知小时"
    TimePrecision.DOUBLE_HOUR_ONLY -> "仅知时辰"
    TimePrecision.UNKNOWN -> "时刻未知"
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
    onConfirmAiRecognition: () -> Unit,
    onCancelAiRecognition: () -> Unit,
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
                    state.busy -> "截图正在由 AI 模型识别"
                    state.awaitingAiModelConsent -> "等待确认上传截图"
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
            if (state.awaitingAiModelConsent) {
                Text(
                    "将上传原图给 ${state.aiModelProviderName ?: "已选服务"}" +
                        " · ${state.aiModelName ?: "已选模型"} 进行识别；" +
                        "仅在你确认后发送，结果仍须逐项核对。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onConfirmAiRecognition,
                    modifier = Modifier.testTag("confirm_ai_screenshot_recognition"),
                ) {
                    Text("确认并开始识别")
                }
                TextButton(
                    onClick = onCancelAiRecognition,
                    modifier = Modifier.testTag("cancel_ai_screenshot_recognition"),
                ) {
                    Text("暂不上传")
                }
            }
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
    libraryType: CaseLibraryType,
    libraryCaseCount: Int,
    activeCriteria: List<String>,
    onCreate: () -> Unit,
    onClearConditions: () -> Unit,
) {
    val hasNoMatchedResults = visibility == CaseVisibility.ACTIVE &&
        libraryCaseCount > 0 &&
        activeCriteria.isNotEmpty()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                when {
                    visibility == CaseVisibility.TRASHED -> "回收站为空"
                    hasNoMatchedResults && libraryType == CaseLibraryType.CELEBRITY ->
                        "未找到符合当前条件的名人案例"
                    hasNoMatchedResults -> "未找到符合当前条件的命例"
                    libraryType == CaseLibraryType.CELEBRITY -> "还没有名人案例"
                    else -> "还没有命例"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    visibility == CaseVisibility.TRASHED ->
                        "移入回收站的命例会保留完整数据，并可从这里恢复。"
                    hasNoMatchedResults -> buildString {
                        append("当前条件：")
                        append(activeCriteria.joinToString("、"))
                        append("。可清除条件后查看全部 $libraryCaseCount 例")
                        append(if (libraryType == CaseLibraryType.CELEBRITY) "名人案例。" else "命例。")
                    }
                    libraryType == CaseLibraryType.CELEBRITY ->
                        "可先手动录入名人出生资料；后续文字自动录入也会保存到这里。"
                    else -> "先手动录入出生资料，应用会完成排盘并保存到本机。"
                },
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasNoMatchedResults) {
                OutlinedButton(
                    onClick = onClearConditions,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .heightIn(min = 48.dp)
                        .testTag("case_empty_clear_conditions"),
                ) {
                    Text("清除搜索与筛选")
                }
            } else if (visibility == CaseVisibility.ACTIVE) {
                Button(
                    onClick = onCreate,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        if (libraryType == CaseLibraryType.CELEBRITY) {
                            "新建名人案例"
                        } else {
                            "新建第一个命例"
                        },
                    )
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
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
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
    onOpenBaziCompatibility: () -> Unit,
    onCreateGroup: (String) -> Unit,
    compatibilityParticipantRole: SexForFortuneDirection?,
    bottomContentInset: Dp,
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
        onOpenBaziCompatibility = onOpenBaziCompatibility,
        onCreateGroup = onCreateGroup,
        compatibilityParticipantRole = compatibilityParticipantRole,
        onBack = onBack,
        bottomContentInset = bottomContentInset,
        modifier = modifier,
    )
}

@Composable
private fun HomeCaseGroupPickerDialog(
    groups: List<CaseGroup>,
    selectedGroupId: String?,
    saving: Boolean,
    creationError: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onCreate: (String) -> Unit,
) {
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedGroupId, groups) {
        val selectedName = groups.firstOrNull { it.id == selectedGroupId }?.name
        if (selectedName != null && selectedName == newGroupName.trim()) {
            newGroupName = ""
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .testTag("home_group_picker_dialog"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
                    .testTag("home_group_picker_scrim"),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(
                            start = 18.dp,
                            top = 16.dp,
                            end = 18.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "保存到分组",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onDismiss) { Text("完成") }
                    }
                    Text(
                        "所有分组（${groups.size}）",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            HomeCaseGroupOption(
                                name = "全部",
                                selected = selectedGroupId == null,
                                tag = "home_group_option_none",
                                featured = true,
                                onClick = { onSelect(null) },
                            )
                        }
                        items(groups, key = { it.id }) { group ->
                            Column {
                                HomeCaseGroupOption(
                                    name = group.name,
                                    selected = selectedGroupId == group.id,
                                    tag = "home_group_option_${group.id}",
                                    featured = false,
                                    onClick = { onSelect(group.id) },
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                    if (adding) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = NanfengControlSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = newGroupName,
                                    onValueChange = { newGroupName = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_group_new_name"),
                                    placeholder = { Text("输入新分组名称") },
                                    singleLine = true,
                                )
                                Button(
                                    onClick = { onCreate(newGroupName) },
                                    enabled = newGroupName.isNotBlank() && !saving,
                                    modifier = Modifier.testTag("home_group_create_and_select"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NanfengNavigation,
                                    ),
                                ) { Text(if (saving) "创建中" else "创建") }
                                IconButton(onClick = { adding = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "取消新增分组")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { adding = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("home_group_add"),
                            colors = ButtonDefaults.buttonColors(containerColor = NanfengNavigation),
                        ) { Text("添加") }
                    }
                    creationError?.let { error ->
                        Text(
                            error,
                            modifier = Modifier.testTag("home_group_creation_error"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(
                        Modifier
                            .height(GroupDialogBottomSafetySpace)
                            .testTag("home_group_bottom_safety_space"),
                    )
                }
            }
        }
    }
}

private val GroupDialogBottomSafetySpace = 96.dp

@Composable
private fun HomeCaseGroupOption(
    name: String,
    selected: Boolean,
    tag: String,
    featured: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (featured) 58.dp else 64.dp)
            .testTag(tag)
            .semantics { stateDescription = if (selected) "已选中" else "未选中" },
        shape = RoundedCornerShape(if (featured) 14.dp else 0.dp),
        color = when {
            selected -> NanfengGold.copy(alpha = 0.12f)
            featured -> NanfengControlSurface
            else -> Color.Transparent
        },
        border = if (selected && featured) {
            androidx.compose.foundation.BorderStroke(1.dp, NanfengGold.copy(alpha = 0.45f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (featured) 16.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            if (selected) {
                Text(
                    "已选",
                    color = if (featured) NanfengGold else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
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
    onOpenBaziCompatibility: () -> Unit,
    onCreateGroup: (String) -> Unit,
    compatibilityParticipantRole: SexForFortuneDirection?,
    onBack: () -> Unit,
    bottomContentInset: Dp,
    modifier: Modifier = Modifier,
) {
    val isCompatibilityParticipantCreate = compatibilityParticipantRole != null
    var saveCase by rememberSaveable { mutableStateOf(true) }
    var showBirthPicker by rememberSaveable { mutableStateOf(false) }
    var birthPickerEntryMode by rememberSaveable { mutableStateOf(BirthPickerMode.SOLAR) }
    var showBirthplacePicker by rememberSaveable { mutableStateOf(false) }
    var showGroupPicker by rememberSaveable { mutableStateOf(false) }
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
                        timePrecision = selection.timePrecision,
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
    if (showGroupPicker) {
        HomeCaseGroupPickerDialog(
            groups = state.availableFormGroups,
            selectedGroupId = form.groupId,
            saving = state.mutationSaving,
            creationError = state.mutationError,
            onDismiss = { showGroupPicker = false },
            onSelect = { groupId ->
                onFormChange { it.copy(groupId = groupId) }
                showGroupPicker = false
            },
            onCreate = onCreateGroup,
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
                    if (compatibilityParticipantRole != null) {
                        "录入${if (compatibilityParticipantRole == SexForFortuneDirection.MAN) "男方" else "女方"}八字"
                    } else if (form.libraryType == CaseLibraryType.CELEBRITY) {
                        "名人案例录入"
                    } else {
                        "首页排盘"
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isCompatibilityParticipantCreate) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("返回")
                    }
                }
            }
        }
        BoxWithConstraints(
            modifier = Modifier.weight(1f),
        ) {
            // 内外屏的余量问题不同：内屏不再让入口卡填满剩余空间，
            // 外屏只把导航上方已确认的少量余量给入口卡。
            val compactHomeLayout = !isCompatibilityParticipantCreate &&
                maxWidth >= INNER_DISPLAY_HOME_MIN_WIDTH
            val contentVerticalPadding = if (compactHomeLayout) 8.dp else 12.dp
            val sectionSpacing = if (compactHomeLayout) 8.dp else 12.dp
            val formVerticalPadding = if (compactHomeLayout) 8.dp else 12.dp
            val formNameRowHeight = if (compactHomeLayout) 48.dp else 52.dp
            val pickerRowHeight = if (compactHomeLayout) 60.dp else 68.dp
            val compactDividerPadding = if (compactHomeLayout) 6.dp else 10.dp
            val saveRowHeight = if (compactHomeLayout) 42.dp else 46.dp
            val submitButtonHeight = if (compactHomeLayout) 48.dp else 50.dp
            // 入口卡两边都由明确的 10dp 规则约束，不再把可用高度留成无定义空白。
            val homeQuickEntryEdgeGap = 10.dp
            val quickEntryNavigationBottomInset =
                ROOT_NAVIGATION_BAR_HEIGHT +
                    ROOT_NAVIGATION_BOTTOM_MARGIN +
                    homeQuickEntryEdgeGap
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = contentVerticalPadding)
                    .padding(
                        bottom = bottomContentInset + if (isCompatibilityParticipantCreate) {
                            0.dp
                        } else {
                            quickEntryNavigationBottomInset
                        },
                    ),
            ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                    ) {
                    if (form.libraryType != CaseLibraryType.CELEBRITY && !isCompatibilityParticipantCreate) {
                        BaziHomeSkinHeader(
                            height = if (compactHomeLayout) 132.dp else 148.dp,
                            modifier = Modifier.testTag("home_skin_header"),
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_input_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = formVerticalPadding)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(formNameRowHeight)
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
                                    "请输入姓名",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                            BasicTextField(
                                value = form.alias,
                                onValueChange = { value ->
                                    onFormChange { it.copy(alias = value, name = value) }
                                },
                                enabled = !state.saving,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = NanfengInk,
                                    textAlign = TextAlign.End,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics { contentDescription = "姓名" }
                                    .testTag("case_alias"),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        innerTextField()
                                    }
                                },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = compactDividerPadding, bottom = 2.dp),
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
                    HorizontalDivider(modifier = Modifier.padding(top = compactDividerPadding))
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
                        rowHeight = pickerRowHeight,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HomePickerRow(
                        title = "出生地区",
                        value = form.locationName.ifBlank { "请选择地区" },
                        supporting = "",
                        onClick = { showBirthplacePicker = true },
                        tag = "open_birthplace_picker",
                        rowHeight = pickerRowHeight,
                    )
                    if (!isCompatibilityParticipantCreate) {
                        HorizontalDivider()
                        HomePickerRow(
                            title = "分组",
                            value = state.availableFormGroups
                                .firstOrNull { it.id == form.groupId }
                                ?.name
                                ?: "全部",
                            supporting = "",
                            onClick = { showGroupPicker = true },
                            tag = "open_case_group_picker",
                            rowHeight = pickerRowHeight,
                        )
                        HorizontalDivider()
                    }
                    if (!isCompatibilityParticipantCreate) Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(saveRowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "保存命例",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = NanfengInk,
                        )
                        Surface(
                            onClick = { saveCase = !saveCase },
                            modifier = Modifier
                                .size(width = 66.dp, height = 40.dp)
                                .semantics {
                                    contentDescription = "保存命例"
                                    role = Role.Switch
                                    stateDescription = if (saveCase) "已开启" else "已关闭"
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Switch(
                                    checked = saveCase,
                                    onCheckedChange = null,
                                    modifier = Modifier.clearAndSetSemantics { },
                                )
                            }
                        }
                    }
                            Button(
                        onClick = if (isCompatibilityParticipantCreate || saveCase) onSubmit else onPreview,
                        enabled = !state.saving && !state.previewing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(submitButtonHeight)
                            .testTag(if (saveCase) "save_case" else "preview_case"),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(25.dp),
                            ) {
                        Text(
                            when {
                                state.saving -> "正在排盘并保存…"
                                state.previewing -> "正在即时排盘…"
                                isCompatibilityParticipantCreate -> "保存并返回"
                                else -> "开始排盘"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                            }
                        }
                    }
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
                    if (state.duplicateCandidates.isNotEmpty()) {
                        DuplicateCandidatesCard(
                            candidates = state.duplicateCandidates,
                            saving = state.saving || state.previewing,
                            onConfirm = onConfirmDuplicate,
                        )
                    }
                }
                if (!isCompatibilityParticipantCreate) {
                    Spacer(modifier = Modifier.height(homeQuickEntryEdgeGap))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            BaziCompatibilityHomeEntry(
                                onClick = onOpenBaziCompatibility,
                                compact = compactHomeLayout,
                                modifier = Modifier.weight(1f),
                            )
                            AlmanacHomeEntry(
                                onClick = onOpenAlmanac,
                                compact = compactHomeLayout,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
        }
    }
}
}

@Composable
private fun BaziCompatibilityHomeEntry(
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .semantics { contentDescription = "打开八字合盘" }
            .testTag("home_open_bazi_compatibility"),
        shape = HomeQuickEntryPillShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(if (compact) 42.dp else 52.dp),
                    shape = CircleShape,
                    color = NanfengGold.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bazi_compatibility),
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 22.dp else 26.dp),
                            tint = NanfengGold,
                        )
                    }
                }
                Text(
                    "八字合盘",
                    style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NanfengInk,
                )
            }
        }
    }
}

@Composable
private fun HomeChoiceGroup(
    options: List<Pair<String, Boolean>>,
    onSelect: (String) -> Unit,
    tags: List<String>,
    enabled: Boolean = true,
    itemWidth: Dp = 70.dp,
    itemHeight: Dp = 32.dp,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            options.forEachIndexed { index, (label, selected) ->
                Surface(
                    onClick = { onSelect(label) },
                    enabled = enabled,
                    modifier = Modifier
                        .height(itemHeight)
                        .width(itemWidth)
                        .testTag(tags[index]),
                    shape = RoundedCornerShape(24.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
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
    titleWidth: Dp = 68.dp,
    rowHeight: Dp = 68.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$title，$value" }
            .padding(horizontal = 2.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            modifier = Modifier.width(titleWidth),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = NanfengInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

private fun String.dropLastTextElement(): String {
    if (isEmpty()) return this
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    val previousBoundary = iterator.preceding(length)
    return if (previousBoundary == BreakIterator.DONE) "" else substring(0, previousBoundary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseScreen(
    form: CaseFormState,
    groupNames: String,
    availableGroups: List<CaseGroup>,
    error: String?,
    saving: Boolean,
    onBack: () -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onGroupNamesChange: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onSave: () -> Unit,
    onCreateCopy: () -> Unit,
    duplicateCandidates: List<DuplicateCaseCandidate>,
    onConfirmDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBirthPicker by rememberSaveable { mutableStateOf(false) }
    var showBirthplacePicker by rememberSaveable { mutableStateOf(false) }
    var showGroupPicker by rememberSaveable { mutableStateOf(false) }
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
                        timePrecision = selection.timePrecision,
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
    if (showGroupPicker) {
        HomeCaseGroupPickerDialog(
            groups = availableGroups,
            selectedGroupId = availableGroups.firstOrNull { it.name == groupNames }?.id,
            saving = saving,
            creationError = error,
            onDismiss = { showGroupPicker = false },
            onSelect = { groupId ->
                onGroupNamesChange(
                    availableGroups.firstOrNull { it.id == groupId }?.name.orEmpty(),
                )
                showGroupPicker = false
            },
            onCreate = onCreateGroup,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("edit_case_screen"),
    ) {
        TopAppBar(
            title = { Text("编辑命例", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EditCaseSectionCard(title = "基本资料") {
                val displayName = form.name.ifBlank { form.alias }
                Text(
                    "姓名",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { value ->
                        onFormChange { it.copy(alias = value, name = value) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("case_alias"),
                    singleLine = true,
                    enabled = !saving,
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                    ),
                )
                Text(
                    "性别",
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
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
                    enabled = !saving,
                )
            }

            EditCaseSectionCard(title = "分组") {
                HomePickerRow(
                    title = "分组",
                    value = groupNames.ifBlank { "全部" },
                    supporting = "",
                    onClick = { if (!saving) showGroupPicker = true },
                    tag = "edit_case_groups",
                )
            }

            EditCaseSectionCard(title = "出生时间") {
                HomePickerRow(
                    title = if (form.calendarSystem == CalendarSystem.LUNAR) {
                        "农历出生时间"
                    } else {
                        "公历出生时间"
                    },
                    value = form.birthDateTimeDisplay(),
                    supporting = "",
                    onClick = { if (!saving) showBirthPicker = true },
                    tag = "open_birth_datetime_picker",
                    titleWidth = 104.dp,
                )
            }

            EditCaseSectionCard(title = "出生地区") {
                HomePickerRow(
                    title = "地区",
                    value = form.locationName.ifBlank { "请选择地区" },
                    supporting = "",
                    onClick = { if (!saving) showBirthplacePicker = true },
                    tag = "open_birthplace_picker",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "真太阳时校正",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = form.useTrueSolarTime,
                        onCheckedChange = { checked ->
                            onFormChange { it.copy(useTrueSolarTime = checked) }
                        },
                        modifier = Modifier
                            .size(width = 56.dp, height = 48.dp)
                            .semantics { contentDescription = "真太阳时校正" }
                            .testTag("birth_true_solar_time"),
                        enabled = !saving,
                    )
                }
                if (form.useTrueSolarTime) {
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
                            modifier = Modifier.weight(1f).testTag("birth_longitude"),
                            label = { Text("经度") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(15.dp),
                        )
                        OutlinedTextField(
                            value = form.latitude,
                            onValueChange = { value ->
                                onFormChange { it.copy(latitude = value) }
                            },
                            modifier = Modifier.weight(1f).testTag("birth_latitude"),
                            label = { Text("纬度") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(15.dp),
                        )
                    }
                }
            }

            if (form.availableUtcOffsetSeconds.isNotEmpty()) {
                EditCaseSectionCard(title = "时区确认") {
                    Text(
                        "该当地时间出现两次，请选择原始记录对应的 UTC offset。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
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
            }

            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_error"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            if (duplicateCandidates.isNotEmpty()) {
                DuplicateCandidatesCard(
                    candidates = duplicateCandidates,
                    saving = saving,
                    onConfirm = onConfirmDuplicate,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCreateCopy,
                    enabled = !saving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("create_case_copy"),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("创建副本")
                }
                Button(
                    onClick = onSave,
                    enabled = !saving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_case"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(if (saving) "保存中…" else "保存")
                }
            }
        }
    }
}

@Composable
private fun EditCaseSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text(
                title,
                modifier = Modifier.padding(bottom = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NanfengInk,
            )
            content()
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
                        timePrecision = selection.timePrecision,
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
                SectionHeading("身份信息", "")
                OutlinedTextField(
                    value = form.name.ifBlank { form.alias },
                    onValueChange = { value ->
                        onFormChange { it.copy(alias = value, name = value) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("case_alias"),
                    label = { Text("姓名 *") },
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
            containerColor = Color.White,
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
            DecadeFortuneDetailsView(calculation)
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
                state.objectiveSummaryLoading && state.objectiveSummary == null ->
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
    captureRegistry: CaseDetailPageCaptureRegistry,
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
    onAiCommentaryChange: (String) -> Unit,
    onSelectAiCommentaryVersion: (String) -> Unit,
    onAddNotesTimeline: (CaseEventTimelineLevel, Int, String) -> Unit,
    onNotesTimelineContentChange: (String, String) -> Unit,
    onSaveCaseNotes: () -> Unit,
    onEnsureCaseNotesHydrated: () -> Unit,
    onRetryPreparedSave: (Boolean) -> Unit,
    onReturnToPreparedSaveForm: () -> Unit,
    onDismissPreparedSaveDialog: () -> Unit,
    onShowPreparedSaveDialog: () -> Unit,
    onDuplicate: () -> Unit,
    onExportSingleCase: () -> Unit,
    onOpenObjectiveSummary: () -> Unit,
    onOpenExternalAnalysis: () -> Unit,
    onExportCaseImage: () -> Unit,
    onShareCaseImage: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    onSelectSection: (CaseDetailSection) -> Unit,
    onFortuneObservationChange: (String, String) -> Unit,
    onFortuneObservationSelect: (ProfessionalFortuneSelection) -> Unit,
    onFortuneToday: () -> Unit,
    onAiPromptCopied: () -> Unit,
    onOpenAiCommentary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pendingSaveNeedsDuplicateDecision =
        state.detailIsTransient &&
            !state.detailSavePending &&
            state.detailSaveDialogVisible &&
            state.duplicateCandidates.isNotEmpty()
    val pendingSaveNeedsRetry =
        state.detailIsTransient &&
            !state.detailSavePending &&
            state.detailSaveDialogVisible &&
            state.detailSaveError != null &&
            !pendingSaveNeedsDuplicateDecision
    if (pendingSaveNeedsDuplicateDecision) {
        AlertDialog(
            onDismissRequest = onDismissPreparedSaveDialog,
            title = { Text("发现疑似重复命例") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("命盘已生成，但尚未写入案例库。系统不会自动覆盖或合并已有命例。")
                    state.duplicateCandidates.take(5).forEach { candidate ->
                        Text(
                            "• ${candidate.summary.alias}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        "可返回修改出生资料，或确认仍保留两份。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onRetryPreparedSave(true) },
                    modifier = Modifier.testTag("confirm_duplicate_save_from_detail"),
                ) { Text("仍然保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = onReturnToPreparedSaveForm,
                    modifier = Modifier.testTag("return_to_case_form_from_detail"),
                ) { Text("返回修改") }
            },
        )
    } else if (pendingSaveNeedsRetry) {
        AlertDialog(
            onDismissRequest = onDismissPreparedSaveDialog,
            title = { Text("命例尚未保存") },
            text = {
                Text(requireNotNull(state.detailSaveError))
            },
            confirmButton = {
                Button(
                    onClick = { onRetryPreparedSave(false) },
                    modifier = Modifier.testTag("retry_prepared_case_save"),
                ) { Text("重试保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = onReturnToPreparedSaveForm,
                    modifier = Modifier.testTag("return_to_case_form_from_save_error"),
                ) { Text("返回修改") }
            },
        )
    }
    var managementMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var requestedDetailSection by remember(state.detail?.id) {
        mutableStateOf(state.detailSection)
    }
    var pendingSectionCommit by remember { mutableStateOf<CaseDetailSection?>(null) }
    val latestOnSelectSection by rememberUpdatedState(onSelectSection)
    LaunchedEffect(state.detailSection) {
        if (pendingSectionCommit == null) {
            requestedDetailSection = state.detailSection
        }
    }
    LaunchedEffect(pendingSectionCommit) {
        val target = pendingSectionCommit ?: return@LaunchedEffect
        withFrameNanos { }
        latestOnSelectSection(target)
        if (pendingSectionCommit == target) {
            pendingSectionCommit = null
        }
    }
    // Keep the identity header on the section that is actually drawn so users
    // never see a new header paired with old content.
    var renderedDetailSection by remember(state.detail?.id) {
        mutableStateOf(state.detailSection)
    }
    var mountedDetailContentId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.detail?.id) {
        val detailId = state.detail?.id
        if (detailId == null) {
            mountedDetailContentId = null
        } else {
            // Commit the correct app bar, tabs and identity header first. The dense
            // chart body joins on the following frame instead of holding navigation.
            withFrameNanos { }
            mountedDetailContentId = detailId
        }
    }
    val detailScrollStates = remember(state.detail?.id) {
        CaseDetailSection.entries.associateWith { ScrollState(0) }
    }
    val caseNotesReady = state.detail?.let { detail ->
        state.caseNotesCaseId == detail.id &&
            state.caseNotesRevision == detail.revision &&
            !state.caseNotesHydrating
    } == true
    LaunchedEffect(
        state.detail?.id,
        state.detail?.revision,
        state.caseNotesCaseId,
        state.caseNotesRevision,
        state.caseNotesHydrating,
    ) {
        if (state.detail != null && !caseNotesReady) {
            onEnsureCaseNotesHydrated()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .registerCaseDetailPageBounds(captureRegistry)
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
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                if (state.detailIsTransient) {
                    val label = when {
                        state.detailSavePending -> "正在保存"
                        state.detailSaveError != null -> "保存未完成"
                        else -> "未保存"
                    }
                    val labelColor = if (state.detailSaveError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    if (state.detailSaveError != null) {
                        TextButton(
                            onClick = onShowPreparedSaveDialog,
                            modifier = Modifier.testTag("open_prepared_case_save_action"),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = labelColor)
                        }
                    } else {
                        Text(
                            label,
                            modifier = Modifier.padding(end = 12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = labelColor,
                        )
                    }
                } else Box {
                    androidx.compose.material3.IconButton(
                        onClick = { managementMenuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_case_management"),
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "管理命例")
                    }
                    NanfengWhiteDropdownMenu(
                        expanded = managementMenuExpanded,
                        onDismissRequest = { managementMenuExpanded = false },
                    ) {
                        fun closeThen(action: () -> Unit) {
                            managementMenuExpanded = false
                            action()
                        }
                        if (state.detail?.deletedAt == null) {
                            NanfengOverflowMenuItem(
                                label = "编辑基本资料",
                                icon = Icons.Filled.Edit,
                                accent = NanfengGreen,
                                onClick = { closeThen(onEditCase) },
                                modifier = Modifier.testTag("edit_case_button"),
                            )
                            NanfengOverflowMenuItem(
                                label = "分组与标签",
                                icon = Icons.Filled.Label,
                                accent = NanfengGreen,
                                onClick = { closeThen(onEditMetadata) },
                                modifier = Modifier.testTag("edit_metadata_button"),
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            NanfengOverflowMenuItem(
                                label = if (state.singleCaseExchangeBusy) "正在准备导出…" else "导出当前命例",
                                icon = Icons.Filled.FileUpload,
                                accent = NanfengGoldText,
                                onClick = { closeThen(onExportSingleCase) },
                                enabled = !state.singleCaseExchangeBusy,
                                modifier = Modifier.testTag("export_single_case_menu"),
                            )
                            NanfengOverflowMenuItem(
                                label = "保存命盘长图",
                                icon = Icons.Filled.Image,
                                accent = NanfengGoldText,
                                onClick = { closeThen(onExportCaseImage) },
                                enabled = !state.caseImageBusy,
                                modifier = Modifier.testTag("save_case_image_to_gallery"),
                            )
                            NanfengOverflowMenuItem(
                                label = "分享命盘长图",
                                icon = Icons.Filled.Share,
                                accent = NanfengOrange,
                                onClick = { closeThen(onShareCaseImage) },
                                enabled = !state.caseImageBusy,
                                modifier = Modifier.testTag("share_case_image_button"),
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            NanfengOverflowMenuItem(
                                label = "移入回收站",
                                icon = Icons.Filled.Delete,
                                accent = NanfengSolarTermRed,
                                onClick = { closeThen(onMoveToTrash) },
                            )
                        } else {
                            NanfengOverflowMenuItem(
                                label = "恢复命例",
                                icon = Icons.Filled.RestoreFromTrash,
                                accent = NanfengGreen,
                                onClick = { closeThen(onRestore) },
                            )
                        }
                    }
                }
            },
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        )
        if (state.detail != null && state.detailError == null) {
            CaseDetailTabs(
                selectedSection = requestedDetailSection,
                onSelectSection = { section ->
                    if (section != requestedDetailSection) {
                        requestedDetailSection = section
                        // The tab and already-retained page update locally first;
                        // the keyed effect commits shared/restorable state next frame.
                        pendingSectionCommit = section
                    }
                },
            )
        }
        if (state.detail != null && state.detailError == null) {
            WenzhenCaseIdentityHeader(
                case = state.detail,
                adopted = state.detail.calculationSnapshots.asReversed()
                    .firstOrNull { it.adopted },
                section = renderedDetailSection,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.detailLoading && state.detail == null ->
                    LoadingBox("正在读取命例详情…")
                state.detailError != null -> ErrorBox(
                    message = state.detailError,
                    actionLabel = "返回列表",
                    onAction = onBack,
                )
                state.detail != null && mountedDetailContentId == state.detail.id ->
                    ReferenceCaseDetailContent(
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
                        onOwnerFeedbackChange = onOwnerFeedbackChange,
                        onMasterCommentaryChange = onMasterCommentaryChange,
                        onAiCommentaryChange = onAiCommentaryChange,
                        onSelectAiCommentaryVersion = onSelectAiCommentaryVersion,
                        onAddNotesTimeline = onAddNotesTimeline,
                        onNotesTimelineContentChange = onNotesTimelineContentChange,
                        onSaveCaseNotes = onSaveCaseNotes,
                        caseNotesReady = caseNotesReady,
                        selectedSection = requestedDetailSection,
                        onDisplayedSectionChanged = { renderedDetailSection = it },
                        scrollStates = detailScrollStates,
                        captureRegistry = captureRegistry,
                        mutationSaving = state.mutationSaving,
                        mutationError = state.mutationError,
                        fortuneObservationDate = state.fortuneObservationDate,
                        fortuneObservationTime = state.fortuneObservationTime,
                        fortunePosition = state.fortunePosition,
                        professionalFortunePosition = state.professionalFortunePosition,
                        fortunePositionError = state.fortunePositionError,
                        fortunePositionLoading = state.fortunePositionLoading,
                        onFortuneObservationChange = onFortuneObservationChange,
                        onFortuneObservationSelect = onFortuneObservationSelect,
                        onFortuneToday = onFortuneToday,
                        onAiPromptCopied = onAiPromptCopied,
                        onOpenAiCommentary = onOpenAiCommentary,
                    )
                state.detail != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (requestedDetailSection == CaseDetailSection.FORTUNE) {
                                MaterialTheme.colorScheme.background
                            } else {
                                Color.White
                            },
                        ),
                )
            }
        }
    }
}

internal fun CaseDetailSection.displayName(): String = when (this) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DetailIdentityHeaderHeight)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ZodiacIdentityBadge(
                        westernZodiac = westernZodiac,
                        iconRes = iconRes,
                        size = 50.dp,
                        iconSize = 18.dp,
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
                        .height(DetailIdentityHeaderHeight)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("notes_identity_header"),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val pillars = result?.fourPillars?.let {
                        listOf(it.year, it.month, it.day, it.hour)
                    }.orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    case.name.value ?: case.alias,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notes_identity_name"),
                                    color = NanfengGoldLight,
                                    fontSize = 15.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(modifier = Modifier.width(136.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    case.sexForFortuneDirection.displayName(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notes_identity_sex"),
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (pillars.isEmpty()) {
                            Text(
                                "暂无已采用排盘",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Row(
                                modifier = Modifier.testTag("notes_identity_four_pillars"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pillars.forEachIndexed { index, pillar ->
                                    Column(
                                        modifier = Modifier.width(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            pillar.take(1),
                                            modifier = Modifier.testTag("notes_identity_stem_$index"),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            pillar.drop(1).take(1),
                                            modifier = Modifier.testTag("notes_identity_branch_$index"),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 1.dp, bottom = 3.dp),
                        color = Color.White.copy(alpha = 0.12f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "大运",
                            modifier = Modifier.width(34.dp),
                            color = NanfengGold,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
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
                                lineHeight = 12.sp,
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

// Basic chart, professional chart and notes are one detail-header family. 78dp preserves
// the notes page's four pillars and decade rail at readable sizes while giving the two
// chart pages enough vertical breathing room for the identity information.
private val DetailIdentityHeaderHeight = 78.dp

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
    AI_COMMENTARY,
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
    onOwnerFeedbackChange: (String) -> Unit,
    onMasterCommentaryChange: (String) -> Unit,
    onAiCommentaryChange: (String) -> Unit,
    onSelectAiCommentaryVersion: (String) -> Unit,
    onAddNotesTimeline: (CaseEventTimelineLevel, Int, String) -> Unit,
    onNotesTimelineContentChange: (String, String) -> Unit,
    onSaveCaseNotes: () -> Unit,
    caseNotesReady: Boolean,
    selectedSection: CaseDetailSection,
    onDisplayedSectionChanged: (CaseDetailSection) -> Unit,
    scrollStates: Map<CaseDetailSection, ScrollState>,
    captureRegistry: CaseDetailPageCaptureRegistry,
    mutationSaving: Boolean,
    mutationError: String?,
    fortuneObservationDate: String,
    fortuneObservationTime: String,
    fortunePosition: FortunePosition?,
    professionalFortunePosition: ProfessionalFortunePosition?,
    fortunePositionError: String?,
    fortunePositionLoading: Boolean,
    onFortuneObservationChange: (String, String) -> Unit,
    onFortuneObservationSelect: (ProfessionalFortuneSelection) -> Unit,
    onFortuneToday: () -> Unit,
    onAiPromptCopied: () -> Unit,
    onOpenAiCommentary: () -> Unit,
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
                onFortuneObservationChange(date, time)
                showObservationPicker = false
            },
            showQuickLocateInput = true,
        )
    }
    val wide = LocalConfiguration.current.screenWidthDp >= 840
    var retainedSections by remember(case.id) {
        mutableStateOf(setOf(selectedSection))
    }
    LaunchedEffect(case.id) {
        // Let the selected page become fully interactive before doing any
        // off-screen composition work. This keeps list -> detail entry clean.
        delay(900L)
        val selectedIndex = CaseDetailSection.entries.indexOf(selectedSection)
        CaseDetailSection.entries
            .filterNot { it == selectedSection }
            .sortedBy { section ->
                kotlin.math.abs(CaseDetailSection.entries.indexOf(section) - selectedIndex)
            }
            .forEach { section ->
                // Never compose three dense pages in one frame. Spread the idle warm-up
                // and keep every finished page resident for layer-only switching.
                delay(180L)
                withFrameNanos { }
                retainedSections = retainedSections + section
            }
    }
    LaunchedEffect(selectedSection) {
        retainedSections = retainedSections + selectedSection
        captureRegistry.updateDisplayedSection(selectedSection)
        onDisplayedSectionChanged(selectedSection)
    }
    val sectionsToRender = retainedSections + selectedSection
    Box(modifier = Modifier.fillMaxSize()) {
        CaseDetailSection.entries.forEach { pageSection ->
            if (pageSection !in sectionsToRender) return@forEach
            val active = pageSection == selectedSection
            key(pageSection) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (active) 1f else 0f)
                        .graphicsLayer { alpha = if (active) 1f else 0f }
                        .then(
                            if (active) Modifier else Modifier.clearAndSetSemantics { },
                        ),
                ) {
                    val pageScrollState = scrollStates.getValue(pageSection)
                    if (pageSection == CaseDetailSection.RECORDS) {
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
                            if (!caseNotesReady) {
                                LoadingBox("正在校验断事笔记…")
                            } else {
                                ReferenceCaseNotes(
                                    case = case,
                                    adopted = adopted,
                                    mode = notesMode,
                                    captureForLongImage = captureRegistry.notesCaptureActive,
                                    onModeChange = { notesMode = it },
                                    onEditRecord = onEditRecord,
                                    draft = caseNotesDraft,
                                    onOwnerFeedbackChange = onOwnerFeedbackChange,
                                    onMasterCommentaryChange = onMasterCommentaryChange,
                                    onAiCommentaryChange = onAiCommentaryChange,
                                    onSelectAiCommentaryVersion = onSelectAiCommentaryVersion,
                                    onAddTimeline = onAddNotesTimeline,
                                    onTimelineContentChange = onNotesTimelineContentChange,
                                    onOpenAiCommentary = onOpenAiCommentary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .registerCaseDetailPageCaptureTarget(
                                            captureRegistry,
                                            pageSection,
                                            pageScrollState,
                                        )
                                        .then(
                                            if (
                                                notesMode == CaseNotesMode.OWNER_FEEDBACK ||
                                                captureRegistry.notesCaptureActive
                                            ) {
                                                Modifier.verticalScroll(pageScrollState)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .padding(bottom = 10.dp),
                                )
                                CaseNotesSaveFooter(
                                    case = case,
                                    saving = caseNotesSaving,
                                    saveError = caseNotesSaveError,
                                    onSave = onSaveCaseNotes,
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .registerCaseDetailPageCaptureTarget(
                                    captureRegistry,
                                    pageSection,
                                    pageScrollState,
                                )
                                .verticalScroll(pageScrollState)
                                .background(
                                    color = if (pageSection == CaseDetailSection.FORTUNE) {
                                        MaterialTheme.colorScheme.background
                                    } else {
                                        Color.White
                                    },
                                    shape = if (pageSection == CaseDetailSection.BASIC_INFO) {
                                        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                                    } else {
                                        RoundedCornerShape(0.dp)
                                    },
                                )
                                .padding(
                                    horizontal = when {
                                        pageSection == CaseDetailSection.FORTUNE -> 0.dp
                                        wide -> 28.dp
                                        else -> 10.dp
                                    },
                                    vertical = when (pageSection) {
                                        CaseDetailSection.BASIC_INFO -> 8.dp
                                        CaseDetailSection.FORTUNE -> 0.dp
                                        else -> 6.dp
                                    },
                                ),
                        ) {
                            when (pageSection) {
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
                                    professionalFortunePosition = professionalFortunePosition.takeIf {
                                        selectedSection == CaseDetailSection.BASIC_CHART
                                    },
                                    onAiPromptCopied = onAiPromptCopied,
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
                                            fortunePositionLoading = fortunePositionLoading,
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
                }
            }
        }
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
    val usesHistoricalCalculationProxy = result?.normalizedInput?.calendarInput !=
        case.birthInput.calendarInput
    val solarText = if (usesHistoricalCalculationProxy) {
        case.birthInput.displayDateTime()
    } else {
        conversion?.solarDateTime?.display() ?: case.birthInput.displayDateTime()
    }
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
    if (usesHistoricalCalculationProxy && result != null) {
        WenzhenFactRow("史实出生", solarText.removePrefix("公历 "), alternate = nextAlternate())
        WenzhenFactRow(
            "排盘采用日期",
            result.normalizedInput.displayDateTime().removePrefix("公历 "),
            alternate = nextAlternate(),
        )
        WenzhenFactRow("排盘采用农历", lunarText, alternate = nextAlternate())
    } else {
        WenzhenFactRow("农历", lunarText, alternate = nextAlternate())
        WenzhenFactRow("阳历", solarText.removePrefix("公历 "), alternate = nextAlternate())
    }
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    professionalFortunePosition: ProfessionalFortunePosition?,
    onAiPromptCopied: () -> Unit,
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
    BasicChartAiPromptSection(
        case = case,
        observation = professionalFortunePosition,
        onCopied = onAiPromptCopied,
        modifier = Modifier.padding(top = 18.dp),
    )
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
    onAiCommentaryChange: (String) -> Unit,
    onSelectAiCommentaryVersion: (String) -> Unit,
    onAddTimeline: (CaseEventTimelineLevel, Int, String) -> Unit,
    onTimelineContentChange: (String, String) -> Unit,
    onOpenAiCommentary: () -> Unit,
    captureForLongImage: Boolean,
    modifier: Modifier = Modifier,
) {
    var pickerVisible by rememberSaveable(case.id) { mutableStateOf(false) }
    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val switcherWidth = minOf(maxWidth * 0.82f, 330.dp)
            val switcherShape = RoundedCornerShape(18.dp)
            val switcherThemeColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .width(switcherWidth)
                    // The visible rail is intentionally compact, but the transparent
                    // segment surfaces below retain a full 48dp touch target.
                    .height(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(switcherShape)
                        .background(Color.White)
                        // The outline is deliberately drawn before the segments: the active
                        // color reaches the shared edge instead of becoming a small pill
                        // floating inside a white frame.
                        .drawWithContent {
                            val stroke = 1.5.dp.toPx()
                            drawRoundRect(
                                color = switcherThemeColor,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(18.dp.toPx()),
                                style = Stroke(width = stroke),
                            )
                            drawContent()
                        }
                        .testTag("notes_mode_switcher"),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NotesModeTab(
                        text = "命主反馈",
                        selected = mode == CaseNotesMode.OWNER_FEEDBACK,
                        groupedPill = true,
                        onClick = { onModeChange(CaseNotesMode.OWNER_FEEDBACK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notes_mode_owner"),
                    )
                    NotesModeTab(
                        text = "师傅点评",
                        selected = mode == CaseNotesMode.MASTER_COMMENTARY,
                        groupedPill = true,
                        onClick = { onModeChange(CaseNotesMode.MASTER_COMMENTARY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notes_mode_master"),
                    )
                    NotesModeTab(
                        text = "AI 点评",
                        selected = mode == CaseNotesMode.AI_COMMENTARY,
                        groupedPill = true,
                        onClick = { onModeChange(CaseNotesMode.AI_COMMENTARY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notes_mode_ai"),
                    )
                }
            }
        }
        if (captureForLongImage) {
            CaseNotesCommentaryHeader(
                title = "命主反馈",
                tag = "owner_feedback_capture_header",
            )
            CaseNotesTextEditor(
                value = draft.ownerFeedback,
                onValueChange = onOwnerFeedbackChange,
                placeholder = "直接记录命主的反馈信息",
                enabled = case.deletedAt == null,
                modifier = Modifier.testTag("owner_feedback_capture"),
            )
            Text(
                "关键事件反馈记录",
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CaseNotesTimeline(
                draft = draft,
                calculation = adopted?.result,
                displayAsIndependentChronology = case.libraryType == CaseLibraryType.CELEBRITY,
                enabled = case.deletedAt == null,
                onContentChange = onTimelineContentChange,
            )
            CaseNotesCommentaryHeader(title = "师傅点评", tag = "master_commentary_capture")
            CaseNotesTextEditor(
                value = draft.masterCommentary,
                onValueChange = onMasterCommentaryChange,
                placeholder = "输入师傅点评",
                enabled = case.deletedAt == null,
                modifier = Modifier.testTag("master_commentary_capture"),
            )
            AiCommentaryEditor(
                value = draft.aiCommentary,
                versions = draft.aiCommentaryVersions,
                selectedRecordId = draft.aiCommentaryRecordId,
                onValueChange = onAiCommentaryChange,
                onSelectVersion = onSelectAiCommentaryVersion,
                enabled = case.deletedAt == null,
                onGenerate = onOpenAiCommentary,
                captureForLongImage = true,
                modifier = Modifier.testTag("ai_commentary_capture"),
            )
        } else if (mode == CaseNotesMode.OWNER_FEEDBACK) {
            // 与师傅点评复用同一个固定标题槽：左侧金线、文本基线和上下留白完全一致，
            // 切换时不会因为标题本身的高度差让正文发生跳动。
            CaseNotesCommentaryHeader(
                title = "命主反馈",
                tag = "owner_feedback_header",
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
                        .size(48.dp)
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
                displayAsIndependentChronology = case.libraryType == CaseLibraryType.CELEBRITY,
                enabled = case.deletedAt == null,
                onContentChange = onTimelineContentChange,
            )
        } else if (mode == CaseNotesMode.MASTER_COMMENTARY) {
            CaseNotesCommentaryHeader(
                title = "师傅点评",
                tag = "master_commentary_header",
            )
            CaseNotesTextEditor(
                value = draft.masterCommentary,
                onValueChange = onMasterCommentaryChange,
                placeholder = "输入师傅点评",
                enabled = case.deletedAt == null,
                fillAvailableSpace = !captureForLongImage,
                modifier = if (captureForLongImage) Modifier else Modifier.weight(1f)
                    .testTag("master_commentary_input"),
            )
        } else {
            AiCommentaryEditor(
                value = draft.aiCommentary,
                versions = draft.aiCommentaryVersions,
                selectedRecordId = draft.aiCommentaryRecordId,
                onValueChange = onAiCommentaryChange,
                onSelectVersion = onSelectAiCommentaryVersion,
                enabled = case.deletedAt == null,
                onGenerate = onOpenAiCommentary,
                captureForLongImage = captureForLongImage,
                modifier = if (captureForLongImage) Modifier else Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))
    }

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
private fun AiCommentaryEditor(
    value: String,
    versions: List<AiCommentaryVersion>,
    selectedRecordId: String?,
    onValueChange: (String) -> Unit,
    onSelectVersion: (String) -> Unit,
    enabled: Boolean,
    onGenerate: () -> Unit,
    captureForLongImage: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CaseNotesCommentaryHeader(
            title = "AI 点评",
            tag = "ai_commentary_header",
            actionLabel = if (value.isBlank()) "生成点评" else "再次生成",
            actionTag = "open_ai_commentary",
            enabled = enabled,
            onAction = onGenerate,
        )
        if (captureForLongImage && versions.size > 1) {
            // 长图是可离线留存的完整视图，因此顺序呈现全部模型版本，不能只导出当前一份。
            versions.asReversed().forEach { version ->
                val versionValue = if (version.recordId == selectedRecordId) value else version.body
                Text(
                    version.label,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                CaseNotesTextEditor(
                    value = versionValue,
                    onValueChange = onValueChange,
                    placeholder = "输入或粘贴 AI 点评",
                    enabled = enabled,
                    modifier = Modifier.testTag("ai_commentary_capture_${version.recordId}"),
                )
            }
        } else {
            AiCommentaryVersionSelector(
                versions = versions,
                selectedRecordId = selectedRecordId,
                enabled = enabled,
                onSelectVersion = onSelectVersion,
            )
            CaseNotesTextEditor(
                value = value,
                onValueChange = onValueChange,
                placeholder = "输入或粘贴 AI 点评",
                enabled = enabled,
                fillAvailableSpace = !captureForLongImage,
                modifier = if (captureForLongImage) Modifier else Modifier.weight(1f)
                    .testTag("ai_commentary_input"),
            )
        }
    }
}

@Composable
private fun AiCommentaryVersionSelector(
    versions: List<AiCommentaryVersion>,
    selectedRecordId: String?,
    enabled: Boolean,
    onSelectVersion: (String) -> Unit,
) {
    if (versions.size <= 1) return
    LazyRow(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("ai_commentary_version_selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(versions, key = { it.recordId }) { version ->
            val selected = version.recordId == selectedRecordId
            Surface(
                onClick = { onSelectVersion(version.recordId) },
                enabled = enabled && !selected,
                shape = RoundedCornerShape(14.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("ai_commentary_version_${version.recordId}"),
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .widthIn(max = 156.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        version.label,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * 师傅与 AI 点评共享同一标题槽，避免切换模式时正文编辑区发生高度和基线跳动。
 */
@Composable
private fun CaseNotesCommentaryHeader(
    title: String,
    tag: String,
    actionLabel: String? = null,
    actionTag: String? = null,
    enabled: Boolean = true,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
            .padding(top = 18.dp, bottom = 8.dp)
            .height(40.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
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
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (actionLabel != null && onAction != null) {
            Surface(
                onClick = onAction,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(36.dp)
                    .widthIn(min = 76.dp, max = 88.dp)
                    .testTag(requireNotNull(actionTag)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaseNotesSaveFooter(
    case: BaziCase,
    saving: Boolean,
    saveError: String?,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("case_notes_save_footer"),
    ) {
        // 正常状态不占用一行提示；按钮自身已经承担“保存中”的即时反馈。
        // 仅在确有错误时显示可行动的异常信息，避免把“已保存”一类重复状态堆在正文下方。
        if (saveError != null) {
            Text(
                saveError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onSave,
            enabled = case.deletedAt == null && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_case_notes_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
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
    fillAvailableSpace: Boolean = false,
) {
    val editorScrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val openUrl by rememberUpdatedState<(String) -> Unit> { url -> uriHandler.openUri(url) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        Box(
            modifier = if (fillAvailableSpace) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier.fillMaxWidth()
            },
        ) {
            Surface(
                modifier = if (fillAvailableSpace) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = (minLines * 25).dp + 32.dp)
                },
                shape = RoundedCornerShape(14.dp),
                color = if (enabled) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = (if (fillAvailableSpace) {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScrollState)
                    } else {
                        Modifier.fillMaxSize()
                    })
                        .padding(start = 16.dp, top = 14.dp, end = 18.dp, bottom = 14.dp)
                        .openCaseNotesUrlWhenTapped(
                            value = value,
                            textLayoutResult = { textLayoutResult },
                            onOpenUrl = openUrl,
                        ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                    maxLines = Int.MAX_VALUE,
                    visualTransformation = caseNotesWebUrlVisualTransformation(
                        MaterialTheme.colorScheme.primary,
                    ),
                    onTextLayout = { textLayoutResult = it },
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isEmpty()) {
                                Text(
                                    placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            if (fillAvailableSpace && editorScrollState.maxValue > 0) {
                CaseNotesEditorScrollbar(
                    scrollState = editorScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 14.dp, end = 5.dp, bottom = 14.dp),
                )
            }
        }
    }
}

private val CaseNotesWebUrlPattern = Regex("""https?://[^\s<>"'，。；、！？]+""")

internal fun extractCaseNotesWebUrls(value: String): List<String> =
    CaseNotesWebUrlPattern.findAll(value)
        .map { it.normalizedCaseNotesWebUrl() }
        .filter(String::isNotEmpty)
        .distinct()
        .toList()

internal fun caseNotesWebUrlAtOffset(value: String, offset: Int): String? =
    CaseNotesWebUrlPattern.findAll(value)
        .firstNotNullOfOrNull { match ->
            val normalized = match.normalizedCaseNotesWebUrl()
            normalized.takeIf { offset in match.range.first until (match.range.first + it.length) }
        }

private fun MatchResult.normalizedCaseNotesWebUrl(): String =
    value.trimEnd('.', ',', ';', ':', '!', '?', '。', '，', '；', '！', '？', '、')

private fun caseNotesWebUrlVisualTransformation(linkColor: Color): VisualTransformation =
    VisualTransformation { original ->
        TransformedText(
            buildAnnotatedString {
                append(original)
                CaseNotesWebUrlPattern.findAll(original.text).forEach { match ->
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        match.range.first,
                        match.range.last + 1,
                    )
                }
            },
            OffsetMapping.Identity,
        )
    }

private fun Modifier.openCaseNotesUrlWhenTapped(
    value: String,
    textLayoutResult: () -> TextLayoutResult?,
    onOpenUrl: (String) -> Unit,
): Modifier = pointerInput(value) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
        val layout = textLayoutResult() ?: return@awaitEachGesture
        caseNotesWebUrlAtOffset(value, layout.getOffsetForPosition(up.position))?.let { url ->
            up.consume()
            onOpenUrl(url)
        }
    }
}

@Composable
private fun CaseNotesEditorScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f))
            .testTag("case_notes_editor_scrollbar"),
    ) {
        val thumbHeight = maxHeight * 0.24f
        val progress = if (scrollState.maxValue == 0) {
            0f
        } else {
            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        }
        Box(
            modifier = Modifier
                .offset(y = (maxHeight - thumbHeight) * progress)
                .fillMaxWidth()
                .height(thumbHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun CaseNotesTimeline(
    draft: CaseNotesDraft,
    calculation: CalculationResult?,
    displayAsIndependentChronology: Boolean,
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
    if (displayAsIndependentChronology) {
        CelebrityCaseTimeline(
            entries = draft.timeline,
            enabled = enabled,
            onContentChange = onContentChange,
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
private fun CelebrityCaseTimeline(
    entries: List<CaseNotesTimelineDraft>,
    enabled: Boolean,
    onContentChange: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .drawBehind {
                val x = 9.dp.toPx()
                drawLine(
                    color = NanfengGold.copy(alpha = 0.32f),
                    start = Offset(x, 14.dp.toPx()),
                    end = Offset(x, size.height - 12.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        entries.sortedBy { it.year }.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.padding(top = 8.dp).size(18.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        4.dp,
                        NanfengGold.copy(alpha = 0.72f),
                    ),
                ) {}
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(
                        entry.year.toTimelineYearLabel(),
                        color = NanfengGold,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    CaseNotesTimelineInput(
                        entry = entry,
                        enabled = enabled,
                        onContentChange = onContentChange,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

private fun Int.toTimelineYearLabel(): String =
    if (this < 0) "公元前${-this}年" else "${this}年"

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
                    text = entry.sourceLabel.ifBlank { "${entry.year}年  ${entry.stemBranch}" } +
                        entry.status.takeIf { it.isNotBlank() }?.let { "  【$it】" }.orEmpty(),
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
            text = entry.sourceLabel.ifBlank { "${entry.year}年  ${entry.stemBranch}" } +
                entry.status.takeIf { it.isNotBlank() }?.let { "  【$it】" }.orEmpty() +
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
    Column(modifier = modifier.fillMaxWidth()) {
        CaseNotesTextEditor(
            value = entry.content,
            onValueChange = { onContentChange(entry.id, it) },
            placeholder = "输入这一阶段的关键事件",
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timeline_event_input")
                .semantics { contentDescription = "时间线事件输入 ${entry.id}" },
            minLines = 1,
        )
    }
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NotesModeTab(
    text: String,
    selected: Boolean,
    groupedPill: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groupedPill) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val visualShape = RoundedCornerShape(18.dp)
        Box(
            modifier = modifier
                .fillMaxHeight()
                // The outer layer is intentionally larger for touch. Its system indication
                // is disabled because a rectangular ripple would not match the compact rail.
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(visualShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ),
            ) {
                if (pressed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = if (selected) 0.13f else 0.08f),
                            ),
                    )
                }
            }
            Text(
                text,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        return
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = when {
                    selected -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
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
    fortunePositionLoading: Boolean,
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
            .background(MaterialTheme.colorScheme.background)
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
                Text(if (singleCaseExchangeBusy) "正在导出…" else "导出当前命例")
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
                        fortunePositionLoading = fortunePositionLoading,
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
                        onClick = { onEditRecord(record.id) },
                        enabled = case.deletedAt == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("record_card"),
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
                        onClick = { onEditEvent(event.id) },
                        enabled = case.deletedAt == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
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
    fortunePositionLoading: Boolean,
    onOpenObservationPicker: () -> Unit,
    onObservationSelect: (ProfessionalFortuneSelection) -> Unit = {},
    onToday: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("professional_page_surface")
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        if (professionalFortunePosition == null) {
            if (fortunePositionError == null) {
                ProfessionalFortuneLoading(
                    loading = fortunePositionLoading,
                )
            } else {
                ReferenceEmptyText(fortunePositionError)
            }
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
        ProfessionalTimelineRows(
            position = professionalFortunePosition,
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
private fun ProfessionalFortuneLoading(
    loading: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("professional_fortune_warm_start"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            Text(
                if (loading) "正在生成专业细盘…" else "专业细盘正在准备，请稍后重试。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            .padding(bottom = 3.dp)
            .testTag("professional_fortune_position"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
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
private fun ProfessionalTimelineRows(
    position: ProfessionalFortunePosition,
    onSelect: (ProfessionalFortuneSelection) -> Unit,
) {
    var pendingSelection by remember(position) {
        mutableStateOf<ProfessionalFortuneSelection?>(null)
    }
    val selectImmediately: (ProfessionalFortuneSelection) -> Unit = { selection ->
        pendingSelection = selection
        onSelect(selection)
    }
    ProfessionalTimelineRow(
        title = "大运",
        items = position.decadeTimeline,
        tag = "decade_fortune_details",
        layer = ProfessionalFortuneLayer.DECADE,
        pendingSelection = pendingSelection,
        onSelect = selectImmediately,
    )
    ProfessionalTimelineRow(
        title = "流年",
        items = position.annualTimeline,
        tag = "annual_fortune_details",
        layer = ProfessionalFortuneLayer.ANNUAL,
        pendingSelection = pendingSelection,
        onSelect = selectImmediately,
    )
    ProfessionalTimelineRow(
        title = "流月",
        items = position.monthlyTimeline,
        tag = "monthly_fortune_details",
        layer = ProfessionalFortuneLayer.MONTHLY,
        pendingSelection = pendingSelection,
        onSelect = selectImmediately,
    )
    ProfessionalTimelineRow(
        title = "流日",
        items = position.dailyTimeline,
        tag = "daily_fortune_details",
        layer = ProfessionalFortuneLayer.DAILY,
        pendingSelection = pendingSelection,
        onSelect = selectImmediately,
    )
    ProfessionalTimelineRow(
        title = "流时",
        items = position.hourlyTimeline,
        tag = "hourly_fortune_details",
        layer = ProfessionalFortuneLayer.HOURLY,
        pendingSelection = pendingSelection,
        onSelect = selectImmediately,
    )
}

@Composable
private fun ProfessionalTimelineRow(
    title: String,
    items: List<ProfessionalTimelineItem>,
    tag: String,
    layer: ProfessionalFortuneLayer,
    pendingSelection: ProfessionalFortuneSelection?,
    onSelect: (ProfessionalFortuneSelection) -> Unit,
) {
    if (items.isEmpty()) return
    val trailingDivider = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = professionalTimelineCardColor(title)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ProfessionalTimelineCardHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(26.dp)
                    .fillMaxHeight()
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
                        .fillMaxHeight()
                        .testTag("${tag}_list"),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                        val selected = pendingSelection
                            ?.takeIf { it.layer == layer }
                            ?.observedAt
                            ?.let { it == item.observedAt }
                            ?: item.selected
                        Box(
                            modifier = Modifier
                                .width(timelineColumnWidth)
                                .testTag("${tag}_column")
                                .then(
                                    if (index < items.lastIndex) {
                                        Modifier.drawWithContent {
                                            drawContent()
                                            val dividerStrokeWidth = 1.dp.toPx()
                                            val dividerX = size.width - dividerStrokeWidth / 2f
                                            drawLine(
                                                color = trailingDivider,
                                                start = Offset(dividerX, 0f),
                                                end = Offset(dividerX, size.height),
                                                strokeWidth = dividerStrokeWidth,
                                            )
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            ProfessionalTimelineCell(
                                item = item,
                                selected = selected,
                                modifier = Modifier.fillMaxWidth(),
                                compact = true,
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
private val ProfessionalTimelineCardHeight = 104.dp

private val ProfessionalTimelinePrimarySurface = Color.White
private val ProfessionalTimelineAlternateSurface = Color.White

private fun professionalTimelineCardColor(title: String): Color = when (title) {
    "流年", "流日" -> ProfessionalTimelineAlternateSurface
    else -> ProfessionalTimelinePrimarySurface
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfessionalTimelineCell(
    item: ProfessionalTimelineItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val platformViewConfiguration = LocalViewConfiguration.current
    val exactCellViewConfiguration = remember(platformViewConfiguration) {
        object : ViewConfiguration by platformViewConfiguration {
            override val minimumTouchTargetSize = DpSize(0.dp, 0.dp)
        }
    }
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentEnforcement provides false,
        LocalViewConfiguration provides exactCellViewConfiguration,
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .testTag("timeline_${item.key}")
                .then(if (selected) Modifier.testTag("selected_${item.key}") else Modifier),
            color = if (selected) NanfengGold.copy(alpha = 0.12f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (compact) 0.dp else 2.dp,
                    vertical = 4.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Text(
                item.label,
                modifier = Modifier.testTag("timeline_${item.key}_label"),
                fontSize = if (compact) 8.sp else 9.sp,
                lineHeight = if (compact) 9.sp else 11.sp,
                color = if (selected) NanfengGold else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Box(modifier = Modifier.testTag("timeline_${item.key}_stem_detail")) {
                    ProfessionalTenGodLabel(item.stemTenGod, compact)
                }
                Text(
                    stem.toString(),
                    modifier = Modifier.testTag("timeline_${item.key}_stem"),
                    color = baziElementColor(stem),
                    fontSize = if (compact) 14.sp else 15.sp,
                    lineHeight = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
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
@OptIn(ExperimentalMaterial3Api::class)
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
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    onClick = onOpenPicker,
                    modifier = Modifier
                        .testTag("fortune_observation_picker")
                        .height(30.dp)
                        .semantics { contentDescription = "修改观察时间" },
                    color = Color.White.copy(alpha = 0.76f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        NanfengGold.copy(alpha = 0.28f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = NanfengGold.copy(alpha = 0.72f),
                        )
                        Text(
                            "阳历 $date $time　${detail.ifBlank { "农历未记录" }}",
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
                    onClick = onToday,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("fortune_today")
                        .semantics { contentDescription = "定位今天" },
                    color = NanfengGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(24.dp),
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
    val themeBackground = LocalBaziSkinTokens.current.background
    val themeText = LocalBaziSkinTokens.current.textPrimary
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(tag)) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth().background(themeBackground)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = themeText,
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DecadeFortuneDetailsView(calculation: CalculationResult) {
    val birth = calculation.solarBirthDateTimeForFortuneDisplay()
    Column(modifier = Modifier.fillMaxWidth().testTag("decade_fortune_details")) {
        calculation.decadeFortunes.take(8).forEachIndexed { index, decade ->
            DetailRow(
                "第${index + 1}运",
                "${decade.name}　${decade.completedAgeRangeDisplay(birth)}　" +
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
    TimeSourceType.WENZHEN_WEB_IMPORT -> "历史网页资料导入"
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
    CaseSourceType.WENZHEN_WEB_IMPORT -> "历史网页资料导入"
    CaseSourceType.CURATED_CELEBRITY_CATALOG -> "名人案例统一资料"
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
    TextRecordSourceType.WENZHEN_WEB_IMPORT -> "历史网页资料导入"
    TextRecordSourceType.CURATED_RESEARCH -> "资料编审"
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
            "阳历${year.displayHistoricalYear()}年${month}月${day}日"
        }
        is BirthCalendarInput.Lunar -> with(calendar.dateTime) {
            "农历${year.displayHistoricalYear()}年${if (isLeapMonth) "闰" else ""}${month}月${day}日"
        }
    }

private fun CivilDateTime.display(): String =
    "%s-%02d-%02d %02d:%02d:%02d".format(
        year.displayHistoricalYear(),
        month,
        day,
        hour,
        minute,
        second,
    )

internal fun Int.displayHistoricalYear(): String =
    if (this <= 0) "公元前${1 - this}" else toString()

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
    CaseSortOrder.NAME_ASC -> "姓名"
    CaseSortOrder.LAST_VIEWED_DESC -> "最近查看"
    CaseSortOrder.UPDATED_DESC -> "最近编辑"
    CaseSortOrder.CREATED_DESC -> "最近创建"
    CaseSortOrder.BIRTH_ASC -> "出生时间"
}
