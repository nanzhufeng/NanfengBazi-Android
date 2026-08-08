package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.AlmanacContract
import com.nanzhufeng.nanfengbazi.domain.AlmanacError
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthQuery
import com.nanzhufeng.nanfengbazi.domain.AlmanacResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TymeAlmanacReaderTest {
    private val reader = TymeAlmanacReader()

    @Test
    fun `month contains fixed grid and reproducible selected details`() = runTest {
        val result = reader.loadMonth(AlmanacMonthQuery(2026, 8, 2))

        assertTrue(result is AlmanacResult.Completed)
        val month = (result as AlmanacResult.Completed).month
        assertEquals(AlmanacContract.CELL_COUNT, month.cells.size)
        assertEquals("戊申", month.selected.dayPillar)
        assertEquals("狮子座", month.selected.constellation)
        assertEquals("六月二十", month.selected.lunarDateText)
        assertEquals(2, month.cells.count { it.marker == "立秋" || it.marker == "处暑" })
        assertEquals(2, month.cells.count { it.solarTerm == "立秋" || it.solarTerm == "处暑" })
    }

    @Test
    fun `day pillar repeats after sixty days`() = runTest {
        val first = reader.loadMonth(AlmanacMonthQuery(2026, 8, 2)) as AlmanacResult.Completed
        val second = reader.loadMonth(AlmanacMonthQuery(2026, 10, 1)) as AlmanacResult.Completed

        assertEquals(first.month.selected.dayPillar, second.month.selected.dayPillar)
    }

    @Test
    fun `selected double hour yields four pillars with hidden stems and folk weight`() = runTest {
        val child = reader.loadMonth(
            AlmanacMonthQuery(2026, 8, 2, selectedDoubleHourIndex = 0),
        ) as AlmanacResult.Completed
        val noon = reader.loadMonth(
            AlmanacMonthQuery(2026, 8, 2, selectedDoubleHourIndex = 6),
        ) as AlmanacResult.Completed

        assertEquals(4, child.month.selected.pillars.size)
        assertEquals("子", child.month.selected.selectedDoubleHour.branch)
        assertTrue(child.month.selected.pillars.all { it.hiddenStems.isNotEmpty() })
        assertTrue(child.month.selected.pillars.all { it.tenGod.isNotBlank() })
        assertTrue(child.month.selected.pillars.flatMap { it.hiddenStems }.all { it.tenGod.isNotBlank() })
        assertTrue(child.month.selected.pillars.flatMap { it.shenSha }.isNotEmpty())
        assertTrue(child.month.selected.hourPillar != noon.month.selected.hourPillar)
        assertTrue(child.month.selected.folkBoneWeight != null)
    }

    @Test
    fun `supported year boundaries succeed and invalid input is structured failure`() = runTest {
        val minimum = reader.loadMonth(
            AlmanacMonthQuery(AlmanacContract.MIN_YEAR, 1, 1),
        )
        val maximum = reader.loadMonth(
            AlmanacMonthQuery(AlmanacContract.MAX_YEAR, 12, 31),
        )
        val tooEarly = reader.loadMonth(
            AlmanacMonthQuery(AlmanacContract.MIN_YEAR - 1, 1, 1),
        )
        val tooLate = reader.loadMonth(
            AlmanacMonthQuery(AlmanacContract.MAX_YEAR + 1, 1, 1),
        )
        val invalidMonth = reader.loadMonth(AlmanacMonthQuery(2026, 13, 1))
        val invalidDay = reader.loadMonth(AlmanacMonthQuery(2026, 2, 30))

        assertTrue(minimum is AlmanacResult.Completed)
        assertTrue(maximum is AlmanacResult.Completed)
        assertTrue((tooEarly as AlmanacResult.Failed).error is AlmanacError.YearOutOfBounds)
        assertTrue((tooLate as AlmanacResult.Failed).error is AlmanacError.YearOutOfBounds)
        assertTrue((invalidMonth as AlmanacResult.Failed).error is AlmanacError.InvalidMonth)
        assertTrue((invalidDay as AlmanacResult.Failed).error is AlmanacError.InvalidDay)
    }
}
