package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenSourceFidelityContract
import com.nanzhufeng.nanfengbazi.domain.model.completedAgeRangeDisplay
import com.nanzhufeng.nanfengbazi.domain.model.solarBirthDateTimeForFortuneDisplay
import kotlinx.serialization.Serializable

/**
 * 双盘传统关系的可审计规则合同。
 *
 * 本合同只读取两个命例已经采用的四柱快照，不重新排盘，也不对现实婚恋作确定性判断。
 */
const val BAZI_COMPATIBILITY_RULE_VERSION = "compatibility-v3"

@Serializable
enum class BaziCompatibilitySide {
    LEFT,
    RIGHT,
}

@Serializable
data class BaziCompatibilityPillarRef(
    val side: BaziCompatibilitySide,
    val position: PillarPosition,
    val value: String,
)

@Serializable
enum class BaziCompatibilitySignalKind(
    val title: String,
    val isCoordination: Boolean,
) {
    STEM_FIVE_COMBINATION("天干五合", true),
    BRANCH_SIX_HARMONY("地支六合", true),
    BRANCH_THREE_HARMONY("地支三合", true),
    BRANCH_THREE_HARMONY_CANDIDATE("地支半合候选", true),
    BRANCH_THREE_MEETING("地支三会", true),
    BRANCH_THREE_MEETING_CANDIDATE("地支半会候选", true),
    BRANCH_SIX_CLASH("地支六冲", false),
    BRANCH_SIX_HARM("地支六害", false),
    BRANCH_SIX_BREAK("地支六破", false),
    BRANCH_THREE_PUNISHMENT("地支三刑", false),
    BRANCH_SELF_PUNISHMENT("地支自刑", false),
}

@Serializable
data class BaziCompatibilitySignal(
    val kind: BaziCompatibilitySignalKind,
    val values: String,
    val pillars: List<BaziCompatibilityPillarRef>,
    val explanation: String,
)

@Serializable
enum class BaziCompatibilityWarningCode {
    LEFT_TIME_NOT_EXACT,
    RIGHT_TIME_NOT_EXACT,
    CALCULATION_PROFILE_DIFFERENT,
    LEFT_CALCULATION_WARNING,
    RIGHT_CALCULATION_WARNING,
}

@Serializable
data class BaziCompatibilityWarning(
    val code: BaziCompatibilityWarningCode,
    val message: String,
)

/** 合盘记录保留的基础命盘展示投影；不依赖原命例后续编辑。 */
@Serializable
data class BaziCompatibilityPillarPresentation(
    val position: PillarPosition,
    val primaryTenGod: String,
    val heavenStem: String,
    val earthBranch: String,
    val hiddenStemSummary: String,
)

@Serializable
data class BaziCompatibilityDecadePresentation(
    val name: String,
    val ageRange: String,
    /** 大运天干相对日主的十神；基础快照未单独保存时由已保存干支补齐。 */
    val stemTenGod: String = "",
)

/** 合盘提示词可引用的双方断事材料，和命盘事实分开保存。 */
@Serializable
data class BaziCompatibilityReferenceNote(
    val label: String,
    val content: String,
)

@Serializable
data class BaziCompatibilityParticipant(
    val caseId: String,
    val alias: String,
    val revision: Long,
    val adoptedSnapshotId: String,
    val pillars: FourPillars,
    val dayMaster: String,
    val timePrecision: TimePrecision,
    val calculationProfileId: String,
    val solarDateTimeText: String = "",
    val lunarDateTimeText: String = "",
    val solarDateValues: List<String> = emptyList(),
    val zodiac: String = "",
    val pillarPresentation: List<BaziCompatibilityPillarPresentation> = emptyList(),
    val decadeFortunes: List<BaziCompatibilityDecadePresentation> = emptyList(),
    /** 问真导入时的原始格局来源，和本机结构候选分开保存。 */
    val sourceStructureText: String = "",
    /** 只根据本参与者已采用快照的四柱与基础盘明细得出的结构候选。 */
    val structuralProfile: BaziStructuralProfile? = null,
    val referenceNotes: List<BaziCompatibilityReferenceNote> = emptyList(),
)

/** 兼容旧合盘快照：只根据其冻结四柱补齐结构候选，不读取或改写单命例。 */
fun BaziCompatibilityParticipant.structuralProfileOrAnalyze(): BaziStructuralProfile =
    structuralProfile
        ?.takeIf { it.ruleVersion == BAZI_STRUCTURAL_PROFILE_RULE_VERSION }
        ?: BaziStructuralProfileAnalyzer.analyze(pillars)

@Serializable
data class BaziDayMasterRelation(
    val leftElement: String,
    val rightElement: String,
    val description: String,
    val explanation: String,
)

/** 面向普通人的关系总结；每句话来自同一份冻结双盘关系信号，不展示专业术语。 */
@Serializable
data class BaziCompatibilityRelationshipSummary(
    val relationshipJudgement: String = "",
    val advantage: String = "",
    val caution: String = "",
    val suggestion: String = "",
    val dataReminder: String = "",
)

@Serializable
data class BaziCompatibilityReport(
    val ruleVersion: String,
    val left: BaziCompatibilityParticipant,
    val right: BaziCompatibilityParticipant,
    val dayMasterRelation: BaziDayMasterRelation,
    val coordinationSignals: List<BaziCompatibilitySignal>,
    val tensionSignals: List<BaziCompatibilitySignal>,
    val warnings: List<BaziCompatibilityWarning>,
    val relationshipSummary: BaziCompatibilityRelationshipSummary = BaziCompatibilityRelationshipSummary(),
    val guidance: String,
)

/** 已保存的合盘快照；它不跟随单命例后续修改而改变。 */
@Serializable
data class BaziCompatibilityRecord(
    val id: String,
    val createdAtEpochMillis: Long,
    val report: BaziCompatibilityReport,
)

