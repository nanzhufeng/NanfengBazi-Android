package com.nanzhufeng.nanfengbazi.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaziAiAnalysisPromptContractTest {
    @Test
    fun `默认全项指令隐去身份地点但保留排盘必要字段`() {
        val request = BaziAiAnalysisPromptRequest(
            summary = summary(),
            referenceDate = LocalDate.of(2026, 8, 9),
        )

        val first = BaziAiAnalysisPromptContract.prepare(request)
            as BaziAiAnalysisPromptResult.Success
        val second = BaziAiAnalysisPromptContract.prepare(request)
            as BaziAiAnalysisPromptResult.Success

        assertEquals(first.prompt, second.prompt)
        assertFalse(first.prompt.copyText.contains("合成姓名"))
        assertFalse(first.prompt.copyText.contains("合成地区"))
        assertTrue(first.prompt.copyText.contains("性别口径：乾造"))
        assertTrue(first.prompt.copyText.contains("换算公历：1992-08-24 12:00:00"))
        assertTrue(first.prompt.copyText.contains("四柱：壬申　戊申　壬申　丙午"))
        assertTrue(first.prompt.copyText.contains("2021—2031"))
        assertTrue(first.prompt.copyText.contains("事实与推演"))
        assertTrue(first.prompt.copyText.contains("熟悉盲派命理、主流子平命理"))
        assertTrue(first.prompt.copyText.contains("先单列“盲派断事观察”"))
        assertTrue(first.prompt.copyText.contains("非专业决策依据"))
        assertTrue(first.prompt.hiddenFieldCount > 0)
    }

    @Test
    fun `专题和隐私选择进入稳定材料身份并约束高风险结论`() {
        val career = prompt(BaziAiAnalysisTopic.CAREER, hideIdentity = true)
        val health = prompt(BaziAiAnalysisTopic.HEALTH, hideIdentity = false)

        assertNotEquals(career.id, health.id)
        assertTrue(career.copyText.contains("事业专题解读"))
        assertTrue(health.copyText.contains("健康专题解读"))
        assertTrue(health.copyText.contains("合成姓名"))
        assertTrue(health.copyText.contains("不得作疾病诊断"))
        assertTrue(health.copyText.contains("不构成投资建议"))
        assertTrue(health.copyText.contains("不得写成确定事实"))
    }

    @Test
    fun `缺少必要摘要章节时结构化拒绝`() {
        val result = BaziAiAnalysisPromptContract.prepare(
            BaziAiAnalysisPromptRequest(
                summary = summary().copy(
                    sections = summary().sections.filterNot { it.id == "fortune_facts" },
                ),
                referenceDate = LocalDate.of(2026, 8, 9),
            ),
        ) as BaziAiAnalysisPromptResult.Rejected

        assertEquals(
            BaziAiAnalysisPromptErrorCode.REQUIRED_SECTION_MISSING,
            result.failure.code,
        )
    }

    private fun prompt(
        topic: BaziAiAnalysisTopic,
        hideIdentity: Boolean,
    ): BaziAiAnalysisPrompt = (
        BaziAiAnalysisPromptContract.prepare(
            BaziAiAnalysisPromptRequest(
                summary = summary(),
                topic = topic,
                referenceDate = LocalDate.of(2026, 8, 9),
                hideIdentityAndLocation = hideIdentity,
            ),
        ) as BaziAiAnalysisPromptResult.Success
        ).prompt

    private fun summary(): CaseObjectiveSummary = CaseObjectiveSummary(
        version = 1,
        caseId = "synthetic-case",
        caseRevision = 7,
        adoptedSnapshotId = "synthetic-snapshot",
        title = "合成客观摘要",
        sections = listOf(
            section(
                "birth_facts",
                "出生资料",
                listOf(
                    field("命例别名", "合成别名"),
                    field("姓名", "合成姓名"),
                    field("性别口径", "乾造"),
                    field("换算公历", "1992-08-24 12:00:00"),
                    field("换算农历", "1992年七月廿六 午时"),
                    field("出生地区", "合成地区"),
                ),
            ),
            section(
                "chart_facts",
                "四柱与基础盘",
                listOf(
                    field("四柱", "壬申　戊申　壬申　丙午"),
                    field("日主", "壬"),
                    field("年柱明细", "壬申｜主星 比肩｜藏干 庚 偏印"),
                ),
            ),
            section(
                "fortune_facts",
                "起运与大运",
                listOf(
                    field("起运方向", "顺排"),
                    field("精确交运时间", "1997-05-21 00:40:00"),
                    field("大运 1", "己酉｜6–15岁｜1997–2006"),
                ),
            ),
        ),
        provenanceNotice = "合成资料只用于测试。",
        interpretationNotice = "不包含主观判断。",
        copyText = "合成摘要文本",
    )

    private fun section(
        id: String,
        title: String,
        fields: List<CaseObjectiveSummaryField>,
    ) = CaseObjectiveSummarySection(id, title, fields)

    private fun field(label: String, value: String) = CaseObjectiveSummaryField(
        label = label,
        value = value,
        source = CaseObjectiveSummarySource.ADOPTED_CALCULATION,
    )
}
