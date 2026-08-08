package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneResolver
import com.nanzhufeng.nanfengbazi.domain.ProfessionalPillarColumn
import com.nanzhufeng.nanfengbazi.domain.ProfessionalTextGroup
import com.nanzhufeng.nanfengbazi.domain.ProfessionalTimelineItem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
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
import java.time.LocalDate

class TymeProfessionalFortuneResolver(
    private val fortunePositionResolver: TymeFortunePositionResolver =
        TymeFortunePositionResolver(),
) : ProfessionalFortuneResolver {
    override fun locate(
        result: CalculationResult,
        observedAt: CivilDateTime,
    ): ProfessionalFortunePosition {
        require(result.profile.yearBoundaryRule == YearBoundaryRule.SPRING_EXACT) {
            "当前专业岁运只支持精确立春换年。"
        }
        require(result.profile.monthBoundaryRule == MonthBoundaryRule.SOLAR_TERM_EXACT) {
            "当前专业岁运只支持精确节令换月。"
        }
        val solar = observedAt.toTyme()
        val eightChar = solar.lunarHour.resolveEightChar(result.profile.ratHourRule)
        val previousTerm = solar.term
        val dayMaster = HeavenStem.fromName(result.fourPillars.day.take(1))
        val position = fortunePositionResolver.locate(result, observedAt)
        val rawColumns = listOf(
            "flow_hour" to ("流时" to eightChar.hour.name),
            "flow_day" to ("流日" to eightChar.day.name),
            "flow_month" to ("流月" to eightChar.month.name),
            "flow_year" to ("流年" to eightChar.year.name),
            "decade" to ("大运" to (position.decadeFortune?.name ?: "—")),
            "natal_year" to ("年柱" to result.fourPillars.year),
            "natal_month" to ("月柱" to result.fourPillars.month),
            "natal_day" to ("日柱" to result.fourPillars.day),
            "natal_hour" to ("时柱" to result.fourPillars.hour),
        )
        val columns = rawColumns.map { (key, labelled) ->
            labelled.second.toPillarColumn(key, labelled.first, dayMaster)
        }
        return ProfessionalFortunePosition(
            position = position,
            flowPillars = FourPillars(
                year = eightChar.year.name,
                month = eightChar.month.name,
                day = eightChar.day.name,
                hour = eightChar.hour.name,
            ),
            pillarColumns = columns,
            decadeTimeline = buildDecadeTimeline(result, position, dayMaster),
            annualTimeline = buildAnnualTimeline(result, position, dayMaster),
            monthlyTimeline = buildMonthlyTimeline(result, position, observedAt, dayMaster),
            dailyTimeline = buildDailyTimeline(result, observedAt, dayMaster),
            hourlyTimeline = buildHourlyTimeline(result, observedAt, dayMaster),
            interactionGroups = buildInteractionGroups(columns),
            shenShaGroups = buildShenShaGroups(columns),
            selectedDateDetail = solar.toSelectedDateDetail(eightChar.hour.earthBranch.name),
            previousSolarTerm = previousTerm.toDomainPoint(),
            nextSolarTerm = previousTerm.next(1).toDomainPoint(),
            observationTimeMode = SolarTimeMode.CIVIL_TIME,
            profileId = result.profile.id,
            ruleVersion = result.profile.ruleVersion,
            detailRuleVersion = DETAIL_RULE_VERSION,
        )
    }
}

private fun SolarTime.toSelectedDateDetail(hourBranch: String): String {
    val lunarDay = lunarHour.lunarDay
    val lunarMonth = lunarDay.lunarMonth
    return "农历 ${lunarMonth.lunarYear.name}${lunarMonth.name}${lunarDay.name} $hourBranch 时"
}

