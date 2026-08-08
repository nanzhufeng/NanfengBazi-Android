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
    val hiddenTenGods: List<String>,
)

data class ProfessionalTimelineItem(
    val key: String,
    val label: String,
    val subtitle: String,
    val observedAt: CivilDateTime,
    val pillar: String,
    val stemTenGod: String,
    val hiddenTenGods: List<String>,
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
