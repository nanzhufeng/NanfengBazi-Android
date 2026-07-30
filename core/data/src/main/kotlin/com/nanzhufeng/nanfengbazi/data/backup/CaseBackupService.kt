package com.nanzhufeng.nanfengbazi.data.backup

import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.db.RoomDataSnapshot
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString

interface CaseBackupOperations {
    suspend fun export(
        output: OutputStream,
        attachmentRoot: Path,
        appVersion: String,
        protection: BackupProtection,
    ): BackupExportResult

    fun suggestedFileName(): String

    suspend fun preview(
        input: InputStream,
        workRoot: Path,
    ): BackupPreviewResult
}

class CaseBackupService(
    private val database: NanfengBaziDatabase,
    private val clock: Clock = Clock.systemUTC(),
) : CaseBackupOperations {
    private val dao = database.caseDao()

    override suspend fun export(
        output: OutputStream,
        attachmentRoot: Path,
        appVersion: String,
        protection: BackupProtection,
    ): BackupExportResult {
        if (protection is BackupProtection.PasswordProtected) {
            return BackupExportResult.Rejected(
                code = "PASSWORD_ENCRYPTION_NOT_IMPLEMENTED",
                message = "当前阶段尚未实现密码加密备份，不能静默降级为未加密文件。",
            )
        }
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
            val manifest = buildManifest(snapshot, sources, appVersion)
            val manifestBytes = DomainJson.encodeToString(manifest).encodeToByteArray()

            ZipOutputStream(output.buffered()).use { zip ->
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

    override suspend fun preview(
        input: InputStream,
        workRoot: Path,
    ): BackupPreviewResult {
        return try {
            Files.createDirectories(workRoot)
            val stagingRoot = Files.createTempDirectory(workRoot, "nanfeng-bazi-preview-")
            try {
                inspectBackup(input, stagingRoot)
            } finally {
                deleteRecursively(stagingRoot)
            }
        } catch (_: Exception) {
            BackupPreviewResult.Rejected(
                code = "PREVIEW_FAILED",
                message = "无法验证该完整备份；文件可能损坏或内容不受支持。",
            )
        }
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

    private fun inspectBackup(
        input: InputStream,
        stagingRoot: Path,
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
        validateExtracted(manifest, extracted)?.let { error ->
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
            ),
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
    ): BackupManifest {
        val calculationSnapshots = snapshot.calculationSnapshots.map {
            DomainJson.decodeFromString(CaseCalculationSnapshot.serializer(), it.resultJson)
        }
        return BackupManifest(
            formatVersion = BACKUP_FORMAT_VERSION,
            appVersion = appVersion,
            databaseSchemaVersion = NanfengBaziDatabase.SCHEMA_VERSION,
            createdAt = clock.instant().toString(),
            encrypted = false,
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
    ): Pair<String, String>? {
        if (manifest.formatVersion != BACKUP_FORMAT_VERSION) {
            return "UNSUPPORTED_FORMAT" to "不支持该备份格式版本。"
        }
        if (manifest.encrypted) {
            return "ENCRYPTION_NOT_SUPPORTED" to "当前阶段尚未实现加密备份恢复。"
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
