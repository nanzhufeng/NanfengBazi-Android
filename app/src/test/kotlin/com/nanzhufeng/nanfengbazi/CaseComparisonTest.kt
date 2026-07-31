package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseComparisonTest {
    @Test
    fun `命例对比只报告客观字段异同且不生成吉凶判断`() {
        val left = sampleStoredCase("left").copy(alias = "甲盘")
        val rightSnapshot = sampleStoredCase("right")
            .calculationSnapshots
            .single()
            .let { snapshot ->
                snapshot.copy(
                    id = "snapshot-right",
                    result = snapshot.result.copy(
                        fourPillars = FourPillars("辛巳", "戊寅", "丁巳", "丙午"),
                    ),
                )
            }
        val right = sampleStoredCase("right").copy(
            alias = "乙盘",
            calculationSnapshots = listOf(rightSnapshot),
            revision = 2,
        )

        val report = CaseComparisonEngine.compare(left, right)

        assertEquals("甲盘", report.leftAlias)
        assertEquals("乙盘", report.rightAlias)
        assertEquals(
            CaseComparisonOutcome.DIFFERENT,
            report.sections
                .single { it.title == "基础命盘" }
                .rows
                .single { it.label == "年柱" }
                .outcome,
        )
        assertEquals(
            CaseComparisonOutcome.SAME,
            report.sections
                .single { it.title == "基础命盘" }
                .rows
                .single { it.label == "日柱" }
                .outcome,
        )
        assertTrue(report.differentCount >= 2)
        assertTrue(
            report.sections
                .flatMap { it.rows }
                .none { row ->
                    listOf(row.label, row.leftValue, row.rightValue).any {
                        it.contains("吉") || it.contains("凶") || it.contains("合婚")
                    }
                },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `同一命例不能与自身比较`() {
        val case = sampleStoredCase("same")
        CaseComparisonEngine.compare(case, case)
    }
}
