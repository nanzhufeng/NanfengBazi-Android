package com.nanzhufeng.nanfengbazi

import android.os.Bundle
import android.util.Log
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
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityAnalyzer
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityReport
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityRecord
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityResult
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
    data object BaziCompatibility : AppDestination
    data object BaziCompatibilityReport : AppDestination
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

    fun openBaziCompatibility(): AppDestination = push(AppDestination.BaziCompatibility)

    fun openBaziCompatibilityReport(): AppDestination = push(AppDestination.BaziCompatibilityReport)

    fun openCompatibilityParticipantCreate(): AppDestination = push(AppDestination.CreateCase)

    fun openCompatibilityParticipantList(): AppDestination = push(AppDestination.CaseList)

    fun returnToBaziCompatibility(): AppDestination {
        val target = stack.indexOfLast { it == AppDestination.BaziCompatibility }
        if (target >= 0) {
            while (stack.lastIndex > target) stack.removeAt(stack.lastIndex)
        }
        return current
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

            AppDestination.BaziCompatibility -> {
                stack += AppDestination.CreateCase
                stack += destination
            }

            AppDestination.BaziCompatibilityReport -> {
                // 合盘结果本身是本次会话的瞬时阅读页；进程恢复时回到可重新发起的合盘页，
                // 不显示一个缺少双方快照的空结果页。
                stack += AppDestination.CreateCase
                stack += AppDestination.BaziCompatibility
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
    /** Actual repository groups for the active create/edit form. Never use list presentation groups here. */
    val availableFormGroups: List<CaseGroup> = emptyList(),
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
    val compatibilityCandidates: List<CaseSummary> = emptyList(),
    val compatibilityLeftCaseId: String? = null,
    val compatibilityRightCaseId: String? = null,
    val compatibilityReport: BaziCompatibilityReport? = null,
    val compatibilityLoading: Boolean = false,
    val compatibilityError: String? = null,
    val compatibilityParticipantRole: SexForFortuneDirection? = null,
    val compatibilityParticipantSelectionRole: SexForFortuneDirection? = null,
    val compatibilityParticipantReplacementFromReport: Boolean = false,
    val compatibilityHistory: List<BaziCompatibilityRecord> = emptyList(),
    /** Prevent the records page from treating its initial empty list as a real empty state. */
    val compatibilityHistoryLoading: Boolean = false,
    /** Read/write failures keep the existing file and surface recovery instead of pretending history is empty. */
    val compatibilityHistoryError: String? = null,
    val compatibilityHistoryRecordId: String? = null,
    val compatibilityHistoryReplacementRecordId: String? = null,
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
    val curatedCelebrityImportPreview: CuratedCelebrityImportPreview? = null,
    val curatedCelebrityImportBusy: Boolean = false,
    val curatedCelebrityImportProgress: CuratedCelebrityImportProgress? = null,
    val curatedCelebrityImportResult: CuratedCelebrityImportResult? = null,
    val curatedCelebrityImportError: String? = null,
    /** Visible install/update state for the catalog bundled inside the APK. */
    val builtInCelebrityCatalogSyncBusy: Boolean = false,
    val builtInCelebrityCatalogSyncCaseCount: Int = 0,
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
    val displayGroupsByLibrary: Map<CaseLibraryType, List<CaseGroup>>,
    val cases: List<CaseSummary>,
    val batchCases: List<CaseSummary>,
    val recentCases: List<CaseSummary>,
    val allCasesForControls: List<CaseSummary>,
    val libraryCaseCounts: Map<CaseLibraryType, Int>,
    val trashedCaseCount: Int,
    val groupCaseCounts: Map<String, Int>,
)

private fun SexForFortuneDirection.compatibilityRoleName(): String = when (this) {
    SexForFortuneDirection.MAN -> "男方"
    SexForFortuneDirection.WOMAN -> "女方"
}

/**
 * The record toolbar and its rows are one user-visible session.  A projection
 * produced for an earlier session must never be published into a later tab.
 */
internal data class CaseListSession(
    val visibility: CaseVisibility,
    val libraryType: CaseLibraryType,
    val query: String,
    val selectedGroupId: String?,
    val selectedTagId: String?,
    val sortOrder: CaseSortOrder,
    val advancedFilter: CaseAdvancedFilter,
)

internal fun StageTwoUiState.caseListSession() = CaseListSession(
    visibility = visibility,
    libraryType = libraryType,
    query = query,
    selectedGroupId = selectedGroupId,
    selectedTagId = selectedTagId,
    sortOrder = sortOrder,
    advancedFilter = advancedFilter,
)

internal fun StageTwoUiState.acceptsCaseListSession(session: CaseListSession): Boolean =
    caseListSession() == session

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
    private val compatibilityHistoryStore: BaziCompatibilityHistoryStore =
        InMemoryBaziCompatibilityHistoryStore(),
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
    private var compatibilityJob: Job? = null
    /** Invalidates a pending history read whenever a newer history mutation wins. */
    private var compatibilityHistoryGeneration: Long = 0
    private var fourPillarsLookupJob: Job? = null
    private var almanacJob: Job? = null
    private var almanacEntryJob: Job? = null
    private var detailOpeningJob: Job? = null
    private val detailPrefetchJobs = LinkedHashMap<String, Job>()
    private var fortunePositionJob: Job? = null
    private var fortunePrefetchJob: Job? = null
    private var fortunePositionRequestId: Long = 0
    private var saveCaseJob: Job? = null
    private var preparedCaseSaveAttempt: PreparedCaseSaveAttempt? = null
    private var almanacRequestId: Long = 0
    private var pendingAlmanacQuery: AlmanacMonthQuery? = null
    private var warmedAlmanacView: AlmanacMonthView? = null
    private var almanacWarmJob: Job? = null
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
    private var pendingCuratedCelebrityImport: CuratedCelebrityCatalogPackage? = null
    private val curatedCelebrityImporter = CuratedCelebrityImporter(caseRepository, baziEngine, clock)
    private val unifiedCelebrityCatalogImporter = UnifiedCelebrityCatalogImporter(caseRepository, baziEngine, clock)
    private var builtInCelebrityCatalogSyncJob: Job? = null

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
        if (mutableState.value.destination == AppDestination.CaseComparison) {
            loadComparisonWorkspace()
        }
        if (mutableState.value.destination == AppDestination.BaziCompatibility) {
            loadCompatibilityWorkspace()
        }
        if (
            mutableState.value.destination == AppDestination.FourPillarsLookup &&
            mutableState.value.fourPillarsLookupHasSearched
        ) {
            searchFourPillars()
        }
        if (mutableState.value.destination == AppDestination.Almanac) {
            loadAlmanac()
        } else {
            warmCurrentAlmanacView()
        }
        // 根页面初次组成时不会主动切换筛选条件；这里必须加载一次，避免首次打开
        // 记录页显示空列表、只能靠用户手动切换后才出现已有案例。
        // 历史版本曾实现仓储清理却漏掉此入口，导致无断事的“某某/某某数字”占位仍留在用户列表。
        // 清理本身只走可恢复的生命周期软删除，不能误伤有记录、事件或名人案例。
        // The cleanup continuation always performs the first catalog refresh.  Starting a
        // parallel refresh here used two Room snapshots on every cold start and allowed the
        // pre-cleanup projection to briefly win the list session.
        cleanUpLegacyEmptyPlaceholderCases()
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
        val session = current.caseListSession()
        val request = CaseSearchRequest(
            query = session.query,
            groupId = session.selectedGroupId,
            tagId = session.selectedTagId,
            sortOrder = session.sortOrder,
            visibility = session.visibility,
            libraryType = session.libraryType.takeIf {
                session.visibility == CaseVisibility.ACTIVE
            },
            advancedFilter = session.advancedFilter,
        )
        val sharedCache = caseCatalogStore.cached()
        val cachedCatalog = caseSummaryCatalogCache ?: sharedCache?.cases
        val cachedGroups = caseGroupsCache.takeIf { it.isNotEmpty() }
            ?: sharedCache?.groupsByLibrary.orEmpty()
        val needsCatalogLoad = forceCatalogReload || cachedCatalog == null
        searchJob = viewModelScope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            if (!mutableState.value.acceptsCaseListSession(session)) return@launch
            if (cachedCatalog == null) {
                mutableState.update {
                    if (it.acceptsCaseListSession(session)) {
                        it.copy(
                            listLoading = true,
                            listError = null,
                        )
                    } else {
                        it
                    }
                }
            } else {
                mutableState.update {
                    if (it.acceptsCaseListSession(session)) it.copy(listError = null) else it
                }
            }
            try {
                cachedCatalog?.let { catalog ->
                    publishCaseProjection(
                        projection = withContext(ioDispatcher) {
                            projectCaseList(catalog, cachedGroups, session, request)
                        },
                        session = session,
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
                                session,
                                request,
                            )
                        },
                        session = session,
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
                            session,
                            request,
                        )
                    },
                    session = session,
                    clearMissingFormGroup = true,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    if (!it.acceptsCaseListSession(session)) {
                        it
                    } else if (it.cases.isNotEmpty() || cachedCatalog != null) {
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
        session: CaseListSession,
        request: CaseSearchRequest,
    ): CaseListProjection {
        val useUnifiedCelebrityPresentation = session.visibility == CaseVisibility.ACTIVE &&
            session.libraryType == CaseLibraryType.CELEBRITY
        val presentationCatalog = if (useUnifiedCelebrityPresentation) {
            catalog.toUnifiedCelebrityPresentationCatalog()
        } else {
            catalog
        }
        val presentationGroupsByLibrary = if (useUnifiedCelebrityPresentation) {
            groupsByLibrary + (CaseLibraryType.CELEBRITY to unifiedCelebrityPresentationGroups)
        } else {
            groupsByLibrary
        }
        val presentationRequest = if (
            useUnifiedCelebrityPresentation &&
            request.groupId !in unifiedCelebrityPresentationGroupIds
        ) {
            request.copy(groupId = null)
        } else {
            request
        }
        val allCasesForControls = presentationCatalog.searchCases(
            CaseSearchRequest(
                visibility = session.visibility,
                libraryType = session.libraryType.takeIf {
                    session.visibility == CaseVisibility.ACTIVE
                },
            ),
        )
        val activeCases = catalog.filter { it.deletedAt == null }
        // The top-level library counters must not depend on which tab happens to be selected.
        // In particular, celebrities always use the same date-corrected, verified-deduplicated
        // catalog as the celebrity list itself.
        val unifiedActiveCelebrityCases = catalog
            .toUnifiedCelebrityPresentationCatalog()
            .count { it.libraryType == CaseLibraryType.CELEBRITY && it.deletedAt == null }
        return CaseListProjection(
            catalog = catalog,
            groupsByLibrary = groupsByLibrary,
            displayGroupsByLibrary = presentationGroupsByLibrary,
            cases = presentationCatalog.searchCases(presentationRequest),
            batchCases = presentationCatalog.searchCases(
                CaseSearchRequest(
                    sortOrder = CaseSortOrder.NAME_ASC,
                    visibility = CaseVisibility.ACTIVE,
                    libraryType = session.libraryType,
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
                when (libraryType) {
                    CaseLibraryType.USER -> activeCases.count { it.libraryType == CaseLibraryType.USER }
                    CaseLibraryType.CELEBRITY -> unifiedActiveCelebrityCases
                }
            },
            trashedCaseCount = catalog.count { it.deletedAt != null },
            groupCaseCounts = allCasesForControls
                .asSequence()
                .flatMap { it.groups.asSequence() }
                .groupingBy { it.id }
                .eachCount(),
        )
    }

/**
 * 同一公开人物可能同时存在于本地资料包与问真网页原始包。列表只展示一个入口，
 * 但不改动任一来源记录；问真条目须先通过姓名和公历生日双重核验才允许合并，
 * 以免把同名人物或日期有冲突的排盘错误去重。
 */
private fun List<CaseSummary>.collapseVerifiedCelebrityDuplicates(): List<CaseSummary> {
    val retainedIndexByIdentity = mutableMapOf<String, Int>()
    val result = mutableListOf<CaseSummary>()
    forEach { candidate ->
        val identity = candidate.verifiedCelebrityIdentityKeyOrNull()
        if (identity == null) {
            result += candidate
            return@forEach
        }
        val retainedIndex = retainedIndexByIdentity[identity]
        if (retainedIndex == null) {
            retainedIndexByIdentity[identity] = result.size
            result += candidate
        } else if (candidate.isPreferredCelebrityPresentationOver(result[retainedIndex])) {
            result[retainedIndex] = candidate
        }
    }
    return result
}

/**
 * 名人的原始来源分组只说明导入出处，不适合继续作为阅读目录。
 * 统一目录只在名人活动列表中生效：每人仅有一个主领域，原始分组仍保留在仓储与详情来源中。
 */
private fun List<CaseSummary>.toUnifiedCelebrityPresentationCatalog(): List<CaseSummary> =
    map { summary ->
        if (summary.libraryType == CaseLibraryType.CELEBRITY && summary.deletedAt == null) {
            summary.withVerifiedCelebrityDatePresentation()
        } else {
            summary
        }
    }.collapseVerifiedCelebrityDuplicates().map { summary ->
        if (summary.libraryType != CaseLibraryType.CELEBRITY || summary.deletedAt != null) {
            summary
        } else {
            summary.copy(groups = listOf(summary.unifiedCelebrityPresentationGroup()))
        }
    }

private fun CaseSummary.withVerifiedCelebrityDatePresentation(): CaseSummary {
    if (sourceType != CaseSourceType.WENZHEN_WEB_IMPORT) return this
    val correction = independentlyVerifiedCelebrityDateConflicts[
        alias.celebrityResearchCanonicalName()
    ]?.authoritativeDate ?: return this
    val actual = (birthInput.calendarInput as? BirthCalendarInput.Solar)?.dateTime ?: return this
    if (
        actual.year == correction.year &&
        actual.month == correction.month &&
        actual.day == correction.day
    ) return this
    return copy(
        birthInput = birthInput.copy(
            calendarInput = BirthCalendarInput.Solar(
                actual.copy(
                    year = correction.year,
                    month = correction.month,
                    day = correction.day,
                ),
            ),
        ),
        canonicalSolarDateTime = canonicalSolarDateTime?.copy(
            year = correction.year,
            month = correction.month,
            day = correction.day,
        ),
    )
}

private val unifiedCelebrityPresentationGroups = listOf(
    CaseGroup("unified-celebrity-emperor", "君主", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-politics", "政界", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-military", "军事", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-business", "商界", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-tech", "科技", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-medicine", "医学", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-culture", "文教", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-entertainment", "娱乐传媒", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-sports", "体育", CaseLibraryType.CELEBRITY),
    CaseGroup("unified-celebrity-religion", "僧道", CaseLibraryType.CELEBRITY),
)

private val unifiedCelebrityPresentationGroupIds = unifiedCelebrityPresentationGroups
    .mapTo(mutableSetOf()) { it.id }

private fun CaseSummary.unifiedCelebrityPresentationGroup(): CaseGroup {
    val groupNames = groups.map { it.name }.toSet()
    val tagNames = tags.map { it.name }.toSet()
    val canonicalName = alias.celebrityResearchCanonicalName()
    val groupId = when {
        canonicalName in unifiedNamedCelebrityGroups ->
            unifiedNamedCelebrityGroups.getValue(canonicalName)
        canonicalName in unifiedForeignCelebrityGroups ->
            unifiedForeignCelebrityGroups.getValue(canonicalName)
        "君主" in groupNames || tagNames.any { it in unifiedEmperorCelebrityTags || "皇帝" in it } ->
            "unified-celebrity-emperor"
        tagNames.any { it in unifiedMilitaryCelebrityTags } -> "unified-celebrity-military"
        "僧道" in groupNames || "宗教与公共人物" in groupNames ||
            tagNames.any { it in unifiedReligionCelebrityTags } -> "unified-celebrity-religion"
        tagNames.any { it in unifiedMedicineCelebrityTags } -> "unified-celebrity-medicine"
        "科技" in groupNames || "科学" in groupNames ||
            tagNames.any { it in unifiedTechCelebrityTags } -> "unified-celebrity-tech"
        tagNames.any { it in unifiedEducationCelebrityTags } -> "unified-celebrity-culture"
        tagNames.any { it in unifiedHeritageCelebrityTags } -> "unified-celebrity-culture"
        tagNames.any { it in unifiedThoughtCelebrityTags } -> "unified-celebrity-culture"
        "文学" in groupNames || "文学与艺术" in groupNames ||
            tagNames.any { it in unifiedLiteratureCelebrityTags } -> "unified-celebrity-culture"
        "体育" in groupNames || tagNames.any { it in unifiedSportsCelebrityTags } ->
            "unified-celebrity-sports"
        "传媒" in groupNames || tagNames.any { it in unifiedMediaCelebrityTags } ->
            "unified-celebrity-entertainment"
        "娱乐" in groupNames || tagNames.any { it in unifiedEntertainmentCelebrityTags } ->
            "unified-celebrity-entertainment"
        "商界" in groupNames || "商业" in groupNames ||
            tagNames.any { it in unifiedBusinessCelebrityTags } -> "unified-celebrity-business"
        "政治" in groupNames || "政治与公共人物" in groupNames ||
            tagNames.any { it in unifiedPoliticsCelebrityTags } -> "unified-celebrity-politics"
        else -> "unified-celebrity-culture"
    }
    return unifiedCelebrityPresentationGroups.first { it.id == groupId }
}

private val unifiedEmperorCelebrityTags = setOf("皇帝", "皇后", "开国皇帝", "东汉开国皇帝", "唯一女皇帝", "纳粹元首")
private val unifiedPoliticsCelebrityTags = setOf("政治", "公共事务", "革命", "外交", "法政", "法律", "统战", "民主党派", "名臣")
private val unifiedMilitaryCelebrityTags = setOf("军事", "名将")
private val unifiedBusinessCelebrityTags = setOf(
    "商业", "实业", "价值投资", "长期投资", "企业经营", "私募", "资产管理", "基金管理", "企业家",
    "创业", "投资", "资本家", "实业家", "近代资本家", "近代中国实业家", "商圣", "巨富", "世界巨富",
    "面粉大王", "煤老板", "船王", "华人世界船王", "世界七大船王", "经营之神", "春秋巨富",
    "南浔“四象”之首", "南京巨富", "上海滩大亨", "上海滩教父", "斗富第一人",
)
private val unifiedTechCelebrityTags = setOf(
    "科技", "科学", "科技工作", "技术", "工程", "人工智能", "互联网", "计算机", "芯片", "新能源",
    "电动汽车", "航天", "物理学", "化学", "生物学", "数学", "科学家", "美国发明家", "古建筑学",
)
private val unifiedMedicineCelebrityTags = setOf("医学", "医学家", "郎中", "医疗", "公共卫生", "临床")
private val unifiedEducationCelebrityTags = setOf("教育", "教育家", "教师", "著名学者")
private val unifiedHeritageCelebrityTags = setOf("敦煌学", "数字敦煌", "文化遗产", "文物保护", "考古学", "藏书家")
private val unifiedLiteratureCelebrityTags = setOf("文学", "文学家", "作家", "诗人", "小说", "散文家", "文士", "史学家", "唐宋八大家", "出版", "语言文字")
private val unifiedEntertainmentCelebrityTags = setOf("美术", "书法", "书画家", "钢琴家", "戏曲家", "京剧名家", "京剧演员", "影视", "电影", "电视剧", "演员", "音乐", "歌手", "导演", "制片", "表演", "舞者", "舞蹈演员", "艺人", "作曲家", "综艺")
private val unifiedSportsCelebrityTags = setOf("体育", "体育明星", "篮球", "网球", "足球", "奥运", "武术")
private val unifiedMediaCelebrityTags = setOf("传媒", "主持", "新闻", "电视")
private val unifiedReligionCelebrityTags = setOf("宗教", "宗教事务", "佛教", "佛家", "道家")
private val unifiedThoughtCelebrityTags = setOf("思想", "理论", "哲学", "哲学家", "思想家", "理学家", "儒家创始人", "儒家五圣之一", "命理学家")
private val unifiedForeignCelebrityGroups = mapOf(
    "弗里德里希·尼采" to "unified-celebrity-culture",
    "阿道夫·希特勒" to "unified-celebrity-politics",
    "弗里德里希·恩格斯" to "unified-celebrity-culture",
    "罗宾德拉纳特·泰戈尔" to "unified-celebrity-culture",
    "约翰·D·洛克菲勒" to "unified-celebrity-business",
    "阿尔伯特·爱因斯坦" to "unified-celebrity-tech",
    "托马斯·爱迪生" to "unified-celebrity-tech",
)
private val unifiedNamedCelebrityGroups = mapOf(
    "丁丙" to "unified-celebrity-culture",
    "樊锦诗" to "unified-celebrity-culture",
    "岳飞" to "unified-celebrity-military",
    "戚继光" to "unified-celebrity-military",
    // 公开材料仍待核，但目录不为单例保留“待考”筛选入口。
    "沈辅" to "unified-celebrity-culture",
)

private fun CaseSummary.verifiedCelebrityIdentityKeyOrNull(): String? {
    if (libraryType != CaseLibraryType.CELEBRITY) return null
    val canonicalName = alias.celebrityResearchCanonicalName()
    return when (sourceType) {
        CaseSourceType.CURATED_CELEBRITY_CATALOG -> canonicalName
        CaseSourceType.WENZHEN_WEB_IMPORT -> {
            val expected = independentlyVerifiedCelebrityBirthDates[canonicalName] ?: return null
            val actual = (birthInput.calendarInput as? BirthCalendarInput.Solar)?.dateTime ?: return null
            if (
                actual.year == expected.year &&
                actual.month == expected.month &&
                actual.day == expected.day
            ) {
                canonicalName
            } else {
                null
            }
        }
        else -> null
    }
}

private fun CaseSummary.isPreferredCelebrityPresentationOver(current: CaseSummary): Boolean =
    sourceType == CaseSourceType.CURATED_CELEBRITY_CATALOG &&
        current.sourceType != CaseSourceType.CURATED_CELEBRITY_CATALOG

    private fun publishCaseProjection(
        projection: CaseListProjection,
        session: CaseListSession,
        clearMissingFormGroup: Boolean,
    ) {
        caseSummaryCatalogCache = projection.catalog
        caseGroupsCache = projection.groupsByLibrary
        mutableState.update {
            if (!it.acceptsCaseListSession(session)) return@update it
            val groups = projection.displayGroupsByLibrary[session.libraryType].orEmpty()
            val formGroups = projection.groupsByLibrary[it.form.libraryType].orEmpty()
            it.copy(
                cases = projection.cases,
                batchCases = projection.batchCases,
                recentCases = projection.recentCases,
                availableGroups = groups,
                availableFormGroups = formGroups,
                form = if (
                    clearMissingFormGroup &&
                    it.form.groupId != null && formGroups.none { group ->
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
            it.copy(
                selectedGroupId = groupId,
                selectedTagId = null,
                listError = null,
            )
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

    fun createAndSelectEditCaseGroup(name: String) {
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val created = try {
                caseRepository.createGroup(name, mutableState.value.editForm.libraryType)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            mutableState.update {
                it.copy(
                    metadataDraft = if (created == null) it.metadataDraft else {
                        it.metadataDraft.copy(groupNames = created.name)
                    },
                    mutationSaving = false,
                    mutationError = if (created == null) {
                        "分组名称为空、已经存在或保存失败。"
                    } else {
                        null
                    },
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
                    val session = current.caseListSession()
                    val restoreRequest = CaseSearchRequest(
                        query = session.query,
                        groupId = session.selectedGroupId,
                        tagId = session.selectedTagId,
                        sortOrder = session.sortOrder,
                        visibility = session.visibility,
                        libraryType = session.libraryType.takeIf {
                            session.visibility == CaseVisibility.ACTIVE
                        },
                        advancedFilter = session.advancedFilter,
                    )
                    publishCaseProjection(
                        projection = withContext(ioDispatcher) {
                            projectCaseList(
                                catalogBeforeDelete,
                                caseGroupsCache,
                                session,
                                restoreRequest,
                            )
                        },
                        session = session,
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
            it.copy(
                selectedGroupId = null,
                selectedTagId = tagId,
                listError = null,
            )
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
                compatibilityParticipantRole = null,
                visibility = CaseVisibility.ACTIVE,
                libraryType = CaseLibraryType.CELEBRITY,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                availableGroups = unifiedCelebrityPresentationGroups,
                availableFormGroups = groups,
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
                compatibilityParticipantRole = null,
                visibility = CaseVisibility.ACTIVE,
                libraryType = CaseLibraryType.USER,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                availableGroups = groups,
                availableFormGroups = groups,
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

    fun openBaziCompatibility() {
        val cachedReady = mutableState.value.compatibilityCandidates.isNotEmpty()
        mutableState.update {
            it.copy(
                destination = navigator.openBaziCompatibility(),
                compatibilityLoading = !cachedReady,
                compatibilityError = null,
                compatibilityHistoryLoading = true,
                compatibilityHistoryError = null,
                message = null,
            )
        }
        loadCompatibilityHistory()
        if (!cachedReady) loadCompatibilityWorkspace()
    }

    fun createCompatibilityParticipant(sex: SexForFortuneDirection) {
        val groups = cachedGroupsForLibrary(CaseLibraryType.USER)
        mutableState.update {
            it.copy(
                destination = navigator.openCompatibilityParticipantCreate(),
                compatibilityParticipantRole = sex,
                visibility = CaseVisibility.ACTIVE,
                libraryType = CaseLibraryType.USER,
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                availableGroups = groups,
                availableFormGroups = groups,
                form = it.form.copy(
                    alias = "",
                    name = "",
                    sex = sex,
                    libraryType = CaseLibraryType.USER,
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

    fun openCompatibilityParticipantList(sex: SexForFortuneDirection) {
        val groups = cachedGroupsForLibrary(CaseLibraryType.USER)
        mutableState.update {
            it.copy(
                destination = navigator.openCompatibilityParticipantList(),
                compatibilityParticipantSelectionRole = sex,
                compatibilityParticipantReplacementFromReport =
                    it.destination == AppDestination.BaziCompatibilityReport ||
                        it.compatibilityHistoryRecordId != null,
                visibility = CaseVisibility.ACTIVE,
                libraryType = CaseLibraryType.USER,
                query = "",
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(sex = sex),
                availableGroups = groups,
                listError = null,
                message = null,
            )
        }
        refreshCasesFromCache()
    }

    fun cancelCompatibilityParticipantList() {
        val current = mutableState.value
        if (current.compatibilityParticipantSelectionRole == null) return
        val destination = if (current.compatibilityParticipantReplacementFromReport) {
            // 从报告或历史详情进入时，列表的直接上级就是打开它的报告。
            navigator.back()
        } else {
            navigator.returnToBaziCompatibility()
        }
        mutableState.update {
            it.copy(
                destination = destination,
                compatibilityParticipantSelectionRole = null,
                compatibilityParticipantReplacementFromReport = false,
                query = "",
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                listError = null,
            )
        }
    }

    fun selectCompatibilityParticipantFromList(caseId: String) {
        val current = mutableState.value
        val role = current.compatibilityParticipantSelectionRole ?: return
        val selected = current.cases.firstOrNull {
            it.id == caseId && it.sexForFortuneDirection == role
        } ?: return
        val leftCaseId = if (role == SexForFortuneDirection.MAN) {
            selected.id
        } else {
            current.compatibilityLeftCaseId
        }
        val rightCaseId = if (role == SexForFortuneDirection.WOMAN) {
            selected.id
        } else {
            current.compatibilityRightCaseId
        }
        val returnToReport = current.compatibilityParticipantReplacementFromReport
        val historyReplacementRecordId = current.compatibilityHistoryRecordId
        val destination = if (returnToReport) {
            navigator.returnToBaziCompatibility()
            navigator.openBaziCompatibilityReport()
        } else {
            navigator.returnToBaziCompatibility()
        }
        mutableState.update {
            val candidates = (it.compatibilityCandidates.filterNot { candidate ->
                candidate.id == selected.id
            } + selected)
            it.copy(
                destination = destination,
                compatibilityParticipantSelectionRole = null,
                compatibilityParticipantReplacementFromReport = false,
                compatibilityCandidates = candidates,
                compatibilityLeftCaseId = leftCaseId,
                compatibilityRightCaseId = rightCaseId,
                compatibilityReport = null,
                compatibilityHistoryRecordId = null,
                compatibilityHistoryReplacementRecordId = historyReplacementRecordId,
                compatibilityError = null,
                compatibilityLoading = returnToReport,
                query = "",
                selectedGroupId = null,
                selectedTagId = null,
                advancedFilter = CaseAdvancedFilter(),
                listError = null,
            )
        }
        if (returnToReport && leftCaseId != null && rightCaseId != null) {
            loadCompatibilityReport(leftCaseId, rightCaseId)
        }
    }

    fun cancelCompatibilityParticipantCreate() {
        if (mutableState.value.compatibilityParticipantRole == null) return
        mutableState.update {
            it.copy(
                destination = navigator.returnToBaziCompatibility(),
                compatibilityParticipantRole = null,
                form = CaseFormState(
                    ratHourRule = it.defaultRatHourRule,
                    libraryType = CaseLibraryType.USER,
                ),
                formError = null,
                duplicateCandidates = emptyList(),
                previewing = false,
                instantCalculation = null,
                mutationError = null,
                message = null,
            )
        }
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
        val query = currentAlmanacQuery()
        val visibleSeed = warmedAlmanacView?.takeIf { it.query == query }
            ?: mutableState.value.almanacView?.takeIf { it.query == query }
        // 冷启动时预热已经在读取同一份“今天”视图。不能为了立即换路由先把页面
        // 置成空白：等待这份本地读取完成后一次性进入，避免用户看见闪屏/子时默认值。
        val runningWarmup = almanacWarmJob?.takeIf { it.isActive }
        if (visibleSeed == null && runningWarmup != null) {
            almanacEntryJob?.cancel()
            almanacEntryJob = viewModelScope.launch {
                runningWarmup.join()
                val warmedSeed = warmedAlmanacView?.takeIf { it.query == query }
                enterAlmanac(query, warmedSeed)
                if (warmedSeed == null) loadAlmanac(query)
            }
            return
        }
        enterAlmanac(query, visibleSeed)
        if (visibleSeed == null) loadAlmanac(query)
    }

    private fun enterAlmanac(
        query: AlmanacMonthQuery,
        visibleSeed: AlmanacMonthView?,
    ) {
        mutableState.update {
            it.copy(
                destination = navigator.openAlmanac(),
                almanacYear = query.year,
                almanacMonth = query.month,
                almanacSelectedDay = query.selectedDay,
                almanacSelectedDoubleHourIndex = query.selectedDoubleHourIndex,
                almanacView = visibleSeed,
                almanacLoading = visibleSeed == null,
                almanacRefreshingSelection = false,
                almanacError = null,
                message = null,
            )
        }
    }

    /** 首屏预热只保存月视图，不改变当前页面，避免万年历入口先空白再补齐。 */
    private fun warmCurrentAlmanacView() {
        val reader = almanacReader ?: return
        val query = currentAlmanacQuery()
        if (warmedAlmanacView?.query == query || almanacWarmJob?.isActive == true) return
        almanacWarmJob?.cancel()
        almanacWarmJob = viewModelScope.launch {
            val result = try {
                withContext(ioDispatcher) { reader.loadMonth(query) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            warmedAlmanacView = (result as? AlmanacResult.Completed)?.month
        }
    }

    private fun currentAlmanacQuery(): AlmanacMonthQuery {
        val now = LocalDateTime.now(observationClock)
        return AlmanacMonthQuery(
            year = now.year,
            month = now.monthValue,
            selectedDay = now.dayOfMonth,
            selectedDoubleHourIndex = AlmanacDoubleHours.indexForCivilHour(now.hour),
            ratHourRule = mutableState.value.defaultRatHourRule,
        )
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

    fun selectCompatibilityLeft(caseId: String) {
        val candidates = mutableState.value.compatibilityCandidates
        val selected = candidates.firstOrNull { it.id == caseId }
            ?.takeIf { it.sexForFortuneDirection == SexForFortuneDirection.MAN }
            ?: return
        val rightCaseId = mutableState.value.compatibilityRightCaseId?.takeUnless { it == caseId }
        mutableState.update {
            it.copy(
                compatibilityLeftCaseId = selected.id,
                compatibilityRightCaseId = rightCaseId,
                compatibilityReport = null,
                compatibilityHistoryRecordId = null,
                compatibilityError = null,
                compatibilityLoading = false,
            )
        }
    }

    fun selectCompatibilityRight(caseId: String) {
        val candidates = mutableState.value.compatibilityCandidates
        val selected = candidates.firstOrNull { it.id == caseId }
            ?.takeIf { it.sexForFortuneDirection == SexForFortuneDirection.WOMAN }
            ?: return
        val leftCaseId = mutableState.value.compatibilityLeftCaseId?.takeUnless { it == caseId }
        mutableState.update {
            it.copy(
                compatibilityLeftCaseId = leftCaseId,
                compatibilityRightCaseId = selected.id,
                compatibilityReport = null,
                compatibilityHistoryRecordId = null,
                compatibilityError = null,
                compatibilityLoading = false,
            )
        }
    }

    fun retryBaziCompatibility() {
        val state = mutableState.value
        val leftCaseId = state.compatibilityLeftCaseId
        val rightCaseId = state.compatibilityRightCaseId
        if (
            state.destination == AppDestination.BaziCompatibilityReport &&
                leftCaseId != null && rightCaseId != null
        ) {
            loadCompatibilityReport(leftCaseId, rightCaseId)
        } else {
            loadCompatibilityWorkspace()
        }
    }

    fun openCompatibilityHistoryRecord(recordId: String) {
        val record = mutableState.value.compatibilityHistory.firstOrNull { it.id == recordId } ?: return
        mutableState.update {
            it.copy(
                compatibilityHistoryRecordId = record.id,
                compatibilityLeftCaseId = record.report.left.caseId,
                compatibilityRightCaseId = record.report.right.caseId,
                compatibilityReport = record.report,
                compatibilityLoading = false,
                compatibilityError = null,
            )
        }
    }

    fun closeCompatibilityHistoryRecord() {
        mutableState.update {
            it.copy(
                compatibilityHistoryRecordId = null,
                compatibilityHistoryReplacementRecordId = null,
                compatibilityReport = null,
                compatibilityError = null,
            )
        }
    }

    fun deleteCompatibilityHistoryRecords(recordIds: Set<String>) {
        if (recordIds.isEmpty()) return
        viewModelScope.launch {
            val deleted = withContext(ioDispatcher) {
                compatibilityHistoryStore.delete(recordIds)
            }
            if (deleted == 0) return@launch
            compatibilityHistoryGeneration += 1
            mutableState.update { current ->
                current.copy(
                    compatibilityHistory = current.compatibilityHistory.filterNot { it.id in recordIds },
                    compatibilityHistoryRecordId = current.compatibilityHistoryRecordId
                        ?.takeUnless { it in recordIds },
                    compatibilityHistoryReplacementRecordId = current.compatibilityHistoryReplacementRecordId
                        ?.takeUnless { it in recordIds },
                    compatibilityHistoryLoading = false,
                    compatibilityHistoryError = null,
                )
            }
        }
    }

    fun retryCompatibilityHistory() {
        mutableState.update {
            it.copy(
                compatibilityHistoryLoading = true,
                compatibilityHistoryError = null,
            )
        }
        loadCompatibilityHistory()
    }

    fun analyzeBaziCompatibility() {
        val state = mutableState.value
        val leftCaseId = state.compatibilityLeftCaseId
        val rightCaseId = state.compatibilityRightCaseId
        if (leftCaseId == null || rightCaseId == null) {
            mutableState.update {
                it.copy(
                    compatibilityError = "请先分别选择男方和女方的已保存命例。",
                    compatibilityLoading = false,
                )
            }
            return
        }
        mutableState.update {
            it.copy(
                destination = navigator.openBaziCompatibilityReport(),
                compatibilityHistoryRecordId = null,
                compatibilityHistoryReplacementRecordId = null,
            )
        }
        loadCompatibilityReport(leftCaseId, rightCaseId)
    }

    private fun loadCompatibilityWorkspace() {
        compatibilityJob?.cancel()
        compatibilityJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    compatibilityLoading = true,
                    compatibilityError = null,
                    compatibilityReport = null,
                )
            }
            val candidates = try {
                withContext(ioDispatcher) {
                    caseRepository.search(
                        CaseSearchRequest(
                            sortOrder = CaseSortOrder.UPDATED_DESC,
                            visibility = CaseVisibility.ACTIVE,
                            libraryType = CaseLibraryType.USER,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        compatibilityLoading = false,
                        compatibilityError = "无法读取合盘命例，请重试。",
                    )
                }
                return@launch
            }
            val current = mutableState.value
            mutableState.update {
                it.copy(
                    compatibilityCandidates = candidates,
                    compatibilityLeftCaseId = current.compatibilityLeftCaseId
                        ?.takeIf { id -> candidates.any { it.id == id } },
                    compatibilityRightCaseId = current.compatibilityRightCaseId
                        ?.takeIf { id -> id != current.compatibilityLeftCaseId && candidates.any { it.id == id } },
                    compatibilityReport = null,
                    compatibilityLoading = false,
                    compatibilityError = null,
                )
            }
        }
    }

    private fun loadCompatibilityHistory() {
        val requestGeneration = ++compatibilityHistoryGeneration
        viewModelScope.launch {
            val records = try {
                withContext(ioDispatcher) {
                    val stored = compatibilityHistoryStore.list()
                    val historyCaseIds = stored.flatMap { record ->
                        listOf(record.report.left.caseId, record.report.right.caseId)
                    }.toSet()
                    val cases = if (historyCaseIds.isEmpty()) {
                        emptyMap()
                    } else {
                        runCatching { caseRepository.findByIds(historyCaseIds) }
                            .getOrDefault(emptyMap())
                    }
                    stored.map { record ->
                        val hydrated = BaziCompatibilityAnalyzer.hydrateHistoricalReport(
                            report = record.report,
                            leftCase = cases[record.report.left.caseId],
                            rightCase = cases[record.report.right.caseId],
                        )
                        if (hydrated == record.report) record else compatibilityHistoryStore.replace(record.copy(report = hydrated))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { current ->
                    if (requestGeneration != compatibilityHistoryGeneration) current else current.copy(
                        compatibilityHistoryLoading = false,
                        compatibilityHistoryError = "合盘记录无法读取，原文件已保留。请从完整备份恢复，或联系支持导出诊断后再试。",
                    )
                }
                return@launch
            }
            mutableState.update { current ->
                if (requestGeneration != compatibilityHistoryGeneration) current else current.copy(
                    compatibilityHistory = records,
                    compatibilityHistoryLoading = false,
                    compatibilityHistoryError = null,
                )
            }
        }
    }

    private fun loadCompatibilityReport(leftCaseId: String, rightCaseId: String) {
        compatibilityJob?.cancel()
        compatibilityJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    compatibilityLoading = true,
                    compatibilityError = null,
                    compatibilityReport = null,
                )
            }
            loadCompatibilityReportInCurrentJob(leftCaseId, rightCaseId)
        }
    }

    private suspend fun loadCompatibilityReportInCurrentJob(
        leftCaseId: String,
        rightCaseId: String,
    ) {
        val result = try {
            val cases = withContext(ioDispatcher) {
                caseRepository.findByIds(setOf(leftCaseId, rightCaseId))
            }
            val left = cases[leftCaseId]
            val right = cases[rightCaseId]
            if (left == null || right == null) null else BaziCompatibilityAnalyzer.analyze(left, right)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val historyReplacementRecordId = mutableState.value.compatibilityHistoryReplacementRecordId
        val savedRecord = try {
            (result as? BaziCompatibilityResult.Ready)?.let { ready ->
                withContext(ioDispatcher) {
                    val existing = historyReplacementRecordId?.let { recordId ->
                        compatibilityHistoryStore.list().firstOrNull { it.id == recordId }
                    }
                    if (existing == null) {
                        compatibilityHistoryStore.save(ready.report, clock.millis())
                    } else {
                        compatibilityHistoryStore.replace(existing.copy(report = ready.report))
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            compatibilityHistoryGeneration += 1
            mutableState.update {
                if (
                    it.compatibilityLeftCaseId != leftCaseId ||
                    it.compatibilityRightCaseId != rightCaseId ||
                    result !is BaziCompatibilityResult.Ready
                ) {
                    it
                } else {
                    it.copy(
                        compatibilityReport = result.report,
                        compatibilityHistoryReplacementRecordId = null,
                        compatibilityLoading = false,
                        compatibilityError = null,
                        compatibilityHistoryLoading = false,
                        compatibilityHistoryError = "合盘已生成，但记录无法保存；原文件已保留。请处理合盘记录后再试。",
                    )
                }
            }
            return
        }
        if (savedRecord != null) compatibilityHistoryGeneration += 1
        mutableState.update {
            if (
                it.compatibilityLeftCaseId != leftCaseId ||
                it.compatibilityRightCaseId != rightCaseId
            ) {
                it
            } else when (result) {
                is BaziCompatibilityResult.Ready -> it.copy(
                    compatibilityReport = result.report,
                    compatibilityHistory = savedRecord?.let { record ->
                        listOf(record) + it.compatibilityHistory.filterNot { existing -> existing.id == record.id }
                    } ?: it.compatibilityHistory,
                    compatibilityHistoryRecordId = null,
                    compatibilityHistoryReplacementRecordId = null,
                    compatibilityLoading = false,
                    compatibilityError = null,
                    compatibilityHistoryLoading = false,
                    compatibilityHistoryError = null,
                )
                is BaziCompatibilityResult.Rejected -> it.copy(
                    compatibilityHistoryReplacementRecordId = null,
                    compatibilityLoading = false,
                    compatibilityError = result.reasons.joinToString("\n") { reason -> reason.message },
                )
                null -> it.copy(
                    compatibilityHistoryReplacementRecordId = null,
                    compatibilityLoading = false,
                    compatibilityError = "命例已变化或无法读取，请刷新后重新选择。",
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
        val submittedForm = mutableState.value.form
        mutableState.update { it.copy(saving = true, formError = null) }
        viewModelScope.launch {
            val form = try {
                resolveAutoGeneratedCaseName(submittedForm)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    if (it.form == submittedForm) {
                        it.copy(
                            saving = false,
                            formError = "无法生成默认姓名，未保存任何内容。请稍后重试。",
                        )
                    } else {
                        it.copy(saving = false)
                    }
                }
                return@launch
            }
            if (
                mutableState.value.form != submittedForm ||
                mutableState.value.destination != AppDestination.CreateCase
            ) {
                mutableState.update { it.copy(saving = false) }
                return@launch
            }
            if (form != submittedForm) {
                mutableState.update { it.copy(form = form) }
            }
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

    private suspend fun resolveAutoGeneratedCaseName(form: CaseFormState): CaseFormState {
        if (form.libraryType != CaseLibraryType.USER || form.alias.isNotBlank()) return form
        val existingNames = caseRepository.search(
            CaseSearchRequest(
                visibility = CaseVisibility.ALL,
                libraryType = CaseLibraryType.USER,
            ),
        ).flatMap { summary ->
            listOfNotNull(summary.alias, summary.name.value)
        }
        val generatedName = nextAutoGeneratedCaseAlias(existingNames)
        return form.copy(alias = generatedName, name = generatedName)
    }

    private fun cleanUpLegacyEmptyPlaceholderCases() {
        viewModelScope.launch {
            val moved = try {
                withContext(ioDispatcher) {
                    caseRepository.moveBlankPlaceholderCasesToTrash(clock.instant())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(message = "未能清理无内容的“某某”占位案例；其他命例未受影响。")
                }
                0
            }
            if (moved > 0) {
                mutableState.update {
                    it.copy(message = "已将 $moved 个无断事笔记的“某某”占位案例移入回收站，可恢复。")
                }
            }
            refreshCases()
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
                val createdForCompatibility = mutableState.value.compatibilityParticipantRole
                mutableState.update {
                    val showingPendingDetail =
                        it.destination == AppDestination.CaseDetail(pendingCase.id) &&
                            it.detail?.id == pendingCase.id &&
                            it.form == form
                    val stillOnCreatePage =
                        it.destination == AppDestination.CreateCase && it.form == form
                    val returnToCompatibility = createdForCompatibility != null &&
                        (showingPendingDetail || stillOnCreatePage)
                    val shouldOpenSavedCase = !returnToCompatibility &&
                        (showingPendingDetail || stillOnCreatePage)
                    val compatibilityCandidates = if (returnToCompatibility) {
                        (it.compatibilityCandidates.filterNot { candidate -> candidate.id == result.caseId } +
                            result.case.toCaseSummary())
                            .sortedByDescending { candidate -> candidate.updatedAt }
                    } else {
                        it.compatibilityCandidates
                    }
                    it.copy(
                        destination = if (returnToCompatibility) {
                            navigator.returnToBaziCompatibility()
                        } else if (shouldOpenSavedCase) {
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
                        compatibilityCandidates = compatibilityCandidates,
                        compatibilityLeftCaseId = if (
                            returnToCompatibility && createdForCompatibility == SexForFortuneDirection.MAN
                        ) {
                            result.caseId
                        } else {
                            it.compatibilityLeftCaseId
                        },
                        compatibilityRightCaseId = if (
                            returnToCompatibility && createdForCompatibility == SexForFortuneDirection.WOMAN
                        ) {
                            result.caseId
                        } else {
                            it.compatibilityRightCaseId
                        },
                        compatibilityReport = if (returnToCompatibility) null else it.compatibilityReport,
                        compatibilityLoading = false,
                        compatibilityError = null,
                        compatibilityParticipantRole = if (returnToCompatibility) null else {
                            it.compatibilityParticipantRole
                        },
                        duplicateCandidates = emptyList(),
                        detail = if (shouldOpenSavedCase) result.case else if (returnToCompatibility) null else it.detail,
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
                        detailIsTransient = if (shouldOpenSavedCase || returnToCompatibility) false else it.detailIsTransient,
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
                        message = if (returnToCompatibility) {
                            "${createdForCompatibility?.compatibilityRoleName() ?: "对方"}命例已保存，可继续选择另一方并开始合盘。"
                        } else {
                            "命例已完成排盘并保存。"
                        },
                    )
                }
                if (createdForCompatibility != null) {
                    loadCompatibilityWorkspace()
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

    /**
     * 原问真日期与可直达公开资料存在明确完整日期差异时，统一名人目录只读采用公开日期。
     * 部分早期年份超出本机民用历法引擎范围，来源四柱快照必须保留，不能伪造本机复算。
     */
    private fun BaziCase.withVerifiedCelebrityDateCorrection(): BaziCase {
        if (sourceType != CaseSourceType.WENZHEN_WEB_IMPORT ||
            libraryType != CaseLibraryType.CELEBRITY
        ) return this
        val correction = independentlyVerifiedCelebrityDateConflicts[
            alias.celebrityResearchCanonicalName()
        ]?.authoritativeDate ?: return this
        val current = (birthInput.calendarInput as? BirthCalendarInput.Solar)?.dateTime ?: return this
        if (
            current.year == correction.year &&
            current.month == correction.month &&
            current.day == correction.day
        ) return this
        val originalDateSource = birthInput.sourceNote ?: run {
            val original = current
            "统一名人目录只读展示校正；来源阳历：%04d-%02d-%02d %02d:%02d:%02d。".format(
                original.year,
                original.month,
                original.day,
                original.hour,
                original.minute,
                original.second,
            )
        }
        val correctedInput = birthInput.copy(
            calendarInput = BirthCalendarInput.Solar(
                current.copy(
                    year = correction.year,
                    month = correction.month,
                    day = correction.day,
                ),
            ),
            sourceNote = originalDateSource,
        )
        return copy(birthInput = correctedInput)
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
                val prefetchedDetail = prefetchedSnapshot?.case?.withVerifiedCelebrityDateCorrection()
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
                var detail = caseRepository.findById(caseId)?.withVerifiedCelebrityDateCorrection()
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
        val formGroups = cachedGroupsForLibrary(caseData.libraryType)
        mutableState.update {
            it.copy(
                destination = navigator.openEditCase(caseData.id),
                detail = caseData,
                editForm = caseData.toEditableForm(),
                availableFormGroups = formGroups,
                metadataDraft = CaseMetadataDraft(
                    groupNames = caseData.groups.joinToString("，") { group -> group.name },
                ),
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
        val groupNames = mutableState.value.metadataDraft.groupNames
            .takeUnless {
                it == detail.groups.joinToString("，") { group -> group.name }
            }
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = withContext(ioDispatcher) {
                editCase(detail.id, detail.revision, form, groupNames, allowDuplicate)
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
            // A trashed detail can stay on screen while a background catalog refresh updates
            // its revision. Restore is a lifecycle-only mutation, so re-read the one target
            // first instead of silently rejecting the visible action on a stale revision.
            val latest = try {
                caseRepository.findById(detail.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (latest?.deletedAt == null && latest != null) {
                // The same record may already have been restored while this detail was
                // open. Converge the UI to its actual location instead of leaving the
                // user on a stale recycle-bin detail with only an error message.
                mutableState.update {
                    it.copy(
                        destination = navigator.backToList(),
                        visibility = CaseVisibility.ACTIVE,
                        libraryType = latest.libraryType,
                        detail = null,
                        mutationSaving = false,
                        message = "命例已恢复。",
                    )
                }
                refreshCases()
                return@launch
            }
            val result = when {
                latest == null -> CaseMutationResult.NotFound
                else -> caseLifecycle.restore(latest.id, latest.revision)
            }
            when (result) {
                is CaseMutationResult.Saved -> {
                    val persisted = try {
                        withContext(ioDispatcher) { caseRepository.findById(result.caseId) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    if (persisted == null || persisted.deletedAt != null) {
                        mutableState.update {
                            it.copy(
                                mutationSaving = false,
                                mutationError = "恢复未生效，命例仍在回收站。请稍后重试。",
                            )
                        }
                        refreshCases()
                        return@launch
                    }
                    val cacheUpdated = updateCachedCaseCatalog { catalog ->
                        catalog.map { summary ->
                            if (summary.id == persisted.id) persisted.toCaseSummary() else summary
                        }
                    }
                    mutableState.update {
                        it.copy(
                            destination = navigator.backToList(),
                            visibility = CaseVisibility.ACTIVE,
                            libraryType = persisted.libraryType,
                            detail = null,
                            mutationSaving = false,
                            mutationError = null,
                            message = "命例已从回收站恢复。",
                        )
                    }
                    if (cacheUpdated) {
                        refreshCasesFromCache()
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
        if (
            mutableState.value.destination == AppDestination.CreateCase &&
                mutableState.value.compatibilityParticipantRole != null
        ) {
            cancelCompatibilityParticipantCreate()
            return
        }
        if (
            mutableState.value.destination == AppDestination.CaseList &&
                mutableState.value.compatibilityParticipantSelectionRole != null
        ) {
            cancelCompatibilityParticipantList()
            return
        }
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
                result.savedCase?.let { savedCase ->
                    retainCaseDetail(savedCase)
                    updateCachedCaseCatalog { catalog ->
                        catalog.filterNot { it.id == savedCase.id } + savedCase.toCaseSummary()
                    }
                    mutableState.update {
                        it.copy(
                            destination = navigator.back(),
                            detail = savedCase,
                            mutationSaving = false,
                            mutationError = null,
                            message = successMessage,
                        )
                    }
                    refreshCasesFromCache()
                    return
                }
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
                    val raw = openInput()?.use { it.readUtf8Bounded(WENZHEN_IMPORT_MAX_BYTES) }
                        ?: error("无法读取所选文件。")
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

    fun previewCuratedCelebrityImport(openInput: () -> InputStream?) {
        if (mutableState.value.curatedCelebrityImportBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    curatedCelebrityImportBusy = true,
                    curatedCelebrityImportError = null,
                    curatedCelebrityImportResult = null,
                )
            }
            val decoded = try {
                withContext(ioDispatcher) {
                    val raw = openInput()?.use { it.readUtf8Bounded(CURATED_CELEBRITY_IMPORT_MAX_BYTES) }
                        ?: error("无法读取内置名人案例统一资料库。")
                    curatedCelebrityImporter.decode(raw)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                pendingCuratedCelebrityImport = null
                mutableState.update {
                    it.copy(
                        curatedCelebrityImportBusy = false,
                        curatedCelebrityImportPreview = null,
                        curatedCelebrityImportError = error.message ?: "名人案例统一资料库检查失败。",
                    )
                }
                return@launch
            }
            pendingCuratedCelebrityImport = decoded
            mutableState.update {
                it.copy(
                    curatedCelebrityImportBusy = false,
                    curatedCelebrityImportPreview = curatedCelebrityImporter.preview(decoded),
                    curatedCelebrityImportProgress = null,
                )
            }
        }
    }

    fun cancelCuratedCelebrityImport() {
        if (mutableState.value.curatedCelebrityImportBusy) return
        pendingCuratedCelebrityImport = null
        mutableState.update {
            it.copy(curatedCelebrityImportPreview = null, curatedCelebrityImportError = null)
        }
    }

    fun executeCuratedCelebrityImport() {
        val data = pendingCuratedCelebrityImport ?: return
        if (mutableState.value.curatedCelebrityImportBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    curatedCelebrityImportBusy = true,
                    curatedCelebrityImportPreview = null,
                    curatedCelebrityImportProgress = CuratedCelebrityImportProgress(0, data.cases.size),
                    curatedCelebrityImportError = null,
                )
            }
            val result = try {
                withContext(ioDispatcher) {
                    curatedCelebrityImporter.import(data) { progress ->
                        if (progress.completed % 5 == 0 || progress.completed == progress.total) {
                            mutableState.update { it.copy(curatedCelebrityImportProgress = progress) }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        curatedCelebrityImportBusy = false,
                        curatedCelebrityImportProgress = null,
                        curatedCelebrityImportError = error.message ?: "名人案例统一资料库更新失败，可重新尝试。",
                    )
                }
                return@launch
            }
            pendingCuratedCelebrityImport = null
            mutableState.update {
                it.copy(
                    curatedCelebrityImportBusy = false,
                    curatedCelebrityImportProgress = null,
                    curatedCelebrityImportResult = result,
                    message = "名人案例统一资料库已更新：新增 ${result.created}，更新 ${result.updated}，已存在 ${result.skipped}。",
                )
            }
            refreshCases()
        }
    }

    fun dismissCuratedCelebrityImportResult() {
        mutableState.update {
            it.copy(curatedCelebrityImportResult = null, curatedCelebrityImportError = null)
        }
    }

    /** Applies the single canonical catalog shipped inside the APK. */
    fun synchronizeBuiltInUnifiedCelebrityCatalog(
        openInput: () -> InputStream?,
        installedVersion: () -> String?,
        markInstalled: (String) -> Unit,
        force: Boolean = false,
    ) {
        if (builtInCelebrityCatalogSyncJob?.isActive == true) return
        builtInCelebrityCatalogSyncJob = viewModelScope.launch {
            val data = try {
                withContext(ioDispatcher) {
                    val raw = openInput()?.use { it.readUtf8Bounded(CURATED_CELEBRITY_IMPORT_MAX_BYTES) }
                        ?: error("无法读取内置名人案例统一资料库。")
                    unifiedCelebrityCatalogImporter.decode(raw)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        builtInCelebrityCatalogSyncBusy = false,
                        message = "内置名人案例统一资料库未能读取；用户列表未受影响。",
                    )
                }
                return@launch
            }
            if (!force && installedVersion() == data.catalogVersion) return@launch
            mutableState.update {
                it.copy(
                    builtInCelebrityCatalogSyncBusy = true,
                    builtInCelebrityCatalogSyncCaseCount = data.cases.size,
                )
            }

            val result = try {
                withContext(ioDispatcher) {
                    unifiedCelebrityCatalogImporter.synchronize(data)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        builtInCelebrityCatalogSyncBusy = false,
                        message = "内置名人案例统一资料库同步未完成；用户列表未受影响。",
                    )
                }
                return@launch
            }
            if (result.invalid > 0) {
                val message = result.errors.firstOrNull() ?: "名人资料更新失败。"
                Log.e(
                    "NanfengBaziCatalog",
                    "Built-in celebrity catalog sync failed: invalid=${result.invalid}; errors=${result.errors}",
                )
                mutableState.update {
                    it.copy(
                        builtInCelebrityCatalogSyncBusy = false,
                        message = message,
                    )
                }
                return@launch
            }
            markInstalled(data.catalogVersion)
            mutableState.update {
                it.copy(
                        builtInCelebrityCatalogSyncBusy = false,
                        message = "名人资料已更新。",
                )
            }
            refreshCases()
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
                compatibilityHistoryStore = container.baziCompatibilityHistoryStore,
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
        const val WENZHEN_IMPORT_MAX_BYTES = 8 * 1024 * 1024
        const val CURATED_CELEBRITY_IMPORT_MAX_BYTES = 4 * 1024 * 1024
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
    val storedTimeline = events.mapNotNull { event ->
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
    val timeline = if (
        libraryType == CaseLibraryType.CELEBRITY &&
        storedTimeline.isEmpty()
    ) {
        curatedCelebrityPublicTimeline()
    } else {
        storedTimeline
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
    val ownerFeedback = recordBody(CaseTextRecordType.OWNER_FEEDBACK)
    val masterCommentary = recordBody(CaseTextRecordType.MASTER_COMMENTARY)
    return CaseNotesDraft(
        ownerFeedback = compactCelebrityBirthEvidence(ownerFeedback),
        masterCommentary = compactCelebrityBiography(masterCommentary),
        aiCommentary = selectedAiCommentary?.body.orEmpty(),
        aiCommentaryRecordId = selectedAiCommentary?.recordId,
        aiCommentaryVersions = aiCommentaryVersions,
        timeline = timeline,
    )
}

/** 名人案例只展示可读的出生依据和生平资料；原始导入记录仍留在数据来源中。 */
private fun BaziCase.compactCelebrityBirthEvidence(raw: String): String {
    if (libraryType != CaseLibraryType.CELEBRITY) return raw
    if (raw.isBlank() && sourceType == CaseSourceType.WENZHEN_WEB_IMPORT) {
        return originalWenzhenCelebrityBirthEvidence()
    }
    if ((!raw.contains("【资料等级】") && !raw.contains("【资料可信度】")) ||
        !raw.contains("【默认采用")
    ) return raw

    fun section(marker: String, nextMarker: String? = null): String {
        val value = raw.substringAfter(marker, missingDelimiterValue = "").trim()
        return nextMarker?.let { value.substringBefore(it).trim() } ?: value
    }

    val legacyRating = section("【资料等级】", "\n【出生地与时区】")
        .substringBefore("。等级说明")
        .trim()
    val evidenceSummary = if (raw.contains("【资料可信度】")) {
        section("【资料可信度】", "\n【出生地】")
    } else {
        Regex("Rodden\\s+(AA|A|B|C|DD|X)")
            .find(legacyRating)
            ?.groupValues
            ?.get(1)
            ?.toChineseEvidenceSummary()
            .orEmpty()
    }
    val dateExplanation = section("【日期说明】", "\n【默认采用】")
        .toChineseCelebrityReadingText()
    val dateCandidateLinks = section("【日期说明】", "\n【默认采用】")
        .lineSequence()
        .map(String::trim)
        .filter { it.startsWith("https://") }
        .distinct()
        .toList()
    val adoptedTime = section("【默认采用】", "\n【其他可能时辰】")
        .ifBlank { section("【默认采用】", "\n【来源】") }
        .toChineseCelebrityReadingText()
        .withoutDuplicateClockTime()
    val alternativeTimes = section("【其他可能时辰】", "\n【来源】")
        .lineSequence()
        .map(String::trim)
        .filter { it.startsWith("- ") }
        .map { it.removePrefix("- ") }
        .map(String::toChineseCelebrityReadingText)
        .map(String::withoutDuplicateClockTime)
        .toList()
    val sourceSection = section("【来源】")
    val sources = sourceSection
        .lineSequence()
        .map(String::trim)
        .filter { it.contains("｜") }
        .map { it.removePrefix("- ").substringBefore("｜").trim() }
        .map(String::toChineseCelebrityReadingText)
        .distinct()
        .toList()
    val sourceLinks = sourceSection
        .lineSequence()
        .map(String::trim)
        .filter { it.startsWith("https://") }
        .distinct()
        .toList()

    return buildString {
        append("【出生依据】\n")
        birthInput.locationName?.trim()?.takeIf(String::isNotEmpty)?.let { place ->
            append("出生地：$place\n")
        }
        evidenceSummary.takeIf(String::isNotBlank)?.let { append("资料可信度：$it\n") }
        dateExplanation
            .lineSequence()
            .filterNot { it.trim().startsWith("https://") }
            .joinToString(" ")
            .trim()
            .takeIf(String::isNotBlank)
            ?.let { append("日期说明：$it\n") }
        dateCandidateLinks.takeIf(List<String>::isNotEmpty)?.let { links ->
            append("日期候选来源：\n")
            links.forEach { link -> append("$link\n") }
        }
        adoptedTime.takeIf(String::isNotBlank)?.let { append("采用时刻：$it\n") }
        alternativeTimes.takeIf(List<String>::isNotEmpty)?.let { alternatives ->
            append("其他候选：${alternatives.joinToString("；")}\n")
        }
        if (sources.isNotEmpty() || sourceLinks.isNotEmpty()) {
            append("资料来源：${sources.joinToString("；")}\n")
            sourceLinks.forEach { link -> append("$link\n") }
        }
    }.trim()
}

/**
 * 历史网页资料导入的名人条目没有自带公开履历或出处链接时，仍必须如实展示其来源状态，
 * 不能把空白反馈伪装成已保留的资料等级或已核时辰。
 */
private fun BaziCase.originalWenzhenCelebrityBirthEvidence(): String {
    val displayedTime = when (val calendar = birthInput.calendarInput) {
        is BirthCalendarInput.Solar -> calendar.dateTime.let { value ->
            "%04d-%02d-%02d %02d:%02d".format(
                value.year,
                value.month,
                value.day,
                value.hour,
                value.minute,
            )
        }
        is BirthCalendarInput.Lunar -> calendar.dateTime.let { value ->
            "农历%04d-%02d-%02d %02d:%02d".format(
                value.year,
                value.month,
                value.day,
                value.hour,
                value.minute,
            )
        }
    }
    val sourceTime = birthInput.sourceNote
        ?.let { note ->
            Regex("来源阳历：(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(?::\\d{2})?)")
                .find(note)
                ?.groupValues
                ?.getOrNull(1)
        }
        ?: displayedTime
    val canonicalName = alias.celebrityResearchCanonicalName()
    val birthDateEvidence = independentlyVerifiedCelebrityBirthDates[canonicalName]
    val publicTimelineLinks = listOfNotNull(birthDateEvidence?.sourceUrl) +
        curatedCelebrityTimelineSources[canonicalName]
        .orEmpty()
        .map(CuratedCelebrityTimelineItem::sourceUrl)
        .distinct()
    val publicSourceLinks = publicTimelineLinks.distinct().take(3)
    val dateConflict = independentlyVerifiedCelebrityDateConflicts[canonicalName]
    return buildString {
        appendLine("【出生依据】")
        when {
            dateConflict?.authoritativeDate != null -> {
                appendLine("资料可信度：公开完整生日已核验；原始问真日期存在差异")
                appendLine("来源排盘输入：$sourceTime（保留历史导入源值）")
                appendLine("统一目录采用：$displayedTime（来源四柱快照保留，未伪造本机复算）")
                appendLine("公开资料来源：")
                publicSourceLinks.forEach(::appendLine)
            }
            hasIndependentlyVerifiedPublicIdentity() -> {
                appendLine("资料可信度：出生日期已与独立公开资料核验")
                appendLine("来源排盘输入：$sourceTime（保留历史导入源值）")
                appendLine("公开资料来源：")
                publicSourceLinks.forEach(::appendLine)
            }
            dateConflict != null -> {
                appendLine("资料可信度：出生日期存在公开异说")
                appendLine("来源排盘输入：$sourceTime（保留历史导入源值）")
                appendLine("公开资料日期：${dateConflict.publicDate}；与原始日期不一致，不绑定公开生平时间线。")
                appendLine(dateConflict.sourceUrl)
            }
            else -> {
                appendLine("资料可信度：待核")
                appendLine("来源排盘输入：$sourceTime（历史导入源值，尚未独立核验为公开人物出生时刻）")
                append("资料来源：历史导入资料；该条尚未提供可直达的公开出处链接。")
            }
        }
    }.trim()
}

private fun String.toChineseCelebrityReadingText(): String = this
    .replace("Astro-Databank", "国际人物出生资料库")
    .replace(Regex("[，,]\\s*Rodden\\s+(?:AA|A|B|C|DD|X)[。.]?"), "")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String.withoutDuplicateClockTime(): String =
    replace(Regex("（\\d{2}:\\d{2}(?::\\d{2})?）"), "")

private fun BaziCase.compactCelebrityBiography(raw: String): String {
    if (libraryType != CaseLibraryType.CELEBRITY) return raw
    if (sourceType == CaseSourceType.WENZHEN_WEB_IMPORT && !hasIndependentlyVerifiedPublicIdentity()) {
        return "【人物概览】\n名人案例资料尚待核验。姓名与排盘日期尚未完成同一人交叉核验，暂不绑定同名公开人物的生平与成就，以避免错配。"
    }
    if (!hasIndependentlyVerifiedPublicIdentity()) return raw
    val summary = celebrityContributionOverviews[alias.celebrityResearchCanonicalName()]
        ?: raw
            .removePrefix("资料点评（编审摘要，非命理断语）：")
            .removePrefix("【人物概览】")
            .toCelebrityLifeOverview()
            .trim()
    return summary.takeIf(String::isNotBlank)
        ?.let { "【人物概览】\n$it" }
        .orEmpty()
}

/** 同一人物在问真网页包与本地资料包使用不同姓名时，共用同一份已核公开资料。 */
private fun String.celebrityResearchCanonicalName(): String =
    celebrityResearchAliases[this] ?: this

private val celebrityResearchAliases = mapOf(
    "爱因斯坦" to "阿尔伯特·爱因斯坦",
    "乔丹" to "迈克尔·乔丹",
    "刘基" to "刘伯温",
    "陶朱公" to "范蠡",
    "倪元潞" to "倪元璐",
    "朱文公" to "朱熹",
    "王阳明" to "王守仁",
    "文潞公" to "文彦博",
    "欧阳文忠公" to "欧阳修",
    "真西山" to "真德秀",
    "尼采" to "弗里德里希·尼采",
    "恩格斯" to "弗里德里希·恩格斯",
    "泰戈尔" to "罗宾德拉纳特·泰戈尔",
    "希特勒" to "阿道夫·希特勒",
    "洛克菲勤" to "约翰·D·洛克菲勒",
    "爱迪生" to "托马斯·爱迪生",
    "罗纳尔多" to "罗纳尔多·纳扎里奥",
    "苏东坡" to "苏轼",
)

/** 仅用于名人案例的公开生平概览，不参与出生时刻或命理结论的判断。 */
private val celebrityContributionOverviews = mapOf(
    "阿尔伯特·爱因斯坦" to "理论物理学家。1905 年提出光电效应解释和狭义相对论，1915 年完成广义相对论；其工作深刻改变了现代物理学对时空、引力与能量的理解。",
    "史蒂夫·乔布斯" to "Apple 联合创办人。推动 Macintosh、iPod、iPhone 等消费电子产品走向大众，并参与 Pixar 的早期发展，对个人计算与数字内容产业产生持续影响。",
    "碧昂丝" to "歌手、词曲作者与表演者。从 Destiny’s Child 走向个人创作生涯，以舞台制作、视觉专辑和女性创作者的商业自主性影响当代流行音乐。",
    "塞雷娜·威廉姆斯" to "职业网球运动员。获得 23 个大满贯女单冠军，长期位居世界第一；以力量型打法和稳定竞争力改变了女子网坛的竞技标杆。",
    "萨尔玛·海耶克" to "演员与制片人。因《Frida》获得奥斯卡最佳女主角提名，并持续推进拉丁裔创作者在好莱坞的能见度与制作机会。",
    "比尔·盖茨" to "Microsoft 联合创办人。参与推动个人电脑软件普及；此后通过基金会长期投入全球健康、教育与公共卫生事业。",
    "斯蒂芬·霍金" to "理论物理学家。围绕黑洞、奇点与宇宙学提出重要研究，推动“霍金辐射”等议题进入公众视野；《时间简史》广泛传播科学思想。",
    "唐纳德·特朗普" to "商人、媒体人物与政治人物。长期经营地产与品牌业务，曾主持电视节目，并于 2017—2021 年担任美国总统。",
    "贝拉克·奥巴马" to "政治人物、律师。2009—2017 年担任美国总统，是美国首位非裔总统；其公共演讲、医疗改革与外交议程具有广泛社会影响。",
    "奥普拉·温弗瑞" to "主持人、制作人与企业家。《奥普拉·温弗瑞秀》长期影响美国电视谈话节目和大众文化，也拓展了女性媒体经营者的公共影响力。",
    "迈克尔·乔丹" to "职业篮球运动员与企业家。带领芝加哥公牛六夺 NBA 总冠军，成为篮球全球化的重要符号，并深刻影响运动员个人品牌经营。",
    "戴安娜王妃" to "英国王室成员与公益倡导者。长期参与艾滋病、无家可归者和地雷受害者等议题的公共倡导，推动王室公共形象的亲民化。",
    "迈克尔·B·乔丹" to "演员、制片人与导演。凭《奎迪》《黑豹》等作品扩大影响力，并通过制作工作支持更多非裔创作者进入主流影视项目。",
    "李小龙" to "武术家、演员与截拳道创立者。以电影和跨文化表达将中国武术推向国际大众文化，并对动作片表演和训练理念产生深远影响。",
    "莱昂纳多·迪卡普里奥" to "演员、制片人与环境倡导者。主演多部具有全球影响力的电影，凭《荒野猎人》获奥斯卡；同时长期投入气候与生态保护倡议。",
    "埃隆·马斯克" to "企业家与投资人。参与创办或领导 PayPal、SpaceX、Tesla 等公司，推动商业航天、电动车与能源技术在公众和产业层面的讨论与应用。",
    "卡尔·萨根" to "天文学家与科学传播者。参与行星研究和太空探索项目，以《Cosmos》等作品把天文学与科学方法带入全球大众视野。",
    "泰勒·斯威夫特" to "创作歌手与制作人。以跨流派创作、叙事型歌词和重新录制旧作等实践，显著影响当代流行音乐的创作、版权与粉丝经济讨论。",
    "成龙" to "演员、导演、制片人与武术表演者。将喜剧、特技和动作设计结合，推动华语动作电影走向国际市场，并培养动作电影工业化协作模式。",
    "沃伦·巴菲特" to "投资者与企业经营者。长期执掌伯克希尔·哈撒韦，以价值投资理念和致股东信广泛影响全球投资者教育与企业治理讨论。",
    "勒布朗·詹姆斯" to "职业篮球运动员。多次获得 NBA 总冠军与常规赛最有价值球员，兼具长期竞技表现、球员赋权与教育公益影响力。",
    "弗拉基米尔·普京" to "俄罗斯政治人物。长期担任俄罗斯总统或政府首脑，是 21 世纪俄罗斯国内政治与对外政策的重要决策者。",
    "弗拉基米尔·泽连斯基" to "乌克兰政治人物，曾从事演员、编剧与制作工作。2019 年当选乌克兰总统，其领导角色在俄乌战争期间受到国际广泛关注。",
    "纳伦德拉·莫迪" to "印度政治人物。曾任古吉拉特邦首席部长，2014 年起担任印度总理；其经济、基础设施与数字化政策在印度社会持续引发讨论。",
    "马克·扎克伯格" to "科技企业家。参与创办 Facebook（后发展为 Meta），推动社交网络成为全球日常沟通基础设施，也引发数据隐私与平台治理讨论。",
    "乔·拜登" to "美国政治人物。长期担任联邦参议员、副总统，并于 2021—2025 年担任美国总统，公共生涯跨越外交、司法与国内政策领域。",
    "习近平" to "中国政治人物。长期在地方与中央任职，2012 年起担任中共中央总书记、2013 年起担任国家主席，对当代中国的治理与政策方向具有重要影响。",
    "李克强" to "中国政治人物，经济学博士。曾任河南、辽宁主要领导职务，2013—2023 年担任国务院总理，参与宏观经济和政府行政工作。",
    "释永信" to "佛教界人士，长期担任少林寺住持。围绕少林武术、寺院文化传播与相关社会活动具有较高公众知名度。",
    "王力宏" to "音乐人、歌手与演员。长期从事流行音乐创作、制作和表演，以跨文化音乐元素与多乐器创作建立个人风格。",
    "玛丽莲·梦露" to "演员、歌手与流行文化人物。以《七年之痒》《热情似火》等作品成为 20 世纪银幕偶像，其形象持续影响电影、时尚与大众文化。",
    "迈克尔·杰克逊" to "歌手、舞者与制作人。《Thriller》等作品重塑流行音乐的制作、舞台表演和音乐录像传播方式，具有全球文化影响力。",
    "布拉德·皮特" to "演员与制片人。主演并制作多部重要商业与作者电影，后通过制作公司支持不同类型的电影项目与新锐创作者。",
    "安吉丽娜·朱莉" to "演员、电影制作人与公益活动者。因《移魂女郎》获奥斯卡奖，并长期参与难民、人道援助与女性权益相关公共事务。",
    "艾伦·图灵" to "数学家与计算机科学先驱。提出图灵机等计算理论基础，在二战密码分析中发挥关键作用，其思想奠定现代计算机科学的重要基础。",
    "简·古道尔" to "灵长类学者与保育倡导者。自 1960 年起在贡贝长期研究黑猩猩，改变了人类对灵长类行为的认识，并长期推动自然保护教育。",
    "圣雄甘地" to "印度独立运动领袖。倡导非暴力不合作，以公民行动影响印度独立运动，并对全球和平与民权运动产生长期启发。",
    "克里斯蒂亚诺·罗纳尔多" to "职业足球运动员。多次获得金球奖并在英格兰、西班牙、意大利等联赛取得顶级成就，是当代足球最具全球影响力的运动员之一。",
    "罗杰·费德勒" to "职业网球运动员。获得 20 个大满贯男单冠军，以全面技术、长期稳定性和体育精神成为现代网球的重要代表人物。",
    "利昂内尔·梅西" to "职业足球运动员。多次获得金球奖，带领阿根廷队赢得 2022 年世界杯；其技术风格与持续高水平表现影响全球足球文化。",
    "麦当娜" to "歌手、词曲作者、演员与制作人。以持续的音乐风格更新、舞台形象和商业掌控力影响流行音乐、时尚表达与女性艺人自主性。",
    "阿黛尔" to "歌手与词曲作者。以《21》《25》《30》等专辑建立强烈的抒情创作风格，多次获得格莱美奖，代表作具有广泛的跨地域传播力。",
    "Lady Gaga" to "歌手、词曲作者与演员。以舞台视觉、流行音乐创作和《一个明星的诞生》等影视表演拓展跨媒介影响，也持续参与心理健康与平权倡导。",
    "金·卡戴珊" to "媒体人物、企业家与制作人。通过真人秀、社交媒体与消费品牌建立高影响力个人商业模式，改变了名人品牌与数字传播的结合方式。",
    "道恩·强森" to "演员、制作人与前职业摔角运动员。从 WWE 走向全球电影市场，形成体育娱乐与商业电影之间的代表性跨界路径。",
    "比尔·克林顿" to "美国政治人物。1993—2001 年担任美国总统，卸任后持续参与基金会和国际公共事务，在美国政治与全球公益领域保持影响力。",
    "纳尔逊·曼德拉" to "南非反种族隔离运动领袖。经历长期监禁后推动和解与民主转型，1994—1999 年担任南非总统，是全球人权与和平事业的重要象征。",
    "斯蒂芬·库里" to "职业篮球运动员。以远投能力重塑现代篮球的进攻空间和战术选择，带领金州勇士多次夺冠并扩大三分球在全球篮球中的影响。",
    "科比·布莱恩特" to "职业篮球运动员与创作者。帮助洛杉矶湖人赢得五次 NBA 总冠军，退役后继续从事影视与内容创作，影响一代篮球爱好者。",
    "迈克尔·菲尔普斯" to "奥运游泳运动员。获得 23 枚奥运金牌，是奥运史上最成功的运动员之一，以 2008 年北京奥运会八金成就广为人知。",
    "尤塞恩·博尔特" to "短跑运动员。保持男子 100 米、200 米世界纪录，连续三届奥运会获得 100 米与 200 米金牌，提升了田径短跑的全球关注度。",
    "杰夫·贝索斯" to "企业家。创办 Amazon，推动电子商务、云计算和物流体系的大规模发展；后通过 Blue Origin 参与商业航天领域。",
    "万民英" to "明代官员与命理学者。《三命通会》系统汇集并整理命理材料，后世长期将其视为研究传统命理的重要文献。",
    "刘伯温" to "元末明初政治家、文学家与谋士。参与明初建国相关政务，其著述与民间传说共同构成持续影响的历史文化形象。",
    "张居正" to "明代政治家、内阁首辅。万历初年主持改革，推行考成法、清丈田亩和一条鞭法，对晚明财政与行政制度产生深刻影响。",
    "严嵩" to "明代嘉靖朝内阁首辅。长期参与中枢政务，后因严氏父子贪腐与专权问题被罢黜，是研究嘉靖政治的重要人物。",
    "杨涟" to "明代官员、东林党重要人物。万历三十五年进士，历任给事中、左都御史；曾上疏列举魏忠贤二十四大罪，后遭诬陷死于狱中，是晚明政治冲突与东林党史的重要人物。",
    "魏忠贤" to "明代宦官。天启朝掌司礼监与东厂，结成阉党并迫害东林党人，形成晚明宦官专权的典型个案；崇祯即位后被贬谪并自缢。",
    "王象乾" to "明代边将、兵部尚书。长期镇守宣府、蓟辽等边地，重视长城防务、屯田与互市；其边疆治理经历横跨嘉靖至崇祯数朝，是研究晚明北疆防御的重要人物。",
    "王崇古" to "明代政治家、边臣。历任宣化、大同、山西等地总督，主张与俺答汗议和互市，促成“俺答封贡”；其政策对明代北部边境和平与蒙汉贸易具有重要影响。",
    "高拱" to "明代政治家、内阁首辅。隆庆朝主持吏治、边防与用人事务，重视课吏和边政；万历初以辅政大臣身份遭逐归里，其仕宦经历反映了晚明内阁与宫廷政治的复杂关系。",
    "李春芳" to "明代政治家、文学家。嘉靖二十六年状元，隆庆二年升任内阁首辅；其仕途兼具科举、翰林与中枢政务经历，是研究隆庆朝政局的重要人物。",
    "赵文华" to "明代官员。嘉靖朝依附严嵩集团，曾任工部尚书并主持宫殿营建；其与严党、营建腐败及嘉靖朝政治的关联，是研究明代中后期政治与财政问题的案例。",
    "贾似道" to "南宋末年权臣。曾任右丞相，在蒙古南侵与财政压力下推行公田法、推排法、打算法等措施；其政治得失及后世“奸臣”形象存在复杂史学评价。",
    "史可法" to "明末政治家、军事人物。崇祯元年进士，南明时期督师淮扬、驻守扬州，1645 年城破后拒降遇害；其遗著、衣冠冢与后世纪念共同构成明清鼎革史中的重要记忆。",
    "岳钟琪" to "清代将领。历经康熙、雍正、乾隆三朝，长期参与西南、西北边疆军事与治理事务；其战功、封爵和对边地稳定的作用，使其成为清前期汉人将领的重要代表。",
    "孙承宗" to "明末政治家、军事人物。天启朝受命经略辽东，整顿军饷、屯田与边防，并与袁崇焕等合作经营宁远等防线；其经历是理解晚明辽东军政的重要线索。",
    "夏言" to "明代政治家、文学家。两度入阁为首辅，曾获“上柱国”称号；其在嘉靖朝的政治主张、与严嵩的关系以及最终遭遇，是研究中期明朝权力结构的重要案例。",
    "杨一清" to "明代政治家、军事家。历仕成化、弘治、正德、嘉靖四朝，三次总制军务、两次入阁，长期主持西北边防、马政与吏治事务，是明中期文武兼资的重臣代表。",
    "文彦博" to "北宋政治家。历任宰相，前后任事约五十年；在王安石变法与元祐更化前后的政治争论中具有重要位置，晚年参与洛阳耆英会，留下士大夫政治与文化生活的典型材料。",
    "马新贻" to "清代官员、两江总督。同治年间遇刺身亡，案件因史料与后世演绎分歧而长期被称为“刺马案”；目前可核材料应以军机档案与证词为限，不能把传闻性动机当作事实。",
    "孔子" to "春秋末期思想家、教育家。以私学与儒家思想深刻影响中国教育、伦理和政治文化，后世尊为儒家主要代表人物。",
    "岳飞" to "南宋抗金将领。率岳家军参与多场抗金战事，军事声望与其冤案叙事共同构成中国历史记忆中的重要人物形象。",
    "左宗棠" to "晚清政治家、军事家与洋务派代表人物。主持新疆军务并参与西北建设，对晚清边疆治理和近代军事工业有重要影响。",
    "和珅" to "清代乾隆朝重臣。长期掌握财政与中枢事务，因专权贪腐在嘉庆初年被清算，是研究清代政治与财政的重要人物。",
    "康熙" to "清圣祖爱新觉罗·玄烨。1661—1722 年在位，平定三藩、统一台湾并与俄国缔结《尼布楚条约》，其统治对清初国家整合与边疆治理影响深远。",
    "雍正" to "清世宗爱新觉罗·胤禛。1722—1735 年在位，整顿吏治与财政，并设军机处强化中枢决策，是清代君主集权制度演进的重要阶段。",
    "乾隆" to "清高宗爱新觉罗·弘历。1735—1796 年在位，在疆域经营、典章文化与《四库全书》纂修方面留下重要影响，也使清代中后期的财政与政治问题日益显现。",
    "嘉庆" to "清仁宗爱新觉罗·颙琰。1796—1820 年在位，亲政后清算和珅，并面对财政困境、民变与吏治整顿等清中期结构性挑战。",
    "嘉靖" to "明世宗朱厚熜。1521—1567 年在位，长期主导明中期朝政；大礼议、倭患与内阁政治共同构成研究嘉靖一朝的重要线索。",
    "隆庆" to "明穆宗朱载坖。1567—1572 年在位，在位时间虽短，朝廷对外贸易与边务政策出现调整，是明中后期政治转折的一环。",
    "于谦" to "明代政治家、军事家。土木之变后主张固守北京、参与拥立景泰帝并统筹军务；其政治选择与身后冤案，使其成为明代忠臣叙事的重要人物。",
    "光绪" to "清德宗爱新觉罗·载湉。1875—1908 年在位，甲午战后支持维新变法；戊戌政变后长期受制，其政治处境映照晚清改革与权力结构的矛盾。",
    "同治" to "清穆宗爱新觉罗·载淳。1862—1874 年在位，幼年即位并长期处于两宫太后垂帘的政治格局中；同治中兴、洋务起步与晚清内外压力共同构成其时代背景。",
    "丘濬" to "明代政治家、思想家与学者。官至文渊阁大学士，著《大学衍义补》，围绕治国、财政与制度提出系统论述，是明代经世思想的重要代表。",
    "余华" to "当代作家。早期以先锋小说受到关注，后以《活着》《许三观卖血记》《兄弟》等作品形成广泛读者影响；其作品以克制叙述书写个体命运与社会变迁。",
    "杨振宁" to "理论物理学家、诺贝尔物理学奖获得者。其杨—米尔斯规范场论、宇称不守恒与杨—巴克斯特方程等工作，深刻影响粒子物理、统计物理与现代数学。",
    "郭晶晶" to "跳水运动员。三届奥运会共获四金二银，2008 年北京奥运会在女子三米板单人、双人项目再获两金，是中国跳水项目的重要代表人物。",
    "苏炳添" to "短跑运动员、体育教育工作者。2015 年成为首位在正式比赛跑进十秒的亚洲本土选手，2021 年东京奥运会跑出 9.83 创亚洲纪录并进入百米决赛。",
    "巩俐" to "电影演员。自《红高粱》进入公众视野后，以《秋菊打官司》《霸王别姬》《活着》等作品在华语电影和国际影展建立长期影响。",
    "陈奕迅" to "歌手、音乐人和演员。以粤语、国语流行音乐作品及持续的现场演出建立广泛影响，代表作和唱片制作跨越香港、内地、台湾与海外华语市场。",
    "周鸿祎" to "互联网与网络安全企业家。以 360 推广免费安全服务模式，参与中国个人网络安全产品的普及，并持续投入网络空间安全相关产业与公共议题。",
    "钱学森" to "科学家、航空航天与系统工程领域的重要开拓者。其在空气动力学、工程控制论、导弹与航天器研制组织方面的工作，对中国现代航天与国防科技体系建设产生深远影响。",
    "邓稼先" to "理论物理学家、核物理学家。长期参与中国核武器理论研究与组织工作，在关键基础理论、工程计算与技术决策中作出重要贡献，是“两弹一星”事业的重要科学家。",
    "华罗庚" to "数学家、数学教育家。其数论、代数、几何与应用数学研究，以及对数学普及和学科建设的推动，深刻影响中国现代数学发展。",
    "林巧稚" to "医学家、医学教育家，中国现代妇产科学的重要开拓者。她长期在临床、妇幼保健、妇科肿瘤与人才培养方面工作，并推动妇产科学科建设；以专业实践与公共服务影响多代医务工作者。",
    "李政道" to "理论物理学家、诺贝尔物理学奖获得者。其关于弱相互作用宇称不守恒等研究是现代粒子物理的重要成果；此后长期推动高能物理基础设施、国际合作与青年人才培养，对中国科学教育发展具有持续影响。",
    "钟南山" to "呼吸病学专家、医学教育工作者。长期从事呼吸系统疾病研究，在 SARS 与新冠疫情防控、重症救治和科研组织等工作中发挥重要作用；其专业表达与公共卫生实践使其成为当代中国医学界的代表人物。",
    "吴孟超" to "肝胆外科专家、中国肝脏外科的重要开拓者。其围绕肝脏解剖、止血技术、肝癌外科治疗与学科组织的长期工作，推动了中国肝胆外科临床与研究发展。",
    "李四光" to "地质学家、地质教育家。其地质力学研究与对矿产、石油、地热和地震问题的组织工作，对中国现代地质科学与资源调查具有奠基性影响。",
    "钱三强" to "核物理学家、原子能科学事业的重要组织者。其在裂变研究、科研机构建设、核技术人才组织及“两弹”攻关中的工作，对中国核科学与国防科技体系建设影响深远。",
    "王选" to "计算机科学家、汉字激光照排技术的重要开拓者。其带领团队推动中文出版印刷从铅与火向数字化转型，并将科研、产业化与知识产权实践结合，深刻改变了中国报业和出版业的信息化进程。",
    "邓小平" to "中国近现代政治人物。其长期参与革命、国家建设和改革开放时期的重大决策与制度实践，对中国当代政治、经济和社会发展产生重要影响。",
    "江泽民" to "中国近现代政治人物、工程技术背景公共管理者。其经历横跨高等教育、工业技术与国家公共事务，是理解20世纪后期中国政治与经济发展历程的重要人物。",
    "朱镕基" to "中国近现代政治人物、经济管理背景公共管理者。其长期参与经济计划、城市管理、金融与宏观经济治理等公共事务，是研究中国20世纪末经济改革与政府治理的重要人物。",
    "玛丽·居里" to "物理学家、化学家。围绕放射性现象及钋、镭的研究改变了现代物理与化学的发展轨迹；她是首位两获诺贝尔奖的人，其科研、教学与医学影像服务亦留下深远影响。",
    "伊丽莎白二世" to "英国君主制下的重要公共人物。在长达数十年的公共服务生涯中，她见证并参与英国及英联邦公共仪式、国际访问和君主制制度延续；其准确出生分钟有官方资料可核。",
    "贝利" to "职业足球运动员。以技术、进球能力和世界杯战绩成为世界足球史的重要代表人物；其三次夺得世界杯冠军的经历在国际足联档案中有明确记载。",
    "方济各" to "天主教第266任教宗、全球公共宗教人物。其经历涵盖耶稣会教育、布宜诺斯艾利斯教区治理与世界性宗教公共事务；任内持续就贫困、和平、生态与社会关怀发表立场。",
    "马拉拉·优素福扎伊" to "教育权倡导者、诺贝尔和平奖得主。她以个人经历推动女孩教育与儿童受教育权成为国际公共议题，并以演讲、基金会和公共倡议持续参与教育公益。",
    "加夫列尔·加西亚·马尔克斯" to "哥伦比亚小说家、新闻工作者、诺贝尔文学奖得主。以《百年孤独》等作品建立魔幻现实主义的世界性影响，将拉丁美洲历史、社会经验与叙事创新带入更广泛的文学读者群。",
    "托妮·莫里森" to "美国小说家、编辑、教师、诺贝尔文学奖得主。其作品以复杂叙事呈现非裔美国人的历史、家庭记忆与现实处境；她也通过出版编辑与高等教育持续影响英语文学和文化研究。",
    "旺加里·马塔伊" to "肯尼亚生物学家、环保与民主倡导者、诺贝尔和平奖得主。她把植树、妇女参与、社区发展与民主、人权议题结合，创办绿带运动并推动环境保护成为社会公共行动。",
    "萨姆·奥尔特曼" to "人工智能创业者、OpenAI 首席执行官。其职业经历横跨创业孵化、投资和生成式人工智能产品化；在大模型技术走向广泛公众应用的阶段，参与推动 OpenAI 的组织、产品与生态发展。",
    "拉里·佩奇" to "计算机科学家、互联网企业家、Google与Alphabet共同创始人。其与布林共同推动以链接结构为核心的搜索技术，并在搜索、移动计算、云服务、人工智能及前沿科技投资等方向持续影响全球互联网产业。",
    "谢尔盖·布林" to "计算机科学家、互联网企业家、Google与Alphabet共同创始人。其与佩奇共同开发Google的早期搜索技术，并长期参与Alphabet的技术探索与前沿项目布局，对搜索、人工智能和科学研究资助等领域具有持续影响。",
    "黄仁勋" to "半导体企业家、英伟达联合创始人兼CEO。其长期推动GPU从图形计算扩展到并行计算、加速计算和人工智能基础设施，对当代AI训练与推理产业链具有关键影响。",
    "王传福" to "新能源与制造业企业家、比亚迪创始人兼董事长。其从充电电池技术与制造起步，推动企业进入汽车、动力电池、储能和零排放交通等领域，是观察中国新能源产业化的重要人物。",
    "德米斯·哈萨比斯" to "人工智能研究者、Google DeepMind联合创始人兼CEO。其团队从AlphaGo的强化学习突破延伸至AlphaFold的蛋白质结构预测，把机器学习推进到生命科学研究场景；2024年因AlphaFold相关工作获诺贝尔化学奖。",
    "苏姿丰" to "半导体企业家、AMD董事长兼CEO。她长期从事芯片工程和企业管理，带领AMD在高性能计算、数据中心与AI加速器等方向推进产品和生态布局，是当代全球半导体产业的重要管理者。",
    "杰弗里·辛顿" to "计算机科学家、人工神经网络与机器学习研究的重要代表人物。其围绕统计物理和神经网络的长期研究，为当代深度学习的理论与方法演进提供了关键基础；2024年获诺贝尔物理学奖。",
    "约翰·古迪纳夫" to "材料科学家、锂离子电池技术的重要奠基者。其高电压钴氧化物正极研究推动了可充电锂电池的关键跃迁，后续深刻影响消费电子、电动汽车和可再生能源储能；2019年获诺贝尔化学奖。",
    "吉野彰" to "化学家、锂离子电池产业化的重要推动者。其在1985年制成首个可商业化锂离子电池，为消费电子、电动汽车和新能源储能的普及奠定关键技术基础；2019年获诺贝尔化学奖。",
    "约翰·霍普菲尔德" to "物理学家、神经网络研究的重要先驱。其1982年提出的Hopfield网络把物理系统的集体行为思想用于模式存储与重建，成为现代机器学习和人工神经网络的重要理论来源；2024年获诺贝尔物理学奖。",
    "斯坦利·惠廷厄姆" to "材料化学家、可充电锂电池技术的重要先驱。其在1970年代发展二硫化钛正极与可充电锂电池，为后续锂离子电池奠定基础，影响移动电子、电动汽车和储能产业；2019年获诺贝尔化学奖。",
    "大卫·贝克" to "计算生物学家、蛋白质设计研究的重要代表人物。其用计算方法设计新蛋白质，使AI、算法与生命科学实验形成更紧密的协同，服务于药物、疫苗、材料和传感器等方向；2024年获诺贝尔化学奖。",
    "林纳斯·托瓦兹" to "软件工程师、Linux内核与Git创始人。其推动的开源协作模式深刻影响服务器、云计算、移动设备、嵌入式系统和全球软件开发流程；Linux和Git已成为现代数字基础设施的重要组成部分。",
    "马化腾" to "互联网企业家、腾讯主要创办人。其创业和产品体系覆盖即时通信、社交网络、数字内容、游戏、支付、云服务等领域；QQ与微信等产品深刻影响中国互联网的连接方式与数字生态。",
    "桑达尔·皮查伊" to "科技企业管理者。其职业经历围绕搜索、浏览器、移动操作系统、云计算和人工智能等互联网基础平台展开；在Alphabet架构下参与统筹Google核心产品和长期技术投入。",
    "萨提亚·纳德拉" to "科技企业管理者。其长期参与企业软件、云计算与开发者平台建设；在微软进入云服务与生成式人工智能快速发展的阶段，参与推动公司产品、基础设施和生态布局。",
    "蒂姆·库克" to "科技企业管理者、供应链运营专家。其在Apple长期负责全球运营，并在智能手机、可穿戴设备、服务业务和全球供应链体系持续演进的时期担任关键管理角色。",
    "孙正义" to "科技企业家、投资者。其通过软银推动通信、互联网、芯片和人工智能投资布局，并通过ARM、愿景基金等项目深度参与全球科技资本和基础设施的发展。",
    "迈克尔·戴尔" to "科技企业家、Dell Technologies创始人。其直销与按需供应链模式改变了个人电脑销售和制造组织方式，后续业务延伸至服务器、存储、网络和企业IT基础设施，是观察硬件产业规模化与企业数字化的重要人物。",
    "杰克·多西" to "互联网与金融科技企业家。其参与创办Twitter，后通过Square（现Block）推动小微商户移动支付和数字金融服务；在社交网络、支付和去中心化技术议题中持续具有影响。",
    "埃文·斯皮格尔" to "移动互联网企业家、Snap联合创始人。其以图片和短视频即时通信产品切入年轻用户社交场景，并推动相机、AR滤镜、内容分发和创作者生态的产品化发展。",
    "里德·哈斯廷斯" to "软件与流媒体企业家、Netflix共同创始人。其从软件工具创业延伸至DVD租赁和流媒体平台，参与推动内容分发从实体介质走向订阅式数字服务、原创内容和全球化运营。",
    "丹尼斯·里奇" to "计算机科学家、Unix和C语言的关键创造者。其与同事建立的操作系统与编程语言组合，显著降低了软件在不同硬件平台之间移植的门槛，持续影响操作系统、编译器、网络服务与现代软件工程。",
    "肯·汤普森" to "计算机科学家、Unix系统共同创造者。其在操作系统、编程语言和计算机国际象棋等方向的工作，推动了Unix工具哲学和简洁软件设计方法的形成，对后续服务器、网络设备与开源生态具有深远影响。",
    "文特·瑟夫" to "计算机科学家、互联网协议共同设计者。其与罗伯特·卡恩共同推动TCP/IP，把异构网络互联的技术构想落实为可扩展的通信协议体系；这套基础设施成为全球互联网持续演进的底层支柱。",
    "罗伯特·卡恩" to "计算机科学家、互联网协议共同设计者。其在ARPANET与TCP/IP发展中承担关键角色，使不同网络能够按共同规则互联；这一工作与瑟夫共同构成现代互联网协议栈的历史基础。",
    "詹姆斯·高斯林" to "软件工程师、Java语言的核心创造者之一。其围绕语言、虚拟机和跨平台运行环境的设计，推动“编写一次、到处运行”的工程实践，长期影响企业软件、移动开发、服务器和开发者生态。",
    "格蕾丝·霍珀" to "计算机科学家、编译器与早期程序语言的重要推动者。她把“自动编程”从概念推向工程实践，参与塑造了面向商业数据处理的COBOL生态，也以长期的技术教育和组织工作影响后续软件行业。",
    "海蒂·拉玛" to "演员兼发明者、跳频通信技术共同发明人。她与乔治·安塞尔提出的频率跳变方案，旨在降低无线电控制信号被侦测或干扰的风险；相关思想后来成为无线通信技术史的重要一环。",
    "约翰·麦卡锡" to "计算机科学家、人工智能早期奠基者。其提出并推动“人工智能”作为研究领域，发展Lisp及基于逻辑的知识表示思路，对自动推理、符号主义AI与计算机科学教育产生长期影响。",
    "凯瑟琳·约翰逊" to "数学家、航天轨道计算先驱。她在早期载人航天中进行关键轨道与任务计算，并参与月球任务对接、航天飞机和地球资源卫星研究；其工作体现了严谨计算、独立核验与工程安全在复杂系统中的价值。",
    "玛丽·杰克逊" to "航空航天工程师、NASA首位非裔女性工程师。她从计算工作转入高速风洞实验和工程训练，突破当时的种族与性别壁垒；后转向平等机会工作，为更多女性与少数族裔进入科技岗位创造条件。",
    "拉里·埃里森" to "科技企业家、Oracle共同创始人。其围绕关系型数据库、企业软件与云基础设施的长期布局，推动了数据管理从专用系统走向大规模商业部署；Oracle在数据库、企业应用与云服务领域持续具有全球影响。",
    "马克·安德森" to "程序员、互联网企业家与投资者。其参与开发的Mosaic让图文一体的Web浏览体验走向大众，Netscape推动早期商业互联网发展，随后又在云服务和科技投资领域持续活跃。",
    "蒂姆·伯纳斯-李" to "计算机科学家、万维网发明者。其提出并实现URI、HTTP、HTML及首个Web客户端和服务器，使超文本信息能以开放协议在全球网络中互联；他后来创建W3C，持续推动Web标准与数据自主权。",
    "比雅尼·斯特劳斯特鲁普" to "计算机科学家、C++语言创造者。其将高层抽象与系统级性能需求结合，塑造了大量操作系统、浏览器、游戏引擎、金融系统与嵌入式软件所依赖的编程工具链；C++对现代系统软件生态影响深远。",
    "罗伯特·梅特卡夫" to "计算机科学家、以太网发明者。其以分组和分布式控制机制将局域网互联变为可扩展的工程标准，并通过3Com推动产业化；以太网至今仍是企业网络、数据中心与家庭连接的重要基础。",
    "戈登·摩尔" to "半导体科学家、Intel共同创始人。“摩尔定律”概括了集成电路复杂度持续提升的产业趋势，成为数十年芯片设计、制造、成本下降与计算能力普及的重要参照；其产业组织工作也深刻影响硅谷发展。",
    "伊万·萨瑟兰" to "计算机科学家、交互计算机图形学先驱。其Sketchpad将图形对象、光笔交互、层级结构与几何变换结合，为计算机辅助设计、图形用户界面、三维图形和虚拟现实奠定关键思想基础。",
    "罗伯特·诺伊斯" to "物理学家、企业家、集成电路共同发明者之一。其硅基单片集成电路实践与Fairchild、Intel的创业，连接了实验室半导体技术和大规模产业化，是个人计算、通信设备和现代电子产业的重要历史节点。",
    "费德里科·法金" to "物理学家、工程师与微处理器先驱。其MOS硅栅技术和Intel 4004设计工作，帮助把可编程计算能力压缩到商用芯片；后续Z80与Synaptics的工作又延伸到个人计算和人机交互设备。",
    "斯蒂夫·沃兹尼亚克" to "电子工程师、Apple共同创始人、个人计算机先驱。其对Apple I和Apple II的硬件设计，把面向爱好者的微型计算机推进为更易使用的消费产品；彩色图形、内置键盘等设计也深刻影响个人计算机普及。",
    "唐纳德·克努特" to "计算机科学家、算法分析和数字排版的重要推动者。其《计算机程序设计艺术》系统整理算法知识，TeX与METAFONT则使高质量技术排版成为开放软件实践；其工作连接了理论计算、软件工程与学术传播。",
    "戈登·贝尔" to "计算机工程师、体系结构设计者与技术组织者。他在DEC参与PDP与VAX等小型机体系发展，推动计算能力从昂贵大型机走向更多实验室和机构；后续在超级计算、研究组织和计算历史保存方面持续投入。",
    "查尔斯·巴赫曼" to "计算机工程师、数据库管理系统先驱。其Integrated Data Store等工作把数据的组织、关联和高效访问带入早期企业计算，推动数据库管理、事务处理和信息系统架构的发展；1973年获图灵奖。",
    "丹·布里克林" to "软件企业家、电子表格先驱。VisiCalc把自动重算与“假设分析”带到个人电脑，使商业用户第一次能直观处理财务与经营模型，成为推动个人计算机进入办公室的重要应用；他之后继续探索笔计算和网站发布工具。",
    "马克·贝尼奥夫" to "企业软件创业者、Salesforce联合创始人兼CEO。其围绕CRM和云端订阅软件建立企业服务模式，并将客户数据、工作流与平台生态整合为长期产品方向；Salesforce对企业云软件和“软件即服务”商业实践具有代表性。",
    "袁征" to "工程师、Zoom创始人兼CEO。其从WebEx与Cisco的实时通信工程经验出发，推动Zoom成为视频会议、云电话和协作工具的重要平台；在全球远程协作需求增长阶段，产品的易用性和稳定性获得广泛采用。",
    "斯图尔特·巴特菲尔德" to "互联网产品创业者、Flickr共同创始人、Slack创始人。其产品工作从图片分享延伸到团队协作通信，强调围绕频道、搜索与应用集成组织工作流；Slack成为数字化团队协作的重要工具之一。",
    "凯文·斯特罗姆" to "程序员、Instagram共同创始人及前CEO。其以简洁的移动拍摄、滤镜和社交分享体验推动视觉化社交网络快速普及，Instagram后续成为全球移动互联网内容创作与商业传播的重要平台。",
    "苏珊·沃西基" to "科技管理者、前YouTube CEO。她早期参与Google的广告与视频业务，后在YouTube的全球化、创作者生态、版权与内容平台治理中长期担任领导角色；其职业轨迹体现了搜索广告、在线视频和平台经济的连续演变。",
    "王兴" to "互联网连续创业者、美团创始人兼董事长和CEO。其从校园社交、即时信息服务到本地生活平台的多次创业，推动餐饮外卖、到店服务和即时零售等场景的数字化连接；美团已成为中国本地生活服务的重要平台之一。",
    "李书福" to "企业家、吉利控股集团董事长。其从制造业创业进入汽车产业，推动吉利从民营汽车企业成长为覆盖乘用车、商用车与多品牌业务的汽车集团；其并购和国际化经营也成为中国汽车产业全球化的代表案例之一。",
    "何小鹏" to "互联网与智能电动车创业者、小鹏汽车联合创始人、董事长兼CEO。其先以UC优视参与移动互联网产品发展，后转向智能电动车，把软件、辅助驾驶和整车产品结合，代表了中国智能汽车产业由互联网创业者参与推动的一条路径。",
    "李斌" to "企业家、蔚来创始人、董事长兼CEO。其先在汽车互联网服务领域创业，后进入智能电动车产业，并围绕整车、补能服务与用户社区探索服务体系；蔚来是中国新能源汽车产业中具有代表性的品牌之一。",
    "李彦宏" to "技术创业者、百度联合创始人、董事长兼CEO。其早期搜索技术工作和百度创业推动了中文搜索服务发展，后续业务延展到人工智能、自动驾驶和智能云等方向；其职业路径是观察中国搜索与AI产业演进的重要案例。",
    "丁磊" to "互联网企业家、网易创始人、董事兼CEO。其从早期门户和电子邮件服务起步，带领网易发展网络游戏、音乐、教育、电商等数字内容与服务业务；网易是中国早期互联网商业化的重要参与者之一。",
    "张朝阳" to "互联网企业家、搜狐创始人、董事局主席兼CEO。其在中国互联网早期创建门户与搜索服务，搜狐后拓展新闻、视频和游戏等业务；其创业经历是观察中国门户网站、内容媒体和互联网公司上市发展的代表案例。",
    "王小川" to "技术创业者、百川智能创始人兼CEO。其长期参与中文搜索、输入法和浏览器产品研发与管理，后转向通用人工智能和大模型创业；其从搜索技术到语言AI的路径，体现了中国互联网技术创业向生成式AI延展的一个方向。",
    "程维" to "出行平台创业者、滴滴创始人、董事长兼CEO。其以移动互联网连接乘客、司机和城市交通服务，并持续将数据与人工智能用于出行调度和智慧交通探索；滴滴是中国移动出行平台发展中的代表性企业。",
    "陈景润" to "数学家、解析数论研究者。其围绕哥德巴赫猜想取得的“1+2”结果被称为陈氏定理，是筛法理论和数论研究的重要成果；长期的专注研究也使其成为中国科学精神的代表人物之一。",
    "于敏" to "核物理学家、中国科学院院士。其在原子核理论和氢弹原理突破中作出关键贡献，并长期参与核武器理论研究和国防高技术发展；其学术与工程工作是中国核科学事业的重要组成部分。",
    "孙家栋" to "航天技术专家、中国科学院院士。其长期参与导弹、人造卫星、北斗导航和探月工程的总体技术工作，连接了中国航天多个关键阶段；其工程组织与技术贡献是中国卫星和深空探测事业的重要基础。",
    "钱七虎" to "防护工程学家、中国工程院院士。其长期从事工程防护、地下工程与岩石力学研究，建立并发展相关理论、试验和设计方法；其工作兼具国防工程与重大民生工程的应用价值。",
    "李德仁" to "摄影测量与遥感学家、中国科学院和中国工程院院士。其长期研究测量误差理论、摄影测量、遥感与地理信息技术，推动测绘遥感信息工程及其在城市与空间信息领域的应用；其工作为时空信息技术发展提供了重要基础。",
    "顾方舟" to "病毒学家、免疫学家与医学教育家。其参与研制和推广脊髓灰质炎疫苗，为中国脊髓灰质炎防控作出重要贡献，被公众称为“糖丸爷爷”；其工作体现了公共卫生、疫苗研发和大规模预防接种的结合。",
    "王振义" to "内科血液学专家、中国工程院院士。其在白血病诱导分化治疗、血栓与止血等领域作出开创性工作，推动急性早幼粒细胞白血病治疗方案发展；其研究体现了基础研究与临床医学转化的结合。",
    "韩启德" to "病理生理学家、中国科学院院士。其长期从事分子药理学和心血管基础研究，并参与医学教育、学科交叉与科技组织工作；其职业经历呈现了基础医学研究与高等医学教育的结合。",
    "李兰娟" to "传染病学专家、中国工程院院士。其长期从事感染性疾病、肝衰竭救治和肝病微生态研究，并参与公共卫生应急工作；其科研与临床实践是中国感染病防治领域的重要案例。",
    "樊锦诗" to "考古学家、敦煌研究院名誉院长。其长期扎根敦煌，从事石窟考古、科学保护、管理与文化遗产传承工作，并推动敦煌研究的系统化发展；她的职业生涯是中国文化遗产保护的重要代表。",
    "刘国梁" to "乒乓球运动员、教练与管理者。作为运动员完成世乒赛、世界杯和奥运会“大满贯”，转任教练和协会管理者后，持续参与中国乒乓球与世界职业赛事体系建设。",
    "吴京" to "演员、导演与出品人。以武术动作表演起步，后自导自演《战狼》系列并参与《长津湖》等大片，成为中国商业电影中具有代表性的动作片创作者。",
    "崇祯" to "明思宗朱由检。1628—1644 年在位，面对财政、农民起义与后金军事压力，曾整肃阉党、试图整顿朝政；北京失守后自缢，明朝由此覆亡。",
    "明建文帝" to "明惠宗朱允炆。1399—1402 年在位，试图通过削藩强化中央集权；靖难之役失败后下落成谜，其短暂统治与政治理想长期受到历史讨论。",
    "朱棣" to "明成祖朱棣。1403—1424 年在位，迁都北京、编纂《永乐大典》并派郑和下西洋，对明代政治中心、典籍文化与对外交往产生深远影响。",
    "皇太极" to "清太宗皇太极。继承后金统治后推进制度建设，1636 年改国号为清、族名为满洲，为清朝入关前的国家建构奠定基础。",
    "顺治" to "清世祖爱新觉罗·福临。1644—1661 年在位，是清入关后的第一位皇帝；其统治时期清朝逐步建立在全国的统治秩序。",
    "咸丰" to "清文宗爱新觉罗·奕詝。1851—1861 年在位，面对太平天国与列强侵略等内外危机；其晚年避居热河，身后引出辛酉政变与清廷权力重组。",
    "道光" to "清宣宗爱新觉罗·旻宁。1821—1850 年在位，曾整顿漕运、盐政与吏治；鸦片战争及《南京条约》使清朝进入近代外部冲击急剧加深的阶段。",
    "慈禧太后" to "晚清实际掌权者之一。1861 年辛酉政变后长期参与或主导清廷决策，历经同治、光绪两朝；其统治与晚清改革、对外危机和权力结构密切相关。",
    "溥仪" to "清末代皇帝。幼年即位，1912 年退位；此后经历伪满洲国、战犯改造与成为公民等人生转折，其个人经历折射近现代中国的剧烈变迁。",
    "朱元璋" to "明太祖朱元璋。出身社会底层，参与元末起义并建立明朝；其强化中央集权、整顿制度与严酷统治方式，对明代政治结构影响深远。",
    "朱常洛" to "明光宗朱常洛。1620 年在位仅一月，梃击、红丸、移宫三案与其人生密切相连；短暂即位反映万历末年严重的宫廷与政治危机。",
    "朱由校" to "明熹宗朱由校。1621—1627 年在位，魏忠贤等宦官专权、东林党受迫害与辽东危机加重，使天启朝成为晚明政治失序的重要阶段。",
    "朱翊钧" to "明神宗朱翊钧。1573—1620 年在位，早期在张居正辅政下出现改革成效，后期财政、党争与边防问题加深，对晚明困局影响重大。",
    "正德" to "明武宗朱厚照。1506—1521 年在位，宠信宦官与个人逸乐广受史家批评；其朝亦面临刘瑾专权、宁王之乱等政治事件。",
    "忽必烈" to "元世祖忽必烈。建立元朝并定都大都，完成全国统一；其在制度、城市营建与多民族统治上的实践，深刻塑造了元代国家形态。",
    "元顺帝" to "元惠宗妥懽帖睦尔。1333—1370 年在位，面对财政、灾荒与红巾军起义等多重危机；1368 年北迁后，元朝在中原的统治结束。",
    "武则天" to "唐代政治家、武周开国君主。690—705 年在位，是中国历史上唯一的正统女皇帝；其用人、科举与政治整合具有持续影响，也伴随严厉的权力斗争。",
    "汉世祖光武帝" to "东汉开国皇帝刘秀。25 年建立东汉，在位期间推行休养生息、整顿吏治与减轻赋役等措施，史称“光武中兴”。",
    "俞大猷" to "明代抗倭名将、武术家。转战江浙闽粤，与戚继光并称；其军事实践和《剑经》等著述，对东南海防与武术史均有重要影响。",
    "倪元璐" to "明末政治家、书法家与画家。崇祯朝任户部尚书兼翰林院学士，国变时自缢；其书画以峻拔奇崛著称，是晚明艺术史的重要人物。",
    "史可法" to "明末政治家、军事人物。明亡后参与拥立弘光政权，出守扬州并在城陷后遇害；其政治选择与身后形象构成明清易代史的重要记忆。",
    "卢象升" to "明末将领。多次参与平乱和边防事务，后在巨鹿抗清战死；其治军、守边经历及悲剧结局，是晚明军事困局的重要个案。",
    "严世蕃" to "明代嘉靖朝官员、严嵩之子。依父势升迁并深度介入中枢事务，后因贪腐、专权等问题被查办处决，是嘉靖后期权力运行的重要反面个案。",
    "刘瑾" to "明代宦官，正德初年掌司礼监并一度势倾朝野。其专权及最终因谋逆案被处死，是明中期宦官政治的重要事件。",
    "周延儒" to "明末内阁首辅。两度入阁，曾参与崇祯朝的政务调整；第二次任首辅时因督军失职和欺君等问题被追究并赐死，反映晚明中枢政治困境。",
    "关羽" to "三国时期蜀汉将领。以镇守荆州、北伐襄樊等经历著称；其忠义形象经史传、文学与民间信仰长期传播，成为中国传统文化中具有广泛影响力的历史人物。",
    "冯保" to "明代宦官，万历初年任司礼监掌印并兼掌东厂，与张居正、李太后共同处于幼年万历帝的辅政结构中；其权力活动是研究晚明内廷政治的重要线索。",
    "吴中行" to "明代官员。因上疏反对张居正“夺情”而受廷杖、罢职，张居正去世后复出任职；其经历呈现了万历初年士大夫与中枢权力的张力。",
    "范蠡" to "春秋末期政治家、军事家与经济人物。辅佐越王勾践成就霸业后离开政坛，经商而富，后世称“陶朱公”；其功成身退和经世、经营形象在中国商业文化中影响深远。",
    "王永庆" to "台塑企业创办人。自米店经营起步，1954 年创办台塑前身，长期推动石化产业扩张，并投入教育、医疗与公益事业，对台湾工业发展具有重要影响。",
    "包玉刚" to "航运企业家。1955 年创办环球航运，带领企业发展为大型独立商船队；亦长期支持教育事业，其事业经历是华人航运业国际化的重要案例。",
    "商辂" to "明代大臣。科举中连中乡试、会试、殿试第一，土木之变后主张守京并参与政务；晚年任内阁首辅，对明中期边防、吏治与宦官政治均有建言。",
    "孙承宗" to "明末大臣、军事统帅。督师辽东期间练兵、屯田、修筑城堡并整顿防务；其用兵与去职经历，是理解明末辽东军政困局的重要线索。",
    "张之洞" to "晚清洋务派代表人物。主持兴办近代工业、教育与军事设施，提出“中学为体，西学为用”，对近代中国工业化和教育转型具有重要影响。",
    "岳钟琪" to "清代将领。历经康熙、雍正、乾隆三朝，参与西藏、青海及大金川等军事事务；其经历是研究清代西北、西南军事与边疆治理的重要个案。",
    "张爱玲" to "现代作家。以《传奇》《倾城之恋》《金锁记》《半生缘》等作品形成独特的都市叙事与语言风格，对华语小说和流行文化产生长期影响。",
    "徐志摩" to "新月派代表诗人、散文家。留学剑桥期间开始新诗创作，参与发起新月社；其诗歌推动了现代白话诗的抒情表达与形式探索。",
    "朱自清" to "现代散文家、诗人、学者。以《背影》《荷塘月色》《春》等作品广为传诵，兼具新文学创作、古典文学研究和教育实践的影响力。",
    "沈从文" to "现代作家、历史文物研究者。以湘西题材小说建立独特文学世界，后转向文物与中国古代服饰研究，形成跨文学与学术的长期贡献。",
    "王安石" to "北宋政治家、文学家、思想家。“王安石变法”推动财政、经济与行政制度调整；其散文与诗歌亦为唐宋八大家传统的重要组成。",
    "朱熹" to "南宋思想家、教育家。系统阐发理学，长期讲学、著述并参与地方治理；其《四书》注释和教育思想深刻影响中国及东亚儒学传统。",
    "安妮宝贝" to "当代作家、散文作者，后以“庆山”为笔名继续创作。自网络写作起步，以《告别薇安》《彼岸花》《莲花》《春宴》等作品形成鲜明的都市与个人叙事风格。",
    "王世贞" to "明代文学家、史学家、书画评论家。与李攀龙共主文坛，后独主文坛二十余年；其复古文学主张和大量著述对晚明文坛影响显著。",
    "海子" to "当代诗人。以《亚洲铜》《面朝大海，春暖花开》等作品进入广泛读者视野，其诗歌围绕土地、太阳、河流与精神理想展开，身后持续影响当代诗歌阅读。",
    "张瑞图" to "晚明书法家、画家与官员。行草书以奇逸峻拔著称，与董其昌等并列晚明书坛重要人物；其仕途也与天启朝内阁政治密切相关。",
    "朱用纯" to "明末清初学者，号柏庐。潜心程朱理学，主张知行并进；《朱子治家格言》流传广泛，是中国传统家训文化的重要文本。",
    "刘德华" to "演员、歌手与电影制作人。长期活跃于华语影视和流行音乐领域，以商业电影、音乐作品及持续的公众影响力成为香港流行文化的重要代表人物。",
    "周星驰" to "演员、导演、编剧与制作人。以无厘头喜剧建立独特电影语言，并凭《少林足球》《功夫》等作品拓展华语商业喜剧的国际传播。",
    "周杰伦" to "创作歌手、音乐制作人、演员与导演。自 2000 年出道以来，以 R&B、说唱、摇滚与中国风元素融合的创作，深刻影响华语流行音乐。",
    "姚明" to "篮球运动员、管理者与公益参与者。以 NBA 首位外籍状元身份开启职业生涯，入选奈史密斯篮球名人堂，成为中国篮球国际化的重要象征。",
    "林丹" to "羽毛球运动员。获得两届奥运会男单冠军，并在世锦赛、汤姆斯杯、苏迪曼杯及亚运会等赛事取得顶级成绩，是中国羽毛球代表性运动员。",
    "李娜" to "职业网球运动员。2011 年夺得法网女单冠军，成为首位获得网球大满贯单打冠军的亚洲球员；其职业经历推动了中国网球的公众关注与职业化讨论。",
    "孙杨" to "游泳运动员。主攻中长距离自由泳，在奥运会与世锦赛多次夺冠并创纪录，是中国男子游泳国际赛场的重要代表人物。",
    "梅兰芳" to "京剧表演艺术家、梅派创始人，位列“四大名旦”之首。其表演和新腔实践推动旦角艺术形成独立流派，并多次将京剧带到海外，是20世纪中国戏曲走向世界的重要代表。",
    "张国荣" to "香港歌手、演员。以流行音乐和电影表演并行的职业路径成为华语流行文化的重要代表；其《阿飞正传》《霸王别姬》等银幕角色及与王家卫的合作，持续进入电影研究与观众记忆。",
    "崔健" to "中国内地摇滚音乐人、创作歌手。《一无所有》等作品在1980年代中后期推动摇滚乐进入公众视野，长期被视为中国摇滚乐发展中的代表性人物。",
    "王守仁" to "明代思想家、教育家、政治人物，世称王阳明。其“心学”以“致良知”“知行合一”等命题影响深远；同时参与地方治理与平叛，留下兼具思想史和政治史意义的实践记录。",
    "欧阳修" to "北宋政治家、文学家、史学家，谥文忠，世称欧阳文忠公。其领导北宋诗文革新，主修《新唐书》、独撰《新五代史》，并以《醉翁亭记》等作品奠定古文传统的重要位置。",
    "真德秀" to "南宋后期思想家、官员，号西山。其《大学衍义》系统整理“帝王之学”，《文章正宗》等著作流传广泛；在泉州任职期间的海防、商税与港口治理实践，也具有地方史价值。",
    "琼瑶" to "作家、编剧、影视制作人。以言情小说及影视改编形成持续的华语大众文化影响，《窗外》《还珠格格》等作品跨越出版、电影和电视媒介，塑造了数代观众的情感叙事记忆。",
    "弗里德里希·尼采" to "德国哲学家、古典语文学者与文化批评家。其对传统道德、宗教和现代性的批判，以及对自我、价值重估等问题的论述，长期影响哲学、文学和文化理论。",
    "弗里德里希·恩格斯" to "德国思想家、社会理论家，与马克思长期合作。共同发表《共产党宣言》，并参与《资本论》等著作的整理、传播与编辑工作，对马克思主义理论传统形成具有重要影响。",
    "罗宾德拉纳特·泰戈尔" to "印度诗人、小说家、教育实践者与思想家。1913年获诺贝尔文学奖；其诗歌、歌曲、小说、戏剧与圣地尼克坦教育实践，使他成为近现代印度文化的重要代表。",
    "阿道夫·希特勒" to "纳粹党领袖、德国独裁者。1933年上台后摧毁魏玛民主制度并建立纳粹独裁统治；其政权发动侵略战争，实施对犹太人及其他受害群体的系统性迫害和种族灭绝，是二十世纪历史灾难的主要责任者。",
    "约翰·D·洛克菲勒" to "美国企业家、标准石油创办人与早期大型慈善家。标准石油的发展深刻影响现代石油业和美国反垄断史；其慈善捐赠与基金会实践也推动医学、教育和公共事业发展。",
    "托马斯·爱迪生" to "美国发明家、企业组织者。围绕留声机、白炽灯及配套电力系统进行研发和产业化，推动照明、电力、录音与早期电影技术走向规模化应用。",
    "董浩云" to "航运企业家。创办并经营中国航运、东方海外等企业，推动华人航运业的远洋化、集装箱化和国际化；其经营轨迹是20世纪亚洲航运史的重要个案。",
    "荣宗敬" to "近代民族实业家。与荣德生共同发展面粉、纺织企业，荣氏企业在近代中国民族工业中规模突出；其经营实践体现了民国时期民族资本的积累、扩张与风险。",
    "吴火狮" to "台湾企业家、新光集团创办人之一。参与发展纺织、化纤、保险、百货等产业，新光集团的多元化经营是台湾战后企业集团发展的代表性案例。",
    "文天祥" to "南宋末政治家、文学家。宋亡之际组织勤王并长期抗元，被俘后拒绝降元而就义；其政治选择、诗文与人格象征，在中国历史记忆中具有长期影响。",
    "戚继光" to "明代军事家。整练戚家军、平定东南倭患，并在北方长城防务和练兵制度上进行系统建设；《纪效新书》《练兵实纪》对后世军事训练具有重要影响。",
    "张廷玉" to "清代重臣、史学组织者。康熙、雍正、乾隆三朝长期任职，参与军机处早期规制并主持《明史》续修；其仕宦经历是理解清代中枢行政制度的重要个案。",
    "曾国藩" to "晚清政治、军事人物，湘军的主要组织者。其在镇压太平天国、洋务军事工业和地方治理中的行动，深刻影响晚清政局；其家书、日记和用人实践也长期受到研究。",
    "张巡" to "唐代军事人物。安史之乱中坚守雍丘、睢阳，以少量守军长期牵制叛军；睢阳保卫战及其殉难经历，在唐代平叛史与忠烈叙事中具有重要地位。",
    "彭玉麟" to "晚清湘军水师将领、书画家。参与创建并统率湘军水师，重视船炮、军纪和江防海防；同时以画梅、诗文和清廉形象留有广泛文化影响。",
    "张璁" to "明代嘉靖朝重臣。因“大礼议”受到世宗倚重并入阁，参与《明伦大典》等典籍编修；其仕途是嘉靖初年君臣关系、礼制争论与内阁政治的重要线索。",
    "方从哲" to "明末内阁首辅。万历末入阁，在“梃击、红丸、移宫”三案中立场受到争议，后遭罢归；其经历集中呈现明末宫廷政治与内阁权力的困境。",
    "梅艳芳" to "香港歌手、演员与公益人士。她以高度舞台表现力和多元银幕角色改变了香港流行女歌手的公众形象，《胭脂扣》等作品也奠定其电影表演地位，是港式流行文化的标志性人物。",
    "周深" to "中国内地歌手。以清澈、辨识度高的声线和跨风格演唱受到关注，《大鱼》等作品扩大其公众影响；其舞台和影视音乐作品持续参与当代华语流行音乐的传播。",
    "邓丽君" to "华语流行歌手。以细腻演唱和多语种作品跨越地区传播，对20世纪后期华语流行音乐的唱法、制作与大众审美产生深远影响，《但愿人长久》等歌曲持续被传唱。",
    "宁泽涛" to "游泳运动员，主项为短距离自由泳。2015年获得世界游泳锦标赛男子100米自由泳冠军，成为首位在该项目夺得世锦赛冠军的中国、亚洲运动员。",
    "张常宁" to "排球运动员，中国女排主攻手。参与中国女排获得2015年世界杯、2016年里约奥运会及2019年世界杯冠军，是中国女排新时代的重要成员。",
    "杨威" to "体操运动员。以男子全能能力著称，获得北京奥运会男子全能冠军并多次在世锦赛、世界杯夺冠，是中国男子体操代表性运动员。",
    "陈一冰" to "体操运动员，擅长吊环。北京奥运会获男团和吊环金牌，伦敦奥运会获男团金牌和吊环银牌；其竞技表现与退役后的公众形象均有广泛影响。",
    "赵朴初" to "佛教界人士、社会活动家、书法家。长期参与佛教文化、慈善救济与中外文化交流，并担任全国政协副主席、中国佛教协会会长等职务，对现代佛教公共事务具有重要影响。",
    "陶弘景" to "南朝道学家、医学家、文人。隐居茅山后仍受朝廷咨询，后世称“山中宰相”；其《本草经集注》整理并扩充药物知识，对中国本草学发展影响深远。",
    "张道陵" to "东汉道教人物，后世尊为天师。相传于142年在鹤鸣山创立五斗米道（天师道），其宗教组织与仪式传统对早期道教制度化具有重要影响。",
    "张玉书" to "清代重臣、学者。历仕五十年，官至文华殿大学士兼户部尚书；主持或参与《平定朔漠方略》《佩文韵府》《康熙字典》等大型文献工程，是康熙朝政务与文化编纂的重要人物。",
    "曾国荃" to "晚清湘军将领，曾国藩之弟。参与围攻天京并在晚清地方军政体系中担任多项要职，官至两江总督；其军事与仕宦经历是研究湘军集团的重要个案。",
    "易烊千玺" to "演员、歌手、舞者。以少年组合成员身份进入公众视野，后以《长安十二时辰》《少年的你》等作品拓展表演路径；其从偶像团体到影视表演的转型，是当代青年流行文化中具有代表性的职业经历。",
    "王菲" to "歌手、词曲作者与演员。她以独具辨识度的演唱和跨地域音乐传播形成长期影响，同时在《重庆森林》等电影中留下鲜明银幕形象；其音乐与影视工作共同构成华语流行文化的重要案例。",
    "张学友" to "歌手、演员与作曲人。其长期横跨录音、现场演出、音乐剧与电影表演，《吻别》等作品和大规模巡演扩大了华语流行音乐的传播；电影表演亦获重要奖项肯定。",
    "胡歌" to "演员、歌手。其由《仙剑奇侠传》建立大众认知，后在《琅琊榜》《繁花》等剧集中持续拓展表演角色，并兼有音乐与舞台作品；其职业转型是当代中国电视剧演员发展路径的代表案例。",
    "孙俪" to "演员。她以《玉观音》进入大众视野，之后在《甄嬛传》《芈月传》《那年花开月正圆》等剧集中形成稳定的角色影响力；其作品覆盖都市、历史与现实题材，是当代电视剧表演的重要案例。",
    "李宇春" to "歌手、演员与音乐创作者。她以2005年《超级女声》夺冠进入主流流行音乐市场，后持续举办演唱会、参与专辑策划并拓展电影表演；其职业轨迹是中国选秀节目与当代流行文化发展中的重要案例。",
    "薛之谦" to "歌手、词曲创作者与演员。其以原创流行歌曲建立听众基础，并在舞台演出、综艺和影视客串中保持公众能见度；其职业经历呈现了唱作人与大众娱乐节目并行发展的当代路径。",
    "周冬雨" to "演员。她以《山楂树之恋》进入电影行业，之后在青春、现实与作者电影题材中持续表演，并凭《少年的你》获得多项华语电影表演奖；其从新人演员到电影主演的成长是当代华语电影的重要案例。",
    "陈坤" to "演员、歌手。其以《金粉世家》打开电视剧观众认知，后在《云水谣》《画皮》《龙门飞甲》等电影中拓展类型角色，并持续参与影视制作与公益项目；其跨电视与电影的表演经历具有代表性。",
    "黄晓明" to "演员、歌手。其以《大汉天子》等剧集建立知名度，后在古装、现实与商业电影中持续表演，并凭《中国合伙人》获得多项表演奖；其电视剧与电影并行的职业轨迹具有代表性。",
    "肖战" to "演员、歌手。2015年通过选秀节目进入演艺行业，后凭《陈情令》等影视作品扩大公众影响，并持续参与影视与音乐工作；其职业发展呈现了中国当代影视演员由综艺舞台进入剧集与电影的常见路径。",
    "田亮" to "跳水运动员。2000年悉尼奥运会夺得男子十米台冠军，2004年雅典奥运会再获双人跳台金牌；其奥运与世锦赛成绩使其成为中国跳水项目具有国际知名度的代表人物。",
    "范志毅" to "足球运动员、教练。职业生涯曾效力上海申花与英格兰水晶宫；2001年获亚洲足球先生，是首位获此荣誉的中国球员，其留洋与国家队经历在中国职业足球史上具有代表性。",
    "马琳" to "乒乓球运动员。获得北京奥运会男单冠军，并多次赢得世界杯和团体世界大赛冠军；其横拍直板打法与长期国际竞争经历，是中国乒乓球男子项目的重要个案。",
    "李连杰" to "武术运动员、演员、公益倡导者。少年时期多次获得全国武术冠军，后以《少林寺》《黄飞鸿》等影片确立动作演员地位，并进入好莱坞发展；其创立壹基金后的公益实践也成为职业生涯的重要组成。",
    "林则徐" to "晚清政治人物。任湖广总督时推行禁烟，1838年受命赴广东查办鸦片并主持虎门销烟；同时组织译介西方资料、重视海防与水利，其禁烟实践与近代“开眼看世界”的历史脉络密切相关。",
    "李鸿章" to "晚清重臣、洋务派代表人物。参与组织淮军，主持江南制造总局、北洋海防等近代军政与工业建设；也代表清政府参与《马关条约》《辛丑条约》等外交谈判，其功过须置于晚清内忧外患的历史处境中理解。",
    "海瑞" to "明代官员，以直言敢谏和清廉形象著称。任淳安知县、应天巡抚等职时整饬吏治、疏浚河道并尝试限田；其严厉行政及相关争议，也构成理解晚明政治与土地问题的重要材料。",
    "洪承畴" to "明末清初军政人物。松锦之战兵败后降清，入清后参与江南军政与《明史》纂修；其经历牵涉明清易代中的战争、政治选择与清初统治建构，历来存在复杂评价。",
    "岳锺琪" to "清代将领，长期参与西北边疆军务。雍正年间担任川陕总督并参与西北战事，也卷入曾静、张熙投书策反事件；其军政经历是研究清代西北治理与雍正朝政治的重要个案。",
    "老舍" to "现代作家、剧作家。小说《骆驼祥子》《四世同堂》及话剧《茶馆》等，以北京市民生活、普通劳动者和时代变迁为重要书写对象；其语言风格与戏剧创作对现代中国文学和舞台艺术影响深远。",
    "胡适" to "思想家、文学家、哲学家。倡导白话文与文学改良，参与新文化运动；同时从事中国哲学史、文学史与学术方法论研究，其主张和政治立场均是近现代思想史中长期讨论的对象。",
    "叶恭绰" to "近代交通实业家、学者、书画鉴藏家与文化事业推动者。早年参与铁路和交通教育事业，后持续投入文物收藏保护、古籍整理和公共文化机构建设；其跨交通、学术与文博事业的经历，是近现代文化史的重要案例。",
    "茅盾" to "现代作家、文学批评家。长期参与文学期刊和左翼文化活动，《子夜》《春蚕》等作品以社会结构、城乡变动与普通人的命运为核心，构成中国现代现实主义文学的重要部分。",
    "闻一多" to "诗人、学者、教育家。以《红烛》《死水》及新诗理论成为格律诗派代表，又在《诗经》《楚辞》、古代神话等研究中作出重要贡献；其晚年参与民主运动并遇害，成为现代中国公共知识分子的重要记忆。",
    "黄金荣" to "近代上海法租界巡捕出身的青帮头目、争议人物。其掌控娱乐场所和黑社会网络，造成黄赌毒等社会问题；相关经历是研究租界警务、黑社会与城市治理的负面历史材料，不作为成功人物叙事。",
    "杜月笙" to "近代上海青帮头目、争议人物。其势力与租界社会、帮会网络、工商活动及政治事件交织，对上海城市史具有研究价值；涉及暴力、犯罪和政治责任的部分应以史料审慎辨析，不作美化。",
    "佛印禅师" to "北宋僧人，法名了元，号觉老，又称宝觉禅师。以佛学、诗文和与苏轼等文人的交往闻名，曾长期主持江南寺院；其事迹兼具宋代禅宗传播与文人佛教交往的史料价值。",
    "吕洞宾" to "道教内丹传统中的重要人物，名岩、字洞宾、号纯阳。关于其出生地、经历和生卒年存在多种文献与传说，较稳妥的说法是其形象形成于晚唐五代至宋元道教传播过程；全真道尊为北方五祖之一。",
    "杨幂" to "演员、歌手。自2000年代中期起参与古装、都市与奇幻题材影视作品，以《神雕侠侣》《三生三世十里桃花》等角色扩展公众影响；其演员与影视制作工作构成当代商业电视剧生态中的代表性职业轨迹。",
    "杨紫" to "演员。童星时期进入公众视野，后在《欢乐颂》《香蜜沉沉烬如霜》等电视剧中完成从少年演员到成年演员的转型；其角色选择和持续的剧集曝光使其成为当代电视剧市场的重要演员之一。",
    "赵丽颖" to "演员。以《陆贞传奇》《花千骨》《楚乔传》等电视剧扩大影响，并持续在古装、现实与女性成长题材中工作；其职业路径是中国电视剧演员由配角积累至领衔主演的代表案例之一。",
    "迪丽热巴" to "演员。以《三生三世十里桃花》《漂亮的李慧珍》等电视剧受到关注，并参与综艺与影视项目；其在都市、古装及奇幻题材中的表演构成当代影视市场的持续案例。",
    "英格丽·褒曼" to "瑞典演员，长期在欧洲与好莱坞从事电影表演。凭《煤气灯下》《真假公主》《东方快车谋杀案》等作品多次获得奥斯卡表演奖，其跨国表演生涯是20世纪电影史的重要案例。",
    "费雯·丽" to "英国演员。以《乱世佳人》中的斯嘉丽等银幕角色享有国际声誉，并在电影和戏剧舞台长期工作；其表演生涯与好莱坞经典电影及20世纪英语戏剧史密切相关。",
    "阮玲玉" to "中国早期电影演员。1920—1930年代在明星、联华等公司演出，《故都春梦》《神女》等作品以对女性处境和都市底层的细腻表演著称；其短暂生涯及死亡长期成为中国电影史的重要议题。",
    "陈道明" to "演员。以《末代皇帝》《围城》《康熙王朝》等影视角色形成深厚的观众认知，并长期在电影与电视剧中工作；其表演强调人物心理和历史人物塑造，是中国影视表演的重要个案。",
    "王俊凯" to "歌手、演员，TFBOYS成员。少年时期随组合出道，在音乐作品、综艺舞台和影视项目中持续发展；其成长经历体现了互联网时代少年偶像组合的培养与公众传播路径。",
    "王源" to "歌手、演员，TFBOYS成员。随组合以网络传播方式出道，之后参与音乐、综艺和影视工作；其少年阶段兼顾学业与演艺训练的经历，是当代偶像工业与青少年成长议题的一部分。",
    "李云迪" to "钢琴演奏者。2000年获肖邦国际钢琴比赛金奖，成为首位获得该奖项的中国钢琴家；其国际比赛、演出与音乐教育传播经历，是当代中国钢琴艺术国际化的重要案例。",
    "孟小冬" to "京剧老生演员。早年在江南、京津演出，后师从余叔岩，形成以余派为基础的艺术风格；其录音、舞台经历和女性老生身份，使其在20世纪京剧史中具有独特位置。",
    "杨小楼" to "京剧武生演员。以武生表演、剧目创新和第一舞台的创办著称，与梅兰芳、余叔岩等共同构成民国时期京剧舞台繁荣的重要人物群；其艺术实践对京剧武生行当影响深远。",
    "罗纳尔多·纳扎里奥" to "巴西足球运动员，常被称为“外星人”罗纳尔多。代表巴西参加四届世界杯，1994年和2002年两度夺冠，并以15球长期保持巴西队世界杯进球纪录；其速度、突破和终结能力使其成为现代前锋的标志性人物。",
    "胡蝶" to "中国早期电影演员。1920—1930年代活跃于上海和香港电影界，与阮玲玉等同为早期电影明星；其跨地域电影工作与银幕形象，是研究中国默片至有声片转型的重要材料。",
    "朱珪" to "清代官员、学者。历任地方督抚，后入直南书房并担任上书房总师傅；其仕途跨乾隆、嘉庆两朝，兼具地方治理、经筵与帝王教育的历史观察价值。",
    "李东阳" to "明代政治家、诗人、书法家。成化年间登进士后入翰林，长期参与朝廷文教与政务；诗文上为“茶陵诗派”代表人物，其政绩与在刘瑾专权时期的处境均是明代中后期政治文化史的重要材料。",
    "诸葛亮" to "三国时期蜀汉政治家、军事家。辅佐刘备建立蜀汉政权，后以丞相身份主持政务、推行屯田与北伐；《出师表》等文本及其后世形象，使其长期成为中国传统政治伦理与谋略文化的重要人物。",
    "纪昀" to "清代学者、编纂家与官员。主持《四库全书》及《四库全书总目提要》编纂，兼具目录学、文献整理与笔记写作成就；《阅微草堂笔记》等著作和四库馆工作，对中国文献史与文学史影响深远。",
    "赵孟頫" to "元代书法家、画家、诗文家。书画兼擅，倡导“书画同源”并重视师法古人；其楷书、行书及山水、人物、花鸟画作品深刻影响元以后书画审美与创作传统。",
    "胡宗宪" to "明代抗倭军政人物。任浙江巡抚、总督期间统筹江浙沿海军务，延揽戚继光、俞大猷等将领，并以军事行动与招抚并用处理倭患；其生前两度入狱、身后昭雪，也反映嘉靖朝权力结构与海防治理的复杂性。",
    "杨继盛" to "明代谏官。历任南京吏部主事、兵部员外郎等职，以奏疏弹劾严嵩父子而遇害；其狄道任内兴学、治水等地方实践，与敢谏形象共同构成晚明政治史和廉政文化中的重要材料。",
    "蔡京" to "北宋官员、书法家。曾四度拜相，是徽宗朝权力结构中的关键人物；其书法有独立艺术价值，但其长期执政及以权谋私行为在《宋史》等传统史料中受到严厉批评，评价须区分艺术成就与政治责任。",
    "秦桧" to "南宋政治人物。绍兴年间多次居相位并主导对金和议，相关决策与岳飞案长期构成中国政治史和公共记忆中的争议议题；本条只记录可核史实，不把历史评价当作命理依据。",
    "苏轼" to "北宋文学家、书法家、画家。诗文、词、书画皆有重要成就，列“唐宋八大家”之一，并以豪放词、士夫画思想和宋代书法创作深刻影响后世；其多次仕宦与贬谪经历也映照北宋党争环境。",
    "范仲淹" to "北宋政治家、文学家。历仕真宗、仁宗两朝，官至参知政事；《岳阳楼记》等作品广为传诵，其政务实践、边防经历与“先忧后乐”的公共伦理表达共同构成后世对其影响力的主要理解。",
    "韩愈" to "唐代文学家、思想家。与柳宗元同为古文运动倡导者，主张恢复文章的思想与表达力量；《师说》《原道》等作品及其教育、思想主张，对宋明理学和中国散文传统产生深远影响。",
    "黄庭坚" to "北宋诗人、书法家。与苏轼、米芾、蔡襄并称宋代四大书家，是江西诗派的重要代表；其诗歌、草书与文人交游共同塑造了宋代“尚意”书风和诗学传统的重要部分。",
    "董其昌" to "明代书法家、画家、鉴藏家。提出书画“南北宗”等理论，形成松江画派，对晚明至清初书画创作与鉴赏传统影响深远；其艺术史地位与仕宦、收藏活动均应放在晚明社会环境中理解。",
    "黄宗羲" to "明清之际思想家、史学家。经历明清鼎革后长期讲学著述，《明夷待访录》《明儒学案》等从政治、经史与学术史角度提出系统思考；与顾炎武、王夫之并列为明清之际重要思想家。",
    "郭沫若" to "现代作家、历史学者、社会活动家。参与新文化运动和创造社，后长期从事甲骨文、金文与中国古代史研究，并在新中国文化、科学与公共事务机构中任职；其文学、学术与公共角色均具广泛影响。",
    "聂耳" to "现代作曲家、革命音乐先驱。《义勇军进行曲》等作品将民族危亡时期的社会情绪融入大众歌曲，后来成为中华人民共和国国歌；其短暂生涯对中国近现代音乐史和公共文化记忆影响深远。",
    "李现" to "演员。以电影表演进入行业，后通过《河神》《亲爱的，热爱的》等剧集获得更广泛认知；其职业经历呈现了青年演员在文艺片、网络剧与主流电视剧之间持续积累的路径。",
    "陈伟霆" to "演员、歌手与主持人。早年以歌手身份进入演艺行业，后在电影、古装剧和都市剧中持续发展；其横跨粤语流行音乐、内地影视和综艺主持的职业路径，体现了港澳艺人跨区域发展的代表性。",
    "杨超越" to "歌手、演员。由偶像团体节目出道，后在音乐、综艺与影视项目中持续尝试；其职业轨迹是观察移动互联网平台、偶像团体与青年艺人多媒介发展的一个案例。",
    "杨洋" to "演员。早年接受舞蹈专业训练，后从电视剧表演进入公众视野，并在青春、古装、军旅和都市题材中持续工作；其职业发展反映了当代电视剧演员由青春偶像题材向多类型角色拓展的路径。",
    "鹿晗" to "歌手、演员。早年在韩国以组合成员身份出道，回国后持续参与音乐、影视和综艺项目；其跨国偶像产业经历及个人音乐、影视尝试，是观察互联网时代青年偶像传播的一类案例。",
    "张艺兴" to "歌手、演员与音乐制作人。其从组合成员成长为独立音乐人与影视演员，持续尝试流行音乐制作、舞台与电影表演；其跨音乐、综艺和影视的工作构成当代青年艺人的代表性职业轨迹。",
    "张艺凡" to "演员、歌手与舞者。她由舞蹈训练延展至音乐、综艺、电影和电视剧表演，在青年演员中持续探索多媒介表达；其舞台基础与影视项目并行的职业发展具有当代性。",
    "王一博" to "演员、歌手、舞者与职业赛车手。其由组合舞台进入综艺、电视剧和电影表演，并持续参与舞蹈与赛车相关活动；多领域并行的职业发展是当代青年艺人跨媒介传播的代表案例。",
    "檀健次" to "演员、歌手与舞者。其早期经过舞蹈训练和男团舞台，后逐步转入影视表演，并以《猎罪图鉴》等角色扩大观众认知；其职业路径呈现唱跳训练与影视表演的转换。",
    "白敬亭" to "演员。其由青春题材剧集进入公众视野，后在都市、古装、悬疑和年代题材中持续表演，《开端》《南来北往》等作品拓展了角色类型；其发展体现青年演员由校园题材到多类型剧集的转变。",
    "虞书欣" to "演员、歌手。她由电视剧表演进入行业，后经女团节目扩展音乐舞台，并以《苍兰诀》等剧集扩大影响；其影视、偶像团体与个人音乐并行的职业形态具有当代性。",
    "周也" to "演员。她在电影和电视剧项目中持续积累，以《少年的你》《孤注一掷》《云边有个小卖部》等作品参与青春、现实与商业类型叙事；其职业进展反映青年演员在院线与剧集之间的协同发展。",
    "张译" to "演员。其由话剧团经历进入影视表演，以《士兵突击》《我的团长我的团》等剧集积累口碑，之后在电影与电视剧中持续拓展现实、犯罪和商业类型；其长期稳定的表演产出是当代中国影视演员的重要案例。",
    "吴磊" to "演员、配音演员。其以童星身份进入行业，后在《琅琊榜》《长歌行》《星汉灿烂》等剧集和电影中完成角色类型拓展；其从少年演员到成年主演的持续成长具有代表性。",
    "刘昊然" to "演员。其从青春电影起步，随后以“唐人街探案”系列形成稳定观众认知，并持续参与历史、悬疑、文艺与舞台表演；其学院训练与商业电影并行的职业路线具有代表性。",
    "张若昀" to "演员、配音演员与歌手。其在谍战、刑侦、古装与现实题材剧集中持续工作，《法医秦明》《庆余年》等角色扩大了公众认知；其职业经历显示了当代电视剧演员在多类型叙事中的长期积累。",
    "谭松韵" to "演员。她由舞蹈训练转入影视表演，以《甄嬛传》《最好的我们》《以家人之名》等作品积累多年龄层观众，并在青春、都市与现实题材中拓展角色；其职业发展是当代电视剧女演员持续转型的案例。",
    "彭德怀" to "军事家、政治人物。早年参与军队和革命活动，后在中国革命战争、抗美援朝及新中国国防工作中担任重要职务；其经历是研究20世纪中国军事与公共事务史的重要资料。",
    "陈云" to "政治人物、经济工作领导者。长期参与党和国家的组织、财政经济与政策工作，建国初期主持财经稳定与恢复工作；其经历是20世纪中国经济治理史的重要资料。",
    "朱德" to "军事家、政治人物。参与早期革命和军队创建，在革命战争与新中国公共事务中担任重要职务；其经历是近现代中国军事史的重要资料。",
    "刘少奇" to "革命家、政治人物和理论工作者。参与早期工人运动和党内组织工作，在近现代公共事务史中长期担任重要职务；其生平是研究工人运动、组织建设与国家治理的重要材料。",
    "宋庆龄" to "政治人物、社会活动家。早年从事公共事务与社会运动，后长期投入妇女儿童福利、文化教育、国际友好与和平事务；其工作是20世纪中国公共事务史的重要组成。",
    "周恩来" to "政治人物、外交家。早年参与学生运动和旅欧组织工作，后在革命战争、新中国政府与外交事务中长期担任重要职务；其经历是研究20世纪中国公共事务与外交史的重要材料。",
    "孙中山" to "近代政治人物、革命活动家。接受医学教育后转入公共与革命事务，组织革命团体并推动近代中国政治变革；其生平是研究晚清、辛亥革命与民国初年政治史的重要材料。",
    "陈毅" to "军事家、政治人物与外交工作者。早年留法求学，后参与革命战争、军队组织与新中国地方及外交工作；其经历是研究近现代中国军事、城市治理与外交事务的重要材料。",
    "叶剑英" to "军事家、政治人物。早年接受军事教育并参与近代革命活动，后长期从事军队、城市接管和国家公共事务工作；其生平是理解20世纪中国军政史的重要材料。",
    "胡耀邦" to "政治人物、组织工作者。早年参与革命活动，后长期从事组织、青年、科技与公共事务工作；其经历是研究20世纪中国组织建设和公共治理的重要材料。",
    "李大钊" to "近代政治人物、思想与教育工作者。早年接受法政教育并旅日求学，后从事新文化、思想传播和组织工作；其生平是研究20世纪初中国思想、教育与公共运动史的重要材料。",
    "瞿秋白" to "政治人物、理论与新闻工作者、文学家。曾从事俄文学习、新闻写作与思想传播，后参与组织和宣传工作；其著述与公共活动是研究中国早期马克思主义传播和现代文学史的重要材料。",
    "邓颖超" to "政治人物、社会活动家和妇女运动工作者。早年参与五四爱国运动与天津进步团体，后长期从事妇女、统战与公共事务工作；其经历是研究20世纪中国妇女运动和社会事务的重要材料。",
    "蔡和森" to "政治人物、理论与新闻工作者。早年参与新民学会和留法勤工俭学组织，后从事理论、组织和新闻宣传工作；其著述与公共活动是研究中国早期革命思想和新闻史的重要材料。",
    "任弼时" to "政治人物、组织工作者。早年参与青年运动并赴苏俄学习，后长期从事组织、军队政治与公共事务工作；其经历是研究20世纪中国组织建设与军队政治工作的重要材料。",
    "董必武" to "政治人物、法律与教育工作者。早年参加辛亥革命并赴日学习法律，后从事组织、教育、政法与国家公共事务工作；其生平是研究近现代中国法政与公共治理史的重要材料。",
    "徐向前" to "军事家、政治人物。接受黄埔军校教育后参与革命战争，后在军队建设和国防公共事务中担任重要职务；其经历是研究20世纪中国军事与国防史的重要材料。",
    "张闻天" to "政治人物、理论与教育工作者。早年从事教育和新文化传播，后参与组织、理论与外交事务工作；其著述与经历是研究近现代中国思想、组织与外交史的重要材料。",
    "聂荣臻" to "军事家、政治人物与国防科技工作者。早年赴法勤工俭学，后参与军队组织、城市接管与国防科技相关工作；其生平是研究20世纪中国军事、科技与公共事务史的重要材料。",
    "罗荣桓" to "军事家、政治人物与政法工作者。早年参与学生、农民运动和革命战争，后从事军队政治与检察公共事务工作；其经历是研究近现代中国军事、政法与公共治理史的重要材料。",
    "彭真" to "政治人物、法治与地方治理工作者。早年从事工人和组织工作，后长期参与地方治理、法治建设与国家公共事务；其经历是研究近现代中国城市治理和法治史的重要材料。",
    "李先念" to "政治人物、经济工作者。早年参与农民运动和革命战争，后长期从事地方、财经与国家公共事务工作；其经历是研究20世纪中国经济治理和公共事务史的重要材料。",
    "杨尚昆" to "政治人物、组织工作者。早年参与革命活动并赴苏联学习，后从事组织、办公与国家公共事务工作；其生平是研究20世纪中国组织体系与国家治理的重要材料。",
    "薄一波" to "政治人物、经济工作者。早年参与学生和组织工作，后从事抗战时期组织、地方经济建设和党史研究工作；其经历是研究20世纪中国组织与经济工作的重要材料。",
    "王稼祥" to "政治人物、理论与外交工作者。早年留苏学习，后参与宣传、军事政治、理论与外交事务工作；其著述和生平是研究近现代中国组织、理论与外交史的重要材料。",
    "林伯渠" to "政治人物、教育与组织工作者。早年留日并从事教育和革命活动，后长期参与组织、边区与国家公共事务；其经历是研究近现代中国组织建设和国家筹备史的重要材料。",
    "吴玉章" to "政治人物、教育与文化工作者。早年留日并参与近代革命活动，后长期从事教育、文化和公共事务工作；其生平是研究近现代中国教育与公共文化史的重要材料。",
    "徐特立" to "革命教育家、政治人物。早年长期从事教育实践，后参与革命和国家公共事务工作；其教育思想与社会实践是研究近现代中国教育史的重要材料。",
    "李维汉" to "政治人物、统战与民族事务工作者。早年参与新民学会和留法勤工俭学，后长期从事组织、统战、民族与宗教公共事务工作；其经历是研究20世纪中国统战和民族事务史的重要材料。",
    "黄兴" to "近代政治人物、革命活动家。早年留日，参与华兴会和同盟会组织工作，辛亥革命时期参与军事与政治事务；其经历是研究晚清革命和民国初年政治史的重要材料。",
    "宋教仁" to "近代政治人物、法政工作者。早年组织华兴会并学习法政，后参与民国初年制度建设与政党活动；其生平是研究辛亥革命后政党政治和法制建设的重要材料。",
    "张謇" to "近代实业家、教育家、社会活动家。以实业、教育、博物与公益事业建设著称，并参与近代公共事务；其工作是研究近代民族工业、地方治理与教育事业的重要材料。",
    "梁启超" to "近代思想家、政治活动家、教育家与史学家。长期从事报刊、教育、学术和公共论述工作，著述广泛；其生平是研究晚清民初思想转型、公共传播与学术史的重要材料。",
    "张澜" to "近代政治人物、教育工作者与社会活动家。早年从事教育和公共事务，后参与保路运动、地方治理与民主党派工作；其生平是研究近现代中国教育、地方政治与社会组织史的重要材料。",
    "沈钧儒" to "政治活动家、法学教育家与律师。早年学习法政，后长期从事法律、教育、救国会与民主党派公共事务；其生平是研究近现代中国法政教育和社会组织史的重要材料。",
    "于右任" to "近现代政治人物、教育家、新闻工作者与书法家。早年参与同盟会和报刊活动，后参与教育建设与公共事务；其新闻、教育和书法实践共同构成近代文化史的重要材料。",
    "马叙伦" to "教育家、语言文字学家、政治活动家与书法家。早年从事教育和学术工作，后参与民主党派组织与文化教育公共事务；其著述和职业经历是研究近现代教育、文字学和社会组织史的重要材料。",
    "李济深" to "政治人物、军事工作者与民主党派领导人。早年从事军事教育和军政工作，后参与民革组织、新政协与统一战线公共事务；其经历是研究近现代中国军政转型和民主党派史的重要材料。",
    "何香凝" to "政治活动家、妇女运动工作者与美术家。早年留日并参与同盟会活动，后从事妇女、侨务、民主党派和艺术创作；其生平是研究近现代妇女运动、华侨事务与美术史的重要材料。",
    "张治中" to "军事工作者、政治人物。早年接受军事教育并参与近代军政事务，抗战时期参与地方治理和国防工作，后参与和平谈判与新中国公共事务；其经历是研究20世纪中国军事、地方治理与政治协商史的重要材料。",
    "邓演达" to "政治活动家、军事工作者与民主党派早期组织者。早年接受军事教育，后参与北伐、武汉公共事务与农民问题相关工作，并参与组织农工党前身；其生平是研究近现代军政、社会组织与农民问题史的重要材料。",
    "段永平" to "企业经营者、投资人与慈善活动参与者。曾参与小霸王、步步高等消费电子企业经营，后以长期持有、企业经营和价值判断为主线参与公开投资讨论；其企业与投资实践是观察中国消费电子产业和长期资本配置的重要案例。",
    "但斌" to "投资人、资产管理从业者与作者。早年从事证券研究和投资，后创建东方港湾，并以长期持有、企业研究和价值投资为主要公开表达方向；其经历是观察中国私募资产管理与长期投资讨论的重要案例。",
    "查理·芒格" to "投资人、企业经营者与律师。早年执业法律并从事投资管理，后长期担任伯克希尔·哈撒韦副董事长；其关于企业质量、理性决策和多学科思维的公开讨论，对长期投资者影响广泛。",
    "约翰·邓普顿" to "投资人、基金管理者与慈善家。以全球化、逆向思考和长期配置闻名，创建邓普顿成长基金并长期推动慈善与学术资助；其经历是研究20世纪共同基金、全球投资与慈善资本的重要材料。",
    "塞思·卡拉曼" to "投资人、资产管理从业者与作者。创立并管理Baupost Group，以安全边际、价格与价值区分和风险控制为其公开投资框架的重要部分；其著作与长期管理实践是价值投资研究的重要案例。",
    "本杰明·格雷厄姆" to "投资人、经济学者与教育者。其证券分析、内在价值与安全边际等理念，构成现代价值投资的重要理论基础；与大卫·多德合著的《证券分析》及《聪明的投资者》长期影响投资教育。",
    "菲利普·费雪" to "投资人、长期投资研究者与作者。强调对企业经营、管理层和长期增长质量的实地研究，创立Fisher & Co.并著有《Common Stocks and Uncommon Profits》；其研究方法是理解成长股研究与长期投资结合的重要材料。",
    "乔尔·格林布拉特" to "投资人、资产管理从业者、教育者与作者。创建Gotham Capital，长期教授价值投资课程，并通过特殊情形研究和“神奇公式”等公开著作参与个人投资者教育；其经历是研究价值投资、系统筛选与金融教育传播的重要案例。",
    "彼得·林奇" to "投资人、共同基金管理者、作者与慈善活动参与者。长期管理富达麦哲伦基金，主张从企业和日常生活中理解投资标的，并以著作参与个人投资教育；其职业实践是研究主动管理、企业研究与长期持有的重要案例。",
    "迈克尔·伯里" to "投资人、资产管理从业者与医师。创建Scion Capital，因在美国次级抵押贷款危机前对住房金融风险的研究和交易而广为人知；其经历是研究独立研究、特殊情形与风险识别的重要案例。",
    "莫尼什·帕布莱" to "投资人、企业经营者与慈善活动参与者。先从科技咨询创业，后创建Pabrai Investment Funds并长期公开讨论价值投资、赔率与风险控制；其经历是研究企业家转向资产管理及投资教育传播的重要案例。",
    "黄景仁" to "清代诗人。以七言诗见长，作品多书写贫困、漂泊、仕途困顿与社会感受；其不摹唐拟宋而自成一格的诗歌风貌，使其成为乾嘉诗坛具有辨识度的人物。",
    "邵雍" to "北宋思想家、诗人。以象数易学、宇宙论思考和《皇极经世》《击壤集》等著作著称；其思想是理解北宋理学形成及《易》学演变的重要材料，民间神异传说不作为本条史实依据。",
    "谢枋得" to "南宋末政治人物、文学家。与文天祥同科，元兵南下时组织抗元，宋亡后流亡讲学并拒绝元廷征召，最终绝食而死；其生平成为宋元易代中士人节义叙事的重要个案。",
    "袁宗道" to "晚明文学家、官员，与袁宏道、袁中道并称“公安三袁”。作为公安派代表人物，他反对复古模拟，强调真性情与个性化表达；其文论与创作是晚明文学变革的重要线索。",
    "李成梁" to "明代辽东将领，长期镇守辽东并多次参与边疆战事。其军事声望、对女真与蒙古部落的处置、后期守势和迁民政策均具有复杂后果；评价应同时考察战功、军费与边地治理中的争议。",
    "洪亮吉" to "清代史学家、地理学家、诗人。著有多部疆域、方志与史学著作，并在《治平》《生计》等文章中讨论人口增长、资源与民生问题；其直言时政后遭流放的经历，也是理解嘉庆初年言路的重要材料。",
    "毕沅" to "清代官员、学者。乾隆二十五年状元，历任陕西、河南巡抚及湖广总督；政务之余主持和参与史学、金石、地理文献整理，著有《续资治通鉴》等，体现清代官员兼治学术的传统。",
    "史浩" to "南宋政治人物。孝宗朝拜相并参与朝廷军政讨论，史氏家族在南宋中后期具有显著政治影响；本条仅按可核史料呈现其仕宦节点，不把家族声望等同于个人功绩。",
    "胡林翼" to "晚清官员、湘军重要人物。早年在贵州任地方官，后任湖北巡抚并参与镇压太平天国运动；其治军、筹饷、地方治理及廉政形象对晚清政治史具有影响，但应置于清廷内战与地方军政化的历史背景中理解。",
    "熊廷弼" to "明末军政人物，曾任辽东经略。其应对后金与辽东防务的路线、广宁失守后的责任认定和遭魏忠贤集团处死，集中反映晚明边防制度与党争的相互纠缠；后获平反。",
    "杨镐" to "明代军政人物。曾经略援朝，后受命经略辽东；萨尔浒战败后被逮治，其经历是研究明末辽东军事决策、跨区域战争与责任归属的重要个案。",
    "张三丰" to "道教人物，相关记载主要见于《明史·方伎传》、地方志及后世道教文献。其生卒、籍贯与活动年代存在多种说法，明代朝廷寻访、赐号等可见于史料；长寿、显圣与武学开创等神异叙事不作为本条可核事实。",
    "五台山名僧" to "原始资料包使用的概括性称谓，未对应可唯一识别的实名人物。为避免把寺院传统、传说或多位僧人的事迹错误归于同一命主，本条暂仅保留原始排盘与资料等级，待补充姓名、时代或出处后再建立人物年表。",
    "雷军" to "科技创业者、小米集团创始人、董事长兼CEO。其职业路径横跨软件、互联网零售、消费电子和智能制造，小米以软硬件结合及生态产品协同拓展智能终端场景；其早期在金山和卓越网的经历亦连接了中国互联网的发展阶段。",
    "屠呦呦" to "药学家。长期参与抗疟药物研究，在从传统医药文献中筛选、提取和发展青蒿素类药物的过程中作出关键贡献；其成果推动抗疟联合疗法的全球应用，并获得诺贝尔生理学或医学奖。",
    "袁隆平" to "农业科学家、杂交水稻育种专家。自 1960 年代起长期研究水稻杂种优势利用，推动三系法、两系法和超级杂交稻等路线发展；其研究与推广对中国及全球粮食生产具有重要影响。",
    "马云" to "互联网与电子商务企业家。参与创办阿里巴巴并推动面向中小企业的网络贸易平台发展，后续参与企业治理、公益与教育等事务；其创业路径是观察中国互联网商业化和全球电商发展的重要案例。",
    "莫言" to "当代小说家，本名管谟业。其小说以高密东北乡等乡土经验为重要叙事资源，作品被广泛译介；2012 年获诺贝尔文学奖。出生日期存在公开异说，本人物概览不据此作命理推断。",
    "张艺谋" to "电影导演、摄影师与演员。其早期参与摄影的作品和后续导演作品在中国电影史与国际传播中具有重要位置；同时参与大型文化活动的视觉呈现。生日可核，时柱未证实。",
    "周杰伦" to "歌手、词曲作者、制作人与导演。其创作将 R&B、嘻哈、摇滚及中国风等元素带入华语主流流行音乐，并扩展至影视与现场表演，对华语流行音乐制作和青年文化传播具有广泛影响。生日可核，时柱未证实。",
)

/**
 * 问真网页包的姓名并不等于已核人物身份。只有姓名和阳历生日同时命中独立可考资料包时，
 * 才允许展示外部生平资料；时刻仍沿用原始记录，不由此推定。
 */
private fun BaziCase.hasIndependentlyVerifiedPublicIdentity(): Boolean {
    if (sourceType == CaseSourceType.CURATED_CELEBRITY_CATALOG) return true
    if (sourceType != CaseSourceType.WENZHEN_WEB_IMPORT) return false
    val expected = independentlyVerifiedCelebrityBirthDates[alias.celebrityResearchCanonicalName()]
        ?: return false
    // Details can display a corrected public date while the original 问真 input stays in
    // sourceNote. Identity verification must use that immutable source value so a display
    // correction never turns a same-name, wrong-date import into a verified biography.
    val actual = originalWenzhenSolarDateTime()
        ?: (birthInput.calendarInput as? BirthCalendarInput.Solar)?.dateTime
        ?: return false
    return actual.year == expected.year &&
        actual.month == expected.month &&
        actual.day == expected.day
}

private fun BaziCase.originalWenzhenSolarDateTime(): CivilDateTime? {
    val values = birthInput.sourceNote
        ?.let { note ->
            Regex("来源阳历：(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2})(?::(\\d{2}))?")
                .find(note)
                ?.groupValues
        }
        ?: return null
    return CivilDateTime(
        year = values[1].toInt(),
        month = values[2].toInt(),
        day = values[3].toInt(),
        hour = values[4].toInt(),
        minute = values[5].toInt(),
        second = values[6].toIntOrNull() ?: 0,
    )
}

private data class IndependentlyVerifiedBirthDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val sourceUrl: String? = null,
)

private data class IndependentlyVerifiedDateConflict(
    val publicDate: String,
    val sourceUrl: String,
    val authoritativeDate: IndependentlyVerifiedBirthDate? = null,
)

private val independentlyVerifiedCelebrityBirthDates = mapOf(
    "阿尔伯特·爱因斯坦" to IndependentlyVerifiedBirthDate(1879, 3, 14),
    "弗里德里希·尼采" to IndependentlyVerifiedBirthDate(
        1844,
        10,
        15,
        "https://www.nietzsche.com/nietzsche.pdf",
    ),
    "阿道夫·希特勒" to IndependentlyVerifiedBirthDate(
        1889,
        4,
        20,
        "https://encyclopedia.ushmm.org/content/en/article/adolf-hitler-early-years-1889-1921",
    ),
    "迈克尔·乔丹" to IndependentlyVerifiedBirthDate(1963, 2, 17),
    "罗宾德拉纳特·泰戈尔" to IndependentlyVerifiedBirthDate(
        1861,
        5,
        7,
        "https://www.nobelprize.org/laureate/583",
    ),
    "托马斯·爱迪生" to IndependentlyVerifiedBirthDate(1847, 2, 11),
    "弗里德里希·恩格斯" to IndependentlyVerifiedBirthDate(
        1820,
        11,
        28,
        "https://www.dhm.de/lemo/biografie/friedrich-engels",
    ),
    "董浩云" to IndependentlyVerifiedBirthDate(
        1912,
        9,
        28,
        "https://modernchina.org/article/8268.html",
    ),
    "杜月笙" to IndependentlyVerifiedBirthDate(1888, 8, 22),
    "赵朴初" to IndependentlyVerifiedBirthDate(
        1907,
        11,
        5,
        "https://www.mj.org.cn/hszl/hsgc/mjhistory/202007/t20200724_230006.htm",
    ),
    "罗纳尔多·纳扎里奥" to IndependentlyVerifiedBirthDate(1976, 9, 18),
    "玛丽莲·梦露" to IndependentlyVerifiedBirthDate(1926, 6, 1),
    "李小龙" to IndependentlyVerifiedBirthDate(
        1940,
        11,
        27,
        "https://news.cctv.com/society/20081122/100998.shtml",
    ),
    "李连杰" to IndependentlyVerifiedBirthDate(
        1963,
        4,
        26,
        "https://www.cctv.com/performance/20051020/101274.shtml",
    ),
    "姚明" to IndependentlyVerifiedBirthDate(
        1980,
        9,
        12,
        "https://tv.cctv.com/2013/07/03/VIDA1372845492863341.shtml",
    ),
    "杨幂" to IndependentlyVerifiedBirthDate(1986, 9, 12),
    "赵丽颖" to IndependentlyVerifiedBirthDate(1987, 10, 16),
    "迪丽热巴" to IndependentlyVerifiedBirthDate(1992, 6, 3),
    "周深" to IndependentlyVerifiedBirthDate(1992, 9, 29),
    "孙杨" to IndependentlyVerifiedBirthDate(1991, 12, 1),
    "易烊千玺" to IndependentlyVerifiedBirthDate(2000, 11, 28),
    "肖战" to IndependentlyVerifiedBirthDate(1991, 10, 5),
    "王一博" to IndependentlyVerifiedBirthDate(
        1997,
        8,
        5,
        "https://www.iq.com/actor-info/%E7%8E%8B%E4%B8%80%E5%8D%9A-yibo-233528105?lang=zh_cn",
    ),
    "王俊凯" to IndependentlyVerifiedBirthDate(
        1999,
        9,
        21,
        "https://www.iq.com/actor-info/%E7%8E%8B%E4%BF%8A%E5%87%AF-karry-215980805?lang=zh_cn",
    ),
    "王源" to IndependentlyVerifiedBirthDate(
        2000,
        11,
        8,
        "https://www.iq.com/actor-info/roy-215936505?lang=en_us",
    ),
    "鹿晗" to IndependentlyVerifiedBirthDate(
        1990,
        4,
        20,
        "https://www.iq.com/actor-info/%E9%B9%BF%E6%99%97-lu-han-214891605?lang=zh_cn",
    ),
    "杨紫" to IndependentlyVerifiedBirthDate(1992, 11, 6),
    "张艺谋" to IndependentlyVerifiedBirthDate(1950, 4, 2),
    "戚继光" to IndependentlyVerifiedBirthDate(
        1528,
        11,
        12,
        "https://www.jxdy.gov.cn/c105619/202105/c6f70e2ad31d4b368abc0176f86274a8.shtml",
    ),
    "周杰伦" to IndependentlyVerifiedBirthDate(1979, 1, 18),
    "刘德华" to IndependentlyVerifiedBirthDate(1961, 9, 27),
    "吴京" to IndependentlyVerifiedBirthDate(1974, 4, 3),
    "林丹" to IndependentlyVerifiedBirthDate(
        1983,
        10,
        14,
        "https://library.olympics.com/Default/digitalCollection/DigitalCollectionAttachmentDownloadHandler.ashx?documentId=165327&parentDocumentId=165312",
    ),
    "梅艳芳" to IndependentlyVerifiedBirthDate(
        1963,
        10,
        10,
        "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_TC.pdf",
    ),
    "李娜" to IndependentlyVerifiedBirthDate(
        1982,
        2,
        26,
        "https://wtafiles.wtatennis.com/pdf/publications/2023MediaGuide/2023MG_WTALegends_31Jan2023.pdf",
    ),
    "李现" to IndependentlyVerifiedBirthDate(
        1991,
        10,
        19,
        "https://www.iq.com/actor-info/%E6%9D%8E%E7%8E%B0-li-xian-215196205?lang=zh_cn",
    ),
    "陈伟霆" to IndependentlyVerifiedBirthDate(
        1985,
        11,
        21,
        "https://www.iq.com/actor-info/%E9%99%88%E4%BC%9F%E9%9C%86-william-chan-200033205?lang=zh_cn",
    ),
    "杨超越" to IndependentlyVerifiedBirthDate(
        1998,
        7,
        31,
        "https://www.iq.com/actor-info/%E6%9D%A8%E8%B6%85%E8%B6%8A-yang-chaoyue-238591805?lang=zh_cn",
    ),
    "杨洋" to IndependentlyVerifiedBirthDate(
        1991,
        9,
        9,
        "https://www.iq.com/actor-info/%E6%9D%A8%E6%B4%8B-yang-yang-215007205?lang=zh_cn",
    ),
    "刘国梁" to IndependentlyVerifiedBirthDate(1976, 1, 10),
    "周星驰" to IndependentlyVerifiedBirthDate(1962, 6, 22),
    "张国荣" to IndependentlyVerifiedBirthDate(
        1956,
        9,
        12,
        "https://2008.cctv.com/special/1013/3/86670.html",
    ),
    "陈道明" to IndependentlyVerifiedBirthDate(
        1955,
        4,
        26,
        "https://news.cctv.com/program/czwy/20030530/100547.shtml",
    ),
    "徐志摩" to IndependentlyVerifiedBirthDate(
        1897,
        1,
        15,
        "https://www.chinawriter.com.cn/xdzj/724.shtml",
    ),
    "闻一多" to IndependentlyVerifiedBirthDate(
        1899,
        11,
        24,
        "https://www.tsinghua.edu.cn/info/2032/79051.htm",
    ),
    "朱自清" to IndependentlyVerifiedBirthDate(
        1898,
        11,
        22,
        "https://www.xinhuanet.com/politics/2018-11/20/c_129998309.htm",
    ),
    "梅兰芳" to IndependentlyVerifiedBirthDate(
        1894,
        10,
        22,
        "https://www.dpm.org.cn/lemmas/239927.html",
    ),
    "邓丽君" to IndependentlyVerifiedBirthDate(
        1953,
        1,
        29,
        "https://www.teresa-teng.org/people.php",
    ),
    "崔健" to IndependentlyVerifiedBirthDate(
        1961,
        8,
        2,
        "https://music.sonyselect.net/page/artist.html?artistId=72",
    ),
    "李云迪" to IndependentlyVerifiedBirthDate(
        1982,
        10,
        7,
        "https://www.universal-music.co.jp/yundi-li/biography/",
    ),
    "苏轼" to IndependentlyVerifiedBirthDate(
        1037,
        1,
        8,
        "https://www.hzarchives.org.cn/info/4729",
    ),
    "张常宁" to IndependentlyVerifiedBirthDate(1995, 11, 6),
    "胡适" to IndependentlyVerifiedBirthDate(
        1891,
        12,
        17,
        "https://m.ahjjjc.gov.cn/p/48847.html",
    ),
    "老舍" to IndependentlyVerifiedBirthDate(
        1899,
        2,
        3,
        "https://wwj.beijing.gov.cn/bjww/362679/362680/482911/612163/index.html",
    ),
    "叶恭绰" to IndependentlyVerifiedBirthDate(
        1881,
        11,
        24,
        "https://www.counsellor.gov.cn/2021-11/13/c_1211504856.htm",
    ),
    "左宗棠" to IndependentlyVerifiedBirthDate(
        1812,
        11,
        10,
        "https://www.fuzhou.gov.cn/zgfzzt/zjrc/mdfc/mdrj/202202/t20220216_4310139.htm",
    ),
    "张之洞" to IndependentlyVerifiedBirthDate(
        1837,
        9,
        2,
        "https://www.gzszx.gov.cn/wstd/sldt/41650.shtml",
    ),
    "李鸿章" to IndependentlyVerifiedBirthDate(
        1823,
        2,
        15,
        "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11737386.html",
    ),
    "茅盾" to IndependentlyVerifiedBirthDate(
        1896,
        7,
        4,
        "https://zjjcmspublic.oss-cn-hangzhou-zwynet-d01-a.internet.cloud.zj.gov.cn/jcms_files/jcms1/web3096/site/attach/zfgb/198809.pdf",
    ),
    "郭沫若" to IndependentlyVerifiedBirthDate(
        1892,
        11,
        16,
        "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11734517.html",
    ),
    "余华" to IndependentlyVerifiedBirthDate(
        1960,
        4,
        3,
        "https://dxs.moe.gov.cn/zx/a/ds_dxsd/230703/1843739.shtml",
    ),
    "沈从文" to IndependentlyVerifiedBirthDate(
        1902,
        12,
        28,
        "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977903.html",
    ),
    "琼瑶" to IndependentlyVerifiedBirthDate(
        1938,
        4,
        20,
        "https://www.hyfl.gov.cn/view/68.html",
    ),
    "林则徐" to IndependentlyVerifiedBirthDate(
        1785,
        8,
        30,
        "https://www.minhou.gov.cn/xjwz/mlmh/rwls/gjmr/201706/t20170619_949982.htm",
    ),
    "欧阳修" to IndependentlyVerifiedBirthDate(
        1007,
        8,
        1,
        "https://www.hnhx.gov.cn/portal/zjhx/hxyx/webinfo/2021/01/1612766650239657.htm",
    ),
    "曾国藩" to IndependentlyVerifiedBirthDate(
        1811,
        11,
        26,
        "https://whhlyt.hunan.gov.cn/whhlyt/english/Culture/CulturalFigures/202205/t20220527_24654725.html",
    ),
    "乾隆" to IndependentlyVerifiedBirthDate(
        1711,
        9,
        25,
        "https://minghuaji.dpm.org.cn/author/detail?id=277",
    ),
    "溥仪" to IndependentlyVerifiedBirthDate(
        1906,
        2,
        7,
        "https://www.gdwsw.gov.cn/wsgdgdyb/content/post_25899.html",
    ),
    "光绪" to IndependentlyVerifiedBirthDate(
        1871,
        8,
        14,
        "https://qspfw.moe.gov.cn/html/story/20230303/20304.html",
    ),
    "慈禧太后" to IndependentlyVerifiedBirthDate(
        1835,
        11,
        29,
        "https://news.cctv.com/china/20090130/102327_10.shtml",
    ),
    "咸丰" to IndependentlyVerifiedBirthDate(
        1831,
        7,
        17,
        "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
    ),
    "同治" to IndependentlyVerifiedBirthDate(
        1856,
        4,
        27,
        "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
    ),
    "宁泽涛" to IndependentlyVerifiedBirthDate(
        1993,
        3,
        6,
        "https://www.olympedia.org/athletes/133209",
    ),
    "杨威" to IndependentlyVerifiedBirthDate(
        1980,
        2,
        8,
        "https://www.zgbk.com/ecph/words?ID=115336&SiteID=1&SubID=61516&Type=bkzyb",
    ),
    "陈一冰" to IndependentlyVerifiedBirthDate(
        1984,
        12,
        19,
        "https://gymnasticsresults.com/archive/worlds/2010/mag/part.pdf",
    ),
    "范志毅" to IndependentlyVerifiedBirthDate(
        1969,
        11,
        6,
        "https://sports.cctv.com/old/20081116/102601.shtml",
    ),
    "马琳" to IndependentlyVerifiedBirthDate(
        1980,
        2,
        19,
        "https://www.china.org.cn/olympic/2008-07/14/content_16005165.htm",
    ),
)

/** 已确认原问真排盘日期与权威公开生日不一致的条目；保留来源冲突与原始快照。 */
private val independentlyVerifiedCelebrityDateConflicts = mapOf(
    "约翰·D·洛克菲勒" to IndependentlyVerifiedDateConflict(
        publicDate = "1839-07-08",
        sourceUrl = "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1839,
            7,
            8,
            "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/",
        ),
    ),
    "托马斯·爱迪生" to IndependentlyVerifiedDateConflict(
        publicDate = "1847-02-11",
        sourceUrl = "https://www.nps.gov/people/thomas-edison-biography-1847-1882-birth-to-pearl-street.htm",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1847,
            2,
            11,
            "https://www.nps.gov/people/thomas-edison-biography-1847-1882-birth-to-pearl-street.htm",
        ),
    ),
    "乾隆" to IndependentlyVerifiedDateConflict(
        publicDate = "1711-09-25",
        sourceUrl = "https://minghuaji.dpm.org.cn/author/detail?id=277",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1711,
            9,
            25,
            "https://minghuaji.dpm.org.cn/author/detail?id=277",
        ),
    ),
    "康熙" to IndependentlyVerifiedDateConflict(
        publicDate = "1654-05-04",
        sourceUrl = "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1654,
            5,
            4,
            "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        ),
    ),
    "雍正" to IndependentlyVerifiedDateConflict(
        publicDate = "1678-12-13",
        sourceUrl = "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1678,
            12,
            13,
            "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        ),
    ),
    "嘉庆" to IndependentlyVerifiedDateConflict(
        publicDate = "1760-11-13",
        sourceUrl = "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1760,
            11,
            13,
            "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        ),
    ),
    "道光" to IndependentlyVerifiedDateConflict(
        publicDate = "1782-09-16",
        sourceUrl = "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1782,
            9,
            16,
            "https://theme.npm.edu.tw/Academic/ChineseArtDownload.ashx?bid=10358&eid=0",
        ),
    ),
    "张爱玲" to IndependentlyVerifiedDateConflict(
        publicDate = "1920年9月19日与9月30日（公开资料异说）",
        sourceUrl = "https://www.chinawriter.com.cn/n1/2025/0513/c404064-40478847.html",
    ),
    "包玉刚" to IndependentlyVerifiedDateConflict(
        publicDate = "1918年11月10日与11月16日（公开资料异说）",
        sourceUrl = "https://www.ykpaoschool.cn/cn/News-and-Events/New-Stories/186",
    ),
    "阮玲玉" to IndependentlyVerifiedDateConflict(
        publicDate = "1910年4月26日与6月3日（公开资料存在农历／公历混用风险）",
        sourceUrl = "https://paper.people.com.cn/hqrw/html/2012-02/26/content_1027643.htm",
    ),
    "陶弘景" to IndependentlyVerifiedDateConflict(
        publicDate = "456年（公开资料仅见年份）",
        sourceUrl = "https://m.dayi.org.cn/ancient_physicians/500019.html",
    ),
    "佛印禅师" to IndependentlyVerifiedDateConflict(
        publicDate = "1031年与1032年（公开资料异说）",
        sourceUrl = "https://www.jdz.gov.cn/zsnj/jdzsz/jdzszxc1985/dwj/rwz/P020250717401203628309.pdf",
    ),
    "朱元璋" to IndependentlyVerifiedDateConflict(
        publicDate = "1328年（故宫资料载农历九月十八日，未据此伪造公历日期）",
        sourceUrl = "https://www.dpm.org.cn/court/lineage/226244.html",
    ),
    "忽必烈" to IndependentlyVerifiedDateConflict(
        publicDate = "1215年（故宫资料仅见年份）",
        sourceUrl = "https://www.dpm.org.cn/lemmas/242765.html",
    ),
    "孔子" to IndependentlyVerifiedDateConflict(
        publicDate = "公元前551年（出生日期有不同换算与记载）",
        sourceUrl = "https://www.jining.gov.cn/col/col108404/index.html",
    ),
    "朱棣" to IndependentlyVerifiedDateConflict(
        publicDate = "1360年（故宫资料载四月十七日，未据此伪造公历日期）",
        sourceUrl = "https://www.dpm.org.cn/Uploads/File/pdf/a3/7f/76/a37f76fc804a13aa200a0b02185bba7e.pdf",
    ),
    "武则天" to IndependentlyVerifiedDateConflict(
        publicDate = "624年（公开资料仅可稳定确认年份）",
        sourceUrl = "https://www.dpm.org.cn/Uploads/pdf/1889/T00060_00.pdf",
    ),
    "王安石" to IndependentlyVerifiedDateConflict(
        publicDate = "1021-12-18",
        sourceUrl = "https://www.kflz.gov.cn/sitesources/nysjwjw/page_pc/xcjd/s/article6b572532770246de8d88e9cede115704.html",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1021,
            12,
            18,
            "https://www.kflz.gov.cn/sitesources/nysjwjw/page_pc/xcjd/s/article6b572532770246de8d88e9cede115704.html",
        ),
    ),
    "魏忠贤" to IndependentlyVerifiedDateConflict(
        publicDate = "1568年（故宫资料仅见年份）",
        sourceUrl = "https://www.dpm.org.cn/lemmas/241467.html",
    ),
    "王象乾" to IndependentlyVerifiedDateConflict(
        publicDate = "1546年（公开资料仅见年份）",
        sourceUrl = "https://mw.hebei.gov.cn/system/2025/06/13/030346730.shtml",
    ),
    "高拱" to IndependentlyVerifiedDateConflict(
        publicDate = "1512年（公开资料仅见年份）",
        sourceUrl = "https://wwj.zhengzhou.gov.cn/province/3181251.jhtml",
    ),
    "李春芳" to IndependentlyVerifiedDateConflict(
        publicDate = "1509年与1511年（公开资料异说）",
        sourceUrl = "https://www.jrwmw.gov.cn/news/?id=671&type=detail",
    ),
    "贾似道" to IndependentlyVerifiedDateConflict(
        publicDate = "1213年（公开资料仅见年份）",
        sourceUrl = "https://mzj.sh.gov.cn/lnb-wsws/20220419/bdfde5484a4f4b328601ba40356ac130.html",
    ),
    "史可法" to IndependentlyVerifiedDateConflict(
        publicDate = "1602年（公开资料仅见年份）",
        sourceUrl = "https://www.zmdsjw.gov.cn/sitesources/zmdjw/page_pc/gzdt/qfwy/articledb2173ac64264c90b40e4185bf785646.html",
    ),
    "岳钟琪" to IndependentlyVerifiedDateConflict(
        publicDate = "1686年（具体历法换算待核）",
        sourceUrl = "https://ljcd.gov.cn/show-40-94290-1.html",
    ),
    "夏言" to IndependentlyVerifiedDateConflict(
        publicDate = "1482年（公开资料仅见年份）",
        sourceUrl = "https://www.gzszx.gov.cn/gzzxb/web/doc/detail/d_1617885852074016",
    ),
    "杨一清" to IndependentlyVerifiedDateConflict(
        publicDate = "1454年（公开资料仅见年份）",
        sourceUrl = "https://www.nydi.gov.cn/sitesources/zksjjjcw/page_pc/lzjy/lzwh/article97180c6a6a1d4fbb98c8b52d56224a5d.html",
    ),
    "文彦博" to IndependentlyVerifiedDateConflict(
        publicDate = "1006年（公开资料仅见年份）",
        sourceUrl = "https://dfz.shaanxi.gov.cn/zslm/fzzlk/sxjz/xys_16200/201706/P020240923603583091519.pdf",
    ),
    "聂耳" to IndependentlyVerifiedDateConflict(
        publicDate = "1912-02-14",
        sourceUrl = "https://mzt.fujian.gov.cn/ztzl/dsxxjy/zggcdbnsj/202108/t20210824_5674737.htm",
    ),
    "王永庆" to IndependentlyVerifiedDateConflict(
        publicDate = "1917年（公开资料日不同：1月8日与1月18日）",
        sourceUrl = "https://news.cctv.com/taiwan/20081016/102324.shtml",
    ),
    "朱自清" to IndependentlyVerifiedDateConflict(
        publicDate = "1898-11-22",
        sourceUrl = "https://www.xinhuanet.com/politics/2018-11/20/c_129998309.htm",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1898,
            11,
            22,
            "https://www.xinhuanet.com/politics/2018-11/20/c_129998309.htm",
        ),
    ),
    "苏轼" to IndependentlyVerifiedDateConflict(
        publicDate = "1037-01-08",
        sourceUrl = "https://www.hzarchives.org.cn/info/4729",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1037,
            1,
            8,
            "https://www.hzarchives.org.cn/info/4729",
        ),
    ),
    "诸葛亮" to IndependentlyVerifiedDateConflict(
        publicDate = "181年（公开资料仅见年份）",
        sourceUrl = "https://www.linyi.gov.cn/info/1036/52358.htm",
    ),
    // 公开资料目前只明确到出生年份；已足以证明原包日期不能作为同一人物公开履历的绑定条件。
    "严嵩" to IndependentlyVerifiedDateConflict(
        publicDate = "1480年（公开资料仅见年份）",
        sourceUrl = "https://www.dpm.org.cn/lemmas/241436.html",
    ),
    "张居正" to IndependentlyVerifiedDateConflict(
        publicDate = "1525年（公开资料仅见年份）",
        sourceUrl = "https://www.dpm.org.cn/lemmas/241454.html",
    ),
    "岳飞" to IndependentlyVerifiedDateConflict(
        publicDate = "1103年（公开资料仅见年份）",
        sourceUrl = "https://minghuaji.dpm.org.cn/article/detail?id=20425",
    ),
    "于谦" to IndependentlyVerifiedDateConflict(
        publicDate = "1398年（公开资料仅见年份）",
        sourceUrl = "https://www.smxlz.gov.cn/sitesources/nysjwjw/page_pc/xcjd/s/article8493dbeb820f41c6a051c0beff161d7e.html",
    ),
    "戚继光" to IndependentlyVerifiedDateConflict(
        publicDate = "1528-11-12",
        sourceUrl = "https://www.jxdy.gov.cn/c105619/202105/c6f70e2ad31d4b368abc0176f86274a8.shtml",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1528,
            11,
            12,
            "https://www.jxdy.gov.cn/c105619/202105/c6f70e2ad31d4b368abc0176f86274a8.shtml",
        ),
    ),
    "海瑞" to IndependentlyVerifiedDateConflict(
        publicDate = "1514年（公开资料仅见年份）",
        sourceUrl = "https://m.ccdi.gov.cn/content/db/3d/26954.html",
    ),
    "林则徐" to IndependentlyVerifiedDateConflict(
        publicDate = "1785-08-30",
        sourceUrl = "https://www.minhou.gov.cn/xjwz/mlmh/rwls/gjmr/201706/t20170619_949982.htm",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1785,
            8,
            30,
            "https://www.minhou.gov.cn/xjwz/mlmh/rwls/gjmr/201706/t20170619_949982.htm",
        ),
    ),
    "张廷玉" to IndependentlyVerifiedDateConflict(
        publicDate = "1672年（公开资料仅见年份）",
        sourceUrl = "https://m.ccdi.gov.cn/content/ed/bb/9230.html",
    ),
    "王守仁" to IndependentlyVerifiedDateConflict(
        publicDate = "1472年（公开资料仅见年份）",
        sourceUrl = "https://catweb.ncl.edu.tw/userfiles/cat07/file/100/1317291873.pdf",
    ),
    "欧阳修" to IndependentlyVerifiedDateConflict(
        publicDate = "1007-08-01",
        sourceUrl = "https://www.hnhx.gov.cn/portal/zjhx/hxyx/webinfo/2021/01/1612766650239657.htm",
        authoritativeDate = IndependentlyVerifiedBirthDate(
            1007,
            8,
            1,
            "https://www.hnhx.gov.cn/portal/zjhx/hxyx/webinfo/2021/01/1612766650239657.htm",
        ),
    ),
)

private data class CuratedCelebrityTimelineItem(
    val year: Int,
    val content: String,
    val sourceUrl: String,
)

/**
 * 可考资料包没有迁移旧事件时的只读补充年表。
 * 每项均附公开资料链接；它不写回用户数据库，也不替代用户新增的关键事件。
 */
private fun BaziCase.curatedCelebrityPublicTimeline(): List<CaseNotesTimelineDraft> {
    val curated = if (!hasIndependentlyVerifiedPublicIdentity()) {
        emptyList()
    } else {
        curatedCelebrityTimelineSources[alias.celebrityResearchCanonicalName()].orEmpty()
    }
        .mapIndexed { index, item ->
        CaseNotesTimelineDraft(
            id = "curated-public-timeline-${alias.hashCode()}-$index",
            level = CaseEventTimelineLevel.ANNUAL,
            year = item.year,
            stemBranch = "资料",
            sourceLabel = "公开生平资料",
            status = "可核对",
            content = "${item.content}\n来源：${item.sourceUrl}",
        )
    }
    if (curated.isNotEmpty()) return curated

    val birthYear = when (val calendar = birthInput.calendarInput) {
        is BirthCalendarInput.Solar -> calendar.dateTime.year
        is BirthCalendarInput.Lunar -> calendar.dateTime.year
    }
    val fallback = if (sourceType == CaseSourceType.WENZHEN_WEB_IMPORT) {
        "该年份仅沿用原始导入排盘，不作为公开生平事实引用。该例尚待以姓名、阳历生日和公开来源完成同一人核验后，再补入有来源的生平时间线。"
    } else {
        "该年份仅沿用当前命例的出生输入，不作为公开生平事实引用。该例尚未关联可独立核对的公开人物资料；如需建立人物年表，应先补入姓名、生日和资料来源。"
    }
    return listOf(
        CaseNotesTimelineDraft(
            id = "curated-origin-birth-$id",
            level = CaseEventTimelineLevel.ANNUAL,
            year = birthYear,
            stemBranch = "资料",
            sourceLabel = if (sourceType == CaseSourceType.WENZHEN_WEB_IMPORT) "来源排盘年份" else "当前排盘年份",
            status = "待核实",
            content = fallback,
        ),
    )
}

private val curatedCelebrityTimelineSources = mapOf(
    "杨涟" to listOf(
        CuratedCelebrityTimelineItem(1607, "中进士，后任户部、兵科给事中，进入明末言官体系。", "https://www.dpm.org.cn/lemmas/242703.html"),
        CuratedCelebrityTimelineItem(1622, "上疏弹劾魏忠贤，列举其二十四大罪状。", "https://www.dpm.org.cn/lemmas/242703.html"),
        CuratedCelebrityTimelineItem(1625, "以罪名被捕入狱并死于狱中，后世常将其经历置于东林党与阉党冲突中讨论。", "https://www.dpm.org.cn/lemmas/242703.html"),
    ),
    "魏忠贤" to listOf(
        CuratedCelebrityTimelineItem(1589, "入宫，后在天启朝逐渐获得信任。", "https://www.dpm.org.cn/lemmas/241467.html"),
        CuratedCelebrityTimelineItem(1621, "熹宗即位后任司礼秉笔太监，继而提督东厂，权势扩大。", "https://www.dpm.org.cn/lemmas/241467.html"),
        CuratedCelebrityTimelineItem(1625, "借“乙丑诏狱”“丙寅诏狱”等迫害东林党人，阉党专权加剧。", "https://www.dpm.org.cn/lemmas/241467.html"),
        CuratedCelebrityTimelineItem(1627, "崇祯即位后被贬谪凤阳，旋遭逮治，途中自缢而死。", "https://www.dpm.org.cn/lemmas/241467.html"),
    ),
    "王象乾" to listOf(
        CuratedCelebrityTimelineItem(1571, "中进士，初授闻喜知县，后转任兵部并开始关注边防事务。", "https://mw.hebei.gov.cn/system/2025/06/13/030346730.shtml"),
        CuratedCelebrityTimelineItem(1589, "升山西右参政、驻防宣府，以招抚和整饬边防应对北方局势。", "https://mw.hebei.gov.cn/system/2025/06/13/030346730.shtml"),
        CuratedCelebrityTimelineItem(1608, "升蓟辽总督，主持加固边墙、整饬军屯与边防体系。", "https://mw.hebei.gov.cn/system/2025/06/13/030346730.shtml"),
        CuratedCelebrityTimelineItem(1628, "再受命为蓟辽总督，后获准致仕。", "https://mw.hebei.gov.cn/system/2025/06/13/030346730.shtml"),
        CuratedCelebrityTimelineItem(1630, "在山东新城去世，明廷追赠太师。", "https://mw.hebei.gov.cn/system/2025/06/13/030346730.shtml"),
    ),
    "王崇古" to listOf(
        CuratedCelebrityTimelineItem(1571, "推动与俺答汗议和互市，形成“俺答封贡”后的长期边境缓和局面。", "https://www.gdszx.gov.cn/zxkw/tzgj/2023/02/content/post_32870.html"),
        CuratedCelebrityTimelineItem(1578, "重游家乡伍姓湖并留下诗作；其边政实践和文人活动均有地方文献记载。", "https://www.gdszx.gov.cn/zxkw/tzgj/2023/02/content/post_32870.html"),
    ),
    "高拱" to listOf(
        CuratedCelebrityTimelineItem(1528, "中举，后入仕并逐步进入中枢政务体系。", "https://wwj.zhengzhou.gov.cn/province/3181251.jhtml"),
        CuratedCelebrityTimelineItem(1566, "入阁并参与机务，穆宗即位后进少保兼太子太保。", "https://wwj.zhengzhou.gov.cn/province/3181251.jhtml"),
        CuratedCelebrityTimelineItem(1569, "任内阁并兼吏部事，推动课吏、用人与边防整饬。", "https://wwj.zhengzhou.gov.cn/province/3181251.jhtml"),
        CuratedCelebrityTimelineItem(1572, "被逐去官归里，结束首辅生涯。", "https://wwj.zhengzhou.gov.cn/province/3181251.jhtml"),
        CuratedCelebrityTimelineItem(1578, "在新郑去世。", "https://wwj.zhengzhou.gov.cn/province/3181251.jhtml"),
    ),
    "李春芳" to listOf(
        CuratedCelebrityTimelineItem(1547, "殿试状元及第，授翰林院修撰。", "https://sk.taizhou.gov.cn/tzxp/art/2025/art_03494d8ba5e34111b57c3fadc4735c7a.html"),
        CuratedCelebrityTimelineItem(1568, "升任内阁首辅，参与隆庆朝中枢政务。", "https://szb.taizhou.gov.cn/cms_files/filemanager/923329986/attach/20235/2e35548fce9043d09ecf216b8cce7042.pdf"),
    ),
    "赵文华" to listOf(
        CuratedCelebrityTimelineItem(1557, "在三殿火灾后以工部尚书主持营建，相关材料记录其侵占建材修建私宅的问题。", "https://www.gdwsw.gov.cn/wsbl/content/post_40848.html"),
        CuratedCelebrityTimelineItem(1562, "严嵩集团失势后，赵文华作为其心腹所涉政治与营建问题成为嘉靖后期整肃的一部分。", "https://www.jssjw.gov.cn/art/2016/2/14/art_30_47962.html"),
    ),
    "贾似道" to listOf(
        CuratedCelebrityTimelineItem(1238, "登进士第，进入南宋仕途。", "https://mzj.sh.gov.cn/lnb-wsws/20220419/bdfde5484a4f4b328601ba40356ac130.html"),
        CuratedCelebrityTimelineItem(1259, "鄂州战事与对蒙议和处置成为其政治声誉的重要争议节点。", "https://mzj.sh.gov.cn/lnb-wsws/20220419/bdfde5484a4f4b328601ba40356ac130.html"),
        CuratedCelebrityTimelineItem(1261, "推行抑制囤积谷物等财政措施，后续推动公田法。", "https://mzj.sh.gov.cn/lnb-wsws/20220419/bdfde5484a4f4b328601ba40356ac130.html"),
        CuratedCelebrityTimelineItem(1264, "推行经界推排法，核查民户物力与赋役。", "https://mzj.sh.gov.cn/lnb-wsws/20220419/bdfde5484a4f4b328601ba40356ac130.html"),
        CuratedCelebrityTimelineItem(1275, "改革与执政生涯结束；其政策成败与历史形象仍有不同评价。", "https://mzj.sh.gov.cn/lnb-wsws/20220419/bdfde5484a4f4b328601ba40356ac130.html"),
    ),
    "史可法" to listOf(
        CuratedCelebrityTimelineItem(1627, "中举人。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668715/index.html"),
        CuratedCelebrityTimelineItem(1628, "成进士，进入明末仕途。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668715/index.html"),
        CuratedCelebrityTimelineItem(1637, "擢右佥都御史，提督军务并巡抚安庆、庐州、太平、池州等地。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668715/index.html"),
        CuratedCelebrityTimelineItem(1645, "清军围攻扬州，城破后拒降遇害；后世以衣冠葬梅花岭纪念。", "https://www.zmdsjw.gov.cn/sitesources/zmdjw/page_pc/gzdt/qfwy/articledb2173ac64264c90b40e4185bf785646.html"),
        CuratedCelebrityTimelineItem(1786, "其玄孙汇集遗稿，刊行《史忠正公文集》。", "https://www.zmdsjw.gov.cn/sitesources/zmdjw/page_pc/gzdt/qfwy/articledb2173ac64264c90b40e4185bf785646.html"),
    ),
    "岳钟琪" to listOf(
        CuratedCelebrityTimelineItem(1686, "出生；公开资料对具体历法换算仍需审慎处理。", "https://ljcd.gov.cn/show-40-94290-1.html"),
        CuratedCelebrityTimelineItem(1717, "以四川永宁协副将参与平定拉萨叛乱及相关军事行动。", "https://ljcd.gov.cn/show-40-94290-1.html"),
        CuratedCelebrityTimelineItem(1727, "升任四川大将军督总元帅，参与清前期西北、西南边务。", "https://ytsc.rootinhenan.gov.cn/sitesources/ytsc/page_pc/hnly/xsgq/articleb3ae97e99dd04bbaa7911202bf5e877c.html"),
        CuratedCelebrityTimelineItem(1754, "在军中病逝，获赐祭葬，谥“襄勤”。", "https://ytsc.rootinhenan.gov.cn/sitesources/ytsc/page_pc/hnly/xsgq/articleb3ae97e99dd04bbaa7911202bf5e877c.html"),
    ),
    "孙承宗" to listOf(
        CuratedCelebrityTimelineItem(1621, "天启帝即位后任日讲官，受到重用。", "https://www.jsdfjw.gov.cn/ljwh/qfsj/content_10521"),
        CuratedCelebrityTimelineItem(1622, "辽东广宁战败后受命为兵部尚书兼东阁大学士，经略辽东。", "https://www.jsdfjw.gov.cn/ljwh/qfsj/content_10521"),
        CuratedCelebrityTimelineItem(1622, "整顿练兵、军饷与屯田，并增设防务、修建蓟镇亭障。", "https://www.jsdfjw.gov.cn/ljwh/qfsj/content_10521"),
        CuratedCelebrityTimelineItem(1625, "魏忠贤专权后被党人高第取代辽东经略职务，辽东防务格局随之变化。", "https://www.gdwsw.gov.cn/rwcq/content/post_22575.html"),
    ),
    "夏言" to listOf(
        CuratedCelebrityTimelineItem(1517, "中进士，次年以行人身份奉诏出使云贵等地。", "https://www.gzszx.gov.cn/gzzxb/web/doc/detail/d_1617885852074016"),
        CuratedCelebrityTimelineItem(1548, "在嘉靖朝政治斗争中遇害；其仕途与严嵩崛起的关系成为后世讨论主题。", "https://www.sxfj.gov.cn/723f88/ef332f10941033.shtml"),
    ),
    "杨一清" to listOf(
        CuratedCelebrityTimelineItem(1472, "中进士，开始跨越成化、弘治、正德、嘉靖四朝的仕途。", "https://www.nydi.gov.cn/sitesources/zksjjjcw/page_pc/lzjy/lzwh/article97180c6a6a1d4fbb98c8b52d56224a5d.html"),
        CuratedCelebrityTimelineItem(1502, "督理陕西马政，整顿牧场、军人和茶马旧制。", "https://www.nydi.gov.cn/sitesources/zksjjjcw/page_pc/lzjy/lzwh/article97180c6a6a1d4fbb98c8b52d56224a5d.html"),
        CuratedCelebrityTimelineItem(1504, "巡抚陕西并参与西北边防，后总制三镇军务。", "https://www.nydi.gov.cn/sitesources/zksjjjcw/page_pc/lzjy/lzwh/article97180c6a6a1d4fbb98c8b52d56224a5d.html"),
        CuratedCelebrityTimelineItem(1530, "遭削籍后抑郁而逝，后获追赠太保、谥“文襄”。", "https://www.nydi.gov.cn/sitesources/zksjjjcw/page_pc/lzjy/lzwh/article97180c6a6a1d4fbb98c8b52d56224a5d.html"),
    ),
    "文彦博" to listOf(
        CuratedCelebrityTimelineItem(1006, "出生；后以进士入仕。", "https://dfz.shaanxi.gov.cn/zslm/fzzlk/sxjz/xys_16200/201706/P020240923603583091519.pdf"),
        CuratedCelebrityTimelineItem(1082, "以洛阳留守身份参与耆英会，成为北宋士大夫晚年文化生活的著名记录。", "https://www.tjjw.gov.cn/lswh/2022/11/14/detail_2022111470596.html"),
        CuratedCelebrityTimelineItem(1097, "去世；其长期宰辅经历与北宋政局讨论持续见于后世文献。", "https://dfz.shaanxi.gov.cn/zslm/fzzlk/sxjz/xys_16200/201706/P020240923603583091519.pdf"),
    ),
    "马新贻" to listOf(
        CuratedCelebrityTimelineItem(1868, "曾国藩调任直隶总督后，马新贻受命接任两江总督。", "https://www.dajs.gov.cn/attach/0/180308144458453.pdf"),
        CuratedCelebrityTimelineItem(1870, "在总督衙门附近校阅武职考试时遭张文祥刺伤，次日救治无效身亡。", "https://www.gzszx.gov.cn/wstd/wsmb/40609.shtml"),
        CuratedCelebrityTimelineItem(1870, "“刺马案”围绕主使与动机存在多种传闻；档案证词与后世笔记、戏剧演绎应明确区分。", "https://www.gzszx.gov.cn/wstd/wsmb/40609.shtml"),
    ),
    "阿尔伯特·爱因斯坦" to listOf(
        CuratedCelebrityTimelineItem(1905, "发表光电效应与狭义相对论相关论文，成为现代物理学的重要转折。", "https://www.nobelprize.org/prizes/physics/1921/einstein/biographical/"),
        CuratedCelebrityTimelineItem(1915, "完成广义相对论场方程，重新定义引力与时空的关系。", "https://www.einstein-online.info/en/spotlight/general_relativity/"),
    ),
    "史蒂夫·乔布斯" to listOf(
        CuratedCelebrityTimelineItem(1976, "与史蒂夫·沃兹尼亚克共同创办 Apple。", "https://www.apple.com/leadership/"),
        CuratedCelebrityTimelineItem(2007, "发布 iPhone，推动智能手机成为大众计算平台。", "https://www.apple.com/newsroom/2007/01/09Apple-Reinvents-the-Phone-with-iPhone/"),
    ),
    "斯蒂芬·霍金" to listOf(
        CuratedCelebrityTimelineItem(1974, "提出黑洞量子效应相关理论，后被广泛称为霍金辐射。", "https://www.britannica.com/biography/Stephen-Hawking"),
        CuratedCelebrityTimelineItem(1988, "出版《时间简史》，显著扩大宇宙学在公众中的传播。", "https://www.hawking.org.uk/"),
    ),
    "比尔·盖茨" to listOf(
        CuratedCelebrityTimelineItem(1975, "与保罗·艾伦共同创办 Microsoft。", "https://www.microsoft.com/en-us/about"),
        CuratedCelebrityTimelineItem(2000, "与梅琳达·盖茨共同创立基金会，持续投入全球健康与教育。", "https://www.gatesfoundation.org/about"),
    ),
    "李小龙" to listOf(
        CuratedCelebrityTimelineItem(1967, "创立截拳道理念，强调实用、开放与个人化训练。", "https://www.brucelee.com/"),
        CuratedCelebrityTimelineItem(1972, "《精武门》上映，扩大华语动作电影的国际影响。", "https://www.britannica.com/biography/Bruce-Lee"),
    ),
    "泰勒·斯威夫特" to listOf(
        CuratedCelebrityTimelineItem(2006, "发行同名首张专辑，开启职业录音生涯。", "https://www.taylorswift.com/"),
        CuratedCelebrityTimelineItem(2023, "启动并持续进行 The Eras Tour，成为全球性巡演文化现象。", "https://www.taylorswift.com/"),
    ),
    "习近平" to listOf(
        CuratedCelebrityTimelineItem(2012, "当选中共中央总书记。", "https://www.gov.cn/guoqing/2012-11/15/content_2262624.htm"),
        CuratedCelebrityTimelineItem(2013, "当选中华人民共和国主席。", "https://www.gov.cn/guoqing/2013-03/14/content_2580704.htm"),
    ),
    "李克强" to listOf(
        CuratedCelebrityTimelineItem(2013, "出任国务院总理。", "https://www.gov.cn/guoqing/2013-03/15/content_2580846.htm"),
        CuratedCelebrityTimelineItem(2023, "卸任国务院总理职务。", "https://www.gov.cn/guowuyuan/2023-03/11/content_5745945.htm"),
    ),
    "王力宏" to listOf(
        CuratedCelebrityTimelineItem(1995, "发行首张个人专辑，开始职业音乐创作与表演生涯。", "https://www.wanglee.com/"),
        CuratedCelebrityTimelineItem(2000, "以融合流行、R&B 与华语元素的创作扩大音乐影响力。", "https://www.wanglee.com/"),
    ),
    "阿黛尔" to listOf(
        CuratedCelebrityTimelineItem(2008, "发行首张专辑《19》，以创作歌手身份进入国际乐坛。", "https://www.adele.com/"),
        CuratedCelebrityTimelineItem(2011, "专辑《21》取得全球性成功，代表作广泛传播。", "https://www.grammy.com/artists/adele/5281"),
        CuratedCelebrityTimelineItem(2021, "发行专辑《30》，延续以个人叙事为核心的创作风格。", "https://www.adele.com/"),
    ),
    "Lady Gaga" to listOf(
        CuratedCelebrityTimelineItem(2008, "发行《The Fame》，以鲜明舞台形象与流行作品获得全球关注。", "https://www.grammy.com/artists/lady-gaga/3611"),
        CuratedCelebrityTimelineItem(2018, "主演《一个明星的诞生》，拓展影视表演影响力。", "https://www.oscars.org/oscars/ceremonies/2019"),
    ),
    "纳尔逊·曼德拉" to listOf(
        CuratedCelebrityTimelineItem(1990, "结束长期监禁获释，成为南非民主转型的重要象征。", "https://www.nelsonmandela.org/"),
        CuratedCelebrityTimelineItem(1994, "当选南非总统，推动和解与民主转型。", "https://www.nelsonmandela.org/"),
    ),
    "斯蒂芬·库里" to listOf(
        CuratedCelebrityTimelineItem(2015, "获得 NBA 常规赛最有价值球员，并带领勇士夺冠。", "https://www.nba.com/player/201939/stephen-curry"),
        CuratedCelebrityTimelineItem(2022, "带领勇士再获 NBA 总冠军并当选总决赛最有价值球员。", "https://www.nba.com/player/201939/stephen-curry"),
    ),
    "科比·布莱恩特" to listOf(
        CuratedCelebrityTimelineItem(1996, "进入 NBA，开启洛杉矶湖人职业生涯。", "https://www.nba.com/stats/player/977"),
        CuratedCelebrityTimelineItem(2008, "获得 NBA 常规赛最有价值球员。", "https://www.nba.com/stats/player/977"),
    ),
    "迈克尔·菲尔普斯" to listOf(
        CuratedCelebrityTimelineItem(2008, "北京奥运会获得八枚金牌，创下单届奥运会金牌纪录。", "https://olympics.com/en/athletes/michael-phelps"),
        CuratedCelebrityTimelineItem(2016, "里约奥运会后结束奥运征程，累计 23 枚奥运金牌。", "https://olympics.com/en/athletes/michael-phelps"),
    ),
    "尤塞恩·博尔特" to listOf(
        CuratedCelebrityTimelineItem(2008, "北京奥运会获得男子 100 米、200 米金牌。", "https://worldathletics.org/athletes/jamaica/usain-bolt-14201847"),
        CuratedCelebrityTimelineItem(2009, "柏林世锦赛创造男子 100 米和 200 米世界纪录。", "https://worldathletics.org/athletes/jamaica/usain-bolt-14201847"),
    ),
    "释永信" to listOf(
        CuratedCelebrityTimelineItem(1999, "出任少林寺住持，开始主持寺院日常管理与文化传播事务。", "https://www.shaolin.org.cn/"),
        CuratedCelebrityTimelineItem(2006, "少林寺相关文化传播活动持续受到国内外公众关注。", "https://www.shaolin.org.cn/"),
    ),
    "埃隆·马斯克" to listOf(
        CuratedCelebrityTimelineItem(2002, "创办 SpaceX，进入商业航天领域。", "https://www.spacex.com/"),
        CuratedCelebrityTimelineItem(2008, "Tesla 推出 Roadster，推动电动车进入更广泛的公众视野。", "https://www.tesla.com/about"),
    ),
    "马克·扎克伯格" to listOf(
        CuratedCelebrityTimelineItem(2004, "参与创办 Facebook，社交网络开始快速扩展。", "https://about.meta.com/company-info/"),
        CuratedCelebrityTimelineItem(2021, "Facebook 公司更名为 Meta，转向元宇宙相关战略布局。", "https://about.meta.com/company-info/"),
    ),
    "杰夫·贝索斯" to listOf(
        CuratedCelebrityTimelineItem(1994, "创办 Amazon，起步于在线图书销售。", "https://www.aboutamazon.com/about-us"),
        CuratedCelebrityTimelineItem(2000, "创办 Blue Origin，参与商业航天探索。", "https://www.blueorigin.com/"),
    ),
    "迈克尔·乔丹" to listOf(
        CuratedCelebrityTimelineItem(1984, "进入 NBA，开始芝加哥公牛队职业生涯。", "https://www.nba.com/stats/player/893"),
        CuratedCelebrityTimelineItem(1991, "带领公牛赢得队史首个 NBA 总冠军。", "https://www.nba.com/stats/player/893"),
    ),
    "利昂内尔·梅西" to listOf(
        CuratedCelebrityTimelineItem(2004, "完成巴塞罗那一线队正式比赛首秀。", "https://www.fcbarcelona.com/en/football/first-team/players"),
        CuratedCelebrityTimelineItem(2022, "随阿根廷队获得世界杯冠军。", "https://www.fifa.com/"),
    ),
    "克里斯蒂亚诺·罗纳尔多" to listOf(
        CuratedCelebrityTimelineItem(2008, "首次获得金球奖。", "https://www.ballondor.com/"),
        CuratedCelebrityTimelineItem(2016, "随葡萄牙队获得欧洲杯冠军。", "https://www.uefa.com/"),
    ),
    "纳伦德拉·莫迪" to listOf(
        CuratedCelebrityTimelineItem(2001, "出任古吉拉特邦首席部长。", "https://www.pmindia.gov.in/en/pms-profile/"),
        CuratedCelebrityTimelineItem(2014, "出任印度总理。", "https://www.pmindia.gov.in/en/pms-profile/"),
    ),
    "贝拉克·奥巴马" to listOf(
        CuratedCelebrityTimelineItem(2008, "当选美国总统。", "https://obamawhitehouse.archives.gov/about/presidents/barackobama"),
        CuratedCelebrityTimelineItem(2009, "就任美国总统，成为美国首位非裔总统。", "https://obamawhitehouse.archives.gov/about/presidents/barackobama"),
    ),
    "乔·拜登" to listOf(
        CuratedCelebrityTimelineItem(1972, "首次当选美国联邦参议员，开启长期公共服务生涯。", "https://www.whitehouse.gov/administration/president-biden/"),
        CuratedCelebrityTimelineItem(2021, "就任美国总统。", "https://www.whitehouse.gov/administration/president-biden/"),
    ),
    "勒布朗·詹姆斯" to listOf(
        CuratedCelebrityTimelineItem(2003, "进入 NBA，开始职业篮球生涯。", "https://www.nba.com/stats/player/2544"),
        CuratedCelebrityTimelineItem(2023, "成为 NBA 历史常规赛得分王。", "https://www.nba.com/stats/player/2544"),
    ),
    "卡尔·萨根" to listOf(
        CuratedCelebrityTimelineItem(1977, "参与旅行者号金唱片等太空科学传播项目。", "https://science.nasa.gov/mission/voyager/"),
        CuratedCelebrityTimelineItem(1980, "《Cosmos》播出，成为全球科学传播的重要作品。", "https://www.britannica.com/biography/Carl-Sagan"),
    ),
    "唐纳德·特朗普" to listOf(
        CuratedCelebrityTimelineItem(2016, "当选美国总统。", "https://www.whitehouse.gov/about-the-white-house/presidents/donald-j-trump/"),
        CuratedCelebrityTimelineItem(2017, "就任美国总统。", "https://www.whitehouse.gov/about-the-white-house/presidents/donald-j-trump/"),
    ),
    "圣雄甘地" to listOf(
        CuratedCelebrityTimelineItem(1915, "结束南非时期后返回印度，投身印度独立运动。", "https://www.britannica.com/biography/Mahatma-Gandhi"),
        CuratedCelebrityTimelineItem(1930, "领导食盐进军，推动非暴力不合作运动。", "https://www.britannica.com/biography/Mahatma-Gandhi"),
    ),
    "塞雷娜·威廉姆斯" to listOf(
        CuratedCelebrityTimelineItem(1999, "获得首个美国网球公开赛女单冠军。", "https://www.wtatennis.com/players/230234/serena-williams"),
        CuratedCelebrityTimelineItem(2017, "获得个人第 23 个大满贯女单冠军。", "https://www.wtatennis.com/players/230234/serena-williams"),
    ),
    "奥普拉·温弗瑞" to listOf(
        CuratedCelebrityTimelineItem(1986, "《奥普拉·温弗瑞秀》在美国全国播出。", "https://www.oprah.com/"),
        CuratedCelebrityTimelineItem(2011, "OWN 电视网络开播，拓展媒体经营版图。", "https://www.oprah.com/"),
    ),
    "安吉丽娜·朱莉" to listOf(
        CuratedCelebrityTimelineItem(1999, "凭《移魂女郎》获得奥斯卡最佳女配角。", "https://www.oscars.org/oscars/ceremonies/2000"),
        CuratedCelebrityTimelineItem(2001, "开始与联合国难民署合作并投入难民事务倡导。", "https://www.unhcr.org/"),
    ),
    "布拉德·皮特" to listOf(
        CuratedCelebrityTimelineItem(1991, "凭《末路狂花》受到广泛关注。", "https://www.britannica.com/biography/Brad-Pitt"),
        CuratedCelebrityTimelineItem(2020, "凭《好莱坞往事》获得奥斯卡最佳男配角。", "https://www.oscars.org/oscars/ceremonies/2020"),
    ),
    "弗拉基米尔·普京" to listOf(
        CuratedCelebrityTimelineItem(1999, "出任俄罗斯政府总理。", "https://www.britannica.com/biography/Vladimir-Putin"),
        CuratedCelebrityTimelineItem(2000, "首次当选俄罗斯总统。", "https://www.britannica.com/biography/Vladimir-Putin"),
    ),
    "弗拉基米尔·泽连斯基" to listOf(
        CuratedCelebrityTimelineItem(2019, "当选乌克兰总统。", "https://www.president.gov.ua/en/"),
        CuratedCelebrityTimelineItem(2022, "在俄乌战争全面升级后持续领导乌克兰政府。", "https://www.president.gov.ua/en/"),
    ),
    "成龙" to listOf(
        CuratedCelebrityTimelineItem(1978, "《蛇形刁手》《醉拳》等作品推动其动作喜剧风格成型。", "https://www.britannica.com/biography/Jackie-Chan"),
        CuratedCelebrityTimelineItem(2016, "获得奥斯卡终身成就奖。", "https://www.oscars.org/"),
    ),
    "戴安娜王妃" to listOf(
        CuratedCelebrityTimelineItem(1981, "与威尔士亲王查尔斯结婚，成为英国王室公众人物。", "https://www.royal.uk/"),
        CuratedCelebrityTimelineItem(1997, "持续参与反地雷倡导，其公共行动获得国际关注。", "https://www.un.org/"),
    ),
    "比尔·克林顿" to listOf(
        CuratedCelebrityTimelineItem(1992, "当选美国总统。", "https://clintonwhitehouse6.archives.gov/WH/Accomplishments/"),
        CuratedCelebrityTimelineItem(2001, "卸任后通过克林顿基金会持续参与公共事务。", "https://www.clintonfoundation.org/"),
    ),
    "沃伦·巴菲特" to listOf(
        CuratedCelebrityTimelineItem(1965, "取得伯克希尔·哈撒韦控制权并展开长期经营。", "https://www.berkshirehathaway.com/"),
        CuratedCelebrityTimelineItem(2006, "宣布大规模慈善捐赠计划。", "https://www.gatesfoundation.org/"),
    ),
    "玛丽莲·梦露" to listOf(
        CuratedCelebrityTimelineItem(1953, "《绅士爱美人》等作品巩固其银幕偶像地位。", "https://www.britannica.com/biography/Marilyn-Monroe"),
        CuratedCelebrityTimelineItem(1959, "主演《热情似火》，该片成为经典喜剧电影。", "https://www.britannica.com/biography/Marilyn-Monroe"),
    ),
    "碧昂丝" to listOf(
        CuratedCelebrityTimelineItem(2003, "发行首张个人专辑《Dangerously in Love》，开启个人事业高峰。", "https://www.grammy.com/artists/beyonce/2877"),
        CuratedCelebrityTimelineItem(2016, "发行视觉专辑《Lemonade》，扩大音乐与影像表达影响。", "https://www.grammy.com/artists/beyonce/2877"),
    ),
    "简·古道尔" to listOf(
        CuratedCelebrityTimelineItem(1960, "在贡贝开始黑猩猩长期野外研究。", "https://janegoodall.org/our-story/about-jane/"),
        CuratedCelebrityTimelineItem(1977, "创立简·古道尔研究所，持续推进保育与教育工作。", "https://janegoodall.org/"),
    ),
    "罗杰·费德勒" to listOf(
        CuratedCelebrityTimelineItem(2003, "获得首个温网男单冠军。", "https://www.atptour.com/en/players/roger-federer/f324/overview"),
        CuratedCelebrityTimelineItem(2018, "夺得第 20 个大满贯男单冠军。", "https://www.atptour.com/en/players/roger-federer/f324/overview"),
    ),
    "艾伦·图灵" to listOf(
        CuratedCelebrityTimelineItem(1936, "发表可计算性研究论文，提出图灵机思想。", "https://www.britannica.com/biography/Alan-Turing"),
        CuratedCelebrityTimelineItem(1940, "在布莱切利园参与德军密码分析工作。", "https://bletchleypark.org.uk/our-story/alan-turing"),
    ),
    "莱昂纳多·迪卡普里奥" to listOf(
        CuratedCelebrityTimelineItem(1997, "主演《泰坦尼克号》，成为全球知名电影演员。", "https://www.britannica.com/biography/Leonardo-DiCaprio"),
        CuratedCelebrityTimelineItem(2016, "凭《荒野猎人》获得奥斯卡最佳男主角。", "https://www.oscars.org/oscars/ceremonies/2016"),
    ),
    "萨尔玛·海耶克" to listOf(
        CuratedCelebrityTimelineItem(2002, "主演并制作《Frida》，获得奥斯卡最佳女主角提名。", "https://www.oscars.org/oscars/ceremonies/2003"),
        CuratedCelebrityTimelineItem(2021, "因影视制作与社会倡导持续扩大国际影响。", "https://www.britannica.com/biography/Salma-Hayek"),
    ),
    "迈克尔·B·乔丹" to listOf(
        CuratedCelebrityTimelineItem(2013, "主演《弗鲁特韦尔车站》，获得广泛演技关注。", "https://www.britannica.com/biography/Michael-B-Jordan"),
        CuratedCelebrityTimelineItem(2018, "出演《黑豹》，推动其全球知名度提升。", "https://www.marvel.com/movies/black-panther"),
    ),
    "迈克尔·杰克逊" to listOf(
        CuratedCelebrityTimelineItem(1982, "发行《Thriller》，重塑流行音乐与音乐录像传播。", "https://www.grammy.com/artists/michael-jackson/13202"),
        CuratedCelebrityTimelineItem(1983, "在电视表演中展示月球漫步，形成标志性舞台形象。", "https://www.britannica.com/biography/Michael-Jackson"),
    ),
    "道恩·强森" to listOf(
        CuratedCelebrityTimelineItem(1996, "加入 WWE，开启职业摔角生涯。", "https://www.wwe.com/superstars/the-rock"),
        CuratedCelebrityTimelineItem(2001, "出演《木乃伊归来》，转向商业电影市场。", "https://www.britannica.com/biography/Dwayne-Johnson"),
    ),
    "金·卡戴珊" to listOf(
        CuratedCelebrityTimelineItem(2007, "真人秀《与卡戴珊一家同行》开播，形成高曝光度媒体影响。", "https://www.britannica.com/biography/Kim-Kardashian"),
        CuratedCelebrityTimelineItem(2019, "创立 SKIMS，拓展个人消费品牌业务。", "https://skims.com/"),
    ),
    "麦当娜" to listOf(
        CuratedCelebrityTimelineItem(1983, "发行首张同名专辑，进入国际流行音乐舞台。", "https://www.grammy.com/artists/madonna/5005"),
        CuratedCelebrityTimelineItem(1989, "《Like a Prayer》推动其音乐与公众形象的进一步转型。", "https://www.grammy.com/artists/madonna/5005"),
    ),
    "万民英" to listOf(
        CuratedCelebrityTimelineItem(1522, "出生于明嘉靖元年，后以仕宦与著述活动为人所知。", "https://zh.wikipedia.org/wiki/%E8%90%AC%E6%B0%91%E8%8B%B1"),
        CuratedCelebrityTimelineItem(1550, "中式庚戌科进士，后历任知县、御史及福建布政使司参议等职。", "https://zh.wikipedia.org/wiki/%E8%90%AC%E6%B0%91%E8%8B%B1"),
        CuratedCelebrityTimelineItem(1603, "去世；《三命通会》《星学大成》等著作作为传统命理文献流传。", "https://www.shidianguji.com/book/NGJ89241199902970167139/chapter/1lvordtml3pc6"),
    ),
    "刘伯温" to listOf(
        CuratedCelebrityTimelineItem(1360, "参与朱元璋集团的军政谋划，成为明初建国人物之一。", "https://zh.wikipedia.org/wiki/%E5%88%98%E5%9F%BA"),
        CuratedCelebrityTimelineItem(1375, "去世后，其政治、文学与民间传说形象持续流传。", "https://zh.wikipedia.org/wiki/%E5%88%98%E5%9F%BA"),
    ),
    "张居正" to listOf(
        CuratedCelebrityTimelineItem(1572, "出任内阁首辅，主持万历初年军政与财政改革。", "https://www.dpm.org.cn/court/figure/104014.html"),
        CuratedCelebrityTimelineItem(1581, "推行一条鞭法等改革措施，调整赋役征收与行政运行。", "https://www.dpm.org.cn/court/figure/104014.html"),
    ),
    "严嵩" to listOf(
        CuratedCelebrityTimelineItem(1542, "入阁并在嘉靖朝中枢长期任职。", "https://www.dpm.org.cn/lemmas/241436.html"),
        CuratedCelebrityTimelineItem(1562, "严氏父子因罪被劾，严嵩遭革职、抄家。", "https://www.dpm.org.cn/lemmas/241436.html"),
    ),
    "孔子" to listOf(
        CuratedCelebrityTimelineItem(-551, "生于鲁国；后成为春秋末期重要思想家和教育家。", "https://www.dpm.org.cn/lemmas/243277.html"),
        CuratedCelebrityTimelineItem(-497, "离开鲁国后周游列国，传播其政治与教育理念。", "https://tct.gov.taipei/cp.aspx?n=D1F0936A870F828C"),
        CuratedCelebrityTimelineItem(-479, "去世；其教育、思想及经典整理工作持续影响后世。", "https://www.dpm.org.cn/lemmas/243277.html"),
    ),
    "岳飞" to listOf(
        CuratedCelebrityTimelineItem(1127, "投身南宋抗金战事，开始军事生涯。", "https://www.hzarchives.org.cn/info/4732"),
        CuratedCelebrityTimelineItem(1140, "在郾城等地抗击金军，取得重要战果。", "https://ytsc.rootinhenan.gov.cn/sitesources/ytsc/page_pc/hnly/xsgq/articleb3ae97e99dd04bbaa7911202bf5e877c.html"),
        CuratedCelebrityTimelineItem(1142, "在政治冲突中遇害，后世围绕其军事功绩与冤案形成持续历史叙述。", "https://www.hzarchives.org.cn/info/4732"),
    ),
    "左宗棠" to listOf(
        CuratedCelebrityTimelineItem(1875, "督办新疆军务，组织收复新疆相关军事行动。", "https://www.zjda.gov.cn/col/col1402604/index.html"),
        CuratedCelebrityTimelineItem(1884, "中法战争期间督办福建军务。", "https://www.zjda.gov.cn/col/col1402604/index.html"),
        CuratedCelebrityTimelineItem(1885, "卒于福州，留下边疆治理、洋务与军事相关著述和实践。", "https://www.xiangyin.gov.cn/31165/31172/content_978011.html"),
    ),
    "和珅" to listOf(
        CuratedCelebrityTimelineItem(1776, "升任户部侍郎并逐步进入军机与内务府核心事务。", "https://www.dpm.org.cn/court/event/158665.html"),
        CuratedCelebrityTimelineItem(1792, "兼任翰林院掌院学士等职，权力达到高峰。", "https://www.dpm.org.cn/court/figure/104004.html"),
        CuratedCelebrityTimelineItem(1799, "乾隆去世后被嘉庆帝以二十条大罪赐死、抄家。", "https://www.dpm.org.cn/court/figure/104004.html"),
    ),
    "康熙" to listOf(
        CuratedCelebrityTimelineItem(1661, "继承皇位，开始康熙朝统治。", "https://www.dpm.org.cn/court/lineage/226256.html?pid=15359&url=http%3A%2F%2Fwww.dpm.org.cn%2Fcourt%2Flineage%2F226256.html"),
        CuratedCelebrityTimelineItem(1683, "清军统一台湾，清初国家整合进一步完成。", "https://www.dpm.org.cn/court/lineage/226256.html?pid=15359&url=http%3A%2F%2Fwww.dpm.org.cn%2Fcourt%2Flineage%2F226256.html"),
        CuratedCelebrityTimelineItem(1689, "与俄国签订《尼布楚条约》，处理东北边界问题。", "https://www.dpm.org.cn/court/lineage/226256.html?pid=15359&url=http%3A%2F%2Fwww.dpm.org.cn%2Fcourt%2Flineage%2F226256.html"),
    ),
    "雍正" to listOf(
        CuratedCelebrityTimelineItem(1722, "继承皇位，次年改元雍正。", "https://www.dpm.org.cn/court/lineage/226259.html"),
        CuratedCelebrityTimelineItem(1729, "设军机处，强化处理军国要务的中枢机制。", "https://www.dpm.org.cn/court/lineage/226259.html"),
        CuratedCelebrityTimelineItem(1735, "去世，清高宗继位。", "https://www.dpm.org.cn/court/lineage/226259.html"),
    ),
    "乾隆" to listOf(
        CuratedCelebrityTimelineItem(1735, "继承皇位，次年改元乾隆。", "https://www.dpm.org.cn/court/lineage/226263.html"),
        CuratedCelebrityTimelineItem(1772, "下令开设四库全书馆，组织大规模典籍整理与编纂。", "https://www.dpm.org.cn/court/lineage/226263.html"),
        CuratedCelebrityTimelineItem(1795, "宣布让位于皇太子颙琰，次年成为太上皇。", "https://www.dpm.org.cn/court/lineage/226263.html"),
    ),
    "嘉庆" to listOf(
        CuratedCelebrityTimelineItem(1796, "继承皇位，次年改元嘉庆。", "https://www.dpm.org.cn/court/lineage/226238.html"),
        CuratedCelebrityTimelineItem(1799, "乾隆去世后亲政，并下令查办和珅。", "https://www.dpm.org.cn/court/lineage/226238.html"),
        CuratedCelebrityTimelineItem(1813, "天理教起事，暴露清中期政治与社会治理压力。", "https://www.dpm.org.cn/court/lineage/226238.html"),
    ),
    "嘉靖" to listOf(
        CuratedCelebrityTimelineItem(1521, "继承皇位，开始嘉靖朝统治。", "https://www.dpm.org.cn/court/lineage/226241.html"),
        CuratedCelebrityTimelineItem(1524, "大礼议冲突达到高潮，明中期政治格局出现重要变化。", "https://www.dpm.org.cn/court/lineage/226241.html"),
        CuratedCelebrityTimelineItem(1567, "去世，明穆宗继位。", "https://www.dpm.org.cn/court/lineage/226241.html"),
    ),
    "隆庆" to listOf(
        CuratedCelebrityTimelineItem(1567, "继承皇位，改元隆庆。", "https://www.dpm.org.cn/court/lineage/226243.html"),
        CuratedCelebrityTimelineItem(1568, "与俺答汗关系缓和，北方边务形势出现调整。", "https://www.dpm.org.cn/court/lineage/226243.html"),
        CuratedCelebrityTimelineItem(1572, "去世，明神宗继位。", "https://www.dpm.org.cn/court/lineage/226243.html"),
    ),
    "于谦" to listOf(
        CuratedCelebrityTimelineItem(1421, "中进士，进入明代官僚体系。", "https://www.dpm.org.cn/lemmas/243960.html"),
        CuratedCelebrityTimelineItem(1449, "土木之变后升兵部尚书，主张固守北京并参与统筹军务。", "https://www.dpm.org.cn/lemmas/243960.html"),
        CuratedCelebrityTimelineItem(1457, "英宗复位后以谋逆罪被杀，后世对其功过持续评价。", "https://www.dpm.org.cn/lemmas/243960.html"),
    ),
    "光绪" to listOf(
        CuratedCelebrityTimelineItem(1875, "同治帝无子后继承皇位，开始光绪朝统治。", "https://www.dpm.org.cn/court/lineage/226254.html"),
        CuratedCelebrityTimelineItem(1898, "支持维新变法；政变后被幽禁于瀛台。", "https://www.dpm.org.cn/court/lineage/226254.html"),
        CuratedCelebrityTimelineItem(1908, "逝世，溥仪继位。", "https://www.dpm.org.cn/court/lineage/226254.html"),
    ),
    "同治" to listOf(
        CuratedCelebrityTimelineItem(1861, "咸丰帝去世后即皇帝位，次年改元同治。", "https://www.dpm.org.cn/court/lineage/226248.html"),
        CuratedCelebrityTimelineItem(1873, "举行亲政大典。", "https://www.dpm.org.cn/court/lineage/226248.html"),
        CuratedCelebrityTimelineItem(1874, "病逝，光绪帝继位。", "https://www.dpm.org.cn/court/lineage/226248.html"),
    ),
    "丘濬" to listOf(
        CuratedCelebrityTimelineItem(1454, "登进士第，开始仕宦生涯。", "https://www.twjw.gov.cn/newsshow-17-889-1.html"),
        CuratedCelebrityTimelineItem(1477, "任国子监祭酒时开始编纂《大学衍义补》。", "https://www.twjw.gov.cn/newsshow-17-889-1.html"),
        CuratedCelebrityTimelineItem(1495, "卒于任上，朝廷追赠太傅，谥文庄。", "https://www.twjw.gov.cn/newsshow-17-889-1.html"),
    ),
    "余华" to listOf(
        CuratedCelebrityTimelineItem(1983, "开始发表小说，进入职业写作阶段。", "https://zh.wikipedia.org/wiki/%E4%BD%99%E8%8F%AF"),
        CuratedCelebrityTimelineItem(1993, "出版长篇小说《活着》，成为其最具影响力的作品之一。", "https://zh.wikipedia.org/wiki/%E4%BD%99%E8%8F%AF"),
        CuratedCelebrityTimelineItem(2021, "长篇小说《文城》出版，延续其小说创作。", "https://zh.wikipedia.org/wiki/%E4%BD%99%E8%8F%AF"),
    ),
    "刘国梁" to listOf(
        CuratedCelebrityTimelineItem(2009, "中国奥委会资料将其列为完成世乒赛、世界杯和奥运会“大满贯”的运动员，并记载其 16 次获得世界冠军。", "https://www.olympic.cn/news/olympic/2009/1001/40770.html"),
        CuratedCelebrityTimelineItem(2018, "当选中国乒乓球协会主席。", "https://cn.ittf.com/2018/12/lgywaschosentobecttapresident/"),
        CuratedCelebrityTimelineItem(2020, "出任 WTT 世界乒乓球职业大联盟理事会主席。", "https://cn.ittf.com/2020/06/%E5%88%98%E5%9B%BD%E6%A2%81%E5%87%BA%E4%BB%BBWTT%E4%B8%96%E7%95%8C%E4%B9%92%E4%B9%93%E7%90%83%E8%81%8C%E4%B8%9A%E5%A4%A7%E8%81%94%E7%9B%9F%E7%90%86%E4%BA%8B%E4%BC%9A%E4%B8%BB%E5%B8%AD/"),
        CuratedCelebrityTimelineItem(2025, "辞去中国乒协主席职务，完成协会届中调整。", "https://www.sport.gov.cn/n20001280/n20745751/c28655676/content.html"),
    ),
    "吴京" to listOf(
        CuratedCelebrityTimelineItem(2015, "自导自演的《战狼》上映；中国电影资料馆随后组织影片研讨。", "https://www.cfa.org.cn/eportal/ui?articleKey=d48eed34d4574143a1b523ff73640f42&columnId=bab0d753635c4d0bb869bc752c3b4e8e&pageId=37a0783c76bd4611b82235402ea26f03"),
        CuratedCelebrityTimelineItem(2021, "参演的《长津湖》上映，成为当年中国电影市场的重要现象。", "https://www.chinafilmnews.cn/UploadFiles/file/20211013/202110131210152742.pdf"),
    ),
    "崇祯" to listOf(
        CuratedCelebrityTimelineItem(1627, "继承皇位，次年改元崇祯。", "https://www.dpm.org.cn/court/lineage/226246.html"),
        CuratedCelebrityTimelineItem(1628, "清除魏忠贤为首的阉党，并尝试整顿吏治。", "https://www.dpm.org.cn/court/lineage/226246.html"),
        CuratedCelebrityTimelineItem(1644, "李自成军攻入北京后自缢，明朝覆亡。", "https://www.dpm.org.cn/court/lineage/226246.html"),
    ),
    "明建文帝" to listOf(
        CuratedCelebrityTimelineItem(1398, "继承皇位，次年改元建文。", "https://www.dpm.org.cn/court/lineage/226245.html"),
        CuratedCelebrityTimelineItem(1399, "推行削藩，随后爆发靖难之役。", "https://www.dpm.org.cn/court/lineage/226245.html"),
        CuratedCelebrityTimelineItem(1402, "燕王朱棣攻入南京；其后下落未明，成为历史谜案。", "https://www.dpm.org.cn/court/lineage/226245.html"),
    ),
    "朱棣" to listOf(
        CuratedCelebrityTimelineItem(1402, "靖难之役后即皇帝位，次年改元永乐。", "https://www.dpm.org.cn/court/lineage/226257.html"),
        CuratedCelebrityTimelineItem(1405, "命郑和率舟师出使多国，开启下西洋航行。", "https://www.dpm.org.cn/court/lineage/226257.html"),
        CuratedCelebrityTimelineItem(1421, "迁都北京，确立明代后期政治中心。", "https://www.dpm.org.cn/court/lineage/226257.html"),
    ),
    "皇太极" to listOf(
        CuratedCelebrityTimelineItem(1626, "继承后金汗位，推进内部统治与制度建设。", "https://www.dpm.org.cn/court/lineage/226251.html"),
        CuratedCelebrityTimelineItem(1636, "称帝，定国号为清，改女真为满洲。", "https://www.dpm.org.cn/court/lineage/226251.html"),
        CuratedCelebrityTimelineItem(1643, "去世，福临继位。", "https://www.dpm.org.cn/court/lineage/226251.html"),
    ),
    "顺治" to listOf(
        CuratedCelebrityTimelineItem(1643, "即皇帝位，次年改元顺治。", "https://www.dpm.org.cn/court/lineage/226262.html"),
        CuratedCelebrityTimelineItem(1644, "进入北京并举行登极大典，成为清入关后的第一位皇帝。", "https://www.dpm.org.cn/court/lineage/226262.html"),
        CuratedCelebrityTimelineItem(1661, "去世，康熙帝继位。", "https://www.dpm.org.cn/court/lineage/226262.html"),
    ),
    "咸丰" to listOf(
        CuratedCelebrityTimelineItem(1850, "继承皇位，次年改元咸丰。", "https://www.dpm.org.cn/court/lineage/226247.html"),
        CuratedCelebrityTimelineItem(1860, "英法联军攻占北京后，前往热河避居。", "https://www.dpm.org.cn/court/lineage/226247.html"),
        CuratedCelebrityTimelineItem(1861, "去世，载淳继位。", "https://www.dpm.org.cn/court/lineage/226247.html"),
    ),
    "道光" to listOf(
        CuratedCelebrityTimelineItem(1820, "继承皇位，次年改元道光。", "https://www.dpm.org.cn/court/lineage/226239.html"),
        CuratedCelebrityTimelineItem(1840, "鸦片战争爆发，清朝面临严峻外部军事与外交危机。", "https://www.dpm.org.cn/court/lineage/226239.html"),
        CuratedCelebrityTimelineItem(1842, "清政府签订《南京条约》。", "https://www.dpm.org.cn/court/lineage/226239.html"),
    ),
    "慈禧太后" to listOf(
        CuratedCelebrityTimelineItem(1852, "入宫，后逐步晋封。", "https://www.dpm.org.cn/court/figure/102753.html"),
        CuratedCelebrityTimelineItem(1861, "辛酉政变后与慈安太后开始垂帘听政。", "https://www.dpm.org.cn/court/figure/102753.html"),
        CuratedCelebrityTimelineItem(1898, "发动政变并再次垂帘，戊戌变法失败。", "https://www.dpm.org.cn/court/figure/102753.html"),
        CuratedCelebrityTimelineItem(1908, "去世，结束长期参与清廷决策的历程。", "https://www.dpm.org.cn/court/figure/102753.html"),
    ),
    "溥仪" to listOf(
        CuratedCelebrityTimelineItem(1908, "即皇帝位，次年改元宣统。", "https://www.dpm.org.cn/court/lineage/226255.html"),
        CuratedCelebrityTimelineItem(1912, "颁布退位诏书，清朝结束。", "https://www.dpm.org.cn/court/lineage/226255.html"),
        CuratedCelebrityTimelineItem(1932, "在日本侵略势力扶持下前往长春，成立伪满洲国。", "https://www.dpm.org.cn/court/lineage/226255.html"),
        CuratedCelebrityTimelineItem(1959, "获首批特赦释放。", "https://www.dpm.org.cn/court/lineage/226255.html"),
    ),
    "朱元璋" to listOf(
        CuratedCelebrityTimelineItem(1352, "参加郭子兴部起义，开始元末军政活动。", "https://www.dpm.org.cn/court/lineage/226244.html"),
        CuratedCelebrityTimelineItem(1368, "在应天称帝，建立明朝，年号洪武。", "https://www.dpm.org.cn/court/lineage/226244.html"),
        CuratedCelebrityTimelineItem(1380, "废除中书省和丞相制度，进一步强化皇权。", "https://www.dpm.org.cn/court/system/236406.html"),
        CuratedCelebrityTimelineItem(1398, "去世，皇太孙朱允炆继位。", "https://www.dpm.org.cn/court/lineage/226244.html"),
    ),
    "朱常洛" to listOf(
        CuratedCelebrityTimelineItem(1601, "在朝臣争取与慈圣太后支持下被册立为皇太子。", "https://www.dpm.org.cn/court/lineage/226242.html"),
        CuratedCelebrityTimelineItem(1620, "万历帝去世后即皇帝位，年号泰昌。", "https://www.dpm.org.cn/court/lineage/226242.html"),
        CuratedCelebrityTimelineItem(1620, "在位仅一个月后去世，引发“红丸案”与“移宫案”等宫廷争议。", "https://www.dpm.org.cn/court/lineage/226242.html"),
    ),
    "朱由校" to listOf(
        CuratedCelebrityTimelineItem(1620, "泰昌帝去世后即皇帝位，次年改元天启。", "https://www.dpm.org.cn/court/lineage/226243.html"),
        CuratedCelebrityTimelineItem(1622, "广宁失守，辽东局势恶化。", "https://www.dpm.org.cn/court/lineage/226243.html"),
        CuratedCelebrityTimelineItem(1627, "去世，朱由检继位。", "https://www.dpm.org.cn/court/lineage/226243.html"),
    ),
    "朱翊钧" to listOf(
        CuratedCelebrityTimelineItem(1572, "继承皇位，次年改元万历。", "https://www.dpm.org.cn/court/lineage/226265.html"),
        CuratedCelebrityTimelineItem(1582, "张居正去世后开始亲政，后期政治与财政问题逐步加深。", "https://www.dpm.org.cn/court/lineage/226265.html"),
        CuratedCelebrityTimelineItem(1620, "去世，朱常洛继位。", "https://www.dpm.org.cn/court/lineage/226265.html"),
    ),
    "正德" to listOf(
        CuratedCelebrityTimelineItem(1505, "继承皇位，次年改元正德。", "https://www.dpm.org.cn/court/lineage/226240.html"),
        CuratedCelebrityTimelineItem(1510, "宠信宦官刘瑾等“八虎”，朝廷权力结构受到冲击。", "https://www.dpm.org.cn/court/lineage/226240.html"),
        CuratedCelebrityTimelineItem(1521, "去世，朱厚熜继位。", "https://www.dpm.org.cn/court/lineage/226240.html"),
    ),
    "忽必烈" to listOf(
        CuratedCelebrityTimelineItem(1260, "在部分宗王拥戴下即大汗位。", "https://www.dpm.org.cn/lemmas/242765.html"),
        CuratedCelebrityTimelineItem(1271, "改国号为元，次年定都大都。", "https://en.chnmuseum.cn/Portals/0/web/exhibition/exhibitions/161120Chines-Epic/"),
        CuratedCelebrityTimelineItem(1279, "灭南宋，完成全国统一。", "https://www.dpm.org.cn/lemmas/242765.html"),
    ),
    "元顺帝" to listOf(
        CuratedCelebrityTimelineItem(1333, "即位，开始元末长期统治。", "https://ctext.org/datawiki.pl?if=gb&res=485702"),
        CuratedCelebrityTimelineItem(1346, "颁布《至正条格》，汇编元朝法律条格与案例。", "https://ctext.org/datawiki.pl?if=gb&res=485702"),
        CuratedCelebrityTimelineItem(1368, "离开大都北迁，元朝在中原的统治结束。", "https://ctext.org/datawiki.pl?if=gb&res=485702"),
    ),
    "武则天" to listOf(
        CuratedCelebrityTimelineItem(690, "称帝，建立武周。", "https://www.lvliang.gov.cn/zjll/mlll/lsmr/200702/t20070201_231958.html"),
        CuratedCelebrityTimelineItem(690, "成为中国历史上唯一的正统女皇帝。", "https://www.lishi.gov.cn/hdjl/gqhy/202302/t20230213_1736503.shtml"),
        CuratedCelebrityTimelineItem(705, "退位，唐朝复辟。", "https://www.lvliang.gov.cn/zjll/mlll/lsmr/200702/t20070201_231958.html"),
    ),
    "汉世祖光武帝" to listOf(
        CuratedCelebrityTimelineItem(22, "与刘演等在春陵起兵，参与推翻新朝的斗争。", "https://www.ccdi.gov.cn/2020/202002/t20200229_212518.html"),
        CuratedCelebrityTimelineItem(25, "在河北鄗城即皇帝位，建立东汉。", "https://www.ccdi.gov.cn/2020/202002/t20200229_212518.html"),
        CuratedCelebrityTimelineItem(57, "在位期间形成的休养生息与整顿措施，后世称为“光武中兴”。", "https://www.ccdi.gov.cn/2020/202002/t20200229_212518.html"),
    ),
    "俞大猷" to listOf(
        CuratedCelebrityTimelineItem(1535, "中武进士，授千户，守卫金门。", "https://ctext.org/datawiki.pl?if=gb&res=668190"),
        CuratedCelebrityTimelineItem(1553, "率水师追捣沥港倭寇巢穴，取得镇海一带抗倭战果。", "https://www.dpm.org.cn/lemmas/241761.html"),
        CuratedCelebrityTimelineItem(1563, "与戚继光、刘显等取得平海卫大捷。", "https://www.dpm.org.cn/lemmas/241761.html"),
        CuratedCelebrityTimelineItem(1580, "去世，后获赠左都督，谥武襄。", "https://www.dpm.org.cn/lemmas/241761.html"),
    ),
    "倪元璐" to listOf(
        CuratedCelebrityTimelineItem(1622, "中进士，进入仕途。", "https://www.dpm.org.cn/lemmas/243495.html"),
        CuratedCelebrityTimelineItem(1628, "崇祯初年上疏反对魏忠贤遗党，逐步获得政治声望。", "https://www.dpm.org.cn/lemmas/243495.html"),
        CuratedCelebrityTimelineItem(1644, "李自成军攻陷北京时自缢；其书画与著述流传后世。", "https://www.dpm.org.cn/lemmas/243495.html"),
    ),
    "史可法" to listOf(
        CuratedCelebrityTimelineItem(1628, "中进士，初任西安府推官。", "https://www.zmdsjw.gov.cn/sitesources/zmdjw/page_pc/gzdt/qfwy/articledb2173ac64264c90b40e4185bf785646.html"),
        CuratedCelebrityTimelineItem(1644, "明朝覆亡后参与拥立福王政权，任南京兵部尚书并督师江北。", "https://www.dpm.org.cn/lemmas/243674.html"),
        CuratedCelebrityTimelineItem(1645, "扬州陷落后遇害，遗著后编为《史忠正公集》。", "https://www.dpm.org.cn/lemmas/243674.html"),
    ),
    "卢象升" to listOf(
        CuratedCelebrityTimelineItem(1622, "中进士，进入明代官僚体系。", "https://szzy.7lue.cn/read/fd49474ea4b458f2d0c9cc879fd1d534/04a1d60ddbdc96fd6b7d502631086b92.md"),
        CuratedCelebrityTimelineItem(1636, "清军入关后率师进入京畿，后改总督宣府、大同、山西军务。", "https://szzy.7lue.cn/read/fd49474ea4b458f2d0c9cc879fd1d534/04a1d60ddbdc96fd6b7d502631086b92.md"),
        CuratedCelebrityTimelineItem(1638, "在河北巨鹿抗清作战中战死，留下《忠肃集》《卢象昇疏牍》等。", "https://szzy.7lue.cn/read/fd49474ea4b458f2d0c9cc879fd1d534/04a1d60ddbdc96fd6b7d502631086b92.md"),
    ),
    "严世蕃" to listOf(
        CuratedCelebrityTimelineItem(1542, "严嵩入阁后，依父势在朝廷事务中日益活跃。", "https://www.dpm.org.cn/lemmas/241436.html"),
        CuratedCelebrityTimelineItem(1550, "由尚宝司丞等职逐步升迁至工部侍郎，权势扩张。", "https://www.dpm.org.cn/Uploads/File/pdf/a3/3a/58/a33a5890c94c74be16845fe1324d2d37.pdf"),
        CuratedCelebrityTimelineItem(1562, "严氏父子被劾，严世蕃后被处死，严嵩被革职、抄家。", "https://www.dpm.org.cn/lemmas/241436.html"),
    ),
    "刘瑾" to listOf(
        CuratedCelebrityTimelineItem(1505, "朱厚照即位后受命掌司礼监，进入权力核心。", "https://www.dpm.org.cn/lemmas/241396.html"),
        CuratedCelebrityTimelineItem(1506, "正德初年以“八虎”之一的身份势倾朝野。", "https://www.dpm.org.cn/court/lineage/226240.html"),
        CuratedCelebrityTimelineItem(1510, "被张永弹劾谋逆，遭逮捕、抄家并处死。", "https://www.dpm.org.cn/lemmas/241396.html"),
    ),
    "周延儒" to listOf(
        CuratedCelebrityTimelineItem(1613, "会试、殿试皆第一，授翰林院修撰。", "https://www.dpm.org.cn/court/figure/104039.html"),
        CuratedCelebrityTimelineItem(1629, "以礼部尚书兼东阁大学士身份入阁，参与机务。", "https://www.dpm.org.cn/court/figure/104039.html"),
        CuratedCelebrityTimelineItem(1641, "被重新起用为首辅。", "https://www.dpm.org.cn/court/figure/104039.html"),
        CuratedCelebrityTimelineItem(1643, "因督军失职、谎报战功等被追究，奉命自尽。", "https://www.dpm.org.cn/lemmas/245016.html"),
    ),
    "关羽" to listOf(
        CuratedCelebrityTimelineItem(219, "北伐围攻樊城，汉水暴涨后俘于禁等人，声势大振。", "https://ctext.org/wiki.pl?chapter=612358&if=en"),
        CuratedCelebrityTimelineItem(219, "吕蒙乘其北伐之机袭取荆州，关羽退军后兵势瓦解。", "https://ctext.org/text.pl?if=en&node=378489"),
        CuratedCelebrityTimelineItem(220, "在荆州战局中遭俘杀；后世经多朝褒封，关帝信仰广泛流传。", "https://www.dpm.org.cn/collection/sculpture/234222.html"),
    ),
    "冯保" to listOf(
        CuratedCelebrityTimelineItem(1572, "万历帝即位后任司礼监掌印太监，并继续主管东厂，进入辅政核心。", "https://heritage.bnf.fr/france-chine/zh-hans/node/1694"),
        CuratedCelebrityTimelineItem(1573, "与张居正共同参与万历帝早期教育和内廷事务安排，构成万历初年的辅政结构之一。", "https://heritage.bnf.fr/france-chine/zh-hans/node/1694"),
        CuratedCelebrityTimelineItem(1582, "张居正去世后遭御史弹劾十二项罪状，权势随之逆转。", "https://ctext.org/datawiki.pl?if=en&res=256278"),
    ),
    "吴中行" to listOf(
        CuratedCelebrityTimelineItem(1571, "中进士，后改翰林院庶吉士。", "https://ctext.org/datawiki.pl?if=gb&res=982606"),
        CuratedCelebrityTimelineItem(1577, "上疏反对张居正居丧夺情，受廷杖后被谪并罢职。", "https://ctext.org/datawiki.pl?if=gb&res=982606"),
        CuratedCelebrityTimelineItem(1583, "张居正去世后获荐起复，历任右中允、经筵讲官等职。", "https://ctext.org/datawiki.pl?if=gb&res=982606"),
        CuratedCelebrityTimelineItem(1594, "回籍听勘后去世，次年获赠礼部右侍郎。", "https://ctext.org/datawiki.pl?if=gb&res=982606"),
    ),
    "范蠡" to listOf(
        CuratedCelebrityTimelineItem(-494, "越国战败后随勾践入吴，参与其后复国准备。", "https://www.dpm.org.cn/lemmas/240354.html"),
        CuratedCelebrityTimelineItem(-473, "辅佐勾践灭吴、成就越国霸业后离开政坛。", "https://www.dpm.org.cn/lemmas/240354.html"),
        CuratedCelebrityTimelineItem(-473, "经商致富，后世尊称“陶朱公”，其经营形象长期进入商业文化传统。", "https://www.dpm.org.cn/lemmas/240354.html"),
    ),
    "王永庆" to listOf(
        CuratedCelebrityTimelineItem(1954, "申请美援贷款生产 PVC 粉，创办福懋公司（后更名台塑公司）。", "https://www.fpg.taipei/cn/about/establisher/life-story"),
        CuratedCelebrityTimelineItem(1991, "推动“六轻计划”落脚云林麦寮，成为台塑企业发展的关键转折。", "https://www.fpg.taipei/cn/about/establisher/life-story"),
        CuratedCelebrityTimelineItem(2008, "逝世；其企业经营、教育与医疗公益投入持续产生影响。", "https://www.fpg.taipei/cn/about/establisher/life-story"),
    ),
    "包玉刚" to listOf(
        CuratedCelebrityTimelineItem(1955, "创立环球轮船有限公司集团，开始建设独立商船队。", "https://foundation.sjtu.edu.cn/story/view/900"),
        CuratedCelebrityTimelineItem(1965, "进入石油运输市场，与多家国际油商合作，扩展环球航运的国际业务。", "https://foundation.sjtu.edu.cn/story/view/900"),
        CuratedCelebrityTimelineItem(1980, "参与创立国际联合船舶投资有限公司，成为中国改革开放后早期中外合资航运项目之一。", "https://foundation.sjtu.edu.cn/story/view/900"),
    ),
    "商辂" to listOf(
        CuratedCelebrityTimelineItem(1445, "会试、殿试皆第一，授翰林院修撰，成为明代唯一“三试第一”者。", "https://www.dpm.org.cn/court/figure/104038.html"),
        CuratedCelebrityTimelineItem(1449, "土木之变后反对南迁，主张抵抗瓦剌并配合北京防务。", "https://www.dpm.org.cn/court/figure/104038.html"),
        CuratedCelebrityTimelineItem(1467, "再次被起用入阁，次年任兵部尚书。", "https://www.dpm.org.cn/court/figure/104038.html"),
        CuratedCelebrityTimelineItem(1477, "任内阁首辅，与西厂及汪直专权展开政治斗争，后辞官归田。", "https://www.dpm.org.cn/court/figure/104038.html"),
    ),
    "孙承宗" to listOf(
        CuratedCelebrityTimelineItem(1622, "受命督师，经略山海关及蓟、辽、天津、登莱军务。", "https://www.dpm.org.cn/lemmas/241477.html"),
        CuratedCelebrityTimelineItem(1622, "整顿辽东防务，练兵屯田、修筑城堡并重整军器。", "https://www.dpm.org.cn/lemmas/241477.html"),
        CuratedCelebrityTimelineItem(1629, "后金军入大安口后奉命守通州，继而移镇山海关并收复部分失地。", "https://www.dpm.org.cn/lemmas/241477.html"),
        CuratedCelebrityTimelineItem(1638, "清军破城时率家人拒战后自尽。", "https://www.dpm.org.cn/lemmas/241477.html"),
    ),
    "张之洞" to listOf(
        CuratedCelebrityTimelineItem(1863, "中进士入翰林院，开始仕途。", "https://www.dpm.org.cn/Uploads/file/2025/02/13/1739432384bVnAzDNUi260.pdf"),
        CuratedCelebrityTimelineItem(1887, "奏请在广东设局铸造银元，开启中国自主制造机制银币的尝试。", "https://www.dpm.org.cn/Uploads/file/2025/02/13/1739432384bVnAzDNUi260.pdf"),
        CuratedCelebrityTimelineItem(1890, "在湖北筹建汉阳铁厂，并带动大冶铁矿、湖北枪炮厂等近代工业设施的发展。", "https://www.hanyang.gov.cn/mlhy/lsyg/202101/t20210120_1603668.html"),
        CuratedCelebrityTimelineItem(1909, "去世；其工业与教育实践成为晚清自强运动的重要遗产。", "https://www.dpm.org.cn/lemmas/240573.html"),
    ),
    "岳钟琪" to listOf(
        CuratedCelebrityTimelineItem(1729, "受命与傅尔丹分兵征讨噶尔丹势力，参与清廷西北军事行动。", "https://www.dpm.org.cn/court/lineage/226259.html?hl=%E9%9B%8D%E6%AD%A3+%E8%A2%8D"),
        CuratedCelebrityTimelineItem(1729, "曾静遣张熙投书策反，岳钟琪拘捕张熙并上报朝廷，案件引出“曾静案”。", "https://www.dpm.org.cn/court/event/161109.html"),
        CuratedCelebrityTimelineItem(1748, "以六十二岁高龄重新被起用，协助平定大金川。", "https://img.dpm.org.cn/Uploads/File/2020/11/13/u5fae09322659e.pdf"),
        CuratedCelebrityTimelineItem(1754, "去世；作为历经康雍乾三朝的将领，其军事生涯跨越多项边疆事务。", "https://img.dpm.org.cn/Uploads/File/2020/11/13/u5fae09322659e.pdf"),
    ),
    "张爱玲" to listOf(
        CuratedCelebrityTimelineItem(1939, "进入香港大学文学学院就读，并以《我的天才梦》获《西风》杂志征文奖。", "https://lib.hku.hk/files/general/research/guides/Zhang_Ailing_brochure.pdf"),
        CuratedCelebrityTimelineItem(1944, "出版小说集《传奇》，其中收录《倾城之恋》等重要作品。", "https://lib.hku.hk/files/general/research/guides/Zhang_Ailing_brochure.pdf"),
        CuratedCelebrityTimelineItem(1952, "由上海移居香港，从事翻译工作。", "https://lib.hku.hk/files/general/research/guides/Zhang_Ailing_brochure.pdf"),
        CuratedCelebrityTimelineItem(1955, "离开香港赴美，此后继续写作及参与电影剧本创作。", "https://lib.hku.hk/files/general/research/guides/Zhang_Ailing_brochure.pdf"),
        CuratedCelebrityTimelineItem(1995, "在洛杉矶去世，作品持续被研究、再版和改编。", "https://lib.hku.hk/files/general/research/guides/Zhang_Ailing_brochure.pdf"),
    ),
    "徐志摩" to listOf(
        CuratedCelebrityTimelineItem(1921, "入剑桥大学当特别生，开始新诗创作。", "https://news.pku.edu.cn/bdrw/137-110603.htm"),
        CuratedCelebrityTimelineItem(1923, "参与成立新月社，并加入文学研究会。", "https://news.pku.edu.cn/bdrw/137-110603.htm"),
        CuratedCelebrityTimelineItem(1925, "出版首部诗集《志摩的诗》。", "https://news.pku.edu.cn/bdrw/137-110603.htm"),
        CuratedCelebrityTimelineItem(1931, "搭乘飞机北上途中失事遇难，文艺界反响强烈。", "https://news.pku.edu.cn/bdrw/137-110603.htm"),
    ),
    "朱自清" to listOf(
        CuratedCelebrityTimelineItem(1916, "考入北京大学哲学系，开始接触新文艺思潮。", "https://big5.cctv.com/gate/big5/www.cctv.cn/program/dssgsw/20031021/101528.shtml"),
        CuratedCelebrityTimelineItem(1925, "创作散文《背影》，成为现代散文经典。", "https://big5.cctv.com/gate/big5/www.cctv.cn/program/dssgsw/20031021/101528.shtml"),
        CuratedCelebrityTimelineItem(1948, "因贫病交加去世；其新诗、散文与文艺评论留下广泛影响。", "https://www.xsg.pku.edu.cn/detail/471.html"),
    ),
    "沈从文" to listOf(
        CuratedCelebrityTimelineItem(1921, "开始接触并搜集文物、民间手工艺品，后持续开展相关研究。", "https://www.chnmuseum.cn/yj/zjxz/ygzgjzj/201005/t20100524_4800.shtml"),
        CuratedCelebrityTimelineItem(1947, "完成《读春游图有感》，参与对传世绘画真伪的考证。", "https://www.chnmuseum.cn/yj/zjxz/ygzgjzj/201005/t20100524_4800.shtml"),
        CuratedCelebrityTimelineItem(1978, "调入中国科学院历史研究所，继续中国古代服饰等专题研究。", "https://www.chnmuseum.cn/yj/zjxz/ygzgjzj/201005/t20100524_4800.shtml"),
        CuratedCelebrityTimelineItem(1981, "《中国古代服饰研究》出版，成为其文物与服饰研究的重要成果。", "https://www.chnmuseum.cn/yj/zjxz/ygzgjzj/201005/t20100524_4800.shtml"),
    ),
    "王安石" to listOf(
        CuratedCelebrityTimelineItem(1069, "任参知政事，系统提出改革主张。", "https://www.dpm.org.cn/lemmas/243269.html"),
        CuratedCelebrityTimelineItem(1070, "拜相后推行青苗、均输、市易等新法，后世称“王安石变法”。", "https://www.dpm.org.cn/lemmas/243269.html"),
        CuratedCelebrityTimelineItem(1074, "首次罢相，改革受到保守派强烈阻碍。", "https://www.dpm.org.cn/lemmas/243269.html"),
        CuratedCelebrityTimelineItem(1076, "再次罢相，退居江宁。", "https://www.dpm.org.cn/lemmas/243269.html"),
        CuratedCelebrityTimelineItem(1080, "封荆国公，世称“荆公”。", "https://www.dpm.org.cn/lemmas/243269.html"),
    ),
    "朱熹" to listOf(
        CuratedCelebrityTimelineItem(1162, "应诏上封事，提出讲学、修政和恢复等主张。", "https://www.fjyx.gov.cn/zjyx/yxgk/zzgl/201204/t20120409_976432.htm"),
        CuratedCelebrityTimelineItem(1167, "在岳麓书院与张栻会讲，成为中国学术史和教育史的重要事件。", "https://www.fjyx.gov.cn/zjyx/yxgk/zzgl/201204/t20120409_976432.htm"),
        CuratedCelebrityTimelineItem(1171, "在五夫创建社仓，作为赈济灾民的制度实践。", "https://m.ccdi.gov.cn/content/2c/15/8438.html"),
        CuratedCelebrityTimelineItem(1175, "参与鹅湖之会，与陆九渊等论辩学术思想。", "https://www.fjyx.gov.cn/zjyx/yxgk/zzgl/201204/t20120409_976432.htm"),
        CuratedCelebrityTimelineItem(1200, "在庆元党禁中去世；其著述与讲学持续影响后世儒学。", "https://www.sm.gov.cn/sq/sqgk/smfc/lswh/200510/t20051019_191335.htm"),
    ),
    "安妮宝贝" to listOf(
        CuratedCelebrityTimelineItem(1998, "开始以“安妮宝贝”为笔名在网络写作并发表作品。", "https://www.zhuandsongpress.com/%E5%8D%8E%E4%BA%BA%E4%BD%9C%E5%AE%B6%E5%8D%8F%E4%BC%9A/%E5%AE%89%E5%A6%AE%E5%AE%9D%E8%B4%9D%EF%BC%88%E5%BA%86%E5%B1%B1%EF%BC%89/"),
        CuratedCelebrityTimelineItem(2000, "出版首部小说集《告别薇安》，由网络写作进入纸质出版阶段。", "https://www.zhuandsongpress.com/%E5%8D%8E%E4%BA%BA%E4%BD%9C%E5%AE%B6%E5%8D%8F%E4%BC%9A/%E5%AE%89%E5%A6%AE%E5%AE%9D%E8%B4%9D%EF%BC%88%E5%BA%86%E5%B1%B1%EF%BC%89/"),
        CuratedCelebrityTimelineItem(2006, "出版长篇小说《莲花》，创作风格继续变化。", "https://www.zhuandsongpress.com/%E5%8D%8E%E4%BA%BA%E4%BD%9C%E5%AE%B6%E5%8D%8F%E4%BC%9A/%E5%AE%89%E5%A6%AE%E5%AE%9D%E8%B4%9D%EF%BC%88%E5%BA%86%E5%B1%B1%EF%BC%89/"),
        CuratedCelebrityTimelineItem(2014, "宣布改用笔名“庆山”，出版《得未曾有》。", "https://www.zhuandsongpress.com/%E5%8D%8E%E4%BA%BA%E4%BD%9C%E5%AE%B6%E5%8D%8F%E4%BC%9A/%E5%AE%89%E5%A6%AE%E5%AE%9D%E8%B4%9D%EF%BC%88%E5%BA%86%E5%B1%B1%EF%BC%89/"),
        CuratedCelebrityTimelineItem(2019, "出版长篇小说《夏摩山谷》，延续后期写作。", "https://www.zhuandsongpress.com/%E5%8D%8E%E4%BA%BA%E4%BD%9C%E5%AE%B6%E5%8D%8F%E4%BC%9A/%E5%AE%89%E5%A6%AE%E5%AE%9D%E8%B4%9D%EF%BC%88%E5%BA%86%E5%B1%B1%EF%BC%89/"),
    ),
    "王世贞" to listOf(
        CuratedCelebrityTimelineItem(1547, "中进士，进入仕途。", "https://www.dpm.org.cn/lemmas/239715.html"),
        CuratedCelebrityTimelineItem(1567, "为父王忬辩冤，获大臣相助后王忬得以平反。", "https://ctext.org/datawiki.pl?if=gb&res=848807"),
        CuratedCelebrityTimelineItem(1589, "升任南京刑部尚书。", "https://ctext.org/datawiki.pl?if=gb&res=848807"),
        CuratedCelebrityTimelineItem(1590, "去世，留下《弇山堂别集》《弇州山人四部稿》等大量著述。", "https://www.dpm.org.cn/lemmas/239715.html"),
    ),
    "海子" to listOf(
        CuratedCelebrityTimelineItem(1982, "在北京大学求学期间开始诗歌创作。", "https://m.people.cn/%2Fn4%2F2019%2F0326%2Fc679-12499405.html"),
        CuratedCelebrityTimelineItem(1984, "创作《亚洲铜》，成为其早期重要作品。", "https://m.people.cn/%2Fn4%2F2019%2F0326%2Fc679-12499405.html"),
        CuratedCelebrityTimelineItem(1989, "写下《面朝大海，春暖花开》；同年3月去世。", "https://m.people.cn/%2Fn4%2F2019%2F0326%2Fc679-12499405.html"),
        CuratedCelebrityTimelineItem(1995, "诗集《海子的诗》出版，作品在读者与诗歌研究中持续传播。", "https://zqb.cyol.com/content/2009-03/31/content_2603151.htm"),
    ),
    "张瑞图" to listOf(
        CuratedCelebrityTimelineItem(1607, "殿试获探花，授翰林院编修，开始仕途。", "https://www.dpm.org.cn/lemmas/243521.html"),
        CuratedCelebrityTimelineItem(1626, "受魏忠贤支持入阁，官至礼部尚书兼东阁大学士。", "https://www.metmuseum.org/art/collection/search/48968"),
        CuratedCelebrityTimelineItem(1627, "为魏忠贤撰生祠碑而受士林非议。", "https://www.dpm.org.cn/lemmas/243521.html"),
        CuratedCelebrityTimelineItem(1628, "魏忠贤伏诛后获准致仕，回乡隐居。", "https://web.arte.gov.tw/calligraphy/01wh/whhtm/wh-19.htm"),
    ),
    "朱用纯" to listOf(
        CuratedCelebrityTimelineItem(1645, "父亲朱集璜守昆山抗清，城破后投河自尽，对其人生经历产生深刻影响。", "https://ctext.org/datawiki.pl?if=gb&res=297383"),
        CuratedCelebrityTimelineItem(1698, "去世；《治家格言》《愧讷集》《大学中庸讲义》等著作流传后世。", "https://ctext.org/datawiki.pl?if=gb&res=297383"),
    ),
    "刘德华" to listOf(
        CuratedCelebrityTimelineItem(1990, "主演电影《天若有情》，片中表演与银幕形象成为香港流行文化的经典记忆。", "https://www.filmarchive.gov.hk/sc/web/hkfa/pe-event-2023-mdatm-fs-film02.html"),
    ),
    "周星驰" to listOf(
        CuratedCelebrityTimelineItem(1994, "主演并执导《国产凌凌漆》，开始在主演之外兼任导演与编剧工作。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-02-Filmmakers-Search/Chinese/Stephen-CHOW_c.pdf"),
        CuratedCelebrityTimelineItem(2001, "自导自演《少林足球》，打破香港票房纪录并获香港电影金像奖多项奖项。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-02-Filmmakers-Search/Chinese/Stephen-CHOW_c.pdf"),
        CuratedCelebrityTimelineItem(2004, "导演并主演《功夫》，刷新其此前作品的香港票房纪录并扩大海外传播。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-02-Filmmakers-Search/Chinese/Stephen-CHOW_c.pdf"),
    ),
    "周杰伦" to listOf(
        CuratedCelebrityTimelineItem(2000, "发行首张专辑《Jay》，开启个人录音室专辑与创作歌手生涯。", "https://www.universal-music.co.jp/jay-chou/biography/"),
        CuratedCelebrityTimelineItem(2003, "发行《叶惠美》，以《以父之名》《东风破》等作品强化个人创作辨识度。", "https://music.apple.com/cn/album/%E8%91%89%E6%83%A0%E7%BE%8E/535824731"),
        CuratedCelebrityTimelineItem(2005, "主演电影《头文字 D》，拓展影视表演领域。", "https://www.universal-music.co.jp/jay-chou/biography/"),
    ),
    "姚明" to listOf(
        CuratedCelebrityTimelineItem(1998, "入选中国国家男篮，开始国家队生涯。", "https://www.iq.com/actor-info/%E5%A7%9A%E6%98%8E-yao-ming-215295505?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2002, "带领上海大鲨鱼获得 CBA 冠军，并以首位外籍状元身份进入 NBA。", "https://m.news.cctv.com/2016/09/10/ARTIhecFH1ttksqOyg57uQl6160910.shtml"),
        CuratedCelebrityTimelineItem(2003, "入选 NBA 全明星赛西部先发阵容，开启连续多年的全明星经历。", "https://www.iq.com/actor-info/%E5%A7%9A%E6%98%8E-yao-ming-215295505?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2011, "在上海宣布退役，结束职业球员生涯。", "https://www.iq.com/actor-info/%E5%A7%9A%E6%98%8E-yao-ming-215295505?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2016, "正式入选奈史密斯篮球名人堂，成为首位入选该名人堂的中国球员。", "https://m.news.cctv.com/2016/09/10/ARTIhecFH1ttksqOyg57uQl6160910.shtml"),
    ),
    "林丹" to listOf(
        CuratedCelebrityTimelineItem(2006, "获得世界羽毛球锦标赛男单冠军。", "https://www.sport.gov.cn/n4/n166/n178/c324399/content.html"),
        CuratedCelebrityTimelineItem(2008, "获得北京奥运会羽毛球男单冠军。", "https://www.sport.gov.cn/n4/n10706/c717750/content.html"),
        CuratedCelebrityTimelineItem(2012, "获得伦敦奥运会羽毛球男单冠军，实现卫冕。", "https://www.sport.gov.cn/n4/n166/n178/c324399/content.html"),
        CuratedCelebrityTimelineItem(2014, "获得仁川亚运会冠军，完成两次全满贯壮举。", "https://www.sport.gov.cn/n20001280/n20745751/n20767274/c22156460/content.html"),
    ),
    "杨振宁" to listOf(
        CuratedCelebrityTimelineItem(1942, "从西南联合大学物理系毕业，获理学学士学位。", "https://www.tsinghua.edu.cn/info/1173/121842.htm"),
        CuratedCelebrityTimelineItem(1948, "在芝加哥大学获博士学位。", "https://www.ias.tsinghua.edu.cn/info/1016/1213.htm"),
        CuratedCelebrityTimelineItem(1954, "与米尔斯提出非阿贝尔规范场理论，后称杨—米尔斯规范场论。", "https://www.tsinghua.edu.cn/info/1173/121842.htm"),
        CuratedCelebrityTimelineItem(1957, "与李政道因宇称不守恒研究共同获得诺贝尔物理学奖。", "https://www.tsinghua.edu.cn/info/1173/121842.htm"),
        CuratedCelebrityTimelineItem(1999, "受聘清华大学教授并出任高等研究中心终身名誉主任。", "https://xsg.tsinghua.edu.cn/info/1004/3407.htm"),
        CuratedCelebrityTimelineItem(2003, "归国定居清华园，继续参与高等研究院建设与学术工作。", "https://xsg.tsinghua.edu.cn/info/1004/3407.htm"),
    ),
    "郭晶晶" to listOf(
        CuratedCelebrityTimelineItem(2000, "悉尼奥运会获女子三米板单人、双人两枚银牌。", "https://www.olympedia.org/athletes/46077%26lang%3Den"),
        CuratedCelebrityTimelineItem(2004, "雅典奥运会在女子三米板单人、双人项目夺得两枚金牌。", "https://www.olympedia.org/athletes/46077%26lang%3Den"),
        CuratedCelebrityTimelineItem(2008, "北京奥运会女子三米板单人项目夺冠，个人奥运战绩达到四金二银。", "https://www.sport.gov.cn/n20001280/n20767351/n20767722/c20836569/content.html"),
    ),
    "苏炳添" to listOf(
        CuratedCelebrityTimelineItem(2015, "在国际田联钻石联赛尤金站跑出 9.99，成为首位在正式比赛跑进十秒的亚洲本土选手。", "https://tyb.jnu.edu.cn/2019/0628/c40095a502297/page.htm"),
        CuratedCelebrityTimelineItem(2018, "百米跑出 9.91，刷新亚洲纪录并创造个人当时最好成绩。", "https://worldathletics.org/athletes/pr-of-china/bingtian-su-14171509"),
        CuratedCelebrityTimelineItem(2021, "东京奥运会百米半决赛跑出 9.83 创亚洲纪录，成为中国首位闯入奥运百米决赛的选手。", "https://www.gd.gov.cn/zjgd/ydss/tydt/content/post_3449575.html"),
    ),
    "巩俐" to listOf(
        CuratedCelebrityTimelineItem(1985, "考入中央戏剧学院表演系，接受系统表演训练。", "https://www.1905.com/mdb/star/340/details/"),
        CuratedCelebrityTimelineItem(1988, "主演《红高粱》上映，开始受到国内外电影界关注。", "https://www.iq.com/actor-info/%E5%B7%A9%E4%BF%90-gong-li-214940305?lang=zh_cn"),
        CuratedCelebrityTimelineItem(1992, "凭《秋菊打官司》获第 49 届威尼斯国际电影节最佳女演员奖。", "https://idm.cctv.com/movie/special/C12433/20040615/102741.shtml"),
        CuratedCelebrityTimelineItem(1997, "担任戛纳国际电影节评审团成员，参与国际电影节公共事务。", "https://www.iq.com/actor-info/%E5%B7%A9%E4%BF%90-gong-li-214940305?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2021, "担任第 11 届北京国际电影节天坛奖评审团主席。", "https://www.iq.com/actor-info/%E5%B7%A9%E4%BF%90-gong-li-214940305?lang=zh_cn"),
    ),
    "陈奕迅" to listOf(
        CuratedCelebrityTimelineItem(1995, "获第 14 届新秀歌唱大赛冠军并正式进入乐坛。", "https://www.easonchan.net/hk/about-e/"),
        CuratedCelebrityTimelineItem(2003, "发行《黑白灰》，其中《十年》扩大其在内地的公众影响。", "https://www.easonchan.cn/"),
        CuratedCelebrityTimelineItem(2005, "发行《U87》，获香港年度高销量粤语专辑及多项音乐奖项。", "https://www.easonchan.net/hk/about-e/"),
        CuratedCelebrityTimelineItem(2018, "凭《C'mon in~》第三次获得台湾金曲奖最佳国语男歌手奖。", "https://www.easonchan.net/hk/about-e/"),
        CuratedCelebrityTimelineItem(2021, "发行《孤勇者》，成为跨年龄层广泛传播的流行单曲。", "https://www.easonchan.cn/"),
    ),
    "周鸿祎" to listOf(
        CuratedCelebrityTimelineItem(1998, "创办因特国风软件有限公司，开始互联网创业。", "https://www.360.cn/about/founder.html"),
        CuratedCelebrityTimelineItem(2006, "创立 360 公司并推出免费安全战略。", "https://www.360.cn/about/founder.html"),
        CuratedCelebrityTimelineItem(2011, "率领 360 在纽约证券交易所上市。", "https://www.360.cn/about/founder.html"),
        CuratedCelebrityTimelineItem(2018, "带领公司从美股私有化后回归 A 股，转为内资网络安全企业。", "https://www.360.cn/about/founder.html"),
    ),
    "钱学森" to listOf(
        CuratedCelebrityTimelineItem(1934, "从上海交通大学机械工程系毕业。", "https://www.cas.cn/zt/rwzt/qxsssyzn/qxsjj/"),
        CuratedCelebrityTimelineItem(1935, "赴美国研究航空工程和空气动力学。", "https://www.cas.cn/zt/rwzt/qxsssyzn/qxsjj/"),
        CuratedCelebrityTimelineItem(1938, "获加利福尼亚理工学院博士学位。", "https://www.cas.cn/zt/rwzt/qxsssyzn/qxsjj/"),
        CuratedCelebrityTimelineItem(1955, "历经五年后回到祖国，投入新中国科技建设。", "https://www.cas.cn/zt/rwzt/qxsssyzn/qxsjj/"),
        CuratedCelebrityTimelineItem(1958, "起长期担任火箭、导弹与航天器研制的技术领导职务。", "https://www.cas.cn/zt/rwzt/qxsssyzn/qxsjj/"),
    ),
    "邓稼先" to listOf(
        CuratedCelebrityTimelineItem(1948, "赴美国普渡大学物理系留学。", "https://yszsjs.cas.cn/ldyx/201409/t20140919_4209992.html"),
        CuratedCelebrityTimelineItem(1950, "获物理学博士学位并回国，进入中国科学院工作。", "https://yszsjs.cas.cn/ldyx/201409/t20140919_4209992.html"),
        CuratedCelebrityTimelineItem(1962, "领导完成原子弹理论设计方案，解决试验成功的关键理论难题。", "https://www.cas.cn/xzfc/202109/t20210929_4807592.shtml"),
        CuratedCelebrityTimelineItem(1986, "去世；其核物理与国防科技贡献持续被纪念。", "https://yszsjs.cas.cn/ldyx/201409/t20140919_4209992.html"),
    ),
    "华罗庚" to listOf(
        CuratedCelebrityTimelineItem(1929, "在《科学》杂志发表首篇论文《Sturm氏定理之研究》。", "https://xsw.ynu.edu.cn/info/1010/1352.htm"),
        CuratedCelebrityTimelineItem(1930, "继续在《科学》发表关于五次方程式解法的论文，受到数学界关注。", "https://xsw.ynu.edu.cn/info/1010/1352.htm"),
        CuratedCelebrityTimelineItem(1950, "回到祖国，参与新中国数学研究与学科建设。", "https://amss.cas.cn/ryszl/hlg/202106/t20210607_6080271.html"),
        CuratedCelebrityTimelineItem(1985, "在日本作学术报告期间去世；其研究与数学普及事业持续影响后世。", "https://www.cas.cn/gdlm/2019lm/xw/zjsd/201009/t20100920_2966509.shtml"),
    ),
    "林巧稚" to listOf(
        CuratedCelebrityTimelineItem(1921, "赴北京协和医学院学习医学。", "https://ims.pumch.cn/detail/27608.html"),
        CuratedCelebrityTimelineItem(1929, "从北京协和医学院毕业并留院，开始妇产科临床工作。", "https://ims.pumch.cn/detail/27608.html"),
        CuratedCelebrityTimelineItem(1940, "成为北京协和医院妇产科首位中国籍女主任。", "https://ims.pumch.cn/detail/27608.html"),
        CuratedCelebrityTimelineItem(1955, "被推选为中国科学院首批学部委员，也是其中唯一的女性。", "https://ims.pumch.cn/detail/27608.html"),
        CuratedCelebrityTimelineItem(1973, "受聘担任世界卫生组织医学研究顾问委员会顾问。", "https://ims.pumch.cn/detail/27608.html"),
        CuratedCelebrityTimelineItem(1983, "在北京协和医院病逝；其妇幼医学与医学教育贡献持续被纪念。", "https://www.cams.ac.cn/rcjs/ysfc/zgkxyys/bfd090c71dad4f24afd778287d73e679.htm"),
    ),
    "李政道" to listOf(
        CuratedCelebrityTimelineItem(1946, "赴美国留学，进入芝加哥大学继续物理学研究。", "https://www.xsg.pku.edu.cn/heros/prize/detail/1195.html"),
        CuratedCelebrityTimelineItem(1950, "获芝加哥大学博士学位。", "https://www.xsg.pku.edu.cn/heros/prize/detail/1195.html"),
        CuratedCelebrityTimelineItem(1957, "与杨振宁因弱相互作用宇称不守恒理论共同获诺贝尔物理学奖。", "https://www.xsg.pku.edu.cn/heros/prize/detail/1195.html"),
        CuratedCelebrityTimelineItem(1979, "推动中美高能物理领域正式合作，并参与联合委员会首次会议。", "https://tdllib.sjtu.edu.cn/lzd/rwjs/dsj.htm"),
        CuratedCelebrityTimelineItem(1980, "发起中美联合招考物理研究生计划（CUSPEA），推动青年人才培养。", "https://tdllib.sjtu.edu.cn/lzd/rwjs/dsj.htm"),
        CuratedCelebrityTimelineItem(1986, "参与建立北京现代物理研究中心及中国高等科学技术中心等研究平台。", "https://tdllib.sjtu.edu.cn/lzd/rwjs/dsj.htm"),
        CuratedCelebrityTimelineItem(2024, "在美国旧金山逝世；其科学研究及促进中国科技教育的工作持续被学界纪念。", "https://www.ihep.cas.cn/zt/dnlzd/"),
    ),
    "钟南山" to listOf(
        CuratedCelebrityTimelineItem(1955, "考入北京医学院医疗系，开始系统医学学习。", "https://news.pku.edu.cn/ddhzt/dyfc/0702152e04494560849bfb9372fa025f.htm"),
        CuratedCelebrityTimelineItem(1960, "从北京医学院毕业，开始医学教学与临床工作。", "https://www.gdfao.gov.cn/jjxy/qlkm/content/post_313369.html"),
        CuratedCelebrityTimelineItem(1996, "当选中国工程院院士。", "https://www.cae.cn/cae/html/main/colys/71145511.html"),
        CuratedCelebrityTimelineItem(2003, "参与 SARS 防治一线工作，推动呼吸道传染病临床救治与防控研究。", "https://www.gzlab.ac.cn/Team/index_109.html"),
        CuratedCelebrityTimelineItem(2020, "在新冠疫情防控中参与高级别专家组、重症救治与科研攻关等工作。", "https://www.gzlab.ac.cn/Team/index_109.html"),
    ),
    "吴孟超" to listOf(
        CuratedCelebrityTimelineItem(1949, "从同济大学医学院毕业，进入临床医学工作。", "https://www.most.gov.cn/ztzl/kjrw/200709/t20070911_54255.html"),
        CuratedCelebrityTimelineItem(1958, "开始系统研究肝脏解剖，为肝胆外科理论与手术实践奠定基础。", "https://www.most.gov.cn/ztzl/kjrw/200709/t20070911_54255.html"),
        CuratedCelebrityTimelineItem(1978, "创建国内首个肝胆外科科室并任主任，推动专科建设。", "https://www.mmcs.org.cn/kxjfc/kxjfc/wmc/nb/art/2023/art_da2efc5d14ed46779c9164f69a81cfbf.html"),
        CuratedCelebrityTimelineItem(1991, "当选中国科学院院士。", "https://www.most.gov.cn/ztzl/kjrw/200709/t20070911_54255.html"),
        CuratedCelebrityTimelineItem(2005, "获国家最高科学技术奖。", "https://www.most.gov.cn/ztzl/kjrw/200709/t20070911_54255.html"),
        CuratedCelebrityTimelineItem(2021, "在上海逝世；其肝胆外科学术与临床实践持续被医学界纪念。", "https://www.cnr.cn/shanghai/tt/20220831/t20220831_525993885.shtml"),
    ),
    "李四光" to listOf(
        CuratedCelebrityTimelineItem(1910, "从日本大阪高等工业学校毕业。", "https://www.nigpas.cas.cn/sq70/na/202106/t20210601_6058706.html"),
        CuratedCelebrityTimelineItem(1919, "获英国伯明翰大学自然科学硕士学位。", "https://casad.cas.cn/ysxx2022/ygys/200906/t20090624_1809986.html"),
        CuratedCelebrityTimelineItem(1931, "获英国伯明翰大学自然科学博士学位。", "https://www.nigpas.cas.cn/sq70/na/202106/t20210601_6058706.html"),
        CuratedCelebrityTimelineItem(1948, "当选中央研究院院士。", "https://casad.cas.cn/ysxx2022/ygys/200906/t20090624_1809986.html"),
        CuratedCelebrityTimelineItem(1955, "被选聘为中国科学院学部委员，参与新中国地质与资源调查事业。", "https://casad.cas.cn/ysxx2022/ygys/200906/t20090624_1809986.html"),
        CuratedCelebrityTimelineItem(1971, "在北京逝世；地质力学与地质教育影响持续被学界整理。", "https://www.nigpas.cas.cn/sq70/na/202106/t20210601_6058706.html"),
    ),
    "钱三强" to listOf(
        CuratedCelebrityTimelineItem(1936, "从清华大学本科毕业。", "https://www.cas.cn/xzfc/202206/t20220630_4840013.shtml"),
        CuratedCelebrityTimelineItem(1937, "赴巴黎大学镭学研究所居里实验室攻读博士。", "https://www.cas.cn/xzfc/202206/t20220630_4840013.shtml"),
        CuratedCelebrityTimelineItem(1940, "获法国国家博士学位。", "https://www.cas.cn/xzfc/202206/t20220630_4840013.shtml"),
        CuratedCelebrityTimelineItem(1948, "回国任清华大学物理系教授。", "https://www.cas.cn/xzfc/202206/t20220630_4840013.shtml"),
        CuratedCelebrityTimelineItem(1951, "接任中国科学院近代物理研究所所长，参与原子能科学事业组织建设。", "https://www.cas.cn/xzfc/202206/t20220630_4840013.shtml"),
        CuratedCelebrityTimelineItem(1960, "在原子能所组织氢弹理论与实验预研工作。", "https://yszsjs.cas.cn/ldyx/201409/t20140919_4209990.html"),
        CuratedCelebrityTimelineItem(1992, "在北京逝世；其核物理研究与原子能事业组织贡献持续被纪念。", "https://www.cas.cn/xzfc/202206/t20220630_4840013.shtml"),
    ),
    "王选" to listOf(
        CuratedCelebrityTimelineItem(1954, "考入北京大学数学力学系。", "https://news.pku.edu.cn/xwzh/129-105612.htm"),
        CuratedCelebrityTimelineItem(1958, "从北京大学毕业并留校，从事计算机领域教学与研究。", "https://wangxuan.pku.edu.cn/yyzx/index.htm"),
        CuratedCelebrityTimelineItem(1980, "首本由国产激光照排系统排出的样书问世，北大方正由此起步。", "https://news.pku.edu.cn/xwzh/129-73775.htm"),
        CuratedCelebrityTimelineItem(1991, "当选中国科学院学部委员；其团队持续推动中文出版技术革新。", "https://news.pku.edu.cn/xwzh/129-105612.htm"),
        CuratedCelebrityTimelineItem(1994, "当选中国工程院院士，并推动电脑一体化采编流程等出版技术革新。", "https://news.pku.edu.cn/xwzh/129-105612.htm"),
        CuratedCelebrityTimelineItem(2001, "获国家最高科学技术奖。", "https://www.xsg.pku.edu.cn/heros/winner/detail/1191.html"),
        CuratedCelebrityTimelineItem(2006, "在北京逝世；其激光照排与中文信息处理成果持续影响出版业。", "https://news.pku.edu.cn/xwzh/129-105612.htm"),
    ),
    "邓小平" to listOf(
        CuratedCelebrityTimelineItem(1920, "赴法国勤工俭学，开始旅欧经历。", "https://www.moe.gov.cn/jyb_xwfb/xw_zt/moe_357/s3579/moe_90/tnull_2554.html"),
        CuratedCelebrityTimelineItem(1924, "转为中国共产党党员。", "https://www.moe.gov.cn/jyb_xwfb/xw_zt/moe_357/s3579/moe_90/tnull_2554.html"),
        CuratedCelebrityTimelineItem(1929, "赴广西领导百色起义、龙州起义等革命活动。", "https://www.moe.gov.cn/jyb_xwfb/xw_zt/moe_357/s3579/moe_90/tnull_2554.html"),
        CuratedCelebrityTimelineItem(1938, "任八路军第129师政治委员，参与抗日根据地建设。", "https://www.bjwmb.gov.cn/kz2025/yxsj/10100303.html"),
        CuratedCelebrityTimelineItem(1978, "在改革开放初期参与推进拨乱反正和经济社会改革。", "https://www.moe.gov.cn/jyb_xwfb/xw_zt/moe_357/s3579/moe_90/tnull_2554.html"),
        CuratedCelebrityTimelineItem(1997, "在北京逝世；其政治与改革实践持续被研究和纪念。", "https://www.moe.gov.cn/jyb_xwfb/xw_zt/moe_357/s3579/moe_90/tnull_2554.html"),
    ),
    "江泽民" to listOf(
        CuratedCelebrityTimelineItem(1943, "考入南京中央大学电机系，参加进步学生抗日爱国活动。", "https://www.mod.gov.cn/gfbw/sy/tt_214026/4927445.html"),
        CuratedCelebrityTimelineItem(1946, "加入中国共产党。", "https://www.mod.gov.cn/gfbw/sy/tt_214026/4927445.html"),
        CuratedCelebrityTimelineItem(1947, "从上海交通大学电机系毕业，进入上海工业领域从事工程工作。", "https://www.mod.gov.cn/gfbw/sy/tt_214026/4927445.html"),
        CuratedCelebrityTimelineItem(1985, "在上海参与城市经济与公共管理工作。", "https://www.mod.gov.cn/gfbw/sy/tt_214026/4927445.html"),
        CuratedCelebrityTimelineItem(1989, "开始在中央层面承担重要公共事务职责。", "https://www.mod.gov.cn/gfbw/sy/tt_214026/4927445.html"),
        CuratedCelebrityTimelineItem(2022, "在上海逝世；其政治与公共事务经历持续被研究和纪念。", "https://www.mod.gov.cn/gfbw/sy/tt_214026/4927445.html"),
    ),
    "朱镕基" to listOf(
        CuratedCelebrityTimelineItem(1947, "进入清华大学电机系电机制造专业学习。", "https://www.gov.cn/yaowen/liebiao/202608/content_7078463.htm"),
        CuratedCelebrityTimelineItem(1951, "从清华大学毕业后参加国家工业与经济计划工作。", "https://www.gov.cn/yaowen/liebiao/202608/content_7078463.htm"),
        CuratedCelebrityTimelineItem(1991, "被补选为国务院副总理。", "https://zrzyt.hunan.gov.cn/zrzyt/xjdsrw/202104/t20210425_16513894.html"),
        CuratedCelebrityTimelineItem(1993, "任国务院第一副总理并兼任中国人民银行行长。", "https://zrzyt.hunan.gov.cn/zrzyt/xjdsrw/202104/t20210425_16513894.html"),
        CuratedCelebrityTimelineItem(1998, "任国务院总理，参与宏观经济与政府治理工作。", "https://zrzyt.hunan.gov.cn/zrzyt/xjdsrw/202104/t20210425_16513894.html"),
        CuratedCelebrityTimelineItem(2003, "卸任国务院总理后退出一线公共职务。", "https://www.gov.cn/yaowen/liebiao/202608/content_7078463.htm"),
    ),
    "玛丽·居里" to listOf(
        CuratedCelebrityTimelineItem(1891, "赴巴黎索邦大学继续物理与数学学习。", "https://www.nobelprize.org/prizes/chemistry/1911/marie-curie/biographical/?print=1"),
        CuratedCelebrityTimelineItem(1895, "与皮埃尔·居里结婚，开始长期科学合作。", "https://www.nobelprize.org/prizes/chemistry/1911/marie-curie/biographical/?print=1"),
        CuratedCelebrityTimelineItem(1903, "获诺贝尔物理学奖。", "https://www.nobelprize.org/prizes/physics/1903/marie-curie/"),
        CuratedCelebrityTimelineItem(1906, "继任索邦大学普通物理学教授，成为该职位首位女性。", "https://www.nobelprize.org/prizes/chemistry/1911/marie-curie/biographical/?print=1"),
        CuratedCelebrityTimelineItem(1911, "获诺贝尔化学奖，成为首位两获诺贝尔奖的人。", "https://www.nobelprize.org/prizes/physics/1903/marie-curie/"),
        CuratedCelebrityTimelineItem(1934, "在法国去世；其放射性研究遗产持续影响科学与医学。", "https://www.nobelprize.org/prizes/physics/1903/marie-curie/"),
    ),
    "伊丽莎白二世" to listOf(
        CuratedCelebrityTimelineItem(1926, "出生于伦敦梅费尔布鲁顿街17号，官方记载出生时刻为 02:40。", "https://www.royal.uk/the-queens-early-life-and-education?page=7"),
        CuratedCelebrityTimelineItem(1936, "伯父爱德华八世退位后，父亲乔治六世即位，成为王位继承人。", "https://www.royal.uk/the-queens-early-life-and-education?page=7"),
        CuratedCelebrityTimelineItem(1945, "学习驾驶，参与战时辅助服务工作。", "https://www.royal.uk/50-facts-about-queens-reign"),
        CuratedCelebrityTimelineItem(1952, "父亲乔治六世逝世后即位为英国君主。", "https://www.royal.uk/the-queens-accession-and-coronation"),
        CuratedCelebrityTimelineItem(2022, "结束在位生涯；其公共服务与君主制历史持续被研究。", "https://www.royal.uk/statement-king-following-death-queen?page=2"),
    ),
    "贝利" to listOf(
        CuratedCelebrityTimelineItem(1956, "15 岁进入桑托斯足球俱乐部。", "https://www.fifa.com/pt/articles/pele-apresentou-o-brasil-ao-mundo"),
        CuratedCelebrityTimelineItem(1958, "随巴西队夺得世界杯冠军。", "https://www.fifa.com/pt/articles/pele-apresentou-o-brasil-ao-mundo"),
        CuratedCelebrityTimelineItem(1962, "随巴西队再次夺得世界杯冠军。", "https://www.fifa.com/pt/articles/pele-apresentou-o-brasil-ao-mundo"),
        CuratedCelebrityTimelineItem(1970, "随巴西队第三次夺得世界杯冠军，成为三冠得主。", "https://www.fifa.com/pt/articles/pele-apresentou-o-brasil-ao-mundo"),
        CuratedCelebrityTimelineItem(2022, "在巴西去世；其世界足球影响持续被国际足联及各界纪念。", "https://www.fifa.com/pt/articles/pele-apresentou-o-brasil-ao-mundo"),
    ),
    "方济各" to listOf(
        CuratedCelebrityTimelineItem(1958, "进入耶稣会初学院，开始宗教培育阶段。", "https://www.vatican.va/content/francesco/en/biography/documents/papa-francesco-biografia-bergoglio.html"),
        CuratedCelebrityTimelineItem(1969, "被祝圣为司铎。", "https://www.vatican.va/content/francesco/en/biography/documents/papa-francesco-biografia-bergoglio.html"),
        CuratedCelebrityTimelineItem(1992, "被任命为布宜诺斯艾利斯辅理主教并接受主教祝圣。", "https://www.vatican.va/content/francesco/en/biography/documents/papa-francesco-biografia-bergoglio.html"),
        CuratedCelebrityTimelineItem(1998, "继任布宜诺斯艾利斯总主教。", "https://www.vatican.va/content/francesco/en/biography/documents/papa-francesco-biografia-bergoglio.html"),
        CuratedCelebrityTimelineItem(2001, "被擢升为枢机。", "https://www.vatican.va/content/francesco/en/biography/documents/papa-francesco-biografia-bergoglio.html"),
        CuratedCelebrityTimelineItem(2013, "当选天主教第266任教宗，并于3月19日正式开始教宗职务。", "https://www.vatican.va/content/francesco/en/biography/documents/papa-francesco-biografia-bergoglio.html"),
        CuratedCelebrityTimelineItem(2025, "教宗任期结束。", "https://www.vatican.va/content/vatican/en/holy-father/francesco.html"),
    ),
    "马拉拉·优素福扎伊" to listOf(
        CuratedCelebrityTimelineItem(2009, "以日记等方式记录斯瓦特河谷女孩教育处境，使教育权议题获得更广泛关注。", "https://www.nobelprize.org/prizes/peace/2014/yousafzai/facts/"),
        CuratedCelebrityTimelineItem(2012, "在校车上遭袭受伤，后转往英国接受治疗。", "https://www.nobelprize.org/prizes/peace/2014/yousafzai/facts/"),
        CuratedCelebrityTimelineItem(2013, "在联合国发表演讲，倡议女孩享有平等受教育权。", "https://www.nobelprize.org/prizes/peace/2014/yousafzai/facts/"),
        CuratedCelebrityTimelineItem(2014, "获诺贝尔和平奖，并与父亲共同创办的马拉拉基金会开展教育倡议。", "https://www.nobelprize.org/prizes/peace/2014/yousafzai/biographical/"),
    ),
    "加夫列尔·加西亚·马尔克斯" to listOf(
        CuratedCelebrityTimelineItem(1940, "赴波哥大求学，后转向新闻工作与文学写作。", "https://www.nobelprize.org/laureate/659"),
        CuratedCelebrityTimelineItem(1967, "小说《百年孤独》获得国际性突破。", "https://www.nobelprize.org/laureate/659"),
        CuratedCelebrityTimelineItem(1975, "出版《家长的没落》，进一步发展其小说创作。", "https://www.nobelprize.org/laureate/659"),
        CuratedCelebrityTimelineItem(1982, "获诺贝尔文学奖。", "https://www.nobelprize.org/prizes/literature/1982/marquez/facts/"),
        CuratedCelebrityTimelineItem(1985, "出版《霍乱时期的爱情》。", "https://www.nobelprize.org/laureate/659"),
        CuratedCelebrityTimelineItem(2014, "在墨西哥城去世；其文学影响持续存在。", "https://www.nobelprize.org/prizes/literature/1982/marquez/facts/"),
    ),
    "托妮·莫里森" to listOf(
        CuratedCelebrityTimelineItem(1964, "开始从事出版编辑工作。", "https://www.nobelprize.org/prizes/literature/1993/morrison/"),
        CuratedCelebrityTimelineItem(1970, "发表小说处女作，开始小说创作生涯。", "https://www.nobelprize.org/prizes/literature/1993/morrison/biographical/"),
        CuratedCelebrityTimelineItem(1988, "获普利策奖。", "https://www.nobelprize.org/prizes/literature/1993/morrison/biographical/"),
        CuratedCelebrityTimelineItem(1989, "在普林斯顿大学任教。", "https://www.nobelprize.org/prizes/literature/1993/morrison/biographical/"),
        CuratedCelebrityTimelineItem(1993, "获诺贝尔文学奖。", "https://www.nobelprize.org/prizes/literature/1993/morrison/"),
        CuratedCelebrityTimelineItem(2019, "在纽约去世；其作品持续影响文学研究与公共文化讨论。", "https://www.nobelprize.org/prizes/literature/1993/morrison/"),
    ),
    "旺加里·马塔伊" to listOf(
        CuratedCelebrityTimelineItem(1964, "获生物学学士学位。", "https://www.nobelprize.org/prizes/peace/2004/maathai/biographical/"),
        CuratedCelebrityTimelineItem(1971, "获内罗毕大学博士学位，成为东中非首批取得博士学位的女性学者之一。", "https://www.nobelprize.org/prizes/peace/2004/maathai/biographical/"),
        CuratedCelebrityTimelineItem(1977, "发展植树行动，形成绿带运动的基层实践。", "https://www.nobelprize.org/prizes/peace/2004/maathai/biographical/"),
        CuratedCelebrityTimelineItem(2002, "当选肯尼亚议员。", "https://www.nobelprize.org/prizes/peace/2004/maathai/biographical/"),
        CuratedCelebrityTimelineItem(2003, "任环境、自然资源与野生动物助理部长。", "https://www.nobelprize.org/prizes/peace/2004/maathai/biographical/"),
        CuratedCelebrityTimelineItem(2004, "获诺贝尔和平奖，表彰其在可持续发展、民主与和平方面的贡献。", "https://www.nobelprize.org/prizes/peace/2004/maathai/"),
        CuratedCelebrityTimelineItem(2011, "在内罗毕去世；绿带运动与其公共倡议持续影响环境和社区行动。", "https://www.nobelprize.org/prizes/peace/2004/maathai/"),
    ),
    "萨姆·奥尔特曼" to listOf(
        CuratedCelebrityTimelineItem(2014, "出任Y Combinator总裁，参与创业孵化与投资工作。", "https://www.astro.com/astro-databank/Altman%2C_Sam"),
        CuratedCelebrityTimelineItem(2019, "开始担任OpenAI首席执行官。", "https://www.astro.com/astro-databank/Altman%2C_Sam"),
        CuratedCelebrityTimelineItem(2023, "OpenAI公告宣布其离任，随后于11月29日确认其回任CEO。", "https://openai.com/index/sam-altman-returns-as-ceo-openai-has-a-new-initial-board/"),
    ),
    "拉里·佩奇" to listOf(
        CuratedCelebrityTimelineItem(1995, "在斯坦福大学与谢尔盖·布林相识并开展搜索技术研究。", "https://www.biography.com/business-leaders/larry-page"),
        CuratedCelebrityTimelineItem(1998, "与布林推出Google搜索引擎。", "https://www.biography.com/business-leaders/larry-page"),
        CuratedCelebrityTimelineItem(2004, "Google上市，搜索业务进入更大规模发展阶段。", "https://www.biography.com/business-leaders/larry-page"),
        CuratedCelebrityTimelineItem(2015, "出任新设立的Alphabet首席执行官。", "https://www.biography.com/business-leaders/larry-page"),
        CuratedCelebrityTimelineItem(2019, "卸任Alphabet CEO，继续作为共同创始人、股东和董事会成员参与。", "https://abc.xyz/investor/news/news-details/2019/Alphabet-management-change-12-03-2019/default.aspx"),
    ),
    "谢尔盖·布林" to listOf(
        CuratedCelebrityTimelineItem(1995, "在斯坦福大学与拉里·佩奇共同开展搜索技术研究。", "https://www.biography.com/business-leaders/larry-page"),
        CuratedCelebrityTimelineItem(1998, "与佩奇共同推出Google搜索引擎。", "https://www.biography.com/business-leaders/larry-page"),
        CuratedCelebrityTimelineItem(2015, "作为Google共同创始人参与Alphabet成立后的技术与前沿项目布局。", "https://abc.xyz/assets/investor/static/pdf/2015_alphabet_annual_report.pdf?cache=40474a1"),
        CuratedCelebrityTimelineItem(2019, "卸任Alphabet总裁，继续作为共同创始人、股东和董事会成员参与。", "https://abc.xyz/investor/news/news-details/2019/Alphabet-management-change-12-03-2019/default.aspx"),
    ),
    "黄仁勋" to listOf(
        CuratedCelebrityTimelineItem(1984, "在AMD任职，进入半导体行业。", "https://www.nvidia.com/en-eu/about-nvidia/board-of-directors/jensen-huang/"),
        CuratedCelebrityTimelineItem(1985, "转至LSI Logic，积累芯片行业经验。", "https://www.nvidia.com/en-eu/about-nvidia/board-of-directors/jensen-huang/"),
        CuratedCelebrityTimelineItem(1993, "联合创办英伟达并担任总裁、CEO及董事。", "https://www.nvidia.com/en-eu/about-nvidia/board-of-directors/jensen-huang/"),
        CuratedCelebrityTimelineItem(2017, "获《财富》年度商业人物认可。", "https://www.nvidia.com/en-eu/about-nvidia/board-of-directors/jensen-huang/"),
        CuratedCelebrityTimelineItem(2019, "在《哈佛商业评论》任期表现CEO评选中名列第一。", "https://www.nvidia.com/en-eu/about-nvidia/board-of-directors/jensen-huang/"),
    ),
    "王传福" to listOf(
        CuratedCelebrityTimelineItem(1987, "毕业于中南工业大学冶金物理化学专业。", "https://media.byd.com/wang-chuanfu/?lang=eng"),
        CuratedCelebrityTimelineItem(1990, "获北京有色金属研究总院工程硕士学位。", "https://media.byd.com/wang-chuanfu/?lang=eng"),
        CuratedCelebrityTimelineItem(1995, "在深圳创办比亚迪，早期从充电电池制造起步。", "https://media.byd.com/wang-chuanfu/?lang=eng"),
        CuratedCelebrityTimelineItem(2003, "比亚迪进入汽车产业，将电池技术延伸至电动交通。", "https://www.byd.com/eu/blog/Hello-we-are-BYD"),
        CuratedCelebrityTimelineItem(2024, "比亚迪成为全球首家第1000万辆新能源汽车下线的车企。", "https://www.byd.com/cn/news/2024/detail554"),
    ),
    "德米斯·哈萨比斯" to listOf(
        CuratedCelebrityTimelineItem(2010, "共同创办DeepMind，开始以通用学习系统为长期研究方向。", "https://www.pas.va/en/academicians/ordinary/hassabis.html"),
        CuratedCelebrityTimelineItem(2014, "DeepMind被Google收购，团队获得更大规模的研究与工程资源。", "https://www.pas.va/en/academicians/ordinary/hassabis.html"),
        CuratedCelebrityTimelineItem(2016, "AlphaGo在首尔以4比1战胜李世石，展示强化学习系统的突破。", "https://deepmind.google/research/alphago/"),
        CuratedCelebrityTimelineItem(2020, "AlphaFold在蛋白质结构预测任务上取得重要突破，推进AI用于生命科学。", "https://deepmind.google/science/alphafold/"),
        CuratedCelebrityTimelineItem(2023, "Google将DeepMind与Google Brain整合为Google DeepMind，由其领导。", "https://deepmind.google/blog/announcing-google-deepmind/"),
        CuratedCelebrityTimelineItem(2024, "因蛋白质结构预测相关工作获诺贝尔化学奖。", "https://www.nobelprize.org/prizes/chemistry/2024/hassabis/facts/"),
    ),
    "苏姿丰" to listOf(
        CuratedCelebrityTimelineItem(1991, "在麻省理工学院完成电机工程本科、硕士学习，进入半导体研究与工程领域。", "https://www.munzinger.de/register/portrait/biographien/su%20lisa/00/32264"),
        CuratedCelebrityTimelineItem(1994, "获得麻省理工学院电机工程博士学位，随后进入IBM从事技术工作。", "https://www.munzinger.de/register/portrait/biographien/su%20lisa/00/32264"),
        CuratedCelebrityTimelineItem(2007, "出任飞思卡尔半导体CTO，负责技术与产品方向。", "https://www.amd.com/en/corporate/leadership/lisa-su.html"),
        CuratedCelebrityTimelineItem(2012, "加入AMD，负责全球业务部门。", "https://www.amd.com/en/corporate/leadership/lisa-su.html"),
        CuratedCelebrityTimelineItem(2014, "出任AMD总裁兼CEO。", "https://www.munzinger.de/register/portrait/biographien/su%20lisa/00/32264"),
        CuratedCelebrityTimelineItem(2022, "被AMD董事会选举为董事长。", "https://ir.amd.com/financial-information/sec-filings/content/0000002488-22-000031/amdboardappointmentspressr.htm"),
    ),
    "杰弗里·辛顿" to listOf(
        CuratedCelebrityTimelineItem(1983, "开始以统计物理方法研究可学习的神经网络模型，后形成玻尔兹曼机等重要工作。", "https://www.nobelprize.org/prizes/physics/2024/hinton/facts/"),
        CuratedCelebrityTimelineItem(2024, "因促成机器学习与人工神经网络发展的基础发现和发明获诺贝尔物理学奖。", "https://www.nobelprize.org/prizes/physics/2024/hinton/facts/"),
    ),
    "约翰·古迪纳夫" to listOf(
        CuratedCelebrityTimelineItem(1952, "在芝加哥大学获物理学博士学位。", "https://www.nobelprize.org/prizes/chemistry/2019/goodenough/facts/"),
        CuratedCelebrityTimelineItem(1980, "开发钴氧化物正极锂电池，显著提高电压并成为锂离子电池关键基础。", "https://www.nobelprize.org/prizes/chemistry/2019/goodenough/facts/"),
        CuratedCelebrityTimelineItem(1986, "起任得州大学奥斯汀分校教授，继续从事材料与能源相关研究。", "https://www.nobelprize.org/prizes/chemistry/2019/goodenough/facts/"),
        CuratedCelebrityTimelineItem(2019, "因锂离子电池开发获诺贝尔化学奖。", "https://www.nobelprize.org/prizes/chemistry/2019/goodenough/facts/"),
    ),
    "吉野彰" to listOf(
        CuratedCelebrityTimelineItem(1972, "加入旭化成，开始其企业研发工作。", "https://www.nobelprize.org/prizes/chemistry/2019/yoshino/facts/"),
        CuratedCelebrityTimelineItem(1985, "制成首个可商业化锂离子电池，为移动电子与电动交通提供关键技术基础。", "https://www.nobelprize.org/prizes/chemistry/2019/yoshino/facts/"),
        CuratedCelebrityTimelineItem(2005, "在大阪大学获博士学位，并在旭化成主持实验室。", "https://www.nobelprize.org/prizes/chemistry/2019/yoshino/facts/"),
        CuratedCelebrityTimelineItem(2017, "任名城大学教授，持续参与电池技术研究与人才培养。", "https://www.nobelprize.org/prizes/chemistry/2019/yoshino/facts/"),
        CuratedCelebrityTimelineItem(2019, "因锂离子电池开发获诺贝尔化学奖。", "https://www.nobelprize.org/prizes/chemistry/2019/yoshino/facts/"),
    ),
    "约翰·霍普菲尔德" to listOf(
        CuratedCelebrityTimelineItem(1982, "提出可存储和重建模式的Hopfield网络，成为人工神经网络研究的重要节点。", "https://www.nobelprize.org/prizes/physics/2024/hopfield/facts/"),
        CuratedCelebrityTimelineItem(2024, "因推动机器学习和人工神经网络发展的基础发现和发明获诺贝尔物理学奖。", "https://www.nobelprize.org/prizes/physics/2024/hopfield/facts/"),
    ),
    "斯坦利·惠廷厄姆" to listOf(
        CuratedCelebrityTimelineItem(1968, "在牛津大学完成博士学位。", "https://www.nobelprize.org/prizes/chemistry/2019/whittingham/facts/"),
        CuratedCelebrityTimelineItem(1972, "加入埃克森，开展可充电锂电池相关研究。", "https://www.nobelprize.org/prizes/chemistry/2019/popular-information/"),
        CuratedCelebrityTimelineItem(1976, "公布可充电锂电池成果，形成后续锂离子电池发展的关键基础。", "https://www.nobelprize.org/prizes/chemistry/2019/popular-information/"),
        CuratedCelebrityTimelineItem(1988, "在纽约州立大学宾汉姆顿分校任教授，继续材料化学研究。", "https://www.nobelprize.org/prizes/chemistry/2019/whittingham/facts/"),
        CuratedCelebrityTimelineItem(2019, "因锂离子电池开发获诺贝尔化学奖。", "https://www.nobelprize.org/prizes/chemistry/2019/whittingham/facts/"),
    ),
    "大卫·贝克" to listOf(
        CuratedCelebrityTimelineItem(1984, "在哈佛大学完成生物学学士学位。", "https://www.munzinger.de/register/portrait/biographien/David%2BBaker/00/33630"),
        CuratedCelebrityTimelineItem(1989, "在加州大学伯克利分校获生物化学博士学位。", "https://www.munzinger.de/register/portrait/biographien/David%2BBaker/00/33630"),
        CuratedCelebrityTimelineItem(2003, "以计算方法设计出自然界不存在的新蛋白质，开拓计算蛋白质设计。", "https://www.nobelprize.org/prizes/chemistry/2024/baker/facts/"),
        CuratedCelebrityTimelineItem(2024, "因计算蛋白质设计获诺贝尔化学奖。", "https://www.nobelprize.org/prizes/chemistry/2024/baker/facts/"),
    ),
    "林纳斯·托瓦兹" to listOf(
        CuratedCelebrityTimelineItem(1988, "进入赫尔辛基大学学习计算机科学。", "https://www.linuxfoundation.org/about/leadership"),
        CuratedCelebrityTimelineItem(1991, "宣布正在开发Linux内核，开源操作系统项目由此发展。", "https://www.linuxfoundation.org/about/leadership"),
        CuratedCelebrityTimelineItem(2000, "入选《时代》杂志“本世纪最重要人物”榜单。", "https://www.linuxfoundation.org/about/leadership"),
        CuratedCelebrityTimelineItem(2005, "创建Git，回应大型开源项目的版本协作需求。", "https://www.linuxfoundation.org/about/leadership"),
    ),
    "马化腾" to listOf(
        CuratedCelebrityTimelineItem(1993, "在深圳大学完成计算机及应用专业学习。", "https://www.tencent.com/zh-cn/team/ma-huateng-pony-ma/"),
        CuratedCelebrityTimelineItem(1998, "参与在深圳创立腾讯。", "https://www.tencent.com/zh-cn/about.html"),
        CuratedCelebrityTimelineItem(1999, "QQ诞生，即时通信业务开始规模化发展。", "https://www.tencent.com/zh-cn/about.html"),
        CuratedCelebrityTimelineItem(2011, "微信推出，腾讯进入移动互联网社交平台阶段。", "https://www.tencent.com/zh-cn/about.html"),
    ),
    "桑达尔·皮查伊" to listOf(
        CuratedCelebrityTimelineItem(2015, "出任Google CEO，负责Google核心产品与业务发展。", "https://www.britannica.com/biography/Sundar-Pichai"),
        CuratedCelebrityTimelineItem(2019, "Alphabet公告确认其同时担任Google和Alphabet CEO。", "https://abc.xyz/investor/news/news-details/2019/Alphabet-management-change-12-03-2019/default.aspx"),
    ),
    "萨提亚·纳德拉" to listOf(
        CuratedCelebrityTimelineItem(2014, "微软公告任命其为CEO，企业战略重心进一步转向移动与云服务。", "https://blogs.microsoft.com/blog/2014/02/04/introducing-microsofts-new-ceo-satya-nadella/"),
    ),
    "蒂姆·库克" to listOf(
        CuratedCelebrityTimelineItem(1998, "加入Apple，负责全球运营相关工作。", "https://www.apple.com/ie/leadership/tim-cook/"),
        CuratedCelebrityTimelineItem(2011, "被任命为Apple CEO。", "https://www.apple.com/ie/leadership/tim-cook/"),
    ),
    "孙正义" to listOf(
        CuratedCelebrityTimelineItem(1981, "创立日本软银，开始从软件发行与信息产业发展。", "https://group.softbank/en/about/officer/son"),
        CuratedCelebrityTimelineItem(2005, "出任Alibaba.com董事，扩展互联网投资布局。", "https://group.softbank/en/about/officer/son"),
        CuratedCelebrityTimelineItem(2016, "出任ARM Holdings董事会主席，进入芯片基础设施布局。", "https://group.softbank/en/about/officer/son"),
        CuratedCelebrityTimelineItem(2023, "出任Arm Holdings董事会主席和董事。", "https://group.softbank/en/about/officer/son"),
    ),
    "迈克尔·戴尔" to listOf(
        CuratedCelebrityTimelineItem(1984, "以1000美元创办Dell Technologies，探索电脑按需直销模式。", "https://www.dell.com/en-us/lp/dt/michael-dell"),
    ),
    "杰克·多西" to listOf(
        CuratedCelebrityTimelineItem(2006, "参与创办Twitter，实时公共信息平台开始发展。", "https://www.biography.com/business-leaders/jack-dorsey"),
        CuratedCelebrityTimelineItem(2010, "创办Square，推动小微商户移动支付服务。", "https://www.biography.com/business-leaders/jack-dorsey"),
    ),
    "埃文·斯皮格尔" to listOf(
        CuratedCelebrityTimelineItem(2012, "共同创办Snap，并自5月起担任CEO和董事。", "https://newsroom.snap.com/company/leadership/evan-spiegel"),
        CuratedCelebrityTimelineItem(2014, "与早期参与者Reggie Brown达成知识产权诉讼和解。", "https://www.biography.com/business-leaders/evan-spiegel"),
    ),
    "里德·哈斯廷斯" to listOf(
        CuratedCelebrityTimelineItem(1991, "创办软件工具公司Pure Software。", "https://www.imdb.com/name/nm2484396/bio/"),
        CuratedCelebrityTimelineItem(1997, "共同创办Netflix。", "https://www.imdb.com/name/nm2484396/bio/"),
        CuratedCelebrityTimelineItem(2020, "Netflix公告回顾从DVD、流媒体到原创内容与全球本地化的发展路径。", "https://about.netflix.com/en/news/leadership-update"),
    ),
    "丹尼斯·里奇" to listOf(
        CuratedCelebrityTimelineItem(1967, "加入贝尔实验室，开始从事计算机系统研究。", "https://www.nokia.com/bell-labs/about/dennis-m-ritchie/bigbio1st.html"),
        CuratedCelebrityTimelineItem(1972, "参与推出C语言，为系统软件开发提供新的实现工具。", "https://www.invent.org/inductees/dennis-ritchie"),
        CuratedCelebrityTimelineItem(1973, "Unix系统移植到C语言，增强其跨硬件平台的可移植性。", "https://www.invent.org/inductees/dennis-ritchie"),
    ),
    "肯·汤普森" to listOf(
        CuratedCelebrityTimelineItem(1969, "与同事开始开发Unix，探索多用户操作系统的实现。", "https://www.invent.org/inductees/ken-thompson"),
        CuratedCelebrityTimelineItem(1970, "开发B语言，为后续C语言的形成提供基础。", "https://www.invent.org/inductees/ken-thompson"),
        CuratedCelebrityTimelineItem(1973, "参与将Unix与C语言结合，增强系统的可移植性。", "https://www.invent.org/inductees/ken-thompson"),
        CuratedCelebrityTimelineItem(1983, "与丹尼斯·里奇共同获得图灵奖。", "https://s3-us-west-2.amazonaws.com/belllabs-microsite-unixhistory/thompsonbio.html"),
    ),
    "文特·瑟夫" to listOf(
        CuratedCelebrityTimelineItem(1972, "担任国际网络工作组主席，参与早期互联网互联议题。", "https://computerhalloffame.org/home/computer-hall-of-fame-honorees/class-of-2004/vinton-cerf/"),
        CuratedCelebrityTimelineItem(1974, "与罗伯特·卡恩共同设计TCP/IP，提出异构网络互联的核心协议方案。", "https://computerhalloffame.org/home/computer-hall-of-fame-honorees/class-of-2004/vinton-cerf/"),
        CuratedCelebrityTimelineItem(1992, "出任互联网协会首任主席，参与开放互联网技术社群建设。", "https://computerhalloffame.org/home/computer-hall-of-fame-honorees/class-of-2004/vinton-cerf/"),
    ),
    "罗伯特·卡恩" to listOf(
        CuratedCelebrityTimelineItem(1973, "与文特·瑟夫共同发展TCP/IP的设计工作。", "https://www.invent.org/inductees/robert-e-kahn"),
        CuratedCelebrityTimelineItem(1983, "TCP/IP成为ARPANET主机通信协议的重要节点。", "https://www.invent.org/inductees/robert-e-kahn"),
        CuratedCelebrityTimelineItem(2004, "与文特·瑟夫共同获得图灵奖。", "https://www.invent.org/inductees/robert-e-kahn"),
    ),
    "詹姆斯·高斯林" to listOf(
        CuratedCelebrityTimelineItem(1992, "为Oak语言设计后来演变为Java虚拟机的系统。", "https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-0-preface1.html"),
        CuratedCelebrityTimelineItem(1995, "在Sun Microsystems推动Java形成面向互联网与分布式应用的语言平台。", "https://www.oracle.com/apac/a/ocom/resources/java-turns-25.pdf"),
        CuratedCelebrityTimelineItem(1996, "与合作者发布Java语言环境白皮书，系统阐述语言与平台的设计目标。", "https://www.oracle.com/java/technologies/language-environment.html"),
    ),
    "格蕾丝·霍珀" to listOf(
        CuratedCelebrityTimelineItem(1943, "加入美国海军预备役，进入Harvard计算项目。", "https://www.history.navy.mil/research/library/biographical-files/modern-biographical-files-ndl/modern-bios-h/hopper-grace-murray-text.html"),
        CuratedCelebrityTimelineItem(1949, "加入Eckert-Mauchly公司，参与UNIVAC I相关工作。", "https://www.history.navy.mil/research/library/biographical-files/modern-biographical-files-ndl/modern-bios-h/hopper-grace-murray-text.html"),
        CuratedCelebrityTimelineItem(1952, "发表编译器相关论文，推动自动编程实践。", "https://www.history.navy.mil/research/library/biographical-files/modern-biographical-files-ndl/modern-bios-h/hopper-grace-murray-text.html"),
        CuratedCelebrityTimelineItem(1964, "任系统编程领域职务，继续参与程序语言与标准化工作。", "https://www.history.navy.mil/research/library/biographical-files/modern-biographical-files-ndl/modern-bios-h/hopper-grace-murray-text.html"),
    ),
    "海蒂·拉玛" to listOf(
        CuratedCelebrityTimelineItem(1942, "与George Antheil的跳频通信发明获得美国专利。", "https://www.invent.org/inductees/hedy-lamarr"),
        CuratedCelebrityTimelineItem(1997, "其通信技术贡献获电子前哨基金会肯定。", "https://www.invent.org/inductees/hedy-lamarr"),
        CuratedCelebrityTimelineItem(2014, "入选美国国家发明家名人堂。", "https://www.invent.org/inductees/hedy-lamarr"),
    ),
    "约翰·麦卡锡" to listOf(
        CuratedCelebrityTimelineItem(1955, "在Dartmouth会议提案中首次公开使用“人工智能”一词。", "https://engineering.stanford.edu/news/stanfords-john-mccarthy-seminal-figure-artificial-intelligence-dead-84"),
        CuratedCelebrityTimelineItem(1958, "发表论文并提出以逻辑语言表示AI知识的研究路线。", "https://www4.cs.stanford.edu/memoriam"),
        CuratedCelebrityTimelineItem(1962, "回到斯坦福大学任计算机科学教授，后参与创建斯坦福人工智能实验室。", "https://engineering.stanford.edu/news/stanfords-john-mccarthy-seminal-figure-artificial-intelligence-dead-84"),
        CuratedCelebrityTimelineItem(1971, "获图灵奖。", "https://engineering.stanford.edu/news/stanfords-john-mccarthy-seminal-figure-artificial-intelligence-dead-84"),
    ),
    "凯瑟琳·约翰逊" to listOf(
        CuratedCelebrityTimelineItem(1953, "加入NACA，开始在Langley从事计算工作。", "https://www.nasa.gov/centers-and-facilities/langley/katherine-johnson-biography/"),
        CuratedCelebrityTimelineItem(1962, "手工核验John Glenn轨道任务的计算结果。", "https://www.nasa.gov/centers-and-facilities/langley/katherine-johnson-biography/"),
        CuratedCelebrityTimelineItem(1969, "参与Apollo月球模块与指令服务舱对接相关计算。", "https://www.nasa.gov/centers-and-facilities/langley/katherine-johnson-biography/"),
        CuratedCelebrityTimelineItem(1986, "在Langley结束33年职业生涯后退休。", "https://www.nasa.gov/centers-and-facilities/langley/katherine-johnson-biography/"),
    ),
    "玛丽·杰克逊" to listOf(
        CuratedCelebrityTimelineItem(1951, "加入NACA Langley西区计算组。", "https://www.nasa.gov/wp-content/uploads/2023/11/fy2022-afr-version5-111522-c.pdf"),
        CuratedCelebrityTimelineItem(1953, "转入高速风洞实验工作。", "https://www.nasa.gov/wp-content/uploads/2023/11/fy2022-afr-version5-111522-c.pdf"),
        CuratedCelebrityTimelineItem(1958, "完成工程训练，成为NASA首位非裔女性工程师。", "https://www.nasa.gov/wp-content/uploads/2023/11/fy2022-afr-version5-111522-c.pdf"),
        CuratedCelebrityTimelineItem(1979, "转任平等机会工作，支持女性和少数族裔的职业发展。", "https://www.nasa.gov/history/mary-w-jackson/"),
    ),
    "拉里·埃里森" to listOf(
        CuratedCelebrityTimelineItem(1977, "创办后来发展为Oracle的公司，进入商业数据库软件领域。", "https://www.oracle.com/uk/corporate/executives/larry-ellison/"),
        CuratedCelebrityTimelineItem(2014, "卸任Oracle CEO，继续担任董事会执行主席兼首席技术官。", "https://www.oracle.com/uk/corporate/executives/larry-ellison/"),
    ),
    "马克·安德森" to listOf(
        CuratedCelebrityTimelineItem(1993, "与Eric Bina共同编写Mosaic浏览器，推动图文一体的Web浏览体验。", "https://computerhalloffame.org/home/computer-hall-of-fame-honorees/class-of-2004/marc-andreessen/"),
        CuratedCelebrityTimelineItem(1994, "共同创办Mosaic Communications（后更名Netscape），发展首批商业Web浏览器。", "https://computerhalloffame.org/home/computer-hall-of-fame-honorees/class-of-2004/marc-andreessen/"),
        CuratedCelebrityTimelineItem(1999, "共同创办LoudCloud（后为Opsware），探索按服务交付的云计算模式。", "https://computerhalloffame.org/home/computer-hall-of-fame-honorees/class-of-2004/marc-andreessen/"),
    ),
    "蒂姆·伯纳斯-李" to listOf(
        CuratedCelebrityTimelineItem(1989, "在CERN发明万维网。", "https://www.w3.org/People/Berners-Lee-Bio/"),
        CuratedCelebrityTimelineItem(1990, "编写首个Web客户端与服务器，推动URI、HTTP和HTML规范的发展。", "https://www.w3.org/People/Berners-Lee-Bio/"),
        CuratedCelebrityTimelineItem(1994, "创建W3C，推进开放Web标准。", "https://www.w3.org/People/Berners-Lee-Bio/"),
        CuratedCelebrityTimelineItem(2017, "因发明万维网及其基础协议和算法获图灵奖。", "https://www.w3.org/People/Berners-Lee-Bio/"),
    ),
    "比雅尼·斯特劳斯特鲁普" to listOf(
        CuratedCelebrityTimelineItem(1979, "进入Bell Laboratories，从事分布式系统等研究。", "https://lex.dk/Bjarne_Stroustrup"),
        CuratedCelebrityTimelineItem(1980, "在1980年代初设计并实现C++，推动面向对象的系统编程。", "https://lex.dk/Bjarne_Stroustrup"),
        CuratedCelebrityTimelineItem(2002, "任Texas A&M University教授。", "https://lex.dk/Bjarne_Stroustrup"),
        CuratedCelebrityTimelineItem(2014, "进入哥伦比亚大学，并在金融技术领域继续工作。", "https://lex.dk/Bjarne_Stroustrup"),
    ),
    "罗伯特·梅特卡夫" to listOf(
        CuratedCelebrityTimelineItem(1973, "获哈佛大学博士学位，并参与早期局域网技术研究。", "https://www.invent.org/inductees/robert-m-metcalfe"),
        CuratedCelebrityTimelineItem(1979, "离开Xerox并创办3Com，推动以太网设备产业化。", "https://www.invent.org/inductees/robert-m-metcalfe"),
        CuratedCelebrityTimelineItem(2005, "因发明、标准化和商业化以太网获美国国家技术奖章。", "https://www.invent.org/inductees/robert-m-metcalfe"),
    ),
    "戈登·摩尔" to listOf(
        CuratedCelebrityTimelineItem(1957, "参与创建Fairchild Semiconductor，投入硅半导体产业。", "https://download.intel.com/newsroom/2023/corporate/Intel-Gordon-Moore-timeline-2023.pdf"),
        CuratedCelebrityTimelineItem(1965, "发表对集成电路复杂度增长的预测，后被称为“摩尔定律”。", "https://download.intel.com/newsroom/2023/corporate/Intel-Gordon-Moore-timeline-2023.pdf"),
        CuratedCelebrityTimelineItem(1968, "与罗伯特·诺伊斯共同创立Intel。", "https://www.intel.com/content/www/us/en/history/virtual-vault/articles/intels-founding.html"),
        CuratedCelebrityTimelineItem(1978, "出任Intel总裁兼CEO。", "https://download.intel.com/newsroom/2023/corporate/Intel-Gordon-Moore-timeline-2023.pdf"),
        CuratedCelebrityTimelineItem(2006, "从Intel董事长名誉职位退休。", "https://download.intel.com/newsroom/2023/corporate/Intel-Gordon-Moore-timeline-2023.pdf"),
    ),
    "伊万·萨瑟兰" to listOf(
        CuratedCelebrityTimelineItem(1963, "完成Sketchpad博士论文，展示交互式图形系统的关键思想。", "https://computerhistory.org/profile/ivan-sutherland/"),
        CuratedCelebrityTimelineItem(1964, "至1966年负责ARPA信息处理技术办公室，推动美国计算机研究。", "https://computerhistory.org/profile/ivan-sutherland/"),
        CuratedCelebrityTimelineItem(1968, "与David Evans建立计算机图形研究中心并共同创办Evans and Sutherland公司。", "https://computerhistory.org/profile/ivan-sutherland/"),
        CuratedCelebrityTimelineItem(1988, "获图灵奖。", "https://computerhistory.org/profile/ivan-sutherland/"),
    ),
    "罗伯特·诺伊斯" to listOf(
        CuratedCelebrityTimelineItem(1957, "共同创办Fairchild Semiconductor，参与硅半导体产业发展。", "https://www.computer.org/profiles/robert-noyce"),
        CuratedCelebrityTimelineItem(1961, "获得集成电路相关专利。", "https://www.intel.com/pressroom/kits/45nm/IntelTransistor_Bkgndr_FINAL.pdf"),
        CuratedCelebrityTimelineItem(1968, "与戈登·摩尔共同创办Intel。", "https://www.intel.com/content/www/us/en/history/virtual-vault/articles/intels-founding.html"),
        CuratedCelebrityTimelineItem(1987, "获美国国家技术奖章。", "https://www.intel.com/content/www/us/en/history/history-robert-noyce-man-behind-microchip-video.html"),
    ),
    "费德里科·法金" to listOf(
        CuratedCelebrityTimelineItem(1968, "在Fairchild Semiconductor开发MOS硅栅技术。", "https://computerhistory.org/profile/federico-faggin/"),
        CuratedCelebrityTimelineItem(1970, "进入Intel，主持MCS-4微处理器系列的实现工作。", "https://computerhistory.org/profile/federico-faggin/"),
        CuratedCelebrityTimelineItem(1974, "离开Intel并与Ralph Ungermann共同创办Zilog，主导Z80开发。", "https://computerhistory.org/profile/federico-faggin/"),
        CuratedCelebrityTimelineItem(1986, "共同创办Synaptics，推进触控板与触摸屏技术。", "https://computerhistory.org/profile/federico-faggin/"),
    ),
    "斯蒂夫·沃兹尼亚克" to listOf(
        CuratedCelebrityTimelineItem(1972, "与史蒂夫·乔布斯合作制作“蓝盒子”，展现早期电子工程实践。", "https://computerhistory.org/profile/steve-wozniak/"),
        CuratedCelebrityTimelineItem(1976, "设计Apple I，并与乔布斯开始组装和销售，开启Apple创业历程。", "https://www.computerhistory.org/tdih/august/11/"),
        CuratedCelebrityTimelineItem(1976, "设计Apple II，加入彩色图形和内置键盘等面向大众的个人计算机特征。", "https://computerhistory.org/profile/steve-wozniak/"),
        CuratedCelebrityTimelineItem(1985, "与乔布斯共同获美国国家技术奖章。", "https://computerhistory.org/profile/steve-wozniak/"),
    ),
    "唐纳德·克努特" to listOf(
        CuratedCelebrityTimelineItem(1963, "开始撰写《计算机程序设计艺术》，系统梳理算法和程序设计知识。", "https://engineering.stanford.edu/about/history/heroes/2011-heroes/donald-knuth"),
        CuratedCelebrityTimelineItem(1968, "首卷出版，并受聘为斯坦福大学教授。", "https://engineering.stanford.edu/about/history/heroes/2011-heroes/donald-knuth"),
        CuratedCelebrityTimelineItem(1978, "为改善数学排版开始开发TeX与METAFONT，推动数字排版工具发展。", "https://engineering.stanford.edu/about/history/heroes/2011-heroes/donald-knuth"),
        CuratedCelebrityTimelineItem(1993, "从斯坦福退休，持续推进《计算机程序设计艺术》及相关学术工作。", "https://engineering.stanford.edu/about/history/heroes/2011-heroes/donald-knuth"),
    ),
    "戈登·贝尔" to listOf(
        CuratedCelebrityTimelineItem(1960, "加入Digital Equipment Corporation，参与PDP-1等小型机工程。", "https://www.computerhistory.org/pdp-1/gordon-bell/"),
        CuratedCelebrityTimelineItem(1966, "至1972年在卡内基·梅隆大学任教并参与计算机体系结构研究。", "https://computerhistory.org/blog/in-memoriam-gordon-bell-1934-2024/"),
        CuratedCelebrityTimelineItem(1972, "回到DEC领导研发，后推动VAX系列发展。", "https://computerhistory.org/blog/in-memoriam-gordon-bell-1934-2024/"),
        CuratedCelebrityTimelineItem(1979, "与Gwen Bell共同创办计算机博物馆，成为计算机历史博物馆的源头之一。", "https://www.computerhistory.org/pdp-1/gordon-bell/"),
        CuratedCelebrityTimelineItem(1995, "加入Microsoft Research，继续从事计算与个人数据记录等研究。", "https://computerhistory.org/blog/in-memoriam-gordon-bell-1934-2024/"),
    ),
    "查尔斯·巴赫曼" to listOf(
        CuratedCelebrityTimelineItem(1950, "开始从事计算机通信系统工作，积累企业计算工程经验。", "https://amturing.acm.org/info/bachman_9385610.cfm"),
        CuratedCelebrityTimelineItem(1960, "在通用电气参与制造控制系统和数据库相关工作。", "https://amturing.acm.org/info/bachman_9385610.cfm"),
        CuratedCelebrityTimelineItem(1973, "因数据库管理系统领域的开创性贡献获图灵奖。", "https://amturing.acm.org/info/bachman_9385610.cfm"),
        CuratedCelebrityTimelineItem(1977, "至1982年主持ANSI与ISO开放系统互连相关标准委员会工作。", "https://amturing.acm.org/info/bachman_9385610.cfm"),
    ),
    "丹·布里克林" to listOf(
        CuratedCelebrityTimelineItem(1979, "与Bob Frankston开发VisiCalc，并创办Software Arts，推动电子表格成为个人电脑关键应用。", "https://www.computerhistory.org/timeline/software-languages/?pStoreID=hp_education%2F1000%27%5B0%5D"),
        CuratedCelebrityTimelineItem(1990, "共同创办Slate Corporation，探索笔计算应用软件。", "https://computerhistory.org/profile/dan-bricklin/"),
        CuratedCelebrityTimelineItem(1994, "当选ACM Fellow。", "https://computerhistory.org/profile/dan-bricklin/"),
        CuratedCelebrityTimelineItem(1995, "创办Trellix，进入网站发布技术领域。", "https://computerhistory.org/profile/dan-bricklin/"),
    ),
    "马克·贝尼奥夫" to listOf(
        CuratedCelebrityTimelineItem(1999, "共同创办Salesforce，进入企业云软件与客户关系管理领域。", "https://www.salesforce.com/company/marc-benioff-bio/"),
        CuratedCelebrityTimelineItem(1999, "在公司创立之初确立1-1-1公益模式，将股权、产品与员工时间的一部分用于社区。", "https://www.salesforce.com/company/marc-benioff-bio/"),
        CuratedCelebrityTimelineItem(2024, "获得Yale与Colin Powell领导力奖项，持续以Salesforce CEO身份推进企业软件与公益实践。", "https://www.salesforce.com/company/marc-benioff-bio/"),
    ),
    "袁征" to listOf(
        CuratedCelebrityTimelineItem(1997, "进入WebEx，从事网络会议工程工作。", "https://investors.zoom.us/board-member-management/eric-yuan/"),
        CuratedCelebrityTimelineItem(2007, "WebEx被Cisco收购后，转任Cisco工程管理岗位。", "https://investors.zoom.us/board-member-management/eric-yuan/"),
        CuratedCelebrityTimelineItem(2011, "创办Zoom并担任董事长、总裁兼CEO。", "https://investors.zoom.us/board-member-management/eric-yuan/"),
        CuratedCelebrityTimelineItem(2019, "Zoom于4月18日上市。", "https://investors.zoom.us/resources/investor-faqs/"),
    ),
    "斯图尔特·巴特菲尔德" to listOf(
        CuratedCelebrityTimelineItem(2003, "共同创办Flickr，参与早期网络图片分享服务发展。", "https://www.forbes.com/profile/stewart-butterfield/"),
        CuratedCelebrityTimelineItem(2005, "Flickr被Yahoo收购。", "https://www.forbes.com/profile/stewart-butterfield/"),
        CuratedCelebrityTimelineItem(2013, "将团队内部通信工具发展为Slack，进入企业协作软件领域。", "https://www.ebsco.com/research-starters/biography/stewart-butterfield"),
        CuratedCelebrityTimelineItem(2020, "Salesforce宣布签署收购Slack的协议。", "https://www.salesforce.com/news/press-releases/2020/12/01/salesforce-definitive-agreement-update/?bc=OTH"),
        CuratedCelebrityTimelineItem(2021, "Salesforce完成收购Slack，巴特菲尔德以CEO兼联合创始人身份参与整合初期工作。", "https://www.salesforce.com/news/press-releases/2021/07/21/salesforce-slack-deal-close/"),
    ),
    "凯文·斯特罗姆" to listOf(
        CuratedCelebrityTimelineItem(2010, "与Mike Krieger共同创办Instagram，专注移动端照片分享。", "https://en.wikipedia.org/wiki/Kevin_Systrom"),
        CuratedCelebrityTimelineItem(2012, "Facebook宣布以10亿美元收购Instagram。", "https://about.fb.com/ja/news/2012/04/acquire-instagram/"),
        CuratedCelebrityTimelineItem(2018, "卸任Instagram CEO，结束八年管理工作。", "https://www.imdb.com/name/nm7657107/"),
    ),
    "苏珊·沃西基" to listOf(
        CuratedCelebrityTimelineItem(1998, "Google创始人在其门洛帕克住宅车库开展早期办公，随后她加入Google的发展历程。", "https://apnews.com/article/19e18ebcd986490e0ff9e66e429d66c2"),
        CuratedCelebrityTimelineItem(2014, "出任YouTube CEO，开始领导在线视频平台业务。", "https://en.wikipedia.org/wiki/Susan_Wojcicki"),
        CuratedCelebrityTimelineItem(2023, "宣布卸任YouTube CEO，结束九年管理任期。", "https://apnews.com/article/19e18ebcd986490e0ff9e66e429d66c2"),
    ),
    "王兴" to listOf(
        CuratedCelebrityTimelineItem(2003, "结束在特拉华大学的博士学习后回国创业，创建校内网。", "https://zh.wikipedia.org/wiki/%E7%8E%8B%E5%85%B4_(%E7%BE%8E%E5%9B%A2)"),
        CuratedCelebrityTimelineItem(2007, "创办饭否网，继续探索即时信息服务。", "https://zh.wikipedia.org/wiki/%E7%8E%8B%E5%85%B4_(%E7%BE%8E%E5%9B%A2)"),
        CuratedCelebrityTimelineItem(2010, "创办美团，进入团购与本地生活服务领域。", "https://zh.wikipedia.org/wiki/%E7%8E%8B%E5%85%B4_(%E7%BE%8E%E5%9B%A2)"),
        CuratedCelebrityTimelineItem(2025, "继续担任美团创始人、执行董事、首席执行官兼董事长。", "https://www.meituan.com/investor-relations?darkschemeovr=1"),
    ),
    "李书福" to listOf(
        CuratedCelebrityTimelineItem(1984, "创办浙江台州石曲冰箱配件厂，开始制造业创业。", "https://pdf.dfcfw.com/pdf/H2_AN202103301478478951_1.pdf?1617129823000.pdf="),
        CuratedCelebrityTimelineItem(1994, "创办浙江华田摩托车厂，开始筹建吉利集团。", "https://pdf.dfcfw.com/pdf/H2_AN202103301478478951_1.pdf?1617129823000.pdf="),
        CuratedCelebrityTimelineItem(1997, "吉利进入汽车行业，开始生产汽车。", "https://pdf.dfcfw.com/pdf/H2_AN202103301478478951_1.pdf?1617129823000.pdf="),
        CuratedCelebrityTimelineItem(2003, "成立吉利控股集团，持续拓展汽车制造与投资管理。", "https://pdf.dfcfw.com/pdf/H2_AN202103301478478951_1.pdf?1617129823000.pdf="),
    ),
    "何小鹏" to listOf(
        CuratedCelebrityTimelineItem(2004, "联合创立UC优视，参与移动互联网软件与服务创业。", "https://ir.xiaopeng.com/zh-hans/board-member-management/hexiaopengxiansheng"),
        CuratedCelebrityTimelineItem(2014, "UC优视被阿里巴巴收购，随后任阿里巴巴移动事业群总裁等职务。", "https://ir.xiaopeng.com/zh-hans/board-member-management/hexiaopengxiansheng"),
        CuratedCelebrityTimelineItem(2017, "结束阿里巴巴任职，转向小鹏汽车创业与管理。", "https://ir.xiaopeng.com/zh-hans/board-member-management/hexiaopengxiansheng"),
        CuratedCelebrityTimelineItem(2025, "以小鹏汽车联合创始人、董事长兼CEO身份继续领导智能电动车与物理AI业务。", "https://ir.xiaopeng.com/zh-hans/board-member-management/hexiaopengxiansheng"),
    ),
    "李斌" to listOf(
        CuratedCelebrityTimelineItem(2000, "联合创立北京易车电子商务有限公司，并担任董事及总裁。", "https://ir.nio.com/zh-hans/governance/board-of-directors/"),
        CuratedCelebrityTimelineItem(2002, "作为董事长联合创立北京新意互动数字技术有限公司。", "https://ir.nio.com/zh-hans/governance/board-of-directors/"),
        CuratedCelebrityTimelineItem(2014, "创办蔚来，进入智能电动车领域。", "https://zh.wikipedia.org/wiki/%E6%9D%8E%E6%96%8C_(1974%E5%B9%B4)"),
        CuratedCelebrityTimelineItem(2018, "起担任蔚来首席执行官，并继续担任董事长。", "https://ir.nio.com/zh-hans/governance/board-of-directors/"),
    ),
    "李彦宏" to listOf(
        CuratedCelebrityTimelineItem(1991, "毕业于北京大学信息管理专业，后赴纽约州立大学布法罗分校完成计算机科学硕士学习。", "https://ir.baidu.com/management/robin-li"),
        CuratedCelebrityTimelineItem(2000, "联合创办百度，自公司成立起任董事长。", "https://ir.baidu.com/management/robin-li"),
        CuratedCelebrityTimelineItem(2004, "起担任百度首席执行官，负责整体战略与经营。", "https://ir.baidu.com/management/robin-li"),
    ),
    "丁磊" to listOf(
        CuratedCelebrityTimelineItem(1993, "毕业于电子科技大学，随后从事通信与软件技术工作。", "https://sports.cctv.com/special/tiyufenghui/20090714/106052.shtml"),
        CuratedCelebrityTimelineItem(1997, "6月创立网易，进入互联网服务创业。", "https://sports.cctv.com/special/tiyufenghui/20090714/106052.shtml"),
        CuratedCelebrityTimelineItem(2001, "转任首席架构师，聚焦公司远景战略设计与规划。", "https://ir.netease.com/zh-hans/management/william-ding"),
        CuratedCelebrityTimelineItem(2005, "起担任网易首席执行官。", "https://ir.netease.com/zh-hans/management/william-ding"),
    ),
    "张朝阳" to listOf(
        CuratedCelebrityTimelineItem(1986, "毕业于清华大学，后赴美国继续深造。", "https://news.sohu.com/20100511/n272037042.shtml"),
        CuratedCelebrityTimelineItem(1996, "回国创建爱特信公司，开始互联网创业。", "https://zh.wikipedia.org/wiki/%E5%BC%A0%E6%9C%9D%E9%98%B3"),
        CuratedCelebrityTimelineItem(1998, "推出搜狐品牌网站，公司更名为搜狐。", "https://zh.wikipedia.org/wiki/%E5%BC%A0%E6%9C%9D%E9%98%B3"),
        CuratedCelebrityTimelineItem(2000, "搜狐于7月12日在纳斯达克上市。", "https://news.sohu.com/20100511/n272037042.shtml"),
    ),
    "王小川" to listOf(
        CuratedCelebrityTimelineItem(1996, "在国际信息学奥林匹克竞赛获得金牌。", "https://www.zgcforum.com.cn/zh2024/guest/t2607/105210"),
        CuratedCelebrityTimelineItem(2010, "起担任搜狗CEO，参与搜索、输入法与浏览器产品发展。", "https://zh.wikipedia.org/wiki/%E7%8E%8B%E5%B0%8F%E5%B7%9D"),
        CuratedCelebrityTimelineItem(2017, "带领搜狗在纽约证券交易所上市。", "https://zh.wikipedia.org/wiki/%E7%8E%8B%E5%B0%8F%E5%B7%9D"),
        CuratedCelebrityTimelineItem(2023, "创立百川智能，转向通用人工智能与大模型研发。", "https://www.baichuan-ai.com/?form=tool.geiyi.cn"),
    ),
    "程维" to listOf(
        CuratedCelebrityTimelineItem(2012, "创办小桔科技并推出滴滴打车，进入移动出行服务领域。", "https://zh.wikipedia.org/wiki/%E7%A8%8B%E7%BB%B4"),
        CuratedCelebrityTimelineItem(2016, "滴滴收购优步中国业务，扩大国内移动出行服务网络。", "https://zh.wikipedia.org/wiki/%E7%A8%8B%E7%BB%B4"),
        CuratedCelebrityTimelineItem(2017, "以滴滴创始人、董事长兼CEO身份提出以数据与人工智能推动智慧交通。", "https://www.cac.gov.cn/2017-04/27/c_1120851503.htm"),
    ),
    "陈景润" to listOf(
        CuratedCelebrityTimelineItem(1953, "毕业于厦门大学数学系。", "https://www.amss.cas.cn/zhxw/2023news/202305/t20230523_6761057.html"),
        CuratedCelebrityTimelineItem(1957, "进入中国科学院数学研究所从事数学研究。", "https://www.amss.cas.cn/zhxw/2023news/202305/t20230523_6761057.html"),
        CuratedCelebrityTimelineItem(1966, "发表哥德巴赫猜想“1+2”相关论文，形成后称“陈氏定理”的重要成果。", "https://www.amss.cas.cn/zhxw/2023news/202305/t20230523_6761057.html"),
        CuratedCelebrityTimelineItem(1973, "在《中国科学》发表详细证明，引起国际数学界广泛关注。", "https://www.amss.cas.cn/ryszl/cjr/202106/t20210609_6081203.html"),
    ),
    "于敏" to listOf(
        CuratedCelebrityTimelineItem(1949, "毕业于北京大学物理系。", "https://yswk.csdl.ac.cn/ys_detail?casid=1980A20"),
        CuratedCelebrityTimelineItem(1951, "调入中国科学院近代物理研究所，从量子场论转向原子核理论研究。", "https://www.cas.cn/xzfc/202608/t20260812_5118243.shtml"),
        CuratedCelebrityTimelineItem(1967, "中国第一颗氢弹爆炸成功；其在氢弹原理突破中发挥关键作用。", "https://www.cas.cn/xzfc/202608/t20260817_5118435.shtml"),
        CuratedCelebrityTimelineItem(1980, "当选中国科学院院士。", "https://yswk.csdl.ac.cn/ys_detail?casid=1980A20"),
        CuratedCelebrityTimelineItem(2019, "获授共和国勋章。", "https://www.cas.cn/zt/rwzt/qmj2020/ym/sp/202004/t20200402_4739648.shtml"),
    ),
    "孙家栋" to listOf(
        CuratedCelebrityTimelineItem(1958, "从苏联茹科夫斯基空军工程学院毕业回国，进入国防部第五研究院从事导弹工作。", "https://www.museum.ac.cn/ldyx_news_detail/455.html"),
        CuratedCelebrityTimelineItem(1970, "参与中国第一颗人造地球卫星东方红一号的研制与发射。", "https://www.museum.ac.cn/ldyx_news_detail/455.html"),
        CuratedCelebrityTimelineItem(1991, "当选中国科学院院士。", "https://casad.cas.cn/ysxx2022/ysmd/jskx/200906/t20090624_1808260.html"),
        CuratedCelebrityTimelineItem(1999, "获“两弹一星”功勋奖章。", "https://www.museum.ac.cn/ldyx_news_detail/455.html"),
    ),
    "钱七虎" to listOf(
        CuratedCelebrityTimelineItem(1960, "毕业于中国人民解放军军事工程学院防护工程专业。", "https://cace.cumt.edu.cn/info/1031/22978.htm"),
        CuratedCelebrityTimelineItem(1965, "获苏联莫斯科古比雪夫军事工程学院防护工程专业副博士学位。", "https://cace.cumt.edu.cn/info/1031/22978.htm"),
        CuratedCelebrityTimelineItem(1994, "当选中国工程院院士。", "https://www.cae.cn/cae/html/main/colys/68461805.html"),
        CuratedCelebrityTimelineItem(2018, "获得国家最高科学技术奖。", "https://ysg.ckcest.cn/100/363.html"),
        CuratedCelebrityTimelineItem(2022, "获得八一勋章。", "https://www.ctgu.edu.cn/info/1173/5021.htm"),
    ),
    "李德仁" to listOf(
        CuratedCelebrityTimelineItem(1963, "毕业于武汉测绘学院航测系。", "https://yswk.csdl.ac.cn/ys_detail?casid=1991D14"),
        CuratedCelebrityTimelineItem(1985, "获德国斯图加特大学摄影测量与遥感专业博士学位。", "https://yswk.csdl.ac.cn/ys_detail?casid=1991D14"),
        CuratedCelebrityTimelineItem(1991, "当选中国科学院院士。", "https://yswk.csdl.ac.cn/ys_detail?casid=1991D14"),
        CuratedCelebrityTimelineItem(1994, "当选中国工程院院士。", "https://sges.sysu.edu.cn/node/611"),
        CuratedCelebrityTimelineItem(2024, "获得2023年度国家最高科学技术奖。", "https://yswk.csdl.ac.cn/ys_detail?casid=1991D14"),
    ),
    "顾方舟" to listOf(
        CuratedCelebrityTimelineItem(1951, "作为新中国首批赴苏联留学人员之一，进入苏联医学科学院病毒研究所学习。", "https://www.cast.org.cn/mx2035zt/zyhj/art/2026/art_38c8b546ddb3c29073547e1c3caaff5f.html"),
        CuratedCelebrityTimelineItem(1960, "参与脊髓灰质炎疫苗研究和防控工作，推动国产口服疫苗发展。", "https://zh.wikipedia.org/wiki/%E9%A1%BE%E6%96%B9%E8%88%9F"),
        CuratedCelebrityTimelineItem(2019, "获“人民科学家”国家荣誉称号。", "https://www.cast.org.cn/dsycqgdbdh/cjzs/dfkxfc/art/2026/art_62ef4410575843e2aed3af59799cf133.html"),
    ),
    "王振义" to listOf(
        CuratedCelebrityTimelineItem(1948, "毕业于上海震旦大学医学院。", "https://jktv.net.cn/special/wangzhenyi/"),
        CuratedCelebrityTimelineItem(1994, "当选中国工程院院士。", "https://www.cae.cn/cae/html/main/colys/59122228.html"),
        CuratedCelebrityTimelineItem(2011, "获得2010年度国家最高科学技术奖。", "https://jktv.net.cn/special/wangzhenyi/"),
    ),
    "韩启德" to listOf(
        CuratedCelebrityTimelineItem(1968, "毕业于上海第一医学院医学系。", "https://casad.cas.cn/ysxx2022/ysmd/smkx/200906/t20090624_1803310.html"),
        CuratedCelebrityTimelineItem(1982, "获西安医学院病理生理学专业医学硕士学位。", "https://casad.cas.cn/ysxx2022/ysmd/smkx/200906/t20090624_1803310.html"),
        CuratedCelebrityTimelineItem(1997, "当选中国科学院院士。", "https://www.93.gov.cn/syfc-lyys-zgkxyys-yy/220341.html"),
    ),
    "李兰娟" to listOf(
        CuratedCelebrityTimelineItem(1973, "毕业于浙江医科大学，开始感染性疾病及传染病学研究。", "https://www.cae.cn/cae/html/main/colys/71795027.html"),
        CuratedCelebrityTimelineItem(2005, "当选中国工程院院士。", "https://www.cae.cn/cae/html/main/colys/71795027.html"),
        CuratedCelebrityTimelineItem(2020, "参与新冠疫情防控和公共卫生应急工作。", "https://zh.wikipedia.org/wiki/%E6%9D%8E%E5%85%B0%E5%A8%9F"),
    ),
    "樊锦诗" to listOf(
        CuratedCelebrityTimelineItem(1963, "毕业于北京大学历史系考古专业后进入敦煌文物研究所工作。", "https://www.dha.ac.cn/info/1480/4447.htm"),
        CuratedCelebrityTimelineItem(1998, "担任敦煌研究院院长。", "https://gansu.gscn.com.cn/system/2020/01/17/012306199.shtml"),
        CuratedCelebrityTimelineItem(2015, "转任敦煌研究院名誉院长，继续参与敦煌文化遗产保护与研究。", "https://gansu.gscn.com.cn/system/2020/01/17/012306199.shtml"),
        CuratedCelebrityTimelineItem(2019, "获“文物保护杰出贡献者”国家荣誉称号。", "https://www.gswbj.gov.cn/a/2020/07/28/5637.html"),
    ),
    "李娜" to listOf(
        CuratedCelebrityTimelineItem(2011, "在法网女单决赛夺冠，成为首位赢得网球大满贯单打冠军的亚洲球员。", "https://www.sport.gov.cn/n20001280/n20767351/n20767722/c20836162/content.html"),
        CuratedCelebrityTimelineItem(2014, "获得澳网女单冠军，取得个人第二座大满贯单打冠军。", "https://www.sport.gov.cn/n20001280/n20745751/n20767274/c22157231/content.html"),
        CuratedCelebrityTimelineItem(2014, "宣布从 WTA 正式退役。", "https://www.sport.gov.cn/n20001280/n20745751/n20767274/c22157231/content.html"),
    ),
    "孙杨" to listOf(
        CuratedCelebrityTimelineItem(2011, "在上海世游赛获得 800 米、1500 米自由泳金牌，并打破 1500 米世界纪录。", "https://www.sport.gov.cn/n4/n14741/n14762/c737635/content.html"),
        CuratedCelebrityTimelineItem(2012, "获得伦敦奥运会 400 米自由泳金牌并打破奥运纪录，取得中国男子游泳首枚奥运金牌。", "https://www.sport.gov.cn/n14471/n14482/n14519/c687590/content.html"),
        CuratedCelebrityTimelineItem(2013, "在巴塞罗那世锦赛获得 400 米、800 米和 1500 米自由泳冠军。", "https://www.sport.gov.cn/n4/n14741/n14762/c737635/content.html"),
        CuratedCelebrityTimelineItem(2016, "获得里约奥运会 200 米自由泳冠军、400 米自由泳银牌。", "https://www.sport.gov.cn/n4/n14741/n14762/c737635/content.html"),
    ),
    "梅兰芳" to listOf(
        CuratedCelebrityTimelineItem(1905, "11岁首次登台，开始职业舞台生涯。", "https://www.beijing.gov.cn/renwen/whrl/rdtj/202201/t20220121_2597492.html"),
        CuratedCelebrityTimelineItem(1913, "赴上海首演《穆柯寨》并崭露头角，为艺术创新和“梅派”形成奠定基础。", "https://lgj.bjhd.gov.cn/qcsh/xxyd/202403/t20240322_4645803.htm"),
        CuratedCelebrityTimelineItem(1919, "率团赴日演出，开启京剧海外传播的重要实践。", "https://www.beijing.gov.cn/renwen/whrl/rdtj/202201/t20220121_2597492.html"),
        CuratedCelebrityTimelineItem(1930, "赴美国演出，以京剧表演推动中国戏曲的国际传播。", "https://szb.taizhou.gov.cn/jyxy/hsjy/art/2026/art_daa32078a0b445c0bc4e9748d4edb05f.html"),
        CuratedCelebrityTimelineItem(1961, "去世；梅派艺术和代表剧目持续影响京剧舞台与戏曲教育。", "https://www.beijing.gov.cn/renwen/whrl/rdtj/202201/t20220121_2597492.html"),
    ),
    "张国荣" to listOf(
        CuratedCelebrityTimelineItem(1977, "在亚洲业余歌唱大赛获亚军，开始演艺事业。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_EN.pdf"),
        CuratedCelebrityTimelineItem(1983, "凭《风继续吹》走红，成为香港流行乐坛的重要歌手。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_EN.pdf"),
        CuratedCelebrityTimelineItem(1991, "凭《阿飞正传》获得香港电影金像奖最佳男主角。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_EN.pdf"),
        CuratedCelebrityTimelineItem(1993, "凭《霸王别姬》获日本影评人协会最佳男主角，电影表演获得国际肯定。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_EN.pdf"),
        CuratedCelebrityTimelineItem(2003, "去世；其音乐、电影作品与文化形象持续被纪念和研究。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_EN.pdf"),
    ),
    "崔健" to listOf(
        CuratedCelebrityTimelineItem(1986, "在北京工人体育馆演唱《一无所有》，成为中国摇滚进入公众视野的重要节点。", "https://yptimes.shyp.gov.cn/html/2016-03/12/content_4_4.htm"),
        CuratedCelebrityTimelineItem(1989, "举办“新长征路上的摇滚”演唱会；同名专辑被报道为中国内地早期原创摇滚专辑的代表。", "https://wenlian.taian.gov.cn/art/2019/2/21/art_69765_4800285.html"),
        CuratedCelebrityTimelineItem(2025, "参加“摇滚编年史”演出；相关政府报道仍将其列为中国摇滚发展历程的代表人物。", "https://www.jcgov.gov.cn/dtxx/jcdt/202506/t20250603_2150292.shtml"),
    ),
    "王守仁" to listOf(
        CuratedCelebrityTimelineItem(1499, "中进士，授刑部主事，进入仕途。", "https://www.dpm.org.cn/lemmas/241268.html"),
        CuratedCelebrityTimelineItem(1506, "因反对宦官刘瑾遭廷杖并被流放贵州，此后思想与讲学经历发生重要转折。", "https://www.dpm.org.cn/lemmas/241268.html"),
        CuratedCelebrityTimelineItem(1519, "平定宁王朱宸濠叛乱，成为其政治、军事生涯的重要节点。", "https://m.ahjjjc.gov.cn/p/73442.html"),
        CuratedCelebrityTimelineItem(1528, "去世，留下《王文成全书》；其心学与教育思想持续影响后世。", "https://www.dpm.org.cn/lemmas/241268.html"),
    ),
    "欧阳修" to listOf(
        CuratedCelebrityTimelineItem(1030, "登进士第，开始仕途。", "https://www.hnhx.gov.cn/portal/zjhx/hxyx/webinfo/2021/01/1612766650239657.htm"),
        CuratedCelebrityTimelineItem(1036, "因为范仲淹辩护被贬夷陵，体现其直言立场。", "https://www.qyjj.gov.cn/view.thtml?id=40147"),
        CuratedCelebrityTimelineItem(1045, "再被贬知滁州，后写成《醉翁亭记》。", "https://jssdfz.jiangsu.gov.cn/n97/20231116/i29578.html"),
        CuratedCelebrityTimelineItem(1061, "升参知政事，进入北宋中枢。", "https://www.pdsjjw.gov.cn/sitesources/nysjwjw/page_pc/xcjd/s/article845fe54687164173a3e794b5402d726c.html"),
        CuratedCelebrityTimelineItem(1072, "去世，谥“文忠”；其文学、史学与金石学著述长期流传。", "https://www.hnhx.gov.cn/portal/zjhx/hxyx/webinfo/2021/01/1612766650239657.htm"),
    ),
    "真德秀" to listOf(
        CuratedCelebrityTimelineItem(1199, "登进士第，进入仕途。", "https://sfj.ahsz.gov.cn/pfzl/fzwh/192144351.html"),
        CuratedCelebrityTimelineItem(1217, "任泉州太守期间巡视海防、整顿吏治并减免商税，参与港口治理。", "https://www.quanzhou.gov.cn/lyb/lytj/rmjdtj/202202/t20220224_2699961.htm"),
        CuratedCelebrityTimelineItem(1232, "第二次出任泉州太守，整肃海盗、维护航道与海外贸易秩序。", "https://quanzhou.gov.cn/lyb/lytj/rmjdtj/202202/t20220224_2699979.htm"),
        CuratedCelebrityTimelineItem(1235, "去世；《大学衍义》《文章正宗》《西山集》等著作持续流传。", "https://dfz.shaanxi.gov.cn/zslm/fzzlk/sxjz/hzs_16205/201706/P020240923600200898228.pdf"),
    ),
    "琼瑶" to listOf(
        CuratedCelebrityTimelineItem(1949, "随家人赴台湾生活，日后开始尝试写作。", "https://www.hyfl.gov.cn/view/68.html"),
        CuratedCelebrityTimelineItem(1963, "小说《窗外》发表并走红，进入职业作家发展阶段。", "https://www.hyfl.gov.cn/view/68.html"),
        CuratedCelebrityTimelineItem(2003, "《还珠格格三——天上人间》获授权由北京十月文艺出版社出版，显示作品持续进入大众出版市场。", "https://www.ncac.gov.cn/xxfb/tzgg/200309/t20030925_50187.html"),
    ),
    "弗里德里希·尼采" to listOf(
        CuratedCelebrityTimelineItem(1869, "受聘为巴塞尔大学古典语文学教授，开始学术生涯。", "https://plato.stanford.edu/entries/nietzsche/"),
        CuratedCelebrityTimelineItem(1872, "出版《悲剧的诞生》，以希腊悲剧和瓦格纳音乐为切入点展开文化批评。", "https://plato.stanford.edu/entries/nietzsche/"),
        CuratedCelebrityTimelineItem(1879, "因健康原因辞去教职，转入独立写作阶段。", "https://plato.stanford.edu/entries/nietzsche/"),
        CuratedCelebrityTimelineItem(1883, "开始出版《查拉图斯特拉如是说》，进入成熟创作期。", "https://plato.stanford.edu/entries/nietzsche/"),
        CuratedCelebrityTimelineItem(1889, "在都灵精神崩溃，此后不再进行正常写作。", "https://plato.stanford.edu/entries/nietzsche/"),
        CuratedCelebrityTimelineItem(1900, "去世；其著作在20世纪哲学与文化理论中持续被讨论。", "https://plato.stanford.edu/entries/nietzsche/"),
    ),
    "弗里德里希·恩格斯" to listOf(
        CuratedCelebrityTimelineItem(1845, "发表《英国工人阶级状况》，以工业社会中的劳动者处境为重要研究对象。", "https://www.marxists.org/archive/marx/works/date/"),
        CuratedCelebrityTimelineItem(1848, "与马克思共同发表《共产党宣言》。", "https://www.marxists.org/archive/marx/works/1848/communist-manifesto/index.htm"),
        CuratedCelebrityTimelineItem(1867, "参与《资本论》第一卷的出版、校阅和后续传播工作。", "https://www.marxists.org/archive/marx/works/1867-c1/index.htm"),
    ),
    "罗宾德拉纳特·泰戈尔" to listOf(
        CuratedCelebrityTimelineItem(1901, "在圣地尼克坦创办实验学校，开展教育实践。", "https://www.nobelprize.org/laureate/583"),
        CuratedCelebrityTimelineItem(1913, "获诺贝尔文学奖，成为首位获该奖项的亚洲作家。", "https://www.nobelprize.org/laureate/583"),
        CuratedCelebrityTimelineItem(1915, "获英国政府授予爵士称号。", "https://www.nobelprize.org/prizes/literature/1913/tagore/biographical/?print=1"),
        CuratedCelebrityTimelineItem(1941, "去世；诗歌、小说、戏剧、歌曲与教育思想持续影响印度和世界读者。", "https://www.nobelprize.org/prizes/literature/1913/tagore/biographical/?print=1"),
    ),
    "阿道夫·希特勒" to listOf(
        CuratedCelebrityTimelineItem(1923, "领导纳粹党企图以暴力夺权失败。", "https://encyclopedia.ushmm.org/content/en/article/the-nazi-rise-to-power?parent=en%2F11083"),
        CuratedCelebrityTimelineItem(1930, "纳粹党在德国全国选举中获得显著票数，上升为重要政治力量。", "https://encyclopedia.ushmm.org/content/en/article/the-nazi-rise-to-power?parent=en%2F11083"),
        CuratedCelebrityTimelineItem(1933, "被任命为德国总理；纳粹政权随后将民主制度转化为独裁统治。", "https://encyclopedia.ushmm.org/content/en/article/the-nazi-rise-to-power?parent=en%2F11083"),
    ),
    "约翰·D·洛克菲勒" to listOf(
        CuratedCelebrityTimelineItem(1859, "创办佣金贸易合伙企业，开始商业生涯。", "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/"),
        CuratedCelebrityTimelineItem(1863, "与合伙人进入石油炼制业。", "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/"),
        CuratedCelebrityTimelineItem(1870, "组织成立标准石油公司。", "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/"),
        CuratedCelebrityTimelineItem(1882, "标准石油资产合并为标准石油托拉斯，成为美国大型企业组织史的重要事件。", "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/"),
        CuratedCelebrityTimelineItem(1911, "美国最高法院裁定标准石油托拉斯违反反垄断法并要求拆分。", "https://rockarch.org/resources/about-the-rockefellers/john-d-rockefeller-sr/"),
    ),
    "托马斯·爱迪生" to listOf(
        CuratedCelebrityTimelineItem(1870, "发明改良股票报价机，获得资金建立实验室和制造设施。", "https://www.nps.gov/edis/learn/historyculture/edison-biography.htm"),
        CuratedCelebrityTimelineItem(1876, "迁至门洛帕克，建立团队研发基地。", "https://home.nps.gov/edis/learn/historyculture/edison-biography.htm"),
        CuratedCelebrityTimelineItem(1877, "发明留声机，实现录音与回放。", "https://www.nps.gov/people/thomas-edison-biography-1847-1882-birth-to-pearl-street.htm"),
        CuratedCelebrityTimelineItem(1879, "研制出可实用的白炽灯，并继续完善照明系统。", "https://www.nps.gov/people/thomas-edison-biography-1847-1882-birth-to-pearl-street.htm"),
        CuratedCelebrityTimelineItem(1882, "纽约珍珠街商业电站投运，电力照明进入规模化应用阶段。", "https://home.nps.gov/edis/learn/historyculture/edison-biography.htm"),
    ),
    "董浩云" to listOf(
        CuratedCelebrityTimelineItem(1928, "进入国际运输株式会社任练习生，开始航运职业经历。", "https://maritimemuseum.sjtu.edu.cn/zjdhy/spds.htm"),
        CuratedCelebrityTimelineItem(1941, "在香港注册成立中国航运信托公司。", "https://maritimemuseum.sjtu.edu.cn/zjdhy/spds.htm"),
        CuratedCelebrityTimelineItem(1952, "收购首艘万吨级货轮“海洋海王星”，扩展大型船队。", "https://maritimemuseum.sjtu.edu.cn/zjdhy/spds.htm"),
        CuratedCelebrityTimelineItem(1962, "“如云”号抵达纽约港，“东方海外”开辟中美定期航线。", "https://maritimemuseum.sjtu.edu.cn/zjdhy/spds.htm"),
        CuratedCelebrityTimelineItem(1979, "高速全货柜轮“中华货柜”号下水；同年“海上巨人”号举行命名典礼。", "https://maritimemuseum.sjtu.edu.cn/zjdhy/spds.htm"),
        CuratedCelebrityTimelineItem(1982, "去世；其航运企业与海事文化遗产持续被整理研究。", "https://maritimemuseum.sjtu.edu.cn/zjdhy/spds.htm"),
    ),
    "荣宗敬" to listOf(
        CuratedCelebrityTimelineItem(1896, "与荣德生等人在上海开设广生钱庄，开始独立经营。", "https://daj.wuxi.gov.cn/doc/2017/06/15/2425516.shtml"),
        CuratedCelebrityTimelineItem(1900, "与荣德生创办茂新面粉厂，开启荣氏企业的近代实业发展。", "https://zx.wuxi.gov.cn/doc/2022/09/10/3750071.shtml"),
        CuratedCelebrityTimelineItem(1915, "与荣德生创办申新纺织企业，进入纺织工业。", "https://wenming.shpt.gov.cn/jjpt/20250613/960726.html"),
        CuratedCelebrityTimelineItem(1921, "荣氏面粉生产能力在民族资本机制面粉中占有显著比重，形成“面粉大王”声誉。", "https://www.jssjw.gov.cn/art/2015/10/29/art_601_103276.html"),
        CuratedCelebrityTimelineItem(1938, "去世；荣氏企业继续成为研究近代民族工业的重要样本。", "https://daj.wuxi.gov.cn/doc/2017/07/03/2425519.shtml"),
    ),
    "吴火狮" to listOf(
        CuratedCelebrityTimelineItem(1967, "与相关投资方发起创立新光合成纤维公司，进入化纤产业。", "https://www.shinkong.com.tw/cn/front/about"),
        CuratedCelebrityTimelineItem(1970, "新光合成纤维工厂建成并开始生产。", "https://www.shinkong.com.tw/cn/front/about"),
    ),
    "文天祥" to listOf(
        CuratedCelebrityTimelineItem(1273, "起复入仕，宋末政局日益危急。", "https://www.dpm.org.cn/lemmas/243027.html"),
        CuratedCelebrityTimelineItem(1275, "应诏勤王，先后任平江府、临安府等职，进右丞相兼枢密使。", "https://www.dpm.org.cn/lemmas/243027.html"),
        CuratedCelebrityTimelineItem(1278, "在丽江浦屯兵抗元。", "https://www.dpm.org.cn/lemmas/243027.html"),
        CuratedCelebrityTimelineItem(1283, "在大都柴市就义，留下《文山集》等作品。", "https://www.dpm.org.cn/lemmas/243027.html"),
    ),
    "戚继光" to listOf(
        CuratedCelebrityTimelineItem(1556, "率军在浙江迎战倭寇，投入东南抗倭。", "https://www.dpm.org.cn/court/figure/104028.html"),
        CuratedCelebrityTimelineItem(1559, "在义乌招募并训练新军，形成戚家军。", "https://www.dpm.org.cn/court/figure/104028.html"),
        CuratedCelebrityTimelineItem(1568, "任总理蓟州、昌平、保定三镇军务，转赴北方边防。", "https://www.dpm.org.cn/court/figure/104028.html"),
        CuratedCelebrityTimelineItem(1583, "调任广东镇守，后告老还乡。", "https://www.dpm.org.cn/court/figure/104028.html"),
        CuratedCelebrityTimelineItem(1588, "在故里去世；《纪效新书》《练兵实纪》等兵书持续流传。", "https://www.dpm.org.cn/court/figure/104028.html"),
    ),
    "张廷玉" to listOf(
        CuratedCelebrityTimelineItem(1697, "中进士，授检讨，进入清廷仕途。", "https://www.dpm.org.cn/lemmas/241593.html"),
        CuratedCelebrityTimelineItem(1723, "参与续修《明史》，任总裁之一。", "https://www.dpm.org.cn/court/event/159202.html"),
        CuratedCelebrityTimelineItem(1749, "以老病退休。", "https://www.dpm.org.cn/lemmas/241593.html"),
        CuratedCelebrityTimelineItem(1755, "去世，谥“文和”，有《传经堂集》。", "https://www.dpm.org.cn/lemmas/241593.html"),
    ),
    "曾国藩" to listOf(
        CuratedCelebrityTimelineItem(1854, "所办团练湘军练成并在湖南会集，成为湘军形成的重要节点。", "https://www.dpm.org.cn/court/lineage/226247.html"),
        CuratedCelebrityTimelineItem(1860, "任两江总督、钦差大臣，督办江南军务。", "https://www.dpm.org.cn/lemmas/241724.html?ivk_sa=1024320u"),
        CuratedCelebrityTimelineItem(1864, "湘军攻陷天京，太平天国运动在军事上失败。", "https://www.dpm.org.cn/lemmas/241724.html?ivk_sa=1024320u"),
        CuratedCelebrityTimelineItem(1868, "调任直隶总督。", "https://www.dpm.org.cn/lemmas/241724.html?ivk_sa=1024320u"),
        CuratedCelebrityTimelineItem(1872, "在南京病逝；其奏稿、家书和日记成为研究晚清的重要材料。", "https://www.dpm.org.cn/lemmas/241724.html?ivk_sa=1024320u"),
    ),
    "张巡" to listOf(
        CuratedCelebrityTimelineItem(755, "安史之乱爆发后起兵讨叛军，屡次在雍丘等地作战。", "https://www.dengzhou.gov.cn/2015/07-22/1115504.html"),
        CuratedCelebrityTimelineItem(756, "转战宁陵并击破叛军，继续维持东部防线。", "https://www.zgqx.gov.cn/qx/c00350/pc/content/content_1900780869245550592.html"),
        CuratedCelebrityTimelineItem(757, "率部与许远共同坚守睢阳，长期牵制叛军南下。", "https://www.dengzhou.gov.cn/2015/07-22/1115504.html"),
        CuratedCelebrityTimelineItem(757, "睢阳城陷后遇害；唐肃宗追赠扬州大都督、封邓国公。", "https://www.dengzhou.gov.cn/2015/07-22/1115504.html"),
    ),
    "彭玉麟" to listOf(
        CuratedCelebrityTimelineItem(1853, "加入湘军并参与创建湘军水师。", "https://www.hyzhq.gov.cn/zhqq/msgj/20200213/i968754.html"),
        CuratedCelebrityTimelineItem(1854, "在湘潭等战役中指挥水师作战，逐步形成湘军水师实力。", "https://www.sxfj.gov.cn/gong_zuo_dong_tai/yao_wen/10978204.shtml"),
        CuratedCelebrityTimelineItem(1861, "升水师提督，继续参与九江、安庆等战事。", "https://www.hyzhq.gov.cn/zhqq/msgj/20200213/i968754.html"),
        CuratedCelebrityTimelineItem(1872, "奉命巡阅长江水师，持续关注江防。", "https://css.hunan.gov.cn/css/tslm/hxws/wssy/201803/mryg_3u/202012/t20201231_14098615.html"),
        CuratedCelebrityTimelineItem(1881, "督办江海防务，后在中法战争期间主张加强防务。", "https://css.hunan.gov.cn/css/tslm/hxws/wssy/201803/mryg_3u/202012/t20201231_14098615.html"),
        CuratedCelebrityTimelineItem(1890, "在衡阳病逝；诗文、书法和画梅作品持续流传。", "https://www.hyzhq.gov.cn/zhqq/msgj/20200213/i968754.html"),
    ),
    "张璁" to listOf(
        CuratedCelebrityTimelineItem(1521, "中进士后参与嘉靖初年的礼制争论，仕途由此上升。", "https://www.dpm.org.cn/Uploads/File/pdf/01/66/cb/0166cb286b7d2ee1749f7b3e7d03a296.pdf"),
        CuratedCelebrityTimelineItem(1527, "受命主持《明伦大典》编修并任总裁。", "https://www.dpm.org.cn/Uploads/File/2019/12/15/u5df5e991c6429.pdf"),
        CuratedCelebrityTimelineItem(1528, "《明伦大典》完成进呈，其后因书成受加衔。", "https://www.dpm.org.cn/Uploads/File/2019/12/15/u5df5e991c6429.pdf"),
    ),
    "方从哲" to listOf(
        CuratedCelebrityTimelineItem(1583, "中进士，授庶吉士，后历迁国子监祭酒。", "https://www.dpm.org.cn/lemmas/242699.html"),
        CuratedCelebrityTimelineItem(1613, "升礼部尚书兼东阁大学士，进入内阁。", "https://www.dpm.org.cn/lemmas/242699.html"),
        CuratedCelebrityTimelineItem(1622, "因“三案”争议受攻击，被削发归里。", "https://www.dpm.org.cn/lemmas/242699.html"),
        CuratedCelebrityTimelineItem(1628, "去世；其政治立场持续成为明末史研究对象。", "https://www.dpm.org.cn/lemmas/242699.html"),
    ),
    "梅艳芳" to listOf(
        CuratedCelebrityTimelineItem(1982, "获新秀歌唱大赛冠军，进入香港流行乐坛。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_TC.pdf"),
        CuratedCelebrityTimelineItem(1985, "《坏女孩》走红，确立其流行歌手地位；同年举行首个个人演唱会。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_TC.pdf"),
        CuratedCelebrityTimelineItem(1988, "凭《胭脂扣》获金马奖、香港电影金像奖及亚太影展最佳女主角。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_TC.pdf"),
        CuratedCelebrityTimelineItem(2003, "去世；其音乐、电影与公益形象持续被纪念。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-04_Electronic-Publications/ehouseprog_02_TC.pdf"),
    ),
    "周深" to listOf(
        CuratedCelebrityTimelineItem(2016, "完成音乐学院学业；同年以《大鱼》等作品在音乐节目中获得广泛关注。", "https://ent.cctv.com/2016/06/24/ARTIsxQ49KRFNNRwzDYNmBXK160624.shtml"),
        CuratedCelebrityTimelineItem(2017, "《大鱼》获东方风云榜“十大金曲”，作品影响继续扩大。", "https://ent.cctv.com/2018/03/27/ARTI0Qy49dzf35XlMSRTrwts180327.shtml"),
        CuratedCelebrityTimelineItem(2023, "登上央视春晚演唱《花开忘忧》，扩大面向大众的舞台传播。", "https://news.cctv.com/2023/01/22/ARTIExkzBZMpu37XnCSKcMor230122.shtml"),
    ),
    "邓丽君" to listOf(
        CuratedCelebrityTimelineItem(1982, "演唱《但愿人长久》，歌曲在海内外长期传唱。", "https://scdfz.sc.gov.cn/whzh/slzc1/content_139750"),
        CuratedCelebrityTimelineItem(1985, "在日本举行告别演唱会，展现其多语种演唱能力与海外影响。", "https://www.bjstb.gov.cn/eportal/ui?articleKey=1306399&columnId=1259350&pageId=1259249"),
        CuratedCelebrityTimelineItem(1995, "在泰国清迈去世，华语世界持续举办纪念活动。", "https://www.bjstb.gov.cn/bjtb/jljw/jtgcs/1306666/index.html"),
    ),
    "宁泽涛" to listOf(
        CuratedCelebrityTimelineItem(2015, "随中国队首次进入男子4×100米自由泳接力世锦赛决赛，取得第七名。", "https://www.sport.gov.cn/n20001280/n20067662/n20067613/c22969700/content.html"),
        CuratedCelebrityTimelineItem(2015, "在喀山世锦赛获得男子100米自由泳金牌，成为首位获该项目世锦赛冠军的中国和亚洲选手。", "https://www.sport.gov.cn/n20001280/n20067662/n20067613/c22969302/content.html"),
    ),
    "张常宁" to listOf(
        CuratedCelebrityTimelineItem(2015, "随中国女排获得女排世界杯冠军。", "https://www.sport.gov.cn/n20001280/n20745751/n20767274/c21635010/content.html"),
        CuratedCelebrityTimelineItem(2016, "随中国女排获得里约奥运会冠军。", "https://www.sport.gov.cn/n14471/n14481/n14518/c740688/content.html"),
        CuratedCelebrityTimelineItem(2019, "随中国女排再获女排世界杯冠军。", "https://www.sport.gov.cn/n20001280/n20745751/n20767274/c21635010/content.html"),
    ),
    "杨威" to listOf(
        CuratedCelebrityTimelineItem(2008, "获得北京奥运会男子体操个人全能冠军。", "https://www.sport.gov.cn/n4/n10706/c711964/content.html"),
        CuratedCelebrityTimelineItem(2008, "作为中国体操男团成员获得北京奥运会团体冠军。", "https://www.sport.gov.cn/n4/n14741/n14752/c734663/content.html"),
    ),
    "陈一冰" to listOf(
        CuratedCelebrityTimelineItem(2008, "随中国体操男团获得北京奥运会冠军，并获男子吊环金牌。", "https://www.sport.gov.cn/n4/n14741/n14752/c734663/content.html"),
        CuratedCelebrityTimelineItem(2012, "随中国体操男团获伦敦奥运会冠军，男子吊环比赛获银牌。", "https://www.sport.gov.cn/n4/n166/n170/c322956/content.html"),
        CuratedCelebrityTimelineItem(2013, "在中国体操队建队60周年仪式上举行退役仪式。", "https://www.sport.gov.cn/n20001280/n20067662/n20067613/c23081671/content.html"),
    ),
    "赵朴初" to listOf(
        CuratedCelebrityTimelineItem(1945, "参与发起组织中国民主促进会。", "https://www.cppcc.gov.cn/zxww/2019/08/05/ARTI1564990425737341.shtml"),
        CuratedCelebrityTimelineItem(1949, "以宗教界代表身份出席中国人民政治协商会议第一届全体会议。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197330265227.shtml"),
        CuratedCelebrityTimelineItem(1983, "担任全国政协副主席，并长期参与民族和宗教事务工作。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197330265227.shtml"),
        CuratedCelebrityTimelineItem(1992, "获一等瑞宝章及东国大学哲学博士等国际文化交流荣誉。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197330265227.shtml"),
        CuratedCelebrityTimelineItem(2000, "去世；佛教文化、书法与公共事务实践持续被纪念和研究。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197330265227.shtml"),
    ),
    "陶弘景" to listOf(
        CuratedCelebrityTimelineItem(492, "隐居句曲山（茅山），进入其道教与学术活动的重要阶段。", "https://fzg.changzhou.gov.cn/html/fzg/2016/POBKOFQN_0627/31666.html"),
        CuratedCelebrityTimelineItem(536, "去世；道教、医药、本草与文学著作持续流传。", "https://fzg.changzhou.gov.cn/html/fzg/2016/POBKOFQN_0627/31666.html"),
    ),
    "张道陵" to listOf(
        CuratedCelebrityTimelineItem(142, "相传在鹤鸣山首创五斗米道，后称天师道。", "https://dfz.shaanxi.gov.cn/zslm/fzzlk/xbsxsxz/xbsxz/xas_16198/201711/P020240923626792713048.pdf"),
    ),
    "张玉书" to listOf(
        CuratedCelebrityTimelineItem(1661, "中进士，开始清廷仕途。", "https://www.dpm.org.cn/lemmas/240212.html"),
        CuratedCelebrityTimelineItem(1711, "去世，谥“文贞”；其文献编纂与古文著述留下影响。", "https://www.dpm.org.cn/lemmas/240212.html"),
    ),
    "曾国荃" to listOf(
        CuratedCelebrityTimelineItem(1864, "统率湘军围攻天京，是太平天国战争后期的重要军事人物。", "https://zyk.bjhd.gov.cn/ztzl/sjtj/tjzz/201811/P020151225585161734459.pdf"),
        CuratedCelebrityTimelineItem(1890, "去世，身后赠太傅、谥“忠襄”。", "https://www.bjwmb.gov.cn/zxfw/wmwx/wskt/t20180524_867310.htm"),
    ),
    "易烊千玺" to listOf(
        CuratedCelebrityTimelineItem(2013, "以 TFBOYS 成员身份宣布出道，开始职业演艺活动。", "https://shaoer.cctv.com/special/tfboys/index.shtml"),
        CuratedCelebrityTimelineItem(2019, "主演电影《少年的你》上映，完成首部电影主演作品的公开呈现。", "https://m.news.cctv.com/2020/05/08/ARTINsdTodtf1PxhJSahXYPc200508.shtml"),
        CuratedCelebrityTimelineItem(2020, "凭《少年的你》获得第39届香港电影金像奖最佳新演员奖。", "https://m.news.cctv.com/2020/05/08/ARTINsdTodtf1PxhJSahXYPc200508.shtml"),
    ),
    "王菲" to listOf(
        CuratedCelebrityTimelineItem(1989, "以王靖雯名义发行首张个人专辑，正式在香港歌坛出道。", "https://www.universal-music.co.jp/faye-wong/biography/"),
        CuratedCelebrityTimelineItem(1992, "演唱《容易受伤的女人》后在香港流行乐坛获得广泛关注。", "https://m.1905.com/m/star/info/219/"),
        CuratedCelebrityTimelineItem(1994, "出演王家卫电影《重庆森林》，形成重要银幕角色。", "https://m.1905.com/m/star/info/219/"),
        CuratedCelebrityTimelineItem(1999, "演唱《Eyes on Me》，获得日本唱片大奖亚洲音乐奖。", "https://www.iq.com/actor-info/%E7%8E%8B%E8%8F%B2-faye-wong-214367905?lang=zh_cn"),
    ),
    "张学友" to listOf(
        CuratedCelebrityTimelineItem(1984, "获香港十八区业余歌唱大赛冠军，进入职业歌坛。", "https://m.1905.com/m/star/info/466/"),
        CuratedCelebrityTimelineItem(1985, "发行首张个人专辑《Smile》。", "https://m.1905.com/m/star/info/466/"),
        CuratedCelebrityTimelineItem(1989, "凭《旺角卡门》获第8届香港电影金像奖最佳男配角。", "https://ent.cctv.com/special/C18159/20070405/103419.shtml"),
        CuratedCelebrityTimelineItem(1993, "发行《吻别》，其销量表现成为华语唱片市场的重要节点。", "https://m.1905.com/m/star/info/466/"),
        CuratedCelebrityTimelineItem(1997, "主演音乐剧《雪狼湖》，开拓大型华语音乐剧演出。", "https://m.1905.com/m/star/info/466/"),
        CuratedCelebrityTimelineItem(2012, "“1/2世纪世界巡回演唱会”获吉尼斯世界纪录认证。", "https://m.1905.com/m/star/info/466/"),
        CuratedCelebrityTimelineItem(2023, "开启“60+”世界巡回演唱会。", "https://m.1905.com/m/star/info/466/"),
    ),
    "胡歌" to listOf(
        CuratedCelebrityTimelineItem(2005, "主演《仙剑奇侠传》，以李逍遥一角建立广泛观众认知。", "https://www.1905.com/mdb/star/269/"),
        CuratedCelebrityTimelineItem(2008, "发行音乐专辑《出发》，持续开展音乐工作。", "https://www.iq.com/actor-info/%E8%83%A1%E6%AD%8C-hu-ge-200072905?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2015, "主演电视剧《伪装者》，拓展谍战题材表演。", "https://www.iq.com/actor-info/%E8%83%A1%E6%AD%8C-hu-ge-200072905?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2016, "凭《琅琊榜》获上海电视节白玉兰奖最佳男主角。", "https://www.iq.com/actor-info/%E8%83%A1%E6%AD%8C-hu-ge-200072905?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2023, "主演电影《不虚此行》并获上海国际电影节金爵奖最佳男演员；同年主演《繁花》。", "https://www.iq.com/actor-info/%E8%83%A1%E6%AD%8C-hu-ge-200072905?lang=zh_cn"),
    ),
    "孙俪" to listOf(
        CuratedCelebrityTimelineItem(2001, "参加新加坡“才华横溢出新秀”比赛，获亚军和智慧大奖。", "https://m.1905.com/m/star/info/255/"),
        CuratedCelebrityTimelineItem(2002, "主演《玉观音》并获得广泛关注。", "https://m.1905.com/m/star/info/255/"),
        CuratedCelebrityTimelineItem(2004, "凭《玉观音》获第22届中国电视金鹰奖观众喜爱的女演员及最具人气女演员。", "https://m.1905.com/m/star/info/255/"),
        CuratedCelebrityTimelineItem(2006, "凭电影《霍元甲》获第28届大众电影百花奖最佳新人。", "https://m.1905.com/m/star/info/255/"),
        CuratedCelebrityTimelineItem(2011, "《甄嬛传》播出，主演的甄嬛成为其代表性荧屏角色。", "https://m.1905.com/m/star/info/255/"),
    ),
    "李宇春" to listOf(
        CuratedCelebrityTimelineItem(2002, "在成都举办首次个人演唱会“最后的战役”。", "https://m.1905.com/m/star/info/171/"),
        CuratedCelebrityTimelineItem(2005, "获《超级女声》年度总决赛冠军，进入职业歌坛。", "https://m.1905.com/m/star/info/171/"),
        CuratedCelebrityTimelineItem(2009, "出演电影《十月围城》，拓展银幕表演。", "https://www.1905.com/mdb/star/171/"),
        CuratedCelebrityTimelineItem(2010, "参演徐克电影《龙门飞甲》，继续开展影视工作。", "https://m.1905.com/m/star/info/171/"),
    ),
    "薛之谦" to listOf(
        CuratedCelebrityTimelineItem(2005, "参加《我型我秀》，进入大众音乐节目视野。", "https://ent.sina.com.cn/y/2006-04-03/17011037297.html?from=wap"),
        CuratedCelebrityTimelineItem(2006, "发行首张个人原创同名专辑《薛之谦》。", "https://m.1905.com/m/star/1538"),
        CuratedCelebrityTimelineItem(2006, "《认真的雪》等歌曲使其获得更广泛的音乐关注。", "https://m.1905.com/m/star/1538"),
        CuratedCelebrityTimelineItem(2017, "参演电视剧《我们的少年时代》，持续拓展影视表演。", "https://zh.wikipedia.org/wiki/%E8%96%9B%E4%B9%8B%E8%B0%A6%E5%BD%B1%E8%A7%86%E4%BD%9C%E5%93%81%E5%88%97%E8%A1%A8"),
    ),
    "周冬雨" to listOf(
        CuratedCelebrityTimelineItem(2010, "主演《山楂树之恋》上映，以该片进入电影行业并获新人表演奖。", "https://www.1905.com/mdb/star/391290/life/"),
        CuratedCelebrityTimelineItem(2014, "主演《同桌的你》，扩大青春题材电影观众影响。", "https://m.1905.com/m/star/info/391290/"),
        CuratedCelebrityTimelineItem(2019, "主演《少年的你》上映。", "https://www.1905.com/mdb/star/391290/life/"),
        CuratedCelebrityTimelineItem(2020, "凭《少年的你》获香港电影金像奖、金鸡奖最佳女主角。", "https://m.1905.com/m/star/info/391290/"),
        CuratedCelebrityTimelineItem(2023, "主演电影票房累计破100亿元，成为中国影史首位90后百亿女主演。", "https://www.1905.com/mdb/star/391290/life/"),
    ),
    "陈坤" to listOf(
        CuratedCelebrityTimelineItem(1999, "出演电影《国歌》，开始影视表演工作。", "https://m.1905.com/m/star/info/251/"),
        CuratedCelebrityTimelineItem(2003, "电视剧《金粉世家》首播，以金燕西一角获得广泛关注。", "https://m.1905.com/m/star/info/251/"),
        CuratedCelebrityTimelineItem(2006, "主演《云水谣》，获第12届中国电影华表奖优秀男演员。", "https://m.1905.com/m/star/info/251/"),
        CuratedCelebrityTimelineItem(2010, "凭《画皮》获第30届大众电影百花奖最佳男主角。", "https://m.1905.com/m/star/info/251/"),
        CuratedCelebrityTimelineItem(2011, "主演《龙门飞甲》，一人分饰雨化田和风里刀。", "https://m.1905.com/m/star/info/251/"),
    ),
    "黄晓明" to listOf(
        CuratedCelebrityTimelineItem(1998, "出演电视剧《爱情不是游戏》，开始演艺生涯。", "https://m.1905.com/m/star/info/174/"),
        CuratedCelebrityTimelineItem(2001, "主演《大汉天子》，以汉武帝刘彻一角走入大众视野。", "https://m.1905.com/m/star/info/174/"),
        CuratedCelebrityTimelineItem(2006, "主演电视剧《神雕侠侣》，饰演杨过。", "https://m.1905.com/m/star/info/174/"),
        CuratedCelebrityTimelineItem(2013, "凭《中国合伙人》获金鸡、华表、百花最佳男主角等表演奖。", "https://m.1905.com/m/star/info/174/"),
        CuratedCelebrityTimelineItem(2019, "主演《烈火英雄》，饰演消防队长江立伟。", "https://m.1905.com/m/star/info/174/"),
    ),
    "肖战" to listOf(
        CuratedCelebrityTimelineItem(2015, "通过选秀节目亮相舞台，开始从设计工作转入演艺行业。", "https://v.cctv.com/2019/09/17/VIDEjpMu9DuIsWkPyO3KeTRX190917.shtml"),
        CuratedCelebrityTimelineItem(2019, "电视剧《陈情令》播出后获得广泛关注，随后以《诛仙Ⅰ》完成大银幕主演亮相。", "https://v.cctv.com/2019/09/17/VIDEjpMu9DuIsWkPyO3KeTRX190917.shtml"),
    ),
    "田亮" to listOf(
        CuratedCelebrityTimelineItem(2000, "获悉尼奥运会男子十米台冠军、双人跳台银牌。", "https://www.sport.gov.cn/n20001280/n20067662/n20067613/c23145738/content.html"),
        CuratedCelebrityTimelineItem(2004, "获雅典奥运会双人跳台金牌、男子十米台铜牌。", "https://www.sport.gov.cn/n20001280/n20067662/n20067613/c23145738/content.html"),
        CuratedCelebrityTimelineItem(2012, "进入国际泳联名人堂，获最佳跳水运动员奖。", "https://www.sport.gov.cn/n20001280/n20067662/n20067613/c23145738/content.html"),
    ),
    "范志毅" to listOf(
        CuratedCelebrityTimelineItem(1998, "效力英甲水晶宫期间入围亚洲足球先生最终候选名单。", "https://www.sport.gov.cn/n20001280/n20745751/n20767279/c21270418/content.html"),
        CuratedCelebrityTimelineItem(2001, "获亚洲足球先生，成为首位获此荣誉的中国球员。", "https://www.sport.gov.cn/n20001280/n20745751/n20767279/c21270418/content.html"),
    ),
    "马琳" to listOf(
        CuratedCelebrityTimelineItem(2008, "在北京奥运会获得乒乓球男子单打冠军，实现大满贯。", "https://www.sport.gov.cn/n4/n10706/c727342/content.html"),
        CuratedCelebrityTimelineItem(2008, "作为中国队成员获得北京奥运会乒乓球男子团体冠军。", "https://www.sport.gov.cn/n4/n166/n175/c322691/content.html"),
    ),
    "李连杰" to listOf(
        CuratedCelebrityTimelineItem(1975, "至1979年连续五年获得全国武术比赛冠军，形成早期武术运动经历。", "https://sports.cctv.com/20080328/101107.shtml"),
        CuratedCelebrityTimelineItem(1982, "主演电影《少林寺》公映并广泛走红，成为其从武术运动转入影坛的关键节点。", "https://news.cctv.com/news/entertainment/20060126/100330.shtml"),
        CuratedCelebrityTimelineItem(1991, "开始在香港连续拍摄动作片，塑造黄飞鸿、方世玉等银幕形象。", "https://big5.cctv.com/gate/big5/news.cctv.cn/society/20070429/105679_1.shtml"),
        CuratedCelebrityTimelineItem(1998, "赴美国发展，参演《致命武器4》，进入好莱坞电影市场。", "https://big5.cctv.com/gate/big5/news.cctv.com/performance/20081202/102188.shtml"),
        CuratedCelebrityTimelineItem(2007, "成立壹基金，公益事务成为其职业生涯的重要组成。", "https://big5.cctv.com/gate/big5/news.cctv.com/performance/20081202/102188.shtml"),
    ),
    "林则徐" to listOf(
        CuratedCelebrityTimelineItem(1838, "受命为钦差大臣赴广东查办禁烟，开始主持广东禁烟事务。", "https://www.dpm.org.cn/lemmas/241847.html"),
        CuratedCelebrityTimelineItem(1839, "迫使外国鸦片商缴出鸦片，并在虎门海滩当众销毁。", "https://www.dpm.org.cn/lemmas/241847.html"),
        CuratedCelebrityTimelineItem(1840, "鸦片战争爆发后积极筹备海防、抵抗英军，后受诬陷被革职。", "https://www.dpm.org.cn/lemmas/241847.html"),
        CuratedCelebrityTimelineItem(1850, "奉命赴广西途中病逝；其禁烟、海防与译介西方资料的实践持续受到研究。", "https://www.dpm.org.cn/lemmas/241847.html"),
    ),
    "李鸿章" to listOf(
        CuratedCelebrityTimelineItem(1863, "在上海设立江南制造总局，参与近代军事工业建设。", "https://www.dpm.org.cn/court/lineage/226248.html"),
        CuratedCelebrityTimelineItem(1895, "代表清政府在日本马关议和并签订《马关条约》。", "https://www.dpm.org.cn/court/lineage/226254.html"),
        CuratedCelebrityTimelineItem(1901, "与十一国公使订立《辛丑条约》。", "https://www.dpm.org.cn/court/lineage/226254.html"),
    ),
    "海瑞" to listOf(
        CuratedCelebrityTimelineItem(1558, "升任淳安县令，开始地方行政实践。", "https://www.dpm.org.cn/court/figure/104050.html"),
        CuratedCelebrityTimelineItem(1565, "上《治安疏》直言批评嘉靖朝政，被投入大狱。", "https://www.dpm.org.cn/court/figure/104050.html"),
        CuratedCelebrityTimelineItem(1569, "任应天巡抚，巡视各县、兴修水利并推行限田等措施。", "https://www.dpm.org.cn/court/figure/104050.html"),
        CuratedCelebrityTimelineItem(1587, "卒于南京任所，谥“忠介”。", "https://www.dpm.org.cn/court/figure/104050.html"),
    ),
    "洪承畴" to listOf(
        CuratedCelebrityTimelineItem(1641, "在松锦战事中兵败被俘，后降清。", "https://www.dpm.org.cn/lemmas/244897.html"),
        CuratedCelebrityTimelineItem(1644, "任内秘书院大学士，进入清初中枢。", "https://www.dpm.org.cn/lemmas/244897.html"),
        CuratedCelebrityTimelineItem(1645, "受命总督军务、招抚江南诸省，并参与《明史》纂修。", "https://www.dpm.org.cn/court/event/159202.html"),
        CuratedCelebrityTimelineItem(1661, "致仕，结束仕宦生涯。", "https://www.dpm.org.cn/lemmas/244897.html"),
    ),
    "岳锺琪" to listOf(
        CuratedCelebrityTimelineItem(1724, "任川陕总督，参与雍正朝西北边疆军务。", "https://www.dpm.org.cn/court/lineage/226259.html?hl=%E9%9B%8D%E6%AD%A3+%E8%A2%8D"),
        CuratedCelebrityTimelineItem(1729, "与傅尔丹分领军队出征西北，并卷入曾静、张熙投书策反案。", "https://www.dpm.org.cn/court/lineage/226259.html?hl=%E9%9B%8D%E6%AD%A3+%E8%A2%8D"),
    ),
    "老舍" to listOf(
        CuratedCelebrityTimelineItem(1918, "自北京师范学校毕业，开始在北京、天津等地任教。", "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11735343.html"),
        CuratedCelebrityTimelineItem(1924, "赴英国任伦敦大学东方学院汉语讲师，开始海外教学与早期小说创作。", "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11735343.html"),
        CuratedCelebrityTimelineItem(1937, "小说《骆驼祥子》问世，成为其代表作之一。", "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11735343.html"),
        CuratedCelebrityTimelineItem(1949, "年底回国，后参与新中国文艺与公共文化工作。", "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11735343.html"),
        CuratedCelebrityTimelineItem(1966, "在“文化大革命”初期遭受迫害后去世。", "https://www.yantian.gov.cn/ytdayszxxw/lsjt/content/post_11735343.html"),
    ),
    "胡适" to listOf(
        CuratedCelebrityTimelineItem(1910, "赴美国留学，后师从哲学家杜威。", "https://www.huangshan.gov.cn/zjhs/rwhz/hzmr/8295585.html"),
        CuratedCelebrityTimelineItem(1917, "发表《文学改良刍议》并回国任北京大学教授，推动文学改良讨论。", "https://www.huangshan.gov.cn/zjhs/rwhz/hzmr/8295585.html"),
        CuratedCelebrityTimelineItem(1918, "加入《新青年》编辑部，提倡白话文和新文学创作。", "https://m.ahjjjc.gov.cn/p/48847.html"),
        CuratedCelebrityTimelineItem(1946, "至1948年任北京大学校长。", "https://m.ahjjjc.gov.cn/p/48847.html"),
        CuratedCelebrityTimelineItem(1952, "返台任中央研究院院长。", "https://m.ahjjjc.gov.cn/p/48847.html"),
    ),
    "叶恭绰" to listOf(
        CuratedCelebrityTimelineItem(1905, "赴日本留学，后参与近代交通与铁路事业。", "https://www.counsellor.gov.cn/2021-11/13/c_1211504856.htm"),
        CuratedCelebrityTimelineItem(1921, "出任民国政府交通总长，并曾担任交通大学院长。", "https://www.counsellor.gov.cn/2021-11/13/c_1211504856.htm"),
        CuratedCelebrityTimelineItem(1939, "组织发起中国文化协进会，推动文物展览和文化公益活动。", "https://www.counsellor.gov.cn/2021-11/13/c_1211504856.htm"),
        CuratedCelebrityTimelineItem(1949, "参与新中国文化事业，后任中央文史研究馆代理馆长、北京中国画院首任院长等职。", "https://www.counsellor.gov.cn/2021-11/13/c_1211504856.htm"),
    ),
    "茅盾" to listOf(
        CuratedCelebrityTimelineItem(1916, "自北京大学毕业后进入上海商务印书馆，开始主持《小说月报》等文学编辑工作。", "https://www.sjz.gov.cn/aqxcjy/columns/981fc4e0-28c6-4f18-a8c3-6a1b1b6c92b8/202305/17/c7c043c7-b43f-43fb-b124-fe514e36c4b9.html"),
        CuratedCelebrityTimelineItem(1921, "加入中国共产党早期组织，后转为正式党员并参与上海地方工作。", "https://www.sjz.gov.cn/aqxcjy/columns/981fc4e0-28c6-4f18-a8c3-6a1b1b6c92b8/202305/17/c7c043c7-b43f-43fb-b124-fe514e36c4b9.html"),
        CuratedCelebrityTimelineItem(1933, "长篇小说《子夜》问世，成为其现实主义创作的重要代表。", "https://www.sjz.gov.cn/aqxcjy/columns/981fc4e0-28c6-4f18-a8c3-6a1b1b6c92b8/202305/17/c7c043c7-b43f-43fb-b124-fe514e36c4b9.html"),
        CuratedCelebrityTimelineItem(1981, "病重时设立茅盾文学奖，支持当代长篇小说创作。", "https://www.sjz.gov.cn/aqxcjy/columns/981fc4e0-28c6-4f18-a8c3-6a1b1b6c92b8/202305/17/c7c043c7-b43f-43fb-b124-fe514e36c4b9.html"),
    ),
    "闻一多" to listOf(
        CuratedCelebrityTimelineItem(1922, "赴美留学，接受西洋美术教育并加深对新文学、新诗的关注。", "https://wlt.hubei.gov.cn/bmdt/ztzl/zshb/201912/t20191226_1799459.shtml"),
        CuratedCelebrityTimelineItem(1923, "诗集《红烛》出版。", "https://wlt.hubei.gov.cn/bmdt/ztzl/zshb/201912/t20191226_1799459.shtml"),
        CuratedCelebrityTimelineItem(1928, "诗集《死水》出版，并任武汉大学文学院院长兼中文系主任。", "https://wlt.hubei.gov.cn/bmdt/ztzl/zshb/201912/t20191226_1799459.shtml"),
        CuratedCelebrityTimelineItem(1945, "任中国民主同盟中央委员，参与民主运动。", "https://wlt.hubei.gov.cn/bmdt/ztzl/zshb/201912/t20191226_1799459.shtml"),
        CuratedCelebrityTimelineItem(1946, "在昆明遇害；其诗歌、学术与公共行动持续被纪念和研究。", "https://wlt.hubei.gov.cn/bmdt/ztzl/zshb/201912/t20191226_1799459.shtml"),
    ),
    "黄金荣" to listOf(
        CuratedCelebrityTimelineItem(1901, "进入法租界巡捕房任三等探员，开始在租界警务体系任职。", "https://www.shda.gov.cn/archives/gdxw/202601/t20260121_76431.html"),
        CuratedCelebrityTimelineItem(1925, "自法租界巡捕房退休；此前已升任华人督察长。", "https://www.shda.gov.cn/archives/gdxw/202601/t20260121_76431.html"),
        CuratedCelebrityTimelineItem(1931, "控制上海大世界并改称“荣记大世界”，其管理时期黄赌毒问题猖獗。", "https://www.shda.gov.cn/archives/gdxw/202601/t20260121_76431.html"),
        CuratedCelebrityTimelineItem(1949, "上海解放后公开登报忏悔过去罪行。", "https://www.shda.gov.cn/archives/gdxw/202601/t20260121_76431.html"),
    ),
    "杜月笙" to listOf(
        CuratedCelebrityTimelineItem(1946, "上海市参议会成立时曾被选为议长，后辞任。", "https://www.shanghai.gov.cn/shanghai/newshanghai/%E4%B8%8A%E6%B5%B7%E9%80%9A%E5%BF%97.pdf"),
    ),
    "佛印禅师" to listOf(
        CuratedCelebrityTimelineItem(1031, "出生于浮梁；法名了元，后世称佛印禅师。", "https://www.jdz.gov.cn/zsnj/jdzsz/jdzszxc1985/dwj/rwz/P020250717401203628309.pdf"),
        CuratedCelebrityTimelineItem(1047, "参谒园通讷禅师，被补为书记。", "https://www.jdz.gov.cn/zsnj/jdzsz/jdzszxc1985/dwj/rwz/P020250717401203628309.pdf"),
        CuratedCelebrityTimelineItem(1058, "主持江州承天寺，后又游学并住持多处寺院。", "https://www.jdz.gov.cn/zsnj/jdzsz/jdzszxc1985/dwj/rwz/P020250717401203628309.pdf"),
        CuratedCelebrityTimelineItem(1078, "在云居寺住持时期延续数十年，形成其江南禅林活动的重要阶段。", "https://www.jdz.gov.cn/zsnj/jdzsz/jdzszxc1985/dwj/rwz/P020250717401203628309.pdf"),
        CuratedCelebrityTimelineItem(1098, "去世；相关诗文、书信与佛教史料持续流传。", "https://www.jdz.gov.cn/zsnj/jdzsz/jdzszxc1985/dwj/rwz/P020250717401203628309.pdf"),
    ),
    "吕洞宾" to listOf(
        CuratedCelebrityTimelineItem(796, "岳阳地方档案资料所载的一种传说称其生于唐贞元十二年；出生地与具体生平仍有异说。", "https://www.junshan.gov.cn/32414/32428/32433/content_1172178.html"),
        CuratedCelebrityTimelineItem(1310, "元武宗加封“纯阳演政警化孚佑帝君”，显示其在元代道教体系中的尊崇地位。", "https://www.dxh.gov.cn/YXLKG_16692/wszl/202303/P020250510527315925128.pdf"),
    ),
    "杨幂" to listOf(
        CuratedCelebrityTimelineItem(1990, "参演电视剧《唐明皇》，以童星身份开始演艺经历。", "https://www.iq.com/actor-info/%E6%9D%A8%E5%B9%82-yang-mi-200072305?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2005, "参演电视剧《神雕侠侣》，饰演郭襄。", "https://ent.cctv.com/special/huangxiaoming/20090223/108227.shtml"),
        CuratedCelebrityTimelineItem(2009, "参演电视剧《仙剑奇侠传三》，公众关注度进一步提高。", "https://www.iq.com/actor-info/%E6%9D%A8%E5%B9%82-yang-mi-200072305?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2011, "主演电视剧《宫锁心玉》，获得更广泛的公众认知。", "https://www.iq.com/actor-info/%E6%9D%A8%E5%B9%82-yang-mi-200072305?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2013, "主演电影《小时代》上映，职业重心同时扩展至电影项目。", "https://www.iq.com/actor-info/%E6%9D%A8%E5%B9%82-yang-mi-200072305?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2017, "参演电视剧《三生三世十里桃花》，该剧于2017年播出。", "https://www.iq.com/album/%E4%B8%89%E7%94%9F%E4%B8%89%E4%B8%96%E5%8D%81%E9%87%8C%E6%A1%83%E8%8A%B1-2017-19rrh93l2t?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2021, "参演电影《刺杀小说家》上映，继续拓展类型片表演。", "https://www.iq.com/actor-info/%E6%9D%A8%E5%B9%82-yang-mi-200072305?lang=zh_cn"),
    ),
    "杨紫" to listOf(
        CuratedCelebrityTimelineItem(2002, "参演电视剧《孝庄秘史》，开始在影视作品中崭露头角。", "https://www.iq.com/actor-info/%E6%A5%8A%E7%B4%AB-andy-204405405?lang=zh_tw"),
        CuratedCelebrityTimelineItem(2004, "参演《女生日记》并获中国电影童牛奖优秀儿童演员提名。", "https://www.iq.com/actor-info/%E6%A5%8A%E7%B4%AB-andy-204405405?lang=zh_tw"),
        CuratedCelebrityTimelineItem(2014, "主演电视剧《战长沙》，进入成年演员阶段的重要作品。", "https://www.iq.com/actor-info/%E6%A5%8A%E7%B4%AB-andy-204405405?lang=zh_tw"),
        CuratedCelebrityTimelineItem(2016, "凭电视剧《欢乐颂》获得上海电视节白玉兰奖最佳女配角提名。", "https://www.iq.com/actor-info/%E6%A5%8A%E7%B4%AB-andy-204405405?lang=zh_tw"),
        CuratedCelebrityTimelineItem(2017, "领衔主演《香蜜沉沉烬如霜》开机，进入该剧制作阶段。", "https://www.pwrd.com/cn/news/press/p2017/20170712/79281.shtml"),
        CuratedCelebrityTimelineItem(2018, "电视剧《香蜜沉沉烬如霜》播出，杨紫为主演之一。", "https://wetv.vip/zh-cn/play/jkgn2fxw2j72huk/n0034n35d24"),
    ),
    "赵丽颖" to listOf(
        CuratedCelebrityTimelineItem(2006, "在雅虎搜星比赛中获得冯小刚组冠军，进入演艺行业。", "https://www.iq.com/actor-info/zhao-liying-206109705?lang=en_us"),
        CuratedCelebrityTimelineItem(2011, "参演电视剧《新还珠格格》，以晴儿一角受到更多关注。", "https://www.iq.com/actor-info/zhao-liying-206109705?lang=en_us"),
        CuratedCelebrityTimelineItem(2013, "领衔主演电视剧《陆贞传奇》，该剧在湖南卫视播出。", "https://media.people.com.cn/n/2013/0516/c40733-21505097.html"),
        CuratedCelebrityTimelineItem(2014, "入选第十届中国金鹰电视艺术节金鹰女神。", "https://www.iq.com/actor-info/zhao-liying-206109705?lang=en_us"),
        CuratedCelebrityTimelineItem(2017, "主演电视剧《楚乔传》，于6月5日首播。", "https://dianshiju.cctv.com/2017/05/23/VIDA7AdV1d3c5D8l1zoh9yuv170523.shtml"),
    ),
    "迪丽热巴" to listOf(
        CuratedCelebrityTimelineItem(2010, "考入上海戏剧学院表演系，开始接受系统表演训练。", "https://www.920603.com/"),
        CuratedCelebrityTimelineItem(2013, "主演电视剧《阿娜尔罕》播出，以该剧正式进入演员职业。", "https://www.920603.com/"),
        CuratedCelebrityTimelineItem(2017, "参演电视剧《三生三世十里桃花》，该剧于2017年播出。", "https://www.iq.com/album/%E4%B8%89%E7%94%9F%E4%B8%89%E4%B8%96%E5%8D%81%E9%87%8C%E6%A1%83%E8%8A%B1-2017-19rrh93l2t?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2017, "同年参与《漂亮的李慧珍》等电视剧播出，形成多部剧集连续上线的职业节点。", "https://pdf.dfcfw.com/pdf/H2_AN201803281112863170_1.pdf?1647116576000.pdf="),
        CuratedCelebrityTimelineItem(2018, "获得中国电视金鹰奖观众喜爱的女演员和最具人气女演员等奖项。", "https://www.920603.com/"),
    ),
    "英格丽·褒曼" to listOf(
        CuratedCelebrityTimelineItem(1944, "凭《煤气灯下》获得奥斯卡最佳女主角。", "https://awardsdatabase.oscars.org/Search/GetResults?query=%7B%22Nominee%22%3A%22Ingrid+Bergman%22%2C%22Sort%22%3A%221-Nominee-Alpha%22%2C%22Search%22%3A%22Basic%22%7D"),
        CuratedCelebrityTimelineItem(1956, "凭《真假公主》再次获得奥斯卡最佳女主角。", "https://awardsdatabase.oscars.org/Search/GetResults?query=%7B%22Nominee%22%3A%22Ingrid+Bergman%22%2C%22Sort%22%3A%221-Nominee-Alpha%22%2C%22Search%22%3A%22Basic%22%7D"),
        CuratedCelebrityTimelineItem(1974, "凭《东方快车谋杀案》获得奥斯卡最佳女配角。", "https://www.oscars.org/oscars/ceremonies/1975/V"),
        CuratedCelebrityTimelineItem(1982, "去世；其欧洲与好莱坞表演作品持续进入电影史研究。", "https://awardsdatabase.oscars.org/Search/GetResults?query=%7B%22Nominee%22%3A%22Ingrid+Bergman%22%2C%22Sort%22%3A%221-Nominee-Alpha%22%2C%22Search%22%3A%22Basic%22%7D"),
    ),
    "费雯·丽" to listOf(
        CuratedCelebrityTimelineItem(1939, "在《乱世佳人》中饰演斯嘉丽，并获奥斯卡最佳女主角。", "https://www.oscars.org/oscars/ceremonies/1940"),
        CuratedCelebrityTimelineItem(1939, "影片《乱世佳人》以八项奥斯卡奖成为经典电影史节点。", "https://www.oscars.org/oscars/ceremonies/1940/memorable-moments"),
    ),
    "阮玲玉" to listOf(
        CuratedCelebrityTimelineItem(1926, "考入明星电影公司，首次登上银幕。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-03_newsletter/newsletter53_c.pdf"),
        CuratedCelebrityTimelineItem(1930, "加入联华公司，出演《故都春梦》，演艺事业进入重要阶段。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-03_newsletter/newsletter53_c.pdf"),
        CuratedCelebrityTimelineItem(1934, "主演《神女》，该片成为中国早期电影的重要作品。", "https://www.filmarchive.gov.hk/tc/web/hkfa/2026/film-marathon/pe-event-2026-film-marathon-fs-film01.html"),
        CuratedCelebrityTimelineItem(1935, "去世，年仅25岁；其作品与生涯持续被中国电影史研究和修复项目关注。", "https://www.filmarchive.gov.hk/documents/OLD-6.-Research-and-Publication/06-03_newsletter/newsletter90_e.pdf"),
    ),
    "陈道明" to listOf(
        CuratedCelebrityTimelineItem(1983, "毕业于中央戏剧学院，次年参加电影《一个和八个》演出。", "https://news.cctv.com/program/czwy/20030530/100547.shtml"),
        CuratedCelebrityTimelineItem(1995, "获全国“十大影星”称号，次年获中国电影家协会表演成就奖。", "https://news.cctv.com/program/czwy/20030530/100547.shtml"),
        CuratedCelebrityTimelineItem(2001, "主演电视剧《康熙王朝》，该剧于2001年12月首播。", "https://tv.cctv.com/v/vd/VIDA1355976058271582.html"),
    ),
    "王俊凯" to listOf(
        CuratedCelebrityTimelineItem(2013, "与王源、易烊千玺组成TFBOYS并宣布出道。", "https://shaoer.cctv.com/2017/01/25/ARTIfKxolbA2QoRpCEh8l8MZ170125.shtml"),
        CuratedCelebrityTimelineItem(2014, "随组合发行单曲《魔法城堡》，并获年度盛典奖项。", "https://tv.cctv.com/2017/06/19/VIDEn3MdEROehcY0YYuJExhm170619.shtml"),
        CuratedCelebrityTimelineItem(2016, "随组合参加央视春晚表演歌舞《幸福成长》。", "https://shaoer.cctv.com/special/tfboysqjl/index.shtml"),
        CuratedCelebrityTimelineItem(2016, "参与校园青春剧《我们的少年时代》拍摄并公布角色信息。", "https://ent.cctv.com/2016/10/20/ARTIRx4RCqK7aUHLT2QKh1HF161020.shtml"),
    ),
    "王源" to listOf(
        CuratedCelebrityTimelineItem(2013, "与王俊凯、易烊千玺组成TFBOYS并宣布出道。", "https://shaoer.cctv.com/2017/01/25/ARTIfKxolbA2QoRpCEh8l8MZ170125.shtml"),
        CuratedCelebrityTimelineItem(2014, "随组合发行单曲《魔法城堡》，并获年度盛典奖项。", "https://tv.cctv.com/2017/06/19/VIDEn3MdEROehcY0YYuJExhm170619.shtml"),
        CuratedCelebrityTimelineItem(2016, "随组合参加央视春晚表演歌舞《幸福成长》。", "https://shaoer.cctv.com/special/tfboysqjl/index.shtml"),
        CuratedCelebrityTimelineItem(2016, "参与校园青春剧《我们的少年时代》拍摄并公布角色信息。", "https://ent.cctv.com/2016/10/20/ARTIRx4RCqK7aUHLT2QKh1HF161020.shtml"),
    ),
    "李云迪" to listOf(
        CuratedCelebrityTimelineItem(2000, "获第14届肖邦国际钢琴比赛金奖，成为首位获此荣誉的中国钢琴家。", "https://pl.china-embassy.gov.cn/sghd/201510/t20151020_2173258.htm"),
        CuratedCelebrityTimelineItem(2001, "赴德国汉诺威音乐、戏剧学院深造。", "https://ltzy.chinagscourt.gov.cn/Show/12843"),
        CuratedCelebrityTimelineItem(2006, "完成汉诺威学习并举行回国后的首场国内音乐会。", "https://ltzy.chinagscourt.gov.cn/Show/12843"),
        CuratedCelebrityTimelineItem(2010, "受邀在华沙肖邦诞辰200周年音乐会演出。", "https://pl.china-embassy.gov.cn/ywzn/jlwh/jlda/201003/t20100324_2464687.htm"),
        CuratedCelebrityTimelineItem(2015, "受邀担任第17届肖邦国际钢琴比赛评委。", "https://pl.china-embassy.gov.cn/sghd/201510/t20151020_2173258.htm"),
    ),
    "孟小冬" to listOf(
        CuratedCelebrityTimelineItem(1919, "首次随女演员戏班赴无锡演出，以《逍遥津》登台。", "https://daj.wuxi.gov.cn/doc/2013/03/18/2425303.shtml"),
        CuratedCelebrityTimelineItem(1923, "赴北京演出，以《探母回令》在京津舞台走红。", "https://daj.wuxi.gov.cn/doc/2013/03/18/2425303.shtml"),
        CuratedCelebrityTimelineItem(1938, "拜余叔岩为师，系统学习余派剧目。", "https://daj.wuxi.gov.cn/doc/2013/03/18/2425303.shtml"),
        CuratedCelebrityTimelineItem(1947, "在上海留下《搜孤救孤》录音，成为研究其艺术的重要音响材料。", "https://mzj.sh.gov.cn/lnb-wh/20200518/MZ_LNB13_6712.html"),
        CuratedCelebrityTimelineItem(1977, "去世；其余派老生艺术和录音资料持续受到戏曲研究关注。", "https://daj.wuxi.gov.cn/doc/2013/03/18/2425303.shtml"),
    ),
    "杨小楼" to listOf(
        CuratedCelebrityTimelineItem(1937, "其创办的第一舞台因卢沟桥事变后北平沦陷而未能重建，成为北京近代剧场史的节点。", "https://www.gdwsw.gov.cn/wsgdlnfw/content/post_21993.html"),
        CuratedCelebrityTimelineItem(1940, "与周信芳、盖叫天等参加建造梨园坊义演。", "https://www.gaoyang.gov.cn/info_show.asp?infoid=101"),
        CuratedCelebrityTimelineItem(1944, "去世；其武生表演与剧场经营实践持续影响京剧艺术史研究。", "https://www.bjxch.gov.cn/file/20210224/1614153452088059150.pdf"),
    ),
    "罗纳尔多·纳扎里奥" to listOf(
        CuratedCelebrityTimelineItem(1994, "完成巴西国家队正式出场，并随队获得世界杯冠军。", "https://www.fifa.com/en/tournaments/mens/worldcup/articles/tribute-ronaldo-fenomeno-brazil-moments-goal-stats-videos"),
        CuratedCelebrityTimelineItem(1998, "随巴西队闯入世界杯决赛，成为球队重要前锋。", "https://inside.fifa.com/news/gerson-romario-was-way-better-than-ronaldo-2860332"),
        CuratedCelebrityTimelineItem(2002, "在世界杯决赛对德国梅开二度，帮助巴西夺冠；个人在该届赛事攻入8球。", "https://www.fifa.com/pt/articles/video-todos-gols-ronaldo-brasil-copa-do-mundo-1998-2002-2006"),
        CuratedCelebrityTimelineItem(2006, "参加第四届世界杯并打入个人世界杯第15球，成为巴西队世界杯历史射手。", "https://www.fifa.com/it/tournaments/mens/worldcup/canadamexicousa2026/articles/brasile-coppa-del-mondo-fifa-profilo-storia"),
    ),
    "胡蝶" to listOf(
        CuratedCelebrityTimelineItem(1935, "赴苏联参加莫斯科电影节，开启首次海外电影交流行程。", "https://www.filmarchive.gov.hk/documents/6.-Research-and-Publication/06-03_newsletter/newsletter53_c.pdf"),
        CuratedCelebrityTimelineItem(1939, "参与香港出品的国语片《貂蝉》与粤语片《南国姊妹花》，后者为馆藏最早粤语片之一。", "https://www.filmarchive.gov.hk/documents/18995340/19057015/hkfa_10th.pdf"),
        CuratedCelebrityTimelineItem(1960, "参演多部香港国语片，相关片目收录于香港电影资料馆放映及修复资料。", "https://www.filmarchive.gov.hk/documents/5.-Exhibition-and-Screening/ProFolio/ProFolio%20106_E.pdf"),
    ),
    "朱珪" to listOf(
        CuratedCelebrityTimelineItem(1768, "任山西按察使，进入省级司法监察体系。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668781/index.html"),
        CuratedCelebrityTimelineItem(1790, "任安徽巡抚，负责一省行政与民政事务。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668781/index.html"),
        CuratedCelebrityTimelineItem(1796, "任两广总督并兼署广东巡抚，主持岭南军政事务。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668781/index.html"),
        CuratedCelebrityTimelineItem(1799, "入京直南书房，继而担任上书房总师傅，参与嘉庆帝教育事务。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668781/index.html"),
        CuratedCelebrityTimelineItem(1805, "授体仁阁大学士，管理工部事务。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668781/index.html"),
        CuratedCelebrityTimelineItem(1806, "病逝；其仕途经历成为乾嘉之际政治与帝师制度的史料节点。", "https://www.bjdx.gov.cn/bjsdxqrmzf/zjdx/2050197/gjrw/668781/index.html"),
    ),
    "李东阳" to listOf(
        CuratedCelebrityTimelineItem(1464, "登进士第，选庶吉士，后授编修，开始翰林与朝廷仕宦生涯。", "https://www.dpm.org.cn/lemmas/240885.html"),
        CuratedCelebrityTimelineItem(1516, "去世；其诗文、书法及茶陵诗派的文学影响持续成为明代文学研究的重要议题。", "https://www.dpm.org.cn/lemmas/240885.html"),
    ),
    "诸葛亮" to listOf(
        CuratedCelebrityTimelineItem(207, "刘备三顾隆中，诸葛亮提出以荆、益为根基并联吴抗曹的战略构想，后世称《隆中对》。", "https://www.linyi.gov.cn/info/1036/52358.htm"),
        CuratedCelebrityTimelineItem(221, "刘备在成都建立蜀汉政权，诸葛亮受任丞相，主持朝政。", "https://www.linyi.gov.cn/info/1036/52358.htm"),
        CuratedCelebrityTimelineItem(227, "上《出师表》并率军驻汉中，展开北伐部署。", "https://www.linyi.gov.cn/info/1036/52358.htm"),
        CuratedCelebrityTimelineItem(234, "在五丈原军中病逝；刘禅后追谥为忠武侯。", "https://www.linyi.gov.cn/info/1036/52358.htm"),
    ),
    "纪昀" to listOf(
        CuratedCelebrityTimelineItem(1754, "中进士，改庶吉士并授翰林院编修，进入清廷文职体系。", "https://www.dpm.org.cn/court/figure/104017.html"),
        CuratedCelebrityTimelineItem(1768, "因牵连两淮盐政亏空案被发配乌鲁木齐；两年多后被召回中央文职机构。", "https://www.dpm.org.cn/court/figure/104017.html"),
        CuratedCelebrityTimelineItem(1773, "四库全书馆成立后，参与并主持长期编纂工作，整理乾隆以前重要典籍。", "https://www.shjjjc.gov.cn/shsjjjcw/ywyla/content/5b38edd3-dd52-4116-bde4-65291fdfedad.html"),
        CuratedCelebrityTimelineItem(1805, "在协办大学士任上病逝，谥文达。", "https://www.dpm.org.cn/court/figure/104017.html"),
    ),
    "赵孟頫" to listOf(
        CuratedCelebrityTimelineItem(1254, "生于吴兴；后成为元代书画艺术的重要代表人物。", "https://www.dpm.org.cn/subject_zhaomengfu/event.html"),
        CuratedCelebrityTimelineItem(1286, "受诏赴京，应召入元仕进。", "https://www.dpm.org.cn/subject_zhaomengfu/event.html"),
        CuratedCelebrityTimelineItem(1287, "获元世祖接见，授兵部郎中，主管驿站事务。", "https://www.dpm.org.cn/subject_zhaomengfu/event.html"),
        CuratedCelebrityTimelineItem(1322, "去世；其书画创作和“书画同源”主张持续影响后世。", "https://www.dpm.org.cn/lemmas/240325.html"),
    ),
    "胡宗宪" to listOf(
        CuratedCelebrityTimelineItem(1512, "生于徽州绩溪龙川，后以科举入仕。", "https://www.xuancheng.gov.cn/News/show/350131.html"),
        CuratedCelebrityTimelineItem(1555, "任浙江巡抚按御史，旋擢总督，统制七省军务抗倭。", "https://www.xuancheng.gov.cn/News/show/350131.html"),
        CuratedCelebrityTimelineItem(1565, "第二次入狱后病死狱中；其政治处境与严嵩父子牵连。", "https://www.xuancheng.gov.cn/News/show/350131.html"),
        CuratedCelebrityTimelineItem(1572, "获昭雪，并被史录平倭功绩。", "https://www.xuancheng.gov.cn/News/show/350131.html"),
    ),
    "杨继盛" to listOf(
        CuratedCelebrityTimelineItem(1547, "中进士，授南京吏部主事，进入仕途。", "https://www.xiongan.gov.cn/2019-02/14/c_1210059737.htm"),
        CuratedCelebrityTimelineItem(1550, "遭贬为狄道典史；在当地兴办学校、疏浚河道并推动民生治理。", "https://m.ahjjjc.gov.cn/p/59703.html"),
        CuratedCelebrityTimelineItem(1553, "上《请诛贼臣疏》，弹劾严嵩父子，被投入诏狱。", "https://www.jiwei.gov.cn/news_content.thtml?contentId=11294"),
        CuratedCelebrityTimelineItem(1555, "在狱中遇害，后谥忠愍。", "https://www.dcqjw.gov.cn/ljcq/lsbl/201403/t20140310_477573.html"),
    ),
    "蔡京" to listOf(
        CuratedCelebrityTimelineItem(1070, "中进士，出任钱塘尉、舒州推官等职，开始仕宦生涯。", "https://www.sxfj.gov.cn/jing_cai_zhuan_ti/267e63/10920649.shtml"),
        CuratedCelebrityTimelineItem(1102, "崇宁元年再被起用为相，成为徽宗朝最具影响力的大臣之一。", "https://www.sxfj.gov.cn/jing_cai_zhuan_ti/267e63/10920649.shtml"),
    ),
    "秦桧" to listOf(
        CuratedCelebrityTimelineItem(1131, "任尚书右仆射同平章事兼知枢密院事，提出对金和议主张。", "https://www.hncourt.gov.cn/public/detail.php?id=199775"),
        CuratedCelebrityTimelineItem(1142, "岳飞、岳云和张宪以“莫须有”罪名遇害；秦桧在案件中的角色构成其历史评价的核心争议。", "https://ytsc.rootinhenan.gov.cn/sitesources/ytsc/page_pc/hnly/xsgq/articleb3ae97e99dd04bbaa7911202bf5e877c.html"),
    ),
    "苏轼" to listOf(
        CuratedCelebrityTimelineItem(1057, "中进士，进入北宋仕宦与文坛。", "https://www.dpm.org.cn/lemmas/242068.html"),
        CuratedCelebrityTimelineItem(1097, "党争再起，被贬谪惠州、儋州。", "https://www.dpm.org.cn/lemmas/242068.html"),
        CuratedCelebrityTimelineItem(1101, "北返途中病逝常州，追谥文忠。", "https://www.dpm.org.cn/lemmas/242068.html"),
    ),
    "范仲淹" to listOf(
        CuratedCelebrityTimelineItem(989, "生于吴县，后以科举入仕。", "https://www.dpm.org.cn/lemmas/240582.html"),
        CuratedCelebrityTimelineItem(1052, "去世，赠兵部尚书，谥文正；《岳阳楼记》等文学作品持续广泛流传。", "https://www.dpm.org.cn/lemmas/240582.html"),
    ),
    "韩愈" to listOf(
        CuratedCelebrityTimelineItem(768, "生于唐代中期，后以古文写作与儒家思想阐释闻名。", "https://www.dpm.org.cn/lemmas/242854.html"),
        CuratedCelebrityTimelineItem(793, "二十五岁登进士第，进入仕宦生涯。", "https://www.dpm.org.cn/lemmas/242854.html"),
        CuratedCelebrityTimelineItem(824, "去世；其古文运动倡导与文集对后世散文和思想史影响深远。", "https://www.dpm.org.cn/lemmas/242854.html"),
    ),
    "黄庭坚" to listOf(
        CuratedCelebrityTimelineItem(1045, "生于洪州分宁，后成为北宋诗文和书法的重要人物。", "https://www.dpm.org.cn/lemmas/242585.html"),
        CuratedCelebrityTimelineItem(1067, "举进士，任校书郎并参与《神宗实录》检讨。", "https://www.dpm.org.cn/lemmas/242585.html"),
        CuratedCelebrityTimelineItem(1105, "在宜州去世；其诗歌与书法作品持续影响后世。", "https://www.dpm.org.cn/lemmas/242585.html"),
    ),
    "董其昌" to listOf(
        CuratedCelebrityTimelineItem(1555, "生于华亭，后成为晚明书画与鉴藏的重要人物。", "https://www.dpm.org.cn/lemmas/240247.html"),
        CuratedCelebrityTimelineItem(1588, "中进士，仕至礼部尚书。", "https://www.dpm.org.cn/lemmas/240247.html"),
        CuratedCelebrityTimelineItem(1636, "去世，谥文敏；其书画理论与松江画派持续影响后世。", "https://www.dpm.org.cn/lemmas/240247.html"),
    ),
    "黄宗羲" to listOf(
        CuratedCelebrityTimelineItem(1628, "父亲黄尊素冤案获平反后入京讼冤，并师从刘宗周。", "https://english.court.gov.cn/pdf/TheOriginofLaw.pdf"),
        CuratedCelebrityTimelineItem(1653, "返回故里讲学，屡拒清廷征召，转入长期著述。", "https://english.court.gov.cn/pdf/TheOriginofLaw.pdf"),
        CuratedCelebrityTimelineItem(1695, "去世；其经史研究与《明夷待访录》等著作持续受到思想史研究关注。", "https://www.zjskw.gov.cn/art/2023/12/28/art_1229515903_56480.html"),
    ),
    "郭沫若" to listOf(
        CuratedCelebrityTimelineItem(1914, "赴日本留学，后转向文学创作与新文化运动。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609355.shtml"),
        CuratedCelebrityTimelineItem(1921, "出版首部诗集《女神》，并与郁达夫等创立创造社。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609355.shtml"),
        CuratedCelebrityTimelineItem(1928, "旅居日本，集中从事中国古代史、甲骨文和金文研究。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609355.shtml"),
        CuratedCelebrityTimelineItem(1949, "参与新中国成立前后的文化与公共事务工作，后担任多项国家文化、科学机构职务。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609355.shtml"),
        CuratedCelebrityTimelineItem(1978, "去世；其文学、古史研究和公共活动仍是近现代文化史的重要研究对象。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609355.shtml"),
    ),
    "聂耳" to listOf(
        CuratedCelebrityTimelineItem(1912, "生于云南昆明，后成为中国近现代音乐的重要人物。", "https://www.ynds.yn.gov.cn/html/jinianhuodong/2020/8/18/2f5fa5ce-4972-4ee6-8a7d-dd13432969ea.html"),
        CuratedCelebrityTimelineItem(1930, "赴上海投身革命文艺活动，开始面向大众的音乐实践。", "https://www.ynds.yn.gov.cn/html/nieerweiguoerge/2026/1/30/d5ce7d7e-f81d-4fd6-9a67-9675da4088b7.html"),
        CuratedCelebrityTimelineItem(1933, "经田汉等介绍加入中国共产党，参与新兴音乐研究会等活动。", "https://www.ynds.yn.gov.cn/html/nieerweiguoerge/2026/1/30/d5ce7d7e-f81d-4fd6-9a67-9675da4088b7.html"),
        CuratedCelebrityTimelineItem(1935, "为《义勇军进行曲》谱曲，电影《风云儿女》上映后歌曲迅速传播。", "https://www.ynds.yn.gov.cn/html/jinianhuodong/2020/8/18/2f5fa5ce-4972-4ee6-8a7d-dd13432969ea.html"),
    ),
    "李现" to listOf(
        CuratedCelebrityTimelineItem(2010, "考入北京电影学院，开始接受表演专业训练。", "https://ent.people.com.cn/n1/2019/1217/c1012-31510044.html"),
        CuratedCelebrityTimelineItem(2012, "以电影《万箭穿心》完成早期银幕表演经历。", "https://ent.people.com.cn/n1/2019/1217/c1012-31510044.html"),
        CuratedCelebrityTimelineItem(2017, "主演网络剧《河神》，积累重要观众基础。", "https://ent.people.com.cn/n1/2019/1217/c1012-31510044.html"),
        CuratedCelebrityTimelineItem(2019, "电视剧《亲爱的，热爱的》播出后，公众关注度显著提升。", "https://media.people.com.cn/n1/2019/0722/c40606-31246851.html"),
    ),
    "陈伟霆" to listOf(
        CuratedCelebrityTimelineItem(2003, "参加全球华人新秀香港区选拔赛，进入演艺行业。", "https://www.iq.com/actor-info/%E9%99%88%E4%BC%9F%E9%9C%86-william-chan-200033205?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2008, "推出首张个人专辑《Will Power》，并获香港乐坛新人奖项。", "https://www.iq.com/actor-info/%E9%99%88%E4%BC%9F%E9%9C%86-william-chan-200033205?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2014, "主演《古剑奇谭》播出，获得更广泛电视剧观众认知。", "https://www.iq.com/actor-info/%E9%99%88%E4%BC%9F%E9%9C%86-william-chan-200033205?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2016, "领衔主演的《老九门》播出，并获爱奇艺尖叫之夜年度人物奖。", "https://www.iq.com/actor-info/%E9%99%88%E4%BC%9F%E9%9C%86-william-chan-200033205?lang=zh_cn"),
    ),
    "杨超越" to listOf(
        CuratedCelebrityTimelineItem(2017, "加入女子演唱组合CH2，并发行组合单曲。", "https://www.iq.com/actor-info/%E6%9D%A8%E8%B6%85%E8%B6%8A-yang-chaoyue-238591805?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2018, "参加《创造101》，随后以火箭少女101成员身份出道。", "https://www.iq.com/actor-info/%E6%9D%A8%E8%B6%85%E8%B6%8A-yang-chaoyue-238591805?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2019, "主演励志剧《极限17：羽你同行》播出。", "https://www.iq.com/actor-info/%E6%9D%A8%E8%B6%85%E8%B6%8A-yang-chaoyue-238591805?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2024, "首张个人EP《透明》上线，并获爱奇艺尖叫之夜戏剧单元年度十佳演员奖。", "https://www.iq.com/actor-info/%E6%9D%A8%E8%B6%85%E8%B6%8A-yang-chaoyue-238591805?lang=zh_cn"),
    ),
    "杨洋" to listOf(
        CuratedCelebrityTimelineItem(2007, "开始参与电视剧《红楼梦》拍摄，以成年贾宝玉角色进入演艺行业。", "https://www.iq.com/actor-info/%E6%9D%A8%E6%B4%8B-yang-yang-215007205?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2011, "参演电影《建党伟业》，拓展电影表演经历。", "https://www.iq.com/actor-info/%E6%9D%A8%E6%B4%8B-yang-yang-215007205?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2015, "凭《左耳》《盗墓笔记》《旋风少女》等作品获得更广泛关注。", "https://www.iq.com/actor-info/%E6%9D%A8%E6%B4%8B-yang-yang-215007205?lang=zh_cn"),
        CuratedCelebrityTimelineItem(2022, "主演《特战荣耀》《且试天下》，继续拓展军旅与古装题材。", "https://www.iq.com/actor-info/%E6%9D%A8%E6%B4%8B-yang-yang-215007205?lang=zh_cn"),
    ),
    "鹿晗" to listOf(
        CuratedCelebrityTimelineItem(2015, "回到内地发展，参演《重返20岁》《我是证人》等影视作品。", "https://paper.people.com.cn/hqrw/html/2016-03/16/content_1669634.htm"),
        CuratedCelebrityTimelineItem(2017, "主演电视剧《择天记》，继续在影视领域发展。", "https://media.people.com.cn/n1/2017/0426/c40606-29236055.html"),
        CuratedCelebrityTimelineItem(2020, "担任《创造营2020》教练，参与青年偶像培养类节目。", "https://jx.people.com.cn/n2/2020/0507/c392721-33998666.html"),
    ),
    "张艺兴" to listOf(
        CuratedCelebrityTimelineItem(2005, "参加《明星学院》比赛并获总决赛季军。", "https://m.1905.com/m/star/info/3043701/"),
        CuratedCelebrityTimelineItem(2012, "以EXO及EXO-M成员身份正式出道。", "https://m.1905.com/m/star/info/3043701/"),
        CuratedCelebrityTimelineItem(2015, "参加《极限挑战》，获得更广泛公众关注。", "https://m.1905.com/m/star/info/3043701/"),
        CuratedCelebrityTimelineItem(2016, "发行首张个人专辑《LOSE CONTROL》。", "https://m.1905.com/m/star/info/3043701/"),
        CuratedCelebrityTimelineItem(2023, "主演电影《孤注一掷》上映，并入职国家话剧院。", "https://m.1905.com/m/star/info/3043701/"),
    ),
    "张艺凡" to listOf(
        CuratedCelebrityTimelineItem(2024, "参演电视剧《江河之上》及电影《扫黑·决不放弃》《云边有个小卖部》。", "https://m.1905.com/m/star/info/3038890/"),
        CuratedCelebrityTimelineItem(2024, "获中国电影大数据暨电影频道M榜荣誉之夜年度新锐演员。", "https://m.1905.com/m/star/info/3038890/"),
    ),
    "王一博" to listOf(
        CuratedCelebrityTimelineItem(2014, "以UNIQ成员身份正式出道。", "https://m.1905.com/m/star/info/3045060/"),
        CuratedCelebrityTimelineItem(2016, "加入《天天向上》主持团“天天兄弟”。", "https://m.1905.com/m/star/info/3045060/"),
        CuratedCelebrityTimelineItem(2019, "主演《陈情令》播出，以蓝忘机一角获得广泛关注。", "https://m.1905.com/m/star/info/3045060/"),
        CuratedCelebrityTimelineItem(2023, "主演《无名》《长空之王》《热烈》等电影上映。", "https://m.1905.com/m/star/info/3045060/"),
        CuratedCelebrityTimelineItem(2024, "主演电视剧《追风者》播出，并有电影《维和防暴队》上映。", "https://m.1905.com/m/star/info/3045060/"),
    ),
    "檀健次" to listOf(
        CuratedCelebrityTimelineItem(2007, "主演首部电影《秘岸》，开始银幕表演。", "https://m.1905.com/m/star/info/2941757/"),
        CuratedCelebrityTimelineItem(2010, "以MIC男团成员身份正式出道。", "https://m.1905.com/m/star/info/2941757/"),
        CuratedCelebrityTimelineItem(2016, "主演《大军师司马懿之军师联盟》及续集，饰演司马昭。", "https://m.1905.com/m/star/info/2941757/"),
        CuratedCelebrityTimelineItem(2022, "领衔主演《猎罪图鉴》播出，以沈翊一角获得广泛关注。", "https://m.1905.com/m/star/info/2941757/"),
        CuratedCelebrityTimelineItem(2024, "主演《被我弄丢的你》，并继续主演《猎罪图鉴2》。", "https://m.1905.com/m/star/info/2941757/"),
    ),
    "白敬亭" to listOf(
        CuratedCelebrityTimelineItem(2014, "出演《匆匆那年》，以乔燃一角进入演艺圈。", "https://m.1905.com/m/star/info/3038775/"),
        CuratedCelebrityTimelineItem(2020, "主演职场剧《平凡的荣耀》播出。", "https://m.1905.com/m/star/info/3038775/"),
        CuratedCelebrityTimelineItem(2022, "主演《开端》《卿卿日常》相继播出。", "https://m.1905.com/m/star/info/3038775/"),
        CuratedCelebrityTimelineItem(2024, "主演《南来北往》播出，并获中国电视剧年度盛典年度突破男演员。", "https://m.1905.com/m/star/info/3038775/"),
    ),
    "虞书欣" to listOf(
        CuratedCelebrityTimelineItem(2016, "出演《新边城浪子》，进入演艺行业。", "https://m.1905.com/m/star/info/3164186/"),
        CuratedCelebrityTimelineItem(2020, "参加《青春有你第二季》，以第二名加入THE NINE。", "https://m.1905.com/m/star/info/3164186/"),
        CuratedCelebrityTimelineItem(2022, "主演《苍兰诀》，以小兰花一角获得广泛关注。", "https://m.1905.com/m/star/info/3164186/"),
        CuratedCelebrityTimelineItem(2024, "主演《永夜星河》，并发行首张个人正规专辑《Spicy Honey》。", "https://m.1905.com/m/star/info/3164186/"),
    ),
    "周也" to listOf(
        CuratedCelebrityTimelineItem(2019, "出演电影《少年的你》，开始进入公众电影视野。", "https://www.1905.com/mdb/star/3143866/"),
        CuratedCelebrityTimelineItem(2020, "凭《少年的你》获香港电影金像奖及亚洲电影大奖最佳女配角提名。", "https://www.1905.com/mdb/star/3143866/"),
        CuratedCelebrityTimelineItem(2023, "参演《孤注一掷》，继续参与商业电影项目。", "https://www.1905.com/mdb/star/3143866/"),
        CuratedCelebrityTimelineItem(2024, "主演《云边有个小卖部》，饰演程霜。", "https://www.1905.com/mdb/star/3143866/"),
    ),
    "张译" to listOf(
        CuratedCelebrityTimelineItem(2006, "出演《士兵突击》，以史今一角获得广泛关注。", "https://m.1905.com/m/star/info/621/"),
        CuratedCelebrityTimelineItem(2009, "主演《我的团长我的团》，持续积累电视剧表演口碑。", "https://m.1905.com/m/star/info/621/"),
        CuratedCelebrityTimelineItem(2014, "凭电影《亲爱的》获中国电影金鸡奖最佳男配角。", "https://m.1905.com/m/star/info/621/"),
        CuratedCelebrityTimelineItem(2017, "凭《鸡毛飞上天》获上海电视节白玉兰奖最佳男主角。", "https://m.1905.com/m/star/info/621/"),
        CuratedCelebrityTimelineItem(2023, "主演《狂飙》播出，以安欣一角再获广泛关注。", "https://m.1905.com/m/star/info/621/"),
    ),
    "吴磊" to listOf(
        CuratedCelebrityTimelineItem(2005, "出演《封神榜之凤鸣岐山》，以“小哪吒”开启演艺生涯。", "https://m.1905.com/m/star/info/4092/"),
        CuratedCelebrityTimelineItem(2015, "主演《旋风少女》并出演《琅琊榜》。", "https://m.1905.com/m/star/info/4092/"),
        CuratedCelebrityTimelineItem(2021, "主演《长歌行》，并主演电影《盛夏未来》。", "https://m.1905.com/m/star/info/4092/"),
        CuratedCelebrityTimelineItem(2022, "主演《星汉灿烂》，饰演凌不疑。", "https://m.1905.com/m/star/info/4092/"),
        CuratedCelebrityTimelineItem(2024, "主演《在暴雪时分》《草木人间》等影视作品。", "https://m.1905.com/m/star/info/4092/"),
    ),
    "刘昊然" to listOf(
        CuratedCelebrityTimelineItem(2014, "主演首部电影《北京爱情故事》上映。", "https://m.1905.com/m/star/info/3029970/"),
        CuratedCelebrityTimelineItem(2015, "主演《唐人街探案》，获华鼎奖最佳新人等奖项。", "https://m.1905.com/m/star/info/3029970/"),
        CuratedCelebrityTimelineItem(2018, "主演《唐人街探案2》，获百花奖最佳男主角提名。", "https://m.1905.com/m/star/info/3029970/"),
        CuratedCelebrityTimelineItem(2023, "主演《燃冬》入围戛纳电影节“一种关注”单元。", "https://m.1905.com/m/star/info/3029970/"),
        CuratedCelebrityTimelineItem(2024, "主演《解密》，获东京国际电影节中国电影周金鹤奖最佳男主角。", "https://m.1905.com/m/star/info/3029970/"),
    ),
    "张若昀" to listOf(
        CuratedCelebrityTimelineItem(2016, "出演《麻雀》并主演《法医秦明》，扩大电视剧观众认知。", "https://m.1905.com/m/star/info/3038880/"),
        CuratedCelebrityTimelineItem(2019, "主演《庆余年》，以范闲一角获得广泛关注。", "https://m.1905.com/m/star/info/3038880/"),
        CuratedCelebrityTimelineItem(2024, "《庆余年第二季》播出，再次饰演范闲并获白玉兰奖最佳男主角提名。", "https://www.1905.com/mdb/star/3038880/?fr=h5dsj-yr-2"),
    ),
    "谭松韵" to listOf(
        CuratedCelebrityTimelineItem(2001, "进入四川省舞蹈学校学习古典舞和民间舞。", "https://m.1905.com/m/star/info/7142/"),
        CuratedCelebrityTimelineItem(2012, "出演《后宫·甄嬛传》，以淳贵人一角受到关注。", "https://m.1905.com/m/star/info/7142/"),
        CuratedCelebrityTimelineItem(2016, "主演《最好的我们》，在青春题材剧集中形成代表角色。", "https://m.1905.com/m/star/info/7142/"),
        CuratedCelebrityTimelineItem(2020, "主演《以家人之名》，饰演李尖尖。", "https://m.1905.com/m/star/info/7142/"),
        CuratedCelebrityTimelineItem(2022, "主演《向风而行》，拓展都市职业题材表演。", "https://m.1905.com/m/star/info/7142/"),
    ),
    "彭德怀" to listOf(
        CuratedCelebrityTimelineItem(1916, "投身湘军当兵，开始军旅经历。", "https://cpc.people.com.cn/BIG5/64162/64165/70486/70515/4834478.html"),
        CuratedCelebrityTimelineItem(1928, "加入中国共产党，并领导平江起义、创建红五军。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977891.html"),
        CuratedCelebrityTimelineItem(1928, "率红五军向井冈山进军，与红四军会师。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977891.html"),
        CuratedCelebrityTimelineItem(1950, "担任中国人民志愿军司令员兼政治委员，赴朝鲜参加抗美援朝战争。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977891.html"),
    ),
    "陈云" to listOf(
        CuratedCelebrityTimelineItem(1925, "在上海商务印书馆工作期间加入中国共产党，开始参与工人运动。", "https://www.shqp.gov.cn/qszb/hsyj/20250606/1283600.html"),
        CuratedCelebrityTimelineItem(1927, "受中共江苏省委派遣回青浦，领导农民抗租斗争并参与农民武装活动。", "https://www.shqp.gov.cn/qszb/hsyj/20250606/1283600.html"),
        CuratedCelebrityTimelineItem(1935, "赴莫斯科向共产国际汇报遵义会议情况，后返回国内继续从事组织工作。", "https://cpc.people.com.cn/BIG5/143527/147176/index.html"),
        CuratedCelebrityTimelineItem(1950, "参与领导国家财经工作，推动稳定市场和恢复国民经济的相关工作。", "https://cpc.people.com.cn/BIG5/143527/147176/index.html"),
    ),
    "朱德" to listOf(
        CuratedCelebrityTimelineItem(1909, "进入云南陆军讲武堂学习，并加入中国同盟会。", "https://cpc.people.com.cn/GB/64162/126778/126780/7489937.html"),
        CuratedCelebrityTimelineItem(1926, "回国后继续参与革命活动。", "https://cpc.people.com.cn/GB/64162/126778/126780/7489937.html"),
        CuratedCelebrityTimelineItem(1928, "与陈毅率部到井冈山，同毛泽东领导的部队会师。", "https://cpc.people.com.cn/GB/64162/126778/126780/7489937.html"),
        CuratedCelebrityTimelineItem(1949, "中华人民共和国成立后，继续参与国家与军队公共事务工作。", "https://www.mod.gov.cn/gfbw/gfjy_index/js_214151/4888224.html"),
    ),
    "刘少奇" to listOf(
        CuratedCelebrityTimelineItem(1921, "赴苏联莫斯科东方共产主义劳动大学学习，并于同年加入中国共产党。", "https://cpc.people.com.cn/BIG5/64162/64165/72301/72329/5071384.html"),
        CuratedCelebrityTimelineItem(1922, "回国后参与安源路矿工人大罢工及工人组织工作。", "https://cpc.people.com.cn/BIG5/64162/64165/72301/72329/5071384.html"),
        CuratedCelebrityTimelineItem(1925, "当选全国总工会副委员长，参与工人运动。", "https://cpc.people.com.cn/BIG5/64162/64165/72301/72329/5071384.html"),
        CuratedCelebrityTimelineItem(1949, "中华人民共和国成立后，当选中央人民政府副主席，参与国家政治、经济、文化等方针政策工作。", "https://cpc.people.com.cn/BIG5/64162/64165/72301/72329/5071384.html"),
        CuratedCelebrityTimelineItem(1959, "当选中华人民共和国主席。", "https://cpc.people.com.cn/BIG5/64162/64165/72301/72329/5071384.html"),
    ),
    "宋庆龄" to listOf(
        CuratedCelebrityTimelineItem(1907, "赴美国求学，至1913年完成大学教育。", "https://www.sql.org.cn/gjjj/"),
        CuratedCelebrityTimelineItem(1915, "在日本东京与孙中山结婚。", "https://www.sql.org.cn/gjjj/"),
        CuratedCelebrityTimelineItem(1921, "在广州参加孙中山非常大总统就职仪式和市民庆祝活动。", "https://www.shsoong-chingling.com/list-life.html"),
        CuratedCelebrityTimelineItem(1926, "出席孙中山陵墓奠基典礼。", "https://www.shsoong-chingling.com/list-life.html"),
        CuratedCelebrityTimelineItem(1949, "赴北平参加中国人民政治协商会议第一届全体会议。", "https://www.shsoong-chingling.com/list-life.html"),
    ),
    "周恩来" to listOf(
        CuratedCelebrityTimelineItem(1917, "赴日本留学。", "https://www.cppcc.gov.cn/2011/09/20/ARTI1316515847871354.shtml"),
        CuratedCelebrityTimelineItem(1921, "转入中国共产党，并参与旅欧建党、建团工作。", "https://www.cppcc.gov.cn/2011/09/20/ARTI1316515847871354.shtml"),
        CuratedCelebrityTimelineItem(1924, "从巴黎回国，任黄埔军校政治部主任等职。", "https://www.cppcc.gov.cn/2011/09/20/ARTI1316515847871354.shtml"),
        CuratedCelebrityTimelineItem(1948, "参与领导辽沈、平津、淮海三大战役，并任中央军委副主席兼总参谋长。", "https://www.cppcc.gov.cn/2011/09/20/ARTI1316515847871354.shtml"),
        CuratedCelebrityTimelineItem(1954, "担任中华人民共和国国务院总理；此前兼任外交部长。", "https://www.fmprc.gov.cn/web/ziliao_674904/wjrw_674925/2166_674931/200805/t20080509_9880973.shtml"),
    ),
    "孙中山" to listOf(
        CuratedCelebrityTimelineItem(1892, "从香港西医书院毕业，后在澳门、广州等地行医。", "https://www.cppcc.gov.cn/2011/09/19/ARTI1316420681884963.shtml"),
        CuratedCelebrityTimelineItem(1894, "在檀香山建立兴中会，开始组织革命团体。", "https://www.cppcc.gov.cn/2011/09/19/ARTI1316420681884963.shtml"),
        CuratedCelebrityTimelineItem(1905, "在东京与黄兴等成立中国同盟会。", "https://www.zs.gov.cn/zjzs/zsmr/content/post_220072.html"),
        CuratedCelebrityTimelineItem(1911, "辛亥革命后被推举为中华民国临时大总统。", "https://www.cppcc.gov.cn/2011/09/19/ARTI1316420681884963.shtml"),
        CuratedCelebrityTimelineItem(1919, "将中华革命党改组为中国国民党，并发表《建国方略》等著作。", "https://www.zs.gov.cn/zjzs/zsmr/content/post_220072.html"),
    ),
    "陈毅" to listOf(
        CuratedCelebrityTimelineItem(1919, "赴法国勤工俭学，在旅欧期间开始接受马克思主义。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968868112696401920.html"),
        CuratedCelebrityTimelineItem(1923, "在北京中法大学学习期间转为中国共产党党员。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968868112696401920.html"),
        CuratedCelebrityTimelineItem(1928, "与朱德率部到井冈山会师，参与创建和保卫井冈山革命根据地。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968868112696401920.html"),
        CuratedCelebrityTimelineItem(1941, "皖南事变后参与重建新四军军部，并参与华中部队整编。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968868112696401920.html"),
        CuratedCelebrityTimelineItem(1958, "担任中华人民共和国外交部长，参与新中国外交工作。", "https://www.fmprc.gov.cn/eng/zy/wjrw/3606_665551/202405/t20240531_11367615.html"),
    ),
    "叶剑英" to listOf(
        CuratedCelebrityTimelineItem(1917, "进入云南陆军讲武学校学习，后投身民主革命。", "https://www.bjcc.gov.cn/article/160600209.html"),
        CuratedCelebrityTimelineItem(1924, "参与创建黄埔陆军军官学校，任教授部副主任。", "https://www.bjcc.gov.cn/article/160600209.html"),
        CuratedCelebrityTimelineItem(1948, "参与促成北平和平解放，并参与城市接管与恢复工作。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968866906561372160.html"),
        CuratedCelebrityTimelineItem(1969, "到湖南岳阳考察当地工厂、渔场与城镇情况。", "https://www.yysqw.gov.cn/43338/43340/content_2290968.html"),
        CuratedCelebrityTimelineItem(1972, "协助周恩来接待来华访问的美国和日本领导人，参与外交事务。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968866906561372160.html"),
    ),
    "胡耀邦" to listOf(
        CuratedCelebrityTimelineItem(1930, "参加中国工农红军，开始革命活动。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977919.html"),
        CuratedCelebrityTimelineItem(1949, "中华人民共和国成立后，长期从事青年与组织工作。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977919.html"),
        CuratedCelebrityTimelineItem(1975, "主持中国科学院日常工作，参与推进科技工作和国民经济整顿。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977919.html"),
        CuratedCelebrityTimelineItem(1982, "在中国共产党第十二次全国代表大会上作政治报告，并当选中央委员会总书记。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977919.html"),
    ),
    "李大钊" to listOf(
        CuratedCelebrityTimelineItem(1907, "考入天津北洋法政专门学校学习。", "https://www.chinamartyrs.gov.cn/wusi/bfzmyl/202404/t20240424_414852.html"),
        CuratedCelebrityTimelineItem(1913, "赴日本早稻田大学政治本科求学，开始接触社会主义思想。", "https://www.chinamartyrs.gov.cn/wusi/bfzmyl/202404/t20240424_414852.html"),
        CuratedCelebrityTimelineItem(1916, "中止在日学业回国，开始公共写作与社会活动。", "https://www.chinamartyrs.gov.cn/wusi/bfzmyl/202404/t20240424_414852.html"),
        CuratedCelebrityTimelineItem(1918, "发表《法俄革命之比较观》《庶民的胜利》等文章，参与新文化与思想传播。", "https://szw.yancheng.gov.cn/art/2014/1/7/art_532_551596.html"),
        CuratedCelebrityTimelineItem(1920, "在北京大学组织马克思学说研究会。", "https://szw.yancheng.gov.cn/art/2014/1/7/art_532_551596.html"),
    ),
    "瞿秋白" to listOf(
        CuratedCelebrityTimelineItem(1917, "到北京俄文专修馆学习。", "https://www.bjcc.gov.cn/article/600101567.html"),
        CuratedCelebrityTimelineItem(1920, "参加李大钊组织的马克思学说研究会。", "https://fzg.changzhou.gov.cn/html/fzg/2016/FNBDOFDF_0104/26148.html"),
        CuratedCelebrityTimelineItem(1922, "正式加入中国共产党。", "https://www.bjcc.gov.cn/article/600101567.html"),
        CuratedCelebrityTimelineItem(1927, "主持召开八七会议，参与起草相关文件。", "https://www.bjcc.gov.cn/article/600101567.html"),
        CuratedCelebrityTimelineItem(1928, "在中共六大作政治报告，并继续当选中央委员和政治局委员。", "https://fzg.changzhou.gov.cn/html/fzg/2016/FNBDOFDF_0104/26148.html"),
    ),
    "邓颖超" to listOf(
        CuratedCelebrityTimelineItem(1919, "参加五四爱国运动，组织天津女界爱国同志会并参与觉悟社。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203109.shtml"),
        CuratedCelebrityTimelineItem(1924, "参与组织天津社会主义青年团。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203109.shtml"),
        CuratedCelebrityTimelineItem(1925, "转为中国共产党党员，开展妇女与社会组织工作。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203109.shtml"),
        CuratedCelebrityTimelineItem(1946, "以中共代表团代表身份到重庆参加政治协商会议。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203109.shtml"),
        CuratedCelebrityTimelineItem(1949, "在中国妇女第一次全国代表大会上当选全国民主妇女联合会副主席。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203109.shtml"),
    ),
    "蔡和森" to listOf(
        CuratedCelebrityTimelineItem(1913, "进入湖南省立第一师范学习。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977884.html"),
        CuratedCelebrityTimelineItem(1918, "参与组织新民学会，参与筹办留法勤工俭学。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977884.html"),
        CuratedCelebrityTimelineItem(1920, "赴法国勤工俭学并从事理论与组织活动。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977884.html"),
        CuratedCelebrityTimelineItem(1922, "任中共中央机关报《向导》周报主编，开展新闻宣传工作。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977884.html"),
    ),
    "任弼时" to listOf(
        CuratedCelebrityTimelineItem(1920, "参加俄罗斯研究会，并加入中国社会主义青年团。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977908.html"),
        CuratedCelebrityTimelineItem(1921, "赴苏俄学习，成为早期赴苏俄学习的先进青年之一。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977908.html"),
        CuratedCelebrityTimelineItem(1934, "任中央代表、红六军团军政委员会主席，率部西征。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977908.html"),
        CuratedCelebrityTimelineItem(1949, "参与新中国成立前后的组织与公共事务工作。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977908.html"),
    ),
    "董必武" to listOf(
        CuratedCelebrityTimelineItem(1903, "参加黄安、黄州府科举考试并中秀才。", "https://www.ankang.gov.cn/Content-2243129.html"),
        CuratedCelebrityTimelineItem(1911, "武昌起义消息传到家乡后赴武汉参加辛亥革命。", "https://www.ankang.gov.cn/Content-2243129.html"),
        CuratedCelebrityTimelineItem(1933, "任马克思共产主义学校教务长、副校长。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197421328329.shtml"),
        CuratedCelebrityTimelineItem(1945, "代表中国解放区人民赴美国旧金山参加联合国制宪会议。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197421328329.shtml"),
        CuratedCelebrityTimelineItem(1959, "任中华人民共和国副主席、代主席，并参与法学与党史编纂工作。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197421328329.shtml"),
    ),
    "徐向前" to listOf(
        CuratedCelebrityTimelineItem(1924, "考入黄埔军校第一期学习。", "https://www.mod.gov.cn/gfbw/gfjy_index/js_214151/4850864.html"),
        CuratedCelebrityTimelineItem(1927, "加入中国共产党。", "https://www.mod.gov.cn/gfbw/gfjy_index/js_214151/4850864.html"),
        CuratedCelebrityTimelineItem(1937, "随八路军129师相关人员赴山西参与谈判与抗战工作。", "https://www.sxfj.gov.cn/jing_cai_zhuan_ti/267e63/10938888.shtml"),
        CuratedCelebrityTimelineItem(1955, "被授予中华人民共和国元帅军衔。", "https://www.mod.gov.cn/gfbw/gfjy_index/js_214151/4850864.html"),
    ),
    "张闻天" to listOf(
        CuratedCelebrityTimelineItem(1924, "到重庆任教，参与新文化、新思想传播并创办《南鸿》周刊。", "https://jda.cq.gov.cn/web/article/1464346427985453056/web/content_1464346427985453056.html"),
        CuratedCelebrityTimelineItem(1925, "加入中国共产党。", "https://jda.cq.gov.cn/web/article/1464346427985453056/web/content_1464346427985453056.html"),
        CuratedCelebrityTimelineItem(1934, "参加长征，途中出席遵义会议。", "https://www.wxlx.gov.cn/doc/2016/08/22/1956992.shtml"),
        CuratedCelebrityTimelineItem(1949, "中华人民共和国成立后，任驻苏联大使及外交部副部长等职。", "https://www.wxlx.gov.cn/doc/2016/08/22/1956992.shtml"),
    ),
    "聂荣臻" to listOf(
        CuratedCelebrityTimelineItem(1919, "赴法国勤工俭学，参与留法学生爱国运动。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968867198925332480.html"),
        CuratedCelebrityTimelineItem(1923, "转入中国共产党，并参与旅欧青年组织工作。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968867198925332480.html"),
        CuratedCelebrityTimelineItem(1949, "参与平津战役后北平和平解放的相关谈判与工作。", "https://scjgj.tl.gov.cn/tlsscjdglj/c00056/pc/content/content_1968867198925332480.html"),
        CuratedCelebrityTimelineItem(1959, "持续参与国防科技与相关公共事务工作。", "https://www.npc.gov.cn/zgrdw/subsite/zzs/site341/20100729/0021861abd660dbb753203.pdf"),
    ),
    "罗荣桓" to listOf(
        CuratedCelebrityTimelineItem(1919, "在长沙协均中学、青岛大学等校学习，参与反帝爱国活动。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977902.html"),
        CuratedCelebrityTimelineItem(1927, "参加秋收起义，三湾改编后任连党代表。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977902.html"),
        CuratedCelebrityTimelineItem(1949, "参与建立最高人民检察署并宣布其成立。", "https://www.spp.gov.cn/spp/zdgz/202105/t20210528_519390.shtml"),
        CuratedCelebrityTimelineItem(1954, "与陈毅主持起草《中国人民解放军政治工作条例》。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977902.html"),
    ),
    "彭真" to listOf(
        CuratedCelebrityTimelineItem(1922, "在太原山西省立第一中学参加青年学会，接受马克思主义。", "https://sfj.zhoukou.gov.cn/sitesources/sfj/page_pc/qmyfzs/fzxc/article9516016995d04cc1a3db5f582d529af1.html"),
        CuratedCelebrityTimelineItem(1923, "加入中国共产党，开始组织与工人运动工作。", "https://sfj.zhoukou.gov.cn/sitesources/sfj/page_pc/qmyfzs/fzxc/article9516016995d04cc1a3db5f582d529af1.html"),
        CuratedCelebrityTimelineItem(1982, "参与主持宪法修改草案的研究、讨论和起草工作。", "https://www.npc.gov.cn/WZWSREL25wYy9jMTI0MzQvYzE2MTE0L2MxNjExNS8yMDE5MDUvdDIwMTkwNTIxXzE5NTI5My5odG1s"),
    ),
    "李先念" to listOf(
        CuratedCelebrityTimelineItem(1926, "参加革命并从事农会工作。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203111.shtml"),
        CuratedCelebrityTimelineItem(1927, "参加黄麻起义并加入中国共产党。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203111.shtml"),
        CuratedCelebrityTimelineItem(1941, "任新四军第五师师长兼政治委员。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203111.shtml"),
        CuratedCelebrityTimelineItem(1954, "开始长期担任国务院副总理，并从事财政、财贸与计划工作。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203111.shtml"),
        CuratedCelebrityTimelineItem(1983, "当选中华人民共和国主席。", "https://www.cppcc.gov.cn/2011/06/27/ARTI1309159125203111.shtml"),
    ),
    "杨尚昆" to listOf(
        CuratedCelebrityTimelineItem(1925, "参加革命工作并加入中国共产主义青年团。", "https://www.cqtn.gov.cn/zjtn/tnmr/201509/t20150922_5937046_wap.html"),
        CuratedCelebrityTimelineItem(1926, "转为中国共产党党员，并进入上海大学学习，后赴莫斯科中山大学。", "https://www.cqtn.gov.cn/zjtn/tnmr/201509/t20150922_5937046_wap.html"),
        CuratedCelebrityTimelineItem(1931, "回国后从事工会宣传和组织工作。", "https://www.cqtn.gov.cn/zjtn/tnmr/201509/t20150922_5937046_wap.html"),
        CuratedCelebrityTimelineItem(1949, "新中国成立后任中央办公厅主任等职，参与国家公共事务。", "https://www.cqtn.gov.cn/zjtn/tnmr/201509/t20150922_5937046_wap.html"),
    ),
    "薄一波" to listOf(
        CuratedCelebrityTimelineItem(1925, "参加学生声援五卅爱国运动，并加入青年团。", "https://www.wzdj.gov.cn/system/2007/08/22/100383113.shtml"),
        CuratedCelebrityTimelineItem(1925, "转为中国共产党党员，组建学校党支部并任书记。", "https://www.wzdj.gov.cn/system/2007/08/22/100383113.shtml"),
        CuratedCelebrityTimelineItem(1937, "参与组建山西青年抗敌决死队并任政治委员。", "https://www.wzdj.gov.cn/system/2007/08/22/100383113.shtml"),
        CuratedCelebrityTimelineItem(1945, "任晋冀鲁豫中央局副书记和军区副政委，参与地方与军队工作。", "https://www.wzdj.gov.cn/system/2007/08/22/100383113.shtml"),
    ),
    "王稼祥" to listOf(
        CuratedCelebrityTimelineItem(1925, "进入上海大学附中学习，加入中国共产主义青年团并赴莫斯科中山大学。", "https://www.xuancheng.gov.cn/News/show/350190.html"),
        CuratedCelebrityTimelineItem(1928, "在莫斯科中山大学加入中国共产党，后进入红色教授学院深造。", "https://www.xuancheng.gov.cn/News/show/350190.html"),
        CuratedCelebrityTimelineItem(1930, "回国后在中共中央宣传部任职并参与报刊编辑。", "https://www.xuancheng.gov.cn/News/show/350190.html"),
        CuratedCelebrityTimelineItem(1943, "发表《中国共产党与中国民族解放的道路》，提出相关理论概念。", "https://www.jxjjjc.gov.cn/d/15238.html"),
    ),
    "林伯渠" to listOf(
        CuratedCelebrityTimelineItem(1904, "赴日本东京弘文学校留学。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977874.html"),
        CuratedCelebrityTimelineItem(1905, "加入中国同盟会，后返回湖南从事教育与革命活动。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977874.html"),
        CuratedCelebrityTimelineItem(1921, "加入上海共产主义小组，继续参与组织工作。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977874.html"),
        CuratedCelebrityTimelineItem(1948, "抵达西柏坡，承担筹备中国人民政治协商会议的相关工作。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977874.html"),
    ),
    "吴玉章" to listOf(
        CuratedCelebrityTimelineItem(1903, "赴日本留学，开始寻求救国道路。", "https://www.bjcc.gov.cn/article/600049705.html"),
        CuratedCelebrityTimelineItem(1905, "在东京加入中国同盟会。", "https://www.bjcc.gov.cn/article/600049705.html"),
        CuratedCelebrityTimelineItem(1912, "作为四川代表赴南京参加中华民国临时政府筹备工作。", "https://www.sqlzw.gov.cn/sitesources/jylzw/page_pc/gzdt/xcjy/articleb9b04d8f16ed4c859351818b6254012a.html"),
        CuratedCelebrityTimelineItem(1925, "加入中国共产党。", "https://www.bjcc.gov.cn/article/600049705.html"),
    ),
    "徐特立" to listOf(
        CuratedCelebrityTimelineItem(1911, "参加辛亥革命。", "https://www.enghunan.gov.cn/hneng/AboutHunan/HistoryCulture/Celebrities/201512/t20151211_1947220.html"),
        CuratedCelebrityTimelineItem(1927, "加入中国共产党，并参与南昌起义。", "https://www.enghunan.gov.cn/hneng/AboutHunan/HistoryCulture/Celebrities/201512/t20151211_1947220.html"),
        CuratedCelebrityTimelineItem(1934, "参加中国工农红军长征。", "https://www.enghunan.gov.cn/hneng/AboutHunan/HistoryCulture/Celebrities/201512/t20151211_1947220.html"),
        CuratedCelebrityTimelineItem(1941, "任延安自然科学院院长。", "https://www.qinfeng.gov.cn/info/1259/44479.htm"),
    ),
    "李维汉" to listOf(
        CuratedCelebrityTimelineItem(1918, "与毛泽东、蔡和森等发起成立新民学会。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200810/t20081017_1977964.html"),
        CuratedCelebrityTimelineItem(1919, "赴法国勤工俭学，开始接受马克思主义。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200810/t20081017_1977964.html"),
        CuratedCelebrityTimelineItem(1922, "参与组织旅欧少年中国共产党，并于年底加入中国共产党。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200810/t20081017_1977964.html"),
        CuratedCelebrityTimelineItem(1951, "作为中央人民政府首席全权代表参与相关谈判工作。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200810/t20081017_1977964.html"),
    ),
    "黄兴" to listOf(
        CuratedCelebrityTimelineItem(1902, "赴日本弘文学院学习，开始从事留学生组织与革命宣传活动。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200609/t20060928_1977842.html"),
        CuratedCelebrityTimelineItem(1904, "与宋教仁等在长沙创立华兴会。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200609/t20060928_1977842.html"),
        CuratedCelebrityTimelineItem(1905, "参与中国同盟会相关组织工作。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200609/t20060928_1977842.html"),
        CuratedCelebrityTimelineItem(1911, "辛亥革命爆发后参与武昌保卫战等军事事务。", "https://sdaj.hunan.gov.cn/wszt/xxsl/xxrw/200609/t20060928_1977842.html"),
        CuratedCelebrityTimelineItem(1913, "南京讨袁战事失利后由上海赴香港，后前往日本。", "https://www.mod.gov.cn/2018gfbztlbmb/2018-04/21/content_4810076.htm"),
    ),
    "宋教仁" to listOf(
        CuratedCelebrityTimelineItem(1904, "华兴会在长沙成立，任副会长；同年赴日学习法政。", "https://www.chinamartyrs.gov.cn/x_lsynml/dycgmzzsq/202003/t20200326_38362.html"),
        CuratedCelebrityTimelineItem(1905, "加入中国同盟会，任司法部检事长。", "https://www.chinamartyrs.gov.cn/x_lsynml/dycgmzzsq/202003/t20200326_38362.html"),
        CuratedCelebrityTimelineItem(1911, "武昌起义后参与建设民主共和政权并宣传革命宗旨。", "https://www.chinamartyrs.gov.cn/x_lsynml/dycgmzzsq/202003/t20200326_38362.html"),
        CuratedCelebrityTimelineItem(1912, "中华民国成立后任法制院院长，并参与国民党改组活动。", "https://www.chinamartyrs.gov.cn/x_lsynml/dycgmzzsq/202003/t20200326_38362.html"),
        CuratedCelebrityTimelineItem(1913, "3月在上海火车站遇刺，后不治身亡。", "https://www.chinamartyrs.gov.cn/x_lsynml/dycgmzzsq/202003/t20200326_38362.html"),
    ),
    "张謇" to listOf(
        CuratedCelebrityTimelineItem(1894, "考中状元，进入近代公共事务与社会建设领域。", "https://www.ccdi.gov.cn/yaowen/201507/t20150724_137208.html?from=singlemessage"),
        CuratedCelebrityTimelineItem(1895, "创办大生纱厂，开展近代实业建设。", "https://www.ccdi.gov.cn/yaowen/201507/t20150724_137208.html?from=singlemessage"),
        CuratedCelebrityTimelineItem(1905, "创办南通博物苑。", "https://www.ccdi.gov.cn/yaowen/201507/t20150724_137208.html?from=singlemessage"),
        CuratedCelebrityTimelineItem(1906, "创办通州师范学校，推进地方教育建设。", "https://www.ccdi.gov.cn/yaowen/201507/t20150724_137208.html?from=singlemessage"),
        CuratedCelebrityTimelineItem(1915, "参与地方自治与社会事业建设，相关实践持续展开。", "https://www.ccdi.gov.cn/yaowen/201507/t20150724_137208.html?from=singlemessage"),
    ),
    "梁启超" to listOf(
        CuratedCelebrityTimelineItem(1895, "参与公车上书，进入维新政治活动。", "https://www.counsellor.gov.cn/2021-07/07/c_1211231153.htm"),
        CuratedCelebrityTimelineItem(1898, "维新变法失败后赴日，继续从事报刊与启蒙宣传。", "https://www.xinhui.gov.cn/zlxh/lsrw/xhmr/content/post_3307422.html"),
        CuratedCelebrityTimelineItem(1899, "创办《清议报》，持续以报刊参与公共论述。", "https://www.xinhui.gov.cn/zlxh/lsrw/xhmr/content/post_3307422.html"),
        CuratedCelebrityTimelineItem(1925, "受聘清华学校国学研究院导师，继续从事教育与学术研究。", "https://www.counsellor.gov.cn/2021-07/07/c_1211231153.htm"),
        CuratedCelebrityTimelineItem(1929, "在北平病逝；《饮冰室合集》等著述成为研究其思想与学术的重要文献。", "https://www.xinhui.gov.cn/zlxh/lsrw/xhmr/content/post_3307422.html"),
    ),
    "张澜" to listOf(
        CuratedCelebrityTimelineItem(1902, "赴日留学，开始接触维新思想并关注国是。", "https://hubeimm.gov.cn/index.php?id=11816"),
        CuratedCelebrityTimelineItem(1911, "参与并领导四川保路运动相关工作。", "https://hubeimm.gov.cn/index.php?id=11816"),
        CuratedCelebrityTimelineItem(1920, "以四川省长名义协调川籍学生救济与留法教育基金。", "https://hubeimm.gov.cn/index.php?id=11816"),
        CuratedCelebrityTimelineItem(1944, "参与民主党派组织相关工作。", "https://tzb.changzhou.gov.cn/uploadfile/tzb/2022/0607/20220607112158_97578.pdf"),
        CuratedCelebrityTimelineItem(1955, "在北京逝世。", "https://hubeimm.gov.cn/index.php?id=11816"),
    ),
    "沈钧儒" to listOf(
        CuratedCelebrityTimelineItem(1905, "赴日留学，进入东京法政大学速成科学习。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609357.shtml"),
        CuratedCelebrityTimelineItem(1912, "加入中国同盟会，参与法政与公共事务工作。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609357.shtml"),
        CuratedCelebrityTimelineItem(1935, "参与发起上海文化界救国会并任主席。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609357.shtml"),
        CuratedCelebrityTimelineItem(1936, "与救国会其他领导人被捕，形成“七君子之狱”。", "https://www.cppcc.gov.cn/2011/09/28/ARTI1317197444609357.shtml"),
        CuratedCelebrityTimelineItem(1949, "参与新政协筹备与第一届全体会议相关工作。", "https://paper.minmengsh.gov.cn/resfile/2025-01-12/03/shmx-20250112-003.pdf"),
    ),
    "于右任" to listOf(
        CuratedCelebrityTimelineItem(1895, "以第一名成绩考入县学，开始系统求学。", "https://www.ylq.gov.cn/zjyl/mlyl/rwjg/jdmr/1906937799159377921.html"),
        CuratedCelebrityTimelineItem(1906, "赴日考察新闻并募集办报经费，加入中国同盟会。", "https://www.ylq.gov.cn/zjyl/mlyl/rwjg/jdmr/1906937799159377921.html"),
        CuratedCelebrityTimelineItem(1907, "在上海创办《神州日报》等报刊，参与革命宣传。", "https://www.zjda.gov.cn/col/col1402600/index.html"),
        CuratedCelebrityTimelineItem(1912, "在南京临时政府及陕西相关公共事务中任职。", "https://www.zjda.gov.cn/col/col1402600/index.html"),
        CuratedCelebrityTimelineItem(1922, "参与创办上海大学，继续从事教育工作。", "https://www.zjda.gov.cn/col/col1402600/index.html"),
    ),
    "马叙伦" to listOf(
        CuratedCelebrityTimelineItem(1911, "经章太炎介绍加入中国同盟会。", "https://zjjcmspublic.oss-cn-hangzhou-zwynet-d01-a.internet.cloud.zj.gov.cn/jcms_files/jcms1/web2971/site/attach/0/qikan/200403/htm/new_page_13.htm"),
        CuratedCelebrityTimelineItem(1945, "与文化教育出版和工商界人士组织成立中国民主促进会。", "https://www.kdl.gov.cn/detail/cid/1813/aid/111604"),
        CuratedCelebrityTimelineItem(1948, "与其他民主人士进入东北解放区，参与新政协筹备相关协商。", "https://www.kdl.gov.cn/detail/cid/1813/aid/111604"),
        CuratedCelebrityTimelineItem(1949, "参加开国大典，后参与新中国文化教育公共事务。", "https://www.kdl.gov.cn/detail/cid/1813/aid/111604"),
        CuratedCelebrityTimelineItem(1958, "病中留下关于人生道路的书法绝笔。", "https://www.kdl.gov.cn/detail/cid/1813/aid/111604"),
    ),
    "李济深" to listOf(
        CuratedCelebrityTimelineItem(1914, "毕业于北京陆军大学，留校任教官。", "https://www.cppcc.gov.cn/2011/09/26/ARTI1317001118828577.shtml"),
        CuratedCelebrityTimelineItem(1919, "在广州参加孙中山领导的军政府和护法运动。", "https://www.cppcc.gov.cn/2011/09/26/ARTI1317001118828577.shtml"),
        CuratedCelebrityTimelineItem(1933, "与蔡廷锴、陈铭枢等在福州组织抗日反蒋的中华共和国人民革命政府。", "https://www.cppcc.gov.cn/2011/09/26/ARTI1317001118828577.shtml"),
        CuratedCelebrityTimelineItem(1948, "在香港参与发起成立中国国民党革命委员会并当选主席。", "https://www.cppcc.gov.cn/2011/09/26/ARTI1317001118828577.shtml"),
        CuratedCelebrityTimelineItem(1949, "参与第一届中国人民政治协商会议和新中国公共事务。", "https://www.cppcc.gov.cn/2011/09/26/ARTI1317001118828577.shtml"),
    ),
    "何香凝" to listOf(
        CuratedCelebrityTimelineItem(1902, "与廖仲恺赴日本留学，开始参与留学生爱国活动。", "https://qwgzyj.gqb.gov.cn/qwhg/167/2070.shtml"),
        CuratedCelebrityTimelineItem(1905, "参加中国同盟会，成为其首位女会员。", "https://mg.wuxi.gov.cn/doc/2013/05/08/931834.shtml"),
        CuratedCelebrityTimelineItem(1942, "香港沦陷后在相关帮助下抵达广东海丰。", "https://www.hbmg.gov.cn/infor/html/7959.html"),
        CuratedCelebrityTimelineItem(1949, "参与新中国筹建，后任国家华侨事务委员会主任委员。", "https://qwgzyj.gqb.gov.cn/qwhg/167/2070.shtml"),
        CuratedCelebrityTimelineItem(1951, "参与中国国民党革命委员会相关领导工作。", "https://mg.wuxi.gov.cn/doc/2013/05/08/931834.shtml"),
    ),
    "张治中" to listOf(
        CuratedCelebrityTimelineItem(1916, "毕业后进入安徽安武军，后赴广州参与护法运动。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977879.html"),
        CuratedCelebrityTimelineItem(1924, "在黄埔军校及广州卫戍司令部从事教育和军政工作。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977879.html"),
        CuratedCelebrityTimelineItem(1937, "在上海参与淞沪会战，后任湖南省政府主席。", "https://tyjrswj.cq.gov.cn/martyrs_memorial/ylfc_2022/ylsj_2022/202506/t20250626_14750266_wap.html"),
        CuratedCelebrityTimelineItem(1949, "作为国民党和平谈判代表团首席代表赴北平参与谈判，后留在北平。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977879.html"),
        CuratedCelebrityTimelineItem(1954, "当选第一届全国人大常委会委员、国防委员会副主席。", "https://sdaj.hunan.gov.cn/sdaj/wszt/xxsl/xxrw/200609/t20060928_1977879.html"),
    ),
    "邓演达" to listOf(
        CuratedCelebrityTimelineItem(1909, "考入广东陆军小学堂学习。", "https://www.hbng.gov.cn/index.php?id=1582"),
        CuratedCelebrityTimelineItem(1911, "加入中国同盟会，后参与辛亥革命相关行动。", "https://www.mod.gov.cn/gfbw/zt/gfbwzt/2018_213791/mhyxlsp/4821729.html"),
        CuratedCelebrityTimelineItem(1919, "从保定陆军军官学校毕业，后加入援闽粤军。", "https://www.mod.gov.cn/gfbw/zt/gfbwzt/2018_213791/mhyxlsp/4821729.html"),
        CuratedCelebrityTimelineItem(1927, "参与武汉时期军事与农民问题相关公共事务。", "https://www.hbzx.gov.cn/49/2014-09-15/5790.html"),
        CuratedCelebrityTimelineItem(1930, "在上海主持成立中国农工民主党前身。", "https://www.mod.gov.cn/gfbw/zt/gfbwzt/2018_213791/mhyxlsp/4821729.html"),
    ),
    "段永平" to listOf(
        CuratedCelebrityTimelineItem(1978, "进入浙江大学无线电工程学系学习。", "https://baike.sogou.com/m/fullLemma?lid=7662908"),
        CuratedCelebrityTimelineItem(1988, "进入中山怡华集团下属日华电子厂，参与消费电子企业经营。", "https://www.sohu.com/a/425269329_609541"),
        CuratedCelebrityTimelineItem(1995, "创办步步高，继续开展消费电子企业经营。", "https://xueqiu.com/1720046137/237500710"),
        CuratedCelebrityTimelineItem(2001, "移居美国后持续参与企业投资与公益活动。", "https://xueqiu.com/1720046137/237500710"),
    ),
    "但斌" to listOf(
        CuratedCelebrityTimelineItem(1992, "进入深圳证券投资领域，开始从事研究与投资工作。", "https://www.7hcn.com/article/162760-1.html"),
        CuratedCelebrityTimelineItem(1993, "经历股票和国债期货投资实践，投资方法逐渐转向价值投资。", "https://www.7hcn.com/article/162760-1.html"),
        CuratedCelebrityTimelineItem(2004, "创建深圳市东方港湾投资管理有限公司。", "https://www.7hcn.com/article/162760-1.html"),
        CuratedCelebrityTimelineItem(2007, "出版《时间的玫瑰——但斌投资札记》。", "https://finance.ifeng.com/people/comchief/danbin.shtml"),
    ),
    "查理·芒格" to listOf(
        CuratedCelebrityTimelineItem(1948, "毕业于哈佛法学院，后开始法律职业生涯。", "https://keyt.com/news/national-world/cnn-national/2023/12/04/charlie-munger-fast-facts/"),
        CuratedCelebrityTimelineItem(1962, "共同创办Munger, Tolles & Olson律师事务所，并开展投资管理。", "https://www.mto.com/news/in-memoriam-charlie-munger-1924-2023/"),
        CuratedCelebrityTimelineItem(1978, "出任伯克希尔·哈撒韦副董事长。", "https://keyt.com/news/national-world/cnn-national/2023/12/04/charlie-munger-fast-facts/"),
        CuratedCelebrityTimelineItem(2023, "在加利福尼亚州去世，结束长期企业与投资管理生涯。", "https://www.mto.com/news/in-memoriam-charlie-munger-1924-2023/"),
    ),
    "约翰·邓普顿" to listOf(
        CuratedCelebrityTimelineItem(1934, "从耶鲁大学毕业，后获罗德奖学金赴牛津求学。", "https://www.templeton.org/about/sir-john-templeton"),
        CuratedCelebrityTimelineItem(1954, "进入共同基金领域并创建Templeton Growth Fund。", "https://www.templeton.org/about/sir-john-templeton"),
        CuratedCelebrityTimelineItem(1972, "设立邓普顿奖。", "https://www.templeton.org/about/sir-john-templeton"),
        CuratedCelebrityTimelineItem(1987, "设立约翰·邓普顿基金会。", "https://www.templeton.org/about/sir-john-templeton"),
    ),
    "塞思·卡拉曼" to listOf(
        CuratedCelebrityTimelineItem(1982, "创立Baupost Group，开始长期资产管理实践。", "https://assetsv2.nyra.com/filer_public/e9/d1/e9d18ed6-2dd8-465a-a238-357fa3047bd9/mediaguide2025_web_8_23.pdf"),
        CuratedCelebrityTimelineItem(1991, "出版《Margin of Safety》，系统阐述价值投资与风险控制观点。", "https://books.google.com/books/about/Margin_of_Safety.html?id=1QpjAAAACAAJ"),
        CuratedCelebrityTimelineItem(2025, "公开资料继续列其为Baupost Group负责人。", "https://assetsv2.nyra.com/filer_public/e9/d1/e9d18ed6-2dd8-465a-a238-357fa3047bd9/mediaguide2025_web_8_23.pdf"),
    ),
    "本杰明·格雷厄姆" to listOf(
        CuratedCelebrityTimelineItem(1914, "从哥伦比亚学院毕业，并获多学科教职邀请。", "https://c250.columbia.edu/c250_celebrates/your_columbians/benjamin_graham.html"),
        CuratedCelebrityTimelineItem(1928, "开始在哥伦比亚任教，后长期从事金融与投资教育。", "https://c250.columbia.edu/c250_celebrates/your_columbians/benjamin_graham.html"),
        CuratedCelebrityTimelineItem(1934, "与大卫·多德合著《Security Analysis》。", "https://books.google.com/books/about/Security_Analysis.html?id=8eVSAAAAMAAJ"),
        CuratedCelebrityTimelineItem(1949, "出版《The Intelligent Investor》，持续影响投资教育。", "https://www.betterworldbooks.com/author/benjamin-graham/788186"),
    ),
    "菲利普·费雪" to listOf(
        CuratedCelebrityTimelineItem(1928, "开始证券分析职业生涯。", "https://www.plindia.com/samplereports/acumenphilipfisher.pdf"),
        CuratedCelebrityTimelineItem(1931, "创立Fisher & Co.，开展长期投资管理。", "https://www.plindia.com/samplereports/acumenphilipfisher.pdf"),
        CuratedCelebrityTimelineItem(1958, "出版《Common Stocks and Uncommon Profits》。", "https://www.plindia.com/samplereports/acumenphilipfisher.pdf"),
        CuratedCelebrityTimelineItem(2004, "在旧金山去世，结束长期投资管理生涯。", "https://studylib.net/doc/28361897/common-stocks-and-uncommon-profits-and-oth---philip-a-fisher"),
    ),
    "乔尔·格林布拉特" to listOf(
        CuratedCelebrityTimelineItem(1997, "出版《You Can Be a Stock Market Genius》。", "https://www.encyclopedia.com/arts/educational-magazines/greenblatt-joel-1957"),
        CuratedCelebrityTimelineItem(2006, "出版《The Little Book That Beats the Market》。", "https://www.encyclopedia.com/arts/educational-magazines/greenblatt-joel-1957"),
    ),
    "彼得·林奇" to listOf(
        CuratedCelebrityTimelineItem(1974, "出任富达研究主管。", "https://www.amacad.org/person/peter-s-lynch"),
        CuratedCelebrityTimelineItem(1977, "开始管理富达麦哲伦基金。", "https://www.amacad.org/person/peter-s-lynch"),
        CuratedCelebrityTimelineItem(1989, "出版《One Up on Wall Street》。", "https://www.plindia.com/samplereports/acumenseries2-peterlynch28march2020.pdf"),
        CuratedCelebrityTimelineItem(1990, "结束麦哲伦基金管理工作。", "https://www.amacad.org/person/peter-s-lynch"),
    ),
    "迈克尔·伯里" to listOf(
        CuratedCelebrityTimelineItem(1997, "获范德堡大学医学博士学位。", "https://leaders.com/rankings/person/michael-burry/"),
        CuratedCelebrityTimelineItem(2000, "创建Scion Capital，开始资产管理实践。", "https://www.thefamouspeople.com/profiles/michael-burry-51881.php"),
        CuratedCelebrityTimelineItem(2007, "围绕次级抵押贷款市场风险建立研究和交易。", "https://leaders.com/rankings/person/michael-burry/"),
    ),
    "莫尼什·帕布莱" to listOf(
        CuratedCelebrityTimelineItem(1991, "创建科技咨询与系统集成公司TransTech。", "https://prabook.com/web/mohnish.pabrai/2084819"),
        CuratedCelebrityTimelineItem(1999, "创建Pabrai Investment Funds。", "https://prabook.com/web/mohnish.pabrai/2084819"),
        CuratedCelebrityTimelineItem(2000, "出售TransTech，转向投资管理工作。", "https://prabook.com/web/mohnish.pabrai/2084819"),
    ),
    "黄景仁" to listOf(
        CuratedCelebrityTimelineItem(1749, "生于高淳，后成为清代诗人。", "https://www.changzhou.gov.cn/ns_news/667156992117782"),
        CuratedCelebrityTimelineItem(1766, "为谋生开始四方奔波，长期在幕府、书院等处辗转。", "https://www.changzhou.gov.cn/ns_news/667156992117782"),
        CuratedCelebrityTimelineItem(1781, "被任命为县丞，但仕途与生计仍多困顿。", "https://www.changzhou.gov.cn/ns_news/667156992117782"),
        CuratedCelebrityTimelineItem(1783, "病逝；《两当轩集》等作品成为研究其诗歌的重要文本。", "https://fzg.changzhou.gov.cn/html/fzg/2015/FNBDOFDF_1222/24062.html"),
    ),
    "邵雍" to listOf(
        CuratedCelebrityTimelineItem(1011, "生于北宋天禧年间，后以《易》学与诗文研究闻名。", "https://ct.xinxiang.gov.cn/ggfw/fwzwhyc/135965.html"),
        CuratedCelebrityTimelineItem(1077, "去世；《皇极经世》《观物内外篇》《击壤集》等著作流传。", "https://ct.xinxiang.gov.cn/ggfw/fwzwhyc/135965.html"),
    ),
    "谢枋得" to listOf(
        CuratedCelebrityTimelineItem(1256, "中进士，与文天祥同科，后任抚州司户参军。", "https://wwj.beijing.gov.cn/bjww/resource/cms/article/bjww_362762/10837017/2020073115111634980.pdf"),
        CuratedCelebrityTimelineItem(1275, "以江东提举、江西招谕使身份知信州，组织抗元。", "https://wwj.beijing.gov.cn/bjww/resource/cms/article/bjww_362762/10837017/2020073115111634980.pdf"),
        CuratedCelebrityTimelineItem(1289, "被强行押至大都后绝食而逝，后获谥文节。", "https://wwj.beijing.gov.cn/bjww/resource/cms/article/bjww_362762/10837017/2020073115111634980.pdf"),
    ),
    "袁宗道" to listOf(
        CuratedCelebrityTimelineItem(1560, "生于湖北公安，后与两位弟弟共同形成公安派文学群体。", "https://www.lhlzw.gov.cn/sitesources/nysjwjw/page_pc/xcjd/s/article94f413b9efc040b4bb6e9ed96ff12fc9.html"),
        CuratedCelebrityTimelineItem(1600, "去世；其与袁宏道、袁中道的文学理论和创作，成为晚明反复古思潮的重要部分。", "https://www.hbzx.gov.cn/49/2014-09-15/5791.html"),
    ),
    "李成梁" to listOf(
        CuratedCelebrityTimelineItem(1566, "袭铁岭卫指挥佥事，因战功升辽东险山参将。", "https://www.dpm.org.cn/court/figure/104040.html"),
        CuratedCelebrityTimelineItem(1570, "署理辽东总兵官，整顿军务、选拔将校。", "https://www.dpm.org.cn/court/figure/104040.html"),
        CuratedCelebrityTimelineItem(1571, "在卓山指挥夹击土蛮部，获升迁。", "https://www.dpm.org.cn/court/figure/104040.html"),
        CuratedCelebrityTimelineItem(1591, "因战败隐瞒等问题遭弹劾，十一月解任。", "https://www.dpm.org.cn/court/figure/104040.html"),
        CuratedCelebrityTimelineItem(1601, "应请复镇辽东，面对建州女真势力上升转攻为守。", "https://www.dpm.org.cn/court/figure/104040.html"),
        CuratedCelebrityTimelineItem(1615, "去世；其辽东军政遗产在明末边防史中持续存在争议。", "https://www.dpm.org.cn/court/figure/104040.html"),
    ),
    "洪亮吉" to listOf(
        CuratedCelebrityTimelineItem(1746, "生于江苏阳湖，后从事诗文、史地与方志研究。", "https://fzg.changzhou.gov.cn/html/fzg/2016/NPIEOFQM_0829/32506.html"),
        CuratedCelebrityTimelineItem(1790, "考中进士，任翰林院编修。", "https://www.gzszx.gov.cn/gzzxb/web/doc/detail/d_1643438216839200"),
        CuratedCelebrityTimelineItem(1792, "任贵州学政，至1795年间推进地方文教并完成多部著述。", "https://www.gzszx.gov.cn/gzzxb/web/doc/detail/d_1643438216839200"),
        CuratedCelebrityTimelineItem(1799, "因上书直陈时政被革职，后发配伊犁，随后获赦归乡。", "https://fzg.changzhou.gov.cn/html/fzg/2016/NPIEOFQM_0829/32506.html"),
        CuratedCelebrityTimelineItem(1809, "去世；其人口、史地与方志研究持续受到学界关注。", "https://fzg.changzhou.gov.cn/html/fzg/2016/NPIEOFQM_0829/32506.html"),
    ),
    "毕沅" to listOf(
        CuratedCelebrityTimelineItem(1730, "生于江苏太仓，后成为清代官员、学者。", "https://zjjcmspublic.oss-cn-hangzhou-zwynet-d01-a.internet.cloud.zj.gov.cn/jcms_files/jcms1/web3192/site/attach/szb/szyj/szkw/201507/P020150721383203266902.pdf"),
        CuratedCelebrityTimelineItem(1760, "考中状元，进入仕途。", "https://www.jssjw.cn/art/2016/12/27/art_437_31492.html"),
        CuratedCelebrityTimelineItem(1785, "任河南巡抚时拜谒苏轼墓冢并作祭文，反映其金石、文献兴趣。", "https://www.jssjw.cn/art/2016/12/27/art_437_31492.html"),
        CuratedCelebrityTimelineItem(1797, "去世；其《续资治通鉴》等著作成为清代史学整理的重要成果。", "https://zjjcmspublic.oss-cn-hangzhou-zwynet-d01-a.internet.cloud.zj.gov.cn/jcms_files/jcms1/web3192/site/attach/szb/szyj/szkw/201507/P020150721383203266902.pdf"),
    ),
    "史浩" to listOf(
        CuratedCelebrityTimelineItem(1163, "隆兴元年拜相，并推荐张浚参与恢复中原的朝廷讨论。", "https://www.zjsjw.gov.cn/zhuantizhuanlan/qinglianwenhua/jiaguijiaxun/202104/t20210401_3889098_ext.html"),
    ),
    "胡林翼" to listOf(
        CuratedCelebrityTimelineItem(1812, "生于湖南益阳，后成为晚清军政人物。", "https://www.hnhs.gov.cn/22556/22562/content_1186169.html"),
        CuratedCelebrityTimelineItem(1836, "中进士，选庶吉士，后授翰林院编修。", "https://www.sxfj.gov.cn/gong_zuo_dong_tai/tu_pian_xin_wen/10982052.shtml"),
        CuratedCelebrityTimelineItem(1847, "赴贵州任职，署理安顺知府，开始长期地方治理工作。", "https://www.gzszx.gov.cn/gzzxb/web/doc/detail/1946/A3"),
        CuratedCelebrityTimelineItem(1856, "调任湖北巡抚，参与清军对太平天国的军事与地方治理。", "https://www.hnhs.gov.cn/22556/22562/content_1186169.html"),
        CuratedCelebrityTimelineItem(1861, "病逝于安徽军营，获追赠并谥文忠。", "https://www.hnhs.gov.cn/22556/22562/content_1186169.html"),
    ),
    "熊廷弼" to listOf(
        CuratedCelebrityTimelineItem(1625, "因辽东军务与广宁失守等问题被处死，辽东防务争议随之加深。", "https://www.whzx.gov.cn/zxzl/wsl/whwszl/202301/P020240129730658098465.pdf"),
        CuratedCelebrityTimelineItem(1629, "崇祯帝准其归葬并为案件平反，谥襄愍。", "https://www.whzx.gov.cn/zxzl/wsl/whwszl/202301/P020240129730658098465.pdf"),
    ),
    "杨镐" to listOf(
        CuratedCelebrityTimelineItem(1597, "奉命经略援朝，参与万历朝鲜战争的明军部署。", "https://www.dpm.org.cn/lemmas/244893.html"),
        CuratedCelebrityTimelineItem(1598, "蔚山战事失利后，以弃军丧师罪被罢免。", "https://www.dpm.org.cn/lemmas/244893.html"),
        CuratedCelebrityTimelineItem(1619, "受命经略辽东军务；萨尔浒之战后因指挥失误被逮下狱。", "https://www.dpm.org.cn/lemmas/244893.html"),
        CuratedCelebrityTimelineItem(1629, "被处死；其军事决策与责任归属成为明末辽东史的重要议题。", "https://www.dpm.org.cn/lemmas/244893.html"),
    ),
    "张三丰" to listOf(
        CuratedCelebrityTimelineItem(1391, "据《明史·方伎传》记载，明太祖曾遣使寻访，未得其踪。", "https://ctext.org/wiki.pl?chapter=979959&if=gb&remap=gb"),
        CuratedCelebrityTimelineItem(1460, "天顺三年，明英宗赐诰赠“通微显化真人”；其生卒与行迹仍不可确考。", "https://ctext.org/wiki.pl?chapter=979959&if=gb&remap=gb"),
    ),
    "雷军" to listOf(
        CuratedCelebrityTimelineItem(1992, "加入金山软件，开始在软件产业的职业生涯。", "https://ir.mi.com/zh-hans/board-member-management/leijun"),
        CuratedCelebrityTimelineItem(2000, "创办卓越网，进入互联网零售创业领域。", "https://ir.mi.com/zh-hans/board-member-management/leijun"),
        CuratedCelebrityTimelineItem(2004, "卓越网被亚马逊收购，完成该阶段创业的资本退出。", "https://ir.mi.com/zh-hans/board-member-management/leijun"),
        CuratedCelebrityTimelineItem(2010, "参与创办小米公司，开启智能手机与消费电子创业阶段。", "https://www.mi.com/ir"),
        CuratedCelebrityTimelineItem(2018, "小米集团在香港联交所上市，企业进入公开市场阶段。", "https://ir.mi.com/zh-hans/corporate-information/company-profile"),
    ),
    "屠呦呦" to listOf(
        CuratedCelebrityTimelineItem(1951, "考入北京医学院药学系，开始系统学习药学。", "https://www.most.gov.cn/ztzl/tyy/rwjs/201510/t20151006_121867.html"),
        CuratedCelebrityTimelineItem(1955, "毕业后进入卫生部中医研究院工作。", "https://www.most.gov.cn/ztzl/tyy/rwjs/201510/t20151006_121867.html"),
        CuratedCelebrityTimelineItem(1969, "以科研组长身份加入“523 项目”，开始抗疟新药研究。", "https://www.most.gov.cn/ztzl/tyy/rwjs/201510/t20151006_121867.html"),
        CuratedCelebrityTimelineItem(1972, "团队成功提取青蒿素晶体，攻克抗疟研究的关键环节。", "https://www.xinhuanet.com/politics/2019-11/15/c_1210355338.htm"),
        CuratedCelebrityTimelineItem(2011, "因发现青蒿素等抗疟药物获得拉斯克临床医学奖。", "https://www.most.gov.cn/ztzl/gjkxjsjldh/jldh2016/2016zgj/201701/t20170105_130123.html"),
        CuratedCelebrityTimelineItem(2015, "获得诺贝尔生理学或医学奖。", "https://www.nobelprize.org/prizes/medicine/2015/tu/biographical/"),
        CuratedCelebrityTimelineItem(2016, "获得国家最高科学技术奖。", "https://www.most.gov.cn/ztzl/gjkxjsjldh/jldh2016/2016zgj/201701/t20170105_130123.html"),
    ),
    "袁隆平" to listOf(
        CuratedCelebrityTimelineItem(1953, "毕业于西南农学院，后从事农业教学与研究。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(1964, "开始研究杂交水稻。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(1973, "实现三系配套，形成杂交水稻育种的重要技术路径。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(1974, "育成第一个杂交水稻强优组合南优 2 号。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(1995, "当选中国工程院院士，并研制成功两系杂交水稻。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(1997, "提出超级杂交稻育种技术路线。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(2000, "实现中国超级稻育种第一期目标。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
        CuratedCelebrityTimelineItem(2021, "在长沙逝世；其杂交水稻研究与推广持续影响农业科技。", "https://www.cae.cn/cae/html/main/colys/84368456.html"),
    ),
    "马云" to listOf(
        CuratedCelebrityTimelineItem(1995, "接触互联网后在杭州创办“中国黄页”，进行早期互联网创业尝试。", "https://xmwb.xinmin.cn/home/resfile/2013-05-22/16/16.pdf"),
        CuratedCelebrityTimelineItem(1999, "与伙伴在杭州创办阿里巴巴，面向中小企业建立网络贸易平台。", "https://www.alibabagroup.com/zh-HK/faqs-corporate-information"),
        CuratedCelebrityTimelineItem(2010, "阿里巴巴开始试行合伙人制度，推进创始人文化向合伙人治理结构演进。", "https://www.alibabagroup.com/zh-HK/document-1491503555327033344"),
        CuratedCelebrityTimelineItem(2014, "阿里巴巴集团在美国纽约证券交易所上市，进入国际公开资本市场。", "https://www.alibabagroup.com/zh-HK/faqs-investor-information"),
        CuratedCelebrityTimelineItem(2019, "不再担任阿里巴巴集团董事局主席，企业进入新的管理交接阶段。", "https://esg.alibabagroup.com/ui/pdfs/Alibaba-ESG-Report-2018-Letter-from-the-Chairman.pdf"),
    ),
    "莫言" to listOf(
        CuratedCelebrityTimelineItem(1981, "发表小说《春夜雨霏霏》，开始进入当代文学创作视野。", "https://www.nobelprize.org/prizes/literature/2012/yan/biographical/"),
        CuratedCelebrityTimelineItem(1986, "发表《红高粱家族》，以高密东北乡等叙事空间形成鲜明创作辨识度。", "https://www.nobelprize.org/prizes/literature/2012/yan/biographical/"),
        CuratedCelebrityTimelineItem(2011, "小说《蛙》获第八届茅盾文学奖。", "https://www.zgbk.com/ecph/words?ID=130262&SiteID=1"),
        CuratedCelebrityTimelineItem(2012, "获诺贝尔文学奖，成为首位获该奖项的中国籍作家。", "https://www.nobelprize.org/prizes/literature/2012/press-release/"),
    ),
    "张艺谋" to listOf(
        CuratedCelebrityTimelineItem(1984, "担任《黄土地》摄影，进入中国电影创作的重要阶段。", "https://www.zgbk.com/ecph/words?ID=89294&SiteID=1"),
        CuratedCelebrityTimelineItem(1987, "执导《红高粱》，开启导演代表作阶段。", "https://www.zgbk.com/ecph/words?ID=89294&SiteID=1"),
        CuratedCelebrityTimelineItem(2008, "参与北京奥运会开闭幕式相关创作工作，拓展大型文化活动视觉表达。", "https://www.zgbk.com/ecph/words?ID=89294&SiteID=1"),
    ),
    "周杰伦" to listOf(
        CuratedCelebrityTimelineItem(2000, "发行首张个人专辑《Jay》，以创作歌手身份进入华语流行音乐市场。", "https://www.1905.com/mdb/star/239/"),
        CuratedCelebrityTimelineItem(2007, "自编自导并主演电影《不能说的秘密》，扩展至电影创作。", "https://www.1905.com/mdb/star/239/"),
        CuratedCelebrityTimelineItem(2010, "发行专辑《跨时代》，持续推进词曲创作、制作与舞台表演的综合路线。", "https://www.1905.com/mdb/star/239/"),
    ),
)

private fun String.toCelebrityLifeOverview(): String =
    split('；')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filterNot { clause ->
            listOf("时刻", "候选", "校时", "出生证", "资料等级", "来源等级", "来源链")
                .any(clause::contains)
        }
        .filterNot { clause ->
            listOf("资料包", "资料点评", "本例", "本条", "此处", "条目")
                .any(clause::startsWith)
        }
        .joinToString("；")

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
    AppDestination.BaziCompatibility,
    AppDestination.BaziCompatibilityReport,
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
    putString("compatibilityLeftCaseId", compatibilityLeftCaseId)
    putString("compatibilityRightCaseId", compatibilityRightCaseId)
    putString("compatibilityParticipantRole", compatibilityParticipantRole?.name)
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
        compatibilityLeftCaseId = getString("compatibilityLeftCaseId"),
        compatibilityRightCaseId = getString("compatibilityRightCaseId"),
        compatibilityParticipantRole = enumValueOrNull<SexForFortuneDirection>(
            getString("compatibilityParticipantRole"),
        ),
        compatibilityLoading = destination == AppDestination.BaziCompatibility,
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
        AppDestination.BaziCompatibility -> putString("type", "bazi_compatibility")
        AppDestination.BaziCompatibilityReport -> putString("type", "bazi_compatibility")
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
        "bazi_compatibility" -> AppDestination.BaziCompatibility
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
