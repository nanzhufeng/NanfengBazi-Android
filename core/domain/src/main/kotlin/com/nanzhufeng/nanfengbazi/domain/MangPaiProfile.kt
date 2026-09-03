package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import kotlinx.serialization.Serializable

const val MANGPAI_PROFILE_RULE_VERSION = "mangpai-binzhu-zuogong-v3"

/** 盲派宾主的柱位层级：日时为主，月为近宾，年为远宾。 */
@Serializable
enum class MangPaiGuestHostRole(val displayName: String, val imagery: String) {
    HOST("主", "自我、内圈与行动结果"),
    CLOSE_GUEST("近宾", "父母、兄弟与近身环境"),
    DISTANT_GUEST("远宾", "祖辈、社会与外部环境"),
}

/** 段氏体系的体用三分；食神、伤官作为中性工具保留其偏向。 */
@Serializable
enum class MangPaiTiyongCategory(val displayName: String) {
    BODY("体"),
    USE("用"),
    NEUTRAL("中性"),
}

@Serializable
data class MangPaiTiyongItem(
    val position: PillarPosition,
    val stem: String,
    val tenGod: String,
    val category: MangPaiTiyongCategory,
    val bias: MangPaiTiyongCategory? = null,
)

/**
 * 原局干支关系的客观检测结果。
 *
 * 它仅标出可能形成的盲派做功路径，不等价于做功已成，也不产生吉凶或现实结论。
 */
@Serializable
data class MangPaiWorkCandidate(
    val kind: String,
    val fromPosition: PillarPosition,
    val toPosition: PillarPosition,
    val characters: String,
    val description: String,
)

@Serializable
data class MangPaiPillarImagery(
    val position: PillarPosition,
    val role: MangPaiGuestHostRole,
    val pillar: String,
)

/** 原局八字逐字的十神、宾主位置与基础取象，不包含现实吉凶断语。 */
@Serializable
data class MangPaiCharacterImagery(
    val position: PillarPosition,
    val role: MangPaiGuestHostRole,
    val layer: String,
    val character: String,
    val tenGod: String,
    val imagery: String,
)

/**
 * 南枫八字盲派基础快照。
 *
 * 参照段建业系盲派的宾主、体用、做功框架，保存原局可核验事实。旺衰比例另属独立模型，
 * 两者不相互覆盖。
 */
@Serializable
data class MangPaiProfile(
    val ruleVersion: String,
    val pillarImagery: List<MangPaiPillarImagery>,
    val tiyongItems: List<MangPaiTiyongItem>,
    val bodyCount: Int,
    val useCount: Int,
    val neutralCount: Int,
    val workCandidates: List<MangPaiWorkCandidate>,
    val characterImagery: List<MangPaiCharacterImagery> = emptyList(),
)

object MangPaiProfileAnalyzer {
    fun analyze(fourPillars: FourPillars): MangPaiProfile {
        val pillars = listOf(
            Pillar(PillarPosition.YEAR, fourPillars.year),
            Pillar(PillarPosition.MONTH, fourPillars.month),
            Pillar(PillarPosition.DAY, fourPillars.day),
            Pillar(PillarPosition.HOUR, fourPillars.hour),
        )
        val dayStem = pillars.first { it.position == PillarPosition.DAY }.stem
        val tiyongItems = pillars.map { pillar -> tiyongItem(dayStem, pillar) }
        return MangPaiProfile(
            ruleVersion = MANGPAI_PROFILE_RULE_VERSION,
            pillarImagery = pillars.map { pillar ->
                MangPaiPillarImagery(
                    position = pillar.position,
                    role = guestHostRole(pillar.position),
                    pillar = pillar.text,
                )
            },
            tiyongItems = tiyongItems,
            bodyCount = tiyongItems.count { it.category == MangPaiTiyongCategory.BODY || it.bias == MangPaiTiyongCategory.BODY },
            useCount = tiyongItems.count { it.category == MangPaiTiyongCategory.USE || it.bias == MangPaiTiyongCategory.USE },
            neutralCount = tiyongItems.count { it.category == MangPaiTiyongCategory.NEUTRAL },
            workCandidates = workCandidates(pillars),
            characterImagery = characterImagery(dayStem, pillars),
        )
    }

    private fun characterImagery(dayStem: Char, pillars: List<Pillar>): List<MangPaiCharacterImagery> =
        pillars.flatMap { pillar ->
            val role = guestHostRole(pillar.position)
            listOf(
                MangPaiCharacterImagery(
                    position = pillar.position,
                    role = role,
                    layer = "干",
                    character = pillar.stem.toString(),
                    tenGod = if (pillar.position == PillarPosition.DAY) "日主" else tenGod(dayStem, pillar.stem),
                    imagery = stemImagery(pillar.stem),
                ),
                MangPaiCharacterImagery(
                    position = pillar.position,
                    role = role,
                    layer = "支",
                    character = pillar.branch.toString(),
                    tenGod = tenGod(dayStem, branchElement(pillar.branch), isYangBranch(pillar.branch)),
                    imagery = branchImagery(pillar.branch),
                ),
            )
        }

