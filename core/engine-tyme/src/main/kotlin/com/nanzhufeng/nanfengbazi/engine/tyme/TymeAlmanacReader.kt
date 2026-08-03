package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.AlmanacContract
import com.nanzhufeng.nanfengbazi.domain.AlmanacDate
import com.nanzhufeng.nanfengbazi.domain.AlmanacDayDetails
import com.nanzhufeng.nanfengbazi.domain.AlmanacDaySummary
import com.nanzhufeng.nanfengbazi.domain.AlmanacError
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthQuery
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthView
import com.nanzhufeng.nanfengbazi.domain.AlmanacReader
import com.nanzhufeng.nanfengbazi.domain.AlmanacResult
import com.tyme.solar.SolarDay
import com.tyme.solar.SolarMonth

class TymeAlmanacReader : AlmanacReader {
    override suspend fun loadMonth(query: AlmanacMonthQuery): AlmanacResult {
        AlmanacContract.validate(query)?.let { return AlmanacResult.Failed(it) }
        return try {
            val month = SolarMonth.fromYm(query.year, query.month)
            val firstDay = month.firstDay
            val gridStart = firstDay.next(-firstDay.week.index)
            val cells = List(AlmanacContract.CELL_COUNT) { offset ->
                gridStart.next(offset).toSummary(query)
            }
            val selectedDay = SolarDay.fromYmd(query.year, query.month, query.selectedDay)
            AlmanacResult.Completed(
                AlmanacMonthView(
                    query = query,
                    cells = cells,
                    selected = selectedDay.toDetails(),
                ),
            )
        } catch (_: IllegalArgumentException) {
            AlmanacResult.Failed(
                AlmanacError.InvalidDay(query.year, query.month, query.selectedDay),
            )
        } catch (_: Exception) {
            AlmanacResult.Failed(AlmanacError.EngineUnavailable)
        }
    }
}

private fun SolarDay.toSummary(query: AlmanacMonthQuery): AlmanacDaySummary {
    val lunar = lunarDay
    val solarTerm = termDay.takeIf { it.dayIndex == 0 }?.solarTerm?.name
    val marker = solarTerm
        ?: festival?.name
        ?: lunar.festival?.name
        ?: legalHoliday?.name
    return AlmanacDaySummary(
        date = AlmanacDate(year, month, day),
        weekIndex = week.index,
        inSelectedMonth = year == query.year && month == query.month,
        lunarDayText = if (lunar.day == 1) lunar.lunarMonth.name else lunar.name,
        dayPillar = sixtyCycleDay.sixtyCycle.name,
        marker = marker,
    )
}

private fun SolarDay.toDetails(): AlmanacDayDetails {
    val lunar = lunarDay
    val sixtyCycle = sixtyCycleDay
    val termName = termDay.takeIf { it.dayIndex == 0 }?.solarTerm?.name
    val festivals = listOfNotNull(
        festival?.name,
        lunar.festival?.name,
        legalHoliday?.name,
    ).distinct()
    return AlmanacDayDetails(
        date = AlmanacDate(year, month, day),
        weekName = week.name,
        lunarDateText = "${lunar.lunarMonth.name}${lunar.name}",
        yearPillar = sixtyCycle.year.name,
        monthPillar = sixtyCycle.month.name,
        dayPillar = sixtyCycle.sixtyCycle.name,
        constellation = "${constellation.name}座",
        solarTerm = termName,
        festivalNames = festivals,
        duty = sixtyCycle.duty.name,
        twelveStar = "${sixtyCycle.twelveStar.name} · ${sixtyCycle.twelveStar.ecliptic.name}",
        twentyEightStar = "${sixtyCycle.twentyEightStar.name} · ${sixtyCycle.twentyEightStar.luck.name}",
        fetusPosition = sixtyCycle.fetusDay.name,
        recommends = sixtyCycle.recommends.map { it.name }.distinct().take(12),
        avoids = sixtyCycle.avoids.map { it.name }.distinct().take(12),
    )
}