enum class BaziCompatibilityRejectionCode {
    SAME_CASE,
    LEFT_NOT_MAN,
    RIGHT_NOT_WOMAN,
    LEFT_NOT_USER_CASE,
    RIGHT_NOT_USER_CASE,
    LEFT_NOT_ACTIVE,
    RIGHT_NOT_ACTIVE,
    LEFT_NO_ADOPTED_SNAPSHOT,
    RIGHT_NO_ADOPTED_SNAPSHOT,
}

data class BaziCompatibilityRejection(
    val code: BaziCompatibilityRejectionCode,
    val message: String,
)

sealed interface BaziCompatibilityResult {
    data class Ready(val report: BaziCompatibilityReport) : BaziCompatibilityResult
    data class Rejected(val reasons: List<BaziCompatibilityRejection>) : BaziCompatibilityResult
}

/** 唯一合盘规则入口；结果是传统结构提示，而不是匹配率或现实关系判决。 */
object BaziCompatibilityAnalyzer {
    fun analyze(leftCase: BaziCase, rightCase: BaziCase): BaziCompatibilityResult {
        val rejections = buildList {
            if (leftCase.id == rightCase.id) {
                add(BaziCompatibilityRejection(BaziCompatibilityRejectionCode.SAME_CASE, "双方必须选择两个不同命例。"))
            }
            validateCase(leftCase, BaziCompatibilitySide.LEFT, this)
            validateCase(rightCase, BaziCompatibilitySide.RIGHT, this)
        }
        if (rejections.isNotEmpty()) return BaziCompatibilityResult.Rejected(rejections)

        val leftSnapshot = leftCase.calculationSnapshots.last { it.adopted }
        val rightSnapshot = rightCase.calculationSnapshots.last { it.adopted }
        val left = participant(leftCase, leftSnapshot.id)
        val right = participant(rightCase, rightSnapshot.id)
        val crossSignals = crossSignals(left.pillars, right.pillars)
        val structuralSignals = structuralSignals(left.pillars, right.pillars)
        val signals = (crossSignals + structuralSignals)
            .distinctBy { listOf(it.kind, it.values, it.pillars) }
            .sortedWith(compareBy<BaziCompatibilitySignal>({ !it.kind.isCoordination }, { it.kind.ordinal }, { it.values }))
        val warnings = warnings(leftCase, rightCase, left, right)
        val coordination = signals.filter { it.kind.isCoordination }
        val tension = signals.filterNot { it.kind.isCoordination }
        val dayMasterRelation = dayMasterRelation(left.dayMaster, right.dayMaster)
        val relationshipSummary = relationshipSummary(
            dayMasterRelation = dayMasterRelation,
            coordinationSignals = coordination,
            tensionSignals = tension,
            warnings = warnings,
        )

        return BaziCompatibilityResult.Ready(
            BaziCompatibilityReport(
                ruleVersion = BAZI_COMPATIBILITY_RULE_VERSION,
                left = left,
                right = right,
                dayMasterRelation = dayMasterRelation,
                coordinationSignals = coordination,
                tensionSignals = tension,
                warnings = warnings,
                relationshipSummary = relationshipSummary,
                guidance = relationshipSummary.suggestion,
            ),
        )
    }

    /**
     * 旧版合盘记录只保存了四柱与快照 ID。补齐展示字段时严格读取该记录原来的快照，
     * 不采用命例后来修改后的快照，也不重算既有的关系结论。
     */
    fun hydrateHistoricalReport(
        report: BaziCompatibilityReport,
        leftCase: BaziCase?,
        rightCase: BaziCase?,
    ): BaziCompatibilityReport {
        val left = report.left.hydratePresentationFrom(leftCase)
        val right = report.right.hydratePresentationFrom(rightCase)
        val coordination = report.coordinationSignals.map { signal -> signal.withReadableExplanation() }
        val tension = report.tensionSignals.map { signal -> signal.withReadableExplanation() }
        val relationshipSummary = relationshipSummary(
            dayMasterRelation = report.dayMasterRelation,
            coordinationSignals = coordination,
            tensionSignals = tension,
            warnings = report.warnings,
        )
        return report.copy(
            left = left,
            right = right,
            coordinationSignals = coordination,
            tensionSignals = tension,
            relationshipSummary = relationshipSummary,
            guidance = relationshipSummary.suggestion,
        )
    }

    private fun validateCase(
        case: BaziCase,
        side: BaziCompatibilitySide,
        target: MutableList<BaziCompatibilityRejection>,
    ) {
        val prefix = if (side == BaziCompatibilitySide.LEFT) "男方" else "女方"
        if (case.libraryType != CaseLibraryType.USER) {
            target += BaziCompatibilityRejection(
                if (side == BaziCompatibilitySide.LEFT) {
                    BaziCompatibilityRejectionCode.LEFT_NOT_USER_CASE
                } else {
                    BaziCompatibilityRejectionCode.RIGHT_NOT_USER_CASE
                },
                "${prefix}必须是用户命例，名人资料不参与合盘。",
            )
        }
        if (side == BaziCompatibilitySide.LEFT && case.sexForFortuneDirection != com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection.MAN) {
            target += BaziCompatibilityRejection(
                BaziCompatibilityRejectionCode.LEFT_NOT_MAN,
                "男方位置只能选择男性命例。",
            )
        }
        if (side == BaziCompatibilitySide.RIGHT && case.sexForFortuneDirection != com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection.WOMAN) {
            target += BaziCompatibilityRejection(
                BaziCompatibilityRejectionCode.RIGHT_NOT_WOMAN,
                "女方位置只能选择女性命例。",
            )
        }
        if (case.deletedAt != null) {
            target += BaziCompatibilityRejection(
                if (side == BaziCompatibilitySide.LEFT) {
                    BaziCompatibilityRejectionCode.LEFT_NOT_ACTIVE
                } else {
                    BaziCompatibilityRejectionCode.RIGHT_NOT_ACTIVE
                },
                "${prefix}命例已在回收站，恢复后才能合盘。",
            )
        }
        if (case.calculationSnapshots.none { it.adopted }) {
            target += BaziCompatibilityRejection(
                if (side == BaziCompatibilitySide.LEFT) {
                    BaziCompatibilityRejectionCode.LEFT_NO_ADOPTED_SNAPSHOT
                } else {
                    BaziCompatibilityRejectionCode.RIGHT_NO_ADOPTED_SNAPSHOT
                },
                "${prefix}没有已采用的排盘快照，请先完成排盘。",
            )
        }
    }

