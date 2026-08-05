package com.fueru.app.escalation

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fueru.app.FueruApplication
import com.fueru.app.notifications.NotificationHelper

/**
 * §8.3 — resolves any Stage 4 firings that queued because the device was offline, the moment
 * connectivity actually returns (NetworkType.CONNECTED constraint, same WorkManager tool
 * DailyNudgeWorker already uses elsewhere in this project). Surfaces each one as a fresh
 * notification rather than silently completing it in the background — the whole point of a real
 * stake is that it's visible, not a background job that could quietly fail.
 */
class ConsequenceRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as FueruApplication
        val database = application.database
        val queued = database.consequenceEventDao().getQueued()
        for (event in queued) {
            val practice = database.practiceDao().getById(event.practiceId) ?: continue
            database.consequenceEventDao().update(event.copy(queued = false))
            NotificationHelper.notifyConsequenceReady(applicationContext, event.id, practice.name, event.charityName)
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<ConsequenceRetryWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "consequence_retry",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
