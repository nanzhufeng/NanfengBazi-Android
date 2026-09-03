package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars

/**
 * 原局八字表层的阴阳分布：只统计年、月、日、时四柱的天干地支，不把藏干混入。
 *
 * 这是一项可复算的盘面统计，不替代旺衰、用神或断语判断。
 */
data class BaziYinYangBalance(
    val classification: String,
    val yangCount: Int,
    val yinCount: Int,
) {
    val displayName: String
        get() = "$classification · 阳$yangCount 阴$yinCount"
}

fun FourPillars.yinYangBalance(): BaziYinYangBalance {
    val characters = listOf(year, month, day, hour).flatMap { it.take(2).toList() }
    val yangCount = characters.count { it in YANG_CHARACTERS }
    val yinCount = characters.count { it in YIN_CHARACTERS }
    val knownCount = yangCount + yinCount
    val classification = when {
        knownCount == 0 -> "待校验"
        yinCount == 0 -> "纯阳"
        yangCount == 0 -> "纯阴"
        yangCount >= 6 -> "阳盛"
        yangCount == 5 -> "偏阳"
        yangCount == yinCount -> "阴阳平衡"
        yinCount == 5 -> "偏阴"
        yinCount >= 6 -> "阴盛"
        else -> "阴阳平衡"
    }
    return BaziYinYangBalance(
        classification = classification,
        yangCount = yangCount,
        yinCount = yinCount,
    )
}

private val YANG_CHARACTERS = setOf('甲', '丙', '戊', '庚', '壬', '子', '寅', '辰', '午', '申', '戌')
private val YIN_CHARACTERS = setOf('乙', '丁', '己', '辛', '癸', '丑', '卯', '巳', '未', '酉', '亥')
