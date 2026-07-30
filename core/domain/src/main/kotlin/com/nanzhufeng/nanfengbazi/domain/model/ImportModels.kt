package com.nanzhufeng.nanfengbazi.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ImportSourceApp {
    WENZHEN_BAZI,
    UNKNOWN,
}

@Serializable
enum class ImportStatus {
    WAITING,
    COPYING_IMAGES,
    CLASSIFYING,
    RECOGNIZING,
    GROUPING_CASES,
    NEEDS_REVIEW,
    READY_TO_COMMIT,
    COMMITTING,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED,
    CANCELLED,
}

fun ImportStatus.canTransitionTo(next: ImportStatus): Boolean {
    if (next == this) return true
    if (next == ImportStatus.CANCELLED && this !in FINAL_IMPORT_STATUSES) return true
    if (next == ImportStatus.FAILED && this in ACTIVE_IMPORT_STATUSES) return true
    return next in IMPORT_TRANSITIONS.getValue(this)
}

private val FINAL_IMPORT_STATUSES = setOf(
    ImportStatus.COMPLETED,
    ImportStatus.CANCELLED,
)

private val ACTIVE_IMPORT_STATUSES = ImportStatus.entries.toSet() - FINAL_IMPORT_STATUSES -
    ImportStatus.FAILED

private val IMPORT_TRANSITIONS = mapOf(
    ImportStatus.WAITING to setOf(ImportStatus.COPYING_IMAGES),
    ImportStatus.COPYING_IMAGES to setOf(ImportStatus.CLASSIFYING),
    ImportStatus.CLASSIFYING to setOf(ImportStatus.RECOGNIZING),
    ImportStatus.RECOGNIZING to setOf(ImportStatus.GROUPING_CASES),
    ImportStatus.GROUPING_CASES to setOf(
        ImportStatus.NEEDS_REVIEW,
        ImportStatus.READY_TO_COMMIT,
        ImportStatus.PARTIALLY_COMPLETED,
    ),
    ImportStatus.NEEDS_REVIEW to setOf(
        ImportStatus.RECOGNIZING,
        ImportStatus.GROUPING_CASES,
        ImportStatus.READY_TO_COMMIT,
    ),
    ImportStatus.READY_TO_COMMIT to setOf(
        ImportStatus.NEEDS_REVIEW,
        ImportStatus.COMMITTING,
    ),
    ImportStatus.COMMITTING to setOf(
        ImportStatus.COMPLETED,
        ImportStatus.PARTIALLY_COMPLETED,
    ),
    ImportStatus.PARTIALLY_COMPLETED to setOf(
        ImportStatus.NEEDS_REVIEW,
        ImportStatus.COMMITTING,
    ),
    ImportStatus.FAILED to setOf(
        ImportStatus.COPYING_IMAGES,
        ImportStatus.CLASSIFYING,
        ImportStatus.RECOGNIZING,
        ImportStatus.GROUPING_CASES,
        ImportStatus.NEEDS_REVIEW,
    ),
    ImportStatus.COMPLETED to emptySet(),
    ImportStatus.CANCELLED to emptySet(),
)

@Serializable
enum class WenzhenPageType {
    HOME_INPUT,
    USER_LIST,
    BASIC_INFO,
    BASIC_CHART,
    PROFESSIONAL_CHART,
    COMMENTARY,
    FEEDBACK,
    UNKNOWN,
}

@Serializable
data class ImportImageRef(
    val id: String,
    val originalFileName: String,
    val mimeType: String,
    val relativePath: String,
    val sha256: String,
    val perceptualHash: String? = null,
    val byteSize: Long,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val pageType: WenzhenPageType = WenzhenPageType.UNKNOWN,
    val pageConfidence: Float? = null,
    val classifierVersion: String? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "导入图片 id 不能为空" }
        require(originalFileName.isNotBlank()) { "导入图片原文件名不能为空" }
        require(mimeType.startsWith("image/")) { "导入文件必须是图片" }
        require(relativePath.isSafeRelativePath()) { "导入图片路径格式无效" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "导入图片 SHA-256 格式无效" }
        require(byteSize > 0) { "导入图片不能为空" }
        require(widthPx == null || widthPx > 0) { "导入图片宽度必须为正数" }
        require(heightPx == null || heightPx > 0) { "导入图片高度必须为正数" }
        pageConfidence?.let { require(it in 0f..1f) { "页面分类置信度超出范围" } }
        require(classifierVersion == null || classifierVersion.isNotBlank()) {
            "页面分类器版本不能为空"
        }
    }
}

