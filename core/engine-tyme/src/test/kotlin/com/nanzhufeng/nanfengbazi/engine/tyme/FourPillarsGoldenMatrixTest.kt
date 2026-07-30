package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
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
        val cases = loadCases()
        assertTrue("黄金样本必须至少 50 个", cases.size >= 50)
        assertEquals("黄金样本 id 必须唯一", cases.size, cases.map { it.id }.distinct().size)

        cases.forEach { golden ->
            val input = BirthInput(
                calendarInput = BirthCalendarInput.Solar(golden.dateTime.toCivilDateTime()),
                sexForFortuneDirection = SexForFortuneDirection.MAN,
                timePrecision = TimePrecision.EXACT_TO_SECOND,
            )
            val actual = engine.calculate(input, CalculationProfile.tymeDefault()).fourPillars
            assertEquals("${golden.id} 的冻结四柱发生变化", golden.expected, actual)
            assertEquals(
                "${golden.id} 与 lunar-java 1.7.7 差分不一致",
                golden.expected,
                golden.referencePillars(),
            )
        }
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
                require(fields.size == 9) { "非法黄金样本行：$line" }
                require(fields[1] == "lunar-java-1.7.7")
                require(fields[2] == "solar")
                require(fields[4] == "false")
                GoldenCase(
                    id = fields[0],
                    dateTime = LocalDateTime.parse(fields[3]),
                    expected = FourPillars(
                        year = fields[5],
                        month = fields[6],
                        day = fields[7],
                        hour = fields[8],
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
