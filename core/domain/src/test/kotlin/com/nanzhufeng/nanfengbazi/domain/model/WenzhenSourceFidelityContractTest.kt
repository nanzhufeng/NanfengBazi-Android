package com.nanzhufeng.nanfengbazi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WenzhenSourceFidelityContractTest {
    @Test
    fun `静态字段与四柱神煞都明确标记为仅来源证据`() {
        assertTrue(WenzhenSourceFidelityContract.isSourceOnly("chart.star_lodge"))
        assertTrue(
            WenzhenSourceFidelityContract.isSourceOnly("chart.year.spirits"),
        )
        assertEquals(
            "年柱 · 神煞",
            WenzhenSourceFidelityContract
                .definitionFor("chart.year.spirits")
                ?.displayLabel,
        )
        assertFalse(WenzhenSourceFidelityContract.isSourceOnly("chart.fetal_origin"))
    }

    @Test
    fun `五行和党派比例使用百分比类型且其他来源字段保持文本`() {
        assertTrue(
            WenzhenSourceFidelityContract
                .isPercentage("chart.five_element.metal_percent"),
        )
        assertTrue(
            WenzhenSourceFidelityContract
                .isPercentage("chart.five_element.same_party_percent"),
        )
        assertFalse(WenzhenSourceFidelityContract.isPercentage("chart.life_gua"))
    }
}
