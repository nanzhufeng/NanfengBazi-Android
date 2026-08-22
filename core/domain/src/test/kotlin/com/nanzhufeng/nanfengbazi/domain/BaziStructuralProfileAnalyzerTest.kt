package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaziStructuralProfileAnalyzerTest {
    @Test
    fun `月令藏干透出会生成可审计的格局候选`() {
        val profile = BaziStructuralProfileAnalyzer.analyze(
            FourPillars("甲子", "乙寅", "丙寅", "丁卯"),
        )

        assertEquals("丙", profile.dayMaster)
        assertEquals("寅", profile.monthOrderBranch)
        assertEquals("偏印格候选", profile.selectedPattern.name)
        assertTrue(profile.selectedPattern.evidence.any { it.label == "月令藏干" && it.detail.contains("寅本气甲") })
        assertTrue(profile.selectedPattern.evidence.any { it.label == "透干" && it.detail.contains("年干透出甲") })
        assertTrue(profile.strengthEvidence.any { it.label == "月令" })
        assertTrue(profile.strengthEvidence.any { it.label == "通根" })
    }

    @Test
    fun `根与生扶不足只列从弱复核项而不直接定格`() {
        val profile = BaziStructuralProfileAnalyzer.analyze(
            FourPillars("甲子", "丙午", "庚午", "壬子"),
        )

        assertEquals(BaziDayMasterStrength.WEAK_LEANING, profile.strength)
        assertTrue(profile.reviewItems.any { it.startsWith("从弱格复核项") })
        assertFalse(profile.selectedPattern.name.startsWith("从弱格"))
    }
}
