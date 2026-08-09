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
import java.time.LocalDateTime
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
        assertEquals(listOf("1992", "1993", "1994", "1995", "1996"), position.minorTimeline.map { it.label })
        assertEquals(listOf("丁未", "戊申", "己酉", "庚戌", "辛亥"), position.minorTimeline.map { it.pillar })
        assertTrue(position.minorTimeline.last().subtitle.contains("至1997/5/21"))
        assertEquals("professional-minor-fortune-v1", position.minorFortuneRuleVersion)
        assertEquals(13, position.decadeTimeline.size)
        assertEquals("小运", position.decadeTimeline.first().stageLabel)
        assertEquals("1992–97", position.decadeTimeline.first().label)
        assertEquals("0–4岁", position.decadeTimeline.first().subtitle)
        assertEquals("4–14岁", position.decadeTimeline[1].subtitle)
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
        assertEquals(listOf("原局", "岁运"), position.shenShaGroups.map { it.title })
        val shenShaLines = position.shenShaGroups.flatMap { it.lines }
        assertTrue(shenShaLines.all { it.take(2) in position.pillarColumns.map { column -> column.pillar } })
        assertTrue(shenShaLines.all { line ->
            listOf("年柱", "月柱", "日柱", "时柱", "大运", "流年", "流月", "流日", "流时")
                .none(line::contains)
        })
        assertTrue(shenShaLines.all { "、" !in it && "\n" !in it })
        assertEquals("professional-detail-relations-shensha-v4", position.detailRuleVersion)
    }

    @Test
    fun `小运由时柱下一位按岁递进且只覆盖精确交运前`() = runTest {
        val forwardResult = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val forward = resolver.locate(forwardResult, CivilDateTime(2026, 8, 24, 12, 0, 0))

        assertEquals("丁未", forward.minorTimeline.first().pillar)
        assertEquals(5, forward.minorTimeline.size)
        assertTrue(forward.minorTimeline.all {
            it.observedAt.toLocalDateTime().isBefore(
                requireNotNull(forwardResult.decadeFortunes.first().startAt).toLocalDateTime(),
            )
        })

        val firstMinor = forward.minorTimeline.first()
        val beforeDecade = resolver.locate(forwardResult, firstMinor.observedAt)
        assertEquals(FortunePositionStatus.BEFORE_FIRST_DECADE, beforeDecade.position.status)
        assertEquals(firstMinor.key, beforeDecade.minorTimeline.single { it.selected }.key)
        assertEquals("小运", beforeDecade.decadeTimeline.single { it.selected }.stageLabel)
        assertEquals(
            listOf("1992", "1993", "1994", "1995", "1996", "1997"),
            beforeDecade.annualTimeline.map { it.label },
        )

        val backwardResult = engine.calculate(
            sampleInput(SexForFortuneDirection.WOMAN),
            CalculationProfile.tymeDefault(),
        )
        val backward = resolver.locate(backwardResult, CivilDateTime(2026, 8, 24, 12, 0, 0))
        assertEquals("乙巳", backward.minorTimeline.first().pillar)
        assertTrue(backward.minorTimeline.last().observedAt.toLocalDateTime().isBefore(
            requireNotNull(backwardResult.decadeFortunes.first().startAt).toLocalDateTime(),
        ))
    }

    @Test
    fun `旧八步快照在专业时间轴兼容补足一百二十年`() = runTest {
        val complete = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val legacyDecades = complete.decadeFortunes.take(8)
        val legacy = complete.copy(
            decadeFortunes = legacyDecades,
            annualFortunes = complete.annualFortunes.filter {
                it.calendarYear <= legacyDecades.last().endYear
            },
        )

        val position = resolver.locate(legacy, CivilDateTime(2026, 8, 24, 12, 0, 0))

        assertEquals(13, position.decadeTimeline.size)
        assertEquals(
            legacyDecades.map { it.name },
            position.decadeTimeline.drop(1).take(8).map { it.pillar },
        )
        assertEquals(
            complete.decadeFortunes.map { it.name },
            position.decadeTimeline.drop(1).map { it.pillar },
        )
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
    fun `流年流月候选截断到分钟后仍定位自身而不回退左侧`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(2026, 8, 8, 12, 34, 0))

        current.annualTimeline.forEach { item ->
            val relocated = resolver.locate(result, item.observedAt.copy(second = 0))
            assertEquals(item.pillar, relocated.flowPillars.year)
            assertEquals(item.key, relocated.annualTimeline.single { it.selected }.key)
        }
        current.monthlyTimeline.forEach { item ->
            val relocated = resolver.locate(result, item.observedAt.copy(second = 0))
            assertEquals(item.pillar, relocated.flowPillars.month)
            assertEquals(item.key, relocated.monthlyTimeline.single { it.selected }.key)
        }
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

        afterDaySelect.hourlyTimeline.forEach { hour ->
            assertEquals(14, hour.observedAt.day)
            val afterHourSelect = resolver.locate(result, hour.observedAt)
            assertEquals(14, afterHourSelect.position.observedAt.day)
            assertEquals(afterDaySelect.flowPillars.month, afterHourSelect.flowPillars.month)
            assertEquals(hour.key, afterHourSelect.hourlyTimeline.single { it.selected }.key)
        }
    }

    private fun sampleInput(
        sex: SexForFortuneDirection = SexForFortuneDirection.MAN,
    ): BirthInput = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(1992, 8, 24, 12, 0, 0),
        ),
        sexForFortuneDirection = sex,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
    )
}

private fun com.tyme.solar.SolarTime.toCivilDateTime(): CivilDateTime =
    CivilDateTime(year, month, day, hour, minute, second)

private fun CivilDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, month, day, hour, minute, second)
