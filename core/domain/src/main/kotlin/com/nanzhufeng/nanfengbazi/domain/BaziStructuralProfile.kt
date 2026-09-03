package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import kotlinx.serialization.Serializable

/**
 * 子平结构候选的唯一领域入口。
 *
 * 它只消费已确定的四柱与基础盘明细：月令为提纲，再记录藏干透出、通根和生扶克泄耗。
 * 结果是可回溯的传统文化候选，不生成喜忌、用神、吉凶或现实关系结论。
 */
const val BAZI_STRUCTURAL_PROFILE_RULE_VERSION = "structural-profile-v3"

@Serializable
enum class BaziStructuralConfidence(val displayName: String) {
    HIGH("高"),
    MEDIUM("中"),
    LOW("低"),
}

@Serializable
enum class BaziDayMasterStrength(val displayName: String) {
    STRONG_LEANING("偏旺"),
    BALANCED_LEANING("中和"),
    WEAK_LEANING("偏弱"),
}

@Serializable
data class BaziStructuralEvidence(
    val label: String,
    val detail: String,
)

@Serializable
data class BaziPatternCandidate(
    val name: String,
    val confidence: BaziStructuralConfidence,
    val evidence: List<BaziStructuralEvidence>,
)

@Serializable
data class BaziStructuralProfile(
    val ruleVersion: String,
    val dayMaster: String,
    val monthOrderBranch: String,
    val strength: BaziDayMasterStrength,
    val strengthConfidence: BaziStructuralConfidence,
    val strengthEvidence: List<BaziStructuralEvidence>,
    val selectedPattern: BaziPatternCandidate,
    val patternCandidates: List<BaziPatternCandidate>,
    /** 需要完整制化、气势与岁运再核对的项目，绝不当作已经成立的定格。 */
    val reviewItems: List<String> = emptyList(),
)

object BaziStructuralProfileAnalyzer {
    fun analyze(
        fourPillars: FourPillars,
        basicChartDetails: BasicChartDetails? = null,
    ): BaziStructuralProfile {
        val pillars = listOf(
            StructuralPillar(PillarPosition.YEAR, fourPillars.year),
            StructuralPillar(PillarPosition.MONTH, fourPillars.month),
            StructuralPillar(PillarPosition.DAY, fourPillars.day),
            StructuralPillar(PillarPosition.HOUR, fourPillars.hour),
        )
        val dayMaster = pillars.first { it.position == PillarPosition.DAY }.stem
        val monthPillar = pillars.first { it.position == PillarPosition.MONTH }
        val hiddenStems = basicChartDetails
            ?.pillars
            ?.associate { it.position to it.hiddenStems.map { hidden -> hidden.heavenStem } }
            .orEmpty()
        val monthHidden = hiddenStems[monthPillar.position]
            .orEmpty()
            .ifEmpty { hiddenStemsFor(monthPillar.branch) }
        val monthMainStem = monthHidden.first()
        val monthDeity = tenGod(dayMaster, monthMainStem)
        val strength = analyzeStrength(dayMaster, pillars, monthPillar, monthMainStem, monthDeity, hiddenStems)
        val patterns = analyzePatterns(dayMaster, pillars, monthPillar, monthHidden, strength.reviewItems)
        return BaziStructuralProfile(
            ruleVersion = BAZI_STRUCTURAL_PROFILE_RULE_VERSION,
            dayMaster = dayMaster,
            monthOrderBranch = monthPillar.branch,
            strength = strength.status,
            strengthConfidence = strength.confidence,
            strengthEvidence = strength.evidence,
            selectedPattern = patterns.selected,
            patternCandidates = patterns.candidates,
            reviewItems = strength.reviewItems + patterns.reviewItems,
        )
    }

