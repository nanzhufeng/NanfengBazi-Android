package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseAttachmentMode
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseBundleService
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocumentProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExchangeService
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportDecision
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePlan
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePreparationResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreviewResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseProtection
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SingleCaseBundleServiceTest {
    private val fixedClock = Clock.fixed(FixtureInstant, ZoneOffset.UTC)

    @Test
    fun `明文命例包携带附件并完成严格零写入预览`() = runTest {
        val attachmentBytes = "脱敏命例包截图".encodeToByteArray()
        val root = Files.createTempDirectory("single-case-bundle-plain-")
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)

        val bundleBytes = withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val caseData = sampleCase(attachmentBytes).copy(alias = "脱敏/命例包")
            repository.save(caseData, null)
            ByteArrayOutputStream().also { output ->
                val result = SingleCaseBundleService(database, fixedClock).export(
                    caseId = caseData.id,
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-test",
                    protection = SingleCaseProtection.UnencryptedSensitiveDataConfirmed,
                )
                assertTrue(result is SingleCaseExportResult.Success)
                result as SingleCaseExportResult.Success
                assertEquals("脱敏_命例包_南枫八字命例包.nfbcase", result.suggestedFileName)
                assertEquals(output.size().toLong(), result.byteSize)
                assertEquals(sha256(output.toByteArray()), result.sha256)
            }.toByteArray()
        }

        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val result = SingleCaseBundleService(database, fixedClock).preview(
                input = ByteArrayInputStream(bundleBytes),
                workRoot = root.resolve("preview-work"),
            )

            assertTrue(result is SingleCasePreviewResult.Success)
            val preview = (result as SingleCasePreviewResult.Success).preview
            assertTrue(preview.containsAttachmentBinaries)
            assertEquals(
                SingleCaseAttachmentMode.BUNDLED_BINARIES,
                preview.document.attachmentMode,
            )
            assertEquals(
                SingleCaseDocumentProtection.UNENCRYPTED,
                preview.protection,
            )
            assertEquals(1, preview.counts.attachmentReferences)
            assertEquals(1, preview.counts.fieldEvidence)
            assertTrue(preview.conflicts.isEmpty())
            assertTrue(repository.search().isEmpty())
        }
        assertArrayEquals(attachmentBytes, Files.readAllBytes(sourceFile))
    }

    @Test
    fun `密码命例包错误密码拒绝且正确密码恢复同一附件清单`() = runTest {
        val attachmentBytes = "脱敏加密命例包截图".encodeToByteArray()
        val password = "correct-password".toCharArray()
        val root = Files.createTempDirectory("single-case-bundle-encrypted-")
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)

        val bundleBytes = withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val caseData = sampleCase(attachmentBytes)
            repository.save(caseData, null)
            ByteArrayOutputStream().also { output ->
                val result = SingleCaseBundleService(database, fixedClock).export(
                    caseId = caseData.id,
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-test",
                    protection = SingleCaseProtection.PasswordProtected(password),
                )
                assertTrue(result is SingleCaseExportResult.Success)
                result as SingleCaseExportResult.Success
                assertEquals(
                    "脱敏案例一_南枫八字命例包_加密.nfbcase",
                    result.suggestedFileName,
                )
                assertEquals(output.size().toLong(), result.byteSize)
                assertEquals(sha256(output.toByteArray()), result.sha256)
            }.toByteArray()
        }

        withDatabase { database ->
            val service = SingleCaseBundleService(database, fixedClock)
            val wrong = service.preview(
                ByteArrayInputStream(bundleBytes),
                root.resolve("wrong-password-work"),
                "wrong-password".toCharArray(),
            )
            assertTrue(wrong is SingleCasePreviewResult.Rejected)
            assertEquals(
                "DECRYPTION_FAILED",
                (wrong as SingleCasePreviewResult.Rejected).code,
            )

            val result = service.preview(
                ByteArrayInputStream(bundleBytes),
                root.resolve("correct-password-work"),
                password,
            )
            assertTrue(result is SingleCasePreviewResult.Success)
            val preview = (result as SingleCasePreviewResult.Success).preview
            assertTrue(preview.containsAttachmentBinaries)
            assertEquals(
                SingleCaseDocumentProtection.PASSWORD_PROTECTED,
                preview.protection,
            )
            assertFalse(preview.document.caseData.attachments.isEmpty())
        }
    }

    @Test
    fun `命例包保留两份重建全部附件引用并写入可读字节`() = runTest {
        val attachmentBytes = "脱敏命例包提交截图".encodeToByteArray()
        val root = Files.createTempDirectory("single-case-bundle-import-")
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val bundleBytes = withDatabase { database ->
            val repository = RoomCaseRepository(database)
            repository.save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also { output ->
                SingleCaseBundleService(database, fixedClock).export(
                    caseId = "case-1",
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-import-test",
                    protection = SingleCaseProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }

        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val service = SingleCaseBundleService(database, fixedClock)
            val preview = service.preview(
                ByteArrayInputStream(bundleBytes),
                root.resolve("preview-work"),
            ) as SingleCasePreviewResult.Success
            val destinationAttachmentRoot = root.resolve("destination-attachments")

            val result = service.commitImport(
                preview = preview.preview,
                decision = SingleCaseImportDecision.KEEP_BOTH,
                input = ByteArrayInputStream(bundleBytes),
                workRoot = root.resolve("commit-work"),
                attachmentRoot = destinationAttachmentRoot,
            )

            assertTrue(result is SingleCaseImportResult.Imported)
            result as SingleCaseImportResult.Imported
            val imported = repository.findById(result.caseId)
            assertNotNull(imported)
            imported!!
            assertNotEquals("case-1", imported.id)
            assertEquals("case-1", imported.copiedFromCaseId)
            assertEquals(1, imported.attachments.size)
            val restoredAttachment = imported.attachments.single()
            assertNotEquals("attachment-1", restoredAttachment.id)
            assertTrue(
                imported.textRecords.all {
                    it.sourceAttachmentId == restoredAttachment.id
                },
            )
            assertEquals(
                restoredAttachment.id,
                imported.events.single().sourceAttachmentId,
            )
            assertEquals(
                restoredAttachment.id,
                imported.fieldEvidence.single().attachmentId,
            )
            assertArrayEquals(
                attachmentBytes,
                Files.readAllBytes(
                    destinationAttachmentRoot.resolve(restoredAttachment.relativePath),
                ),
            )
            assertFalse(
                Files.exists(
                    destinationAttachmentRoot.resolve(".restore-journal"),
                ) && Files.list(
                    destinationAttachmentRoot.resolve(".restore-journal"),
                ).use { it.findAny().isPresent },
            )
        }
    }

    @Test
    fun `命例包附件合并跨记录事件去重复制且数据库失败会清理本事务`() = runTest {
        val attachmentBytes = "脱敏命例包合并截图".encodeToByteArray()
        val root = Files.createTempDirectory("single-case-bundle-merge-")
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val bundleBytes = withDatabase { database ->
            val repository = RoomCaseRepository(database)
            repository.save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also { output ->
                SingleCaseBundleService(database, fixedClock).export(
                    caseId = "case-1",
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-merge-test",
                    protection = SingleCaseProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }

        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val localBase = sampleCase(attachmentBytes)
            val localCase = localBase.copy(
                id = "local-target",
                alias = "本地合并目标",
                textRecords = emptyList(),
                textRecordRevisions = emptyList(),
                events = emptyList(),
                eventRevisions = emptyList(),
                attachments = emptyList(),
                fieldEvidence = emptyList(),
                groups = emptyList(),
                tags = emptyList(),
            )
            repository.save(localCase, null)
            val service = SingleCaseBundleService(database, fixedClock)
            val preview = service.preview(
                ByteArrayInputStream(bundleBytes),
                root.resolve("preview-work"),
            ) as SingleCasePreviewResult.Success
            val preparation = SingleCaseExchangeService(repository, fixedClock)
                .prepareMerge(preview.preview, localCase.id)
            assertTrue(preparation is SingleCaseMergePreparationResult.Success)
            preparation as SingleCaseMergePreparationResult.Success
            val plan = SingleCaseMergePlan(
                preparation = preparation.preparation,
                modules = setOf(
                    SingleCaseMergeModule.TEXT_RECORDS,
                    SingleCaseMergeModule.EVENTS,
                ),
            )
            val destinationAttachmentRoot = root.resolve("destination-attachments")
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_bundle_merge
                BEFORE UPDATE ON cases
                WHEN OLD.id = 'local-target'
                BEGIN
                    SELECT RAISE(ABORT, 'forced bundle merge failure');
                END
                """.trimIndent(),
            )

            val failed = service.commitMerge(
                plan = plan,
                input = ByteArrayInputStream(bundleBytes),
                workRoot = root.resolve("failed-work"),
                attachmentRoot = destinationAttachmentRoot,
            )
            assertTrue(failed is SingleCaseImportResult.Rejected)
            assertEquals(
                "MERGE_FAILED_ROLLED_BACK",
                (failed as SingleCaseImportResult.Rejected).code,
            )
            assertTrue(repository.findById(localCase.id)!!.attachments.isEmpty())
            val restoredRoot = destinationAttachmentRoot.resolve("restored")
            assertTrue(
                !Files.exists(restoredRoot) ||
                    Files.list(restoredRoot).use { it.findAny().isEmpty },
            )
            database.openHelper.writableDatabase.execSQL(
                "DROP TRIGGER reject_bundle_merge",
            )

            val mergedResult = service.commitMerge(
                plan = plan,
                input = ByteArrayInputStream(bundleBytes),
                workRoot = root.resolve("success-work"),
                attachmentRoot = destinationAttachmentRoot,
            )
            assertTrue(mergedResult is SingleCaseImportResult.Merged)
            val merged = repository.findById(localCase.id)
            assertNotNull(merged)
            merged!!
            assertEquals(1, merged.attachments.size)
            assertEquals(2, merged.textRecords.size)
            assertEquals(1, merged.events.size)
            val restoredAttachment = merged.attachments.single()
            assertTrue(
                merged.textRecords.all {
                    it.sourceAttachmentId == restoredAttachment.id
                },
            )
            assertEquals(
                restoredAttachment.id,
                merged.events.single().sourceAttachmentId,
            )
            assertArrayEquals(
                attachmentBytes,
                Files.readAllBytes(
                    destinationAttachmentRoot.resolve(restoredAttachment.relativePath),
                ),
            )
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
}