    private fun participant(case: BaziCase, snapshotId: String): BaziCompatibilityParticipant {
        val result = case.calculationSnapshots.last { it.id == snapshotId }.result
        val calendar = result.calendarConversion
        val dayMaster = result.basicChartDetails?.dayMaster
            ?.takeIf { it.isNotBlank() }
            ?: result.fourPillars.day.take(1)
        val basicPillarPresentation = result.basicChartDetails?.pillars
            ?.sortedBy { it.position.ordinal }
            ?.map { pillar ->
                BaziCompatibilityPillarPresentation(
                    position = pillar.position,
                    primaryTenGod = pillar.primaryTenGod,
                    heavenStem = pillar.heavenStem,
                    earthBranch = pillar.earthBranch,
                    hiddenStemSummary = pillar.hiddenStems.joinToString(" · ") { hidden ->
                        "${hidden.heavenStem}${hidden.tenGod}"
                    },
                )
            }
            ?.takeIf { it.size == 4 }
            ?: result.fourPillars.compatibilityPillarPresentation(dayMaster)
        return BaziCompatibilityParticipant(
            caseId = case.id,
            alias = case.alias,
            revision = case.revision,
            adoptedSnapshotId = snapshotId,
            pillars = result.fourPillars,
            dayMaster = dayMaster,
            timePrecision = result.normalizedInput.timePrecision,
            calculationProfileId = result.profile.id,
            solarDateTimeText = calendar?.solarDateTime?.compatibilityDisplay()
                ?: result.normalizedInput.calendarInput.compatibilitySolarDisplay(),
            lunarDateTimeText = calendar?.lunarDateTime?.compatibilityDisplay().orEmpty(),
            solarDateValues = calendar?.solarDateTime?.compatibilityDateValues()
                ?: result.normalizedInput.calendarInput.compatibilityDateValues(),
            zodiac = result.basicChartDetails?.zodiac
                ?.takeIf { it.isNotBlank() }
                ?: result.fourPillars.year.lastOrNull()?.compatibilityZodiac().orEmpty(),
            pillarPresentation = basicPillarPresentation,
            decadeFortunes = result.decadeFortunes.take(8).map { decade ->
                decade.compatibilityPresentation(
                    birth = result.solarBirthDateTimeForFortuneDisplay(),
                    dayMaster = dayMaster,
                )
            },
            sourceStructureText = case.compatibilitySourceStructure(),
            structuralProfile = result.structuralProfile
                ?: BaziStructuralProfileAnalyzer.analyze(
                    fourPillars = result.fourPillars,
                    basicChartDetails = result.basicChartDetails,
                ),
            referenceNotes = case.compatibilityReferenceNotes(),
        )
    }

    private fun BaziCompatibilityParticipant.hydratePresentationFrom(case: BaziCase?): BaziCompatibilityParticipant {
        val source = case
            ?.calculationSnapshots
            ?.lastOrNull { snapshot -> snapshot.id == adoptedSnapshotId }
            ?.let { snapshot -> participant(case, snapshot.id) }
        val fallbackPillars = pillars.compatibilityPillarPresentation(
            dayMaster.ifBlank { pillars.day.take(1) },
        )
        val sourcePillars = source?.pillarPresentation ?: fallbackPillars
        return copy(
            solarDateTimeText = solarDateTimeText.ifBlank { source?.solarDateTimeText.orEmpty() },
            lunarDateTimeText = lunarDateTimeText.ifBlank { source?.lunarDateTimeText.orEmpty() },
            solarDateValues = solarDateValues.takeIf { it.size == 4 } ?: source?.solarDateValues.orEmpty(),
            zodiac = zodiac.ifBlank { source?.zodiac ?: pillars.year.lastOrNull()?.compatibilityZodiac().orEmpty() },
            pillarPresentation = pillarPresentation.mergeMissingPresentation(sourcePillars),
            // 同一 adoptedSnapshot 已有精确交运边界时，历史合盘也重用其统一实岁投影。
            decadeFortunes = source?.decadeFortunes?.takeIf { it.isNotEmpty() } ?: decadeFortunes,
            sourceStructureText = sourceStructureText.ifBlank { source?.sourceStructureText.orEmpty() },
            structuralProfile = structuralProfile
                ?: source?.structuralProfile
                ?: BaziStructuralProfileAnalyzer.analyze(pillars),
        )
    }

    private fun BaziCase.compatibilitySourceStructure(): String = fieldEvidence
        .asReversed()
        .firstOrNull { evidence ->
            evidence.fieldKey == WenzhenSourceFidelityContract.FIELD_USER_STRUCTURE
        }
        ?.let { evidence ->
            listOf(evidence.adoptedValue, evidence.normalizedValue)
                .firstNotNullOfOrNull { value -> (value as? TypedFieldValue.Text)?.value?.trim()?.takeIf(String::isNotBlank) }
                ?: evidence.rawText.trim()
        }
        .orEmpty()

