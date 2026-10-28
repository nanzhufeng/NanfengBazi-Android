package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine

/** Display only confirmed discrepancies; evidence age alone is not a warning. */
internal fun resolveCaseYearZodiacNotice(saved: CalculationResult): String? {
    val mismatch = TymeBaziEngine().verifyYearAndZodiac(saved) ?: return null
    val issues = buildList {
        if (mismatch.expectedYearPillars.isNotEmpty()) {
            add("年柱应为${mismatch.expectedYearPillars.joinToString("/")}")
        }
        if (mismatch.expectedZodiacs.isNotEmpty()) {
            add("属相应为${mismatch.expectedZodiacs.joinToString("/")}")
        }
    }
    return "${issues.joinToString("，")}；请核对出生资料后重新排盘。"
}
