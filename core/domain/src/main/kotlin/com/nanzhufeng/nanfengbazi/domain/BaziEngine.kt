package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult

/**
 * 南枫八字唯一计算入口。调用方不得直接依赖具体历法库。
 */
fun interface BaziEngine {
    suspend fun calculate(
        input: BirthInput,
        profile: CalculationProfile,
    ): CalculationResult
}

