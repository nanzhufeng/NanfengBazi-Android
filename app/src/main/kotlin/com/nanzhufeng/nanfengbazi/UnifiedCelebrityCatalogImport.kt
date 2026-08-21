package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseCatalogWriteRequest
import com.nanzhufeng.nanfengbazi.domain.CaseCatalogWriteResult
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthTimeCandidate
import com.nanzhufeng.nanfengbazi.domain.model.CalculationWarning
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * The install-time artifact is a single canonical case list.  Historical and
 * researched records only survive in [sourceEvidence] as provenance; they are
 * never installed as parallel libraries.
 */
@Serializable
data class UnifiedCelebrityCatalogPackage(
    val format: String,
    val version: Int,
    val catalogVersion: String,
    val compiledAt: String,
    val groups: List<UnifiedCelebrityGroup>,
    val cases: List<UnifiedCelebrityCatalogCase>,
) {
    init {
        require(format == FORMAT) { "不是南枫八字支持的名人案例统一资料库。" }
        require(version == VERSION) { "名人案例统一资料库版本不受支持：$version。" }
        require(cases.map { it.caseId }.distinct().size == cases.size) { "统一资料库存在重复案例标识。" }
        require(cases.all { it.sourceEvidence.isNotEmpty() }) { "统一资料库缺少资料来源证据。" }
    }

    companion object {
        const val FORMAT = "nanfeng-bazi-unified-celebrity-catalog"
        const val VERSION = 1
    }
}

@Serializable
data class UnifiedCelebrityGroup(val id: String, val name: String)

@Serializable
data class UnifiedCelebrityEvidence(
    val sourceType: CaseSourceType,
    val sourceId: String,
    val originalBirthInput: JsonObject? = null,
)

@Serializable
data class UnifiedCelebrityCatalogCase(
    val caseId: String,
    val canonicalName: String,
    val sex: SexForFortuneDirection,
    val birthInput: BirthInput,
    val birthTimeCandidates: List<BirthTimeCandidate>,
    val groupId: String,
    val tags: List<String>,
    val sourceEvidence: List<UnifiedCelebrityEvidence>,
    val curatedCase: CuratedCelebrityCase? = null,
    val supersededCaseIds: List<String> = emptyList(),
    val restoreCaseIds: List<String> = emptyList(),
)

data class UnifiedCelebrityCatalogImportResult(
    val created: Int,
    val updated: Int,
    val skipped: Int,
    val archivedDuplicates: Int,
    val invalid: Int,
    val errors: List<String>,
    val restoredFromTrash: Int = 0,
)

