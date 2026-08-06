package com.fueru.app.escalation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fueru.app.FueruApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BOOT_COMPLETED rescheduling round — a device reboot or app update clears every AlarmManager exact
 * alarm outright, including the daily reschedule alarm itself, so both need re-arming from here, not
 * just the practice stages. Listens for MY_PACKAGE_REPLACED too (an app update is the same failure
 * mode as a reboot for anything AlarmManager-scheduled) — the only exported receiver in this app,
 * since both of these are genuine system broadcasts, not something only fueru itself ever sends.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val application = context.applicationContext as FueruApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Covers the rest of *today* directly (boot can happen mid-day, after the old daily
                // reschedule alarm reboot just cleared would have already fired), then re-arms the
                // recurring daily trigger reboot/update cleared.
                EscalationScheduler.scheduleTodaysEscalations(context, application.database)
                EscalationScheduler.scheduleDailyRescheduleAlarm(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
