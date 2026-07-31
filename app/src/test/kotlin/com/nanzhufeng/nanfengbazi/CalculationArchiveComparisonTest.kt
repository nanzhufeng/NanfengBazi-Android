package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationArchiveComparisonTest {
    @Test
    fun `同一输入和口径的版本升级可归因且逐项报告结果变化`() {
        val previousResult = calculationResult(validBirthInput())
        val currentResult = previousResult.copy(
            profile = previousResult.profile.copy(
                engineVersion = "1.6.0",
                ruleVersion = "stage8-v1",
            ),
            fourPillars = FourPillars("辛巳", "戊寅", "丁巳", "乙巳"),
            evidence = previousResult.evidence.copy(
                engineVersion = "1.6.0",
                ruleVersion = "stage8-v1",
            ),
        )

        val comparison = compareCalculationSnapshots(
            snapshot("old", previousResult),
            snapshot("new", currentResult),
        )

        assertFalse(comparison.inputChanged)
        assertFalse(comparison.ruleConfigurationChanged)
        assertTrue(comparison.engineVersionChanged)
        assertTrue(comparison.ruleVersionChanged)
        assertTrue(comparison.versionUpgradeComparable)
        assertEquals(listOf("年柱"), comparison.outcomeChanges.map { it.label })
        assertTrue(comparison.attributionSummary.contains("可用于核对"))
    }

    @Test
    fun `出生输入变化时不会把差异归因于引擎升级`() {
        val previousResult = calculationResult(validBirthInput())
        val currentResult = previousResult.copy(
            normalizedInput = previousResult.normalizedInput.copy(locationName = "上海市"),
            evidence = previousResult.evidence.copy(engineVersion = "1.6.0"),
        )

        val comparison = compareCalculationSnapshots(
            snapshot("old", previousResult),
            snapshot("new", currentResult),
        )

        assertTrue(comparison.inputChanged)
        assertFalse(comparison.versionUpgradeComparable)
        assertTrue(comparison.attributionSummary.contains("不能归因"))
    }

    @Test
    fun `计算口径变化时与同版本稳定重算分别明确标注`() {
        val result = calculationResult(validBirthInput())
        val changedRule = result.copy(
            profile = CalculationProfile.tymeDefault(
                ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
            ),
        )
        val ruleComparison = compareCalculationSnapshots(
            snapshot("old", result),
            snapshot("rule", changedRule),
        )
        val stableComparison = compareCalculationSnapshots(
            snapshot("old", result),
            snapshot("same", result),
        )

        assertTrue(ruleComparison.ruleConfigurationChanged)
        assertTrue(ruleComparison.attributionSummary.contains("计算口径已变化"))
        assertTrue(stableComparison.outcomeConsistent)
        assertTrue(stableComparison.attributionSummary.contains("同一版本重新计算"))
    }

    private fun snapshot(
        id: String,
        result: com.nanzhufeng.nanfengbazi.domain.model.CalculationResult,
    ): CaseCalculationSnapshot = CaseCalculationSnapshot(
        id = id,
        result = result,
        adopted = id != "old",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun validBirthInput(): BirthInput =
        (CaseFormValidator.validate(validForm()) as CaseFormValidation.Valid).birthInput
}