    private fun BaziCase.compatibilityReferenceNotes(): List<BaziCompatibilityReferenceNote> = buildList {
        textRecords
            .sortedBy { it.updatedAt }
            .forEach { record ->
                val label = when (record.type) {
                    com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType.OWNER_FEEDBACK -> "命主反馈"
                    com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType.MASTER_COMMENTARY -> "师傅点评"
                    com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType.ANALYSIS -> "既有分析"
                    com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType.NOTE -> "断事笔记"
                }
                add(BaziCompatibilityReferenceNote(label = label, content = record.content.trim()))
            }
        events
            .filter { it.rawText.isNotBlank() }
            .sortedWith(compareBy({ it.year ?: Int.MAX_VALUE }, { it.month ?: Int.MAX_VALUE }, { it.day ?: Int.MAX_VALUE }))
            .forEach { event ->
                val time = listOfNotNull(event.year?.toString(), event.month?.let { "${it}月" }, event.day?.let { "${it}日" }).joinToString(" ")
                val title = listOfNotNull(time.ifBlank { null }, event.title).joinToString(" · ")
                add(BaziCompatibilityReferenceNote(label = "应事记录${title.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", content = event.rawText.trim()))
            }
    }

    private fun BirthCalendarInput.compatibilitySolarDisplay(): String = when (this) {
        is BirthCalendarInput.Solar -> dateTime.compatibilityDisplay()
        is BirthCalendarInput.Lunar -> ""
    }

    private fun BirthCalendarInput.compatibilityDateValues(): List<String> = when (this) {
        is BirthCalendarInput.Solar -> dateTime.compatibilityDateValues()
        is BirthCalendarInput.Lunar -> emptyList()
    }

    private fun com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime.compatibilityDisplay(): String =
        "%04d-%02d-%02d %02d:%02d".format(year, month, day, hour, minute)

    private fun com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime.compatibilityDateValues(): List<String> =
        listOf("${year}年", "${month}月", "${day}日", "${hour}时")

    private fun com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime.compatibilityDisplay(): String =
        "%04d年%s%02d月%02d日 %02d:%02d".format(
            year,
            if (isLeapMonth) "闰" else "",
            month,
            day,
            hour,
            minute,
        )

    private fun DecadeFortune.compatibilityPresentation(
        birth: com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime?,
        dayMaster: String,
    ): BaziCompatibilityDecadePresentation = BaziCompatibilityDecadePresentation(
        name = name,
        ageRange = completedAgeRangeDisplay(birth),
        stemTenGod = compatibilityTenGod(dayMaster, name.firstOrNull()?.toString().orEmpty()),
    )

    private fun dayMasterRelation(leftDayMaster: String, rightDayMaster: String): BaziDayMasterRelation {
        val leftElement = STEM_ELEMENTS[leftDayMaster] ?: "未知"
        val rightElement = STEM_ELEMENTS[rightDayMaster] ?: "未知"
        val description = when {
            leftElement == "未知" || rightElement == "未知" -> "日主五行未完整记录"
            leftElement == rightElement -> "同五行"
            FIVE_ELEMENT_GENERATES[leftElement] == rightElement -> "男方日主生女方"
            FIVE_ELEMENT_GENERATES[rightElement] == leftElement -> "女方日主生男方"
            FIVE_ELEMENT_CONTROLS[leftElement] == rightElement -> "男方日主克女方"
            FIVE_ELEMENT_CONTROLS[rightElement] == leftElement -> "女方日主克男方"
            else -> "日主五行关系待核"
        }
        val explanation = when (description) {
            "同五行" ->
                "双方的核心驱动力同属$leftElement，容易认同彼此的做事方式与需求；优势是默契，风险是把自己的标准直接当成对方的标准。"
            "男方日主生女方" ->
                "传统五行里$leftElement 生 $rightElement，男方更像向女方输出支持的一方；现实中常表现为更主动付出时间、照顾或资源，需避免长期单向透支。"
            "女方日主生男方" ->
                "传统五行里$rightElement 生 $leftElement，女方更像向男方输出支持的一方；现实中常表现为更主动付出时间、照顾或资源，需避免长期单向透支。"
            "男方日主克女方" ->
                "传统五行里$leftElement 克 $rightElement，男方在节奏、规则或决策上更容易形成制约与主导；适度能帮助落地，过强会让女方感到被占据或消耗。"
            "女方日主克男方" ->
                "传统五行里$rightElement 克 $leftElement，女方在节奏、规则或决策上更容易形成制约与主导；适度能帮助落地，过强会让男方感到被占据或消耗。"
            else -> "日主信息不完整，暂不对双方的支持、付出或主导方向作判断。"
        }
        return BaziDayMasterRelation(
            leftElement = leftElement,
            rightElement = rightElement,
            description = description,
            explanation = explanation,
        )
    }

    private fun warnings(
        leftCase: BaziCase,
        rightCase: BaziCase,
        left: BaziCompatibilityParticipant,
        right: BaziCompatibilityParticipant,
    ): List<BaziCompatibilityWarning> = buildList {
        if (left.timePrecision != TimePrecision.EXACT_TO_SECOND && left.timePrecision != TimePrecision.EXACT_TO_MINUTE) {
            add(BaziCompatibilityWarning(BaziCompatibilityWarningCode.LEFT_TIME_NOT_EXACT, "男方出生时刻为${left.timePrecision.display()}，时柱关系仅作参考。"))
        }
        if (right.timePrecision != TimePrecision.EXACT_TO_SECOND && right.timePrecision != TimePrecision.EXACT_TO_MINUTE) {
            add(BaziCompatibilityWarning(BaziCompatibilityWarningCode.RIGHT_TIME_NOT_EXACT, "女方出生时刻为${right.timePrecision.display()}，时柱关系仅作参考。"))
        }
        if (left.calculationProfileId != right.calculationProfileId) {
            add(BaziCompatibilityWarning(BaziCompatibilityWarningCode.CALCULATION_PROFILE_DIFFERENT, "双方排盘口径不同，报告保留各自已采用快照，不比较口径差异。"))
        }
        if (leftCase.calculationSnapshots.last { it.adopted }.result.warnings.isNotEmpty()) {
            add(BaziCompatibilityWarning(BaziCompatibilityWarningCode.LEFT_CALCULATION_WARNING, "男方排盘含已有技术提示，请先核对命例详情。"))
        }
        if (rightCase.calculationSnapshots.last { it.adopted }.result.warnings.isNotEmpty()) {
            add(BaziCompatibilityWarning(BaziCompatibilityWarningCode.RIGHT_CALCULATION_WARNING, "女方排盘含已有技术提示，请先核对命例详情。"))
        }
    }