private fun String.toPillarColumn(
    key: String,
    label: String,
    dayMaster: HeavenStem,
): ProfessionalPillarColumn {
    if (length < 2) {
        return ProfessionalPillarColumn(key, label, this, "—", emptyList())
    }
    val cycle = SixtyCycle.fromName(take(2))
    return ProfessionalPillarColumn(
        key = key,
        label = label,
        pillar = cycle.name,
        stemTenGod = dayMaster.getTenStar(cycle.heavenStem).name,
        hiddenTenGods = cycle.earthBranch.hideHeavenStems.map {
            dayMaster.getTenStar(it.heavenStem).name
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
        hiddenTenGods = detail.hiddenTenGods,
        selected = selected,
    )
}

private fun buildDecadeTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> = result.decadeFortunes.mapIndexed { index, decade ->
    buildTimelineItem(
        key = "decade_$index",
        label = decade.startYear.toString(),
        subtitle = "${decade.startAge}–${decade.endAge}岁",
        observedAt = decade.startAt ?: CivilDateTime(decade.startYear, 7, 1, 12, 0, 0),
        pillar = decade.name,
        selected = position.decadeFortune?.name == decade.name,
        dayMaster = dayMaster,
    )
}

private fun buildAnnualTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val visible = position.decadeFortune?.let { decade ->
        result.annualFortunes.filter { it.calendarYear in decade.startYear..decade.endYear }
    }.orEmpty().ifEmpty {
        val index = result.annualFortunes.indexOfFirst {
            it.calendarYear == position.annualFortune.calendarYear
        }.coerceAtLeast(0)
        result.annualFortunes.drop((index / 10) * 10).take(10)
    }
    return visible.map { annual ->
        val annualStart = SolarTerm.fromName(annual.calendarYear, "立春").julianDay.solarTime.toDomain()
        val at = position.decadeFortune?.startAt
            ?.takeIf { start ->
                start.year == annual.calendarYear && annualStart.toTyme().isBefore(start.toTyme())
            }
            ?: annualStart
        buildTimelineItem(
            key = "annual_${annual.calendarYear}",
            label = annual.calendarYear.toString(),
            subtitle = "虚${annual.nominalAge}",
            observedAt = at,
            pillar = annual.name,
            selected = annual.calendarYear == position.annualFortune.calendarYear,
            dayMaster = dayMaster,
        )
    }
}

private fun buildMonthlyTimeline(
    result: CalculationResult,
    position: com.nanzhufeng.nanfengbazi.domain.FortunePosition,
    observedAt: CivilDateTime,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val firstJie = SolarTerm.fromName(position.annualFortune.calendarYear, "立春")
    return (0 until 12).mapNotNull { index ->
        val at = firstJie.next(index * 2).julianDay.solarTime.toDomain()
        if (!at.isWithinSupportedCalendarRange()) return@mapNotNull null
        val pillar = at.toTyme().lunarHour.resolveEightChar(result.profile.ratHourRule).month.name
        buildTimelineItem(
            key = "month_${at.year}_${at.month}_${at.day}",
            label = "${at.month}/${at.day}",
            subtitle = firstJie.next(index * 2).name,
            observedAt = at,
            pillar = pillar,
            selected = pillar == observedAt.toTyme().lunarHour
                .resolveEightChar(result.profile.ratHourRule).month.name,
            dayMaster = dayMaster,
        )
    }
}

private fun buildDailyTimeline(
    result: CalculationResult,
    observedAt: CivilDateTime,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val selected = LocalDate.of(observedAt.year, observedAt.month, observedAt.day)
    val start = selected.minusDays(4).coerceInSupportedCalendarRange(10)
    return (0 until 10).map { offset ->
        val date = start.plusDays(offset.toLong())
        val at = CivilDateTime(
            date.year,
            date.monthValue,
            date.dayOfMonth,
            observedAt.hour,
            observedAt.minute,
            0,
        )
        val pillar = at.toTyme().lunarHour.resolveEightChar(result.profile.ratHourRule).day.name
        buildTimelineItem(
            key = "day_${date}",
            label = "${date.monthValue}/${date.dayOfMonth}",
            subtitle = if (date == selected) "已选" else date.dayOfWeek.chineseShortName(),
            observedAt = at,
            pillar = pillar,
            selected = date == selected,
            dayMaster = dayMaster,
        )
    }
}