class UnifiedCelebrityCatalogImporter(
    private val caseRepository: CaseRepository,
    private val baziEngine: BaziEngine,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
        classDiscriminator = "_type"
    }
    private val curatedImporter = CuratedCelebrityImporter(caseRepository, baziEngine, clock)

    fun decode(raw: String): UnifiedCelebrityCatalogPackage =
        json.decodeFromString(UnifiedCelebrityCatalogPackage.serializer(), raw)

    suspend fun synchronize(data: UnifiedCelebrityCatalogPackage): UnifiedCelebrityCatalogImportResult {
        val groups = try {
            ensureGroups(data.groups)
        } catch (error: Exception) {
            return UnifiedCelebrityCatalogImportResult(0, 0, 0, 0, 1, listOf(
                "统一分组准备失败：" + (error.message ?: "无法写入分组目录"),
            ))
        }
        val plans = mutableListOf<UnifiedCelebrityCatalogWritePlan>()
        val preflightErrors = mutableListOf<String>()
        var skipped = 0
        val catalogAndDuplicateIds = buildSet {
            data.cases.forEach { source ->
                add(source.caseId)
                addAll(source.supersededCaseIds)
                addAll(source.restoreCaseIds)
            }
        }
        val existingById = try {
            caseRepository.findByIds(catalogAndDuplicateIds)
        } catch (error: Exception) {
            return UnifiedCelebrityCatalogImportResult(
                created = 0,
                updated = 0,
                skipped = 0,
                archivedDuplicates = 0,
                invalid = 1,
                errors = listOf("统一资料预读失败：" + (error.message ?: "同步可在下次启动重试")),
            )
        }
        data.cases.forEach { source ->
            try {
                val group = requireNotNull(groups[source.groupId]) { "统一分组不存在：" + source.groupId }
                val candidate = source.buildCase(group)
                val existing = existingById[source.caseId]
                when {
                    existing == null -> plans += UnifiedCelebrityCatalogWritePlan(source, candidate, null)
                    existing.libraryType != CaseLibraryType.CELEBRITY -> {
                        preflightErrors += source.canonicalName + "：案例标识与用户案例冲突，已保留用户案例。"
                    }
                    else -> {
                        val preserved = candidate.preservePresentationAndUserContributions(existing)
                        if (existing.hasSameCatalogContentAs(preserved)) {
                            skipped++
                        } else {
                            plans += UnifiedCelebrityCatalogWritePlan(source, preserved, existing.revision)
                        }
                    }
                }
            } catch (error: Exception) {
                preflightErrors += source.canonicalName + "：" + (error.message ?: "资料预检失败")
            }
        }
        // 先完整预检再写入；任一资料无法构建时，旧资料库保持完整并可在下次启动重试。
        if (preflightErrors.isNotEmpty()) {
            return UnifiedCelebrityCatalogImportResult(0, 0, 0, 0, preflightErrors.size, preflightErrors.take(20))
        }
        val batch = try {
            caseRepository.saveCatalogAtomically(
                plans.map { CaseCatalogWriteRequest(it.case, it.expectedRevision) },
            )
        } catch (error: Exception) {
            return UnifiedCelebrityCatalogImportResult(
                0,
                0,
                0,
                0,
                1,
                listOf("资料包写入失败：" + (error.message ?: "同步可在下次启动重试")),
            )
        }
        val created: Int
        val updated: Int
        when (batch) {
            is CaseCatalogWriteResult.Applied -> {
                created = batch.created
                updated = batch.updated
            }
            is CaseCatalogWriteResult.NotApplied -> {
                val conflictId = when (val cause = batch.cause) {
                    is CaseWriteResult.Created -> cause.caseId
                    is CaseWriteResult.Updated -> cause.caseId
                    is CaseWriteResult.AlreadyExists -> cause.caseId
                    is CaseWriteResult.RevisionConflict -> cause.caseId
                }
                val conflictedName = plans.firstOrNull { it.case.id == conflictId }
                    ?.source
                    ?.canonicalName
                    ?: "资料包"
                return UnifiedCelebrityCatalogImportResult(
                    0,
                    0,
                    0,
                    0,
                    1,
                    listOf(conflictedName + "：同步期间发生修订冲突，旧资料保持不变。"),
                )
            }
        }
        return try {
            val restoreRequests = data.cases.flatMap { source ->
                val targetGroup = requireNotNull(groups[source.groupId]) {
                    "统一分组不存在：${source.groupId}"
                }
                source.restoreCaseIds.mapNotNull { caseId ->
                    existingById[caseId]
                        ?.takeIf {
                            it.libraryType == CaseLibraryType.CELEBRITY && it.deletedAt != null
                        }
                        ?.let { legacy ->
                            CaseCatalogWriteRequest(
                                case = legacy.copy(
                                    groups = listOf(targetGroup),
                                    deletedAt = null,
                                    updatedAt = maxOf(clock.instant(), legacy.createdAt),
                                ),
                                expectedRevision = legacy.revision,
                            )
                        }
                }
            }
            val restored = when {
                restoreRequests.isEmpty() -> 0
                else -> when (val result = caseRepository.saveCatalogAtomically(restoreRequests)) {
                    is CaseCatalogWriteResult.Applied -> result.updated
                    is CaseCatalogWriteResult.NotApplied -> return UnifiedCelebrityCatalogImportResult(
                        created,
                        updated,
                        skipped,
                        0,
                        1,
                        listOf("资料包恢复发生修订冲突。"),
                    )
                }
            }
            UnifiedCelebrityCatalogImportResult(
                created,
                updated,
                skipped,
                archivedDuplicates = 0,
                invalid = 0,
                errors = emptyList(),
                restoredFromTrash = restored,
            )
        } catch (error: Exception) {
            UnifiedCelebrityCatalogImportResult(
                created,
                updated,
                skipped,
                0,
                1,
                listOf("资料包恢复失败：" + (error.message ?: "同步可在下次启动重试")),
            )
        }
    }

    private suspend fun ensureGroups(source: List<UnifiedCelebrityGroup>): Map<String, CaseGroup> {
        val existingByName = caseRepository.listGroups(CaseLibraryType.CELEBRITY).associateBy { it.name }.toMutableMap()
        return source.associate { item ->
            val group = existingByName[item.name] ?: requireNotNull(
                caseRepository.createGroup(item.name, CaseLibraryType.CELEBRITY),
            ) { "无法创建统一分组：${item.name}" }.also { existingByName[item.name] = it }
            item.id to group
        }
    }

    private suspend fun UnifiedCelebrityCatalogCase.buildCase(group: CaseGroup): BaziCase {
        val fromCurated = curatedCase?.let { curatedImporter.buildCase(it, group, caseId) }
        val raw = fromCurated ?: buildFromCanonicalInput(group)
        return raw.copy(
            id = caseId,
            alias = canonicalName,
            sourceType = CaseSourceType.CURATED_CELEBRITY_CATALOG,
            libraryType = CaseLibraryType.CELEBRITY,
            groups = listOf(group),
            tags = tags.map(String::trim).filter(String::isNotEmpty).distinct().map { tag ->
                CaseTag(unifiedStableId("tag", tag), tag)
            },
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            revision = 0,
        )
    }

    private suspend fun UnifiedCelebrityCatalogCase.buildFromCanonicalInput(group: CaseGroup): BaziCase {
        val now = clock.instant()
        val originals = birthTimeCandidates.ifEmpty {
            listOf(
                BirthTimeCandidate(
                    id = unifiedStableId("time", caseId),
                    label = "统一资料默认采用",
                    birthInput = birthInput,
                    calculationSnapshotId = unifiedStableId("snapshot", caseId),
                    adopted = true,
                    createdAt = now,
                ),
            )
        }
        val adoptedIndex = originals.indexOfFirst { it.adopted }.takeIf { it >= 0 } ?: 0
        val built = originals.mapIndexed { index, original ->
            val candidateId = original.id.ifBlank { unifiedStableId("time", "$caseId:$index") }
            val snapshotId = unifiedStableId("snapshot", "$caseId:$candidateId")
            val calculationInput = original.birthInput.toSupportedHistoricalCalculationInput()
            val calculation = baziEngine.calculate(calculationInput, CalculationProfile.tymeDefault())
            val historicalBceInput = original.birthInput.hasBceCalendarYear()
            val normalized = if (historicalBceInput) original.birthInput else calculation.normalizedInput
            val persistedCalculation = if (historicalBceInput) {
                calculation.copy(
                    normalizedInput = original.birthInput,
                    calendarConversion = null,
                    basicChartDetails = null,
                    warnings = calculation.warnings + CalculationWarning(
                        code = "HISTORICAL_BCE_CALCULATION_UNAVAILABLE",
                        message = "本条公元前年份已按史料保存；当前排盘引擎不支持公元前历法，四柱和流运仅为不可采用的技术占位。",
                    ),
                )
            } else {
                calculation
            }
            BirthTimeCandidate(
                id = candidateId,
                label = original.label.ifBlank { "统一资料候选" },
                birthInput = normalized,
                calculationSnapshotId = snapshotId,
                adopted = index == adoptedIndex,
                createdAt = now,
            ) to CaseCalculationSnapshot(
                id = snapshotId,
                result = persistedCalculation,
                adopted = index == adoptedIndex,
                birthTimeCandidateId = candidateId,
                createdAt = now,
            )
        }
        return BaziCase(
            id = caseId,
            alias = canonicalName,
            name = com.nanzhufeng.nanfengbazi.domain.model.ExplicitText.present(canonicalName),
            sexForFortuneDirection = sex,
            sourceType = CaseSourceType.CURATED_CELEBRITY_CATALOG,
            birthInput = built[adoptedIndex].first.birthInput,
            libraryType = CaseLibraryType.CELEBRITY,
            birthTimeCandidates = built.map { it.first },
            calculationSnapshots = built.map { it.second },
            groups = listOf(group),
            createdAt = now,
            updatedAt = now,
        )
    }
}

