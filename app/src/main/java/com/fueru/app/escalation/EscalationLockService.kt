package com.fueru.app.escalation

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fueru.app.R
import com.fueru.app.notifications.NotificationHelper

/**
 * Stage 2 (§7.2) — the hard lock, "the floor," needs no external dependency. A short-lived
 * foreground service whose only job is to post a full-screen-intent notification (the modern
 * replacement for SYSTEM_ALERT_WINDOW — see the Phase 4 plan's "one deliberate divergence" note)
 * and directly launch EscalationLockActivity as a belt-and-suspenders measure, since a full-screen
 * intent isn't guaranteed to auto-launch when the app is already in the foreground.
 */
class EscalationLockService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val practiceId = intent?.getLongExtra(EXTRA_PRACTICE_ID, -1L) ?: -1L
        val practiceName = intent?.getStringExtra(EXTRA_PRACTICE_NAME)
        if (practiceId == -1L || practiceName == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        NotificationHelper.ensureEscalationChannel(this)
        val lockIntent = EscalationLockActivity.intentFor(this, practiceId, practiceName)
        val lockPendingIntent = PendingIntent.getActivity(
            this,
            practiceId.toInt(),
            lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, NotificationHelper.ESCALATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(practiceName)
            .setContentText("locked until you start the flow")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(lockPendingIntent, true)
            .setContentIntent(lockPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(LOCK_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(LOCK_NOTIFICATION_ID, notification)
        }

        startActivity(lockIntent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val LOCK_NOTIFICATION_ID = 3000

        fun start(context: Context, practiceId: Long, practiceName: String) {
            val intent = Intent(context, EscalationLockService::class.java).apply {
                putExtra(EXTRA_PRACTICE_ID, practiceId)
                putExtra(EXTRA_PRACTICE_NAME, practiceName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Called once EscalationLockActivity signals Ignite was reached (see ResistanceFlowScreen's onIgnited). */
        fun stop(context: Context) {
            context.stopService(Intent(context, EscalationLockService::class.java))
        }
    }
}
