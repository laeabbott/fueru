package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.practiceStartDataStore by preferencesDataStore(name = "fueru_practice_start")

/**
 * Scheduling & escalation alignment pass, §D — "I've started" marks a practice as underway for
 * today without writing a [com.fueru.app.data.entity.PracticeLogEntry]. Deliberately not a new
 * log-entry status: PracticeScoring's targetFor() would silently treat any unrecognized status as
 * a miss, and "started but not yet finished" shouldn't be scored either way until it actually
 * resolves (via a real done/partial/skip/miss). Same "throwaway day-scoped UI state" reasoning as
 * ResistanceFlowPrefs/WorkoutSessionStore — Preferences DataStore, keyed dynamically per
 * (practiceId, date) since it's one flag per practice per day, never pruned.
 */
object PracticeStartStore {

    suspend fun markStarted(context: Context, practiceId: Long, date: String) {
        context.practiceStartDataStore.edit { it[key(practiceId, date)] = true }
    }

    suspend fun isStarted(context: Context, practiceId: Long, date: String): Boolean =
        context.practiceStartDataStore.data.first()[key(practiceId, date)] ?: false

    private fun key(practiceId: Long, date: String) = booleanPreferencesKey("${practiceId}_$date")
}
