package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import java.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StageTwoSavedStateTest {
    @Test
    fun savedStateRestoresFourPillarsLookupFormAndReloadsCandidates() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<NanfengBaziApplication>()
        val container = application.container
        val original = createViewModel(container)
        original.openCreate()
        original.openFourPillarsLookup()
        original.updateFourPillarsLookupForm {
            it.copy(
                yearPillar = "己丑",
                monthPillar = "癸酉",
                dayPillar = "甲子",
                hourPillar = "壬申",
                startYear = "1949",
                endYear = "1949",
                timeZoneId = "Asia/Shanghai",
                ratHourRule = RatHourRule.TYME_DEFAULT,
            )
        }
        original.searchFourPillars()
        waitUntil {
            !original.state.value.fourPillarsLookupLoading &&
                original.state.value.fourPillarsLookupCandidates.isNotEmpty()
        }
        val handle = SavedStateHandle()
        original.saveRestorableStateTo(handle)

        val restored = createViewModel(container, handle)
        waitUntil {
            !restored.state.value.fourPillarsLookupLoading &&
                restored.state.value.fourPillarsLookupCandidates.isNotEmpty()
        }

        assertEquals(AppDestination.FourPillarsLookup, restored.state.value.destination)
        assertEquals("己丑", restored.state.value.fourPillarsLookupForm.yearPillar)
        assertEquals("1949", restored.state.value.fourPillarsLookupForm.startYear)
        assertEquals("Asia/Shanghai", restored.state.value.fourPillarsLookupForm.timeZoneId)
        assertNotNull(restored.state.value.fourPillarsLookupEvidence)
        restored.navigateBack()
        assertEquals(AppDestination.CreateCase, restored.state.value.destination)
    }

    @Test
    fun savedStateRestoresCreateDraftAndCaseBoundEditor() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<NanfengBaziApplication>()
        val container = application.container
        val original = createViewModel(container)
        val alias = "状态恢复-${System.currentTimeMillis()}"

        original.updateQuery("待研究")
        original.openCreate()
        original.updateForm {
            it.copy(
                alias = alias,
                name = "合成状态样例",
                sex = SexForFortuneDirection.MAN,
                year = "1992",
                month = "8",
                day = "24",
                hour = "12",
                minute = "0",
                second = "0",
                locationName = "江苏省宿迁市泗阳县",
                sourceNote = "进程重建前保留",
                ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
            )
        }
        val createHandle = SavedStateHandle()
        original.saveRestorableStateTo(createHandle)
        val restoredCreate = createViewModel(container, createHandle)

        assertEquals(AppDestination.CreateCase, restoredCreate.state.value.destination)
        assertEquals("待研究", restoredCreate.state.value.query)
        assertEquals(alias, restoredCreate.state.value.form.alias)
        assertEquals("进程重建前保留", restoredCreate.state.value.form.sourceNote)
        assertEquals(
            RatHourRule.LATE_RAT_SAME_DAY,
            restoredCreate.state.value.form.ratHourRule,
        )

        restoredCreate.submitCase(allowDuplicate = true)
        waitUntil {
            restoredCreate.state.value.destination == AppDestination.CaseList &&
                !restoredCreate.state.value.saving
        }
        restoredCreate.updateQuery("")
        waitUntil {
            restoredCreate.state.value.cases.any { it.alias == alias }
        }
        val caseId = requireNotNull(
            restoredCreate.state.value.cases.firstOrNull { it.alias == alias }?.id,
        )
        restoredCreate.openDetail(caseId)
        waitUntil { restoredCreate.state.value.detail?.id == caseId }
        restoredCreate.updateFortuneObservationTime("23:15")
        restoredCreate.selectDetailSection(CaseDetailSection.RECORDS)
        restoredCreate.openTextRecord()
        restoredCreate.updateRecordDraft {
            it.copy(
                type = CaseTextRecordType.ANALYSIS,
                content = "尚未保存的记录草稿",
                analysisCategory = AnalysisCategory.CAREER,
            )
        }
        val editorHandle = SavedStateHandle()
        restoredCreate.saveRestorableStateTo(editorHandle)
        val restoredEditor = createViewModel(container, editorHandle)
        waitUntil { restoredEditor.state.value.detail?.id == caseId }

        assertEquals(
            AppDestination.EditTextRecord(caseId, null),
            restoredEditor.state.value.destination,
        )
        assertEquals(
            "尚未保存的记录草稿",
            restoredEditor.state.value.recordDraft.content,
        )
        assertEquals(CaseDetailSection.RECORDS, restoredEditor.state.value.detailSection)
        assertEquals("23:15", restoredEditor.state.value.fortuneObservationTime)
        assertNotNull(restoredEditor.state.value.detail)
        restoredEditor.navigateBack()
        assertEquals(
            AppDestination.CaseDetail(caseId),
            restoredEditor.state.value.destination,
        )

        restoredEditor.openObjectiveSummary()
        waitUntil { restoredEditor.state.value.objectiveSummary != null }
        val summaryHandle = SavedStateHandle()
        restoredEditor.saveRestorableStateTo(summaryHandle)
        val restoredSummary = createViewModel(container, summaryHandle)
        waitUntil { restoredSummary.state.value.objectiveSummary != null }
        assertEquals(
            AppDestination.CaseObjectiveSummary(caseId),
            restoredSummary.state.value.destination,
        )
        assertEquals(
            restoredEditor.state.value.objectiveSummary?.copyText,
            restoredSummary.state.value.objectiveSummary?.copyText,
        )
        restoredSummary.navigateBack()
        assertEquals(
            AppDestination.CaseDetail(caseId),
            restoredSummary.state.value.destination,
        )

        restoredSummary.duplicateCase()
        waitUntil {
            val detailId = restoredSummary.state.value.detail?.id
            detailId != null && detailId != caseId && !restoredSummary.state.value.mutationSaving
        }
        restoredSummary.backToList()
        restoredSummary.openCaseComparison()
        waitUntil { restoredSummary.state.value.comparisonReport != null }
        val comparisonHandle = SavedStateHandle()
        restoredSummary.saveRestorableStateTo(comparisonHandle)
        val restoredComparison = createViewModel(container, comparisonHandle)
        waitUntil { restoredComparison.state.value.comparisonReport != null }

        assertEquals(
            AppDestination.CaseComparison,
            restoredComparison.state.value.destination,
        )
        assertNotNull(restoredComparison.state.value.comparisonLeftCaseId)
        assertNotNull(restoredComparison.state.value.comparisonRightCaseId)
        assertNotNull(restoredComparison.state.value.comparisonReport)
    }

    @Test
    fun savedStateRestoresCommentaryCandidateDecisionsAndFormalAdoption() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<NanfengBaziApplication>()
        val container = application.container
        val original = createViewModel(container)
        val alias = "点评候选状态-${System.currentTimeMillis()}"
        original.openCreate()
        original.updateForm {
            it.copy(
                alias = alias,
                name = "合成点评候选样例",
                sex = SexForFortuneDirection.WOMAN,
                year = "1996",
                month = "8",
                day = "15",
                hour = "9",
                minute = "20",
                second = "0",
                locationName = "合成点评候选地区",
            )
        }
        original.submitCase(allowDuplicate = true)
        waitUntil("保存合成命例") {
            original.state.value.destination == AppDestination.CaseList &&
                !original.state.value.saving
        }
        original.updateQuery("")
        waitUntil("列表读取合成命例") {
            original.state.value.cases.any { it.alias == alias }
        }
        val caseId = requireNotNull(
            original.state.value.cases.firstOrNull { it.alias == alias }?.id,
        )
        original.openDetail(caseId)
        waitUntil("读取合成命例详情") { original.state.value.detail?.id == caseId }
        original.openTextRecord()
        original.updateRecordDraft {
            it.copy(
                type = CaseTextRecordType.MASTER_COMMENTARY,
                content = "事业需要人工核对。财运也需要人工核对",
            )
        }
        original.saveTextRecord(null)
        waitUntil("保存师傅点评") {
            original.state.value.destination == AppDestination.CaseDetail(caseId) &&
                original.state.value.detail?.textRecords?.isNotEmpty() == true
        }
        val commentary = requireNotNull(
            original.state.value.detail?.textRecords?.firstOrNull {
                it.type == CaseTextRecordType.MASTER_COMMENTARY
            },
        )
        original.openMasterCommentaryCandidates(commentary.id)
        val initial = requireNotNull(original.state.value.commentaryCandidateSet)
        val firstId = initial.candidates.first().id
        val secondId = initial.candidates.last().id
        original.updateMasterCommentaryCandidateContent(firstId, "状态恢复后的正式分析")
        original.rejectMasterCommentaryCandidate(secondId)
        val handle = SavedStateHandle()
        original.saveRestorableStateTo(handle)

        val restored = createViewModel(container, handle)
        waitUntil("恢复点评候选") {
            restored.state.value.commentaryCandidateSet != null &&
                restored.state.value.detail?.id == caseId &&
                !restored.state.value.detailLoading
        }
        assertEquals(
            AppDestination.MasterCommentaryCandidates(caseId, commentary.id),
            restored.state.value.destination,
        )
        val restoredCandidates =
            requireNotNull(restored.state.value.commentaryCandidateSet).candidates
        assertEquals("状态恢复后的正式分析", restoredCandidates.first().proposedContent)
        assertEquals(
            MasterCommentaryCandidateStatus.REJECTED,
            restoredCandidates.last().status,
        )

        restored.adoptMasterCommentaryCandidate(firstId)
        waitUntil("采用点评候选") {
            restored.state.value.commentaryCandidateSavingId == null &&
                (
                    restored.state.value.commentaryCandidateSet
                        ?.candidates
                        ?.first()
                        ?.status == MasterCommentaryCandidateStatus.ADOPTED ||
                        restored.state.value.commentaryCandidateAdoptionFailure != null
                    )
        }
        assertEquals(null, restored.state.value.commentaryCandidateAdoptionFailure)
        val detail = requireNotNull(restored.state.value.detail)
        assertEquals(
            "事业需要人工核对。财运也需要人工核对",
            detail.textRecords.single {
                it.type == CaseTextRecordType.MASTER_COMMENTARY
            }.content,
        )
        assertEquals(
            "状态恢复后的正式分析",
            detail.textRecords.single {
                it.type == CaseTextRecordType.ANALYSIS
            }.content,
        )
    }

    @Test
    fun savedStateRestoresFeedbackThemeDecisionsAndFormalAdoption() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<NanfengBaziApplication>()
        val container = application.container
        val original = createViewModel(container)
        val alias = "反馈主题状态-${System.currentTimeMillis()}"
        original.openCreate()
        original.updateForm {
            it.copy(
                alias = alias,
                name = "合成反馈主题样例",
                sex = SexForFortuneDirection.MAN,
                year = "1994",
                month = "3",
                day = "8",
                hour = "10",
                minute = "10",
                second = "0",
                locationName = "合成反馈主题地区",
            )
        }
        original.submitCase(allowDuplicate = true)
        waitUntil("保存反馈主题合成命例") {
            original.state.value.destination == AppDestination.CaseList &&
                !original.state.value.saving
        }
        original.updateQuery("")
        waitUntil("读取反馈主题合成命例") {
            original.state.value.cases.any { it.alias == alias }
        }
        val caseId = requireNotNull(
            original.state.value.cases.firstOrNull { it.alias == alias }?.id,
        )
        original.openDetail(caseId)
        waitUntil("读取反馈主题详情") { original.state.value.detail?.id == caseId }
        original.openTextRecord()
        original.updateRecordDraft {
            it.copy(
                type = CaseTextRecordType.OWNER_FEEDBACK,
                content = "工作有变化。健康需要复查",
            )
        }
        original.saveTextRecord(null)
        waitUntil("保存命主反馈") {
            original.state.value.destination == AppDestination.CaseDetail(caseId) &&
                original.state.value.detail?.textRecords?.isNotEmpty() == true
        }
        val feedback = requireNotNull(
            original.state.value.detail?.textRecords?.firstOrNull {
                it.type == CaseTextRecordType.OWNER_FEEDBACK
            },
        )
        original.openFeedbackThemeCandidates(feedback.id)
        val initial = requireNotNull(original.state.value.feedbackThemeCandidateSet)
        val firstId = initial.candidates.first().id
        val secondId = initial.candidates.last().id
        original.updateFeedbackThemeCandidateTag(firstId, "事业复盘")
        original.rejectFeedbackThemeCandidate(secondId)
        val handle = SavedStateHandle()
        original.saveRestorableStateTo(handle)

        val restored = createViewModel(container, handle)
        waitUntil("恢复反馈主题候选") {
            restored.state.value.feedbackThemeCandidateSet != null &&
                restored.state.value.detail?.id == caseId &&
                !restored.state.value.detailLoading
        }
        assertEquals(
            AppDestination.FeedbackThemeCandidates(caseId, feedback.id),
            restored.state.value.destination,
        )
        val restoredCandidates =
            requireNotNull(restored.state.value.feedbackThemeCandidateSet).candidates
        assertEquals("事业复盘", restoredCandidates.first().proposedTagName)
        assertEquals(
            FeedbackThemeCandidateStatus.REJECTED,
            restoredCandidates.last().status,
        )

        restored.adoptFeedbackThemeCandidate(firstId)
        waitUntil("采用反馈主题候选") {
            restored.state.value.feedbackThemeSavingId == null &&
                (
                    restored.state.value.feedbackThemeCandidateSet
                        ?.candidates
                        ?.first()
                        ?.status == FeedbackThemeCandidateStatus.ADOPTED ||
                        restored.state.value.feedbackThemeAdoptionFailure != null
                    )
        }
        assertEquals(null, restored.state.value.feedbackThemeAdoptionFailure)
        val detail = requireNotNull(restored.state.value.detail)
        assertEquals(
            "工作有变化。健康需要复查",
            detail.textRecords.single {
                it.type == CaseTextRecordType.OWNER_FEEDBACK
            }.content,
        )
        assertEquals(listOf("事业复盘"), detail.tags.map { it.name })
        assertEquals(0, detail.events.size)
    }

    private suspend fun waitUntil(
        description: String = "状态满足",
        condition: () -> Boolean,
    ) {
        try {
            withTimeout(15_000) {
                while (!condition()) delay(50)
            }
        } catch (_: TimeoutCancellationException) {
            throw AssertionError("等待超时：$description")
        }
    }

    private fun createViewModel(
        container: AppContainer,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): StageTwoViewModel = StageTwoViewModel(
        caseRepository = container.caseRepository,
        createCase = CreateCaseUseCase(
            baziEngine = container.baziEngine,
            caseRepository = container.caseRepository,
        ),
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
        clock = Clock.systemUTC(),
        fourPillarsLookup = container.fourPillarsLookup,
        singleCaseBundleService = container.singleCaseBundleService,
        caseBackupService = container.caseBackupService,
        backupAttachmentRoot = container.backupAttachmentRoot,
        backupWorkRoot = container.backupWorkRoot,
        savedStateHandle = savedStateHandle,
    )
}
