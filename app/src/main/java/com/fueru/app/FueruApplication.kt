package com.fueru.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.AppLogger
import com.fueru.app.data.seed.ensureSeeded
import com.fueru.app.escalation.EscalationScheduler
import com.fueru.app.notifications.DailyNudgeWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FueruApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // In-app logging round — installed first, before anything else has a chance to crash.
        AppLogger.installUncaughtExceptionHandler(this)
        AppLogger.log(this, "App", "onCreate")
        // Defensive re-seed on every launch, not just RoomDatabase.Callback.onCreate — cheap COUNT
        // check, self-heals if a schema migration ever leaves the program table empty.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            ensureSeeded(this@FueruApplication, database)
            EscalationScheduler.scheduleTodaysEscalations(this@FueruApplication, database)
            // BOOT_COMPLETED rescheduling round — also (re)arm the daily reschedule alarm here as
            // defense in depth, covering the case where exact-alarm permission gets granted after
            // boot (BootCompletedReceiver's own attempt would have silently no-op'd until then).
            EscalationScheduler.scheduleDailyRescheduleAlarm(this@FueruApplication)
        }
        scheduleDailyNudge()
    }

    /** KEEP policy: re-enqueuing on every app launch must not reset an already-scheduled cycle. */
    private fun scheduleDailyNudge() {
        val request = PeriodicWorkRequestBuilder<DailyNudgeWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_nudge",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