    private fun analyzeStrength(
        dayMaster: String,
        pillars: List<StructuralPillar>,
        monthPillar: StructuralPillar,
        monthMainStem: String,
        monthDeity: String,
        hiddenStems: Map<PillarPosition, List<String>>,
    ): StrengthAnalysis {
        var score = when (monthDeity) {
            "比肩", "劫财" -> 3.0
            "正印", "偏印" -> 2.0
            "食神", "伤官" -> -1.75
            "正财", "偏财" -> -1.25
            else -> -2.5
        }
        val evidence = mutableListOf(
            BaziStructuralEvidence(
                label = "月令",
                detail = "月支${monthPillar.branch}本气${monthMainStem}为$monthDeity，${monthJudgement(monthDeity)}",
            ),
        )
        val roots = mutableListOf<String>()
        pillars.forEach { pillar ->
            val hidden = hiddenStems[pillar.position].orEmpty().ifEmpty { hiddenStemsFor(pillar.branch) }
            val rootIndex = hidden.indexOfFirst { stemElement(it) == stemElement(dayMaster) }
            if (rootIndex >= 0) {
                score += ROOT_WEIGHTS[rootIndex.coerceAtMost(ROOT_WEIGHTS.lastIndex)]
                roots += "${pillar.position.displayName}${pillar.branch}藏${hidden[rootIndex]}"
            }
        }
        if (roots.isNotEmpty()) {
            evidence += BaziStructuralEvidence("通根", roots.joinToString("、"))
        } else {
            evidence += BaziStructuralEvidence("通根", "四支未见与日主同五行的藏干")
        }
        val supportStems = mutableListOf<String>()
        val drainStems = mutableListOf<String>()
        pillars.filterNot { it.position == PillarPosition.DAY }.forEach { pillar ->
            val deity = tenGod(dayMaster, pillar.stem)
            val factor = STEM_POSITION_WEIGHTS.getValue(pillar.position)
            when (deity) {
                "比肩", "劫财" -> {
                    score += factor
                    supportStems += "${pillar.position.displayName}干${pillar.stem}（$deity）"
                }
                "正印", "偏印" -> {
                    score += factor * 0.8
                    supportStems += "${pillar.position.displayName}干${pillar.stem}（$deity）"
                }
                "正官", "七杀" -> {
                    score -= factor
                    drainStems += "${pillar.position.displayName}干${pillar.stem}（$deity）"
                }
                "食神", "伤官" -> {
                    score -= factor * 0.75
                    drainStems += "${pillar.position.displayName}干${pillar.stem}（$deity）"
                }
                else -> {
                    score -= factor * 0.65
                    drainStems += "${pillar.position.displayName}干${pillar.stem}（$deity）"
                }
            }
        }
        if (supportStems.isNotEmpty()) evidence += BaziStructuralEvidence("天干生扶", supportStems.joinToString("、"))
        if (drainStems.isNotEmpty()) evidence += BaziStructuralEvidence("天干克泄耗", drainStems.joinToString("、"))
        pillars.filterNot { it.position == PillarPosition.MONTH }.forEach { pillar ->
            val mainStem = hiddenStems[pillar.position].orEmpty().ifEmpty { hiddenStemsFor(pillar.branch) }.first()
            when (tenGod(dayMaster, mainStem)) {
                "正印", "偏印" -> score += 0.6
                "正官", "七杀" -> score -= 0.6
                "食神", "伤官" -> score -= 0.45
                "正财", "偏财" -> score -= 0.35
            }
        }
        val status = when {
            score >= 3.0 -> BaziDayMasterStrength.STRONG_LEANING
            score <= -3.0 -> BaziDayMasterStrength.WEAK_LEANING
            else -> BaziDayMasterStrength.BALANCED_LEANING
        }
        val confidence = when {
            kotlin.math.abs(score) >= 5.0 -> BaziStructuralConfidence.HIGH
            kotlin.math.abs(score) >= 2.5 -> BaziStructuralConfidence.MEDIUM
            else -> BaziStructuralConfidence.LOW
        }
        val reviewItems = buildList {
            if (status == BaziDayMasterStrength.WEAK_LEANING && roots.isEmpty() && supportStems.isEmpty()) {
                add("从弱格复核项：通根与生扶不足，仍须核对制化、合化和岁运，不能据此直接定从。")
            }
            if (status == BaziDayMasterStrength.STRONG_LEANING && drainStems.isEmpty()) {
                add("专旺格复核项：泄耗制约不显，仍须核对气势是否纯粹，不能据此直接定专旺。")
            }
        }
        return StrengthAnalysis(status, confidence, evidence, reviewItems)
    }