/** Tyme4j cannot construct BCE solar dates. The source date is still persisted verbatim. */
private fun BirthInput.toSupportedHistoricalCalculationInput(): BirthInput = when (val calendar = calendarInput) {
    is BirthCalendarInput.Solar -> if (calendar.dateTime.year <= 0) {
        copy(
            calendarInput = BirthCalendarInput.Solar(
                calendar.dateTime.copy(year = calendar.dateTime.year + 6_000),
            ),
        )
    } else {
        this
    }
    is BirthCalendarInput.Lunar -> this
}

private fun BirthInput.hasBceCalendarYear(): Boolean = when (val calendar = calendarInput) {
    is BirthCalendarInput.Solar -> calendar.dateTime.year <= 0
    is BirthCalendarInput.Lunar -> calendar.dateTime.year <= 0
}

private data class UnifiedCelebrityCatalogWritePlan(
    val source: UnifiedCelebrityCatalogCase,
    val case: BaziCase,
    val expectedRevision: Long?,
)

private fun BaziCase.preservePresentationAndUserContributions(existing: BaziCase): BaziCase {
    val userRecords = existing.textRecords.filter { it.sourceType != TextRecordSourceType.CURATED_RESEARCH }
    val retainedRecordIds = userRecords.mapTo(mutableSetOf()) { it.id }
    return copy(
        isFavorite = existing.isFavorite,
        isPinned = existing.isPinned,
        lastViewedAt = existing.lastViewedAt,
        deletedAt = existing.deletedAt,
        textRecords = textRecords + userRecords.filterNot { record ->
            textRecords.any { managed -> managed.id == record.id }
        },
        textRecordRevisions = textRecordRevisions + existing.textRecordRevisions.filter {
            it.recordId in retainedRecordIds
        },
        events = events + existing.events.filterNot { event ->
            events.any { managed -> managed.id == event.id }
        },
        eventRevisions = eventRevisions + existing.eventRevisions.filter { revision ->
            events.none { managed -> managed.id == revision.eventId }
        },
        attachments = attachments + existing.attachments.filterNot { attachment ->
            attachments.any { managed -> managed.id == attachment.id }
        },
        fieldEvidence = fieldEvidence + existing.fieldEvidence.filter { evidence ->
            fieldEvidence.none { managed -> managed.id == evidence.id }
        },
    )
}

