package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars

data class NatalChartRelations(
    val heavenStemRelations: List<String>,
    val earthBranchRelations: List<String>,
    val wholePillarRelations: List<String>,
)

/**
 * 只描述原局四柱中可由字符组合确定的关系，不推演格局、旺衰、喜忌或吉凶。
 */
object NatalChartRelationResolver {
    fun resolve(pillars: FourPillars): NatalChartRelations {
        val columns = listOf(
            "年" to pillars.year,
            "月" to pillars.month,
            "日" to pillars.day,
            "时" to pillars.hour,
        ).filter { (_, pillar) -> pillar.length >= 2 }
        val stemRelations = pairRelations(columns) { first, second ->
            val pair = setOf(first[0], second[0])
            buildList {
                STEM_COMBINES[pair]?.let(::add)
                STEM_CLASHES[pair]?.let(::add)
            }
        }
        val branchRelations = buildList {
            addAll(
                pairRelations(columns) { first, second ->
                    val a = first[1]
                    val b = second[1]
                    val pair = setOf(a, b)
                    buildList {
                        BRANCH_COMBINES[pair]?.let(::add)
                        BRANCH_CLASHES[pair]?.let(::add)
                        BRANCH_HARMS[pair]?.let(::add)
                        if (a == b && a in SELF_PUNISH) add("$a${b}自刑")
                        BRANCH_PUNISH_PAIRS[pair]?.let(::add)
                    }
                },
            )
            val branches = columns.map { it.second[1] }.toSet()
            (THREE_HARMONIES + THREE_MEETINGS).forEach { (required, label) ->
                if (branches.containsAll(required)) add(label)
            }
        }.distinct()
        val wholePillarRelations = buildList {
            columns.forEachIndexed { firstIndex, (firstLabel, first) ->
                columns.drop(firstIndex + 1).forEach { (secondLabel, second) ->
                    if (first == second) {
                        add("${firstLabel}柱与${secondLabel}柱：${first}伏吟")
                    }
                }
            }
        }
        return NatalChartRelations(
            heavenStemRelations = stemRelations,
            earthBranchRelations = branchRelations,
            wholePillarRelations = wholePillarRelations,
        )
    }

    private fun pairRelations(
        columns: List<Pair<String, String>>,
        relations: (String, String) -> List<String>,
    ): List<String> = buildList {
        columns.forEachIndexed { firstIndex, (firstLabel, first) ->
            columns.drop(firstIndex + 1).forEach { (secondLabel, second) ->
                relations(first, second).forEach { relation ->
                    add("${firstLabel}柱与${secondLabel}柱：$relation")
                }
            }
        }
    }.distinct()

    private val STEM_COMBINES = mapOf(
        setOf('甲', '己') to "甲己合",
        setOf('乙', '庚') to "乙庚合",
        setOf('丙', '辛') to "丙辛合",
        setOf('丁', '壬') to "丁壬合",
        setOf('戊', '癸') to "戊癸合",
    )
    private val STEM_CLASHES = mapOf(
        setOf('甲', '庚') to "甲庚冲",
        setOf('乙', '辛') to "乙辛冲",
        setOf('丙', '壬') to "丙壬冲",
        setOf('丁', '癸') to "丁癸冲",
    )
    private val BRANCH_COMBINES = mapOf(
        setOf('子', '丑') to "子丑合",
        setOf('寅', '亥') to "寅亥合",
        setOf('卯', '戌') to "卯戌合",
        setOf('辰', '酉') to "辰酉合",
        setOf('巳', '申') to "巳申合",
        setOf('午', '未') to "午未合",
    )
    private val BRANCH_CLASHES = mapOf(
        setOf('子', '午') to "子午冲",
        setOf('丑', '未') to "丑未冲",
        setOf('寅', '申') to "寅申冲",
        setOf('卯', '酉') to "卯酉冲",
        setOf('辰', '戌') to "辰戌冲",
        setOf('巳', '亥') to "巳亥冲",
    )
    private val BRANCH_HARMS = mapOf(
        setOf('子', '未') to "子未相害",
        setOf('丑', '午') to "丑午相害",
        setOf('寅', '巳') to "寅巳相害",
        setOf('卯', '辰') to "卯辰相害",
        setOf('申', '亥') to "申亥相害",
        setOf('酉', '戌') to "酉戌相害",
    )
    private val BRANCH_PUNISH_PAIRS = mapOf(
        setOf('子', '卯') to "子卯相刑",
        setOf('寅', '巳') to "寅巳相刑",
        setOf('巳', '申') to "巳申相刑",
        setOf('丑', '戌') to "丑戌相刑",
        setOf('戌', '未') to "戌未相刑",
        setOf('未', '丑') to "未丑相刑",
    )
    private val SELF_PUNISH = setOf('辰', '午', '酉', '亥')
    private val THREE_HARMONIES = listOf(
        setOf('申', '子', '辰') to "申子辰三合水局",
        setOf('亥', '卯', '未') to "亥卯未三合木局",
        setOf('寅', '午', '戌') to "寅午戌三合火局",
        setOf('巳', '酉', '丑') to "巳酉丑三合金局",
    )
    private val THREE_MEETINGS = listOf(
        setOf('寅', '卯', '辰') to "寅卯辰三会木局",
        setOf('巳', '午', '未') to "巳午未三会火局",
        setOf('申', '酉', '戌') to "申酉戌三会金局",
        setOf('亥', '子', '丑') to "亥子丑三会水局",
    )
}
