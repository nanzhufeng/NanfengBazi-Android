package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.nlf.calendar.Solar as LunarJavaSolar
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FourPillarsGoldenMatrixTest {
    private val engine = TymeBaziEngine()

    @Test
    fun `六十个冻结样本与独立历法实现逐柱一致`() = runTest {
        val evidenceContract = loadEvidenceContract()
        val cases = loadCases()
        assertTrue("黄金样本必须至少 50 个", cases.size >= 50)
        assertEquals("黄金样本 id 必须唯一", cases.size, cases.map { it.id }.distinct().size)
        assertTrue("标准案例必须至少包含 10 个男性样本", cases.count { it.sex == SexForFortuneDirection.MAN } >= 10)
        assertTrue("标准案例必须至少包含 10 个女性样本", cases.count { it.sex == SexForFortuneDirection.WOMAN } >= 10)
        assertTrue(evidenceContract.contains("time-precision:EXACT_TO_SECOND"))
        assertTrue(evidenceContract.contains("time-source:OTHER_RECORD"))
        assertTrue(evidenceContract.contains("source-note:SYNTHETIC_TEST_FIXTURE"))
        assertTrue(evidenceContract.contains("timezone:Asia/Shanghai"))
        assertTrue(evidenceContract.contains("offset-seconds:RESOLVED_FROM_TZDB_PER_INSTANT"))
        assertTrue(evidenceContract.contains("coordinates:NOT_REQUIRED_CIVIL_TIME"))
        assertTrue(evidenceContract.contains("NOT_AVAILABLE_REQUIRES_AUTHORIZED_WENZHEN_SAMPLE"))
        assertTrue(evidenceContract.contains("AUTOMATED_DIFFERENTIAL"))

        cases.forEach { golden ->
            val input = BirthInput(
                calendarInput = BirthCalendarInput.Solar(golden.dateTime.toCivilDateTime()),
                sexForFortuneDirection = golden.sex,
                timePrecision = TimePrecision.EXACT_TO_SECOND,
                timeZoneId = "Asia/Shanghai",
                timeSourceType = TimeSourceType.OTHER_RECORD,
                sourceNote = "SYNTHETIC_TEST_FIXTURE",
            )
            val actual = engine.calculate(input, CalculationProfile.tymeDefault())
            assertEquals("${golden.id} 的冻结四柱发生变化", golden.expected, actual.fourPillars)
            assertEquals(
                "${golden.id} 与 lunar-java 1.7.7 差分不一致",
                golden.expected,
                golden.referencePillars(),
            )
            assertEquals("${golden.id} 必须生成十二步大运", 12, actual.decadeFortunes.size)
            assertTrue(
                "${golden.id} 必须冻结时区解析结果",
                actual.normalizedInput.resolvedUtcOffsetSeconds != null,
            )
            assertTrue(
                "${golden.id} 大运年龄区间必须可复算",
                actual.decadeFortunes.all { it.startAge <= it.endAge },
            )
        }
    }

    private fun loadEvidenceContract(): String {
        val resource = requireNotNull(
            javaClass.classLoader.getResource("four-pillars-golden-v2.psv"),
        )
        return resource.readText()
            .lineSequence()
            .map(String::trim)
            .filter { it.startsWith("#") }
            .joinToString("\n")
    }

    private fun loadCases(): List<GoldenCase> {
        val resource = requireNotNull(
            javaClass.classLoader.getResource("four-pillars-golden-v2.psv"),
        )
        return resource.readText()
            .lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                val fields = line.split('|')
                require(fields.size == 10) { "非法黄金样本行：$line" }
                require(fields[1] == "lunar-java-1.7.7")
                require(fields[2] == "solar")
                require(fields[4] == "false")
                GoldenCase(
                    id = fields[0],
                    dateTime = LocalDateTime.parse(fields[3]),
                    sex = SexForFortuneDirection.valueOf(fields[5]),
                    expected = FourPillars(
                        year = fields[6],
                        month = fields[7],
                        day = fields[8],
                        hour = fields[9],
                    ),
                    source = fields[1],
                )
            }
            .toList()
    }
}

private data class GoldenCase(
    val id: String,
    val dateTime: LocalDateTime,
    val sex: SexForFortuneDirection,
    val expected: FourPillars,
    val source: String,
) {
    fun referencePillars(): FourPillars {
        require(source == "lunar-java-1.7.7")
        val lunar = LunarJavaSolar.fromYmdHms(
            dateTime.year,
            dateTime.monthValue,
            dateTime.dayOfMonth,
            dateTime.hour,
            dateTime.minute,
            dateTime.second,
        ).lunar
        return FourPillars(
            year = lunar.yearInGanZhiExact,
            month = lunar.monthInGanZhiExact,
            day = lunar.dayInGanZhiExact,
            hour = lunar.timeInGanZhi,
        )
    }
}

private fun LocalDateTime.toCivilDateTime() = CivilDateTime(
    year = year,
    month = monthValue,
    day = dayOfMonth,
    hour = hour,
    minute = minute,
    second = second,
)
