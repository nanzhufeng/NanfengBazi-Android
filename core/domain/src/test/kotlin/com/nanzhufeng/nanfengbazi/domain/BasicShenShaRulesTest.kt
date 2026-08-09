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
            listOf("学堂", "太极贵人", "月德贵人", "德秀贵人"),
            BasicShenShaRules.resolveNames("壬申", context),
        )
    }

    @Test
    fun `常用补充神煞保持明确的日干月令和年支参照`() {
        val context = BasicShenShaRules.context(
            dayStem = '甲',
            roots = emptyList(),
            monthBranch = '寅',
            yearBranch = '子',
        )

        assertEquals(
            listOf("天乙贵人", "天医"),
            BasicShenShaRules.resolveNames("乙丑", context),
        )
        assertEquals(
            listOf("福星贵人", "禄神", "孤辰", "丧门"),
            BasicShenShaRules.resolveNames("甲寅", context),
        )
        assertEquals(
            listOf("寡宿", "吊客", "国印贵人"),
            BasicShenShaRules.resolveNames("甲戌", context),
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