/**
 * Catalog builds use the current clock for child metadata, while database writes preserve
 * the already stored child timestamps. They are not catalog-content changes and must not
 * cause every launch to rewrite the full library.
 */
private fun BaziCase.hasSameCatalogContentAs(other: BaziCase): Boolean =
    catalogComparable() == other.catalogComparable()

private fun BaziCase.catalogComparable(): BaziCase = copy(
    birthTimeCandidates = birthTimeCandidates
        .map { it.copy(createdAt = Instant.EPOCH) }
        .sortedBy { it.id },
    calculationSnapshots = calculationSnapshots
        .map { it.copy(createdAt = Instant.EPOCH) }
        .sortedBy { it.id },
    textRecords = textRecords
        .map { it.copy(createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH) }
        .sortedBy { it.id },
    events = events.map { it.copy(createdAt = Instant.EPOCH) }.sortedBy { it.id },
    attachments = attachments.map { it.copy(createdAt = Instant.EPOCH) }.sortedBy { it.id },
    fieldEvidence = fieldEvidence.map { it.copy(createdAt = Instant.EPOCH) }.sortedBy { it.id },
    groups = groups.sortedBy { it.id },
    tags = tags.sortedBy { it.id },
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
    revision = 0,
)

private fun unifiedStableId(namespace: String, value: String): String = UUID.nameUUIDFromBytes(
    "$namespace:$value".toByteArray(StandardCharsets.UTF_8),
).toString()
