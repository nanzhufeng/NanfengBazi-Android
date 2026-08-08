package com.nanzhufeng.nanfengbazi.domain

/**
 * 称骨民俗查表 v1。
 *
 * 数据为年干支、农历月日、时辰的公开民俗对照表；男女歌诀是独立版本化民俗文本。
 * 闰月依公开表的常见约定：上半月按本月、下半月按下月；闰十二月下半月未在该
 * 约定中定义，因此明确返回空结果，避免猜测。
 */
object FolkBoneWeightRulesV1 {
    const val VERSION = "folk-bone-weight-v1"

    private val yearWeights = mapOf(
        "甲子" to 12, "乙丑" to 9, "丙寅" to 6, "丁卯" to 7, "戊辰" to 12, "己巳" to 5,
        "庚午" to 9, "辛未" to 8, "壬申" to 7, "癸酉" to 8, "甲戌" to 15, "乙亥" to 9,
        "丙子" to 16, "丁丑" to 8, "戊寅" to 8, "己卯" to 19, "庚辰" to 12, "辛巳" to 6,
        "壬午" to 8, "癸未" to 7, "甲申" to 5, "乙酉" to 15, "丙戌" to 6, "丁亥" to 16,
        "戊子" to 15, "己丑" to 7, "庚寅" to 9, "辛卯" to 12, "壬辰" to 10, "癸巳" to 7,
        "甲午" to 15, "乙未" to 6, "丙申" to 5, "丁酉" to 14, "戊戌" to 14, "己亥" to 9,
        "庚子" to 7, "辛丑" to 7, "壬寅" to 9, "癸卯" to 12, "甲辰" to 8, "乙巳" to 7,
        "丙午" to 13, "丁未" to 5, "戊申" to 14, "己酉" to 5, "庚戌" to 9, "辛亥" to 17,
        "壬子" to 5, "癸丑" to 7, "甲寅" to 12, "乙卯" to 8, "丙辰" to 8, "丁巳" to 6,
        "戊午" to 19, "己未" to 6, "庚申" to 8, "辛酉" to 16, "壬戌" to 10, "癸亥" to 6,
    )
    private val monthWeights = listOf(6, 7, 18, 9, 5, 16, 9, 15, 18, 8, 9, 5)
    private val dayWeights = listOf(
        5, 10, 8, 15, 16, 15, 8, 16, 8, 9,
        17, 17, 8, 17, 10, 8, 9, 18, 5, 15,
        10, 9, 8, 9, 15, 18, 7, 8, 16, 6,
    )
    private val hourWeights = listOf(16, 6, 7, 10, 9, 16, 10, 8, 8, 9, 6, 6)

    fun calculate(
        lunarYearPillar: String,
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean,
        doubleHourIndex: Int,
    ): FolkBoneWeight? {
        val effectiveMonth = when {
            !isLeapMonth -> lunarMonth
            lunarDay <= 15 -> lunarMonth
            lunarMonth == 12 -> return null
            else -> lunarMonth + 1
        }
        val year = yearWeights[lunarYearPillar] ?: return null
        val month = monthWeights.getOrNull(effectiveMonth - 1) ?: return null
        val day = dayWeights.getOrNull(lunarDay - 1) ?: return null
        val hour = hourWeights.getOrNull(doubleHourIndex) ?: return null
        return FolkBoneWeight(
            version = VERSION,
            verdictVersion = FolkBoneWeightVerdictsV1.VERSION,
            totalQian = year + month + day + hour,
            yearQian = year,
            monthQian = month,
            dayQian = day,
            hourQian = hour,
            maleVerdict = FolkBoneWeightVerdictsV1.male(year + month + day + hour),
            femaleVerdict = FolkBoneWeightVerdictsV1.female(year + month + day + hour),
        )
    }
}
