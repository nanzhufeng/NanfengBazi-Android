package com.nanzhufeng.nanfengbazi.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "cases",
    indices = [
        Index(value = ["alias"]),
        Index(value = ["nameValue"]),
        Index(value = ["updatedAtEpochMillis"]),
        Index(value = ["lastViewedAtEpochMillis"]),
        Index(value = ["deletedAtEpochMillis"]),
    ],
)
data class CaseEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val alias: String,
    val nameState: String,
    val nameValue: String?,
    val sexForFortuneDirection: String,
    val sourceType: String,
    val birthInputJson: String,
    val profileJson: String,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val copiedFromCaseId: String? = null,
    val lastViewedAtEpochMillis: Long? = null,
    val deletedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
)

@Serializable
@Entity(
    tableName = "calculation_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["caseId"])],
)
data class CalculationSnapshotEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val caseId: String,
    val resultJson: String,
    val adopted: Boolean,
    val createdAtEpochMillis: Long,
    val sortOrder: Int,
)

@Serializable
@Entity(
    tableName = "text_records",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["sourceAttachmentId"]),
    ],
)
data class TextRecordEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val caseId: String,
    val type: String,
    val content: String,
    val sourceAttachmentId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val sortOrder: Int,
)

@Serializable
@Entity(
    tableName = "case_events",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["sourceAttachmentId"]),
    ],
)
data class CaseEventEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val caseId: String,
    val eventJson: String,
    val sourceAttachmentId: String?,
    val createdAtEpochMillis: Long,
    val sortOrder: Int,
)

@Serializable
@Entity(
    tableName = "source_attachments",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["sha256"]),
    ],
)
data class SourceAttachmentEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val caseId: String,
    val relativePath: String,
    val originalFileName: String,
    val mimeType: String,
    val sha256: String,
    val byteSize: Long,
    val createdAtEpochMillis: Long,
    val sortOrder: Int,
)

@Serializable
@Entity(
    tableName = "field_evidence",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SourceAttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["caseId"]),
        Index(value = ["attachmentId"]),
    ],
)
data class FieldEvidenceEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val caseId: String,
    val attachmentId: String,
    val fieldKey: String,
    val evidenceJson: String,
    val createdAtEpochMillis: Long,
    val sortOrder: Int,
)

@Serializable
@Entity(
    tableName = "case_groups",
)
data class CaseGroupEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val name: String,
)

@Serializable
@Entity(
    tableName = "case_tags",
)
data class CaseTagEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val name: String,
)

@Serializable
@Entity(
    tableName = "case_group_cross_ref",
    primaryKeys = ["caseId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CaseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["groupId"])],
)
data class CaseGroupCrossRefEntity(
    val caseId: String,
    val groupId: String,
)

@Serializable
@Entity(
    tableName = "case_tag_cross_ref",
    primaryKeys = ["caseId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CaseTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tagId"])],
)
data class CaseTagCrossRefEntity(
    val caseId: String,
    val tagId: String,
)

internal data class RoomDataSnapshot(
    val cases: List<CaseEntity>,
    val calculationSnapshots: List<CalculationSnapshotEntity>,
    val textRecords: List<TextRecordEntity>,
    val events: List<CaseEventEntity>,
    val attachments: List<SourceAttachmentEntity>,
    val fieldEvidence: List<FieldEvidenceEntity>,
    val groups: List<CaseGroupEntity>,
    val tags: List<CaseTagEntity>,
    val caseGroupCrossRefs: List<CaseGroupCrossRefEntity>,
    val caseTagCrossRefs: List<CaseTagCrossRefEntity>,
)
