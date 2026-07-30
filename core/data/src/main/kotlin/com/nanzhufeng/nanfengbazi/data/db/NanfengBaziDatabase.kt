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
        CaseEventEntity::class,
        SourceAttachmentEntity::class,
        FieldEvidenceEntity::class,
        CaseGroupEntity::class,
        CaseTagEntity::class,
        CaseGroupCrossRefEntity::class,
        CaseTagCrossRefEntity::class,
    ],
    version = NanfengBaziDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class NanfengBaziDatabase : RoomDatabase() {
    internal abstract fun caseDao(): CaseDao

    companion object {
        const val SCHEMA_VERSION = 2
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
}
