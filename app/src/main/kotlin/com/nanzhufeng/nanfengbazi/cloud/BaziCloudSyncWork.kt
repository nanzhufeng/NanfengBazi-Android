package com.nanzhufeng.nanfengbazi.cloud

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nanzhufeng.nanfengbazi.NanfengBaziApplication
import java.util.concurrent.TimeUnit

class BaziCloudSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? NanfengBaziApplication ?: return Result.failure()
        val coordinator = app.container.cloudSyncCoordinator
        val isLocalChange = inputData.getBoolean(LOCAL_CHANGE_INPUT, false)
        val startedGeneration = if (isLocalChange) BaziCloudSyncScheduler.localChangeGeneration(applicationContext) else null
        val syncResult = if (isLocalChange) {
            coordinator.syncAfterLocalChange()
        } else {
            coordinator.syncInBackground()
        }
        val changedDuringSync = startedGeneration != null &&
            startedGeneration != BaziCloudSyncScheduler.localChangeGeneration(applicationContext)
        return when {
            syncResult == BaziCloudSyncRunResult.FAILURE -> Result.failure()
            syncResult == BaziCloudSyncRunResult.RETRY || changedDuringSync -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        const val LOCAL_CHANGE_INPUT = "nanfeng_bazi_cloud_local_change"
    }
}

object BaziCloudSyncScheduler {
    private const val PERIODIC_WORK = "nanfeng-bazi-cloud-sync-periodic"
    private const val CHANGE_WORK = "nanfeng-bazi-cloud-sync-after-change"
    private const val RETRY_WORK = "nanfeng-bazi-cloud-sync-retry"
    private const val SCHEDULER_PREFERENCES = "nanfeng_bazi_cloud_scheduler"
    private const val LOCAL_CHANGE_GENERATION = "local_change_generation"

    fun enable(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            // App 冷启动只确保周期任务存在，不用 UPDATE 取消正在执行的同步。
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BaziCloudSyncWorker>(12, TimeUnit.HOURS, 2, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun enqueueAfterLocalChange(context: Context) {
        recordLocalChange(context)
        WorkManager.getInstance(context).enqueueUniqueWork(
            CHANGE_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BaziCloudSyncWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setInputData(
                    Data.Builder()
                        .putBoolean(BaziCloudSyncWorker.LOCAL_CHANGE_INPUT, true)
                        .build(),
                )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun enqueueRetry(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            RETRY_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BaziCloudSyncWorker>()
                .setInitialDelay(15, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun localChangeGeneration(context: Context): Long = context.applicationContext
        .getSharedPreferences(SCHEDULER_PREFERENCES, Context.MODE_PRIVATE)
        .getLong(LOCAL_CHANGE_GENERATION, 0L)

    @Synchronized
    private fun recordLocalChange(context: Context) {
        val preferences = context.applicationContext
            .getSharedPreferences(SCHEDULER_PREFERENCES, Context.MODE_PRIVATE)
        val next = if (preferences.getLong(LOCAL_CHANGE_GENERATION, 0L) == Long.MAX_VALUE) {
            1L
        } else {
            preferences.getLong(LOCAL_CHANGE_GENERATION, 0L) + 1L
        }
        preferences.edit().putLong(LOCAL_CHANGE_GENERATION, next).commit()
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(CHANGE_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(RETRY_WORK)
    }
}
