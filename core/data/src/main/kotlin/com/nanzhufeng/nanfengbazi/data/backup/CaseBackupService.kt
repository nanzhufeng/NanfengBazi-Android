package com.nanzhufeng.nanfengbazi.data.backup

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.db.CalculationSnapshotEntity
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.db.RoomDataSnapshot
import com.nanzhufeng.nanfengbazi.data.exchange.PasswordCrypto
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExchangeService
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.data.repository.hasSameBirthIdentity
import com.nanzhufeng.nanfengbazi.data.repository.toDomainCases
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.BaziTimeZoneDefaults
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayInputStream
import java.io.SequenceInputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString

interface CaseBackupOperations {
    suspend fun export(
        output: OutputStream,
        attachmentRoot: Path,
        appVersion: String,
        protection: BackupProtection,
    ): BackupExportResult

    fun suggestedFileName(): String

    fun suggestedEncryptedFileName(): String

    suspend fun preview(
        input: InputStream,
        workRoot: Path,
        password: CharArray? = null,
    ): BackupPreviewResult

    suspend fun prepareRestorePlan(
        preview: RestorePreview,
        decisions: List<BackupCaseRestoreDecision>,
    ): BackupRestorePlanResult

    suspend fun prepareCaseMerge(
        preview: RestorePreview,
        sourceCaseId: String,
        targetCaseId: String,
    ): BackupCaseMergePreparationResult

    suspend fun executeRestorePlan(
        plan: BackupRestorePlan,
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
        password: CharArray? = null,
    ): BackupRestoreExecutionResult = BackupRestoreExecutionResult.Rejected(
        code = "RESTORE_EXECUTION_UNAVAILABLE",
        message = "完整备份提交执行器尚未就绪。",
    )

    suspend fun recoverInterruptedRestores(
        attachmentRoot: Path,
    ): BackupRestoreRecoveryResult = BackupRestoreRecoveryResult.Success(
        rolledBackTransactions = 0,
        finalizedTransactions = 0,
    )
}

