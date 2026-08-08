package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermType
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.tyme.solar.SolarTerm
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TymeProfessionalFortuneResolverTest {
    private val engine = TymeBaziEngine(
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
    )
    private val resolver = TymeProfessionalFortuneResolver()

    @Test
    fun `十二节交界瞬间换流月而十二气不换流月`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())

        listOf(2024, 2025, 2026, 2033).forEach { year ->
            (3..25 step 2).forEach { index ->
                val jie = SolarTerm.fromIndex(year, index)
                assertTrue("${jie.name} 应为节", jie.isJie)
                val at = jie.julianDay.solarTime
                val beforePosition = resolver.locate(result, at.next(-1).toCivilDateTime())
                val atPosition = resolver.locate(result, at.toCivilDateTime())

                assertNotEquals(
                    "$year ${jie.name} 交节应切换流月",
                    beforePosition.flowPillars.month,
                    atPosition.flowPillars.month,
                )
            }

            (4..24 step 2).forEach { index ->
                val qi = SolarTerm.fromIndex(year, index)
                assertTrue("${qi.name} 应为气", !qi.isJie)
                val at = qi.julianDay.solarTime
                val beforePosition = resolver.locate(result, at.next(-1).toCivilDateTime())
                val atPosition = resolver.locate(result, at.toCivilDateTime())

                assertEquals(
                    "$year ${qi.name} 交气不应切换流月",
                    beforePosition.flowPillars.month,
                    atPosition.flowPillars.month,
                )
            }
        }
    }

    @Test
    fun `专业岁运返回精确前后节气及版本证据`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val jingZhe = SolarTerm.fromIndex(2026, 5)
        val position = resolver.locate(
            result,
            jingZhe.julianDay.solarTime.toCivilDateTime(),
        )

        assertEquals(jingZhe.name, position.previousSolarTerm.name)
        assertEquals(SolarTermType.JIE, position.previousSolarTerm.type)
        assertEquals(
            jingZhe.julianDay.solarTime.toCivilDateTime(),
            position.previousSolarTerm.at,
        )
        assertEquals(jingZhe.next(1).name, position.nextSolarTerm.name)
        assertEquals(
            jingZhe.next(1).julianDay.solarTime.toCivilDateTime(),
            position.nextSolarTerm.at,
        )
        assertEquals(result.profile.id, position.profileId)
        assertEquals(result.profile.ruleVersion, position.ruleVersion)
        assertEquals(SolarTimeMode.CIVIL_TIME, position.observationTimeMode)
        assertEquals(2026, position.position.annualFortune.calendarYear)
        assertNotNull(position.position.decadeFortune)
        assertEquals(FortunePositionStatus.WITHIN_DECADE, position.position.status)
        assertEquals(9, position.pillarColumns.size)
        assertTrue(position.pillarColumns.all { it.stemTenGod.isNotBlank() })
        assertTrue(position.pillarColumns.all { it.heavenStemElement in setOf("木", "火", "土", "金", "水") })
        assertTrue(position.pillarColumns.all { it.earthBranchElement in setOf("木", "火", "土", "金", "水") })
        assertTrue(position.pillarColumns.all { it.hiddenStems.isNotEmpty() })
        assertTrue(position.pillarColumns.flatMap { it.hiddenStems }.all { it.tenGod.isNotBlank() })
        assertEquals(8, position.decadeTimeline.size)
        assertEquals("4–14周岁", position.decadeTimeline.first().subtitle)
        assertTrue(position.annualTimeline.none { it.subtitle.startsWith("虚") })
        assertEquals(12, position.monthlyTimeline.size)
        assertEquals(31, position.dailyTimeline.size)
        assertEquals(12, position.hourlyTimeline.size)
        assertTrue(position.annualTimeline.any { it.selected })
        assertTrue(position.monthlyTimeline.any { it.selected })
        assertTrue(position.dailyTimeline.single { it.selected }.subtitle == "已选")
        assertTrue(position.hourlyTimeline.any { it.selected })
        assertTrue(position.hourlyTimeline.all { it.hiddenStems.isNotEmpty() })
        assertEquals(33, position.completedAge)
        assertEquals(listOf("天干", "地支"), position.interactionGroups.map { it.title })
        assertTrue(position.interactionGroups.flatMap { it.lines }.none { "流年" in it || "流月" in it })
        assertTrue(position.shenShaGroups.isNotEmpty())
        assertTrue(position.shenShaGroups.flatMap { it.lines }.all { "：—" !in it })
        assertEquals("professional-detail-relations-shensha-v2", position.detailRuleVersion)
    }

    @Test
    fun `周岁按完整生日计算而不是虚岁或年份差`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())

        assertEquals(
            33,
            resolver.locate(result, CivilDateTime(2026, 8, 23, 23, 59, 0)).completedAge,
        )
        assertEquals(
            34,
            resolver.locate(result, CivilDateTime(2026, 8, 24, 0, 0, 0)).completedAge,
        )
    }

    @Test
    fun `专业流日按所选子时规则切换且零点重新一致`() = runTest {
        val defaultResult = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val lateResult = engine.calculate(
            sampleInput(),
            CalculationProfile.tymeDefault(
                ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
            ),
        )

        val atRatHour = CivilDateTime(2026, 7, 30, 23, 0, 0)
        val defaultAtRatHour = resolver.locate(defaultResult, atRatHour)
        val lateAtRatHour = resolver.locate(lateResult, atRatHour)
        assertNotEquals(
            defaultAtRatHour.flowPillars.day,
            lateAtRatHour.flowPillars.day,
        )
        assertEquals(
            defaultAtRatHour.flowPillars.hour,
            lateAtRatHour.flowPillars.hour,
        )

        val atMidnight = CivilDateTime(2026, 7, 31, 0, 0, 0)
        val defaultAtMidnight = resolver.locate(defaultResult, atMidnight)
        val lateAtMidnight = resolver.locate(lateResult, atMidnight)
        assertEquals(defaultAtMidnight.flowPillars, lateAtMidnight.flowPillars)
        assertEquals(
            "tyme-late-rat-same-day-v1",
            lateAtRatHour.profileId,
        )
        assertEquals("stage7b-rat-hour-v1", lateAtRatHour.ruleVersion)
    }

    @Test
    fun `点击大运首年仍定位到所选大运和流年`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(2026, 8, 8, 12, 0, 0))
        val selected = requireNotNull(current.annualTimeline.firstOrNull { it.label == "2017" })
        val relocated = resolver.locate(result, selected.observedAt)

        assertEquals(2017, relocated.position.annualFortune.calendarYear)
        assertEquals("2017", relocated.annualTimeline.single { it.selected }.label)
        assertEquals(current.position.decadeFortune?.name, relocated.position.decadeFortune?.name)
    }

    @Test
    fun `专业时间轴候选不越过统一支持的二二零零年边界`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val end = resolver.locate(result, CivilDateTime(2200, 12, 31, 21, 0, 0))

        assertTrue(end.monthlyTimeline.all { it.observedAt.year <= 2200 })
        assertEquals(31, end.dailyTimeline.size)
        assertTrue(end.dailyTimeline.all { it.observedAt.year <= 2200 })
        assertTrue(end.hourlyTimeline.all { it.observedAt.year <= 2200 })
        assertTrue(end.dailyTimeline.any { it.selected })
    }

    @Test
    fun `流日整月横向选择不再以新选日居中且流时保持同一民用日期`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val initial = resolver.locate(result, CivilDateTime(2026, 7, 30, 12, 0, 0))

        assertEquals(1, initial.dailyTimeline.first().observedAt.day)
        assertEquals(31, initial.dailyTimeline.last().observedAt.day)

        val selectedDay = initial.dailyTimeline.first { it.observedAt.day == 14 }
        val afterDaySelect = resolver.locate(result, selectedDay.observedAt)
        assertEquals(1, afterDaySelect.dailyTimeline.first().observedAt.day)
        assertEquals(31, afterDaySelect.dailyTimeline.last().observedAt.day)
        assertEquals(14, afterDaySelect.dailyTimeline.single { it.selected }.observedAt.day)

        val lateHour = afterDaySelect.hourlyTimeline.first { it.observedAt.hour == 23 }
        assertEquals(14, lateHour.observedAt.day)
        val afterHourSelect = resolver.locate(result, lateHour.observedAt)
        assertEquals(14, afterHourSelect.position.observedAt.day)
        assertEquals(afterDaySelect.flowPillars.month, afterHourSelect.flowPillars.month)
    }

    private fun sampleInput(): BirthInput = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(1992, 8, 24, 12, 0, 0),
        ),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
    )
}

private fun com.tyme.solar.SolarTime.toCivilDateTime(): CivilDateTime =
    CivilDateTime(year, month, day, hour, minute, second)
