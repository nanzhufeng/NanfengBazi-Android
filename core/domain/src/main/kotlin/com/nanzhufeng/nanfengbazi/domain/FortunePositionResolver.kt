package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune

enum class FortunePositionStatus {
    BEFORE_FIRST_DECADE,
    WITHIN_DECADE,
    AFTER_TIMELINE,
}

data class FortunePosition(
    val observedAt: CivilDateTime,
    val annualFortune: AnnualFortune,
    val decadeFortune: DecadeFortune?,
    val status: FortunePositionStatus,
)

fun interface FortunePositionResolver {
    fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): FortunePosition
}
