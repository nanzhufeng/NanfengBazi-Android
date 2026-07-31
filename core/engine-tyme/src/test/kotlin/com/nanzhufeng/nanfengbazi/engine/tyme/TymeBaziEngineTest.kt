package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.LuckStartRule
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermType
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.tyme.eightchar.ChildLimit
import com.tyme.eightchar.provider.impl.China95ChildLimitProvider
import com.tyme.eightchar.provider.impl.LunarSect2EightCharProvider
import com.tyme.lunar.LunarHour
import com.tyme.solar.SolarTerm
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TymeBaziEngineTest {
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-01-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
    private val engine = TymeBaziEngine(fixedClock)

    @Test
    fun `公开样本四柱和衍生字段与 Tyme4j 一致`() = runTest {
        val result = engine.calculate(
            input = solarInput(1986, 5, 29, 13, 37, 0),
            profile = CalculationProfile.tymeDefault(),
        )

        assertEquals("丙寅", result.fourPillars.year)
        assertEquals("癸巳", result.fourPillars.month)
        assertEquals("癸酉", result.fourPillars.day)
        assertEquals("己未", result.fourPillars.hour)
        assertEquals("癸巳", result.ownSign)
        assertEquals("辛丑", result.bodySign)
        assertEquals("甲申", result.fetalOrigin)
        assertEquals("戊辰", result.fetalBreath)
        assertEquals(8, result.decadeFortunes.size)
        assertEquals("1.5.1", result.evidence.engineVersion)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), result.evidence.calculatedAt)
        assertEquals(CalendarSystem.SOLAR, result.calendarConversion?.inputCalendarSystem)
    }

    @Test
    fun `流年基础覆盖出生年到第八步大运结束且保留精确十年区间`() = runTest {
        val result = engine.calculate(
            input = solarInput(1992, 8, 24, 12, 0, 0),
            profile = CalculationProfile.tymeDefault(),
        )

        assertEquals(1992, result.annualFortunes.first().calendarYear)
        assertEquals("壬申", result.annualFortunes.first().name)
        assertEquals(1, result.annualFortunes.first().nominalAge)
        assertEquals(
            result.decadeFortunes.last().endYear,
            result.annualFortunes.last().calendarYear,
        )
        assertEquals(
            result.annualFortunes.first().calendarYear
                .rangeTo(result.annualFortunes.last().calendarYear)
                .toList(),
            result.annualFortunes.map { it.calendarYear },
        )
        result.decadeFortunes.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endAtExclusive, next.startAt)
        }
        assertTrue(result.decadeFortunes.all { it.startAt != null })
        assertTrue(result.decadeFortunes.all { it.endAtExclusive != null })
    }

    @Test
    fun `当前岁运按精确交运时刻和立春定位`() = runTest {
        val result = engine.calculate(
            input = solarInput(1992, 8, 24, 12, 0, 0),
            profile = CalculationProfile.tymeDefault(),
        )
        val resolver = TymeFortunePositionResolver()
        val firstStart = requireNotNull(result.decadeFortunes.first().startAt)
        val beforeStart = firstStart.toLocalDateTime().minusSeconds(1).toCivilDateTime()

        val before = resolver.locate(result, beforeStart)
        val at = resolver.locate(result, firstStart)
        assertEquals(FortunePositionStatus.BEFORE_FIRST_DECADE, before.status)
        assertEquals(null, before.decadeFortune)
        assertEquals(FortunePositionStatus.WITHIN_DECADE, at.status)
        assertEquals(result.decadeFortunes.first(), at.decadeFortune)

        val spring = SolarTerm.fromIndex(2026, 3).julianDay.solarTime
        val beforeSpring = resolver.locate(result, spring.next(-1).toCivilDateTime())
        val atSpring = resolver.locate(result, spring.toCivilDateTime())
        assertEquals("乙巳", beforeSpring.annualFortune.name)
        assertEquals(2025, beforeSpring.annualFortune.calendarYear)
        assertEquals("丙午", atSpring.annualFortune.name)
        assertEquals(2026, atSpring.annualFortune.calendarYear)
    }

    @Test
    fun `同一时刻的公历与农历输入得到相同命盘`() = runTest {
        val solar = engine.calculate(
            solarInput(2023, 1, 22, 13, 0, 0),
            CalculationProfile.tymeDefault(),
        )
        val lunar = engine.calculate(
            lunarInput(2023, 1, 1, 13, 0, 0),
            CalculationProfile.tymeDefault(),
        )

        assertEquals(solar.fourPillars, lunar.fourPillars)
        assertEquals(solar.fortuneStart, lunar.fortuneStart)
        assertEquals(
            CivilDateTime(2023, 1, 22, 13, 0, 0),
            lunar.calendarConversion?.solarDateTime,
        )
        assertEquals(CalendarSystem.LUNAR, lunar.calendarConversion?.inputCalendarSystem)
    }

    @Test
    fun `闰月输入可转换并保留闰月证据`() = runTest {
        val result = engine.calculate(
            lunarInput(2023, 2, 1, 10, 30, 0, isLeapMonth = true),
            CalculationProfile.tymeDefault(),
        )

        assertEquals(
            CivilDateTime(2023, 3, 22, 10, 30, 0),
            result.calendarConversion?.solarDateTime,
        )
        assertEquals(2, result.calendarConversion?.lunarDateTime?.month)
        assertTrue(result.calendarConversion?.lunarDateTime?.isLeapMonth == true)
    }

    @Test
    fun `计算快照保存解析后的 offset 和时区数据版本`() = runTest {
        val result = engine.calculate(
            solarInput(2024, 1, 15, 12, 0, 0).copy(
                timeZoneId = "America/New_York",
            ),
            CalculationProfile.tymeDefault(),
        )

        assertEquals(-18_000, result.normalizedInput.resolvedUtcOffsetSeconds)
        assertTrue(result.normalizedInput.timeZoneDataVersion.orEmpty().isNotBlank())
    }

    @Test
    fun `夏令时重叠要求选择 offset 且选择后可计算`() = runTest {
        val input = solarInput(2024, 11, 3, 1, 30, 0).copy(
            timeZoneId = "America/New_York",
        )

        val error = assertThrows(TimeZoneChoiceRequiredException::class.java) {
            kotlinx.coroutines.runBlocking {
                engine.calculate(input, CalculationProfile.tymeDefault())
            }
        }
        assertEquals(listOf(-14_400, -18_000), error.validUtcOffsetSeconds)

        val resolved = engine.calculate(
            input.copy(resolvedUtcOffsetSeconds = -18_000),
            CalculationProfile.tymeDefault(),
        )
        assertEquals(-18_000, resolved.normalizedInput.resolvedUtcOffsetSeconds)
    }

    @Test
    fun `夏令时不存在时刻明确拒绝计算`() {
        val input = solarInput(2024, 3, 10, 2, 30, 0).copy(
            timeZoneId = "America/New_York",
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                engine.calculate(input, CalculationProfile.tymeDefault())
            }
        }
        assertTrue(error.message.orEmpty().contains("不存在"))
    }

    @Test
    fun `不存在的闰月必须以中文明确失败`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                engine.calculate(
                    lunarInput(2022, 2, 1, 10, 30, 0, isLeapMonth = true),
                    CalculationProfile.tymeDefault(),
                )
            }
        }

        assertTrue(error.message.orEmpty().contains("农历日期无效"))
    }

    @Test
    fun `立春交节瞬间切换年柱`() = runTest {
        val spring = SolarTerm.fromIndex(2025, 3).julianDay.solarTime
        val before = spring.next(-1)

        val beforeResult = engine.calculate(before.toBirthInput(), CalculationProfile.tymeDefault())
        val atResult = engine.calculate(spring.toBirthInput(), CalculationProfile.tymeDefault())

        assertEquals("甲辰", beforeResult.fourPillars.year)
        assertEquals("乙巳", atResult.fourPillars.year)
        assertNotEquals(beforeResult.fourPillars.year, atResult.fourPillars.year)
    }

    @Test
    fun `1900到2100跨年段立春前时后1秒同步切换年柱生肖与前后节`() = runTest {
        val zodiacByBranch = mapOf(
            '子' to "鼠",
            '丑' to "牛",
            '寅' to "虎",
            '卯' to "兔",
            '辰' to "龙",
            '巳' to "蛇",
            '午' to "马",
            '未' to "羊",
            '申' to "猴",
            '酉' to "鸡",
            '戌' to "狗",
            '亥' to "猪",
        )

        (1900..2100 step 10).forEach { year ->
            val spring = SolarTerm.fromIndex(year, 3).julianDay.solarTime
            val before = engine.calculate(
                spring.next(-1).toBirthInput(),
                CalculationProfile.tymeDefault(),
            )
            val at = engine.calculate(
                spring.toBirthInput(),
                CalculationProfile.tymeDefault(),
            )
            val after = engine.calculate(
                spring.next(1).toBirthInput(),
                CalculationProfile.tymeDefault(),
            )

            assertNotEquals("$year 立春前后年柱必须切换", before.fourPillars.year, at.fourPillars.year)
            assertEquals("$year 立春后一秒年柱必须稳定", at.fourPillars.year, after.fourPillars.year)
            assertEquals(
                "$year 立春前生肖必须跟随年柱地支",
                zodiacByBranch.getValue(before.fourPillars.year.last()),
                before.basicChartDetails?.zodiac,
            )
            assertEquals(
                "$year 立春时生肖必须跟随新年柱地支",
                zodiacByBranch.getValue(at.fourPillars.year.last()),
                at.basicChartDetails?.zodiac,
            )
            assertEquals(at.basicChartDetails?.zodiac, after.basicChartDetails?.zodiac)
            assertEquals("大寒", before.basicChartDetails?.previousSolarTerm?.name)
            assertEquals("立春", before.basicChartDetails?.nextSolarTerm?.name)
            assertEquals("小寒", before.basicChartDetails?.previousJie?.name)
            assertEquals("立春", before.basicChartDetails?.nextJie?.name)
            assertEquals("立春", at.basicChartDetails?.previousSolarTerm?.name)
            assertEquals("雨水", at.basicChartDetails?.nextSolarTerm?.name)
            assertEquals("立春", at.basicChartDetails?.previousJie?.name)
            assertEquals("惊蛰", at.basicChartDetails?.nextJie?.name)
        }
    }

    @Test
    fun `计算完成后恢复调用方原有 provider`() = runTest {
        val original = ChildLimit.provider
        val marker = China95ChildLimitProvider()
        try {
            ChildLimit.provider = marker

            engine.calculate(
                solarInput(1986, 5, 29, 13, 37, 0),
                CalculationProfile.tymeDefault(),
            )

            assertSame(marker, ChildLimit.provider)
        } finally {
            ChildLimit.provider = original
        }
    }

    @Test
    fun `不同起运配置互不污染`() = runTest {
        val input = solarInput(1986, 5, 29, 13, 37, 0)
        val defaultResult = engine.calculate(input, CalculationProfile.tymeDefault())
        val china95Result = engine.calculate(
            input,
            CalculationProfile.tymeDefault().copy(
                id = "china-95-v1",
                luckStartRule = LuckStartRule.CHINA_95,
            ),
        )
        val defaultAgain = engine.calculate(input, CalculationProfile.tymeDefault())

        assertNotEquals(defaultResult.fortuneStart.endAt, china95Result.fortuneStart.endAt)
        assertEquals(defaultResult.fortuneStart, defaultAgain.fortuneStart)
    }

    @Test
    fun `晚子时规则只在二十三点保留当天日柱`() = runTest {
        val defaultProfile = CalculationProfile.tymeDefault()
        val lateRatProfile = CalculationProfile.tymeDefault(
            ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
        )

        val beforeDefault = engine.calculate(
            solarInput(2026, 7, 30, 22, 59, 59),
            defaultProfile,
        )
        val beforeLate = engine.calculate(
            solarInput(2026, 7, 30, 22, 59, 59),
            lateRatProfile,
        )
        val atDefault = engine.calculate(
            solarInput(2026, 7, 30, 23, 0, 0),
            defaultProfile,
        )
        val atLate = engine.calculate(
            solarInput(2026, 7, 30, 23, 0, 0),
            lateRatProfile,
        )
        val midnightDefault = engine.calculate(
            solarInput(2026, 7, 31, 0, 0, 0),
            defaultProfile,
        )
        val midnightLate = engine.calculate(
            solarInput(2026, 7, 31, 0, 0, 0),
            lateRatProfile,
        )

        assertEquals(beforeDefault.fourPillars, beforeLate.fourPillars)
        assertEquals(beforeDefault.fourPillars.day, atLate.fourPillars.day)
        assertEquals(midnightDefault.fourPillars.day, atDefault.fourPillars.day)
        assertNotEquals(atDefault.fourPillars.day, atLate.fourPillars.day)
        assertEquals(atDefault.fourPillars.hour, atLate.fourPillars.hour)
        assertEquals(midnightDefault.fourPillars, midnightLate.fourPillars)
        assertEquals(
            "tyme-late-rat-same-day-v1",
            atLate.profile.id,
        )
        assertEquals("stage7b-rat-hour-v1", atLate.profile.ruleVersion)
    }

    @Test
    fun `子时计算不读取或改写调用方全局八字 provider`() = runTest {
        val original = LunarHour.provider
        val marker = LunarSect2EightCharProvider()
        try {
            LunarHour.provider = marker
            val default = engine.calculate(
                solarInput(2026, 7, 30, 23, 0, 0),
                CalculationProfile.tymeDefault(),
            )
            val late = engine.calculate(
                solarInput(2026, 7, 30, 23, 0, 0),
                CalculationProfile.tymeDefault(
                    ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
                ),
            )

            assertNotEquals(default.fourPillars.day, late.fourPillars.day)
            assertSame(marker, LunarHour.provider)
        } finally {
            LunarHour.provider = original
        }
    }

    @Test
    fun `真太阳时按民用时年月至校正后当地日时组合四柱`() = runTest {
        val input = solarInput(1992, 8, 24, 13, 4, 0).copy(
            locationName = "江苏省宿迁市泗阳县",
            longitude = 118.68,
            latitude = 33.73,
            useTrueSolarTime = true,
        )
        val profile = CalculationProfile.tymeDefault(SolarTimeMode.TRUE_SOLAR_TIME)

        val civil = engine.calculate(
            input.copy(useTrueSolarTime = false),
            CalculationProfile.tymeDefault(),
        )
        val correctedCivil = engine.calculate(
            solarInput(1992, 8, 24, 12, 56, 23),
            CalculationProfile.tymeDefault(),
        )
        val result = engine.calculate(input, profile)

        assertEquals(civil.fourPillars.year, result.fourPillars.year)
        assertEquals(civil.fourPillars.month, result.fourPillars.month)
        assertEquals(correctedCivil.fourPillars.day, result.fourPillars.day)
        assertEquals(correctedCivil.fourPillars.hour, result.fourPillars.hour)
        assertEquals(FourPillars("壬申", "戊申", "壬申", "丙午"), result.fourPillars)
        assertNotEquals(civil.fourPillars.hour, result.fourPillars.hour)
        assertTrue(result.trueSolarTimeEvidence?.crossesDoubleHour == true)
        assertEquals("tyme-true-solar-provisional-v1", result.profile.id)
        assertTrue(result.warnings.any { it.code == "TRUE_SOLAR_BOUNDARY_RULE_PROVISIONAL" })
        assertTrue(result.warnings.any { it.code == "TRUE_SOLAR_CROSSES_DOUBLE_HOUR" })
    }

    @Test
    fun `问真截图样本四柱一致且真太阳时分钟差不超过一分钟`() = runTest {
        val result = engine.calculate(
            solarInput(1992, 8, 24, 12, 0, 0).copy(
                locationName = "江苏省宿迁市泗阳县",
                longitude = 118.68,
                latitude = 33.73,
                useTrueSolarTime = true,
            ),
            CalculationProfile.tymeDefault(SolarTimeMode.TRUE_SOLAR_TIME),
        )

        assertEquals(FourPillars("壬申", "戊申", "壬申", "丙午"), result.fourPillars)
        val trueSolar = requireNotNull(result.trueSolarTimeEvidence).trueSolarDateTime
        assertEquals(CivilDateTime(1992, 8, 24, 11, 52, 23), trueSolar)
        val actual = java.time.LocalDateTime.of(
            trueSolar.year,
            trueSolar.month,
            trueSolar.day,
            trueSolar.hour,
            trueSolar.minute,
            trueSolar.second,
        )
        assertTrue(
            kotlin.math.abs(
                java.time.Duration.between(
                    java.time.LocalDateTime.of(1992, 8, 24, 11, 53, 0),
                    actual,
                ).seconds,
            ) <= 60,
        )

        val basic = requireNotNull(result.basicChartDetails)
        assertEquals("猴", basic.zodiac)
        assertEquals("处女", basic.westernZodiac)
        assertEquals("壬", basic.dayMaster)
        assertEquals("处暑", basic.previousSolarTerm.name)
        assertEquals("白露", basic.nextSolarTerm.name)
        assertEquals("立秋", basic.previousJie?.name)
        assertEquals("白露", basic.nextJie?.name)
        assertEquals(SolarTermType.JIE, basic.previousJie?.type)
        assertEquals(SolarTermType.JIE, basic.nextJie?.type)
        val pillars = basic.pillars.associateBy { it.position }
        assertEquals(
            listOf("比肩", "七杀", "比肩", "偏财"),
            PillarPosition.entries.map { requireNotNull(pillars[it]).primaryTenGod },
        )
        assertEquals(
            listOf("剑锋金", "大驿土", "剑锋金", "天河水"),
            PillarPosition.entries.map { requireNotNull(pillars[it]).naYin },
        )
        assertEquals(
            listOf("长生", "长生", "长生", "胎"),
            PillarPosition.entries.map { requireNotNull(pillars[it]).terrain },
        )
        assertEquals(
            listOf("长生", "病", "长生", "帝旺"),
            PillarPosition.entries.map { requireNotNull(pillars[it]).selfSittingTerrain },
        )
        assertEquals(
            listOf("庚", "壬", "戊"),
            requireNotNull(pillars[PillarPosition.YEAR]).hiddenStems.map { it.heavenStem },
        )
        assertEquals(
            listOf("偏印", "比肩", "七杀"),
            requireNotNull(pillars[PillarPosition.YEAR]).hiddenStems.map { it.tenGod },
        )
        assertEquals(
            listOf("戌", "亥"),
            requireNotNull(pillars[PillarPosition.YEAR]).voidEarthBranches,
        )
        assertEquals(
            listOf("寅", "卯"),
            requireNotNull(pillars[PillarPosition.MONTH]).voidEarthBranches,
        )
    }

    @Test
    fun `真太阳时跨日仍保留民用时间年柱月柱`() = runTest {
        val input = solarInput(2023, 1, 22, 2, 0, 0).copy(
            locationName = "新疆乌鲁木齐",
            longitude = 87.6,
            latitude = 43.8,
            useTrueSolarTime = true,
        )

        val civil = engine.calculate(
            input.copy(useTrueSolarTime = false),
            CalculationProfile.tymeDefault(),
        )
        val correctedCivil = engine.calculate(
            solarInput(2023, 1, 21, 23, 38, 59),
            CalculationProfile.tymeDefault(),
        )
        val result = engine.calculate(
            input,
            CalculationProfile.tymeDefault(SolarTimeMode.TRUE_SOLAR_TIME),
        )

        assertEquals(civil.fourPillars.year, result.fourPillars.year)
        assertEquals(civil.fourPillars.month, result.fourPillars.month)
        assertEquals(correctedCivil.fourPillars.day, result.fourPillars.day)
        assertEquals(correctedCivil.fourPillars.hour, result.fourPillars.hour)
        assertTrue(result.trueSolarTimeEvidence?.crossesDate == true)
        assertTrue(result.warnings.any { it.code == "TRUE_SOLAR_CROSSES_DATE" })
    }

    @Test
    fun `真太阳时缺少经纬度必须明确失败`() {
        val input = solarInput(1986, 5, 29, 13, 37, 0).copy(
            useTrueSolarTime = true,
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                engine.calculate(
                    input,
                    CalculationProfile.tymeDefault(SolarTimeMode.TRUE_SOLAR_TIME),
                )
            }
        }

        assertTrue(error.message.orEmpty().contains("经度和纬度"))
    }

    @Test
    fun `时辰未知不能伪造唯一命盘`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                engine.calculate(
                    solarInput(1986, 5, 29, 13, 0, 0).copy(
                        timePrecision = TimePrecision.UNKNOWN,
                    ),
                    CalculationProfile.tymeDefault(),
                )
            }
        }

        assertTrue(error.message.orEmpty().contains("候选时间"))
    }

    @Test
    fun `黄金样本资源包含版本和期望字段`() {
        val json = requireNotNull(
            javaClass.classLoader.getResource("golden-cases-v1.json"),
        ).readText()

        assert(json.contains("\"schemaVersion\": 1"))
        assert(json.contains("\"engineVersion\": \"1.5.1\""))
        assert(json.contains("\"expected\""))
    }

    private fun solarInput(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): BirthInput = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(year, month, day, hour, minute, second),
        ),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
    )

    private fun lunarInput(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        isLeapMonth: Boolean = false,
    ): BirthInput = BirthInput(
        calendarInput = BirthCalendarInput.Lunar(
            LunarDateTime(year, month, day, hour, minute, second, isLeapMonth),
        ),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
    )
}

private fun com.tyme.solar.SolarTime.toBirthInput(): BirthInput = BirthInput(
    calendarInput = BirthCalendarInput.Solar(
        CivilDateTime(year, month, day, hour, minute, second),
    ),
    sexForFortuneDirection = SexForFortuneDirection.MAN,
    timePrecision = TimePrecision.EXACT_TO_SECOND,
)

private fun com.tyme.solar.SolarTime.toCivilDateTime(): CivilDateTime =
    CivilDateTime(year, month, day, hour, minute, second)

private fun CivilDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, month, day, hour, minute, second)

private fun LocalDateTime.toCivilDateTime(): CivilDateTime =
    CivilDateTime(year, monthValue, dayOfMonth, hour, minute, second)
