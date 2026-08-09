package com.nanzhufeng.nanfengbazi.domain.model

fun LunarDateTime.toTraditionalChineseText(): String = buildString {
    append(year)
    append('年')
    if (isLeapMonth) append('闰')
    append(TRADITIONAL_LUNAR_MONTHS.getValue(month))
    append('月')
    append(TRADITIONAL_LUNAR_DAYS.getValue(day))
    append(' ')
    append(TRADITIONAL_DOUBLE_HOURS[(hour + 1) / 2 % 12])
    append('时')
}

private val TRADITIONAL_LUNAR_MONTHS = mapOf(
    1 to "正",
    2 to "二",
    3 to "三",
    4 to "四",
    5 to "五",
    6 to "六",
    7 to "七",
    8 to "八",
    9 to "九",
    10 to "十",
    11 to "冬",
    12 to "腊",
)

private val TRADITIONAL_LUNAR_DAYS = mapOf(
    1 to "初一",
    2 to "初二",
    3 to "初三",
    4 to "初四",
    5 to "初五",
    6 to "初六",
    7 to "初七",
    8 to "初八",
    9 to "初九",
    10 to "初十",
    11 to "十一",
    12 to "十二",
    13 to "十三",
    14 to "十四",
    15 to "十五",
    16 to "十六",
    17 to "十七",
    18 to "十八",
    19 to "十九",
    20 to "二十",
    21 to "廿一",
    22 to "廿二",
    23 to "廿三",
    24 to "廿四",
    25 to "廿五",
    26 to "廿六",
    27 to "廿七",
    28 to "廿八",
    29 to "廿九",
    30 to "三十",
)

private val TRADITIONAL_DOUBLE_HOURS = listOf(
    "子",
    "丑",
    "寅",
    "卯",
    "辰",
    "巳",
    "午",
    "未",
    "申",
    "酉",
    "戌",
    "亥",
)
