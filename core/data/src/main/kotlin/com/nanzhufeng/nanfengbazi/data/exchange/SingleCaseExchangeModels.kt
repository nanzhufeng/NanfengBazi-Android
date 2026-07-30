package com.nanzhufeng.nanfengbazi.data.exchange

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import kotlinx.serialization.Serializable

@Serializable
enum class SingleCaseAttachmentMode {
    REFERENCES_ONLY,
    BUNDLED_BINARIES,
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

@Serializable
data class SingleCaseEncryptedDocument(
    val containerType: String,
    val protectionVersion: Int,
    val kdfAlgorithm: String,
    val kdfIterations: Int,
    val saltBase64: String,
    val cipherAlgorithm: String,
    val nonceBase64: String,
    val ciphertextBase64: String,
)

@Serializable
data class SingleCaseBundleManifest(
    val formatVersion: Int,
    val documentPath: String,
    val documentByteSize: Long,
    val documentSha256: String,
    val attachments: List<SingleCaseBundleAttachmentEntry>,
)

@Serializable
data class SingleCaseBundleAttachmentEntry(
    val attachmentId: String,
    val path: String,
    val byteSize: Long,
    val sha256: String,
)

enum class SingleCaseDocumentProtection {
    UNENCRYPTED,
    PASSWORD_PROTECTED,
}

data class SingleCaseCounts(
    val calculationSnapshots: Int,
    val textRecords: Int,
    val textRecordRevisions: Int,
    val events: Int,
    val eventRevisions: Int,
    val attachmentReferences: Int,
    val fieldEvidence: Int,
    val groups: Int = 0,
    val tags: Int = 0,
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
    val bundleManifestSha256: String? = null,
    val protection: SingleCaseDocumentProtection =
        SingleCaseDocumentProtection.UNENCRYPTED,
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

enum class SingleCaseMergeModule {
    CALCULATION_SNAPSHOTS,
    TEXT_RECORDS,
    EVENTS,
    ORGANIZATION,
}

enum class SingleCaseFieldKey {
    ALIAS,
    NAME,
    SOURCE_TYPE,
    BIRTH_INPUT,
    PROFILE_OCCUPATION,
    PROFILE_EDUCATION,
    PROFILE_FINANCE,
    PROFILE_MARRIAGE,
    PROFILE_HEALTH,
    FAVORITE,
    PINNED,
}

enum class SingleCaseValueChoice {
    LOCAL,
    IMPORTED,
}

data class SingleCaseFieldDifference(
    val key: SingleCaseFieldKey,
    val label: String,
    val localValue: String,
    val importedValue: String,
)

data class CaseMergeAnalysis(
    val fieldDifferences: List<SingleCaseFieldDifference>,
    val addableCounts: SingleCaseCounts,
)

data class SingleCaseMergePreparation(
    val sourcePreview: SingleCasePreview,
    val targetCaseId: String,
    val targetAlias: String,
    val targetRevision: Long,
    val targetPayloadSha256: String,
    val fieldDifferences: List<SingleCaseFieldDifference>,
    val addableCounts: SingleCaseCounts,
)

data class SingleCaseMergePlan(
    val preparation: SingleCaseMergePreparation,
    val modules: Set<SingleCaseMergeModule> = emptySet(),
    val fieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice> = emptyMap(),
)

sealed interface SingleCaseMergePreparationResult {
    data class Success(
        val preparation: SingleCaseMergePreparation,
    ) : SingleCaseMergePreparationResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : SingleCaseMergePreparationResult
}

sealed interface SingleCaseImportResult {
    data class Imported(
        val caseId: String,
        val revision: Long,
    ) : SingleCaseImportResult

    data class Skipped(
        val sourceCaseId: String,
    ) : SingleCaseImportResult

    data class Merged(
        val caseId: String,
        val revision: Long,
        val addedCounts: SingleCaseCounts,
    ) : SingleCaseImportResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : SingleCaseImportResult
}

internal sealed interface PreparedBundledImportResult {
    data class Success(
        val caseData: BaziCase,
    ) : PreparedBundledImportResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : PreparedBundledImportResult
}

internal sealed interface PreparedBundledMergeResult {
    data class Success(
        val caseData: BaziCase,
        val targetRevision: Long,
        val previousPayloadSha256: String,
        val expectedCommittedCase: BaziCase,
        val addedCounts: SingleCaseCounts,
    ) : PreparedBundledMergeResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : PreparedBundledMergeResult
}

internal data class BundledPreviewRejection(
    val code: String,
    val message: String,
)
