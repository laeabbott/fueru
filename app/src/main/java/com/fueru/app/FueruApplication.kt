package com.fueru.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fueru.app.data.AppDatabase
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
        // Defensive re-seed on every launch, not just RoomDatabase.Callback.onCreate — cheap COUNT
        // check, self-heals if a schema migration ever leaves the program table empty.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            ensureSeeded(this@FueruApplication, database)
            // App-open-triggered escalation scheduling (§7.2 Stages 0-2) — see the Phase 4 plan's
            // "scheduling trigger" note: a day the app is never opened won't get alarms scheduled,
            // a known v1 limitation, not a BOOT_COMPLETED/daily-WorkManager job yet.
            EscalationScheduler.scheduleTodaysEscalations(this@FueruApplication, database)
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
