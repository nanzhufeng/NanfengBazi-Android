package com.nanzhufeng.nanfengbazi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BasicShenShaRulesTest {
    @Test
    fun `基础排盘与专业细盘共用同一神煞规则`() {
        val roots = listOf('巳', '寅')

        assertEquals(
            listOf("禄神", "亡神"),
            BasicShenShaRules.resolveNames("己巳", '丙', roots),
        )
        assertEquals(
            listOf("羊刃", "桃花", "将星"),
            BasicShenShaRules.resolveNames("甲午", '丙', roots),
        )
    }

    @Test
    fun `月令和年支参照星也保持在同一领域规则内`() {
        val context = BasicShenShaRules.context(
            dayStem = '壬',
            roots = listOf('申', '申'),
            monthBranch = '申',
            yearBranch = '申',
        )

        assertEquals(
            listOf("太极贵人", "月德贵人"),
            BasicShenShaRules.resolveNames("壬申", context),
        )
    }

    @Test
    fun `三合局的劫灾亡也由共享规则统一输出`() {
        assertEquals(
            listOf("灾煞"),
            BasicShenShaRules.resolveNames("丙午", '壬', listOf('申')),
        )
    }
}