    private fun crossSignals(left: FourPillars, right: FourPillars): List<BaziCompatibilitySignal> = buildList {
        pillarValues(BaziCompatibilitySide.LEFT, left).forEach { l ->
            pillarValues(BaziCompatibilitySide.RIGHT, right).forEach { r ->
                val stems = listOf(l.value.first().toString(), r.value.first().toString())
                val branches = listOf(l.value.last().toString(), r.value.last().toString())
                pairKind(stems, STEM_FIVE_COMBINATIONS)?.let { kind ->
                    add(signal(kind, stems.joinToString("·"), listOf(l.stem(), r.stem())))
                }
                pairKind(branches, BRANCH_SIX_HARMONIES)?.let { kind ->
                    add(signal(kind, branches.joinToString("·"), listOf(l.branch(), r.branch())))
                }
                pairKind(branches, BRANCH_SIX_CLASHES)?.let { kind ->
                    add(signal(kind, branches.joinToString("·"), listOf(l.branch(), r.branch())))
                }
                pairKind(branches, BRANCH_SIX_HARMS)?.let { kind ->
                    add(signal(kind, branches.joinToString("·"), listOf(l.branch(), r.branch())))
                }
                pairKind(branches, BRANCH_SIX_BREAKS)?.let { kind ->
                    add(signal(kind, branches.joinToString("·"), listOf(l.branch(), r.branch())))
                }
                if (branches.toSet() == setOf("子", "卯")) {
                    add(signal(BaziCompatibilitySignalKind.BRANCH_THREE_PUNISHMENT, branches.joinToString("·"), listOf(l.branch(), r.branch())))
                }
                if (branches[0] == branches[1] && branches[0] in SELF_PUNISHMENT_BRANCHES) {
                    add(signal(BaziCompatibilitySignalKind.BRANCH_SELF_PUNISHMENT, branches[0], listOf(l.branch(), r.branch())))
                }
            }
        }
    }

    private fun structuralSignals(left: FourPillars, right: FourPillars): List<BaziCompatibilitySignal> = buildList {
        val leftBranches = pillarValues(BaziCompatibilitySide.LEFT, left).map { it.branch() }
        val rightBranches = pillarValues(BaziCompatibilitySide.RIGHT, right).map { it.branch() }
        val all = leftBranches + rightBranches
        addStructuralSignals(all, leftBranches, rightBranches, BRANCH_THREE_HARMONIES, BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY, BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY_CANDIDATE, this)
        addStructuralSignals(all, leftBranches, rightBranches, BRANCH_THREE_MEETINGS, BaziCompatibilitySignalKind.BRANCH_THREE_MEETING, BaziCompatibilitySignalKind.BRANCH_THREE_MEETING_CANDIDATE, this)
        BRANCH_THREE_PUNISHMENTS.filter { group -> group.all { it in all.map { ref -> ref.value } } && group.any { it in leftBranches.map { ref -> ref.value } } && group.any { it in rightBranches.map { ref -> ref.value } } }.forEach { group ->
            add(signal(BaziCompatibilitySignalKind.BRANCH_THREE_PUNISHMENT, group.joinToString("·"), all.filter { it.value in group }))
        }
    }

    private fun addStructuralSignals(
        all: List<BaziCompatibilityPillarRef>,
        left: List<BaziCompatibilityPillarRef>,
        right: List<BaziCompatibilityPillarRef>,
        groups: List<Set<String>>,
        completeKind: BaziCompatibilitySignalKind,
        candidateKind: BaziCompatibilitySignalKind,
        target: MutableList<BaziCompatibilitySignal>,
    ) {
        groups.forEach { group ->
            val matching = all.filter { it.value in group }
            val leftParticipates = left.any { it.value in group }
            val rightParticipates = right.any { it.value in group }
            if (!leftParticipates || !rightParticipates || matching.map { it.value }.toSet().size < 2) return@forEach
            val kind = if (group.all { it in matching.map { ref -> ref.value } }) completeKind else candidateKind
            target += signal(kind, group.joinToString("·"), matching)
        }
    }

    private fun signal(
        kind: BaziCompatibilitySignalKind,
        values: String,
        pillars: List<BaziCompatibilityPillarRef>,
    ): BaziCompatibilitySignal = BaziCompatibilitySignal(
        kind = kind,
        values = values,
        pillars = pillars,
        explanation = compatibilitySignalExplanation(kind, pillars),
    )

    private fun BaziCompatibilitySignal.withReadableExplanation(): BaziCompatibilitySignal = copy(
        explanation = compatibilitySignalExplanation(kind, pillars),
    )