    private fun tiyongItem(dayStem: Char, pillar: Pillar): MangPaiTiyongItem {
        if (pillar.position == PillarPosition.DAY) {
            return MangPaiTiyongItem(
                position = pillar.position,
                stem = pillar.stem.toString(),
                tenGod = "日主",
                category = MangPaiTiyongCategory.BODY,
            )
        }
        val tenGod = tenGod(dayStem, pillar.stem)
        val category = when (tenGod) {
            "比肩", "劫财", "正印", "偏印" -> MangPaiTiyongCategory.BODY
            "正财", "偏财", "正官", "七杀" -> MangPaiTiyongCategory.USE
            else -> MangPaiTiyongCategory.NEUTRAL
        }
        val bias = when (tenGod) {
            "食神" -> MangPaiTiyongCategory.BODY
            "伤官" -> MangPaiTiyongCategory.USE
            else -> null
        }
        return MangPaiTiyongItem(
            position = pillar.position,
            stem = pillar.stem.toString(),
            tenGod = tenGod,
            category = category,
            bias = bias,
        )
    }

    private fun workCandidates(pillars: List<Pillar>): List<MangPaiWorkCandidate> = buildList {
        pillars.forEachIndexed { firstIndex, first ->
            pillars.drop(firstIndex + 1).forEach { second ->
                if (!crossesHostAndGuest(first.position, second.position)) return@forEach
                elementalWorkCandidate(first, second, isStem = true)?.let(::add)
                elementalWorkCandidate(first, second, isStem = false)?.let(::add)
                stemCombination(first.stem, second.stem)?.let { transformedElement ->
                    add(
                        MangPaiWorkCandidate(
                            kind = "合用",
                            fromPosition = first.position,
                            toPosition = second.position,
                            characters = "${first.stem}${second.stem}",
                            description = "天干五合",
                        ),
                    )
                }
                branchCombination(first.branch, second.branch)?.let { transformedElement ->
                    add(
                        MangPaiWorkCandidate(
                            kind = "合用",
                            fromPosition = first.position,
                            toPosition = second.position,
                            characters = "${first.branch}${second.branch}",
                            description = "地支六合",
                        ),
                    )
                }
                if (isBranchClash(first.branch, second.branch)) {
                    add(workCandidate("制用", first, second, "地支六冲"))
                }
                if (isBranchPunishment(first.branch, second.branch)) {
                    add(workCandidate("制用", first, second, "地支刑"))
                }
                if (isBranchHarm(first.branch, second.branch)) {
                    add(workCandidate("制用", first, second, "地支害／穿"))
                }
                if (isBranchBreak(first.branch, second.branch)) {
                    add(workCandidate("制用", first, second, "地支破"))
                }
            }
        }
    }

    private fun workCandidate(kind: String, first: Pillar, second: Pillar, description: String) =
        MangPaiWorkCandidate(
            kind = kind,
            fromPosition = first.position,
            toPosition = second.position,
            characters = "${first.branch}${second.branch}",
            description = description,
        )

    private fun elementalWorkCandidate(first: Pillar, second: Pillar, isStem: Boolean): MangPaiWorkCandidate? {
        val firstCharacter = if (isStem) first.stem else first.branch
        val secondCharacter = if (isStem) second.stem else second.branch
        val firstElement = if (isStem) stemElement(firstCharacter) else branchElement(firstCharacter)
        val secondElement = if (isStem) stemElement(secondCharacter) else branchElement(secondCharacter)
        val layer = if (isStem) "天干" else "地支"
        val relation = when {
            generates(firstElement, secondElement) -> "${first.position.mangPaiName()}${layer}生${second.position.mangPaiName()}${layer}"
            generates(secondElement, firstElement) -> "${second.position.mangPaiName()}${layer}生${first.position.mangPaiName()}${layer}"
            controls(firstElement, secondElement) -> "${first.position.mangPaiName()}${layer}制${second.position.mangPaiName()}${layer}"
            controls(secondElement, firstElement) -> "${second.position.mangPaiName()}${layer}制${first.position.mangPaiName()}${layer}"
            firstElement == secondElement -> "${layer}同气相扶"
            else -> return null
        }
        return MangPaiWorkCandidate(
            kind = when {
                relation.contains("生") -> "生用"
                relation.contains("制") -> "制用"
                else -> "比助"
            },
            fromPosition = first.position,
            toPosition = second.position,
            characters = "$firstCharacter$secondCharacter",
            description = relation,
        )
    }

    private fun guestHostRole(position: PillarPosition) = when (position) {
        PillarPosition.DAY, PillarPosition.HOUR -> MangPaiGuestHostRole.HOST
        PillarPosition.MONTH -> MangPaiGuestHostRole.CLOSE_GUEST
        PillarPosition.YEAR -> MangPaiGuestHostRole.DISTANT_GUEST
    }

    private fun crossesHostAndGuest(first: PillarPosition, second: PillarPosition): Boolean =
        (guestHostRole(first) == MangPaiGuestHostRole.HOST) !=
            (guestHostRole(second) == MangPaiGuestHostRole.HOST)

