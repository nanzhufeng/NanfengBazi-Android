package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase

/** Catalog maintenance may refresh sources, but a rule upgrade is not consent to re-adopt. */
internal fun BaziCase.preservingCalculationsAcrossRuleChange(existing: BaziCase): BaziCase {
    val old = existing.calculationSnapshots.lastOrNull { it.adopted }?.result ?: return this
    val next = calculationSnapshots.lastOrNull { it.adopted }?.result
    if (old.evidence.ruleVersion == next?.evidence?.ruleVersion &&
        old.solarYearMonthEvidence?.ruleVersion == next?.solarYearMonthEvidence?.ruleVersion
    ) return this
    return copy(
        birthInput = existing.birthInput,
        birthTimeCandidates = existing.birthTimeCandidates,
        calculationSnapshots = existing.calculationSnapshots,
    )
}
