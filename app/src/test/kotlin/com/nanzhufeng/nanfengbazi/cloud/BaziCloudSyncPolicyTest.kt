package com.nanzhufeng.nanfengbazi.cloud

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaziCloudSyncPolicyTest {
    @Test
    fun `empty device restores an existing remote backup`() {
        assertEquals(
            BaziCloudSyncAction.RESTORE_REMOTE,
            decideBaziCloudSyncAction(
                localHasData = false,
                remoteRevision = 7,
                knownRemoteRevision = 0,
                localFingerprintMatches = false,
            ),
        )
    }

    @Test
    fun `nonempty device always remains the backup source when versions differ`() {
        assertEquals(
            BaziCloudSyncAction.UPLOAD_LOCAL,
            decideBaziCloudSyncAction(
                localHasData = true,
                remoteRevision = 9,
                knownRemoteRevision = 7,
                localFingerprintMatches = true,
            ),
        )
        assertEquals(
            BaziCloudSyncAction.UPLOAD_LOCAL,
            decideBaziCloudSyncAction(
                localHasData = true,
                remoteRevision = 9,
                knownRemoteRevision = 7,
                localFingerprintMatches = false,
            ),
        )
    }

    @Test
    fun `matching local and remote metadata performs no write`() {
        assertEquals(
            BaziCloudSyncAction.ALREADY_CURRENT,
            decideBaziCloudSyncAction(
                localHasData = true,
                remoteRevision = 9,
                knownRemoteRevision = 9,
                localFingerprintMatches = true,
            ),
        )
    }

    @Test
    fun `missing remote document uploads the local snapshot`() {
        assertEquals(
            BaziCloudSyncAction.UPLOAD_LOCAL,
            decideBaziCloudSyncAction(
                localHasData = true,
                remoteRevision = null,
                knownRemoteRevision = 0,
                localFingerprintMatches = false,
            ),
        )
    }

    @Test
    fun `coroutine cancellation is control flow and periodic enable does not replace running work`() {
        val sourceRoot = locateCloudSourceRoot()
        val coordinator = File(sourceRoot, "BaziCloudCore.kt").readText()
        val scheduler = File(sourceRoot, "BaziCloudSyncWork.kt").readText()
        val syncBody = coordinator.substringAfter("suspend fun syncNow()")
            .substringBefore("suspend fun overwriteRemoteWithLocal()")

        assertTrue(syncBody.contains("catch (cancelled: CancellationException)"))
        assertTrue(syncBody.contains("throw cancelled"))
        assertFalse(scheduler.contains("ExistingPeriodicWorkPolicy.UPDATE"))
        assertTrue(scheduler.contains("ExistingPeriodicWorkPolicy.KEEP"))
    }

    @Test
    fun `only idempotent reads retry transient transport failures`() {
        assertTrue(shouldRetryBaziCloudRequest("GET", 1, IOException("temporary")))
        assertTrue(shouldRetryBaziCloudRequest("GET", 2, BaziCloudHttpException(503, null)))
        assertFalse(shouldRetryBaziCloudRequest("GET", 3, IOException("temporary")))
        assertFalse(shouldRetryBaziCloudRequest("POST", 1, IOException("unknown commit outcome")))
        assertFalse(shouldRetryBaziCloudRequest("GET", 1, BaziCloudHttpException(401, null)))
    }

    @Test
    fun `background worker uses structured result and preserves changes made during sync`() {
        val sourceRoot = locateCloudSourceRoot()
        val coordinator = File(sourceRoot, "BaziCloudCore.kt").readText()
        val scheduler = File(sourceRoot, "BaziCloudSyncWork.kt").readText()

        assertTrue(coordinator.contains("suspend fun syncInBackground(): BaziCloudSyncRunResult"))
        assertTrue(coordinator.contains("if (origin == BaziCloudSyncOrigin.MANUAL)"))
        assertTrue(coordinator.contains("网络恢复后会自动同步"))
        assertTrue(coordinator.contains("commitWithReconciliation"))
        assertTrue(coordinator.contains("readDocumentMetadata"))
        assertTrue(coordinator.contains("BaziCloudCrypto.sha256(readBackCiphertext) == encrypted.sha256"))
        assertTrue(coordinator.contains("setFixedLengthStreamingMode(bodyBytes.size)"))
        assertFalse(scheduler.contains("when (coordinator.state.value)"))
        assertTrue(scheduler.contains("syncResult == BaziCloudSyncRunResult.RETRY || changedDuringSync"))
        assertTrue(scheduler.contains("localChangeGeneration"))
        assertTrue(scheduler.contains("enqueueRetry"))
    }

    private fun locateCloudSourceRoot(): File {
        val candidates = listOf(
            File("src/main/kotlin/com/nanzhufeng/nanfengbazi/cloud"),
            File("app/src/main/kotlin/com/nanzhufeng/nanfengbazi/cloud"),
        )
        return candidates.firstOrNull(File::isDirectory)
            ?: error("无法定位南枫云源码目录")
    }
}
