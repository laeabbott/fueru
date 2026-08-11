package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.guidedSessionDefaultDataStore by preferencesDataStore(name = "fueru_guided_session_default")
private val KEY_MINUTES = intPreferencesKey("minutes")
private const val DEFAULT_MINUTES = 45

/**
 * The default guided-session length (minutes) — feeds both the fuwari Home quick-start button and
 * the generic guided-session Commit step's pre-filled duration for any guidedSessionEnabled
 * practice (see ResistanceFlowScreen.kt's GUIDED_DEFAULT_SECONDS), so there's one coherent "default
 * length" concept regardless of entry point. Preferences DataStore, same reasoning as
 * WeightUnitStore — a UI/input setting, not data worth a schema bump.
 */
object GuidedSessionDefaultStore {

    suspend fun getMinutes(context: Context): Int =
        context.guidedSessionDefaultDataStore.data.first()[KEY_MINUTES] ?: DEFAULT_MINUTES

    suspend fun saveMinutes(context: Context, minutes: Int) {
        context.guidedSessionDefaultDataStore.edit { it[KEY_MINUTES] = minutes }
    }
}
