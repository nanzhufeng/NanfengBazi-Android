package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Test

class StageTwoNavigatorTest {
    @Test
    fun `新建与详情均可返回单列列表`() {
        val navigator = StageTwoNavigator()

        assertEquals(AppDestination.CaseList, navigator.current)
        assertEquals(AppDestination.RecordHub, navigator.openRecordHub())
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
            AppDestination.MasterCommentaryCandidates("case-1", "record-1"),
            navigator.openMasterCommentaryCandidates("case-1", "record-1"),
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