    private fun analyzePatterns(
        dayMaster: String,
        pillars: List<StructuralPillar>,
        monthPillar: StructuralPillar,
        monthHidden: List<String>,
        inheritedReviewItems: List<String>,
    ): PatternAnalysis {
        val regularMonthHidden = monthHidden.mapIndexedNotNull { index, stem ->
            if (index > 1) return@mapIndexedNotNull null
            val deity = tenGod(dayMaster, stem)
            deity.takeIf(::isRegularPatternDeity)?.let { RegularPatternSource(stem, index, it) }
        }
        val transparent = regularMonthHidden.mapNotNull { source ->
            val positions = pillars
                .filter { it.position != PillarPosition.DAY && it.stem == source.stem }
                .map { it.position }
            positions.takeIf { it.isNotEmpty() }
                ?.let { TransparentStem(source.stem, source.index, source.deity, it) }
        }
        val primaryRegular = regularMonthHidden.firstOrNull { it.index == 0 }
        val selected = transparent.firstOrNull()
            ?.let { source -> RegularPatternSource(source.stem, source.index, source.deity) }
            ?: primaryRegular
        val candidates = transparent
            .plus(listOfNotNull(primaryRegular?.let { main ->
                TransparentStem(main.stem, main.index, main.deity, emptyList())
            }))
            .distinctBy { it.stem }
            .map { source ->
                BaziPatternCandidate(
                    name = patternName(source.deity),
                    confidence = when {
                        source.index == 0 && source.positions.isNotEmpty() && transparent.size == 1 -> BaziStructuralConfidence.HIGH
                        source.positions.isNotEmpty() || source.index == 0 -> BaziStructuralConfidence.MEDIUM
                        else -> BaziStructuralConfidence.LOW
                    },
                    evidence = buildList {
                        add(BaziStructuralEvidence("月令藏干", "月支${monthPillar.branch}${hiddenLevel(source.index)}${source.stem}为${source.deity}"))
                        if (source.positions.isNotEmpty()) {
                            add(BaziStructuralEvidence("透干", "${source.positions.joinToString("、") { it.displayName }}干透出${source.stem}"))
                        } else {
                            add(BaziStructuralEvidence("透干", "未见主气、中气透于年、月、时干，按月令主气取格"))
                        }
                    },
                )
            }
        val selectedCandidate = selected
            ?.let { source -> candidates.first { it.name == patternName(source.deity) } }
            ?: BaziPatternCandidate(
                name = "未取格",
                confidence = BaziStructuralConfidence.LOW,
                evidence = listOf(
                    BaziStructuralEvidence(
                        "月令",
                        "月支${monthPillar.branch}主气为${tenGod(dayMaster, monthHidden.first())}，比劫只作为旺衰依据，不以比劫取格",
                    ),
                ),
            )
        val reviewItems = inheritedReviewItems.toMutableList()
        val branchSet = pillars.map { it.branch }.toSet()
        THREE_HARMONIES.forEach { (branches, label) ->
            if (monthPillar.branch in branches && branches.all { it in branchSet }) {
                reviewItems += "${label}覆盖月令，是否成局、是否改变月令取用需结合全局复核。"
            }
        }
        val combinedStem = STEM_COMBINATIONS[dayMaster]
        if (combinedStem != null) {
            val positions = pillars.filter { it.position != PillarPosition.DAY && it.stem == combinedStem }.map { it.position.displayName }
            if (positions.isNotEmpty()) {
                reviewItems += "${dayMaster}${combinedStem}合化复核项：${positions.joinToString("、")}干见$combinedStem，仍须核对化神得令与有无破局。"
            }
        }
        return PatternAnalysis(selectedCandidate, candidates, reviewItems.distinct())
    }

    private fun patternName(deity: String): String = when (deity) {
        "正官" -> "正官格"
        "七杀" -> "七杀格"
        "正财" -> "正财格"
        "偏财" -> "偏财格"
        "正印" -> "正印格"
        "偏印" -> "偏印格"
        "食神" -> "食神格"
        "伤官" -> "伤官格"
        else -> error("比劫不能用于八正格取格：$deity")
    }

    private fun isRegularPatternDeity(deity: String): Boolean = deity in REGULAR_PATTERN_DEITIES

    private fun monthJudgement(deity: String): String = when (deity) {
        "比肩", "劫财" -> "得令（比劫当令）"
        "正印", "偏印" -> "得月令之生（印星当令）"
        "食神", "伤官" -> "失令且泄身（食伤当令）"
        "正财", "偏财" -> "失令且耗身（财星当令）"
        else -> "失令受制（官杀当令）"
    }

    private fun hiddenLevel(index: Int): String = listOf("本气", "中气", "余气").getOrElse(index) { "藏干" }

