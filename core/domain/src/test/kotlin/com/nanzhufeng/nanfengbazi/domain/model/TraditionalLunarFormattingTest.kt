package com.nanzhufeng.nanfengbazi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TraditionalLunarFormattingTest {
    @Test
    fun `传统农历使用中文月日和十二时辰`() {
        assertEquals(
            "1992年七月廿六 午时",
            LunarDateTime(
                year = 1992,
                month = 7,
                day = 26,
                hour = 12,
                minute = 34,
                second = 56,
                isLeapMonth = false,
            ).toTraditionalChineseText(),
        )
    }

    @Test
    fun `传统农历保留闰月并正确合并跨午夜子时`() {
        val lateRatHour = LunarDateTime(2023, 2, 1, 23, 59, 0, true)
        val earlyRatHour = lateRatHour.copy(hour = 0)

        assertEquals("2023年闰二月初一 子时", lateRatHour.toTraditionalChineseText())
        assertEquals("2023年闰二月初一 子时", earlyRatHour.toTraditionalChineseText())
    }
}
