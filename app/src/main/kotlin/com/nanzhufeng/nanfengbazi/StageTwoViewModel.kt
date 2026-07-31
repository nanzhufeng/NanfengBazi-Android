package com.nanzhufeng.nanfengbazi

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
import com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportInput
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderResult
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderer
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummary
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryContract
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryErrorCode
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryFailure
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryGenerator
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryInput
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryResult
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CommentaryCandidateRuleEvidence
import com.nanzhufeng.nanfengbazi.domain.CommentaryTextRange
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeAdoptionErrorCode
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeAdoptionFailure
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeAdoptionResult
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidate
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateErrorCode
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractionInput
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractionResult
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractor
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateFailure
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateSet
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeSourceEvidence
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeTextRange
import com.nanzhufeng.nanfengbazi.domain.FortunePosition
import com.nanzhufeng.nanfengbazi.domain.FortunePositionResolver
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookup
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupCandidate
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupEvidence
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupResult
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidate
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateAdoptionErrorCode
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateAdoptionFailure
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateAdoptionResult
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateErrorCode
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractionInput
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractionResult
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractor
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateFailure
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateSet
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneResolver
import com.nanzhufeng.nanfengbazi.domain.RenderedCaseImage
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.nanzhufeng.nanfengbazi.imageparser.DeterministicMasterCommentaryCandidateExtractor
import com.nanzhufeng.nanfengbazi.imageparser.DeterministicFeedbackThemeCandidateExtractor
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.file.Path
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
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
    data object CaseComparison : AppDestination
    data object FourPillarsLookup : AppDestination
    data object RecordHub : AppDestination
    data object Settings : AppDestination
    data object CreateCase : AppDestination
    data object ScreenshotImportReview : AppDestination
    data class CaseDetail(val caseId: String) : AppDestination
    data class CaseObjectiveSummary(val caseId: String) : AppDestination
    data class MasterCommentaryCandidates(
        val caseId: String,
        val recordId: String,
    ) : AppDestination
    data class FeedbackThemeCandidates(
        val caseId: String,
        val recordId: String,
    ) : AppDestination
    data class EditCase(val caseId: String) : AppDestination
    data class AddBirthTimeCandidate(val caseId: String) : AppDestination
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

enum class CaseDetailSection {
    BASIC_INFO,
    BASIC_CHART,
    FORTUNE,
    RECORDS,
}

class StageTwoNavigator {
    private val stack = mutableListOf<AppDestination>(AppDestination.CaseList)
    val current: AppDestination
        get() = stack.last()

    fun openCreate(): AppDestination {
        return push(AppDestination.CreateCase)
    }

    fun openRecordHub(): AppDestination = openRoot(AppDestination.RecordHub)

    fun openSettings(): AppDestination = openRoot(AppDestination.Settings)

    fun openCaseComparison(): AppDestination {
        return push(AppDestination.CaseComparison)
    }

    fun openFourPillarsLookup(): AppDestination {
        return push(AppDestination.FourPillarsLookup)
    }

    fun openScreenshotImportReview(): AppDestination {
        return push(AppDestination.ScreenshotImportReview)
    }

    fun openDetail(caseId: String): AppDestination {
        return push(AppDestination.CaseDetail(caseId))
    }

    fun openObjectiveSummary(caseId: String): AppDestination {
        return push(AppDestination.CaseObjectiveSummary(caseId))
    }

    fun openMasterCommentaryCandidates(
        caseId: String,
        recordId: String,
    ): AppDestination {
        return push(AppDestination.MasterCommentaryCandidates(caseId, recordId))
    }

    fun openFeedbackThemeCandidates(
        caseId: String,
        recordId: String,
    ): AppDestination = push(AppDestination.FeedbackThemeCandidates(caseId, recordId))

    fun openEditCase(caseId: String): AppDestination {
        return push(AppDestination.EditCase(caseId))
    }

    fun openBirthTimeCandidate(caseId: String): AppDestination {
        return push(AppDestination.AddBirthTimeCandidate(caseId))
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
        return openRoot(AppDestination.CaseList)
    }

