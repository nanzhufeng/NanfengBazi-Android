package com.nanzhufeng.nanfengbazi.data.exchange

import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class SingleCaseExchangeService(
    private val repository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
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

        val adoptedFourPillars = document.caseData.calculationSnapshots
            .asReversed()
            .firstOrNull { it.adopted }
            ?.result
            ?.fourPillars
        val conflicts = linkedMapOf<String, MutableConflict>()
        val duplicateCandidates = try {
            repository.findById(document.caseData.id)?.let { existing ->
                conflicts.getOrPut(existing.id) {
                    MutableConflict(existing.id, existing.alias, existing.deletedAt != null)
                }.reasons += SingleCaseConflictReason.STABLE_ID_EXISTS
            }
            repository.findDuplicateCandidates(
                birthInput = document.caseData.birthInput,
                fourPillars = adoptedFourPillars,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedPreview("CONFLICT_LOOKUP_FAILED", "读取本地冲突候选失败，未写入数据。")
        }
        duplicateCandidates.forEach { candidate ->
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

        return SingleCasePreviewResult.Success(
            SingleCasePreview(
                document = document,
                counts = document.caseData.counts(),
                conflicts = conflicts.values
                    .map { it.toCandidate() }
                    .sortedWith(compareBy({ it.isTrashed }, { it.alias }, { it.caseId })),
            ),
        )
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
