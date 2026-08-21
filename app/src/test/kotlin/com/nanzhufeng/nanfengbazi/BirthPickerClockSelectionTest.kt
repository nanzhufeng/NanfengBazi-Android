package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthPickerClockSelectionTest {
    @Test
    fun `任一时分选未知时两列同步未知`() {
        val selection = BirthPickerClockSelection
            .from(hour = 8, minute = 30, timePrecision = TimePrecision.EXACT_TO_MINUTE)
            .selectHour(null)

        assertTrue(selection.isUnknown)
        assertEquals(null, selection.hourValue)
        assertEquals(null, selection.minuteValue)
        assertEquals(TimePrecision.UNKNOWN, selection.timePrecision)
    }

    @Test
    fun `未知时分改回数值时两列同步恢复数值模式`() {
        val selection = BirthPickerClockSelection
            .from(hour = 8, minute = 30, timePrecision = TimePrecision.UNKNOWN)
            .selectMinute(45)

        assertFalse(selection.isUnknown)
        assertEquals(8, selection.hourValue)
        assertEquals(45, selection.minuteValue)
        assertEquals(TimePrecision.EXACT_TO_MINUTE, selection.timePrecision)
    }
}
