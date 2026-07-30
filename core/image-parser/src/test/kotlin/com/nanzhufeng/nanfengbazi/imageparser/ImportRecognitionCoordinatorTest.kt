package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportSourceApp
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import com.nanzhufeng.nanfengbazi.domain.model.canTransitionTo
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportRecognitionCoordinatorTest {
    @Test
    fun `合成图片经过离线 OCR 分类并把原文持久化到待核对会话`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val repository = InMemoryImportSessionRepository(fixture(bytes.size.toLong()))
        val coordinator = coordinator(
            repository = repository,
            bytes = bytes,
            engine = FixtureOcrEngine(),
        )

        val result = coordinator.recognize("session-1")

        assertTrue(result is RecognitionRunResult.NeedsReview)
        val session = (result as RecognitionRunResult.NeedsReview).session
        assertEquals(ImportStatus.NEEDS_REVIEW, session.status)
        assertEquals(1, session.attemptCount)
        assertEquals(4, session.revision)
        assertEquals(WenzhenPageType.USER_LIST, session.images.single().pageType)
        assertEquals(
            "问真八字 用户列表 筛选 阳历1992年8月24日",
            session.ocrDocuments.single().rawText,
        )
        assertEquals(session, repository.findById("session-1"))
    }

    @Test
    fun `识别失败保存可重试状态且第二次继续使用原图片完成`() = runTest {
        val bytes = byteArrayOf(7, 8, 9)
        val repository = InMemoryImportSessionRepository(fixture(bytes.size.toLong()))
        val engine = FixtureOcrEngine(failFirst = true)
        val coordinator = coordinator(repository, bytes, engine)

        val failed = coordinator.recognize("session-1")
        assertTrue(failed is RecognitionRunResult.Failed)
        val failedSession = (failed as RecognitionRunResult.Failed).session
        assertEquals(ImportStatus.FAILED, failedSession.status)
        assertTrue(requireNotNull(failedSession.failure).retryable)
        assertEquals(1, failedSession.images.size)

        val recovered = coordinator.recognize("session-1")
        assertTrue(recovered is RecognitionRunResult.NeedsReview)
        val recoveredSession = (recovered as RecognitionRunResult.NeedsReview).session
        assertEquals(2, recoveredSession.attemptCount)
        assertEquals(ImportStatus.NEEDS_REVIEW, recoveredSession.status)
        assertNotNull(recoveredSession.ocrDocuments.single())
    }

    @Test
    fun `进程在归组阶段中断后可直接恢复到待核对而不重复 OCR`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val document = OcrDocument(
            imageId = "image-1",
            rawText = "问真八字 用户列表 筛选 阳历1992年8月24日",
            blocks = emptyList(),
            engineId = "fixture-offline",
            engineVersion = "1",
            recognizedAt = Instant.parse("2026-01-01T00:01:00Z"),
        )
        val interrupted = fixture(bytes.size.toLong()).copy(
            status = ImportStatus.GROUPING_CASES,
            ocrDocuments = listOf(document),
        )
        val repository = InMemoryImportSessionRepository(interrupted)
        val coordinator = coordinator(repository, bytes, FixtureOcrEngine(failFirst = true))

        val result = coordinator.recognize("session-1")

        assertTrue(result is RecognitionRunResult.NeedsReview)
        assertEquals(
            ImportStatus.NEEDS_REVIEW,
            (result as RecognitionRunResult.NeedsReview).session.status,
        )
    }

    @Test
    fun `批次中单张失败不阻塞其余图片且重试只处理失败图片`() = runTest {
        val bytes = byteArrayOf(4, 5, 6)
        val first = fixture(bytes.size.toLong())
        val secondImage = first.images.single().copy(
            id = "image-2",
            originalFileName = "synthetic-2.png",
            relativePath = "session-1/image-2.png",
            sha256 = "b".repeat(64),
        )
        val repository = InMemoryImportSessionRepository(
            first.copy(images = first.images + secondImage),
        )
        val engine = SelectiveFailureOcrEngine(failingImageId = "image-2")
        val coordinator = ImportRecognitionCoordinator(
            repository = repository,
            contentReader = ImportImageContentReader { bytes },
            ocrEngine = engine,
            pageClassifier = AnchorBasedWenzhenPageClassifier(),
            clock = Clock.fixed(Instant.parse("2026-01-01T00:01:00Z"), ZoneOffset.UTC),
            diagnosticIdFactory = { "diagnostic-${engine.totalCalls}" },
        )

        val partial = coordinator.recognize("session-1")

        assertTrue(partial is RecognitionRunResult.NeedsReview)
        val partialSession = (partial as RecognitionRunResult.NeedsReview).session
        assertEquals(listOf("image-1"), partialSession.ocrDocuments.map { it.imageId })
        assertEquals(listOf("image-2"), partialSession.imageFailures.map { it.imageId })
        assertEquals(1, engine.callsByImage.getValue("image-1"))
        assertEquals(1, engine.callsByImage.getValue("image-2"))

        engine.failingImageId = null
        val recovered = coordinator.recognize("session-1")

        assertTrue(recovered is RecognitionRunResult.NeedsReview)
        val recoveredSession = (recovered as RecognitionRunResult.NeedsReview).session
        assertTrue(recoveredSession.imageFailures.isEmpty())
        assertEquals(setOf("image-1", "image-2"), recoveredSession.ocrDocuments.map { it.imageId }.toSet())
        assertEquals("成功图片不应在局部重试时重复 OCR", 1, engine.callsByImage.getValue("image-1"))
        assertEquals(2, engine.callsByImage.getValue("image-2"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `在线 OCR 引擎不能接入图片导入主链路`() {
        val bytes = byteArrayOf(1)
        ImportRecognitionCoordinator(
            repository = InMemoryImportSessionRepository(fixture(1)),
            contentReader = ImportImageContentReader { bytes },
            ocrEngine = object : FixtureOcrEngine() {
                override val executionMode: OcrExecutionMode = OcrExecutionMode.ONLINE
            },
            pageClassifier = AnchorBasedWenzhenPageClassifier(),
        )
    }

    private fun coordinator(
        repository: ImportSessionRepository,
        bytes: ByteArray,
        engine: FixtureOcrEngine,
    ) = ImportRecognitionCoordinator(
        repository = repository,
        contentReader = ImportImageContentReader { bytes },
        ocrEngine = engine,
        pageClassifier = AnchorBasedWenzhenPageClassifier(),
        clock = Clock.fixed(Instant.parse("2026-01-01T00:01:00Z"), ZoneOffset.UTC),
        diagnosticIdFactory = { "diagnostic-1" },
    )

    private fun fixture(byteSize: Long): ImportSession {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        return ImportSession(
            id = "session-1",
            sourceApp = ImportSourceApp.WENZHEN_BAZI,
            status = ImportStatus.CLASSIFYING,
            images = listOf(
                ImportImageRef(
                    id = "image-1",
                    originalFileName = "synthetic.png",
                    mimeType = "image/png",
                    relativePath = "session-1/image-1.png",
                    sha256 = "a".repeat(64),
                    byteSize = byteSize,
                    createdAt = now,
                ),
            ),
            parserVersion = "wenzhen-p0-v1",
            createdAt = now,
            updatedAt = now,
            revision = 1,
        )
    }
}

private open class FixtureOcrEngine(
    private val failFirst: Boolean = false,
) : OcrEngine {
    private var calls = 0
    override val engineId: String = "fixture-offline"
    override val engineVersion: String = "1"
    override val executionMode: OcrExecutionMode = OcrExecutionMode.OFFLINE

    override suspend fun recognize(input: OcrImageInput): OcrDocument {
        calls += 1
        if (failFirst && calls == 1) error("synthetic failure")
        return OcrDocument(
            imageId = input.image.id,
            rawText = "问真八字 用户列表 筛选 阳历1992年8月24日",
            blocks = emptyList(),
            engineId = engineId,
            engineVersion = engineVersion,
            recognizedAt = Instant.parse("2026-01-01T00:01:00Z"),
        )
    }
}

private class SelectiveFailureOcrEngine(
    var failingImageId: String?,
) : OcrEngine {
    val callsByImage = mutableMapOf<String, Int>()
    val totalCalls: Int
        get() = callsByImage.values.sum()

    override val engineId: String = "fixture-offline"
    override val engineVersion: String = "1"
    override val executionMode: OcrExecutionMode = OcrExecutionMode.OFFLINE

    override suspend fun recognize(input: OcrImageInput): OcrDocument {
        callsByImage[input.image.id] = callsByImage.getOrDefault(input.image.id, 0) + 1
        if (input.image.id == failingImageId) error("synthetic selected image failure")
        return OcrDocument(
            imageId = input.image.id,
            rawText = "问真八字 用户列表 筛选 阳历1992年8月24日",
            blocks = emptyList(),
            engineId = engineId,
            engineVersion = engineVersion,
            recognizedAt = Instant.parse("2026-01-01T00:01:00Z"),
        )
    }
}

private class InMemoryImportSessionRepository(
    initial: ImportSession,
) : ImportSessionRepository {
    private var session: ImportSession? = initial

    override suspend fun save(
        session: ImportSession,
        expectedRevision: Long?,
    ): ImportSessionWriteResult {
        val current = this.session
        if (current == null) {
            val persisted = session.copy(revision = 1)
            this.session = persisted
            return ImportSessionWriteResult.Created(session.id, 1)
        }
        if (expectedRevision != current.revision) {
            return ImportSessionWriteResult.RevisionConflict(
                sessionId = session.id,
                expectedRevision = expectedRevision ?: -1,
                actualRevision = current.revision,
            )
        }
        if (!current.status.canTransitionTo(session.status)) {
            return ImportSessionWriteResult.InvalidTransition(
                session.id,
                current.status,
                session.status,
            )
        }
        val persisted = session.copy(revision = current.revision + 1)
        this.session = persisted
        return ImportSessionWriteResult.Updated(session.id, persisted.revision)
    }

    override suspend fun findById(id: String): ImportSession? =
        session?.takeIf { it.id == id }

    override suspend fun list(statuses: Set<ImportStatus>): List<ImportSession> =
        listOfNotNull(session).filter { statuses.isEmpty() || it.status in statuses }

    override suspend fun delete(
        id: String,
        expectedRevision: Long,
    ): ImportSessionDeleteResult = ImportSessionDeleteResult.NotFound
}
