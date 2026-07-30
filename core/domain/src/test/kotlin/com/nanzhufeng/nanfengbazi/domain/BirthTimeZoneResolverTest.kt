package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthTimeZoneResolverTest {
    @Test
    fun `普通本地时间自动解析 offset 并记录时区数据版本`() {
        val result = BirthTimeZoneResolver.resolve(
            localDateTime = CivilDateTime(2024, 1, 15, 12, 0, 0),
            timeZoneId = "America/New_York",
        )

        assertTrue(result is BirthTimeZoneResolution.Resolved)
        result as BirthTimeZoneResolution.Resolved
        assertEquals(-18_000, result.utcOffsetSeconds)
        assertTrue(result.timeZoneDataVersion.isNotBlank())
    }

    @Test
    fun `夏令时回拨重叠时刻要求从两个 offset 中选择`() {
        val result = BirthTimeZoneResolver.resolve(
            localDateTime = CivilDateTime(2024, 11, 3, 1, 30, 0),
            timeZoneId = "America/New_York",
        )

        assertTrue(result is BirthTimeZoneResolution.ChoiceRequired)
        result as BirthTimeZoneResolution.ChoiceRequired
        assertEquals(listOf(-14_400, -18_000), result.validUtcOffsetSeconds)
    }

    @Test
    fun `夏令时跳时产生的不存在时刻明确返回缺口`() {
        val result = BirthTimeZoneResolver.resolve(
            localDateTime = CivilDateTime(2024, 3, 10, 2, 30, 0),
            timeZoneId = "America/New_York",
        )

        assertEquals(
            BirthTimeZoneResolution.Nonexistent(
                gapStartsAt = CivilDateTime(2024, 3, 10, 2, 0, 0),
                gapEndsAt = CivilDateTime(2024, 3, 10, 3, 0, 0),
                timeZoneDataVersion = (
                    result as BirthTimeZoneResolution.Nonexistent
                    ).timeZoneDataVersion,
            ),
            result,
        )
    }

    @Test
    fun `无效时区和不匹配 offset 都不会被静默接受`() {
        assertEquals(
            BirthTimeZoneResolution.InvalidZone("Asia/Not_A_Zone"),
            BirthTimeZoneResolver.resolve(
                CivilDateTime(2024, 1, 1, 0, 0, 0),
                "Asia/Not_A_Zone",
            ),
        )
        val mismatch = BirthTimeZoneResolver.resolve(
            localDateTime = CivilDateTime(2024, 11, 3, 1, 30, 0),
            timeZoneId = "America/New_York",
            selectedUtcOffsetSeconds = 28_800,
        )
        assertTrue(mismatch is BirthTimeZoneResolution.InvalidOffsetSelection)
        assertEquals(
            BirthTimeZoneResolution.InvalidZone("+08:00"),
            BirthTimeZoneResolver.resolve(
                CivilDateTime(2024, 1, 1, 0, 0, 0),
                "+08:00",
            ),
        )
    }
}