    fun restore(destination: AppDestination) {
        stack.clear()
        when (destination) {
            AppDestination.CaseList,
            AppDestination.RecordHub,
            AppDestination.Settings,
            AppDestination.CreateCase,
            AppDestination.ScreenshotImportReview,
            -> stack += destination

            AppDestination.CaseComparison -> {
                stack += AppDestination.CaseList
                stack += destination
            }

            AppDestination.FourPillarsLookup -> {
                stack += AppDestination.CreateCase
                stack += destination
            }

            is AppDestination.CaseDetail -> {
                stack += AppDestination.CaseList
                stack += destination
            }

            is AppDestination.CaseObjectiveSummary -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.MasterCommentaryCandidates -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.FeedbackThemeCandidates -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.EditCase -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.AddBirthTimeCandidate -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.EditMetadata -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.EditTextRecord -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }

            is AppDestination.EditEvent -> {
                stack += AppDestination.CaseList
                stack += AppDestination.CaseDetail(destination.caseId)
                stack += destination
            }
        }
    }

    private fun openRoot(destination: AppDestination): AppDestination {
        stack.clear()
        stack += destination
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
    val recentCases: List<CaseSummary> = emptyList(),
    val comparisonCandidates: List<CaseSummary> = emptyList(),
    val comparisonLeftCaseId: String? = null,
    val comparisonRightCaseId: String? = null,
    val comparisonReport: CaseComparisonReport? = null,
    val comparisonLoading: Boolean = false,
    val comparisonError: String? = null,
    val fourPillarsLookupForm: FourPillarsLookupFormState =
        FourPillarsLookupFormState(),
    val fourPillarsLookupCandidates: List<FourPillarsLookupCandidate> = emptyList(),
    val fourPillarsLookupEvidence: FourPillarsLookupEvidence? = null,
    val fourPillarsLookupLoading: Boolean = false,
    val fourPillarsLookupHasSearched: Boolean = false,
    val fourPillarsLookupError: String? = null,
    val listLoading: Boolean = false,
    val listError: String? = null,
    val form: CaseFormState = CaseFormState(),
    val formError: String? = null,
    val saving: Boolean = false,
    val previewing: Boolean = false,
    val instantCalculation: CalculationResult? = null,
    val duplicateCandidates: List<DuplicateCaseCandidate> = emptyList(),
    val detail: BaziCase? = null,
    val detailSection: CaseDetailSection = CaseDetailSection.BASIC_INFO,
    val fortuneObservationDate: String = "",
    val fortuneObservationTime: String = "12:00",
    val fortunePosition: FortunePosition? = null,
    val professionalFortunePosition: ProfessionalFortunePosition? = null,
    val fortunePositionError: String? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val editForm: CaseFormState = CaseFormState(),
    val candidateLabel: String = "",
    val candidateForm: CaseFormState = CaseFormState(),
    val metadataDraft: CaseMetadataDraft = CaseMetadataDraft(),
    val recordDraft: TextRecordDraft = TextRecordDraft(),
    val eventDraft: EventDraft = EventDraft(),
    val mutationSaving: Boolean = false,
    val mutationError: String? = null,
    val deleteConfirmationVisible: Boolean = false,
    val caseImageConfirmationMode: CaseImageDeliveryMode? = null,
    val caseImageBusy: Boolean = false,
    val caseImageError: String? = null,
    val caseImageLastResultCode: CaseImageExportErrorCode? = null,
    val objectiveSummary: CaseObjectiveSummary? = null,
    val objectiveSummaryLoading: Boolean = false,
    val objectiveSummaryFailure: CaseObjectiveSummaryFailure? = null,
    val objectiveSummaryCopied: Boolean = false,
    val commentaryCandidateSet: MasterCommentaryCandidateSet? = null,
    val commentaryCandidateFailure: MasterCommentaryCandidateFailure? = null,
    val commentaryCandidateAdoptionFailure:
        MasterCommentaryCandidateAdoptionFailure? = null,
    val commentaryCandidateSavingId: String? = null,
    val feedbackThemeCandidateSet: FeedbackThemeCandidateSet? = null,
    val feedbackThemeCandidateFailure: FeedbackThemeCandidateFailure? = null,
    val feedbackThemeAdoptionFailure: FeedbackThemeAdoptionFailure? = null,
    val feedbackThemeSavingId: String? = null,
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
    private val birthTimeCandidates: BirthTimeCandidateUseCase,
    private val caseMetadata: CaseMetadataUseCase = CaseMetadataUseCase(caseRepository),
    private val textRecords: TextRecordUseCase,
    private val caseEvents: CaseEventUseCase,
    private val caseLifecycle: CaseLifecycleUseCase = CaseLifecycleUseCase(caseRepository),
    private val navigator: StageTwoNavigator = StageTwoNavigator(),
    private val clock: Clock = Clock.systemUTC(),
    private val observationClock: Clock = Clock.systemDefaultZone(),
    private val fortunePositionResolver: FortunePositionResolver? = null,
    private val professionalFortuneResolver: ProfessionalFortuneResolver? = null,
    private val fourPillarsLookup: FourPillarsLookup? = null,
    private val caseImageRenderer: CaseImageRenderer? = null,
    private val objectiveSummaryGenerator: CaseObjectiveSummaryGenerator =
        CaseObjectiveSummaryContract,
    private val commentaryCandidateExtractor: MasterCommentaryCandidateExtractor =
        DeterministicMasterCommentaryCandidateExtractor(),
    private val feedbackThemeCandidateExtractor: FeedbackThemeCandidateExtractor =
        DeterministicFeedbackThemeCandidateExtractor(),
    private val singleCaseExchange: SingleCaseExchangeService =
        SingleCaseExchangeService(caseRepository, clock),
    private val singleCaseBundleService: SingleCaseBundleOperations? = null,
    private val caseBackupService: CaseBackupOperations? = null,
    private val backupAttachmentRoot: Path? = null,
    private val backupWorkRoot: Path? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        savedStateHandle.get<Bundle>(SAVED_UI_STATE_KEY)
            ?.toStageTwoUiState()
            ?: StageTwoUiState(),
    )
    val state: StateFlow<StageTwoUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var comparisonJob: Job? = null
    private var fourPillarsLookupJob: Job? = null
    private var pendingExportPassword: CharArray? = null
    private var pendingSingleCaseBundleExport: Boolean = false
    private var pendingSingleCaseBundleCommit: PendingSingleCaseBundleCommit? = null
    private var pendingFullBackupPassword: CharArray? = null
    private var pendingCaseImage: RenderedCaseImage? = null

    init {
        if (mutableState.value.fortuneObservationDate.isBlank()) {
            mutableState.update {
                it.copy(
                    fortuneObservationDate = LocalDate.now(observationClock).toString(),
                )
            }
        }
        if (mutableState.value.fortuneObservationTime.isBlank()) {
            mutableState.update {
                it.copy(fortuneObservationTime = "12:00")
            }
        }
        navigator.restore(mutableState.value.destination)
        savedStateHandle.setSavedStateProvider(SAVED_UI_STATE_KEY) {
            mutableState.value.toSavedStateBundle()
        }
        recoverInterruptedRestores()
        restoreCaseBoundDestination()
        refreshCases()
        if (mutableState.value.destination == AppDestination.CaseComparison) {
            loadComparisonWorkspace()
        }
        if (
            mutableState.value.destination == AppDestination.FourPillarsLookup &&
            mutableState.value.fourPillarsLookupHasSearched
        ) {
            searchFourPillars()
        }
    }

    private fun restoreCaseBoundDestination() {
        val caseId = mutableState.value.destination.caseIdOrNull() ?: return
        viewModelScope.launch {
            val restoredCase = try {
                caseRepository.findById(caseId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val summaryResult = if (
                restoredCase != null &&
                mutableState.value.destination is AppDestination.CaseObjectiveSummary
            ) {
                objectiveSummaryGenerator.generate(
                    CaseObjectiveSummaryInput(restoredCase),
                )
            } else {
                null
            }
            val candidateDestination =
                mutableState.value.destination as? AppDestination.MasterCommentaryCandidates
            val commentaryRecord = if (restoredCase != null && candidateDestination != null) {
                restoredCase.textRecords.firstOrNull {
                    it.id == candidateDestination.recordId
                }
            } else {
                null
            }
            val candidateResult = if (restoredCase != null && commentaryRecord != null) {
                commentaryCandidateExtractor.extract(
                    MasterCommentaryCandidateExtractionInput(
                        sourceRecordId = commentaryRecord.id,
                        sourceRecordType = commentaryRecord.type,
                        sourceContent = commentaryRecord.content,
                        sourceRevision =
                            restoredCase.sourceRecordRevision(commentaryRecord.id),
                    ),
                )
            } else {
                null
            }
            val feedbackDestination =
                mutableState.value.destination as? AppDestination.FeedbackThemeCandidates
            val feedbackRecord = if (restoredCase != null && feedbackDestination != null) {
                restoredCase.textRecords.firstOrNull { it.id == feedbackDestination.recordId }
            } else {
                null
            }
            val feedbackResult = if (restoredCase != null && feedbackRecord != null) {
                feedbackThemeCandidateExtractor.extract(
                    FeedbackThemeCandidateExtractionInput(
                        sourceRecordId = feedbackRecord.id,
                        sourceRecordType = feedbackRecord.type,
                        sourceContent = feedbackRecord.content,
                        sourceRevision = restoredCase.sourceRecordRevision(feedbackRecord.id),
                    ),
                )
            } else {
                null
            }
            mutableState.update {
                if (restoredCase == null) {
                    it.copy(
                        destination = navigator.backToList(),
                        detail = null,
                        detailLoading = false,
                        detailError = null,
                        objectiveSummary = null,
                        objectiveSummaryLoading = false,
                        objectiveSummaryFailure = null,
                        commentaryCandidateSet = null,
                        commentaryCandidateFailure = null,
                        commentaryCandidateAdoptionFailure = null,
                        commentaryCandidateSavingId = null,
                        feedbackThemeCandidateSet = null,
                        feedbackThemeCandidateFailure = null,
                        feedbackThemeAdoptionFailure = null,
                        feedbackThemeSavingId = null,
                        message = "上次打开的命例无法恢复，已返回命例列表。",
                    )
                } else {
                    when {
                        summaryResult is CaseObjectiveSummaryResult.Success -> it.copy(
                            detail = restoredCase,
                            detailLoading = false,
                            detailError = null,
                            objectiveSummary = summaryResult.summary,
                            objectiveSummaryLoading = false,
                            objectiveSummaryFailure = null,
                        )
                        summaryResult is CaseObjectiveSummaryResult.Rejected -> it.copy(
                            detail = restoredCase,
                            detailLoading = false,
                            detailError = null,
                            objectiveSummary = null,
                            objectiveSummaryLoading = false,
                            objectiveSummaryFailure = summaryResult.failure,
                        )
                        candidateResult is
                            MasterCommentaryCandidateExtractionResult.Success -> it.copy(
                                detail = restoredCase,
                                detailLoading = false,
                                detailError = null,
                                commentaryCandidateSet = candidateResult.candidateSet
                                    .mergeDecisionsFrom(it.commentaryCandidateSet),
                                commentaryCandidateFailure = null,
                                commentaryCandidateAdoptionFailure = null,
                                commentaryCandidateSavingId = null,
                            )
                        candidateResult is
                            MasterCommentaryCandidateExtractionResult.Failure -> it.copy(
                                detail = restoredCase,
                                detailLoading = false,
                                detailError = null,
                                commentaryCandidateSet = null,
                                commentaryCandidateFailure = candidateResult.failure,
                                commentaryCandidateAdoptionFailure = null,
                                commentaryCandidateSavingId = null,
                            )
                        candidateDestination != null -> it.copy(
                            detail = restoredCase,
                            detailLoading = false,
                            detailError = null,
                            commentaryCandidateSet = null,
                            commentaryCandidateFailure = null,
                            commentaryCandidateAdoptionFailure =
                                MasterCommentaryCandidateAdoptionFailure(
                                    MasterCommentaryCandidateAdoptionErrorCode.SOURCE_NOT_FOUND,
                                    "来源点评已不存在，候选无法恢复。",
                                ),
                            commentaryCandidateSavingId = null,
                        )
                        feedbackResult is FeedbackThemeCandidateExtractionResult.Success ->
                            it.copy(
                                detail = restoredCase,
                                detailLoading = false,
                                detailError = null,
                                feedbackThemeCandidateSet = feedbackResult.candidateSet
                                    .mergeDecisionsFrom(it.feedbackThemeCandidateSet),
                                feedbackThemeCandidateFailure = null,
                                feedbackThemeAdoptionFailure = null,
                                feedbackThemeSavingId = null,
                            )
                        feedbackResult is FeedbackThemeCandidateExtractionResult.Failure ->
                            it.copy(
                                detail = restoredCase,
                                detailLoading = false,
                                detailError = null,
                                feedbackThemeCandidateSet = null,
                                feedbackThemeCandidateFailure = feedbackResult.failure,
                                feedbackThemeAdoptionFailure = null,
                                feedbackThemeSavingId = null,
                            )
                        feedbackDestination != null -> it.copy(
                            detail = restoredCase,
                            detailLoading = false,
                            detailError = null,
                            feedbackThemeCandidateSet = null,
                            feedbackThemeCandidateFailure = null,
                            feedbackThemeAdoptionFailure = FeedbackThemeAdoptionFailure(
                                FeedbackThemeAdoptionErrorCode.SOURCE_NOT_FOUND,
                                "来源反馈已不存在，候选无法恢复。",
                            ),
                            feedbackThemeSavingId = null,
                        )
                        else -> it.copy(
                            detail = restoredCase,
                            detailLoading = false,
                            detailError = null,
                        )
                    }
                }
            }
            if (mutableState.value.detailSection == CaseDetailSection.FORTUNE) {
                resolveFortunePosition()
            }
        }
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
                val recentCases = caseRepository.search(
                    CaseSearchRequest(
                        sortOrder = CaseSortOrder.LAST_VIEWED_DESC,
                        visibility = CaseVisibility.ACTIVE,
                    ),
                ).filter { it.lastViewedAt != null }
                    .take(3)
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
                        recentCases = recentCases,
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

    fun requestCaseImageDelivery(mode: CaseImageDeliveryMode) {
        val detail = mutableState.value.detail
        if (detail == null || mutableState.value.caseImageBusy) return
        mutableState.update {
            it.copy(
                caseImageConfirmationMode = mode,
                caseImageError = null,
                caseImageLastResultCode = null,
            )
        }
    }

    fun cancelCaseImageConfirmation() {
        mutableState.update { it.copy(caseImageConfirmationMode = null) }
    }

    fun confirmCaseImageDelivery(
        onPrepared: (CaseImageDeliveryMode, String) -> Unit,
    ) {
        val current = mutableState.value
        val detail = current.detail ?: return
        val mode = current.caseImageConfirmationMode ?: return
        val renderer = caseImageRenderer
        if (renderer == null) {
            setCaseImageFailure(
                CaseImageExportErrorCode.RENDER_FAILED,
                "当前环境未配置命盘图片渲染器。",
            )
            return
        }
        mutableState.update {
            it.copy(
                caseImageConfirmationMode = null,
                caseImageBusy = true,
                caseImageError = null,
                caseImageLastResultCode = null,
            )
        }
        viewModelScope.launch {
            val cached = pendingCaseImage?.takeIf { image ->
                image.facts.caseId == detail.id &&
                    image.facts.caseRevision == detail.revision &&
                    detail.calculationSnapshots.any {
                        it.adopted && it.id == image.facts.adoptedSnapshotId
                    }
            }
            val result = if (cached != null) {
                CaseImageRenderResult.Success(cached)
            } else {
                renderer.render(CaseImageExportInput(detail))
            }
            when (result) {
                is CaseImageRenderResult.Success -> {
                    pendingCaseImage = result.image
                    mutableState.update { it.copy(caseImageBusy = false) }
                    onPrepared(
                        mode,
                        result.image.facts.suggestedFileStem.toSafeImageFileName(),
                    )
                }

                is CaseImageRenderResult.Rejected ->
                    setCaseImageFailure(
                        result.failure.code,
                        result.failure.message,
                    )
            }
        }
    }

    fun exportPreparedCaseImage(openOutput: () -> OutputStream?) {
        val image = pendingCaseImage
        if (image == null) {
            setCaseImageFailure(
                CaseImageExportErrorCode.RENDER_FAILED,
                "已生成的命盘图片已失效，请重新选择导出。",
            )
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(caseImageBusy = true, caseImageError = null) }
            val failure = writeCaseImageBytes(image, openOutput)
            if (failure == null) {
                mutableState.update {
                    it.copy(
                        caseImageBusy = false,
                        caseImageLastResultCode = null,
                        message = "命盘长图已保存到所选系统位置。",
                    )
                }
            } else {
                setCaseImageFailure(failure.code, failure.message)
            }
        }
    }

    fun copyPreparedCaseImageForShare(
        openOutput: () -> OutputStream?,
        onReady: () -> Unit,
    ) {
        val image = pendingCaseImage
        if (image == null) {
            setCaseImageFailure(
                CaseImageExportErrorCode.RENDER_FAILED,
                "已生成的命盘图片已失效，请重新选择分享。",
            )
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(caseImageBusy = true, caseImageError = null) }
            val failure = writeCaseImageBytes(image, openOutput)
            if (failure == null) {
                onReady()
            } else {
                setCaseImageFailure(failure.code, failure.message)
            }
        }
    }

    fun cancelPreparedCaseImageDelivery(mode: CaseImageDeliveryMode) {
        mutableState.update {
            it.copy(
                caseImageBusy = false,
                caseImageLastResultCode = CaseImageExportErrorCode.USER_CANCELLED,
                message = when (mode) {
                    CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE ->
                        "已取消保存，当前详情和已生成图片仍保留。"
                    CaseImageDeliveryMode.SHARE_LONG_IMAGE ->
                        "已取消分享，当前详情和已生成图片仍保留。"
                },
            )
        }
    }

    fun reportNoCaseImageShareTarget() {
        setCaseImageFailure(
            CaseImageExportErrorCode.NO_SHARE_TARGET,
            "系统中没有可接收 PNG 图片的分享目标；可先使用“导出图片”保存文件。",
        )
    }

    fun reportCaseImageShareLaunchFailed() {
        setCaseImageFailure(
            CaseImageExportErrorCode.SHARE_LAUNCH_FAILED,
            "系统分享面板无法打开；当前详情和图片仍保留，可重试或改为保存文件。",
        )
    }

    fun completeCaseImageShare(cancelled: Boolean) {
        mutableState.update {
            it.copy(
                caseImageBusy = false,
                caseImageLastResultCode =
                    if (cancelled) CaseImageExportErrorCode.USER_CANCELLED else null,
                message = if (cancelled) {
                    "分享面板已关闭；南枫八字无法证明图片已由目标应用发送。"
                } else {
                    "图片已交给目标应用；是否实际发送以目标应用状态为准。"
                },
            )
        }
    }

    fun dismissCaseImageError() {
        mutableState.update { it.copy(caseImageError = null) }
    }

    private suspend fun writeCaseImageBytes(
        image: RenderedCaseImage,
        openOutput: () -> OutputStream?,
    ) = try {
        withContext(ioDispatcher) {
            val output = openOutput()
                ?: return@withContext com.nanzhufeng.nanfengbazi.domain.CaseImageExportFailure(
                    CaseImageExportErrorCode.OUTPUT_UNAVAILABLE,
                    "无法打开目标文件。",
                )
            output.use {
                it.write(image.bytes)
                it.flush()
            }
            null
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        com.nanzhufeng.nanfengbazi.domain.CaseImageExportFailure(
            CaseImageExportErrorCode.WRITE_FAILED,
            "命盘图片写入失败；当前详情和已生成图片仍保留。",
        )
    }

    private fun setCaseImageFailure(
        code: CaseImageExportErrorCode,
        message: String,
    ) {
        mutableState.update {
            it.copy(
                caseImageConfirmationMode = null,
                caseImageBusy = false,
                caseImageError = "$message（${code.name}）",
                caseImageLastResultCode = code,
            )
        }
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
        refreshCases()
    }

    fun openRecordHub() {
        mutableState.update {
            it.copy(
                destination = navigator.openRecordHub(),
                message = null,
            )
        }
        refreshCases()
    }

    fun openCaseComparison() {
        mutableState.update {
            it.copy(
                destination = navigator.openCaseComparison(),
                comparisonLoading = true,
                comparisonError = null,
                comparisonReport = null,
                message = null,
            )
        }
        loadComparisonWorkspace()
    }

    fun openFourPillarsLookup() {
        mutableState.update {
            it.copy(
                destination = navigator.openFourPillarsLookup(),
                fourPillarsLookupError = null,
                message = null,
            )
        }
    }

    fun updateFourPillarsLookupForm(
        transform: (FourPillarsLookupFormState) -> FourPillarsLookupFormState,
    ) {
        fourPillarsLookupJob?.cancel()
        mutableState.update {
            it.copy(
                fourPillarsLookupForm = transform(it.fourPillarsLookupForm),
                fourPillarsLookupCandidates = emptyList(),
                fourPillarsLookupEvidence = null,
                fourPillarsLookupLoading = false,
                fourPillarsLookupHasSearched = false,
                fourPillarsLookupError = null,
            )
        }
    }

    fun searchFourPillars() {
        val lookup = fourPillarsLookup
        if (lookup == null) {
            mutableState.update {
                it.copy(
                    fourPillarsLookupHasSearched = true,
                    fourPillarsLookupLoading = false,
                    fourPillarsLookupError = "四柱反查引擎当前不可用，请稍后重试。",
                )
            }
            return
        }
        val form = mutableState.value.fourPillarsLookupForm
        val query = when (val validation = form.toQuery()) {
            is FourPillarsLookupFormValidation.Valid -> validation.query
            is FourPillarsLookupFormValidation.Invalid -> {
                mutableState.update {
                    it.copy(
                        fourPillarsLookupHasSearched = true,
                        fourPillarsLookupLoading = false,
                        fourPillarsLookupCandidates = emptyList(),
                        fourPillarsLookupEvidence = null,
                        fourPillarsLookupError = validation.message,
                    )
                }
                return
            }
        }
        fourPillarsLookupJob?.cancel()
        fourPillarsLookupJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    fourPillarsLookupLoading = true,
                    fourPillarsLookupHasSearched = true,
                    fourPillarsLookupCandidates = emptyList(),
                    fourPillarsLookupEvidence = null,
                    fourPillarsLookupError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    lookup.search(query)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                FourPillarsLookupResult.Failed(
                    com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupError
                        .EngineUnavailable,
                )
            }
            if (mutableState.value.fourPillarsLookupForm != form) return@launch
            mutableState.update {
                when (result) {
                    is FourPillarsLookupResult.Completed -> it.copy(
                        fourPillarsLookupCandidates = result.candidates,
                        fourPillarsLookupEvidence = result.evidence,
                        fourPillarsLookupLoading = false,
                        fourPillarsLookupError = null,
                    )
                    is FourPillarsLookupResult.Failed -> it.copy(
                        fourPillarsLookupCandidates = emptyList(),
                        fourPillarsLookupEvidence = null,
                        fourPillarsLookupLoading = false,
                        fourPillarsLookupError = result.error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun selectComparisonLeft(caseId: String) {
        val candidates = mutableState.value.comparisonCandidates
        if (candidates.none { it.id == caseId }) return
        val rightCaseId = mutableState.value.comparisonRightCaseId
            ?.takeUnless { it == caseId }
        mutableState.update {
            it.copy(
                comparisonLeftCaseId = caseId,
                comparisonRightCaseId = rightCaseId,
                comparisonReport = null,
                comparisonError = null,
                comparisonLoading = rightCaseId != null,
            )
        }
        if (rightCaseId != null) {
            loadComparisonReport(caseId, rightCaseId)
        }
    }

    fun selectComparisonRight(caseId: String) {
        val candidates = mutableState.value.comparisonCandidates
        if (candidates.none { it.id == caseId }) return
        val leftCaseId = mutableState.value.comparisonLeftCaseId
            ?.takeUnless { it == caseId }
        mutableState.update {
            it.copy(
                comparisonLeftCaseId = leftCaseId,
                comparisonRightCaseId = caseId,
                comparisonReport = null,
                comparisonError = null,
                comparisonLoading = leftCaseId != null,
            )
        }
        if (leftCaseId != null) {
            loadComparisonReport(leftCaseId, caseId)
        }
    }

    fun retryCaseComparison() {
        loadComparisonWorkspace()
    }

    private fun loadComparisonWorkspace() {
        comparisonJob?.cancel()
        comparisonJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    comparisonLoading = true,
                    comparisonError = null,
                    comparisonReport = null,
                )
            }
            val candidates = try {
                withContext(ioDispatcher) {
                    caseRepository.search(
                        CaseSearchRequest(
                            sortOrder = CaseSortOrder.UPDATED_DESC,
                            visibility = CaseVisibility.ACTIVE,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        comparisonLoading = false,
                        comparisonError = "无法读取命例对比列表，请重试。",
                    )
                }
                return@launch
            }
            if (candidates.size < 2) {
                mutableState.update {
                    it.copy(
                        comparisonCandidates = candidates,
                        comparisonLeftCaseId = candidates.firstOrNull()?.id,
                        comparisonRightCaseId = null,
                        comparisonReport = null,
                        comparisonLoading = false,
                        comparisonError = "至少需要两个活动命例才能进行客观字段对比。",
                    )
                }
                return@launch
            }
            val current = mutableState.value
            val leftCaseId = current.comparisonLeftCaseId
                ?.takeIf { id -> candidates.any { it.id == id } }
                ?: candidates.first().id
            val rightCaseId = current.comparisonRightCaseId
                ?.takeIf { id -> id != leftCaseId && candidates.any { it.id == id } }
                ?: candidates.first { it.id != leftCaseId }.id
            mutableState.update {
                it.copy(
                    comparisonCandidates = candidates,
                    comparisonLeftCaseId = leftCaseId,
                    comparisonRightCaseId = rightCaseId,
                )
            }
            loadComparisonReportInCurrentJob(leftCaseId, rightCaseId)
        }
    }

    private fun loadComparisonReport(leftCaseId: String, rightCaseId: String) {
        comparisonJob?.cancel()
        comparisonJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    comparisonLoading = true,
                    comparisonError = null,
                    comparisonReport = null,
                )
            }
            loadComparisonReportInCurrentJob(leftCaseId, rightCaseId)
        }
    }

    private suspend fun loadComparisonReportInCurrentJob(
        leftCaseId: String,
        rightCaseId: String,
    ) {
        val report = try {
            val (left, right) = withContext(ioDispatcher) {
                caseRepository.findById(leftCaseId) to caseRepository.findById(rightCaseId)
            }
            if (left == null || right == null || left.deletedAt != null || right.deletedAt != null) {
                null
            } else {
                CaseComparisonEngine.compare(left, right)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        mutableState.update {
            if (
                it.comparisonLeftCaseId != leftCaseId ||
                it.comparisonRightCaseId != rightCaseId
            ) {
                it
            } else if (report == null) {
                it.copy(
                    comparisonLoading = false,
                    comparisonError = "命例已变化或无法读取，请刷新后重新选择。",
                )
            } else {
                it.copy(
                    comparisonReport = report,
                    comparisonLoading = false,
                    comparisonError = null,
                )
            }
        }
    }

    fun openSettings() {
        mutableState.update {
            it.copy(
                destination = navigator.openSettings(),
                message = null,
            )
        }
    }

    fun openScreenshotImportReview() {
        mutableState.update {
            it.copy(
                destination = navigator.openScreenshotImportReview(),
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
        if (pendingCaseImage?.facts?.caseId != caseId) {
            pendingCaseImage = null
        }
        mutableState.update {
            it.copy(
                destination = navigator.openDetail(caseId),
                detail = null,
                detailSection = CaseDetailSection.BASIC_INFO,
                detailLoading = true,
                detailError = null,
                caseImageConfirmationMode = null,
                caseImageBusy = false,
                caseImageError = null,
                caseImageLastResultCode = null,
                objectiveSummary = null,
                objectiveSummaryLoading = false,
                objectiveSummaryFailure = null,
                objectiveSummaryCopied = false,
                commentaryCandidateSet = null,
                commentaryCandidateFailure = null,
                commentaryCandidateAdoptionFailure = null,
                commentaryCandidateSavingId = null,
                feedbackThemeCandidateSet = null,
                feedbackThemeCandidateFailure = null,
                feedbackThemeAdoptionFailure = null,
                feedbackThemeSavingId = null,
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

    fun openObjectiveSummary() {
        val detail = mutableState.value.detail ?: return
        mutableState.update {
            it.copy(
                destination = navigator.openObjectiveSummary(detail.id),
                objectiveSummary = null,
                objectiveSummaryLoading = true,
                objectiveSummaryFailure = null,
                objectiveSummaryCopied = false,
                message = null,
            )
        }
        loadObjectiveSummary(detail.id)
    }

    fun openMasterCommentaryCandidates(recordId: String) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val record = detail.textRecords.firstOrNull { it.id == recordId } ?: return
        val result = commentaryCandidateExtractor.extract(
            MasterCommentaryCandidateExtractionInput(
                sourceRecordId = record.id,
                sourceRecordType = record.type,
                sourceContent = record.content,
                sourceRevision = detail.sourceRecordRevision(record.id),
            ),
        )
        mutableState.update {
            when (result) {
                is MasterCommentaryCandidateExtractionResult.Success -> it.copy(
                    destination = navigator.openMasterCommentaryCandidates(
                        detail.id,
                        record.id,
                    ),
                    commentaryCandidateSet = result.candidateSet,
                    commentaryCandidateFailure = null,
                    commentaryCandidateAdoptionFailure = null,
                    commentaryCandidateSavingId = null,
                    message = null,
                )
                is MasterCommentaryCandidateExtractionResult.Failure -> it.copy(
                    destination = navigator.openMasterCommentaryCandidates(
                        detail.id,
                        record.id,
                    ),
                    commentaryCandidateSet = null,
                    commentaryCandidateFailure = result.failure,
                    commentaryCandidateAdoptionFailure = null,
                    commentaryCandidateSavingId = null,
                    message = null,
                )
            }
        }
    }

    fun updateMasterCommentaryCandidateContent(candidateId: String, content: String) {
        updateMasterCommentaryCandidate(candidateId) {
            if (it.status == MasterCommentaryCandidateStatus.PENDING) {
                it.copy(proposedContent = content)
            } else {
                it
            }
        }
    }

    fun updateMasterCommentaryCandidateCategory(
        candidateId: String,
        category: AnalysisCategory,
    ) {
        updateMasterCommentaryCandidate(candidateId) {
            if (it.status == MasterCommentaryCandidateStatus.PENDING) {
                it.copy(proposedCategory = category)
            } else {
                it
            }
        }
    }

    fun rejectMasterCommentaryCandidate(candidateId: String) {
        updateMasterCommentaryCandidate(candidateId) {
            if (it.status == MasterCommentaryCandidateStatus.PENDING) {
                it.copy(status = MasterCommentaryCandidateStatus.REJECTED)
            } else {
                it
            }
        }
    }

    fun restoreRejectedMasterCommentaryCandidate(candidateId: String) {
        updateMasterCommentaryCandidate(candidateId) {
            if (it.status == MasterCommentaryCandidateStatus.REJECTED) {
                it.copy(status = MasterCommentaryCandidateStatus.PENDING)
            } else {
                it
            }
        }
    }

    fun adoptMasterCommentaryCandidate(candidateId: String) {
        val current = mutableState.value
        val detail = current.detail
        if (detail == null) {
            mutableState.update {
                it.copy(
                    commentaryCandidateAdoptionFailure =
                        MasterCommentaryCandidateAdoptionFailure(
                            MasterCommentaryCandidateAdoptionErrorCode.CONTEXT_NOT_READY,
                            "命例详情仍在恢复，请稍候再采用。",
                        ),
                )
            }
            return
        }
        val candidate = current.commentaryCandidateSet
            ?.candidates
            ?.firstOrNull { it.id == candidateId }
            ?: return
        if (current.commentaryCandidateSavingId != null) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    commentaryCandidateSavingId = candidateId,
                    commentaryCandidateAdoptionFailure = null,
                )
            }
            when (
                val result = textRecords.adoptCommentaryCandidate(
                    caseId = detail.id,
                    expectedRevision = detail.revision,
                    candidate = candidate,
                )
            ) {
                is MasterCommentaryCandidateAdoptionResult.Saved -> {
                    val refreshed = try {
                        caseRepository.findById(detail.id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    mutableState.update {
                        if (refreshed == null) {
                            it.copy(
                                commentaryCandidateSavingId = null,
                                commentaryCandidateAdoptionFailure =
                                    MasterCommentaryCandidateAdoptionFailure(
                                        MasterCommentaryCandidateAdoptionErrorCode.STORAGE_FAILED,
                                        "分析已保存，但详情刷新失败。请返回详情重新打开。",
                                    ),
                            )
                        } else {
                            it.copy(
                                detail = refreshed,
                                commentaryCandidateSet = it.commentaryCandidateSet
                                    ?.updateCandidate(candidateId) { current ->
                                        current.copy(
                                            status =
                                                MasterCommentaryCandidateStatus.ADOPTED,
                                        )
                                    },
                                commentaryCandidateSavingId = null,
                                commentaryCandidateAdoptionFailure = null,
                                message = "已新增正式分析记录；完整师傅点评未修改。",
                            )
                        }
                    }
                    refreshCases()
                }
                is MasterCommentaryCandidateAdoptionResult.Failure ->
                    mutableState.update {
                        it.copy(
                            commentaryCandidateSavingId = null,
                            commentaryCandidateAdoptionFailure = result.failure,
                        )
                    }
            }
        }
    }

    private fun updateMasterCommentaryCandidate(
        candidateId: String,
        transform: (MasterCommentaryCandidate) -> MasterCommentaryCandidate,
    ) {
        mutableState.update {
            it.copy(
                commentaryCandidateSet = it.commentaryCandidateSet
                    ?.updateCandidate(candidateId, transform),
                commentaryCandidateAdoptionFailure = null,
            )
        }
    }

    fun openFeedbackThemeCandidates(recordId: String) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val record = detail.textRecords.firstOrNull { it.id == recordId } ?: return
        val result = feedbackThemeCandidateExtractor.extract(
            FeedbackThemeCandidateExtractionInput(
                sourceRecordId = record.id,
                sourceRecordType = record.type,
                sourceContent = record.content,
                sourceRevision = detail.sourceRecordRevision(record.id),
            ),
        )
        mutableState.update {
            when (result) {
                is FeedbackThemeCandidateExtractionResult.Success -> it.copy(
                    destination = navigator.openFeedbackThemeCandidates(detail.id, record.id),
                    feedbackThemeCandidateSet = result.candidateSet,
                    feedbackThemeCandidateFailure = null,
                    feedbackThemeAdoptionFailure = null,
                    feedbackThemeSavingId = null,
                    message = null,
                )
                is FeedbackThemeCandidateExtractionResult.Failure -> it.copy(
                    destination = navigator.openFeedbackThemeCandidates(detail.id, record.id),
                    feedbackThemeCandidateSet = null,
                    feedbackThemeCandidateFailure = result.failure,
                    feedbackThemeAdoptionFailure = null,
                    feedbackThemeSavingId = null,
                    message = null,
                )
            }
        }
    }

    fun updateFeedbackThemeCandidateTag(candidateId: String, tagName: String) {
        updateFeedbackThemeCandidate(candidateId) {
            if (it.status == FeedbackThemeCandidateStatus.PENDING) {
                it.copy(proposedTagName = tagName)
            } else {
                it
            }
        }
    }

    fun rejectFeedbackThemeCandidate(candidateId: String) {
        updateFeedbackThemeCandidate(candidateId) {
            if (it.status == FeedbackThemeCandidateStatus.PENDING) {
                it.copy(status = FeedbackThemeCandidateStatus.REJECTED)
            } else {
                it
            }
        }
    }

    fun restoreRejectedFeedbackThemeCandidate(candidateId: String) {
        updateFeedbackThemeCandidate(candidateId) {
            if (it.status == FeedbackThemeCandidateStatus.REJECTED) {
                it.copy(status = FeedbackThemeCandidateStatus.PENDING)
            } else {
                it
            }
        }
    }

    fun adoptFeedbackThemeCandidate(candidateId: String) {
        val current = mutableState.value
        val detail = current.detail
        if (detail == null) {
            mutableState.update {
                it.copy(
                    feedbackThemeAdoptionFailure = FeedbackThemeAdoptionFailure(
                        FeedbackThemeAdoptionErrorCode.CONTEXT_NOT_READY,
                        "命例详情仍在恢复，请稍候再采用。",
                    ),
                )
            }
            return
        }
        val candidate = current.feedbackThemeCandidateSet
            ?.candidates
            ?.firstOrNull { it.id == candidateId }
            ?: return
        if (current.feedbackThemeSavingId != null) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    feedbackThemeSavingId = candidateId,
                    feedbackThemeAdoptionFailure = null,
                )
            }
            when (
                val result = caseMetadata.adoptFeedbackThemeCandidate(
                    caseId = detail.id,
                    expectedRevision = detail.revision,
                    candidate = candidate,
                )
            ) {
                is FeedbackThemeAdoptionResult.Saved -> {
                    val refreshed = try {
                        caseRepository.findById(detail.id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    mutableState.update {
                        if (refreshed == null) {
                            it.copy(
                                feedbackThemeSavingId = null,
                                feedbackThemeAdoptionFailure = FeedbackThemeAdoptionFailure(
                                    FeedbackThemeAdoptionErrorCode.STORAGE_FAILED,
                                    "标签已保存，但详情刷新失败。请返回详情重新打开。",
                                ),
                            )
                        } else {
                            it.copy(
                                detail = refreshed,
                                feedbackThemeCandidateSet = it.feedbackThemeCandidateSet
                                    ?.updateCandidate(candidateId) { currentCandidate ->
                                        currentCandidate.copy(
                                            status = FeedbackThemeCandidateStatus.ADOPTED,
                                        )
                                    },
                                feedbackThemeSavingId = null,
                                feedbackThemeAdoptionFailure = null,
                                message = "已新增正式命例标签；完整命主反馈未修改。",
                            )
                        }
                    }
                    refreshCases()
                }
                is FeedbackThemeAdoptionResult.Failure -> mutableState.update {
                    it.copy(
                        feedbackThemeSavingId = null,
                        feedbackThemeAdoptionFailure = result.failure,
                    )
                }
            }
        }
    }

    private fun updateFeedbackThemeCandidate(
        candidateId: String,
        transform: (FeedbackThemeCandidate) -> FeedbackThemeCandidate,
    ) {
        mutableState.update {
            it.copy(
                feedbackThemeCandidateSet = it.feedbackThemeCandidateSet
                    ?.updateCandidate(candidateId, transform),
                feedbackThemeAdoptionFailure = null,
            )
        }
    }

    private fun loadObjectiveSummary(caseId: String) {
        viewModelScope.launch {
            val result = try {
                val current = caseRepository.findById(caseId)
                if (current == null) {
                    CaseObjectiveSummaryResult.Rejected(
                        CaseObjectiveSummaryFailure(
                            CaseObjectiveSummaryErrorCode.SUMMARY_UNAVAILABLE,
                            "未找到该命例，无法生成客观摘要。",
                        ),
                    )
                } else {
                    objectiveSummaryGenerator.generate(
                        CaseObjectiveSummaryInput(current),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                CaseObjectiveSummaryResult.Rejected(
                    CaseObjectiveSummaryFailure(
                        CaseObjectiveSummaryErrorCode.SUMMARY_UNAVAILABLE,
                        "客观摘要生成失败，命例数据未被修改。",
                    ),
                )
            }
            mutableState.update {
                when (result) {
                    is CaseObjectiveSummaryResult.Success -> it.copy(
                        objectiveSummary = result.summary,
                        objectiveSummaryLoading = false,
                        objectiveSummaryFailure = null,
                    )
                    is CaseObjectiveSummaryResult.Rejected -> it.copy(
                        objectiveSummary = null,
                        objectiveSummaryLoading = false,
                        objectiveSummaryFailure = result.failure,
                    )
                }
            }
        }
    }

    fun retryObjectiveSummary() {
        val caseId = (mutableState.value.destination as? AppDestination.CaseObjectiveSummary)
            ?.caseId
            ?: return
        mutableState.update {
            it.copy(
                objectiveSummaryLoading = true,
                objectiveSummaryFailure = null,
                objectiveSummaryCopied = false,
            )
        }
        loadObjectiveSummary(caseId)
    }

    fun copyObjectiveSummary(writeToClipboard: (String) -> Boolean) {
        val summary = mutableState.value.objectiveSummary
        if (summary == null) {
            mutableState.update {
                it.copy(
                    objectiveSummaryCopied = false,
                    objectiveSummaryFailure = CaseObjectiveSummaryFailure(
                        CaseObjectiveSummaryErrorCode.SUMMARY_UNAVAILABLE,
                        "客观摘要尚未生成，请重试后再复制。",
                    ),
                )
            }
            return
        }
        val failure = try {
            if (writeToClipboard(summary.copyText)) {
                null
            } else {
                CaseObjectiveSummaryFailure(
                    CaseObjectiveSummaryErrorCode.CLIPBOARD_UNAVAILABLE,
                    "系统剪贴板不可用，请稍后重试。",
                )
            }
        } catch (_: Exception) {
            CaseObjectiveSummaryFailure(
                CaseObjectiveSummaryErrorCode.COPY_FAILED,
                "复制客观摘要失败，摘要仍保留在当前页面。",
            )
        }
        mutableState.update {
            if (failure == null) {
                it.copy(
                    objectiveSummaryCopied = true,
                    objectiveSummaryFailure = null,
                    message = "客观摘要已复制；其中包含出生资料，请妥善使用。",
                )
            } else {
                it.copy(
                    objectiveSummaryCopied = false,
                    objectiveSummaryFailure = failure,
                )
            }
        }
    }

    fun selectDetailSection(section: CaseDetailSection) {
        mutableState.update {
            it.copy(detailSection = section)
        }
        if (section == CaseDetailSection.FORTUNE) {
            resolveFortunePosition()
        }
    }

    fun updateFortuneObservationDate(value: String) {
        if (value.length > 10 || value.any { !it.isDigit() && it != '-' }) return
        mutableState.update {
            it.copy(
                fortuneObservationDate = value,
                fortunePosition = null,
                professionalFortunePosition = null,
                fortunePositionError = null,
            )
        }
        resolveFortunePosition()
    }

    fun updateFortuneObservationTime(value: String) {
        if (value.length > 5 || value.any { !it.isDigit() && it != ':' }) return
        mutableState.update {
            it.copy(
                fortuneObservationTime = value,
                fortunePosition = null,
                professionalFortunePosition = null,
                fortunePositionError = null,
            )
        }
        resolveFortunePosition()
    }

    private fun resolveFortunePosition() {
        val current = mutableState.value
        val result = current.detail
            ?.calculationSnapshots
            ?.asReversed()
            ?.firstOrNull { it.adopted }
            ?.result
        if (result == null) {
            mutableState.update {
                it.copy(
                    fortunePosition = null,
                    professionalFortunePosition = null,
                    fortunePositionError = "当前命例没有已采用的计算快照。",
                )
            }
            return
        }
        if (fortunePositionResolver == null && professionalFortuneResolver == null) {
            mutableState.update {
                it.copy(
                    fortunePosition = null,
                    professionalFortunePosition = null,
                    fortunePositionError = "当前运行环境未配置岁运定位器。",
                )
            }
            return
        }
        val date = runCatching {
            LocalDate.parse(current.fortuneObservationDate)
        }.getOrNull()
        if (date == null) {
            mutableState.update {
                it.copy(
                    fortunePosition = null,
                    professionalFortunePosition = null,
                    fortunePositionError = "观察日期请按 YYYY-MM-DD 填写。",
                )
            }
            return
        }
        val time = runCatching {
            LocalTime.parse(current.fortuneObservationTime)
        }.getOrNull()
        if (time == null) {
            mutableState.update {
                it.copy(
                    fortunePosition = null,
                    professionalFortunePosition = null,
                    fortunePositionError = "观察时间请按 HH:mm 填写。",
                )
            }
            return
        }
        val observedAt = com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime(
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            hour = time.hour,
            minute = time.minute,
            second = 0,
        )
        val position = runCatching {
            val professional = professionalFortuneResolver?.locate(
                result = result,
                observedAt = observedAt,
            )
            val basic = professional?.position
                ?: requireNotNull(fortunePositionResolver) {
                    "当前运行环境未配置岁运定位器。"
                }.locate(result, observedAt)
            basic to professional
        }
        mutableState.update {
            position.fold(
                onSuccess = { (resolved, professional) ->
                    it.copy(
                        fortunePosition = resolved,
                        professionalFortunePosition = professional,
                        fortunePositionError = null,
                    )
                },
                onFailure = { error ->
                    it.copy(
                        fortunePosition = null,
                        professionalFortunePosition = null,
                        fortunePositionError =
                            error.message ?: "岁运定位失败，请核对观察日期与时间。",
                    )
                },
            )
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

    fun openBirthTimeCandidate() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        mutableState.update {
            it.copy(
                destination = navigator.openBirthTimeCandidate(detail.id),
                candidateLabel = "",
                candidateForm = detail.toEditableForm(),
                mutationError = null,
            )
        }
    }

    fun updateCandidateLabel(value: String) {
        mutableState.update {
            it.copy(candidateLabel = value, mutationError = null)
        }
    }

    fun updateCandidateForm(transform: (CaseFormState) -> CaseFormState) {
        val detail = mutableState.value.detail ?: return
        mutableState.update {
            it.copy(
                candidateForm = transform(it.candidateForm).copy(
                    sex = detail.sexForFortuneDirection,
                ),
                mutationError = null,
            )
        }
    }

    fun saveBirthTimeCandidate() {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val label = mutableState.value.candidateLabel
        val form = mutableState.value.candidateForm.copy(
            sex = detail.sexForFortuneDirection,
        )
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = birthTimeCandidates.add(
                caseId = detail.id,
                expectedRevision = detail.revision,
                label = label,
                form = form,
            )
            finishBirthTimeCandidateMutation(
                result = result,
                caseId = detail.id,
                successMessage = "出生时间候选已添加；当前采用盘未改变。",
                navigateBackOnSuccess = true,
            )
        }
    }

    fun adoptBirthTimeCandidate(candidateId: String) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null || mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = birthTimeCandidates.adopt(
                caseId = detail.id,
                expectedRevision = detail.revision,
                candidateId = candidateId,
            )
            finishBirthTimeCandidateMutation(
                result = result,
                caseId = detail.id,
                successMessage = "已切换采用的出生时间与对应排盘。",
                navigateBackOnSuccess = false,
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
        if (mutableState.value.destination == AppDestination.FourPillarsLookup) {
            fourPillarsLookupJob?.cancel()
        }
        val leavingCommentaryCandidates =
            mutableState.value.destination is AppDestination.MasterCommentaryCandidates
        val leavingFeedbackThemeCandidates =
            mutableState.value.destination is AppDestination.FeedbackThemeCandidates
        mutableState.update {
            it.copy(
                destination = navigator.back(),
                mutationError = null,
                formError = null,
                commentaryCandidateSet = if (leavingCommentaryCandidates) {
                    null
                } else {
                    it.commentaryCandidateSet
                },
                commentaryCandidateFailure = if (leavingCommentaryCandidates) {
                    null
                } else {
                    it.commentaryCandidateFailure
                },
                commentaryCandidateAdoptionFailure = if (leavingCommentaryCandidates) {
                    null
                } else {
                    it.commentaryCandidateAdoptionFailure
                },
                commentaryCandidateSavingId = null,
                feedbackThemeCandidateSet = if (leavingFeedbackThemeCandidates) {
                    null
                } else {
                    it.feedbackThemeCandidateSet
                },
                feedbackThemeCandidateFailure = if (leavingFeedbackThemeCandidates) {
                    null
                } else {
                    it.feedbackThemeCandidateFailure
                },
                feedbackThemeAdoptionFailure = if (leavingFeedbackThemeCandidates) {
                    null
                } else {
                    it.feedbackThemeAdoptionFailure
                },
                feedbackThemeSavingId = null,
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
                objectiveSummary = null,
                objectiveSummaryLoading = false,
                objectiveSummaryFailure = null,
                objectiveSummaryCopied = false,
                commentaryCandidateSet = null,
                commentaryCandidateFailure = null,
                commentaryCandidateAdoptionFailure = null,
                commentaryCandidateSavingId = null,
                feedbackThemeCandidateSet = null,
                feedbackThemeCandidateFailure = null,
                feedbackThemeAdoptionFailure = null,
                feedbackThemeSavingId = null,
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

    private suspend fun finishBirthTimeCandidateMutation(
        result: CaseMutationResult,
        caseId: String,
        successMessage: String,
        navigateBackOnSuccess: Boolean,
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
                mutableState.update {
                    if (refreshed == null) {
                        it.copy(
                            mutationSaving = false,
                            mutationError = "更改已保存，但详情刷新失败。请返回列表后重新打开。",
                        )
                    } else {
                        it.copy(
                            destination = if (navigateBackOnSuccess) {
                                navigator.back()
                            } else {
                                navigator.current
                            },
                            detail = refreshed,
                            mutationSaving = false,
                            mutationError = null,
                            message = successMessage,
                        )
                    }
                }
                if (refreshed != null) refreshCases()
            }
            is CaseMutationResult.ValidationFailed -> mutableState.update {
                it.copy(mutationSaving = false, mutationError = result.message)
            }
            is CaseMutationResult.TimeZoneChoiceRequired -> mutableState.update {
                it.copy(
                    candidateForm = it.candidateForm.copy(
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
                    mutationError = "目标命例不存在，未保存任何更改。",
                )
            }
            is CaseMutationResult.RevisionConflict -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "保存冲突：命例已有较新修订（${result.actualRevision}），" +
                        "请返回详情重新操作。",
                )
            }
            is CaseMutationResult.CalculationFailed -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "候选排盘失败：${result.message} 当前输入仍保留。",
                )
            }
            is CaseMutationResult.StorageFailed -> mutableState.update {
                it.copy(mutationSaving = false, mutationError = result.message)
            }
            is CaseMutationResult.DuplicateCandidates -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "候选时间重复检查异常，未保存任何更改。",
                )
            }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    internal fun saveRestorableStateTo(target: SavedStateHandle) {
        target[SAVED_UI_STATE_KEY] = mutableState.value.toSavedStateBundle()
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
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
                birthTimeCandidates = BirthTimeCandidateUseCase(
                    baziEngine = container.baziEngine,
                    caseRepository = container.caseRepository,
                ),
                caseMetadata = CaseMetadataUseCase(container.caseRepository),
                textRecords = TextRecordUseCase(container.caseRepository),
                caseEvents = CaseEventUseCase(container.caseRepository),
                caseLifecycle = CaseLifecycleUseCase(container.caseRepository),
                fortunePositionResolver =
                    com.nanzhufeng.nanfengbazi.engine.tyme.TymeFortunePositionResolver(),
                professionalFortuneResolver =
                    com.nanzhufeng.nanfengbazi.engine.tyme.TymeProfessionalFortuneResolver(),
                fourPillarsLookup = container.fourPillarsLookup,
                caseImageRenderer = container.caseImageRenderer,
                singleCaseBundleService = container.singleCaseBundleService,
                caseBackupService = container.caseBackupService,
                backupAttachmentRoot = container.backupAttachmentRoot,
                backupWorkRoot = container.backupWorkRoot,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private companion object {
        const val SAVED_UI_STATE_KEY = "stage_two_ui_state"
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

private fun AppDestination.caseIdOrNull(): String? = when (this) {
    is AppDestination.CaseDetail -> caseId
    is AppDestination.CaseObjectiveSummary -> caseId
    is AppDestination.MasterCommentaryCandidates -> caseId
    is AppDestination.FeedbackThemeCandidates -> caseId
    is AppDestination.EditCase -> caseId
    is AppDestination.AddBirthTimeCandidate -> caseId
    is AppDestination.EditMetadata -> caseId
    is AppDestination.EditTextRecord -> caseId
    is AppDestination.EditEvent -> caseId
    AppDestination.CaseList,
    AppDestination.CaseComparison,
    AppDestination.FourPillarsLookup,
    AppDestination.RecordHub,
    AppDestination.Settings,
    AppDestination.CreateCase,
    AppDestination.ScreenshotImportReview,
    -> null
}

private fun StageTwoUiState.toSavedStateBundle(): Bundle = Bundle().apply {
    putBundle("destination", destination.toSavedStateBundle())
    putString("query", query)
    putString("selectedGroupId", selectedGroupId)
    putString("selectedTagId", selectedTagId)
    putString("comparisonLeftCaseId", comparisonLeftCaseId)
    putString("comparisonRightCaseId", comparisonRightCaseId)
    putBundle(
        "fourPillarsLookupForm",
        fourPillarsLookupForm.toSavedStateBundle(),
    )
    putBoolean("fourPillarsLookupHasSearched", fourPillarsLookupHasSearched)
    putString("sortOrder", sortOrder.name)
    putString("visibility", visibility.name)
    putString("detailSection", detailSection.name)
    commentaryCandidateSet?.let {
        putBundle("commentaryCandidateSet", it.toSavedStateBundle())
    }
    commentaryCandidateFailure?.let {
        putString("commentaryCandidateFailureCode", it.code.name)
        putString("commentaryCandidateFailureMessage", it.message)
    }
    commentaryCandidateAdoptionFailure?.let {
        putString("commentaryCandidateAdoptionFailureCode", it.code.name)
        putString("commentaryCandidateAdoptionFailureMessage", it.message)
    }
    feedbackThemeCandidateSet?.let {
        putBundle("feedbackThemeCandidateSet", it.toSavedStateBundle())
    }
    feedbackThemeCandidateFailure?.let {
        putString("feedbackThemeCandidateFailureCode", it.code.name)
        putString("feedbackThemeCandidateFailureMessage", it.message)
    }
    feedbackThemeAdoptionFailure?.let {
        putString("feedbackThemeAdoptionFailureCode", it.code.name)
        putString("feedbackThemeAdoptionFailureMessage", it.message)
    }
    putString("fortuneObservationDate", fortuneObservationDate)
    putString("fortuneObservationTime", fortuneObservationTime)
    putBundle("form", form.toSavedStateBundle())
    putBundle("editForm", editForm.toSavedStateBundle())
    putString("candidateLabel", candidateLabel)
    putBundle("candidateForm", candidateForm.toSavedStateBundle())
    putBundle("metadataDraft", metadataDraft.toSavedStateBundle())
    putBundle("recordDraft", recordDraft.toSavedStateBundle())
    putBundle("eventDraft", eventDraft.toSavedStateBundle())
}

private fun Bundle.toStageTwoUiState(): StageTwoUiState {
    val destination = getBundle("destination")?.toAppDestination()
        ?: AppDestination.CaseList
    return StageTwoUiState(
        destination = destination,
        query = getString("query").orEmpty(),
        selectedGroupId = getString("selectedGroupId"),
        selectedTagId = getString("selectedTagId"),
        comparisonLeftCaseId = getString("comparisonLeftCaseId"),
        comparisonRightCaseId = getString("comparisonRightCaseId"),
        comparisonLoading = destination == AppDestination.CaseComparison,
        fourPillarsLookupForm = getBundle("fourPillarsLookupForm")
            ?.toFourPillarsLookupFormState()
            ?: FourPillarsLookupFormState(),
        fourPillarsLookupHasSearched = getBoolean("fourPillarsLookupHasSearched"),
        fourPillarsLookupLoading =
            destination == AppDestination.FourPillarsLookup &&
                getBoolean("fourPillarsLookupHasSearched"),
        sortOrder = enumValueOrDefault(
            getString("sortOrder"),
            CaseSortOrder.UPDATED_DESC,
        ),
        visibility = enumValueOrDefault(
            getString("visibility"),
            CaseVisibility.ACTIVE,
        ),
        form = getBundle("form")?.toCaseFormState() ?: CaseFormState(),
        detailSection = enumValueOrDefault(
            getString("detailSection"),
            CaseDetailSection.BASIC_INFO,
        ),
        fortuneObservationDate = getString("fortuneObservationDate").orEmpty(),
        fortuneObservationTime = getString("fortuneObservationTime") ?: "12:00",
        detailLoading = destination.caseIdOrNull() != null,
        objectiveSummaryLoading = destination is AppDestination.CaseObjectiveSummary,
        commentaryCandidateSet =
            getBundle("commentaryCandidateSet")?.toMasterCommentaryCandidateSet(),
        commentaryCandidateFailure =
            enumValueOrNull<MasterCommentaryCandidateErrorCode>(
                getString("commentaryCandidateFailureCode"),
            )?.let { code ->
                MasterCommentaryCandidateFailure(
                    code,
                    getString("commentaryCandidateFailureMessage").orEmpty(),
                )
            },
        commentaryCandidateAdoptionFailure =
            enumValueOrNull<MasterCommentaryCandidateAdoptionErrorCode>(
                getString("commentaryCandidateAdoptionFailureCode"),
            )?.let { code ->
                MasterCommentaryCandidateAdoptionFailure(
                    code,
                    getString("commentaryCandidateAdoptionFailureMessage").orEmpty(),
                )
            },
        feedbackThemeCandidateSet =
            getBundle("feedbackThemeCandidateSet")?.toFeedbackThemeCandidateSet(),
        feedbackThemeCandidateFailure =
            enumValueOrNull<FeedbackThemeCandidateErrorCode>(
                getString("feedbackThemeCandidateFailureCode"),
            )?.let { code ->
                FeedbackThemeCandidateFailure(
                    code,
                    getString("feedbackThemeCandidateFailureMessage").orEmpty(),
                )
            },
        feedbackThemeAdoptionFailure =
            enumValueOrNull<FeedbackThemeAdoptionErrorCode>(
                getString("feedbackThemeAdoptionFailureCode"),
            )?.let { code ->
                FeedbackThemeAdoptionFailure(
                    code,
                    getString("feedbackThemeAdoptionFailureMessage").orEmpty(),
                )
            },
        editForm = getBundle("editForm")?.toCaseFormState() ?: CaseFormState(),
        candidateLabel = getString("candidateLabel").orEmpty(),
        candidateForm = getBundle("candidateForm")?.toCaseFormState() ?: CaseFormState(),
        metadataDraft =
            getBundle("metadataDraft")?.toCaseMetadataDraft() ?: CaseMetadataDraft(),
        recordDraft = getBundle("recordDraft")?.toTextRecordDraft() ?: TextRecordDraft(),
        eventDraft = getBundle("eventDraft")?.toEventDraft() ?: EventDraft(),
    )
}

private fun AppDestination.toSavedStateBundle(): Bundle = Bundle().apply {
    when (this@toSavedStateBundle) {
        AppDestination.CaseList -> putString("type", "case_list")
        AppDestination.CaseComparison -> putString("type", "case_comparison")
        AppDestination.FourPillarsLookup -> putString("type", "four_pillars_lookup")
        AppDestination.RecordHub -> putString("type", "record_hub")
        AppDestination.Settings -> putString("type", "settings")
        AppDestination.CreateCase -> putString("type", "create_case")
        AppDestination.ScreenshotImportReview -> putString("type", "screenshot_review")
        is AppDestination.CaseDetail -> {
            putString("type", "case_detail")
            putString("caseId", caseId)
        }
        is AppDestination.CaseObjectiveSummary -> {
            putString("type", "case_objective_summary")
            putString("caseId", caseId)
        }
        is AppDestination.MasterCommentaryCandidates -> {
            putString("type", "master_commentary_candidates")
            putString("caseId", caseId)
            putString("recordId", recordId)
        }
        is AppDestination.FeedbackThemeCandidates -> {
            putString("type", "feedback_theme_candidates")
            putString("caseId", caseId)
            putString("recordId", recordId)
        }
        is AppDestination.EditCase -> {
            putString("type", "edit_case")
            putString("caseId", caseId)
        }
        is AppDestination.AddBirthTimeCandidate -> {
            putString("type", "birth_time_candidate")
            putString("caseId", caseId)
        }
        is AppDestination.EditMetadata -> {
            putString("type", "edit_metadata")
            putString("caseId", caseId)
        }
        is AppDestination.EditTextRecord -> {
            putString("type", "edit_record")
            putString("caseId", caseId)
            putString("recordId", recordId)
        }
        is AppDestination.EditEvent -> {
            putString("type", "edit_event")
            putString("caseId", caseId)
            putString("eventId", eventId)
        }
    }
}

private fun Bundle.toAppDestination(): AppDestination {
    val caseId = getString("caseId")
    return when (getString("type")) {
        "record_hub" -> AppDestination.RecordHub
        "case_comparison" -> AppDestination.CaseComparison
        "four_pillars_lookup" -> AppDestination.FourPillarsLookup
        "settings" -> AppDestination.Settings
        "create_case" -> AppDestination.CreateCase
        "screenshot_review" -> AppDestination.ScreenshotImportReview
        "case_detail" -> caseId?.let(AppDestination::CaseDetail)
        "case_objective_summary" -> caseId?.let(AppDestination::CaseObjectiveSummary)
        "master_commentary_candidates" -> caseId?.let {
            getString("recordId")?.let { recordId ->
                AppDestination.MasterCommentaryCandidates(it, recordId)
            }
        }
        "feedback_theme_candidates" -> caseId?.let {
            getString("recordId")?.let { recordId ->
                AppDestination.FeedbackThemeCandidates(it, recordId)
            }
        }
        "edit_case" -> caseId?.let(AppDestination::EditCase)
        "birth_time_candidate" -> caseId?.let(AppDestination::AddBirthTimeCandidate)
        "edit_metadata" -> caseId?.let(AppDestination::EditMetadata)
        "edit_record" -> caseId?.let {
            AppDestination.EditTextRecord(it, getString("recordId"))
        }
        "edit_event" -> caseId?.let {
            AppDestination.EditEvent(it, getString("eventId"))
        }
        else -> AppDestination.CaseList
    } ?: AppDestination.CaseList
}

private fun MasterCommentaryCandidateSet.toSavedStateBundle(): Bundle = Bundle().apply {
    putInt("ruleVersion", ruleVersion)
    putString("sourceRecordId", sourceRecordId)
    putInt("sourceRevision", sourceRevision)
    putParcelableArrayList(
        "candidates",
        ArrayList(candidates.map { it.toSavedStateBundle() }),
    )
}

@Suppress("DEPRECATION")
private fun Bundle.toMasterCommentaryCandidateSet(): MasterCommentaryCandidateSet? =
    runCatching {
        val sourceRecordId = requireNotNull(getString("sourceRecordId"))
        val candidates = getParcelableArrayList<Bundle>("candidates")
            .orEmpty()
            .mapNotNull { it.toMasterCommentaryCandidate() }
        MasterCommentaryCandidateSet(
            ruleVersion = getInt("ruleVersion"),
            sourceRecordId = sourceRecordId,
            sourceRevision = getInt("sourceRevision"),
            candidates = candidates,
        )
    }.getOrNull()

private fun MasterCommentaryCandidate.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("id", id)
    putString("sourceRecordId", sourceRecordId)
    putInt("sourceRevision", sourceRevision)
    putInt("startInclusive", sourceRange.startInclusive)
    putInt("endExclusive", sourceRange.endExclusive)
    putString("sourceExcerpt", sourceExcerpt)
    putString("proposedContent", proposedContent)
    putString("proposedCategory", proposedCategory.name)
    putString("status", status.name)
    putParcelableArrayList(
        "ruleEvidence",
        ArrayList(ruleEvidence.map { it.toSavedStateBundle() }),
    )
}

@Suppress("DEPRECATION")
private fun Bundle.toMasterCommentaryCandidate(): MasterCommentaryCandidate? =
    runCatching {
        MasterCommentaryCandidate(
            id = requireNotNull(getString("id")),
            sourceRecordId = requireNotNull(getString("sourceRecordId")),
            sourceRevision = getInt("sourceRevision"),
            sourceRange = CommentaryTextRange(
                startInclusive = getInt("startInclusive"),
                endExclusive = getInt("endExclusive"),
            ),
            sourceExcerpt = requireNotNull(getString("sourceExcerpt")),
            proposedContent = getString("proposedContent").orEmpty(),
            proposedCategory = enumValueOrDefault(
                getString("proposedCategory"),
                AnalysisCategory.GENERAL,
            ),
            ruleEvidence = getParcelableArrayList<Bundle>("ruleEvidence")
                .orEmpty()
                .mapNotNull { it.toCommentaryCandidateRuleEvidence() },
            status = enumValueOrDefault(
                getString("status"),
                MasterCommentaryCandidateStatus.PENDING,
            ),
        )
    }.getOrNull()

private fun CommentaryCandidateRuleEvidence.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("ruleId", ruleId)
    putString("explanation", explanation)
    putStringArrayList("matchedTerms", ArrayList(matchedTerms))
}

private fun Bundle.toCommentaryCandidateRuleEvidence(): CommentaryCandidateRuleEvidence? =
    runCatching {
        CommentaryCandidateRuleEvidence(
            ruleId = requireNotNull(getString("ruleId")),
            explanation = requireNotNull(getString("explanation")),
            matchedTerms = getStringArrayList("matchedTerms").orEmpty(),
        )
    }.getOrNull()

private fun MasterCommentaryCandidateSet.updateCandidate(
    candidateId: String,
    transform: (MasterCommentaryCandidate) -> MasterCommentaryCandidate,
): MasterCommentaryCandidateSet = copy(
    candidates = candidates.map {
        if (it.id == candidateId) transform(it) else it
    },
)

private fun MasterCommentaryCandidateSet.mergeDecisionsFrom(
    saved: MasterCommentaryCandidateSet?,
): MasterCommentaryCandidateSet {
    if (
        saved == null ||
        saved.ruleVersion != ruleVersion ||
        saved.sourceRecordId != sourceRecordId ||
        saved.sourceRevision != sourceRevision
    ) {
        return this
    }
    val savedById = saved.candidates.associateBy { it.id }
    return copy(
        candidates = candidates.map { fresh ->
            val previous = savedById[fresh.id] ?: return@map fresh
            fresh.copy(
                proposedContent = previous.proposedContent,
                proposedCategory = previous.proposedCategory,
                status = previous.status,
            )
        },
    )
}

private fun FeedbackThemeCandidateSet.toSavedStateBundle(): Bundle = Bundle().apply {
    putInt("ruleVersion", ruleVersion)
    putString("sourceRecordId", sourceRecordId)
    putInt("sourceRevision", sourceRevision)
    putParcelableArrayList(
        "candidates",
        ArrayList(candidates.map { it.toSavedStateBundle() }),
    )
}

@Suppress("DEPRECATION")
private fun Bundle.toFeedbackThemeCandidateSet(): FeedbackThemeCandidateSet? = runCatching {
    FeedbackThemeCandidateSet(
        ruleVersion = getInt("ruleVersion"),
        sourceRecordId = requireNotNull(getString("sourceRecordId")),
        sourceRevision = getInt("sourceRevision"),
        candidates = getParcelableArrayList<Bundle>("candidates")
            .orEmpty()
            .mapNotNull { it.toFeedbackThemeCandidate() },
    )
}.getOrNull()

private fun FeedbackThemeCandidate.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("id", id)
    putString("sourceRecordId", sourceRecordId)
    putInt("sourceRevision", sourceRevision)
    putString("canonicalTagName", canonicalTagName)
    putString("proposedTagName", proposedTagName)
    putString("suggestedEventCategory", suggestedEventCategory.name)
    putString("ruleId", ruleId)
    putString("ruleExplanation", ruleExplanation)
    putString("status", status.name)
    putParcelableArrayList(
        "sourceEvidence",
        ArrayList(sourceEvidence.map { it.toSavedStateBundle() }),
    )
}

@Suppress("DEPRECATION")
private fun Bundle.toFeedbackThemeCandidate(): FeedbackThemeCandidate? = runCatching {
    FeedbackThemeCandidate(
        id = requireNotNull(getString("id")),
        sourceRecordId = requireNotNull(getString("sourceRecordId")),
        sourceRevision = getInt("sourceRevision"),
        canonicalTagName = requireNotNull(getString("canonicalTagName")),
        proposedTagName = getString("proposedTagName").orEmpty(),
        suggestedEventCategory = enumValueOrDefault(
            getString("suggestedEventCategory"),
            CaseEventCategory.GENERAL,
        ),
        sourceEvidence = getParcelableArrayList<Bundle>("sourceEvidence")
            .orEmpty()
            .mapNotNull { it.toFeedbackThemeSourceEvidence() },
        ruleId = requireNotNull(getString("ruleId")),
        ruleExplanation = requireNotNull(getString("ruleExplanation")),
        status = enumValueOrDefault(
            getString("status"),
            FeedbackThemeCandidateStatus.PENDING,
        ),
    )
}.getOrNull()

private fun FeedbackThemeSourceEvidence.toSavedStateBundle(): Bundle = Bundle().apply {
    putInt("startInclusive", range.startInclusive)
    putInt("endExclusive", range.endExclusive)
    putString("excerpt", excerpt)
    putStringArrayList("matchedTerms", ArrayList(matchedTerms))
}

private fun Bundle.toFeedbackThemeSourceEvidence(): FeedbackThemeSourceEvidence? = runCatching {
    FeedbackThemeSourceEvidence(
        range = FeedbackThemeTextRange(
            startInclusive = getInt("startInclusive"),
            endExclusive = getInt("endExclusive"),
        ),
        excerpt = requireNotNull(getString("excerpt")),
        matchedTerms = getStringArrayList("matchedTerms").orEmpty(),
    )
}.getOrNull()

private fun FeedbackThemeCandidateSet.updateCandidate(
    candidateId: String,
    transform: (FeedbackThemeCandidate) -> FeedbackThemeCandidate,
): FeedbackThemeCandidateSet = copy(
    candidates = candidates.map { candidate ->
        if (candidate.id == candidateId) transform(candidate) else candidate
    },
)

private fun FeedbackThemeCandidateSet.mergeDecisionsFrom(
    saved: FeedbackThemeCandidateSet?,
): FeedbackThemeCandidateSet {
    if (
        saved == null ||
        saved.ruleVersion != ruleVersion ||
        saved.sourceRecordId != sourceRecordId ||
        saved.sourceRevision != sourceRevision
    ) {
        return this
    }
    val savedById = saved.candidates.associateBy { it.id }
    return copy(
        candidates = candidates.map { fresh ->
            val previous = savedById[fresh.id] ?: return@map fresh
            fresh.copy(
                proposedTagName = previous.proposedTagName,
                status = previous.status,
            )
        },
    )
}

private fun BaziCase.sourceRecordRevision(recordId: String): Int =
    textRecordRevisions
        .filter { it.recordId == recordId }
        .maxOfOrNull { it.version }
        ?: 0

private fun CaseFormState.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("alias", alias)
    putString("name", name)
    putString("sex", sex?.name)
    putString("year", year)
    putString("month", month)
    putString("day", day)
    putString("hour", hour)
    putString("minute", minute)
    putString("second", second)
    putString("calendarSystem", calendarSystem.name)
    putBoolean("isLeapMonth", isLeapMonth)
    putString("locationName", locationName)
    putString("longitude", longitude)
    putString("latitude", latitude)
    putString("timeZoneId", timeZoneId)
    resolvedUtcOffsetSeconds?.let { putInt("resolvedUtcOffsetSeconds", it) }
    putIntArray("availableUtcOffsetSeconds", availableUtcOffsetSeconds.toIntArray())
    putBoolean("useTrueSolarTime", useTrueSolarTime)
    putString("ratHourRule", ratHourRule.name)
    putString("timePrecision", timePrecision.name)
    putString("timeSourceType", timeSourceType.name)
    putString("sourceNote", sourceNote)
}

private fun Bundle.toCaseFormState(): CaseFormState = CaseFormState(
    alias = getString("alias").orEmpty(),
    name = getString("name").orEmpty(),
    sex = enumValueOrNull<SexForFortuneDirection>(getString("sex")),
    year = getString("year").orEmpty(),
    month = getString("month").orEmpty(),
    day = getString("day").orEmpty(),
    hour = getString("hour").orEmpty(),
    minute = getString("minute").orEmpty(),
    second = getString("second") ?: "0",
    calendarSystem = enumValueOrDefault(
        getString("calendarSystem"),
        CalendarSystem.SOLAR,
    ),
    isLeapMonth = getBoolean("isLeapMonth"),
    locationName = getString("locationName").orEmpty(),
    longitude = getString("longitude").orEmpty(),
    latitude = getString("latitude").orEmpty(),
    timeZoneId = getString("timeZoneId") ?: "Asia/Shanghai",
    resolvedUtcOffsetSeconds = if (containsKey("resolvedUtcOffsetSeconds")) {
        getInt("resolvedUtcOffsetSeconds")
    } else {
        null
    },
    availableUtcOffsetSeconds =
        getIntArray("availableUtcOffsetSeconds")?.toList().orEmpty(),
    useTrueSolarTime = getBoolean("useTrueSolarTime"),
    ratHourRule = enumValueOrDefault(
        getString("ratHourRule"),
        RatHourRule.TYME_DEFAULT,
    ),
    timePrecision = enumValueOrDefault(
        getString("timePrecision"),
        TimePrecision.EXACT_TO_MINUTE,
    ),
    timeSourceType = enumValueOrDefault(
        getString("timeSourceType"),
        TimeSourceType.UNKNOWN,
    ),
    sourceNote = getString("sourceNote").orEmpty(),
)

private fun CaseMetadataDraft.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("groupNames", groupNames)
    putString("tagNames", tagNames)
    putBoolean("isFavorite", isFavorite)
    putBoolean("isPinned", isPinned)
}