private fun buildHourlyTimeline(
    result: CalculationResult,
    observedAt: CivilDateTime,
    dayMaster: HeavenStem,
): List<ProfessionalTimelineItem> {
    val selectedDate = LocalDate.of(observedAt.year, observedAt.month, observedAt.day)
    val selectedPillar = observedAt.toTyme().lunarHour
        .resolveEightChar(result.profile.ratHourRule).hour.name
    val hours = listOf(23, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21)
    return hours.mapNotNull { hour ->
        val date = if (hour == 23) selectedDate.minusDays(1) else selectedDate
        if (date !in SUPPORTED_CALENDAR_START..SUPPORTED_CALENDAR_END) return@mapNotNull null
        val at = CivilDateTime(date.year, date.monthValue, date.dayOfMonth, hour, 0, 0)
        val pillar = at.toTyme().lunarHour.resolveEightChar(result.profile.ratHourRule).hour.name
        buildTimelineItem(
            key = "hour_${at.year}_${at.month}_${at.day}_$hour",
            label = "%02d:00".format(hour),
            subtitle = pillar.takeLast(1) + "时",
            observedAt = at,
            pillar = pillar,
            selected = pillar == selectedPillar,
            dayMaster = dayMaster,
        )
    }
}

private fun CivilDateTime.isWithinSupportedCalendarRange(): Boolean =
    LocalDate.of(year, month, day) in SUPPORTED_CALENDAR_START..SUPPORTED_CALENDAR_END

private fun LocalDate.coerceInSupportedCalendarRange(windowSize: Int): LocalDate {
    val latestStart = SUPPORTED_CALENDAR_END.minusDays((windowSize - 1).toLong())
    return when {
        this < SUPPORTED_CALENDAR_START -> SUPPORTED_CALENDAR_START
        this > latestStart -> latestStart
        else -> this
    }
}

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
    val natal = columns.filter { it.key.startsWith("natal_") }
    val transit = columns.filterNot { it.key.startsWith("natal_") }
        .filter { it.pillar.length >= 2 }
    return listOf(
        ProfessionalTextGroup(
            "岁运天干",
            pairRelations(transit, natal, useStem = true),
        ),
        ProfessionalTextGroup(
            "岁运地支",
            pairRelations(transit, natal, useStem = false) + tripleBranchRelations(transit + natal),
        ),
        ProfessionalTextGroup(
            "原局天干",
            pairRelations(natal, natal, useStem = true, withinSameList = true),
        ),
        ProfessionalTextGroup(
            "原局地支",
            pairRelations(natal, natal, useStem = false, withinSameList = true) +
                tripleBranchRelations(natal),
        ),
    )
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
            relations.forEach { relation ->
                output += "${a.label}$first·${b.label}$second $relation"
            }
        }
    }
    return output.distinct()
}

private fun stemRelations(a: Char, b: Char): List<String> = buildList {
    val pair = setOf(a, b)
    if (pair in STEM_COMBINES) add("合")
    if (pair in STEM_CLASHES) add("冲")
}

private fun branchRelations(a: Char, b: Char): List<String> = buildList {
    val pair = setOf(a, b)
    if (pair in BRANCH_COMBINES) add("六合")
    if (pair in BRANCH_CLASHES) add("六冲")
    if (pair in BRANCH_HARMS) add("相害")
    if ((a == b && a in SELF_PUNISH) || pair in BRANCH_PUNISH_PAIRS) add("相刑")
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
    val dayStem = natal.first { it.key == "natal_day" }.pillar[0]
    val roots = listOf(
        natal.first { it.key == "natal_year" }.pillar[1],
        natal.first { it.key == "natal_day" }.pillar[1],
    )
    fun line(column: ProfessionalPillarColumn): String {
        val branch = column.pillar.getOrNull(1) ?: return "${column.label}：—"
        val stars = buildList {
            if (branch in TIAN_YI.getValue(dayStem)) add("天乙贵人")
            if (branch == WEN_CHANG.getValue(dayStem)) add("文昌贵人")
            if (branch == LU_SHEN.getValue(dayStem)) add("禄神")
            if (branch == YANG_REN.getValue(dayStem)) add("羊刃")
            roots.forEach { root ->
                val group = TRINE_ROOT.getValue(root)
                if (branch == group.horse) add("驿马")
                if (branch == group.peach) add("桃花")
                if (branch == group.canopy) add("华盖")
                if (branch == group.general) add("将星")
            }
        }.distinct()
        val displayedStars = stars.ifEmpty { listOf("—") }.joinToString("、")
        return "${column.label}：$displayedStars"
    }
    val transitKeys = listOf("decade", "flow_year", "flow_month", "flow_day", "flow_hour")
    return listOf(
        ProfessionalTextGroup("原局神煞", natal.map(::line)),
        ProfessionalTextGroup(
            "岁运神煞",
            transitKeys.mapNotNull { key -> columns.firstOrNull { it.key == key }?.let(::line) },
        ),
    )
}

