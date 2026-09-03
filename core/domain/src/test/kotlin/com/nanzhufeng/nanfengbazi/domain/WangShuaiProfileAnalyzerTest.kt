package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import org.junit.Assert.assertEquals
import org.junit.Test

class WangShuaiProfileAnalyzerTest {
    @Test
    fun `month command is first and hour branch is second`() {
        val profile = WangShuaiProfileAnalyzer.analyze(
            FourPillars(year = "甲子", month = "己丑", day = "丙寅", hour = "丁卯"),
        )

        assertEquals(WANG_SHUAI_PROFILE_RULE_VERSION, profile.ruleVersion)
        assertEquals("丙", profile.dayMaster)
        assertEquals("休", profile.monthOrderState)
        assertEquals(0f, profile.hourRootBonus, 0.0001f)
        assertEquals(44f, profile.supportScore, 0.0001f)
        assertEquals(56f, profile.drainScore, 0.0001f)
        assertEquals(profile.supportScore, profile.samePartyScore, 0.0001f)
        assertEquals(profile.drainScore, profile.differentPartyScore, 0.0001f)
        assertEquals(profile.supportPercent, profile.samePartyPercent, 0.0001f)
        assertEquals(profile.drainPercent, profile.differentPartyPercent, 0.0001f)
        assertEquals(44, profile.score)
        assertEquals(WangShuaiLevel.SLIGHTLY_WEAK, profile.level)
    }
}
