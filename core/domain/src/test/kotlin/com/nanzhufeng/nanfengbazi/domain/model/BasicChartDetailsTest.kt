package com.nanzhufeng.nanfengbazi.domain.model

import org.junit.Assert.assertThrows
import org.junit.Test

class BasicChartDetailsTest {
    @Test
    fun `基础排盘拒绝缺柱或重复柱位`() {
        val year = pillar(PillarPosition.YEAR)
        val error = assertThrows(IllegalArgumentException::class.java) {
            BasicChartDetails(
                zodiac = "猴",
                westernZodiac = "处女",
                dayMaster = "壬",
                pillars = listOf(
                    year,
                    pillar(PillarPosition.MONTH),
                    pillar(PillarPosition.DAY),
                    year.copy(),
                ),
                previousSolarTerm = term("处暑"),
                nextSolarTerm = term("白露"),
            )
        }

        check(error.message.orEmpty().contains("年、月、日、时"))
    }

    private fun pillar(position: PillarPosition) = PillarDetail(
        position = position,
        name = "壬申",
        heavenStem = "壬",
        earthBranch = "申",
        heavenStemElement = "水",
        earthBranchElement = "金",
        primaryTenGod = "比肩",
        hiddenStems = emptyList(),
        terrain = "长生",
        selfSittingTerrain = "长生",
        voidEarthBranches = listOf("戌", "亥"),
        naYin = "剑锋金",
    )

    private fun term(name: String) = SolarTermPoint(
        name = name,
        type = SolarTermType.QI,
        at = CivilDateTime(1992, 8, 23, 12, 0, 0),
    )
}