    private fun tenGod(dayMaster: String, otherStem: String): String {
        val dayElement = stemElement(dayMaster)
        val otherElement = stemElement(otherStem)
        val samePolarity = (dayMaster in YANG_STEMS) == (otherStem in YANG_STEMS)
        return when {
            dayElement == otherElement -> if (samePolarity) "比肩" else "劫财"
            GENERATES.getValue(dayElement) == otherElement -> if (samePolarity) "食神" else "伤官"
            CONTROLS.getValue(dayElement) == otherElement -> if (samePolarity) "偏财" else "正财"
            CONTROLS.getValue(otherElement) == dayElement -> if (samePolarity) "七杀" else "正官"
            else -> if (samePolarity) "偏印" else "正印"
        }
    }

    private fun stemElement(stem: String): String = STEM_ELEMENTS.getValue(stem)

    private fun hiddenStemsFor(branch: String): List<String> = HIDDEN_STEMS.getValue(branch)

    private data class StructuralPillar(val position: PillarPosition, val value: String) {
        val stem: String get() = value.first().toString()
        val branch: String get() = value.last().toString()
    }

    private data class RegularPatternSource(val stem: String, val index: Int, val deity: String)
    private data class TransparentStem(
        val stem: String,
        val index: Int,
        val deity: String,
        val positions: List<PillarPosition>,
    )
    private data class StrengthAnalysis(
        val status: BaziDayMasterStrength,
        val confidence: BaziStructuralConfidence,
        val evidence: List<BaziStructuralEvidence>,
        val reviewItems: List<String>,
    )
    private data class PatternAnalysis(
        val selected: BaziPatternCandidate,
        val candidates: List<BaziPatternCandidate>,
        val reviewItems: List<String>,
    )

    private val STEM_ELEMENTS = mapOf(
        "甲" to "木", "乙" to "木", "丙" to "火", "丁" to "火", "戊" to "土",
        "己" to "土", "庚" to "金", "辛" to "金", "壬" to "水", "癸" to "水",
    )
    private val YANG_STEMS = setOf("甲", "丙", "戊", "庚", "壬")
    private val GENERATES = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val CONTROLS = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
    private val HIDDEN_STEMS = mapOf(
        "子" to listOf("癸"), "丑" to listOf("己", "癸", "辛"), "寅" to listOf("甲", "丙", "戊"), "卯" to listOf("乙"),
        "辰" to listOf("戊", "乙", "癸"), "巳" to listOf("丙", "庚", "戊"), "午" to listOf("丁", "己"), "未" to listOf("己", "丁", "乙"),
        "申" to listOf("庚", "壬", "戊"), "酉" to listOf("辛"), "戌" to listOf("戊", "辛", "丁"), "亥" to listOf("壬", "甲"),
    )
    private val ROOT_WEIGHTS = listOf(1.75, 1.0, 0.5)
    private val STEM_POSITION_WEIGHTS = mapOf(PillarPosition.YEAR to 0.8, PillarPosition.MONTH to 1.2, PillarPosition.HOUR to 1.0)
    private val REGULAR_PATTERN_DEITIES = setOf("正官", "七杀", "正财", "偏财", "正印", "偏印", "食神", "伤官")
    private val STEM_COMBINATIONS = mapOf("甲" to "己", "己" to "甲", "乙" to "庚", "庚" to "乙", "丙" to "辛", "辛" to "丙", "丁" to "壬", "壬" to "丁", "戊" to "癸", "癸" to "戊")
    private val THREE_HARMONIES = listOf(
        setOf("申", "子", "辰") to "申子辰三合水局复核",
        setOf("亥", "卯", "未") to "亥卯未三合木局复核",
        setOf("寅", "午", "戌") to "寅午戌三合火局复核",
        setOf("巳", "酉", "丑") to "巳酉丑三合金局复核",
        setOf("亥", "子", "丑") to "亥子丑三会水方复核",
        setOf("寅", "卯", "辰") to "寅卯辰三会木方复核",
        setOf("巳", "午", "未") to "巳午未三会火方复核",
        setOf("申", "酉", "戌") to "申酉戌三会金方复核",
    )
}

private val PillarPosition.displayName: String
    get() = when (this) {
        PillarPosition.YEAR -> "年"
        PillarPosition.MONTH -> "月"
        PillarPosition.DAY -> "日"
        PillarPosition.HOUR -> "时"
    }
