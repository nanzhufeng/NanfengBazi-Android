package com.nanzhufeng.nanfengbazi.domain.model

enum class YearBoundaryRule {
    SPRING_EXACT,
}

enum class MonthBoundaryRule {
    SOLAR_TERM_EXACT,
}

enum class RatHourRule {
    TYME_DEFAULT,
}

enum class SolarTimeMode {
    CIVIL_TIME,
    TRUE_SOLAR_TIME,
}

enum class LuckStartRule {
    TYME_DEFAULT,
    CHINA_95,
}

data class CalculationProfile(
    val id: String,
    val engineVersion: String,
    val ruleVersion: String,
    val yearBoundaryRule: YearBoundaryRule = YearBoundaryRule.SPRING_EXACT,
    val monthBoundaryRule: MonthBoundaryRule = MonthBoundaryRule.SOLAR_TERM_EXACT,
    val ratHourRule: RatHourRule = RatHourRule.TYME_DEFAULT,
    val solarTimeMode: SolarTimeMode = SolarTimeMode.CIVIL_TIME,
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

        fun tymeDefault(): CalculationProfile = CalculationProfile(
            id = "tyme-default-v1",
            engineVersion = TYME_ENGINE_VERSION,
            ruleVersion = DEFAULT_RULE_VERSION,
        )
    }
}

