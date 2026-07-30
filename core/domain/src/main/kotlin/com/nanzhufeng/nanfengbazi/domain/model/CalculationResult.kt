package com.nanzhufeng.nanfengbazi.domain.model

import java.time.Instant

data class FourPillars(
    val year: String,
    val month: String,
    val day: String,
    val hour: String,
)

enum class FortuneDirection {
    FORWARD,
    BACKWARD,
}

data class FortuneStart(
    val direction: FortuneDirection,
    val startAt: CivilDateTime,
    val endAt: CivilDateTime,
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int,
)

data class DecadeFortune(
    val name: String,
    val startAge: Int,
    val endAge: Int,
    val startYear: Int,
    val endYear: Int,
)

data class CalculationEvidence(
    val engineName: String,
    val engineVersion: String,
    val ruleVersion: String,
    val calculatedAt: Instant,
)

data class CalculationWarning(
    val code: String,
    val message: String,
)

data class CalculationResult(
    val normalizedInput: BirthInput,
    val profile: CalculationProfile,
    val fourPillars: FourPillars,
    val ownSign: String,
    val bodySign: String,
    val fetalOrigin: String,
    val fetalBreath: String,
    val fortuneStart: FortuneStart,
    val decadeFortunes: List<DecadeFortune>,
    val evidence: CalculationEvidence,
    val warnings: List<CalculationWarning> = emptyList(),
)

