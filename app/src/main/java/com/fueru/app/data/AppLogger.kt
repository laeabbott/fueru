package com.fueru.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app logging round — the user's own words: "so that I can feed you the logs so that you can
 * see what happened." A plain append-only text file in app-private storage
 * (`filesDir/logs/fueru.log`), exported through Settings via a share sheet (see
 * SettingsCategoryScreens.kt's "Export logs" and the FileProvider entry in AndroidManifest.xml) —
 * no in-app log viewer, since handing it off is the actual stated use case, not reading it here.
 *
 * Deliberately not instrumented everywhere — that would just be noise. Logged from the handful of
 * spots most likely to matter when something's gone wrong and isn't otherwise observable from the
 * UI: escalation scheduling/firing, notifications getting silently blocked by a missing permission,
 * the daily vacation pass, and — the single highest-value entry — every uncaught exception, via
 * [installUncaughtExceptionHandler].
 */
object AppLogger {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "fueru.log"

    /** Once the file passes this size, it's trimmed down to its own tail — cheap rotation without a second file to manage. */
    private const val MAX_BYTES = 512 * 1024L

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun log(context: Context, tag: String, message: String) {
        runCatching {
            val file = logFile(context)
            rotateIfNeeded(file)
            file.appendText("${timestampFormat.format(Date())} [$tag] $message\n")
        }
    }

    fun logError(context: Context, tag: String, message: String, throwable: Throwable) {
        log(context, tag, "$message: ${throwable.stackTraceToString()}")
    }

    /**
     * Wraps whatever uncaught-exception handler is already installed (the system default, which
     * shows the "app has stopped" dialog and reports to Play/ANR tracking) — this only adds a log
     * write in front of it, it never suppresses or replaces the crash itself. Call once, from
     * FueruApplication.onCreate().
     */
    fun installUncaughtExceptionHandler(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { logError(context, "CRASH", "uncaught exception on thread ${thread.name}", throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun logFile(context: Context): File {
        val dir = File(context.filesDir, LOG_DIR).apply { mkdirs() }
        return File(dir, LOG_FILE)
    }

    /**
     * Settings' "Export logs" — the actual stated use case ("feed you the logs"). Uses the
     * FileProvider declared in AndroidManifest.xml (authority "$applicationId.fileprovider",
     * scoped to just this log directory via res/xml/file_paths.xml) to get a content:// URI another
     * app can actually read, then hands it to the system share sheet — whatever the user picks
     * (Messages, email, back into a chat app) receives the file, this doesn't assume which.
     * Returns false (caller shows a message) if there's nothing to export yet.
     */
    fun shareLogFile(context: Context): Boolean {
        val file = logFile(context)
        if (!file.exists() || file.length() == 0L) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "fueru logs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share fueru logs").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() <= MAX_BYTES) return
        val lines = file.readLines()
        file.writeText(lines.takeLast(lines.size / 2).joinToString("\n") + "\n")
    }
}
