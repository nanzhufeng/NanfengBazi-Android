package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneLayer
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneSelection
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
import org.junit.Assert.assertSame
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
        assertEquals("1992", position.decadeTimeline.first().label)
        assertEquals("0–4岁", position.decadeTimeline.first().subtitle)
        assertEquals("4–14岁", position.decadeTimeline[1].subtitle)
        assertTrue(position.annualTimeline.none { it.subtitle.startsWith("虚") })
        assertEquals(12, position.monthlyTimeline.size)
        assertTrue(position.dailyTimeline.size in 29..32)
        assertTrue(position.hourlyTimeline.size in 1..12)
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
        assertEquals("lichun-instant-late-rat-v2", lateAtRatHour.ruleVersion)
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
        assertTrue(end.dailyTimeline.size in 1..32)
        assertTrue(end.dailyTimeline.all { it.observedAt.year <= 2200 })
        assertTrue(end.hourlyTimeline.all { it.observedAt.year <= 2200 })
        assertTrue(end.dailyTimeline.any { it.selected })
    }

    @Test
    fun `流日覆盖当前节令月且流时保持同一流日父级`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val initial = resolver.locate(result, CivilDateTime(2026, 7, 30, 12, 0, 0))

        assertEquals("7/7", initial.dailyTimeline.first().label)
        assertEquals("8/7", initial.dailyTimeline.last().label)

        val selectedDay = initial.dailyTimeline.first { it.observedAt.day == 14 }
        val afterDaySelect = resolver.select(
            result,
            initial,
            ProfessionalFortuneSelection(ProfessionalFortuneLayer.DAILY, selectedDay.observedAt),
        )
        assertEquals("7/7", afterDaySelect.dailyTimeline.first().label)
        assertEquals("8/7", afterDaySelect.dailyTimeline.last().label)
        assertEquals(14, afterDaySelect.dailyTimeline.single { it.selected }.observedAt.day)
        assertEquals(initial.position.decadeFortune?.startAt, afterDaySelect.position.decadeFortune?.startAt)
        assertEquals(initial.position.annualFortune.calendarYear, afterDaySelect.position.annualFortune.calendarYear)
        assertEquals(initial.flowPillars.month, afterDaySelect.flowPillars.month)

        afterDaySelect.hourlyTimeline.forEach { hour ->
            val afterHourSelect = resolver.select(
                result,
                afterDaySelect,
                ProfessionalFortuneSelection(ProfessionalFortuneLayer.HOURLY, hour.observedAt),
            )
            val expected = resolver.locate(result, hour.observedAt)
            assertEquals(expected.flowPillars, afterHourSelect.flowPillars)
            assertEquals(expected.position.decadeFortune?.startAt, afterHourSelect.position.decadeFortune?.startAt)
            assertEquals(hour.key, afterHourSelect.hourlyTimeline.single { it.selected }.key)
        }
    }

    @Test
    fun `交运年和交节日前仍展示完整流月流日流时`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(2027, 11, 4, 6, 37, 0))

        assertEquals(12, current.monthlyTimeline.size)
        assertEquals("10/8", current.dailyTimeline.first().label)
        assertEquals("11/8", current.dailyTimeline.last().label)
        assertEquals(32, current.dailyTimeline.size)
        assertEquals(12, current.hourlyTimeline.size)

        val earlierMonth = current.monthlyTimeline.first()
        val afterMonthSelect = resolver.select(
            result,
            current,
            ProfessionalFortuneSelection(ProfessionalFortuneLayer.MONTHLY, earlierMonth.observedAt),
        )
        assertEquals(earlierMonth.pillar, afterMonthSelect.flowPillars.month)
        assertNotEquals(current.position.decadeFortune?.startAt, afterMonthSelect.position.decadeFortune?.startAt)

        val nextJieDay = current.dailyTimeline.last()
        val afterDaySelect = resolver.select(
            result,
            current,
            ProfessionalFortuneSelection(ProfessionalFortuneLayer.DAILY, nextJieDay.observedAt),
        )
        assertEquals(current.flowPillars.month, afterDaySelect.flowPillars.month)
        assertEquals(nextJieDay.key, afterDaySelect.dailyTimeline.single { it.selected }.key)

        val lateRatHour = current.hourlyTimeline.first { it.label == "23:00" }
        val afterHourSelect = resolver.select(
            result,
            current,
            ProfessionalFortuneSelection(ProfessionalFortuneLayer.HOURLY, lateRatHour.observedAt),
        )
        assertEquals(current.flowPillars.day, afterHourSelect.flowPillars.day)
        assertEquals(lateRatHour.key, afterHourSelect.hourlyTimeline.single { it.selected }.key)
    }

    @Test
    fun `逐层选择复用未受影响的时间轴`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(2026, 7, 30, 12, 0, 0))

        val afterDay = resolver.select(
            result,
            current,
            ProfessionalFortuneSelection(
                ProfessionalFortuneLayer.DAILY,
                current.dailyTimeline.first { it.observedAt.day == 14 }.observedAt,
            ),
        )
        assertSame(current.minorTimeline, afterDay.minorTimeline)
        assertSame(current.decadeTimeline, afterDay.decadeTimeline)
        assertSame(current.annualTimeline, afterDay.annualTimeline)
        assertSame(current.monthlyTimeline, afterDay.monthlyTimeline)

        val afterHour = resolver.select(
            result,
            afterDay,
            ProfessionalFortuneSelection(
                ProfessionalFortuneLayer.HOURLY,
                afterDay.hourlyTimeline.first { it.label == "13:00" }.observedAt,
            ),
        )
        assertSame(afterDay.minorTimeline, afterHour.minorTimeline)
        assertSame(afterDay.decadeTimeline, afterHour.decadeTimeline)
        assertSame(afterDay.annualTimeline, afterHour.annualTimeline)
        assertSame(afterDay.monthlyTimeline, afterHour.monthlyTimeline)
        assertSame(afterDay.dailyTimeline, afterHour.dailyTimeline)
    }

    @Test
    fun `五层选择命令在边界内保留父级并在跨界时同步更新`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(2026, 2, 3, 12, 0, 0))

        current.annualTimeline.forEach { item ->
            val selected = resolver.select(
                result,
                current,
                ProfessionalFortuneSelection(ProfessionalFortuneLayer.ANNUAL, item.observedAt),
            )
            assertEquals(current.position.decadeFortune?.startAt, selected.position.decadeFortune?.startAt)
        }
        current.monthlyTimeline.forEach { item ->
            val selected = resolver.select(
                result,
                current,
                ProfessionalFortuneSelection(ProfessionalFortuneLayer.MONTHLY, item.observedAt),
            )
            assertEquals(current.position.decadeFortune?.startAt, selected.position.decadeFortune?.startAt)
            assertEquals(current.position.annualFortune.calendarYear, selected.position.annualFortune.calendarYear)
        }
        current.dailyTimeline.forEach { item ->
            val selected = resolver.select(
                result,
                current,
                ProfessionalFortuneSelection(ProfessionalFortuneLayer.DAILY, item.observedAt),
            )
            val expected = resolver.locate(result, item.observedAt)
            assertEquals(expected.flowPillars, selected.flowPillars)
            assertEquals(expected.position.decadeFortune?.startAt, selected.position.decadeFortune?.startAt)
            if (expected.flowPillars.month == current.flowPillars.month) {
                assertEquals(current.position.annualFortune.calendarYear, selected.position.annualFortune.calendarYear)
            }
        }
    }

    @Test
    fun `截图寒露流日从十月八日开始并延续到立冬交节前`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(2026, 10, 10, 15, 44, 0))
        assertEquals("寒露", current.monthlyTimeline.single { it.selected }.subtitle)
        assertEquals("10/8", current.dailyTimeline.first().label)
        assertEquals("11/7", current.dailyTimeline.last().label)
        assertEquals(31, current.dailyTimeline.size)
        current.dailyTimeline.forEach { day ->
            val selected = resolver.select(result, current,
                ProfessionalFortuneSelection(ProfessionalFortuneLayer.DAILY, day.observedAt))
            assertEquals("戊戌", selected.flowPillars.month)
            assertEquals(day.pillar, selected.flowPillars.day)
            assertEquals(day.key, selected.dailyTimeline.single { it.selected }.key)
        }
    }

    @Test
    fun `默认子时十二候选属于同一流日而不是民用日期拼接`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        listOf(CivilDateTime(2026, 10, 10, 15, 44, 0),
            CivilDateTime(2026, 10, 10, 23, 30, 0),
            CivilDateTime(2026, 10, 11, 0, 30, 0)).forEach { at ->
            val current = resolver.locate(result, at)
            assertEquals(12, current.hourlyTimeline.size)
            current.hourlyTimeline.forEach { hour ->
                val selected = resolver.select(result, current,
                    ProfessionalFortuneSelection(ProfessionalFortuneLayer.HOURLY, hour.observedAt))
                assertEquals(current.flowPillars.day, selected.flowPillars.day)
                assertEquals(current.flowPillars.month, selected.flowPillars.month)
                assertEquals(hour.pillar, selected.flowPillars.hour)
                assertEquals(hour.key, selected.hourlyTimeline.single { it.selected }.key)
            }
            assertEquals(current.flowPillars.day, current.dailyTimeline.single { it.selected }.pillar)
        }
    }

    @Test
    fun `全部节令的首末流日与流时候选不越过精确父级边界`() = runTest {
        listOf(RatHourRule.TYME_DEFAULT, RatHourRule.LATE_RAT_SAME_DAY).forEach { rule ->
            val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault(ratHourRule = rule))
            listOf(2024, 2026, 2033).forEach { year ->
                val firstJie = SolarTerm.fromName(year, "立春")
                (0 until 12).forEach { index ->
                    val jie = firstJie.next(index * 2)
                    val start = jie.julianDay.solarTime.toCivilDateTime().toLocalDateTime()
                    val end = jie.next(2).julianDay.solarTime.toCivilDateTime().toLocalDateTime()
                    listOf(start, end.minusSeconds(1)).forEach { at ->
                        val current = resolver.locate(result, at.toCivilDateTime())
                        current.dailyTimeline.forEach { day ->
                            val candidate = day.observedAt.toLocalDateTime()
                            assertTrue("$year ${jie.name} ${day.label}", candidate >= start && candidate < end)
                        }
                        current.hourlyTimeline.forEach { hour ->
                            val candidate = hour.observedAt.toLocalDateTime()
                            assertTrue("$year ${jie.name} ${hour.label}", candidate >= start && candidate < end)
                            val selected = resolver.locate(result, hour.observedAt)
                            assertEquals(current.flowPillars.month, selected.flowPillars.month)
                            assertEquals(current.flowPillars.day, selected.flowPillars.day)
                        }
                        assertEquals(current.flowPillars.day, current.dailyTimeline.single { it.selected }.pillar)
                        assertEquals(current.flowPillars.hour, current.hourlyTimeline.single { it.selected }.pillar)
                    }
                }
            }
        }
    }

    @Test
    fun `晚子同日规则分别呈现早子和晚子且不偷换日柱`() = runTest {
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault(
            ratHourRule = RatHourRule.LATE_RAT_SAME_DAY))
        val current = resolver.locate(result, CivilDateTime(2026, 10, 10, 15, 44, 0))
        assertEquals(13, current.hourlyTimeline.size)
        assertEquals("早子时", current.hourlyTimeline.first().subtitle)
        assertEquals("晚子时", current.hourlyTimeline.last().subtitle)
        assertNotEquals(current.hourlyTimeline.first().pillar, current.hourlyTimeline.last().pillar)
        current.hourlyTimeline.forEach { hour ->
            val selected = resolver.select(result, current,
                ProfessionalFortuneSelection(ProfessionalFortuneLayer.HOURLY, hour.observedAt))
            assertEquals(current.flowPillars.day, selected.flowPillars.day)
            assertEquals(hour.pillar, selected.flowPillars.hour)
            assertEquals(hour.key, selected.hourlyTimeline.single { it.selected }.key)
        }
    }

    @Test
    fun `天文台寒露分钟外部锚点前后定位不同流月`() = runTest {
        // HKO Almanac 2026: Cold Dew, 8 October 14:29 (Hong Kong civil time).
        val result = engine.calculate(sampleInput(), CalculationProfile.tymeDefault())
        val before = resolver.locate(result, CivilDateTime(2026, 10, 8, 14, 28, 0))
        val after = resolver.locate(result, CivilDateTime(2026, 10, 8, 14, 31, 0))
        assertEquals("丁酉", before.flowPillars.month)
        assertEquals("戊戌", after.flowPillars.month)
        assertEquals("9/7", before.dailyTimeline.first().label)
        assertEquals("10/8", after.dailyTimeline.first().label)
    }

    @Test
    fun `历法支持起点保留仍覆盖当前时刻的节令月与子时片段`() = runTest {
        val result = engine.calculate(sampleInput().copy(calendarInput =
            BirthCalendarInput.Solar(CivilDateTime(1800, 1, 1, 12, 0, 0))), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(1800, 1, 1, 12, 0, 0))
        assertEquals(1, current.monthlyTimeline.count { it.selected })
        assertEquals("1/1", current.dailyTimeline.first().label)
        assertEquals(12, current.hourlyTimeline.size)
        assertEquals("23:00", current.hourlyTimeline.first().label)
        assertEquals(CivilDateTime(1800, 1, 1, 0, 0, 0), current.hourlyTimeline.first().observedAt)
        assertTrue(current.dailyTimeline.all { it.observedAt.year >= 1800 })
    }

    @Test
    fun `立春前出生的童限保留上一节气年的流年且能选中`() = runTest {
        val result = engine.calculate(sampleInput().copy(calendarInput =
            BirthCalendarInput.Solar(CivilDateTime(1990, 1, 1, 12, 0, 0))), CalculationProfile.tymeDefault())
        val current = resolver.locate(result, CivilDateTime(1990, 1, 1, 12, 0, 0))
        assertEquals(1989, current.position.annualFortune.calendarYear)
        assertEquals("1989", current.annualTimeline.single { it.selected }.label)
        val year = current.annualTimeline.first()
        val selected = resolver.select(result, current,
            ProfessionalFortuneSelection(ProfessionalFortuneLayer.ANNUAL, year.observedAt))
        assertEquals(year.pillar, selected.flowPillars.year)
        assertEquals(year.key, selected.annualTimeline.single { it.selected }.key)
        assertEquals("0岁", selected.annualTimeline.single { it.selected }.subtitle)
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

private fun LocalDateTime.toCivilDateTime(): CivilDateTime =
    CivilDateTime(year, monthValue, dayOfMonth, hour, minute, second)
