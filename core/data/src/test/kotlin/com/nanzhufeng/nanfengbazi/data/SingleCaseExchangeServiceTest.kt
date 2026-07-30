package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseAttachmentMode
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocument
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocumentProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseEncryptedDocument
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExchangeService
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportDecision
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePlan
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePreparationResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreviewResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SingleCaseExchangeServiceTest {
    private val fixedClock = Clock.fixed(FixtureInstant, ZoneOffset.UTC)

    @Test
    fun `单命例导出与空库预览保持完整聚合且零写入`() = runTest {
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            val sourceCase = sampleCase().copy(alias = "脱敏/案例:一")
            sourceRepository.save(sourceCase, null)

            ByteArrayOutputStream().also { output ->
                val result = SingleCaseExchangeService(
                    sourceRepository,
                    fixedClock,
                ).export(
                    caseId = sourceCase.id,
                    output = output,
                    appVersion = "0.3.0-test",
                    protection = SingleCaseProtection.UnencryptedSensitiveDataConfirmed,
                )

                assertTrue(result is SingleCaseExportResult.Success)
                result as SingleCaseExportResult.Success
                assertEquals("脱敏_案例_一_南枫八字命例.json", result.suggestedFileName)
                assertEquals(output.size().toLong(), result.byteSize)
                assertTrue(result.sha256.matches(Regex("[0-9a-f]{64}")))
            }.toByteArray()
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val result = SingleCaseExchangeService(
                destinationRepository,
                fixedClock,
            ).preview(ByteArrayInputStream(bytes))

            assertTrue(result is SingleCasePreviewResult.Success)
            val preview = (result as SingleCasePreviewResult.Success).preview
            assertEquals(
                sampleCase().copy(alias = "脱敏/案例:一", revision = 1),
                preview.document.caseData,
            )
            assertEquals(1, preview.counts.calculationSnapshots)
            assertEquals(2, preview.counts.textRecords)
            assertEquals(1, preview.counts.events)
            assertEquals(1, preview.counts.attachmentReferences)
            assertTrue(preview.conflicts.isEmpty())
            assertFalse(preview.containsAttachmentBinaries)
            assertEquals(
                SingleCaseAttachmentMode.REFERENCES_ONLY,
                preview.document.attachmentMode,
            )
            assertTrue(destinationRepository.search().isEmpty())
        }
    }

    @Test
    fun `预览合并稳定ID和出生四柱冲突但不修改现有命例`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val existing = sampleCase()
            repository.save(existing, null)
            val bytes = exportBytes(repository, existing.id)

            val result = SingleCaseExchangeService(repository, fixedClock)
                .preview(ByteArrayInputStream(bytes))

            assertTrue(result is SingleCasePreviewResult.Success)
            val conflicts = (result as SingleCasePreviewResult.Success).preview.conflicts
            assertEquals(1, conflicts.size)
            assertEquals(existing.id, conflicts.single().caseId)
            assertEquals(
                setOf(
                    SingleCaseConflictReason.STABLE_ID_EXISTS,
                    SingleCaseConflictReason.SAME_BIRTH_INPUT,
                    SingleCaseConflictReason.SAME_FOUR_PILLARS,
                ),
                conflicts.single().reasons,
            )
            assertEquals(existing.copy(revision = 1), repository.findById(existing.id))
        }
    }

    @Test
    fun `载荷哈希不匹配时预览拒绝`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val source = sampleCase()
            repository.save(source, null)
            val exported = exportBytes(repository, source.id)
            val document = DomainJson.decodeFromString<SingleCaseDocument>(
                exported.decodeToString(),
            )
            val tampered = DomainJson.encodeToString(
                document.copy(payloadSha256 = "0".repeat(64)),
            ).encodeToByteArray()

            val result = SingleCaseExchangeService(repository, fixedClock)
                .preview(ByteArrayInputStream(tampered))

            assertTrue(result is SingleCasePreviewResult.Rejected)
            assertEquals(
                "PAYLOAD_HASH_MISMATCH",
                (result as SingleCasePreviewResult.Rejected).code,
            )
        }
    }

    @Test
    fun `未知单命例格式版本和非法JSON明确拒绝`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val source = sampleCase()
            repository.save(source, null)
            val exported = exportBytes(repository, source.id)
            val document = DomainJson.decodeFromString<SingleCaseDocument>(
                exported.decodeToString(),
            )
            val futureVersion = DomainJson.encodeToString(
                document.copy(formatVersion = 99),
            ).encodeToByteArray()
            val service = SingleCaseExchangeService(repository, fixedClock)

            val futureResult = service.preview(ByteArrayInputStream(futureVersion))
            val invalidResult = service.preview(
                ByteArrayInputStream("不是 JSON".encodeToByteArray()),
            )

            assertEquals(
                "UNSUPPORTED_FORMAT_VERSION",
                (futureResult as SingleCasePreviewResult.Rejected).code,
            )
            assertEquals(
                "INVALID_JSON",
                (invalidResult as SingleCasePreviewResult.Rejected).code,
            )
        }
    }

    @Test
    fun `密码加密文件不含明文且仅正确密码可预览`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val source = sampleCase()
            repository.save(source, null)
            val output = ByteArrayOutputStream()
            val password = "正确密码-Stage3B".toCharArray()
            val service = SingleCaseExchangeService(
                repository = repository,
                clock = fixedClock,
                passwordKdfIterations = 100_000,
            )

            val result = service.export(
                caseId = source.id,
                output = output,
                appVersion = "0.3.0-test",
                protection = SingleCaseProtection.PasswordProtected(password),
            )

            assertTrue(result is SingleCaseExportResult.Success)
            result as SingleCaseExportResult.Success
            assertEquals(
                "脱敏案例一_南枫八字命例_加密.json",
                result.suggestedFileName,
            )
            val encryptedBytes = output.toByteArray()
            assertFalse(encryptedBytes.decodeToString().contains(source.alias))
            assertFalse(encryptedBytes.decodeToString().contains("脱敏师傅点评完整原文"))

            val withoutPassword = service.preview(ByteArrayInputStream(encryptedBytes))
            val wrongPassword = service.preview(
                ByteArrayInputStream(encryptedBytes),
                "错误密码".toCharArray(),
            )
            val correctPassword = service.preview(
                ByteArrayInputStream(encryptedBytes),
                password,
            )

            assertEquals(
                "PASSWORD_REQUIRED",
                (withoutPassword as SingleCasePreviewResult.Rejected).code,
            )
            assertEquals(
                "DECRYPTION_FAILED",
                (wrongPassword as SingleCasePreviewResult.Rejected).code,
            )
            assertEquals(
                SingleCaseDocumentProtection.PASSWORD_PROTECTED,
                (correctPassword as SingleCasePreviewResult.Success).preview.protection,
            )
            assertEquals(source.copy(revision = 1), correctPassword.preview.document.caseData)
        }
    }

    @Test
    fun `加密容器篡改与降级参数统一拒绝且不写入`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val source = sampleCase()
            repository.save(source, null)
            val password = "篡改测试密码".toCharArray()
            val service = SingleCaseExchangeService(
                repository = repository,
                clock = fixedClock,
                passwordKdfIterations = 100_000,
            )
            val output = ByteArrayOutputStream()
            service.export(
                caseId = source.id,
                output = output,
                appVersion = "0.3.0-test",
                protection = SingleCaseProtection.PasswordProtected(password),
            )
            val encrypted = DomainJson.decodeFromString<SingleCaseEncryptedDocument>(
                output.toByteArray().decodeToString(),
            )
            val tamperedCiphertext = encrypted.ciphertextBase64.toCharArray().also {
                val index = it.lastIndex
                it[index] = if (it[index] == 'A') 'B' else 'A'
            }.concatToString()
            val tampered = DomainJson.encodeToString(
                encrypted.copy(ciphertextBase64 = tamperedCiphertext),
            ).encodeToByteArray()
            val downgraded = DomainJson.encodeToString(
                encrypted.copy(kdfIterations = 1),
            ).encodeToByteArray()

            val tamperedResult = service.preview(ByteArrayInputStream(tampered), password)
            val downgradedResult = service.preview(ByteArrayInputStream(downgraded), password)

            assertEquals(
                "DECRYPTION_FAILED",
                (tamperedResult as SingleCasePreviewResult.Rejected).code,
            )
            assertEquals(
                "DECRYPTION_FAILED",
                (downgradedResult as SingleCasePreviewResult.Rejected).code,
            )
            assertEquals(1, repository.search().size)
        }
    }

    @Test
    fun `超出大小上限的单命例文件在解析前拒绝`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val oversized = ByteArray(SingleCaseExchangeService.MAX_DOCUMENT_BYTES + 1)

            val result = SingleCaseExchangeService(repository, fixedClock)
                .preview(ByteArrayInputStream(oversized))

            assertEquals(
                "DOCUMENT_TOO_LARGE",
                (result as SingleCasePreviewResult.Rejected).code,
            )
            assertTrue(repository.search().isEmpty())
        }
    }

    @Test
    fun `保留两份提交会重建聚合身份且不覆盖来源身份`() = runTest {
        val base = attachmentFree(sampleCase())
        val source = base.copy(
            textRecordRevisions = listOf(
                CaseTextRecordRevision(
                    id = "record-revision-1",
                    recordId = base.textRecords.first().id,
                    version = 1,
                    changeType = RecordChangeType.CREATED,
                    snapshot = base.textRecords.first(),
                    changedAt = FixtureInstant,
                ),
            ),
            eventRevisions = listOf(
                CaseEventRevision(
                    id = "event-revision-1",
                    eventId = base.events.first().id,
                    version = 1,
                    changeType = RecordChangeType.CREATED,
                    snapshot = base.events.first(),
                    changedAt = FixtureInstant,
                ),
            ),
        )
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            sourceRepository.save(source, null)
            exportBytes(sourceRepository, source.id)
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val generatedIds = generateSequence(1) { it + 1 }.iterator()
            val service = SingleCaseExchangeService(
                repository = destinationRepository,
                clock = fixedClock,
                idGenerator = { "import-${generatedIds.next()}" },
            )
            val preview = (
                service.preview(ByteArrayInputStream(bytes)) as
                    SingleCasePreviewResult.Success
                ).preview

            val result = service.commitImport(
                preview = preview,
                decision = SingleCaseImportDecision.KEEP_BOTH,
            )

            assertTrue(result is SingleCaseImportResult.Imported)
            val importedId = (result as SingleCaseImportResult.Imported).caseId
            val imported = checkNotNull(destinationRepository.findById(importedId))
            assertEquals(source.id, imported.copiedFromCaseId)
            assertEquals(FixtureInstant, imported.createdAt)
            assertEquals(FixtureInstant, imported.updatedAt)
            assertEquals(1, imported.revision)
            assertTrue(imported.id != source.id)
            assertTrue(
                imported.textRecords.map { it.id }.toSet()
                    .intersect(source.textRecords.map { it.id }.toSet())
                    .isEmpty(),
            )
            assertTrue(
                imported.events.map { it.id }.toSet()
                    .intersect(source.events.map { it.id }.toSet())
                    .isEmpty(),
            )
            assertEquals(
                imported.textRecords.first().id,
                imported.textRecordRevisions.single().recordId,
            )
            assertEquals(
                imported.textRecordRevisions.single().recordId,
                imported.textRecordRevisions.single().snapshot.id,
            )
            assertEquals(
                imported.events.first().id,
                imported.eventRevisions.single().eventId,
            )
            assertEquals(
                imported.eventRevisions.single().eventId,
                imported.eventRevisions.single().snapshot.id,
            )
            assertTrue(
                imported.calculationSnapshots.map { it.id }.toSet()
                    .intersect(source.calculationSnapshots.map { it.id }.toSet())
                    .isEmpty(),
            )
            assertTrue(imported.groups.single().id != source.groups.single().id)
            assertTrue(imported.tags.single().id != source.tags.single().id)
            assertEquals(1, destinationRepository.search().size)
        }
    }

    @Test
    fun `提交前冲突变化会使预览过期且保持零写入`() = runTest {
        val source = attachmentFree(sampleCase())
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            sourceRepository.save(source, null)
            exportBytes(sourceRepository, source.id)
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val service = SingleCaseExchangeService(destinationRepository, fixedClock)
            val preview = (
                service.preview(ByteArrayInputStream(bytes)) as
                    SingleCasePreviewResult.Success
                ).preview
            val concurrentCase = source.copy(
                id = "concurrent-case",
                alias = "预览后新增",
                copiedFromCaseId = null,
                revision = 0,
            )
            destinationRepository.save(concurrentCase, null)

            val result = service.commitImport(
                preview = preview,
                decision = SingleCaseImportDecision.KEEP_BOTH,
            )

            assertEquals(
                "PREVIEW_STALE",
                (result as SingleCaseImportResult.Rejected).code,
            )
            assertEquals(
                listOf("concurrent-case"),
                destinationRepository.search().map { it.id },
            )
        }
    }

    @Test
    fun `引用附件缺少二进制时拒绝提交但允许明确跳过`() = runTest {
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            val source = sampleCase()
            sourceRepository.save(source, null)
            exportBytes(sourceRepository, source.id)
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val service = SingleCaseExchangeService(destinationRepository, fixedClock)
            val preview = (
                service.preview(ByteArrayInputStream(bytes)) as
                    SingleCasePreviewResult.Success
                ).preview

            val rejected = service.commitImport(
                preview = preview,
                decision = SingleCaseImportDecision.KEEP_BOTH,
            )
            val skipped = service.commitImport(
                preview = preview,
                decision = SingleCaseImportDecision.SKIP,
            )

            assertEquals(
                "ATTACHMENT_BINARIES_REQUIRED",
                (rejected as SingleCaseImportResult.Rejected).code,
            )
            assertTrue(skipped is SingleCaseImportResult.Skipped)
            assertTrue(destinationRepository.search().isEmpty())
        }
    }

    @Test
    fun `按模块合并只追加独有内容且逐字段只采用明确来源值`() = runTest {
        val source = attachmentFree(sampleCase()).copy(alias = "来源别名")
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            sourceRepository.save(source, null)
            exportBytes(sourceRepository, source.id)
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val localSnapshot = source.calculationSnapshots.single().copy(
                id = "local-snapshot",
                result = source.calculationSnapshots.single().result.copy(
                    fourPillars = FourPillars("甲子", "乙丑", "丙寅", "丁卯"),
                ),
            )
            val localRecord = source.textRecords.first().copy(id = "local-record")
            val target = source.copy(
                id = "local-target",
                alias = "本地别名",
                name = ExplicitText.present("本地姓名"),
                profile = source.profile.copy(
                    occupation = ExplicitText.present("本地职业"),
                ),
                textRecords = listOf(localRecord),
                birthTimeCandidates = source.birthTimeCandidates.map {
                    it.copy(calculationSnapshotId = localSnapshot.id)
                },
                calculationSnapshots = listOf(localSnapshot),
                events = emptyList(),
                groups = source.groups.map { it.copy(id = "local-group") },
                tags = emptyList(),
                revision = 0,
            )
            destinationRepository.save(target, null)
            val generatedIds = generateSequence(1) { it + 1 }.iterator()
            val service = SingleCaseExchangeService(
                repository = destinationRepository,
                clock = fixedClock,
                idGenerator = { "merge-${generatedIds.next()}" },
            )
            val preview = (
                service.preview(ByteArrayInputStream(bytes)) as
                    SingleCasePreviewResult.Success
                ).preview
            val preparation = (
                service.prepareMerge(preview, target.id) as
                    SingleCaseMergePreparationResult.Success
                ).preparation

            assertEquals(1, preparation.addableCounts.calculationSnapshots)
            assertEquals(1, preparation.addableCounts.textRecords)
            assertEquals(1, preparation.addableCounts.events)
            assertTrue(
                preparation.fieldDifferences.any {
                    it.key == SingleCaseFieldKey.ALIAS
                },
            )
            val result = service.commitMerge(
                SingleCaseMergePlan(
                    preparation = preparation,
                    modules = SingleCaseMergeModule.entries.toSet(),
                    fieldChoices = mapOf(
                        SingleCaseFieldKey.ALIAS to SingleCaseValueChoice.IMPORTED,
                        SingleCaseFieldKey.NAME to SingleCaseValueChoice.LOCAL,
                        SingleCaseFieldKey.PROFILE_OCCUPATION to
                            SingleCaseValueChoice.LOCAL,
                    ),
                ),
            )

            assertTrue(result is SingleCaseImportResult.Merged)
            val merged = checkNotNull(destinationRepository.findById(target.id))
            assertEquals("来源别名", merged.alias)
            assertEquals("本地姓名", merged.name.value)
            assertEquals("本地职业", merged.profile.occupation.value)
            assertEquals(2, merged.calculationSnapshots.size)
            assertEquals(1, merged.calculationSnapshots.count { it.adopted })
            assertEquals(localSnapshot.result, merged.calculationSnapshots.single { it.adopted }.result)
            assertEquals(2, merged.textRecords.size)
            assertEquals(1, merged.events.size)
            assertEquals(1, merged.groups.size)
            assertEquals(1, merged.tags.size)
            assertEquals(2, merged.revision)
        }
    }

    @Test
    fun `采用导入出生输入时同步迁入候选和计算证据`() = runTest {
        val base = attachmentFree(sampleCase())
        val importedInput = base.birthInput.copy(
            calendarInput = BirthCalendarInput.Solar(
                CivilDateTime(1986, 5, 29, 15, 37, 0),
            ),
        )
        val source = base.copy(
            birthInput = importedInput,
            birthTimeCandidates = base.birthTimeCandidates.map {
                it.copy(birthInput = importedInput)
            },
            calculationSnapshots = base.calculationSnapshots.map {
                it.copy(
                    result = it.result.copy(
                        normalizedInput = importedInput,
                        fourPillars = FourPillars("丙寅", "癸巳", "癸酉", "庚申"),
                    ),
                )
            },
        )
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            sourceRepository.save(source, null)
            exportBytes(sourceRepository, source.id)
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val target = base
            destinationRepository.save(target, null)
            val ids = generateSequence(1) { it + 1 }.iterator()
            val service = SingleCaseExchangeService(
                repository = destinationRepository,
                clock = fixedClock,
                idGenerator = { "birth-merge-${ids.next()}" },
            )
            val preview = (
                service.preview(ByteArrayInputStream(bytes)) as
                    SingleCasePreviewResult.Success
                ).preview
            val preparation = (
                service.prepareMerge(preview, target.id) as
                    SingleCaseMergePreparationResult.Success
                ).preparation

            val result = service.commitMerge(
                SingleCaseMergePlan(
                    preparation = preparation,
                    fieldChoices = mapOf(
                        SingleCaseFieldKey.BIRTH_INPUT to
                            SingleCaseValueChoice.IMPORTED,
                    ),
                ),
            )

            assertTrue(result is SingleCaseImportResult.Merged)
            val merged = checkNotNull(destinationRepository.findById(target.id))
            assertEquals(importedInput, merged.birthInput)
            assertEquals(2, merged.birthTimeCandidates.size)
            val adoptedCandidate = merged.birthTimeCandidates.single { it.adopted }
            val adoptedSnapshot = merged.calculationSnapshots.single { it.adopted }
            assertEquals(importedInput, adoptedCandidate.birthInput)
            assertEquals(adoptedCandidate.calculationSnapshotId, adoptedSnapshot.id)
            assertEquals(adoptedCandidate.id, adoptedSnapshot.birthTimeCandidateId)
            assertEquals(
                FourPillars("丙寅", "癸巳", "癸酉", "庚申"),
                adoptedSnapshot.result.fourPillars,
            )
        }
    }

    @Test
    fun `合并方案生成后目标事实变化会拒绝且不覆盖`() = runTest {
        val source = attachmentFree(sampleCase())
        val bytes = withDatabase { sourceDatabase ->
            val sourceRepository = RoomCaseRepository(sourceDatabase)
            sourceRepository.save(source, null)
            exportBytes(sourceRepository, source.id)
        }

        withDatabase { destinationDatabase ->
            val destinationRepository = RoomCaseRepository(destinationDatabase)
            val target = source.copy(id = "local-target", alias = "本地目标", revision = 0)
            destinationRepository.save(target, null)
            val service = SingleCaseExchangeService(destinationRepository, fixedClock)
            val preview = (
                service.preview(ByteArrayInputStream(bytes)) as
                    SingleCasePreviewResult.Success
                ).preview
            val preparation = (
                service.prepareMerge(preview, target.id) as
                    SingleCaseMergePreparationResult.Success
                ).preparation
            destinationRepository.markViewed(
                target.id,
                FixtureInstant.plusSeconds(60),
            )

            val result = service.commitMerge(
                SingleCaseMergePlan(
                    preparation = preparation,
                    fieldChoices = mapOf(
                        SingleCaseFieldKey.ALIAS to SingleCaseValueChoice.IMPORTED,
                    ),
                ),
            )

            assertEquals(
                "TARGET_CHANGED",
                (result as SingleCaseImportResult.Rejected).code,
            )
            assertEquals("本地目标", destinationRepository.findById(target.id)?.alias)
        }
    }

    private suspend fun exportBytes(
        repository: RoomCaseRepository,
        caseId: String,
    ): ByteArray = ByteArrayOutputStream().also { output ->
        val result = SingleCaseExchangeService(repository, fixedClock).export(
            caseId = caseId,
            output = output,
            appVersion = "0.3.0-test",
            protection = SingleCaseProtection.UnencryptedSensitiveDataConfirmed,
        )
        check(result is SingleCaseExportResult.Success)
    }.toByteArray()

    private fun attachmentFree(case: com.nanzhufeng.nanfengbazi.domain.model.BaziCase) =
        case.copy(
            textRecords = case.textRecords.map { it.copy(sourceAttachmentId = null) },
            events = case.events.map { it.copy(sourceAttachmentId = null) },
            attachments = emptyList(),
            fieldEvidence = emptyList(),
        )

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
