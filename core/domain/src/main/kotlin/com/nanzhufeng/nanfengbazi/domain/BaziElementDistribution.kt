package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import kotlinx.serialization.Serializable

const val BAZI_ELEMENT_DISTRIBUTION_RULE_VERSION = "blind-month-command-hour-branch-v2"

/** 单一元素的表层数、藏干数、十神归类及月令主导计权后的能量占比。 */
@Serializable
data class BaziElementMetric(
    val element: String,
    val surfaceCount: Int,
    val hiddenStemCount: Int,
    val energyScore: Float,
    val energySharePercent: Float,
    val tenGodGroup: String = "",
)

/**
 * 南枫八字盲派旺点口径的五行能量统计 v1。
 *
 * 旺衰能量只取地支，并按「月令 50、时支 35、日支 10、年支 5」分配；支内再按
 * 本气／中气／余气 0.6／0.3／0.1 拆分到五行。表层个数及含藏数量仍只计出现，
 * 不与能量权重混用。
 *
 * 这是一套可复算的旺衰辅助统计，不取代盲派宾主、体用和做功分析。数值依据见
 * docs/domain-rules.md 的「盲派旺点比例」条目。
 */
@Serializable
data class BaziElementDistribution(
    val ruleVersion: String,
    val dayMasterElement: String,
    val supportingElement: String,
    val metrics: List<BaziElementMetric>,
)

object BaziElementDistributionAnalyzer {
    private val orderedElements = listOf("木", "火", "土", "金", "水")

    fun analyze(
        fourPillars: FourPillars,
        basicChartDetails: BasicChartDetails? = null,
    ): BaziElementDistribution {
        val surfaceElements = basicChartDetails?.pillars?.flatMap { pillar ->
            listOf(pillar.heavenStemElement, pillar.earthBranchElement)
        } ?: fourPillars.characters().map(::elementFor)
        val hiddenElements = basicChartDetails?.pillars
            ?.flatMap { pillar -> pillar.hiddenStems.map { it.element } }
            ?: fourPillars.branches().flatMap { branch ->
                HIDDEN_STEM_WEIGHTS.getValue(branch).keys.map(::elementFor)
            }
        val dayMasterElement = basicChartDetails?.pillars
            ?.firstOrNull { it.position.name == "DAY" }
            ?.heavenStemElement
            ?: elementFor(fourPillars.day.first())
        val supportingElement = elementThatGenerates(dayMasterElement)
        val energyScores = orderedElements.associateWith { 0f }.toMutableMap()
        listOf(
            fourPillars.year[1] to YEAR_BRANCH_WEIGHT,
            fourPillars.month[1] to MONTH_BRANCH_WEIGHT,
            fourPillars.day[1] to DAY_BRANCH_WEIGHT,
            fourPillars.hour[1] to HOUR_BRANCH_WEIGHT,
        ).forEach { (branch, branchWeight) ->
            HIDDEN_STEM_WEIGHTS.getValue(branch).forEach { (stem, weight) ->
                val element = elementFor(stem)
                energyScores[element] = energyScores.getValue(element) + branchWeight * weight
            }
        }
        val totalEnergy = energyScores.values.sum().takeIf { it > 0f } ?: 1f
        val metrics = orderedElements.map { element ->
            BaziElementMetric(
                element = element,
                surfaceCount = surfaceElements.count { it == element },
                hiddenStemCount = hiddenElements.count { it == element },
                energyScore = energyScores.getValue(element),
                energySharePercent = energyScores.getValue(element) / totalEnergy * 100f,
                tenGodGroup = tenGodGroupFor(element, dayMasterElement),
            )
        }
        return BaziElementDistribution(
            ruleVersion = BAZI_ELEMENT_DISTRIBUTION_RULE_VERSION,
            dayMasterElement = dayMasterElement,
            supportingElement = supportingElement,
            metrics = metrics,
        )
    }

    private fun FourPillars.characters(): List<Char> = listOf(year, month, day, hour)
        .flatMap { pillar -> pillar.take(2).toList() }

    private fun FourPillars.branches(): List<Char> = listOf(year, month, day, hour)
        .map { pillar -> pillar[1] }

    private fun elementFor(character: Char): String = when (character) {
        '甲', '乙', '寅', '卯' -> "木"
        '丙', '丁', '巳', '午' -> "火"
        '戊', '己', '辰', '戌', '丑', '未' -> "土"
        '庚', '辛', '申', '酉' -> "金"
        '壬', '癸', '亥', '子' -> "水"
        else -> "未知"
    }

    private fun elementThatGenerates(dayMasterElement: String): String = when (dayMasterElement) {
        "木" -> "水"
        "火" -> "木"
        "土" -> "火"
        "金" -> "土"
        "水" -> "金"
        else -> "未知"
    }

    private fun tenGodGroupFor(element: String, dayMasterElement: String): String = when {
        element == dayMasterElement -> "比劫"
        element == elementThatGenerates(dayMasterElement) -> "印绶"
        elementGeneratedBy(dayMasterElement) == element -> "食伤"
        elementThatControls(dayMasterElement) == element -> "财星"
        elementThatControls(element) == dayMasterElement -> "官杀"
        else -> "—"
    }

    private fun elementThatControls(element: String): String = when (element) {
        "木" -> "土"
        "火" -> "金"
        "土" -> "水"
        "金" -> "木"
        "水" -> "火"
        else -> "未知"
    }

    private fun elementGeneratedBy(element: String): String = when (element) {
        "木" -> "火"
        "火" -> "土"
        "土" -> "金"
        "金" -> "水"
        "水" -> "木"
        else -> "未知"
    }

    private const val YEAR_BRANCH_WEIGHT = 5f
    private const val MONTH_BRANCH_WEIGHT = 50f
    private const val DAY_BRANCH_WEIGHT = 10f
    private const val HOUR_BRANCH_WEIGHT = 35f

    /** 本气／中气／余气折算采用 0.6／0.3／0.1，保证每个地支的能量守恒。 */
    private val HIDDEN_STEM_WEIGHTS = mapOf(
        '子' to mapOf('癸' to 1f),
        '丑' to mapOf('己' to 0.6f, '癸' to 0.3f, '辛' to 0.1f),
        '寅' to mapOf('甲' to 0.6f, '丙' to 0.3f, '戊' to 0.1f),
        '卯' to mapOf('乙' to 1f),
        '辰' to mapOf('戊' to 0.6f, '乙' to 0.3f, '癸' to 0.1f),
        '巳' to mapOf('丙' to 0.6f, '戊' to 0.3f, '庚' to 0.1f),
        '午' to mapOf('丁' to 0.7f, '己' to 0.3f),
        '未' to mapOf('己' to 0.6f, '丁' to 0.3f, '乙' to 0.1f),
        '申' to mapOf('庚' to 0.6f, '壬' to 0.3f, '戊' to 0.1f),
        '酉' to mapOf('辛' to 1f),
        '戌' to mapOf('戊' to 0.6f, '辛' to 0.3f, '丁' to 0.1f),
        '亥' to mapOf('壬' to 0.7f, '甲' to 0.3f),
    )
}
