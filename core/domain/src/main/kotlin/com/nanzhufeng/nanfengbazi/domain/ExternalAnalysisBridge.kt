package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import java.security.MessageDigest

const val EXTERNAL_ANALYSIS_BRIDGE_VERSION: Int = 1
const val EXTERNAL_ANALYSIS_RESULT_MAX_LENGTH: Int = 50_000

enum class ExternalAnalysisFieldGroup(
    val summarySectionId: String,
    val displayName: String,
) {
    BIRTH_FACTS("birth_facts", "出生资料"),
    CHART_FACTS("chart_facts", "四柱与基础盘"),
    FORTUNE_FACTS("fortune_facts", "起运与大运"),
    CALCULATION_EVIDENCE("calculation_evidence", "计算档案"),
    FORMAL_RECORD_INDEX("formal_record_index", "已有研究资料计数"),
}

data class ExternalAnalysisRedactionPolicy(
    val enabled: Boolean = true,
)

data class ExternalAnalysisExportRequest(
    val summary: CaseObjectiveSummary,
    val selectedGroups: Set<ExternalAnalysisFieldGroup> =
        ExternalAnalysisFieldGroup.entries.toSet(),
    val redactionPolicy: ExternalAnalysisRedactionPolicy =
        ExternalAnalysisRedactionPolicy(),
    val bridgeVersion: Int = EXTERNAL_ANALYSIS_BRIDGE_VERSION,
)

data class ExternalAnalysisPreviewField(
    val group: ExternalAnalysisFieldGroup,
    val label: String,
    val value: String,
    val source: CaseObjectiveSummarySource,
    val sensitivity: CaseObjectiveSummarySensitivity,
    val redacted: Boolean,
) {
    init {
        require(label.isNotBlank()) { "外部分析预览字段标签不能为空" }
        require(value.isNotBlank()) { "外部分析预览字段值不能为空" }
    }
}

data class ExternalAnalysisPayload(
    val version: Int,
    val id: String,
    val caseId: String,
    val caseRevision: Long,
    val adoptedSnapshotId: String,
    val selectedGroups: List<ExternalAnalysisFieldGroup>,
    val redactionEnabled: Boolean,
    val fields: List<ExternalAnalysisPreviewField>,
    val copyText: String,
) {
    init {
        require(version > 0) { "外部分析桥接版本必须大于零" }
        require(id.matches(Regex("vx11-[0-9a-f]{16}"))) { "外部分析材料 id 无效" }
        require(caseId.isNotBlank()) { "外部分析材料必须关联命例" }
        require(adoptedSnapshotId.isNotBlank()) { "外部分析材料必须关联采用快照" }
        require(selectedGroups.isNotEmpty() && fields.isNotEmpty()) {
            "外部分析材料至少需要一个字段"
        }
        require(copyText.isNotBlank()) { "外部分析材料复制文本不能为空" }
    }
}

data class ExternalAnalysisImportRequest(
    val currentSummary: CaseObjectiveSummary,
    val payload: ExternalAnalysisPayload,
    val providerName: String,
    val modelName: String = "",
    val resultText: String,
    val analysisCategory: AnalysisCategory = AnalysisCategory.GENERAL,
    val userConfirmedExternalSource: Boolean,
    val bridgeVersion: Int = EXTERNAL_ANALYSIS_BRIDGE_VERSION,
)

data class ExternalAnalysisBackfillDraft(
    val payloadId: String,
    val providerName: String,
    val modelName: String?,
    val analysisCategory: AnalysisCategory,
    val content: String,
) {
    init {
        require(payloadId.isNotBlank())
        require(providerName.isNotBlank())
        require(content.isNotBlank())
    }
}

enum class ExternalAnalysisBridgeErrorCode {
    UNSUPPORTED_VERSION,
    NO_FIELDS_SELECTED,
    SUMMARY_SECTION_MISSING,
    SUMMARY_UNAVAILABLE,
    CLIPBOARD_UNAVAILABLE,
    COPY_FAILED,
    CONFIRMATION_REQUIRED,
    SOURCE_REQUIRED,
    SOURCE_TOO_LONG,
    RESULT_EMPTY,
    RESULT_TOO_LONG,
    STALE_PAYLOAD,
    STORAGE_FAILED,
}