    private fun compatibilitySignalExplanation(
        kind: BaziCompatibilitySignalKind,
        pillars: List<BaziCompatibilityPillarRef>,
    ): String {
        val scope = pillars.compatibilityRelationshipScope()
        val meaning = when (kind) {
            BaziCompatibilitySignalKind.STEM_FIVE_COMBINATION ->
                "有利于沟通和协作时找到交集；把共同目标、分工和承诺说具体，更容易形成合力。"
            BaziCompatibilitySignalKind.BRANCH_SIX_HARMONY ->
                "有利于在这些生活议题上配合；把已有默契落实成固定安排，会比只靠感觉更稳定。"
            BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY ->
                "有利于在这些议题上形成共同方向；适合一起推进长期计划，并明确各自承担的部分。"
            BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY_CANDIDATE ->
                "有协同倾向，但条件尚未齐全；需要主动把共同目标和执行方式约定下来。"
            BaziCompatibilitySignalKind.BRANCH_THREE_MEETING ->
                "有利于家庭环境、生活圈或共同规划形成联动；先统一优先级，能减少彼此拉扯。"
            BaziCompatibilitySignalKind.BRANCH_THREE_MEETING_CANDIDATE ->
                "在共同生活与外部环境上有靠拢空间；需要通过持续沟通把想法变成一致行动。"
            BaziCompatibilitySignalKind.BRANCH_SIX_CLASH ->
                "需要注意这些议题上容易出现不同节奏或偏好；有分歧时先说清需求与边界，再讨论解决方式。"
            BaziCompatibilitySignalKind.BRANCH_SIX_HARM ->
                "需要注意期待没有说透、彼此感受没被接住；不要让对方猜，直接表达真实需求。"
            BaziCompatibilitySignalKind.BRANCH_SIX_BREAK ->
                "需要注意共同安排容易被小问题反复打断；对金钱、时间和承诺设定可执行的规则，并定期复盘。"
            BaziCompatibilitySignalKind.BRANCH_THREE_PUNISHMENT ->
                "需要注意压力大时容易陷入较劲、挑剔或反复争论；约定暂停机制，避免在情绪高点处理关键决定。"
            BaziCompatibilitySignalKind.BRANCH_SELF_PUNISHMENT ->
                "需要注意容易把各自的焦虑或固执带进同一议题；先分辨是现实问题还是情绪反应，再回应对方。"
        }
        return "$scope${kind.title}，$meaning"
    }

    private fun List<BaziCompatibilityPillarRef>.compatibilityRelationshipScope(): String {
        val bySide = groupBy(BaziCompatibilityPillarRef::side)
        return listOfNotNull(
            bySide[BaziCompatibilitySide.LEFT]
                ?.distinctBy(BaziCompatibilityPillarRef::position)
                ?.sortedBy(BaziCompatibilityPillarRef::position)
                ?.joinToString("、") { ref -> "男方${ref.position.compatibilityRelationshipTopic()}" },
            bySide[BaziCompatibilitySide.RIGHT]
                ?.distinctBy(BaziCompatibilityPillarRef::position)
                ?.sortedBy(BaziCompatibilityPillarRef::position)
                ?.joinToString("、") { ref -> "女方${ref.position.compatibilityRelationshipTopic()}" },
        ).joinToString("与") + "之间形成"
    }

    private fun PillarPosition.compatibilityRelationshipTopic(): String = when (this) {
        PillarPosition.YEAR -> "年柱（成长背景、家庭根基）"
        PillarPosition.MONTH -> "月柱（日常节奏、工作分工）"
        PillarPosition.DAY -> "日柱（伴侣相处、亲密边界）"
        PillarPosition.HOUR -> "时柱（未来计划、私人生活）"
    }

    private fun pairKind(
        values: List<String>,
        rule: Set<Set<String>>,
    ): BaziCompatibilitySignalKind? {
        if (values.toSet() !in rule) return null
        // 关系表彼此不必互斥（例如寅亥可同时落入不同传统关系）；调用方逐表取证。
        return when {
            rule === STEM_FIVE_COMBINATIONS -> BaziCompatibilitySignalKind.STEM_FIVE_COMBINATION
            rule === BRANCH_SIX_HARMONIES -> BaziCompatibilitySignalKind.BRANCH_SIX_HARMONY
            rule === BRANCH_SIX_CLASHES -> BaziCompatibilitySignalKind.BRANCH_SIX_CLASH
            rule === BRANCH_SIX_HARMS -> BaziCompatibilitySignalKind.BRANCH_SIX_HARM
            rule === BRANCH_SIX_BREAKS -> BaziCompatibilitySignalKind.BRANCH_SIX_BREAK
            else -> null
        }
    }

    private fun pillarValues(side: BaziCompatibilitySide, pillars: FourPillars): List<BaziCompatibilityPillarRef> = listOf(
        BaziCompatibilityPillarRef(side, PillarPosition.YEAR, pillars.year),
        BaziCompatibilityPillarRef(side, PillarPosition.MONTH, pillars.month),
        BaziCompatibilityPillarRef(side, PillarPosition.DAY, pillars.day),
        BaziCompatibilityPillarRef(side, PillarPosition.HOUR, pillars.hour),
    )

    private fun BaziCompatibilityPillarRef.stem() = copy(value = value.first().toString())
    private fun BaziCompatibilityPillarRef.branch() = copy(value = value.last().toString())

