package com.fueru.app.escalation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fueru.app.FueruApplication
import com.fueru.app.data.AppLogger
import com.fueru.app.data.PracticeStartStore
import com.fueru.app.notifications.NotificationHelper
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when one of EscalationScheduler's exact alarms goes off. Re-checks "still unlogged today"
 * before doing anything — the gap between scheduling and firing is exactly when the user might
 * have actually logged the practice, and per spec a slot marked complete means no escalation.
 * goAsync() since the unlogged-check is a suspend DB read and onReceive itself must return fast.
 */
class EscalationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val practiceId = intent.getLongExtra(EXTRA_PRACTICE_ID, -1L)
        val practiceName = intent.getStringExtra(EXTRA_PRACTICE_NAME) ?: return
        val stage = intent.getIntExtra(EXTRA_STAGE, -1)
        if (practiceId == -1L || stage == -1) return

        val pendingResult = goAsync()
        val application = context.applicationContext as FueruApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now().toString()
                val alreadyLogged = application.database.practiceLogEntryDao()
                    .getForPracticeAndDate(practiceId, today) != null
                val alreadyStarted = PracticeStartStore.isStarted(context, practiceId, today)
                if (alreadyLogged || alreadyStarted) {
                    AppLogger.log(context, "Escalation", "stage $stage for '$practiceName' suppressed — already logged=$alreadyLogged started=$alreadyStarted")
                    return@launch
                }
                AppLogger.log(context, "Escalation", "stage $stage firing for '$practiceName'")

                when (stage) {
                    STAGE_UPCOMING -> NotificationHelper.notifyEscalationUpcoming(context, practiceId, practiceName)
                    STAGE_NUDGE -> NotificationHelper.notifyEscalationNudge(context, practiceId, practiceName)
                    STAGE_PERSIST -> NotificationHelper.notifyEscalationPersist(context, practiceId, practiceName)
                    STAGE_LOCK -> EscalationLockService.start(context, practiceId, practiceName)
                    STAGE_WARNING -> NotificationHelper.notifyEscalationWarning(context, practiceId, practiceName)
                    STAGE_CONSEQUENCE -> ConsequenceExecutor.fire(context, application.database, practiceId, practiceName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
