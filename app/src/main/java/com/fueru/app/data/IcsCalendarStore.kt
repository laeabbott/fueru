package com.fueru.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.icsDataStore by preferencesDataStore(name = "fueru_ics")
private val KEY_URI = stringPreferencesKey("ics_uri")

/**
 * Persists the user's picked .ics file (Proton Calendar export, etc. — see project memory on why
 * CalendarContract alone isn't enough) as a content URI with a long-lived read grant, so This
 * Week's busy-block check can re-read it on any future day without asking the user to re-pick the
 * file. Preferences DataStore, not a Room table — this is one optional string, not worth a schema
 * version bump.
 */
object IcsCalendarStore {

    suspend fun save(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.icsDataStore.edit { it[KEY_URI] = uri.toString() }
    }

    suspend fun clear(context: Context) {
        context.icsDataStore.edit { it.remove(KEY_URI) }
    }

    suspend fun savedUri(context: Context): Uri? =
        context.icsDataStore.data.first()[KEY_URI]?.let { Uri.parse(it) }

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