data class ExternalAnalysisBridgeFailure(
    val code: ExternalAnalysisBridgeErrorCode,
    val message: String,
) {
    init {
        require(message.isNotBlank()) { "外部分析桥接失败说明不能为空" }
    }
}

sealed interface ExternalAnalysisExportResult {
    data class Success(val payload: ExternalAnalysisPayload) : ExternalAnalysisExportResult
    data class Rejected(
        val failure: ExternalAnalysisBridgeFailure,
    ) : ExternalAnalysisExportResult
}

sealed interface ExternalAnalysisImportResult {
    data class Ready(val draft: ExternalAnalysisBackfillDraft) : ExternalAnalysisImportResult
    data class Rejected(
        val failure: ExternalAnalysisBridgeFailure,
    ) : ExternalAnalysisImportResult
}

interface ExternalAnalysisBridge {
    fun prepareExport(request: ExternalAnalysisExportRequest): ExternalAnalysisExportResult
    fun prepareImport(request: ExternalAnalysisImportRequest): ExternalAnalysisImportResult
}

object ExternalAnalysisBridgeContract : ExternalAnalysisBridge {
    override fun prepareExport(
        request: ExternalAnalysisExportRequest,
    ): ExternalAnalysisExportResult {
        if (request.bridgeVersion != EXTERNAL_ANALYSIS_BRIDGE_VERSION) {
            return exportFailure(
                ExternalAnalysisBridgeErrorCode.UNSUPPORTED_VERSION,
                "暂不支持外部分析桥接版本 ${request.bridgeVersion}。",
            )
        }
        if (request.selectedGroups.isEmpty()) {
            return exportFailure(
                ExternalAnalysisBridgeErrorCode.NO_FIELDS_SELECTED,
                "请至少选择一组要复制的字段。",
            )
        }
        val sectionsById = request.summary.sections.associateBy { it.id }
        val selectedGroups = ExternalAnalysisFieldGroup.entries.filter {
            it in request.selectedGroups
        }
        val fields = buildList {
            selectedGroups.forEach { group ->
                val section = sectionsById[group.summarySectionId]
                    ?: return exportFailure(
                        ExternalAnalysisBridgeErrorCode.SUMMARY_SECTION_MISSING,
                        "客观摘要缺少“${group.displayName}”，未生成外部分析材料。",
                    )
                section.fields.forEach { field ->
                    val redacted = request.redactionPolicy.enabled &&
                        field.sensitivity != CaseObjectiveSummarySensitivity.PUBLIC
                    add(
                        ExternalAnalysisPreviewField(
                            group = group,
                            label = field.label,
                            value = if (redacted) REDACTED_VALUE else field.value,
                            source = field.source,
                            sensitivity = field.sensitivity,
                            redacted = redacted,
                        ),
                    )
                }
            }
        }
        val identity = listOf(
            request.bridgeVersion.toString(),
            request.summary.caseId,
            request.summary.caseRevision.toString(),
            request.summary.adoptedSnapshotId,
            selectedGroups.joinToString(",") { it.name },
            request.redactionPolicy.enabled.toString(),
            fields.joinToString("\u0000") {
                "${it.group.name}|${it.label}|${it.value}|${it.source.name}|" +
                    "${it.sensitivity.name}|${it.redacted}"
            },
        ).joinToString("\u0001")
        val payloadId = "vx11-${identity.sha256().take(16)}"
        val copyText = buildPayloadText(
            payloadId = payloadId,
            caseRevision = request.summary.caseRevision,
            selectedGroups = selectedGroups,
            redactionEnabled = request.redactionPolicy.enabled,
            fields = fields,
        )
        return ExternalAnalysisExportResult.Success(
            ExternalAnalysisPayload(
                version = request.bridgeVersion,
                id = payloadId,
                caseId = request.summary.caseId,
                caseRevision = request.summary.caseRevision,
                adoptedSnapshotId = request.summary.adoptedSnapshotId,
                selectedGroups = selectedGroups,
                redactionEnabled = request.redactionPolicy.enabled,
                fields = fields,
                copyText = copyText,
            ),
        )
    }

