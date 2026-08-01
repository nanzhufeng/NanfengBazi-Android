package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookup
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupQuery
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import java.time.LocalDate

fun interface WenzhenParseResultRefiner {
    suspend fun refine(result: WenzhenP0ParseResult): WenzhenP0ParseResult
}

object NoOpWenzhenParseResultRefiner : WenzhenParseResultRefiner {
    override suspend fun refine(result: WenzhenP0ParseResult): WenzhenP0ParseResult = result
}

class WenzhenPillarDateConsistencyRefiner(
    private val fourPillarsLookup: FourPillarsLookup,
    private val timeZoneId: String = "Asia/Shanghai",
) : WenzhenParseResultRefiner {
    override suspend fun refine(result: WenzhenP0ParseResult): WenzhenP0ParseResult {
        val fieldsById = result.fields.associateBy(CaseFieldEvidence::id)
        val consistencyByFieldId = mutableMapOf<String, Float>()
        result.candidates.forEach { candidate ->
            val fields = candidate.fieldEvidenceIds.mapNotNull(fieldsById::get)
            val date = (fields.firstOrNull { it.fieldKey == FIELD_SOLAR_DATE }
                ?.normalizedValue as? TypedFieldValue.Text)?.value ?: return@forEach
            val sourceDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@forEach
            val pillarField = fields.firstOrNull { it.fieldKey == FIELD_FOUR_PILLARS }
                ?: return@forEach
            val pillars = (pillarField.normalizedValue as? TypedFieldValue.FourPillarsValue)
                ?.value ?: return@forEach
            var completedRuleCount = 0
            var matches = false
            for (rule in RatHourRule.entries) {
                when (val lookup = fourPillarsLookup.search(
                    FourPillarsLookupQuery(
                        fourPillars = pillars,
                        startYear = sourceDate.year,
                        endYear = sourceDate.year,
                        timeZoneId = timeZoneId,
                        ratHourRule = rule,
                    ),
                )) {
                    is FourPillarsLookupResult.Completed -> {
                        completedRuleCount += 1
                        matches = matches || lookup.candidates.any { value ->
                            with(value.civilDateTime) {
                                year == sourceDate.year &&
                                    month == sourceDate.monthValue &&
                                    day == sourceDate.dayOfMonth
                            }
                        }
                    }
                    is FourPillarsLookupResult.Failed -> Unit
                }
            }
            when {
                matches -> consistencyByFieldId[pillarField.id] = 1f
                completedRuleCount == RatHourRule.entries.size -> {
                    consistencyByFieldId[pillarField.id] = 0f
                }
            }
        }
        if (consistencyByFieldId.isEmpty()) return result
        return result.copy(
            fields = result.fields.map { field ->
                consistencyByFieldId[field.id]?.let { confidence ->
                    field.copy(consistencyConfidence = confidence)
                } ?: field
            },
        )
    }

    private companion object {
        const val FIELD_SOLAR_DATE = "birth.solar_date"
        const val FIELD_FOUR_PILLARS = "chart.four_pillars"
    }
}
