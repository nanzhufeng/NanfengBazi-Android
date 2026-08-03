package com.nanzhufeng.nanfengbazi.domain

data class AlmanacDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

data class AlmanacMonthQuery(
    val year: Int,
    val month: Int,
    val selectedDay: Int,
)

data class AlmanacDaySummary(
    val date: AlmanacDate,
    val weekIndex: Int,
    val inSelectedMonth: Boolean,
    val lunarDayText: String,
    val dayPillar: String,
    val marker: String? = null,
)

data class AlmanacDayDetails(
    val date: AlmanacDate,
    val weekName: String,
    val lunarDateText: String,
    val yearPillar: String,
    val monthPillar: String,
    val dayPillar: String,
    val constellation: String,
    val solarTerm: String? = null,
    val festivalNames: List<String> = emptyList(),
    val duty: String,
    val twelveStar: String,
    val twentyEightStar: String,
    val fetusPosition: String,
    val recommends: List<String> = emptyList(),
    val avoids: List<String> = emptyList(),
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
    const val MAX_YEAR = 2100
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
        return null
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
}
