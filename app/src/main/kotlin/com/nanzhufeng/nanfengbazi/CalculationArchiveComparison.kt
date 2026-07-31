package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CalendarConversionResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.TrueSolarTimeEvidence
import java.util.Locale

internal data class CalculationOutcomeChange(
    val label: String,
    val previousValue: String,
    val currentValue: String,
)

internal data class CalculationArchiveComparison(
    val inputChanged: Boolean,
    val ruleConfigurationChanged: Boolean,
    val profileIdChanged: Boolean,
    val engineVersionChanged: Boolean,
    val ruleVersionChanged: Boolean,
    val outcomeChanges: List<CalculationOutcomeChange>,
) {
    val versionUpgradeComparable: Boolean
        get() = !inputChanged &&
            !ruleConfigurationChanged &&
            (engineVersionChanged || ruleVersionChanged)

    val outcomeConsistent: Boolean
        get() = outcomeChanges.isEmpty()

    val attributionSummary: String
        get() = when {
            inputChanged ->
                "出生输入已变化，结果差异不能归因于引擎升级。"
            ruleConfigurationChanged ->
                "计算口径已变化，结果差异必须按新口径解释。"
            versionUpgradeComparable ->
                "输入与计算口径一致，可用于核对引擎或规则升级。"
            profileIdChanged ->
                "计算档案标识已变化，但输入与计算口径一致。"
            else ->
                "同一输入与同一版本重新计算，可核对结果稳定性。"
        }
}

internal fun compareCalculationSnapshots(
    previous: CaseCalculationSnapshot,
    current: CaseCalculationSnapshot,
): CalculationArchiveComparison {
    val previousResult = previous.result
    val currentResult = current.result
    return CalculationArchiveComparison(
        inputChanged = previousResult.normalizedInput != currentResult.normalizedInput,
        ruleConfigurationChanged =
            !previousResult.profile.hasSameRuleConfiguration(currentResult.profile),
        profileIdChanged = previousResult.profile.id != currentResult.profile.id,
        engineVersionChanged =
            previousResult.evidence.engineVersion != currentResult.evidence.engineVersion,
        ruleVersionChanged =
            previousResult.evidence.ruleVersion != currentResult.evidence.ruleVersion,
        outcomeChanges = compareOutcomes(previousResult, currentResult),
    )
}

private fun CalculationProfile.hasSameRuleConfiguration(other: CalculationProfile): Boolean =
    yearBoundaryRule == other.yearBoundaryRule &&
        monthBoundaryRule == other.monthBoundaryRule &&
        ratHourRule == other.ratHourRule &&
        solarTimeMode == other.solarTimeMode &&
        trueSolarTimeApplicationRule == other.trueSolarTimeApplicationRule &&
        luckStartRule == other.luckStartRule

