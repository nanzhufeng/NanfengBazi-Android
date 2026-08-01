package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAnalysisBridgeContractTest {
    @Test
    fun `默认选择全部字段并脱敏身份时间地点且材料可重复生成`() {
        val request = ExternalAnalysisExportRequest(summary())

        val first = ExternalAnalysisBridgeContract.prepareExport(request)
            as ExternalAnalysisExportResult.Success
        val second = ExternalAnalysisBridgeContract.prepareExport(request)
            as ExternalAnalysisExportResult.Success

        assertEquals(first.payload, second.payload)
        assertEquals(ExternalAnalysisFieldGroup.entries, first.payload.selectedGroups)
        assertTrue(first.payload.redactionEnabled)
        assertTrue(
            first.payload.fields.first { it.label == "姓名" }.redacted,
        )
        assertEquals(
            "[已脱敏]",
            first.payload.fields.first { it.label == "出生地区" }.value,
        )
        assertEquals(
            "甲子　乙丑　丙寅　丁卯",
            first.payload.fields.first { it.label == "四柱" }.value,
        )
        assertFalse(first.payload.copyText.contains("合成姓名"))
        assertFalse(first.payload.copyText.contains("上海市"))
        assertFalse(first.payload.copyText.contains("1990-01-02 03:04"))
        assertTrue(first.payload.copyText.contains("App 不联网、不自动发送"))
        assertTrue(first.payload.copyText.contains("不会成为南枫八字算法真值"))
    }

    @Test
    fun `关闭脱敏只复制用户明确选择的组并精确预览`() {
        val result = ExternalAnalysisBridgeContract.prepareExport(
            ExternalAnalysisExportRequest(
                summary = summary(),
                selectedGroups = setOf(ExternalAnalysisFieldGroup.BIRTH_FACTS),
                redactionPolicy = ExternalAnalysisRedactionPolicy(enabled = false),
            ),
        ) as ExternalAnalysisExportResult.Success

        assertEquals(
            listOf(ExternalAnalysisFieldGroup.BIRTH_FACTS),
            result.payload.selectedGroups,
        )
        assertFalse(result.payload.redactionEnabled)
        assertTrue(result.payload.copyText.contains("合成姓名"))
        assertTrue(result.payload.copyText.contains("1990-01-02 03:04"))
        assertFalse(result.payload.copyText.contains("【四柱与基础盘】"))
    }

    @Test
    fun `没有字段和摘要缺少请求章节时返回结构化失败`() {
        val noFields = ExternalAnalysisBridgeContract.prepareExport(
            ExternalAnalysisExportRequest(summary(), selectedGroups = emptySet()),
        ) as ExternalAnalysisExportResult.Rejected
        assertEquals(
            ExternalAnalysisBridgeErrorCode.NO_FIELDS_SELECTED,
            noFields.failure.code,
        )

        val missingSection = ExternalAnalysisBridgeContract.prepareExport(
            ExternalAnalysisExportRequest(
                summary().copy(
                    sections = summary().sections.filterNot { it.id == "fortune_facts" },
                ),
                selectedGroups = setOf(ExternalAnalysisFieldGroup.FORTUNE_FACTS),
            ),
        ) as ExternalAnalysisExportResult.Rejected
        assertEquals(
            ExternalAnalysisBridgeErrorCode.SUMMARY_SECTION_MISSING,
            missingSection.failure.code,
        )
    }

    @Test
    fun `手动回填必须确认来源和正文并生成明确外部标记`() {
        val payload = payload()
        val ready = ExternalAnalysisBridgeContract.prepareImport(
            ExternalAnalysisImportRequest(
                currentSummary = summary(),
                payload = payload,
                providerName = "某外部服务",
                modelName = "分析模型 A",
                resultText = "  这是外部分析内容。  ",
                analysisCategory = AnalysisCategory.CAREER,
                userConfirmedExternalSource = true,
            ),
        ) as ExternalAnalysisImportResult.Ready

        assertEquals(AnalysisCategory.CAREER, ready.draft.analysisCategory)
        assertTrue(ready.draft.content.contains("来源：某外部服务"))
        assertTrue(ready.draft.content.contains("模型：分析模型 A"))
        assertTrue(ready.draft.content.contains("材料编号：${payload.id}"))
        assertTrue(ready.draft.content.contains("不是南枫八字本机算法真值"))
        assertTrue(ready.draft.content.endsWith("这是外部分析内容。"))

        val notConfirmed = ExternalAnalysisBridgeContract.prepareImport(
            ExternalAnalysisImportRequest(
                currentSummary = summary(),
                payload = payload,
                providerName = "某外部服务",
                resultText = "内容",
                userConfirmedExternalSource = false,
            ),
        ) as ExternalAnalysisImportResult.Rejected
        assertEquals(
            ExternalAnalysisBridgeErrorCode.CONFIRMATION_REQUIRED,
            notConfirmed.failure.code,
        )

        val noSource = ExternalAnalysisBridgeContract.prepareImport(
            ExternalAnalysisImportRequest(
                currentSummary = summary(),
                payload = payload,
                providerName = " ",
                resultText = "内容",
                userConfirmedExternalSource = true,
            ),
        ) as ExternalAnalysisImportResult.Rejected
        assertEquals(ExternalAnalysisBridgeErrorCode.SOURCE_REQUIRED, noSource.failure.code)

        val noResult = ExternalAnalysisBridgeContract.prepareImport(
            ExternalAnalysisImportRequest(
                currentSummary = summary(),
                payload = payload,
                providerName = "来源",
                resultText = " ",
                userConfirmedExternalSource = true,
            ),
        ) as ExternalAnalysisImportResult.Rejected
        assertEquals(ExternalAnalysisBridgeErrorCode.RESULT_EMPTY, noResult.failure.code)
    }

    @Test
    fun `命例修订或采用快照变化后拒绝旧材料`() {
        val stale = ExternalAnalysisBridgeContract.prepareImport(
            ExternalAnalysisImportRequest(
                currentSummary = summary().copy(caseRevision = 8),
                payload = payload(),
                providerName = "来源",
                resultText = "内容",
                userConfirmedExternalSource = true,
            ),
        ) as ExternalAnalysisImportResult.Rejected

        assertEquals(ExternalAnalysisBridgeErrorCode.STALE_PAYLOAD, stale.failure.code)
    }

    private fun payload(): ExternalAnalysisPayload =
        (ExternalAnalysisBridgeContract.prepareExport(
            ExternalAnalysisExportRequest(summary()),
        ) as ExternalAnalysisExportResult.Success).payload

    private fun summary(): CaseObjectiveSummary {
        val sections = listOf(
            section(
                "birth_facts",
                "出生资料",
                listOf(
                    field("命例别名", "合成命例", CaseObjectiveSummarySource.CASE_IDENTITY),
                    field("姓名", "合成姓名", CaseObjectiveSummarySource.CASE_IDENTITY),
                    field(
                        "出生历法与时间",
                        "1990-01-02 03:04",
                        CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    ),
                    field(
                        "出生地区",
                        "上海市",
                        CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    ),
                ),
            ),
            section(
                "chart_facts",
                "四柱与基础盘",
                listOf(
                    field(
                        "四柱",
                        "甲子　乙丑　丙寅　丁卯",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                ),
            ),
            section(
                "fortune_facts",
                "起运与大运",
                listOf(field("起运", "顺行", CaseObjectiveSummarySource.ADOPTED_CALCULATION)),
            ),
            section(
                "calculation_evidence",
                "计算档案",
                listOf(field("引擎", "Tyme4j", CaseObjectiveSummarySource.CALCULATION_EVIDENCE)),
            ),
            section(
                "formal_record_index",
                "已有研究资料",
                listOf(field("分析记录", "2 条", CaseObjectiveSummarySource.FORMAL_RECORD_INDEX)),
            ),
        )
        return CaseObjectiveSummary(
            version = 1,
            caseId = "case-1",
            caseRevision = 7,
            adoptedSnapshotId = "snapshot-1",
            title = "合成摘要",
            sections = sections,
            provenanceNotice = "只来自已采用快照。",
            interpretationNotice = "不生成主观判断。",
            copyText = "unused",
        )
    }

    private fun section(
        id: String,
        title: String,
        fields: List<CaseObjectiveSummaryField>,
    ) = CaseObjectiveSummarySection(id, title, fields)

    private fun field(
        label: String,
        value: String,
        source: CaseObjectiveSummarySource,
    ) = CaseObjectiveSummaryField(label, value, source)
}
