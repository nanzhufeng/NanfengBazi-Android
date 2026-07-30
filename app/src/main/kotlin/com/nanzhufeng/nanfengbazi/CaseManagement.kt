package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import java.time.Clock
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException

sealed interface CaseMutationResult {
    data class Saved(
        val caseId: String,
        val revision: Long,
    ) : CaseMutationResult

    data class ValidationFailed(val message: String) : CaseMutationResult
    data class DuplicateCandidates(
        val candidates: List<DuplicateCaseCandidate>,
    ) : CaseMutationResult
    data class TimeZoneChoiceRequired(
        val timeZoneId: String,
        val validUtcOffsetSeconds: List<Int>,
        val timeZoneDataVersion: String,
    ) : CaseMutationResult
    data object NotFound : CaseMutationResult
    data class RevisionConflict(val actualRevision: Long) : CaseMutationResult
    data class CalculationFailed(val message: String) : CaseMutationResult
    data class StorageFailed(val message: String) : CaseMutationResult
}

class EditCaseUseCase(
    private val baziEngine: BaziEngine,
    private val caseRepository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = UuidGenerator(),
) {
    suspend operator fun invoke(
        caseId: String,
        expectedRevision: Long,
        form: CaseFormState,
        allowDuplicate: Boolean = false,
    ): CaseMutationResult {
        val valid = when (val validation = CaseFormValidator.validate(form)) {
            is CaseFormValidation.Invalid ->
                return CaseMutationResult.ValidationFailed(validation.message)
            is CaseFormValidation.Valid -> validation
        }
        val existing = when (val loaded = loadCase(caseRepository, caseId)) {
            is CaseLoadResult.Found -> loaded.case
            CaseLoadResult.NotFound -> return CaseMutationResult.NotFound
            CaseLoadResult.Failed -> return caseReadFailure()
        }
        if (existing.revision != expectedRevision) {
            return CaseMutationResult.RevisionConflict(existing.revision)
        }
        val profile = CalculationProfile.tymeDefault(
            solarTimeMode = if (valid.birthInput.useTrueSolarTime) {
                SolarTimeMode.TRUE_SOLAR_TIME
            } else {
                SolarTimeMode.CIVIL_TIME
            },
        )
        val calculation = try {
            baziEngine.calculate(valid.birthInput, profile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: TimeZoneChoiceRequiredException) {
            return CaseMutationResult.TimeZoneChoiceRequired(
                timeZoneId = error.timeZoneId,
                validUtcOffsetSeconds = error.validUtcOffsetSeconds,
                timeZoneDataVersion = error.timeZoneDataVersion,
            )
        } catch (error: Exception) {
            val message = if (error is IllegalArgumentException) {
                error.message?.takeIf { it.isNotBlank() }
                    ?: "当前出生资料或计算口径不受支持。"
            } else {
                "计算引擎暂时无法完成排盘，请稍后重试。"
            }
            return CaseMutationResult.CalculationFailed(message)
        }
        if (!allowDuplicate) {
            val candidates = try {
                caseRepository.findDuplicateCandidates(
                    birthInput = calculation.normalizedInput,
                    fourPillars = calculation.fourPillars,
                    canonicalSolarDateTime = calculation.calendarConversion?.solarDateTime,
                    excludeCaseId = caseId,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return caseReadFailure("无法完成重复命例检查，未保存任何更改。")
            }
            if (candidates.isNotEmpty()) {
                return CaseMutationResult.DuplicateCandidates(candidates)
            }
        }
        val now = clock.instant()
        val updated = existing.copy(
            alias = valid.alias,
            name = valid.name?.let(ExplicitText::present) ?: when (existing.name.state) {
                FieldValueState.ABSENT -> ExplicitText.absent()
                FieldValueState.PRESENT,
                FieldValueState.CLEARED,
                -> ExplicitText.cleared()
            },
            sexForFortuneDirection = calculation.normalizedInput.sexForFortuneDirection,
            birthInput = calculation.normalizedInput,
            calculationSnapshots = existing.calculationSnapshots
                .map { it.copy(adopted = false) } +
                CaseCalculationSnapshot(
                    id = idGenerator.nextId(),
                    result = calculation,
                    adopted = true,
                    createdAt = now,
                ),
            updatedAt = now,
        )
        return persistCase(caseRepository, updated, expectedRevision)
    }
}

class CaseLifecycleUseCase(
    private val caseRepository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = UuidGenerator(),
) {
    suspend fun moveToTrash(
        caseId: String,
        expectedRevision: Long,
    ): CaseMutationResult {
        val existing = loadMutableCase(caseId, expectedRevision)
            ?: return lifecycleLoadFailure(caseId, expectedRevision)
        if (existing.deletedAt != null) {
            return CaseMutationResult.ValidationFailed("该命例已经在回收站中。")
        }
        return persistCase(
            caseRepository = caseRepository,
            case = existing.copy(
                isPinned = false,
                deletedAt = clock.instant(),
                updatedAt = clock.instant(),
            ),
            expectedRevision = expectedRevision,
        )
    }

    suspend fun restore(
        caseId: String,
        expectedRevision: Long,
    ): CaseMutationResult {
        val existing = loadMutableCase(caseId, expectedRevision)
            ?: return lifecycleLoadFailure(caseId, expectedRevision)
        if (existing.deletedAt == null) {
            return CaseMutationResult.ValidationFailed("该命例不在回收站中。")
        }
        return persistCase(
            caseRepository = caseRepository,
            case = existing.copy(
                deletedAt = null,
                updatedAt = clock.instant(),
            ),
            expectedRevision = expectedRevision,
        )
    }

    suspend fun duplicate(
        caseId: String,
        expectedRevision: Long,
    ): CaseMutationResult {
        val existing = loadMutableCase(caseId, expectedRevision)
            ?: return lifecycleLoadFailure(caseId, expectedRevision)
        if (existing.deletedAt != null) {
            return CaseMutationResult.ValidationFailed("请先恢复回收站中的命例，再进行复制。")
        }
        val aliases = try {
            caseRepository.search(
                CaseSearchRequest(visibility = CaseVisibility.ALL),
            ).mapTo(mutableSetOf()) { it.alias }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return caseReadFailure("无法检查命例别名，未创建副本。")
        }
        val now = clock.instant()
        val copy = existing.copy(
            id = idGenerator.nextId(),
            alias = nextCopyAlias(existing.alias, aliases),
            sourceType = com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType.CASE_COPY,
            textRecords = emptyList(),
            textRecordRevisions = emptyList(),
            events = emptyList(),
            eventRevisions = emptyList(),
            calculationSnapshots = existing.calculationSnapshots.map {
                it.copy(id = idGenerator.nextId(), createdAt = now)
            },
            attachments = emptyList(),
            fieldEvidence = emptyList(),
            copiedFromCaseId = existing.id,
            isFavorite = false,
            isPinned = false,
            lastViewedAt = null,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
            revision = 0,
        )
        return try {
            when (val result = caseRepository.save(copy, expectedRevision = null)) {
                is CaseWriteResult.Created ->
                    CaseMutationResult.Saved(result.caseId, result.revision)
                is CaseWriteResult.Updated ->
                    CaseMutationResult.Saved(result.caseId, result.revision)
                is CaseWriteResult.AlreadyExists ->
                    CaseMutationResult.StorageFailed("副本 ID 冲突，未创建命例，请重试。")
                is CaseWriteResult.RevisionConflict ->
                    CaseMutationResult.StorageFailed("副本未创建，请刷新命例后重试。")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            CaseMutationResult.StorageFailed("副本未创建，数据库暂时不可用。")
        }
    }

    private suspend fun loadMutableCase(
        caseId: String,
        expectedRevision: Long,
    ): BaziCase? {
        val existing = try {
            caseRepository.findById(caseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return null
        return existing.takeIf { it.revision == expectedRevision }
    }

    private suspend fun lifecycleLoadFailure(
        caseId: String,
        expectedRevision: Long,
    ): CaseMutationResult {
        val current = try {
            caseRepository.findById(caseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return caseReadFailure()
        } ?: return CaseMutationResult.NotFound
        return if (current.revision != expectedRevision) {
            CaseMutationResult.RevisionConflict(current.revision)
        } else {
            caseReadFailure()
        }
    }

    private fun nextCopyAlias(baseAlias: String, existingAliases: Set<String>): String {
        val first = "$baseAlias（副本）"
        if (first !in existingAliases) return first
        var index = 2
        while ("$baseAlias（副本 $index）" in existingAliases) index += 1
        return "$baseAlias（副本 $index）"
    }
}

data class CaseMetadataDraft(
    val groupNames: String = "",
    val tagNames: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
)

class CaseMetadataUseCase(
    private val caseRepository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = UuidGenerator(),
) {
    suspend fun save(
        caseId: String,
        expectedRevision: Long,
        draft: CaseMetadataDraft,
    ): CaseMutationResult {
        val groupNames = when (val result = validateNames(draft.groupNames, "分组")) {
            is NameValidation.Invalid ->
                return CaseMutationResult.ValidationFailed(result.message)
            is NameValidation.Valid -> result.names
        }
        val tagNames = when (val result = validateNames(draft.tagNames, "标签")) {
            is NameValidation.Invalid ->
                return CaseMutationResult.ValidationFailed(result.message)
            is NameValidation.Valid -> result.names
        }
        val existing: BaziCase
        val catalog: List<com.nanzhufeng.nanfengbazi.domain.model.CaseSummary>
        try {
            existing = caseRepository.findById(caseId)
                ?: return CaseMutationResult.NotFound
            catalog = caseRepository.search(CaseSearchRequest())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return caseReadFailure()
        }
        if (existing.revision != expectedRevision) {
            return CaseMutationResult.RevisionConflict(existing.revision)
        }
        val knownGroups = (catalog.flatMap { it.groups } + existing.groups)
            .associateBy { it.name.normalizedMetadataName() }
        val knownTags = (catalog.flatMap { it.tags } + existing.tags)
            .associateBy { it.name.normalizedMetadataName() }
        val updated = existing.copy(
            groups = groupNames.map { name ->
                knownGroups[name.normalizedMetadataName()] ?: CaseGroup(
                    id = idGenerator.nextId(),
                    name = name,
                )
            },
            tags = tagNames.map { name ->
                knownTags[name.normalizedMetadataName()] ?: CaseTag(
                    id = idGenerator.nextId(),
                    name = name,
                )
            },
            isFavorite = draft.isFavorite,
            isPinned = draft.isPinned,
            updatedAt = clock.instant(),
        )
        return persistCase(caseRepository, updated, expectedRevision)
    }

    private sealed interface NameValidation {
        data class Valid(val names: List<String>) : NameValidation
        data class Invalid(val message: String) : NameValidation
    }

    private fun validateNames(raw: String, label: String): NameValidation {
        val names = raw.split(Regex("[,，\\n]"))
            .map(String::trim)
            .filter(String::isNotEmpty)
        if (names.any { it.length > MAX_METADATA_NAME_LENGTH }) {
            return NameValidation.Invalid(
                "${label}名称不能超过 $MAX_METADATA_NAME_LENGTH 个字符。",
            )
        }
        val unique = names.distinctBy(String::normalizedMetadataName)
        if (unique.size > MAX_METADATA_COUNT) {
            return NameValidation.Invalid("每个命例最多设置 $MAX_METADATA_COUNT 个$label。")
        }
        return NameValidation.Valid(unique)
    }

    private companion object {
        const val MAX_METADATA_NAME_LENGTH = 20
        const val MAX_METADATA_COUNT = 10
    }
}

private fun String.normalizedMetadataName(): String = lowercase()

data class TextRecordDraft(
    val type: CaseTextRecordType = CaseTextRecordType.NOTE,
    val content: String = "",
    val analysisCategory: AnalysisCategory = AnalysisCategory.GENERAL,
)

class TextRecordUseCase(
    private val caseRepository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = UuidGenerator(),
) {
    suspend fun save(
        caseId: String,
        expectedRevision: Long,
        recordId: String?,
        draft: TextRecordDraft,
    ): CaseMutationResult {
        val content = draft.content.trim()
        if (content.isEmpty()) {
            return CaseMutationResult.ValidationFailed("记录内容不能为空。")
        }
        val existing = when (val loaded = loadCase(caseRepository, caseId)) {
            is CaseLoadResult.Found -> loaded.case
            CaseLoadResult.NotFound -> return CaseMutationResult.NotFound
            CaseLoadResult.Failed -> return caseReadFailure()
        }
        if (existing.revision != expectedRevision) {
            return CaseMutationResult.RevisionConflict(existing.revision)
        }
        val now = clock.instant()
        val currentRecord = recordId?.let { id ->
            existing.textRecords.firstOrNull { it.id == id }
                ?: return CaseMutationResult.NotFound
        }
        val nextRecord = if (currentRecord == null) {
            CaseTextRecord(
                id = idGenerator.nextId(),
                type = draft.type,
                content = content,
                analysisCategory = draft.analysisCategory.takeIf {
                    draft.type == CaseTextRecordType.ANALYSIS
                },
                createdAt = now,
                updatedAt = now,
            )
        } else {
            currentRecord.copy(
                type = draft.type,
                content = content,
                analysisCategory = draft.analysisCategory.takeIf {
                    draft.type == CaseTextRecordType.ANALYSIS
                },
                updatedAt = now,
            )
        }
        val history = existing.textRecordRevisions
            .ensureRecordBaseline(currentRecord)
            .appendRecordRevision(
                snapshot = nextRecord,
                changeType = if (currentRecord == null) {
                    RecordChangeType.CREATED
                } else {
                    RecordChangeType.UPDATED
                },
                changedAt = now,
            )
        val updated = existing.copy(
            textRecords = if (currentRecord == null) {
                existing.textRecords + nextRecord
            } else {
                existing.textRecords.map {
                    if (it.id == currentRecord.id) nextRecord else it
                }
            },
            textRecordRevisions = history,
            updatedAt = now,
        )
        return persistCase(caseRepository, updated, expectedRevision)
    }

    suspend fun delete(
        caseId: String,
        expectedRevision: Long,
        recordId: String,
    ): CaseMutationResult {
        val existing = when (val loaded = loadCase(caseRepository, caseId)) {
            is CaseLoadResult.Found -> loaded.case
            CaseLoadResult.NotFound -> return CaseMutationResult.NotFound
            CaseLoadResult.Failed -> return caseReadFailure()
        }
        if (existing.revision != expectedRevision) {
            return CaseMutationResult.RevisionConflict(existing.revision)
        }
        val currentRecord = existing.textRecords.firstOrNull { it.id == recordId }
            ?: return CaseMutationResult.NotFound
        val now = clock.instant()
        val history = existing.textRecordRevisions
            .ensureRecordBaseline(currentRecord)
            .appendRecordRevision(
                snapshot = currentRecord,
                changeType = RecordChangeType.DELETED,
                changedAt = now,
            )
        return persistCase(
            caseRepository = caseRepository,
            case = existing.copy(
                textRecords = existing.textRecords.filterNot { it.id == recordId },
                textRecordRevisions = history,
                updatedAt = now,
            ),
            expectedRevision = expectedRevision,
        )
    }
}

data class EventDraft(
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val status: String = "",
    val rawText: String = "",
    val title: String = "",
    val category: CaseEventCategory = CaseEventCategory.GENERAL,
)

private data class ValidEventDraft(
    val title: String?,
    val category: CaseEventCategory,
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val precision: EventDatePrecision,
    val status: String?,
    val rawText: String,
)

class CaseEventUseCase(
    private val caseRepository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = UuidGenerator(),
) {
    suspend fun save(
        caseId: String,
        expectedRevision: Long,
        eventId: String?,
        draft: EventDraft,
    ): CaseMutationResult {
        val valid = when (val validation = validateEvent(draft)) {
            is EventValidation.Invalid ->
                return CaseMutationResult.ValidationFailed(validation.message)
            is EventValidation.Valid -> validation.value
        }
        val existing = when (val loaded = loadCase(caseRepository, caseId)) {
            is CaseLoadResult.Found -> loaded.case
            CaseLoadResult.NotFound -> return CaseMutationResult.NotFound
            CaseLoadResult.Failed -> return caseReadFailure()
        }
        if (existing.revision != expectedRevision) {
            return CaseMutationResult.RevisionConflict(existing.revision)
        }
        val currentEvent = eventId?.let { id ->
            existing.events.firstOrNull { it.id == id }
                ?: return CaseMutationResult.NotFound
        }
        val now = clock.instant()
        val nextEvent = if (currentEvent == null) {
            CaseEvent(
                id = idGenerator.nextId(),
                title = valid.title,
                category = valid.category,
                year = valid.year,
                month = valid.month,
                day = valid.day,
                datePrecision = valid.precision,
                status = valid.status,
                rawText = valid.rawText,
                createdAt = now,
            )
        } else {
            currentEvent.copy(
                title = valid.title,
                category = valid.category,
                year = valid.year,
                month = valid.month,
                day = valid.day,
                datePrecision = valid.precision,
                status = valid.status,
                rawText = valid.rawText,
            )
        }
        val history = existing.eventRevisions
            .ensureEventBaseline(currentEvent)
            .appendEventRevision(
                snapshot = nextEvent,
                changeType = if (currentEvent == null) {
                    RecordChangeType.CREATED
                } else {
                    RecordChangeType.UPDATED
                },
                changedAt = now,
            )
        val updated = existing.copy(
            events = if (currentEvent == null) {
                existing.events + nextEvent
            } else {
                existing.events.map {
                    if (it.id == currentEvent.id) nextEvent else it
                }
            },
            eventRevisions = history,
            updatedAt = now,
        )
        return persistCase(caseRepository, updated, expectedRevision)
    }

    suspend fun delete(
        caseId: String,
        expectedRevision: Long,
        eventId: String,
    ): CaseMutationResult {
        val existing = when (val loaded = loadCase(caseRepository, caseId)) {
            is CaseLoadResult.Found -> loaded.case
            CaseLoadResult.NotFound -> return CaseMutationResult.NotFound
            CaseLoadResult.Failed -> return caseReadFailure()
        }
        if (existing.revision != expectedRevision) {
            return CaseMutationResult.RevisionConflict(existing.revision)
        }
        val currentEvent = existing.events.firstOrNull { it.id == eventId }
            ?: return CaseMutationResult.NotFound
        val now = clock.instant()
        val history = existing.eventRevisions
            .ensureEventBaseline(currentEvent)
            .appendEventRevision(
                snapshot = currentEvent,
                changeType = RecordChangeType.DELETED,
                changedAt = now,
            )
        return persistCase(
            caseRepository = caseRepository,
            case = existing.copy(
                events = existing.events.filterNot { it.id == eventId },
                eventRevisions = history,
                updatedAt = now,
            ),
            expectedRevision = expectedRevision,
        )
    }

    private sealed interface EventValidation {
        data class Valid(val value: ValidEventDraft) : EventValidation
        data class Invalid(val message: String) : EventValidation
    }

    private fun validateEvent(draft: EventDraft): EventValidation {
        val rawText = draft.rawText.trim()
        if (rawText.isEmpty()) {
            return EventValidation.Invalid("事件内容不能为空。")
        }
        val title = draft.title.trim().takeIf { it.isNotEmpty() }
        if (title != null && title.length > MAX_EVENT_TITLE_LENGTH) {
            return EventValidation.Invalid(
                "事件标题不能超过 $MAX_EVENT_TITLE_LENGTH 个字符。",
            )
        }
        val year = draft.year.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val month = draft.month.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val day = draft.day.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (draft.year.isNotBlank() && year == null) {
            return EventValidation.Invalid("事件年份必须是数字。")
        }
        if (draft.month.isNotBlank() && month == null) {
            return EventValidation.Invalid("事件月份必须是数字。")
        }
        if (draft.day.isNotBlank() && day == null) {
            return EventValidation.Invalid("事件日期必须是数字。")
        }
        if (month != null && year == null) {
            return EventValidation.Invalid("填写月份时必须同时填写年份。")
        }
        if (day != null && month == null) {
            return EventValidation.Invalid("填写日期时必须同时填写年份和月份。")
        }
        try {
            when {
                year != null && month != null && day != null -> LocalDate.of(year, month, day)
                year != null && month != null -> YearMonth.of(year, month)
                year != null && year !in 1..9999 ->
                    return EventValidation.Invalid("事件年份超出支持范围。")
            }
        } catch (_: DateTimeException) {
            return EventValidation.Invalid("事件日期无效，请检查年月日。")
        }
        val precision = when {
            day != null -> EventDatePrecision.DAY
            month != null -> EventDatePrecision.MONTH
            year != null -> EventDatePrecision.YEAR
            else -> EventDatePrecision.UNKNOWN
        }
        return EventValidation.Valid(
            ValidEventDraft(
                title = title,
                category = draft.category,
                year = year,
                month = month,
                day = day,
                precision = precision,
                status = draft.status.trim().takeIf { it.isNotEmpty() },
                rawText = rawText,
            ),
        )
    }

    private companion object {
        const val MAX_EVENT_TITLE_LENGTH = 80
    }
}

private fun List<CaseTextRecordRevision>.ensureRecordBaseline(
    current: CaseTextRecord?,
): List<CaseTextRecordRevision> {
    if (current == null || any { it.recordId == current.id }) return this
    return appendRecordRevision(
        snapshot = current,
        changeType = RecordChangeType.CREATED,
        changedAt = current.createdAt,
    )
}

private fun List<CaseTextRecordRevision>.appendRecordRevision(
    snapshot: CaseTextRecord,
    changeType: RecordChangeType,
    changedAt: java.time.Instant,
): List<CaseTextRecordRevision> {
    val version = filter { it.recordId == snapshot.id }
        .maxOfOrNull { it.version }
        ?.plus(1)
        ?: 1
    return this + CaseTextRecordRevision(
        id = "${snapshot.id}:revision:$version",
        recordId = snapshot.id,
        version = version,
        changeType = changeType,
        snapshot = snapshot,
        changedAt = changedAt,
    )
}

private fun List<CaseEventRevision>.ensureEventBaseline(
    current: CaseEvent?,
): List<CaseEventRevision> {
    if (current == null || any { it.eventId == current.id }) return this
    return appendEventRevision(
        snapshot = current,
        changeType = RecordChangeType.CREATED,
        changedAt = current.createdAt,
    )
}

private fun List<CaseEventRevision>.appendEventRevision(
    snapshot: CaseEvent,
    changeType: RecordChangeType,
    changedAt: java.time.Instant,
): List<CaseEventRevision> {
    val version = filter { it.eventId == snapshot.id }
        .maxOfOrNull { it.version }
        ?.plus(1)
        ?: 1
    return this + CaseEventRevision(
        id = "${snapshot.id}:revision:$version",
        eventId = snapshot.id,
        version = version,
        changeType = changeType,
        snapshot = snapshot,
        changedAt = changedAt,
    )
}

private sealed interface CaseLoadResult {
    data class Found(val case: BaziCase) : CaseLoadResult
    data object NotFound : CaseLoadResult
    data object Failed : CaseLoadResult
}

private suspend fun loadCase(
    caseRepository: CaseRepository,
    caseId: String,
): CaseLoadResult = try {
    caseRepository.findById(caseId)?.let(CaseLoadResult::Found)
        ?: CaseLoadResult.NotFound
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    CaseLoadResult.Failed
}

private fun caseReadFailure(
    message: String = "无法读取最新命例，未保存任何更改。请返回详情后重试。",
): CaseMutationResult.StorageFailed =
    CaseMutationResult.StorageFailed(
        message,
    )

private suspend fun persistCase(
    caseRepository: CaseRepository,
    case: BaziCase,
    expectedRevision: Long,
): CaseMutationResult = try {
    when (val result = caseRepository.save(case, expectedRevision)) {
        is CaseWriteResult.Updated -> CaseMutationResult.Saved(
            caseId = result.caseId,
            revision = result.revision,
        )
        is CaseWriteResult.RevisionConflict ->
            CaseMutationResult.RevisionConflict(result.actualRevision)
        is CaseWriteResult.AlreadyExists ->
            CaseMutationResult.RevisionConflict(result.revision)
        is CaseWriteResult.Created -> CaseMutationResult.Saved(
            caseId = result.caseId,
            revision = result.revision,
        )
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    CaseMutationResult.StorageFailed(
        "更改未保存，数据库暂时不可用。输入内容仍保留在当前页面。",
    )
}
