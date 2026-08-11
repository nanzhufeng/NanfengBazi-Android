package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.BasicShenShaRules
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneResolver
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneLayer
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneSelection
import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.ProfessionalHiddenStem
import com.nanzhufeng.nanfengbazi.domain.ProfessionalPillarColumn
import com.nanzhufeng.nanfengbazi.domain.ProfessionalTextGroup
import com.nanzhufeng.nanfengbazi.domain.ProfessionalTimelineItem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.DECADE_FORTUNE_COUNT
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.MonthBoundaryRule
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermType
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.YearBoundaryRule
import com.tyme.solar.SolarTerm
import com.tyme.solar.SolarTime
import com.tyme.sixtycycle.EarthBranch
import com.tyme.sixtycycle.HeavenStem
import com.tyme.sixtycycle.SixtyCycle
import com.tyme.sixtycycle.SixtyCycleYear
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration

class TymeProfessionalFortuneResolver(
    private val fortunePositionResolver: TymeFortunePositionResolver =
        TymeFortunePositionResolver(),
) : ProfessionalFortuneResolver {
    override fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): ProfessionalFortunePosition {
        val coveredResult = result.withProfessionalDecadeCoverage()
        require(coveredResult.profile.yearBoundaryRule == YearBoundaryRule.SPRING_EXACT) {
            "当前专业岁运只支持精确立春换年。"
        }
        require(coveredResult.profile.monthBoundaryRule == MonthBoundaryRule.SOLAR_TERM_EXACT) {
            "当前专业岁运只支持精确节令换月。"
        }
        val solar = observedAt.toTyme()
        val eightChar = solar.lunarHour.resolveEightChar(coveredResult.profile.ratHourRule)
        val previousTerm = solar.term
        val dayMaster = HeavenStem.fromName(coveredResult.fourPillars.day.take(1))
        val position = fortunePositionResolver.locate(coveredResult, observedAt)
        val flowPillars = FourPillars(
            year = eightChar.year.name,
            month = eightChar.month.name,
            day = eightChar.day.name,
            hour = eightChar.hour.name,
        )
        val columns = buildProfessionalColumns(coveredResult, position, flowPillars, dayMaster)
        val minorTimeline = buildMinorTimeline(coveredResult, position, dayMaster)
        return ProfessionalFortunePosition(
            position = position,
            flowPillars = flowPillars,
            pillarColumns = columns,
            minorTimeline = minorTimeline,
            decadeTimeline = buildDecadeTimeline(
                coveredResult,
                position,
                dayMaster,
                minorTimeline,
            ),
            annualTimeline = buildAnnualTimeline(coveredResult, position, dayMaster),
            monthlyTimeline = buildMonthlyTimeline(coveredResult, position, observedAt, dayMaster),
            dailyTimeline = buildDailyTimeline(coveredResult, position, observedAt, dayMaster),
            hourlyTimeline = buildHourlyTimeline(
                coveredResult,
                position,
                observedAt,
                dayMaster,
            ),
            interactionGroups = buildInteractionGroups(columns),
            shenShaGroups = buildShenShaGroups(columns),
            completedAge = coveredResult.completedAgeAt(observedAt),
            selectedDateDetail = solar.toSelectedDateDetail(eightChar.hour.earthBranch.name),
            previousSolarTerm = previousTerm.toDomainPoint(),
            nextSolarTerm = previousTerm.next(1).toDomainPoint(),
            observationTimeMode = SolarTimeMode.CIVIL_TIME,
            profileId = coveredResult.profile.id,
            ruleVersion = coveredResult.profile.ruleVersion,
            detailRuleVersion = DETAIL_RULE_VERSION,
            minorFortuneRuleVersion = MINOR_FORTUNE_RULE_VERSION,
        )
    }

    override fun select(
        result: CalculationResult,
        current: ProfessionalFortunePosition,
        selection: ProfessionalFortuneSelection,
    ): ProfessionalFortunePosition {
        val coveredResult = result.withProfessionalDecadeCoverage()
        val solar = selection.observedAt.toTyme()
        val eightChar = solar.lunarHour.resolveEightChar(coveredResult.profile.ratHourRule)
        val position = fortunePositionResolver.locate(coveredResult, selection.observedAt)
        val flowPillars = FourPillars(
            year = eightChar.year.name,
            month = eightChar.month.name,
            day = eightChar.day.name,
            hour = eightChar.hour.name,
        )
        val candidate = current.copy(position = position, flowPillars = flowPillars)
        require(candidate.keepsParentsOf(current, selection.layer)) {
            "${selection.layer.name.lowercase()} 候选越过当前父级岁运区间。"
        }
        val dayMaster = HeavenStem.fromName(coveredResult.fourPillars.day.take(1))
        val columns = buildProfessionalColumns(coveredResult, position, flowPillars, dayMaster)
        val selectedAt = selection.observedAt
        val timelineAtSelection: List<ProfessionalTimelineItem>.() -> List<ProfessionalTimelineItem> = {
            map { item -> item.copy(selected = item.observedAt == selectedAt) }
        }
        val minorTimeline: List<ProfessionalTimelineItem>
        val decadeTimeline: List<ProfessionalTimelineItem>
        val annualTimeline: List<ProfessionalTimelineItem>
        val monthlyTimeline: List<ProfessionalTimelineItem>
        val dailyTimeline: List<ProfessionalTimelineItem>
        val hourlyTimeline: List<ProfessionalTimelineItem>
        when (selection.layer) {
            ProfessionalFortuneLayer.DECADE -> {
                minorTimeline = buildMinorTimeline(coveredResult, position, dayMaster)
                decadeTimeline = buildDecadeTimeline(coveredResult, position, dayMaster, minorTimeline)
                annualTimeline = buildAnnualTimeline(coveredResult, position, dayMaster)
                monthlyTimeline = buildMonthlyTimeline(coveredResult, position, selectedAt, dayMaster)
                dailyTimeline = buildDailyTimeline(coveredResult, position, selectedAt, dayMaster)
                hourlyTimeline = buildHourlyTimeline(coveredResult, position, selectedAt, dayMaster)
            }
            ProfessionalFortuneLayer.ANNUAL -> {
                minorTimeline = current.minorTimeline
                decadeTimeline = current.decadeTimeline
                annualTimeline = current.annualTimeline.timelineAtSelection()
                monthlyTimeline = buildMonthlyTimeline(coveredResult, position, selectedAt, dayMaster)
                dailyTimeline = buildDailyTimeline(coveredResult, position, selectedAt, dayMaster)
                hourlyTimeline = buildHourlyTimeline(coveredResult, position, selectedAt, dayMaster)
            }
            ProfessionalFortuneLayer.MONTHLY -> {
                minorTimeline = current.minorTimeline
                decadeTimeline = current.decadeTimeline
                annualTimeline = current.annualTimeline
                monthlyTimeline = current.monthlyTimeline.timelineAtSelection()
                dailyTimeline = buildDailyTimeline(coveredResult, position, selectedAt, dayMaster)
                hourlyTimeline = buildHourlyTimeline(coveredResult, position, selectedAt, dayMaster)
            }
            ProfessionalFortuneLayer.DAILY -> {
                minorTimeline = current.minorTimeline
                decadeTimeline = current.decadeTimeline
                annualTimeline = current.annualTimeline
                monthlyTimeline = current.monthlyTimeline
                dailyTimeline = current.dailyTimeline.timelineAtSelection()
                hourlyTimeline = buildHourlyTimeline(coveredResult, position, selectedAt, dayMaster)
            }
            ProfessionalFortuneLayer.HOURLY -> {
                minorTimeline = current.minorTimeline
                decadeTimeline = current.decadeTimeline
                annualTimeline = current.annualTimeline
                monthlyTimeline = current.monthlyTimeline
                dailyTimeline = current.dailyTimeline
                hourlyTimeline = current.hourlyTimeline.timelineAtSelection()
            }
        }
        return current.copy(
            position = position,
            flowPillars = flowPillars,
            pillarColumns = columns,
            minorTimeline = minorTimeline,
            decadeTimeline = decadeTimeline,
            annualTimeline = annualTimeline,
            monthlyTimeline = monthlyTimeline,
            dailyTimeline = dailyTimeline,
            hourlyTimeline = hourlyTimeline,
            interactionGroups = buildInteractionGroups(columns),
            shenShaGroups = buildShenShaGroups(columns),
            completedAge = coveredResult.completedAgeAt(selectedAt),
            selectedDateDetail = solar.toSelectedDateDetail(eightChar.hour.earthBranch.name),
            previousSolarTerm = solar.term.toDomainPoint(),
            nextSolarTerm = solar.term.next(1).toDomainPoint(),
        )
    }
}

