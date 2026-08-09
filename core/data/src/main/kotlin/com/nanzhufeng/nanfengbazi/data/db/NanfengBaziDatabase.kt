package com.nanzhufeng.nanfengbazi.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CaseEntity::class,
        CalculationSnapshotEntity::class,
        TextRecordEntity::class,
        TextRecordRevisionEntity::class,
        CaseEventEntity::class,
        CaseEventRevisionEntity::class,
        SourceAttachmentEntity::class,
        FieldEvidenceEntity::class,
        CaseGroupEntity::class,
        CaseTagEntity::class,
        CaseGroupCrossRefEntity::class,
        CaseTagCrossRefEntity::class,
        ImportSessionEntity::class,
    ],
    version = NanfengBaziDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class NanfengBaziDatabase : RoomDatabase() {
    internal abstract fun caseDao(): CaseDao
    internal abstract fun importSessionDao(): ImportSessionDao

    companion object {
        const val SCHEMA_VERSION = 9
    }
}

object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MANUAL'",
            )
            db.execSQL(
                "ALTER TABLE calculation_snapshots ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE text_records ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE case_events ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE source_attachments ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE field_evidence ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN lastViewedAtEpochMillis INTEGER",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_cases_lastViewedAtEpochMillis " +
                    "ON cases(lastViewedAtEpochMillis)",
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN copiedFromCaseId TEXT",
            )
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN deletedAtEpochMillis INTEGER",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_cases_deletedAtEpochMillis " +
                    "ON cases(deletedAtEpochMillis)",
            )
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE text_records ADD COLUMN analysisCategory TEXT",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS text_record_revisions (
                    id TEXT NOT NULL,
                    caseId TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    changeType TEXT NOT NULL,
                    revisionJson TEXT NOT NULL,
                    changedAtEpochMillis INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(caseId) REFERENCES cases(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_text_record_revisions_caseId " +
                    "ON text_record_revisions(caseId)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_text_record_revisions_caseId_recordId_version " +
                    "ON text_record_revisions(caseId, recordId, version)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS case_event_revisions (
                    id TEXT NOT NULL,
                    caseId TEXT NOT NULL,
                    eventId TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    changeType TEXT NOT NULL,
                    revisionJson TEXT NOT NULL,
                    changedAtEpochMillis INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(caseId) REFERENCES cases(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_case_event_revisions_caseId " +
                    "ON case_event_revisions(caseId)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_case_event_revisions_caseId_eventId_version " +
                    "ON case_event_revisions(caseId, eventId, version)",
            )
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE cases ADD COLUMN birthTimeCandidatesJson " +
                    "TEXT NOT NULL DEFAULT '[]'",
            )
        }
    }

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS import_sessions (
                    id TEXT NOT NULL,
                    status TEXT NOT NULL,
                    sessionJson TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_import_sessions_status " +
                    "ON import_sessions(status)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_import_sessions_updatedAtEpochMillis " +
                    "ON import_sessions(updatedAtEpochMillis)",
            )
        }
    }

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE text_records ADD COLUMN " +
                    "sourceType TEXT NOT NULL DEFAULT 'USER'",
            )
            db.execSQL(
                "UPDATE text_records SET sourceType = 'IMPORTED_IMAGE' " +
                    "WHERE sourceAttachmentId IS NOT NULL",
            )
        }
    }

    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE case_groups ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "UPDATE case_groups SET sortOrder = " +
                    "(SELECT COUNT(*) FROM case_groups AS earlier " +
                    "WHERE earlier.name < case_groups.name OR " +
                    "(earlier.name = case_groups.name AND earlier.id < case_groups.id))",
            )
        }
    }
}