    private fun tenGod(dayStem: Char, otherStem: Char): String {
        return tenGod(dayStem, stemElement(otherStem), isYang(otherStem))
    }

    private fun tenGod(dayStem: Char, otherElement: String, otherIsYang: Boolean): String {
        val dayElement = stemElement(dayStem)
        val samePolarity = isYang(dayStem) == otherIsYang
        return when {
            dayElement == otherElement -> if (samePolarity) "比肩" else "劫财"
            generates(dayElement, otherElement) -> if (samePolarity) "食神" else "伤官"
            generates(otherElement, dayElement) -> if (samePolarity) "偏印" else "正印"
            controls(dayElement, otherElement) -> if (samePolarity) "偏财" else "正财"
            else -> if (samePolarity) "七杀" else "正官"
        }
    }

    private fun stemElement(stem: Char) = when (stem) {
        '甲', '乙' -> "木"
        '丙', '丁' -> "火"
        '戊', '己' -> "土"
        '庚', '辛' -> "金"
        else -> "水"
    }

    private fun branchElement(branch: Char) = when (branch) {
        '寅', '卯' -> "木"
        '巳', '午' -> "火"
        '辰', '戌', '丑', '未' -> "土"
        '申', '酉' -> "金"
        else -> "水"
    }

    private fun isYang(stem: Char) = stem in setOf('甲', '丙', '戊', '庚', '壬')

    private fun isYangBranch(branch: Char) = branch in setOf('子', '寅', '辰', '午', '申', '戌')

    private fun stemImagery(stem: Char) = mapOf(
        '甲' to "大树、开拓、向上", '乙' to "花草、柔韧、牵引",
        '丙' to "太阳、照耀、外放", '丁' to "灯火、温养、专注",
        '戊' to "高山、承载、守成", '己' to "田园、包容、经营",
        '庚' to "矿石、果断、变革", '辛' to "珠玉、精细、修饰",
        '壬' to "江河、流动、包容", '癸' to "雨露、滋养、渗透",
    ).getValue(stem)

    private fun branchImagery(branch: Char) = mapOf(
        '子' to "寒水、起始、流动", '丑' to "湿土、收藏、蓄积",
        '寅' to "初春、生发、行动", '卯' to "春木、舒展、交往",
        '辰' to "湿土、水库、转折", '巳' to "初夏、火势、显化",
        '午' to "盛火、显现、扩张", '未' to "燥土、收纳、经营",
        '申' to "初秋、金气、执行", '酉' to "纯金、规则、收敛",
        '戌' to "燥土、火库、守界", '亥' to "大水、包容、潜藏",
    ).getValue(branch)

    private fun generates(from: String, to: String) = mapOf(
        "木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木",
    )[from] == to

    private fun controls(from: String, to: String) = mapOf(
        "木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木",
    )[from] == to

    private fun stemCombination(first: Char, second: Char) = mapOf(
        setOf('甲', '己') to "土", setOf('乙', '庚') to "金", setOf('丙', '辛') to "水",
        setOf('丁', '壬') to "木", setOf('戊', '癸') to "火",
    )[setOf(first, second)]

    private fun branchCombination(first: Char, second: Char) = mapOf(
        setOf('子', '丑') to "土", setOf('寅', '亥') to "木", setOf('卯', '戌') to "火",
        setOf('辰', '酉') to "金", setOf('巳', '申') to "水", setOf('午', '未') to "土",
    )[setOf(first, second)]

    private fun isBranchClash(first: Char, second: Char) = setOf(first, second) in setOf(
        setOf('子', '午'), setOf('丑', '未'), setOf('寅', '申'),
        setOf('卯', '酉'), setOf('辰', '戌'), setOf('巳', '亥'),
    )

    private fun isBranchPunishment(first: Char, second: Char): Boolean {
        val pair = setOf(first, second)
        return pair in setOf(setOf('子', '卯'), setOf('寅', '巳'), setOf('巳', '申'), setOf('寅', '申')) ||
            (first == second && first in setOf('辰', '午', '酉', '亥')) ||
            pair.all { it in setOf('丑', '未', '戌') } && pair.size > 1
    }

    private fun isBranchHarm(first: Char, second: Char) = setOf(first, second) in setOf(
        setOf('子', '未'), setOf('丑', '午'), setOf('寅', '巳'),
        setOf('卯', '辰'), setOf('申', '亥'), setOf('酉', '戌'),
    )

    private fun isBranchBreak(first: Char, second: Char) = setOf(first, second) in setOf(
        setOf('子', '酉'), setOf('卯', '午'), setOf('辰', '丑'),
        setOf('未', '戌'), setOf('寅', '亥'), setOf('巳', '申'),
    )

    private data class Pillar(val position: PillarPosition, val text: String) {
        val stem: Char get() = text.first()
        val branch: Char get() = text[1]
    }

    private fun PillarPosition.mangPaiName() = when (this) {
        PillarPosition.YEAR -> "年"
        PillarPosition.MONTH -> "月"
        PillarPosition.DAY -> "日"
        PillarPosition.HOUR -> "时"
    }
}
