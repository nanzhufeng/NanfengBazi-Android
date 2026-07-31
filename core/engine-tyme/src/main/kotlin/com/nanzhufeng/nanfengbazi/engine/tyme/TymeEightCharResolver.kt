package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.tyme.eightchar.EightChar
import com.tyme.eightchar.provider.impl.DefaultEightCharProvider
import com.tyme.eightchar.provider.impl.LunarSect2EightCharProvider
import com.tyme.lunar.LunarHour

internal fun LunarHour.resolveEightChar(rule: RatHourRule): EightChar = when (rule) {
    RatHourRule.TYME_DEFAULT -> DefaultEightCharProvider().getEightChar(this)
    RatHourRule.LATE_RAT_SAME_DAY -> LunarSect2EightCharProvider().getEightChar(this)
}
