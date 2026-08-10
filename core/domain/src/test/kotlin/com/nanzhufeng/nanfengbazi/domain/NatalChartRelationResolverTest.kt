package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import org.junit.Assert.assertTrue
import org.junit.Test

class NatalChartRelationResolverTest {
    @Test
    fun `原局关系同时覆盖天干地支三合和整柱伏吟`() {
        val result = NatalChartRelationResolver.resolve(
            FourPillars(
                year = "甲申",
                month = "己子",
                day = "甲辰",
                hour = "甲申",
            ),
        )

        assertTrue(result.heavenStemRelations.any { it.contains("甲己合") })
        assertTrue(result.earthBranchRelations.any { it.contains("申子辰三合水局") })
        assertTrue(result.wholePillarRelations.any { it.contains("甲申伏吟") })
    }
}
