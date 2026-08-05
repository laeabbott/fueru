package com.fueru.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fueru.app.MainActivity
import com.fueru.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Posts exactly the two notifications promised in onboarding's NotificationPermissionStep copy —
 * "a weekly planning nudge, and the occasional workout reminder. No guilt-trip pings if you miss a
 * day." Never fires anything about a missed/skipped day.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "fueru_nudges"
    private const val PLANNING_NOTIFICATION_ID = 1001
    private const val WORKOUT_NOTIFICATION_ID = 1002

    /** Separate, higher-importance channel for Escalation (brief §7.2 Stages 0-2) — distinct from the gentle planning/workout nudges above, deliberately, since §9 calls out that this is the one place the tone should break from calm. Exposed (not private) so EscalationLockService can post its own full-screen-intent notification on the same channel. */
    const val ESCALATION_CHANNEL_ID = "fueru_escalation"
    private const val ESCALATION_NOTIFICATION_ID_BASE = 2000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Planning & workout nudges",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun ensureEscalationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ESCALATION_CHANNEL_ID,
                "Practice escalation alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Nudges when a scheduled practice goes unmarked — escalates the longer it's ignored."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyPlanNextWeek(context: Context) {
        if (!canPostNotifications(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("plan next week")
            .setContentText("Nothing lined up yet — line up your next arc when you get a sec.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(PLANNING_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyTodaysWorkout(context: Context, dayLabel: String, exerciseCount: Int) {
        if (!canPostNotifications(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(dayLabel)
            .setContentText("$exerciseCount exercises today — whenever you're ready.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(WORKOUT_NOTIFICATION_ID, notification)
    }

    /** Stage 0 (§7.2) — plain notification, tapping opens Resistance Flow for this practice directly (via MainActivity's deep-link extra, see FueruNavGraph). */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyEscalationNudge(context: Context, practiceId: Long, practiceName: String) {
        if (!canPostNotifications(context)) return
        ensureEscalationChannel(context)
        val notification = NotificationCompat.Builder(context, ESCALATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(practiceName)
            .setContentText("still there whenever you're ready")
            .setContentIntent(resistanceFlowPendingIntent(context, practiceId))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(escalationNotificationId(practiceId), notification)
    }

    /** Stage 1 (§7.2) — "escalates to repeating/high-priority, harder to dismiss without opening the app": ongoing (can't be swiped away) + max priority. Android doesn't allow a truly undismissable plain notification, so this is the honest interpretation of that line, not a literal one. */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyEscalationPersist(context: Context, practiceId: Long, practiceName: String) {
        if (!canPostNotifications(context)) return
        ensureEscalationChannel(context)
        val notification = NotificationCompat.Builder(context, ESCALATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(practiceName)
            .setContentText("still waiting on this one")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setContentIntent(resistanceFlowPendingIntent(context, practiceId))
            .build()
        NotificationManagerCompat.from(context).notify(escalationNotificationId(practiceId), notification)
    }

    fun cancelEscalationNotification(context: Context, practiceId: Long) {
        NotificationManagerCompat.from(context).cancel(escalationNotificationId(practiceId))
    }

    /** Stage 3 (§7.2/§7.3) — a stated deadline clock time rather than a live-ticking countdown UI; see the Phase 5 plan's judgment call #1. Only ever fires for a practice with a stick consequence configured (EscalationScheduler only schedules Stage 3/4 alarms in that case). */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyEscalationWarning(context: Context, practiceId: Long, practiceName: String) {
        if (!canPostNotifications(context)) return
        ensureEscalationChannel(context)
        val deadline = Instant.ofEpochMilli(System.currentTimeMillis() + 10 * 60_000L)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
        val notification = NotificationCompat.Builder(context, ESCALATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(practiceName)
            .setContentText("the pledge fires at $deadline unless this is completed")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setContentIntent(resistanceFlowPendingIntent(context, practiceId))
            .build()
        NotificationManagerCompat.from(context).notify(escalationNotificationId(practiceId), notification)
    }

    /** Stage 4 fired and resolved (online, or a queued one that just came back online) — tap opens the pledge screen for this specific ConsequenceEvent. */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyConsequenceReady(context: Context, eventId: Long, practiceName: String, charityName: String) {
        if (!canPostNotifications(context)) return
        ensureEscalationChannel(context)
        val notification = NotificationCompat.Builder(context, ESCALATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("$practiceName — pledge to $charityName")
            .setContentText("tap to complete it")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(consequencePledgePendingIntent(context, eventId))
            .build()
        NotificationManagerCompat.from(context).notify(escalationNotificationId(eventId), notification)
    }

    /** §8.3 — Stage 4 fired while offline. Surfaced clearly rather than retried silently; ConsequenceRetryWorker resolves it (calling notifyConsequenceReady) once connectivity returns. */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    fun notifyConsequenceQueued(context: Context, practiceName: String) {
        if (!canPostNotifications(context)) return
        ensureEscalationChannel(context)
        val notification = NotificationCompat.Builder(context, ESCALATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(practiceName)
            .setContentText("pledge queued — waiting for a connection to show it")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(escalationNotificationId(0L), notification)
    }

    private fun escalationNotificationId(id: Long) = (ESCALATION_NOTIFICATION_ID_BASE + id).toInt()

    private fun resistanceFlowPendingIntent(context: Context, practiceId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_RESISTANCE_FLOW_PRACTICE_ID, practiceId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            practiceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun consequencePledgePendingIntent(context: Context, eventId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_CONSEQUENCE_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            (100_000_000L + eventId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
