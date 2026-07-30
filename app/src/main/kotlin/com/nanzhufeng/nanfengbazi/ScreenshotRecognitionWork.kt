package com.nanzhufeng.nanfengbazi

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
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

    companion object {
        const val SESSION_ID_KEY = "session_id"
    }
}