private fun compareOutcomes(
    previous: CalculationResult,
    current: CalculationResult,
): List<CalculationOutcomeChange> = buildList {
    addChange("年柱", previous.fourPillars.year, current.fourPillars.year)
    addChange("月柱", previous.fourPillars.month, current.fourPillars.month)
    addChange("日柱", previous.fourPillars.day, current.fourPillars.day)
    addChange("时柱", previous.fourPillars.hour, current.fourPillars.hour)
    addChange("胎元", previous.fetalOrigin, current.fetalOrigin)
    addChange("胎息", previous.fetalBreath, current.fetalBreath)
    addChange("命宫", previous.ownSign, current.ownSign)
    addChange("身宫", previous.bodySign, current.bodySign)
    addChange(
        label = "起运",
        previousRaw = previous.fortuneStart,
        currentRaw = current.fortuneStart,
        previousDisplay = previous.fortuneStart.archiveDisplay(),
        currentDisplay = current.fortuneStart.archiveDisplay(),
    )
    addChange(
        label = "大运序列",
        previousRaw = previous.decadeFortunes,
        currentRaw = current.decadeFortunes,
        previousDisplay = previous.decadeFortunes.archiveDisplay(),
        currentDisplay = current.decadeFortunes.archiveDisplay(),
    )
    addChange(
        label = "流年序列",
        previousRaw = previous.annualFortunes,
        currentRaw = current.annualFortunes,
        previousDisplay = previous.annualFortunes.let { fortunes ->
            if (fortunes.isEmpty()) {
                "未记录"
            } else {
                "${fortunes.size}年：${fortunes.first().calendarYear}" +
                    "–${fortunes.last().calendarYear}"
            }
        },
        currentDisplay = current.annualFortunes.let { fortunes ->
            if (fortunes.isEmpty()) {
                "未记录"
            } else {
                "${fortunes.size}年：${fortunes.first().calendarYear}" +
                    "–${fortunes.last().calendarYear}"
            }
        },
    )
    addChange(
        label = "公农历换算",
        previousRaw = previous.calendarConversion,
        currentRaw = current.calendarConversion,
        previousDisplay = previous.calendarConversion.archiveDisplay(),
        currentDisplay = current.calendarConversion.archiveDisplay(),
    )
    addChange(
        label = "真太阳时",
        previousRaw = previous.trueSolarTimeEvidence,
        currentRaw = current.trueSolarTimeEvidence,
        previousDisplay = previous.trueSolarTimeEvidence.archiveDisplay(),
        currentDisplay = current.trueSolarTimeEvidence.archiveDisplay(),
    )
    addChange(
        label = "基本排盘明细",
        previousRaw = previous.basicChartDetails,
        currentRaw = current.basicChartDetails,
        previousDisplay = previous.basicChartDetails.archiveDisplay(),
        currentDisplay = current.basicChartDetails.archiveDisplay(),
    )
}

private fun MutableList<CalculationOutcomeChange>.addChange(
    label: String,
    previousValue: String,
    currentValue: String,
) {
    addChange(label, previousValue, currentValue, previousValue, currentValue)
}

private fun <T> MutableList<CalculationOutcomeChange>.addChange(
    label: String,
    previousRaw: T,
    currentRaw: T,
    previousDisplay: String,
    currentDisplay: String,
) {
    if (previousRaw != currentRaw) {
        add(
            CalculationOutcomeChange(
                label = label,
                previousValue = previousDisplay,
                currentValue = currentDisplay,
            ),
        )
    }
}

private fun FortuneStart.archiveDisplay(): String =
    "${if (direction == FortuneDirection.FORWARD) "顺排" else "逆排"} · " +
        "${startAt.archiveDisplay()}起运"

private fun List<DecadeFortune>.archiveDisplay(): String =
    if (isEmpty()) {
        "未记录"
    } else {
        joinToString("、") { fortune ->
            "${fortune.name}(${fortune.startYear}–${fortune.endYear})"
        }
    }

private fun CalendarConversionResult?.archiveDisplay(): String =
    this?.let { conversion ->
        val lunar = conversion.lunarDateTime
        "${conversion.solarDateTime.archiveDisplay()} / " +
            "农历${lunar.year}-${if (lunar.isLeapMonth) "闰" else ""}" +
            "${lunar.month}-${lunar.day}"
    } ?: "未记录"

private fun TrueSolarTimeEvidence?.archiveDisplay(): String =
    this?.let { evidence ->
        "${evidence.trueSolarDateTime.archiveDisplay()} " +
            "(${evidence.totalCorrectionSeconds.archiveSignedSeconds()})"
    } ?: "未启用"

private fun BasicChartDetails?.archiveDisplay(): String =
    this?.let { details ->
        "生肖${details.zodiac} · ${details.westernZodiac} · 日主${details.dayMaster} · " +
            "${details.previousSolarTerm.name}—${details.nextSolarTerm.name}"
    } ?: "旧快照未记录"

private fun CivilDateTime.archiveDisplay(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(
        Locale.ROOT,
        year,
        month,
        day,
        hour,
        minute,
        second,
    )

private fun Int.archiveSignedSeconds(): String =
    if (this >= 0) "+${this}秒" else "${this}秒"