private fun buildProfessionalColumns(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    flowPillars: FourPillars,
    dayMaster: HeavenStem,
): List<ProfessionalPillarColumn> = listOf(
    "flow_hour" to ("流时" to flowPillars.hour),
    "flow_day" to ("流日" to flowPillars.day),
    "flow_month" to ("流月" to flowPillars.month),
    "flow_year" to ("流年" to flowPillars.year),
    "decade" to ("大运" to (position.decadeFortune?.name ?: "—")),
    "natal_year" to ("年柱" to result.fourPillars.year),
    "natal_month" to ("月柱" to result.fourPillars.month),
    "natal_day" to ("日柱" to result.fourPillars.day),
    "natal_hour" to ("时柱" to result.fourPillars.hour),
).map { (key, labelled) ->
    labelled.second.toPillarColumn(key, labelled.first, dayMaster)
}

private fun ProfessionalFortunePosition.keepsParentsOf(
    current: ProfessionalFortunePosition,
    layer: ProfessionalFortuneLayer,
): Boolean {
    val sameDecade = position.status == current.position.status &&
        position.decadeFortune?.name == current.position.decadeFortune?.name &&
        position.decadeFortune?.startAt == current.position.decadeFortune?.startAt
    val sameAnnual = position.annualFortune.calendarYear ==
        current.position.annualFortune.calendarYear
    val sameMonth = flowPillars.month == current.flowPillars.month
    val sameDay = flowPillars.day == current.flowPillars.day
    return when (layer) {
        ProfessionalFortuneLayer.DECADE -> true
        ProfessionalFortuneLayer.ANNUAL -> sameDecade
        ProfessionalFortuneLayer.MONTHLY -> sameDecade && sameAnnual
        ProfessionalFortuneLayer.DAILY -> sameDecade && sameAnnual && sameMonth
        ProfessionalFortuneLayer.HOURLY -> sameDecade && sameAnnual && sameMonth && sameDay
    }
}

