package com.nanzhufeng.nanfengbazi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
internal interface CaseDao {
    @Query("SELECT * FROM cases WHERE id = :id")
    suspend fun findCase(id: String): CaseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCase(entity: CaseEntity)

    @Update
    suspend fun updateCase(entity: CaseEntity)

    @Query(
        """
        UPDATE cases SET lastViewedAtEpochMillis = :viewedAtEpochMillis
        WHERE id = :caseId AND deletedAtEpochMillis IS NULL
        """,
    )
    suspend fun markViewed(caseId: String, viewedAtEpochMillis: Long): Int

    @Query(
        "DELETE FROM cases WHERE id IN (:caseIds) AND deletedAtEpochMillis IS NOT NULL",
    )
    suspend fun deleteTrashedCasesPermanently(caseIds: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCalculationSnapshots(entities: List<CalculationSnapshotEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTextRecords(entities: List<TextRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTextRecordRevisions(entities: List<TextRecordRevisionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvents(entities: List<CaseEventEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEventRevisions(entities: List<CaseEventRevisionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttachments(entities: List<SourceAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFieldEvidence(entities: List<FieldEvidenceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(entities: List<CaseGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(entity: CaseGroupEntity)

    @Query("UPDATE case_groups SET name = :name WHERE id = :groupId")
    suspend fun renameGroup(groupId: String, name: String): Int

    @Query("UPDATE case_groups SET sortOrder = :sortOrder WHERE id = :groupId")
    suspend fun updateGroupSortOrder(groupId: String, sortOrder: Int): Int

    @Query("DELETE FROM case_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String): Int

    @Query(
        "SELECT COUNT(*) FROM case_groups " +
            "WHERE name = :name COLLATE NOCASE AND libraryType = :libraryType",
    )
    suspend fun groupNameCount(name: String, libraryType: String): Int

    @Query(
        "SELECT COALESCE(MAX(sortOrder), -1) FROM case_groups WHERE libraryType = :libraryType",
    )
    suspend fun maxGroupSortOrder(libraryType: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(entities: List<CaseTagEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCaseGroupCrossRefs(entities: List<CaseGroupCrossRefEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCaseTagCrossRefs(entities: List<CaseTagCrossRefEntity>)

    @Query("SELECT * FROM calculation_snapshots WHERE caseId = :caseId ORDER BY sortOrder, id")
    suspend fun calculationSnapshots(caseId: String): List<CalculationSnapshotEntity>

    @Query("SELECT * FROM text_records WHERE caseId = :caseId ORDER BY sortOrder, id")
    suspend fun textRecords(caseId: String): List<TextRecordEntity>

    @Query(
        "SELECT * FROM text_record_revisions WHERE caseId = :caseId ORDER BY sortOrder, id",
    )
    suspend fun textRecordRevisions(caseId: String): List<TextRecordRevisionEntity>

    @Query("SELECT * FROM case_events WHERE caseId = :caseId ORDER BY sortOrder, id")
    suspend fun events(caseId: String): List<CaseEventEntity>

    @Query(
        "SELECT * FROM case_event_revisions WHERE caseId = :caseId ORDER BY sortOrder, id",
    )
    suspend fun eventRevisions(caseId: String): List<CaseEventRevisionEntity>

    @Query("SELECT * FROM source_attachments WHERE caseId = :caseId ORDER BY sortOrder, id")
    suspend fun attachments(caseId: String): List<SourceAttachmentEntity>

    @Query("SELECT * FROM field_evidence WHERE caseId = :caseId ORDER BY sortOrder, id")
    suspend fun fieldEvidence(caseId: String): List<FieldEvidenceEntity>

    @Query(
        """
        SELECT case_groups.* FROM case_groups
        INNER JOIN case_group_cross_ref ON case_groups.id = case_group_cross_ref.groupId
        WHERE case_group_cross_ref.caseId = :caseId
        ORDER BY case_groups.name, case_groups.id
        """,
    )
    suspend fun groups(caseId: String): List<CaseGroupEntity>

    @Query(
        """
        SELECT case_tags.* FROM case_tags
        INNER JOIN case_tag_cross_ref ON case_tags.id = case_tag_cross_ref.tagId
        WHERE case_tag_cross_ref.caseId = :caseId
        ORDER BY case_tags.name, case_tags.id
        """,
    )
    suspend fun tags(caseId: String): List<CaseTagEntity>

    @Query("DELETE FROM calculation_snapshots WHERE caseId = :caseId")
    suspend fun deleteCalculationSnapshots(caseId: String)

    @Query("DELETE FROM text_records WHERE caseId = :caseId")
    suspend fun deleteTextRecords(caseId: String)

    @Query("DELETE FROM text_record_revisions WHERE caseId = :caseId")
    suspend fun deleteTextRecordRevisions(caseId: String)

    @Query("DELETE FROM case_events WHERE caseId = :caseId")
    suspend fun deleteEvents(caseId: String)

    @Query("DELETE FROM case_event_revisions WHERE caseId = :caseId")
    suspend fun deleteEventRevisions(caseId: String)

    @Query("DELETE FROM field_evidence WHERE caseId = :caseId")
    suspend fun deleteFieldEvidence(caseId: String)

    @Query("DELETE FROM source_attachments WHERE caseId = :caseId")
    suspend fun deleteAttachments(caseId: String)

    @Query("DELETE FROM case_group_cross_ref WHERE caseId = :caseId")
    suspend fun deleteCaseGroupCrossRefs(caseId: String)

    @Query("DELETE FROM case_tag_cross_ref WHERE caseId = :caseId")
    suspend fun deleteCaseTagCrossRefs(caseId: String)

    @Query("SELECT * FROM cases ORDER BY id")
    suspend fun allCases(): List<CaseEntity>

    @Query("SELECT * FROM calculation_snapshots ORDER BY caseId, sortOrder, id")
    suspend fun allCalculationSnapshots(): List<CalculationSnapshotEntity>

    @Query("SELECT * FROM calculation_snapshots WHERE adopted = 1 ORDER BY caseId, sortOrder, id")
    suspend fun adoptedCalculationSnapshots(): List<CalculationSnapshotEntity>

    @Query("SELECT * FROM text_records ORDER BY caseId, sortOrder, id")
    suspend fun allTextRecords(): List<TextRecordEntity>

    @Query("SELECT * FROM text_record_revisions ORDER BY caseId, sortOrder, id")
    suspend fun allTextRecordRevisions(): List<TextRecordRevisionEntity>

    @Query("SELECT * FROM case_events ORDER BY caseId, sortOrder, id")
    suspend fun allEvents(): List<CaseEventEntity>

    @Query("SELECT * FROM case_event_revisions ORDER BY caseId, sortOrder, id")
    suspend fun allEventRevisions(): List<CaseEventRevisionEntity>

    @Query("SELECT * FROM source_attachments ORDER BY caseId, sortOrder, id")
    suspend fun allAttachments(): List<SourceAttachmentEntity>

    @Query("SELECT * FROM field_evidence ORDER BY caseId, sortOrder, id")
    suspend fun allFieldEvidence(): List<FieldEvidenceEntity>

    @Query("SELECT * FROM case_groups ORDER BY sortOrder, id")
    suspend fun allGroups(): List<CaseGroupEntity>

    @Query(
        "SELECT * FROM case_groups WHERE libraryType = :libraryType ORDER BY sortOrder, id",
    )
    suspend fun groupsByLibrary(libraryType: String): List<CaseGroupEntity>

    @Query(
        """
        UPDATE cases SET isPinned = :pinned, updatedAtEpochMillis = :updatedAtEpochMillis,
            revision = revision + 1
        WHERE id IN (:caseIds) AND deletedAtEpochMillis IS NULL
        """,
    )
    suspend fun setCasesPinned(
        caseIds: List<String>,
        pinned: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE cases SET deletedAtEpochMillis = :deletedAtEpochMillis,
            updatedAtEpochMillis = :deletedAtEpochMillis, revision = revision + 1
        WHERE id IN (:caseIds) AND deletedAtEpochMillis IS NULL
        """,
    )
    suspend fun moveCasesToTrash(caseIds: List<String>, deletedAtEpochMillis: Long): Int

    @Query("SELECT * FROM case_tags ORDER BY id")
    suspend fun allTags(): List<CaseTagEntity>

    @Query("SELECT * FROM case_group_cross_ref ORDER BY caseId, groupId")
    suspend fun allCaseGroupCrossRefs(): List<CaseGroupCrossRefEntity>

    @Query("SELECT * FROM case_tag_cross_ref ORDER BY caseId, tagId")
    suspend fun allCaseTagCrossRefs(): List<CaseTagCrossRefEntity>
}