    private fun relationshipSummary(
        dayMasterRelation: BaziDayMasterRelation,
        coordinationSignals: List<BaziCompatibilitySignal>,
        tensionSignals: List<BaziCompatibilitySignal>,
        warnings: List<BaziCompatibilityWarning>,
    ): BaziCompatibilityRelationshipSummary {
        val strongestCoordination = coordinationSignals.minByOrNull(::summarySignalPriority)
        val strongestTension = tensionSignals.minByOrNull(::summarySignalPriority)
        val coordinationArea = strongestCoordination?.summaryRelationshipArea()
        val tensionArea = strongestTension?.summaryRelationshipArea()
        val judgement = buildString {
            append(dayMasterRelation.summaryRelationshipJudgement())
            coordinationArea?.let { append(" 同时，两人在${it}上更容易找到彼此配合的方式。") }
            tensionArea?.let { append(" 但${it}也是更需要主动磨合的地方。") }
        }
        val advantage = coordinationArea?.let { area ->
            "${area}是这对关系较容易形成合力的部分。把目标、责任和完成时间讲清楚，已有的配合感更容易变成稳定行动。"
        } ?: "当前资料没有显示特别突出的自然配合点；稳定感更需要靠双方把期待说具体、把承诺落实到日常安排。"
        val caution = tensionArea?.let { area ->
            "${area}最容易因各自习惯不同而反复拉扯。出现分歧时，先说清各自真正需要什么，再讨论谁来做、何时做，避免只争对错。"
        } ?: "当前资料没有显示特别集中的高摩擦点；但这不等于不需要沟通，重要决定仍应提前确认双方是否真的同意。"
        val suggestion = when (tensionArea ?: coordinationArea) {
            "亲密相处与个人边界" -> "建议：把陪伴频率、独处需求和冲突后的处理方式提前约定，避免把沉默误解成不在意。"
            "日常安排与分工" -> "建议：把家务、作息、金钱使用和临时变动的处理方式具体化，减少靠猜测分工。"
            "与对方家庭相处" -> "建议：涉及父母、节日和家庭责任时先站在同一边，再共同决定怎样回应外部意见。"
            "家庭与长辈互动" -> "建议：遇到长辈意见或家庭安排时，先确认两人的底线和优先级，不让外部期待替代共同决定。"
            "未来安排与私人空间" -> "建议：把长期计划、个人时间和阶段目标分别说清，定期复盘是否仍与彼此期待一致。"
            else -> "建议：把最在意的目标、底线和可承担的责任说具体，并在重要决定前留出一次共同确认。"
        }
        return BaziCompatibilityRelationshipSummary(
            relationshipJudgement = judgement,
            advantage = advantage,
            caution = caution,
            suggestion = suggestion,
            dataReminder = warnings.summaryDataReminder(),
        )
    }

    private fun BaziDayMasterRelation.summaryRelationshipJudgement(): String = when (description) {
        "同五行" -> "两人的做事和表达习惯比较接近，理解彼此会更快；发生分歧时，也容易各自坚持自己的标准。"
        "男方日主生女方" -> "男方更容易主动承担、投入时间或照顾关系；女方需要及时表达真实需求，避免长期由一方单向付出。"
        "女方日主生男方" -> "女方更容易主动承担、投入时间或照顾关系；男方需要及时表达真实需求，避免长期由一方单向付出。"
        "男方日主克女方" -> "男方更容易推动规则和决定落地；执行会更有效率，但女方也可能感到被安排，重要选择应保留共同决定的空间。"
        "女方日主克男方" -> "女方更容易推动规则和决定落地；执行会更有效率，但男方也可能感到被安排，重要选择应保留共同决定的空间。"
        else -> "双方的核心资料不足，暂不对谁更主动投入或更容易主导作判断。"
    }

    private fun BaziCompatibilitySignal.summaryRelationshipArea(): String {
        val leftPositions = pillars.filter { it.side == BaziCompatibilitySide.LEFT }.map { it.position }.toSet()
        val rightPositions = pillars.filter { it.side == BaziCompatibilitySide.RIGHT }.map { it.position }.toSet()
        return when {
            PillarPosition.DAY in leftPositions && PillarPosition.DAY in rightPositions -> "亲密相处与个人边界"
            (PillarPosition.DAY in leftPositions && PillarPosition.YEAR in rightPositions) ||
                (PillarPosition.YEAR in leftPositions && PillarPosition.DAY in rightPositions) -> "与对方家庭相处"
            PillarPosition.MONTH in leftPositions || PillarPosition.MONTH in rightPositions -> "日常安排与分工"
            PillarPosition.YEAR in leftPositions || PillarPosition.YEAR in rightPositions -> "家庭与长辈互动"
            PillarPosition.HOUR in leftPositions || PillarPosition.HOUR in rightPositions -> "未来安排与私人空间"
            else -> "共同生活"
        }
    }

    private fun summarySignalPriority(signal: BaziCompatibilitySignal): Int = signal.pillars.fold(0) { total, ref ->
        total + when (ref.position) {
            PillarPosition.DAY -> 0
            PillarPosition.MONTH -> 1
            PillarPosition.YEAR -> 2
            PillarPosition.HOUR -> 3
        }
    }

    private fun List<BaziCompatibilityWarning>.summaryDataReminder(): String = when {
        isEmpty() -> "资料提醒：双方出生资料和计算口径可用于本次静态对照；它不替代现实相处中的沟通与选择。"
        any { it.code == BaziCompatibilityWarningCode.LEFT_TIME_NOT_EXACT || it.code == BaziCompatibilityWarningCode.RIGHT_TIME_NOT_EXACT } ->
            "资料提醒：至少一方的出生时间未精确到分钟，涉及私人习惯和长期安排的提示只能作宽泛参考。"
        any { it.code == BaziCompatibilityWarningCode.CALCULATION_PROFILE_DIFFERENT } ->
            "资料提醒：双方资料采用了不同计算口径，细节差异不应直接当作现实矛盾。"
        else -> "资料提醒：至少一方资料带有待核对提示，请先确认原始出生信息，再参考本页关系建议。"
    }
}

private fun List<BaziCompatibilityPillarPresentation>.mergeMissingPresentation(
    fallback: List<BaziCompatibilityPillarPresentation>,
): List<BaziCompatibilityPillarPresentation> = fallback.mapIndexed { index, fallbackPillar ->
    val existing = getOrNull(index)?.takeIf { it.position == fallbackPillar.position }
    if (existing == null) {
        fallbackPillar
    } else {
        existing.copy(
            primaryTenGod = existing.primaryTenGod.ifBlank { fallbackPillar.primaryTenGod },
            heavenStem = existing.heavenStem.ifBlank { fallbackPillar.heavenStem },
            earthBranch = existing.earthBranch.ifBlank { fallbackPillar.earthBranch },
            hiddenStemSummary = existing.hiddenStemSummary.ifBlank { fallbackPillar.hiddenStemSummary },
        )
    }
}

