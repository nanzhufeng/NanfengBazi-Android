package com.nanzhufeng.nanfengbazi.data.exchange

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import kotlinx.serialization.Serializable

@Serializable
enum class SingleCaseAttachmentMode {
    REFERENCES_ONLY,
}

@Serializable
data class SingleCaseDocument(
    val formatVersion: Int,
    val appVersion: String,
    val databaseSchemaVersion: Int,
    val exportedAt: String,
    val attachmentMode: SingleCaseAttachmentMode = SingleCaseAttachmentMode.REFERENCES_ONLY,
    val payloadSha256: String,
    val caseData: BaziCase,
)

data class SingleCaseCounts(
    val calculationSnapshots: Int,
    val textRecords: Int,
    val textRecordRevisions: Int,
    val events: Int,
    val eventRevisions: Int,
    val attachmentReferences: Int,
    val fieldEvidence: Int,
)

enum class SingleCaseConflictReason {
    STABLE_ID_EXISTS,
    SAME_BIRTH_INPUT,
    SAME_FOUR_PILLARS,
}

data class SingleCaseConflictCandidate(
    val caseId: String,
    val alias: String,
    val reasons: Set<SingleCaseConflictReason>,
    val isTrashed: Boolean,
)

data class SingleCasePreview(
    val document: SingleCaseDocument,
    val counts: SingleCaseCounts,
    val conflicts: List<SingleCaseConflictCandidate>,
    val containsAttachmentBinaries: Boolean = false,
)

sealed interface SingleCaseProtection {
    data object UnencryptedSensitiveDataConfirmed : SingleCaseProtection

    data class PasswordProtected(
        val password: CharArray,
    ) : SingleCaseProtection
}

sealed interface SingleCaseExportResult {
    data class Success(
        val suggestedFileName: String,
        val byteSize: Long,
        val sha256: String,
    ) : SingleCaseExportResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : SingleCaseExportResult
}

sealed interface SingleCasePreviewResult {
    data class Success(
        val preview: SingleCasePreview,
    ) : SingleCasePreviewResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : SingleCasePreviewResult
}

enum class SingleCaseImportDecision {
    SKIP,
    KEEP_BOTH,
}

sealed interface SingleCaseImportResult {
    data class Imported(
        val caseId: String,
        val revision: Long,
    ) : SingleCaseImportResult

    data class Skipped(
        val sourceCaseId: String,
    ) : SingleCaseImportResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : SingleCaseImportResult
}
