package com.nanzhufeng.nanfengbazi.data.exchange

import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class SingleCaseExchangeService(
    private val repository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun export(
        caseId: String,
        output: OutputStream,
        appVersion: String,
        protection: SingleCaseProtection,
    ): SingleCaseExportResult {
        if (protection is SingleCaseProtection.PasswordProtected) {
            return rejectedExport(
                "PASSWORD_ENCRYPTION_NOT_IMPLEMENTED",
                "当前阶段尚未实现单命例密码加密，不能静默降级为未加密文件。",
            )
        }
        if (caseId.isBlank()) {
            return rejectedExport("CASE_ID_REQUIRED", "导出单命例必须提供稳定 ID。")
        }
        if (appVersion.isBlank()) {
            return rejectedExport("APP_VERSION_REQUIRED", "单命例文件必须记录 App 版本。")
        }
        val case = try {
            repository.findById(caseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedExport("CASE_READ_FAILED", "读取待导出命例失败。")
        } ?: return rejectedExport("CASE_NOT_FOUND", "没有找到要导出的命例。")
        return try {
            val payloadBytes = canonicalPayload(case)
            val document = SingleCaseDocument(
                formatVersion = FORMAT_VERSION,
                appVersion = appVersion,
                databaseSchemaVersion = NanfengBaziDatabase.SCHEMA_VERSION,
                exportedAt = clock.instant().toString(),
                payloadSha256 = sha256(payloadBytes),
                caseData = case,
            )
            val bytes = DomainJson.encodeToString(document).encodeToByteArray()
            if (bytes.size > MAX_DOCUMENT_BYTES) {
                return rejectedExport(
                    "DOCUMENT_TOO_LARGE",
                    "单命例 JSON 超过 ${MAX_DOCUMENT_BYTES / 1024 / 1024} MiB 上限。",
                )
            }
            output.write(bytes)
            SingleCaseExportResult.Success(
                suggestedFileName = suggestedFileName(case),
                byteSize = bytes.size.toLong(),
                sha256 = sha256(bytes),
            )
        } catch (_: Exception) {
            rejectedExport("EXPORT_IO_ERROR", "单命例导出失败，请保留诊断代码并重试。")
        }
    }

    fun suggestedFileName(case: BaziCase): String = buildSuggestedFileName(case)

    suspend fun preview(input: InputStream): SingleCasePreviewResult {
        val bytes = try {
            input.readBounded(MAX_DOCUMENT_BYTES)
        } catch (_: DocumentTooLargeException) {
            return rejectedPreview(
                "DOCUMENT_TOO_LARGE",
                "单命例 JSON 超过 ${MAX_DOCUMENT_BYTES / 1024 / 1024} MiB 上限。",
            )
        } catch (_: Exception) {
            return rejectedPreview("READ_FAILED", "无法读取单命例 JSON。")
        }
        val document = try {
            DomainJson.decodeFromString<SingleCaseDocument>(bytes.decodeToString())
        } catch (_: SerializationException) {
            return rejectedPreview("INVALID_JSON", "文件不是受支持的南枫八字单命例 JSON。")
        } catch (_: IllegalArgumentException) {
            return rejectedPreview("INVALID_CASE_DATA", "命例字段或引用关系不合法。")
        }

        validateDocument(document)?.let { return it }

        val conflicts = try {
            loadConflicts(document.caseData)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedPreview("CONFLICT_LOOKUP_FAILED", "读取本地冲突候选失败，未写入数据。")
        }

        return SingleCasePreviewResult.Success(
            SingleCasePreview(
                document = document,
                counts = document.caseData.counts(),
                conflicts = conflicts,
            ),
        )
    }

    suspend fun commitImport(
        preview: SingleCasePreview,
        decision: SingleCaseImportDecision,
    ): SingleCaseImportResult {
        if (decision == SingleCaseImportDecision.SKIP) {
            return SingleCaseImportResult.Skipped(preview.document.caseData.id)
        }
        validateDocument(preview.document)?.let {
            return rejectedImport(it.code, it.message)
        }
        val currentConflicts = try {
            loadConflicts(preview.document.caseData)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedImport(
                "CONFLICT_LOOKUP_FAILED",
                "提交前无法重新核对本地冲突，未写入数据。",
            )
        }
        if (currentConflicts != preview.conflicts) {
            return rejectedImport(
                "PREVIEW_STALE",
                "本地命例已在预览后发生变化，请重新选择文件并预览。",
            )
        }
        if (preview.document.caseData.attachments.isNotEmpty()) {
            return rejectedImport(
                "ATTACHMENT_BINARIES_REQUIRED",
                "该命例含图片或字段证据，但 JSON 只有引用信息。为避免证据缺失，当前不允许提交导入。",
            )
        }
        val importedCase = try {
            cloneForKeepBoth(preview.document.caseData)
        } catch (_: IllegalArgumentException) {
            return rejectedImport("ID_GENERATION_FAILED", "无法生成安全的新命例身份，未写入数据。")
        }
        val writeResult = try {
            repository.save(importedCase, expectedRevision = 0)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedImport("IMPORT_WRITE_FAILED", "导入写入失败，事务已回滚。")
        }
        return when (writeResult) {
            is CaseWriteResult.Created -> SingleCaseImportResult.Imported(
                caseId = writeResult.caseId,
                revision = writeResult.revision,
            )
            is CaseWriteResult.AlreadyExists,
            is CaseWriteResult.RevisionConflict,
            is CaseWriteResult.Updated,
            -> rejectedImport("GENERATED_ID_CONFLICT", "新命例身份发生冲突，未覆盖任何本地命例。")
        }
    }

    suspend fun prepareMerge(
        preview: SingleCasePreview,
        targetCaseId: String,
    ): SingleCaseMergePreparationResult {
        validateDocument(preview.document)?.let {
            return rejectedMergePreparation(it.code, it.message)
        }
        if (targetCaseId.isBlank()) {
            return rejectedMergePreparation("TARGET_REQUIRED", "合并必须明确选择一个本地目标命例。")
        }
        if (preview.conflicts.none { it.caseId == targetCaseId }) {
            return rejectedMergePreparation(
                "TARGET_NOT_IN_PREVIEW",
                "所选目标不在当前冲突候选中，请重新预览。",
            )
        }
        if (preview.conflicts.single { it.caseId == targetCaseId }.isTrashed) {
            return rejectedMergePreparation(
                "TARGET_TRASHED",
                "所选目标位于回收站，请先恢复后再合并。",
            )
        }
        if (preview.document.caseData.attachments.isNotEmpty()) {
            return rejectedMergePreparation(
                "ATTACHMENT_BINARIES_REQUIRED",
                "来源命例含图片或字段证据，但 JSON 没有图片二进制，当前不能合并。",
            )
        }
        val currentConflicts = try {
            loadConflicts(preview.document.caseData)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedMergePreparation(
                "CONFLICT_LOOKUP_FAILED",
                "无法重新核对本地冲突，未写入数据。",
            )
        }
        if (currentConflicts != preview.conflicts) {
            return rejectedMergePreparation(
                "PREVIEW_STALE",
                "本地冲突已发生变化，请重新选择文件并预览。",
            )
        }
        val target = try {
            repository.findById(targetCaseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return rejectedMergePreparation("TARGET_NOT_FOUND", "没有找到所选本地目标命例。")
        val additions = calculateMergeAdditions(preview.document.caseData, target)
        return SingleCaseMergePreparationResult.Success(
            SingleCaseMergePreparation(
                sourcePreview = preview,
                targetCaseId = target.id,
                targetAlias = target.alias,
                targetRevision = target.revision,
                targetPayloadSha256 = sha256(canonicalPayload(target)),
                fieldDifferences = fieldDifferences(target, preview.document.caseData),
                addableCounts = additions.counts(),
            ),
        )
    }

    suspend fun commitMerge(
        plan: SingleCaseMergePlan,
    ): SingleCaseImportResult {
        val preparation = plan.preparation
        val source = preparation.sourcePreview.document.caseData
        validateDocument(preparation.sourcePreview.document)?.let {
            return rejectedImport(it.code, it.message)
        }
        if (source.attachments.isNotEmpty()) {
            return rejectedImport(
                "ATTACHMENT_BINARIES_REQUIRED",
                "来源命例含图片或字段证据，但 JSON 没有图片二进制，当前不能合并。",
            )
        }
        val currentConflicts = try {
            loadConflicts(source)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedImport(
                "CONFLICT_LOOKUP_FAILED",
                "提交前无法重新核对本地冲突，未写入数据。",
            )
        }
        if (currentConflicts != preparation.sourcePreview.conflicts) {
            return rejectedImport(
                "PREVIEW_STALE",
                "本地冲突已在选择方案后发生变化，请重新预览。",
            )
        }
        if (currentConflicts.none { it.caseId == preparation.targetCaseId }) {
            return rejectedImport(
                "TARGET_NOT_IN_PREVIEW",
                "所选目标不再是当前冲突候选，请重新预览。",
            )
        }
        val target = try {
            repository.findById(preparation.targetCaseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return rejectedImport("TARGET_NOT_FOUND", "所选本地目标命例已不存在，未写入数据。")
        if (
            target.revision != preparation.targetRevision ||
            sha256(canonicalPayload(target)) != preparation.targetPayloadSha256
        ) {
            return rejectedImport(
                "TARGET_CHANGED",
                "本地目标命例已在选择方案后变化，请重新生成差异。",
            )
        }
        val differences = fieldDifferences(target, source).mapTo(mutableSetOf()) { it.key }
        if (plan.fieldChoices.keys.any { it !in differences }) {
            return rejectedImport(
                "INVALID_FIELD_CHOICE",
                "字段采用方案包含当前不存在的差异，请重新生成差异。",
            )
        }
        val additions = calculateMergeAdditions(source, target)
        val importedFieldKeys = plan.fieldChoices
            .filterValues { it == SingleCaseValueChoice.IMPORTED }
            .keys
        val hasModuleChanges = (
            SingleCaseMergeModule.CALCULATION_SNAPSHOTS in plan.modules &&
                additions.calculationSnapshots.isNotEmpty() ||
                SingleCaseMergeModule.TEXT_RECORDS in plan.modules &&
                (additions.textRecords.isNotEmpty() ||
                    additions.textRecordRevisions.isNotEmpty()) ||
                SingleCaseMergeModule.EVENTS in plan.modules &&
                (additions.events.isNotEmpty() || additions.eventRevisions.isNotEmpty()) ||
                SingleCaseMergeModule.ORGANIZATION in plan.modules &&
                (additions.groups.isNotEmpty() || additions.tags.isNotEmpty())
            )
        if (importedFieldKeys.isEmpty() && !hasModuleChanges) {
            return rejectedImport("NO_CHANGES_SELECTED", "当前方案没有选择任何可写入差异。")
        }
        val merged = try {
            buildMergedCase(
                target = target,
                source = source,
                plan = plan,
                additions = additions,
            )
        } catch (_: Exception) {
            return rejectedImport("MERGE_BUILD_FAILED", "无法构建安全合并结果，未写入数据。")
        }
        val writeResult = try {
            repository.save(merged, expectedRevision = preparation.targetRevision)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedImport("IMPORT_WRITE_FAILED", "合并写入失败，事务已回滚。")
        }
        return when (writeResult) {
            is CaseWriteResult.Updated -> SingleCaseImportResult.Merged(
                caseId = writeResult.caseId,
                revision = writeResult.revision,
                addedCounts = additions.selectedCounts(plan.modules),
            )
            is CaseWriteResult.RevisionConflict -> rejectedImport(
                "TARGET_CHANGED",
                "本地目标命例已有较新修订，请重新生成差异。",
            )
            is CaseWriteResult.AlreadyExists,
            is CaseWriteResult.Created,
            -> rejectedImport("MERGE_WRITE_CONTRACT_FAILED", "合并没有按更新合同完成，未确认成功。")
        }
    }

    private fun validateDocument(
        document: SingleCaseDocument,
    ): SingleCasePreviewResult.Rejected? {
        if (document.formatVersion != FORMAT_VERSION) {
            return rejectedPreview(
                "UNSUPPORTED_FORMAT_VERSION",
                "单命例格式版本 ${document.formatVersion} 不受支持。",
            )
        }
        if (
            document.databaseSchemaVersion !in
            MIN_SUPPORTED_DATABASE_SCHEMA..NanfengBaziDatabase.SCHEMA_VERSION
        ) {
            return rejectedPreview(
                "UNSUPPORTED_DATABASE_SCHEMA",
                "单命例数据库版本 ${document.databaseSchemaVersion} 不受支持。",
            )
        }
        if (document.appVersion.isBlank()) {
            return rejectedPreview("INVALID_METADATA", "单命例文件缺少 App 版本。")
        }
        try {
            Instant.parse(document.exportedAt)
        } catch (_: Exception) {
            return rejectedPreview("INVALID_METADATA", "单命例导出时间格式无效。")
        }
        if (!document.payloadSha256.matches(SHA256_PATTERN)) {
            return rejectedPreview("INVALID_METADATA", "单命例载荷哈希格式无效。")
        }
        if (sha256(canonicalPayload(document.caseData)) != document.payloadSha256) {
            return rejectedPreview("PAYLOAD_HASH_MISMATCH", "单命例内容与载荷哈希不一致。")
        }
        return null
    }

    private fun rejectedExport(code: String, message: String) =
        SingleCaseExportResult.Rejected(code, message)

    private fun rejectedPreview(code: String, message: String) =
        SingleCasePreviewResult.Rejected(code, message)

    private fun rejectedImport(code: String, message: String) =
        SingleCaseImportResult.Rejected(code, message)

    private fun rejectedMergePreparation(code: String, message: String) =
        SingleCaseMergePreparationResult.Rejected(code, message)

    private suspend fun loadConflicts(case: BaziCase): List<SingleCaseConflictCandidate> {
        val adoptedFourPillars = case.calculationSnapshots
            .asReversed()
            .firstOrNull { it.adopted }
            ?.result
            ?.fourPillars
        val conflicts = linkedMapOf<String, MutableConflict>()
        repository.findById(case.id)?.let { existing ->
            conflicts.getOrPut(existing.id) {
                MutableConflict(existing.id, existing.alias, existing.deletedAt != null)
            }.reasons += SingleCaseConflictReason.STABLE_ID_EXISTS
        }
        repository.findDuplicateCandidates(
            birthInput = case.birthInput,
            fourPillars = adoptedFourPillars,
        ).forEach { candidate ->
            val conflict = conflicts.getOrPut(candidate.summary.id) {
                MutableConflict(
                    caseId = candidate.summary.id,
                    alias = candidate.summary.alias,
                    isTrashed = candidate.summary.deletedAt != null,
                )
            }
            candidate.reasons.forEach { reason ->
                conflict.reasons += when (reason) {
                    DuplicateReason.SAME_BIRTH_INPUT ->
                        SingleCaseConflictReason.SAME_BIRTH_INPUT
                    DuplicateReason.SAME_FOUR_PILLARS ->
                        SingleCaseConflictReason.SAME_FOUR_PILLARS
                }
            }
        }
        return conflicts.values
            .map { it.toCandidate() }
            .sortedWith(compareBy({ it.isTrashed }, { it.alias }, { it.caseId }))
    }

    private fun calculateMergeAdditions(
        source: BaziCase,
        target: BaziCase,
    ): MergeAdditions {
        val targetRecordKeys = target.textRecords.mapTo(mutableSetOf()) { it.mergeKey() }
        val textRecords = source.textRecords.filter { it.mergeKey() !in targetRecordKeys }
        val sourceLiveRecordIds = source.textRecords.mapTo(mutableSetOf()) { it.id }
        val includedRecordIds = buildSet {
            addAll(textRecords.map { it.id })
            addAll(
                source.textRecordRevisions
                    .filter { it.recordId !in sourceLiveRecordIds }
                    .map { it.recordId },
            )
        }
        val textRecordRevisions = source.textRecordRevisions
            .filter { it.recordId in includedRecordIds }

        val targetEventKeys = target.events.mapTo(mutableSetOf()) { it.mergeKey() }
        val events = source.events.filter { it.mergeKey() !in targetEventKeys }
        val sourceLiveEventIds = source.events.mapTo(mutableSetOf()) { it.id }
        val includedEventIds = buildSet {
            addAll(events.map { it.id })
            addAll(
                source.eventRevisions
                    .filter { it.eventId !in sourceLiveEventIds }
                    .map { it.eventId },
            )
        }
        val eventRevisions = source.eventRevisions.filter { it.eventId in includedEventIds }

        val targetCalculationResults = target.calculationSnapshots.mapTo(mutableSetOf()) {
            it.result
        }
        val targetGroupNames = target.groups.mapTo(mutableSetOf()) { it.name.trim() }
        val targetTagNames = target.tags.mapTo(mutableSetOf()) { it.name.trim() }
        return MergeAdditions(
            calculationSnapshots = source.calculationSnapshots
                .filter { it.result !in targetCalculationResults },
            textRecords = textRecords,
            textRecordRevisions = textRecordRevisions,
            events = events,
            eventRevisions = eventRevisions,
            groups = source.groups.filter { it.name.trim() !in targetGroupNames },
            tags = source.tags.filter { it.name.trim() !in targetTagNames },
        )
    }

    private fun fieldDifferences(
        local: BaziCase,
        imported: BaziCase,
    ): List<SingleCaseFieldDifference> = buildList {
        addDifference(SingleCaseFieldKey.ALIAS, "别名", local.alias, imported.alias)
        addDifference(
            SingleCaseFieldKey.NAME,
            "姓名",
            local.name,
            imported.name,
            ExplicitText::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.SOURCE_TYPE,
            "来源类型",
            local.sourceType,
            imported.sourceType,
            { it.name },
        )
        addDifference(
            SingleCaseFieldKey.BIRTH_INPUT,
            "出生输入与性别口径",
            BirthIdentityMergeValue(
                birthInput = local.birthInput.toString(),
                sex = local.sexForFortuneDirection.name,
            ),
            BirthIdentityMergeValue(
                birthInput = imported.birthInput.toString(),
                sex = imported.sexForFortuneDirection.name,
            ),
            { "${it.birthInput} / ${it.sex}" },
        )
        addDifference(
            SingleCaseFieldKey.PROFILE_OCCUPATION,
            "职业",
            local.profile.occupation,
            imported.profile.occupation,
            ExplicitText::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.PROFILE_EDUCATION,
            "学历",
            local.profile.education,
            imported.profile.education,
            ExplicitText::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.PROFILE_FINANCE,
            "财务",
            local.profile.finance,
            imported.profile.finance,
            ExplicitText::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.PROFILE_MARRIAGE,
            "婚姻",
            local.profile.marriage,
            imported.profile.marriage,
            ExplicitText::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.PROFILE_HEALTH,
            "健康",
            local.profile.health,
            imported.profile.health,
            ExplicitText::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.FAVORITE,
            "收藏",
            local.isFavorite,
            imported.isFavorite,
            Boolean::displayValue,
        )
        addDifference(
            SingleCaseFieldKey.PINNED,
            "置顶",
            local.isPinned,
            imported.isPinned,
            Boolean::displayValue,
        )
    }

    private fun buildMergedCase(
        target: BaziCase,
        source: BaziCase,
        plan: SingleCaseMergePlan,
        additions: MergeAdditions,
    ): BaziCase {
        val usedIds = mutableSetOf<String>()
        fun nextId() = nextGeneratedId(usedIds)
        fun imported(key: SingleCaseFieldKey) =
            plan.fieldChoices[key] == SingleCaseValueChoice.IMPORTED

        val appendRecords = SingleCaseMergeModule.TEXT_RECORDS in plan.modules
        val appendEvents = SingleCaseMergeModule.EVENTS in plan.modules
        val appendCalculations =
            SingleCaseMergeModule.CALCULATION_SNAPSHOTS in plan.modules
        val appendOrganization = SingleCaseMergeModule.ORGANIZATION in plan.modules
        val recordIds = if (appendRecords) {
            (
                additions.textRecords.map { it.id } +
                    additions.textRecordRevisions.map { it.recordId }
                ).distinct().associateWith { nextId() }
        } else {
            emptyMap()
        }
        val eventIds = if (appendEvents) {
            (
                additions.events.map { it.id } +
                    additions.eventRevisions.map { it.eventId }
                ).distinct().associateWith { nextId() }
        } else {
            emptyMap()
        }
        val useImportedBirth = imported(SingleCaseFieldKey.BIRTH_INPUT)
        val birthInput = if (imported(SingleCaseFieldKey.BIRTH_INPUT)) {
            source.birthInput
        } else {
            target.birthInput
        }
        return target.copy(
            alias = if (imported(SingleCaseFieldKey.ALIAS)) source.alias else target.alias,
            name = if (imported(SingleCaseFieldKey.NAME)) source.name else target.name,
            sexForFortuneDirection = if (useImportedBirth) {
                source.sexForFortuneDirection
            } else {
                target.sexForFortuneDirection
            },
            sourceType = if (imported(SingleCaseFieldKey.SOURCE_TYPE)) {
                source.sourceType
            } else {
                target.sourceType
            },
            birthInput = birthInput,
            profile = target.profile.copy(
                occupation = if (imported(SingleCaseFieldKey.PROFILE_OCCUPATION)) {
                    source.profile.occupation
                } else {
                    target.profile.occupation
                },
                education = if (imported(SingleCaseFieldKey.PROFILE_EDUCATION)) {
                    source.profile.education
                } else {
                    target.profile.education
                },
                finance = if (imported(SingleCaseFieldKey.PROFILE_FINANCE)) {
                    source.profile.finance
                } else {
                    target.profile.finance
                },
                marriage = if (imported(SingleCaseFieldKey.PROFILE_MARRIAGE)) {
                    source.profile.marriage
                } else {
                    target.profile.marriage
                },
                health = if (imported(SingleCaseFieldKey.PROFILE_HEALTH)) {
                    source.profile.health
                } else {
                    target.profile.health
                },
            ),
            textRecords = target.textRecords + if (appendRecords) {
                additions.textRecords.map { record ->
                    record.copy(id = recordIds.getValue(record.id))
                }
            } else {
                emptyList()
            },
            textRecordRevisions = target.textRecordRevisions + if (appendRecords) {
                additions.textRecordRevisions.map { revision ->
                    val newRecordId = recordIds.getValue(revision.recordId)
                    revision.copy(
                        id = nextId(),
                        recordId = newRecordId,
                        snapshot = revision.snapshot.copy(id = newRecordId),
                    )
                }
            } else {
                emptyList()
            },
            events = target.events + if (appendEvents) {
                additions.events.map { event ->
                    event.copy(id = eventIds.getValue(event.id))
                }
            } else {
                emptyList()
            },
            eventRevisions = target.eventRevisions + if (appendEvents) {
                additions.eventRevisions.map { revision ->
                    val newEventId = eventIds.getValue(revision.eventId)
                    revision.copy(
                        id = nextId(),
                        eventId = newEventId,
                        snapshot = revision.snapshot.copy(id = newEventId),
                    )
                }
            } else {
                emptyList()
            },
            calculationSnapshots = target.calculationSnapshots + if (appendCalculations) {
                additions.calculationSnapshots.map { snapshot ->
                    snapshot.copy(id = nextId(), adopted = false)
                }
            } else {
                emptyList()
            },
            groups = target.groups + if (appendOrganization) {
                additions.groups.map { it.copy(id = nextId()) }
            } else {
                emptyList()
            },
            tags = target.tags + if (appendOrganization) {
                additions.tags.map { it.copy(id = nextId()) }
            } else {
                emptyList()
            },
            isFavorite = if (imported(SingleCaseFieldKey.FAVORITE)) {
                source.isFavorite
            } else {
                target.isFavorite
            },
            isPinned = if (imported(SingleCaseFieldKey.PINNED)) {
                source.isPinned
            } else {
                target.isPinned
            },
            updatedAt = maxOf(clock.instant(), target.createdAt),
        )
    }

    private fun nextGeneratedId(usedIds: MutableSet<String>): String {
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val candidate = idGenerator().trim()
            if (candidate.isNotEmpty() && usedIds.add(candidate)) return candidate
        }
        throw IllegalArgumentException("Unable to generate a unique non-blank id")
    }

    private fun cloneForKeepBoth(source: BaziCase): BaziCase {
        val usedIds = mutableSetOf<String>()
        fun nextId() = nextGeneratedId(usedIds)

        val recordIds = (
            source.textRecords.map { it.id } +
                source.textRecordRevisions.map { it.recordId }
            ).distinct().associateWith { nextId() }
        val eventIds = (
            source.events.map { it.id } +
                source.eventRevisions.map { it.eventId }
            ).distinct().associateWith { nextId() }
        val copiedRecords = source.textRecords.map { record ->
            record.copy(id = recordIds.getValue(record.id))
        }
        val copiedRecordRevisions = source.textRecordRevisions.map { revision ->
            val newRecordId = recordIds.getValue(revision.recordId)
            revision.copy(
                id = nextId(),
                recordId = newRecordId,
                snapshot = revision.snapshot.copy(id = newRecordId),
            )
        }
        val copiedEvents = source.events.map { event ->
            event.copy(id = eventIds.getValue(event.id))
        }
        val copiedEventRevisions = source.eventRevisions.map { revision ->
            val newEventId = eventIds.getValue(revision.eventId)
            revision.copy(
                id = nextId(),
                eventId = newEventId,
                snapshot = revision.snapshot.copy(id = newEventId),
            )
        }
        val now = clock.instant()
        return source.copy(
            id = nextId(),
            textRecords = copiedRecords,
            textRecordRevisions = copiedRecordRevisions,
            events = copiedEvents,
            eventRevisions = copiedEventRevisions,
            calculationSnapshots = source.calculationSnapshots.map {
                it.copy(id = nextId())
            },
            groups = source.groups.map { it.copy(id = nextId()) },
            tags = source.tags.map { it.copy(id = nextId()) },
            copiedFromCaseId = source.id,
            lastViewedAt = null,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
            revision = 0,
        )
    }

    private data class MutableConflict(
        val caseId: String,
        val alias: String,
        val isTrashed: Boolean,
        val reasons: MutableSet<SingleCaseConflictReason> = linkedSetOf(),
    ) {
        fun toCandidate() = SingleCaseConflictCandidate(
            caseId = caseId,
            alias = alias,
            reasons = reasons.toSet(),
            isTrashed = isTrashed,
        )
    }

    private data class MergeAdditions(
        val calculationSnapshots: List<CaseCalculationSnapshot>,
        val textRecords: List<CaseTextRecord>,
        val textRecordRevisions: List<CaseTextRecordRevision>,
        val events: List<CaseEvent>,
        val eventRevisions: List<CaseEventRevision>,
        val groups: List<CaseGroup>,
        val tags: List<CaseTag>,
    ) {
        fun counts() = SingleCaseCounts(
            calculationSnapshots = calculationSnapshots.size,
            textRecords = textRecords.size,
            textRecordRevisions = textRecordRevisions.size,
            events = events.size,
            eventRevisions = eventRevisions.size,
            attachmentReferences = 0,
            fieldEvidence = 0,
            groups = groups.size,
            tags = tags.size,
        )

        fun selectedCounts(modules: Set<SingleCaseMergeModule>) = SingleCaseCounts(
            calculationSnapshots = calculationSnapshots.size.takeIf {
                SingleCaseMergeModule.CALCULATION_SNAPSHOTS in modules
            } ?: 0,
            textRecords = textRecords.size.takeIf {
                SingleCaseMergeModule.TEXT_RECORDS in modules
            } ?: 0,
            textRecordRevisions = textRecordRevisions.size.takeIf {
                SingleCaseMergeModule.TEXT_RECORDS in modules
            } ?: 0,
            events = events.size.takeIf {
                SingleCaseMergeModule.EVENTS in modules
            } ?: 0,
            eventRevisions = eventRevisions.size.takeIf {
                SingleCaseMergeModule.EVENTS in modules
            } ?: 0,
            attachmentReferences = 0,
            fieldEvidence = 0,
            groups = groups.size.takeIf {
                SingleCaseMergeModule.ORGANIZATION in modules
            } ?: 0,
            tags = tags.size.takeIf {
                SingleCaseMergeModule.ORGANIZATION in modules
            } ?: 0,
        )
    }

    private data class BirthIdentityMergeValue(
        val birthInput: String,
        val sex: String,
    )

    companion object {
        const val FORMAT_VERSION = 1
        const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024
        private const val MAX_ID_GENERATION_ATTEMPTS = 32
        private const val MIN_SUPPORTED_DATABASE_SCHEMA = 2
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

private fun <T> MutableList<SingleCaseFieldDifference>.addDifference(
    key: SingleCaseFieldKey,
    label: String,
    local: T,
    imported: T,
    display: (T) -> String = { it.toString() },
) {
    if (local == imported) return
    add(
        SingleCaseFieldDifference(
            key = key,
            label = label,
            localValue = display(local),
            importedValue = display(imported),
        ),
    )
}

private fun ExplicitText.displayValue(): String = when (state) {
    FieldValueState.ABSENT -> "未提供"
    FieldValueState.CLEARED -> "明确清空"
    FieldValueState.PRESENT -> value.orEmpty()
}

private fun Boolean.displayValue(): String = if (this) "是" else "否"

private data class TextRecordMergeKey(
    val type: String,
    val content: String,
    val analysisCategory: String?,
)

private fun CaseTextRecord.mergeKey() = TextRecordMergeKey(
    type = type.name,
    content = content,
    analysisCategory = analysisCategory?.name,
)

private data class EventMergeKey(
    val title: String?,
    val category: String,
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val datePrecision: String,
    val stemBranch: String?,
    val status: String?,
    val rawText: String,
    val normalizedText: ExplicitText,
)

private fun CaseEvent.mergeKey() = EventMergeKey(
    title = title,
    category = category.name,
    year = year,
    month = month,
    day = day,
    datePrecision = datePrecision.name,
    stemBranch = stemBranch,
    status = status,
    rawText = rawText,
    normalizedText = normalizedText,
)

private fun BaziCase.counts() = SingleCaseCounts(
    calculationSnapshots = calculationSnapshots.size,
    textRecords = textRecords.size,
    textRecordRevisions = textRecordRevisions.size,
    events = events.size,
    eventRevisions = eventRevisions.size,
    attachmentReferences = attachments.size,
    fieldEvidence = fieldEvidence.size,
    groups = groups.size,
    tags = tags.size,
)

private fun canonicalPayload(case: BaziCase): ByteArray =
    DomainJson.encodeToString(BaziCase.serializer(), case).encodeToByteArray()

private fun buildSuggestedFileName(case: BaziCase): String {
    val safeAlias = case.alias
        .trim()
        .replace(INVALID_FILE_NAME_PATTERN, "_")
        .trim('.', ' ')
        .take(48)
        .ifBlank { case.id.take(48) }
    return "${safeAlias}_南枫八字命例.json"
}

private fun InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBytes))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw DocumentTooLargeException()
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private class DocumentTooLargeException : Exception()

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

private val INVALID_FILE_NAME_PATTERN = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
