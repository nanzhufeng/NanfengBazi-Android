package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupContract
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupQuery
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupResult
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.tyme.eightchar.provider.impl.LunarSect2EightCharProvider
import com.tyme.lunar.LunarHour
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TymeFourPillarsLookupTest {
    private val engine = TymeBaziEngine()
    private val lookup = TymeFourPillarsLookup(engine)

    @Test
    fun `正向排盘结果能在指定年份反查并由唯一引擎复算`() = runTest {
        val source = engine.calculate(
            solarInput(1949, 10, 1, 15, 0),
            CalculationProfile.tymeDefault(),
        )

        val completed = lookup.search(
            query(
                pillars = source.fourPillars,
                startYear = 1949,
                endYear = 1949,
            ),
        ) as FourPillarsLookupResult.Completed

        assertTrue(completed.candidates.isNotEmpty())
        assertTrue(completed.candidates.all { it.fourPillars == source.fourPillars })
        assertTrue(completed.candidates.all { it.civilDateTime.year == 1949 })
        assertTrue(completed.candidates.all { it.resolvedUtcOffsetSeconds == 28_800 })
        assertEquals("1.5.1", completed.evidence.engineVersion)
    }

    @Test
    fun `结构合法但不相容的四柱返回无解而不是错误`() = runTest {
        val completed = lookup.search(
            query(
                pillars = FourPillars("甲子", "甲子", "甲子", "甲子"),
                startYear = 2000,
                endYear = 2000,
            ),
        ) as FourPillarsLookupResult.Completed

        assertTrue(completed.candidates.isEmpty())
    }

    @Test
    fun `Tyme原始反查漏候选时以六十日扫描并由唯一引擎复算`() = runTest {
        val source = engine.calculate(
            solarInput(1946, 1, 5, 5, 0),
            CalculationProfile.tymeDefault(),
        )

        val completed = lookup.search(
            query(
                pillars = source.fourPillars,
                startYear = 1946,
                endYear = 1946,
            ),
        ) as FourPillarsLookupResult.Completed

        assertTrue(
            completed.candidates.any {
                with(it.civilDateTime) {
                    year == 1946 && month == 1 && day == 5 && hour == 5
                }
            },
        )
        assertTrue(completed.candidates.all { it.fourPillars == source.fourPillars })
        assertTrue(completed.evidence.lookupMethod.contains("60-day civil scan"))
    }

    @Test
    fun `年份上下边界均可反查`() = runTest {
        listOf(
            FourPillarsLookupContract.MIN_YEAR,
            FourPillarsLookupContract.MAX_YEAR,
        ).forEach { year ->
            val source = engine.calculate(
                solarInput(year, 6, 15, 12, 0),
                CalculationProfile.tymeDefault(),
            )
            val completed = lookup.search(
                query(
                    pillars = source.fourPillars,
                    startYear = year,
                    endYear = year,
                ),
            ) as FourPillarsLookupResult.Completed

            assertTrue("$year 边界应至少有一个候选", completed.candidates.isNotEmpty())
            assertTrue(completed.candidates.all { it.civilDateTime.year == year })
        }
    }

    @Test
    fun `跨六十年周期返回相隔六十年的候选组`() = runTest {
        val source = engine.calculate(
            solarInput(2020, 6, 15, 12, 0),
            CalculationProfile.tymeDefault(),
        )

        val completed = lookup.search(
            query(
                pillars = source.fourPillars,
                startYear = 1960,
                endYear = 2020,
            ),
        ) as FourPillarsLookupResult.Completed
        val years = completed.candidates.map { it.civilDateTime.year }.distinct()

        assertTrue(years.contains(1960))
        assertTrue(years.contains(2020))
        assertTrue(years.zipWithNext().any { (left, right) -> right - left == 60 })
    }

    @Test
    fun `两种子时口径分别反查各自正向结果`() = runTest {
        val defaultProfile = CalculationProfile.tymeDefault()
        val lateProfile = CalculationProfile.tymeDefault(
            ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
        )
        val input = solarInput(2026, 7, 30, 23, 30)
        val defaultPillars = engine.calculate(input, defaultProfile).fourPillars
        val latePillars = engine.calculate(input, lateProfile).fourPillars

        assertNotEquals(defaultPillars.day, latePillars.day)
        val defaultResult = lookup.search(
            query(defaultPillars, 2026, 2026, RatHourRule.TYME_DEFAULT),
        ) as FourPillarsLookupResult.Completed
        val lateResult = lookup.search(
            query(latePillars, 2026, 2026, RatHourRule.LATE_RAT_SAME_DAY),
        ) as FourPillarsLookupResult.Completed

        assertTrue(defaultResult.candidates.any { it.civilDateTime.hour == 0 })
        assertTrue(lateResult.candidates.any { it.civilDateTime.hour == 23 })
        assertTrue(
            defaultResult.candidates.all {
                it.ratHourRule == RatHourRule.TYME_DEFAULT
            },
        )
        assertTrue(
            lateResult.candidates.all {
                it.ratHourRule == RatHourRule.LATE_RAT_SAME_DAY
            },
        )
    }

    @Test
    fun `查询成功后恢复调用方原有 LunarHour provider`() = runTest {
        val original = LunarHour.provider
        val marker = LunarSect2EightCharProvider()
        try {
            LunarHour.provider = marker
            lookup.search(
                query(
                    FourPillars("己丑", "癸酉", "甲子", "壬申"),
                    1949,
                    1949,
                ),
            )

            assertSame(marker, LunarHour.provider)
        } finally {
            LunarHour.provider = original
        }
    }

    @Test
    fun `异常和取消后均恢复 LunarHour provider`() {
        val original = LunarHour.provider
        val marker = LunarSect2EightCharProvider()
        try {
            LunarHour.provider = marker
            listOf<Throwable>(
                IllegalStateException("test"),
                CancellationException("test"),
            ).forEach { expected ->
                try {
                    LunarHourProviderGuard.withProvider(RatHourRule.TYME_DEFAULT) {
                        throw expected
                    }
                    fail("应抛出 ${expected::class.simpleName}")
                } catch (actual: Throwable) {
                    assertSame(expected, actual)
                }
                assertSame(marker, LunarHour.provider)
            }
        } finally {
            LunarHour.provider = original
        }
    }

    @Test
    fun `并发反查 provider 切换保持串行且最终恢复`() {
        val original = LunarHour.provider
        val marker = LunarSect2EightCharProvider()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondEnteredTooEarly = AtomicBoolean(false)
        try {
            LunarHour.provider = marker
            val first = thread {
                LunarHourProviderGuard.withProvider(RatHourRule.TYME_DEFAULT) {
                    firstEntered.countDown()
                    releaseFirst.await(2, TimeUnit.SECONDS)
                }
            }
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS))
            val second = thread {
                LunarHourProviderGuard.withProvider(RatHourRule.LATE_RAT_SAME_DAY) {
                    if (releaseFirst.count > 0) secondEnteredTooEarly.set(true)
                }
            }
            Thread.sleep(50)
            releaseFirst.countDown()
            first.join(2_000)
            second.join(2_000)

            assertFalse(secondEnteredTooEarly.get())
            assertSame(marker, LunarHour.provider)
        } finally {
            releaseFirst.countDown()
            LunarHour.provider = original
        }
    }

    private fun query(
        pillars: FourPillars,
        startYear: Int,
        endYear: Int,
        ratHourRule: RatHourRule = RatHourRule.TYME_DEFAULT,
    ) = FourPillarsLookupQuery(
        fourPillars = pillars,
        startYear = startYear,
        endYear = endYear,
        timeZoneId = "Asia/Shanghai",
        ratHourRule = ratHourRule,
    )

    private fun solarInput(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ) = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(year, month, day, hour, minute, 0),
        ),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        timePrecision = TimePrecision.EXACT_TO_MINUTE,
        timeZoneId = "Asia/Shanghai",
        locationName = "测试地点",
        timeSourceType = TimeSourceType.SELF_REPORTED,
    )
}
