package com.nanzhufeng.nanfengbazi.data.repository

import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.db.CalculationSnapshotEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEventEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEventRevisionEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseGroupCrossRefEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseGroupEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseTagCrossRefEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseTagEntity
import com.nanzhufeng.nanfengbazi.data.db.FieldEvidenceEntity
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.db.RoomDataSnapshot
import com.nanzhufeng.nanfengbazi.data.db.SourceAttachmentEntity
import com.nanzhufeng.nanfengbazi.data.db.TextRecordEntity
import com.nanzhufeng.nanfengbazi.data.db.TextRecordRevisionEntity
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.BasicShenShaRules
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.SeasonalWuxingStateResolver
import com.nanzhufeng.nanfengbazi.domain.searchCases
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthTimeCandidate
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import com.nanzhufeng.nanfengbazi.domain.model.resolveLegacySourceType
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json

private const val CASE_ID_SQL_BATCH_SIZE = 900

class RoomCaseRepository(
    private val database: NanfengBaziDatabase,
) : CaseRepository {
    private val dao = database.caseDao()

    override suspend fun listGroups(libraryType: CaseLibraryType?): List<CaseGroup> =
        (libraryType?.let { dao.groupsByLibrary(it.name) } ?: dao.allGroups()).map {
        CaseGroup(
            id = it.id,
            name = it.name,
            libraryType = CaseLibraryType.valueOf(it.libraryType),
        )
    }

    override suspend fun createGroup(
        name: String,
        libraryType: CaseLibraryType,
    ): CaseGroup? = database.withTransaction {
        val normalized = name.trim()
        if (normalized.isBlank() || dao.groupNameCount(normalized, libraryType.name) > 0) {
            return@withTransaction null
        }
        val group = CaseGroup(
            id = UUID.randomUUID().toString(),
            name = normalized,
            libraryType = libraryType,
        )
        dao.insertGroup(
            CaseGroupEntity(
                id = group.id,
                name = group.name,
                sortOrder = dao.maxGroupSortOrder(libraryType.name) + 1,
                libraryType = libraryType.name,
            ),
        )
        group
    }

    override suspend fun renameGroup(groupId: String, name: String): Boolean =
        database.withTransaction {
            val normalized = name.trim()
            if (normalized.isBlank()) return@withTransaction false
            val existing = dao.allGroups().firstOrNull { it.id == groupId }
                ?: return@withTransaction false
            if (existing.name.equals(normalized, ignoreCase = true)) {
                return@withTransaction true
            }
            if (dao.groupNameCount(normalized, existing.libraryType) > 0) {
                return@withTransaction false
            }
            dao.renameGroup(groupId, normalized) == 1
        }

    override suspend fun reorderGroups(
        groupIds: List<String>,
        libraryType: CaseLibraryType,
    ): Boolean =
        database.withTransaction {
            val existing = dao.groupsByLibrary(libraryType.name).map { it.id }
            if (groupIds.toSet() != existing.toSet()) return@withTransaction false
            groupIds.forEachIndexed { index, groupId ->
                dao.updateGroupSortOrder(groupId, index)
            }
            true
        }

    override suspend fun deleteGroup(groupId: String): Boolean =
        database.withTransaction { dao.deleteGroup(groupId) == 1 }

    override suspend fun setCasesPinned(
        caseIds: Set<String>,
        pinned: Boolean,
        updatedAt: Instant,
    ): Int = if (caseIds.isEmpty()) {
        0
    } else {
        database.withTransaction {
            caseIds.chunked(CASE_ID_SQL_BATCH_SIZE).sumOf { caseIdBatch ->
                dao.setCasesPinned(caseIdBatch, pinned, updatedAt.toEpochMilli())
            }
        }
    }

    override suspend fun moveCasesToTrash(
        caseIds: Set<String>,
        deletedAt: Instant,
    ): Int = if (caseIds.isEmpty()) {
        0
    } else {
        database.withTransaction {
            caseIds.chunked(CASE_ID_SQL_BATCH_SIZE).sumOf { caseIdBatch ->
                dao.moveCasesToTrash(caseIdBatch, deletedAt.toEpochMilli())
            }
        }
    }

    override suspend fun deleteTrashedCasesPermanently(caseIds: Set<String>): Int =
        if (caseIds.isEmpty()) {
            0
        } else {
            database.withTransaction {
                caseIds.chunked(CASE_ID_SQL_BATCH_SIZE).sumOf { caseIdBatch ->
                    dao.deleteTrashedCasesPermanently(caseIdBatch)
                }
            }
        }

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

    internal suspend fun insertRestored(case: BaziCase): CaseWriteResult =
        database.withTransaction {
            require(case.revision > 0) { "恢复命例必须携带已持久化修订号" }
            val current = dao.findCase(case.id)
            if (current != null) {
                return@withTransaction CaseWriteResult.AlreadyExists(
                    caseId = case.id,
                    revision = current.revision,
                )
            }
            dao.insertCase(case.toCaseEntity())
            insertChildren(case)
            CaseWriteResult.Created(case.id, case.revision)
        }

    override suspend fun findById(id: String): BaziCase? = database.withTransaction {
        val entity = dao.findCase(id) ?: return@withTransaction null
        entity.toDomain(
            snapshots = dao.calculationSnapshots(id),
            textRecords = dao.textRecords(id),
            textRecordRevisions = dao.textRecordRevisions(id),
            events = dao.events(id),
            eventRevisions = dao.eventRevisions(id),
            attachments = dao.attachments(id),
            fieldEvidence = dao.fieldEvidence(id),
            groups = dao.groups(id),
            tags = dao.tags(id),
        )
    }

    override suspend fun search(request: CaseSearchRequest): List<CaseSummary> =
        database.withTransaction {
            loadSummaries().searchCases(request)
        }

    override suspend fun searchBatch(
        requests: List<CaseSearchRequest>,
    ): List<List<CaseSummary>> = database.withTransaction {
        if (requests.isEmpty()) return@withTransaction emptyList()
        val summaries = loadSummaries()
        requests.map { request -> summaries.searchCases(request) }
    }

    private suspend fun loadSummaries(): List<CaseSummary> {
        val adoptedSnapshots = dao.adoptedCalculationSnapshots()
            .groupBy { snapshot -> snapshot.caseId }
            .mapValues { (_, snapshots) ->
                snapshots.asReversed().firstOrNull { snapshot -> snapshot.adopted }
            }
        val groupsById = dao.allGroups().associateBy { group -> group.id }
        val groupIdsByCase = dao.allCaseGroupCrossRefs().groupBy { reference -> reference.caseId }
        val tagsById = dao.allTags().associateBy { tag -> tag.id }
        val tagIdsByCase = dao.allCaseTagCrossRefs().groupBy { reference -> reference.caseId }

        return dao.allCases().map { entity ->
            val adoptedSnapshot = adoptedSnapshots[entity.id]?.let { snapshot ->
                DomainJson.decodeFromString(
                    CaseCalculationSnapshot.serializer(),
                    snapshot.resultJson,
                )
            }
            val groups = groupIdsByCase[entity.id].orEmpty()
                .mapNotNull { reference -> groupsById[reference.groupId] }
                .sortedWith(compareBy({ group -> group.name }, { group -> group.id }))
                .map { group ->
                    CaseGroup(
                        group.id,
                        group.name,
                        CaseLibraryType.valueOf(group.libraryType),
                    )
                }
            val tags = tagIdsByCase[entity.id].orEmpty()
                .mapNotNull { reference -> tagsById[reference.tagId] }
                .sortedWith(compareBy({ tag -> tag.name }, { tag -> tag.id }))
                .map { tag -> CaseTag(tag.id, tag.name) }
            entity.toSummary(
                adoptedSnapshot = adoptedSnapshot,
                groups = groups,
                tags = tags,
            )
        }
    }

    override suspend fun findDuplicateCandidates(
        birthInput: BirthInput,
        fourPillars: FourPillars?,
        canonicalSolarDateTime: CivilDateTime?,
        excludeCaseId: String?,
    ): List<DuplicateCaseCandidate> =
        search(CaseSearchRequest(visibility = CaseVisibility.ALL))
            .asSequence()
            .filterNot { it.id == excludeCaseId }
            .mapNotNull { summary ->
                val reasons = buildSet {
                    if (
                        summary.hasSameBirthIdentity(
                            other = birthInput,
                            otherCanonicalSolarDateTime = canonicalSolarDateTime,
                        )
                    ) {
                        add(DuplicateReason.SAME_BIRTH_INPUT)
                    }
                    if (fourPillars != null && summary.fourPillars == fourPillars) {
                        add(DuplicateReason.SAME_FOUR_PILLARS)
                    }
                }
                reasons.takeIf { it.isNotEmpty() }?.let {
                    DuplicateCaseCandidate(summary, it)
                }
            }
            .toList()

    override suspend fun markViewed(caseId: String, viewedAt: Instant): Boolean =
        dao.markViewed(caseId, viewedAt.toEpochMilli()) == 1

    private suspend fun deleteChildren(caseId: String) {
        dao.deleteFieldEvidence(caseId)
        dao.deleteCalculationSnapshots(caseId)
        dao.deleteTextRecords(caseId)
        dao.deleteTextRecordRevisions(caseId)
        dao.deleteEvents(caseId)
        dao.deleteEventRevisions(caseId)
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
        dao.insertTextRecordRevisions(
            case.textRecordRevisions.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertEvents(
            case.events.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertEventRevisions(
            case.eventRevisions.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertFieldEvidence(
            case.fieldEvidence.mapIndexed { index, value ->
                value.toEntity(case.id, index)
            },
        )
        dao.insertGroups(
            case.groups.map {
                CaseGroupEntity(
                    id = it.id,
                    name = it.name,
                    libraryType = it.libraryType.name,
                )
            },
        )
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
    libraryType = libraryType.name,
    birthInputJson = DomainJson.encodeToString(BirthInput.serializer(), birthInput),
    birthTimeCandidatesJson = DomainJson.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(BirthTimeCandidate.serializer()),
        birthTimeCandidates,
    ),
    profileJson = DomainJson.encodeToString(CaseProfile.serializer(), profile),
    isFavorite = isFavorite,
    isPinned = isPinned,
    copiedFromCaseId = copiedFromCaseId,
    lastViewedAtEpochMillis = lastViewedAt?.toEpochMilli(),
    deletedAtEpochMillis = deletedAt?.toEpochMilli(),
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
    analysisCategory = analysisCategory?.name,
    sourceType = sourceType.name,
    sourceAttachmentId = sourceAttachmentId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    sortOrder = sortOrder,
)

private fun CaseTextRecordRevision.toEntity(caseId: String, sortOrder: Int) =
    TextRecordRevisionEntity(
        id = id,
        caseId = caseId,
        recordId = recordId,
        version = version,
        changeType = changeType.name,
        revisionJson = DomainJson.encodeToString(
            CaseTextRecordRevision.serializer(),
            this,
        ),
        changedAtEpochMillis = changedAt.toEpochMilli(),
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

private fun CaseEventRevision.toEntity(caseId: String, sortOrder: Int) =
    CaseEventRevisionEntity(
        id = id,
        caseId = caseId,
        eventId = eventId,
        version = version,
        changeType = changeType.name,
        revisionJson = DomainJson.encodeToString(
            CaseEventRevision.serializer(),
            this,
        ),
        changedAtEpochMillis = changedAt.toEpochMilli(),
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

internal fun CaseEntity.toDomain(
    snapshots: List<CalculationSnapshotEntity>,
    textRecords: List<TextRecordEntity>,
    textRecordRevisions: List<TextRecordRevisionEntity>,
    events: List<CaseEventEntity>,
    eventRevisions: List<CaseEventRevisionEntity>,
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
    libraryType = runCatching { CaseLibraryType.valueOf(libraryType) }
        .getOrDefault(CaseLibraryType.USER),
    birthTimeCandidates = DomainJson.decodeFromString(
        kotlinx.serialization.builtins.ListSerializer(BirthTimeCandidate.serializer()),
        birthTimeCandidatesJson,
    ),
    profile = DomainJson.decodeFromString(CaseProfile.serializer(), profileJson),
    textRecords = textRecords.map {
        CaseTextRecord(
            id = it.id,
            type = CaseTextRecordType.valueOf(it.type),
            content = it.content,
            analysisCategory = it.analysisCategory?.let(AnalysisCategory::valueOf),
            sourceType = runCatching {
                TextRecordSourceType.valueOf(it.sourceType)
            }.getOrDefault(TextRecordSourceType.LEGACY_UNSPECIFIED),
            sourceAttachmentId = it.sourceAttachmentId,
            createdAt = Instant.ofEpochMilli(it.createdAtEpochMillis),
            updatedAt = Instant.ofEpochMilli(it.updatedAtEpochMillis),
        ).resolveLegacySourceType()
    },
    textRecordRevisions = textRecordRevisions.map {
        DomainJson.decodeFromString(CaseTextRecordRevision.serializer(), it.revisionJson)
            .let { revision ->
                revision.copy(snapshot = revision.snapshot.resolveLegacySourceType())
            }
    },
    events = events.map {
        DomainJson.decodeFromString(CaseEvent.serializer(), it.eventJson)
    },
    eventRevisions = eventRevisions.map {
        DomainJson.decodeFromString(CaseEventRevision.serializer(), it.revisionJson)
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
    groups = groups.map {
        CaseGroup(it.id, it.name, CaseLibraryType.valueOf(it.libraryType))
    },
    tags = tags.map { CaseTag(it.id, it.name) },
    isFavorite = isFavorite,
    isPinned = isPinned,
    copiedFromCaseId = copiedFromCaseId,
    lastViewedAt = lastViewedAtEpochMillis?.let(Instant::ofEpochMilli),
    deletedAt = deletedAtEpochMillis?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    revision = revision,
)

internal fun RoomDataSnapshot.toDomainCases(): List<BaziCase> = cases.map { entity ->
    val caseId = entity.id
    val groupIds = caseGroupCrossRefs.asSequence()
        .filter { it.caseId == caseId }
        .map { it.groupId }
        .toSet()
    val tagIds = caseTagCrossRefs.asSequence()
        .filter { it.caseId == caseId }
        .map { it.tagId }
        .toSet()
    entity.toDomain(
        snapshots = calculationSnapshots.filter { it.caseId == caseId },
        textRecords = textRecords.filter { it.caseId == caseId },
        textRecordRevisions = textRecordRevisions.filter { it.caseId == caseId },
        events = events.filter { it.caseId == caseId },
        eventRevisions = eventRevisions.filter { it.caseId == caseId },
        attachments = attachments.filter { it.caseId == caseId },
        fieldEvidence = fieldEvidence.filter { it.caseId == caseId },
        groups = groups.filter { it.id in groupIds }.sortedWith(compareBy({ it.name }, { it.id })),
        tags = tags.filter { it.id in tagIds }.sortedWith(compareBy({ it.name }, { it.id })),
    )
}

private fun CaseEntity.toSummary(
    adoptedSnapshot: CaseCalculationSnapshot?,
    groups: List<CaseGroup>,
    tags: List<CaseTag>,
): CaseSummary {
    val result = adoptedSnapshot?.result
    val pillars = result?.fourPillars
    return CaseSummary(
    id = id,
    alias = alias,
    name = ExplicitText(FieldValueState.valueOf(nameState), nameValue),
    sexForFortuneDirection = SexForFortuneDirection.valueOf(sexForFortuneDirection),
    sourceType = CaseSourceType.valueOf(sourceType),
    birthInput = DomainJson.decodeFromString(BirthInput.serializer(), birthInputJson),
    libraryType = runCatching { CaseLibraryType.valueOf(libraryType) }
        .getOrDefault(CaseLibraryType.USER),
    fourPillars = pillars,
    groups = groups,
    tags = tags,
    isFavorite = isFavorite,
    isPinned = isPinned,
    copiedFromCaseId = copiedFromCaseId,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    lastViewedAt = lastViewedAtEpochMillis?.let(Instant::ofEpochMilli),
    deletedAt = deletedAtEpochMillis?.let(Instant::ofEpochMilli),
    revision = revision,
    canonicalSolarDateTime = adoptedSnapshot
        ?.result
        ?.calendarConversion
        ?.solarDateTime,
    zodiac = adoptedSnapshot
        ?.result
        ?.basicChartDetails
        ?.zodiac,
    westernZodiac = adoptedSnapshot
        ?.result
        ?.basicChartDetails
        ?.westernZodiac,
    seasonalWuxingStates = pillars?.month?.getOrNull(1)
        ?.let(SeasonalWuxingStateResolver::resolve)
        .orEmpty(),
    shenShaNames = result?.basicChartDetails?.pillars
        ?.let(BasicShenShaRules::resolve)
        ?.flatMap { it.names }
        ?.toSet()
        .orEmpty(),
    pillarStemTenGods = PillarPosition.entries.map { position ->
        result?.basicChartDetails?.pillars
            ?.firstOrNull { it.position == position }
            ?.primaryTenGod
            .orEmpty()
    },
    pillarBranchTenGods = PillarPosition.entries.map { position ->
        result?.basicChartDetails?.pillars
            ?.firstOrNull { it.position == position }
            ?.hiddenStems
            ?.firstOrNull()
            ?.tenGod
            .orEmpty()
    },
    )
}

private fun CaseSummary.hasSameBirthIdentity(
    other: BirthInput,
    otherCanonicalSolarDateTime: CivilDateTime?,
): Boolean = birthInput.hasSameBirthIdentity(
    other = other,
    canonicalSolarDateTime = canonicalSolarDateTime,
    otherCanonicalSolarDateTime = otherCanonicalSolarDateTime,
)

internal fun BirthInput.hasSameBirthIdentity(
    other: BirthInput,
    canonicalSolarDateTime: CivilDateTime? = null,
    otherCanonicalSolarDateTime: CivilDateTime? = null,
): Boolean =
    sexForFortuneDirection == other.sexForFortuneDirection &&
        if (canonicalSolarDateTime != null && otherCanonicalSolarDateTime != null) {
            canonicalSolarDateTime == otherCanonicalSolarDateTime
        } else {
            calendarInput == other.calendarInput
        }
