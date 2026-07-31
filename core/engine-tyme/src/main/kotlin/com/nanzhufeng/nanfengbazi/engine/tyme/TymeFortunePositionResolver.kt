package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.FortunePosition
import com.nanzhufeng.nanfengbazi.domain.FortunePositionResolver
import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.YearBoundaryRule
import com.tyme.sixtycycle.SixtyCycleYear
import com.tyme.solar.SolarTime
import java.time.LocalDateTime

class TymeFortunePositionResolver : FortunePositionResolver {
    override fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): FortunePosition {
        require(result.profile.yearBoundaryRule == YearBoundaryRule.SPRING_EXACT) {
            "当前只支持按精确立春切换流年。"
        }
        val observation = observedAt.toLocalDateTime()
        val flowYearName = observedAt.toTyme().sixtyCycleHour.year.name
        val labelYear = when {
            SixtyCycleYear.fromYear(observedAt.year).sixtyCycle.name == flowYearName ->
                observedAt.year
            SixtyCycleYear.fromYear(observedAt.year - 1).sixtyCycle.name == flowYearName ->
                observedAt.year - 1
            else -> error("无法把观察时刻映射到版本化流年。")
        }
        val exactDecade = result.decadeFortunes.firstOrNull { decade ->
            val start = decade.startAt?.toLocalDateTime()
            val end = decade.endAtExclusive?.toLocalDateTime()
            start != null && end != null &&
                !observation.isBefore(start) && observation.isBefore(end)
        }
        val fallbackDecade = if (
            result.decadeFortunes.any { it.startAt != null || it.endAtExclusive != null }
        ) {
            null
        } else {
            result.decadeFortunes.firstOrNull { labelYear in it.startYear..it.endYear }
        }
        val currentDecade = exactDecade ?: fallbackDecade
        val firstStart = result.decadeFortunes.firstOrNull()?.startAt?.toLocalDateTime()
        val lastEnd = result.decadeFortunes.lastOrNull()?.endAtExclusive?.toLocalDateTime()
        val status = when {
            currentDecade != null -> FortunePositionStatus.WITHIN_DECADE
            firstStart != null && observation.isBefore(firstStart) ->
                FortunePositionStatus.BEFORE_FIRST_DECADE
            lastEnd != null && !observation.isBefore(lastEnd) ->
                FortunePositionStatus.AFTER_TIMELINE
            result.decadeFortunes.isNotEmpty() &&
                labelYear < result.decadeFortunes.first().startYear ->
                FortunePositionStatus.BEFORE_FIRST_DECADE
            else -> FortunePositionStatus.AFTER_TIMELINE
        }
        val birthYear = result.calendarConversion?.solarDateTime?.year
            ?: result.annualFortunes.firstOrNull()?.calendarYear
            ?: result.decadeFortunes.firstOrNull()?.let {
                it.startYear - it.startAge + 1
            }
            ?: labelYear
        val decadeIndex = currentDecade?.let {
            result.decadeFortunes.indexOf(it).takeIf { index -> index >= 0 }?.plus(1)
        }
        val annual = (
            result.annualFortunes.firstOrNull { it.calendarYear == labelYear }
                ?: AnnualFortune(
                name = flowYearName,
                calendarYear = labelYear,
                nominalAge = labelYear - birthYear + 1,
            )
            ).copy(
                decadeIndex = decadeIndex,
                decadeName = currentDecade?.name,
            )
        return FortunePosition(
            observedAt = observedAt,
            annualFortune = annual,
            decadeFortune = currentDecade,
            status = status,
        )
    }
}

private fun CivilDateTime.toTyme(): SolarTime =
    SolarTime.fromYmdHms(year, month, day, hour, minute, second)

private fun CivilDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, month, day, hour, minute, second)
