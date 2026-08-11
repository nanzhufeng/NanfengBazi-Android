package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import kotlin.math.abs

enum class CaseComparisonOutcome {
    SAME,
    DIFFERENT,
    MISSING,
}

data class CaseComparisonRow(
    val label: String,
    val leftValue: String,
    val rightValue: String,
    val outcome: CaseComparisonOutcome,
)

data class CaseComparisonSection(
    val title: String,
    val rows: List<CaseComparisonRow>,
)

data class CaseComparisonReport(
    val leftCaseId: String,
    val leftAlias: String,
    val rightCaseId: String,
    val rightAlias: String,
    val sections: List<CaseComparisonSection>,
) {
    val sameCount: Int = sections.sumOf { section ->
        section.rows.count { it.outcome == CaseComparisonOutcome.SAME }
    }
    val differentCount: Int = sections.sumOf { section ->
        section.rows.count { it.outcome == CaseComparisonOutcome.DIFFERENT }
    }
    val missingCount: Int = sections.sumOf { section ->
        section.rows.count { it.outcome == CaseComparisonOutcome.MISSING }
    }
}

object CaseComparisonEngine {
    fun compare(left: BaziCase, right: BaziCase): CaseComparisonReport {
        require(left.id != right.id) { "命例对比必须选择两个不同命例" }
        val leftSnapshot = left.calculationSnapshots.asReversed().firstOrNull { it.adopted }
        val rightSnapshot = right.calculationSnapshots.asReversed().firstOrNull { it.adopted }
        val leftResult = leftSnapshot?.result
        val rightResult = rightSnapshot?.result
        val leftInput = leftResult?.normalizedInput ?: left.birthInput
        val rightInput = rightResult?.normalizedInput ?: right.birthInput

        fun row(
            label: String,
            leftValue: String?,
            rightValue: String?,
        ): CaseComparisonRow {
            val normalizedLeft = leftValue?.takeIf { it.isNotBlank() }
            val normalizedRight = rightValue?.takeIf { it.isNotBlank() }
            val outcome = when {
                normalizedLeft == null || normalizedRight == null -> CaseComparisonOutcome.MISSING
                normalizedLeft == normalizedRight -> CaseComparisonOutcome.SAME
                else -> CaseComparisonOutcome.DIFFERENT
            }
            return CaseComparisonRow(
                label = label,
                leftValue = normalizedLeft ?: "未记录",
                rightValue = normalizedRight ?: "未记录",
                outcome = outcome,
            )
        }

        val leftPillars = leftResult?.fourPillars
        val rightPillars = rightResult?.fourPillars
        val leftBasic = leftResult?.basicChartDetails
        val rightBasic = rightResult?.basicChartDetails
        val leftFortune = leftResult?.fortuneStart
        val rightFortune = rightResult?.fortuneStart

        return CaseComparisonReport(
            leftCaseId = left.id,
            leftAlias = left.alias,
            rightCaseId = right.id,
            rightAlias = right.alias,
            sections = listOf(
                CaseComparisonSection(
                    title = "出生与历法证据",
                    rows = listOf(
                        row(
                            "性别",
                            left.sexForFortuneDirection.display(),
                            right.sexForFortuneDirection.display(),
                        ),
                        row(
                            "来源",
                            left.sourceType.display(),
                            right.sourceType.display(),
                        ),
                        row(
                            "标准公历",
                            leftResult?.calendarConversion?.solarDateTime?.display(),
                            rightResult?.calendarConversion?.solarDateTime?.display(),
                        ),
                        row(
                            "标准农历",
                            leftResult?.calendarConversion?.lunarDateTime?.display(),
                            rightResult?.calendarConversion?.lunarDateTime?.display(),
                        ),
                        row("出生地区", leftInput.locationName, rightInput.locationName),
                        row("IANA 时区", leftInput.timeZoneId, rightInput.timeZoneId),
                        row(
                            "UTC offset",
                            leftInput.resolvedUtcOffsetSeconds?.displayUtcOffset(),
                            rightInput.resolvedUtcOffsetSeconds?.displayUtcOffset(),
                        ),
                        row(
                            "真太阳时",
                            leftResult?.profile?.solarTimeMode?.display(),
                            rightResult?.profile?.solarTimeMode?.display(),
                        ),
                    ),
                ),
                CaseComparisonSection(
                    title = "基础命盘",
                    rows = listOf(
                        row("年柱", leftPillars?.year, rightPillars?.year),
                        row("月柱", leftPillars?.month, rightPillars?.month),
                        row("日柱", leftPillars?.day, rightPillars?.day),
                        row("时柱", leftPillars?.hour, rightPillars?.hour),
                        row("生肖", leftBasic?.zodiac, rightBasic?.zodiac),
                        row("星座", leftBasic?.westernZodiac, rightBasic?.westernZodiac),
                        row("日主", leftBasic?.dayMaster, rightBasic?.dayMaster),
                        row("胎元", leftResult?.fetalOrigin, rightResult?.fetalOrigin),
                        row("胎息", leftResult?.fetalBreath, rightResult?.fetalBreath),
                        row("命宫", leftResult?.ownSign, rightResult?.ownSign),
                        row("身宫", leftResult?.bodySign, rightResult?.bodySign),
                    ),
                ),
                CaseComparisonSection(
                    title = "起运与大运",
                    rows = listOf(
                        row(
                            "起运方向",
                            leftFortune?.direction?.display(),
                            rightFortune?.direction?.display(),
                        ),
                        row(
                            "交运时刻",
                            leftFortune?.endAt?.display(),
                            rightFortune?.endAt?.display(),
                        ),
                        row(
                            "起运年龄",
                            leftFortune?.displayAge(),
                            rightFortune?.displayAge(),
                        ),
                        row(
                            "120 年大运",
                            leftResult?.decadeFortunes?.take(12)?.joinToString(" · ") {
                                "${it.name} ${it.startAge}-${it.endAge}岁"
                            },
                            rightResult?.decadeFortunes?.take(12)?.joinToString(" · ") {
                                "${it.name} ${it.startAge}-${it.endAge}岁"
                            },
                        ),
                    ),
                ),
                CaseComparisonSection(
                    title = "计算档案",
                    rows = listOf(
                        row(
                            "计算配置",
                            leftResult?.profile?.id,
                            rightResult?.profile?.id,
                        ),
                        row(
                            "引擎版本",
                            leftResult?.evidence?.let { "${it.engineName} ${it.engineVersion}" },
                            rightResult?.evidence?.let { "${it.engineName} ${it.engineVersion}" },
                        ),
                        row(
                            "规则版本",
                            leftResult?.evidence?.ruleVersion,
                            rightResult?.evidence?.ruleVersion,
                        ),
                    ),
                ),
                CaseComparisonSection(
                    title = "研究资料",
                    rows = listOf(
                        row(
                            "分组",
                            left.groups.map { it.name }.sorted().joinToString("、")
                                .ifBlank { "未分组" },
                            right.groups.map { it.name }.sorted().joinToString("、")
                                .ifBlank { "未分组" },
                        ),
                        row(
                            "标签",
                            left.tags.map { it.name }.sorted().joinToString("、")
                                .ifBlank { "无标签" },
                            right.tags.map { it.name }.sorted().joinToString("、")
                                .ifBlank { "无标签" },
                        ),
                        row("文本记录数", left.textRecords.size.toString(), right.textRecords.size.toString()),
                        row("关键事件数", left.events.size.toString(), right.events.size.toString()),
                        row("来源附件数", left.attachments.size.toString(), right.attachments.size.toString()),
                        row("命例修订号", left.revision.toString(), right.revision.toString()),
                    ),
                ),
            ),
        )
    }
}

