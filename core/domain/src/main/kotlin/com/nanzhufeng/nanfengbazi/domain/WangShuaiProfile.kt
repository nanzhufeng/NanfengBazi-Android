package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * 盲派旺点的旺衰比例模型：月令第一、时支第二。
 *
 * 这是南枫八字可复算的工程规则，并非把任一民间权重表宣称为古籍定论。盲派宾主、
 * 体用和做功另存于 [MangPaiProfile]，不以此比例替代。
 */
const val WANG_SHUAI_PROFILE_RULE_VERSION = "blind-month-command-hour-branch-v2"

@Serializable
enum class WangShuaiSide(val displayName: String) {
    SUPPORT("生扶"),
    DRAIN("克泄耗"),
}

@Serializable
enum class WangShuaiLevel(val displayName: String) {
    EXTREMELY_STRONG("极旺"),
    WANG("旺"),
    STRONG("偏旺"),
    BALANCED("平衡"),
    SLIGHTLY_WEAK("稍弱"),
    WEAK("偏弱"),
    EXTREMELY_WEAK("极弱"),
}

@Serializable
data class WangShuaiContribution(
    val position: PillarPosition,
    val source: String,
    val character: String,
    val element: String,
    val side: WangShuaiSide,
    val score: Float,
)

@Serializable
data class WangShuaiProfile(
    val ruleVersion: String,
    val dayMaster: String,
    val monthOrderState: String,
    val supportScore: Float,
    val drainScore: Float,
    val supportPercent: Float,
    val drainPercent: Float,
    /** 同党为比劫、印绶；异党为食伤、财星、官杀。与生扶／克泄耗共用同一份贡献明细。 */
    val samePartyScore: Float = supportScore,
    val differentPartyScore: Float = drainScore,
    val samePartyPercent: Float = supportPercent,
    val differentPartyPercent: Float = drainPercent,
    val score: Int,
    val level: WangShuaiLevel,
    val hourRootBonus: Float,
    val contributions: List<WangShuaiContribution>,
)

object WangShuaiProfileAnalyzer {
    private const val YEAR_BRANCH_WEIGHT = 5f
    private const val MONTH_BRANCH_WEIGHT = 50f
    private const val DAY_BRANCH_WEIGHT = 10f
    private const val HOUR_BRANCH_WEIGHT = 35f
    private const val PRIMARY_HIDDEN_SHARE = 0.6f
    private const val MIDDLE_HIDDEN_SHARE = 0.3f
    private const val RESIDUAL_HIDDEN_SHARE = 0.1f

    fun analyze(fourPillars: FourPillars): WangShuaiProfile {
        val dayMaster = fourPillars.day.first()
        val dayElement = elementOf(dayMaster)
        val resourceElement = resourceOf(dayElement)
        val contributions = buildList {
            addBranch(PillarPosition.YEAR, fourPillars.year[1], YEAR_BRANCH_WEIGHT, dayElement, resourceElement)
            addBranch(PillarPosition.MONTH, fourPillars.month[1], MONTH_BRANCH_WEIGHT, dayElement, resourceElement)
            addBranch(PillarPosition.DAY, fourPillars.day[1], DAY_BRANCH_WEIGHT, dayElement, resourceElement)
            addBranch(PillarPosition.HOUR, fourPillars.hour[1], HOUR_BRANCH_WEIGHT, dayElement, resourceElement)
        }
        val allContributions = contributions
        val supportScore = allContributions.filter { it.side == WangShuaiSide.SUPPORT }.sumOf { it.score.toDouble() }.toFloat()
        val drainScore = allContributions.filter { it.side == WangShuaiSide.DRAIN }.sumOf { it.score.toDouble() }.toFloat()
        val total = (supportScore + drainScore).takeIf { it > 0f } ?: 1f
        val supportPercent = supportScore / total * 100f
        val drainPercent = drainScore / total * 100f
        val score = supportPercent.roundToInt()
        return WangShuaiProfile(
            ruleVersion = WANG_SHUAI_PROFILE_RULE_VERSION,
            dayMaster = dayMaster.toString(),
            monthOrderState = monthOrderState(dayElement, fourPillars.month[1]),
            supportScore = supportScore,
            drainScore = drainScore,
            supportPercent = supportPercent,
            drainPercent = drainPercent,
            samePartyScore = supportScore,
            differentPartyScore = drainScore,
            samePartyPercent = supportPercent,
            differentPartyPercent = drainPercent,
            score = score,
            level = levelFor(score),
            hourRootBonus = 0f,
            contributions = allContributions,
        )
    }

