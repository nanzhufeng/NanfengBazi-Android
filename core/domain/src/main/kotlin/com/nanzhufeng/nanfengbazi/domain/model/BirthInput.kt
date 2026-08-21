package com.nanzhufeng.nanfengbazi.domain.model

import com.nanzhufeng.nanfengbazi.domain.BaziTimeZoneDefaults
import kotlinx.serialization.Serializable

@Serializable
data class CivilDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
) {
    init {
        // 天文纪年允许公元前年份（例如公元前 5 年为 -4），供史料型名人资料
        // 如实保存；0 年不存在。普通用户输入范围仍由表单与排盘引擎分别约束。
        require(year in -9999..9999 && year != 0) { "年份必须在公元前9999年至公元9999年，且不能为0" }
        require(month in 1..12) { "月份必须在 1..12" }
        require(day in 1..31) { "日期必须在 1..31" }
        require(hour in 0..23) { "小时必须在 0..23" }
        require(minute in 0..59) { "分钟必须在 0..59" }
        require(second in 0..59) { "秒必须在 0..59" }
    }
}

@Serializable
data class LunarDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val isLeapMonth: Boolean,
) {
    init {
        require(year in 1..9999) { "农历年份必须在 1..9999" }
        require(month in 1..12) { "农历月份必须在 1..12" }
        require(day in 1..30) { "农历日期必须在 1..30" }
        require(hour in 0..23) { "小时必须在 0..23" }
        require(minute in 0..59) { "分钟必须在 0..59" }
        require(second in 0..59) { "秒必须在 0..59" }
    }
}

@Serializable
sealed interface BirthCalendarInput {
    @Serializable
    data class Solar(val dateTime: CivilDateTime) : BirthCalendarInput

    @Serializable
    data class Lunar(val dateTime: LunarDateTime) : BirthCalendarInput
}

@Serializable
enum class SexForFortuneDirection {
    WOMAN,
    MAN,
}

@Serializable
enum class TimePrecision {
    EXACT_TO_SECOND,
    EXACT_TO_MINUTE,
    APPROXIMATE,
    HOUR_ONLY,
    DOUBLE_HOUR_ONLY,
    UNKNOWN,
}

@Serializable
enum class TimeSourceType {
    SELF_REPORTED,
    FAMILY_REPORTED,
    OFFICIAL_RECORD,
    WENZHEN_SCREENSHOT,
    WENZHEN_WEB_IMPORT,
    OTHER_RECORD,
    UNKNOWN,
}

@Serializable
enum class CoordinateSource {
    USER_ENTERED,
}

@Serializable
data class BirthInput(
    val calendarInput: BirthCalendarInput,
    val sexForFortuneDirection: SexForFortuneDirection,
    val timePrecision: TimePrecision,
    val timeZoneId: String = BaziTimeZoneDefaults.BEIJING_IANA_ID,
    val resolvedUtcOffsetSeconds: Int? = null,
    val timeZoneDataVersion: String? = null,
    val locationName: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val coordinateSource: CoordinateSource? = null,
    val useTrueSolarTime: Boolean = false,
    val timeSourceType: TimeSourceType = TimeSourceType.UNKNOWN,
    val sourceNote: String? = null,
) {
    init {
        require(timeZoneId.isNotBlank()) { "时区标识不能为空" }
        require((longitude == null) == (latitude == null)) {
            "经纬度必须同时提供或同时省略"
        }
        longitude?.let { require(it in -180.0..180.0) { "经度超出范围" } }
        latitude?.let { require(it in -90.0..90.0) { "纬度超出范围" } }
    }
}
