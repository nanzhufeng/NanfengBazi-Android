package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.canTransitionTo
import com.nanzhufeng.nanfengbazi.imageparser.AnchorBasedWenzhenPageClassifier
import com.nanzhufeng.nanfengbazi.imageparser.ImportImageContentReader
import com.nanzhufeng.nanfengbazi.imageparser.ImportRecognitionCoordinator
import com.nanzhufeng.nanfengbazi.imageparser.OcrEngine
import com.nanzhufeng.nanfengbazi.imageparser.OcrExecutionMode
import com.nanzhufeng.nanfengbazi.imageparser.OcrImageInput
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotImportViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `选择图片后先私有复制再离线识别且不写正式命例`() = runBlocking {
        val repository = FakeImportSessionRepository()
        val imageStore = PrivateImportImageStore(temporaryFolder.newFolder("imports").toPath())
        val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        val coordinator = ImportRecognitionCoordinator(
            repository = repository,
            contentReader = ImportImageContentReader(imageStore::readBytes),
            ocrEngine = FixtureOfflineOcrEngine(),
            pageClassifier = AnchorBasedWenzhenPageClassifier(),
            clock = clock,
            diagnosticIdFactory = { "diagnostic-1" },
        )
        val ids = ArrayDeque(listOf("session-1", "image-1"))
        val viewModel = ScreenshotImportViewModel(
            repository = repository,
            imageStore = imageStore,
            recognitionCoordinator = coordinator,
            clock = clock,
            idFactory = ids::removeFirst,
        )
        val bytes = "synthetic-image".encodeToByteArray()

        viewModel.importImages(
            listOf(
                PendingImportImage(
                    originalFileName = "合成问真列表.png",
                    mimeType = "image/png",
                    openInput = { ByteArrayInputStream(bytes) },
                ),
            ),
        )

        val state = withTimeout(5_000) {
            viewModel.state.first {
                it.needsReview && !it.busy && it.recoverableSessionCount == 1
            }
        }
        assertEquals(1, state.completedImageCount)
        assertEquals(1, state.recoverableSessionCount)
        val session = requireNotNull(repository.findById("session-1"))
        assertEquals(ImportStatus.NEEDS_REVIEW, session.status)
        assertEquals(bytes.toList(), imageStore.readBytes(session.images.single()).toList())
        assertTrue(session.ocrDocuments.single().rawText.contains("用户列表"))
    }
}

private class FixtureOfflineOcrEngine : OcrEngine {
    override val engineId: String = "fixture-offline"
    override val engineVersion: String = "1"
    override val executionMode: OcrExecutionMode = OcrExecutionMode.OFFLINE

    override suspend fun recognize(input: OcrImageInput): OcrDocument = OcrDocument(
        imageId = input.image.id,
        rawText = "问真八字 用户列表 筛选 阳历1992年8月24日",
        blocks = emptyList(),
        engineId = engineId,
        engineVersion = engineVersion,
        recognizedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}

private class FakeImportSessionRepository : ImportSessionRepository {
    private val sessions = linkedMapOf<String, ImportSession>()

    override suspend fun save(
        session: ImportSession,
        expectedRevision: Long?,
    ): ImportSessionWriteResult {
        val current = sessions[session.id]
        if (current == null) {
            if (expectedRevision != null && expectedRevision != 0L) {
                return ImportSessionWriteResult.RevisionConflict(
                    session.id,
                    expectedRevision,
                    0,
                )
            }
            sessions[session.id] = session.copy(revision = 1)
            return ImportSessionWriteResult.Created(session.id, 1)
        }
        if (expectedRevision != current.revision) {
            return ImportSessionWriteResult.RevisionConflict(
                session.id,
                expectedRevision ?: -1,
                current.revision,
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
        sessions[session.id] = persisted
        return ImportSessionWriteResult.Updated(session.id, persisted.revision)
    }

    override suspend fun findById(id: String): ImportSession? = sessions[id]

    override suspend fun list(statuses: Set<ImportStatus>): List<ImportSession> =
        sessions.values.filter { statuses.isEmpty() || it.status in statuses }

    override suspend fun delete(
        id: String,
        expectedRevision: Long,
    ): ImportSessionDeleteResult = ImportSessionDeleteResult.NotFound
}