private fun CalculationResult.withProfessionalDecadeCoverage(): CalculationResult {
    if (decadeFortunes.size >= DECADE_FORTUNE_COUNT || decadeFortunes.isEmpty()) return this
    val directionStep = if (fortuneStart.direction == FortuneDirection.FORWARD) 1 else -1
    val completedDecades = buildList {
        addAll(decadeFortunes)
        while (size < DECADE_FORTUNE_COUNT) {
            val previous = last()
            val startAt = previous.endAtExclusive
                ?: CivilDateTime(previous.endYear + 1, 1, 1, 0, 0, 0)
            add(
                DecadeFortune(
                    name = SixtyCycle.fromName(previous.name).next(directionStep).name,
                    startAge = previous.startAge + 10,
                    endAge = previous.endAge + 10,
                    startYear = previous.startYear + 10,
                    endYear = previous.endYear + 10,
                    startAt = startAt,
                    endAtExclusive = startAt.plusYearsForCoverage(10),
                ),
            )
        }
    }
    val birthYear = calendarConversion?.solarDateTime?.year
        ?: (normalizedInput.calendarInput as? com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar)
            ?.dateTime?.year
        ?: annualFortunes.firstOrNull()?.calendarYear
        ?: return copy(decadeFortunes = completedDecades)
    val completedAnnuals = (birthYear..completedDecades.last().endYear).map { year ->
        val decadeIndex = completedDecades.indexOfFirst { year in it.startYear..it.endYear }
            .takeIf { it >= 0 }
        AnnualFortune(
            name = SixtyCycleYear.fromYear(year).sixtyCycle.name,
            calendarYear = year,
            nominalAge = year - birthYear + 1,
            decadeIndex = decadeIndex?.plus(1),
            decadeName = decadeIndex?.let(completedDecades::get)?.name,
        )
    }
    return copy(decadeFortunes = completedDecades, annualFortunes = completedAnnuals)
}

private fun CivilDateTime.plusYearsForCoverage(years: Long): CivilDateTime {
    val value = LocalDateTime.of(year, month, day, hour, minute, second).plusYears(years)
    return CivilDateTime(
        year = value.year,
        month = value.monthValue,
        day = value.dayOfMonth,
        hour = value.hour,
        minute = value.minute,
        second = value.second,
    )
}

private fun SolarTime.toSelectedDateDetail(hourBranch: String): String {
    val lunarDay = lunarHour.lunarDay
    val lunarMonth = lunarDay.lunarMonth
    val lunarYear = lunarMonth.lunarYear.name.removePrefix("农历")
    return "农历 $lunarYear${lunarMonth.name}${lunarDay.name} $hourBranch 时"
}

private fun String.toPillarColumn(
    key: String,
    label: String,
    dayMaster: HeavenStem,
): ProfessionalPillarColumn {
    if (length < 2) {
        return ProfessionalPillarColumn(
            key = key,
            label = label,
            pillar = this,
            stemTenGod = "—",
            heavenStemElement = "—",
            earthBranchElement = "—",
            hiddenStems = emptyList(),
        )
    }
    val cycle = SixtyCycle.fromName(take(2))
    return ProfessionalPillarColumn(
        key = key,
        label = label,
        pillar = cycle.name,
        stemTenGod = dayMaster.getTenStar(cycle.heavenStem).name,
        heavenStemElement = cycle.heavenStem.element.name,
        earthBranchElement = cycle.earthBranch.element.name,
        hiddenStems = cycle.earthBranch.hideHeavenStems.map {
            ProfessionalHiddenStem(
                heavenStem = it.heavenStem.name,
                element = it.heavenStem.element.name,
                tenGod = dayMaster.getTenStar(it.heavenStem).name,
            )
        },
    )
}

