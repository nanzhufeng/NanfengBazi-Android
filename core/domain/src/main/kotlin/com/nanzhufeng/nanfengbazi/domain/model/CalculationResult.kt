package com.nanzhufeng.nanfengbazi.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FourPillars(
    val year: String,
    val month: String,
    val day: String,
    val hour: String,
)

@Serializable
enum class FortuneDirection {
    FORWARD,
    BACKWARD,
}

@Serializable
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

@Serializable
data class DecadeFortune(
    val name: String,
    val startAge: Int,
    val endAge: Int,
    val startYear: Int,
    val endYear: Int,
)

@Serializable
data class CalculationEvidence(
    val engineName: String,
    val engineVersion: String,
    val ruleVersion: String,
    @Serializable(with = InstantIsoSerializer::class)
    val calculatedAt: Instant,
)

@Serializable
data class CalculationWarning(
    val code: String,
    val message: String,
)

@Serializable
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