@Serializable
data class OcrTextBlock(
    val id: String,
    val text: String,
    val confidence: Float?,
    val boundingBox: EvidenceBoundingBox?,
) {
    init {
        require(id.isNotBlank()) { "OCR 文本块 id 不能为空" }
        require(text.isNotBlank()) { "OCR 文本块内容不能为空" }
        confidence?.let { require(it in 0f..1f) { "OCR 文本块置信度超出范围" } }
    }
}

@Serializable
data class OcrDocument(
    val imageId: String,
    val rawText: String,
    val blocks: List<OcrTextBlock>,
    val engineId: String,
    val engineVersion: String,
    @Serializable(with = InstantIsoSerializer::class)
    val recognizedAt: Instant,
) {
    init {
        require(imageId.isNotBlank()) { "OCR 文档必须关联图片" }
        require(rawText.isNotBlank()) { "OCR 原文不能为空" }
        require(blocks.map { it.id }.distinct().size == blocks.size) {
            "OCR 文本块 id 不能重复"
        }
        require(engineId.isNotBlank()) { "OCR 引擎 id 不能为空" }
        require(engineVersion.isNotBlank()) { "OCR 引擎版本不能为空" }
    }
}

@Serializable
enum class ImportedLongTextType {
    OWNER_FEEDBACK,
    MASTER_COMMENTARY,
    UNKNOWN,
}

@Serializable
data class ImportedLongTextEvidence(
    val id: String,
    val imageId: String,
    val type: ImportedLongTextType,
    val rawText: String,
    val ocrConfidence: Float?,
    val parserConfidence: Float,
    val parserRuleId: String,
    val userEdited: Boolean = false,
    val adopted: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "长文本证据 id 不能为空" }
        require(imageId.isNotBlank()) { "长文本证据必须关联图片" }
        require(rawText.isNotBlank()) { "长文本原文不能为空" }
        ocrConfidence?.let { require(it in 0f..1f) { "OCR 置信度超出范围" } }
        require(parserConfidence in 0f..1f) { "解析置信度超出范围" }
        require(parserRuleId.isNotBlank()) { "解析规则 id 不能为空" }
    }
}

@Serializable
data class ImportCaseCandidate(
    val id: String,
    val imageIds: List<String>,
    val fieldEvidenceIds: List<String> = emptyList(),
    val longTextEvidenceIds: List<String> = emptyList(),
    val suggestedAlias: String? = null,
    val targetCaseId: String? = null,
    val groupingConfidence: Float?,
    val requiresReview: Boolean,
) {
    init {
        require(id.isNotBlank()) { "导入命例候选 id 不能为空" }
        require(imageIds.isNotEmpty()) { "导入命例候选至少关联一张图片" }
        require(imageIds.distinct().size == imageIds.size) { "候选图片不能重复" }
        require(fieldEvidenceIds.distinct().size == fieldEvidenceIds.size) {
            "候选字段证据不能重复"
        }
        require(longTextEvidenceIds.distinct().size == longTextEvidenceIds.size) {
            "候选长文本证据不能重复"
        }
        require(suggestedAlias == null || suggestedAlias.isNotBlank()) {
            "建议别名不能为空"
        }
        require(targetCaseId == null || targetCaseId.isNotBlank()) { "目标命例 id 不能为空" }
        groupingConfidence?.let { require(it in 0f..1f) { "归组置信度超出范围" } }
    }
}

@Serializable
data class ImportComparisonItem(
    val fieldKey: String,
    val importedValue: TypedFieldValue?,
    val calculatedValue: TypedFieldValue?,
    val matches: Boolean?,
) {
    init {
        require(fieldKey.isNotBlank()) { "对照字段 key 不能为空" }
    }
}

@Serializable
data class ImportComparisonReport(
    val items: List<ImportComparisonItem>,
    val engineVersion: String,
    @Serializable(with = InstantIsoSerializer::class)
    val comparedAt: Instant,
) {
    init {
        require(engineVersion.isNotBlank()) { "对照引擎版本不能为空" }
        require(items.map { it.fieldKey }.distinct().size == items.size) {
            "对照字段不能重复"
        }
    }
}

@Serializable
data class ImportFailure(
    val code: String,
    val userMessage: String,
    val retryable: Boolean,
    val failedStage: ImportStatus,
    val diagnosticId: String,
) {
    init {
        require(code.isNotBlank()) { "导入错误码不能为空" }
        require(userMessage.isNotBlank()) { "导入错误提示不能为空" }
        require(diagnosticId.isNotBlank()) { "导入诊断 id 不能为空" }
        require(failedStage !in FINAL_IMPORT_STATUSES) { "完成状态不能作为失败阶段" }
    }
}

