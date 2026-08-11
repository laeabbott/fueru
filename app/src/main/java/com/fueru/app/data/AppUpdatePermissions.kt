package com.fueru.app.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * In-app-update round — "install unknown apps" is granted per-calling-app on Android 8+, not
 * globally, so this app specifically needs it before it can launch the system installer for a
 * self-downloaded APK. Requested contextually from Settings, same convention this project already
 * uses for every other special permission — see [com.fueru.app.escalation.EscalationPermissions]
 * for the exact pattern this mirrors.
 */
object AppUpdatePermissions {

    fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Opens the system "allow this app to install unknown apps" screen for this app specifically. */
    fun requestInstallPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
