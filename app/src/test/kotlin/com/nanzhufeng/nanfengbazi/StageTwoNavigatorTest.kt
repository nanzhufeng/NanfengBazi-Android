package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Test

class StageTwoNavigatorTest {
    @Test
    fun `后台保存完成时原位替换临时详情且返回首页`() {
        val navigator = StageTwoNavigator()

        assertEquals(
            AppDestination.CaseDetail("pending-manual-save"),
            navigator.openDetail("pending-manual-save"),
        )
        assertEquals(
            AppDestination.CaseDetail("saved-case"),
            navigator.replaceCurrentDetail("saved-case"),
        )
        assertEquals(AppDestination.CreateCase, navigator.back())
    }

    @Test
    fun `排盘为首页且详情可返回记录列表`() {
        val navigator = StageTwoNavigator()

        assertEquals(AppDestination.CreateCase, navigator.current)
        assertEquals(AppDestination.CaseList, navigator.openRecordHub())
        assertEquals(AppDestination.Settings, navigator.openSettings())
        assertEquals(AppDestination.CaseList, navigator.backToList())
        assertEquals(AppDestination.CaseComparison, navigator.openCaseComparison())
        assertEquals(AppDestination.CaseList, navigator.back())
        assertEquals(AppDestination.CreateCase, navigator.openCreate())
        assertEquals(AppDestination.CaseList, navigator.backToList())
        assertEquals(
            AppDestination.CaseDetail("case-1"),
            navigator.openDetail("case-1"),
        )
        assertEquals(
            AppDestination.CaseObjectiveSummary("case-1"),
            navigator.openObjectiveSummary("case-1"),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(
            AppDestination.ExternalAnalysisBridge("case-1"),
            navigator.openExternalAnalysisBridge("case-1"),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(
            AppDestination.MasterCommentaryCandidates("case-1", "record-1"),
            navigator.openMasterCommentaryCandidates("case-1", "record-1"),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(
            AppDestination.FeedbackThemeCandidates("case-1", "feedback-1"),
            navigator.openFeedbackThemeCandidates("case-1", "feedback-1"),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(
            AppDestination.EditTextRecord("case-1", null),
            navigator.openTextRecord("case-1", null),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(
            AppDestination.AddBirthTimeCandidate("case-1"),
            navigator.openBirthTimeCandidate("case-1"),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(AppDestination.CaseList, navigator.backToList())
    }
}
