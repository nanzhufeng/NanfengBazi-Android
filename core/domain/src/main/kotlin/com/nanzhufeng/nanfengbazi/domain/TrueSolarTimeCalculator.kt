package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.TrueSolarTimeEvidence

data class TrueSolarTimeRequest(
    val civilDateTime: CivilDateTime,
    val timeZoneId: String,
    val resolvedUtcOffsetSeconds: Int,
    val longitude: Double,
    val latitude: Double,
)

/**
 * 真太阳时校正的唯一领域入口。排盘引擎只消费带版本的校正证据，
 * 不直接依赖具体天文算法库。
 */
fun interface TrueSolarTimeCalculator {
    fun calculate(request: TrueSolarTimeRequest): TrueSolarTimeEvidence
}
