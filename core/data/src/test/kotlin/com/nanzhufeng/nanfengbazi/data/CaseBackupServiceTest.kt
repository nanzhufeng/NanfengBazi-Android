package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.backup.BackupExportResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupEncryption
import com.nanzhufeng.nanfengbazi.data.backup.BackupEncryptionHeader
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreAction
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreDecision
import com.nanzhufeng.nanfengbazi.data.backup.BackupFileManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupProtection
import com.nanzhufeng.nanfengbazi.data.backup.BackupPreviewResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlanResult
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupService
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
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
                val destinationService = CaseBackupService(
                    destinationDatabase,
                    fixedClock,
                )
                val preview = destinationService.preview(
                    input = ByteArrayInputStream(firstBytes),
                    workRoot = root.resolve("preview-work"),
                ) as BackupPreviewResult.Success
                assertTrue(
                    destinationService.prepareRestorePlan(
                        preview.preview,
                        listOf(
                            BackupCaseRestoreDecision(
                                sourceCaseId = "case-1",
                                action = BackupCaseRestoreAction.IMPORT_AS_IS,
                            ),
                        ),
                    ) is BackupRestorePlanResult.Success,
                )
                val restoreResult = destinationService.restoreIntoEmptyStore(
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
            val casePreview = result.preview.cases.single()
            assertEquals(
                RoomCaseRepository(database).findById("case-1"),
                casePreview.sourceCase,
            )
            assertEquals("case-1", casePreview.sourceCaseId)
            assertEquals("脱敏案例一", casePreview.sourceAlias)
            val conflict = casePreview.conflicts.single()
            assertEquals("case-1", conflict.localCaseId)
            assertEquals(1L, conflict.localRevision)
            assertEquals(
                setOf(
                    BackupCaseConflictReason.STABLE_ID_EXISTS,
                    BackupCaseConflictReason.SAME_BIRTH_INPUT,
                    BackupCaseConflictReason.SAME_FOUR_PILLARS,
                ),
                conflict.reasons,
            )
            assertEquals(before, database.caseDao().allCases())
            assertEquals(
                "DECISIONS_INCOMPLETE",
                (service.prepareRestorePlan(result.preview, emptyList()) as
                    BackupRestorePlanResult.Rejected).code,
            )
            assertEquals(
                "DUPLICATE_DECISION",
                (service.prepareRestorePlan(
                    result.preview,
                    listOf(
                        BackupCaseRestoreDecision("case-1", BackupCaseRestoreAction.SKIP),
                        BackupCaseRestoreDecision("case-1", BackupCaseRestoreAction.SKIP),
                    ),
                ) as BackupRestorePlanResult.Rejected).code,
            )
            assertEquals(
                "IMPORT_AS_IS_CONFLICT",
                (service.prepareRestorePlan(
                    result.preview,
                    listOf(
                        BackupCaseRestoreDecision(
                            sourceCaseId = "case-1",
                            action = BackupCaseRestoreAction.IMPORT_AS_IS,
                        ),
                    ),
                ) as BackupRestorePlanResult.Rejected).code,
            )
            assertEquals(
                "EMPTY_MERGE_SCOPE",
                (service.prepareRestorePlan(
                    result.preview,
                    listOf(
                        BackupCaseRestoreDecision(
                            sourceCaseId = "case-1",
                            action = BackupCaseRestoreAction.MERGE,
                            targetCaseId = "case-1",
                        ),
                    ),
                ) as BackupRestorePlanResult.Rejected).code,
            )
            assertTrue(
                service.prepareRestorePlan(
                    result.preview,
                    listOf(
                        BackupCaseRestoreDecision(
                            sourceCaseId = "case-1",
                            action = BackupCaseRestoreAction.MERGE,
                            targetCaseId = "case-1",
                            modules = setOf(SingleCaseMergeModule.TEXT_RECORDS),
                        ),
                    ),
                ) is BackupRestorePlanResult.Success,
            )
            val skipDecision = listOf(
                BackupCaseRestoreDecision(
                    sourceCaseId = "case-1",
                    action = BackupCaseRestoreAction.SKIP,
                ),
            )
            assertTrue(
                service.prepareRestorePlan(result.preview, skipDecision) is
                    BackupRestorePlanResult.Success,
            )
            val current = RoomCaseRepository(database).findById("case-1")!!
            RoomCaseRepository(database).save(
                current.copy(alias = "预览后变化"),
                expectedRevision = current.revision,
            )
            assertEquals(
                "PREVIEW_STALE",
                (service.prepareRestorePlan(result.preview, skipDecision) as
                    BackupRestorePlanResult.Rejected).code,
            )
        }
    }

    @Test
    fun `完整备份预览识别不同稳定ID下的出生输入与四柱冲突`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-conflicts-")
        val attachmentBytes = "脱敏冲突附件".encodeToByteArray()
        val sourceAttachments = root.resolve("source-attachments")
        val sourceFile = sourceAttachments.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val backupBytes = withDatabase { sourceDatabase ->
            RoomCaseRepository(sourceDatabase).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also { output ->
                assertTrue(
                    CaseBackupService(sourceDatabase, fixedClock).export(
                        output = output,
                        attachmentRoot = sourceAttachments,
                        appVersion = "0.3.0-test",
                        protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                    ) is BackupExportResult.Success,
                )
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val localCase = sampleCase(attachmentBytes).copy(
                id = "local-case-2",
                alias = "本地同盘候选",
            )
            RoomCaseRepository(destinationDatabase).save(localCase, null)
            val before = destinationDatabase.caseDao().allCases()

            val result = CaseBackupService(destinationDatabase, fixedClock).preview(
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("preview-work"),
            )

            assertTrue(result is BackupPreviewResult.Success)
            val conflict = (result as BackupPreviewResult.Success)
                .preview
                .cases
                .single()
                .conflicts
                .single()
            assertEquals("local-case-2", conflict.localCaseId)
            assertEquals("本地同盘候选", conflict.localAlias)
            assertEquals(
                setOf(
                    BackupCaseConflictReason.SAME_BIRTH_INPUT,
                    BackupCaseConflictReason.SAME_FOUR_PILLARS,
                ),
                conflict.reasons,
            )
            assertEquals(before, destinationDatabase.caseDao().allCases())
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
    fun `完整备份密码容器不含明文且仅正确密码可预览`() = runTest {
        val output = ByteArrayOutputStream()
        withDatabase { database ->
            val attachmentBytes = "脱敏截图夹具".encodeToByteArray()
            val attachmentRoot = Files.createTempDirectory("nanfeng-encrypted-attachments-")
            val attachmentFile = attachmentRoot.resolve("case-1/source/screen.png")
            Files.createDirectories(attachmentFile.parent)
            Files.write(attachmentFile, attachmentBytes)
            RoomCaseRepository(database).save(sampleCase(attachmentBytes), null)
            val password = "完整备份测试密码123".toCharArray()
            val service = CaseBackupService(
                database = database,
                clock = fixedClock,
                passwordKdfIterations = 100_000,
            )
            val result = service.export(
                output,
                attachmentRoot,
                "0.2.0-test",
                BackupProtection.PasswordProtected(password),
            )

            assertTrue(result is BackupExportResult.Success)
            val encrypted = output.toByteArray()
            assertTrue(
                encrypted.copyOfRange(0, BackupEncryption.MAGIC_BYTES.size)
                    .contentEquals(BackupEncryption.MAGIC_BYTES),
            )
            assertFalse(encrypted.decodeToString().contains("脱敏案例一"))
            assertEquals(
                "PASSWORD_REQUIRED",
                (service.preview(
                    ByteArrayInputStream(encrypted),
                    Files.createTempDirectory("nanfeng-encrypted-preview-required-"),
                ) as BackupPreviewResult.Rejected).code,
            )
            assertEquals(
                "DECRYPTION_FAILED",
                (service.preview(
                    ByteArrayInputStream(encrypted),
                    Files.createTempDirectory("nanfeng-encrypted-preview-wrong-"),
                    "错误密码".toCharArray(),
                ) as BackupPreviewResult.Rejected).code,
            )
            val correct = service.preview(
                ByteArrayInputStream(encrypted),
                Files.createTempDirectory("nanfeng-encrypted-preview-correct-"),
                password,
            )
            assertTrue(correct is BackupPreviewResult.Success)
            correct as BackupPreviewResult.Success
            assertTrue(correct.preview.manifest.encrypted)
            assertEquals(1, correct.preview.manifest.encryptionParametersVersion)
            assertEquals(1, correct.preview.manifest.counts.cases)

            val tampered = encrypted.copyOf().also {
                it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
            }
            assertEquals(
                "DECRYPTION_FAILED",
                (service.preview(
                    ByteArrayInputStream(tampered),
                    Files.createTempDirectory("nanfeng-encrypted-preview-tampered-"),
                    password,
                ) as BackupPreviewResult.Rejected).code,
            )
            val downgraded = rewriteEncryptionHeader(encrypted) {
                it.copy(kdfIterations = 1)
            }
            assertEquals(
                "DECRYPTION_FAILED",
                (service.preview(
                    ByteArrayInputStream(downgraded),
                    Files.createTempDirectory("nanfeng-encrypted-preview-downgraded-"),
                    password,
                ) as BackupPreviewResult.Rejected).code,
            )
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

    private fun rewriteEncryptionHeader(
        bytes: ByteArray,
        transform: (BackupEncryptionHeader) -> BackupEncryptionHeader,
    ): ByteArray {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val magic = ByteArray(BackupEncryption.MAGIC_BYTES.size)
        input.readFully(magic)
        check(magic.contentEquals(BackupEncryption.MAGIC_BYTES))
        val oldHeader = ByteArray(input.readInt())
        input.readFully(oldHeader)
        val header = DomainJson.decodeFromString<BackupEncryptionHeader>(
            oldHeader.decodeToString(),
        )
        val newHeader = DomainJson.encodeToString(
            BackupEncryptionHeader.serializer(),
            transform(header),
        ).encodeToByteArray()
        return ByteArrayOutputStream().also { output ->
            DataOutputStream(output).use { data ->
                data.write(magic)
                data.writeInt(newHeader.size)
                data.write(newHeader)
                input.copyTo(data)
            }
        }.toByteArray()
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
