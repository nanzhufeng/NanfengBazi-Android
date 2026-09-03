package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult

/**
 * Older adopted snapshots predate the derived reference modules. Recompute only the missing
 * deterministic projection from their frozen pillars; never overwrite source facts or snapshots.
 */
fun CalculationResult.structuralProfileOrAnalyze(): BaziStructuralProfile =
    structuralProfile
        ?.takeIf { it.ruleVersion == BAZI_STRUCTURAL_PROFILE_RULE_VERSION }
        ?: BaziStructuralProfileAnalyzer.analyze(fourPillars, basicChartDetails)

fun CalculationResult.elementDistributionOrAnalyze(): BaziElementDistribution =
    elementDistribution
        ?.takeIf { distribution ->
            distribution.ruleVersion == BAZI_ELEMENT_DISTRIBUTION_RULE_VERSION &&
                distribution.metrics.all { it.tenGodGroup.isNotBlank() }
        }
        ?: BaziElementDistributionAnalyzer.analyze(fourPillars, basicChartDetails)

fun CalculationResult.mangPaiProfileOrAnalyze(): MangPaiProfile =
    mangPaiProfile
        ?.takeIf { profile ->
            profile.ruleVersion == MANGPAI_PROFILE_RULE_VERSION && profile.characterImagery.size == 8
        }
        ?: MangPaiProfileAnalyzer.analyze(fourPillars)

fun CalculationResult.wangShuaiProfileOrAnalyze(): WangShuaiProfile =
    wangShuaiProfile ?: WangShuaiProfileAnalyzer.analyze(fourPillars)
