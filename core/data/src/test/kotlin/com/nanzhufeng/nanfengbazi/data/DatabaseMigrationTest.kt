package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.DatabaseMigrations
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {
    @Test
    fun `v1 命例迁移到 v2 时补充来源类型且保留原值`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersionOneSchema(db)
                        db.execSQL(
                            """
                            INSERT INTO cases (
                                id, alias, nameState, nameValue,
                                sexForFortuneDirection, birthInputJson, profileJson,
                                createdAtEpochMillis, updatedAtEpochMillis, revision
                            ) VALUES (
                                'legacy-case', '旧版脱敏案例', 'PRESENT', '测试乙',
                                'MAN', '{}', '{}', 1, 2, 3
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use {
            it.writableDatabase
        }

        val migrated = Room.databaseBuilder(
            context,
            NanfengBaziDatabase::class.java,
            name,
        ).addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            migrated.openHelper.readableDatabase.query(
                "SELECT alias, sourceType, revision FROM cases WHERE id = 'legacy-case'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("旧版脱敏案例", cursor.getString(0))
                assertEquals("MANUAL", cursor.getString(1))
                assertEquals(3L, cursor.getLong(2))
            }
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    private fun createVersionOneSchema(db: SupportSQLiteDatabase) {
        VERSION_ONE_SQL.forEach(db::execSQL)
    }

    companion object {
        private val VERSION_ONE_SQL = listOf(
            """
            CREATE TABLE IF NOT EXISTS `cases` (
                `id` TEXT NOT NULL,
                `alias` TEXT NOT NULL,
                `nameState` TEXT NOT NULL,
                `nameValue` TEXT,
                `sexForFortuneDirection` TEXT NOT NULL,
                `birthInputJson` TEXT NOT NULL,
                `profileJson` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_cases_alias` ON `cases` (`alias`)",
            "CREATE INDEX IF NOT EXISTS `index_cases_nameValue` ON `cases` (`nameValue`)",
            "CREATE INDEX IF NOT EXISTS `index_cases_updatedAtEpochMillis` ON `cases` (`updatedAtEpochMillis`)",
            """
            CREATE TABLE IF NOT EXISTS `calculation_snapshots` (
                `id` TEXT NOT NULL,
                `caseId` TEXT NOT NULL,
                `resultJson` TEXT NOT NULL,
                `adopted` INTEGER NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_calculation_snapshots_caseId` ON `calculation_snapshots` (`caseId`)",
            """
            CREATE TABLE IF NOT EXISTS `text_records` (
                `id` TEXT NOT NULL,
                `caseId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `sourceAttachmentId` TEXT,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_text_records_caseId` ON `text_records` (`caseId`)",
            "CREATE INDEX IF NOT EXISTS `index_text_records_sourceAttachmentId` ON `text_records` (`sourceAttachmentId`)",
            """
            CREATE TABLE IF NOT EXISTS `case_events` (
                `id` TEXT NOT NULL,
                `caseId` TEXT NOT NULL,
                `eventJson` TEXT NOT NULL,
                `sourceAttachmentId` TEXT,
                `createdAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_case_events_caseId` ON `case_events` (`caseId`)",
            "CREATE INDEX IF NOT EXISTS `index_case_events_sourceAttachmentId` ON `case_events` (`sourceAttachmentId`)",
            """
            CREATE TABLE IF NOT EXISTS `source_attachments` (
                `id` TEXT NOT NULL,
                `caseId` TEXT NOT NULL,
                `relativePath` TEXT NOT NULL,
                `originalFileName` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `sha256` TEXT NOT NULL,
                `byteSize` INTEGER NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_source_attachments_caseId` ON `source_attachments` (`caseId`)",
            "CREATE INDEX IF NOT EXISTS `index_source_attachments_sha256` ON `source_attachments` (`sha256`)",
            """
            CREATE TABLE IF NOT EXISTS `field_evidence` (
                `id` TEXT NOT NULL,
                `caseId` TEXT NOT NULL,
                `attachmentId` TEXT NOT NULL,
                `fieldKey` TEXT NOT NULL,
                `evidenceJson` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`attachmentId`) REFERENCES `source_attachments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_field_evidence_caseId` ON `field_evidence` (`caseId`)",
            "CREATE INDEX IF NOT EXISTS `index_field_evidence_attachmentId` ON `field_evidence` (`attachmentId`)",
            "CREATE TABLE IF NOT EXISTS `case_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `case_tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))",
            """
            CREATE TABLE IF NOT EXISTS `case_group_cross_ref` (
                `caseId` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                PRIMARY KEY(`caseId`, `groupId`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`groupId`) REFERENCES `case_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_case_group_cross_ref_groupId` ON `case_group_cross_ref` (`groupId`)",
            """
            CREATE TABLE IF NOT EXISTS `case_tag_cross_ref` (
                `caseId` TEXT NOT NULL,
                `tagId` TEXT NOT NULL,
                PRIMARY KEY(`caseId`, `tagId`),
                FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `case_tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_case_tag_cross_ref_tagId` ON `case_tag_cross_ref` (`tagId`)",
        )
    }
}
