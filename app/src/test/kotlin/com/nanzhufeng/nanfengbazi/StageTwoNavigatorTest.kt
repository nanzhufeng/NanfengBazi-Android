package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Test

class StageTwoNavigatorTest {
    @Test
    fun `新建与详情均可返回单列列表`() {
        val navigator = StageTwoNavigator()

        assertEquals(AppDestination.CaseList, navigator.current)
        assertEquals(AppDestination.CreateCase, navigator.openCreate())
        assertEquals(AppDestination.CaseList, navigator.backToList())
        assertEquals(
            AppDestination.CaseDetail("case-1"),
            navigator.openDetail("case-1"),
        )
        assertEquals(
            AppDestination.EditTextRecord("case-1", null),
            navigator.openTextRecord("case-1", null),
        )
        assertEquals(AppDestination.CaseDetail("case-1"), navigator.back())
        assertEquals(AppDestination.CaseList, navigator.backToList())
    }
}