class CaseBackupService(
    private val database: NanfengBaziDatabase,
    private val clock: Clock = Clock.systemUTC(),
    secureRandom: SecureRandom = SecureRandom(),
    passwordKdfIterations: Int = PasswordCrypto.DEFAULT_KDF_ITERATIONS,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val stagingDatabaseContext: Context? = null,
) : CaseBackupOperations {
    private val dao = database.caseDao()
    private val encryption = BackupEncryption(secureRandom, passwordKdfIterations)
    private val repository = RoomCaseRepository(database)
    private val mergeService = SingleCaseExchangeService(repository, clock)

    override suspend fun export(
        output: OutputStream,
        attachmentRoot: Path,
        appVersion: String,
        protection: BackupProtection,
    ): BackupExportResult {
        if (appVersion.isBlank()) {
            return BackupExportResult.Rejected(
                code = "APP_VERSION_REQUIRED",
                message = "备份必须记录 App 版本。",
            )
        }

        return try {
            val snapshot = database.withTransaction { readSnapshot() }
            val sources = buildEntrySources(snapshot, attachmentRoot)
                ?: return BackupExportResult.Rejected(
                    code = "ATTACHMENT_MISMATCH",
                    message = "附件文件缺失，或大小、SHA-256 与数据库记录不一致。",
                )
            if (protection is BackupProtection.PasswordProtected && protection.password.isEmpty()) {
                return BackupExportResult.Rejected(
                    code = "PASSWORD_REQUIRED",
                    message = "密码加密完整备份必须提供密码。",
                )
            }
            val encrypted = protection is BackupProtection.PasswordProtected
            val manifest = buildManifest(snapshot, sources, appVersion, encrypted)
            val manifestBytes = DomainJson.encodeToString(manifest).encodeToByteArray()
            val protectedOutput = when (protection) {
                BackupProtection.UnencryptedSensitiveDataConfirmed -> output
                is BackupProtection.PasswordProtected ->
                    encryption.encryptingStream(output, protection.password)
            }
            ZipOutputStream(protectedOutput.buffered()).use { zip ->
                zip.putNextEntry(stableZipEntry(MANIFEST_PATH))
                zip.write(manifestBytes)
                zip.closeEntry()
                sources.forEach { source ->
                    zip.putNextEntry(stableZipEntry(source.path))
                    source.writeTo(zip)
                    zip.closeEntry()
                }
            }

            BackupExportResult.Success(
                counts = manifest.counts,
                fileCount = sources.size + 1,
            )
        } catch (error: Exception) {
            BackupExportResult.Rejected(
                code = "EXPORT_IO_ERROR",
                message = "备份导出失败，请保留诊断代码并重试。",
            )
        }
    }

    override fun suggestedFileName(): String =
        "南枫八字备份_${FILE_NAME_TIME_FORMAT.format(clock.instant().atZone(BaziTimeZoneDefaults.beijingZoneId))}.zip"

    override fun suggestedEncryptedFileName(): String =
        "南枫八字备份_${FILE_NAME_TIME_FORMAT.format(clock.instant().atZone(BaziTimeZoneDefaults.beijingZoneId))}_加密.nfbak"

    override suspend fun preview(
        input: InputStream,
        workRoot: Path,
        password: CharArray?,
    ): BackupPreviewResult {
        return try {
            Files.createDirectories(workRoot)
            val stagingRoot = Files.createTempDirectory(workRoot, "nanfeng-bazi-preview-")
            try {
                when (val prepared = preparePreviewInput(input, password, stagingRoot)) {
                    is PreparedBackupInput.Rejected -> BackupPreviewResult.Rejected(
                        prepared.code,
                        prepared.message,
                    )
                    is PreparedBackupInput.Success -> prepared.input.use {
                        inspectBackup(it, stagingRoot, prepared.encrypted)
                    }
                }
            } finally {
                deleteRecursively(stagingRoot)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            BackupPreviewResult.Rejected(
                code = "PREVIEW_FAILED",
                message = "无法验证该完整备份；文件可能损坏或内容不受支持。",
            )
        }
    }

    override suspend fun prepareRestorePlan(
        preview: RestorePreview,
        decisions: List<BackupCaseRestoreDecision>,
    ): BackupRestorePlanResult {
        val sourceIds = preview.cases.map { it.sourceCaseId }
        if (sourceIds.size != sourceIds.distinct().size) {
            return rejectedPlan("DUPLICATE_SOURCE_CASE", "恢复预览包含重复来源命例，必须重新检查文件。")
        }
        if (sourceIds.size != preview.manifest.counts.cases) {
            return rejectedPlan("PREVIEW_CASE_COUNT_MISMATCH", "恢复预览命例数与清单不一致。")
        }
        if (decisions.map { it.sourceCaseId }.distinct().size != decisions.size) {
            return rejectedPlan("DUPLICATE_DECISION", "同一来源命例不能选择多个恢复方案。")
        }
        if (
            decisions.size != sourceIds.size ||
            decisions.map { it.sourceCaseId }.toSet() != sourceIds.toSet()
        ) {
            return rejectedPlan("DECISIONS_INCOMPLETE", "每个来源命例都必须明确选择一个恢复方案。")
        }
        val mergeTargets = decisions
            .filter { it.action == BackupCaseRestoreAction.MERGE }
            .mapNotNull { it.targetCaseId }
        if (mergeTargets.size != mergeTargets.distinct().size) {
            return rejectedPlan(
                "DUPLICATE_MERGE_TARGET",
                "同一批恢复中一个本地命例只能接收一个来源合并，避免顺序依赖。",
            )
        }
        val refreshed = try {
            refreshCaseRestorePreviews(preview.cases)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedPlan("CONFLICT_LOOKUP_FAILED", "无法重新核对当前库冲突，未生成恢复方案。")
        }
        if (refreshed != preview.cases) {
            return rejectedPlan("PREVIEW_STALE", "当前库已在预览后变化，请重新检查备份。")
        }
        val previewById = preview.cases.associateBy { it.sourceCaseId }
        decisions.forEach { decision ->
            val source = previewById.getValue(decision.sourceCaseId)
            when (decision.action) {
                BackupCaseRestoreAction.IMPORT_AS_IS -> {
                    if (source.conflicts.isNotEmpty()) {
                        return rejectedPlan(
                            "IMPORT_AS_IS_CONFLICT",
                            "存在冲突的命例不能按原稳定 ID 直接导入。",
                        )
                    }
                    if (decision.targetCaseId != null) {
                        return rejectedPlan("UNEXPECTED_TARGET", "直接导入不应指定本地目标。")
                    }
                    if (decision.modules.isNotEmpty() || decision.fieldChoices.isNotEmpty()) {
                        return rejectedPlan("UNEXPECTED_MERGE_SCOPE", "直接导入不应携带合并范围。")
                    }
                }
                BackupCaseRestoreAction.SKIP -> {
                    if (decision.targetCaseId != null) {
                        return rejectedPlan("UNEXPECTED_TARGET", "跳过不应指定本地目标。")
                    }
                    if (decision.modules.isNotEmpty() || decision.fieldChoices.isNotEmpty()) {
                        return rejectedPlan("UNEXPECTED_MERGE_SCOPE", "跳过不应携带合并范围。")
                    }
                }
                BackupCaseRestoreAction.KEEP_BOTH -> {
                    if (source.conflicts.isEmpty()) {
                        return rejectedPlan(
                            "KEEP_BOTH_WITHOUT_CONFLICT",
                            "没有冲突的来源命例应按原稳定 ID 导入或跳过。",
                        )
                    }
                    if (decision.targetCaseId != null) {
                        return rejectedPlan("UNEXPECTED_TARGET", "保留两份不应指定本地目标。")
                    }
                    if (decision.modules.isNotEmpty() || decision.fieldChoices.isNotEmpty()) {
                        return rejectedPlan("UNEXPECTED_MERGE_SCOPE", "保留两份不应携带合并范围。")
                    }
                }
                BackupCaseRestoreAction.MERGE -> {
                    val targetId = decision.targetCaseId
                        ?: return rejectedPlan("TARGET_REQUIRED", "范围合并必须明确选择本地目标。")
                    val target = source.conflicts.singleOrNull { it.localCaseId == targetId }
                        ?: return rejectedPlan(
                            "TARGET_NOT_IN_PREVIEW",
                            "范围合并目标不在当前冲突候选中。",
                        )
                    if (target.isTrashed) {
                        return rejectedPlan("TARGET_TRASHED", "回收站命例必须先恢复后才能合并。")
                    }
                    val targetCase = try {
                        repository.findById(targetId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    } ?: return rejectedPlan(
                        "TARGET_NOT_FOUND",
                        "所选本地目标命例已不存在，请重新检查备份。",
                    )
                    val analysis = mergeService.analyzeMerge(source.sourceCase, targetCase)
                    val differenceKeys = analysis.fieldDifferences.mapTo(mutableSetOf()) { it.key }
                    if (decision.fieldChoices.keys.any { it !in differenceKeys }) {
                        return rejectedPlan(
                            "INVALID_FIELD_CHOICE",
                            "字段采用方案包含当前不存在的差异，请重新选择合并范围。",
                        )
                    }
                    val counts = analysis.addableCounts
                    val modulesWithChanges = buildSet {
                        if (counts.calculationSnapshots > 0) {
                            add(SingleCaseMergeModule.CALCULATION_SNAPSHOTS)
                        }
                        if (counts.textRecords + counts.textRecordRevisions > 0) {
                            add(SingleCaseMergeModule.TEXT_RECORDS)
                        }
                        if (counts.events + counts.eventRevisions > 0) {
                            add(SingleCaseMergeModule.EVENTS)
                        }
                        if (counts.groups + counts.tags > 0) {
                            add(SingleCaseMergeModule.ORGANIZATION)
                        }
                    }
                    if (decision.modules.any { it !in modulesWithChanges }) {
                        return rejectedPlan(
                            "EMPTY_SELECTED_MODULE",
                            "合并范围包含当前没有可追加内容的模块，请重新选择。",
                        )
                    }
                    if (decision.modules.isEmpty() &&
                        decision.fieldChoices.values.none {
                            it == SingleCaseValueChoice.IMPORTED
                        }
                    ) {
                        return rejectedPlan(
                            "EMPTY_MERGE_SCOPE",
                            "范围合并必须至少选择一个有变化的模块或来源字段。",
                        )
                    }
                }
            }
        }
        val sourceIndex = sourceIds.withIndex().associate { it.value to it.index }
        return BackupRestorePlanResult.Success(
            BackupRestorePlan(
                preview = preview,
                decisions = decisions.sortedBy { sourceIndex.getValue(it.sourceCaseId) },
            ),
        )
    }

    override suspend fun prepareCaseMerge(
        preview: RestorePreview,
        sourceCaseId: String,
        targetCaseId: String,
    ): BackupCaseMergePreparationResult {
        val source = preview.cases.singleOrNull { it.sourceCaseId == sourceCaseId }
            ?: return rejectedCaseMerge("SOURCE_NOT_IN_PREVIEW", "来源命例不在当前完整备份预览中。")
        val refreshed = try {
            refreshCaseRestorePreviews(preview.cases)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedCaseMerge("CONFLICT_LOOKUP_FAILED", "无法重新核对当前库冲突。")
        }
        if (refreshed != preview.cases) {
            return rejectedCaseMerge("PREVIEW_STALE", "当前库已在预览后变化，请重新检查备份。")
        }
        val candidate = source.conflicts.singleOrNull { it.localCaseId == targetCaseId }
            ?: return rejectedCaseMerge("TARGET_NOT_IN_PREVIEW", "所选目标不在当前冲突候选中。")
        if (candidate.isTrashed) {
            return rejectedCaseMerge("TARGET_TRASHED", "回收站命例必须先恢复后才能合并。")
        }
        val target = try {
            repository.findById(targetCaseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return rejectedCaseMerge("TARGET_NOT_FOUND", "没有找到所选本地目标命例。")
        return BackupCaseMergePreparationResult.Success(
            BackupCaseMergePreparation(
                sourceCaseId = sourceCaseId,
                targetCaseId = target.id,
                targetAlias = target.alias,
                targetRevision = target.revision,
                analysis = mergeService.analyzeMerge(source.sourceCase, target),
            ),
        )
    }

    override suspend fun executeRestorePlan(
        plan: BackupRestorePlan,
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
        password: CharArray?,
    ): BackupRestoreExecutionResult {
        when (val recovery = recoverInterruptedRestores(attachmentRoot)) {
            is BackupRestoreRecoveryResult.RequiresAttention -> {
                return BackupRestoreExecutionResult.Rejected(
                    code = recovery.code,
                    message = recovery.message,
                )
            }
            is BackupRestoreRecoveryResult.Success -> Unit
        }
        Files.createDirectories(workRoot)
        val stagingRoot = Files.createTempDirectory(workRoot, "nanfeng-bazi-execute-")
        var attachmentStageRoot: Path? = null
        var finalAttachmentRoot: Path? = null
        var journalPath: Path? = null
        var journal: RestoreJournal? = null
        var committed = false
        return try {
            val checkedPlan = when (
                val checked = prepareRestorePlan(plan.preview, plan.decisions)
            ) {
                is BackupRestorePlanResult.Success -> checked.plan
                is BackupRestorePlanResult.Rejected -> {
                    return BackupRestoreExecutionResult.Rejected(
                        checked.code,
                        checked.message,
                    )
                }
            }
            val validated = when (
                val prepared = preparePreviewInput(input, password, stagingRoot)
            ) {
                is PreparedBackupInput.Rejected -> {
                    return BackupRestoreExecutionResult.Rejected(
                        prepared.code,
                        prepared.message,
                    )
                }
                is PreparedBackupInput.Success -> prepared.input.use {
                    readValidatedExecutionSource(it, stagingRoot, prepared.encrypted)
                }
            }
            if (validated is ExecutionSourceResult.Rejected) {
                return BackupRestoreExecutionResult.Rejected(
                    validated.code,
                    validated.message,
                )
            }
            validated as ExecutionSourceResult.Success
            val expectedSources = checkedPlan.preview.cases
                .associate { it.sourceCaseId to it.sourceCase }
            val actualSources = validated.snapshot.toDomainCases().associateBy { it.id }
            if (
                validated.manifest != checkedPlan.preview.manifest ||
                validated.extracted.size != checkedPlan.preview.sourceFileCount ||
                actualSources != expectedSources
            ) {
                return BackupRestoreExecutionResult.Rejected(
                    code = "SOURCE_CHANGED_AFTER_PREVIEW",
                    message = "提交时读取的备份已不是生成恢复方案的同一份文件。",
                )
            }

            val transactionId = UUID.randomUUID().toString()
            val usedIds = database.withTransaction { readSnapshot().allStableIds() }
            val preparedImports = checkedPlan.decisions.mapNotNull { decision ->
                val source = expectedSources.getValue(decision.sourceCaseId)
                when (decision.action) {
                    BackupCaseRestoreAction.IMPORT_AS_IS -> prepareImportedCase(
                        source = source,
                        restoredCaseId = source.id,
                        restoredRevision = maxOf(1, source.revision),
                        copiedFromCaseId = source.copiedFromCaseId,
                        transactionId = transactionId,
                        usedIds = usedIds,
                        extracted = validated.extracted,
                    )
                    BackupCaseRestoreAction.KEEP_BOTH -> {
                        val newCaseId = nextRestoreId(usedIds)
                        prepareImportedCase(
                            source = source,
                            restoredCaseId = newCaseId,
                            restoredRevision = 1,
                            copiedFromCaseId = source.id,
                            transactionId = transactionId,
                            usedIds = usedIds,
                            extracted = validated.extracted,
                        )
                    }
                    BackupCaseRestoreAction.SKIP,
                    BackupCaseRestoreAction.MERGE,
                    -> null
                }
            }.associateBy { it.sourceCaseId }

            val preparedMerges = checkedPlan.decisions.mapNotNull { decision ->
                if (decision.action != BackupCaseRestoreAction.MERGE) {
                    return@mapNotNull null
                }
                val targetId = checkNotNull(decision.targetCaseId)
                val target = repository.findById(targetId)
                    ?: throw RestoreExecutionRejectedException(
                        "TARGET_NOT_FOUND",
                        "范围合并目标已不存在，事务已回滚。",
                    )
                prepareMergedCase(
                    source = expectedSources.getValue(decision.sourceCaseId),
                    target = target,
                    modules = decision.modules,
                    fieldChoices = decision.fieldChoices,
                    transactionId = transactionId,
                    usedIds = usedIds,
                    extracted = validated.extracted,
                )
            }.associateBy { it.sourceCaseId }

            val attachmentFiles =
                preparedImports.values.flatMap { it.attachmentFiles } +
                    preparedMerges.values.flatMap { it.attachmentFiles }
            if (attachmentFiles.isNotEmpty()) {
                Files.createDirectories(attachmentRoot)
                val restoreJournal = RestoreJournal(
                    transactionId = transactionId,
                    state = RestoreJournalState.PREPARED,
                    finalDirectory = "$RESTORED_ATTACHMENTS_DIRECTORY/$transactionId",
                    expectedCases = (
                        preparedImports.values
                            .filter { it.attachmentFiles.isNotEmpty() }
                            .map {
                                RestoreExpectedCase(
                                    caseId = it.caseData.id,
                                    payloadSha256 = casePayloadSha256(it.caseData),
                                )
                            } +
                            preparedMerges.values
                                .filter { it.attachmentFiles.isNotEmpty() }
                                .map {
                                    RestoreExpectedCase(
                                        caseId = it.targetCaseId,
                                        payloadSha256 = casePayloadSha256(
                                            it.expectedCommittedCase,
                                        ),
                                        previousPayloadSha256 =
                                            it.previousPayloadSha256,
                                    )
                                }
                        ).also { expectedCases ->
                            check(
                                expectedCases.map { it.caseId }.distinct().size ==
                                    expectedCases.size,
                            ) { "恢复日志目标命例重复" }
                        },
                )
                journal = restoreJournal
                journalPath = writeRestoreJournal(attachmentRoot, restoreJournal)
                val stageParent = attachmentRoot.resolve(RESTORE_STAGING_DIRECTORY)
                Files.createDirectories(stageParent)
                val stageRoot = stageParent.resolve(transactionId)
                Files.createDirectory(stageRoot)
                attachmentStageRoot = stageRoot
                attachmentFiles.forEach { preparedAttachment ->
                    val target = stageRoot.resolve(preparedAttachment.fileName)
                    Files.copy(preparedAttachment.source, target)
                    check(
                        Files.size(target) == preparedAttachment.byteSize &&
                            sha256(target) == preparedAttachment.sha256,
                    ) { "暂存附件校验失败" }
                }
                val finalParent = attachmentRoot.resolve(RESTORED_ATTACHMENTS_DIRECTORY)
                Files.createDirectories(finalParent)
                val finalRoot = finalParent.resolve(transactionId)
                check(!Files.exists(finalRoot)) { "恢复附件事务目录已存在" }
                moveDirectory(stageRoot, finalRoot)
                attachmentStageRoot = null
                finalAttachmentRoot = finalRoot
                val movedJournal = restoreJournal.copy(
                    state = RestoreJournalState.FILES_MOVED,
                )
                journal = movedJournal
                journalPath = writeRestoreJournal(attachmentRoot, movedJournal)
            }

            var importedCount = 0
            var keptBothCount = 0
            var mergedCount = 0
            var skippedCount = 0
            database.withTransaction {
                when (
                    val rechecked = prepareRestorePlan(
                        checkedPlan.preview,
                        checkedPlan.decisions,
                    )
                ) {
                    is BackupRestorePlanResult.Rejected -> throw RestoreExecutionRejectedException(
                        rechecked.code,
                        rechecked.message,
                    )
                    is BackupRestorePlanResult.Success -> Unit
                }
                checkedPlan.decisions.forEach { decision ->
                    when (decision.action) {
                        BackupCaseRestoreAction.IMPORT_AS_IS -> {
                            val restored = preparedImports.getValue(decision.sourceCaseId)
                            val result = repository.insertRestored(restored.caseData)
                            if (result !is CaseWriteResult.Created) {
                                throw RestoreExecutionRejectedException(
                                    "IMPORT_ID_CONFLICT",
                                    "按原 ID 导入时检测到身份冲突，事务已回滚。",
                                )
                            }
                            importedCount += 1
                        }
                        BackupCaseRestoreAction.KEEP_BOTH -> {
                            val restored = preparedImports.getValue(decision.sourceCaseId)
                            val result = repository.insertRestored(restored.caseData)
                            if (result !is CaseWriteResult.Created) {
                                throw RestoreExecutionRejectedException(
                                    "GENERATED_ID_CONFLICT",
                                    "保留两份生成的身份发生冲突，事务已回滚。",
                                )
                            }
                            keptBothCount += 1
                        }
                        BackupCaseRestoreAction.MERGE -> {
                            val targetId = checkNotNull(decision.targetCaseId)
                            val target = repository.findById(targetId)
                                ?: throw RestoreExecutionRejectedException(
                                    "TARGET_NOT_FOUND",
                                    "范围合并目标已不存在，事务已回滚。",
                                )
                            val prepared = preparedMerges.getValue(decision.sourceCaseId)
                            if (
                                prepared.targetCaseId != targetId ||
                                prepared.targetRevision != target.revision
                            ) {
                                throw RestoreExecutionRejectedException(
                                    "PREVIEW_STALE",
                                    "范围合并目标在提交前发生变化，事务已回滚。",
                                )
                            }
                            val result = repository.save(
                                prepared.caseData,
                                expectedRevision = target.revision,
                            )
                            if (result !is CaseWriteResult.Updated) {
                                throw RestoreExecutionRejectedException(
                                    "MERGE_WRITE_CONTRACT_FAILED",
                                    "范围合并未按更新合同完成，事务已回滚。",
                                )
                            }
                            mergedCount += 1
                        }
                        BackupCaseRestoreAction.SKIP -> skippedCount += 1
                    }
                }
            }
            committed = true
            journal?.let { restoreJournal ->
                runCatching {
                    writeRestoreJournal(
                        attachmentRoot,
                        restoreJournal.copy(state = RestoreJournalState.DB_COMMITTED),
                    )
                    Files.deleteIfExists(
                        attachmentRoot
                            .resolve(RESTORE_JOURNAL_DIRECTORY)
                            .resolve("${restoreJournal.transactionId}.json"),
                    )
                }
            }
            BackupRestoreExecutionResult.Success(
                BackupRestoreExecutionSummary(
                    importedCases = importedCount,
                    keptBothCases = keptBothCount,
                    mergedCases = mergedCount,
                    skippedCases = skippedCount,
                    restoredAttachments = attachmentFiles.size,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (rejected: RestoreExecutionRejectedException) {
            BackupRestoreExecutionResult.Rejected(rejected.code, rejected.safeMessage)
        } catch (_: Exception) {
            BackupRestoreExecutionResult.Rejected(
                code = "RESTORE_EXECUTION_FAILED_ROLLED_BACK",
                message = "恢复提交失败，数据库事务已回滚，未覆盖现有附件。",
            )
        } finally {
            if (!committed) {
                finalAttachmentRoot?.let(::deleteRecursively)
                journalPath?.let(Files::deleteIfExists)
            }
            attachmentStageRoot?.let(::deleteRecursively)
            deleteRecursively(stagingRoot)
        }
    }

    override suspend fun recoverInterruptedRestores(
        attachmentRoot: Path,
    ): BackupRestoreRecoveryResult {
        val journalRoot = attachmentRoot.resolve(RESTORE_JOURNAL_DIRECTORY)
        if (!Files.isDirectory(journalRoot, LinkOption.NOFOLLOW_LINKS)) {
            return BackupRestoreRecoveryResult.Success(0, 0)
        }
        val journalFiles = mutableListOf<Path>()
        Files.list(journalRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                .filter { it.fileName.toString().endsWith(".json") }
                .sorted()
                .forEach(journalFiles::add)
        }
        if (journalFiles.size > MAX_RECOVERY_JOURNAL_COUNT) {
            return recoveryAttention(
                "RESTORE_JOURNAL_LIMIT_EXCEEDED",
                "待恢复事务日志数量异常，已停止自动处理以保护数据。",
            )
        }
        var rolledBack = 0
        var finalized = 0
        journalFiles.forEach { journalFile ->
            val journal = try {
                if (Files.size(journalFile) > MAX_RESTORE_JOURNAL_BYTES) {
                    return recoveryAttention(
                        "RESTORE_JOURNAL_INVALID",
                        "恢复事务日志大小异常，需要人工检查。",
                    )
                }
                DomainJson.decodeFromString<RestoreJournal>(readUtf8(journalFile))
            } catch (_: Exception) {
                return recoveryAttention(
                    "RESTORE_JOURNAL_INVALID",
                    "恢复事务日志损坏，需要人工检查；未删除任何恢复附件。",
                )
            }
            if (!journal.isValid()) {
                return recoveryAttention(
                    "RESTORE_JOURNAL_INVALID",
                    "恢复事务日志包含不安全路径或无效身份，需要人工检查。",
                )
            }
            val finalRoot = attachmentRoot.resolve(journal.finalDirectory).normalize()
            val stageRoot = attachmentRoot
                .resolve(RESTORE_STAGING_DIRECTORY)
                .resolve(journal.transactionId)
                .normalize()
            val restoredCases = journal.expectedCases.map { expected ->
                val caseData = repository.findById(expected.caseId)
                val payloadSha256 = caseData?.let(::casePayloadSha256)
                val state = when {
                    expected.previousPayloadSha256 == null && caseData == null ->
                        RecoveryCaseState.PREVIOUS
                    expected.previousPayloadSha256 != null &&
                        payloadSha256 == expected.previousPayloadSha256 ->
                        RecoveryCaseState.PREVIOUS
                    payloadSha256 == expected.payloadSha256 ->
                        RecoveryCaseState.COMMITTED
                    else -> RecoveryCaseState.UNKNOWN
                }
                RestoredCaseState(expected, caseData, state)
            }
            if (restoredCases.all { it.state == RecoveryCaseState.PREVIOUS }) {
                deleteRecursively(finalRoot)
                deleteRecursively(stageRoot)
                Files.deleteIfExists(journalFile)
                rolledBack += 1
            } else if (
                restoredCases.any { it.state == RecoveryCaseState.PREVIOUS } &&
                restoredCases.any { it.state == RecoveryCaseState.COMMITTED }
            ) {
                return recoveryAttention(
                    "RESTORE_PARTIAL_DATABASE_STATE",
                    "检测到恢复事务只写入了部分命例，已保留日志和附件等待人工检查。",
                )
            } else if (restoredCases.all { it.state == RecoveryCaseState.COMMITTED }) {
                val exactAndReadable = restoredCases.all { restored ->
                    restored.caseData != null &&
                        casePayloadSha256(restored.caseData) ==
                            restored.expected.payloadSha256 &&
                        restored.caseData.attachments.all { attachment ->
                            val file = resolveContained(
                                attachmentRoot,
                                attachment.relativePath,
                            )
                            file != null &&
                                Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) &&
                                Files.size(file) == attachment.byteSize &&
                                sha256(file) == attachment.sha256
                        }
                }
                if (!exactAndReadable) {
                    return recoveryAttention(
                        "RESTORE_COMMITTED_STATE_MISMATCH",
                        "恢复命例已存在但事实或附件不完整，已保留现场等待人工检查。",
                    )
                }
                deleteRecursively(stageRoot)
                Files.deleteIfExists(journalFile)
                finalized += 1
            } else {
                return recoveryAttention(
                    "RESTORE_COMMITTED_STATE_MISMATCH",
                    "恢复命例处于未知或被修改状态，已保留日志和附件等待人工检查。",
                )
            }
        }
        return BackupRestoreRecoveryResult.Success(rolledBack, finalized)
    }

    suspend fun restoreIntoEmptyStore(
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
    ): BackupRestoreResult {
        Files.createDirectories(workRoot)
        if (database.withTransaction { dao.allCases().isNotEmpty() }) {
            return BackupRestoreResult.Rejected(
                code = "DESTINATION_NOT_EMPTY",
                message = "当前阶段只允许恢复到空数据库，不能覆盖已有命例。",
            )
        }
        if (
            Files.exists(attachmentRoot) &&
            (
                !Files.isDirectory(attachmentRoot, LinkOption.NOFOLLOW_LINKS) ||
                    Files.list(attachmentRoot).use { it.findAny().isPresent }
                )
        ) {
            return BackupRestoreResult.Rejected(
                code = "ATTACHMENT_DESTINATION_NOT_EMPTY",
                message = "当前阶段只允许恢复到空附件目录。",
            )
        }

        val stagingRoot = Files.createTempDirectory(workRoot, "nanfeng-bazi-restore-")
        var attachmentTargetTouched = false
        return try {
            val extracted = extractSafely(input, stagingRoot)
                ?: return rejectedAndClean(
                    stagingRoot,
                    "INVALID_ZIP",
                    "备份压缩包路径、数量或展开大小不符合安全限制。",
                )
            val manifestPath = extracted[MANIFEST_PATH]
                ?: return rejectedAndClean(
                    stagingRoot,
                    "MANIFEST_MISSING",
                    "备份缺少 manifest.json。",
                )
            val manifest = DomainJson.decodeFromString<BackupManifest>(
                readUtf8(manifestPath),
            )
            val validationError = validateExtracted(manifest, extracted)
            if (validationError != null) {
                return rejectedAndClean(
                    stagingRoot,
                    validationError.first,
                    validationError.second,
                )
            }

            val snapshot = parseSnapshot(extracted)
            validateReferences(snapshot, extracted, manifest)?.let {
                return rejectedAndClean(stagingRoot, it.first, it.second)
            }
            validateInIndependentDatabase(snapshot)?.let {
                return rejectedAndClean(stagingRoot, it.first, it.second)
            }

            val stagedAttachments = stagingRoot.resolve(ATTACHMENTS_DIRECTORY)
            Files.createDirectories(stagedAttachments)
            database.withTransaction {
                check(dao.allCases().isEmpty()) {
                    "恢复提交前检测到其他命例写入"
                }
                insertSnapshot(snapshot)
                if (Files.exists(attachmentRoot)) {
                    Files.delete(attachmentRoot)
                }
                attachmentTargetTouched = true
                Files.createDirectories(attachmentRoot.parent)
                moveDirectory(stagedAttachments, attachmentRoot)
            }
            deleteRecursively(stagingRoot)

            BackupRestoreResult.Success(
                RestorePreview(
                    manifest = manifest,
                    sourceFileCount = extracted.size,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (attachmentTargetTouched && Files.exists(attachmentRoot)) {
                deleteRecursively(attachmentRoot)
            }
            deleteRecursively(stagingRoot)
            BackupRestoreResult.Rejected(
                code = "RESTORE_FAILED_ROLLED_BACK",
                message = "恢复失败，已回滚本次写入；现有数据未被覆盖。",
            )
        }
    }

    private suspend fun readSnapshot(
        sourceDatabase: NanfengBaziDatabase = database,
    ): RoomDataSnapshot {
        val sourceDao = sourceDatabase.caseDao()
        return RoomDataSnapshot(
            cases = sourceDao.allCases(),
            calculationSnapshots = sourceDao.allCalculationSnapshots(),
            textRecords = sourceDao.allTextRecords(),
            textRecordRevisions = sourceDao.allTextRecordRevisions(),
            events = sourceDao.allEvents(),
            eventRevisions = sourceDao.allEventRevisions(),
            attachments = sourceDao.allAttachments(),
            fieldEvidence = sourceDao.allFieldEvidence(),
            groups = sourceDao.allGroups(),
            tags = sourceDao.allTags(),
            caseGroupCrossRefs = sourceDao.allCaseGroupCrossRefs(),
            caseTagCrossRefs = sourceDao.allCaseTagCrossRefs(),
        )
    }

    private suspend fun readValidatedExecutionSource(
        input: InputStream,
        stagingRoot: Path,
        encrypted: Boolean,
    ): ExecutionSourceResult {
        val extracted = extractSafely(input, stagingRoot)
            ?: return ExecutionSourceResult.Rejected(
                "INVALID_ZIP",
                "备份压缩包路径、数量或展开大小不符合安全限制。",
            )
        val manifestPath = extracted[MANIFEST_PATH]
            ?: return ExecutionSourceResult.Rejected(
                "MANIFEST_MISSING",
                "备份缺少 manifest.json。",
            )
        val manifest = DomainJson.decodeFromString<BackupManifest>(readUtf8(manifestPath))
        validateExtracted(manifest, extracted, encrypted)?.let { error ->
            return ExecutionSourceResult.Rejected(error.first, error.second)
        }
        val snapshot = parseSnapshot(extracted)
        validateReferences(snapshot, extracted, manifest)?.let { error ->
            return ExecutionSourceResult.Rejected(error.first, error.second)
        }
        validateInIndependentDatabase(snapshot)?.let { error ->
            return ExecutionSourceResult.Rejected(error.first, error.second)
        }
        return ExecutionSourceResult.Success(manifest, snapshot, extracted)
    }

    private fun prepareImportedCase(
        source: BaziCase,
        restoredCaseId: String,
        restoredRevision: Long,
        copiedFromCaseId: String?,
        transactionId: String,
        usedIds: MutableSet<String>,
        extracted: Map<String, Path>,
    ): PreparedImportedCase {
        usedIds.add(restoredCaseId)
        val attachmentIds = source.attachments.associate { it.id to nextRestoreId(usedIds) }
        val recordIds = (
            source.textRecords.map { it.id } +
                source.textRecordRevisions.map { it.recordId }
            ).distinct().associateWith { nextRestoreId(usedIds) }
        val eventIds = (
            source.events.map { it.id } +
                source.eventRevisions.map { it.eventId }
            ).distinct().associateWith { nextRestoreId(usedIds) }
        val candidateIds = source.birthTimeCandidates.associate {
            it.id to nextRestoreId(usedIds)
        }
        val snapshotIds = source.calculationSnapshots.associate {
            it.id to nextRestoreId(usedIds)
        }
        val restoredAttachments = source.attachments.map { attachment ->
            val newId = attachmentIds.getValue(attachment.id)
            attachment.copy(
                id = newId,
                relativePath =
                    "$RESTORED_ATTACHMENTS_DIRECTORY/$transactionId/$newId",
            )
        }
        val caseData = source.copy(
            id = restoredCaseId,
            textRecords = source.textRecords.map { record ->
                record.copy(
                    id = recordIds.getValue(record.id),
                    sourceAttachmentId = record.sourceAttachmentId?.let(attachmentIds::getValue),
                )
            },
            textRecordRevisions = source.textRecordRevisions.map { revision ->
                val recordId = recordIds.getValue(revision.recordId)
                revision.copy(
                    id = nextRestoreId(usedIds),
                    recordId = recordId,
                    snapshot = revision.snapshot.copy(
                        id = recordId,
                        sourceAttachmentId = revision.snapshot.sourceAttachmentId
                            ?.let(attachmentIds::getValue),
                    ),
                )
            },
            events = source.events.map { event ->
                event.copy(
                    id = eventIds.getValue(event.id),
                    sourceAttachmentId = event.sourceAttachmentId?.let(attachmentIds::getValue),
                )
            },
            eventRevisions = source.eventRevisions.map { revision ->
                val eventId = eventIds.getValue(revision.eventId)
                revision.copy(
                    id = nextRestoreId(usedIds),
                    eventId = eventId,
                    snapshot = revision.snapshot.copy(
                        id = eventId,
                        sourceAttachmentId = revision.snapshot.sourceAttachmentId
                            ?.let(attachmentIds::getValue),
                    ),
                )
            },
            birthTimeCandidates = source.birthTimeCandidates.map { candidate ->
                candidate.copy(
                    id = candidateIds.getValue(candidate.id),
                    calculationSnapshotId = snapshotIds.getValue(
                        candidate.calculationSnapshotId,
                    ),
                )
            },
            calculationSnapshots = source.calculationSnapshots.map { snapshot ->
                snapshot.copy(
                    id = snapshotIds.getValue(snapshot.id),
                    birthTimeCandidateId = snapshot.birthTimeCandidateId?.let(
                        candidateIds::getValue,
                    ),
                )
            },
            attachments = restoredAttachments,
            fieldEvidence = source.fieldEvidence.map { evidence ->
                evidence.copy(
                    id = nextRestoreId(usedIds),
                    attachmentId = attachmentIds.getValue(evidence.attachmentId),
                )
            },
            groups = source.groups.map { it.copy(id = nextRestoreId(usedIds)) },
            tags = source.tags.map { it.copy(id = nextRestoreId(usedIds)) },
            copiedFromCaseId = copiedFromCaseId,
            revision = restoredRevision,
        )
        val attachmentFiles = source.attachments.map { sourceAttachment ->
            val restored = restoredAttachments.single {
                it.id == attachmentIds.getValue(sourceAttachment.id)
            }
            PreparedAttachmentFile(
                source = extracted.getValue(
                    "$ATTACHMENTS_DIRECTORY/${sourceAttachment.relativePath}",
                ),
                fileName = restored.id,
                byteSize = restored.byteSize,
                sha256 = restored.sha256,
            )
        }
        return PreparedImportedCase(source.id, caseData, attachmentFiles)
    }

    private fun prepareMergedCase(
        source: BaziCase,
        target: BaziCase,
        modules: Set<SingleCaseMergeModule>,
        fieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice>,
        transactionId: String,
        usedIds: MutableSet<String>,
        extracted: Map<String, Path>,
    ): PreparedMergedCase {
        val requiredAttachmentIds = mergeService.requiredAttachmentIdsForRestore(
            source = source,
            target = target,
            modules = modules,
        )
        val sourceAttachments = source.attachments
            .filter { it.id in requiredAttachmentIds }
        check(sourceAttachments.size == requiredAttachmentIds.size) {
            "所选合并模块存在无法解析的附件引用"
        }
        val attachmentIds = sourceAttachments.associate {
            it.id to nextRestoreId(usedIds)
        }
        val restoredAttachments = sourceAttachments.map { attachment ->
            val newId = attachmentIds.getValue(attachment.id)
            attachment.copy(
                id = newId,
                relativePath =
                    "$RESTORED_ATTACHMENTS_DIRECTORY/$transactionId/$newId",
            )
        }
        val merged = mergeService.buildMergedCaseForRestore(
            source = source,
            target = target,
            modules = modules,
            fieldChoices = fieldChoices,
            attachmentIdMapping = attachmentIds,
            importedAttachments = restoredAttachments,
        )
        val attachmentFiles = sourceAttachments.map { sourceAttachment ->
            val restored = restoredAttachments.single {
                it.id == attachmentIds.getValue(sourceAttachment.id)
            }
            PreparedAttachmentFile(
                source = extracted.getValue(
                    "$ATTACHMENTS_DIRECTORY/${sourceAttachment.relativePath}",
                ),
                fileName = restored.id,
                byteSize = restored.byteSize,
                sha256 = restored.sha256,
            )
        }
        return PreparedMergedCase(
            sourceCaseId = source.id,
            targetCaseId = target.id,
            targetRevision = target.revision,
            caseData = merged,
            expectedCommittedCase = merged.copy(revision = target.revision + 1),
            previousPayloadSha256 = casePayloadSha256(target),
            attachmentFiles = attachmentFiles,
        )
    }

    private fun nextRestoreId(usedIds: MutableSet<String>): String {
        repeat(MAX_RESTORE_ID_GENERATION_ATTEMPTS) {
            val candidate = idGenerator().trim()
            if (
                candidate.matches(RESTORE_GENERATED_ID_PATTERN) &&
                usedIds.add(candidate)
            ) {
                return candidate
            }
        }
        throw IllegalArgumentException("Unable to generate restore id")
    }

    private fun recoveryAttention(
        code: String,
        message: String,
    ) = BackupRestoreRecoveryResult.RequiresAttention(code, message)

    private suspend fun inspectBackup(
        input: InputStream,
        stagingRoot: Path,
        encrypted: Boolean,
    ): BackupPreviewResult {
        val extracted = extractSafely(input, stagingRoot)
            ?: return BackupPreviewResult.Rejected(
                code = "INVALID_ZIP",
                message = "备份压缩包路径、数量或展开大小不符合安全限制。",
            )
        val manifestPath = extracted[MANIFEST_PATH]
            ?: return BackupPreviewResult.Rejected(
                code = "MANIFEST_MISSING",
                message = "备份缺少 manifest.json。",
            )
        val manifest = DomainJson.decodeFromString<BackupManifest>(readUtf8(manifestPath))
        validateExtracted(manifest, extracted, encrypted)?.let { error ->
            return BackupPreviewResult.Rejected(error.first, error.second)
        }
        val snapshot = parseSnapshot(extracted)
        validateReferences(snapshot, extracted, manifest)?.let { error ->
            return BackupPreviewResult.Rejected(error.first, error.second)
        }
        val databasePreflight = validateInIndependentDatabase(snapshot)?.let { error ->
            return BackupPreviewResult.Rejected(error.first, error.second)
        } ?: if (stagingDatabaseContext == null) {
            BackupDatabasePreflight.NOT_RUN
        } else {
            BackupDatabasePreflight.INDEPENDENT_ROOM_ROUND_TRIP_VERIFIED
        }
        return BackupPreviewResult.Success(
            RestorePreview(
                manifest = manifest,
                sourceFileCount = extracted.size,
                cases = buildCaseRestorePreviews(snapshot),
                databasePreflight = databasePreflight,
            ),
        )
    }

    private suspend fun buildCaseRestorePreviews(
        source: RoomDataSnapshot,
    ): List<BackupCaseRestorePreview> {
        val sources = source.toDomainCases().map { sourceCase ->
            BackupCaseRestorePreview(
                sourceCase = sourceCase,
                conflicts = emptyList(),
            )
        }
        return refreshCaseRestorePreviews(sources)
    }

    private suspend fun refreshCaseRestorePreviews(
        sources: List<BackupCaseRestorePreview>,
    ): List<BackupCaseRestorePreview> {
        val (localCases, localSnapshots) = database.withTransaction {
            dao.allCases() to dao.allCalculationSnapshots()
        }
        val localResults = adoptedResultsByCase(localSnapshots)
        val localBirthInputs = localCases.associate { entity ->
            entity.id to DomainJson.decodeFromString<BirthInput>(entity.birthInputJson)
        }
        return sources.map { sourceCase ->
            val conflicts = localCases.mapNotNull { localCase ->
                val reasons = buildSet {
                    if (sourceCase.sourceCaseId == localCase.id) {
                        add(BackupCaseConflictReason.STABLE_ID_EXISTS)
                    }
                    if (
                        sourceCase.sourceBirthInput.hasSameBirthIdentity(
                            localBirthInputs.getValue(localCase.id),
                            sourceCase.sourceCanonicalSolarDateTime,
                            localResults[localCase.id]
                                ?.calendarConversion
                                ?.solarDateTime,
                        )
                    ) {
                        add(BackupCaseConflictReason.SAME_BIRTH_INPUT)
                    }
                    if (
                        sourceCase.sourceFourPillars != null &&
                        sourceCase.sourceFourPillars == localResults[localCase.id]?.fourPillars
                    ) {
                        add(BackupCaseConflictReason.SAME_FOUR_PILLARS)
                    }
                }
                reasons.takeIf { it.isNotEmpty() }?.let {
                    BackupCaseConflictCandidate(
                        localCaseId = localCase.id,
                        localAlias = localCase.alias,
                        localRevision = localCase.revision,
                        isTrashed = localCase.deletedAtEpochMillis != null,
                        reasons = it,
                    )
                }
            }.sortedWith(compareBy({ it.isTrashed }, { it.localAlias }, { it.localCaseId }))
            sourceCase.copy(conflicts = conflicts)
        }.sortedWith(compareBy({ it.isTrashed }, { it.sourceAlias }, { it.sourceCaseId }))
    }

    private fun rejectedPlan(code: String, message: String) =
        BackupRestorePlanResult.Rejected(code, message)

    private fun rejectedCaseMerge(code: String, message: String) =
        BackupCaseMergePreparationResult.Rejected(code, message)

    private fun adoptedResultsByCase(
        snapshots: List<CalculationSnapshotEntity>,
    ) = snapshots
        .asSequence()
        .filter { it.adopted }
        .groupBy { it.caseId }
        .mapValues { (_, values) ->
            values.maxByOrNull { it.sortOrder }
                ?.let { DomainJson.decodeFromString<CaseCalculationSnapshot>(it.resultJson) }
                ?.result
        }

    private fun preparePreviewInput(
        input: InputStream,
        password: CharArray?,
        stagingRoot: Path,
    ): PreparedBackupInput {
        val prefix = ByteArray(BackupEncryption.MAGIC_BYTES.size)
        var count = 0
        while (count < prefix.size) {
            val read = input.read(prefix, count, prefix.size - count)
            if (read < 0) break
            count += read
        }
        if (
            count == BackupEncryption.MAGIC_BYTES.size &&
            prefix.contentEquals(BackupEncryption.MAGIC_BYTES)
        ) {
            if (password == null || password.isEmpty()) {
                return PreparedBackupInput.Rejected(
                    "PASSWORD_REQUIRED",
                    "该完整备份已加密，请输入密码。",
                )
            }
            val target = stagingRoot.resolve("decrypted-backup.zip")
            return try {
                encryption.decryptAfterMagic(input, password, target)
                PreparedBackupInput.Success(
                    input = Files.newInputStream(target),
                    encrypted = true,
                )
            } catch (_: Exception) {
                PreparedBackupInput.Rejected(
                    "DECRYPTION_FAILED",
                    "密码错误、文件已损坏或加密参数不受支持。",
                )
            }
        }
        return PreparedBackupInput.Success(
            input = SequenceInputStream(ByteArrayInputStream(prefix, 0, count), input),
            encrypted = false,
        )
    }

    private fun buildEntrySources(
        snapshot: RoomDataSnapshot,
        attachmentRoot: Path,
    ): List<EntrySource>? {
        val notes = snapshot.textRecords.filter {
            it.type == "NOTE" || it.type == "OWNER_FEEDBACK"
        }
        val analysis = snapshot.textRecords.filter {
            it.type == "MASTER_COMMENTARY" || it.type == "ANALYSIS"
        }
        val revisionsByType = snapshot.textRecordRevisions.groupBy { entity ->
            DomainJson.decodeFromString(
                CaseTextRecordRevision.serializer(),
                entity.revisionJson,
            ).snapshot.type.name
        }
        val noteRevisions = revisionsByType["NOTE"].orEmpty() +
            revisionsByType["OWNER_FEEDBACK"].orEmpty()
        val analysisRevisions = revisionsByType["MASTER_COMMENTARY"].orEmpty() +
            revisionsByType["ANALYSIS"].orEmpty()
        val jsonSources = listOf(
            CASES_PATH to DomainJson.encodeToString(CasesFile(snapshot.cases)),
            SNAPSHOTS_PATH to DomainJson.encodeToString(
                SnapshotsFile(snapshot.calculationSnapshots),
            ),
            NOTES_PATH to DomainJson.encodeToString(
                TextRecordsFile(notes, noteRevisions),
            ),
            ANALYSIS_PATH to DomainJson.encodeToString(
                TextRecordsFile(analysis, analysisRevisions),
            ),
            EVENTS_PATH to DomainJson.encodeToString(
                EventsFile(snapshot.events, snapshot.eventRevisions),
            ),
            GROUPS_PATH to DomainJson.encodeToString(
                GroupsFile(snapshot.groups, snapshot.caseGroupCrossRefs),
            ),
            TAGS_PATH to DomainJson.encodeToString(
                TagsFile(snapshot.tags, snapshot.caseTagCrossRefs),
            ),
            SETTINGS_PATH to DomainJson.encodeToString(SettingsFile()),
            IMPORTS_PATH to DomainJson.encodeToString(
                ImportsFile(snapshot.attachments, snapshot.fieldEvidence),
            ),
        ).map { (path, json) -> EntrySource.fromBytes(path, json.encodeToByteArray()) }

        val attachmentSources = snapshot.attachments.map { attachment ->
            val file = resolveContained(attachmentRoot, attachment.relativePath) ?: return null
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null
            if (Files.size(file) != attachment.byteSize) return null
            if (sha256(file) != attachment.sha256) return null
            EntrySource.fromFile("$ATTACHMENTS_DIRECTORY/${attachment.relativePath}", file)
        }
        return jsonSources + attachmentSources
    }

    private fun buildManifest(
        snapshot: RoomDataSnapshot,
        sources: List<EntrySource>,
        appVersion: String,
        encrypted: Boolean,
    ): BackupManifest {
        val calculationSnapshots = snapshot.calculationSnapshots.map {
            DomainJson.decodeFromString(CaseCalculationSnapshot.serializer(), it.resultJson)
        }
        return BackupManifest(
            formatVersion = BACKUP_FORMAT_VERSION,
            appVersion = appVersion,
            databaseSchemaVersion = NanfengBaziDatabase.SCHEMA_VERSION,
            createdAt = clock.instant().toString(),
            encrypted = encrypted,
            encryptionParametersVersion = if (encrypted) {
                BackupEncryption.PROTECTION_VERSION
            } else {
                null
            },
            engineVersions = calculationSnapshots
                .map { it.result.evidence.engineVersion }
                .distinct()
                .sorted(),
            ruleVersions = calculationSnapshots
                .map { it.result.evidence.ruleVersion }
                .distinct()
                .sorted(),
            counts = BackupCounts(
                cases = snapshot.cases.size,
                snapshots = snapshot.calculationSnapshots.size,
                textRecords = snapshot.textRecords.size,
                events = snapshot.events.size,
                attachments = snapshot.attachments.size,
                textRecordRevisions = snapshot.textRecordRevisions.size,
                eventRevisions = snapshot.eventRevisions.size,
            ),
            files = sources.map {
                BackupFileManifest(it.path, it.byteSize, it.sha256)
            },
        )
    }

    private fun extractSafely(
        input: InputStream,
        stagingRoot: Path,
    ): Map<String, Path>? {
        val extracted = linkedMapOf<String, Path>()
        var totalBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                if (extracted.size >= MAX_ENTRY_COUNT) return null
                val name = entry.name
                if (!isSafeEntryName(name) || extracted.containsKey(name)) return null
                val target = resolveContained(stagingRoot, name) ?: return null
                Files.createDirectories(target.parent)
                var entryBytes = 0L
                Files.newOutputStream(target).buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        if (entryBytes > MAX_SINGLE_ENTRY_BYTES || totalBytes > MAX_TOTAL_BYTES) {
                            return null
                        }
                        output.write(buffer, 0, count)
                    }
                }
                if (name.endsWith(".json") && entryBytes > MAX_JSON_BYTES) return null
                extracted[name] = target
                zip.closeEntry()
            }
        }
        return extracted
    }

    private fun validateExtracted(
        manifest: BackupManifest,
        extracted: Map<String, Path>,
        encryptedContainer: Boolean = false,
    ): Pair<String, String>? {
        if (manifest.formatVersion != BACKUP_FORMAT_VERSION) {
            return "UNSUPPORTED_FORMAT" to "不支持该备份格式版本。"
        }
        if (
            manifest.encrypted != encryptedContainer ||
            manifest.encryptionParametersVersion != if (encryptedContainer) {
                BackupEncryption.PROTECTION_VERSION
            } else {
                null
            }
        ) {
            return "FILE_PROTECTION_MISMATCH" to "备份清单与文件保护状态不一致。"
        }
        if (manifest.databaseSchemaVersion !in
            MIN_SUPPORTED_BACKUP_SCHEMA..NanfengBaziDatabase.SCHEMA_VERSION
        ) {
            return "UNSUPPORTED_SCHEMA" to "备份数据库 Schema 与当前版本不兼容。"
        }
        val required = setOf(
            CASES_PATH,
            SNAPSHOTS_PATH,
            NOTES_PATH,
            ANALYSIS_PATH,
            EVENTS_PATH,
            GROUPS_PATH,
            TAGS_PATH,
            SETTINGS_PATH,
            IMPORTS_PATH,
        )
        if (!extracted.keys.containsAll(required)) {
            return "DATA_FILE_MISSING" to "备份缺少必要数据文件。"
        }
        val expectedPaths = manifest.files.map { it.path }
        if (expectedPaths.size != expectedPaths.distinct().size) {
            return "DUPLICATE_MANIFEST_PATH" to "manifest 中存在重复文件路径。"
        }
        if (expectedPaths.toSet() != extracted.keys.minus(MANIFEST_PATH)) {
            return "FILE_SET_MISMATCH" to "实际文件集合与 manifest 不一致。"
        }
        manifest.files.forEach { file ->
            val path = extracted.getValue(file.path)
            if (Files.size(path) != file.byteSize || sha256(path) != file.sha256) {
                return "FILE_HASH_MISMATCH" to "文件 ${file.path} 的大小或 SHA-256 不一致。"
            }
        }
        return null
    }

    private fun parseSnapshot(extracted: Map<String, Path>): RoomDataSnapshot {
        val cases = decode<CasesFile>(extracted.getValue(CASES_PATH)).cases
        val snapshots = decode<SnapshotsFile>(
            extracted.getValue(SNAPSHOTS_PATH),
        ).snapshots
        val notes = decode<TextRecordsFile>(extracted.getValue(NOTES_PATH))
        val analysis = decode<TextRecordsFile>(
            extracted.getValue(ANALYSIS_PATH),
        )
        val events = decode<EventsFile>(extracted.getValue(EVENTS_PATH))
        val groups = decode<GroupsFile>(extracted.getValue(GROUPS_PATH))
        val tags = decode<TagsFile>(extracted.getValue(TAGS_PATH))
        val imports = decode<ImportsFile>(extracted.getValue(IMPORTS_PATH))
        decode<SettingsFile>(extracted.getValue(SETTINGS_PATH))
        return RoomDataSnapshot(
            cases = cases,
            calculationSnapshots = snapshots,
            textRecords = notes.records + analysis.records,
            textRecordRevisions = notes.revisions + analysis.revisions,
            events = events.events,
            eventRevisions = events.revisions,
            attachments = imports.attachments,
            fieldEvidence = imports.fieldEvidence,
            groups = groups.groups,
            tags = tags.tags,
            caseGroupCrossRefs = groups.crossRefs,
            caseTagCrossRefs = tags.crossRefs,
        )
    }

    private inline fun <reified T> decode(path: Path): T =
        DomainJson.decodeFromString(readUtf8(path))

    private fun validateReferences(
        snapshot: RoomDataSnapshot,
        extracted: Map<String, Path>,
        manifest: BackupManifest,
    ): Pair<String, String>? {
        fun <T> duplicate(values: List<T>): Boolean = values.distinct().size != values.size
        val caseIds = snapshot.cases.map { it.id }
        val attachmentIds = snapshot.attachments.map { it.id }
        if (
            duplicate(caseIds) ||
            duplicate(attachmentIds) ||
            duplicate(snapshot.textRecordRevisions.map { it.id }) ||
            duplicate(snapshot.eventRevisions.map { it.id })
        ) {
            return "DUPLICATE_ID" to "备份中存在重复命例、附件或历史版本 ID。"
        }
        val caseIdSet = caseIds.toSet()
        if (
            snapshot.calculationSnapshots.any { it.caseId !in caseIdSet } ||
            snapshot.textRecords.any { it.caseId !in caseIdSet } ||
            snapshot.textRecordRevisions.any { it.caseId !in caseIdSet } ||
            snapshot.events.any { it.caseId !in caseIdSet } ||
            snapshot.eventRevisions.any { it.caseId !in caseIdSet } ||
            snapshot.attachments.any { it.caseId !in caseIdSet } ||
            snapshot.fieldEvidence.any { it.caseId !in caseIdSet }
        ) {
            return "BROKEN_CASE_REFERENCE" to "备份存在无法关联到命例的记录。"
        }
        val attachmentIdSet = attachmentIds.toSet()
        if (snapshot.fieldEvidence.any { it.attachmentId !in attachmentIdSet }) {
            return "BROKEN_ATTACHMENT_REFERENCE" to "字段证据关联的附件不存在。"
        }
        val attachmentsById = snapshot.attachments.associateBy { it.id }
        if (
            snapshot.fieldEvidence.any {
                attachmentsById[it.attachmentId]?.caseId != it.caseId
            } ||
            snapshot.textRecords.any {
                it.sourceAttachmentId != null &&
                    attachmentsById[it.sourceAttachmentId]?.caseId != it.caseId
            } ||
            snapshot.textRecordRevisions.any {
                val revision = DomainJson.decodeFromString(
                    CaseTextRecordRevision.serializer(),
                    it.revisionJson,
                )
                revision.id != it.id ||
                    revision.recordId != it.recordId ||
                    revision.version != it.version ||
                    revision.changeType.name != it.changeType ||
                    revision.changedAt.toEpochMilli() != it.changedAtEpochMillis ||
                    revision.snapshot.sourceAttachmentId != null &&
                    attachmentsById[revision.snapshot.sourceAttachmentId]?.caseId != it.caseId
            } ||
            snapshot.events.any {
                it.sourceAttachmentId != null &&
                    attachmentsById[it.sourceAttachmentId]?.caseId != it.caseId
            } ||
            snapshot.eventRevisions.any {
                val revision = DomainJson.decodeFromString(
                    CaseEventRevision.serializer(),
                    it.revisionJson,
                )
                revision.id != it.id ||
                    revision.eventId != it.eventId ||
                    revision.version != it.version ||
                    revision.changeType.name != it.changeType ||
                    revision.changedAt.toEpochMilli() != it.changedAtEpochMillis ||
                    revision.snapshot.sourceAttachmentId != null &&
                    attachmentsById[revision.snapshot.sourceAttachmentId]?.caseId != it.caseId
            }
        ) {
            return "CROSS_CASE_ATTACHMENT_REFERENCE" to "记录引用了其他命例的附件。"
        }
        val metadataAttachmentPaths = snapshot.attachments
            .map { "$ATTACHMENTS_DIRECTORY/${it.relativePath}" }
            .toSet()
        val extractedAttachmentPaths = extracted.keys
            .filter { it.startsWith("$ATTACHMENTS_DIRECTORY/") }
            .toSet()
        if (metadataAttachmentPaths != extractedAttachmentPaths) {
            return "ATTACHMENT_PATH_MISMATCH" to "附件路径集合不一致。"
        }
        snapshot.attachments.forEach {
            val path = extracted.getValue("$ATTACHMENTS_DIRECTORY/${it.relativePath}")
            if (Files.size(path) != it.byteSize || sha256(path) != it.sha256) {
                return "ATTACHMENT_METADATA_MISMATCH" to
                    "附件 ${it.relativePath} 与数据库元数据不一致。"
            }
        }
        val groupIds = snapshot.groups.map { it.id }.toSet()
        val tagIds = snapshot.tags.map { it.id }.toSet()
        if (
            snapshot.caseGroupCrossRefs.any {
                it.caseId !in caseIdSet || it.groupId !in groupIds
            } ||
            snapshot.caseTagCrossRefs.any {
                it.caseId !in caseIdSet || it.tagId !in tagIds
            }
        ) {
            return "BROKEN_CLASSIFICATION_REFERENCE" to "分组或标签关联不完整。"
        }
        val expectedCounts = BackupCounts(
            cases = snapshot.cases.size,
            snapshots = snapshot.calculationSnapshots.size,
            textRecords = snapshot.textRecords.size,
            events = snapshot.events.size,
            attachments = snapshot.attachments.size,
            textRecordRevisions = snapshot.textRecordRevisions.size,
            eventRevisions = snapshot.eventRevisions.size,
        )
        if (manifest.counts != expectedCounts) {
            return "COUNT_MISMATCH" to "manifest 中的数据数量与实际内容不一致。"
        }
        return null
    }

    private suspend fun validateInIndependentDatabase(
        snapshot: RoomDataSnapshot,
    ): Pair<String, String>? {
        val context = stagingDatabaseContext ?: return null
        val stagingDatabase = Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            NanfengBaziDatabase::class.java,
        ).build()
        return try {
            stagingDatabase.withTransaction {
                insertSnapshot(snapshot, stagingDatabase)
            }
            val reread = stagingDatabase.withTransaction {
                readSnapshot(stagingDatabase)
            }
            if (reread != snapshot || reread.toDomainCases() != snapshot.toDomainCases()) {
                "STAGING_DATABASE_ROUND_TRIP_MISMATCH" to
                    "备份写入独立临时数据库后内容不一致，已停止恢复。"
            } else {
                null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            "STAGING_DATABASE_REJECTED" to
                "备份无法完整写入独立临时数据库，已停止恢复且未触碰正式数据。"
        } finally {
            stagingDatabase.close()
        }
    }

    private suspend fun insertSnapshot(
        snapshot: RoomDataSnapshot,
        targetDatabase: NanfengBaziDatabase = database,
    ) {
        val targetDao = targetDatabase.caseDao()
        snapshot.cases.forEach { targetDao.insertCase(it) }
        targetDao.insertGroups(snapshot.groups)
        targetDao.insertTags(snapshot.tags)
        targetDao.insertAttachments(snapshot.attachments)
        targetDao.insertCalculationSnapshots(snapshot.calculationSnapshots)
        targetDao.insertTextRecords(snapshot.textRecords)
        targetDao.insertTextRecordRevisions(snapshot.textRecordRevisions)
        targetDao.insertEvents(snapshot.events)
        targetDao.insertEventRevisions(snapshot.eventRevisions)
        targetDao.insertFieldEvidence(snapshot.fieldEvidence)
        targetDao.insertCaseGroupCrossRefs(snapshot.caseGroupCrossRefs)
        targetDao.insertCaseTagCrossRefs(snapshot.caseTagCrossRefs)
    }

    private fun rejectedAndClean(
        stagingRoot: Path,
        code: String,
        message: String,
    ): BackupRestoreResult.Rejected {
        deleteRecursively(stagingRoot)
        return BackupRestoreResult.Rejected(code, message)
    }

    private data class EntrySource(
        val path: String,
        val byteSize: Long,
        val sha256: String,
        val writeTo: (OutputStream) -> Unit,
    ) {
        companion object {
            fun fromBytes(path: String, bytes: ByteArray): EntrySource = EntrySource(
                path = path,
                byteSize = bytes.size.toLong(),
                sha256 = sha256(bytes),
                writeTo = { it.write(bytes) },
            )

            fun fromFile(path: String, file: Path): EntrySource = EntrySource(
                path = path,
                byteSize = Files.size(file),
                sha256 = sha256(file),
                writeTo = { output ->
                    Files.newInputStream(file).buffered().use { it.copyTo(output) }
                },
            )
        }
    }

    private data class PreparedImportedCase(
        val sourceCaseId: String,
        val caseData: BaziCase,
        val attachmentFiles: List<PreparedAttachmentFile>,
    )

    private data class PreparedMergedCase(
        val sourceCaseId: String,
        val targetCaseId: String,
        val targetRevision: Long,
        val caseData: BaziCase,
        val expectedCommittedCase: BaziCase,
        val previousPayloadSha256: String,
        val attachmentFiles: List<PreparedAttachmentFile>,
    )

    private data class PreparedAttachmentFile(
        val source: Path,
        val fileName: String,
        val byteSize: Long,
        val sha256: String,
    )

    private data class RestoredCaseState(
        val expected: RestoreExpectedCase,
        val caseData: BaziCase?,
        val state: RecoveryCaseState,
    )

    private enum class RecoveryCaseState {
        PREVIOUS,
        COMMITTED,
        UNKNOWN,
    }

    private sealed interface ExecutionSourceResult {
        data class Success(
            val manifest: BackupManifest,
            val snapshot: RoomDataSnapshot,
            val extracted: Map<String, Path>,
        ) : ExecutionSourceResult

        data class Rejected(
            val code: String,
            val message: String,
        ) : ExecutionSourceResult
    }

    private class RestoreExecutionRejectedException(
        val code: String,
        val safeMessage: String,
    ) : IllegalStateException(safeMessage)

    private fun RoomDataSnapshot.allStableIds(): MutableSet<String> = buildSet {
        addAll(cases.map { it.id })
        addAll(calculationSnapshots.map { it.id })
        addAll(textRecords.map { it.id })
        addAll(textRecordRevisions.map { it.id })
        addAll(events.map { it.id })
        addAll(eventRevisions.map { it.id })
        addAll(attachments.map { it.id })
        addAll(fieldEvidence.map { it.id })
        addAll(groups.map { it.id })
        addAll(tags.map { it.id })
    }.toMutableSet()

    private sealed interface PreparedBackupInput {
        data class Success(
            val input: InputStream,
            val encrypted: Boolean,
        ) : PreparedBackupInput

        data class Rejected(
            val code: String,
            val message: String,
        ) : PreparedBackupInput
    }

    companion object {
        const val BACKUP_FORMAT_VERSION = 1
        private const val MANIFEST_PATH = "manifest.json"
        private const val MIN_SUPPORTED_BACKUP_SCHEMA = 2
        private const val CASES_PATH = "cases.json"
        private const val SNAPSHOTS_PATH = "snapshots.json"
        private const val NOTES_PATH = "notes.json"
        private const val ANALYSIS_PATH = "analysis.json"
        private const val EVENTS_PATH = "events.json"
        private const val GROUPS_PATH = "groups.json"
        private const val TAGS_PATH = "tags.json"
        private const val SETTINGS_PATH = "settings.json"
        private const val IMPORTS_PATH = "imports.json"
        private const val ATTACHMENTS_DIRECTORY = "attachments"
        private const val RESTORE_STAGING_DIRECTORY = RESTORE_STAGING_DIRECTORY_NAME
        private const val RESTORE_JOURNAL_DIRECTORY = RESTORE_JOURNAL_DIRECTORY_NAME
        private const val RESTORED_ATTACHMENTS_DIRECTORY =
            RESTORED_ATTACHMENTS_DIRECTORY_NAME
        private const val MAX_RECOVERY_JOURNAL_COUNT = 1_000
        private const val MAX_RESTORE_JOURNAL_BYTES = 64L * 1024
        private const val MAX_RESTORE_ID_GENERATION_ATTEMPTS = 100
        private const val MAX_ENTRY_COUNT = 10_000
        private const val MAX_JSON_BYTES = 16L * 1024 * 1024
        private const val MAX_SINGLE_ENTRY_BYTES = 512L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
        private val FILE_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")
        private val RESTORE_GENERATED_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

private fun isSafeEntryName(name: String): Boolean {
    if (name.isBlank() || name.startsWith("/") || name.startsWith("\\")) return false
    if ('\\' in name || ':' in name) return false
    return name.split('/').none { it.isBlank() || it == "." || it == ".." }
}

private fun resolveContained(root: Path, relative: String): Path? {
    val normalizedRoot = root.toAbsolutePath().normalize()
    val resolved = normalizedRoot.resolve(relative).normalize()
    return resolved.takeIf { it.startsWith(normalizedRoot) }
}

private fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().toHex()
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

private fun stableZipEntry(path: String): ZipEntry = ZipEntry(path).apply {
    time = 0L
}

private fun moveDirectory(source: Path, target: Path) {
    try {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source, target)
    }
}

private fun readUtf8(path: Path): String =
    Files.newBufferedReader(path, Charsets.UTF_8).use { it.readText() }

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun deleteRecursively(root: Path) {
    if (!Files.exists(root)) return
    Files.walk(root).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
