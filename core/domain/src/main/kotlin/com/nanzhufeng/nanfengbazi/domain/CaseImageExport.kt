package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType

const val CASE_IMAGE_DOCUMENT_VERSION: Int = 1

enum class CaseImageExportScope {
    ADOPTED_CHART_AND_FORMAL_RECORDS,
}

enum class CaseImageDeliveryMode {
    SAVE_TO_SYSTEM_FILE,
    SHARE_LONG_IMAGE,
}

data class CaseImageExportInput(
    val caseData: BaziCase,
    val documentVersion: Int = CASE_IMAGE_DOCUMENT_VERSION,
    val scope: CaseImageExportScope =
        CaseImageExportScope.ADOPTED_CHART_AND_FORMAL_RECORDS,
)

data class CaseImageRenderRow(
    val label: String,
    val value: String,
) {
    init {
        require(label.isNotBlank()) { "导出字段标签不能为空" }
        require(value.isNotBlank()) { "导出字段值不能为空" }
    }
}

sealed interface CaseImageRenderBlock {
    val title: String

    data class Rows(
        override val title: String,
        val rows: List<CaseImageRenderRow>,
    ) : CaseImageRenderBlock {
        init {
            require(title.isNotBlank()) { "导出区块标题不能为空" }
            require(rows.isNotEmpty()) { "导出字段区块不能为空" }
        }
    }

    data class Paragraphs(
        override val title: String,
        val paragraphs: List<String>,
    ) : CaseImageRenderBlock {
        init {
            require(title.isNotBlank()) { "导出区块标题不能为空" }
            require(paragraphs.isNotEmpty()) { "导出段落区块不能为空" }
            require(paragraphs.none(String::isBlank)) { "导出段落不能为空" }
        }
    }
}

data class CaseImageRenderFacts(
    val documentVersion: Int,
    val scope: CaseImageExportScope,
    val caseId: String,
    val caseRevision: Long,
    val adoptedSnapshotId: String,
    val title: String,
    val subtitle: String,
    val suggestedFileStem: String,
    val blocks: List<CaseImageRenderBlock>,
    val provenanceNotice: String,
    val privacyNotice: String,
) {
    init {
        require(documentVersion > 0) { "图片文档版本必须大于零" }
        require(caseId.isNotBlank()) { "图片导出必须关联命例" }
        require(adoptedSnapshotId.isNotBlank()) { "图片导出必须关联采用快照" }
        require(title.isNotBlank() && subtitle.isNotBlank()) { "图片标题不能为空" }
        require(suggestedFileStem.isNotBlank()) { "建议文件名不能为空" }
        require(blocks.isNotEmpty()) { "图片导出内容不能为空" }
        require(provenanceNotice.isNotBlank() && privacyNotice.isNotBlank()) {
            "图片导出必须声明来源与隐私边界"
        }
    }
}

enum class CaseImageExportErrorCode {
    UNSUPPORTED_DOCUMENT_VERSION,
    NO_ADOPTED_SNAPSHOT,
    MULTIPLE_ADOPTED_SNAPSHOTS,
    CONTENT_TOO_LARGE,
    RENDER_FAILED,
    OUTPUT_UNAVAILABLE,
    WRITE_FAILED,
    USER_CANCELLED,
    NO_SHARE_TARGET,
    SHARE_LAUNCH_FAILED,
}

data class CaseImageExportFailure(
    val code: CaseImageExportErrorCode,
    val message: String,
) {
    init {
        require(message.isNotBlank()) { "图片导出失败说明不能为空" }
    }
}

sealed interface CaseImageFactsResult {
    data class Prepared(
        val facts: CaseImageRenderFacts,
    ) : CaseImageFactsResult

    data class Rejected(
        val failure: CaseImageExportFailure,
    ) : CaseImageFactsResult
}

data class RenderedCaseImage(
    val facts: CaseImageRenderFacts,
    val mimeType: String,
    val fileExtension: String,
    val bytes: ByteArray,
    val widthPixels: Int,
    val heightPixels: Int,
    val sha256: String,
) {
    init {
        require(mimeType.isNotBlank() && fileExtension.isNotBlank())
        require(bytes.isNotEmpty()) { "渲染图片字节不能为空" }
        require(widthPixels > 0 && heightPixels > 0) { "渲染图片尺寸无效" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "渲染图片哈希无效" }
    }
}

sealed interface CaseImageRenderResult {
    data class Success(
        val image: RenderedCaseImage,
    ) : CaseImageRenderResult

    data class Rejected(
        val failure: CaseImageExportFailure,
    ) : CaseImageRenderResult
}

interface CaseImageRenderer {
    suspend fun render(input: CaseImageExportInput): CaseImageRenderResult
}

