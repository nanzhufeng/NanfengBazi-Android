package com.nanzhufeng.nanfengbazi.data.backup

import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class RestoreJournal(
    val formatVersion: Int = RESTORE_JOURNAL_PROTOCOL_VERSION,
    val transactionId: String,
    val state: RestoreJournalState,
    val finalDirectory: String,
    val expectedCases: List<RestoreExpectedCase>,
) {
    fun isValid(): Boolean =
        formatVersion == RESTORE_JOURNAL_PROTOCOL_VERSION &&
            transactionId.matches(RESTORE_TRANSACTION_ID_PATTERN) &&
            finalDirectory == "$RESTORED_ATTACHMENTS_DIRECTORY_NAME/$transactionId" &&
            expectedCases.isNotEmpty() &&
            expectedCases.map { it.caseId }.distinct().size == expectedCases.size &&
            expectedCases.all {
                it.caseId.isNotBlank() &&
                    it.payloadSha256.matches(RESTORE_SHA256_PATTERN) &&
                    (
                        it.previousPayloadSha256 == null ||
                            (
                                it.previousPayloadSha256.matches(
                                    RESTORE_SHA256_PATTERN,
                                ) &&
                                    it.previousPayloadSha256 != it.payloadSha256
                                )
                        )
            }
}

@Serializable
internal enum class RestoreJournalState {
    PREPARED,
    FILES_MOVED,
    DB_COMMITTED,
}

@Serializable
internal data class RestoreExpectedCase(
    val caseId: String,
    val payloadSha256: String,
    val previousPayloadSha256: String? = null,
)

internal fun casePayloadSha256(caseData: BaziCase): String =
    restoreSha256(DomainJson.encodeToString(caseData).encodeToByteArray())

internal fun writeRestoreJournal(
    attachmentRoot: Path,
    journal: RestoreJournal,
): Path {
    val journalRoot = attachmentRoot.resolve(RESTORE_JOURNAL_DIRECTORY_NAME)
    Files.createDirectories(journalRoot)
    val target = journalRoot.resolve("${journal.transactionId}.json")
    val temporary = journalRoot.resolve(
        "${journal.transactionId}.${UUID.randomUUID()}.tmp",
    )
    return try {
        Files.write(
            temporary,
            DomainJson.encodeToString(journal).encodeToByteArray(),
        )
        FileChannel.open(temporary, StandardOpenOption.WRITE).use {
            it.force(true)
        }
        try {
            Files.move(
                temporary,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporary,
                target,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        target
    } finally {
        Files.deleteIfExists(temporary)
    }
}

internal fun restoreSha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

internal const val RESTORE_STAGING_DIRECTORY_NAME = ".restore-staging"
internal const val RESTORE_JOURNAL_DIRECTORY_NAME = ".restore-journal"
internal const val RESTORED_ATTACHMENTS_DIRECTORY_NAME = "restored"
internal const val RESTORE_JOURNAL_PROTOCOL_VERSION = 1
internal val RESTORE_TRANSACTION_ID_PATTERN =
    Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
internal val RESTORE_SHA256_PATTERN = Regex("[0-9a-f]{64}")