private fun buildTimelineItem(
    key: String,
    label: String,
    subtitle: String,
    observedAt: CivilDateTime,
    pillar: String,
    selected: Boolean,
    dayMaster: HeavenStem,
): ProfessionalTimelineItem {
    val detail = pillar.toPillarColumn(key, label, dayMaster)
    return ProfessionalTimelineItem(
        key = key,
        label = label,
        subtitle = subtitle,
        observedAt = observedAt,
        pillar = detail.pillar,
        stemTenGod = detail.stemTenGod,
        heavenStemElement = detail.heavenStemElement,
        earthBranchElement = detail.earthBranchElement,
        hiddenStems = detail.hiddenStems,
        selected = selected,
    )
}

private fun CalculationResult.completedAgeAt(observedAt: CivilDateTime): Int {
    val birth = calendarConversion?.solarDateTime
        ?: (normalizedInput.calendarInput as? com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar)
            ?.dateTime
        ?: return 0
    val birthDate = LocalDate.of(birth.year, birth.month, birth.day)
    val observedDate = LocalDate.of(observedAt.year, observedAt.month, observedAt.day)
    if (observedDate < birthDate) return 0
    val roughYears = observedDate.year - birthDate.year
    return (roughYears - if (observedDate < birthDate.plusYears(roughYears.toLong())) 1 else 0)
        .coerceAtLeast(0)
}

/**
 * 小运采用《三命通会·论小运》的时柱起法：落地即从时柱下一位起，
 * 阳男阴女顺排、阴男阳女逆排，一位一年，只覆盖精确交运前的童限。
 */
private fun buildMinorTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val birth = result.calendarConversion?.solarDateTime
        ?: (result.normalizedInput.calendarInput as?
            com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar)?.dateTime
        ?: return emptyList()
    val firstDecadeStart = result.decadeFortunes.firstOrNull()?.startAt
        ?: result.fortuneStart.endAt
    val birthValue = birth.toLocalDateTime()
    val firstDecadeValue = firstDecadeStart.toLocalDateTime()
    if (!birthValue.isBefore(firstDecadeValue)) return emptyList()

    val observation = position.observedAt.toLocalDateTime()
    val directionStep = if (result.fortuneStart.direction == FortuneDirection.FORWARD) 1 else -1
    val hourPillar = SixtyCycle.fromName(result.fourPillars.hour)
    return buildList {
        var index = 0
        while (true) {
            val periodStart = birthValue.plusYears(index.toLong())
            if (!periodStart.isBefore(firstDecadeValue)) break
            val fullYearEnd = birthValue.plusYears(index + 1L)
            val periodEnd = minOf(fullYearEnd, firstDecadeValue)
            val anchor = periodStart.plusMinutes(1).withSecond(0)
            if (!anchor.isBefore(periodEnd)) break
            val observedAt = anchor.toDomain()
            val clippedAtDecadeStart = periodEnd == firstDecadeValue && periodEnd != fullYearEnd
            val endHint = if (clippedAtDecadeStart) {
                "·至${periodEnd.year}/${periodEnd.monthValue}/${periodEnd.dayOfMonth}"
            } else {
                ""
            }
            add(
                buildTimelineItem(
                    key = "minor_$index",
                    label = periodStart.year.toString(),
                    subtitle = "${result.completedAgeAt(observedAt)}岁$endHint",
                    observedAt = observedAt,
                    pillar = hourPillar.next(directionStep * (index + 1)).name,
                    selected = position.status == FortunePositionStatus.BEFORE_FIRST_DECADE &&
                        !observation.isBefore(periodStart) && observation.isBefore(periodEnd),
                    dayMaster = dayMaster,
                ),
            )
            index += 1
        }
    }
}

private fun buildDecadeTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    dayMaster: HeavenStem,
    minorTimeline: List<ProfessionalTimelineItem>,
): List<ProfessionalTimelineItem> = buildList {
    buildMinorStageItem(result, position, minorTimeline)?.let(::add)
    result.decadeFortunes.forEachIndexed { index, decade ->
        val startAt = decade.startAt ?: CivilDateTime(decade.startYear, 7, 1, 12, 0, 0)
        val finalMoment = decade.endAtExclusive?.minusOneSecond()
            ?: CivilDateTime(decade.endYear, 12, 31, 23, 59, 59)
        add(
            buildTimelineItem(
                key = "decade_$index",
                label = decade.startYear.toString(),
                subtitle = "${result.completedAgeAt(startAt)}–${result.completedAgeAt(finalMoment)}岁",
                observedAt = startAt.stableMinuteAfterBoundary(),
                pillar = decade.name,
                selected = position.decadeFortune?.name == decade.name,
                dayMaster = dayMaster,
            ),
        )
    }
}

