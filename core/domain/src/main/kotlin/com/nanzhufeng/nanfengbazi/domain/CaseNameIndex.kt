package com.nanzhufeng.nanfengbazi.domain

import java.text.Collator
import java.util.Locale

private val chineseNameCollator = ThreadLocal.withInitial {
    Collator.getInstance(Locale.CHINA).apply {
        strength = Collator.PRIMARY
    }
}

private val chineseInitialBoundaries = listOf(
    'A' to "阿",
    'B' to "芭",
    'C' to "擦",
    'D' to "搭",
    'E' to "蛾",
    'F' to "发",
    'G' to "噶",
    'H' to "哈",
    'J' to "击",
    'K' to "喀",
    'L' to "垃",
    'M' to "妈",
    'N' to "拿",
    'O' to "哦",
    'P' to "啪",
    'Q' to "期",
    'R' to "然",
    'S' to "撒",
    'T' to "塌",
    'W' to "挖",
    'X' to "昔",
    'Y' to "压",
    'Z' to "匝",
)

fun String.caseNameInitial(): Char {
    val normalized = trim()
    val first = normalized.firstOrNull() ?: return '#'
    if (first in 'A'..'Z' || first in 'a'..'z') return first.uppercaseChar()
    val collator = chineseNameCollator.get()
    return chineseInitialBoundaries
        .asReversed()
        .firstOrNull { (_, boundary) -> collator.compare(normalized, boundary) >= 0 }
        ?.first
        ?: '#'
}

internal fun compareCaseNames(left: String, right: String): Int =
    chineseNameCollator.get().compare(left, right)

/**
 * Keeps the A–Z list order and its visible section headers on the same key.
 * Chinese names use their pinyin initial; Latin names use their own initial.
 */
internal fun compareCaseNameGroups(left: String, right: String): Int {
    val initialOrder = left.caseNameInitial().compareTo(right.caseNameInitial())
    return if (initialOrder != 0) initialOrder else compareCaseNames(left, right)
}
