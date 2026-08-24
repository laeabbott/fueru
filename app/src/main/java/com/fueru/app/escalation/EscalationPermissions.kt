package com.fueru.app.escalation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Exact-alarm permission is requested contextually (Settings screen), not at launch — same
 * convention this project already uses for calendar/notification permissions. Below API 31 there's
 * nothing to request; exact alarms just work.
 */
object EscalationPermissions {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /** Opens the system "allow exact alarms" screen for this app. No-op below API 31. */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Live exact-alarm permission state, re-checked on every ON_RESUME — same DisposableEffect
 * pattern SettingsAboutScreen's install-permission check already uses, closing what used to be a
 * deliberately-accepted gap here (this screen only re-checked if you left and reopened it
 * entirely; coming straight back from the system "allow exact alarms" screen still showed "not
 * granted" until then). [onNewlyGranted] fires exactly once per false→true transition, so a caller
 * can kick scheduling immediately — a granted-then-forgotten permission would otherwise sit idle
 * until the next full process start's Application.onCreate.
 */
@Composable
fun rememberExactAlarmPermissionGranted(onNewlyGranted: () -> Unit = {}): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(EscalationPermissions.canScheduleExactAlarms(context)) }
    val currentOnNewlyGranted by rememberUpdatedState(onNewlyGranted)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = EscalationPermissions.canScheduleExactAlarms(context)
                if (nowGranted && !granted) currentOnNewlyGranted()
                granted = nowGranted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}
