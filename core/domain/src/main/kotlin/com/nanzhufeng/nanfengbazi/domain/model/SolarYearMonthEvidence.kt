package com.nanzhufeng.nanfengbazi.domain.model

import kotlinx.serialization.Serializable

/** Exact-event evidence. ISO UTC strings avoid depending on the device's display zone. */
@Serializable
data class SolarYearMonthEvidence(
    val ruleVersion: String,
    val providerId: String,
    val definitionId: String,
    val birthInstantUtc: String,
    val birthUtcOffsetSeconds: Int,
    val liChunInstantUtc: String,
    val effectiveSolarYear: Int,
    val previousJie: SolarTermPoint,
    val nextJie: SolarTermPoint,
    val precisionNote: String,
)
