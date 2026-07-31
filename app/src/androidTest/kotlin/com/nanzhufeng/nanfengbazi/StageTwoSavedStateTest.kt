package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import java.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

        restoredEditor.duplicateCase()
        waitUntil {
            val detailId = restoredEditor.state.value.detail?.id
            detailId != null && detailId != caseId && !restoredEditor.state.value.mutationSaving
        }
        restoredEditor.backToList()
        restoredEditor.openCaseComparison()
        waitUntil { restoredEditor.state.value.comparisonReport != null }
        val comparisonHandle = SavedStateHandle()
        restoredEditor.saveRestorableStateTo(comparisonHandle)
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

    private suspend fun waitUntil(condition: () -> Boolean) {
        withTimeout(15_000) {
            while (!condition()) delay(50)
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
