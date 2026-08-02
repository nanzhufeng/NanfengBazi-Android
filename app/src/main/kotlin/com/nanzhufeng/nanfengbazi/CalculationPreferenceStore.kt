package com.nanzhufeng.nanfengbazi

import android.content.Context
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule

interface CalculationPreferenceStore {
    fun readRatHourRule(): RatHourRule
    fun writeRatHourRule(rule: RatHourRule)
}

class AndroidCalculationPreferenceStore(context: Context) : CalculationPreferenceStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun readRatHourRule(): RatHourRule = runCatching {
        RatHourRule.valueOf(
            preferences.getString(KEY_RAT_HOUR_RULE, null) ?: RatHourRule.TYME_DEFAULT.name,
        )
    }.getOrDefault(RatHourRule.TYME_DEFAULT)

    override fun writeRatHourRule(rule: RatHourRule) {
        preferences.edit().putString(KEY_RAT_HOUR_RULE, rule.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "calculation_preferences"
        const val KEY_RAT_HOUR_RULE = "default_rat_hour_rule"
    }
}

class InMemoryCalculationPreferenceStore(
    initialRule: RatHourRule = RatHourRule.TYME_DEFAULT,
) : CalculationPreferenceStore {
    private var rule = initialRule

    override fun readRatHourRule(): RatHourRule = rule

    override fun writeRatHourRule(rule: RatHourRule) {
        this.rule = rule
    }
}
