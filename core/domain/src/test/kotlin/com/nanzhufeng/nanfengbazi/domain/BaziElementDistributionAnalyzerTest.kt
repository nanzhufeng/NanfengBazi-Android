package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaziElementDistributionAnalyzerTest {
    @Test
    fun `五行能量以月令第一时支第二并保留十神归类`() {
        val result = BaziElementDistributionAnalyzer.analyze(
            FourPillars("甲子", "乙丑", "丙寅", "丁卯"),
        )

        assertEquals("火", result.dayMasterElement)
        assertEquals("木", result.supportingElement)
        assertEquals(4, result.metrics.first { it.element == "木" }.surfaceCount)
        assertEquals(2, result.metrics.first { it.element == "火" }.surfaceCount)
        assertEquals(1, result.metrics.first { it.element == "土" }.surfaceCount)
        assertEquals(1, result.metrics.first { it.element == "水" }.surfaceCount)
        assertEquals(41f, result.metrics.first { it.element == "木" }.energyScore, 0.0001f)
        assertEquals(3f, result.metrics.first { it.element == "火" }.energyScore, 0.0001f)
        assertEquals(31f, result.metrics.first { it.element == "土" }.energyScore, 0.0001f)
        assertEquals(20f, result.metrics.first { it.element == "水" }.energyScore, 0.0001f)
        assertEquals(5f, result.metrics.first { it.element == "金" }.energyScore, 0.0001f)
        assertEquals("印绶", result.metrics.first { it.element == "木" }.tenGodGroup)
        assertEquals("比劫", result.metrics.first { it.element == "火" }.tenGodGroup)
        assertEquals("食伤", result.metrics.first { it.element == "土" }.tenGodGroup)
        assertEquals("财星", result.metrics.first { it.element == "金" }.tenGodGroup)
        assertEquals("官杀", result.metrics.first { it.element == "水" }.tenGodGroup)
        assertEquals(100f, result.metrics.sumOf { it.energySharePercent.toDouble() }.toFloat())
    }

    @Test
    fun `藏干参与能量但不改变表层个数`() {
        val result = BaziElementDistributionAnalyzer.analyze(
            FourPillars("甲子", "乙丑", "丙寅", "丁卯"),
        )

        assertEquals(2, result.metrics.first { it.element == "木" }.hiddenStemCount)
        assertEquals(1, result.metrics.first { it.element == "火" }.hiddenStemCount)
        assertEquals(2, result.metrics.first { it.element == "土" }.hiddenStemCount)
        assertEquals(2, result.metrics.first { it.element == "水" }.hiddenStemCount)
        assertEquals(1, result.metrics.first { it.element == "金" }.hiddenStemCount)
    }
}
