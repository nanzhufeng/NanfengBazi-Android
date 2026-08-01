package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import java.time.Instant
import java.time.ZoneId

data class FourPillarsLookupQuery(
    val fourPillars: FourPillars,
    val startYear: Int,
    val endYear: Int,
    val timeZoneId: String,
    val ratHourRule: RatHourRule,
)

data class FourPillarsLookupCandidate(
    val civilDateTime: CivilDateTime,
    val timeZoneId: String,
    val resolvedUtcOffsetSeconds: Int,
    val timeZoneDataVersion: String,
    val instant: Instant,
    val fourPillars: FourPillars,
    val ratHourRule: RatHourRule,
    val engineVersion: String,
    val ruleVersion: String,
)

data class FourPillarsLookupEvidence(
    val engineName: String,
    val engineVersion: String,
    val ruleVersion: String,
    val lookupMethod: String,
)

enum class FourPillarsField {
    YEAR,
    MONTH,
    DAY,
    HOUR,
}

enum class YearRangeBoundary {
    START,
    END,
}

sealed interface FourPillarsLookupError {
    data class InvalidPillar(
        val field: FourPillarsField,
        val value: String,
    ) : FourPillarsLookupError

    data class YearOutOfBounds(
        val boundary: YearRangeBoundary,
        val value: Int,
        val minimum: Int,
        val maximum: Int,
    ) : FourPillarsLookupError

    data class InvalidYearOrder(
        val startYear: Int,
        val endYear: Int,
    ) : FourPillarsLookupError

    data class YearRangeTooLarge(
        val inclusiveYearCount: Int,
        val maximumInclusiveYearCount: Int,
    ) : FourPillarsLookupError

    data class InvalidTimeZone(val timeZoneId: String) : FourPillarsLookupError

    data object EngineUnavailable : FourPillarsLookupError
}

sealed interface FourPillarsLookupResult {
    data class Completed(
        val query: FourPillarsLookupQuery,
        val candidates: List<FourPillarsLookupCandidate>,
        val evidence: FourPillarsLookupEvidence,
    ) : FourPillarsLookupResult

    data class Failed(val error: FourPillarsLookupError) : FourPillarsLookupResult
}

/**
 * 四柱反查唯一公开入口。生产调用方不得直接依赖具体历法库。
 */
fun interface FourPillarsLookup {
    suspend fun search(query: FourPillarsLookupQuery): FourPillarsLookupResult
}

object FourPillarsLookupContract {
    const val MIN_YEAR = 1800
    const val MAX_YEAR = 2100
    const val MAX_INCLUSIVE_YEAR_COUNT = 301

    const val CANDIDATE_NOTICE =
        "候选仅表示该民用代表时刻可复算出所填四柱，不是出生分钟的唯一证明；" +
            "同一时辰内可能有多个分钟得到相同四柱。首版不做真太阳时推算。"

    private val sixtyCycles = buildList {
        val stems = "甲乙丙丁戊己庚辛壬癸"
        val branches = "子丑寅卯辰巳午未申酉戌亥"
        repeat(60) { index ->
            add("${stems[index % stems.length]}${branches[index % branches.length]}")
        }
    }.toSet()

    fun normalize(query: FourPillarsLookupQuery): FourPillarsLookupQuery = query.copy(
        fourPillars = query.fourPillars.copy(
            year = query.fourPillars.year.trim(),
            month = query.fourPillars.month.trim(),
            day = query.fourPillars.day.trim(),
            hour = query.fourPillars.hour.trim(),
        ),
        timeZoneId = query.timeZoneId.trim(),
    )

    fun validate(query: FourPillarsLookupQuery): FourPillarsLookupError? {
        listOf(
            FourPillarsField.YEAR to query.fourPillars.year,
            FourPillarsField.MONTH to query.fourPillars.month,
            FourPillarsField.DAY to query.fourPillars.day,
            FourPillarsField.HOUR to query.fourPillars.hour,
        ).firstOrNull { (_, value) -> value !in sixtyCycles }?.let { (field, value) ->
            return FourPillarsLookupError.InvalidPillar(field, value)
        }
        if (query.startYear !in MIN_YEAR..MAX_YEAR) {
            return FourPillarsLookupError.YearOutOfBounds(
                boundary = YearRangeBoundary.START,
                value = query.startYear,
                minimum = MIN_YEAR,
                maximum = MAX_YEAR,
            )
        }
        if (query.endYear !in MIN_YEAR..MAX_YEAR) {
            return FourPillarsLookupError.YearOutOfBounds(
                boundary = YearRangeBoundary.END,
                value = query.endYear,
                minimum = MIN_YEAR,
                maximum = MAX_YEAR,
            )
        }
        if (query.startYear > query.endYear) {
            return FourPillarsLookupError.InvalidYearOrder(
                startYear = query.startYear,
                endYear = query.endYear,
            )
        }
        val inclusiveYearCount = query.endYear - query.startYear + 1
        if (inclusiveYearCount > MAX_INCLUSIVE_YEAR_COUNT) {
            return FourPillarsLookupError.YearRangeTooLarge(
                inclusiveYearCount = inclusiveYearCount,
                maximumInclusiveYearCount = MAX_INCLUSIVE_YEAR_COUNT,
            )
        }
        if (
            query.timeZoneId !in ZoneId.getAvailableZoneIds() &&
            query.timeZoneId !in setOf("UTC", "GMT", "UT")
        ) {
            return FourPillarsLookupError.InvalidTimeZone(query.timeZoneId)
        }
        return null
    }
}
