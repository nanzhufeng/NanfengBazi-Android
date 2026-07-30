package com.nanzhufeng.nanfengbazi.solar

import com.nanzhufeng.nanfengbazi.domain.TrueSolarTimeCalculator
import com.nanzhufeng.nanfengbazi.domain.TrueSolarTimeRequest
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.TrueSolarTimeApplicationRule
import com.nanzhufeng.nanfengbazi.domain.model.TrueSolarTimeEvidence
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import net.e175.klaus.solarpositioning.DeltaT
import net.e175.klaus.solarpositioning.SPA

class SpaTrueSolarTimeCalculator : TrueSolarTimeCalculator {
    override fun calculate(request: TrueSolarTimeRequest): TrueSolarTimeEvidence {
        val original = request.civilDateTime.toLocalDateTime()
        require(original.year in MIN_SUPPORTED_YEAR..MAX_SUPPORTED_YEAR) {
            "真太阳时 SPA 校正仅支持 $MIN_SUPPORTED_YEAR..$MAX_SUPPORTED_YEAR 年。"
        }
        require(request.longitude in -180.0..180.0) { "经度超出范围。" }
        require(request.latitude in -90.0..90.0) { "纬度超出范围。" }

        val zone = ZoneId.of(request.timeZoneId)
        val selectedOffset = ZoneOffset.ofTotalSeconds(request.resolvedUtcOffsetSeconds)
        require(selectedOffset in zone.rules.getValidOffsets(original)) {
            "所选 UTC offset 与 ${request.timeZoneId} 在该出生时间的规则不一致。"
        }
        // 中天结果必须使用出生墙上时间所选的 offset。若直接保留 IANA ZoneId，
        // 回拨日的中午可能已切换到另一个 offset，从而覆盖用户对重叠时刻的选择。
        val zoned = ZonedDateTime.of(original, selectedOffset)
        val transit = SPA.calculateSunriseTransitSet(
            zoned,
            request.latitude,
            request.longitude,
            DeltaT.estimate(original.toLocalDate()),
        ).transit()
        val localNoon = original.toLocalDate().atTime(LocalTime.NOON)
        val totalCorrectionSeconds = (
            Duration.between(transit.toLocalDateTime(), localNoon).toMillis() / 1_000.0
            ).roundToInt()
        val meanSolarCorrectionSeconds =
            (request.longitude * SECONDS_PER_LONGITUDE_DEGREE).roundToInt() -
                request.resolvedUtcOffsetSeconds
        val equationOfTimeCorrectionSeconds =
            totalCorrectionSeconds - meanSolarCorrectionSeconds
        val trueSolar = original.plusSeconds(totalCorrectionSeconds.toLong())

        return TrueSolarTimeEvidence(
            originalCivilDateTime = request.civilDateTime,
            timeZoneId = request.timeZoneId,
            resolvedUtcOffsetSeconds = request.resolvedUtcOffsetSeconds,
            longitude = request.longitude,
            latitude = request.latitude,
            meanSolarCorrectionSeconds = meanSolarCorrectionSeconds,
            equationOfTimeCorrectionSeconds = equationOfTimeCorrectionSeconds,
            totalCorrectionSeconds = totalCorrectionSeconds,
            trueSolarDateTime = trueSolar.toDomain(),
            crossesDate = original.toLocalDate() != trueSolar.toLocalDate(),
            crossesDoubleHour = doubleHourIndex(original) != doubleHourIndex(trueSolar),
            algorithmVersion = ALGORITHM_VERSION,
            applicationRule =
                TrueSolarTimeApplicationRule
                    .CIVIL_YEAR_MONTH_TRUE_SOLAR_DAY_HOUR_PROVISIONAL_V1,
        )
    }

    private fun doubleHourIndex(dateTime: LocalDateTime): Int =
        ((dateTime.hour + 1) / 2) % 12

    companion object {
        const val ALGORITHM_VERSION = "nrel-spa-2008+solarpositioning-2.0.12-v1"
        const val MIN_SUPPORTED_YEAR = 1
        const val MAX_SUPPORTED_YEAR = 6000
        private const val SECONDS_PER_LONGITUDE_DEGREE = 240.0
    }
}

private fun CivilDateTime.toLocalDateTime(): LocalDateTime = LocalDateTime.of(
    year,
    month,
    day,
    hour,
    minute,
    second,
)

private fun LocalDateTime.toDomain(): CivilDateTime = CivilDateTime(
    year = year,
    month = monthValue,
    day = dayOfMonth,
    hour = hour,
    minute = minute,
    second = second,
)
