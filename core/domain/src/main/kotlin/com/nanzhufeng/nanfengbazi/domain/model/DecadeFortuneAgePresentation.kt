package com.nanzhufeng.nanfengbazi.domain.model

import java.time.LocalDateTime

/**
 * 大运年龄只从同一已采用快照中的出生时刻和精确交运边界投影。
 *
 * `startAge` / `endAge` 是引擎保留的原始摘要，不能作为界面上的实岁口径。
 * 有精确边界时统一显示交运瞬间对应的完整周岁；旧快照没有边界时保留原摘要，
 * 不在展示层伪造交运日期。
 */
fun DecadeFortune.completedAgeRangeDisplay(birth: CivilDateTime?): String {
    val start = startAt
    val end = endAtExclusive
    return if (birth != null && start != null && end != null) {
        "${birth.completedAgeAt(start)}–${birth.completedAgeAt(end)}实岁"
    } else {
        "${startAge}–${endAge}岁"
    }
}

fun CalculationResult.solarBirthDateTimeForFortuneDisplay(): CivilDateTime? =
    calendarConversion?.solarDateTime
        ?: (normalizedInput.calendarInput as? BirthCalendarInput.Solar)?.dateTime

private fun CivilDateTime.completedAgeAt(observedAt: CivilDateTime): Int {
    val birth = LocalDateTime.of(year, month, day, hour, minute, second)
    val observed = LocalDateTime.of(
        observedAt.year,
        observedAt.month,
        observedAt.day,
        observedAt.hour,
        observedAt.minute,
        observedAt.second,
    )
    if (observed.isBefore(birth)) return 0
    val roughYears = observed.year - birth.year
    return (roughYears - if (observed.isBefore(birth.plusYears(roughYears.toLong()))) 1 else 0)
        .coerceAtLeast(0)
}
