package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportSourceApp
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomImportSessionRepositoryTest {
    private lateinit var database: NanfengBaziDatabase
    private lateinit var repository: RoomImportSessionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NanfengBaziDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomImportSessionRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `会话跨仓储实例持久化并按状态筛选`() = runTest {
        val waiting = fixture()
        assertTrue(repository.save(waiting, expectedRevision = 0) is ImportSessionWriteResult.Created)

        val anotherRepository = RoomImportSessionRepository(database)
        val restored = requireNotNull(anotherRepository.findById(waiting.id))
        assertEquals(1, restored.revision)
        assertEquals(ImportStatus.WAITING, restored.status)
        assertEquals(listOf(restored), anotherRepository.list(setOf(ImportStatus.WAITING)))
        assertEquals(emptyList<ImportSession>(), anotherRepository.list(setOf(ImportStatus.FAILED)))
    }

    @Test
    fun `更新执行修订冲突和状态迁移门禁`() = runTest {
        val waiting = fixture()
        repository.save(waiting, expectedRevision = 0)

        val invalid = waiting.copy(
            status = ImportStatus.READY_TO_COMMIT,
            updatedAt = waiting.updatedAt.plusSeconds(1),
        )
        assertTrue(
            repository.save(invalid, expectedRevision = 1) is
                ImportSessionWriteResult.InvalidTransition,
        )

        val copying = waiting.copy(
            status = ImportStatus.COPYING_IMAGES,
            updatedAt = waiting.updatedAt.plusSeconds(1),
        )
        val updated = repository.save(copying, expectedRevision = 1)
        assertTrue(updated is ImportSessionWriteResult.Updated)
        assertEquals(2, (updated as ImportSessionWriteResult.Updated).revision)

        val stale = repository.save(
            copying.copy(updatedAt = copying.updatedAt.plusSeconds(1)),
            expectedRevision = 1,
        )
        assertTrue(stale is ImportSessionWriteResult.RevisionConflict)
    }

    @Test
    fun `删除要求准确修订号`() = runTest {
        repository.save(fixture(), expectedRevision = 0)
        assertTrue(
            repository.delete("session-1", expectedRevision = 0) is
                ImportSessionDeleteResult.RevisionConflict,
        )
        assertEquals(
            ImportSessionDeleteResult.Deleted,
            repository.delete("session-1", expectedRevision = 1),
        )
        assertNull(repository.findById("session-1"))
    }

    private fun fixture(): ImportSession {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        return ImportSession(
            id = "session-1",
            sourceApp = ImportSourceApp.WENZHEN_BAZI,
            status = ImportStatus.WAITING,
            parserVersion = "unclassified-v1",
            createdAt = now,
            updatedAt = now,
        )
    }
}
