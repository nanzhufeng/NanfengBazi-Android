package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.PillarDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition

data class PillarShenSha(
    val position: PillarPosition,
    val names: List<String>,
)

/** 仅保存共享神煞规则所需的原局参照，不把推导拆散到各个 UI 入口。 */
data class BasicShenShaContext(
    val dayStem: Char,
    val roots: List<Char>,
    val monthBranch: Char,
    val yearBranch: Char,
)

object BasicShenShaRules {
    fun resolve(pillars: List<PillarDetail>): List<PillarShenSha> {
        val byPosition = pillars.associateBy(PillarDetail::position)
        val context = context(pillars)
        return PillarPosition.entries.map { position ->
            val pillar = requireNotNull(byPosition[position])
            PillarShenSha(
                position = position,
                names = resolveNames(pillar.name, context),
            )
        }
    }

    fun context(pillars: List<PillarDetail>): BasicShenShaContext {
        val byPosition = pillars.associateBy(PillarDetail::position)
        return BasicShenShaContext(
            dayStem = requireNotNull(byPosition[PillarPosition.DAY]).heavenStem.single(),
            roots = listOf(
                requireNotNull(byPosition[PillarPosition.YEAR]).earthBranch.single(),
                requireNotNull(byPosition[PillarPosition.DAY]).earthBranch.single(),
            ),
            monthBranch = requireNotNull(byPosition[PillarPosition.MONTH]).earthBranch.single(),
            yearBranch = requireNotNull(byPosition[PillarPosition.YEAR]).earthBranch.single(),
        )
    }

    fun context(
        dayStem: Char,
        roots: List<Char>,
        monthBranch: Char,
        yearBranch: Char,
    ): BasicShenShaContext = BasicShenShaContext(dayStem, roots, monthBranch, yearBranch)

    fun resolveNames(
        pillar: String,
        dayStem: Char,
        roots: List<Char>,
    ): List<String> = resolveNames(
        pillar = pillar,
        context = BasicShenShaContext(
            dayStem = dayStem,
            roots = roots,
            monthBranch = '\u0000',
            yearBranch = '\u0000',
        ),
    )

    fun resolveNames(
        pillar: String,
        context: BasicShenShaContext,
    ): List<String> {
        val branch = pillar.getOrNull(1) ?: return emptyList()
        val stem = pillar.firstOrNull() ?: return emptyList()
        return buildList {
            if (branch in TIAN_YI.getValue(context.dayStem)) add("天乙贵人")
            if (branch == WEN_CHANG.getValue(context.dayStem)) add("文昌贵人")
            if (branch == LU_SHEN.getValue(context.dayStem)) add("禄神")
            if (branch == YANG_REN.getValue(context.dayStem)) add("羊刃")
            if (branch in TAI_JI.getValue(context.dayStem)) add("太极贵人")
            if (stem == TIAN_DE[context.monthBranch]) add("天德贵人")
            if (stem == YUE_DE[context.monthBranch]) add("月德贵人")
            if (branch == HONG_LUAN[context.yearBranch]) add("红鸾")
            if (branch == TIAN_XI[context.yearBranch]) add("天喜")
            if (branch == JIN_YU[context.dayStem]) add("金舆")
            if (branch == GUO_YIN[context.dayStem]) add("国印贵人")
            context.roots.forEach { root ->
                val group = TRINE_ROOT.getValue(root)
                if (branch == group.horse) add("驿马")
                if (branch == group.peach) add("桃花")
                if (branch == group.canopy) add("华盖")
                if (branch == group.general) add("将星")
                if (branch == group.robbery) add("劫煞")
                if (branch == group.disaster) add("灾煞")
                if (branch == group.death) add("亡神")
            }
        }.distinct()
    }
}

private data class TrineShenSha(
    val horse: Char,
    val peach: Char,
    val canopy: Char,
    val general: Char,
    val robbery: Char,
    val disaster: Char,
    val death: Char,
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
private val TAI_JI = mapOf(
    '甲' to setOf('子', '午'), '乙' to setOf('子', '午'),
    '丙' to setOf('卯', '酉'), '丁' to setOf('卯', '酉'),
    '戊' to setOf('辰', '戌', '丑', '未'), '己' to setOf('辰', '戌', '丑', '未'),
    '庚' to setOf('寅', '亥'), '辛' to setOf('寅', '亥'),
    '壬' to setOf('巳', '申'), '癸' to setOf('巳', '申'),
)
private val TIAN_DE = mapOf(
    '寅' to '丁', '卯' to '申', '辰' to '壬', '巳' to '辛',
    '午' to '亥', '未' to '甲', '申' to '癸', '酉' to '寅',
    '戌' to '丙', '亥' to '乙', '子' to '巳', '丑' to '庚',
)
private val YUE_DE = mapOf(
    '寅' to '丙', '午' to '丙', '戌' to '丙',
    '申' to '壬', '子' to '壬', '辰' to '壬',
    '亥' to '甲', '卯' to '甲', '未' to '甲',
    '巳' to '庚', '酉' to '庚', '丑' to '庚',
)
private val HONG_LUAN = mapOf(
    '子' to '卯', '丑' to '寅', '寅' to '丑', '卯' to '子',
    '辰' to '亥', '巳' to '戌', '午' to '酉', '未' to '申',
    '申' to '未', '酉' to '午', '戌' to '巳', '亥' to '辰',
)
private val TIAN_XI = HONG_LUAN.mapValues { (_, branch) ->
    listOf('子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥')
        .let { branches -> branches[(branches.indexOf(branch) + 6) % branches.size] }
}
private val JIN_YU = mapOf(
    '甲' to '辰', '乙' to '巳', '丙' to '未', '丁' to '申', '戊' to '未',
    '己' to '申', '庚' to '戌', '辛' to '亥', '壬' to '丑', '癸' to '寅',
)
private val GUO_YIN = mapOf(
    '甲' to '戌', '乙' to '亥', '丙' to '丑', '丁' to '寅', '戊' to '丑',
    '己' to '寅', '庚' to '辰', '辛' to '巳', '壬' to '未', '癸' to '申',
)
private val TRINE_ROOT = mapOf(
    '申' to TrineShenSha('寅', '酉', '辰', '子', '巳', '午', '亥'),
    '子' to TrineShenSha('寅', '酉', '辰', '子', '巳', '午', '亥'),
    '辰' to TrineShenSha('寅', '酉', '辰', '子', '巳', '午', '亥'),
    '寅' to TrineShenSha('申', '卯', '戌', '午', '亥', '子', '巳'),
    '午' to TrineShenSha('申', '卯', '戌', '午', '亥', '子', '巳'),
    '戌' to TrineShenSha('申', '卯', '戌', '午', '亥', '子', '巳'),
    '巳' to TrineShenSha('亥', '午', '丑', '酉', '寅', '卯', '申'),
    '酉' to TrineShenSha('亥', '午', '丑', '酉', '寅', '卯', '申'),
    '丑' to TrineShenSha('亥', '午', '丑', '酉', '寅', '卯', '申'),
    '亥' to TrineShenSha('巳', '子', '未', '卯', '申', '酉', '寅'),
    '卯' to TrineShenSha('巳', '子', '未', '卯', '申', '酉', '寅'),
    '未' to TrineShenSha('巳', '子', '未', '卯', '申', '酉', '寅'),
)