object CaseImageExportContract {
    fun prepare(input: CaseImageExportInput): CaseImageFactsResult {
        if (input.documentVersion != CASE_IMAGE_DOCUMENT_VERSION) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.UNSUPPORTED_DOCUMENT_VERSION,
                    "暂不支持图片文档版本 ${input.documentVersion}。",
                ),
            )
        }
        val summary = when (
            val result = CaseObjectiveSummaryContract.generate(
                CaseObjectiveSummaryInput(input.caseData),
            )
        ) {
            is CaseObjectiveSummaryResult.Success -> result.summary
            is CaseObjectiveSummaryResult.Rejected -> {
                return CaseImageFactsResult.Rejected(result.failure.toImageFailure())
            }
        }
        val blocks = buildList {
            summary.sections.forEach { section ->
                add(
                    CaseImageRenderBlock.Rows(
                        title = section.imageTitle(),
                        rows = section.fields.map { field ->
                            CaseImageRenderRow(field.label, field.value)
                        },
                    ),
                )
            }
            input.caseData.textRecords
                .takeIf(List<CaseTextRecord>::isNotEmpty)
                ?.let { records ->
                    add(
                        CaseImageRenderBlock.Paragraphs(
                            "正式分析记录",
                            records.map(CaseTextRecord::displayText),
                        ),
                    )
                }
            input.caseData.events
                .takeIf(List<CaseEvent>::isNotEmpty)
                ?.let { events ->
                    add(
                        CaseImageRenderBlock.Paragraphs(
                            "关键事件",
                            events.map(CaseEvent::displayText),
                        ),
                    )
                }
        }
        return CaseImageFactsResult.Prepared(
            CaseImageRenderFacts(
                documentVersion = input.documentVersion,
                scope = input.scope,
                caseId = summary.caseId,
                caseRevision = summary.caseRevision,
                adoptedSnapshotId = summary.adoptedSnapshotId,
                title = summary.title,
                subtitle = "南枫八字 · 已采用命盘",
                suggestedFileStem = input.caseData.alias.take(48),
                blocks = blocks,
                provenanceNotice =
                    "命盘字段复用客观摘要 v${summary.version} 的唯一采用快照投影；" +
                        "正式记录按当前命例原文附加。来源截图值、未采用候选和旧快照" +
                        "未作为计算真值。",
                privacyNotice =
                    "图片包含出生资料及研究记录。保存后请妥善保管；" +
                        "分享后由目标应用负责传输与存储。",
            ),
        )
    }
}

private fun CaseObjectiveSummaryFailure.toImageFailure(): CaseImageExportFailure =
    CaseImageExportFailure(
        code = when (code) {
            CaseObjectiveSummaryErrorCode.UNSUPPORTED_SUMMARY_VERSION ->
                CaseImageExportErrorCode.UNSUPPORTED_DOCUMENT_VERSION
            CaseObjectiveSummaryErrorCode.NO_ADOPTED_SNAPSHOT ->
                CaseImageExportErrorCode.NO_ADOPTED_SNAPSHOT
            CaseObjectiveSummaryErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS ->
                CaseImageExportErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS
            CaseObjectiveSummaryErrorCode.SUMMARY_UNAVAILABLE,
            CaseObjectiveSummaryErrorCode.CLIPBOARD_UNAVAILABLE,
            CaseObjectiveSummaryErrorCode.COPY_FAILED,
            -> CaseImageExportErrorCode.RENDER_FAILED
        },
        message = message,
    )

private fun CaseObjectiveSummarySection.imageTitle(): String = when (id) {
    "birth_facts" -> "采用资料"
    "chart_facts" -> "基础命盘"
    "fortune_facts" -> "起运与大运"
    "calculation_evidence" -> "计算档案"
    "formal_record_index" -> "研究资料索引"
    else -> title
}

private fun CaseTextRecord.displayText(): String {
    val typeText = when (type) {
        CaseTextRecordType.NOTE -> "笔记"
        CaseTextRecordType.OWNER_FEEDBACK -> "命主反馈"
        CaseTextRecordType.MASTER_COMMENTARY -> "师傅点评"
        CaseTextRecordType.ANALYSIS -> "分析"
    }
    val categoryText = analysisCategory?.displayName()?.let { " · $it" }.orEmpty()
    return "【$typeText$categoryText】$content"
}

private fun AnalysisCategory.displayName(): String = when (this) {
    AnalysisCategory.GENERAL -> "综合"
    AnalysisCategory.PERSONALITY -> "性格"
    AnalysisCategory.CAREER -> "事业"
    AnalysisCategory.WEALTH -> "财运"
    AnalysisCategory.RELATIONSHIP -> "婚姻感情"
    AnalysisCategory.HEALTH -> "健康"
    AnalysisCategory.EDUCATION -> "学业"
    AnalysisCategory.FAMILY -> "家庭"
    AnalysisCategory.OTHER -> "其他"
}

private fun CaseEvent.displayText(): String {
    val date = listOfNotNull(
        year?.toString(),
        month?.toString()?.padStart(2, '0'),
        day?.toString()?.padStart(2, '0'),
    ).joinToString("-").ifEmpty { "日期未知" }
    val titleText = title?.let { " · $it" }.orEmpty()
    val statusText = status?.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
    return "【$date · ${category.displayName()}$titleText$statusText】$rawText"
}

private fun CaseEventCategory.displayName(): String = when (this) {
    CaseEventCategory.GENERAL -> "综合"
    CaseEventCategory.EDUCATION -> "学业"
    CaseEventCategory.CAREER -> "事业"
    CaseEventCategory.WEALTH -> "财运"
    CaseEventCategory.RELATIONSHIP -> "婚恋"
    CaseEventCategory.FAMILY -> "家庭"
    CaseEventCategory.HEALTH -> "健康"
    CaseEventCategory.OTHER -> "其他"
}
