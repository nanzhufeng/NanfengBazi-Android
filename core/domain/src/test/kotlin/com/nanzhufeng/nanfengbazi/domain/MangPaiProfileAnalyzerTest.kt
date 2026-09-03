package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangPaiProfileAnalyzerTest {
    @Test
    fun `records guest host tiyong actual work paths and imagery for all eight characters`() {
        val profile = MangPaiProfileAnalyzer.analyze(
            FourPillars(year = "甲亥", month = "己丑", day = "丙寅", hour = "辛巳"),
        )

        assertEquals(MANGPAI_PROFILE_RULE_VERSION, profile.ruleVersion)
        assertEquals(MangPaiGuestHostRole.DISTANT_GUEST, profile.pillarImagery.first { it.position == PillarPosition.YEAR }.role)
        assertEquals(MangPaiGuestHostRole.CLOSE_GUEST, profile.pillarImagery.first { it.position == PillarPosition.MONTH }.role)
        assertEquals(MangPaiGuestHostRole.HOST, profile.pillarImagery.first { it.position == PillarPosition.DAY }.role)
        assertEquals(MangPaiGuestHostRole.HOST, profile.pillarImagery.first { it.position == PillarPosition.HOUR }.role)
        assertEquals("日主", profile.tiyongItems.first { it.position == PillarPosition.DAY }.tenGod)
        assertEquals(8, profile.characterImagery.size)
        assertEquals("大树、开拓、向上", profile.characterImagery.first { it.position == PillarPosition.YEAR && it.layer == "干" }.imagery)
        assertEquals("初春、生发、行动", profile.characterImagery.first { it.position == PillarPosition.DAY && it.layer == "支" }.imagery)
        assertTrue(profile.workCandidates.any { it.characters == "亥寅" && it.kind == "合用" })
        assertTrue(profile.workCandidates.any { it.kind == "生用" || it.kind == "制用" })
        assertTrue(profile.workCandidates.all { candidate ->
            val fromRole = profile.pillarImagery.first { it.position == candidate.fromPosition }.role
            val toRole = profile.pillarImagery.first { it.position == candidate.toPosition }.role
            (fromRole == MangPaiGuestHostRole.HOST) != (toRole == MangPaiGuestHostRole.HOST)
        })
    }
}
