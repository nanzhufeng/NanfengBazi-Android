package com.nanzhufeng.nanfengbazi.domain.model

enum class SourceFidelityValueKind {
    TEXT,
    PERCENTAGE,
}

data class SourceFidelityFieldDefinition(
    val fieldKey: String,
    val displayLabel: String,
    val valueKind: SourceFidelityValueKind,
)

/**
 * 问真页面中当前没有版本化本机算法的字段合同。
 *
 * 这些字段允许保存 OCR 原文、规范值、人工修正、置信度和原图框，但不得生成
 * calculatedValue，也不得参与本地命盘真值。
 */
object WenzhenSourceFidelityContract {
    const val CALCULATION_MESSAGE =
        "仅保留问真来源证据；当前无版本化算法，不自动复算"
    const val FIELD_STAR_LODGE = "chart.star_lodge"
    const val FIELD_LIFE_GUA = "chart.life_gua"
    const val FIELD_DAY_MASTER_ATTRIBUTE = "chart.day_master_attribute"
    const val FIELD_YIN_YANG_ATTRIBUTE = "chart.yin_yang_attribute"
    const val FIELD_USER_STRENGTH = "chart.user_strength"
    const val FIELD_USER_STRUCTURE = "chart.user_structure"
    const val FIELD_SAME_PARTY_PERCENT = "chart.five_element.same_party_percent"
    const val FIELD_OPPOSING_PARTY_PERCENT = "chart.five_element.opposing_party_percent"
    const val FIELD_WOOD_PERCENT = "chart.five_element.wood_percent"
    const val FIELD_FIRE_PERCENT = "chart.five_element.fire_percent"
    const val FIELD_EARTH_PERCENT = "chart.five_element.earth_percent"
    const val FIELD_METAL_PERCENT = "chart.five_element.metal_percent"
    const val FIELD_WATER_PERCENT = "chart.five_element.water_percent"

    val staticFields: List<SourceFidelityFieldDefinition> = listOf(
        text(FIELD_STAR_LODGE, "星宿"),
        text(FIELD_LIFE_GUA, "命卦"),
        text(FIELD_DAY_MASTER_ATTRIBUTE, "日主属性（问真来源）"),
        text(FIELD_YIN_YANG_ATTRIBUTE, "阴阳属性（问真来源）"),
        text(FIELD_USER_STRENGTH, "自定旺衰（问真来源）"),
        text(FIELD_USER_STRUCTURE, "自定格局（问真来源）"),
        percentage(FIELD_SAME_PARTY_PERCENT, "同党比例（%）"),
        percentage(FIELD_OPPOSING_PARTY_PERCENT, "异党比例（%）"),
        percentage(FIELD_WOOD_PERCENT, "木比例（%）"),
        percentage(FIELD_FIRE_PERCENT, "火比例（%）"),
        percentage(FIELD_EARTH_PERCENT, "土比例（%）"),
        percentage(FIELD_METAL_PERCENT, "金比例（%）"),
        percentage(FIELD_WATER_PERCENT, "水比例（%）"),
    )

    fun definitionFor(fieldKey: String): SourceFidelityFieldDefinition? =
        staticFields.firstOrNull { it.fieldKey == fieldKey }
            ?: SPIRITS_FIELD_PATTERN.matchEntire(fieldKey)?.let { match ->
                SourceFidelityFieldDefinition(
                    fieldKey = fieldKey,
                    displayLabel = "${match.groupValues[1].pillarLabel()} · 神煞",
                    valueKind = SourceFidelityValueKind.TEXT,
                )
            }

    fun isSourceOnly(fieldKey: String): Boolean = definitionFor(fieldKey) != null

    fun isPercentage(fieldKey: String): Boolean =
        definitionFor(fieldKey)?.valueKind == SourceFidelityValueKind.PERCENTAGE

    private fun text(fieldKey: String, label: String) =
        SourceFidelityFieldDefinition(fieldKey, label, SourceFidelityValueKind.TEXT)

    private fun percentage(fieldKey: String, label: String) =
        SourceFidelityFieldDefinition(fieldKey, label, SourceFidelityValueKind.PERCENTAGE)

    private fun String.pillarLabel(): String = when (this) {
        "year" -> "年柱"
        "month" -> "月柱"
        "day" -> "日柱"
        "hour" -> "时柱"
        else -> this
    }

    private val SPIRITS_FIELD_PATTERN =
        Regex("chart\\.(year|month|day|hour)\\.spirits")
}
