package com.nanzhufeng.nanfengbazi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BirthInputTest {
    @Test
    fun `农历日期只接受基础字段范围`() {
        val valid = LunarDateTime(2023, 2, 30, 23, 59, 59, isLeapMonth = true)

        assertEquals(2, valid.month)
        assertThrows(IllegalArgumentException::class.java) {
            LunarDateTime(2023, 13, 1, 0, 0, 0, isLeapMonth = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LunarDateTime(2023, 2, 31, 0, 0, 0, isLeapMonth = false)
        }
    }

    @Test
    fun `经纬度必须成对出现`() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthInput(
                calendarInput = BirthCalendarInput.Solar(
                    CivilDateTime(2025, 1, 1, 12, 0, 0),
                ),
                sexForFortuneDirection = SexForFortuneDirection.MAN,
                timePrecision = TimePrecision.EXACT_TO_MINUTE,
                longitude = 120.0,
            )
        }
    }

    @Test
    fun `默认配置固定引擎和规则版本`() {
        val profile = CalculationProfile.tymeDefault()

        assertEquals("1.5.1", profile.engineVersion)
        assertEquals("stage0-v1", profile.ruleVersion)
    }
}
