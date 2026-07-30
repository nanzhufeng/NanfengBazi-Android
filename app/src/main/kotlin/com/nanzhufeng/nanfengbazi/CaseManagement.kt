package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
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
        val profile = CalculationProfile.tymeDefault()
        val calculation = try {
            baziEngine.calculate(valid.birthInput, profile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = if (error is IllegalArgumentException) {
                error.message?.takeIf { it.isNotBlank() }
                    ?: "当前出生资料或计算口径不受支持。"
            } else {
                "计算引擎暂时无法完成排盘，请稍后重试。"
            }
            return CaseMutationResult.CalculationFailed(message)
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
            sexForFortuneDirection = valid.birthInput.sexForFortuneDirection,
            birthInput = valid.birthInput,
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
                createdAt = now,
                updatedAt = now,
            )
        } else {
            currentRecord.copy(
                type = draft.type,
                content = content,
                updatedAt = now,
            )
        }
        val updated = existing.copy(
            textRecords = if (currentRecord == null) {
                existing.textRecords + nextRecord
            } else {
                existing.textRecords.map {
                    if (it.id == currentRecord.id) nextRecord else it
                }
            },
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
        if (existing.textRecords.none { it.id == recordId }) {
            return CaseMutationResult.NotFound
        }
        return persistCase(
            caseRepository = caseRepository,
            case = existing.copy(
                textRecords = existing.textRecords.filterNot { it.id == recordId },
                updatedAt = clock.instant(),
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
)

private data class ValidEventDraft(
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
        val nextEvent = if (currentEvent == null) {
            CaseEvent(
                id = idGenerator.nextId(),
                year = valid.year,
                month = valid.month,
                day = valid.day,
                datePrecision = valid.precision,
                status = valid.status,
                rawText = valid.rawText,
                createdAt = clock.instant(),
            )
        } else {
            currentEvent.copy(
                year = valid.year,
                month = valid.month,
                day = valid.day,
                datePrecision = valid.precision,
                status = valid.status,
                rawText = valid.rawText,
            )
        }
        val updated = existing.copy(
            events = if (currentEvent == null) {
                existing.events + nextEvent
            } else {
                existing.events.map {
                    if (it.id == currentEvent.id) nextEvent else it
                }
            },
            updatedAt = clock.instant(),
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
        if (existing.events.none { it.id == eventId }) {
            return CaseMutationResult.NotFound
        }
        return persistCase(
            caseRepository = caseRepository,
            case = existing.copy(
                events = existing.events.filterNot { it.id == eventId },
                updatedAt = clock.instant(),
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
                year = year,
                month = month,
                day = day,
                precision = precision,
                status = draft.status.trim().takeIf { it.isNotEmpty() },
                rawText = rawText,
            ),
        )
    }
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

private fun caseReadFailure(): CaseMutationResult.StorageFailed =
    CaseMutationResult.StorageFailed(
        "无法读取最新命例，未保存任何更改。请返回详情后重试。",
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
