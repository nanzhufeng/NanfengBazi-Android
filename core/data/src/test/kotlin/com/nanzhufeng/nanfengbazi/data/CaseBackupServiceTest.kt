package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.backup.BackupExportResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupFileManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupProtection
import com.nanzhufeng.nanfengbazi.data.backup.BackupPreviewResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreResult
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupService
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaseBackupServiceTest {
    private val fixedClock = Clock.fixed(FixtureInstant, ZoneOffset.UTC)

    @Test
    fun `完整备份恢复保持命例和附件字节一致`() = runTest {
        val attachmentBytes = "脱敏截图夹具".encodeToByteArray()
        val root = Files.createTempDirectory("nanfeng-backup-roundtrip-")
        val sourceAttachments = root.resolve("source-attachments")
        val sourceFile = sourceAttachments.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)

        withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            val base = sampleCase(attachmentBytes)
            val analysis = base.textRecords.last().copy(
                type = CaseTextRecordType.ANALYSIS,
                analysisCategory = AnalysisCategory.RELATIONSHIP,
            )
            val sourceCase = base.copy(
                textRecords = base.textRecords.dropLast(1) + analysis,
                textRecordRevisions = listOf(
                    CaseTextRecordRevision(
                        id = "record-history-1",
                        recordId = analysis.id,
                        version = 1,
                        changeType = RecordChangeType.CREATED,
                        snapshot = analysis,
                        changedAt = FixtureInstant,
                    ),
                ),
                eventRevisions = listOf(
                    CaseEventRevision(
                        id = "event-history-1",
                        eventId = base.events.single().id,
                        version = 1,
                        changeType = RecordChangeType.CREATED,
                        snapshot = base.events.single(),
                        changedAt = FixtureInstant,
                    ),
                ),
            )
            sourceRepository.save(sourceCase, null)
            val firstBytes = ByteArrayOutputStream().also {
                val result = CaseBackupService(sourceDatabase, fixedClock).export(
                    output = it,
                    attachmentRoot = sourceAttachments,
                    appVersion = "0.2.0-test",
                    protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
                assertTrue(result is BackupExportResult.Success)
            }.toByteArray()

            withDatabase { destinationDatabase ->
                val destinationAttachments = root.resolve("destination-attachments")
                val restoreResult = CaseBackupService(
                    destinationDatabase,
                    fixedClock,
                ).restoreIntoEmptyStore(
                    input = ByteArrayInputStream(firstBytes),
                    workRoot = root.resolve("work"),
                    attachmentRoot = destinationAttachments,
                )

                assertTrue(restoreResult is BackupRestoreResult.Success)
                assertEquals(
                    sourceRepository.findById("case-1"),
                    RoomCaseRepository(destinationDatabase).findById("case-1"),
                )
                assertArrayEquals(
                    attachmentBytes,
                    Files.readAllBytes(destinationAttachments.resolve("case-1/source/screen.png")),
                )

                val secondBytes = ByteArrayOutputStream().also {
                    val exportResult = CaseBackupService(
                        destinationDatabase,
                        fixedClock,
                    ).export(
                        output = it,
                        attachmentRoot = destinationAttachments,
                        appVersion = "0.2.0-test",
                        protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                    )
                    assertTrue(exportResult is BackupExportResult.Success)
                }.toByteArray()
                assertArrayEquals(firstBytes, secondBytes)
            }
        }
    }

    @Test
    fun `完整备份预览校验附件与清单但不写数据库`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-preview-")
        val attachmentBytes = "脱敏预览附件".encodeToByteArray()
        val attachmentRoot = root.resolve("attachments")
        val attachmentFile = attachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(attachmentFile.parent)
        Files.write(attachmentFile, attachmentBytes)

        withDatabase { database ->
            RoomCaseRepository(database).save(sampleCase(attachmentBytes), null)
            val service = CaseBackupService(database, fixedClock)
            val bytes = ByteArrayOutputStream().also { output ->
                assertTrue(
                    service.export(
                        output = output,
                        attachmentRoot = attachmentRoot,
                        appVersion = "0.3.0-test",
                        protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                    ) is BackupExportResult.Success,
                )
            }.toByteArray()
            val before = database.caseDao().allCases()

            val result = service.preview(
                input = ByteArrayInputStream(bytes),
                workRoot = root.resolve("work"),
            )

            assertTrue(result is BackupPreviewResult.Success)
            result as BackupPreviewResult.Success
            assertEquals(1, result.preview.manifest.counts.cases)
            assertEquals(1, result.preview.manifest.counts.attachments)
            assertEquals(11, result.preview.sourceFileCount)
            assertEquals(before, database.caseDao().allCases())
        }
    }

    @Test
    fun `文件被篡改时拒绝恢复且数据库保持为空`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-tamper-")
        val sourceAttachments = root.resolve("source")
        val attachmentBytes = "脱敏截图夹具".encodeToByteArray()
        val file = sourceAttachments.resolve("case-1/source/screen.png")
        Files.createDirectories(file.parent)
        Files.write(file, attachmentBytes)

        val backupBytes = withDatabase { database ->
            RoomCaseRepository(database).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also {
                CaseBackupService(database, fixedClock).export(
                    it,
                    sourceAttachments,
                    "0.2.0-test",
                    BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }
        val tampered = tamperCasesJson(backupBytes)

        withDatabase { destination ->
            val service = CaseBackupService(destination, fixedClock)
            val preview = service.preview(
                ByteArrayInputStream(tampered),
                root.resolve("preview-work"),
            )
            assertTrue(preview is BackupPreviewResult.Rejected)
            assertEquals(
                "FILE_HASH_MISMATCH",
                (preview as BackupPreviewResult.Rejected).code,
            )
            assertNull(RoomCaseRepository(destination).findById("case-1"))

            val result = service.restoreIntoEmptyStore(
                ByteArrayInputStream(tampered),
                root.resolve("work"),
                root.resolve("destination"),
            )

            assertTrue(result is BackupRestoreResult.Rejected)
            assertEquals("FILE_HASH_MISMATCH", (result as BackupRestoreResult.Rejected).code)
            assertNull(RoomCaseRepository(destination).findById("case-1"))
            assertFalse(Files.exists(root.resolve("destination")))
        }
    }

    @Test
    fun `路径穿越压缩包在写文件前被拒绝`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-slip-")
        val malicious = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("../escape.txt"))
                zip.write("不应写出".encodeToByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        withDatabase { database ->
            val result = CaseBackupService(database, fixedClock).restoreIntoEmptyStore(
                ByteArrayInputStream(malicious),
                root.resolve("work"),
                root.resolve("attachments"),
            )

            assertTrue(result is BackupRestoreResult.Rejected)
            assertEquals("INVALID_ZIP", (result as BackupRestoreResult.Rejected).code)
            assertFalse(Files.exists(root.resolve("escape.txt")))
        }
    }

    @Test
    fun `已有命例时拒绝无范围覆盖恢复`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-nonempty-")
        withDatabase { database ->
            RoomCaseRepository(database).save(sampleCase(), null)

            val result = CaseBackupService(database, fixedClock).restoreIntoEmptyStore(
                ByteArrayInputStream(ByteArray(0)),
                root.resolve("work"),
                root.resolve("attachments"),
            )

            assertTrue(result is BackupRestoreResult.Rejected)
            assertEquals("DESTINATION_NOT_EMPTY", (result as BackupRestoreResult.Rejected).code)
            assertNotNull(RoomCaseRepository(database).findById("case-1"))
        }
    }

    @Test
    fun `附件提交失败时数据库写入会回滚`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-rollback-")
        val sourceAttachments = root.resolve("source")
        val attachmentBytes = "脱敏截图夹具".encodeToByteArray()
        val sourceFile = sourceAttachments.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val backup = withDatabase { source ->
            RoomCaseRepository(source).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also {
                CaseBackupService(source, fixedClock).export(
                    it,
                    sourceAttachments,
                    "0.2.0-test",
                    BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }
        val blockedParent = root.resolve("blocked-parent")
        Files.write(blockedParent, "这是文件，不是目录".encodeToByteArray())

        withDatabase { destination ->
            val result = CaseBackupService(destination, fixedClock).restoreIntoEmptyStore(
                ByteArrayInputStream(backup),
                root.resolve("work"),
                blockedParent.resolve("attachments"),
            )

            assertTrue(result is BackupRestoreResult.Rejected)
            assertEquals(
                "RESTORE_FAILED_ROLLED_BACK",
                (result as BackupRestoreResult.Rejected).code,
            )
            assertNull(RoomCaseRepository(destination).findById("case-1"))
        }
    }

    @Test
    fun `密码加密尚未实现时不得降级导出明文`() = runTest {
        val output = ByteArrayOutputStream()
        withDatabase { database ->
            val result = CaseBackupService(database, fixedClock).export(
                output,
                Files.createTempDirectory("nanfeng-empty-attachments-"),
                "0.2.0-test",
                BackupProtection.PasswordProtected("test-only".toCharArray()),
            )

            assertTrue(result is BackupExportResult.Rejected)
            assertEquals(
                "PASSWORD_ENCRYPTION_NOT_IMPLEMENTED",
                (result as BackupExportResult.Rejected).code,
            )
            assertEquals(0, output.size())
        }
    }

    @Test
    fun `Schema v2 备份缺少管理字段时仍可恢复到v4默认值`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-v2-")
        val sourceAttachments = root.resolve("source")
        val attachmentBytes = "脱敏截图夹具".encodeToByteArray()
        val sourceFile = sourceAttachments.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val currentBackup = withDatabase { source ->
            RoomCaseRepository(source).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also {
                CaseBackupService(source, fixedClock).export(
                    it,
                    sourceAttachments,
                    "0.2.0-test",
                    BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }
        val schemaTwoBackup = downgradeToSchemaTwo(currentBackup)

        withDatabase { destination ->
            val result = CaseBackupService(destination, fixedClock).restoreIntoEmptyStore(
                ByteArrayInputStream(schemaTwoBackup),
                root.resolve("work"),
                root.resolve("destination"),
            )

            assertTrue(result is BackupRestoreResult.Success)
            val restored = RoomCaseRepository(destination).findById("case-1")
            assertNotNull(restored)
            assertFalse(restored!!.isFavorite)
            assertFalse(restored.isPinned)
            assertNull(restored.lastViewedAt)
            assertNull(restored.copiedFromCaseId)
            assertNull(restored.deletedAt)
        }
    }

    private fun newDatabase(): NanfengBaziDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(
            context,
            NanfengBaziDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    private suspend fun <T> withDatabase(
        block: suspend (NanfengBaziDatabase) -> T,
    ): T {
        val database = newDatabase()
        return try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun tamperCasesJson(source: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(source)).use { input ->
            ZipOutputStream(output).use { zip ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val bytes = input.readBytes()
                    zip.putNextEntry(ZipEntry(entry.name).apply { time = 0L })
                    zip.write(
                        if (entry.name == "cases.json") {
                            bytes + "\n".encodeToByteArray()
                        } else {
                            bytes
                        },
                    )
                    zip.closeEntry()
                    input.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }

    private fun downgradeToSchemaTwo(source: ByteArray): ByteArray {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(source)).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                entries[entry.name] = input.readBytes()
                input.closeEntry()
            }
        }
        val currentCases = entries.getValue("cases.json").decodeToString()
        val schemaTwoCases = currentCases.replace(
            ",\"isFavorite\":false,\"isPinned\":false,\"copiedFromCaseId\":null," +
                "\"lastViewedAtEpochMillis\":null,\"deletedAtEpochMillis\":null",
            "",
        ).encodeToByteArray()
        check(!schemaTwoCases.decodeToString().contains("\"isFavorite\"")) {
            "测试夹具没有成功移除 Schema v3 管理字段"
        }
        entries["cases.json"] = schemaTwoCases
        val manifest = DomainJson.decodeFromString<BackupManifest>(
            entries.getValue("manifest.json").decodeToString(),
        )
        entries["manifest.json"] = DomainJson.encodeToString(
            BackupManifest.serializer(),
            manifest.copy(
                databaseSchemaVersion = 2,
                files = manifest.files.map { file ->
                    if (file.path == "cases.json") {
                        BackupFileManifest(
                            path = file.path,
                            byteSize = schemaTwoCases.size.toLong(),
                            sha256 = sha256(schemaTwoCases),
                        )
                    } else {
                        file
                    }
                },
            ),
        ).encodeToByteArray()
        return ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name).apply { time = 0L })
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }
}
