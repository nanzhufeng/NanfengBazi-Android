package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BirthPickerQuickLocateTest {
    @Test
    fun `完整紧凑输入解析为年月日时分`() {
        assertEquals(
            BirthPickerQuickLocate(year = 1990, month = 1, day = 2, hour = 8, minute = 30),
            parseBirthPickerQuickLocate("199001020830"),
        )
    }

    @Test
    fun `分隔完整输入和局部中文输入均可定位`() {
        assertEquals(
            BirthPickerQuickLocate(year = 1990, month = 1, day = 2, hour = 8, minute = 30),
            parseBirthPickerQuickLocate("1990-01-02 08:30"),
        )
        assertEquals(
            BirthPickerQuickLocate(year = 1990, month = 3, day = 15),
            parseBirthPickerQuickLocate("1990年3月15日"),
        )
        assertEquals(
            BirthPickerQuickLocate(hour = 8, minute = 30),
            parseBirthPickerQuickLocate("8:30"),
        )
    }

    @Test
    fun `不完整无语义数字不擅自改变滚轮`() {
        assertNull(parseBirthPickerQuickLocate("31"))
        assertEquals(BirthPickerQuickLocate(year = 1990), parseBirthPickerQuickLocate("１９９０"))
    }
}