private fun buildMinorStageItem(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    minorTimeline: List<ProfessionalTimelineItem>,
): ProfessionalTimelineItem? {
    val firstMinor = minorTimeline.firstOrNull() ?: return null
    val firstDecadeStart = result.decadeFortunes.firstOrNull()?.startAt
        ?: result.fortuneStart.endAt
    val finalMoment = firstDecadeStart.minusOneSecond()
    return ProfessionalTimelineItem(
        key = "minor_stage",
        label = firstMinor.observedAt.year.toString(),
        subtitle = "${result.completedAgeAt(firstMinor.observedAt)}–" +
            "${result.completedAgeAt(finalMoment)}岁",
        observedAt = firstMinor.observedAt,
        pillar = "",
        stemTenGod = "",
        heavenStemElement = "",
        earthBranchElement = "",
        selected = position.status == FortunePositionStatus.BEFORE_FIRST_DECADE,
        stageLabel = "小运",
    )
}

private fun buildAnnualTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val birth = result.calendarConversion?.solarDateTime
        ?: (result.normalizedInput.calendarInput as?
            com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar)?.dateTime
    val firstDecadeStart = result.decadeFortunes.firstOrNull()?.startAt
        ?: result.fortuneStart.endAt
    val currentDecade = position.decadeFortune
    val visible = when {
        position.status == FortunePositionStatus.BEFORE_FIRST_DECADE && birth != null -> {
            val birthValue = birth.toLocalDateTime()
            val firstDecadeValue = firstDecadeStart.toLocalDateTime()
            result.annualFortunes.filter { annual ->
                val annualStart = SolarTerm.fromName(annual.calendarYear, "立春")
                    .julianDay.solarTime.toDomain().toLocalDateTime()
                val annualEnd = SolarTerm.fromName(annual.calendarYear + 1, "立春")
                    .julianDay.solarTime.toDomain().toLocalDateTime()
                maxOf(annualStart, birthValue).isBefore(minOf(annualEnd, firstDecadeValue))
            }
        }
        currentDecade != null -> {
            result.annualFortunes.filter {
                it.calendarYear in currentDecade.startYear..currentDecade.endYear
            }
        }
        else -> {
            val index = result.annualFortunes.indexOfFirst {
                it.calendarYear == position.annualFortune.calendarYear
            }.coerceAtLeast(0)
            result.annualFortunes.drop((index / 10) * 10).take(10)
        }
    }
    return visible.map { annual ->
        val annualStart = SolarTerm.fromName(annual.calendarYear, "立春").julianDay.solarTime.toDomain()
        val boundaryAt = when {
            position.status == FortunePositionStatus.BEFORE_FIRST_DECADE && birth != null &&
                annualStart.toLocalDateTime().isBefore(birth.toLocalDateTime()) -> birth
            else -> position.decadeFortune?.startAt
                ?.takeIf { start ->
                    start.year == annual.calendarYear && annualStart.toTyme().isBefore(start.toTyme())
                }
                ?: annualStart
        }
        val at = boundaryAt.stableMinuteAfterBoundary()
        buildTimelineItem(
            key = "annual_${annual.calendarYear}",
            label = annual.calendarYear.toString(),
            subtitle = "${result.completedAgeAt(at)}岁",
            observedAt = at,
            pillar = annual.name,
            selected = annual.calendarYear == position.annualFortune.calendarYear,
            dayMaster = dayMaster,
        )
    }
}

private fun CivilDateTime.minusOneSecond(): CivilDateTime {
    val value = LocalDateTime.of(year, month, day, hour, minute, second).minusSeconds(1)
    return CivilDateTime(
        year = value.year,
        month = value.monthValue,
        day = value.dayOfMonth,
        hour = value.hour,
        minute = value.minute,
        second = value.second,
    )
}

/**
 * 时间轴候选会写回只精确到分钟的观察时间。边界若带秒数，直接截断会落回上一柱，
 * 因此候选统一使用边界后的第一个完整分钟作为可重复定位的点击锚点。
 */
private fun CivilDateTime.stableMinuteAfterBoundary(): CivilDateTime {
    val value = LocalDateTime.of(year, month, day, hour, minute, second)
        .plusMinutes(1)
        .withSecond(0)
    return CivilDateTime(
        year = value.year,
        month = value.monthValue,
        day = value.dayOfMonth,
        hour = value.hour,
        minute = value.minute,
        second = 0,
    )
}

private fun CivilDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, month, day, hour, minute, second)

private fun LocalDateTime.toDomain(): CivilDateTime = CivilDateTime(
    year = year,
    month = monthValue,
    day = dayOfMonth,
    hour = hour,
    minute = minute,
    second = second,
)

