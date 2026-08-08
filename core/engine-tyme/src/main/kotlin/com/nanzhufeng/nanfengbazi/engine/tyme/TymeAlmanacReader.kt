package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.AlmanacContract
import com.nanzhufeng.nanfengbazi.domain.AlmanacDate
import com.nanzhufeng.nanfengbazi.domain.AlmanacDayDetails
import com.nanzhufeng.nanfengbazi.domain.AlmanacDaySummary
import com.nanzhufeng.nanfengbazi.domain.AlmanacDoubleHours
import com.nanzhufeng.nanfengbazi.domain.AlmanacError
import com.nanzhufeng.nanfengbazi.domain.AlmanacHiddenStem
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthQuery
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthView
import com.nanzhufeng.nanfengbazi.domain.AlmanacPillarDetail
import com.nanzhufeng.nanfengbazi.domain.AlmanacPillarRelation
import com.nanzhufeng.nanfengbazi.domain.AlmanacReader
import com.nanzhufeng.nanfengbazi.domain.AlmanacResult
import com.nanzhufeng.nanfengbazi.domain.BasicShenShaRules
import com.nanzhufeng.nanfengbazi.domain.FolkBoneWeightRulesV1
import com.tyme.solar.SolarDay
import com.tyme.solar.SolarMonth
import com.tyme.solar.SolarTime
import com.tyme.sixtycycle.SixtyCycle

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
                    selected = selectedDay.toDetails(query),
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
        solarTerm = solarTerm,
        marker = marker,
    )
}

private fun SolarDay.toDetails(query: AlmanacMonthQuery): AlmanacDayDetails {
    val lunar = lunarDay
    val sixtyCycle = sixtyCycleDay
    val selectedDoubleHour = AlmanacDoubleHours.fromIndex(query.selectedDoubleHourIndex)
    val solarTime = SolarTime.fromYmdHms(
        year,
        month,
        day,
        selectedDoubleHour.representativeHour,
        0,
        0,
    )
    val eightChar = solarTime.lunarHour.resolveEightChar(query.ratHourRule)
    val pillarCycles = listOf(eightChar.year, eightChar.month, eightChar.day, eightChar.hour)
    val dayMaster = eightChar.day.heavenStem
    val shenShaRoots = listOf(
        eightChar.year.earthBranch.name.single(),
        eightChar.day.earthBranch.name.single(),
    )
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
        lunarYear = lunar.lunarMonth.lunarYear.year,
        lunarMonth = kotlin.math.abs(lunar.lunarMonth.monthWithLeap),
        lunarDay = lunar.day,
        isLeapMonth = lunar.lunarMonth.isLeap,
        yearPillar = sixtyCycle.year.name,
        monthPillar = sixtyCycle.month.name,
        dayPillar = sixtyCycle.sixtyCycle.name,
        hourPillar = eightChar.hour.name,
        selectedDoubleHour = selectedDoubleHour,
        pillars = pillarCycles.mapIndexed { index, cycle ->
            cycle.toAlmanacPillar(
                label = listOf("年柱", "月柱", "日柱", "时柱")[index],
                tenGod = if (index == 2) "日元" else dayMaster.getTenStar(cycle.heavenStem).name,
                hiddenStemTenGod = { hidden -> dayMaster.getTenStar(hidden).name },
                shenSha = BasicShenShaRules.resolveNames(
                    pillar = cycle.name,
                    dayStem = dayMaster.name.single(),
                    roots = shenShaRoots,
                ),
            )
        },
        relations = pillarCycles.toAlmanacRelations(),
        folkBoneWeight = FolkBoneWeightRulesV1.calculate(
            lunarYearPillar = lunar.lunarMonth.lunarYear.sixtyCycle.name,
            lunarMonth = kotlin.math.abs(lunar.lunarMonth.monthWithLeap),
            lunarDay = lunar.day,
            isLeapMonth = lunar.lunarMonth.isLeap,
            doubleHourIndex = selectedDoubleHour.index,
        ),
        constellation = "${constellation.name}座",
        solarTerm = termName,
        festivalNames = festivals,
        recommends = sixtyCycle.recommends.map { it.name }.distinct().take(12),
        avoids = sixtyCycle.avoids.map { it.name }.distinct().take(12),
    )
}

private fun SixtyCycle.toAlmanacPillar(
    label: String,
    tenGod: String,
    hiddenStemTenGod: (com.tyme.sixtycycle.HeavenStem) -> String,
    shenSha: List<String>,
): AlmanacPillarDetail = AlmanacPillarDetail(
    label = label,
    value = name,
    heavenStem = heavenStem.name,
    heavenStemElement = heavenStem.element.name,
    tenGod = tenGod,
    earthBranch = earthBranch.name,
    earthBranchElement = earthBranch.element.name,
    hiddenStems = earthBranch.hideHeavenStems.map { hidden ->
        AlmanacHiddenStem(
            heavenStem = hidden.heavenStem.name,
            element = hidden.heavenStem.element.name,
            tenGod = hiddenStemTenGod(hidden.heavenStem),
        )
    },
    shenSha = shenSha,
)

private fun List<SixtyCycle>.toAlmanacRelations(): List<AlmanacPillarRelation> = buildList {
    this@toAlmanacRelations.forEachIndexed { index, left ->
        this@toAlmanacRelations.drop(index + 1).forEach { right ->
            val stemPair = left.heavenStem.name + right.heavenStem.name
            when {
                left.heavenStem.combine(right.heavenStem) != null ->
                    add(AlmanacPillarRelation("天干合", stemPair))
                left.earthBranch.combine(right.earthBranch) != null ->
                    add(AlmanacPillarRelation("地支合", left.earthBranch.name + right.earthBranch.name))
                left.earthBranch.opposite.name == right.earthBranch.name ->
                    add(AlmanacPillarRelation("地支冲", left.earthBranch.name + right.earthBranch.name))
            }
        }
    }
}.distinct()
