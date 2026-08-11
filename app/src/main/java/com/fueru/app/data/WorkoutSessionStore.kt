package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.workoutSessionDataStore by preferencesDataStore(name = "fueru_workout_session")
private val KEY_SCHEDULED_WORKOUT_ID = longPreferencesKey("scheduled_workout_id")
private val KEY_SLOT_INDEX = intPreferencesKey("slot_index")
private val KEY_SET_NUMBER = intPreferencesKey("set_number")

/**
 * Persists in-progress Workout session position so leaving mid-session (back button, bottom nav
 * tap, process death) and coming back resumes where you left off instead of restarting at set 1
 * and risking duplicate SetLog rows for exercises already finished. Preferences DataStore, not a
 * Room table — this is transient UI state that should vanish the moment a session finishes or a
 * new one starts for a different day, not something that belongs in the durable data model.
 *
 * Known gap: does not remember mid-session exercise substitutions — resuming rebuilds the session
 * from the program's original exercise list, so a substitution chosen for a not-yet-logged exercise
 * is lost if you leave before logging its first set. Already-logged sets are unaffected either way,
 * since SetLog rows capture whichever exercise id was actually used at the time.
 */
data class WorkoutSessionProgress(val scheduledWorkoutId: Long, val slotIndex: Int, val setNumber: Int)

object WorkoutSessionStore {

    suspend fun save(context: Context, progress: WorkoutSessionProgress) {
        context.workoutSessionDataStore.edit {
            it[KEY_SCHEDULED_WORKOUT_ID] = progress.scheduledWorkoutId
            it[KEY_SLOT_INDEX] = progress.slotIndex
            it[KEY_SET_NUMBER] = progress.setNumber
        }
    }

    /** Only returns a result if it matches [scheduledWorkoutId] — a saved position for a different (stale, e.g. yesterday's) workout is discarded rather than resumed. */
    suspend fun resumeFor(context: Context, scheduledWorkoutId: Long): WorkoutSessionProgress? {
        val p = context.workoutSessionDataStore.data.first()
        val savedId = p[KEY_SCHEDULED_WORKOUT_ID] ?: return null
        if (savedId != scheduledWorkoutId) return null
        return WorkoutSessionProgress(
            scheduledWorkoutId = savedId,
            slotIndex = p[KEY_SLOT_INDEX] ?: 0,
            setNumber = p[KEY_SET_NUMBER] ?: 1,
        )
    }

    suspend fun clear(context: Context) {
        context.workoutSessionDataStore.edit { it.clear() }
    }
}