private data class ProfessionalTimeInterval(
    val start: LocalDateTime,
    val endExclusive: LocalDateTime,
) {
    init {
        require(start.isBefore(endExclusive))
    }

    fun intersect(other: ProfessionalTimeInterval): ProfessionalTimeInterval? {
        val intersectionStart = maxOf(start, other.start)
        val intersectionEnd = minOf(endExclusive, other.endExclusive)
        return if (intersectionStart.isBefore(intersectionEnd)) {
            ProfessionalTimeInterval(intersectionStart, intersectionEnd)
        } else {
            null
        }
    }

    fun contains(value: LocalDateTime): Boolean =
        !value.isBefore(start) && value.isBefore(endExclusive)

    fun firstStableMinute(): LocalDateTime = start.plusMinutes(1).withSecond(0)

    fun anchorOn(date: LocalDate, preferredTime: LocalTime): LocalDateTime? {
        val dayInterval = ProfessionalTimeInterval(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
        val overlap = intersect(dayInterval) ?: return null
        val preferred = date.atTime(preferredTime).withSecond(0)
        if (preferred.isAfter(overlap.start) && preferred.isBefore(overlap.endExclusive)) {
            return preferred
        }
        val afterStart = overlap.firstStableMinute()
        if (afterStart.isBefore(overlap.endExclusive)) return afterStart
        val beforeEnd = overlap.endExclusive.minusMinutes(1).withSecond(0)
        return beforeEnd.takeIf { !it.isBefore(overlap.start) && it.isBefore(overlap.endExclusive) }
    }
}

private fun flowMonthBoundaries(calendarYear: Int): List<LocalDateTime> {
    val firstJie = SolarTerm.fromName(calendarYear, "立春")
    return (0..12).map { index ->
        firstJie.next(index * 2).julianDay.solarTime.toDomain().toLocalDateTime()
    }
}

private fun CalculationResult.professionalParentInterval(
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
): ProfessionalTimeInterval? {
    position.decadeFortune?.let { decade ->
        val start = decade.startAt ?: return@let
        val end = decade.endAtExclusive ?: return@let
        return ProfessionalTimeInterval(start.toLocalDateTime(), end.toLocalDateTime())
    }
    if (position.status != FortunePositionStatus.BEFORE_FIRST_DECADE) return null
    val birth = calendarConversion?.solarDateTime
        ?: (normalizedInput.calendarInput as?
            com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar)?.dateTime
        ?: return null
    val firstDecadeStart = decadeFortunes.firstOrNull()?.startAt ?: fortuneStart.endAt
    return ProfessionalTimeInterval(birth.toLocalDateTime(), firstDecadeStart.toLocalDateTime())
}

private fun buildMonthlyTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    observedAt: CivilDateTime,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val boundaries = flowMonthBoundaries(position.annualFortune.calendarYear)
    val annualInterval = ProfessionalTimeInterval(boundaries.first(), boundaries.last())
    val parentInterval = result.professionalParentInterval(position)
        ?.intersect(annualInterval) ?: annualInterval
    return (0 until 12).mapNotNull { index ->
        val interval = ProfessionalTimeInterval(boundaries[index], boundaries[index + 1])
            .intersect(parentInterval) ?: return@mapNotNull null
        val at = interval.firstStableMinute().toDomain()
        if (!at.isWithinSupportedCalendarRange()) return@mapNotNull null
        val pillar = at.toTyme().lunarHour.resolveEightChar(result.profile.ratHourRule).month.name
        val boundaryAt = boundaries[index]
        buildTimelineItem(
            key = "month_${boundaryAt.year}_${boundaryAt.monthValue}_${boundaryAt.dayOfMonth}",
            label = "${boundaryAt.monthValue}/${boundaryAt.dayOfMonth}",
            subtitle = SolarTerm.fromName(position.annualFortune.calendarYear, "立春")
                .next(index * 2).name,
            observedAt = at,
            pillar = pillar,
            selected = interval.contains(observedAt.toLocalDateTime()),
            dayMaster = dayMaster,
        )
    }
}

private fun buildDailyTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    observedAt: CivilDateTime,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val selectedAt = observedAt.toLocalDateTime()
    val boundaries = flowMonthBoundaries(position.annualFortune.calendarYear)
    val monthIndex = (0 until 12).firstOrNull { index ->
        !selectedAt.isBefore(boundaries[index]) && selectedAt.isBefore(boundaries[index + 1])
    } ?: return emptyList()
    val flowMonthInterval = ProfessionalTimeInterval(
        boundaries[monthIndex],
        boundaries[monthIndex + 1],
    )
    val interval = result.professionalParentInterval(position)
        ?.intersect(flowMonthInterval) ?: flowMonthInterval
    val selectedDate = selectedAt.toLocalDate()
    val finalDate = interval.endExclusive.minusNanos(1).toLocalDate()
    return generateSequence(interval.start.toLocalDate()) { date ->
        date.plusDays(1).takeIf { !it.isAfter(finalDate) }
    }.mapNotNull { date ->
        val at = interval.anchorOn(date, selectedAt.toLocalTime())?.toDomain()
            ?: return@mapNotNull null
        if (!at.isWithinSupportedCalendarRange()) return@mapNotNull null
        val pillar = at.toTyme().lunarHour.resolveEightChar(result.profile.ratHourRule).day.name
        buildTimelineItem(
            key = "day_${date}",
            label = "${date.monthValue}/${date.dayOfMonth}",
            subtitle = if (date == selectedDate) "已选" else date.dayOfWeek.chineseShortName(),
            observedAt = at,
            pillar = pillar,
            selected = date == selectedDate,
            dayMaster = dayMaster,
        )
    }.toList()
}

