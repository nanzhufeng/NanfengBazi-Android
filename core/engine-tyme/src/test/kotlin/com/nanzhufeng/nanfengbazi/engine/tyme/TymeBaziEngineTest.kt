package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.LuckStartRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.tyme.eightchar.ChildLimit
import com.tyme.eightchar.provider.impl.China95ChildLimitProvider
import com.tyme.solar.SolarTerm
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
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
    fun `真太阳时请求必须明确失败`() {
        val input = solarInput(1986, 5, 29, 13, 37, 0).copy(useTrueSolarTime = true)
        val profile = CalculationProfile.tymeDefault().copy(
            solarTimeMode = SolarTimeMode.TRUE_SOLAR_TIME,
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                engine.calculate(input, profile)
            }
        }
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
}

private fun com.tyme.solar.SolarTime.toBirthInput(): BirthInput = BirthInput(
    calendarInput = BirthCalendarInput.Solar(
        CivilDateTime(year, month, day, hour, minute, second),
    ),
    sexForFortuneDirection = SexForFortuneDirection.MAN,
    timePrecision = TimePrecision.EXACT_TO_SECOND,
)
