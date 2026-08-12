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
import com.nanzhufeng.nanfengbazi.domain.AlmanacContract
import com.nanzhufeng.nanfengbazi.domain.BaziTimeZoneDefaults
import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptContract
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptRequest
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptResult
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisTopic
import com.nanzhufeng.nanfengbazi.domain.AlmanacDate
import com.nanzhufeng.nanfengbazi.domain.AlmanacDoubleHours
import com.nanzhufeng.nanfengbazi.domain.AlmanacError
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthQuery
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthView
import com.nanzhufeng.nanfengbazi.domain.AlmanacReader
import com.nanzhufeng.nanfengbazi.domain.AlmanacResult
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseAdvancedFilter
import com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportContract
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportInput
import com.nanzhufeng.nanfengbazi.domain.CaseImageFactsResult
import com.nanzhufeng.nanfengbazi.domain.CaseImageCaptureFacts
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
import com.nanzhufeng.nanfengbazi.domain.searchCases
import com.nanzhufeng.nanfengbazi.domain.FourPillarsSearchFilter
import com.nanzhufeng.nanfengbazi.domain.PillarCharacterFilter
import com.nanzhufeng.nanfengbazi.domain.CommentaryCandidateRuleEvidence
import com.nanzhufeng.nanfengbazi.domain.CommentaryTextRange
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisBridge
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisBridgeContract
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisBridgeErrorCode
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisBridgeFailure
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisExportRequest
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisExportResult
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisFieldGroup
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisImportRequest
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisImportResult
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisPayload
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisRedactionPolicy
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
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneLayer
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneSelection
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneResolver
import com.nanzhufeng.nanfengbazi.domain.RenderedCaseImage
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthTimeCandidate
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.nanzhufeng.nanfengbazi.imageparser.DeterministicMasterCommentaryCandidateExtractor
import com.nanzhufeng.nanfengbazi.imageparser.DeterministicFeedbackThemeCandidateExtractor
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.file.Path
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed interface AppDestination {
    data object CaseList : AppDestination
    data object CaseComparison : AppDestination
    data object FourPillarsLookup : AppDestination
    data object Almanac : AppDestination
    data object RecordHub : AppDestination
    data object Settings : AppDestination
    data object CreateCase : AppDestination
    data object ScreenshotImportReview : AppDestination
    data class CaseDetail(val caseId: String) : AppDestination
    data class CaseObjectiveSummary(val caseId: String) : AppDestination
    data class ExternalAnalysisBridge(val caseId: String) : AppDestination
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

data class ExternalAnalysisDraftState(
    val selectedGroups: Set<ExternalAnalysisFieldGroup> =
        ExternalAnalysisFieldGroup.entries.toSet(),
    val redactionEnabled: Boolean = true,
    val providerName: String = "",
    val modelName: String = "",
    val resultText: String = "",
    val exportConfirmed: Boolean = false,
    val importConfirmed: Boolean = false,
)

data class AiCommentaryUiState(
    val dialogVisible: Boolean = false,
    val serviceMenuVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val historyVisible: Boolean = false,
    val callRecords: List<AiCommentaryCallRecord> = emptyList(),
    val configs: Map<AiCommentaryProviderId, AiCommentaryProviderConfig> = emptyMap(),
    val apiKeys: Map<AiCommentaryProviderId, String> = emptyMap(),
    val selectedProvider: AiCommentaryProviderId = AiCommentaryProviderId.OPEN_ROUTER,
    val privacyConfirmed: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val generatedDraft: AiCommentaryDraft? = null,
    val editedContent: String = "",
    val error: String? = null,
)

class StageTwoNavigator {
    private val stack = mutableListOf<AppDestination>(AppDestination.CreateCase)
    val current: AppDestination
        get() = stack.last()

    fun openCreate(): AppDestination {
        return openRoot(AppDestination.CreateCase)
    }

    fun openRecordHub(): AppDestination = openRoot(AppDestination.CaseList)

    fun openSettings(): AppDestination = openRoot(AppDestination.Settings)

    fun openCaseComparison(): AppDestination {
        return push(AppDestination.CaseComparison)
    }

    fun openFourPillarsLookup(): AppDestination {
        return push(AppDestination.FourPillarsLookup)
    }

    fun openAlmanac(): AppDestination = push(AppDestination.Almanac)

    fun openScreenshotImportReview(): AppDestination {
        return push(AppDestination.ScreenshotImportReview)
    }

    fun openDetail(caseId: String): AppDestination {
        return push(AppDestination.CaseDetail(caseId))
    }

    fun replaceCurrentDetail(caseId: String): AppDestination {
        val destination = AppDestination.CaseDetail(caseId)
        if (stack.lastOrNull() is AppDestination.CaseDetail) {
            stack[stack.lastIndex] = destination
        } else {
            stack += destination
        }
        return current
    }

    fun openObjectiveSummary(caseId: String): AppDestination {
        return push(AppDestination.CaseObjectiveSummary(caseId))
    }

    fun openExternalAnalysisBridge(caseId: String): AppDestination {
        return push(AppDestination.ExternalAnalysisBridge(caseId))
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
            AppDestination.Settings,
            AppDestination.CreateCase,
            AppDestination.ScreenshotImportReview,
            -> stack += destination

            AppDestination.RecordHub -> stack += AppDestination.CaseList

            AppDestination.CaseComparison -> {
                stack += AppDestination.CaseList
                stack += destination
            }

            AppDestination.FourPillarsLookup -> {
                stack += AppDestination.CreateCase
                stack += destination
            }

            AppDestination.Almanac -> {
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

            is AppDestination.ExternalAnalysisBridge -> {
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

enum class SingleCaseExportDocumentKind {
    JSON,
    ENCRYPTED_JSON,
    BUNDLE,
    ENCRYPTED_BUNDLE,
}

data class SingleCaseExportDocumentRequest(
    val fileName: String,
    val kind: SingleCaseExportDocumentKind,
)

data class StageTwoUiState(
    val destination: AppDestination = AppDestination.CreateCase,
    val query: String = "",
    val selectedGroupId: String? = null,
    val selectedTagId: String? = null,
    val sortOrder: CaseSortOrder = CaseSortOrder.NAME_ASC,
    val visibility: CaseVisibility = CaseVisibility.ACTIVE,
    val libraryType: CaseLibraryType = CaseLibraryType.USER,
    val advancedFilter: CaseAdvancedFilter = CaseAdvancedFilter(),
    val availableGroups: List<CaseGroup> = emptyList(),
    val availableTags: List<CaseTag> = emptyList(),
    val availableBirthRegions: List<String> = emptyList(),
    val availableSeasonalWuxingStates: List<String> = emptyList(),
    val availableShenSha: List<String> = emptyList(),
    val libraryCaseCounts: Map<CaseLibraryType, Int> = emptyMap(),
    val trashedCaseCount: Int = 0,
    val groupCaseCounts: Map<String, Int> = emptyMap(),
    val cases: List<CaseSummary> = emptyList(),
    val batchCases: List<CaseSummary> = emptyList(),
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
    val almanacYear: Int = LocalDate.now().year,
    val almanacMonth: Int = LocalDate.now().monthValue,
    val almanacSelectedDay: Int = LocalDate.now().dayOfMonth,
    val almanacSelectedDoubleHourIndex: Int = 0,
    val almanacView: AlmanacMonthView? = null,
    val almanacLoading: Boolean = false,
    val almanacRefreshingSelection: Boolean = false,
    val almanacError: String? = null,
    val birthPickerTodaySnapshot: BirthPickerTodaySnapshot? = null,
    val defaultRatHourRule: RatHourRule = RatHourRule.TYME_DEFAULT,
    val listLoading: Boolean = false,
    val listError: String? = null,
    val form: CaseFormState = CaseFormState(),
    val formError: String? = null,
    val saving: Boolean = false,
    val previewing: Boolean = false,
    val instantCalculation: CalculationResult? = null,
    val duplicateCandidates: List<DuplicateCaseCandidate> = emptyList(),
    val detail: BaziCase? = null,
    val detailIsTransient: Boolean = false,
    val detailSavePending: Boolean = false,
    /**
     * Saving starts after the chart is already visible.  A later duplicate or storage result
     * belongs to that same chart and must never force the user back to the entry page.
     */
    val detailSaveError: String? = null,
    val detailSaveDialogVisible: Boolean = false,
    val detailSection: CaseDetailSection = CaseDetailSection.BASIC_INFO,
    val fortuneObservationDate: String = "",
    val fortuneObservationTime: String = "12:00",
    val fortunePosition: FortunePosition? = null,
    val professionalFortunePosition: ProfessionalFortunePosition? = null,
    val fortunePositionError: String? = null,
    val fortunePositionLoading: Boolean = false,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val editForm: CaseFormState = CaseFormState(),
    val candidateLabel: String = "",
    val candidateForm: CaseFormState = CaseFormState(),
    val metadataDraft: CaseMetadataDraft = CaseMetadataDraft(),
    val recordDraft: TextRecordDraft = TextRecordDraft(),
    val eventDraft: EventDraft = EventDraft(),
    val caseNotesCaseId: String? = null,
    val caseNotesRevision: Long? = null,
    val caseNotesDraft: CaseNotesDraft = CaseNotesDraft(),
    val caseNotesSavedDraft: CaseNotesDraft = CaseNotesDraft(),
    val caseNotesHydrating: Boolean = false,
    val caseNotesSaving: Boolean = false,
    val caseNotesSaveError: String? = null,
    val caseNotesLastSavedAt: java.time.Instant? = null,
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
    val externalAnalysisDraft: ExternalAnalysisDraftState =
        ExternalAnalysisDraftState(),
    val externalAnalysisPayload: ExternalAnalysisPayload? = null,
    val externalAnalysisFailure: ExternalAnalysisBridgeFailure? = null,
    val externalAnalysisCopied: Boolean = false,
    val externalAnalysisSaving: Boolean = false,
    val aiCommentary: AiCommentaryUiState = AiCommentaryUiState(),
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
    val wenzhenImportPreview: WenzhenImportPreview? = null,
    val wenzhenImportBusy: Boolean = false,
    val wenzhenImportProgress: WenzhenImportProgress? = null,
    val wenzhenImportResult: WenzhenImportResult? = null,
    val wenzhenImportError: String? = null,
    val message: String? = null,
)

private data class RetainedCaseDetailSnapshot(
    val case: BaziCase,
    val notes: CaseNotesDraft,
    val professionalObservation: ProfessionalObservationSeed? = null,
)

private data class PreparedCaseSaveAttempt(
    val pendingCase: BaziCase,
    val form: CaseFormState,
    val preview: PreviewCaseResult.Calculated,
    val allowDuplicate: Boolean,
)

private sealed interface PendingSingleCaseBundleCommit {
    data class Import(
        val decision: SingleCaseImportDecision,
    ) : PendingSingleCaseBundleCommit

    data class Merge(
        val plan: SingleCaseMergePlan,
    ) : PendingSingleCaseBundleCommit
}

private data class CaseListProjection(
    val catalog: List<CaseSummary>,
    val groupsByLibrary: Map<CaseLibraryType, List<CaseGroup>>,
    val cases: List<CaseSummary>,
    val batchCases: List<CaseSummary>,
    val recentCases: List<CaseSummary>,
    val allCasesForControls: List<CaseSummary>,
    val libraryCaseCounts: Map<CaseLibraryType, Int>,
    val trashedCaseCount: Int,
    val groupCaseCounts: Map<String, Int>,
)

private data class FortunePositionCacheKey(
    val caseId: String,
    val calculationSnapshotId: String,
    val observedAt: CivilDateTime,
    val selectionLayer: ProfessionalFortuneLayer? = null,
)

private data class FortunePositionCacheValue(
    val position: FortunePosition,
    val professionalPosition: ProfessionalFortunePosition?,
)

private data class ProfessionalObservationSeed(
    val calculationSnapshotId: String?,
    val observedAt: CivilDateTime,
    val cached: FortunePositionCacheValue?,
)

class StageTwoViewModel(
    private val caseRepository: CaseRepository,
    private val caseCatalogStore: CaseCatalogStore = RepositoryCaseCatalogStore(caseRepository),
    private val createCase: CreateCaseUseCase,
    private val editCase: EditCaseUseCase,
    private val birthTimeCandidates: BirthTimeCandidateUseCase,
    private val caseMetadata: CaseMetadataUseCase = CaseMetadataUseCase(caseRepository),
    private val textRecords: TextRecordUseCase,
    private val caseEvents: CaseEventUseCase,
    private val caseNotesEditor: CaseNotesEditorUseCase = CaseNotesEditorUseCase(caseRepository),
    private val caseLifecycle: CaseLifecycleUseCase = CaseLifecycleUseCase(caseRepository),
    private val navigator: StageTwoNavigator = StageTwoNavigator(),
    private val clock: Clock = Clock.systemUTC(),
    private val observationClock: Clock = Clock.systemDefaultZone(),
    private val fortunePositionResolver: FortunePositionResolver? = null,
    private val professionalFortuneResolver: ProfessionalFortuneResolver? = null,
    private val fourPillarsLookup: FourPillarsLookup? = null,
    private val almanacReader: AlmanacReader? = null,
    private val objectiveSummaryGenerator: CaseObjectiveSummaryGenerator =
        CaseObjectiveSummaryContract,
    private val externalAnalysisBridge: ExternalAnalysisBridge =
        ExternalAnalysisBridgeContract,
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
    private val calculationPreferenceStore: CalculationPreferenceStore =
        InMemoryCalculationPreferenceStore(),
    private val aiCommentarySettings: AiCommentarySettingsStore? = null,
    private val aiCommentaryGenerator: AiCommentaryGenerator? = null,
    private val aiCommentaryCallLog: AiCommentaryCallLogStore? = null,
    private val baziEngine: BaziEngine = BaziEngine { _, _ ->
        error("当前 ViewModel 未配置批量排盘引擎。")
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @OptIn(ExperimentalCoroutinesApi::class)
    private val fortuneCalculationDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(1),
    private val searchDebounceMillis: Long = 180L,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    restoredUiStateOverride: StageTwoUiState? = null,
) : ViewModel() {
    // Timeline speculation and the selected case share one calculation lane. This keeps
    // cancellation/priority deterministic and avoids wrapping an injected dispatcher again.
    private val fortunePrefetchDispatcher: CoroutineDispatcher = fortuneCalculationDispatcher
    private val restoredStateBundle = savedStateHandle.get<Bundle>(SAVED_UI_STATE_KEY)
    private val mutableState = MutableStateFlow(
        restoredUiStateOverride ?: restoredStateBundle?.toStageTwoUiState() ?: run {
            val defaultRule = calculationPreferenceStore.readRatHourRule()
            StageTwoUiState(
                defaultRatHourRule = defaultRule,
                form = CaseFormState(ratHourRule = defaultRule),
                fourPillarsLookupForm = FourPillarsLookupFormState(ratHourRule = defaultRule),
            )
        },
    )
    val state: StateFlow<StageTwoUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var caseSummaryCatalogCache: List<CaseSummary>? = null
    private var caseGroupsCache: Map<CaseLibraryType, List<CaseGroup>> = emptyMap()
    private val retainedCaseDetails = object : LinkedHashMap<String, RetainedCaseDetailSnapshot>(
        RETAINED_DETAIL_CACHE_SIZE,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, RetainedCaseDetailSnapshot>?,
        ): Boolean = size > RETAINED_DETAIL_CACHE_SIZE
    }
    private val fortunePositionCache = object : LinkedHashMap<
        FortunePositionCacheKey,
        FortunePositionCacheValue,
    >(
        FORTUNE_POSITION_CACHE_SIZE,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<
                FortunePositionCacheKey,
                FortunePositionCacheValue,
            >?,
        ): Boolean = size > FORTUNE_POSITION_CACHE_SIZE
    }
    private var comparisonJob: Job? = null
    private var fourPillarsLookupJob: Job? = null
    private var almanacJob: Job? = null
    private var detailOpeningJob: Job? = null
    private val detailPrefetchJobs = LinkedHashMap<String, Job>()
    private var fortunePositionJob: Job? = null
    private var fortunePrefetchJob: Job? = null
    private var fortunePositionRequestId: Long = 0
    private var saveCaseJob: Job? = null
    private var preparedCaseSaveAttempt: PreparedCaseSaveAttempt? = null
    private var almanacRequestId: Long = 0
    private var pendingAlmanacQuery: AlmanacMonthQuery? = null
    private var birthPickerTodayJob: Job? = null
    private var caseNotesAutoSaveJob: Job? = null
    private var caseNotesHydrationJob: Job? = null
    private var pendingExportPassword: CharArray? = null
    private var pendingSingleCaseBundleExport: Boolean = false
    private var pendingSingleCaseBundleCommit: PendingSingleCaseBundleCommit? = null
    private var pendingFullBackupPassword: CharArray? = null
    private var pendingCaseImages: List<RenderedCaseImage> = emptyList()
    private val pendingCaseImage: RenderedCaseImage? get() = pendingCaseImages.firstOrNull()
    private var pendingWenzhenImport: WenzhenWebImportPackage? = null
    private val wenzhenImporter = WenzhenWebImporter(caseRepository, baziEngine, clock)

    private fun retainCaseDetail(
        case: BaziCase,
        professionalObservation: ProfessionalObservationSeed? = null,
    ) {
        synchronized(retainedCaseDetails) {
            val previous = retainedCaseDetails[case.id]
            val adoptedSnapshotId = case.calculationSnapshots
                .asReversed()
                .firstOrNull { it.adopted }
                ?.id
            val reusableObservation = previous
                ?.professionalObservation
                ?.takeIf { it.calculationSnapshotId == adoptedSnapshotId }
            retainedCaseDetails[case.id] = RetainedCaseDetailSnapshot(
                case = case,
                notes = case.toCaseNotesDraft(),
                professionalObservation = professionalObservation ?: reusableObservation,
            )
        }
    }

    private fun retainedCaseDetailSnapshot(caseId: String): RetainedCaseDetailSnapshot? =
        synchronized(retainedCaseDetails) { retainedCaseDetails[caseId] }

    private fun retainedCaseDetail(caseId: String): BaziCase? =
        synchronized(retainedCaseDetails) { retainedCaseDetails[caseId]?.case }

    private fun retainedCaseNotes(caseId: String): CaseNotesDraft? =
        synchronized(retainedCaseDetails) { retainedCaseDetails[caseId]?.notes }

    private fun retainedProfessionalObservation(
        case: BaziCase,
    ): ProfessionalObservationSeed? {
        val snapshot = retainedCaseDetailSnapshot(case.id) ?: return null
        val adoptedSnapshotId = case.calculationSnapshots
            .asReversed()
            .firstOrNull { it.adopted }
            ?.id
        val expectedObservedAt = case.defaultProfessionalObservation(
            now = LocalDateTime.now(observationClock),
        ).copy(second = 0)
        return snapshot.professionalObservation?.takeIf { seed ->
            seed.calculationSnapshotId == adoptedSnapshotId &&
                seed.observedAt == expectedObservedAt &&
                seed.cached?.professionalPosition != null
        }
    }

    private fun activeDetailPrefetchJob(caseId: String): Job? =
        synchronized(detailPrefetchJobs) { detailPrefetchJobs[caseId] }

    fun prefetchCaseDetail(caseId: String) {
        val retained = retainedCaseDetail(caseId)
        if (
            activeDetailPrefetchJob(caseId) != null ||
            (retained != null && retainedProfessionalObservation(retained) != null)
        ) return
        lateinit var prefetchJob: Job
        prefetchJob = viewModelScope.launch(ioDispatcher, start = CoroutineStart.LAZY) {
            try {
                val detail = caseRepository.findById(caseId) ?: return@launch
                retainCaseDetail(detail)
                // Read + professional positioning are one tracked prefetch transaction.
                // A cache entry is considered ready only after the complete screen seed exists.
                val observation = prewarmProfessionalFortune(detail)
                retainCaseDetail(detail, observation)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Prefetch is opportunistic. The normal detail load owns user-visible errors.
            } finally {
                synchronized(detailPrefetchJobs) {
                    if (detailPrefetchJobs[caseId] === prefetchJob) {
                        detailPrefetchJobs.remove(caseId)
                    }
                }
            }
        }
        val shouldStart = synchronized(detailPrefetchJobs) {
            val currentRetained = retainedCaseDetail(caseId)
            if (
                detailPrefetchJobs.containsKey(caseId) ||
                (currentRetained != null &&
                    retainedProfessionalObservation(currentRetained) != null)
            ) {
                false
            } else {
                while (detailPrefetchJobs.size >= RETAINED_DETAIL_CACHE_SIZE) {
                    val oldest = detailPrefetchJobs.entries.first()
                    detailPrefetchJobs.remove(oldest.key)
                    oldest.value.cancel()
                }
                detailPrefetchJobs[caseId] = prefetchJob
                true
            }
        }
        if (shouldStart) prefetchJob.start() else prefetchJob.cancel()
    }

    private fun cachedFortunePosition(
        key: FortunePositionCacheKey,
    ): FortunePositionCacheValue? =
        synchronized(fortunePositionCache) { fortunePositionCache[key] }

    private fun retainFortunePosition(
        key: FortunePositionCacheKey,
        value: FortunePositionCacheValue,
    ) {
        synchronized(fortunePositionCache) {
            fortunePositionCache[key] = value
        }
    }

    init {
        if (restoredStateBundle != null) {
            mutableState.update {
                it.copy(defaultRatHourRule = calculationPreferenceStore.readRatHourRule())
            }
        }
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
        if (mutableState.value.destination == AppDestination.Almanac) {
            loadAlmanac()
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
            if (
                restoredCase != null &&
                    mutableState.value.destination is AppDestination.CaseDetail &&
                    mutableState.value.detailSection == CaseDetailSection.FORTUNE
            ) {
                // A process restore must follow the same finished-first contract as
                // opening a record from the list.
                prewarmProfessionalFortune(restoredCase)
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
            val externalSummaryResult = if (
                restoredCase != null &&
                mutableState.value.destination is AppDestination.ExternalAnalysisBridge
            ) {
                objectiveSummaryGenerator.generate(CaseObjectiveSummaryInput(restoredCase))
            } else {
                null
            }
            val externalExportResult = if (
                externalSummaryResult is CaseObjectiveSummaryResult.Success
            ) {
                externalAnalysisBridge.prepareExport(
                    ExternalAnalysisExportRequest(
                        summary = externalSummaryResult.summary,
                        selectedGroups = mutableState.value.externalAnalysisDraft.selectedGroups,
                        redactionPolicy = ExternalAnalysisRedactionPolicy(
                            mutableState.value.externalAnalysisDraft.redactionEnabled,
                        ),
                    ),
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
                        caseNotesCaseId = null,
                        caseNotesRevision = null,
                        caseNotesDraft = CaseNotesDraft(),
                        caseNotesSavedDraft = CaseNotesDraft(),
                        caseNotesHydrating = false,
                        caseNotesSaving = false,
                        caseNotesSaveError = null,
                        detailLoading = false,
                        detailError = null,
                        objectiveSummary = null,
                        objectiveSummaryLoading = false,
                        objectiveSummaryFailure = null,
                        externalAnalysisPayload = null,
                        externalAnalysisFailure = null,
                        externalAnalysisSaving = false,
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
                    val restoredNotesDraft = restoredCase.toCaseNotesDraft()
                    val restoredState = it.copy(
                        detail = restoredCase,
                        caseNotesCaseId = restoredCase.id,
                        caseNotesRevision = restoredCase.revision,
                        caseNotesDraft = restoredNotesDraft,
                        caseNotesSavedDraft = restoredNotesDraft,
                        caseNotesHydrating = false,
                        caseNotesSaving = false,
                        caseNotesSaveError = null,
                        detailLoading = false,
                        detailError = null,
                    )
                    when {
                        externalSummaryResult is CaseObjectiveSummaryResult.Rejected -> restoredState.copy(
                            objectiveSummary = null,
                            externalAnalysisPayload = null,
                            externalAnalysisFailure = ExternalAnalysisBridgeFailure(
                                ExternalAnalysisBridgeErrorCode.SUMMARY_UNAVAILABLE,
                                externalSummaryResult.failure.message,
                            ),
                            externalAnalysisSaving = false,
                        )
                        externalExportResult is ExternalAnalysisExportResult.Success -> restoredState.copy(
                            objectiveSummary =
                                (externalSummaryResult as CaseObjectiveSummaryResult.Success)
                                    .summary,
                            externalAnalysisPayload = externalExportResult.payload,
                            externalAnalysisFailure = null,
                            externalAnalysisCopied = false,
                            externalAnalysisSaving = false,
                            externalAnalysisDraft = it.externalAnalysisDraft.copy(
                                exportConfirmed = false,
                                importConfirmed = false,
                            ),
                        )
                        externalExportResult is ExternalAnalysisExportResult.Rejected -> restoredState.copy(
                            objectiveSummary =
                                (externalSummaryResult as CaseObjectiveSummaryResult.Success)
                                    .summary,
                            externalAnalysisPayload = null,
                            externalAnalysisFailure = externalExportResult.failure,
                            externalAnalysisSaving = false,
                        )
                        summaryResult is CaseObjectiveSummaryResult.Success -> restoredState.copy(
                            objectiveSummary = summaryResult.summary,
                            objectiveSummaryLoading = false,
                            objectiveSummaryFailure = null,
                        )
                        summaryResult is CaseObjectiveSummaryResult.Rejected -> restoredState.copy(
                            objectiveSummary = null,
                            objectiveSummaryLoading = false,
                            objectiveSummaryFailure = summaryResult.failure,
                        )
                        candidateResult is
                            MasterCommentaryCandidateExtractionResult.Success -> restoredState.copy(
                                commentaryCandidateSet = candidateResult.candidateSet
                                    .mergeDecisionsFrom(it.commentaryCandidateSet),
                                commentaryCandidateFailure = null,
                                commentaryCandidateAdoptionFailure = null,
                                commentaryCandidateSavingId = null,
                            )
                        candidateResult is
                            MasterCommentaryCandidateExtractionResult.Failure -> restoredState.copy(
                                commentaryCandidateSet = null,
                                commentaryCandidateFailure = candidateResult.failure,
                                commentaryCandidateAdoptionFailure = null,
                                commentaryCandidateSavingId = null,
                            )
                        candidateDestination != null -> restoredState.copy(
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
                            restoredState.copy(
                                feedbackThemeCandidateSet = feedbackResult.candidateSet
                                    .mergeDecisionsFrom(it.feedbackThemeCandidateSet),
                                feedbackThemeCandidateFailure = null,
                                feedbackThemeAdoptionFailure = null,
                                feedbackThemeSavingId = null,
                            )
                        feedbackResult is FeedbackThemeCandidateExtractionResult.Failure ->
                            restoredState.copy(
                                feedbackThemeCandidateSet = null,
                                feedbackThemeCandidateFailure = feedbackResult.failure,
                                feedbackThemeAdoptionFailure = null,
                                feedbackThemeSavingId = null,
                            )
                        feedbackDestination != null -> restoredState.copy(
                            feedbackThemeCandidateSet = null,
                            feedbackThemeCandidateFailure = null,
                            feedbackThemeAdoptionFailure = FeedbackThemeAdoptionFailure(
                                FeedbackThemeAdoptionErrorCode.SOURCE_NOT_FOUND,
                                "来源反馈已不存在，候选无法恢复。",
                            ),
                            feedbackThemeSavingId = null,
                        )
                        else -> restoredState
                    }
                }
            }
            if (
                mutableState.value.detailSection == CaseDetailSection.FORTUNE ||
                mutableState.value.detailSection == CaseDetailSection.BASIC_CHART
            ) {
                if (mutableState.value.detailSection == CaseDetailSection.FORTUNE) {
                    mutableState.value.detail?.let(::resetProfessionalObservation)
                }
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
        refreshCases(delayMillis = 0L, forceCatalogReload = true)
    }

    private fun refreshCasesFromCache(delayMillis: Long = 0L) {
        refreshCases(delayMillis = delayMillis, forceCatalogReload = false)
    }

    /**
     * 创建页的分组目录必须与表单的案例库归属同步，不能借用上一个记录列表的展示投影。
     * 目录缓存未就绪时只返回空列表；后续由 [refreshCasesFromCache] 从同一目录读取链路补齐。
     */
    private fun cachedGroupsForLibrary(libraryType: CaseLibraryType): List<CaseGroup> =
        caseGroupsCache[libraryType]
            ?: caseCatalogStore.cached()?.groupsByLibrary?.get(libraryType)
            ?: emptyList()

    private fun refreshCreateGroupsIfCatalogUnavailable() {
        if (caseGroupsCache.isEmpty() && caseCatalogStore.cached() == null) {
            refreshCasesFromCache()
        }
    }

    private suspend fun updateCachedCaseCatalog(
        transform: (List<CaseSummary>) -> List<CaseSummary>,
    ): Boolean {
        val catalog = caseSummaryCatalogCache ?: caseCatalogStore.cached()?.cases ?: return false
        val updatedCatalog = transform(catalog)
        caseSummaryCatalogCache = updatedCatalog
        caseCatalogStore.replaceCachedCases(updatedCatalog)
        return true
    }

    private fun refreshCases(
        delayMillis: Long,
        forceCatalogReload: Boolean,
    ) {
        searchJob?.cancel()
        val current = mutableState.value
        val request = CaseSearchRequest(
            query = current.query,
            groupId = current.selectedGroupId,
            tagId = current.selectedTagId,
            sortOrder = current.sortOrder,
            visibility = current.visibility,
            libraryType = current.libraryType.takeIf {
                current.visibility == CaseVisibility.ACTIVE
            },
            advancedFilter = current.advancedFilter,
        )
        val sharedCache = caseCatalogStore.cached()
        val cachedCatalog = caseSummaryCatalogCache ?: sharedCache?.cases
        val cachedGroups = caseGroupsCache.takeIf { it.isNotEmpty() }
            ?: sharedCache?.groupsByLibrary.orEmpty()
        val needsCatalogLoad = forceCatalogReload || cachedCatalog == null
        searchJob = viewModelScope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            if (cachedCatalog == null) {
                mutableState.update {
                    it.copy(
                        listLoading = true,
                        listError = null,
                    )
                }
            } else {
                mutableState.update { it.copy(listError = null) }
            }
            try {
                cachedCatalog?.let { catalog ->
                    publishCaseProjection(
                        projection = withContext(ioDispatcher) {
                            projectCaseList(catalog, cachedGroups, current, request)
                        },
                        current = current,
                        clearMissingFormGroup = !needsCatalogLoad,
                    )
                }
                if (!needsCatalogLoad) return@launch

                if (cachedCatalog == null) {
                    val bootstrap = caseCatalogStore.bootstrap()
                    publishCaseProjection(
                        projection = withContext(ioDispatcher) {
                            projectCaseList(
                                bootstrap.snapshot.cases,
                                bootstrap.snapshot.groupsByLibrary,
                                current,
                                request,
                            )
                        },
                        current = current,
                        clearMissingFormGroup = !bootstrap.fromPersistentCache,
                    )
                    if (!bootstrap.fromPersistentCache) return@launch
                }

                val refreshed = caseCatalogStore.load(forceRefresh = true)
                publishCaseProjection(
                    projection = withContext(ioDispatcher) {
                        projectCaseList(
                            refreshed.cases,
                            refreshed.groupsByLibrary,
                            current,
                            request,
                        )
                    },
                    current = current,
                    clearMissingFormGroup = true,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    if (it.cases.isNotEmpty() || cachedCatalog != null) {
                        it.copy(listLoading = false)
                    } else {
                        it.copy(
                            listLoading = false,
                            listError = "命例列表读取失败，请点击重试。",
                        )
                    }
                }
            }
        }
    }

    private fun projectCaseList(
        catalog: List<CaseSummary>,
        groupsByLibrary: Map<CaseLibraryType, List<CaseGroup>>,
        current: StageTwoUiState,
        request: CaseSearchRequest,
    ): CaseListProjection {
        val allCasesForControls = catalog.searchCases(
            CaseSearchRequest(
                visibility = current.visibility,
                libraryType = current.libraryType.takeIf {
                    current.visibility == CaseVisibility.ACTIVE
                },
            ),
        )
        val activeCases = catalog.filter { it.deletedAt == null }
        return CaseListProjection(
            catalog = catalog,
            groupsByLibrary = groupsByLibrary,
            cases = catalog.searchCases(request),
            batchCases = catalog.searchCases(
                CaseSearchRequest(
                    sortOrder = CaseSortOrder.NAME_ASC,
                    visibility = CaseVisibility.ACTIVE,
                    libraryType = current.libraryType,
                ),
            ),
            recentCases = catalog.searchCases(
                CaseSearchRequest(
                    sortOrder = CaseSortOrder.LAST_VIEWED_DESC,
                    visibility = CaseVisibility.ACTIVE,
                    libraryType = CaseLibraryType.USER,
                ),
            ).filter { it.lastViewedAt != null }.take(3),
            allCasesForControls = allCasesForControls,
            libraryCaseCounts = CaseLibraryType.entries.associateWith { libraryType ->
                activeCases.count { it.libraryType == libraryType }
            },
            trashedCaseCount = catalog.count { it.deletedAt != null },
            groupCaseCounts = allCasesForControls
                .asSequence()
                .flatMap { it.groups.asSequence() }
                .groupingBy { it.id }
                .eachCount(),
        )
    }

    private fun publishCaseProjection(
        projection: CaseListProjection,
        current: StageTwoUiState,
        clearMissingFormGroup: Boolean,
    ) {
        caseSummaryCatalogCache = projection.catalog
        caseGroupsCache = projection.groupsByLibrary
        val groups = projection.groupsByLibrary[current.libraryType].orEmpty()
        mutableState.update {
            it.copy(
                cases = projection.cases,
                batchCases = projection.batchCases,
                recentCases = projection.recentCases,
                availableGroups = groups,
                form = if (
                    clearMissingFormGroup &&
                    it.form.groupId != null && groups.none { group ->
                        group.id == it.form.groupId
                    }
                ) {
                    it.form.copy(groupId = null)
                } else {
                    it.form
                },
                availableTags = projection.allCasesForControls.flatMap { item -> item.tags }
                    .distinctBy { tag -> tag.id }
                    .sortedBy { tag -> tag.name },
                availableBirthRegions = projection.allCasesForControls.mapNotNull { item ->
                    item.birthInput.locationName?.trim()?.takeIf(String::isNotEmpty)
                }.distinct().sorted(),
                availableSeasonalWuxingStates = projection.allCasesForControls
                    .flatMap { item -> item.seasonalWuxingStates }
                    .distinct()
                    .sorted(),
                availableShenSha = projection.allCasesForControls
                    .flatMap { item -> item.shenShaNames }
                    .distinct()
                    .sorted(),
                libraryCaseCounts = projection.libraryCaseCounts,
                trashedCaseCount = projection.trashedCaseCount,
                groupCaseCounts = projection.groupCaseCounts,
                listLoading = false,
                listError = null,
            )
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
                fullBackupExportConfirmationVisible = true,
                fullBackupPasswordExportVisible = false,
                fullBackupPasswordError = null,
            )
        }
    }

    fun confirmPasswordFullBackupExport(
        password: CharArray,
    ): String? {
        val service = caseBackupService
        if (service == null || backupAttachmentRoot == null) {
            password.fill('\u0000')
            mutableState.update {
                it.copy(fullBackupError = "完整备份服务尚未就绪（BACKUP_SERVICE_UNAVAILABLE）。")
            }
            return null
        }
        val error = validateExportPassword(password)
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
        if (detail == null || detail.deletedAt != null || mutableState.value.caseImageBusy) return
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
        onCaptureRequired: (CaseImageDeliveryMode, CaseImageCaptureFacts, List<String>) -> Unit,
    ) {
        val current = mutableState.value
        val detail = current.detail ?: return
        if (detail.deletedAt != null) {
            cancelCaseImageConfirmation()
            return
        }
        val mode = current.caseImageConfirmationMode ?: return
        val prepared = CaseImageExportContract.prepare(CaseImageExportInput(detail))
        if (prepared is CaseImageFactsResult.Rejected) {
            setCaseImageFailure(prepared.failure.code, prepared.failure.message)
            return
        }
        val facts = (prepared as CaseImageFactsResult.Prepared).facts
        mutableState.update {
            it.copy(
                caseImageConfirmationMode = null,
                caseImageBusy = true,
                caseImageError = null,
                caseImageLastResultCode = null,
            )
        }
        onCaptureRequired(mode, facts, facts.suggestedFileStem.toSafeImageFileNames())
    }

    internal fun completeCaseDetailPageCapture(
        mode: CaseImageDeliveryMode,
        facts: CaseImageCaptureFacts,
        fileName: String,
        captured: CapturedCaseDetailLongImage,
        onPrepared: (CaseImageDeliveryMode, String) -> Unit,
    ) {
        completeCaseDetailPageCapture(
            mode = mode,
            facts = facts,
            fileNames = listOf(fileName, fileName),
            captured = CapturedCaseDetailLongImages(captured, captured),
        ) { completedMode, completedFileNames ->
            onPrepared(completedMode, completedFileNames.first())
        }
    }

    internal fun completeCaseDetailPageCapture(
        mode: CaseImageDeliveryMode,
        facts: CaseImageCaptureFacts,
        fileNames: List<String>,
        captured: CapturedCaseDetailLongImages,
        onPrepared: (CaseImageDeliveryMode, List<String>) -> Unit,
    ) {
        val detail = mutableState.value.detail
        val factsStillCurrent = detail != null &&
            detail.id == facts.caseId &&
            detail.revision == facts.caseRevision &&
            detail.calculationSnapshots.any { snapshot ->
                snapshot.adopted && snapshot.id == facts.adoptedSnapshotId
            }
        if (!factsStillCurrent) {
            setCaseImageFailure(
                CaseImageExportErrorCode.RENDER_FAILED,
                "截图期间命例内容已更新，请重新生成当前页面长图。",
            )
            return
        }
        if (fileNames.size != 2) {
            setCaseImageFailure(CaseImageExportErrorCode.RENDER_FAILED, "命盘图片文件名不完整，请重新生成。")
            return
        }
        pendingCaseImages = listOf(captured.chart, captured.notes).map { image ->
            RenderedCaseImage(
                facts = facts,
                mimeType = "image/png",
                fileExtension = "png",
                bytes = image.bytes,
                widthPixels = image.widthPixels,
                heightPixels = image.heightPixels,
                sha256 = image.sha256,
            )
        }
        mutableState.update { it.copy(caseImageBusy = false) }
        onPrepared(mode, fileNames)
    }

    fun reportCaseDetailPageCaptureFailed(message: String) {
        setCaseImageFailure(
            CaseImageExportErrorCode.RENDER_FAILED,
            message,
        )
    }

    fun exportPreparedCaseImage(
        openOutput: () -> OutputStream?,
        onCompleted: (Boolean) -> Boolean = { true },
    ) {
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
                if (!onCompleted(true)) {
                    reportCaseImageGallerySaveFailed()
                    return@launch
                }
                mutableState.update {
                    it.copy(
                        caseImageBusy = false,
                        caseImageLastResultCode = null,
                        message = "命盘长图已保存到手机图库。",
                    )
                }
            } else {
                onCompleted(false)
                setCaseImageFailure(failure.code, failure.message)
            }
        }
    }

    fun exportPreparedCaseImages(
        openOutput: (Int) -> OutputStream?,
        onCompleted: (Boolean) -> Boolean = { true },
    ) {
        if (pendingCaseImages.size != 2) {
            setCaseImageFailure(CaseImageExportErrorCode.RENDER_FAILED, "已生成的两张命盘图片已失效，请重新选择导出。")
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(caseImageBusy = true, caseImageError = null) }
            val failure = writeCaseImageBytes(pendingCaseImages, openOutput)
            if (failure == null) {
                if (!onCompleted(true)) {
                    reportCaseImageGallerySaveFailed()
                    return@launch
                }
                mutableState.update {
                    it.copy(caseImageBusy = false, caseImageLastResultCode = null, message = "两张命盘长图已保存到手机图库。")
                }
            } else {
                onCompleted(false)
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

    fun copyPreparedCaseImagesForShare(
        openOutput: (Int) -> OutputStream?,
        onReady: () -> Unit,
    ) {
        if (pendingCaseImages.size != 2) {
            setCaseImageFailure(CaseImageExportErrorCode.RENDER_FAILED, "已生成的两张命盘图片已失效，请重新选择分享。")
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(caseImageBusy = true, caseImageError = null) }
            val failure = writeCaseImageBytes(pendingCaseImages, openOutput)
            if (failure == null) onReady() else setCaseImageFailure(failure.code, failure.message)
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
            "系统中没有可接收 PNG 图片的分享目标；可先保存到手机图库。",
        )
    }

    fun reportCaseImageShareLaunchFailed() {
        setCaseImageFailure(
            CaseImageExportErrorCode.SHARE_LAUNCH_FAILED,
            "系统分享面板无法打开；当前详情和图片仍保留，可重试或改为保存文件。",
        )
    }

    /**
     * Android does not expose whether a third-party app eventually sent the images.
     * Only acknowledge the hand-off to the system chooser, never the external send.
     */
    fun markCaseImageShareHandedOff() {
        mutableState.update {
            it.copy(
                caseImageBusy = false,
                caseImageError = null,
                caseImageLastResultCode = null,
                message = "已打开系统分享面板，请在目标应用内确认发送。",
            )
        }
    }

    fun reportCaseImageGallerySaveFailed() {
        setCaseImageFailure(
            CaseImageExportErrorCode.WRITE_FAILED,
            "无法写入手机默认图库，当前详情和已生成图片仍保留。",
        )
    }

    fun completeCaseImageShare(cancelled: Boolean) {
        mutableState.update {
            // A late chooser callback must not overwrite a real launch/copy failure.
            if (it.caseImageError != null) return@update it
            it.copy(
                caseImageBusy = false,
                caseImageLastResultCode =
                    if (cancelled) CaseImageExportErrorCode.USER_CANCELLED else null,
                message = if (cancelled) {
                    "分享面板已关闭；南枫八字无法证明图片已由目标应用发送。"
                } else {
                    "分享面板已关闭；是否实际发送请以目标应用状态为准。"
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

    private suspend fun writeCaseImageBytes(
        images: List<RenderedCaseImage>,
        openOutput: (Int) -> OutputStream?,
    ) = try {
        withContext(ioDispatcher) {
            images.forEachIndexed { index, image ->
                val output = openOutput(index)
                    ?: return@withContext com.nanzhufeng.nanfengbazi.domain.CaseImageExportFailure(
                        CaseImageExportErrorCode.OUTPUT_UNAVAILABLE,
                        "无法打开第 ${index + 1} 张命盘图片的目标文件。",
                    )
                output.use { it.write(image.bytes); it.flush() }
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
        if (detail == null || detail.deletedAt != null || mutableState.value.singleCaseExchangeBusy) return
        mutableState.update {
            it.copy(
                singleCaseExportConfirmationVisible = true,
                singleCaseExportIncludesAttachments = false,
                singleCaseExchangeError = null,
            )
        }
    }

    fun chooseSingleCaseExportAttachments(include: Boolean) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null || mutableState.value.singleCaseExchangeBusy) return
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

    fun confirmSingleCaseExport(): SingleCaseExportDocumentRequest? {
        val detail = mutableState.value.detail ?: return null
        if (detail.deletedAt != null) {
            cancelSingleCaseExport()
            return null
        }
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
            singleCaseBundleService?.suggestedFileName(detail)?.let { fileName ->
                SingleCaseExportDocumentRequest(
                    fileName = fileName,
                    kind = SingleCaseExportDocumentKind.BUNDLE,
                )
            }
        } else {
            SingleCaseExportDocumentRequest(
                fileName = singleCaseExchange.suggestedFileName(detail),
                kind = SingleCaseExportDocumentKind.JSON,
            )
        }
    }

    fun requestPasswordSingleCaseExport() {
        val detail = mutableState.value.detail
        if (detail == null || detail.deletedAt != null || mutableState.value.singleCaseExchangeBusy) return
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
                singleCaseExportConfirmationVisible = true,
                singleCasePasswordExportVisible = false,
                singleCasePasswordError = null,
            )
        }
    }

    fun confirmPasswordSingleCaseExport(
        password: CharArray,
    ): SingleCaseExportDocumentRequest? {
        val detail = mutableState.value.detail
        if (detail == null) {
            password.fill('\u0000')
            return null
        }
        val error = when {
            password.size < MIN_EXPORT_PASSWORD_LENGTH ->
                "密码至少需要 $MIN_EXPORT_PASSWORD_LENGTH 个字符。"
            password.size > MAX_EXPORT_PASSWORD_LENGTH ->
                "密码不能超过 $MAX_EXPORT_PASSWORD_LENGTH 个字符。"
            else -> null
        }
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
            singleCaseBundleService?.suggestedEncryptedFileName(detail)?.let { fileName ->
                SingleCaseExportDocumentRequest(
                    fileName = fileName,
                    kind = SingleCaseExportDocumentKind.ENCRYPTED_BUNDLE,
                )
            }
        } else {
            SingleCaseExportDocumentRequest(
                fileName = singleCaseExchange.suggestedEncryptedFileName(detail),
                kind = SingleCaseExportDocumentKind.ENCRYPTED_JSON,
            )
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
        refreshCasesFromCache(delayMillis = searchDebounceMillis)
    }

    fun selectGroup(groupId: String?) {
        mutableState.update {
            it.copy(selectedGroupId = groupId, listError = null)
        }
        refreshCasesFromCache()
    }

    fun createCaseGroup(name: String) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val created = caseRepository.createGroup(name, mutableState.value.libraryType)
            mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = if (created == null) "分组名称为空或已经存在。" else null,
                    message = if (created != null) "分组已添加。" else it.message,
                )
            }
            if (created != null) refreshCases()
        }
    }

    fun createAndSelectCaseGroup(name: String) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val created = try {
                caseRepository.createGroup(name, mutableState.value.form.libraryType)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            mutableState.update {
                it.copy(
                    form = if (created == null) it.form else it.form.copy(groupId = created.id),
                    mutationSaving = false,
                    mutationError = if (created == null) {
                        "分组名称为空、已经存在或保存失败。"
                    } else {
                        null
                    },
                    message = if (created != null) "新分组已创建并选中。" else it.message,
                )
            }
            if (created != null) refreshCases()
        }
    }

    fun renameCaseGroup(groupId: String, name: String) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val saved = caseRepository.renameGroup(groupId, name)
            mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = if (saved) null else "分组重命名失败，请检查名称是否重复。",
                    message = if (saved) "分组名称已更新。" else it.message,
                )
            }
            if (saved) refreshCases()
        }
    }

    fun reorderCaseGroups(groupIds: List<String>) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            val saved = caseRepository.reorderGroups(groupIds, mutableState.value.libraryType)
            mutableState.update {
                it.copy(
                    mutationError = if (saved) null else "分组顺序保存失败。",
                    message = if (saved) "分组顺序已更新。" else it.message,
                )
            }
            if (saved) refreshCases()
        }
    }

    fun deleteCaseGroup(groupId: String) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val deleted = caseRepository.deleteGroup(groupId)
            mutableState.update {
                it.copy(
                    mutationSaving = false,
                    selectedGroupId = if (it.selectedGroupId == groupId) null else it.selectedGroupId,
                    mutationError = if (deleted) null else "分组删除失败。",
                    message = if (deleted) "分组已删除，命例本身未删除。" else it.message,
                )
            }
            if (deleted) refreshCases()
        }
    }

    fun updatePinnedCases(pinnedCaseIds: Set<String>) {
        if (mutableState.value.mutationSaving) return
        val currentCases = mutableState.value.batchCases
        val currentlyPinned = currentCases.filter { it.isPinned }.map { it.id }.toSet()
        val toPin = pinnedCaseIds - currentlyPinned
        val toUnpin = currentlyPinned - pinnedCaseIds
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val now = clock.instant()
            val changed = caseRepository.setCasesPinned(toPin, true, now) +
                caseRepository.setCasesPinned(toUnpin, false, now)
            mutableState.update {
                it.copy(
                    mutationSaving = false,
                    message = "已更新 $changed 个命例的星标置顶状态。",
                )
            }
            val expectedChanges = toPin.size + toUnpin.size
            val cacheUpdated = updateCachedCaseCatalog { catalog ->
                catalog.map { summary ->
                    when (summary.id) {
                        in toPin -> summary.copy(
                            isPinned = true,
                            updatedAt = now,
                            revision = summary.revision + 1,
                        )
                        in toUnpin -> summary.copy(
                            isPinned = false,
                            updatedAt = now,
                            revision = summary.revision + 1,
                        )
                        else -> summary
                    }
                }
            }
            if (cacheUpdated && changed == expectedChanges) {
                refreshCasesFromCache()
            } else {
                refreshCases()
            }
        }
    }

    fun togglePinnedCase(caseId: String) {
        val cases = mutableState.value.batchCases
        val target = cases.firstOrNull { it.id == caseId } ?: return
        val pinnedIds = cases.filter { it.isPinned }.map { it.id }.toMutableSet()
        if (target.isPinned) pinnedIds.remove(caseId) else pinnedIds.add(caseId)
        updatePinnedCases(pinnedIds)
    }

    fun batchDeleteCases(caseIds: Set<String>) {
        if (caseIds.isEmpty()) return
        val permanentlyDelete = mutableState.value.visibility == CaseVisibility.TRASHED
        val changedAt = clock.instant()
        val catalogBeforeDelete = caseSummaryCatalogCache
        val expectedChanges = catalogBeforeDelete.orEmpty().count { summary ->
            summary.id in caseIds && if (permanentlyDelete) {
                summary.deletedAt != null
            } else {
                summary.deletedAt == null
            }
        }
        val optimisticCatalog = catalogBeforeDelete?.let { catalog ->
            if (permanentlyDelete) {
                catalog.filterNot { summary ->
                    summary.id in caseIds && summary.deletedAt != null
                }
            } else {
                catalog.map { summary ->
                    if (summary.id in caseIds && summary.deletedAt == null) {
                        summary.copy(
                            deletedAt = changedAt,
                            updatedAt = changedAt,
                            revision = summary.revision + 1,
                        )
                    } else {
                        summary
                    }
                }
            }
        }
        viewModelScope.launch {
            if (optimisticCatalog != null) {
                caseSummaryCatalogCache = optimisticCatalog
                caseCatalogStore.replaceCachedCases(optimisticCatalog)
                refreshCasesFromCache()
            }
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            try {
                val changed = withContext(ioDispatcher) {
                    if (permanentlyDelete) {
                        caseRepository.deleteTrashedCasesPermanently(caseIds)
                    } else {
                        caseRepository.moveCasesToTrash(caseIds, changedAt)
                    }
                }
                mutableState.update {
                    it.copy(
                        mutationSaving = false,
                        message = if (permanentlyDelete) {
                            "已永久删除 $changed 个命例。"
                        } else {
                            "已将 $changed 个命例移入回收站，可在回收站恢复。"
                        },
                    )
                }
                if (optimisticCatalog == null || changed != expectedChanges) {
                    refreshCases()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (catalogBeforeDelete != null) {
                    caseSummaryCatalogCache = catalogBeforeDelete
                    caseCatalogStore.replaceCachedCases(catalogBeforeDelete)
                    val current = mutableState.value
                    val restoreRequest = CaseSearchRequest(
                        query = current.query,
                        groupId = current.selectedGroupId,
                        tagId = current.selectedTagId,
                        sortOrder = current.sortOrder,
                        visibility = current.visibility,
                        libraryType = current.libraryType.takeIf {
                            current.visibility == CaseVisibility.ACTIVE
                        },
                        advancedFilter = current.advancedFilter,
                    )
                    publishCaseProjection(
                        projection = withContext(ioDispatcher) {
                            projectCaseList(
                                catalogBeforeDelete,
                                caseGroupsCache,
                                current,
                                restoreRequest,
                            )
                        },
                        current = current,
                        clearMissingFormGroup = false,
                    )
                } else {
                    refreshCases()
                }
                mutableState.update {
                    it.copy(
                        mutationSaving = false,
                        mutationError = "删除失败，列表已恢复，请稍后重试。",
                        message = "删除失败，未更改案例数据。",
                    )
                }
            }
        }
    }

    fun selectTag(tagId: String?) {
        mutableState.update {
            it.copy(selectedTagId = tagId, listError = null)
        }
        refreshCasesFromCache()
    }

    fun selectSortOrder(sortOrder: CaseSortOrder) {
        mutableState.update {
            it.copy(sortOrder = sortOrder, listError = null)
        }
        refreshCasesFromCache()
    }

    fun applyAdvancedFilter(filter: CaseAdvancedFilter) {
        mutableState.update { it.copy(advancedFilter = filter, listError = null) }
        refreshCasesFromCache()
    }

    fun clearCaseFilters() {
        mutableState.update {
            it.copy(
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                listError = null,
            )
        }
        refreshCasesFromCache()
    }

    fun selectVisibility(visibility: CaseVisibility) {
        mutableState.update {
            it.copy(
                visibility = visibility,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                listError = null,
            )
        }
        refreshCasesFromCache()
    }

    fun selectCaseLibrary(libraryType: CaseLibraryType) {
        mutableState.update {
            it.copy(
                visibility = CaseVisibility.ACTIVE,
                libraryType = libraryType,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                listError = null,
            )
        }
        refreshCasesFromCache()
    }

    fun openCreateCelebrityCase() {
        val groups = cachedGroupsForLibrary(CaseLibraryType.CELEBRITY)
        mutableState.update {
            it.copy(
                destination = navigator.openCreate(),
                visibility = CaseVisibility.ACTIVE,
                libraryType = CaseLibraryType.CELEBRITY,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                availableGroups = groups,
                form = CaseFormState(
                    ratHourRule = it.defaultRatHourRule,
                    libraryType = CaseLibraryType.CELEBRITY,
                ),
                formError = null,
                duplicateCandidates = emptyList(),
                previewing = false,
                instantCalculation = null,
                mutationError = null,
                message = null,
            )
        }
        refreshCreateGroupsIfCatalogUnavailable()
    }

    fun openCreate() {
        val groups = cachedGroupsForLibrary(CaseLibraryType.USER)
        mutableState.update {
            it.copy(
                destination = navigator.openCreate(),
                visibility = CaseVisibility.ACTIVE,
                libraryType = CaseLibraryType.USER,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                availableGroups = groups,
                form = it.form.copy(libraryType = CaseLibraryType.USER),
                formError = null,
                duplicateCandidates = emptyList(),
                previewing = false,
                instantCalculation = null,
                mutationError = null,
                message = null,
            )
        }
        refreshCreateGroupsIfCatalogUnavailable()
    }

    fun openRecordHub() {
        mutableState.update {
            it.copy(
                destination = navigator.openRecordHub(),
                message = null,
            )
        }
    }

    fun openCaseComparison() {
        val cachedReady = mutableState.value.comparisonCandidates.size >= 2 &&
            mutableState.value.comparisonReport != null
        mutableState.update {
            it.copy(
                destination = navigator.openCaseComparison(),
                comparisonLoading = !cachedReady,
                comparisonError = null,
                message = null,
            )
        }
        if (!cachedReady) loadComparisonWorkspace()
    }

    fun openFourPillarsLookup(pillars: List<String> = emptyList()) {
        mutableState.update {
            val selected = if (pillars.size == 4) {
                it.fourPillarsLookupForm.copy(
                    yearPillar = pillars[0],
                    monthPillar = pillars[1],
                    dayPillar = pillars[2],
                    hourPillar = pillars[3],
                    ratHourRule = it.defaultRatHourRule,
                )
            } else {
                it.fourPillarsLookupForm.copy(ratHourRule = it.defaultRatHourRule)
            }
            it.copy(
                destination = navigator.openFourPillarsLookup(),
                fourPillarsLookupForm = selected,
                fourPillarsLookupCandidates = emptyList(),
                fourPillarsLookupEvidence = null,
                fourPillarsLookupHasSearched = false,
                fourPillarsLookupError = null,
                message = null,
            )
        }
    }

    fun openAlmanac() {
        val today = LocalDate.now(observationClock)
        mutableState.update {
            it.copy(
                destination = navigator.openAlmanac(),
                almanacYear = today.year,
                almanacMonth = today.monthValue,
                almanacSelectedDay = today.dayOfMonth,
                almanacView = null,
                almanacError = null,
                message = null,
            )
        }
        loadAlmanac()
    }

    fun prepareBirthPickerToday() {
        val reader = almanacReader ?: return
        val now = LocalDateTime.now(observationClock)
        val doubleHourIndex = when (now.hour) {
            0, 23 -> 0
            else -> (now.hour + 1) / 2
        }
        val query = AlmanacMonthQuery(
            year = now.year,
            month = now.monthValue,
            selectedDay = now.dayOfMonth,
            selectedDoubleHourIndex = doubleHourIndex,
            ratHourRule = mutableState.value.defaultRatHourRule,
        )
        birthPickerTodayJob?.cancel()
        mutableState.update { it.copy(birthPickerTodaySnapshot = null) }
        birthPickerTodayJob = viewModelScope.launch {
            val result = try {
                withContext(ioDispatcher) { reader.loadMonth(query) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                AlmanacResult.Failed(AlmanacError.EngineUnavailable)
            }
            val details = (result as? AlmanacResult.Completed)?.month?.selected ?: return@launch
            mutableState.update {
                it.copy(
                    birthPickerTodaySnapshot = BirthPickerTodaySnapshot(
                        solarYear = details.date.year,
                        solarMonth = details.date.month,
                        solarDay = details.date.day,
                        lunarYear = details.lunarYear,
                        lunarMonth = details.lunarMonth,
                        lunarDay = details.lunarDay,
                        isLeapMonth = details.isLeapMonth,
                        hour = now.hour,
                        minute = now.minute,
                        pillars = listOf(
                            details.yearPillar,
                            details.monthPillar,
                            details.dayPillar,
                            details.hourPillar,
                        ),
                    ),
                )
            }
        }
    }

    fun moveAlmanacMonth(offset: Int) {
        if (offset == 0) return
        val state = mutableState.value
        val baseQuery = pendingAlmanacQuery ?: state.almanacView?.query ?: AlmanacMonthQuery(
            year = state.almanacYear,
            month = state.almanacMonth,
            selectedDay = state.almanacSelectedDay,
            selectedDoubleHourIndex = state.almanacSelectedDoubleHourIndex,
            ratHourRule = state.defaultRatHourRule,
        )
        val target = YearMonth.of(baseQuery.year, baseQuery.month).plusMonths(offset.toLong())
        if (target.year !in AlmanacContract.MIN_YEAR..AlmanacContract.MAX_YEAR) return
        loadAlmanac(
            AlmanacMonthQuery(
                year = target.year,
                month = target.monthValue,
                selectedDay = baseQuery.selectedDay.coerceAtMost(target.lengthOfMonth()),
                selectedDoubleHourIndex = baseQuery.selectedDoubleHourIndex,
                ratHourRule = baseQuery.ratHourRule,
            ),
            preserveVisibleContent = true,
        )
    }

    fun showTodayInAlmanac() {
        val now = LocalDateTime.now(observationClock)
        val state = mutableState.value
        loadAlmanac(
            AlmanacMonthQuery(
                year = now.year,
                month = now.monthValue,
                selectedDay = now.dayOfMonth,
                selectedDoubleHourIndex = AlmanacDoubleHours.indexForCivilHour(now.hour),
                ratHourRule = state.defaultRatHourRule,
            ),
            preserveVisibleContent = true,
        )
    }

    fun selectAlmanacDate(date: AlmanacDate) {
        if (date.year !in AlmanacContract.MIN_YEAR..AlmanacContract.MAX_YEAR) return
        val state = mutableState.value
        val baseQuery = pendingAlmanacQuery ?: state.almanacView?.query
        loadAlmanac(
            AlmanacMonthQuery(
                year = date.year,
                month = date.month,
                selectedDay = date.day,
                selectedDoubleHourIndex = baseQuery?.selectedDoubleHourIndex
                    ?: state.almanacSelectedDoubleHourIndex,
                ratHourRule = state.defaultRatHourRule,
            ),
            preserveVisibleContent = true,
        )
    }

    /** 快捷跳转一次性更新日期与民用时间对应的时辰，避免界面重复刷新。 */
    fun selectAlmanacDateTime(date: AlmanacDate, civilHour: Int) {
        if (date.year !in AlmanacContract.MIN_YEAR..AlmanacContract.MAX_YEAR || civilHour !in 0..23) return
        val state = mutableState.value
        loadAlmanac(
            AlmanacMonthQuery(
                year = date.year,
                month = date.month,
                selectedDay = date.day,
                selectedDoubleHourIndex = AlmanacDoubleHours.indexForCivilHour(civilHour),
                ratHourRule = state.defaultRatHourRule,
            ),
            preserveVisibleContent = true,
        )
    }

    fun selectAlmanacDoubleHour(index: Int) {
        if (index !in 0..11) return
        val state = mutableState.value
        val baseQuery = pendingAlmanacQuery ?: state.almanacView?.query ?: AlmanacMonthQuery(
            year = state.almanacYear,
            month = state.almanacMonth,
            selectedDay = state.almanacSelectedDay,
            selectedDoubleHourIndex = state.almanacSelectedDoubleHourIndex,
            ratHourRule = state.defaultRatHourRule,
        )
        if (index == baseQuery.selectedDoubleHourIndex) return
        loadAlmanac(
            AlmanacMonthQuery(
                year = baseQuery.year,
                month = baseQuery.month,
                selectedDay = baseQuery.selectedDay,
                selectedDoubleHourIndex = index,
                ratHourRule = baseQuery.ratHourRule,
            ),
            preserveVisibleContent = true,
        )
    }

    fun useAlmanacDateForChart() {
        val state = mutableState.value
        if (state.previewing) return
        val selected = state.almanacView?.selected?.date
            ?: AlmanacDate(state.almanacYear, state.almanacMonth, state.almanacSelectedDay)
        val selectedHour = AlmanacDoubleHours
            .fromIndex(state.almanacSelectedDoubleHourIndex)
            .representativeHour
        val instantForm = state.form.copy(
            alias = "即时排盘案例",
            name = "",
            calendarSystem = CalendarSystem.SOLAR,
            year = selected.year.toString(),
            month = selected.month.toString(),
            day = selected.day.toString(),
            hour = selectedHour.toString(),
            minute = "0",
            second = "0",
            isLeapMonth = false,
            timePrecision = TimePrecision.DOUBLE_HOUR_ONLY,
        ).clearTimeZoneResolution()
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    form = instantForm,
                    previewing = true,
                    formError = null,
                    message = null,
                )
            }
            when (val result = withContext(ioDispatcher) { createCase.preview(instantForm) }) {
                is PreviewCaseResult.Calculated -> {
                    val now = clock.instant()
                    val caseId = "instant-almanac-preview"
                    val candidateId = "$caseId-candidate"
                    val snapshotId = "$caseId-snapshot"
                    val previewCase = BaziCase(
                        id = caseId,
                        alias = "即时排盘案例",
                        name = ExplicitText.absent(),
                        sexForFortuneDirection = result.calculation.normalizedInput
                            .sexForFortuneDirection,
                        sourceType = CaseSourceType.MANUAL,
                        birthInput = result.calculation.normalizedInput,
                        libraryType = CaseLibraryType.USER,
                        birthTimeCandidates = listOf(
                            BirthTimeCandidate(
                                id = candidateId,
                                label = "采用时间",
                                birthInput = result.calculation.normalizedInput,
                                calculationSnapshotId = snapshotId,
                                adopted = true,
                                createdAt = now,
                            ),
                        ),
                        calculationSnapshots = listOf(
                            CaseCalculationSnapshot(
                                id = snapshotId,
                                result = result.calculation,
                                adopted = true,
                                birthTimeCandidateId = candidateId,
                                createdAt = now,
                            ),
                        ),
                        createdAt = now,
                        updatedAt = now,
                    )
                    val initialObservation = prewarmProfessionalFortune(previewCase)
                    mutableState.update {
                        it.copy(
                            destination = navigator.openDetail(caseId),
                            previewing = false,
                            detail = previewCase,
                            caseNotesCaseId = previewCase.id,
                            caseNotesRevision = previewCase.revision,
                            caseNotesDraft = CaseNotesDraft(),
                            caseNotesSavedDraft = CaseNotesDraft(),
                            caseNotesHydrating = false,
                            detailIsTransient = true,
                            detailSection = CaseDetailSection.FORTUNE,
                            fortuneObservationDate = "%04d-%02d-%02d".format(
                                initialObservation.observedAt.year,
                                initialObservation.observedAt.month,
                                initialObservation.observedAt.day,
                            ),
                            fortuneObservationTime = "%02d:%02d".format(
                                initialObservation.observedAt.hour,
                                initialObservation.observedAt.minute,
                            ),
                            fortunePosition = initialObservation.cached?.position,
                            professionalFortunePosition =
                                initialObservation.cached?.professionalPosition,
                            fortunePositionError = null,
                            fortunePositionLoading = initialObservation.cached == null,
                            detailLoading = false,
                            detailError = null,
                            instantCalculation = result.calculation,
                            message = "即时排盘仅供查看，未保存到案例库。",
                        )
                    }
                }
                is PreviewCaseResult.ValidationFailed -> mutableState.update {
                    it.copy(
                        previewing = false,
                        formError = result.message,
                        message = "无法即时排盘：${result.message}",
                    )
                }
                is PreviewCaseResult.CalculationFailed -> mutableState.update {
                    it.copy(
                        previewing = false,
                        formError = "排盘失败：${result.message}",
                        message = "排盘失败：${result.message}",
                    )
                }
                is PreviewCaseResult.TimeZoneChoiceRequired -> mutableState.update {
                    it.copy(
                        previewing = false,
                        form = it.form.copy(
                            resolvedUtcOffsetSeconds = null,
                            availableUtcOffsetSeconds = result.validUtcOffsetSeconds,
                        ),
                        formError = "该时间在 ${result.timeZoneId} 出现两次，请先返回排盘页选择实际 UTC offset。",
                        message = "该时间存在两个 UTC offset，需先返回排盘页确认。",
                    )
                }
            }
        }
    }

    private fun loadAlmanac(
        query: AlmanacMonthQuery = mutableState.value.let { state ->
            AlmanacMonthQuery(
                year = state.almanacYear,
                month = state.almanacMonth,
                selectedDay = state.almanacSelectedDay,
                selectedDoubleHourIndex = state.almanacSelectedDoubleHourIndex,
                ratHourRule = state.defaultRatHourRule,
            )
        },
        preserveVisibleContent: Boolean = false,
    ) {
        val reader = almanacReader
        if (reader == null) {
            mutableState.update {
                it.copy(
                    almanacLoading = false,
                    almanacRefreshingSelection = false,
                    almanacError = "万年历引擎当前不可用。",
                )
            }
            return
        }
        if (mutableState.value.almanacView?.query == query || pendingAlmanacQuery == query) return
        almanacJob?.cancel()
        val requestId = ++almanacRequestId
        pendingAlmanacQuery = query
        almanacJob = viewModelScope.launch {
            val keepCurrentFrame = preserveVisibleContent && mutableState.value.almanacView != null
            mutableState.update {
                it.copy(
                    almanacLoading = !keepCurrentFrame,
                    almanacRefreshingSelection = keepCurrentFrame,
                    almanacError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) { reader.loadMonth(query) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                AlmanacResult.Failed(AlmanacError.EngineUnavailable)
            }
            if (requestId != almanacRequestId) return@launch
            pendingAlmanacQuery = null
            mutableState.update {
                when (result) {
                    is AlmanacResult.Completed -> it.copy(
                        almanacYear = query.year,
                        almanacMonth = query.month,
                        almanacSelectedDay = query.selectedDay,
                        almanacSelectedDoubleHourIndex = query.selectedDoubleHourIndex,
                        almanacView = result.month,
                        almanacLoading = false,
                        almanacRefreshingSelection = false,
                        almanacError = null,
                    )
                    is AlmanacResult.Failed -> it.copy(
                        almanacLoading = false,
                        almanacRefreshingSelection = false,
                        almanacError = result.error.toAlmanacUserMessage(),
                    )
                }
            }
        }
    }

    fun updateDefaultRatHourRule(rule: RatHourRule) {
        calculationPreferenceStore.writeRatHourRule(rule)
        fourPillarsLookupJob?.cancel()
        mutableState.update {
            it.copy(
                defaultRatHourRule = rule,
                form = it.form.copy(ratHourRule = rule),
                fourPillarsLookupForm = it.fourPillarsLookupForm.copy(ratHourRule = rule),
                fourPillarsLookupCandidates = emptyList(),
                fourPillarsLookupEvidence = null,
                fourPillarsLookupHasSearched = false,
                fourPillarsLookupError = null,
                message = "子时口径已设为${
                    if (rule == RatHourRule.TYME_DEFAULT) "23:00 换日" else "晚子时算当天"
                }。新排盘与四柱反查将使用该口径。",
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

    internal fun confirmFourPillarsLookup(selection: FourPillarsLookupSelection) {
        fourPillarsLookupJob?.cancel()
        val destination = if (mutableState.value.destination == AppDestination.FourPillarsLookup) {
            AppDestination.FourPillarsLookup
        } else {
            navigator.openFourPillarsLookup()
        }
        mutableState.update {
            it.copy(
                destination = destination,
                fourPillarsLookupForm = it.fourPillarsLookupForm.copy(
                    yearPillar = selection.pillars[0],
                    monthPillar = selection.pillars[1],
                    dayPillar = selection.pillars[2],
                    hourPillar = selection.pillars[3],
                    startYear = selection.startYear.toString(),
                    endYear = selection.endYear.toString(),
                    timeZoneId = it.form.timeZoneId,
                    ratHourRule = it.defaultRatHourRule,
                ),
                fourPillarsLookupCandidates = emptyList(),
                fourPillarsLookupEvidence = null,
                fourPillarsLookupLoading = false,
                fourPillarsLookupHasSearched = false,
                fourPillarsLookupError = null,
                message = null,
            )
        }
        searchFourPillars()
    }

    fun useFourPillarsLookupCandidate(candidate: FourPillarsLookupCandidate) {
        fourPillarsLookupJob?.cancel()
        val destination = navigator.back()
        mutableState.update {
            val candidateTime = candidate.civilDateTime
            val needsBirthplace = it.form.locationName.isBlank()
            it.copy(
                destination = destination,
                form = it.form.copy(
                    calendarSystem = CalendarSystem.SOLAR,
                    year = candidateTime.year.toString(),
                    month = candidateTime.month.toString(),
                    day = candidateTime.day.toString(),
                    hour = candidateTime.hour.toString(),
                    minute = "0",
                    second = "0",
                    isLeapMonth = false,
                    timeZoneId = it.form.timeZoneId,
                    resolvedUtcOffsetSeconds = candidate.resolvedUtcOffsetSeconds,
                    availableUtcOffsetSeconds = emptyList(),
                    useTrueSolarTime = false,
                    ratHourRule = candidate.ratHourRule,
                    timePrecision = TimePrecision.DOUBLE_HOUR_ONLY,
                ),
                fourPillarsLookupLoading = false,
                message = if (needsBirthplace) {
                    "已回填四柱候选时间，请补充出生地区。"
                } else {
                    "已回填四柱候选时间。"
                },
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
            when (val result = withContext(ioDispatcher) { createCase.preview(form) }) {
                is PreviewCaseResult.Calculated -> {
                    val previewCase = result.toTransientCase(
                        caseId = "instant-manual-preview",
                        fallbackAlias = "即时排盘案例",
                        libraryType = form.libraryType,
                        now = clock.instant(),
                    )
                    val initialObservation = prewarmProfessionalFortune(previewCase)
                    mutableState.update {
                        if (it.form == form && it.destination == AppDestination.CreateCase) {
                            it.copy(
                                destination = navigator.openDetail(previewCase.id),
                                previewing = false,
                                instantCalculation = result.calculation,
                                detail = previewCase,
                                caseNotesCaseId = previewCase.id,
                                caseNotesRevision = previewCase.revision,
                                caseNotesDraft = CaseNotesDraft(),
                                caseNotesSavedDraft = CaseNotesDraft(),
                                caseNotesHydrating = false,
                                detailIsTransient = true,
                                detailSection = CaseDetailSection.FORTUNE,
                                fortuneObservationDate = "%04d-%02d-%02d".format(
                                    initialObservation.observedAt.year,
                                    initialObservation.observedAt.month,
                                    initialObservation.observedAt.day,
                                ),
                                fortuneObservationTime = "%02d:%02d".format(
                                    initialObservation.observedAt.hour,
                                    initialObservation.observedAt.minute,
                                ),
                                fortunePosition = initialObservation.cached?.position,
                                professionalFortunePosition =
                                    initialObservation.cached?.professionalPosition,
                                fortunePositionError = null,
                                fortunePositionLoading = initialObservation.cached == null,
                                detailLoading = false,
                                detailError = null,
                                message = "即时排盘仅供查看，未保存到案例库。",
                            )
                        } else {
                            it.copy(previewing = false)
                        }
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
            when (val preview = withContext(ioDispatcher) { createCase.previewForSave(form) }) {
                is PreviewCaseResult.Calculated -> {
                    val pendingCase = preview.toTransientCase(
                        caseId = "pending-manual-save",
                        fallbackAlias = "即时排盘案例",
                        libraryType = form.libraryType,
                        now = clock.instant(),
                    )
                    val saveAttempt = PreparedCaseSaveAttempt(
                        pendingCase = pendingCase,
                        form = form,
                        preview = preview,
                        allowDuplicate = allowDuplicate,
                    )
                    preparedCaseSaveAttempt = saveAttempt
                    // The chart is already calculated at this point. Prepare the first
                    // professional position before navigation, then let duplicate checks
                    // and database persistence continue behind the detail screen.
                    val initialObservation = prewarmProfessionalFortune(pendingCase)
                    mutableState.update {
                        if (it.form == form && it.destination == AppDestination.CreateCase) {
                            it.copy(
                                destination = navigator.openDetail(pendingCase.id),
                                saving = true,
                                instantCalculation = preview.calculation,
                                duplicateCandidates = emptyList(),
                                detail = pendingCase,
                                caseNotesCaseId = pendingCase.id,
                                caseNotesRevision = pendingCase.revision,
                                caseNotesDraft = CaseNotesDraft(),
                                caseNotesSavedDraft = CaseNotesDraft(),
                                caseNotesHydrating = false,
                                detailIsTransient = true,
                                detailSavePending = true,
                                detailSaveError = null,
                                detailSaveDialogVisible = false,
                                detailSection = CaseDetailSection.FORTUNE,
                                fortuneObservationDate = "%04d-%02d-%02d".format(
                                    initialObservation.observedAt.year,
                                    initialObservation.observedAt.month,
                                    initialObservation.observedAt.day,
                                ),
                                fortuneObservationTime = "%02d:%02d".format(
                                    initialObservation.observedAt.hour,
                                    initialObservation.observedAt.minute,
                                ),
                                fortunePosition = initialObservation.cached?.position,
                                professionalFortunePosition =
                                    initialObservation.cached?.professionalPosition,
                                fortunePositionError = null,
                                fortunePositionLoading = initialObservation.cached == null,
                                detailLoading = false,
                                detailError = null,
                                message = null,
                            )
                        } else {
                            it.copy(saving = false)
                        }
                    }
                    if (mutableState.value.detail?.id == pendingCase.id) {
                        saveCaseJob = viewModelScope.launch {
                            val result = withContext(ioDispatcher) {
                                createCase.savePrepared(form, preview, allowDuplicate)
                            }
                            finishPreparedCaseSave(saveAttempt, result)
                        }
                    }
                }
                is PreviewCaseResult.ValidationFailed -> mutableState.update {
                    it.copy(saving = false, formError = preview.message)
                }
                is PreviewCaseResult.TimeZoneChoiceRequired -> mutableState.update {
                    it.copy(
                        form = it.form.copy(
                            resolvedUtcOffsetSeconds = null,
                            availableUtcOffsetSeconds = preview.validUtcOffsetSeconds,
                        ),
                        saving = false,
                        formError = "该出生时间在 ${preview.timeZoneId} 出现两次。" +
                            "请选择实际 UTC offset 后再次保存。",
                    )
                }
                is PreviewCaseResult.CalculationFailed -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "排盘失败：${preview.message} 输入内容已保留，可修改后重试。",
                    )
                }
            }
        }
    }

    private suspend fun finishPreparedCaseSave(
        attempt: PreparedCaseSaveAttempt,
        result: CreateCaseResult,
    ) {
        val pendingCase = attempt.pendingCase
        val form = attempt.form
        when (result) {
            is CreateCaseResult.Created -> {
                preparedCaseSaveAttempt = null
                retainCaseDetail(result.case)
                val savedObservation = prewarmProfessionalFortune(result.case)
                updateCachedCaseCatalog { catalog ->
                    catalog.filterNot { it.id == result.caseId } + result.case.toCaseSummary()
                }
                mutableState.update {
                    val showingPendingDetail =
                        it.destination == AppDestination.CaseDetail(pendingCase.id) &&
                            it.detail?.id == pendingCase.id &&
                            it.form == form
                    val stillOnCreatePage =
                        it.destination == AppDestination.CreateCase && it.form == form
                    val shouldOpenSavedCase = showingPendingDetail || stillOnCreatePage
                    it.copy(
                        destination = if (shouldOpenSavedCase) {
                            if (showingPendingDetail) {
                                navigator.replaceCurrentDetail(result.caseId)
                            } else {
                                navigator.openDetail(result.caseId)
                            }
                        } else {
                            it.destination
                        },
                        query = "",
                        form = if (it.form == form) CaseFormState() else it.form,
                        saving = false,
                        instantCalculation = if (shouldOpenSavedCase) null else it.instantCalculation,
                        duplicateCandidates = emptyList(),
                        detail = if (shouldOpenSavedCase) result.case else it.detail,
                        caseNotesCaseId = if (shouldOpenSavedCase) {
                            result.case.id
                        } else {
                            it.caseNotesCaseId
                        },
                        caseNotesRevision = if (shouldOpenSavedCase) {
                            result.case.revision
                        } else {
                            it.caseNotesRevision
                        },
                        caseNotesDraft = if (shouldOpenSavedCase) {
                            result.case.toCaseNotesDraft()
                        } else {
                            it.caseNotesDraft
                        },
                        caseNotesSavedDraft = if (shouldOpenSavedCase) {
                            result.case.toCaseNotesDraft()
                        } else {
                            it.caseNotesSavedDraft
                        },
                        caseNotesHydrating = false,
                        detailIsTransient = if (shouldOpenSavedCase) false else it.detailIsTransient,
                        detailSavePending = false,
                        detailSaveError = null,
                        detailSaveDialogVisible = false,
                        detailSection = if (shouldOpenSavedCase) {
                            CaseDetailSection.FORTUNE
                        } else {
                            it.detailSection
                        },
                        fortuneObservationDate = if (shouldOpenSavedCase) {
                            "%04d-%02d-%02d".format(
                                savedObservation.observedAt.year,
                                savedObservation.observedAt.month,
                                savedObservation.observedAt.day,
                            )
                        } else {
                            it.fortuneObservationDate
                        },
                        fortuneObservationTime = if (shouldOpenSavedCase) {
                            "%02d:%02d".format(
                                savedObservation.observedAt.hour,
                                savedObservation.observedAt.minute,
                            )
                        } else {
                            it.fortuneObservationTime
                        },
                        fortunePosition = if (shouldOpenSavedCase) {
                            savedObservation.cached?.position
                        } else {
                            it.fortunePosition
                        },
                        professionalFortunePosition = if (shouldOpenSavedCase) {
                            savedObservation.cached?.professionalPosition
                        } else {
                            it.professionalFortunePosition
                        },
                        fortunePositionError = if (shouldOpenSavedCase) null else it.fortunePositionError,
                        fortunePositionLoading = if (shouldOpenSavedCase) {
                            savedObservation.cached == null
                        } else {
                            it.fortunePositionLoading
                        },
                        detailLoading = false,
                        detailError = null,
                        message = "命例已完成排盘并保存。",
                    )
                }
                refreshCasesFromCache()
            }
            else -> showPreparedCaseSaveFailure(attempt, result)
        }
    }

    private fun showPreparedCaseSaveFailure(
        attempt: PreparedCaseSaveAttempt,
        result: CreateCaseResult,
    ) {
        mutableState.update {
            val showingPendingDetail =
                it.destination == AppDestination.CaseDetail(attempt.pendingCase.id) &&
                    it.detail?.id == attempt.pendingCase.id &&
                    it.detailIsTransient
            if (!showingPendingDetail) return@update it.copy(saving = false)
            when (result) {
                is CreateCaseResult.ValidationFailed -> it.copy(
                    saving = false,
                    detailSaveError = result.message,
                    detailSaveDialogVisible = true,
                    detailSavePending = false,
                    detailLoading = false,
                )
                is CreateCaseResult.DuplicateCandidates -> it.copy(
                    saving = false,
                    detailSaveError = "发现疑似重复命例。请核对后决定是否仍保留两份。",
                    detailSaveDialogVisible = true,
                    detailSavePending = false,
                    detailLoading = false,
                    duplicateCandidates = result.candidates,
                )
                is CreateCaseResult.AlreadyExists -> it.copy(
                    saving = false,
                    detailSaveError = "保存冲突：该命例已经存在，未覆盖原记录。",
                    detailSaveDialogVisible = true,
                    detailSavePending = false,
                    detailLoading = false,
                )
                is CreateCaseResult.RevisionConflict -> it.copy(
                    saving = false,
                    detailSaveError = "保存冲突：命例已被更新，未覆盖较新的记录。",
                    detailSaveDialogVisible = true,
                    detailSavePending = false,
                    detailLoading = false,
                )
                is CreateCaseResult.StorageFailed -> it.copy(
                    saving = false,
                    detailSaveError = result.message,
                    detailSaveDialogVisible = true,
                    detailSavePending = false,
                    detailLoading = false,
                )
                is CreateCaseResult.TimeZoneChoiceRequired,
                is CreateCaseResult.CalculationFailed,
                is CreateCaseResult.Created,
                -> it
            }
        }
    }

    fun retryPreparedCaseSave(allowDuplicate: Boolean = false) {
        if (mutableState.value.saving || mutableState.value.detailSavePending) return
        val attempt = preparedCaseSaveAttempt ?: return
        if (
            mutableState.value.destination != AppDestination.CaseDetail(attempt.pendingCase.id) ||
            mutableState.value.detail?.id != attempt.pendingCase.id ||
            !mutableState.value.detailIsTransient
        ) return
        val retry = attempt.copy(allowDuplicate = allowDuplicate)
        preparedCaseSaveAttempt = retry
        mutableState.update {
            it.copy(
                saving = true,
                detailSavePending = true,
                detailSaveError = null,
                detailSaveDialogVisible = false,
                duplicateCandidates = emptyList(),
            )
        }
        saveCaseJob = viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                createCase.savePrepared(retry.form, retry.preview, retry.allowDuplicate)
            }
            finishPreparedCaseSave(retry, result)
        }
    }

    fun returnToPreparedCaseForm() {
        val attempt = preparedCaseSaveAttempt ?: return
        val current = mutableState.value
        if (
            current.destination != AppDestination.CaseDetail(attempt.pendingCase.id) ||
            current.detail?.id != attempt.pendingCase.id ||
            !current.detailIsTransient ||
            current.detailSavePending
        ) return
        preparedCaseSaveAttempt = null
        fortunePositionJob?.cancel()
        fortunePrefetchJob?.cancel()
        fortunePositionRequestId += 1
        mutableState.update {
            it.copy(
                destination = navigator.back(),
                detail = null,
                detailIsTransient = false,
                detailSavePending = false,
                detailSaveError = null,
                detailSaveDialogVisible = false,
                detailLoading = false,
                saving = false,
                instantCalculation = null,
                fortunePosition = null,
                professionalFortunePosition = null,
                fortunePositionError = null,
            )
        }
    }

    fun dismissPreparedSaveDialog() {
        mutableState.update {
            if (!it.detailIsTransient || it.detailSaveError == null) it else {
                it.copy(detailSaveDialogVisible = false)
            }
        }
    }

    fun showPreparedSaveDialog() {
        mutableState.update {
            if (!it.detailIsTransient || it.detailSaveError == null) it else {
                it.copy(detailSaveDialogVisible = true)
            }
        }
    }

    fun openDetail(caseId: String) {
        openDetail(caseId, CaseDetailSection.BASIC_INFO)
    }

    fun openDetailFromList(caseId: String) {
        // Navigation is foreground work and must never wait behind Room or fortune
        // calculations. Cancel unrelated speculative work so the selected case owns the
        // calculation lane, then enter immediately and adopt its tracked prefetch snapshot.
        val obsoletePrefetches = synchronized(detailPrefetchJobs) {
            detailPrefetchJobs
                .filterKeys { it != caseId }
                .values
                .also { jobs ->
                    detailPrefetchJobs.keys.retainAll(setOf(caseId))
                }
        }
        obsoletePrefetches.forEach(Job::cancel)
        openDetail(caseId, CaseDetailSection.FORTUNE)
    }

    private suspend fun prewarmProfessionalFortune(
        detail: BaziCase,
    ): ProfessionalObservationSeed {
        val snapshot = detail.calculationSnapshots.asReversed().firstOrNull { it.adopted }
            ?: return ProfessionalObservationSeed(
                calculationSnapshotId = null,
                observedAt = detail.defaultProfessionalObservation(
                    now = LocalDateTime.now(observationClock),
                ).copy(second = 0),
                cached = null,
            )
        val observedAt = detail.defaultProfessionalObservation(
            now = LocalDateTime.now(observationClock),
        ).copy(second = 0)
        val cacheKey = FortunePositionCacheKey(
            caseId = detail.id,
            calculationSnapshotId = snapshot.id,
            observedAt = observedAt,
        )
        cachedFortunePosition(cacheKey)?.let { cached ->
            if (cached.professionalPosition != null) {
                return ProfessionalObservationSeed(snapshot.id, observedAt, cached)
            }
        }
        val resolved = try {
            withTimeout(FORTUNE_POSITION_TIMEOUT_MILLIS) {
                withContext(fortuneCalculationDispatcher) {
                    val professional = professionalFortuneResolver?.locate(snapshot.result, observedAt)
                        ?: return@withContext null
                    professional.position to professional
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        resolved?.let { (basic, professional) ->
            retainFortunePosition(
                cacheKey,
                FortunePositionCacheValue(basic, professional),
            )
        }
        return ProfessionalObservationSeed(
            calculationSnapshotId = snapshot.id,
            observedAt = observedAt,
            cached = cachedFortunePosition(cacheKey),
        )
    }

    private fun openDetail(caseId: String, initialSection: CaseDetailSection) {
        detailOpeningJob?.cancel()
        caseNotesAutoSaveJob?.cancel()
        caseNotesHydrationJob?.cancel()
        if (pendingCaseImage?.facts?.caseId != caseId) {
            pendingCaseImages = emptyList()
        }
        val retainedDetail = retainedCaseDetail(caseId)
        val retainedNotesDraft = retainedCaseNotes(caseId)
        val targetPrefetchJob = activeDetailPrefetchJob(caseId)
        // Opening a case establishes its single default observation context before any of
        // the four pages can ask for a fortune result.  It must not depend on which tab is
        // initially visible, otherwise Basic chart -> Professional chart starts from a stale
        // form default and invites a second visible refresh.
        val retainedObservation = retainedDetail?.let { detail ->
            retainedProfessionalObservation(detail) ?: professionalObservationSeed(detail)
        }
        mutableState.update {
            val preserveUnsavedDraft = retainedDetail != null &&
                it.caseNotesCaseId == caseId &&
                it.caseNotesRevision == retainedDetail.revision &&
                it.caseNotesDraft != it.caseNotesSavedDraft
            val savedNotesDraft = retainedNotesDraft
                ?: if (it.caseNotesCaseId == caseId) it.caseNotesSavedDraft else CaseNotesDraft()
            it.copy(
                destination = navigator.openDetail(caseId),
                detail = retainedDetail,
                detailIsTransient = false,
                detailSection = initialSection,
                detailLoading = retainedDetail == null,
                detailError = null,
                caseNotesCaseId = caseId,
                caseNotesRevision = retainedDetail?.revision,
                caseNotesDraft = if (preserveUnsavedDraft) {
                    it.caseNotesDraft
                } else {
                    retainedNotesDraft ?: CaseNotesDraft()
                },
                caseNotesSavedDraft = savedNotesDraft,
                // An empty retained draft is never trusted as proof that the database
                // has no notes. Keep the notes page guarded until the canonical Room
                // aggregate has been read again.
                caseNotesHydrating = retainedDetail == null ||
                    retainedNotesDraft?.isEffectivelyEmpty() != false,
                caseNotesSaving = false,
                caseNotesSaveError = null,
                fortuneObservationDate = retainedObservation?.observedAt?.let { observedAt ->
                    "%04d-%02d-%02d".format(observedAt.year, observedAt.month, observedAt.day)
                } ?: it.fortuneObservationDate,
                fortuneObservationTime = retainedObservation?.observedAt?.let { observedAt ->
                    "%02d:%02d".format(observedAt.hour, observedAt.minute)
                } ?: it.fortuneObservationTime,
                fortunePosition = retainedObservation?.cached?.position,
                professionalFortunePosition = retainedObservation?.cached?.professionalPosition,
                fortunePositionError = null,
                fortunePositionLoading = initialSection in setOf(
                    CaseDetailSection.BASIC_CHART,
                    CaseDetailSection.FORTUNE,
                ) && retainedObservation?.cached == null,
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
        if (retainedDetail != null && initialSection == CaseDetailSection.FORTUNE) {
            if (retainedObservation?.cached == null && targetPrefetchJob == null) {
                resolveFortunePosition()
            }
        }
        detailOpeningJob = viewModelScope.launch(ioDispatcher) {
            try {
                targetPrefetchJob?.join()
                val prefetchedSnapshot = retainedCaseDetailSnapshot(caseId)
                val prefetchedDetail = prefetchedSnapshot?.case
                if (prefetchedDetail != null) {
                    val prefetchedObservation = retainedProfessionalObservation(prefetchedDetail)
                        ?: professionalObservationSeed(prefetchedDetail)
                    mutableState.update {
                        if (it.destination == AppDestination.CaseDetail(caseId)) {
                            val notesDraft = prefetchedDetail.toCaseNotesDraft()
                            val adoptDetail = it.detail?.id != caseId
                            val adoptObservation =
                                prefetchedObservation.cached?.professionalPosition != null &&
                                    it.professionalFortunePosition == null
                            it.copy(
                                detail = if (adoptDetail) prefetchedDetail else it.detail,
                                detailLoading = false,
                                detailError = null,
                                caseNotesCaseId = caseId,
                                caseNotesRevision = prefetchedDetail.revision,
                                caseNotesDraft = if (adoptDetail) notesDraft else it.caseNotesDraft,
                                caseNotesSavedDraft = if (adoptDetail) {
                                    notesDraft
                                } else {
                                    it.caseNotesSavedDraft
                                },
                                caseNotesHydrating = if (adoptDetail) {
                                    notesDraft.isEffectivelyEmpty()
                                } else {
                                    it.caseNotesHydrating
                                },
                                fortuneObservationDate = if (adoptObservation) {
                                    prefetchedObservation.observedAt.let { observedAt ->
                                    "%04d-%02d-%02d".format(
                                        observedAt.year,
                                        observedAt.month,
                                        observedAt.day,
                                    )
                                    }
                                } else {
                                    it.fortuneObservationDate
                                },
                                fortuneObservationTime = if (adoptObservation) {
                                    prefetchedObservation.observedAt.let { observedAt ->
                                    "%02d:%02d".format(observedAt.hour, observedAt.minute)
                                    }
                                } else {
                                    it.fortuneObservationTime
                                },
                                fortunePosition = if (adoptObservation) {
                                    prefetchedObservation.cached?.position
                                } else {
                                    it.fortunePosition
                                },
                                professionalFortunePosition = if (adoptObservation) {
                                    prefetchedObservation.cached?.professionalPosition
                                } else {
                                    it.professionalFortunePosition
                                },
                                fortunePositionLoading = initialSection == CaseDetailSection.FORTUNE &&
                                    !adoptObservation &&
                                    it.professionalFortunePosition == null,
                            )
                        } else {
                            it
                        }
                    }
                    if (initialSection in setOf(
                            CaseDetailSection.BASIC_CHART,
                            CaseDetailSection.FORTUNE,
                        )
                    ) {
                        if (
                            prefetchedObservation.cached == null &&
                            fortunePositionJob?.isActive != true
                        ) {
                            resolveFortunePosition()
                        }
                    }
                }
                // Retained/prefetched content is the fast first frame, not the source of
                // truth for refresh. Always re-read so deletion/restoration and revisions
                // cannot be hidden by a stale cache entry.
                var detail = caseRepository.findById(caseId)
                val viewWarning = if (detail != null && detail.deletedAt == null) {
                    try {
                        val viewedAt = clock.instant()
                        if (caseRepository.markViewed(caseId, viewedAt)) {
                            detail = detail.copy(lastViewedAt = viewedAt)
                            updateCachedCaseCatalog { catalog ->
                                catalog.map { summary ->
                                    if (summary.id == caseId) {
                                        summary.copy(lastViewedAt = viewedAt)
                                    } else {
                                        summary
                                    }
                                }
                            }
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
                detail?.let(::retainCaseDetail)
                val refreshedObservation = detail?.let(::professionalObservationSeed)
                mutableState.update {
                    if (it.destination != AppDestination.CaseDetail(caseId)) {
                        it
                    } else if (detail == null) {
                        it.copy(
                            detailLoading = false,
                            detailError = "未找到该命例，记录可能已被移除。",
                        )
                    } else {
                        val currentDetail = it.detail
                        val sameRevision = it.detail?.let { currentDetail ->
                            currentDetail.id == detail.id &&
                                currentDetail.revision == detail.revision &&
                                currentDetail.updatedAt == detail.updatedAt &&
                                currentDetail.deletedAt == detail.deletedAt
                        } == true
                        val sameAggregateIgnoringViewedAt = currentDetail?.let { current ->
                            current.id == detail.id &&
                                current.copy(lastViewedAt = detail.lastViewedAt) == detail
                        } == true
                        // 保留用户刚选择的模型版本；标记已查看等后台刷新不能把阅读位置
                        // 悄悄切回最新一份，避免详情页出现可见跳变。
                        val preferredAiCommentaryRecordId = if (it.caseNotesCaseId == caseId) {
                            it.caseNotesDraft.aiCommentaryRecordId
                        } else {
                            null
                        }
                        val notesDraft = detail.toCaseNotesDraft(preferredAiCommentaryRecordId)
                        val preserveUnsavedDraft = it.caseNotesCaseId == caseId &&
                            it.caseNotesRevision == detail.revision &&
                            it.caseNotesDraft != it.caseNotesSavedDraft
                        it.copy(
                            // markViewed changes list metadata only. Replacing the complete
                            // aggregate for that write caused the already-rendered detail page
                            // to recompose and visibly jump. A genuine child/parent change still
                            // replaces the aggregate; a lastViewedAt-only delta does not.
                            detail = if (sameAggregateIgnoringViewedAt) currentDetail else detail,
                            detailLoading = false,
                            caseNotesCaseId = caseId,
                            caseNotesRevision = detail.revision,
                            caseNotesDraft = if (preserveUnsavedDraft) {
                                it.caseNotesDraft
                            } else {
                                notesDraft
                            },
                            caseNotesSavedDraft = notesDraft,
                            caseNotesHydrating = false,
                            caseNotesSaving = false,
                            caseNotesSaveError = null,
                            fortuneObservationDate = if (!sameRevision) {
                                refreshedObservation?.observedAt?.let { observedAt ->
                                    "%04d-%02d-%02d".format(
                                        observedAt.year,
                                        observedAt.month,
                                        observedAt.day,
                                    )
                                } ?: it.fortuneObservationDate
                            } else {
                                it.fortuneObservationDate
                            },
                            fortuneObservationTime = if (!sameRevision) {
                                refreshedObservation?.observedAt?.let { observedAt ->
                                    "%02d:%02d".format(observedAt.hour, observedAt.minute)
                                } ?: it.fortuneObservationTime
                            } else {
                                it.fortuneObservationTime
                            },
                            fortunePosition = if (!sameRevision) {
                                refreshedObservation?.cached?.position
                            } else {
                                it.fortunePosition
                            },
                            professionalFortunePosition = if (!sameRevision) {
                                refreshedObservation?.cached?.professionalPosition
                            } else {
                                it.professionalFortunePosition
                            },
                            fortunePositionLoading = if (
                                !sameRevision && initialSection == CaseDetailSection.FORTUNE
                            ) {
                                refreshedObservation?.cached == null
                            } else {
                                it.fortunePositionLoading
                            },
                            message = viewWarning,
                        )
                    }
                }
                if (
                    detail != null &&
                    mutableState.value.destination == AppDestination.CaseDetail(caseId) &&
                    initialSection in setOf(
                        CaseDetailSection.BASIC_CHART,
                        CaseDetailSection.FORTUNE,
                    )
                ) {
                    if (
                        initialSection != CaseDetailSection.FORTUNE ||
                        (
                            mutableState.value.professionalFortunePosition == null &&
                                fortunePositionJob?.isActive != true
                        )
                    ) {
                        resolveFortunePosition()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    if (it.destination != AppDestination.CaseDetail(caseId)) {
                        it
                    } else if (it.detail?.id == caseId) {
                        it.copy(
                            detailLoading = false,
                            detailError = null,
                            message = "详情刷新失败，已保留上次打开的内容。",
                        )
                    } else {
                        it.copy(
                            detailLoading = false,
                            detailError = "命例详情读取失败，请返回列表后重试。",
                        )
                    }
                }
            }
        }
    }

    fun openObjectiveSummary() {
        val detail = mutableState.value.detail ?: return
        mutableState.update {
            it.copy(
                destination = navigator.openObjectiveSummary(detail.id),
                objectiveSummaryLoading = true,
                objectiveSummaryFailure = null,
                objectiveSummaryCopied = false,
                message = null,
            )
        }
        loadObjectiveSummary(detail.id)
    }

    fun openExternalAnalysisBridge() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val summaryResult = objectiveSummaryGenerator.generate(
            CaseObjectiveSummaryInput(detail),
        )
        val draft = ExternalAnalysisDraftState()
        val exportResult = if (summaryResult is CaseObjectiveSummaryResult.Success) {
            externalAnalysisBridge.prepareExport(
                ExternalAnalysisExportRequest(
                    summary = summaryResult.summary,
                    selectedGroups = draft.selectedGroups,
                    redactionPolicy = ExternalAnalysisRedactionPolicy(draft.redactionEnabled),
                ),
            )
        } else {
            null
        }
        val summary = (summaryResult as? CaseObjectiveSummaryResult.Success)?.summary
        mutableState.update {
            when {
                summaryResult is CaseObjectiveSummaryResult.Rejected -> it.copy(
                    destination = navigator.openExternalAnalysisBridge(detail.id),
                    objectiveSummary = null,
                    externalAnalysisDraft = draft,
                    externalAnalysisPayload = null,
                    externalAnalysisFailure = ExternalAnalysisBridgeFailure(
                        ExternalAnalysisBridgeErrorCode.SUMMARY_UNAVAILABLE,
                        summaryResult.failure.message,
                    ),
                    externalAnalysisCopied = false,
                    externalAnalysisSaving = false,
                    message = null,
                )
                exportResult is ExternalAnalysisExportResult.Success -> it.copy(
                    destination = navigator.openExternalAnalysisBridge(detail.id),
                    objectiveSummary = summary,
                    externalAnalysisDraft = draft,
                    externalAnalysisPayload = exportResult.payload,
                    externalAnalysisFailure = null,
                    externalAnalysisCopied = false,
                    externalAnalysisSaving = false,
                    message = null,
                )
                exportResult is ExternalAnalysisExportResult.Rejected -> it.copy(
                    destination = navigator.openExternalAnalysisBridge(detail.id),
                    objectiveSummary = summary,
                    externalAnalysisDraft = draft,
                    externalAnalysisPayload = null,
                    externalAnalysisFailure = exportResult.failure,
                    externalAnalysisCopied = false,
                    externalAnalysisSaving = false,
                    message = null,
                )
                else -> it
            }
        }
    }

    fun openMasterCommentaryCandidates(recordId: String) {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val record = detail.textRecords.firstOrNull { it.id == recordId } ?: return
        mutableState.update {
            it.copy(
                destination = navigator.openMasterCommentaryCandidates(detail.id, record.id),
                commentaryCandidateSet = null,
                commentaryCandidateFailure = null,
                commentaryCandidateAdoptionFailure = null,
                commentaryCandidateSavingId = null,
                message = null,
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val result = commentaryCandidateExtractor.extract(
                MasterCommentaryCandidateExtractionInput(
                    sourceRecordId = record.id,
                    sourceRecordType = record.type,
                    sourceContent = record.content,
                    sourceRevision = detail.sourceRecordRevision(record.id),
                ),
            )
            mutableState.update {
                if (
                    it.destination != AppDestination.MasterCommentaryCandidates(
                        detail.id,
                        record.id,
                    )
                ) {
                    it
                } else {
                    when (result) {
                        is MasterCommentaryCandidateExtractionResult.Success -> it.copy(
                            commentaryCandidateSet = result.candidateSet,
                            commentaryCandidateFailure = null,
                        )
                        is MasterCommentaryCandidateExtractionResult.Failure -> it.copy(
                            commentaryCandidateSet = null,
                            commentaryCandidateFailure = result.failure,
                        )
                    }
                }
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
        mutableState.update {
            it.copy(
                destination = navigator.openFeedbackThemeCandidates(detail.id, record.id),
                feedbackThemeCandidateSet = null,
                feedbackThemeCandidateFailure = null,
                feedbackThemeAdoptionFailure = null,
                feedbackThemeSavingId = null,
                message = null,
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val result = feedbackThemeCandidateExtractor.extract(
                FeedbackThemeCandidateExtractionInput(
                    sourceRecordId = record.id,
                    sourceRecordType = record.type,
                    sourceContent = record.content,
                    sourceRevision = detail.sourceRecordRevision(record.id),
                ),
            )
            mutableState.update {
                if (
                    it.destination != AppDestination.FeedbackThemeCandidates(
                        detail.id,
                        record.id,
                    )
                ) {
                    it
                } else {
                    when (result) {
                        is FeedbackThemeCandidateExtractionResult.Success -> it.copy(
                            feedbackThemeCandidateSet = result.candidateSet,
                            feedbackThemeCandidateFailure = null,
                        )
                        is FeedbackThemeCandidateExtractionResult.Failure -> it.copy(
                            feedbackThemeCandidateSet = null,
                            feedbackThemeCandidateFailure = result.failure,
                        )
                    }
                }
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
                withContext(ioDispatcher) {
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

    fun setExternalAnalysisGroupSelected(
        group: ExternalAnalysisFieldGroup,
        selected: Boolean,
    ) {
        mutableState.update {
            val groups = if (selected) {
                it.externalAnalysisDraft.selectedGroups + group
            } else {
                it.externalAnalysisDraft.selectedGroups - group
            }
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    selectedGroups = groups,
                    exportConfirmed = false,
                    importConfirmed = false,
                ),
                externalAnalysisCopied = false,
                externalAnalysisFailure = null,
            )
        }
        regenerateExternalAnalysisPayload()
    }

    fun setExternalAnalysisRedaction(enabled: Boolean) {
        mutableState.update {
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    redactionEnabled = enabled,
                    exportConfirmed = false,
                    importConfirmed = false,
                ),
                externalAnalysisCopied = false,
                externalAnalysisFailure = null,
            )
        }
        regenerateExternalAnalysisPayload()
    }

    fun setExternalAnalysisExportConfirmed(confirmed: Boolean) {
        mutableState.update {
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    exportConfirmed = confirmed,
                ),
                externalAnalysisFailure = null,
            )
        }
    }

    fun updateExternalAnalysisProvider(value: String) {
        if (value == mutableState.value.externalAnalysisDraft.providerName) return
        mutableState.update {
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    providerName = value,
                    importConfirmed = false,
                ),
                externalAnalysisFailure = null,
            )
        }
    }

    fun updateExternalAnalysisModel(value: String) {
        if (value == mutableState.value.externalAnalysisDraft.modelName) return
        mutableState.update {
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    modelName = value,
                    importConfirmed = false,
                ),
                externalAnalysisFailure = null,
            )
        }
    }

    fun updateExternalAnalysisResult(value: String) {
        if (value == mutableState.value.externalAnalysisDraft.resultText) return
        mutableState.update {
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    resultText = value,
                    importConfirmed = false,
                ),
                externalAnalysisFailure = null,
            )
        }
    }

    fun setExternalAnalysisImportConfirmed(confirmed: Boolean) {
        mutableState.update {
            it.copy(
                externalAnalysisDraft = it.externalAnalysisDraft.copy(
                    importConfirmed = confirmed,
                ),
                externalAnalysisFailure = null,
            )
        }
    }

    fun copyExternalAnalysisPayload(writeToClipboard: (String) -> Boolean) {
        val current = mutableState.value
        val payload = current.externalAnalysisPayload
        if (!current.externalAnalysisDraft.exportConfirmed) {
            setExternalAnalysisFailure(
                ExternalAnalysisBridgeErrorCode.CONFIRMATION_REQUIRED,
                "请先确认预览中的字段和脱敏状态。",
            )
            return
        }
        if (payload == null) {
            setExternalAnalysisFailure(
                ExternalAnalysisBridgeErrorCode.SUMMARY_UNAVAILABLE,
                "外部分析材料尚未生成，请检查字段选择。",
            )
            return
        }
        val failure = try {
            if (writeToClipboard(payload.copyText)) {
                null
            } else {
                ExternalAnalysisBridgeFailure(
                    ExternalAnalysisBridgeErrorCode.CLIPBOARD_UNAVAILABLE,
                    "系统剪贴板不可用，材料仍保留在当前页面。",
                )
            }
        } catch (_: Exception) {
            ExternalAnalysisBridgeFailure(
                ExternalAnalysisBridgeErrorCode.COPY_FAILED,
                "复制外部分析材料失败，材料仍保留在当前页面。",
            )
        }
        mutableState.update {
            if (failure == null) {
                it.copy(
                    externalAnalysisCopied = true,
                    externalAnalysisFailure = null,
                    message = "材料已复制；App 没有发送数据，请自行选择可信外部工具。",
                )
            } else {
                it.copy(
                    externalAnalysisCopied = false,
                    externalAnalysisFailure = failure,
                )
            }
        }
    }

    fun saveExternalAnalysisResult() {
        val current = mutableState.value
        val detail = current.detail ?: run {
            setExternalAnalysisFailure(
                ExternalAnalysisBridgeErrorCode.SUMMARY_UNAVAILABLE,
                "命例详情仍在恢复，请稍候再保存。",
            )
            return
        }
        val summary = current.objectiveSummary
        val payload = current.externalAnalysisPayload
        if (summary == null || payload == null || current.externalAnalysisSaving) {
            if (payload == null) {
                setExternalAnalysisFailure(
                    ExternalAnalysisBridgeErrorCode.SUMMARY_UNAVAILABLE,
                    "请先生成当前命例的外部分析材料。",
                )
            }
            return
        }
        val prepared = externalAnalysisBridge.prepareImport(
            ExternalAnalysisImportRequest(
                currentSummary = summary,
                payload = payload,
                providerName = current.externalAnalysisDraft.providerName,
                modelName = current.externalAnalysisDraft.modelName,
                resultText = current.externalAnalysisDraft.resultText,
                userConfirmedExternalSource =
                    current.externalAnalysisDraft.importConfirmed,
            ),
        )
        if (prepared is ExternalAnalysisImportResult.Rejected) {
            mutableState.update {
                it.copy(
                    externalAnalysisFailure = prepared.failure,
                    externalAnalysisSaving = false,
                )
            }
            return
        }
        val backfill = (prepared as ExternalAnalysisImportResult.Ready).draft
        viewModelScope.launch {
            mutableState.update {
                it.copy(externalAnalysisSaving = true, externalAnalysisFailure = null)
            }
            val result = textRecords.save(
                caseId = detail.id,
                expectedRevision = detail.revision,
                recordId = null,
                draft = TextRecordDraft(
                    type = CaseTextRecordType.ANALYSIS,
                    content = backfill.content,
                    analysisCategory = backfill.analysisCategory,
                    sourceType = TextRecordSourceType.EXTERNAL_AI,
                ),
            )
            when (result) {
                is CaseMutationResult.Saved -> {
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
                                externalAnalysisSaving = false,
                                externalAnalysisFailure = ExternalAnalysisBridgeFailure(
                                    ExternalAnalysisBridgeErrorCode.STORAGE_FAILED,
                                    "外部分析已保存，但详情刷新失败。请返回后重新打开命例。",
                                ),
                            )
                        } else {
                            it.copy(
                                destination = navigator.back(),
                                detail = refreshed,
                                detailSection = CaseDetailSection.RECORDS,
                                externalAnalysisDraft = ExternalAnalysisDraftState(),
                                externalAnalysisPayload = null,
                                externalAnalysisFailure = null,
                                externalAnalysisCopied = false,
                                externalAnalysisSaving = false,
                                message = "已保存为带来源标记的外部分析记录；本机算法结果未改变。",
                            )
                        }
                    }
                    refreshCases()
                }
                is CaseMutationResult.RevisionConflict -> setExternalAnalysisFailure(
                    ExternalAnalysisBridgeErrorCode.STALE_PAYLOAD,
                    "命例已有较新修订（${result.actualRevision}），请重新生成材料后再回填。",
                )
                is CaseMutationResult.ValidationFailed -> setExternalAnalysisFailure(
                    ExternalAnalysisBridgeErrorCode.RESULT_EMPTY,
                    result.message,
                )
                CaseMutationResult.NotFound -> setExternalAnalysisFailure(
                    ExternalAnalysisBridgeErrorCode.SUMMARY_UNAVAILABLE,
                    "命例已不存在，未保存外部分析。",
                )
                is CaseMutationResult.StorageFailed -> setExternalAnalysisFailure(
                    ExternalAnalysisBridgeErrorCode.STORAGE_FAILED,
                    result.message,
                )
                is CaseMutationResult.DuplicateCandidates,
                is CaseMutationResult.TimeZoneChoiceRequired,
                is CaseMutationResult.CalculationFailed,
                -> setExternalAnalysisFailure(
                    ExternalAnalysisBridgeErrorCode.STORAGE_FAILED,
                    "保存外部分析时出现不适用的内部状态，命例未被修改。",
                )
            }
        }
    }

    private fun regenerateExternalAnalysisPayload() {
        val summary = mutableState.value.objectiveSummary ?: return
        val draft = mutableState.value.externalAnalysisDraft
        val result = externalAnalysisBridge.prepareExport(
            ExternalAnalysisExportRequest(
                summary = summary,
                selectedGroups = draft.selectedGroups,
                redactionPolicy = ExternalAnalysisRedactionPolicy(draft.redactionEnabled),
            ),
        )
        mutableState.update {
            when (result) {
                is ExternalAnalysisExportResult.Success -> it.copy(
                    externalAnalysisPayload = result.payload,
                    externalAnalysisFailure = null,
                )
                is ExternalAnalysisExportResult.Rejected -> it.copy(
                    externalAnalysisPayload = null,
                    externalAnalysisFailure = result.failure,
                )
            }
        }
    }

    private fun setExternalAnalysisFailure(
        code: ExternalAnalysisBridgeErrorCode,
        message: String,
    ) {
        mutableState.update {
            it.copy(
                externalAnalysisSaving = false,
                externalAnalysisCopied = false,
                externalAnalysisFailure = ExternalAnalysisBridgeFailure(code, message),
            )
        }
    }

    fun selectDetailSection(section: CaseDetailSection) {
        if (mutableState.value.detailSection == section) return
        mutableState.update {
            it.copy(detailSection = section)
        }
        // A tab switch is presentation-only.  Resetting the professional observation here
        // discarded an already rendered chart whenever the user returned from another tab,
        // then re-entered the loading card while the same chart was calculated again.  The
        // default observation is established only when a different case is opened (or when
        // the user explicitly chooses a new time), never while moving among its four pages.
        if (
            section == CaseDetailSection.BASIC_CHART &&
            mutableState.value.fortunePosition == null
        ) {
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
                fortunePositionLoading = true,
            )
        }
        resolveFortunePosition()
    }

    fun updateFortuneObservation(
        date: String,
        time: String,
    ) {
        if (date.length > 10 || date.any { !it.isDigit() && it != '-' }) return
        if (time.length > 5 || time.any { !it.isDigit() && it != ':' }) return
        mutableState.update {
            it.copy(
                fortuneObservationDate = date,
                fortuneObservationTime = time,
                fortunePositionError = null,
                fortunePositionLoading = true,
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
                fortunePositionLoading = true,
            )
        }
        resolveFortunePosition()
    }

    fun selectFortuneObservation(value: CivilDateTime) {
        updateFortuneObservation(value)
        resolveFortunePosition()
    }

    fun selectProfessionalFortuneObservation(
        selection: ProfessionalFortuneSelection,
    ) {
        resolveFortunePosition(selection)
    }

    private fun updateFortuneObservation(value: CivilDateTime) {
        mutableState.update {
            it.copy(
                fortuneObservationDate = "%04d-%02d-%02d".format(
                    value.year,
                    value.month,
                    value.day,
                ),
                fortuneObservationTime = "%02d:%02d".format(value.hour, value.minute),
                fortunePositionError = null,
            )
        }
    }

    fun locateFortuneToday() {
        val now = LocalDateTime.now(observationClock)
        selectFortuneObservation(
            CivilDateTime(
                now.year,
                now.monthValue,
                now.dayOfMonth,
                now.hour,
                now.minute,
                0,
            ),
        )
    }

    /**
     * 专业细盘每次进入都从一个可解释的默认观察时刻开始，不能继承上一命例或上次页面的选择。
     * 已故和跨越百年的历史命例不以“今天”制造无意义的超长岁运，固定落在周岁 36。
     */
    private fun resetProfessionalObservation(case: BaziCase) {
        val seed = professionalObservationSeed(case)
        val observedAt = seed.observedAt
        val cached = seed.cached
        mutableState.update {
            it.copy(
                fortuneObservationDate = "%04d-%02d-%02d".format(
                    observedAt.year,
                    observedAt.month,
                    observedAt.day,
                ),
                fortuneObservationTime = "%02d:%02d".format(observedAt.hour, observedAt.minute),
                fortunePosition = cached?.position,
                professionalFortunePosition = cached?.professionalPosition,
                fortunePositionError = null,
                fortunePositionLoading = cached == null,
            )
        }
    }

    private fun professionalObservationSeed(case: BaziCase): ProfessionalObservationSeed {
        val observedAt = case.defaultProfessionalObservation(
            now = LocalDateTime.now(observationClock),
        ).copy(second = 0)
        val adoptedSnapshot = case.calculationSnapshots
            .asReversed()
            .firstOrNull { it.adopted }
        val cached = adoptedSnapshot
            ?.let { snapshot ->
                cachedFortunePosition(
                    FortunePositionCacheKey(
                        caseId = case.id,
                        calculationSnapshotId = snapshot.id,
                        observedAt = observedAt,
                    ),
                )
            }
        return ProfessionalObservationSeed(adoptedSnapshot?.id, observedAt, cached)
    }

    private fun resolveFortunePosition(
        selection: ProfessionalFortuneSelection? = null,
    ) {
        val current = mutableState.value
        val requestId = ++fortunePositionRequestId
        fortunePositionJob?.cancel()
        val detail = current.detail
        val adoptedSnapshot = detail
            ?.calculationSnapshots
            ?.asReversed()
            ?.firstOrNull { it.adopted }
        val result = adoptedSnapshot?.result
        if (detail == null || adoptedSnapshot == null || result == null) {
            mutableState.update {
                it.copy(
                    fortunePosition = null,
                    professionalFortunePosition = null,
                    fortunePositionError = "当前命例没有已采用的计算快照。",
                    fortunePositionLoading = false,
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
                    fortunePositionLoading = false,
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
                    fortunePositionLoading = false,
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
                    fortunePositionLoading = false,
                )
            }
            return
        }
        val observedAt = selection?.observedAt
            ?: com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime(
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                hour = time.hour,
                minute = time.minute,
                second = 0,
            )
        val cacheKey = FortunePositionCacheKey(
            caseId = detail.id,
            calculationSnapshotId = adoptedSnapshot.id,
            observedAt = observedAt,
            selectionLayer = selection?.layer,
        )
        cachedFortunePosition(cacheKey)?.let { cached ->
            mutableState.update {
                it.copy(
                    fortuneObservationDate = "%04d-%02d-%02d".format(
                        observedAt.year,
                        observedAt.month,
                        observedAt.day,
                    ),
                    fortuneObservationTime = "%02d:%02d".format(
                        observedAt.hour,
                        observedAt.minute,
                    ),
                    fortunePosition = cached.position,
                    professionalFortunePosition = cached.professionalPosition,
                    fortunePositionError = null,
                    fortunePositionLoading = false,
                )
            }
            cached.professionalPosition?.let { professional ->
                prefetchProfessionalFortuneSelections(
                    cacheKey = cacheKey,
                    result = result,
                    current = professional,
                )
            }
            return
        }
        fortunePrefetchJob?.cancel()
        mutableState.update { it.copy(fortunePositionLoading = true) }
        fortunePositionJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            if (selection != null) {
                // 连续横向点选时先让最新选中态完成一帧，再只计算最终落点，避免 CPU 任务堆叠。
                delay(PROFESSIONAL_SELECTION_SETTLE_MILLIS)
            }
            val position = try {
                Result.success(
                    withTimeout(FORTUNE_POSITION_TIMEOUT_MILLIS) {
                        withContext(fortuneCalculationDispatcher) {
                            val professional = professionalFortuneResolver?.let { resolver ->
                                val currentProfessional = current.professionalFortunePosition
                                if (selection != null && currentProfessional != null) {
                                    resolver.select(result, currentProfessional, selection)
                                } else {
                                    resolver.locate(result, observedAt)
                                }
                            }
                            val basic = professional?.position
                                ?: requireNotNull(fortunePositionResolver) {
                                    "当前运行环境未配置岁运定位器。"
                                }.locate(result, observedAt)
                            basic to professional
                        }
                    },
                )
            } catch (timeout: TimeoutCancellationException) {
                Result.failure(IllegalStateException("专业细盘生成超时，请返回后重试。", timeout))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            mutableState.update {
                position.fold(
                    onSuccess = { (resolved, professional) ->
                        retainFortunePosition(
                            cacheKey,
                            FortunePositionCacheValue(resolved, professional),
                        )
                        if (
                            requestId != fortunePositionRequestId ||
                            !it.matchesFortuneRequest(cacheKey)
                        ) {
                            it
                        } else {
                            it.copy(
                                fortuneObservationDate = "%04d-%02d-%02d".format(
                                    observedAt.year,
                                    observedAt.month,
                                    observedAt.day,
                                ),
                                fortuneObservationTime = "%02d:%02d".format(
                                    observedAt.hour,
                                    observedAt.minute,
                                ),
                                fortunePosition = resolved,
                                professionalFortunePosition = professional,
                                fortunePositionError = null,
                                fortunePositionLoading = false,
                            )
                        }
                    },
                    onFailure = { error ->
                        if (
                            requestId != fortunePositionRequestId ||
                            !it.matchesFortuneRequest(cacheKey)
                        ) {
                            it
                        } else {
                            it.copy(
                                fortunePosition =
                                    if (selection == null) null else it.fortunePosition,
                                professionalFortunePosition =
                                    if (selection == null) null else it.professionalFortunePosition,
                                fortunePositionError =
                                    error.message ?: "岁运定位失败，请核对观察日期与时间。",
                                fortunePositionLoading = false,
                            )
                        }
                    },
                )
            }
            position.getOrNull()?.second?.let { professional ->
                if (
                    requestId == fortunePositionRequestId &&
                    mutableState.value.matchesFortuneRequest(cacheKey)
                ) {
                    prefetchProfessionalFortuneSelections(
                        cacheKey = cacheKey,
                        result = result,
                        current = professional,
                    )
                }
            }
        }
    }

    private fun prefetchProfessionalFortuneSelections(
        cacheKey: FortunePositionCacheKey,
        result: CalculationResult,
        current: ProfessionalFortunePosition,
    ) {
        val resolver = professionalFortuneResolver ?: return
        if (PREFETCH_ITEMS_PER_FORTUNE_LAYER == 0) return
        val timelines = listOf(
            ProfessionalFortuneLayer.HOURLY to current.hourlyTimeline,
            ProfessionalFortuneLayer.DAILY to current.dailyTimeline,
            ProfessionalFortuneLayer.MONTHLY to current.monthlyTimeline,
            ProfessionalFortuneLayer.ANNUAL to current.annualTimeline,
            ProfessionalFortuneLayer.DECADE to current.decadeTimeline,
        )
        val selections = buildList {
            repeat(PREFETCH_ITEMS_PER_FORTUNE_LAYER) { index ->
                timelines.forEach { (layer, items) ->
                    items.getOrNull(index)?.let { item ->
                        add(ProfessionalFortuneSelection(layer, item.observedAt))
                    }
                }
            }
        }.distinctBy { it.layer to it.observedAt }
        fortunePrefetchJob?.cancel()
        fortunePrefetchJob = viewModelScope.launch(fortunePrefetchDispatcher) {
            delay(FORTUNE_PREFETCH_DELAY_MILLIS)
            selections.forEach { selection ->
                val itemKey = cacheKey.copy(
                    observedAt = selection.observedAt,
                    selectionLayer = selection.layer,
                )
                if (cachedFortunePosition(itemKey) == null) {
                    try {
                        val prefetched = resolver.select(result, current, selection)
                        retainFortunePosition(
                            itemKey,
                            FortunePositionCacheValue(prefetched.position, prefetched),
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // 预取失败不影响当前真实结果，点击时仍可正常计算。
                    }
                }
            }
        }
    }

    fun openEditCase() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        showEditCase(detail)
    }

    fun openEditCase(caseId: String) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            val caseData = try {
                caseRepository.findById(caseId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (caseData == null || caseData.deletedAt != null) {
                mutableState.update {
                    it.copy(mutationError = "命例不存在或已在回收站，无法编辑。")
                }
            } else {
                showEditCase(caseData)
            }
        }
    }

    private fun showEditCase(caseData: BaziCase) {
        mutableState.update {
            it.copy(
                destination = navigator.openEditCase(caseData.id),
                detail = caseData,
                editForm = caseData.toEditableForm(),
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
            val result = withContext(ioDispatcher) {
                birthTimeCandidates.add(
                    caseId = detail.id,
                    expectedRevision = detail.revision,
                    label = label,
                    form = form,
                )
            }
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
            val result = withContext(ioDispatcher) {
                birthTimeCandidates.adopt(
                    caseId = detail.id,
                    expectedRevision = detail.revision,
                    candidateId = candidateId,
                )
            }
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
            val result = withContext(ioDispatcher) {
                editCase(detail.id, detail.revision, form, allowDuplicate)
            }
            finishMutation(
                result = result,
                caseId = detail.id,
                successMessage = "当前命例已更新并重新排盘。",
            )
        }
    }

    fun createEditedCaseCopy() {
        if (mutableState.value.mutationSaving) return
        val form = mutableState.value.editForm
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            when (
                val result = withContext(ioDispatcher) {
                    createCase(form, allowDuplicate = true)
                }
            ) {
                is CreateCaseResult.Created -> {
                    navigator.backToList()
                    mutableState.update {
                        it.copy(
                            mutationSaving = false,
                            duplicateCandidates = emptyList(),
                            message = "已根据当前编辑内容创建副本，原命例未改动。",
                        )
                    }
                    refreshCases()
                    openDetail(result.caseId)
                }
                is CreateCaseResult.ValidationFailed -> mutableState.update {
                    it.copy(mutationSaving = false, mutationError = result.message)
                }
                is CreateCaseResult.TimeZoneChoiceRequired -> mutableState.update {
                    it.copy(
                        editForm = it.editForm.copy(
                            resolvedUtcOffsetSeconds = null,
                            availableUtcOffsetSeconds = result.validUtcOffsetSeconds,
                        ),
                        mutationSaving = false,
                        mutationError = "该出生时间在 ${result.timeZoneId} 出现两次，" +
                            "请选择实际 UTC offset 后再创建副本。",
                    )
                }
                is CreateCaseResult.CalculationFailed -> mutableState.update {
                    it.copy(
                        mutationSaving = false,
                        mutationError = "排盘失败：${result.message}",
                    )
                }
                is CreateCaseResult.DuplicateCandidates -> mutableState.update {
                    it.copy(mutationSaving = false, mutationError = "创建副本失败。")
                }
                is CreateCaseResult.AlreadyExists,
                is CreateCaseResult.RevisionConflict,
                -> mutableState.update {
                    it.copy(mutationSaving = false, mutationError = "副本保存冲突，请重试。")
                }
                is CreateCaseResult.StorageFailed -> mutableState.update {
                    it.copy(mutationSaving = false, mutationError = result.message)
                }
            }
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
                        sourceType = record.sourceType,
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

    fun updateOwnerFeedback(value: String) {
        updateCaseNotesDraft { it.copy(ownerFeedback = value) }
    }

    fun updateMasterCommentary(value: String) {
        updateCaseNotesDraft { it.copy(masterCommentary = value) }
    }

    fun updateManualAiCommentary(value: String) {
        updateCaseNotesDraft { it.copy(aiCommentary = value) }
    }

    /**
     * 只切换当前阅读/编辑的版本，不写数据库，也不把不同模型的内容互相覆盖。
     * 若当前文本仍有未保存修改，先要求完成保存，避免切换时静默丢字。
     */
    fun selectAiCommentaryVersion(recordId: String) {
        val current = mutableState.value
        val detail = current.detail ?: return
        if (
            current.caseNotesCaseId != detail.id ||
            current.caseNotesRevision != detail.revision ||
            current.caseNotesHydrating
        ) {
            ensureCaseNotesHydrated()
            return
        }
        if (current.caseNotesSaving || current.caseNotesDraft != current.caseNotesSavedDraft) {
            mutableState.update {
                it.copy(caseNotesSaveError = "当前版本仍在编辑，请先保存后再切换模型点评。")
            }
            return
        }
        val selected = current.caseNotesDraft.aiCommentaryVersions
            .firstOrNull { it.recordId == recordId }
            ?: return
        if (selected.recordId == current.caseNotesDraft.aiCommentaryRecordId) return
        mutableState.update {
            val switched = it.caseNotesDraft.copy(
                aiCommentary = selected.body,
                aiCommentaryRecordId = selected.recordId,
            )
            it.copy(
                caseNotesDraft = switched,
                caseNotesSavedDraft = it.caseNotesSavedDraft.copy(
                    aiCommentary = selected.body,
                    aiCommentaryRecordId = selected.recordId,
                ),
                caseNotesSaveError = null,
            )
        }
    }

    /**
     * Runtime ownership guard for the notes aggregate. A notes draft is renderable only
     * when both its case id and parent revision match the mounted detail. Any mismatch is
     * rehydrated as one aggregate instead of exposing an empty or cross-case draft.
     */
    fun ensureCaseNotesHydrated() {
        val current = mutableState.value
        val detail = current.detail ?: return
        val detailDraft = detail.toCaseNotesDraft(current.caseNotesDraft.aiCommentaryRecordId)
        val suspiciousEmptySnapshot = current.caseNotesDraft.isEffectivelyEmpty() &&
            current.caseNotesSavedDraft.isEffectivelyEmpty() &&
            !detailDraft.isEffectivelyEmpty()
        val snapshotMatches = current.caseNotesCaseId == detail.id &&
            current.caseNotesRevision == detail.revision &&
            !suspiciousEmptySnapshot
        if (snapshotMatches || current.caseNotesHydrating) return

        caseNotesAutoSaveJob?.cancel()
        caseNotesHydrationJob?.cancel()
        if (current.detailIsTransient) {
            mutableState.update {
                if (it.detail?.id != detail.id) {
                    it
                } else {
                    it.copy(
                        caseNotesCaseId = detail.id,
                        caseNotesRevision = detail.revision,
                        caseNotesDraft = detailDraft,
                        caseNotesSavedDraft = detailDraft,
                        caseNotesHydrating = false,
                        caseNotesSaving = false,
                        caseNotesSaveError = null,
                    )
                }
            }
            return
        }

        mutableState.update {
            if (it.detail?.id == detail.id) {
                it.copy(
                    caseNotesHydrating = true,
                    caseNotesSaving = false,
                    caseNotesSaveError = null,
                )
            } else {
                it
            }
        }
        caseNotesHydrationJob = viewModelScope.launch(ioDispatcher) {
            val refreshed = try {
                caseRepository.findById(detail.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (refreshed != null) retainCaseDetail(refreshed)
            mutableState.update {
                if (it.detail?.id != detail.id) {
                    it
                } else if (refreshed == null) {
                    it.copy(
                        caseNotesHydrating = false,
                        caseNotesSaveError = "断事笔记读取失败，请返回后重新打开命例。",
                    )
                } else {
                    val refreshedDraft = refreshed.toCaseNotesDraft(
                        current.caseNotesDraft.aiCommentaryRecordId,
                    )
                    it.copy(
                        detail = refreshed,
                        caseNotesCaseId = refreshed.id,
                        caseNotesRevision = refreshed.revision,
                        caseNotesDraft = refreshedDraft,
                        caseNotesSavedDraft = refreshedDraft,
                        caseNotesHydrating = false,
                        caseNotesSaving = false,
                        caseNotesSaveError = null,
                    )
                }
            }
        }
    }

    fun openAiCommentary() {
        val detail = mutableState.value.detail ?: return
        if (detail.deletedAt != null) return
        val settings = aiCommentarySettings
        if (settings == null || aiCommentaryGenerator == null) {
            mutableState.update {
                it.copy(message = "当前构建未提供 AI 点评服务。")
            }
            return
        }
        mutableState.update {
            it.copy(
                aiCommentary = AiCommentaryUiState(
                    dialogVisible = true,
                    configs = settings.configs(),
                    selectedProvider = settings.selectedProvider(),
                ),
            )
        }
    }

    fun dismissAiCommentary() {
        if (mutableState.value.aiCommentary.generating || mutableState.value.aiCommentary.saving) return
        mutableState.update { it.copy(aiCommentary = AiCommentaryUiState()) }
    }

    fun openAiServiceMenu() {
        val settings = aiCommentarySettings ?: return
        mutableState.update {
            it.copy(
                aiCommentary = AiCommentaryUiState(
                    serviceMenuVisible = true,
                    configs = settings.configs(),
                    selectedProvider = settings.selectedProvider(),
                    callRecords = aiCommentaryCallLog?.records().orEmpty(),
                ),
            )
        }
    }

    fun openAiServicePage() {
        val settings = aiCommentarySettings ?: return
        mutableState.update {
            it.copy(
                aiCommentary = AiCommentaryUiState(
                    configs = settings.configs(),
                    selectedProvider = settings.selectedProvider(),
                    callRecords = aiCommentaryCallLog?.records().orEmpty(),
                ),
            )
        }
    }

    fun closeAiServiceMenu() {
        mutableState.update { it.copy(aiCommentary = AiCommentaryUiState()) }
    }

    fun openAiCallHistory() {
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(
                    serviceMenuVisible = false,
                    historyVisible = true,
                    callRecords = aiCommentaryCallLog?.records().orEmpty(),
                    error = null,
                ),
            )
        }
    }

    fun closeAiCallHistory() {
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(
                    historyVisible = false,
                    serviceMenuVisible = false,
                ),
            )
        }
    }

    fun openAiCommentarySettings() {
        val settings = aiCommentarySettings ?: return
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(
                    serviceMenuVisible = false,
                    settingsVisible = true,
                    configs = settings.configs(),
                    apiKeys = AiCommentaryProviderPresets.providerIds.associateWith { providerId ->
                        settings.apiKey(providerId).orEmpty()
                    },
                    selectedProvider = settings.selectedProvider(),
                    error = null,
                ),
            )
        }
    }

    fun closeAiCommentarySettings() {
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(
                    settingsVisible = false,
                    serviceMenuVisible = false,
                    apiKeys = emptyMap(),
                ),
            )
        }
    }

    fun saveAiCommentaryProvider(
        config: AiCommentaryProviderConfig,
        apiKey: String?,
    ) {
        val settings = aiCommentarySettings ?: return
        if (config.baseUrl.isBlank() || config.model.isBlank()) {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        error = "模型名称和接口地址不能为空。",
                    ),
                )
            }
            return
        }
        runCatching {
            settings.saveConfig(config, apiKey?.takeIf(String::isNotBlank))
            if (config.enabled) settings.selectProvider(config.providerId)
        }.onSuccess {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        configs = settings.configs(),
                        apiKeys = emptyMap(),
                        selectedProvider = settings.selectedProvider(),
                        settingsVisible = false,
                        serviceMenuVisible = it.aiCommentary.dialogVisible.not(),
                        error = null,
                    ),
                    message = "${config.providerId.displayName} 配置已安全保存在本机。",
                )
            }
        }.onFailure {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        error = "模型配置保存失败，请重试。",
                    ),
                )
            }
        }
    }

    fun selectAiCommentaryProvider(providerId: AiCommentaryProviderId) {
        val settings = aiCommentarySettings ?: return
        settings.selectProvider(providerId)
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(
                    selectedProvider = providerId,
                    generatedDraft = null,
                    editedContent = "",
                    error = null,
                ),
            )
        }
    }

    fun setAiCommentaryPrivacyConfirmed(confirmed: Boolean) {
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(
                    privacyConfirmed = confirmed,
                    error = null,
                ),
            )
        }
    }

    fun generateAiCommentary() {
        val current = mutableState.value
        val detail = current.detail ?: return
        if (current.caseNotesCaseId != detail.id) return
        val feature = current.aiCommentary
        val settings = aiCommentarySettings ?: return
        val generator = aiCommentaryGenerator ?: return
        if (feature.generating || feature.saving) return
        if (current.caseNotesDraft != current.caseNotesSavedDraft) {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        error = "断事笔记还有未保存修改，请先保存后再生成 AI 点评。",
                    ),
                )
            }
            return
        }
        if (!feature.privacyConfirmed) {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        error = "请先确认将脱敏命盘资料及命主反馈（如有）发送给所选模型服务。",
                    ),
                )
            }
            return
        }
        val config = feature.configs[feature.selectedProvider]
        if (config == null || !config.enabled || !config.hasApiKey) {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        settingsVisible = true,
                        error = "请先启用所选服务并填写 API Key。",
                    ),
                )
            }
            return
        }
        val summary = when (
            val result = objectiveSummaryGenerator.generate(CaseObjectiveSummaryInput(detail))
        ) {
            is CaseObjectiveSummaryResult.Success -> result.summary
            is CaseObjectiveSummaryResult.Rejected -> {
                mutableState.update {
                    it.copy(
                        aiCommentary = it.aiCommentary.copy(error = result.failure.message),
                    )
                }
                return
            }
        }
        val prompt = when (
            val result = BaziAiAnalysisPromptContract.prepare(
                BaziAiAnalysisPromptRequest(
                    summary = summary,
                    topic = BaziAiAnalysisTopic.ALL,
                    referenceDate = LocalDate.now(observationClock),
                    hideIdentityAndLocation = true,
                    ownerFeedback = detail.toAiAnalysisOwnerFeedback(),
                ),
            )
        ) {
            is BaziAiAnalysisPromptResult.Success -> result.prompt
            is BaziAiAnalysisPromptResult.Rejected -> {
                mutableState.update {
                    it.copy(
                        aiCommentary = it.aiCommentary.copy(error = result.failure.message),
                    )
                }
                return
            }
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    aiCommentary = it.aiCommentary.copy(
                        generating = true,
                        generatedDraft = null,
                        editedContent = "",
                        error = null,
                    ),
                )
            }
            val requestedAt = clock.instant()
            val startedAtNanos = System.nanoTime()
            val result = generator.generate(
                AiCommentaryGenerationRequest(
                    provider = config,
                    apiKey = settings.apiKey(config.providerId),
                    promptId = prompt.id,
                    promptText = prompt.copyText,
                ),
            )
            val durationMillis = ((System.nanoTime() - startedAtNanos) / 1_000_000L).coerceAtLeast(0L)
            val callRecord = when (result) {
                is AiCommentaryGenerationResult.Success -> AiCommentaryCallRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    requestedAtEpochMillis = requestedAt.toEpochMilli(),
                    durationMillis = durationMillis,
                    providerName = result.draft.providerName,
                    model = result.draft.model,
                    succeeded = true,
                    inputTokens = result.draft.inputTokens,
                    outputTokens = result.draft.outputTokens,
                )
                is AiCommentaryGenerationResult.Failure -> AiCommentaryCallRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    requestedAtEpochMillis = requestedAt.toEpochMilli(),
                    durationMillis = durationMillis,
                    providerName = config.providerId.displayName,
                    model = config.model,
                    succeeded = false,
                    errorSummary = result.message.trim().take(180),
                )
            }
            runCatching { aiCommentaryCallLog?.append(callRecord) }
            mutableState.update {
                when (result) {
                    is AiCommentaryGenerationResult.Success -> it.copy(
                        aiCommentary = it.aiCommentary.copy(
                            generating = false,
                            generatedDraft = result.draft,
                            editedContent = result.draft.content,
                            error = null,
                        ),
                    )
                    is AiCommentaryGenerationResult.Failure -> it.copy(
                        aiCommentary = it.aiCommentary.copy(
                            generating = false,
                            error = result.message,
                        ),
                    )
                }
            }
        }
    }

    fun updateAiCommentaryContent(value: String) {
        mutableState.update {
            it.copy(
                aiCommentary = it.aiCommentary.copy(editedContent = value, error = null),
            )
        }
    }

    fun saveAiCommentary() {
        val current = mutableState.value
        val detail = current.detail ?: return
        val draft = current.aiCommentary.generatedDraft ?: return
        val edited = current.aiCommentary.editedContent.trim()
        if (edited.isEmpty()) {
            mutableState.update {
                it.copy(aiCommentary = it.aiCommentary.copy(error = "AI 点评内容不能为空。"))
            }
            return
        }
        if (current.aiCommentary.saving) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(aiCommentary = it.aiCommentary.copy(saving = true, error = null))
            }
            when (
                val result = textRecords.save(
                    caseId = detail.id,
                    expectedRevision = detail.revision,
                    recordId = null,
                    draft = TextRecordDraft(
                        type = CaseTextRecordType.ANALYSIS,
                        content = draft.toRecordContent(edited),
                        analysisCategory = AnalysisCategory.GENERAL,
                        sourceType = TextRecordSourceType.EXTERNAL_AI,
                    ),
                )
            ) {
                is CaseMutationResult.Saved -> {
                    val refreshed = runCatching { caseRepository.findById(detail.id) }.getOrNull()
                    mutableState.update {
                        if (refreshed == null) {
                            it.copy(
                                aiCommentary = it.aiCommentary.copy(
                                    saving = false,
                                    error = "点评已保存，但详情刷新失败，请重新打开命例。",
                                ),
                            )
                        } else {
                            val notesDraft = refreshed.toCaseNotesDraft()
                            it.copy(
                                detail = refreshed,
                                caseNotesCaseId = refreshed.id,
                                caseNotesRevision = refreshed.revision,
                                caseNotesDraft = notesDraft,
                                caseNotesSavedDraft = notesDraft,
                                caseNotesHydrating = false,
                                aiCommentary = AiCommentaryUiState(),
                                message = "AI 点评已保存，可在断事笔记中切换模型版本对比。",
                            )
                        }
                    }
                    refreshCases()
                }
                is CaseMutationResult.RevisionConflict -> mutableState.update {
                    it.copy(
                        aiCommentary = it.aiCommentary.copy(
                            saving = false,
                            error = "命例已发生变化，请关闭后重新生成或保存。",
                        ),
                    )
                }
                is CaseMutationResult.ValidationFailed -> mutableState.update {
                    it.copy(
                        aiCommentary = it.aiCommentary.copy(saving = false, error = result.message),
                    )
                }
                CaseMutationResult.NotFound -> mutableState.update {
                    it.copy(
                        aiCommentary = it.aiCommentary.copy(saving = false, error = "命例已不存在。"),
                    )
                }
                else -> mutableState.update {
                    it.copy(
                        aiCommentary = it.aiCommentary.copy(
                            saving = false,
                            error = "AI 点评保存失败，当前草稿仍保留。",
                        ),
                    )
                }
            }
        }
    }

    fun addCaseNotesTimeline(
        level: CaseEventTimelineLevel,
        year: Int,
        stemBranch: String,
    ) {
        if (level == CaseEventTimelineLevel.LEGACY || stemBranch.isBlank()) return
        updateCaseNotesDraft { draft ->
            if (draft.timeline.any { it.level == level && it.year == year }) {
                draft
            } else {
                draft.copy(
                    timeline = draft.timeline + CaseNotesTimelineDraft(
                        id = java.util.UUID.randomUUID().toString(),
                        level = level,
                        year = year,
                        stemBranch = stemBranch,
                    ),
                )
            }
        }
    }

    fun updateCaseNotesTimelineContent(eventId: String, value: String) {
        updateCaseNotesDraft { draft ->
            draft.copy(
                timeline = draft.timeline.map { item ->
                    if (item.id == eventId) item.copy(content = value) else item
                },
            )
        }
    }

    fun saveCaseNotes() {
        caseNotesAutoSaveJob?.cancel()
        persistCaseNotes(manual = true)
    }

    private fun updateCaseNotesDraft(transform: (CaseNotesDraft) -> CaseNotesDraft) {
        val current = mutableState.value
        val detail = current.detail ?: return
        if (
            detail.deletedAt != null ||
            current.caseNotesCaseId != detail.id ||
            current.caseNotesRevision != detail.revision ||
            current.caseNotesHydrating
        ) {
            ensureCaseNotesHydrated()
            return
        }
        mutableState.update {
            it.copy(
                caseNotesDraft = transform(it.caseNotesDraft),
                caseNotesSaveError = null,
            )
        }
        scheduleCaseNotesAutoSave()
    }

    private fun scheduleCaseNotesAutoSave() {
        caseNotesAutoSaveJob?.cancel()
        if (mutableState.value.caseNotesDraft == mutableState.value.caseNotesSavedDraft) return
        caseNotesAutoSaveJob = viewModelScope.launch {
            delay(CASE_NOTES_AUTO_SAVE_DELAY_MILLIS)
            persistCaseNotes(manual = false)
        }
    }

    private fun persistCaseNotes(manual: Boolean) {
        val current = mutableState.value
        val detail = current.detail ?: return
        if (
            current.caseNotesCaseId != detail.id ||
            current.caseNotesRevision != detail.revision ||
            current.caseNotesHydrating ||
            current.caseNotesSaving
        ) {
            ensureCaseNotesHydrated()
            return
        }
        val submitted = current.caseNotesDraft
        val canonical = detail.toCaseNotesDraft()
        if (
            submitted.isEffectivelyEmpty() &&
            current.caseNotesSavedDraft.isEffectivelyEmpty() &&
            !canonical.isEffectivelyEmpty()
        ) {
            mutableState.update {
                it.copy(caseNotesSaveError = "检测到笔记内存状态异常，已阻止空内容覆盖并重新读取。")
            }
            ensureCaseNotesHydrated()
            return
        }
        if (!manual && submitted == current.caseNotesSavedDraft) return
        viewModelScope.launch {
            mutableState.update {
                if (it.detail?.id == detail.id && it.caseNotesCaseId == detail.id) {
                    it.copy(caseNotesSaving = true, caseNotesSaveError = null)
                } else {
                    it
                }
            }
            val result = caseNotesEditor.save(detail.id, detail.revision, submitted)
            when (result) {
                is CaseMutationResult.Saved -> {
                    val refreshed = try {
                        caseRepository.findById(detail.id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    if (refreshed == null) {
                        mutableState.update {
                            if (it.detail?.id != detail.id || it.caseNotesCaseId != detail.id) {
                                it
                            } else it.copy(
                                caseNotesSaving = false,
                                caseNotesSaveError = "内容已写入，但详情刷新失败。",
                            )
                        }
                    } else {
                        retainCaseDetail(refreshed)
                        val currentAfterSave = mutableState.value
                        val stillCurrent = currentAfterSave.detail?.id == detail.id &&
                            currentAfterSave.caseNotesCaseId == detail.id &&
                            currentAfterSave.caseNotesRevision == detail.revision &&
                            currentAfterSave.caseNotesDraft == submitted
                        val refreshedDraft = refreshed.toCaseNotesDraft(
                            submitted.aiCommentaryRecordId,
                        )
                        mutableState.update {
                            if (it.detail?.id != detail.id || it.caseNotesCaseId != detail.id) {
                                it
                            } else it.copy(
                                detail = refreshed,
                                caseNotesCaseId = refreshed.id,
                                caseNotesRevision = refreshed.revision,
                                caseNotesDraft = if (stillCurrent) refreshedDraft else it.caseNotesDraft,
                                caseNotesSavedDraft = if (stillCurrent) refreshedDraft else submitted,
                                caseNotesSaving = false,
                                caseNotesSaveError = null,
                                caseNotesLastSavedAt = clock.instant(),
                                message = if (manual) "断事笔记已保存。" else it.message,
                            )
                        }
                        refreshCases()
                        if (!stillCurrent) scheduleCaseNotesAutoSave()
                    }
                }
                is CaseMutationResult.ValidationFailed -> mutableState.update {
                    if (it.detail?.id == detail.id && it.caseNotesCaseId == detail.id) {
                        it.copy(caseNotesSaving = false, caseNotesSaveError = result.message)
                    } else it
                }
                is CaseMutationResult.RevisionConflict -> {
                    mutableState.update {
                        if (it.detail?.id != detail.id || it.caseNotesCaseId != detail.id) {
                            it
                        } else it.copy(
                            caseNotesSaving = false,
                            caseNotesSaveError = "命例已在别处更新，当前输入未覆盖数据库；请重新打开后核对。",
                        )
                    }
                }
                CaseMutationResult.NotFound -> mutableState.update {
                    if (it.detail?.id == detail.id && it.caseNotesCaseId == detail.id) {
                        it.copy(caseNotesSaving = false, caseNotesSaveError = "命例已不存在。")
                    } else it
                }
                is CaseMutationResult.StorageFailed -> mutableState.update {
                    if (it.detail?.id == detail.id && it.caseNotesCaseId == detail.id) {
                        it.copy(caseNotesSaving = false, caseNotesSaveError = result.message)
                    } else it
                }
                else -> mutableState.update {
                    if (it.detail?.id == detail.id && it.caseNotesCaseId == detail.id) {
                        it.copy(
                            caseNotesSaving = false,
                            caseNotesSaveError = "断事笔记保存失败，请重试。",
                        )
                    } else it
                }
            }
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

    fun closeTransientDetail() {
        if (!mutableState.value.detailIsTransient) {
            backToList()
            return
        }
        if (mutableState.value.detailSavePending) {
            saveCaseJob?.cancel()
        }
        preparedCaseSaveAttempt = null
        fortunePositionJob?.cancel()
        fortunePrefetchJob?.cancel()
        fortunePositionRequestId += 1
        mutableState.update {
            it.copy(
                destination = navigator.back(),
                detail = null,
                detailIsTransient = false,
                detailSavePending = false,
                detailSaveError = null,
                detailSaveDialogVisible = false,
                detailError = null,
                detailLoading = false,
                saving = false,
                instantCalculation = null,
                fortunePosition = null,
                professionalFortunePosition = null,
                fortunePositionError = null,
                message = null,
            )
        }
    }

    fun backToList() {
        caseNotesAutoSaveJob?.cancel()
        if (mutableState.value.detailSavePending) {
            saveCaseJob?.cancel()
        }
        preparedCaseSaveAttempt = null
        mutableState.update {
            it.copy(
                destination = navigator.backToList(),
                detail = null,
                detailIsTransient = false,
                detailSavePending = false,
                detailSaveError = null,
                detailSaveDialogVisible = false,
                detailError = null,
                formError = null,
                saving = false,
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

    fun previewWenzhenWebImport(openInput: () -> InputStream?) {
        if (mutableState.value.wenzhenImportBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    wenzhenImportBusy = true,
                    wenzhenImportError = null,
                    wenzhenImportResult = null,
                )
            }
            val decoded = try {
                withContext(ioDispatcher) {
                    val raw = openInput()?.bufferedReader()?.use { it.readText() }
                        ?: error("无法读取所选文件。")
                    require(raw.length <= WENZHEN_IMPORT_MAX_CHARACTERS) {
                        "问真数据包体积异常，已停止读取。"
                    }
                    wenzhenImporter.decode(raw)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                pendingWenzhenImport = null
                mutableState.update {
                    it.copy(
                        wenzhenImportBusy = false,
                        wenzhenImportPreview = null,
                        wenzhenImportError = error.message ?: "问真数据包检查失败。",
                    )
                }
                return@launch
            }
            pendingWenzhenImport = decoded
            mutableState.update {
                it.copy(
                    wenzhenImportBusy = false,
                    wenzhenImportPreview = wenzhenImporter.preview(decoded),
                    wenzhenImportProgress = null,
                )
            }
        }
    }

    fun cancelWenzhenWebImport() {
        if (mutableState.value.wenzhenImportBusy) return
        pendingWenzhenImport = null
        mutableState.update {
            it.copy(wenzhenImportPreview = null, wenzhenImportError = null)
        }
    }

    fun executeWenzhenWebImport() {
        val data = pendingWenzhenImport ?: return
        if (mutableState.value.wenzhenImportBusy) return
        viewModelScope.launch {
            val total = data.userCases.size + data.celebrityCases.size
            mutableState.update {
                it.copy(
                    wenzhenImportBusy = true,
                    wenzhenImportPreview = null,
                    wenzhenImportProgress = WenzhenImportProgress(0, total, "准备导入"),
                    wenzhenImportError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    wenzhenImporter.import(data) { progress ->
                        if (progress.completed % 10 == 0 || progress.completed == progress.total) {
                            mutableState.update { it.copy(wenzhenImportProgress = progress) }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        wenzhenImportBusy = false,
                        wenzhenImportProgress = null,
                        wenzhenImportError = error.message ?: "问真案例导入失败，可重新选择同一数据包续传。",
                    )
                }
                return@launch
            }
            pendingWenzhenImport = null
            mutableState.update {
                it.copy(
                    wenzhenImportBusy = false,
                    wenzhenImportProgress = null,
                    wenzhenImportResult = result,
                    message = "问真案例导入完成：新增 ${result.created}，已存在 ${result.skipped}。",
                )
            }
            refreshCases()
        }
    }

    fun dismissWenzhenImportResult() {
        mutableState.update {
            it.copy(wenzhenImportResult = null, wenzhenImportError = null)
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
                caseCatalogStore = container.caseCatalogStore,
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
                almanacReader = container.almanacReader,
                singleCaseBundleService = container.singleCaseBundleService,
                caseBackupService = container.caseBackupService,
                backupAttachmentRoot = container.backupAttachmentRoot,
                backupWorkRoot = container.backupWorkRoot,
                calculationPreferenceStore = container.calculationPreferenceStore,
                aiCommentarySettings = container.aiCommentarySettings,
                aiCommentaryGenerator = container.aiCommentaryGenerator,
                aiCommentaryCallLog = container.aiCommentaryCallLog,
                baziEngine = container.baziEngine,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private companion object {
        const val SAVED_UI_STATE_KEY = "stage_two_ui_state"
        const val MIN_EXPORT_PASSWORD_LENGTH = 6
        const val MAX_EXPORT_PASSWORD_LENGTH = 256
        const val SINGLE_CASE_FORMAT_PROBE_BYTES = 64
        const val CASE_NOTES_AUTO_SAVE_DELAY_MILLIS = 2_000L
        const val WENZHEN_IMPORT_MAX_CHARACTERS = 8_000_000
        const val RETAINED_DETAIL_CACHE_SIZE = 12
        const val FORTUNE_POSITION_CACHE_SIZE = 96
        const val PREFETCH_ITEMS_PER_FORTUNE_LAYER = 1
        const val FORTUNE_PREFETCH_DELAY_MILLIS = 1_200L
        const val FORTUNE_POSITION_TIMEOUT_MILLIS = 8_000L
        const val PROFESSIONAL_SELECTION_SETTLE_MILLIS = 20L
    }

    private fun validateExportPassword(password: CharArray): String? = when {
        password.size < MIN_EXPORT_PASSWORD_LENGTH ->
            "密码至少需要 $MIN_EXPORT_PASSWORD_LENGTH 个字符。"
        password.size > MAX_EXPORT_PASSWORD_LENGTH ->
            "密码不能超过 $MAX_EXPORT_PASSWORD_LENGTH 个字符。"
        else -> null
    }
}

private fun StageTwoUiState.matchesFortuneRequest(
    key: FortunePositionCacheKey,
): Boolean {
    if (detail?.id != key.caseId) return false
    return detail.calculationSnapshots
        .asReversed()
        .firstOrNull { it.adopted }
        ?.id == key.calculationSnapshotId
}

private fun PreviewCaseResult.Calculated.toTransientCase(
    caseId: String,
    fallbackAlias: String,
    libraryType: CaseLibraryType,
    now: java.time.Instant,
): BaziCase {
    val candidateId = "$caseId-candidate"
    val snapshotId = "$caseId-snapshot"
    return BaziCase(
        id = caseId,
        alias = alias.ifBlank { fallbackAlias },
        name = name?.let(ExplicitText::present) ?: ExplicitText.absent(),
        sexForFortuneDirection = calculation.normalizedInput.sexForFortuneDirection,
        sourceType = CaseSourceType.MANUAL,
        birthInput = calculation.normalizedInput,
        libraryType = libraryType,
        birthTimeCandidates = listOf(
            BirthTimeCandidate(
                id = candidateId,
                label = "采用时间",
                birthInput = calculation.normalizedInput,
                calculationSnapshotId = snapshotId,
                adopted = true,
                createdAt = now,
            ),
        ),
        calculationSnapshots = listOf(
            CaseCalculationSnapshot(
                id = snapshotId,
                result = calculation,
                adopted = true,
                birthTimeCandidateId = candidateId,
                createdAt = now,
            ),
        ),
        createdAt = now,
        updatedAt = now,
    )
}

private fun BaziCase.toCaseSummary(): CaseSummary {
    val adopted = calculationSnapshots.asReversed().firstOrNull { it.adopted }?.result
    return CaseSummary(
        id = id,
        alias = alias,
        name = name,
        sexForFortuneDirection = sexForFortuneDirection,
        sourceType = sourceType,
        birthInput = birthInput,
        libraryType = libraryType,
        fourPillars = adopted?.fourPillars,
        groups = groups,
        tags = tags,
        isFavorite = isFavorite,
        isPinned = isPinned,
        copiedFromCaseId = copiedFromCaseId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastViewedAt = lastViewedAt,
        deletedAt = deletedAt,
        revision = revision,
        canonicalSolarDateTime = adopted?.calendarConversion?.solarDateTime,
    )
}

private fun BaziCase.defaultProfessionalObservation(
    now: LocalDateTime,
): com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime {
    val current = com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime(
        year = now.year,
        month = now.monthValue,
        day = now.dayOfMonth,
        hour = now.hour,
        minute = now.minute,
        second = 0,
    )
    val adopted = calculationSnapshots.asReversed().firstOrNull { it.adopted }?.result
        ?: return current
    val birth = adopted.calendarConversion?.solarDateTime
        ?: (adopted.normalizedInput.calendarInput as?
            com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar)
            ?.dateTime
        ?: return current
    val birthDateTime = runCatching {
        LocalDateTime.of(
            birth.year,
            birth.month,
            birth.day,
            birth.hour,
            birth.minute,
            birth.second,
        )
    }.getOrNull() ?: return current
    val bornMoreThanOneCenturyAgo = birthDateTime.toLocalDate()
        .isBefore(now.toLocalDate().minusYears(100))
    if (!bornMoreThanOneCenturyAgo && !profile.isExplicitlyDeceased()) return current
    val ageThirtySix = birthDateTime.plusYears(36)
    return com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime(
        year = ageThirtySix.year,
        month = ageThirtySix.monthValue,
        day = ageThirtySix.dayOfMonth,
        hour = ageThirtySix.hour,
        minute = ageThirtySix.minute,
        second = ageThirtySix.second,
    )
}

/** 只识别明确的已故状态，不把“预测某年去世”之类的资料误判为死亡。 */
private fun com.nanzhufeng.nanfengbazi.domain.model.CaseProfile.isExplicitlyDeceased(): Boolean {
    val healthText = health.value?.replace(Regex("\\s+"), "") ?: return false
    return listOf("已故", "已逝", "已去世", "已离世", "已身故", "逝世", "身故", "去世于", "卒于")
        .any(healthText::contains)
}

private fun BaziCase.toCaseNotesDraft(
    preferredAiCommentaryRecordId: String? = null,
): CaseNotesDraft {
    val adopted = calculationSnapshots.asReversed().firstOrNull { it.adopted }?.result
    fun recordBody(type: CaseTextRecordType): String = textRecords
        .filter { it.type == type }
        .joinToString("\n\n") { it.content }
    val timeline = events.mapNotNull { event ->
        val year = event.year ?: return@mapNotNull null
        val level = when (event.timelineLevel) {
            CaseEventTimelineLevel.LEGACY -> CaseEventTimelineLevel.ANNUAL
            else -> event.timelineLevel
        }
        val stemBranch = event.stemBranch
            ?: when (level) {
                CaseEventTimelineLevel.DECADE -> adopted?.decadeFortunes
                    ?.firstOrNull { year in it.startYear..it.endYear }
                    ?.name
                CaseEventTimelineLevel.ANNUAL -> adopted?.annualFortunes
                    ?.firstOrNull { it.calendarYear == year }
                    ?.name
                CaseEventTimelineLevel.LEGACY -> null
            }
            ?: "时间"
        CaseNotesTimelineDraft(
            id = event.id,
            level = level,
            year = year,
            stemBranch = stemBranch,
            sourceLabel = event.title.orEmpty(),
            status = event.status.orEmpty(),
            content = event.rawText,
        )
    }
    val aiCommentaryRecords = textRecords
        .filter { it.isAiCommentaryRecord() }
    val versionTotals = aiCommentaryRecords
        .groupingBy { it.aiCommentaryVersionLabel() }
        .eachCount()
    val versionLabels = buildMap {
        aiCommentaryRecords
            .sortedBy { it.createdAt }
            .groupBy { it.aiCommentaryVersionLabel() }
            .forEach { (baseLabel, records) ->
                records.forEachIndexed { index, record ->
                    put(
                        record.id,
                        if (versionTotals.getValue(baseLabel) > 1) {
                            "$baseLabel · 第${index + 1}版"
                        } else {
                            baseLabel
                        },
                    )
                }
            }
    }
    val aiCommentaryVersions = aiCommentaryRecords
        .sortedByDescending { it.updatedAt }
        .map { record ->
            AiCommentaryVersion(
                recordId = record.id,
                label = versionLabels.getValue(record.id),
                body = record.aiCommentaryBody(),
            )
        }
    val selectedAiCommentary = aiCommentaryVersions
        .firstOrNull { it.recordId == preferredAiCommentaryRecordId }
        ?: aiCommentaryVersions.firstOrNull()
    return CaseNotesDraft(
        ownerFeedback = recordBody(CaseTextRecordType.OWNER_FEEDBACK),
        masterCommentary = recordBody(CaseTextRecordType.MASTER_COMMENTARY),
        aiCommentary = selectedAiCommentary?.body.orEmpty(),
        aiCommentaryRecordId = selectedAiCommentary?.recordId,
        aiCommentaryVersions = aiCommentaryVersions,
        timeline = timeline,
    )
}

private fun CaseNotesDraft.isEffectivelyEmpty(): Boolean =
    ownerFeedback.isBlank() &&
        masterCommentary.isBlank() &&
        aiCommentary.isBlank() &&
        timeline.isEmpty()

private fun AppDestination.caseIdOrNull(): String? = when (this) {
    is AppDestination.CaseDetail -> caseId
    is AppDestination.CaseObjectiveSummary -> caseId
    is AppDestination.ExternalAnalysisBridge -> caseId
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
    AppDestination.Almanac,
    AppDestination.RecordHub,
    AppDestination.Settings,
    AppDestination.CreateCase,
    AppDestination.ScreenshotImportReview,
    -> null
}

private fun AlmanacError.toAlmanacUserMessage(): String = when (this) {
    is AlmanacError.YearOutOfBounds -> "万年历支持 $minimum–$maximum 年。"
    is AlmanacError.InvalidMonth -> "月份 $month 无效。"
    is AlmanacError.InvalidDay -> "日期 $year-$month-$day 无效。"
    is AlmanacError.InvalidDoubleHour -> "时辰序号 $index 无效。"
    AlmanacError.EngineUnavailable -> "万年历计算失败，请重试。"
}

private fun StageTwoUiState.toSavedStateBundle(): Bundle = Bundle().apply {
    putBundle(
        "destination",
        (if (detailIsTransient) AppDestination.Almanac else destination).toSavedStateBundle(),
    )
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
    putInt("almanacYear", almanacYear)
    putInt("almanacMonth", almanacMonth)
    putInt("almanacSelectedDay", almanacSelectedDay)
    putInt("almanacSelectedDoubleHourIndex", almanacSelectedDoubleHourIndex)
    putString("defaultRatHourRule", defaultRatHourRule.name)
    putString("sortOrder", sortOrder.name)
    putString("visibility", visibility.name)
    putString("libraryType", libraryType.name)
    putString("filterSex", advancedFilter.sex?.name)
    putString("filterGanZhi", advancedFilter.ganZhi.joinToString(""))
    putString("filterBirthRegion", advancedFilter.birthRegion)
    putStringArrayList(
        "filterSeasonalStates",
        ArrayList(advancedFilter.seasonalWuxingStates),
    )
    putStringArrayList("filterShenSha", ArrayList(advancedFilter.shenSha))
    putString("filterYearPillar", advancedFilter.fourPillars.year.savedValue())
    putString("filterMonthPillar", advancedFilter.fourPillars.month.savedValue())
    putString("filterDayPillar", advancedFilter.fourPillars.day.savedValue())
    putString("filterHourPillar", advancedFilter.fourPillars.hour.savedValue())
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
    putBundle("externalAnalysisDraft", externalAnalysisDraft.toSavedStateBundle())
}

private fun Bundle.toStageTwoUiState(): StageTwoUiState {
    val destination = getBundle("destination")?.toAppDestination()
        ?: AppDestination.CreateCase
    return StageTwoUiState(
        destination = destination,
        query = getString("query").orEmpty(),
        selectedGroupId = getString("selectedGroupId"),
        selectedTagId = getString("selectedTagId"),
        advancedFilter = CaseAdvancedFilter(
            sex = enumValueOrNull<SexForFortuneDirection>(getString("filterSex")),
            ganZhi = getString("filterGanZhi").orEmpty().toSet(),
            fourPillars = FourPillarsSearchFilter(
                year = getString("filterYearPillar").toPillarCharacterFilter(),
                month = getString("filterMonthPillar").toPillarCharacterFilter(),
                day = getString("filterDayPillar").toPillarCharacterFilter(),
                hour = getString("filterHourPillar").toPillarCharacterFilter(),
            ),
            birthRegion = getString("filterBirthRegion").orEmpty(),
            seasonalWuxingStates = getStringArrayList("filterSeasonalStates")?.toSet().orEmpty(),
            shenSha = getStringArrayList("filterShenSha")?.toSet().orEmpty(),
        ),
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
        almanacYear = getInt("almanacYear").takeIf { it != 0 } ?: LocalDate.now().year,
        almanacMonth = getInt("almanacMonth").takeIf { it in 1..12 }
            ?: LocalDate.now().monthValue,
        almanacSelectedDay = getInt("almanacSelectedDay").takeIf { it in 1..31 }
            ?: LocalDate.now().dayOfMonth,
        almanacSelectedDoubleHourIndex =
            getInt("almanacSelectedDoubleHourIndex").takeIf { it in 0..11 } ?: 0,
        almanacLoading = destination == AppDestination.Almanac,
        defaultRatHourRule = enumValueOrDefault(
            getString("defaultRatHourRule"),
            RatHourRule.TYME_DEFAULT,
        ),
        sortOrder = enumValueOrDefault(
            getString("sortOrder"),
            CaseSortOrder.NAME_ASC,
        ),
        visibility = enumValueOrDefault(
            getString("visibility"),
            CaseVisibility.ACTIVE,
        ),
        libraryType = enumValueOrDefault(
            getString("libraryType"),
            CaseLibraryType.USER,
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
        externalAnalysisDraft = getBundle("externalAnalysisDraft")
            ?.toExternalAnalysisDraftState()
            ?: ExternalAnalysisDraftState(),
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

private fun PillarCharacterFilter.savedValue(): String =
    listOf(
        stem?.toString().orEmpty(),
        branch?.toString().orEmpty(),
        stemTenGod.orEmpty(),
        branchTenGod.orEmpty(),
    ).joinToString("|")

private fun String?.toPillarCharacterFilter(): PillarCharacterFilter {
    if (this.isNullOrBlank()) return PillarCharacterFilter()
    if ('|' !in this) {
        return PillarCharacterFilter(
            stem = getOrNull(0)?.takeUnless { it == '-' },
            branch = getOrNull(1)?.takeUnless { it == '-' },
        )
    }
    val parts = split('|', limit = 4)
    return PillarCharacterFilter(
        stem = parts.getOrNull(0)?.firstOrNull(),
        branch = parts.getOrNull(1)?.firstOrNull(),
        stemTenGod = parts.getOrNull(2)?.takeIf(String::isNotBlank),
        branchTenGod = parts.getOrNull(3)?.takeIf(String::isNotBlank),
    )
}

private fun AppDestination.toSavedStateBundle(): Bundle = Bundle().apply {
    when (this@toSavedStateBundle) {
        AppDestination.CaseList -> putString("type", "case_list")
        AppDestination.CaseComparison -> putString("type", "case_comparison")
        AppDestination.FourPillarsLookup -> putString("type", "four_pillars_lookup")
        AppDestination.Almanac -> putString("type", "almanac")
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
        is AppDestination.ExternalAnalysisBridge -> {
            putString("type", "external_analysis_bridge")
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
        "case_list", "record_hub" -> AppDestination.CaseList
        "case_comparison" -> AppDestination.CaseComparison
        "four_pillars_lookup" -> AppDestination.FourPillarsLookup
        "almanac" -> AppDestination.Almanac
        "settings" -> AppDestination.Settings
        "create_case" -> AppDestination.CreateCase
        "screenshot_review" -> AppDestination.ScreenshotImportReview
        "case_detail" -> caseId?.let(AppDestination::CaseDetail)
        "case_objective_summary" -> caseId?.let(AppDestination::CaseObjectiveSummary)
        "external_analysis_bridge" -> caseId?.let(AppDestination::ExternalAnalysisBridge)
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
        else -> AppDestination.CreateCase
    } ?: AppDestination.CreateCase
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
    putString("groupId", groupId)
    putString("libraryType", libraryType.name)
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
    timeZoneId = getString("timeZoneId") ?: BaziTimeZoneDefaults.BEIJING_IANA_ID,
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
    groupId = getString("groupId"),
    libraryType = enumValueOrDefault(
        getString("libraryType"),
        CaseLibraryType.USER,
    ),
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
    putString("sourceType", sourceType.name)
}

private fun Bundle.toTextRecordDraft(): TextRecordDraft = TextRecordDraft(
    type = enumValueOrDefault(getString("type"), CaseTextRecordType.NOTE),
    content = getString("content").orEmpty(),
    analysisCategory = enumValueOrDefault(
        getString("analysisCategory"),
        AnalysisCategory.GENERAL,
    ),
    sourceType = enumValueOrDefault(
        getString("sourceType"),
        TextRecordSourceType.USER,
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

private fun ExternalAnalysisDraftState.toSavedStateBundle(): Bundle = Bundle().apply {
    putStringArrayList(
        "selectedGroups",
        ArrayList(selectedGroups.map { it.name }),
    )
    putBoolean("redactionEnabled", redactionEnabled)
    putString("providerName", providerName)
    putString("modelName", modelName)
    putString("resultText", resultText)
    // 主动确认属于一次性授权，生命周期重建后必须重新确认。
}

private fun Bundle.toExternalAnalysisDraftState(): ExternalAnalysisDraftState {
    val groups = getStringArrayList("selectedGroups")
        ?.mapNotNull { enumValueOrNull<ExternalAnalysisFieldGroup>(it) }
        ?.toSet()
        ?: ExternalAnalysisFieldGroup.entries.toSet()
    return ExternalAnalysisDraftState(
        selectedGroups = groups,
        redactionEnabled = getBoolean("redactionEnabled", true),
        providerName = getString("providerName").orEmpty(),
        modelName = getString("modelName").orEmpty(),
        resultText = getString("resultText").orEmpty(),
        exportConfirmed = false,
        importConfirmed = false,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
    value?.let { candidate ->
        enumValues<T>().firstOrNull { it.name == candidate }
    }

private fun String.toSafeImageFileName(): String = toSafeImageFileNames().first()

private fun String.toSafeImageFileNames(): List<String> {
    val safeStem = trim()
        .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
        .trim('.', ' ')
        .take(48)
        .ifBlank { "命例" }
    return listOf(
        "${safeStem}_南枫八字命盘.png",
        "${safeStem}_南枫八字断事笔记.png",
    )
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
        libraryType = libraryType,
    )
}
