package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.PillarDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition

data class PillarShenSha(
    val position: PillarPosition,
    val names: List<String>,
)

object BasicShenShaRules {
    fun resolve(pillars: List<PillarDetail>): List<PillarShenSha> {
        val byPosition = pillars.associateBy(PillarDetail::position)
        val dayStem = requireNotNull(byPosition[PillarPosition.DAY])
            .heavenStem
            .single()
        val roots = listOf(
            requireNotNull(byPosition[PillarPosition.YEAR]).earthBranch.single(),
            requireNotNull(byPosition[PillarPosition.DAY]).earthBranch.single(),
        )
        return PillarPosition.entries.map { position ->
            val pillar = requireNotNull(byPosition[position])
            PillarShenSha(
                position = position,
                names = resolveNames(pillar.name, dayStem, roots),
            )
        }
    }

    fun resolveNames(
        pillar: String,
        dayStem: Char,
        roots: List<Char>,
    ): List<String> {
        val branch = pillar.getOrNull(1) ?: return emptyList()
        return buildList {
            if (branch in TIAN_YI.getValue(dayStem)) add("天乙贵人")
            if (branch == WEN_CHANG.getValue(dayStem)) add("文昌贵人")
            if (branch == LU_SHEN.getValue(dayStem)) add("禄神")
            if (branch == YANG_REN.getValue(dayStem)) add("羊刃")
            roots.forEach { root ->
                val group = TRINE_ROOT.getValue(root)
                if (branch == group.horse) add("驿马")
                if (branch == group.peach) add("桃花")
                if (branch == group.canopy) add("华盖")
                if (branch == group.general) add("将星")
            }
        }.distinct()
    }
}

private data class TrineShenSha(
    val horse: Char,
    val peach: Char,
    val canopy: Char,
    val general: Char,
)

private val TIAN_YI = mapOf(
    '甲' to setOf('丑', '未'), '戊' to setOf('丑', '未'), '庚' to setOf('丑', '未'),
    '乙' to setOf('子', '申'), '己' to setOf('子', '申'),
    '丙' to setOf('亥', '酉'), '丁' to setOf('亥', '酉'),
    '壬' to setOf('卯', '巳'), '癸' to setOf('卯', '巳'),
    '辛' to setOf('寅', '午'),
)
private val WEN_CHANG = mapOf(
    '甲' to '巳', '乙' to '午', '丙' to '申', '戊' to '申', '丁' to '酉',
    '己' to '酉', '庚' to '亥', '辛' to '子', '壬' to '寅', '癸' to '卯',
)
private val LU_SHEN = mapOf(
    '甲' to '寅', '乙' to '卯', '丙' to '巳', '戊' to '巳', '丁' to '午',
    '己' to '午', '庚' to '申', '辛' to '酉', '壬' to '亥', '癸' to '子',
)
private val YANG_REN = mapOf(
    '甲' to '卯', '乙' to '寅', '丙' to '午', '戊' to '午', '丁' to '巳',
    '己' to '巳', '庚' to '酉', '辛' to '申', '壬' to '子', '癸' to '亥',
)
private val TRINE_ROOT = mapOf(
    '申' to TrineShenSha('寅', '酉', '辰', '子'),
    '子' to TrineShenSha('寅', '酉', '辰', '子'),
    '辰' to TrineShenSha('寅', '酉', '辰', '子'),
    '寅' to TrineShenSha('申', '卯', '戌', '午'),
    '午' to TrineShenSha('申', '卯', '戌', '午'),
    '戌' to TrineShenSha('申', '卯', '戌', '午'),
    '巳' to TrineShenSha('亥', '午', '丑', '酉'),
    '酉' to TrineShenSha('亥', '午', '丑', '酉'),
    '丑' to TrineShenSha('亥', '午', '丑', '酉'),
    '亥' to TrineShenSha('巳', '子', '未', '卯'),
    '卯' to TrineShenSha('巳', '子', '未', '卯'),
    '未' to TrineShenSha('巳', '子', '未', '卯'),
)
