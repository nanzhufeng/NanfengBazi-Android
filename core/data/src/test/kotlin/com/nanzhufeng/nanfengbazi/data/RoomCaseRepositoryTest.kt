package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
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
class RoomCaseRepositoryTest {
    private lateinit var database: NanfengBaziDatabase
    private lateinit var repository: RoomCaseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NanfengBaziDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomCaseRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `命例及来源证据可以完整往返`() = runTest {
        val source = sampleCase()

        assertEquals(CaseWriteResult.Created("case-1", 1), repository.save(source, null))

        val restored = repository.findById(source.id)
        assertEquals(source.copy(revision = 1), restored)
        assertEquals(listOf("case-1"), repository.search("测试甲").map { it.id })
        assertEquals(listOf("case-1"), repository.search("脱敏案例").map { it.id })
    }

    @Test
    fun `修订号阻止旧编辑覆盖新数据`() = runTest {
        val source = sampleCase()
        repository.save(source, null)

        val updated = source.copy(
            profile = source.profile.copy(health = ExplicitText.cleared()),
            updatedAt = FixtureInstant.plusSeconds(60),
        )
        assertEquals(CaseWriteResult.Updated("case-1", 2), repository.save(updated, 1))
        assertEquals(
            CaseWriteResult.RevisionConflict("case-1", 1, 2),
            repository.save(source, 1),
        )
        assertEquals(ExplicitText.cleared(), repository.findById("case-1")?.profile?.health)
    }

    @Test
    fun `没有修订号的重复稳定ID不会覆盖已有命例`() = runTest {
        val source = sampleCase()
        repository.save(source, null)

        val result = repository.save(source.copy(alias = "不应覆盖"), null)

        assertTrue(result is CaseWriteResult.AlreadyExists)
        assertEquals("脱敏案例一", repository.findById("case-1")?.alias)
        assertNull(repository.findById("missing"))
    }
}