private fun CivilDateTime.display(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun LunarDateTime.display(): String =
    "%04d年%s%02d月%02d日 %02d:%02d:%02d".format(
        year,
        if (isLeapMonth) "闰" else "",
        month,
        day,
        hour,
        minute,
        second,
    )

private fun Int.displayUtcOffset(): String {
    val sign = if (this >= 0) "+" else "-"
    val absolute = abs(this)
    return "UTC$sign%02d:%02d".format(absolute / 3_600, absolute % 3_600 / 60)
}

private fun SexForFortuneDirection.display(): String = when (this) {
    SexForFortuneDirection.MAN -> "男"
    SexForFortuneDirection.WOMAN -> "女"
}

private fun CaseSourceType.display(): String = when (this) {
    CaseSourceType.MANUAL -> "手工录入"
    CaseSourceType.CASE_COPY -> "命例副本"
    CaseSourceType.WENZHEN_SCREENSHOT -> "问真截图"
    CaseSourceType.WENZHEN_WEB_IMPORT -> "问真网页导入"
    CaseSourceType.BACKUP_RESTORE -> "备份恢复"
}

private fun SolarTimeMode.display(): String = when (this) {
    SolarTimeMode.CIVIL_TIME -> "民用时"
    SolarTimeMode.TRUE_SOLAR_TIME -> "真太阳时"
}

private fun FortuneDirection.display(): String = when (this) {
    FortuneDirection.FORWARD -> "顺排"
    FortuneDirection.BACKWARD -> "逆排"
}

private fun com.nanzhufeng.nanfengbazi.domain.model.FortuneStart.displayAge(): String =
    "${years}年${months}月${days}日${hours}时${minutes}分"
