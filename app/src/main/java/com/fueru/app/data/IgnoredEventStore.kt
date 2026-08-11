package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.ignoredEventDataStore by preferencesDataStore(name = "fueru_ignored_events")
private val KEY_IDS = stringSetPreferencesKey("ignored_ids")

/**
 * Calendar-redesign round — persists which imported/device calendar events the user has dismissed
 * ("ignore" on the weekly scheduling grid) by [BusyBlock.id], so a dismissed event stays gone
 * across re-parses of the same .ics file or re-queries of the device calendar, not just for one
 * screen visit. Same Preferences DataStore shape as IcsCalendarStore — this is display-filtering
 * state, not data worth a schema bump.
 */
object IgnoredEventStore {

    suspend fun getIgnoredIds(context: Context): Set<String> =
        context.ignoredEventDataStore.data.first()[KEY_IDS] ?: emptySet()

    suspend fun ignore(context: Context, id: String) {
        context.ignoredEventDataStore.edit { it[KEY_IDS] = (it[KEY_IDS] ?: emptySet()) + id }
    }

    suspend fun clear(context: Context) {
        context.ignoredEventDataStore.edit { it.remove(KEY_IDS) }
    }
}