    private fun MutableList<WangShuaiContribution>.addBranch(
        position: PillarPosition,
        branch: Char,
        branchWeight: Float,
        dayElement: String,
        resourceElement: String,
    ) {
        hiddenStems(branch).forEachIndexed { index, hiddenStem ->
            val share = when (index) {
                0 -> if (hiddenStems(branch).size == 1) 1f else PRIMARY_HIDDEN_SHARE
                1 -> MIDDLE_HIDDEN_SHARE
                else -> RESIDUAL_HIDDEN_SHARE
            }
            addContribution(
                position = position,
                source = "${if (index == 0) "本" else if (index == 1) "中" else "余"}气",
                character = hiddenStem,
                element = elementOf(hiddenStem),
                score = branchWeight * share,
                dayElement = dayElement,
                resourceElement = resourceElement,
            )
        }
    }

    private fun MutableList<WangShuaiContribution>.addContribution(
        position: PillarPosition,
        source: String,
        character: Char,
        element: String,
        score: Float,
        dayElement: String,
        resourceElement: String,
    ) {
        add(
            WangShuaiContribution(
                position = position,
                source = source,
                character = character.toString(),
                element = element,
                side = if (element == dayElement || element == resourceElement) {
                    WangShuaiSide.SUPPORT
                } else {
                    WangShuaiSide.DRAIN
                },
                score = score,
            ),
        )
    }

    /** 用户指定的旺衰区间；截图未标示的 50–60 归入平衡，避免制造断层。 */
    private fun levelFor(score: Int) = when {
        score < 10 -> WangShuaiLevel.EXTREMELY_WEAK
        score < 35 -> WangShuaiLevel.WEAK
        score < 45 -> WangShuaiLevel.SLIGHTLY_WEAK
        score < 60 -> WangShuaiLevel.BALANCED
        score < 85 -> WangShuaiLevel.STRONG
        score < 95 -> WangShuaiLevel.WANG
        else -> WangShuaiLevel.EXTREMELY_STRONG
    }

    private fun monthOrderState(dayElement: String, monthBranch: Char): String {
        val monthElement = elementOf(monthBranch)
        return when {
            monthElement == dayElement -> "旺"
            generates(monthElement) == dayElement -> "相"
            generates(dayElement) == monthElement -> "休"
            controls(dayElement) == monthElement -> "囚"
            else -> "死"
        }
    }

    private fun elementOf(character: Char) = when (character) {
        '甲', '乙', '寅', '卯' -> "木"
        '丙', '丁', '巳', '午' -> "火"
        '戊', '己', '辰', '戌', '丑', '未' -> "土"
        '庚', '辛', '申', '酉' -> "金"
        else -> "水"
    }

    private fun generates(element: String) = mapOf(
        "木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木",
    ).getValue(element)

    private fun resourceOf(element: String) = mapOf(
        "木" to "水", "火" to "木", "土" to "火", "金" to "土", "水" to "金",
    ).getValue(element)

    private fun controls(element: String) = mapOf(
        "木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木",
    ).getValue(element)

    private fun hiddenStems(branch: Char): List<Char> = when (branch) {
        '子' -> listOf('癸')
        '丑' -> listOf('己', '癸', '辛')
        '寅' -> listOf('甲', '丙', '戊')
        '卯' -> listOf('乙')
        '辰' -> listOf('戊', '乙', '癸')
        '巳' -> listOf('丙', '戊', '庚')
        '午' -> listOf('丁', '己')
        '未' -> listOf('己', '丁', '乙')
        '申' -> listOf('庚', '壬', '戊')
        '酉' -> listOf('辛')
        '戌' -> listOf('戊', '辛', '丁')
        else -> listOf('壬', '甲')
    }
}
