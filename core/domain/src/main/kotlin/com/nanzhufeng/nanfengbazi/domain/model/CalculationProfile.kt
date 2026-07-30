package com.nanzhufeng.nanfengbazi.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class YearBoundaryRule {
    SPRING_EXACT,
}

@Serializable
enum class MonthBoundaryRule {
    SOLAR_TERM_EXACT,
}

@Serializable
enum class RatHourRule {
    TYME_DEFAULT,
}

@Serializable
enum class SolarTimeMode {
    CIVIL_TIME,
    TRUE_SOLAR_TIME,
}

@Serializable
enum class TrueSolarTimeApplicationRule {
    /**
     * 暂定公开参考口径：年柱、月柱保留原始民用时；日柱、时柱按真太阳时当地边界。
     * 问真跨日、跨时辰和节气边界样本通过前不得移除 PROVISIONAL 标记。
     */
    CIVIL_YEAR_MONTH_TRUE_SOLAR_DAY_HOUR_PROVISIONAL_V1,
}

@Serializable
enum class LuckStartRule {
    TYME_DEFAULT,
    CHINA_95,
}

@Serializable
data class CalculationProfile(
    val id: String,
    val engineVersion: String,
    val ruleVersion: String,
    val yearBoundaryRule: YearBoundaryRule = YearBoundaryRule.SPRING_EXACT,
    val monthBoundaryRule: MonthBoundaryRule = MonthBoundaryRule.SOLAR_TERM_EXACT,
    val ratHourRule: RatHourRule = RatHourRule.TYME_DEFAULT,
    val solarTimeMode: SolarTimeMode = SolarTimeMode.CIVIL_TIME,
    val trueSolarTimeApplicationRule: TrueSolarTimeApplicationRule =
        TrueSolarTimeApplicationRule.CIVIL_YEAR_MONTH_TRUE_SOLAR_DAY_HOUR_PROVISIONAL_V1,
    val luckStartRule: LuckStartRule = LuckStartRule.TYME_DEFAULT,
) {
    init {
        require(id.isNotBlank()) { "计算配置 id 不能为空" }
        require(engineVersion.isNotBlank()) { "引擎版本不能为空" }
        require(ruleVersion.isNotBlank()) { "规则版本不能为空" }
    }

    companion object {
        const val TYME_ENGINE_VERSION = "1.5.1"
        const val DEFAULT_RULE_VERSION = "stage0-v1"

        fun tymeDefault(
            solarTimeMode: SolarTimeMode = SolarTimeMode.CIVIL_TIME,
        ): CalculationProfile = CalculationProfile(
            id = when (solarTimeMode) {
                SolarTimeMode.CIVIL_TIME -> "tyme-default-v1"
                SolarTimeMode.TRUE_SOLAR_TIME -> "tyme-true-solar-provisional-v1"
            },
            engineVersion = TYME_ENGINE_VERSION,
            ruleVersion = DEFAULT_RULE_VERSION,
            solarTimeMode = solarTimeMode,
        )
    }
}
