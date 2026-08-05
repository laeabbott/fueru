package com.fueru.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "fueru_ics"
private const val KEY_URI = "ics_uri"

/**
 * Persists the user's picked .ics file (Proton Calendar export, etc. — see project memory on why
 * CalendarContract alone isn't enough) as a content URI with a long-lived read grant, so This
 * Week's busy-block check can re-read it on any future day without asking the user to re-pick the
 * file. Plain SharedPreferences, not a Room table — this is one optional string, not worth a
 * schema version bump (which would also destructively wipe local dev data under the current
 * fallbackToDestructiveMigration setup).
 */
object IcsCalendarStore {

    fun save(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs(context).edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_URI).apply()
    }

    fun savedUri(context: Context): Uri? =
        prefs(context).getString(KEY_URI, null)?.let { Uri.parse(it) }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Busy blocks from the imported .ics file (if any) overlapping the 24h window starting at
     * [dayStartMillis]. Silently empty if nothing's imported, or if the file can no longer be read
     * (moved/deleted/permission revoked since) — a stale import shouldn't crash This Week, so
     * every failure mode here is deliberately swallowed rather than surfaced.
     */
    suspend fun busyBlocksForDay(context: Context, dayStartMillis: Long): List<BusyBlock> = withContext(Dispatchers.IO) {
        val uri = savedUri(context) ?: return@withContext emptyList()
        val dayEndMillis = dayStartMillis + 86_400_000L
        val events = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                IcsParser.parse(stream.readBytes().toString(Charsets.UTF_8))
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        events.filter { it.startMillis < dayEndMillis && it.endMillis > dayStartMillis }
    }
}
