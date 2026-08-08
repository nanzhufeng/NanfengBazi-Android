package com.nanzhufeng.nanfengbazi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BasicShenShaRulesTest {
    @Test
    fun `基础排盘与专业细盘共用同一神煞规则`() {
        val roots = listOf('巳', '寅')

        assertEquals(
            listOf("禄神"),
            BasicShenShaRules.resolveNames("己巳", '丙', roots),
        )
        assertEquals(
            listOf("羊刃", "桃花", "将星"),
            BasicShenShaRules.resolveNames("甲午", '丙', roots),
        )
    }
}
