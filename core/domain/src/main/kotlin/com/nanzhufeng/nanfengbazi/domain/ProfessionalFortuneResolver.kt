package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode

data class ProfessionalFortunePosition(
    val position: FortunePosition,
    val flowPillars: FourPillars,
    val previousSolarTerm: SolarTermPoint,
    val nextSolarTerm: SolarTermPoint,
    val observationTimeMode: SolarTimeMode,
    val profileId: String,
    val ruleVersion: String,
)

fun interface ProfessionalFortuneResolver {
    fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): ProfessionalFortunePosition
}
