package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule

data class AlmanacDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

data class AlmanacMonthQuery(
    val year: Int,
    val month: Int,
    val selectedDay: Int,
    /** 选中的十二时辰序号；子时以 00:00 作为日内展示代表点。 */
    val selectedDoubleHourIndex: Int = 0,
    val ratHourRule: RatHourRule = RatHourRule.TYME_DEFAULT,
)

data class AlmanacDoubleHour(
    val index: Int,
    val branch: String,
    val representativeHour: Int,
    val timeRangeLabel: String,
)

object AlmanacDoubleHours {
    val all: List<AlmanacDoubleHour> = listOf(
        AlmanacDoubleHour(0, "子", 0, "23:00–01:00"),
        AlmanacDoubleHour(1, "丑", 1, "01:00–03:00"),
        AlmanacDoubleHour(2, "寅", 3, "03:00–05:00"),
        AlmanacDoubleHour(3, "卯", 5, "05:00–07:00"),
        AlmanacDoubleHour(4, "辰", 7, "07:00–09:00"),
        AlmanacDoubleHour(5, "巳", 9, "09:00–11:00"),
        AlmanacDoubleHour(6, "午", 11, "11:00–13:00"),
        AlmanacDoubleHour(7, "未", 13, "13:00–15:00"),
        AlmanacDoubleHour(8, "申", 15, "15:00–17:00"),
        AlmanacDoubleHour(9, "酉", 17, "17:00–19:00"),
        AlmanacDoubleHour(10, "戌", 19, "19:00–21:00"),
        AlmanacDoubleHour(11, "亥", 21, "21:00–23:00"),
    )

    fun fromIndex(index: Int): AlmanacDoubleHour = all[index]

    /** 将民用小时归入当前界面所使用的十二时辰。23:00 与 00:00 都归子时。 */
    fun indexForCivilHour(hour: Int): Int {
        require(hour in 0..23) { "hour must be in 0..23" }
        return if (hour == 0 || hour == 23) 0 else (hour + 1) / 2
    }
}

data class AlmanacDaySummary(
    val date: AlmanacDate,
    val weekIndex: Int,
    val inSelectedMonth: Boolean,
    val lunarDayText: String,
    val dayPillar: String,
    /** 仅在当天为二十四节气交节日时有值；不要由 UI 从普通标记文本猜测。 */
    val solarTerm: String? = null,
    val marker: String? = null,
)

data class AlmanacDayDetails(
    val date: AlmanacDate,
    val weekName: String,
    val lunarDateText: String,
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeapMonth: Boolean,
    val yearPillar: String,
    val monthPillar: String,
    val dayPillar: String,
    val hourPillar: String,
    val selectedDoubleHour: AlmanacDoubleHour,
    val pillars: List<AlmanacPillarDetail>,
    val relations: List<AlmanacPillarRelation> = emptyList(),
    val folkBoneWeight: FolkBoneWeight? = null,
    val constellation: String,
    val solarTerm: String? = null,
    val festivalNames: List<String> = emptyList(),
    val recommends: List<String> = emptyList(),
    val avoids: List<String> = emptyList(),
)

data class AlmanacPillarDetail(
    val label: String,
    val value: String,
    val heavenStem: String,
    val heavenStemElement: String,
    val tenGod: String,
    val earthBranch: String,
    val earthBranchElement: String,
    val hiddenStems: List<AlmanacHiddenStem> = emptyList(),
    val shenSha: List<String> = emptyList(),
)

data class AlmanacHiddenStem(
    val heavenStem: String,
    val element: String,
    val tenGod: String,
)

data class AlmanacPillarRelation(
    val category: String,
    val pillars: String,
)

/** 民俗称骨的纯查表结果；与 Tyme4j 历法/四柱真值完全分离。 */
data class FolkBoneWeight(
    val version: String,
    val verdictVersion: String,
    val totalQian: Int,
    val yearQian: Int,
    val monthQian: Int,
    val dayQian: Int,
    val hourQian: Int,
    val maleVerdict: String,
    val femaleVerdict: String,
)

data class AlmanacMonthView(
    val query: AlmanacMonthQuery,
    val cells: List<AlmanacDaySummary>,
    val selected: AlmanacDayDetails,
)

sealed interface AlmanacError {
    data class YearOutOfBounds(
        val year: Int,
        val minimum: Int,
        val maximum: Int,
    ) : AlmanacError

    data class InvalidMonth(val month: Int) : AlmanacError
    data class InvalidDay(val year: Int, val month: Int, val day: Int) : AlmanacError
    data class InvalidDoubleHour(val index: Int) : AlmanacError
    data object EngineUnavailable : AlmanacError
}

sealed interface AlmanacResult {
    data class Completed(val month: AlmanacMonthView) : AlmanacResult
    data class Failed(val error: AlmanacError) : AlmanacResult
}

fun interface AlmanacReader {
    suspend fun loadMonth(query: AlmanacMonthQuery): AlmanacResult
}

object AlmanacContract {
    const val MIN_YEAR = 1800
    const val MAX_YEAR = 2200
    const val CELL_COUNT = 42

    fun validate(query: AlmanacMonthQuery): AlmanacError? {
        if (query.year !in MIN_YEAR..MAX_YEAR) {
            return AlmanacError.YearOutOfBounds(query.year, MIN_YEAR, MAX_YEAR)
        }
        if (query.month !in 1..12) return AlmanacError.InvalidMonth(query.month)
        val maximumDay = when (query.month) {
            2 -> if (isLeapYear(query.year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        if (query.selectedDay !in 1..maximumDay) {
            return AlmanacError.InvalidDay(query.year, query.month, query.selectedDay)
        }
        if (query.selectedDoubleHourIndex !in AlmanacDoubleHours.all.indices) {
            return AlmanacError.InvalidDoubleHour(query.selectedDoubleHourIndex)
        }
        return null
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
}