private fun buildHourlyTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    observedAt: CivilDateTime,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val selectedDate = LocalDate.of(observedAt.year, observedAt.month, observedAt.day)
    val selectedAt = observedAt.toLocalDateTime()
    val selectedEightChar = observedAt.toTyme().lunarHour
        .resolveEightChar(result.profile.ratHourRule)
    val parentInterval = result.professionalParentInterval(position)
    val hours = listOf(23, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21)
    return hours.mapNotNull { hour ->
        val candidates = buildList {
            addAll((-1L..1L).map { dayOffset ->
                selectedDate.plusDays(dayOffset).atTime(hour, 0)
            })
            if (observedAt.hour.belongsToDoubleHourStartingAt(hour)) add(selectedAt)
        }
        val at = candidates.distinct().filter { candidate ->
            val candidateAt = candidate.toDomain()
            if (!candidateAt.isWithinSupportedCalendarRange()) return@filter false
            if (parentInterval != null && !parentInterval.contains(candidate)) return@filter false
            val candidateEightChar = candidateAt.toTyme().lunarHour
                .resolveEightChar(result.profile.ratHourRule)
            candidateEightChar.year.name == selectedEightChar.year.name &&
                candidateEightChar.month.name == selectedEightChar.month.name &&
                candidateEightChar.day.name == selectedEightChar.day.name
        }.minByOrNull { candidate ->
            kotlin.math.abs(Duration.between(selectedAt, candidate).seconds)
        }?.toDomain() ?: return@mapNotNull null
        val pillar = at.toTyme().lunarHour.resolveEightChar(result.profile.ratHourRule).hour.name
        buildTimelineItem(
            key = "hour_${at.year}_${at.month}_${at.day}_$hour",
            label = "%02d:00".format(hour),
            subtitle = pillar.takeLast(1) + "时",
            observedAt = at,
            pillar = pillar,
            selected = selectedEightChar.hour.name == pillar,
            dayMaster = dayMaster,
        )
    }
}

private fun Int.belongsToDoubleHourStartingAt(startHour: Int): Boolean = when (startHour) {
    23 -> this == 23 || this == 0
    else -> this in startHour..(startHour + 1)
}

private fun CivilDateTime.isWithinSupportedCalendarRange(): Boolean =
    LocalDate.of(year, month, day) in SUPPORTED_CALENDAR_START..SUPPORTED_CALENDAR_END

private fun java.time.DayOfWeek.chineseShortName(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "周一"
    java.time.DayOfWeek.TUESDAY -> "周二"
    java.time.DayOfWeek.WEDNESDAY -> "周三"
    java.time.DayOfWeek.THURSDAY -> "周四"
    java.time.DayOfWeek.FRIDAY -> "周五"
    java.time.DayOfWeek.SATURDAY -> "周六"
    java.time.DayOfWeek.SUNDAY -> "周日"
}

private fun buildInteractionGroups(
    columns: List<ProfessionalPillarColumn>,
): List<ProfessionalTextGroup> {
    val allColumns = columns.filter { it.pillar.length >= 2 }
    return listOf(
        ProfessionalTextGroup(
            "天干",
            pairRelations(allColumns, allColumns, useStem = true, withinSameList = true),
        ),
        ProfessionalTextGroup(
            "地支",
            pairRelations(allColumns, allColumns, useStem = false, withinSameList = true) +
                tripleBranchRelations(allColumns),
        ),
    ).map { group -> group.copy(lines = group.lines.distinct()) }
}

private fun pairRelations(
    left: List<ProfessionalPillarColumn>,
    right: List<ProfessionalPillarColumn>,
    useStem: Boolean,
    withinSameList: Boolean = false,
): List<String> {
    val output = mutableListOf<String>()
    left.forEachIndexed { leftIndex, a ->
        right.forEachIndexed { rightIndex, b ->
            if (a.key == b.key || (withinSameList && rightIndex <= leftIndex)) return@forEachIndexed
            val first = a.pillar[if (useStem) 0 else 1]
            val second = b.pillar[if (useStem) 0 else 1]
            val relations = if (useStem) stemRelations(first, second) else branchRelations(first, second)
            output += relations
        }
    }
    return output.distinct()
}

private fun stemRelations(a: Char, b: Char): List<String> = buildList {
    val pair = setOf(a, b)
    STEM_COMBINES[pair]?.let(::add)
    STEM_CLASHES[pair]?.let(::add)
}

private fun branchRelations(a: Char, b: Char): List<String> = buildList {
    val pair = setOf(a, b)
    BRANCH_COMBINES[pair]?.let(::add)
    BRANCH_CLASHES[pair]?.let(::add)
    BRANCH_HARMS[pair]?.let(::add)
    if (a == b && a in SELF_PUNISH) add("$a${a}自刑")
    BRANCH_PUNISH_PAIRS[pair]?.let(::add)
}

