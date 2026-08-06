package com.fueru.app.escalation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fueru.app.FueruApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BOOT_COMPLETED rescheduling round — fires once daily (see
 * EscalationScheduler.scheduleDailyRescheduleAlarm), regardless of whether the app is ever opened
 * that day. Schedules the new day's escalation stages, then immediately re-arms itself for the
 * *next* day — a self-rescheduling one-shot alarm, matching EscalationReceiver's goAsync() pattern
 * since both steps are suspend/DB-touching work that must outlive onReceive's synchronous return.
 */
class DailyRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val application = context.applicationContext as FueruApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EscalationScheduler.scheduleTodaysEscalations(context, application.database)
                EscalationScheduler.scheduleDailyRescheduleAlarm(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
