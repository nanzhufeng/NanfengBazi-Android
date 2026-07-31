package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneResolver
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.MonthBoundaryRule
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermType
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.YearBoundaryRule
import com.tyme.solar.SolarTerm
import com.tyme.solar.SolarTime

class TymeProfessionalFortuneResolver(
    private val fortunePositionResolver: TymeFortunePositionResolver =
        TymeFortunePositionResolver(),
) : ProfessionalFortuneResolver {
    override fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): ProfessionalFortunePosition {
        require(result.profile.yearBoundaryRule == YearBoundaryRule.SPRING_EXACT) {
            "当前专业岁运只支持精确立春换年。"
        }
        require(result.profile.monthBoundaryRule == MonthBoundaryRule.SOLAR_TERM_EXACT) {
            "当前专业岁运只支持精确节令换月。"
        }
        val solar = observedAt.toTyme()
        val eightChar = solar.lunarHour.resolveEightChar(result.profile.ratHourRule)
        val previousTerm = solar.term
        return ProfessionalFortunePosition(
            position = fortunePositionResolver.locate(result, observedAt),
            flowPillars = FourPillars(
                year = eightChar.year.name,
                month = eightChar.month.name,
                day = eightChar.day.name,
                hour = eightChar.hour.name,
            ),
            previousSolarTerm = previousTerm.toDomainPoint(),
            nextSolarTerm = previousTerm.next(1).toDomainPoint(),
            observationTimeMode = SolarTimeMode.CIVIL_TIME,
            profileId = result.profile.id,
            ruleVersion = result.profile.ruleVersion,
        )
    }
}

private fun CivilDateTime.toTyme(): SolarTime =
    SolarTime.fromYmdHms(year, month, day, hour, minute, second)

private fun SolarTerm.toDomainPoint(): SolarTermPoint =
    SolarTermPoint(
        name = name,
        type = if (isJie) SolarTermType.JIE else SolarTermType.QI,
        at = julianDay.solarTime.toDomain(),
    )

private fun SolarTime.toDomain(): CivilDateTime =
    CivilDateTime(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
    )
