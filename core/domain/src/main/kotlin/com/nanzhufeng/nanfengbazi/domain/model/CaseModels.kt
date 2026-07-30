package com.nanzhufeng.nanfengbazi.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class FieldValueState {
    ABSENT,
    PRESENT,
    CLEARED,
}

@Serializable
data class ExplicitText(
    val state: FieldValueState,
    val value: String? = null,
) {
    init {
        when (state) {
            FieldValueState.PRESENT -> require(!value.isNullOrBlank()) {
                "PRESENT 状态必须包含非空文本"
            }

            FieldValueState.ABSENT,
            FieldValueState.CLEARED,
            -> require(value == null) { "$state 状态不能携带文本" }
        }
    }

    companion object {
        fun absent(): ExplicitText = ExplicitText(FieldValueState.ABSENT)
        fun present(value: String): ExplicitText =
            ExplicitText(FieldValueState.PRESENT, value)

        fun cleared(): ExplicitText = ExplicitText(FieldValueState.CLEARED)
    }
}

@Serializable
enum class CaseSourceType {
    MANUAL,
    WENZHEN_SCREENSHOT,
    BACKUP_RESTORE,
}

@Serializable
data class CaseProfile(
    val occupation: ExplicitText = ExplicitText.absent(),
    val education: ExplicitText = ExplicitText.absent(),
    val finance: ExplicitText = ExplicitText.absent(),
    val marriage: ExplicitText = ExplicitText.absent(),
    val health: ExplicitText = ExplicitText.absent(),
)

@Serializable
enum class CaseTextRecordType {
    NOTE,
    OWNER_FEEDBACK,
    MASTER_COMMENTARY,
    ANALYSIS,
}

@Serializable
data class CaseTextRecord(
    val id: String,
    val type: CaseTextRecordType,
    val content: String,
    val sourceAttachmentId: String? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "记录 id 不能为空" }
        require(content.isNotBlank()) { "记录正文不能为空" }
    }
}

@Serializable
enum class EventDatePrecision {
    YEAR,
    MONTH,
    DAY,
    UNKNOWN,
}

@Serializable
data class CaseEvent(
    val id: String,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val datePrecision: EventDatePrecision = EventDatePrecision.UNKNOWN,
    val stemBranch: String? = null,
    val status: String? = null,
    val rawText: String,
    val normalizedText: ExplicitText = ExplicitText.absent(),
    val sourceAttachmentId: String? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "事件 id 不能为空" }
        require(rawText.isNotBlank()) { "事件原文不能为空" }
        month?.let { require(it in 1..12) { "事件月份超出范围" } }
        day?.let { require(it in 1..31) { "事件日期超出范围" } }
    }
}

@Serializable
data class CaseCalculationSnapshot(
    val id: String,
    val result: CalculationResult,
    val adopted: Boolean,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "快照 id 不能为空" }
    }
}

@Serializable
data class SourceAttachment(
    val id: String,
    val relativePath: String,
    val originalFileName: String,
    val mimeType: String,
    val sha256: String,
    val byteSize: Long,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "附件 id 不能为空" }
        require(relativePath.isNotBlank() && !relativePath.startsWith("/")) {
            "附件必须使用相对路径"
        }
        require('\\' !in relativePath && ':' !in relativePath) { "附件路径格式无效" }
        require(relativePath.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "附件路径不能包含空段、. 或 .."
        }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "附件 SHA-256 格式无效" }
        require(byteSize >= 0) { "附件大小不能为负数" }
    }
}

@Serializable
sealed interface TypedFieldValue {
    @Serializable
    data class Text(val value: String) : TypedFieldValue

    @Serializable
    data class IntegerNumber(val value: Long) : TypedFieldValue

    @Serializable
    data class DecimalNumber(val canonicalValue: String) : TypedFieldValue

    @Serializable
    data class BooleanValue(val value: Boolean) : TypedFieldValue

    @Serializable
    data class DateTimeValue(val value: CivilDateTime) : TypedFieldValue

    @Serializable
    data class FourPillarsValue(val value: FourPillars) : TypedFieldValue
}

@Serializable
data class EvidenceBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(left <= right && top <= bottom) { "证据边界框无效" }
    }
}

@Serializable
data class CaseFieldEvidence(
    val id: String,
    val attachmentId: String,
    val fieldKey: String,
    val rawText: String,
    val normalizedValue: TypedFieldValue? = null,
    val adoptedValue: TypedFieldValue? = null,
    val ocrConfidence: Float? = null,
    val parserConfidence: Float,
    val consistencyConfidence: Float? = null,
    val boundingBox: EvidenceBoundingBox? = null,
    val parserRuleId: String,
    val userEdited: Boolean,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "字段证据 id 不能为空" }
        require(attachmentId.isNotBlank()) { "字段证据必须关联附件" }
        require(fieldKey.isNotBlank()) { "字段 key 不能为空" }
        require(rawText.isNotBlank()) { "字段原文不能为空" }
        require(parserRuleId.isNotBlank()) { "解析规则 id 不能为空" }
        require(parserConfidence in 0f..1f) { "解析置信度超出范围" }
        ocrConfidence?.let { require(it in 0f..1f) { "OCR 置信度超出范围" } }
        consistencyConfidence?.let {
            require(it in 0f..1f) { "一致性置信度超出范围" }
        }
    }
}

@Serializable
data class CaseGroup(
    val id: String,
    val name: String,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
    }
}

@Serializable
data class CaseTag(
    val id: String,
    val name: String,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
    }
}

@Serializable
data class BaziCase(
    val id: String,
    val alias: String,
    val name: ExplicitText,
    val sexForFortuneDirection: SexForFortuneDirection,
    val sourceType: CaseSourceType,
    val birthInput: BirthInput,
    val profile: CaseProfile = CaseProfile(),
    val textRecords: List<CaseTextRecord> = emptyList(),
    val events: List<CaseEvent> = emptyList(),
    val calculationSnapshots: List<CaseCalculationSnapshot> = emptyList(),
    val attachments: List<SourceAttachment> = emptyList(),
    val fieldEvidence: List<CaseFieldEvidence> = emptyList(),
    val groups: List<CaseGroup> = emptyList(),
    val tags: List<CaseTag> = emptyList(),
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank()) { "命例 id 不能为空" }
        require(alias.isNotBlank()) { "命例别名不能为空" }
        require(revision >= 0) { "命例修订号不能为负数" }
        require(createdAt <= updatedAt) { "更新时间不能早于创建时间" }
        require(textRecords.map { it.id }.distinct().size == textRecords.size)
        require(events.map { it.id }.distinct().size == events.size)
        require(calculationSnapshots.map { it.id }.distinct().size == calculationSnapshots.size)
        require(attachments.map { it.id }.distinct().size == attachments.size)
        require(fieldEvidence.map { it.id }.distinct().size == fieldEvidence.size)
        val attachmentIds = attachments.mapTo(mutableSetOf()) { it.id }
        require(fieldEvidence.all { it.attachmentId in attachmentIds }) {
            "字段证据必须关联当前命例的附件"
        }
        require(textRecords.all {
            it.sourceAttachmentId == null || it.sourceAttachmentId in attachmentIds
        }) { "记录来源必须关联当前命例的附件" }
        require(events.all {
            it.sourceAttachmentId == null || it.sourceAttachmentId in attachmentIds
        }) { "事件来源必须关联当前命例的附件" }
    }
}

@Serializable
data class CaseSummary(
    val id: String,
    val alias: String,
    val name: ExplicitText,
    val sexForFortuneDirection: SexForFortuneDirection,
    val sourceType: CaseSourceType,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
    val revision: Long,
)
