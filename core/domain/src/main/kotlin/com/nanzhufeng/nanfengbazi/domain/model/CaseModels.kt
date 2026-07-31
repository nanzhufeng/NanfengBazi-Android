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
    CASE_COPY,
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
enum class AnalysisCategory {
    GENERAL,
    PERSONALITY,
    CAREER,
    WEALTH,
    RELATIONSHIP,
    HEALTH,
    EDUCATION,
    FAMILY,
    OTHER,
}

@Serializable
data class CaseTextRecord(
    val id: String,
    val type: CaseTextRecordType,
    val content: String,
    val analysisCategory: AnalysisCategory? = null,
    val sourceAttachmentId: String? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "记录 id 不能为空" }
        require(content.isNotBlank()) { "记录正文不能为空" }
        require(type == CaseTextRecordType.ANALYSIS || analysisCategory == null) {
            "非分析记录不能设置分析分类"
        }
    }
}

@Serializable
enum class RecordChangeType {
    CREATED,
    UPDATED,
    DELETED,
}

@Serializable
data class CaseTextRecordRevision(
    val id: String,
    val recordId: String,
    val version: Int,
    val changeType: RecordChangeType,
    val snapshot: CaseTextRecord,
    @Serializable(with = InstantIsoSerializer::class)
    val changedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "记录历史 id 不能为空" }
        require(recordId.isNotBlank()) { "记录历史必须关联记录" }
        require(version > 0) { "记录历史版本必须大于零" }
        require(snapshot.id == recordId) { "记录历史快照身份不一致" }
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
enum class CaseEventCategory {
    GENERAL,
    EDUCATION,
    CAREER,
    WEALTH,
    RELATIONSHIP,
    FAMILY,
    HEALTH,
    OTHER,
}

@Serializable
data class CaseEvent(
    val id: String,
    val title: String? = null,
    val category: CaseEventCategory = CaseEventCategory.GENERAL,
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
        require(title == null || title.isNotBlank()) { "事件标题不能为空白文本" }
        require(rawText.isNotBlank()) { "事件原文不能为空" }
        month?.let { require(it in 1..12) { "事件月份超出范围" } }
        day?.let { require(it in 1..31) { "事件日期超出范围" } }
    }
}

@Serializable
data class CaseEventRevision(
    val id: String,
    val eventId: String,
    val version: Int,
    val changeType: RecordChangeType,
    val snapshot: CaseEvent,
    @Serializable(with = InstantIsoSerializer::class)
    val changedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "事件历史 id 不能为空" }
        require(eventId.isNotBlank()) { "事件历史必须关联事件" }
        require(version > 0) { "事件历史版本必须大于零" }
        require(snapshot.id == eventId) { "事件历史快照身份不一致" }
    }
}

@Serializable
data class CaseCalculationSnapshot(
    val id: String,
    val result: CalculationResult,
    val adopted: Boolean,
    val birthTimeCandidateId: String? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "快照 id 不能为空" }
        require(birthTimeCandidateId == null || birthTimeCandidateId.isNotBlank()) {
            "出生时间候选 id 不能为空"
        }
    }
}

