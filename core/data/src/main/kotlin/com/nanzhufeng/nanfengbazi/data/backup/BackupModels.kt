package com.nanzhufeng.nanfengbazi.data.backup

import com.nanzhufeng.nanfengbazi.data.db.CalculationSnapshotEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEventEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseEventRevisionEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseGroupCrossRefEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseGroupEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseTagCrossRefEntity
import com.nanzhufeng.nanfengbazi.data.db.CaseTagEntity
import com.nanzhufeng.nanfengbazi.data.db.FieldEvidenceEntity
import com.nanzhufeng.nanfengbazi.data.db.SourceAttachmentEntity
import com.nanzhufeng.nanfengbazi.data.db.TextRecordEntity
import com.nanzhufeng.nanfengbazi.data.db.TextRecordRevisionEntity
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.CaseMergeAnalysis
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val databaseSchemaVersion: Int,
    val createdAt: String,
    val encrypted: Boolean,
    val encryptionParametersVersion: Int? = null,
    val engineVersions: List<String>,
    val ruleVersions: List<String>,
    val counts: BackupCounts,
    val files: List<BackupFileManifest>,
)

@Serializable
data class BackupCounts(
    val cases: Int,
    val snapshots: Int,
    val textRecords: Int,
    val events: Int,
    val attachments: Int,
    val textRecordRevisions: Int = 0,
    val eventRevisions: Int = 0,
)

@Serializable
data class BackupFileManifest(
    val path: String,
    val byteSize: Long,
    val sha256: String,
)

sealed interface BackupProtection {
    data object UnencryptedSensitiveDataConfirmed : BackupProtection

    data class PasswordProtected(
        val password: CharArray,
    ) : BackupProtection
}

sealed interface BackupExportResult {
    data class Success(
        val counts: BackupCounts,
        val fileCount: Int,
    ) : BackupExportResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : BackupExportResult
}

data class RestorePreview(
    val manifest: BackupManifest,
    val sourceFileCount: Int,
    val cases: List<BackupCaseRestorePreview> = emptyList(),
)

enum class BackupCaseConflictReason {
    STABLE_ID_EXISTS,
    SAME_BIRTH_INPUT,
    SAME_FOUR_PILLARS,
}

data class BackupCaseConflictCandidate(
    val localCaseId: String,
    val localAlias: String,
    val localRevision: Long,
    val isTrashed: Boolean,
    val reasons: Set<BackupCaseConflictReason>,
)

data class BackupCaseRestorePreview(
    val sourceCase: BaziCase,
    val conflicts: List<BackupCaseConflictCandidate>,
) {
    val sourceCaseId: String get() = sourceCase.id
    val sourceAlias: String get() = sourceCase.alias
    val sourceRevision: Long get() = sourceCase.revision
    val isTrashed: Boolean get() = sourceCase.deletedAt != null
    val sourceBirthInput get() = sourceCase.birthInput
    val sourceFourPillars get() = sourceCase.calculationSnapshots
        .asReversed()
        .firstOrNull { it.adopted }
        ?.result
        ?.fourPillars
}

enum class BackupCaseRestoreAction {
    IMPORT_AS_IS,
    SKIP,
    KEEP_BOTH,
    MERGE,
}

data class BackupCaseRestoreDecision(
    val sourceCaseId: String,
    val action: BackupCaseRestoreAction,
    val targetCaseId: String? = null,
    val modules: Set<SingleCaseMergeModule> = emptySet(),
    val fieldChoices: Map<SingleCaseFieldKey, SingleCaseValueChoice> = emptyMap(),
)

data class BackupRestorePlan(
    val preview: RestorePreview,
    val decisions: List<BackupCaseRestoreDecision>,
)

sealed interface BackupRestorePlanResult {
    data class Success(
        val plan: BackupRestorePlan,
    ) : BackupRestorePlanResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : BackupRestorePlanResult
}

data class BackupCaseMergePreparation(
    val sourceCaseId: String,
    val targetCaseId: String,
    val targetAlias: String,
    val targetRevision: Long,
    val analysis: CaseMergeAnalysis,
)

sealed interface BackupCaseMergePreparationResult {
    data class Success(
        val preparation: BackupCaseMergePreparation,
    ) : BackupCaseMergePreparationResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : BackupCaseMergePreparationResult
}

sealed interface BackupPreviewResult {
    data class Success(
        val preview: RestorePreview,
    ) : BackupPreviewResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : BackupPreviewResult
}

sealed interface BackupRestoreResult {
    data class Success(
        val preview: RestorePreview,
    ) : BackupRestoreResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : BackupRestoreResult
}

@Serializable
internal data class CasesFile(
    val cases: List<CaseEntity>,
)

@Serializable
internal data class SnapshotsFile(
    val snapshots: List<CalculationSnapshotEntity>,
)

@Serializable
internal data class TextRecordsFile(
    val records: List<TextRecordEntity>,
    val revisions: List<TextRecordRevisionEntity> = emptyList(),
)

@Serializable
internal data class EventsFile(
    val events: List<CaseEventEntity>,
    val revisions: List<CaseEventRevisionEntity> = emptyList(),
)

@Serializable
internal data class GroupsFile(
    val groups: List<CaseGroupEntity>,
    val crossRefs: List<CaseGroupCrossRefEntity>,
)

@Serializable
internal data class TagsFile(
    val tags: List<CaseTagEntity>,
    val crossRefs: List<CaseTagCrossRefEntity>,
)

@Serializable
internal data class ImportsFile(
    val attachments: List<SourceAttachmentEntity>,
    val fieldEvidence: List<FieldEvidenceEntity>,
)

@Serializable
internal data class SettingsFile(
    val schemaVersion: Int = 1,
)
