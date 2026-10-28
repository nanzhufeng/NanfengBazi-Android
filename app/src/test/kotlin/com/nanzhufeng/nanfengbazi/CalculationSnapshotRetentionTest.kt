package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import org.junit.Assert.*
import org.junit.Test

class CalculationSnapshotRetentionTest {
    @Test fun `catalog rule upgrade preserves all old snapshots candidates and adopted input`() {
        val old = sampleStoredCase("boundary-retention")
        val snapshot = old.calculationSnapshots.first()
        val oldCase = old.copy(calculationSnapshots = listOf(snapshot.copy(result = snapshot.result.copy(
            evidence=snapshot.result.evidence.copy(ruleVersion="stage0-v1"),
        ))))
        val next = old.copy(calculationSnapshots = listOf(snapshot.copy(id="new-rule-snapshot", result=snapshot.result.copy(
            evidence=snapshot.result.evidence.copy(ruleVersion=CalculationProfile.DEFAULT_RULE_VERSION),
        ))))
        val kept = next.preservingCalculationsAcrossRuleChange(oldCase)
        assertEquals(oldCase.birthInput,kept.birthInput)
        assertEquals(oldCase.birthTimeCandidates,kept.birthTimeCandidates)
        assertEquals(oldCase.calculationSnapshots,kept.calculationSnapshots)
    }

    @Test fun `uncertain new catalog cannot erase an adopted historical chart`() {
        val old = sampleStoredCase("uncertain-retention")
        val next = old.copy(calculationSnapshots=emptyList(),birthTimeCandidates=emptyList())
        val kept = next.preservingCalculationsAcrossRuleChange(old)
        assertEquals(old.calculationSnapshots,kept.calculationSnapshots)
        assertEquals(old.birthTimeCandidates,kept.birthTimeCandidates)
    }
}
