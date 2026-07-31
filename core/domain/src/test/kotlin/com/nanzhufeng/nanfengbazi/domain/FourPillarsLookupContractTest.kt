package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FourPillarsLookupContractTest {
    @Test
    fun `有效干支和完整边界范围通过校验`() {
        val query = query(
            startYear = FourPillarsLookupContract.MIN_YEAR,
            endYear = FourPillarsLookupContract.MAX_YEAR,
        )

        assertNull(FourPillarsLookupContract.validate(query))
    }

    @Test
    fun `四柱必须分别属于六十甲子`() {
        val error = FourPillarsLookupContract.validate(
            query().copy(
                fourPillars = FourPillars(
                    year = "甲子",
                    month = "甲丑",
                    day = "丙寅",
                    hour = "丁卯",
                ),
            ),
        )

        assertEquals(
            FourPillarsLookupError.InvalidPillar(
                field = FourPillarsField.MONTH,
                value = "甲丑",
            ),
            error,
        )
    }

    @Test
    fun `年份越界和倒序返回结构化错误`() {
        assertTrue(
            FourPillarsLookupContract.validate(
                query(startYear = 1899),
            ) is FourPillarsLookupError.YearOutOfBounds,
        )
        assertEquals(
            FourPillarsLookupError.InvalidYearOrder(2026, 2025),
            FourPillarsLookupContract.validate(
                query(startYear = 2026, endYear = 2025),
            ),
        )
    }

    @Test
    fun `无效 IANA 时区不会被接受`() {
        assertEquals(
            FourPillarsLookupError.InvalidTimeZone("Asia/Not_A_Zone"),
            FourPillarsLookupContract.validate(
                query().copy(timeZoneId = "Asia/Not_A_Zone"),
            ),
        )
    }

    @Test
    fun `输入首尾空白在校验前统一归一化`() {
        val normalized = FourPillarsLookupContract.normalize(
            query().copy(
                fourPillars = FourPillars(" 甲子 ", " 丙寅", "甲子 ", " 甲子 "),
                timeZoneId = " Asia/Shanghai ",
            ),
        )

        assertNull(FourPillarsLookupContract.validate(normalized))
        assertEquals("甲子", normalized.fourPillars.year)
        assertEquals("Asia/Shanghai", normalized.timeZoneId)
    }

    private fun query(
        startYear: Int = 1949,
        endYear: Int = 1949,
    ) = FourPillarsLookupQuery(
        fourPillars = FourPillars("己丑", "癸酉", "甲子", "壬申"),
        startYear = startYear,
        endYear = endYear,
        timeZoneId = "Asia/Shanghai",
        ratHourRule = RatHourRule.TYME_DEFAULT,
    )
}
