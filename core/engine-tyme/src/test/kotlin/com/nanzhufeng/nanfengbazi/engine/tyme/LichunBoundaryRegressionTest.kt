package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.*
import com.nanzhufeng.nanfengbazi.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LichunBoundaryRegressionTest {
    @Test fun `2000 morning chart remains in rabbit year before precise lichun`() = runTest {
        val result = TymeBaziEngine().calculate(input(8), CalculationProfile.tymeDefault())
        assertEquals("己卯", result.fourPillars.year)
        assertEquals("丁丑", result.fourPillars.month)
        assertEquals("兔", result.basicChartDetails?.zodiac)
    }

    @Test fun `almanac scalar and detailed pillars share selected morning time`() = runTest {
        val result = TymeAlmanacReader().loadMonth(AlmanacMonthQuery(2000, 2, 4, 4)) as AlmanacResult.Completed
        assertEquals("己卯", result.month.selected.yearPillar)
        assertEquals("丁丑", result.month.selected.monthPillar)
        assertEquals(result.month.selected.yearPillar, result.month.selected.pillars[0].value)
    }

    @Test fun `same wall clock in Los Angeles is after lichun`() = runTest {
        val result = TymeBaziEngine().calculate(input(8).copy(timeZoneId = "America/Los_Angeles"), CalculationProfile.tymeDefault())
        assertEquals("庚辰", result.fourPillars.year)
        assertEquals("戊寅", result.fourPillars.month)
    }

    @Test fun `minute crossing lichun cannot produce a unique chart`() = runTest {
        val ambiguous = input(20).copy(
            calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 4, 20, 40, 0)),
            timePrecision = TimePrecision.EXACT_TO_MINUTE,
        )
        try {
            TymeBaziEngine().calculate(ambiguous, CalculationProfile.tymeDefault())
            fail("交节分钟不能强制采用单一年柱")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("待确认"))
            assertTrue(expected.message.orEmpty().contains("己卯"))
            assertTrue(expected.message.orEmpty().contains("庚辰"))
        }
    }

    @Test fun `fixed 2000 examples distinguish new year spring and civil midnight`() = runTest {
        val examples = listOf(
            CivilDateTime(2000,2,3,23,59,0) to "己卯",
            CivilDateTime(2000,2,4,0,0,0) to "己卯",
            CivilDateTime(2000,2,4,8,0,0) to "己卯",
            CivilDateTime(2000,2,4,20,39,59) to "己卯",
            CivilDateTime(2000,2,4,20,41,0) to "庚辰",
            CivilDateTime(2000,2,5,0,0,0) to "庚辰",
        )
        for ((date, year) in examples) {
            val r = TymeBaziEngine().calculate(input(0).copy(calendarInput = BirthCalendarInput.Solar(date)), CalculationProfile.tymeDefault())
            assertEquals(date.toString(), year, r.fourPillars.year)
            assertEquals(if (year == "己卯") "丁丑" else "戊寅", r.fourPillars.month)
            assertEquals(if (year == "己卯") "兔" else "龙", r.basicChartDetails?.zodiac)
        }
    }

    @Test fun `1901 to 2100 exact spring switches once and remains consistent with almanac`() = runTest {
        val engine = TymeBaziEngine()
        val almanac = TymeAlmanacReader()
        for (year in 1901..2100) {
            val spring = com.tyme.solar.SolarTerm.fromName(year, "立春").julianDay.solarTime
            val expectedYear = com.tyme.sixtycycle.SixtyCycleYear.fromYear(year).sixtyCycle.name
            for (seconds in listOf(-86400, -60, -1, 0, 1, 60, 86400)) {
                val t = spring.next(seconds).domainDateTime()
                val r = engine.calculate(input(0).copy(
                    calendarInput = BirthCalendarInput.Solar(t), timeZoneId = "Etc/GMT-8",
                ), CalculationProfile.tymeDefault())
                assertEquals("$year/$seconds", if (seconds < 0) com.tyme.sixtycycle.SixtyCycleYear.fromYear(year - 1).sixtyCycle.name else expectedYear, r.fourPillars.year)
                assertEquals(r.fourPillars.year.takeLast(1), r.basicChartDetails!!.pillars.first().earthBranch)
                if (seconds in -1..1) {
                    val view = (almanac.loadMonth(AlmanacMonthQuery(
                        t.year, t.month, t.day, AlmanacDoubleHours.indexForCivilHour(t.hour),
                        selectedHour = t.hour, selectedMinute = t.minute, selectedSecond = t.second,
                        timePrecision = TimePrecision.EXACT_TO_SECOND,
                    )) as AlmanacResult.Completed).month.selected
                    assertEquals(r.fourPillars.year, view.yearPillar)
                    assertEquals(r.fourPillars.month, view.monthPillar)
                    assertEquals(r.fourPillars.day, view.dayPillar)
                    assertEquals(r.fourPillars.hour, view.hourPillar)
                }
            }
        }
    }

    @Test fun `all twelve jie switch month and qi does not switch month`() = runTest {
        for (year in listOf(1984, 2000, 2001, 2021, 2026)) {
            for (i in 0..23) {
                val term = com.tyme.solar.SolarTerm.fromIndex(year, i)
                val t = term.julianDay.solarTime
                val before = TymeSolarYearMonthResolver.resolve(t.next(-1))
                val at = TymeSolarYearMonthResolver.resolve(t)
                if (term.isJie) assertNotEquals("$year/${term.name}", before.month.name, at.month.name)
                else assertEquals("$year/${term.name}", before.month.name, at.month.name)
            }
        }
    }

    @Test fun `same instant has same year month despite timezone longitude and device timezone`() = runTest {
        val engine = TymeBaziEngine()
        val china = input(20).copy(calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000,2,4,20,30,0)))
        val america = china.copy(calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000,2,4,4,30,0)), timeZoneId = "America/Los_Angeles")
        val oldZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Auckland"))
            val a = engine.calculate(china, CalculationProfile.tymeDefault())
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            val b = engine.calculate(america, CalculationProfile.tymeDefault())
            assertEquals(a.fourPillars.year, b.fourPillars.year)
            assertEquals(a.fourPillars.month, b.fourPillars.month)
            assertEquals(a.solarYearMonthEvidence!!.birthInstantUtc, b.solarYearMonthEvidence!!.birthInstantUtc)
            for (longitude in listOf(75.0, 135.0)) {
                val c = engine.calculate(china.copy(useTrueSolarTime = true, longitude = longitude, latitude = 30.0),
                    CalculationProfile.tymeDefault(solarTimeMode = SolarTimeMode.TRUE_SOLAR_TIME))
                assertEquals(a.fourPillars.year, c.fourPillars.year)
                assertEquals(a.fourPillars.month, c.fourPillars.month)
            }
        } finally { java.util.TimeZone.setDefault(oldZone) }
    }

    @Test fun `unknown minute and shichen spanning jie are explicit candidates in almanac`() = runTest {
        val reader = TymeAlmanacReader()
        val minute = (reader.loadMonth(AlmanacMonthQuery(2000,2,4,10, selectedHour=20,selectedMinute=40,
            timePrecision=TimePrecision.EXACT_TO_MINUTE)) as AlmanacResult.Completed).month.selected
        assertTrue(minute.pillars.isEmpty())
        assertTrue(minute.boundaryNotice!!.contains("己卯/庚辰"))
        val shichen = (reader.loadMonth(AlmanacMonthQuery(2000,2,4,10)) as AlmanacResult.Completed).month.selected
        assertTrue(shichen.pillars.isEmpty())
        val precise = (reader.loadMonth(AlmanacMonthQuery(2000,2,4,10,selectedHour=20,selectedMinute=41,
            timePrecision=TimePrecision.EXACT_TO_MINUTE)) as AlmanacResult.Completed).month.selected
        assertEquals("庚辰", precise.yearPillar)
        assertEquals("戊寅", precise.monthPillar)
    }

    @Test fun `historical Shanghai offset is not silently treated as fixed China standard time`() = runTest {
        val spring = com.tyme.solar.SolarTerm.fromName(1900, "立春").julianDay.solarTime
        val old = input(0).copy(calendarInput=BirthCalendarInput.Solar(spring.domainDateTime()),timeZoneId="Asia/Shanghai")
        val r = TymeBaziEngine().calculate(old, CalculationProfile.tymeDefault())
        assertEquals(29143, r.normalizedInput.resolvedUtcOffsetSeconds)
        assertEquals("己亥", r.fourPillars.year)
        val shifted = spring.next(343).domainDateTime()
        val at = TymeBaziEngine().calculate(old.copy(calendarInput=BirthCalendarInput.Solar(shifted)), CalculationProfile.tymeDefault())
        assertEquals("庚子", at.fourPillars.year)
    }

    @Test fun `overseas reverse lookup includes the correct solar year on boundary date`() = runTest {
        val source = TymeBaziEngine().calculate(input(8).copy(timeZoneId="America/Los_Angeles"), CalculationProfile.tymeDefault())
        val result = TymeFourPillarsLookup().search(FourPillarsLookupQuery(source.fourPillars,2000,2000,"America/Los_Angeles",RatHourRule.TYME_DEFAULT)) as FourPillarsLookupResult.Completed
        assertTrue(result.candidates.any { it.civilDateTime.month == 2 && it.civilDateTime.day == 4 && it.civilDateTime.hour in 7..8 })
        assertTrue(result.candidates.all { it.fourPillars == source.fourPillars })
    }

    @Test fun `locked provider agrees with independent development reference within reviewed budget`() {
        val resource = requireNotNull(javaClass.getResourceAsStream("/lichun-reference-1901-2100.csv"))
        val rows = resource.bufferedReader().readLines().drop(1)
        assertEquals(200, rows.size)
        var last: java.time.Instant? = null
        for (row in rows) {
            val parts = row.split(',')
            val year = parts[0].toInt()
            val actual = com.tyme.solar.SolarTerm.fromName(year,"立春").julianDay.solarTime.localDateTime()
                .toInstant(java.time.ZoneOffset.ofHours(8))
            assertEquals(parts[1], actual.toString())
            assertTrue(last == null || actual > last)
            last = actual
            assertTrue(actual.atOffset(java.time.ZoneOffset.ofHours(8)).dayOfMonth in 2..5)
            if (year in 1950..2050) {
                val independent = java.time.OffsetDateTime.parse(parts[2]).toInstant()
                val delta = kotlin.math.abs(java.time.Duration.between(independent,actual).toNanos()/1e9)
                assertTrue("$year: $delta s exceeds review threshold", delta <= 5.0)
            }
        }
    }

    @Test fun `1984 jiazi anchor handles the preceding negative cycle offset`() {
        val t = com.tyme.solar.SolarTerm.fromName(1984,"立春").julianDay.solarTime
        assertEquals("癸亥",TymeSolarYearMonthResolver.resolve(t.next(-1)).year.name)
        assertEquals("甲子",TymeSolarYearMonthResolver.resolve(t).year.name)
        assertEquals("甲子",TymeSolarYearMonthResolver.resolve(t.next(1)).year.name)
    }

    private fun input(hour: Int) = BirthInput(
        calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 4, hour, 0, 0)),
        sexForFortuneDirection = SexForFortuneDirection.WOMAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
    )
}