@Serializable
data class BirthTimeCandidate(
    val id: String,
    val label: String,
    val birthInput: BirthInput,
    val calculationSnapshotId: String,
    val adopted: Boolean,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "出生时间候选 id 不能为空" }
        require(label.isNotBlank()) { "出生时间候选标签不能为空" }
        require(label.length <= 60) { "出生时间候选标签不能超过 60 个字符" }
        require(calculationSnapshotId.isNotBlank()) { "出生时间候选必须关联计算快照" }
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
    val calculatedValue: TypedFieldValue? = null,
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
    val birthTimeCandidates: List<BirthTimeCandidate> = emptyList(),
    val profile: CaseProfile = CaseProfile(),
    val textRecords: List<CaseTextRecord> = emptyList(),
    val textRecordRevisions: List<CaseTextRecordRevision> = emptyList(),
    val events: List<CaseEvent> = emptyList(),
    val eventRevisions: List<CaseEventRevision> = emptyList(),
    val calculationSnapshots: List<CaseCalculationSnapshot> = emptyList(),
    val attachments: List<SourceAttachment> = emptyList(),
    val fieldEvidence: List<CaseFieldEvidence> = emptyList(),
    val groups: List<CaseGroup> = emptyList(),
    val tags: List<CaseTag> = emptyList(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val copiedFromCaseId: String? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val lastViewedAt: Instant? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val deletedAt: Instant? = null,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank()) { "命例 id 不能为空" }
        require(alias.isNotBlank()) { "命例别名不能为空" }
        require(copiedFromCaseId == null || copiedFromCaseId.isNotBlank()) {
            "复制来源命例 id 不能为空"
        }
        require(copiedFromCaseId != id) { "命例不能复制自自身" }
        require(revision >= 0) { "命例修订号不能为负数" }
        require(createdAt <= updatedAt) { "更新时间不能早于创建时间" }
        require(
            birthTimeCandidates.map { it.id }.distinct().size == birthTimeCandidates.size,
        ) { "出生时间候选 id 不能重复" }
        require(
            birthTimeCandidates.map { it.calculationSnapshotId }.distinct().size ==
                birthTimeCandidates.size,
        ) { "出生时间候选不能共用计算快照" }
        if (birthTimeCandidates.isNotEmpty()) {
            require(birthTimeCandidates.count { it.adopted } == 1) {
                "出生时间候选必须且只能采用一个"
            }
            require(birthTimeCandidates.single { it.adopted }.birthInput == birthInput) {
                "命例出生输入必须与采用的出生时间候选一致"
            }
        }
        require(textRecords.map { it.id }.distinct().size == textRecords.size)
        require(
            textRecordRevisions.map { it.id }.distinct().size == textRecordRevisions.size,
        )
        require(
            textRecordRevisions
                .groupBy { it.recordId }
                .values
                .all { revisions ->
                    revisions.map { it.version }.distinct().size == revisions.size
                },
        ) { "同一记录的历史版本号不能重复" }
        require(events.map { it.id }.distinct().size == events.size)
        val snapshotsById = calculationSnapshots.associateBy { it.id }
        birthTimeCandidates.forEach { candidate ->
            val snapshot = snapshotsById[candidate.calculationSnapshotId]
            require(snapshot != null) { "出生时间候选必须关联当前命例的计算快照" }
            require(snapshot.birthTimeCandidateId == candidate.id) {
                "出生时间候选与计算快照关联不一致"
            }
            require(snapshot.result.normalizedInput == candidate.birthInput) {
                "出生时间候选输入与计算快照不一致"
            }
            require(snapshot.adopted == candidate.adopted) {
                "出生时间候选与计算快照采用状态不一致"
            }
        }
        require(eventRevisions.map { it.id }.distinct().size == eventRevisions.size)
        require(
            eventRevisions
                .groupBy { it.eventId }
                .values
                .all { revisions ->
                    revisions.map { it.version }.distinct().size == revisions.size
                },
        ) { "同一事件的历史版本号不能重复" }
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
        require(textRecordRevisions.all {
            it.snapshot.sourceAttachmentId == null ||
                it.snapshot.sourceAttachmentId in attachmentIds
        }) { "记录历史来源必须关联当前命例的附件" }
        require(events.all {
            it.sourceAttachmentId == null || it.sourceAttachmentId in attachmentIds
        }) { "事件来源必须关联当前命例的附件" }
        require(eventRevisions.all {
            it.snapshot.sourceAttachmentId == null ||
                it.snapshot.sourceAttachmentId in attachmentIds
        }) { "事件历史来源必须关联当前命例的附件" }
    }
}

@Serializable
data class CaseSummary(
    val id: String,
    val alias: String,
    val name: ExplicitText,
    val sexForFortuneDirection: SexForFortuneDirection,
    val sourceType: CaseSourceType,
    val birthInput: BirthInput,
    val fourPillars: FourPillars?,
    val groups: List<CaseGroup>,
    val tags: List<CaseTag>,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val copiedFromCaseId: String?,
    @Serializable(with = InstantIsoSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val updatedAt: Instant,
    @Serializable(with = InstantIsoSerializer::class)
    val lastViewedAt: Instant?,
    @Serializable(with = InstantIsoSerializer::class)
    val deletedAt: Instant?,
    val revision: Long,
    val canonicalSolarDateTime: CivilDateTime? = null,
    val zodiac: String? = null,
)
