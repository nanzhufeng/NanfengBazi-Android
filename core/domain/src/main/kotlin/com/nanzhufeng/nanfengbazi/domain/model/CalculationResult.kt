package com.nanzhufeng.nanfengbazi.domain.model

import com.nanzhufeng.nanfengbazi.domain.BaziStructuralProfile
import java.time.Instant
import kotlinx.serialization.Serializable

const val DECADE_FORTUNE_COVERAGE_YEARS = 120
const val DECADE_FORTUNE_COUNT = DECADE_FORTUNE_COVERAGE_YEARS / 10

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
    val startAt: CivilDateTime? = null,
    val endAtExclusive: CivilDateTime? = null,
)

@Serializable
data class AnnualFortune(
    val name: String,
    val calendarYear: Int,
    val nominalAge: Int,
    val decadeIndex: Int? = null,
    val decadeName: String? = null,
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
enum class CalendarSystem {
    SOLAR,
    LUNAR,
}

@Serializable
data class CalendarConversionResult(
    val inputCalendarSystem: CalendarSystem,
    val solarDateTime: CivilDateTime,
    val lunarDateTime: LunarDateTime,
)

@Serializable
data class TrueSolarTimeEvidence(
    val originalCivilDateTime: CivilDateTime,
    val timeZoneId: String,
    val resolvedUtcOffsetSeconds: Int,
    val longitude: Double,
    val latitude: Double,
    val meanSolarCorrectionSeconds: Int,
    val equationOfTimeCorrectionSeconds: Int,
    val totalCorrectionSeconds: Int,
    val trueSolarDateTime: CivilDateTime,
    val crossesDate: Boolean,
    val crossesDoubleHour: Boolean,
    val algorithmVersion: String,
    val applicationRule: TrueSolarTimeApplicationRule,
)

@Serializable
enum class PillarPosition {
    YEAR,
    MONTH,
    DAY,
    HOUR,
}

@Serializable
data class HiddenStemDetail(
    val heavenStem: String,
    val type: String,
    val tenGod: String,
    val element: String,
)

@Serializable
data class PillarDetail(
    val position: PillarPosition,
    val name: String,
    val heavenStem: String,
    val earthBranch: String,
    val heavenStemElement: String,
    val earthBranchElement: String,
    val primaryTenGod: String,
    val hiddenStems: List<HiddenStemDetail>,
    val terrain: String,
    val selfSittingTerrain: String,
    val voidEarthBranches: List<String>,
    val naYin: String,
)

@Serializable
enum class SolarTermType {
    JIE,
    QI,
}

@Serializable
data class SolarTermPoint(
    val name: String,
    val type: SolarTermType,
    val at: CivilDateTime,
)

@Serializable
data class BasicChartDetails(
    val zodiac: String,
    val westernZodiac: String,
    val dayMaster: String,
    val pillars: List<PillarDetail>,
    val previousSolarTerm: SolarTermPoint,
    val nextSolarTerm: SolarTermPoint,
    val previousJie: SolarTermPoint? = null,
    val nextJie: SolarTermPoint? = null,
) {
    init {
        require(
            pillars.size == PillarPosition.entries.size &&
                pillars.map { it.position }.toSet() == PillarPosition.entries.toSet(),
        ) {
            "基础排盘必须且只能包含年、月、日、时四柱明细"
        }
    }
}

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
    val annualFortunes: List<AnnualFortune> = emptyList(),
    val evidence: CalculationEvidence,
    val warnings: List<CalculationWarning> = emptyList(),
    val calendarConversion: CalendarConversionResult? = null,
    val trueSolarTimeEvidence: TrueSolarTimeEvidence? = null,
    val basicChartDetails: BasicChartDetails? = null,
    /** 月令、通根、透干与生扶克泄耗形成的本机结构候选；问真来源字段仍独立保真。 */
    val structuralProfile: BaziStructuralProfile? = null,
)
