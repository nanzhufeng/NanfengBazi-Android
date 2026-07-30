package com.nanzhufeng.nanfengbazi.data.exchange

import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
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

    private fun cloneForKeepBoth(source: BaziCase): BaziCase {
        val usedIds = mutableSetOf<String>()
        fun nextId(): String {
            repeat(MAX_ID_GENERATION_ATTEMPTS) {
                val candidate = idGenerator().trim()
                if (candidate.isNotEmpty() && usedIds.add(candidate)) return candidate
            }
            throw IllegalArgumentException("Unable to generate a unique non-blank id")
        }

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

    companion object {
        const val FORMAT_VERSION = 1
        const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024
        private const val MAX_ID_GENERATION_ATTEMPTS = 32
        private const val MIN_SUPPORTED_DATABASE_SCHEMA = 2
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

private fun BaziCase.counts() = SingleCaseCounts(
    calculationSnapshots = calculationSnapshots.size,
    textRecords = textRecords.size,
    textRecordRevisions = textRecordRevisions.size,
    events = events.size,
    eventRevisions = eventRevisions.size,
    attachmentReferences = attachments.size,
    fieldEvidence = fieldEvidence.size,
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
