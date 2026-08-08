package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 八字字符的唯一五行呈现规则。
 *
 * 记录列表、四柱录入、万年历与详情表都只能从此处取色；它只处理视觉语义，绝不参与排盘。
 */
internal object BaziElementPalette {
    val Wood = Color(0xFF2F9B55)
    val Fire = Color(0xFFD94B43)
    val Earth = Color(0xFFA47B14)
    /** 金行使用亮黄色，申、酉、庚、辛均为此色。 */
    val Metal = Color(0xFFE2A51B)
    val Water = Color(0xFF2E83C8)
}

internal fun baziElementColor(element: String): Color = when (element) {
    "木" -> BaziElementPalette.Wood
    "火" -> BaziElementPalette.Fire
    "土" -> BaziElementPalette.Earth
    "金" -> BaziElementPalette.Metal
    "水" -> BaziElementPalette.Water
    else -> NanfengInk
}

internal fun baziElementColor(character: Char): Color = when (character) {
    '甲', '乙', '寅', '卯' -> BaziElementPalette.Wood
    '丙', '丁', '巳', '午' -> BaziElementPalette.Fire
    '戊', '己', '辰', '戌', '丑', '未' -> BaziElementPalette.Earth
    '庚', '辛', '申', '酉' -> BaziElementPalette.Metal
    '壬', '癸', '亥', '子' -> BaziElementPalette.Water
    else -> NanfengInk
}

private val BaziElementContainerNeutral = Color(0xFFF0F1F0)

/** 灰色底板只保留少量五行色相，字符色始终是视觉主体。 */
internal fun baziElementContainerColor(character: Char?): Color = character
    ?.let(::baziElementColor)
    ?.let { elementColor -> lerp(BaziElementContainerNeutral, elementColor, 0.035f) }
    ?: BaziElementContainerNeutral

/** 选中态仍以灰为主，只比普通态多一点色相，不用高饱和底色抢文字。 */
internal fun baziElementSelectedContainerColor(character: Char): Color =
    lerp(BaziElementContainerNeutral, baziElementColor(character), 0.055f)
