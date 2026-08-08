package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode

data class ProfessionalFortunePosition(
    val position: FortunePosition,
    val flowPillars: FourPillars,
    val pillarColumns: List<ProfessionalPillarColumn> = emptyList(),
    val decadeTimeline: List<ProfessionalTimelineItem> = emptyList(),
    val annualTimeline: List<ProfessionalTimelineItem> = emptyList(),
    val monthlyTimeline: List<ProfessionalTimelineItem> = emptyList(),
    val dailyTimeline: List<ProfessionalTimelineItem> = emptyList(),
    val hourlyTimeline: List<ProfessionalTimelineItem> = emptyList(),
    val interactionGroups: List<ProfessionalTextGroup> = emptyList(),
    val shenShaGroups: List<ProfessionalTextGroup> = emptyList(),
    /** 观察日已经完整度过的生日数量；不是虚岁，也不是公历年份直接相减。 */
    val completedAge: Int = 0,
    val selectedDateDetail: String = "",
    val previousSolarTerm: SolarTermPoint,
    val nextSolarTerm: SolarTermPoint,
    val observationTimeMode: SolarTimeMode,
    val profileId: String,
    val ruleVersion: String,
    val detailRuleVersion: String = "professional-detail-v1",
)

data class ProfessionalPillarColumn(
    val key: String,
    val label: String,
    val pillar: String,
    val stemTenGod: String,
    val heavenStemElement: String,
    val earthBranchElement: String,
    val hiddenStems: List<ProfessionalHiddenStem>,
)

data class ProfessionalHiddenStem(
    val heavenStem: String,
    val element: String,
    val tenGod: String,
)

data class ProfessionalTimelineItem(
    val key: String,
    val label: String,
    val subtitle: String,
    val observedAt: CivilDateTime,
    val pillar: String,
    val stemTenGod: String,
    val heavenStemElement: String,
    val earthBranchElement: String,
    /** 时间轴在地支下完整展示藏干；十神以紧凑简称呈现。 */
    val hiddenStems: List<ProfessionalHiddenStem> = emptyList(),
    val selected: Boolean,
)

data class ProfessionalTextGroup(
    val title: String,
    val lines: List<String>,
)

fun interface ProfessionalFortuneResolver {
    fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): ProfessionalFortunePosition
}
