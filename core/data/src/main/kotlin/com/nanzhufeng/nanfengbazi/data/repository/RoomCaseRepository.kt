package com.nanzhufeng.nanfengbazi.data.repository

import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.db.CalculationSnapshotEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEventEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseGroupCrossRefEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseGroupEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseTagCrossRefEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseTagEntity
import com.nanzhufeng.nanfengbazi.data.db.FieldEvidenceEntity
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.db.SourceAttachmentEntity
import com.nanzhufeng.nanfengbazi.data.db.TextRecordEntity
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import java.time.Instant
import kotlinx.serialization.json.Json

class RoomCaseRepository(
    private val database: NanfengBaziDatabase,
) : CaseRepository {
    private val dao = database.caseDao()

    override suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult = database.withTransaction {
        val current = dao.findCase(case.id)
        if (current == null) {
            if (expectedRevision != null && expectedRevision != 0L) {
                return@withTransaction CaseWriteResult.RevisionConflict(
                    caseId = case.id,
                    expectedRevision = expectedRevision,
                    actualRevision = 0,
                )
            }
            val persisted = case.copy(revision = 1)
            dao.insertCase(persisted.toCaseEntity())
            insertChildren(persisted)
            CaseWriteResult.Created(case.id, 1)
        } else {
            if (expectedRevision == null) {
                return@withTransaction CaseWriteResult.AlreadyExists(
                    caseId = case.id,
                    revision = current.revision,
                )
            }
            if (expectedRevision != current.revision) {
                return@withTransaction CaseWriteResult.RevisionConflict(
                    caseId = case.id,
                    expectedRevision = expectedRevision,
                    actualRevision = current.revision,
                )
            }
            val nextRevision = current.revision + 1
            val persisted = case.copy(
                createdAt = Instant.ofEpochMilli(current.createdAtEpochMillis),
                revision = nextRevision,
            )
            deleteChildren(case.id)
            dao.updateCase(persisted.toCaseEntity())
            insertChildren(persisted)
            CaseWriteResult.Updated(case.id, nextRevision)
        }
    }

    override suspend fun findById(id: String): BaziCase? = database.withTransaction {
        val entity = dao.findCase(id) ?: return@withTransaction null
        entity.toDomain(
            snapshots = dao.calculationSnapshots(id),
            textRecords = dao.textRecords(id),
            events = dao.events(id),
            attachments = dao.attachments(id),
            fieldEvidence = dao.fieldEvidence(id),
            groups = dao.groups(id),
            tags = dao.tags(id),
        )
    }

    override suspend fun search(query: String): List<CaseSummary> =
        database.withTransaction {
            dao.searchCases(query.trim()).map { entity ->
                val adoptedSnapshot = dao.calculationSnapshots(entity.id)
                    .asReversed()
                    .firstOrNull { it.adopted }
                    ?.let {
                        DomainJson.decodeFromString(
                            CaseCalculationSnapshot.serializer(),
                            it.resultJson,
                        )
                    }
                entity.toSummary(adoptedSnapshot)
            }
        }

    private suspend fun deleteChildren(caseId: String) {
        dao.deleteFieldEvidence(caseId)
        dao.deleteCalculationSnapshots(caseId)
        dao.deleteTextRecords(caseId)
        dao.deleteEvents(caseId)
        dao.deleteCaseGroupCrossRefs(caseId)
        dao.deleteCaseTagCrossRefs(caseId)
        dao.deleteAttachments(caseId)
    }

    private suspend fun insertChildren(case: BaziCase) {
        dao.insertAttachments(case.attachments.mapIndexed { index, value ->
            value.toEntity(case.id, index)
        })
        dao.insertCalculationSnapshots(
            case.calculationSnapshots.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertTextRecords(
            case.textRecords.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertEvents(
            case.events.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertFieldEvidence(
            case.fieldEvidence.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertGroups(case.groups.map { CaseGroupEntity(it.id, it.name) })
        dao.insertTags(case.tags.map { CaseTagEntity(it.id, it.name) })
        dao.insertCaseGroupCrossRefs(
            case.groups.map { CaseGroupCrossRefEntity(case.id, it.id) },
        )
        dao.insertCaseTagCrossRefs(
            case.tags.map { CaseTagCrossRefEntity(case.id, it.id) },
        )
    }
}

internal val DomainJson: Json = Json {
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = false
    classDiscriminator = "_type"
}

private fun BaziCase.toCaseEntity(): CaseEntity = CaseEntity(
    id = id,
    alias = alias,
    nameState = name.state.name,
    nameValue = name.value,
    sexForFortuneDirection = sexForFortuneDirection.name,
    sourceType = sourceType.name,
    birthInputJson = DomainJson.encodeToString(BirthInput.serializer(), birthInput),
    profileJson = DomainJson.encodeToString(CaseProfile.serializer(), profile),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    revision = revision,
)

private fun CaseCalculationSnapshot.toEntity(caseId: String, sortOrder: Int) =
    CalculationSnapshotEntity(
        id = id,
        caseId = caseId,
        resultJson = DomainJson.encodeToString(
            CaseCalculationSnapshot.serializer(),
            this,
        ),
        adopted = adopted,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        sortOrder = sortOrder,
    )

private fun CaseTextRecord.toEntity(caseId: String, sortOrder: Int) = TextRecordEntity(
    id = id,
    caseId = caseId,
    type = type.name,
    content = content,
    sourceAttachmentId = sourceAttachmentId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    sortOrder = sortOrder,
)

private fun CaseEvent.toEntity(caseId: String, sortOrder: Int) = CaseEventEntity(
    id = id,
    caseId = caseId,
    eventJson = DomainJson.encodeToString(CaseEvent.serializer(), this),
    sourceAttachmentId = sourceAttachmentId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    sortOrder = sortOrder,
)

private fun SourceAttachment.toEntity(caseId: String, sortOrder: Int) = SourceAttachmentEntity(
    id = id,
    caseId = caseId,
    relativePath = relativePath,
    originalFileName = originalFileName,
    mimeType = mimeType,
    sha256 = sha256,
    byteSize = byteSize,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    sortOrder = sortOrder,
)

private fun CaseFieldEvidence.toEntity(caseId: String, sortOrder: Int) = FieldEvidenceEntity(
    id = id,
    caseId = caseId,
    attachmentId = attachmentId,
    fieldKey = fieldKey,
    evidenceJson = DomainJson.encodeToString(CaseFieldEvidence.serializer(), this),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    sortOrder = sortOrder,
)

private fun CaseEntity.toDomain(
    snapshots: List<CalculationSnapshotEntity>,
    textRecords: List<TextRecordEntity>,
    events: List<CaseEventEntity>,
    attachments: List<SourceAttachmentEntity>,
    fieldEvidence: List<FieldEvidenceEntity>,
    groups: List<CaseGroupEntity>,
    tags: List<CaseTagEntity>,
): BaziCase = BaziCase(
    id = id,
    alias = alias,
    name = ExplicitText(FieldValueState.valueOf(nameState), nameValue),
    sexForFortuneDirection = SexForFortuneDirection.valueOf(sexForFortuneDirection),
    sourceType = CaseSourceType.valueOf(sourceType),
    birthInput = DomainJson.decodeFromString(BirthInput.serializer(), birthInputJson),
    profile = DomainJson.decodeFromString(CaseProfile.serializer(), profileJson),
    textRecords = textRecords.map {
        CaseTextRecord(
            id = it.id,
            type = CaseTextRecordType.valueOf(it.type),
            content = it.content,
            sourceAttachmentId = it.sourceAttachmentId,
            createdAt = Instant.ofEpochMilli(it.createdAtEpochMillis),
            updatedAt = Instant.ofEpochMilli(it.updatedAtEpochMillis),
        )
    },
    events = events.map {
        DomainJson.decodeFromString(CaseEvent.serializer(), it.eventJson)
    },
    calculationSnapshots = snapshots.map {
        DomainJson.decodeFromString(
            CaseCalculationSnapshot.serializer(),
            it.resultJson,
        )
    },
    attachments = attachments.map {
        SourceAttachment(
            id = it.id,
            relativePath = it.relativePath,
            originalFileName = it.originalFileName,
            mimeType = it.mimeType,
            sha256 = it.sha256,
            byteSize = it.byteSize,
            createdAt = Instant.ofEpochMilli(it.createdAtEpochMillis),
        )
    },
    fieldEvidence = fieldEvidence.map {
        DomainJson.decodeFromString(CaseFieldEvidence.serializer(), it.evidenceJson)
    },
    groups = groups.map { CaseGroup(it.id, it.name) },
    tags = tags.map { CaseTag(it.id, it.name) },
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    revision = revision,
)

private fun CaseEntity.toSummary(
    adoptedSnapshot: CaseCalculationSnapshot?,
): CaseSummary = CaseSummary(
    id = id,
    alias = alias,
    name = ExplicitText(FieldValueState.valueOf(nameState), nameValue),
    sexForFortuneDirection = SexForFortuneDirection.valueOf(sexForFortuneDirection),
    sourceType = CaseSourceType.valueOf(sourceType),
    birthInput = DomainJson.decodeFromString(BirthInput.serializer(), birthInputJson),
    fourPillars = adoptedSnapshot?.result?.fourPillars,
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    revision = revision,
)
