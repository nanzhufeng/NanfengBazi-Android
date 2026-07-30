package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarConversionResult
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.LuckStartRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.YearBoundaryRule
import com.nanzhufeng.nanfengbazi.domain.model.MonthBoundaryRule
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.tyme.eightchar.ChildLimit
import com.tyme.eightchar.provider.impl.China95ChildLimitProvider
import com.tyme.eightchar.provider.impl.DefaultChildLimitProvider
import com.tyme.enums.Gender
import com.tyme.lunar.LunarHour
import com.tyme.solar.SolarTime
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TymeBaziEngine(
    private val clock: Clock = Clock.systemUTC(),
) : BaziEngine {
    override suspend fun calculate(
        input: BirthInput,
        profile: CalculationProfile,
    ): CalculationResult {
        validateSupported(input, profile)
        val resolved = resolveCalendar(input.calendarInput)

        return ChildLimitProviderGuard.withProvider(profile.luckStartRule) {
            val solar = resolved.solar
            val lunarHour = resolved.lunarHour
            val eightChar = lunarHour.eightChar
            val childLimit = ChildLimit.fromSolarTime(
                solar,
                when (input.sexForFortuneDirection) {
                    SexForFortuneDirection.WOMAN -> Gender.WOMAN
                    SexForFortuneDirection.MAN -> Gender.MAN
                },
            )
            val startDecade = childLimit.startDecadeFortune
            val decades = generateSequence(startDecade) { it.next(1) }
                .take(8)
                .map {
                    DecadeFortune(
                        name = it.name,
                        startAge = it.startAge,
                        endAge = it.endAge,
                        startYear = it.startSixtyCycleYear.year,
                        endYear = it.endSixtyCycleYear.year,
                    )
                }
                .toList()

            CalculationResult(
                normalizedInput = input,
                profile = profile,
                fourPillars = FourPillars(
                    year = eightChar.year.name,
                    month = eightChar.month.name,
                    day = eightChar.day.name,
                    hour = eightChar.hour.name,
                ),
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
            )
        }
    }

    private fun validateSupported(
        input: BirthInput,
        profile: CalculationProfile,
    ) {
        require(!input.useTrueSolarTime && profile.solarTimeMode == SolarTimeMode.CIVIL_TIME) {
            "当前版本尚未实现真太阳时校正"
        }
        require(profile.engineVersion == CalculationProfile.TYME_ENGINE_VERSION) {
            "配置要求的引擎版本与当前 Tyme4j 适配器不一致"
        }
        require(profile.yearBoundaryRule == YearBoundaryRule.SPRING_EXACT)
        require(profile.monthBoundaryRule == MonthBoundaryRule.SOLAR_TERM_EXACT)
        require(profile.ratHourRule == RatHourRule.TYME_DEFAULT)
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

private fun SolarTime.toDomain(): CivilDateTime = CivilDateTime(
    year = year,
    month = month,
    day = day,
    hour = hour,
    minute = minute,
    second = second,
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
