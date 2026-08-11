package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.backup.BackupExportResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupEncryption
import com.nanzhufeng.nanfengbazi.data.backup.BackupEncryptionHeader
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseMergePreparationResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreAction
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreDecision
import com.nanzhufeng.nanfengbazi.data.backup.BackupDatabasePreflight
import com.nanzhufeng.nanfengbazi.data.backup.BackupFileManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupProtection
import com.nanzhufeng.nanfengbazi.data.backup.BackupPreviewResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreExecutionResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreRecoveryResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlanResult
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupService
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaseBackupServiceTest {
    private val fixedClock = Clock.fixed(FixtureInstant, ZoneOffset.UTC)

    @Test
    fun `云端结构化快照排除设备本地最近查看时间`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            repository.save(sampleCase(), expectedRevision = null)
            repository.markViewed("case-1", FixtureInstant.plusSeconds(90))

            val output = ByteArrayOutputStream()
            val result = CaseBackupService(database, fixedClock).exportCloudSnapshot(
                output = output,
                appVersion = "0.3.0-test",
            )

            assertTrue(result is BackupExportResult.Success)
            val casesJson = ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
                generateSequence { zip.nextEntry }.first { it.name == "cases.json" }
                zip.readBytes().decodeToString()
            }
            assertTrue(casesJson.contains("\"lastViewedAtEpochMillis\":null"))
            assertFalse(casesJson.contains(FixtureInstant.plusSeconds(90).toEpochMilli().toString()))
        }
    }

    @Test
    fun `云端结构化快照不包含来源附件或字段证据`() = runTest {
        val root = Files.createTempDirectory("nanfeng-cloud-snapshot-")
        val attachments = root.resolve("attachments")
        val attachment = attachments.resolve("case-1/source/screen.png")
        Files.createDirectories(attachment.parent)
        Files.write(attachment, "脱敏截图夹具".encodeToByteArray())
        try {
            withDatabase { database ->
                RoomCaseRepository(database).save(sampleCase("脱敏截图夹具".encodeToByteArray()), null)
                val output = ByteArrayOutputStream()
                val result = CaseBackupService(database, fixedClock).exportCloudSnapshot(
                    output = output,
                    appVersion = "0.3.0-test",
                )
                assertTrue(result is BackupExportResult.Success)
                val entries = mutableListOf<String>()
                ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries += entry.name
                    }
                }
                assertFalse(entries.any { it.startsWith("attachments/") })
                assertTrue(entries.contains("imports.json"))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `大型断事笔记云端快照可生成并恢复`() = runTest {
        val root = Files.createTempDirectory("nanfeng-cloud-large-notes-")
        try {
            val noteContent = "x".repeat(384 * 1024)
            val records = (1..48).map { index ->
                sampleCase().textRecords.first().copy(
                    id = "large-note-$index",
                    content = noteContent,
                    sourceAttachmentId = null,
                )
            }
            val sourceCase = sampleCase().copy(
                textRecords = records,
                events = emptyList(),
                attachments = emptyList(),
                fieldEvidence = emptyList(),
            )
            val snapshot = withDatabase { source ->
                RoomCaseRepository(source).save(sourceCase, expectedRevision = null)
                ByteArrayOutputStream().also { output ->
                    assertTrue(
                        CaseBackupService(source, fixedClock).exportCloudSnapshot(
                            output = output,
                            appVersion = "0.3.0-test",
                        ) is BackupExportResult.Success,
                    )
                }.toByteArray()
            }
            val notesEntryBytes = ZipInputStream(ByteArrayInputStream(snapshot)).use { zip ->
                generateSequence { zip.nextEntry }.first { it.name == "notes.json" }
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                }
                total
            }
            assertTrue(notesEntryBytes > 16L * 1024 * 1024)

            withDatabase { destination ->
                val restore = CaseBackupService(destination, fixedClock)
                    .restoreCloudSnapshotIntoEmptyStore(
                        input = ByteArrayInputStream(snapshot),
                        workRoot = root.resolve("work"),
                        attachmentRoot = root.resolve("attachments"),
                    )
                assertTrue(restore is BackupRestoreResult.Success)
                assertEquals(records.size, RoomCaseRepository(destination).findById("case-1")!!.textRecords.size)
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `完整备份预览先经过独立临时数据库完整往返`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-preflight-")
        val backup = ByteArrayOutputStream()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceAttachments = root.resolve("source-attachments")
        val sourceFile = sourceAttachments.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, "脱敏截图夹具".encodeToByteArray())
        try {
            withDatabase { source ->
                val repository = RoomCaseRepository(source)
                repository.save(sampleCase(), expectedRevision = null)
                assertTrue(
                    CaseBackupService(source, fixedClock).export(
                        backup,
                        sourceAttachments,
                        "0.3.0-test",
                        BackupProtection.UnencryptedSensitiveDataConfirmed,
                    ) is BackupExportResult.Success,
                )
            }

            withDatabase { destination ->
                val result = CaseBackupService(
                    database = destination,
                    clock = fixedClock,
                    stagingDatabaseContext = context,
                ).preview(
                    ByteArrayInputStream(backup.toByteArray()),
                    root.resolve("work"),
                )

                assertTrue(result.toString(), result is BackupPreviewResult.Success)
                assertEquals(
                    BackupDatabasePreflight.INDEPENDENT_ROOM_ROUND_TRIP_VERIFIED,
                    (result as BackupPreviewResult.Success).preview.databasePreflight,
                )
                assertNull(RoomCaseRepository(destination).findById("case-1"))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

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
            assertEquals(
                "EMPTY_SELECTED_MODULE",
                (service.prepareRestorePlan(
                    result.preview,
                    listOf(
                        BackupCaseRestoreDecision(
                            sourceCaseId = "case-1",
                            action = BackupCaseRestoreAction.MERGE,
                            targetCaseId = "case-1",
                            modules = setOf(SingleCaseMergeModule.TEXT_RECORDS),
                        ),
                    ),
                ) as BackupRestorePlanResult.Rejected).code,
            )
            assertEquals(
                "INVALID_FIELD_CHOICE",
                (service.prepareRestorePlan(
                    result.preview,
                    listOf(
                        BackupCaseRestoreDecision(
                            sourceCaseId = "case-1",
                            action = BackupCaseRestoreAction.MERGE,
                            targetCaseId = "case-1",
                            fieldChoices = mapOf(
                                SingleCaseFieldKey.ALIAS to
                                    SingleCaseValueChoice.IMPORTED,
                            ),
                        ),
                    ),
                ) as BackupRestorePlanResult.Rejected).code,
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
            val mergePreparation = CaseBackupService(
                destinationDatabase,
                fixedClock,
            ).prepareCaseMerge(
                preview = result.preview,
                sourceCaseId = "case-1",
                targetCaseId = "local-case-2",
            )
            assertTrue(mergePreparation is BackupCaseMergePreparationResult.Success)
            mergePreparation as BackupCaseMergePreparationResult.Success
            assertEquals(
                listOf(SingleCaseFieldKey.ALIAS),
                mergePreparation.preparation.analysis.fieldDifferences.map { it.key },
            )
            assertEquals(
                0,
                mergePreparation.preparation.analysis.addableCounts.textRecords,
            )
            assertEquals(before, destinationDatabase.caseDao().allCases())
        }
    }

    @Test
    fun `非空库保留两份恢复重映射身份并提交真实附件`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-execute-")
        val attachmentBytes = "脱敏非空库恢复附件".encodeToByteArray()
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val backupBytes = withDatabase { sourceDatabase ->
            RoomCaseRepository(sourceDatabase).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also { output ->
                assertTrue(
                    CaseBackupService(sourceDatabase, fixedClock).export(
                        output = output,
                        attachmentRoot = sourceAttachmentRoot,
                        appVersion = "0.3.0-execute-test",
                        protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                    ) is BackupExportResult.Success,
                )
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val destinationAttachmentRoot = root.resolve("destination-attachments")
            val localCase = sampleCase(attachmentBytes).copy(
                id = "local-case",
                alias = "本地冲突命例",
            )
            val localFile = destinationAttachmentRoot.resolve(
                localCase.attachments.single().relativePath,
            )
            Files.createDirectories(localFile.parent)
            Files.write(localFile, attachmentBytes)
            val repository = RoomCaseRepository(destinationDatabase)
            repository.save(localCase, null)
            val localBefore = repository.findById("local-case")
            val service = CaseBackupService(destinationDatabase, fixedClock)
            val preview = service.preview(
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("preview-work"),
            ) as BackupPreviewResult.Success
            val plan = service.prepareRestorePlan(
                preview.preview,
                listOf(
                    BackupCaseRestoreDecision(
                        sourceCaseId = "case-1",
                        action = BackupCaseRestoreAction.KEEP_BOTH,
                    ),
                ),
            ) as BackupRestorePlanResult.Success

            val execution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("execute-work"),
                attachmentRoot = destinationAttachmentRoot,
            )

            assertTrue(execution is BackupRestoreExecutionResult.Success)
            execution as BackupRestoreExecutionResult.Success
            assertEquals(1, execution.summary.keptBothCases)
            assertEquals(1, execution.summary.restoredAttachments)
            assertEquals(localBefore, repository.findById("local-case"))
            val restoredId = destinationDatabase.caseDao()
                .allCases()
                .map { it.id }
                .single { it != "local-case" }
            val restored = repository.findById(restoredId)!!
            assertEquals("case-1", restored.copiedFromCaseId)
            assertEquals(1L, restored.revision)
            assertNotEquals("attachment-1", restored.attachments.single().id)
            assertTrue(restored.attachments.single().relativePath.startsWith("restored/"))
            val restoredFile = destinationAttachmentRoot.resolve(
                restored.attachments.single().relativePath,
            )
            assertArrayEquals(attachmentBytes, Files.readAllBytes(restoredFile))
            assertArrayEquals(attachmentBytes, Files.readAllBytes(localFile))
        }
    }

    @Test
    fun `非空库范围合并复用共享语义且不创建来源命例`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-execute-merge-")
        val sourceCase = sampleCase().copy(
            alias = "来源范围合并命例",
            attachments = emptyList(),
            fieldEvidence = emptyList(),
            textRecords = sampleCase().textRecords.map {
                it.copy(sourceAttachmentId = null)
            },
            events = sampleCase().events.map {
                it.copy(sourceAttachmentId = null)
            },
        )
        val backupBytes = withDatabase { sourceDatabase ->
            RoomCaseRepository(sourceDatabase).save(sourceCase, null)
            ByteArrayOutputStream().also { output ->
                CaseBackupService(sourceDatabase, fixedClock).export(
                    output = output,
                    attachmentRoot = root.resolve("source-attachments"),
                    appVersion = "0.3.0-merge-test",
                    protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val localCase = sourceCase.copy(
                id = "local-merge-target",
                alias = "本地范围合并目标",
                textRecords = emptyList(),
                events = emptyList(),
                groups = emptyList(),
                tags = emptyList(),
            )
            val repository = RoomCaseRepository(destinationDatabase)
            repository.save(localCase, null)
            val service = CaseBackupService(destinationDatabase, fixedClock)
            val preview = service.preview(
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("preview-work"),
            ) as BackupPreviewResult.Success
            val plan = service.prepareRestorePlan(
                preview.preview,
                listOf(
                    BackupCaseRestoreDecision(
                        sourceCaseId = "case-1",
                        action = BackupCaseRestoreAction.MERGE,
                        targetCaseId = "local-merge-target",
                        modules = setOf(
                            SingleCaseMergeModule.TEXT_RECORDS,
                            SingleCaseMergeModule.EVENTS,
                            SingleCaseMergeModule.ORGANIZATION,
                        ),
                        fieldChoices = mapOf(
                            SingleCaseFieldKey.ALIAS to SingleCaseValueChoice.IMPORTED,
                        ),
                    ),
                ),
            ) as BackupRestorePlanResult.Success

            val execution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("execute-work"),
                attachmentRoot = root.resolve("destination-attachments"),
            )

            assertTrue(execution is BackupRestoreExecutionResult.Success)
            execution as BackupRestoreExecutionResult.Success
            assertEquals(1, execution.summary.mergedCases)
            assertEquals(0, execution.summary.restoredAttachments)
            assertNull(repository.findById("case-1"))
            val merged = repository.findById("local-merge-target")!!
            assertEquals("来源范围合并命例", merged.alias)
            assertEquals(sourceCase.textRecords.map { it.content }, merged.textRecords.map { it.content })
            assertEquals(sourceCase.events.map { it.rawText }, merged.events.map { it.rawText })
            assertEquals(sourceCase.groups.map { it.name }, merged.groups.map { it.name })
            assertEquals(sourceCase.tags.map { it.name }, merged.tags.map { it.name })
        }
    }

    @Test
    fun `范围合并只复制新增记录事件共同引用的来源附件`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-merge-attachments-")
        val sourceBytes = "脱敏来源合并附件".encodeToByteArray()
        val localBytes = "脱敏本地既有附件".encodeToByteArray()
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceBytes)
        val sourceCase = sampleCase(sourceBytes).copy(alias = "来源附件合并命例")
        val backupBytes = withDatabase { sourceDatabase ->
            RoomCaseRepository(sourceDatabase).save(sourceCase, null)
            ByteArrayOutputStream().also { output ->
                CaseBackupService(sourceDatabase, fixedClock).export(
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-attachment-merge-test",
                    protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val destinationAttachmentRoot = root.resolve("destination-attachments")
            val localAttachmentPath = "local-case/source/existing.png"
            val localFile = destinationAttachmentRoot.resolve(localAttachmentPath)
            Files.createDirectories(localFile.parent)
            Files.write(localFile, localBytes)
            val localBase = sampleCase(localBytes)
            val localCase = localBase.copy(
                id = "local-attachment-target",
                alias = "本地附件合并目标",
                textRecords = emptyList(),
                textRecordRevisions = emptyList(),
                events = emptyList(),
                eventRevisions = emptyList(),
                attachments = localBase.attachments.map {
                    it.copy(relativePath = localAttachmentPath)
                },
                groups = emptyList(),
                tags = emptyList(),
            )
            val repository = RoomCaseRepository(destinationDatabase)
            repository.save(localCase, null)
            val service = CaseBackupService(destinationDatabase, fixedClock)
            val preview = service.preview(
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("preview-work"),
            ) as BackupPreviewResult.Success
            val mergePreparation = service.prepareCaseMerge(
                preview = preview.preview,
                sourceCaseId = "case-1",
                targetCaseId = "local-attachment-target",
            ) as BackupCaseMergePreparationResult.Success
            assertEquals(1, mergePreparation.preparation.analysis.addableCounts.attachmentReferences)
            val plan = service.prepareRestorePlan(
                preview.preview,
                listOf(
                    BackupCaseRestoreDecision(
                        sourceCaseId = "case-1",
                        action = BackupCaseRestoreAction.MERGE,
                        targetCaseId = "local-attachment-target",
                        modules = setOf(
                            SingleCaseMergeModule.TEXT_RECORDS,
                            SingleCaseMergeModule.EVENTS,
                        ),
                    ),
                ),
            ) as BackupRestorePlanResult.Success

            destinationDatabase.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_attachment_merge
                BEFORE UPDATE ON cases
                WHEN OLD.id = 'local-attachment-target'
                BEGIN
                    SELECT RAISE(ABORT, 'forced attachment merge failure');
                END
                """.trimIndent(),
            )
            val failedExecution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("failed-execute-work"),
                attachmentRoot = destinationAttachmentRoot,
            )
            assertEquals(
                "RESTORE_EXECUTION_FAILED_ROLLED_BACK",
                (failedExecution as BackupRestoreExecutionResult.Rejected).code,
            )
            assertEquals(
                1,
                repository.findById("local-attachment-target")!!.attachments.size,
            )
            val restoredRootAfterFailure = destinationAttachmentRoot.resolve("restored")
            assertTrue(
                !Files.exists(restoredRootAfterFailure) ||
                    Files.list(restoredRootAfterFailure).use { it.findAny().isEmpty },
            )
            assertArrayEquals(localBytes, Files.readAllBytes(localFile))
            destinationDatabase.openHelper.writableDatabase.execSQL(
                "DROP TRIGGER reject_attachment_merge",
            )

            val execution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("execute-work"),
                attachmentRoot = destinationAttachmentRoot,
            ) as BackupRestoreExecutionResult.Success

            assertEquals(1, execution.summary.mergedCases)
            assertEquals(1, execution.summary.restoredAttachments)
            val merged = repository.findById("local-attachment-target")!!
            assertEquals(2, merged.attachments.size)
            val importedAttachment = merged.attachments.single {
                it.relativePath.startsWith("restored/")
            }
            assertNotEquals("attachment-1", importedAttachment.id)
            assertEquals(
                setOf(importedAttachment.id),
                (
                    merged.textRecords.mapNotNull { it.sourceAttachmentId } +
                        merged.events.mapNotNull { it.sourceAttachmentId }
                    ).toSet(),
            )
            assertEquals(
                "attachment-1",
                merged.fieldEvidence.single().attachmentId,
            )
            assertArrayEquals(
                sourceBytes,
                Files.readAllBytes(
                    destinationAttachmentRoot.resolve(importedAttachment.relativePath),
                ),
            )
            assertArrayEquals(localBytes, Files.readAllBytes(localFile))
        }
    }

    @Test
    fun `密码完整备份提交重新认证同一文件并恢复附件`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-execute-encrypted-")
        val attachmentBytes = "脱敏密码恢复附件".encodeToByteArray()
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val password = "Encrypted-Restore-123".toCharArray()
        val backupBytes = withDatabase { sourceDatabase ->
            RoomCaseRepository(sourceDatabase).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also { output ->
                assertTrue(
                    CaseBackupService(
                        database = sourceDatabase,
                        clock = fixedClock,
                        passwordKdfIterations = 100_000,
                    ).export(
                        output = output,
                        attachmentRoot = sourceAttachmentRoot,
                        appVersion = "0.3.0-encrypted-restore-test",
                        protection = BackupProtection.PasswordProtected(password),
                    ) is BackupExportResult.Success,
                )
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val service = CaseBackupService(
                database = destinationDatabase,
                clock = fixedClock,
                passwordKdfIterations = 100_000,
            )
            val preview = service.preview(
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("preview-work"),
                password = password,
            ) as BackupPreviewResult.Success
            val plan = service.prepareRestorePlan(
                preview.preview,
                listOf(
                    BackupCaseRestoreDecision(
                        sourceCaseId = "case-1",
                        action = BackupCaseRestoreAction.IMPORT_AS_IS,
                    ),
                ),
            ) as BackupRestorePlanResult.Success
            val wrongPassword = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("wrong-password-work"),
                attachmentRoot = root.resolve("destination-attachments"),
                password = "Wrong-Restore-Password".toCharArray(),
            )
            assertEquals(
                "DECRYPTION_FAILED",
                (wrongPassword as BackupRestoreExecutionResult.Rejected).code,
            )
            assertNull(RoomCaseRepository(destinationDatabase).findById("case-1"))

            val execution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("execute-work"),
                attachmentRoot = root.resolve("destination-attachments"),
                password = password,
            )

            assertTrue(execution is BackupRestoreExecutionResult.Success)
            val restored = RoomCaseRepository(destinationDatabase).findById("case-1")!!
            val restoredFile = root.resolve("destination-attachments")
                .resolve(restored.attachments.single().relativePath)
            assertArrayEquals(attachmentBytes, Files.readAllBytes(restoredFile))
        }
    }

    @Test
    fun `提交时换成另一份有效备份会拒绝且保持零写入`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-source-changed-")
        val sourceAttachmentRoot = root.resolve("source-attachments")
        Files.createDirectories(sourceAttachmentRoot)
        val backups = withDatabase { sourceDatabase ->
            val repository = RoomCaseRepository(sourceDatabase)
            repository.save(
                sampleCase().copy(
                    textRecords = emptyList(),
                    textRecordRevisions = emptyList(),
                    events = emptyList(),
                    eventRevisions = emptyList(),
                    attachments = emptyList(),
                    fieldEvidence = emptyList(),
                ),
                null,
            )
            val first = ByteArrayOutputStream().also { output ->
                CaseBackupService(sourceDatabase, fixedClock).export(
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-source-a",
                    protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
            val current = repository.findById("case-1")!!
            repository.save(
                current.copy(alias = "另一份有效备份"),
                expectedRevision = current.revision,
            )
            val second = ByteArrayOutputStream().also { output ->
                CaseBackupService(sourceDatabase, fixedClock).export(
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-source-b",
                    protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
            first to second
        }

        withDatabase { destinationDatabase ->
            val service = CaseBackupService(destinationDatabase, fixedClock)
            val preview = service.preview(
                input = ByteArrayInputStream(backups.first),
                workRoot = root.resolve("preview-work"),
            ) as BackupPreviewResult.Success
            val plan = service.prepareRestorePlan(
                preview.preview,
                listOf(
                    BackupCaseRestoreDecision(
                        sourceCaseId = "case-1",
                        action = BackupCaseRestoreAction.IMPORT_AS_IS,
                    ),
                ),
            ) as BackupRestorePlanResult.Success

            val execution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backups.second),
                workRoot = root.resolve("execute-work"),
                attachmentRoot = root.resolve("destination-attachments"),
            )

            assertEquals(
                "SOURCE_CHANGED_AFTER_PREVIEW",
                (execution as BackupRestoreExecutionResult.Rejected).code,
            )
            assertNull(RoomCaseRepository(destinationDatabase).findById("case-1"))
        }
    }

    @Test
    fun `非空库恢复数据库失败时移除本次附件并保留既有事实`() = runTest {
        val root = Files.createTempDirectory("nanfeng-backup-execute-rollback-")
        val attachmentBytes = "脱敏回滚附件".encodeToByteArray()
        val sourceAttachmentRoot = root.resolve("source-attachments")
        val sourceFile = sourceAttachmentRoot.resolve("case-1/source/screen.png")
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, attachmentBytes)
        val backupBytes = withDatabase { sourceDatabase ->
            RoomCaseRepository(sourceDatabase).save(sampleCase(attachmentBytes), null)
            ByteArrayOutputStream().also { output ->
                CaseBackupService(sourceDatabase, fixedClock).export(
                    output = output,
                    attachmentRoot = sourceAttachmentRoot,
                    appVersion = "0.3.0-rollback-test",
                    protection = BackupProtection.UnencryptedSensitiveDataConfirmed,
                )
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val destinationAttachmentRoot = root.resolve("destination-attachments")
            val localCase = sampleCase(attachmentBytes).copy(
                id = "local-case",
                alias = "本地保留事实",
            )
            val localFile = destinationAttachmentRoot.resolve(
                localCase.attachments.single().relativePath,
            )
            Files.createDirectories(localFile.parent)
            Files.write(localFile, attachmentBytes)
            val repository = RoomCaseRepository(destinationDatabase)
            repository.save(localCase, null)
            val localBefore = repository.findById("local-case")
            val service = CaseBackupService(destinationDatabase, fixedClock)
            val preview = service.preview(
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("preview-work"),
            ) as BackupPreviewResult.Success
            val plan = service.prepareRestorePlan(
                preview.preview,
                listOf(
                    BackupCaseRestoreDecision(
                        sourceCaseId = "case-1",
                        action = BackupCaseRestoreAction.KEEP_BOTH,
                    ),
                ),
            ) as BackupRestorePlanResult.Success
            destinationDatabase.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_restored_case
                BEFORE INSERT ON cases
                WHEN NEW.id != 'local-case'
                BEGIN
                    SELECT RAISE(ABORT, 'forced restore failure');
                END
                """.trimIndent(),
            )

            val execution = service.executeRestorePlan(
                plan = plan.plan,
                input = ByteArrayInputStream(backupBytes),
                workRoot = root.resolve("execute-work"),
                attachmentRoot = destinationAttachmentRoot,
            )

            assertTrue(execution is BackupRestoreExecutionResult.Rejected)
            assertEquals(
                "RESTORE_EXECUTION_FAILED_ROLLED_BACK",
                (execution as BackupRestoreExecutionResult.Rejected).code,
            )
            assertEquals(localBefore, repository.findById("local-case"))
            assertEquals(listOf("local-case"), destinationDatabase.caseDao().allCases().map { it.id })
            val restoredRoot = destinationAttachmentRoot.resolve("restored")
            assertTrue(
                !Files.exists(restoredRoot) ||
                    Files.list(restoredRoot).use { it.findAny().isEmpty },
            )
            assertArrayEquals(attachmentBytes, Files.readAllBytes(localFile))
        }
    }

    @Test
    fun `进程中断日志在数据库未提交时清理孤儿附件`() = runTest {
        val root = Files.createTempDirectory("nanfeng-restore-recovery-rollback-")
        val transactionId = "11111111-1111-4111-8111-111111111111"
        val finalRoot = root.resolve("restored/$transactionId")
        Files.createDirectories(finalRoot)
        Files.write(finalRoot.resolve("orphan"), "orphan".encodeToByteArray())
        val journalRoot = root.resolve(".restore-journal")
        Files.createDirectories(journalRoot)
        val journal = """
            {
              "formatVersion": 1,
              "transactionId": "$transactionId",
              "state": "FILES_MOVED",
              "finalDirectory": "restored/$transactionId",
              "expectedCases": [
                {
                  "caseId": "missing-case",
                  "payloadSha256": "${"0".repeat(64)}"
                }
              ]
            }
        """.trimIndent()
        Files.write(
            journalRoot.resolve("$transactionId.json"),
            journal.encodeToByteArray(),
        )

        withDatabase { database ->
            val result = CaseBackupService(database, fixedClock)
                .recoverInterruptedRestores(root)
            assertEquals(
                BackupRestoreRecoveryResult.Success(
                    rolledBackTransactions = 1,
                    finalizedTransactions = 0,
                ),
                result,
            )
        }
        assertFalse(Files.exists(finalRoot))
        assertFalse(Files.exists(journalRoot.resolve("$transactionId.json")))
    }

    @Test
    fun `范围合并中断时目标仍是提交前载荷会清理孤儿附件`() = runTest {
        val root = Files.createTempDirectory("nanfeng-restore-merge-rollback-")
        val transactionId = "33333333-3333-4333-8333-333333333333"
        val finalRoot = root.resolve("restored/$transactionId")
        Files.createDirectories(finalRoot)
        Files.write(finalRoot.resolve("orphan"), "orphan".encodeToByteArray())
        val journalRoot = root.resolve(".restore-journal")
        Files.createDirectories(journalRoot)

        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val previousCase = sampleCase().copy(
                id = "existing-merge-target",
                textRecords = emptyList(),
                textRecordRevisions = emptyList(),
                events = emptyList(),
                eventRevisions = emptyList(),
                attachments = emptyList(),
                fieldEvidence = emptyList(),
            )
            repository.save(previousCase, null)
            val persisted = repository.findById("existing-merge-target")!!
            val previousPayloadSha256 = sha256(
                DomainJson.encodeToString(
                    BaziCase.serializer(),
                    persisted,
                ).encodeToByteArray(),
            )
            val journal = """
                {
                  "formatVersion": 1,
                  "transactionId": "$transactionId",
                  "state": "FILES_MOVED",
                  "finalDirectory": "restored/$transactionId",
                  "expectedCases": [
                    {
                      "caseId": "existing-merge-target",
                      "payloadSha256": "${"0".repeat(64)}",
                      "previousPayloadSha256": "$previousPayloadSha256"
                    }
                  ]
                }
            """.trimIndent()
            val journalFile = journalRoot.resolve("$transactionId.json")
            Files.write(journalFile, journal.encodeToByteArray())

            val result = CaseBackupService(database, fixedClock)
                .recoverInterruptedRestores(root)

            assertEquals(
                BackupRestoreRecoveryResult.Success(
                    rolledBackTransactions = 1,
                    finalizedTransactions = 0,
                ),
                result,
            )
            assertEquals(persisted, repository.findById("existing-merge-target"))
            assertFalse(Files.exists(journalFile))
        }
        assertFalse(Files.exists(finalRoot))
    }

    @Test
    fun `进程中断日志在数据库已提交且附件完整时只完成收尾`() = runTest {
        val root = Files.createTempDirectory("nanfeng-restore-recovery-finalize-")
        val transactionId = "22222222-2222-4222-8222-222222222222"
        val attachmentBytes = "脱敏已提交恢复附件".encodeToByteArray()
        val relativePath = "restored/$transactionId/restored-attachment"
        val finalFile = root.resolve(relativePath)
        Files.createDirectories(finalFile.parent)
        Files.write(finalFile, attachmentBytes)
        val journalRoot = root.resolve(".restore-journal")
        Files.createDirectories(journalRoot)

        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val committedCase = sampleCase(attachmentBytes).copy(
                id = "restored-case",
                attachments = sampleCase(attachmentBytes).attachments.map {
                    it.copy(
                        id = "restored-attachment",
                        relativePath = relativePath,
                    )
                },
                textRecords = sampleCase(attachmentBytes).textRecords.map {
                    it.copy(sourceAttachmentId = "restored-attachment")
                },
                events = sampleCase(attachmentBytes).events.map {
                    it.copy(sourceAttachmentId = "restored-attachment")
                },
                fieldEvidence = sampleCase(attachmentBytes).fieldEvidence.map {
                    it.copy(attachmentId = "restored-attachment")
                },
            )
            repository.save(committedCase, null)
            val persisted = repository.findById("restored-case")!!
            val payloadSha256 = sha256(
                DomainJson.encodeToString(
                    BaziCase.serializer(),
                    persisted,
                ).encodeToByteArray(),
            )
            val journal = """
                {
                  "formatVersion": 1,
                  "transactionId": "$transactionId",
                  "state": "FILES_MOVED",
                  "finalDirectory": "restored/$transactionId",
                  "expectedCases": [
                    {
                      "caseId": "restored-case",
                      "payloadSha256": "$payloadSha256"
                    }
                  ]
                }
            """.trimIndent()
            val journalFile = journalRoot.resolve("$transactionId.json")
            Files.write(journalFile, journal.encodeToByteArray())

            val result = CaseBackupService(database, fixedClock)
                .recoverInterruptedRestores(root)

            assertEquals(
                BackupRestoreRecoveryResult.Success(
                    rolledBackTransactions = 0,
                    finalizedTransactions = 1,
                ),
                result,
            )
            assertFalse(Files.exists(journalFile))
        }
        assertArrayEquals(attachmentBytes, Files.readAllBytes(finalFile))
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
