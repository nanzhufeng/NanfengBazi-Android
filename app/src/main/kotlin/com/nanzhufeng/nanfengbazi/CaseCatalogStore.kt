package com.nanzhufeng.nanfengbazi

import android.util.AtomicFile
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 应用级命例目录：列表、首页最近命例和筛选共享同一份轻量读取快照。
 *
 * 它不拥有命例真值；真值仍只在 [CaseRepository]。目录只负责把一次 Room 读取
 * 结果复用到 Activity/ViewModel 重建后的首屏，避免每个页面各自从零开始整库读取。
 */
@Serializable
data class CaseCatalogSnapshot(
    val cases: List<CaseSummary>,
    val groupsByLibrary: Map<CaseLibraryType, List<CaseGroup>>,
)

data class CaseCatalogBootstrap(
    val snapshot: CaseCatalogSnapshot,
    val fromPersistentCache: Boolean,
)

/**
 * The catalog cache deliberately lives in the app-private no-backup directory. It accelerates
 * first paint after process death, but remains a disposable projection: Room is still the only
 * business truth and every persisted snapshot is calibrated in the background.
 */
interface CaseCatalogSnapshotCache {
    suspend fun read(): CaseCatalogSnapshot?

    suspend fun write(snapshot: CaseCatalogSnapshot)
}

object EmptyCaseCatalogSnapshotCache : CaseCatalogSnapshotCache {
    override suspend fun read(): CaseCatalogSnapshot? = null

    override suspend fun write(snapshot: CaseCatalogSnapshot) = Unit
}

class FileCaseCatalogSnapshotCache(
    file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : CaseCatalogSnapshotCache {
    private val file = AtomicFile(file)

    override suspend fun read(): CaseCatalogSnapshot? = runCatching {
        if (!file.baseFile.isFile) return null
        file.openRead().bufferedReader().use { reader ->
            json.decodeFromString(CaseCatalogSnapshot.serializer(), reader.readText())
        }
    }.getOrNull()

    override suspend fun write(snapshot: CaseCatalogSnapshot) {
        val output = file.startWrite()
        try {
            // AtomicFile owns this stream until finishWrite/failWrite. Closing it through a
            // writer first can make the final fsync fail on some Android implementations.
            output.write(
                json.encodeToString(CaseCatalogSnapshot.serializer(), snapshot)
                    .toByteArray(Charsets.UTF_8),
            )
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }
}

interface CaseCatalogStore {
    fun cached(): CaseCatalogSnapshot?

    /** Publishes a successful, precise list mutation without re-reading the whole catalog. */
    suspend fun replaceCachedCases(cases: List<CaseSummary>): Boolean

    /** Returns a memory/disk first-frame snapshot without forcing another Room traversal. */
    suspend fun bootstrap(): CaseCatalogBootstrap

    /** Reads one coherent repository snapshot. Concurrent callers share the same load. */
    suspend fun load(forceRefresh: Boolean): CaseCatalogSnapshot
}

class RepositoryCaseCatalogStore(
    private val repository: CaseRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val persistentCache: CaseCatalogSnapshotCache = EmptyCaseCatalogSnapshotCache,
) : CaseCatalogStore {
    private val mutex = Mutex()
    private val persistenceScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    @Volatile
    private var latest: CaseCatalogSnapshot? = null

    /** Monotonically increases after a repository snapshot is fully published. */
    @Volatile
    private var refreshGeneration: Long = 0L

    override fun cached(): CaseCatalogSnapshot? = latest

    override suspend fun replaceCachedCases(cases: List<CaseSummary>): Boolean = mutex.withLock {
        val snapshot = latest ?: return@withLock false
        val updated = snapshot.copy(cases = cases)
        latest = updated
        refreshGeneration += 1
        persistAsync(updated, refreshGeneration)
        true
    }

    override suspend fun bootstrap(): CaseCatalogBootstrap = mutex.withLock {
        latest?.let { snapshot -> return@withLock CaseCatalogBootstrap(snapshot, false) }
        readPersisted()?.let { snapshot ->
            latest = snapshot
            refreshGeneration += 1
            return@withLock CaseCatalogBootstrap(snapshot, true)
        }
        CaseCatalogBootstrap(loadFromRepository(), false)
    }

    override suspend fun load(forceRefresh: Boolean): CaseCatalogSnapshot {
        if (!forceRefresh) return bootstrap().snapshot
        // Two callers that requested a refresh at the same time share the first completed
        // snapshot. A later, deliberate refresh still receives a new repository read.
        val observedGeneration = refreshGeneration
        return mutex.withLock {
            latest?.takeIf { !forceRefresh || refreshGeneration != observedGeneration }
                ?: loadFromRepository()
        }
    }

    private suspend fun readPersisted(): CaseCatalogSnapshot? = withContext(ioDispatcher) {
        persistentCache.read()
    }

    private suspend fun loadFromRepository(): CaseCatalogSnapshot = withContext(ioDispatcher) {
        CaseCatalogSnapshot(
            cases = repository.search(
                CaseSearchRequest(visibility = CaseVisibility.ALL),
            ),
            groupsByLibrary = CaseLibraryType.entries.associateWith { libraryType ->
                repository.listGroups(libraryType)
            },
        )
    }.also { snapshot ->
        latest = snapshot
        refreshGeneration += 1
        persistAsync(snapshot, refreshGeneration)
    }

    /**
     * Disk cache must never delay an optimistic list update. Persist only the newest coherent
     * generation and serialize the write behind the same mutex, so an older async write cannot
     * overwrite a newer snapshot after a rapid series of edits.
     */
    private fun persistAsync(snapshot: CaseCatalogSnapshot, generation: Long) {
        persistenceScope.launch {
            mutex.withLock {
                if (latest === snapshot && refreshGeneration == generation) {
                    runCatching { persistentCache.write(snapshot) }
                }
            }
        }
    }
}
