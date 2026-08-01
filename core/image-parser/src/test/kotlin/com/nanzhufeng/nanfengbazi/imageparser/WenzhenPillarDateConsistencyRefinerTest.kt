package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookup
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupCandidate
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupEvidence
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupError
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WenzhenPillarDateConsistencyRefinerTest {
    @Test
    fun `同日反查候选标记一致而无同日候选标记冲突`() = kotlinx.coroutines.test.runTest {
        val pillars = FourPillars("壬申", "戊申", "壬申", "丙午")
        val refiner = WenzhenPillarDateConsistencyRefiner(
            fourPillarsLookup = FourPillarsLookup { query ->
                val candidates = if (query.ratHourRule == RatHourRule.TYME_DEFAULT) {
                    listOf(
                        FourPillarsLookupCandidate(
                            civilDateTime = CivilDateTime(1992, 8, 24, 12, 0, 0),
                            timeZoneId = query.timeZoneId,
                            resolvedUtcOffsetSeconds = 28_800,
                            timeZoneDataVersion = "fixture",
                            instant = Instant.parse("1992-08-24T04:00:00Z"),
                            fourPillars = query.fourPillars,
                            ratHourRule = query.ratHourRule,
                            engineVersion = "fixture",
                            ruleVersion = "fixture",
                        ),
                    )
                } else {
                    emptyList()
                }
                FourPillarsLookupResult.Completed(
                    query = query,
                    candidates = candidates,
                    evidence = FourPillarsLookupEvidence(
                        "fixture",
                        "fixture",
                        "fixture",
                        "fixture",
                    ),
                )
            },
        )

        val matched = refiner.refine(result("1992-08-24", pillars))
        val mismatched = refiner.refine(result("1992-08-25", pillars))

        assertEquals(1f, matched.fields.single { it.fieldKey == FOUR_PILLARS }.consistencyConfidence)
        assertEquals(0f, mismatched.fields.single { it.fieldKey == FOUR_PILLARS }.consistencyConfidence)
    }

    @Test
    fun `反查引擎失败时不得把未知状态误报为日期冲突`() = kotlinx.coroutines.test.runTest {
        val refiner = WenzhenPillarDateConsistencyRefiner(
            fourPillarsLookup = FourPillarsLookup {
                FourPillarsLookupResult.Failed(FourPillarsLookupError.EngineUnavailable)
            },
        )

        val refined = refiner.refine(
            result("1992-08-24", FourPillars("壬申", "戊申", "壬申", "丙午")),
        )

        assertEquals(
            null,
            refined.fields.single { it.fieldKey == FOUR_PILLARS }.consistencyConfidence,
        )
    }

    private fun result(date: String, pillars: FourPillars): WenzhenP0ParseResult {
        val dateField = field("date", SOLAR_DATE, TypedFieldValue.Text(date))
        val pillarField = field("pillars", FOUR_PILLARS, TypedFieldValue.FourPillarsValue(pillars))
        return WenzhenP0ParseResult(
            fields = listOf(dateField, pillarField),
            longTexts = emptyList(),
            candidates = listOf(
                ImportCaseCandidate(
                    id = "candidate",
                    imageIds = listOf("image"),
                    fieldEvidenceIds = listOf(dateField.id, pillarField.id),
                    groupingConfidence = 1f,
                    requiresReview = true,
                ),
            ),
        )
    }

    private fun field(id: String, key: String, value: TypedFieldValue) = CaseFieldEvidence(
        id = id,
        attachmentId = "image",
        fieldKey = key,
        rawText = "fixture",
        normalizedValue = value,
        parserConfidence = 0.9f,
        parserRuleId = "fixture",
        userEdited = false,
        createdAt = Instant.EPOCH,
    )

    private companion object {
        const val SOLAR_DATE = "birth.solar_date"
        const val FOUR_PILLARS = "chart.four_pillars"
    }
}
