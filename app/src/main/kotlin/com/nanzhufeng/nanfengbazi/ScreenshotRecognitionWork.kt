package com.nanzhufeng.nanfengbazi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.NetworkType
import androidx.work.workDataOf
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.imageparser.ImportRecognitionCoordinator
import com.nanzhufeng.nanfengbazi.imageparser.RecognitionRunResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

fun interface ScreenshotRecognitionScheduler {
    suspend fun recognize(sessionId: String): RecognitionRunResult
}

class DirectScreenshotRecognitionScheduler(
    private val coordinator: ImportRecognitionCoordinator,
) : ScreenshotRecognitionScheduler {
    override suspend fun recognize(sessionId: String): RecognitionRunResult =
        coordinator.recognize(sessionId)
}

class WorkManagerScreenshotRecognitionScheduler(
    context: Context,
    private val repository: ImportSessionRepository,
) : ScreenshotRecognitionScheduler {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun recognize(sessionId: String): RecognitionRunResult {
        val request = OneTimeWorkRequestBuilder<ScreenshotRecognitionWorker>()
            .setInputData(workDataOf(ScreenshotRecognitionWorker.SESSION_ID_KEY to sessionId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            "$UNIQUE_WORK_PREFIX$sessionId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        workManager.getWorkInfoByIdFlow(request.id)
            .filterNotNull()
            .first { it.state.isFinished }
        val session = repository.findById(sessionId) ?: return RecognitionRunResult.NotFound
        return when (session.status) {
            ImportStatus.NEEDS_REVIEW -> RecognitionRunResult.NeedsReview(session)
            ImportStatus.FAILED -> RecognitionRunResult.Failed(session)
            else -> RecognitionRunResult.Rejected(session.status)
        }
    }

    private companion object {
        const val UNIQUE_WORK_PREFIX = "screenshot-recognition-"
        const val WORK_TAG = "screenshot-recognition"
    }
}

class ScreenshotRecognitionWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(SESSION_ID_KEY) ?: return Result.failure()
        val container = (applicationContext as NanfengBaziApplication).container
        val session = container.importSessionRepository.findById(sessionId)
            ?: return Result.failure()
        if (recognitionNeedsForeground(session.images)) {
            setForeground(createForegroundInfo(sessionId, session.images.size))
        }
        return when (container.importRecognitionCoordinator.recognize(sessionId)) {
            is RecognitionRunResult.NeedsReview,
            is RecognitionRunResult.Failed,
            -> Result.success()

            RecognitionRunResult.RevisionConflict -> Result.retry()
            RecognitionRunResult.NotFound,
            is RecognitionRunResult.Rejected,
            -> Result.failure()
        }
    }

    private fun createForegroundInfo(
        sessionId: String,
        imageCount: Int,
    ): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(
            NotificationManager::class.java,
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "问真截图 AI 识别",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "仅在较大批次或超长截图由已确认的模型识别期间显示"
            },
        )
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(
            applicationContext,
            NOTIFICATION_CHANNEL_ID,
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在由 AI 模型识别问真截图")
            .setContentText("$imageCount 张图片 · 可返回应用查看进度")
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .addAction(0, "取消", cancelIntent)
            .build()
        val notificationId = sessionId.hashCode().and(Int.MAX_VALUE)
            .coerceAtLeast(1)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        const val SESSION_ID_KEY = "session_id"
        const val NOTIFICATION_CHANNEL_ID = "screenshot-recognition"
    }
}

internal fun recognitionNeedsForeground(images: List<ImportImageRef>): Boolean {
    if (images.size >= FOREGROUND_IMAGE_COUNT_THRESHOLD) return true
    val totalBytes = images.sumOf(ImportImageRef::byteSize)
    if (totalBytes >= FOREGROUND_BYTE_THRESHOLD) return true
    val totalPixels = images.sumOf { image ->
        (image.widthPx?.toLong() ?: 0L) * (image.heightPx?.toLong() ?: 0L)
    }
    return totalPixels >= FOREGROUND_PIXEL_THRESHOLD
}

private const val FOREGROUND_IMAGE_COUNT_THRESHOLD = 8
private const val FOREGROUND_BYTE_THRESHOLD = 32L * 1024L * 1024L
private const val FOREGROUND_PIXEL_THRESHOLD = 48_000_000L