@Serializable
data class ImportImageFailure(
    val imageId: String,
    val code: String,
    val userMessage: String,
    val retryable: Boolean,
    val diagnosticId: String,
) {
    init {
        require(imageId.isNotBlank()) { "失败结果必须关联导入图片" }
        require(code.isNotBlank()) { "图片失败错误码不能为空" }
        require(userMessage.isNotBlank()) { "图片失败提示不能为空" }
        require(diagnosticId.isNotBlank()) { "图片失败诊断 id 不能为空" }
    }
}

@Serializable
data class ImportSession(
    val id: String,
    val sourceApp: ImportSourceApp,
    val status: ImportStatus,
    val images: List<ImportImageRef> = emptyList(),
    val ocrDocuments: List<OcrDocument> = emptyList(),
    val extractedFields: List<CaseFieldEvidence> = emptyList(),
    val extractedLongTexts: List<ImportedLongTextEvidence> = emptyList(),
    val caseCandidates: List<ImportCaseCandidate> = emptyList(),
    val comparisonReport: ImportComparisonReport? = null,
    val targetCaseId: String? = null,
    val parserVersion: String,
    val failure: ImportFailure? = null,
    val imageFailures: List<ImportImageFailure> = emptyList(),
    val attemptCount: Int = 0,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val completedAt: Instant? = null,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank()) { "导入会话 id 不能为空" }
        require(parserVersion.isNotBlank()) { "解析器版本不能为空" }
        require(targetCaseId == null || targetCaseId.isNotBlank()) { "目标命例 id 不能为空" }
        require(attemptCount >= 0) { "导入尝试次数不能为负数" }
        require(revision >= 0) { "导入会话修订号不能为负数" }
        require(updatedAt >= createdAt) { "导入会话更新时间不能早于创建时间" }
        require(images.map { it.id }.distinct().size == images.size) { "导入图片 id 不能重复" }
        require(ocrDocuments.map { it.imageId }.distinct().size == ocrDocuments.size) {
            "每张图片只能保留一份当前 OCR 文档"
        }
        require(extractedFields.map { it.id }.distinct().size == extractedFields.size) {
            "导入字段证据 id 不能重复"
        }
        require(extractedLongTexts.map { it.id }.distinct().size == extractedLongTexts.size) {
            "导入长文本证据 id 不能重复"
        }
        require(caseCandidates.map { it.id }.distinct().size == caseCandidates.size) {
            "导入命例候选 id 不能重复"
        }
        require(imageFailures.map { it.imageId }.distinct().size == imageFailures.size) {
            "每张图片只能保留一份当前失败结果"
        }
        val imageIds = images.mapTo(mutableSetOf()) { it.id }
        require(ocrDocuments.all { it.imageId in imageIds }) {
            "OCR 文档必须关联当前导入会话图片"
        }
        require(extractedFields.all { it.attachmentId in imageIds }) {
            "字段证据必须关联当前导入会话图片"
        }
        require(extractedLongTexts.all { it.imageId in imageIds }) {
            "长文本证据必须关联当前导入会话图片"
        }
        require(imageFailures.all { it.imageId in imageIds }) {
            "图片失败结果必须关联当前导入会话图片"
        }
        val fieldIds = extractedFields.mapTo(mutableSetOf()) { it.id }
        val longTextIds = extractedLongTexts.mapTo(mutableSetOf()) { it.id }
        require(
            caseCandidates.all { candidate ->
                candidate.imageIds.all(imageIds::contains) &&
                    candidate.fieldEvidenceIds.all(fieldIds::contains) &&
                    candidate.longTextEvidenceIds.all(longTextIds::contains)
            },
        ) { "导入命例候选包含当前会话之外的证据" }
        require((status == ImportStatus.FAILED) == (failure != null)) {
            "仅失败会话可以携带失败信息"
        }
        val needsCompletedAt = status in setOf(
            ImportStatus.COMPLETED,
            ImportStatus.PARTIALLY_COMPLETED,
            ImportStatus.CANCELLED,
        )
        require(needsCompletedAt == (completedAt != null)) {
            "完成、部分完成或取消会话必须记录结束时间"
        }
        completedAt?.let { require(it >= createdAt) { "导入结束时间不能早于创建时间" } }
    }
}

private fun String.isSafeRelativePath(): Boolean =
    isNotBlank() &&
        !startsWith("/") &&
        '\\' !in this &&
        ':' !in this &&
        split('/').none { it.isBlank() || it == "." || it == ".." }
