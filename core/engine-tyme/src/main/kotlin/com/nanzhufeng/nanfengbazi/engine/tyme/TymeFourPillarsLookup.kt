package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.BirthTimeZoneResolution
import com.nanzhufeng.nanfengbazi.domain.BirthTimeZoneResolver
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookup
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupCandidate
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupContract
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupError
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupEvidence
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupQuery
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupResult
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.tyme.eightchar.EightChar
import com.tyme.eightchar.provider.EightCharProvider
import com.tyme.eightchar.provider.impl.DefaultEightCharProvider
import com.tyme.eightchar.provider.impl.LunarSect2EightCharProvider
import com.tyme.lunar.LunarHour
import com.tyme.solar.SolarTime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.CancellationException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TymeFourPillarsLookup(
    private val baziEngine: BaziEngine = TymeBaziEngine(),
) : FourPillarsLookup {
    override suspend fun search(query: FourPillarsLookupQuery): FourPillarsLookupResult {
        val normalized = FourPillarsLookupContract.normalize(query)
        FourPillarsLookupContract.validate(normalized)?.let {
            return FourPillarsLookupResult.Failed(it)
        }

        val target = try {
            with(normalized.fourPillars) {
                EightChar(year, month, day, hour)
            }
        } catch (_: IllegalArgumentException) {
            return FourPillarsLookupResult.Failed(
                FourPillarsLookupError.EngineUnavailable,
            )
        }
        val rawCandidates = try {
            LunarHourProviderGuard.withProvider(normalized.ratHourRule) {
                target.getSolarTimes(normalized.startYear, normalized.endYear)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return FourPillarsLookupResult.Failed(
                FourPillarsLookupError.EngineUnavailable,
            )
        }

        val profile = CalculationProfile.tymeDefault(
            ratHourRule = normalized.ratHourRule,
        )
        val candidates = mutableListOf<FourPillarsLookupCandidate>()
        for (rawCandidate in rawCandidates) {
            val civilDateTime = rawCandidate.toDomain()
            val timeZoneVariants = civilDateTime.resolveTimeZoneVariants(
                normalized.timeZoneId,
            )
            for (variant in timeZoneVariants) {
                val result = try {
                    baziEngine.calculate(
                        input = BirthInput(
                            calendarInput = BirthCalendarInput.Solar(civilDateTime),
                            sexForFortuneDirection = SexForFortuneDirection.MAN,
                            timePrecision = TimePrecision.EXACT_TO_SECOND,
                            timeZoneId = normalized.timeZoneId,
                            resolvedUtcOffsetSeconds = variant.utcOffsetSeconds,
                            timeZoneDataVersion = variant.timeZoneDataVersion,
                            locationName = "四柱反查民用时候选",
                            useTrueSolarTime = false,
                            timeSourceType = TimeSourceType.UNKNOWN,
                        ),
                        profile = profile,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    return FourPillarsLookupResult.Failed(
                        FourPillarsLookupError.EngineUnavailable,
                    )
                }
                if (result.fourPillars != normalized.fourPillars) continue
                candidates += FourPillarsLookupCandidate(
                    civilDateTime = civilDateTime,
                    timeZoneId = normalized.timeZoneId,
                    resolvedUtcOffsetSeconds = variant.utcOffsetSeconds,
                    timeZoneDataVersion = variant.timeZoneDataVersion,
                    instant = civilDateTime.toInstant(variant.utcOffsetSeconds),
                    fourPillars = result.fourPillars,
                    ratHourRule = normalized.ratHourRule,
                    engineVersion = result.evidence.engineVersion,
                    ruleVersion = result.evidence.ruleVersion,
                )
            }
        }
        return FourPillarsLookupResult.Completed(
            query = normalized,
            candidates = candidates
                .distinctBy { it.civilDateTime to it.resolvedUtcOffsetSeconds }
                .sortedBy { it.instant },
            evidence = FourPillarsLookupEvidence(
                engineName = "Tyme4j",
                engineVersion = CalculationProfile.TYME_ENGINE_VERSION,
                ruleVersion = profile.ruleVersion,
                lookupMethod = "EightChar#getSolarTimes+BaziEngine.calculate",
            ),
        )
    }
}

/**
 * `EightChar#getSolarTimes` 内部固定读取进程级 `LunarHour.provider`。
 * 反查是项目中唯一允许访问该全局状态的路径，必须串行并恢复调用前状态。
 */
internal object LunarHourProviderGuard {
    private val lock = ReentrantLock()

    fun <T> withProvider(
        rule: RatHourRule,
        block: () -> T,
    ): T = lock.withLock {
        val previous = LunarHour.provider
        LunarHour.provider = rule.toProvider()
        try {
            block()
        } finally {
            LunarHour.provider = previous
        }
    }
}

private fun RatHourRule.toProvider(): EightCharProvider = when (this) {
    RatHourRule.TYME_DEFAULT -> DefaultEightCharProvider()
    RatHourRule.LATE_RAT_SAME_DAY -> LunarSect2EightCharProvider()
}

private data class TimeZoneVariant(
    val utcOffsetSeconds: Int,
    val timeZoneDataVersion: String,
)

private fun CivilDateTime.resolveTimeZoneVariants(timeZoneId: String): List<TimeZoneVariant> =
    when (
        val resolution = BirthTimeZoneResolver.resolve(
            localDateTime = this,
            timeZoneId = timeZoneId,
        )
    ) {
        is BirthTimeZoneResolution.Resolved -> listOf(
            TimeZoneVariant(
                utcOffsetSeconds = resolution.utcOffsetSeconds,
                timeZoneDataVersion = resolution.timeZoneDataVersion,
            ),
        )
        is BirthTimeZoneResolution.ChoiceRequired ->
            resolution.validUtcOffsetSeconds.map { offset ->
                TimeZoneVariant(
                    utcOffsetSeconds = offset,
                    timeZoneDataVersion = resolution.timeZoneDataVersion,
                )
            }
        is BirthTimeZoneResolution.Nonexistent -> emptyList()
        is BirthTimeZoneResolution.InvalidZone,
        is BirthTimeZoneResolution.InvalidOffsetSelection,
        -> emptyList()
    }

private fun SolarTime.toDomain(): CivilDateTime = CivilDateTime(
    year = year,
    month = month,
    day = day,
    hour = hour,
    minute = minute,
    second = second,
)

private fun CivilDateTime.toInstant(utcOffsetSeconds: Int) =
    LocalDateTime.of(year, month, day, hour, minute, second)
        .toInstant(ZoneOffset.ofTotalSeconds(utcOffsetSeconds))