private fun FourPillars.compatibilityPillarPresentation(dayMaster: String): List<BaziCompatibilityPillarPresentation> = listOf(
    PillarPosition.YEAR to year,
    PillarPosition.MONTH to month,
    PillarPosition.DAY to day,
    PillarPosition.HOUR to hour,
).map { (position, pillar) ->
    val heavenStem = pillar.firstOrNull()?.toString().orEmpty()
    val earthBranch = pillar.lastOrNull()?.toString().orEmpty()
    BaziCompatibilityPillarPresentation(
        position = position,
        primaryTenGod = compatibilityTenGod(dayMaster, heavenStem),
        heavenStem = heavenStem,
        earthBranch = earthBranch,
        hiddenStemSummary = earthBranch.compatibilityHiddenStems()
            .joinToString(" · ") { hidden -> "$hidden${compatibilityTenGod(dayMaster, hidden)}" },
    )
}

private fun compatibilityTenGod(dayMaster: String, targetStem: String): String {
    val dayElement = STEM_ELEMENTS[dayMaster] ?: return ""
    val targetElement = STEM_ELEMENTS[targetStem] ?: return ""
    val samePolarity = STEM_POLARITIES[dayMaster] == STEM_POLARITIES[targetStem]
    return when {
        dayElement == targetElement -> if (samePolarity) "比肩" else "劫财"
        FIVE_ELEMENT_GENERATES[dayElement] == targetElement -> if (samePolarity) "食神" else "伤官"
        FIVE_ELEMENT_GENERATES[targetElement] == dayElement -> if (samePolarity) "偏印" else "正印"
        FIVE_ELEMENT_CONTROLS[dayElement] == targetElement -> if (samePolarity) "偏财" else "正财"
        FIVE_ELEMENT_CONTROLS[targetElement] == dayElement -> if (samePolarity) "七杀" else "正官"
        else -> ""
    }
}

private fun String.compatibilityHiddenStems(): List<String> = COMPATIBILITY_HIDDEN_STEMS[this].orEmpty()

private fun Char.compatibilityZodiac(): String = COMPATIBILITY_ZODIACS[this.toString()].orEmpty()

private val STEM_FIVE_COMBINATIONS = setOf(
    setOf("甲", "己"), setOf("乙", "庚"), setOf("丙", "辛"), setOf("丁", "壬"), setOf("戊", "癸"),
)
private val BRANCH_SIX_HARMONIES = setOf(
    setOf("子", "丑"), setOf("寅", "亥"), setOf("卯", "戌"), setOf("辰", "酉"), setOf("巳", "申"), setOf("午", "未"),
)
private val BRANCH_SIX_CLASHES = setOf(
    setOf("子", "午"), setOf("丑", "未"), setOf("寅", "申"), setOf("卯", "酉"), setOf("辰", "戌"), setOf("巳", "亥"),
)
private val BRANCH_SIX_HARMS = setOf(
    setOf("子", "未"), setOf("丑", "午"), setOf("寅", "巳"), setOf("卯", "辰"), setOf("申", "亥"), setOf("酉", "戌"),
)
private val BRANCH_SIX_BREAKS = setOf(
    setOf("子", "酉"), setOf("丑", "辰"), setOf("寅", "亥"), setOf("卯", "午"), setOf("巳", "申"), setOf("未", "戌"),
)
private val BRANCH_THREE_HARMONIES = listOf(
    setOf("申", "子", "辰"), setOf("亥", "卯", "未"), setOf("寅", "午", "戌"), setOf("巳", "酉", "丑"),
)
private val BRANCH_THREE_MEETINGS = listOf(
    setOf("寅", "卯", "辰"), setOf("巳", "午", "未"), setOf("申", "酉", "戌"), setOf("亥", "子", "丑"),
)
private val BRANCH_THREE_PUNISHMENTS = listOf(setOf("寅", "巳", "申"), setOf("丑", "未", "戌"))
private val SELF_PUNISHMENT_BRANCHES = setOf("辰", "午", "酉", "亥")
private val STEM_ELEMENTS = mapOf(
    "甲" to "木", "乙" to "木", "丙" to "火", "丁" to "火", "戊" to "土",
    "己" to "土", "庚" to "金", "辛" to "金", "壬" to "水", "癸" to "水",
)
private val STEM_POLARITIES = mapOf(
    "甲" to true, "乙" to false, "丙" to true, "丁" to false, "戊" to true,
    "己" to false, "庚" to true, "辛" to false, "壬" to true, "癸" to false,
)
private val COMPATIBILITY_HIDDEN_STEMS = mapOf(
    "子" to listOf("癸"), "丑" to listOf("己", "癸", "辛"), "寅" to listOf("甲", "丙", "戊"),
    "卯" to listOf("乙"), "辰" to listOf("戊", "乙", "癸"), "巳" to listOf("丙", "戊", "庚"),
    "午" to listOf("丁", "己"), "未" to listOf("己", "丁", "乙"), "申" to listOf("庚", "壬", "戊"),
    "酉" to listOf("辛"), "戌" to listOf("戊", "辛", "丁"), "亥" to listOf("壬", "甲"),
)
private val COMPATIBILITY_ZODIACS = mapOf(
    "子" to "鼠", "丑" to "牛", "寅" to "虎", "卯" to "兔", "辰" to "龙", "巳" to "蛇",
    "午" to "马", "未" to "羊", "申" to "猴", "酉" to "鸡", "戌" to "狗", "亥" to "猪",
)
private val FIVE_ELEMENT_GENERATES = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
private val FIVE_ELEMENT_CONTROLS = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")

private fun TimePrecision.display(): String = when (this) {
    TimePrecision.EXACT_TO_SECOND -> "精确到秒"
    TimePrecision.EXACT_TO_MINUTE -> "精确到分"
    TimePrecision.APPROXIMATE -> "约略时间"
    TimePrecision.HOUR_ONLY -> "仅知小时"
    TimePrecision.DOUBLE_HOUR_ONLY -> "仅知时辰"
    TimePrecision.UNKNOWN -> "时刻未知"
}