    override fun prepareImport(
        request: ExternalAnalysisImportRequest,
    ): ExternalAnalysisImportResult {
        if (request.bridgeVersion != EXTERNAL_ANALYSIS_BRIDGE_VERSION ||
            request.payload.version != EXTERNAL_ANALYSIS_BRIDGE_VERSION
        ) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.UNSUPPORTED_VERSION,
                "外部结果与当前桥接版本不兼容，未写入命例。",
            )
        }
        if (!request.userConfirmedExternalSource) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.CONFIRMATION_REQUIRED,
                "请先确认该内容来自外部，只作为研究记录保存。",
            )
        }
        if (
            request.payload.caseId != request.currentSummary.caseId ||
            request.payload.caseRevision != request.currentSummary.caseRevision ||
            request.payload.adoptedSnapshotId != request.currentSummary.adoptedSnapshotId
        ) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.STALE_PAYLOAD,
                "命例或采用快照已变化，请重新生成材料后再回填。",
            )
        }
        val provider = request.providerName.trim()
        val model = request.modelName.trim()
        val result = request.resultText.trim()
        if (provider.isEmpty()) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.SOURCE_REQUIRED,
                "请填写外部来源名称，例如服务或分析者名称。",
            )
        }
        if (provider.length > SOURCE_NAME_MAX_LENGTH || model.length > SOURCE_NAME_MAX_LENGTH) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.SOURCE_TOO_LONG,
                "来源和模型名称均不能超过 $SOURCE_NAME_MAX_LENGTH 个字符。",
            )
        }
        if (result.isEmpty()) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.RESULT_EMPTY,
                "请粘贴外部分析结果。",
            )
        }
        if (result.length > EXTERNAL_ANALYSIS_RESULT_MAX_LENGTH) {
            return importFailure(
                ExternalAnalysisBridgeErrorCode.RESULT_TOO_LONG,
                "外部分析结果不能超过 $EXTERNAL_ANALYSIS_RESULT_MAX_LENGTH 个字符。",
            )
        }
        val content = buildString {
            appendLine("【外部分析｜用户手动回填】")
            appendLine("来源：$provider")
            appendLine("模型：${model.ifEmpty { "未填写" }}")
            appendLine("材料编号：${request.payload.id}")
            appendLine("命例修订：${request.payload.caseRevision}")
            appendLine("边界：这是外部内容，不是南枫八字本机算法真值；应用未自动验证其结论。")
            appendLine("---")
            append(result)
        }
        return ExternalAnalysisImportResult.Ready(
            ExternalAnalysisBackfillDraft(
                payloadId = request.payload.id,
                providerName = provider,
                modelName = model.ifEmpty { null },
                analysisCategory = request.analysisCategory,
                content = content,
            ),
        )
    }

    private const val REDACTED_VALUE = "[已脱敏]"
    private const val SOURCE_NAME_MAX_LENGTH = 80
}

private fun buildPayloadText(
    payloadId: String,
    caseRevision: Long,
    selectedGroups: List<ExternalAnalysisFieldGroup>,
    redactionEnabled: Boolean,
    fields: List<ExternalAnalysisPreviewField>,
): String = buildString {
    appendLine("南枫八字 · 外部分析材料 v$EXTERNAL_ANALYSIS_BRIDGE_VERSION")
    appendLine("材料编号：$payloadId")
    appendLine("命例修订：$caseRevision")
    appendLine("传递方式：用户手动复制；App 不联网、不自动发送")
    appendLine("脱敏：${if (redactionEnabled) "已启用" else "未启用"}")
    appendLine("边界：以下是可追溯事实投影。请把事实、推测与建议分开，并说明不确定性。")
    appendLine("注意：返回内容不会成为南枫八字算法真值，只能由用户手动回填为外部分析记录。")
    selectedGroups.forEach { group ->
        appendLine()
        appendLine("【${group.displayName}】")
        fields.filter { it.group == group }.forEach { field ->
            appendLine("- ${field.label}：${field.value}（来源：${field.source.name}）")
        }
    }
}

private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

private fun exportFailure(
    code: ExternalAnalysisBridgeErrorCode,
    message: String,
): ExternalAnalysisExportResult = ExternalAnalysisExportResult.Rejected(
    ExternalAnalysisBridgeFailure(code, message),
)

private fun importFailure(
    code: ExternalAnalysisBridgeErrorCode,
    message: String,
): ExternalAnalysisImportResult = ExternalAnalysisImportResult.Rejected(
    ExternalAnalysisBridgeFailure(code, message),
)
