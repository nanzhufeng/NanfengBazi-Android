package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.YearMonthBoundaryAmbiguousException
import com.nanzhufeng.nanfengbazi.domain.model.*
import com.tyme.sixtycycle.SixtyCycle
import com.tyme.sixtycycle.SixtyCycleYear
import com.tyme.solar.SolarTerm
import com.tyme.solar.SolarTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId

/** Tyme 1.5.1 qiAccurate2/JD expresses China civil time, not a UTC Julian date. */
internal object TymeSolarYearMonthResolver {
    const val RULE_VERSION = CalculationProfile.DEFAULT_RULE_VERSION
    val CHINA_OFFSET: ZoneOffset = ZoneOffset.ofHours(8)
    private val terms = object : LinkedHashMap<Int, List<Boundary>>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<Boundary>>?) = size > 64
    }

    data class Boundary(val point: SolarTermPoint, val instant: Instant)
    data class Resolved(
        val year: SixtyCycle,
        val month: SixtyCycle,
        val previous: Boundary,
        val next: Boundary,
        val evidence: SolarYearMonthEvidence,
    )

    fun resolve(
        civil: SolarTime,
        offsetSeconds: Int = CHINA_OFFSET.totalSeconds,
        precision: TimePrecision = TimePrecision.EXACT_TO_SECOND,
        timeZoneId: String? = null,
    ): Resolved {
        val local = civil.localDateTime()
        val offset = ZoneOffset.ofTotalSeconds(offsetSeconds)
        val instant = local.toInstant(offset)
        val at = at(instant, offsetSeconds)
        // An exact second is the project's point-time input. Other precision values
        // describe half-open ranges; an unspecified approximation covers its civil day.
        val range = when (precision) {
            TimePrecision.EXACT_TO_SECOND -> null
            TimePrecision.EXACT_TO_MINUTE -> local.withSecond(0) to local.withSecond(0).plusMinutes(1)
            TimePrecision.HOUR_ONLY -> local.withMinute(0).withSecond(0) to local.withMinute(0).withSecond(0).plusHours(1)
            TimePrecision.DOUBLE_HOUR_ONLY -> {
                val start = local.withMinute(0).withSecond(0).minusHours(if (local.hour % 2 == 0) 1 else 0)
                start to start.plusHours(2)
            }
            TimePrecision.APPROXIMATE, TimePrecision.UNKNOWN -> local.toLocalDate().atStartOfDay().let { it to it.plusDays(1) }
        }
        if (range != null) {
            fun endpoint(time: LocalDateTime, end: Boolean): Instant {
                val zone = timeZoneId?.let(ZoneId::of) ?: return time.toInstant(offset)
                val valid = zone.rules.getValidOffsets(time)
                if (valid.isEmpty()) return requireNotNull(zone.rules.getTransition(time)).instant
                if (!end && time == local && offset in valid) return time.toInstant(offset)
                val instants = valid.map(time::toInstant)
                return if (end) instants.max() else instants.min()
            }
            val start = endpoint(range.first, false)
            val end = endpoint(range.second, true)
            val first = at(start, offsetSeconds)
            if (first.next.instant < end) {
                val second = at(first.next.instant, offsetSeconds)
                throw YearMonthBoundaryAmbiguousException(
                    first.next.point,
                    listOf(first.year.name, second.year.name).distinct(),
                    listOf(first.month.name, second.month.name).distinct(),
                    listOf(first.year.earthBranch.zodiac.name, second.year.earthBranch.zodiac.name).distinct(),
                )
            }
        }
        return at
    }

    fun at(instant: Instant, offsetSeconds: Int = CHINA_OFFSET.totalSeconds): Resolved {
        val china = LocalDateTime.ofInstant(instant, CHINA_OFFSET)
        val spring = jieForSolarYear(china.year).first()
        val effectiveYear = china.year - if (instant < spring.instant) 1 else 0
        val jie = jieForSolarYear(effectiveYear)
        val monthIndex = (0..11).last { instant >= jie[it].instant }
        val year = SixtyCycleYear.fromYear(effectiveYear).sixtyCycle
        val tigerStem = (year.heavenStem.index % 5 * 2 + 2) % 10
        val month = SixtyCycle.fromName("${"甲乙丙丁戊己庚辛壬癸"[(tigerStem + monthIndex) % 10]}${"子丑寅卯辰巳午未申酉戌亥"[(2 + monthIndex) % 12]}")
        return Resolved(
            year, month, jie[monthIndex], jie[monthIndex + 1],
            SolarYearMonthEvidence(
                ruleVersion = RULE_VERSION,
                providerId = "tyme4j-${CalculationProfile.TYME_ENGINE_VERSION}:qiAccurate2",
                definitionId = "APPARENT_GEOCENTRIC_TROPICAL_LONGITUDE_315",
                birthInstantUtc = instant.toString(),
                birthUtcOffsetSeconds = offsetSeconds,
                liChunInstantUtc = spring.instant.toString(),
                effectiveSolarYear = effectiveYear,
                previousJie = jie[monthIndex].point,
                nextJie = jie[monthIndex + 1].point,
                precisionNote = "交节采用Tyme4j 1.5.1的北京时间秒级输出；模型差异另行核验，不声明毫秒准确度。",
            ),
        )
    }

    fun jieForSolarYear(year: Int): List<Boundary> = synchronized(terms) {
        terms.getOrPut(year) {
            val spring = SolarTerm.fromName(year, "立春")
            (0..12).map { index ->
                val term = spring.next(index * 2)
                val time = term.julianDay.solarTime
                Boundary(
                    SolarTermPoint(term.name, SolarTermType.JIE, time.domainDateTime()),
                    time.localDateTime().toInstant(CHINA_OFFSET),
                )
            }
        }
    }
}

internal fun SolarTime.localDateTime(): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute, second)
internal fun SolarTime.domainDateTime() = CivilDateTime(year, month, day, hour, minute, second)
internal fun LocalDateTime.tymeSolarTime(): SolarTime = SolarTime.fromYmdHms(year, monthValue, dayOfMonth, hour, minute, second)
