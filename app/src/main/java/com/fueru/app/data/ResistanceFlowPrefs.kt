package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.resistanceFlowDataStore by preferencesDataStore(name = "fueru_resistance_flow")
private const val DEFAULT_TIMER_SECONDS = 120

/**
 * Remembers each practice's last Commit choice (micro-action text + timer duration) so short-flow
 * mode (§6.3 — jump straight to Ignite once shortFlowEnabled) still has something sensible to run
 * with, since that mode skips the Commit step entirely. Preferences DataStore keyed by practiceId,
 * not a Room table — same "throwaway UI-preference state" reasoning as WeightUnitStore.
 */
object ResistanceFlowPrefs {

    suspend fun getMicroAction(context: Context, practiceId: Long, fallback: String?): String =
        context.resistanceFlowDataStore.data.first()[microActionKey(practiceId)] ?: fallback ?: ""

    /** [default] lets a guided-session practice (module round 1, "fuwari") default to a real session length (e.g. 20min) on first use, instead of the generic 2min micro-action timer default. */
    suspend fun getTimerSeconds(context: Context, practiceId: Long, default: Int = DEFAULT_TIMER_SECONDS): Int =
        context.resistanceFlowDataStore.data.first()[timerKey(practiceId)] ?: default

    suspend fun save(context: Context, practiceId: Long, microAction: String, timerSeconds: Int) {
        context.resistanceFlowDataStore.edit {
            it[microActionKey(practiceId)] = microAction
            it[timerKey(practiceId)] = timerSeconds
        }
    }

    private fun microActionKey(practiceId: Long) = stringPreferencesKey("microAction_$practiceId")
    private fun timerKey(practiceId: Long) = intPreferencesKey("timerSeconds_$practiceId")
}
