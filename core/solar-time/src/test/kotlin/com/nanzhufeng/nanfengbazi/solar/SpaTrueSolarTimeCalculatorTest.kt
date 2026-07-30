package com.nanzhufeng.nanfengbazi.solar

import com.nanzhufeng.nanfengbazi.domain.TrueSolarTimeRequest
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.TrueSolarTimeApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaTrueSolarTimeCalculatorTest {
    private val calculator = SpaTrueSolarTimeCalculator()

    @Test
    fun `问真截图样本校正到十一点五十三分附近`() {
        val evidence = calculator.calculate(
            request(
                dateTime = CivilDateTime(1992, 8, 24, 12, 0, 0),
                longitude = 118.68,
                latitude = 33.73,
            ),
        )

        assertEquals(-317, evidence.meanSolarCorrectionSeconds)
        assertTrue(evidence.totalCorrectionSeconds in -480..-360)
        assertEquals(CivilDateTime(1992, 8, 24, 11, 52, 23), evidence.trueSolarDateTime)
        assertFalse(evidence.crossesDate)
        assertFalse(evidence.crossesDoubleHour)
        assertEquals(
            TrueSolarTimeApplicationRule
                .CIVIL_YEAR_MONTH_TRUE_SOLAR_DAY_HOUR_PROVISIONAL_V1,
            evidence.applicationRule,
        )
        assertEquals(SpaTrueSolarTimeCalculator.ALGORITHM_VERSION, evidence.algorithmVersion)
    }

    @Test
    fun `校正跨时辰时留下边界证据`() {
        val evidence = calculator.calculate(
            request(
                dateTime = CivilDateTime(1992, 8, 24, 13, 4, 0),
                longitude = 118.68,
                latitude = 33.73,
            ),
        )

        assertTrue(evidence.crossesDoubleHour)
        assertEquals(12, evidence.trueSolarDateTime.hour)
    }

    @Test
    fun `西部地区校正可跨到前一日`() {
        val evidence = calculator.calculate(
            request(
                dateTime = CivilDateTime(2023, 1, 22, 2, 0, 0),
                longitude = 87.6,
                latitude = 43.8,
            ),
        )

        assertTrue(evidence.crossesDate)
        assertEquals(CivilDateTime(2023, 1, 21, 23, 38, 59), evidence.trueSolarDateTime)
    }

    @Test
    fun `超出 SPA 支持年份明确拒绝`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(
                request(
                    dateTime = CivilDateTime(6001, 1, 1, 12, 0, 0),
                    longitude = 120.0,
                    latitude = 30.0,
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains("1..6000"))
    }

    @Test
    fun `夏令时重叠严格使用用户选定的出生 offset`() {
        val base = TrueSolarTimeRequest(
            civilDateTime = CivilDateTime(2024, 11, 3, 1, 30, 0),
            timeZoneId = "America/New_York",
            resolvedUtcOffsetSeconds = -14_400,
            longitude = -74.006,
            latitude = 40.7128,
        )

        val daylight = calculator.calculate(base)
        val standard = calculator.calculate(
            base.copy(resolvedUtcOffsetSeconds = -18_000),
        )

        assertEquals(
            3_600,
            standard.totalCorrectionSeconds - daylight.totalCorrectionSeconds,
        )
        assertEquals(-14_400, daylight.resolvedUtcOffsetSeconds)
        assertEquals(-18_000, standard.resolvedUtcOffsetSeconds)
    }

    private fun request(
        dateTime: CivilDateTime,
        longitude: Double,
        latitude: Double,
    ) = TrueSolarTimeRequest(
        civilDateTime = dateTime,
        timeZoneId = "Asia/Shanghai",
        resolvedUtcOffsetSeconds = 28_800,
        longitude = longitude,
        latitude = latitude,
    )
}
