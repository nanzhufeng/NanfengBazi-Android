package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseAttachmentMode
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocument
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExchangeService
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreviewResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseProtection
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
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
    fun `密码加密未实现时拒绝且不产生明文`() = runTest {
        withDatabase { database ->
            val repository = RoomCaseRepository(database)
            val source = sampleCase()
            repository.save(source, null)
            val output = ByteArrayOutputStream()

            val result = SingleCaseExchangeService(repository, fixedClock).export(
                caseId = source.id,
                output = output,
                appVersion = "0.3.0-test",
                protection = SingleCaseProtection.PasswordProtected("test".toCharArray()),
            )

            assertEquals(
                "PASSWORD_ENCRYPTION_NOT_IMPLEMENTED",
                (result as SingleCaseExportResult.Rejected).code,
            )
            assertEquals(0, output.size())
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
