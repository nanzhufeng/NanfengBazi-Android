package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.SortedMap
import java.util.concurrent.ConcurrentHashMap

sealed interface BirthTimeZoneResolution {
    data class Resolved(
        val utcOffsetSeconds: Int,
        val timeZoneDataVersion: String,
    ) : BirthTimeZoneResolution

    data class ChoiceRequired(
        val validUtcOffsetSeconds: List<Int>,
        val timeZoneDataVersion: String,
    ) : BirthTimeZoneResolution

    data class Nonexistent(
        val gapStartsAt: CivilDateTime,
        val gapEndsAt: CivilDateTime,
        val timeZoneDataVersion: String,
    ) : BirthTimeZoneResolution

    data class InvalidZone(val timeZoneId: String) : BirthTimeZoneResolution

    data class InvalidOffsetSelection(
        val selectedUtcOffsetSeconds: Int,
        val validUtcOffsetSeconds: List<Int>,
        val timeZoneDataVersion: String,
    ) : BirthTimeZoneResolution
}

/**
 * 出生民用时到 UTC offset 的唯一解析入口。
 *
 * 排盘仍使用用户输入的本地墙上时间；这里仅负责证明该时间在指定 IANA 时区下
 * 对应哪个 offset，并把平台时区数据库版本写入可审计快照。
 */
object BirthTimeZoneResolver {
    private val availableZoneIds = ZoneId.getAvailableZoneIds()
    private val dataVersions = ConcurrentHashMap<String, String>()
    fun resolve(
        localDateTime: CivilDateTime,
        timeZoneId: String,
        selectedUtcOffsetSeconds: Int? = null,
    ): BirthTimeZoneResolution {
        if (
            timeZoneId !in availableZoneIds &&
            timeZoneId !in FIXED_REGION_IDS
        ) {
            return BirthTimeZoneResolution.InvalidZone(timeZoneId)
        }
        val zone = try {
            ZoneId.of(timeZoneId)
        } catch (_: DateTimeException) {
            return BirthTimeZoneResolution.InvalidZone(timeZoneId)
        }
        val version = timeZoneDataVersion(zone)
        val local = localDateTime.toJavaLocalDateTime()
        val offsets = zone.rules.getValidOffsets(local)
            .map { it.totalSeconds }

        if (offsets.isEmpty()) {
            val transition = requireNotNull(zone.rules.getTransition(local))
            return BirthTimeZoneResolution.Nonexistent(
                gapStartsAt = transition.dateTimeBefore.toDomain(),
                gapEndsAt = transition.dateTimeAfter.toDomain(),
                timeZoneDataVersion = version,
            )
        }
        if (offsets.size > 1 && selectedUtcOffsetSeconds == null) {
            return BirthTimeZoneResolution.ChoiceRequired(
                validUtcOffsetSeconds = offsets,
                timeZoneDataVersion = version,
            )
        }
        val selected = selectedUtcOffsetSeconds ?: offsets.single()
        if (selected !in offsets) {
            return BirthTimeZoneResolution.InvalidOffsetSelection(
                selectedUtcOffsetSeconds = selected,
                validUtcOffsetSeconds = offsets,
                timeZoneDataVersion = version,
            )
        }
        return BirthTimeZoneResolution.Resolved(
            utcOffsetSeconds = selected,
            timeZoneDataVersion = version,
        )
    }

    private fun timeZoneDataVersion(zoneId: ZoneId): String {
        if (zoneId.rules.isFixedOffset) return "fixed-offset:v1"
        return dataVersions.getOrPut(zoneId.id) {
            // ZoneRulesProvider is a JVM API absent from Android's public API.
            // Optional reflection keeps the domain module usable on both runtimes
            // without shipping a hard reference to an unsupported Android class.
            val providerVersion = runCatching {
                val versions = Class.forName("java.time.zone.ZoneRulesProvider")
                    .getMethod("getVersions", String::class.java)
                    .invoke(null, zoneId.id) as SortedMap<*, *>
                versions.lastKey().toString()
            }.getOrNull()
            if (providerVersion != null) {
                "tzdb:$providerVersion"
            } else {
                val icuVersion = runCatching {
                    Class.forName("android.icu.util.TimeZone")
                        .getMethod("getTZDataVersion").invoke(null) as String
                }.getOrNull()
                icuVersion?.let { "platform-icu-tzdb:$it" } ?: "platform-tzdb:unknown"
            }
        }
    }

    private val FIXED_REGION_IDS = setOf("UTC", "GMT", "UT")
}

class TimeZoneChoiceRequiredException(
    val timeZoneId: String,
    val validUtcOffsetSeconds: List<Int>,
    val timeZoneDataVersion: String,
) : IllegalArgumentException(
    "该出生时间在 $timeZoneId 出现两次，请确认实际 UTC offset。",
)

private fun CivilDateTime.toJavaLocalDateTime(): LocalDateTime = LocalDateTime.of(
    year,
    month,
    day,
    hour,
    minute,
    second,
)

private fun LocalDateTime.toDomain(): CivilDateTime = CivilDateTime(
    year = year,
    month = monthValue,
    day = dayOfMonth,
    hour = hour,
    minute = minute,
    second = second,
)
