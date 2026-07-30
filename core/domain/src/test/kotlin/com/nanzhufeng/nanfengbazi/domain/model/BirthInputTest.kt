package com.nanzhufeng.nanfengbazi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BirthInputTest {
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

