package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.*
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CaseYearZodiacNoticeTest {
    private val input = BirthInput(
        calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 4, 8, 0, 0)),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
        timeZoneId = "Etc/GMT-8",
    )

    private suspend fun chart(birth: BirthInput = input) =
        TymeBaziEngine().calculate(birth, CalculationProfile.tymeDefault())

    @Test fun `正确旧盘不因缺少年界证据而提示`() = runTest {
        val saved = chart().copy(solarYearMonthEvidence = null)
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `截图对应日期的正常旧盘保持安静`() = runTest {
        val saved = chart(input.copy(calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(1989, 7, 8, 16, 0, 0),
        ))).copy(solarYearMonthEvidence = null)
        assertEquals("己巳", saved.fourPillars.year)
        assertEquals("蛇", saved.basicChartDetails?.zodiac)
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `仅年柱错误只提示年柱且保留旧快照`() = runTest {
        val correct = chart()
        val saved = correct.copy(
            fourPillars = correct.fourPillars.copy(year = "庚辰"),
            solarYearMonthEvidence = null,
        )
        assertEquals("年柱应为己卯；请核对出生资料后重新排盘。", resolveCaseYearZodiacNotice(saved))
        assertEquals("庚辰", saved.fourPillars.year)
        assertNull(saved.solarYearMonthEvidence)
    }

    @Test fun `仅属相错误只提示属相`() = runTest {
        val saved = chart().let { it.copy(
            basicChartDetails = it.basicChartDetails!!.copy(zodiac = "龙"),
            solarYearMonthEvidence = null,
        ) }
        assertEquals("属相应为兔；请核对出生资料后重新排盘。", resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `年柱属相同时错误合并提示且新证据不掩盖问题`() = runTest {
        val saved = chart().let { it.copy(
            fourPillars = it.fourPillars.copy(year = "庚辰"),
            basicChartDetails = it.basicChartDetails!!.copy(zodiac = "龙"),
        ) }
        assertEquals("年柱应为己卯，属相应为兔；请核对出生资料后重新排盘。", resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `同属相但年干错误仍提示年柱`() = runTest {
        val saved = chart().let { it.copy(fourPillars = it.fourPillars.copy(year = "丁卯")) }
        assertEquals("年柱应为己卯；请核对出生资料后重新排盘。", resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `缺少旧属相字段不等于属相有误`() = runTest {
        val saved = chart().copy(basicChartDetails = null, solarYearMonthEvidence = null)
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `仅月柱有差异不显示年柱属相提示`() = runTest {
        val saved = chart().let { it.copy(
            fourPillars = it.fourPillars.copy(month = "戊寅"), solarYearMonthEvidence = null,
        ) }
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `同一民用时刻按已保存海外时区核对`() = runTest {
        val saved = chart(input.copy(timeZoneId = "America/Los_Angeles"))
            .copy(solarYearMonthEvidence = null)
        assertEquals("庚辰", saved.fourPillars.year)
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `农历旧盘共用原引擎历法转换`() = runTest {
        val saved = chart(input.copy(calendarInput = BirthCalendarInput.Lunar(
            LunarDateTime(2000, 1, 1, 8, 0, 0, false),
        ))).copy(solarYearMonthEvidence = null)
        assertEquals("庚辰", saved.fourPillars.year)
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `交节分钟属于合法候选时不误报确定错误`() = runTest {
        val saved = chart().let { it.copy(
            normalizedInput = it.normalizedInput.copy(
                calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 4, 20, 40, 0)),
                timePrecision = TimePrecision.EXACT_TO_MINUTE,
            ),
            solarYearMonthEvidence = null,
        ) }
        assertNull(resolveCaseYearZodiacNotice(saved))
    }

    @Test fun `时区无法核实时不把未知误报成年柱错误`() = runTest {
        val saved = chart().let { it.copy(
            normalizedInput = it.normalizedInput.copy(timeZoneId = "Invalid/Zone"),
            solarYearMonthEvidence = null,
        ) }
        assertNull(resolveCaseYearZodiacNotice(saved))
    }
}
