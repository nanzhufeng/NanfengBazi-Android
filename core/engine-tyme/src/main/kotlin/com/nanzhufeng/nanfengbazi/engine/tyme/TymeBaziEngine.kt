package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.BaziStructuralProfileAnalyzer
import com.nanzhufeng.nanfengbazi.domain.BirthTimeZoneResolution
import com.nanzhufeng.nanfengbazi.domain.BirthTimeZoneResolver
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.TrueSolarTimeCalculator
import com.nanzhufeng.nanfengbazi.domain.TrueSolarTimeRequest
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.model.CalendarConversionResult
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CalculationWarning
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.DECADE_FORTUNE_COUNT
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.HiddenStemDetail
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.LuckStartRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.TrueSolarTimeApplicationRule
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.YearBoundaryRule
import com.nanzhufeng.nanfengbazi.domain.model.MonthBoundaryRule
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.PillarDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermType
import com.tyme.eightchar.ChildLimit
import com.tyme.eightchar.EightChar
import com.tyme.eightchar.provider.impl.China95ChildLimitProvider
import com.tyme.eightchar.provider.impl.DefaultChildLimitProvider
import com.tyme.enums.Gender
import com.tyme.lunar.LunarHour
import com.tyme.sixtycycle.SixtyCycle
import com.tyme.sixtycycle.SixtyCycleYear
import com.tyme.solar.SolarTime
import com.nanzhufeng.nanfengbazi.solar.SpaTrueSolarTimeCalculator
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TymeBaziEngine(
    private val clock: Clock = Clock.systemUTC(),
    private val trueSolarTimeCalculator: TrueSolarTimeCalculator =
        SpaTrueSolarTimeCalculator(),
) : BaziEngine {
    override suspend fun calculate(
        input: BirthInput,
        profile: CalculationProfile,
    ): CalculationResult {
        validateSupported(input, profile)
        val resolved = resolveCalendar(input.calendarInput)
        val normalizedInput = resolveTimeZone(input, resolved.solar.toDomain())
        val trueSolarEvidence = calculateTrueSolarTimeIfNeeded(
            input = normalizedInput,
            civilSolarDateTime = resolved.solar.toDomain(),
            profile = profile,
        )

        return ChildLimitProviderGuard.withProvider(profile.luckStartRule) {
            val solar = resolved.solar
            val lunarHour = resolved.lunarHour
            val civilEightChar = lunarHour.resolveEightChar(profile.ratHourRule)
            val eightChar = trueSolarEvidence?.let { evidence ->
                val correctedEightChar = evidence.trueSolarDateTime
                    .toTyme()
                    .lunarHour
                    .resolveEightChar(profile.ratHourRule)
                EightChar(
                    civilEightChar.year,
                    civilEightChar.month,
                    correctedEightChar.day,
                    correctedEightChar.hour,
                )
            } ?: civilEightChar
            val childLimit = ChildLimit.fromSolarTime(
                solar,
                when (input.sexForFortuneDirection) {
                    SexForFortuneDirection.WOMAN -> Gender.WOMAN
                    SexForFortuneDirection.MAN -> Gender.MAN
                },
            )
            val startDecade = childLimit.startDecadeFortune
            val firstDecadeStart = childLimit.endTime.toDomain()
            val decades = generateSequence(startDecade) { it.next(1) }
                .take(DECADE_FORTUNE_COUNT)
                .mapIndexed { index, fortune ->
                    DecadeFortune(
                        name = fortune.name,
                        startAge = fortune.startAge,
                        endAge = fortune.endAge,
                        startYear = fortune.startSixtyCycleYear.year,
                        endYear = fortune.endSixtyCycleYear.year,
                        startAt = firstDecadeStart.plusYears(index * 10L),
                        endAtExclusive = firstDecadeStart.plusYears((index + 1) * 10L),
                    )
                }
                .toList()
            val annuals = buildAnnualFortunes(
                birthYear = solar.year,
                decades = decades,
            )
            val fourPillars = FourPillars(
                year = eightChar.year.name,
                month = eightChar.month.name,
                day = eightChar.day.name,
                hour = eightChar.hour.name,
            )
            val basicChartDetails = buildBasicChartDetails(
                civilSolarTime = solar,
                eightChar = eightChar,
            )

            CalculationResult(
                normalizedInput = normalizedInput,
                profile = profile,
                fourPillars = fourPillars,
                ownSign = eightChar.ownSign.name,
                bodySign = eightChar.bodySign.name,
                fetalOrigin = eightChar.fetalOrigin.name,
                fetalBreath = eightChar.fetalBreath.name,
                fortuneStart = FortuneStart(
                    direction = if (childLimit.isForward) {
                        FortuneDirection.FORWARD
                    } else {
                        FortuneDirection.BACKWARD
                    },
                    startAt = childLimit.startTime.toDomain(),
                    endAt = childLimit.endTime.toDomain(),
                    years = childLimit.yearCount,
                    months = childLimit.monthCount,
                    days = childLimit.dayCount,
                    hours = childLimit.hourCount,
                    minutes = childLimit.minuteCount,
                ),
                decadeFortunes = decades,
                annualFortunes = annuals,
                evidence = CalculationEvidence(
                    engineName = "Tyme4j",
                    engineVersion = CalculationProfile.TYME_ENGINE_VERSION,
                    ruleVersion = profile.ruleVersion,
                    calculatedAt = clock.instant(),
                ),
                calendarConversion = CalendarConversionResult(
                    inputCalendarSystem = resolved.inputCalendarSystem,
                    solarDateTime = solar.toDomain(),
                    lunarDateTime = lunarHour.toDomain(),
                ),
                trueSolarTimeEvidence = trueSolarEvidence,
                basicChartDetails = basicChartDetails,
                structuralProfile = BaziStructuralProfileAnalyzer.analyze(
                    fourPillars = fourPillars,
                    basicChartDetails = basicChartDetails,
                ),
                warnings = buildList {
                    if (trueSolarEvidence != null) {
                        add(
                            CalculationWarning(
                                code = "TRUE_SOLAR_BOUNDARY_RULE_PROVISIONAL",
                                message = "真太阳时边界暂按公开参考口径计算；问真跨界样本" +
                                    "尚未验收，边界命盘请保留复核。",
                            ),
                        )
                    }
                    if (trueSolarEvidence?.crossesDate == true) {
                        add(
                            CalculationWarning(
                                code = "TRUE_SOLAR_CROSSES_DATE",
                                message = "真太阳时校正跨越当地日期，日柱和时柱已按校正后时间计算。",
                            ),
                        )
                    }
                    if (trueSolarEvidence?.crossesDoubleHour == true) {
                        add(
                            CalculationWarning(
                                code = "TRUE_SOLAR_CROSSES_DOUBLE_HOUR",
                                message = "真太阳时校正跨越时辰边界，时柱已按校正后时间计算。",
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun buildAnnualFortunes(
        birthYear: Int,
        decades: List<DecadeFortune>,
    ): List<AnnualFortune> {
        val endYear = decades.lastOrNull()?.endYear ?: birthYear
        return (birthYear..endYear).map { year ->
            val decadeIndex = decades.indexOfFirst { year in it.startYear..it.endYear }
                .takeIf { it >= 0 }
            AnnualFortune(
                name = SixtyCycleYear.fromYear(year).sixtyCycle.name,
                calendarYear = year,
                nominalAge = year - birthYear + 1,
                decadeIndex = decadeIndex?.plus(1),
                decadeName = decadeIndex?.let(decades::get)?.name,
            )
        }
    }

    private fun buildBasicChartDetails(
        civilSolarTime: SolarTime,
        eightChar: EightChar,
    ): BasicChartDetails {
        val dayMaster = eightChar.day.heavenStem
        val pillars = listOf(
            PillarPosition.YEAR to eightChar.year,
            PillarPosition.MONTH to eightChar.month,
            PillarPosition.DAY to eightChar.day,
            PillarPosition.HOUR to eightChar.hour,
        ).map { (position, pillar) ->
            pillar.toDetail(position, dayMaster)
        }
        val previousTerm = civilSolarTime.term
        val nextTerm = previousTerm.next(1)
        val previousJie = if (previousTerm.isJie) previousTerm else previousTerm.next(-1)
        val nextJie = previousJie.next(2)

        return BasicChartDetails(
            zodiac = eightChar.year.earthBranch.zodiac.name,
            westernZodiac = civilSolarTime.solarDay.constellation.name,
            dayMaster = dayMaster.name,
            pillars = pillars,
            previousSolarTerm = previousTerm.toDomainPoint(),
            nextSolarTerm = nextTerm.toDomainPoint(),
            previousJie = previousJie.toDomainPoint(),
            nextJie = nextJie.toDomainPoint(),
        )
    }

    private fun calculateTrueSolarTimeIfNeeded(
        input: BirthInput,
        civilSolarDateTime: CivilDateTime,
        profile: CalculationProfile,
    ) = if (profile.solarTimeMode == SolarTimeMode.TRUE_SOLAR_TIME) {
        val longitude = requireNotNull(input.longitude) {
            "启用真太阳时必须提供出生地经度和纬度。"
        }
        val latitude = requireNotNull(input.latitude) {
            "启用真太阳时必须提供出生地经度和纬度。"
        }
        val resolvedOffset = requireNotNull(input.resolvedUtcOffsetSeconds) {
            "启用真太阳时前必须先解析出生时区的 UTC offset。"
        }
        trueSolarTimeCalculator.calculate(
            TrueSolarTimeRequest(
                civilDateTime = civilSolarDateTime,
                timeZoneId = input.timeZoneId,
                resolvedUtcOffsetSeconds = resolvedOffset,
                longitude = longitude,
                latitude = latitude,
            ),
        ).also {
            require(it.applicationRule == profile.trueSolarTimeApplicationRule) {
                "真太阳时计算器与计算配置的作用规则不一致。"
            }
        }
    } else {
        null
    }

    private fun resolveTimeZone(
        input: BirthInput,
        solarDateTime: CivilDateTime,
    ): BirthInput = when (
        val resolution = BirthTimeZoneResolver.resolve(
            localDateTime = solarDateTime,
            timeZoneId = input.timeZoneId,
            selectedUtcOffsetSeconds = input.resolvedUtcOffsetSeconds,
        )
    ) {
        is BirthTimeZoneResolution.Resolved -> input.copy(
            resolvedUtcOffsetSeconds = resolution.utcOffsetSeconds,
            timeZoneDataVersion = resolution.timeZoneDataVersion,
        )
        is BirthTimeZoneResolution.ChoiceRequired -> throw TimeZoneChoiceRequiredException(
            timeZoneId = input.timeZoneId,
            validUtcOffsetSeconds = resolution.validUtcOffsetSeconds,
            timeZoneDataVersion = resolution.timeZoneDataVersion,
        )
        is BirthTimeZoneResolution.Nonexistent -> throw IllegalArgumentException(
            "该出生时间在 ${input.timeZoneId} 的夏令时切换中不存在；" +
                "当地时间从 ${resolution.gapStartsAt.display()} 跳到 " +
                "${resolution.gapEndsAt.display()}，请核对原始时间。",
        )
        is BirthTimeZoneResolution.InvalidZone -> throw IllegalArgumentException(
            "时区标识 ${resolution.timeZoneId} 无效，请填写 IANA 时区，例如 Asia/Shanghai。",
        )
        is BirthTimeZoneResolution.InvalidOffsetSelection -> throw IllegalArgumentException(
            "所选 UTC offset 与 ${input.timeZoneId} 在该出生时间的规则不一致，请重新确认。",
        )
    }

    private fun validateSupported(
        input: BirthInput,
        profile: CalculationProfile,
    ) {
        val minuteAndSecond = when (val calendar = input.calendarInput) {
            is BirthCalendarInput.Solar ->
                calendar.dateTime.minute to calendar.dateTime.second
            is BirthCalendarInput.Lunar ->
                calendar.dateTime.minute to calendar.dateTime.second
        }
        when (input.timePrecision) {
            TimePrecision.EXACT_TO_SECOND -> Unit
            TimePrecision.EXACT_TO_MINUTE,
            TimePrecision.APPROXIMATE,
            -> require(minuteAndSecond.second == 0) {
                "当前时间精度要求秒数为 0。"
            }
            TimePrecision.HOUR_ONLY,
            TimePrecision.DOUBLE_HOUR_ONLY,
            -> require(minuteAndSecond.first == 0 && minuteAndSecond.second == 0) {
                "当前时间精度要求分钟和秒数都为 0。"
            }
            TimePrecision.UNKNOWN -> throw IllegalArgumentException(
                "时辰未知不能生成唯一命盘，请先提供可计算的候选时间。",
            )
        }
        require(
            input.useTrueSolarTime ==
                (profile.solarTimeMode == SolarTimeMode.TRUE_SOLAR_TIME),
        ) {
            "出生资料与计算配置的真太阳时选项不一致。"
        }
        require(profile.engineVersion == CalculationProfile.TYME_ENGINE_VERSION) {
            "配置要求的引擎版本与当前 Tyme4j 适配器不一致"
        }
        require(profile.yearBoundaryRule == YearBoundaryRule.SPRING_EXACT)
        require(profile.monthBoundaryRule == MonthBoundaryRule.SOLAR_TERM_EXACT)
        require(
            profile.trueSolarTimeApplicationRule ==
                TrueSolarTimeApplicationRule
                    .CIVIL_YEAR_MONTH_TRUE_SOLAR_DAY_HOUR_PROVISIONAL_V1,
        )
    }

    private fun resolveCalendar(calendarInput: BirthCalendarInput): ResolvedCalendar =
        when (calendarInput) {
            is BirthCalendarInput.Solar -> {
                val solar = calendarInput.dateTime.toTyme()
                ResolvedCalendar(CalendarSystem.SOLAR, solar, solar.lunarHour)
            }
            is BirthCalendarInput.Lunar -> {
                val dateTime = calendarInput.dateTime
                val lunarHour = try {
                    LunarHour.fromYmdHms(
                        dateTime.year,
                        if (dateTime.isLeapMonth) -dateTime.month else dateTime.month,
                        dateTime.day,
                        dateTime.hour,
                        dateTime.minute,
                        dateTime.second,
                    )
                } catch (error: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "农历日期无效：该年份可能没有所选闰月，或日期超出当月天数。",
                        error,
                    )
                }
                ResolvedCalendar(CalendarSystem.LUNAR, lunarHour.solarTime, lunarHour)
            }
        }
}

private data class ResolvedCalendar(
    val inputCalendarSystem: CalendarSystem,
    val solar: SolarTime,
    val lunarHour: LunarHour,
)

/**
 * Tyme4j 的起运 provider 是进程级可变状态，只能在此保护器内切换。
 */
internal object ChildLimitProviderGuard {
    private val lock = ReentrantLock()

    fun <T> withProvider(
        rule: LuckStartRule,
        block: () -> T,
    ): T = lock.withLock {
        val previous = ChildLimit.provider
        ChildLimit.provider = when (rule) {
            LuckStartRule.TYME_DEFAULT -> DefaultChildLimitProvider()
            LuckStartRule.CHINA_95 -> China95ChildLimitProvider()
        }
        try {
            block()
        } finally {
            ChildLimit.provider = previous
        }
    }
}

private fun CivilDateTime.toTyme(): SolarTime =
    SolarTime.fromYmdHms(year, month, day, hour, minute, second)

private fun CivilDateTime.plusYears(years: Long): CivilDateTime =
    LocalDateTime.of(year, month, day, hour, minute, second)
        .plusYears(years)
        .let {
            CivilDateTime(
                year = it.year,
                month = it.monthValue,
                day = it.dayOfMonth,
                hour = it.hour,
                minute = it.minute,
                second = it.second,
            )
        }

private fun SolarTime.toDomain(): CivilDateTime = CivilDateTime(
    year = year,
    month = month,
    day = day,
    hour = hour,
    minute = minute,
    second = second,
)

private fun CivilDateTime.display(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun SixtyCycle.toDetail(
    position: PillarPosition,
    dayMaster: com.tyme.sixtycycle.HeavenStem,
): PillarDetail = PillarDetail(
    position = position,
    name = name,
    heavenStem = heavenStem.name,
    earthBranch = earthBranch.name,
    heavenStemElement = heavenStem.element.name,
    earthBranchElement = earthBranch.element.name,
    primaryTenGod = dayMaster.getTenStar(heavenStem).name,
    hiddenStems = earthBranch.hideHeavenStems.map { hidden ->
        HiddenStemDetail(
            heavenStem = hidden.heavenStem.name,
            type = hidden.type.toString(),
            tenGod = dayMaster.getTenStar(hidden.heavenStem).name,
            element = hidden.heavenStem.element.name,
        )
    },
    terrain = dayMaster.getTerrain(earthBranch).name,
    selfSittingTerrain = heavenStem.getTerrain(earthBranch).name,
    voidEarthBranches = extraEarthBranches.map { it.name },
    naYin = sound.name,
)

private fun com.tyme.solar.SolarTerm.toDomainPoint(): SolarTermPoint =
    SolarTermPoint(
        name = name,
        type = if (isJie) SolarTermType.JIE else SolarTermType.QI,
        at = julianDay.solarTime.toDomain(),
    )

private fun LunarHour.toDomain(): LunarDateTime {
    val month = lunarDay.lunarMonth
    return LunarDateTime(
        year = month.lunarYear.year,
        month = kotlin.math.abs(month.monthWithLeap),
        day = day,
        hour = hour,
        minute = minute,
        second = second,
        isLeapMonth = month.isLeap,
    )
}
