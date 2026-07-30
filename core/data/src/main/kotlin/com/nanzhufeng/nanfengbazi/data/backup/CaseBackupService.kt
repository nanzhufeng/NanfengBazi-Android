package com.nanzhufeng.nanfengbazi.data.backup

import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.db.CalculationSnapshotEntity
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.db.RoomDataSnapshot
import com.nanzhufeng.nanfengbazi.data.exchange.PasswordCrypto
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.repository.hasSameBirthIdentity
import com.nanzhufeng.nanfengbazi.data.repository.toDomainCases
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
}

class CaseBackupService(
    private val database: NanfengBaziDatabase,
    private val clock: Clock = Clock.systemUTC(),
    secureRandom: SecureRandom = SecureRandom(),
    passwordKdfIterations: Int = PasswordCrypto.DEFAULT_KDF_ITERATIONS,
) : CaseBackupOperations {
    private val dao = database.caseDao()
    private val encryption = BackupEncryption(secureRandom, passwordKdfIterations)

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
        "南枫八字备份_${FILE_NAME_TIME_FORMAT.format(clock.instant().atZone(ZoneId.systemDefault()))}.zip"

    override fun suggestedEncryptedFileName(): String =
        "南枫八字备份_${FILE_NAME_TIME_FORMAT.format(clock.instant().atZone(ZoneId.systemDefault()))}_加密.nfbak"

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
                    if (
                        decision.modules.isEmpty() &&
                        decision.fieldChoices.values.none { it == SingleCaseValueChoice.IMPORTED }
                    ) {
                        return rejectedPlan("EMPTY_MERGE_SCOPE", "范围合并必须至少选择一个模块或来源字段。")
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

    private suspend fun readSnapshot(): RoomDataSnapshot = RoomDataSnapshot(
        cases = dao.allCases(),
        calculationSnapshots = dao.allCalculationSnapshots(),
        textRecords = dao.allTextRecords(),
        textRecordRevisions = dao.allTextRecordRevisions(),
        events = dao.allEvents(),
        eventRevisions = dao.allEventRevisions(),
        attachments = dao.allAttachments(),
        fieldEvidence = dao.allFieldEvidence(),
        groups = dao.allGroups(),
        tags = dao.allTags(),
        caseGroupCrossRefs = dao.allCaseGroupCrossRefs(),
        caseTagCrossRefs = dao.allCaseTagCrossRefs(),
    )

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
        return BackupPreviewResult.Success(
            RestorePreview(
                manifest = manifest,
                sourceFileCount = extracted.size,
                cases = buildCaseRestorePreviews(snapshot),
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
        val localPillars = adoptedPillarsByCase(localSnapshots)
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
                        )
                    ) {
                        add(BackupCaseConflictReason.SAME_BIRTH_INPUT)
                    }
                    if (
                        sourceCase.sourceFourPillars != null &&
                        sourceCase.sourceFourPillars == localPillars[localCase.id]
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

    private fun adoptedPillarsByCase(
        snapshots: List<CalculationSnapshotEntity>,
    ) = snapshots
        .asSequence()
        .filter { it.adopted }
        .groupBy { it.caseId }
        .mapValues { (_, values) ->
            values.maxByOrNull { it.sortOrder }
                ?.let { DomainJson.decodeFromString<CaseCalculationSnapshot>(it.resultJson) }
                ?.result
                ?.fourPillars
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

    private suspend fun insertSnapshot(snapshot: RoomDataSnapshot) {
        snapshot.cases.forEach { dao.insertCase(it) }
        dao.insertGroups(snapshot.groups)
        dao.insertTags(snapshot.tags)
        dao.insertAttachments(snapshot.attachments)
        dao.insertCalculationSnapshots(snapshot.calculationSnapshots)
        dao.insertTextRecords(snapshot.textRecords)
        dao.insertTextRecordRevisions(snapshot.textRecordRevisions)
        dao.insertEvents(snapshot.events)
        dao.insertEventRevisions(snapshot.eventRevisions)
        dao.insertFieldEvidence(snapshot.fieldEvidence)
        dao.insertCaseGroupCrossRefs(snapshot.caseGroupCrossRefs)
        dao.insertCaseTagCrossRefs(snapshot.caseTagCrossRefs)
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
        private const val MAX_ENTRY_COUNT = 10_000
        private const val MAX_JSON_BYTES = 16L * 1024 * 1024
        private const val MAX_SINGLE_ENTRY_BYTES = 512L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
        private val FILE_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")
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