private data class TrineShenSha(val horse: Char, val peach: Char, val canopy: Char, val general: Char)

private val STEM_COMBINES = setOf(setOf('甲', '己'), setOf('乙', '庚'), setOf('丙', '辛'), setOf('丁', '壬'), setOf('戊', '癸'))
private val STEM_CLASHES = setOf(setOf('甲', '庚'), setOf('乙', '辛'), setOf('丙', '壬'), setOf('丁', '癸'))
private val BRANCH_COMBINES = setOf(setOf('子', '丑'), setOf('寅', '亥'), setOf('卯', '戌'), setOf('辰', '酉'), setOf('巳', '申'), setOf('午', '未'))
private val BRANCH_CLASHES = setOf(setOf('子', '午'), setOf('丑', '未'), setOf('寅', '申'), setOf('卯', '酉'), setOf('辰', '戌'), setOf('巳', '亥'))
private val BRANCH_HARMS = setOf(setOf('子', '未'), setOf('丑', '午'), setOf('寅', '巳'), setOf('卯', '辰'), setOf('申', '亥'), setOf('酉', '戌'))
private val BRANCH_PUNISH_PAIRS = setOf(setOf('子', '卯'), setOf('寅', '巳'), setOf('巳', '申'), setOf('申', '寅'), setOf('丑', '戌'), setOf('戌', '未'), setOf('未', '丑'))
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
private val TIAN_YI = mapOf(
    '甲' to setOf('丑', '未'), '戊' to setOf('丑', '未'), '庚' to setOf('丑', '未'),
    '乙' to setOf('子', '申'), '己' to setOf('子', '申'),
    '丙' to setOf('亥', '酉'), '丁' to setOf('亥', '酉'),
    '壬' to setOf('卯', '巳'), '癸' to setOf('卯', '巳'),
    '辛' to setOf('寅', '午'),
)
private val WEN_CHANG = mapOf('甲' to '巳', '乙' to '午', '丙' to '申', '戊' to '申', '丁' to '酉', '己' to '酉', '庚' to '亥', '辛' to '子', '壬' to '寅', '癸' to '卯')
private val LU_SHEN = mapOf('甲' to '寅', '乙' to '卯', '丙' to '巳', '戊' to '巳', '丁' to '午', '己' to '午', '庚' to '申', '辛' to '酉', '壬' to '亥', '癸' to '子')
private val YANG_REN = mapOf('甲' to '卯', '乙' to '寅', '丙' to '午', '戊' to '午', '丁' to '巳', '己' to '巳', '庚' to '酉', '辛' to '申', '壬' to '子', '癸' to '亥')
private val TRINE_ROOT = mapOf(
    '申' to TrineShenSha('寅', '酉', '辰', '子'), '子' to TrineShenSha('寅', '酉', '辰', '子'), '辰' to TrineShenSha('寅', '酉', '辰', '子'),
    '寅' to TrineShenSha('申', '卯', '戌', '午'), '午' to TrineShenSha('申', '卯', '戌', '午'), '戌' to TrineShenSha('申', '卯', '戌', '午'),
    '巳' to TrineShenSha('亥', '午', '丑', '酉'), '酉' to TrineShenSha('亥', '午', '丑', '酉'), '丑' to TrineShenSha('亥', '午', '丑', '酉'),
    '亥' to TrineShenSha('巳', '子', '未', '卯'), '卯' to TrineShenSha('巳', '子', '未', '卯'), '未' to TrineShenSha('巳', '子', '未', '卯'),
)

private const val DETAIL_RULE_VERSION = "professional-detail-relations-shensha-v1"
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
