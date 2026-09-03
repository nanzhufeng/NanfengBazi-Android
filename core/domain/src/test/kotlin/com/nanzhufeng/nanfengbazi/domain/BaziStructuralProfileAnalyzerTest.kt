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
        assertEquals("偏印格", profile.selectedPattern.name)
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

    @Test
    fun `月令余气即使透干也不单独取格`() {
        val profile = BaziStructuralProfileAnalyzer.analyze(
            FourPillars("丁亥", "甲戌", "己卯", "丙子"),
        )

        assertEquals("未取格", profile.selectedPattern.name)
        assertTrue(profile.selectedPattern.evidence.single().detail.contains("比劫只作为旺衰依据"))
        assertFalse(profile.patternCandidates.any { it.name.contains("月劫") || it.name.contains("建禄") })
        assertFalse(profile.selectedPattern.name.contains("候选"))
    }

    @Test
    fun `月令中气透干优先于未透主气取格`() {
        val profile = BaziStructuralProfileAnalyzer.analyze(
            FourPillars("乙亥", "丁寅", "庚申", "丙子"),
        )

        assertEquals("七杀格", profile.selectedPattern.name)
        assertTrue(profile.selectedPattern.evidence.any { it.detail.contains("寅中气丙为七杀") })
        assertTrue(profile.selectedPattern.evidence.any { it.detail.contains("时干透出丙") })
    }

    @Test
    fun `月令只有比劫时不将比劫显示为格局`() {
        val profile = BaziStructuralProfileAnalyzer.analyze(
            FourPillars("丙辰", "辛卯", "甲子", "戊辰"),
        )

        assertEquals("未取格", profile.selectedPattern.name)
        assertTrue(profile.selectedPattern.evidence.single().detail.contains("比劫只作为旺衰依据"))
        assertFalse(profile.selectedPattern.name.contains("月劫"))
        assertFalse(profile.selectedPattern.name.contains("候选"))
    }
}