private fun tripleBranchRelations(columns: List<ProfessionalPillarColumn>): List<String> {
    val branches = columns.filter { it.pillar.length >= 2 }.map { it.pillar[1] }.toSet()
    return (THREE_HARMONIES + THREE_MEETINGS).mapNotNull { (required, label) ->
        label.takeIf { branches.containsAll(required) }
    }
}

private fun buildShenShaGroups(
    columns: List<ProfessionalPillarColumn>,
): List<ProfessionalTextGroup> {
    val natal = columns.filter { it.key.startsWith("natal_") }
    val context = BasicShenShaRules.context(
        dayStem = natal.first { it.key == "natal_day" }.pillar[0],
        roots = listOf(
            natal.first { it.key == "natal_year" }.pillar[1],
            natal.first { it.key == "natal_day" }.pillar[1],
        ),
        monthBranch = natal.first { it.key == "natal_month" }.pillar[1],
        yearBranch = natal.first { it.key == "natal_year" }.pillar[1],
    )
    fun line(column: ProfessionalPillarColumn): String? {
        val names = BasicShenShaRules.resolveNames(column.pillar, context)
            .take(DETAIL_SHEN_SHA_LIMIT)
        return names.takeIf { it.isNotEmpty() }
            ?.let { "${column.pillar}　${it.joinToString(" · ")}" }
    }

    val transitKeys = listOf("decade", "flow_year", "flow_month", "flow_day", "flow_hour")
    return listOfNotNull(
        ProfessionalTextGroup("原局", natal.mapNotNull(::line))
            .takeIf { it.lines.isNotEmpty() },
        ProfessionalTextGroup(
            "岁运",
            transitKeys.mapNotNull { key -> columns.firstOrNull { it.key == key }?.let(::line) },
        ).takeIf { it.lines.isNotEmpty() },
    )
}

private val STEM_COMBINES = mapOf(
    setOf('甲', '己') to "甲己合", setOf('乙', '庚') to "乙庚合",
    setOf('丙', '辛') to "丙辛合", setOf('丁', '壬') to "丁壬合", setOf('戊', '癸') to "戊癸合",
)
private val STEM_CLASHES = mapOf(
    setOf('甲', '庚') to "甲庚冲", setOf('乙', '辛') to "乙辛冲",
    setOf('丙', '壬') to "丙壬冲", setOf('丁', '癸') to "丁癸冲",
)
private val BRANCH_COMBINES = mapOf(
    setOf('子', '丑') to "子丑合", setOf('寅', '亥') to "寅亥合", setOf('卯', '戌') to "卯戌合",
    setOf('辰', '酉') to "辰酉合", setOf('巳', '申') to "巳申合", setOf('午', '未') to "午未合",
)
private val BRANCH_CLASHES = mapOf(
    setOf('子', '午') to "子午冲", setOf('丑', '未') to "丑未冲", setOf('寅', '申') to "寅申冲",
    setOf('卯', '酉') to "卯酉冲", setOf('辰', '戌') to "辰戌冲", setOf('巳', '亥') to "巳亥冲",
)
private val BRANCH_HARMS = mapOf(
    setOf('子', '未') to "子未相害", setOf('丑', '午') to "丑午相害", setOf('寅', '巳') to "寅巳相害",
    setOf('卯', '辰') to "卯辰相害", setOf('申', '亥') to "申亥相害", setOf('酉', '戌') to "酉戌相害",
)
private val BRANCH_PUNISH_PAIRS = mapOf(
    setOf('子', '卯') to "子卯相刑", setOf('寅', '巳') to "寅巳相刑", setOf('巳', '申') to "巳申相刑",
    setOf('丑', '戌') to "丑戌相刑", setOf('戌', '未') to "戌未相刑", setOf('未', '丑') to "未丑相刑",
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
private const val DETAIL_RULE_VERSION = "professional-detail-relations-shensha-v4"
private const val DETAIL_SHEN_SHA_LIMIT = 5
private const val MINOR_FORTUNE_RULE_VERSION = "professional-minor-fortune-v1"
private val SUPPORTED_CALENDAR_START: LocalDate = LocalDate.of(1800, 1, 1)
private val SUPPORTED_CALENDAR_END: LocalDate = LocalDate.of(2200, 12, 31)

private fun CivilDateTime.toTyme(): SolarTime =
    SolarTime.fromYmdHms(year, month, day, hour, minute, second)

private fun SolarTerm.toDomainPoint(): SolarTermPoint =
    SolarTermPoint(
        name = name,
        type = if (isJie) SolarTermType.JIE else SolarTermType.QI,
        at = julianDay.solarTime.toDomain(),
    )

private fun SolarTime.toDomain(): CivilDateTime =
    CivilDateTime(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
    )
