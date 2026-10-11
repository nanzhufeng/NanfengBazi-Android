package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseCatalogStoreTest {
    @Test
    fun `慢写盘不阻塞目录修改且最终磁盘保留最新一代`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val writes = mutableListOf<CaseCatalogSnapshot>()
        val cache = object : CaseCatalogSnapshotCache {
            override suspend fun read(): CaseCatalogSnapshot? = null
            override suspend fun write(snapshot: CaseCatalogSnapshot) {
                started.complete(Unit)
                gate.await()
                writes += snapshot
            }
        }
        val repository = FakeCaseRepository().apply {
            stored["first"] = sampleStoredCase("first")
        }
        val store = RepositoryCaseCatalogStore(repository, UnconfinedTestDispatcher(testScheduler), cache)
        store.load(false)
        started.await()
        repository.stored["second"] = sampleStoredCase("second")
        val newest = repository.search(CaseSearchRequest(visibility = CaseVisibility.ALL))
        val replacement = async { store.replaceCachedCases(newest) }
        runCurrent()
        try {
            assertTrue("写盘挂起时更新仍须完成", replacement.isCompleted)
            assertEquals(listOf("first", "second"), store.cached()?.cases?.map { it.id })
        } finally {
            gate.complete(Unit)
        }
        assertTrue(replacement.await())
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("first", "second"), writes.last().cases.map { it.id })
    }
}
