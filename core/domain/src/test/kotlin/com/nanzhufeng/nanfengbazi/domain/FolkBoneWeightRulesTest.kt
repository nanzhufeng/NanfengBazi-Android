package com.nanzhufeng.nanfengbazi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolkBoneWeightRulesTest {
    @Test
    fun `weights are a reproducible four-part folk lookup`() {
        val result = requireNotNull(
            FolkBoneWeightRulesV1.calculate(
                lunarYearPillar = "甲子",
                lunarMonth = 1,
                lunarDay = 1,
                isLeapMonth = false,
                doubleHourIndex = 0,
            ),
        )

        assertEquals(FolkBoneWeightRulesV1.VERSION, result.version)
        assertEquals(FolkBoneWeightVerdictsV1.VERSION, result.verdictVersion)
        assertEquals(12, result.yearQian)
        assertEquals(6, result.monthQian)
        assertEquals(5, result.dayQian)
        assertEquals(16, result.hourQian)
        assertEquals(39, result.totalQian)
        assertEquals(
            "不须劳碌过平生，独自成家福不轻，早有福星常照命，任君行去百般成。",
            result.maleVerdict,
        )
        assertEquals(
            "此命推来运不通，劳碌奔波一场空。好似俊鸟关笼中，中年末限起秋风。",
            result.femaleVerdict,
        )
    }

    @Test
    fun `leap month boundary is explicit rather than guessed`() {
        val firstHalf = requireNotNull(
            FolkBoneWeightRulesV1.calculate("甲子", 4, 15, true, 0),
        )
        val secondHalf = requireNotNull(
            FolkBoneWeightRulesV1.calculate("甲子", 4, 16, true, 0),
        )

        assertEquals(9, firstHalf.monthQian)
        assertEquals(5, secondHalf.monthQian)
        assertNull(FolkBoneWeightRulesV1.calculate("甲子", 12, 16, true, 0))
    }
}