private fun Bundle.toCaseMetadataDraft(): CaseMetadataDraft = CaseMetadataDraft(
    groupNames = getString("groupNames").orEmpty(),
    tagNames = getString("tagNames").orEmpty(),
    isFavorite = getBoolean("isFavorite"),
    isPinned = getBoolean("isPinned"),
)

private fun TextRecordDraft.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("type", type.name)
    putString("content", content)
    putString("analysisCategory", analysisCategory.name)
}

private fun Bundle.toTextRecordDraft(): TextRecordDraft = TextRecordDraft(
    type = enumValueOrDefault(getString("type"), CaseTextRecordType.NOTE),
    content = getString("content").orEmpty(),
    analysisCategory = enumValueOrDefault(
        getString("analysisCategory"),
        AnalysisCategory.GENERAL,
    ),
)

private fun EventDraft.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("year", year)
    putString("month", month)
    putString("day", day)
    putString("status", status)
    putString("rawText", rawText)
    putString("title", title)
    putString("category", category.name)
}

private fun Bundle.toEventDraft(): EventDraft = EventDraft(
    year = getString("year").orEmpty(),
    month = getString("month").orEmpty(),
    day = getString("day").orEmpty(),
    status = getString("status").orEmpty(),
    rawText = getString("rawText").orEmpty(),
    title = getString("title").orEmpty(),
    category = enumValueOrDefault(
        getString("category"),
        CaseEventCategory.GENERAL,
    ),
)

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
    value?.let { candidate ->
        enumValues<T>().firstOrNull { it.name == candidate }
    }

private fun String.toSafeImageFileName(): String {
    val safeStem = trim()
        .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
        .trim('.', ' ')
        .take(48)
        .ifBlank { "命例" }
    return "${safeStem}_南枫八字命盘.png"
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String?,
    default: T,
): T = enumValueOrNull<T>(value) ?: default

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
        ratHourRule = calculationSnapshots
            .asReversed()
            .firstOrNull { it.adopted }
            ?.result
            ?.profile
            ?.ratHourRule
            ?: RatHourRule.TYME_DEFAULT,
        timePrecision = birthInput.timePrecision,
        timeSourceType = birthInput.timeSourceType,
        sourceNote = birthInput.sourceNote.orEmpty(),
    )
}
