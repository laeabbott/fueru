package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.workoutCelebrationDataStore by preferencesDataStore(name = "workout_celebration")
private val KEY_SCHEDULED_WORKOUT_ID = longPreferencesKey("scheduledWorkoutId")
private val KEY_GIF_URL = stringPreferencesKey("gifUrl")

/** Snapshot of a finished session's celebration — just which gif got picked. Used to be bundled with a points/streak result before that system was removed; gifs stayed because they're not the gamification the pivot was about, just a nice moment. */
data class WorkoutCelebration(
    val scheduledWorkoutId: Long,
    val gifUrl: String?,
)

/**
 * Persists [WorkoutCelebration] so leaving the Workout tab and coming back later the same day
 * still shows the same gif instead of nothing (the old static "done for today" state) or a
 * freshly re-rolled one. Preferences DataStore, not a Room table — this is throwaway,
 * one-day-lifetime UI state, not worth a schema bump, same reasoning as WorkoutSessionStore.
 */
object WorkoutCelebrationStore {

    suspend fun save(context: Context, celebration: WorkoutCelebration) {
        context.workoutCelebrationDataStore.edit {
            it[KEY_SCHEDULED_WORKOUT_ID] = celebration.scheduledWorkoutId
            if (celebration.gifUrl != null) it[KEY_GIF_URL] = celebration.gifUrl else it.remove(KEY_GIF_URL)
        }
    }

    /** Only returns a match for [scheduledWorkoutId] — a stale save from an earlier completed day shouldn't leak into today's view. */
    suspend fun get(context: Context, scheduledWorkoutId: Long): WorkoutCelebration? {
        val prefs = context.workoutCelebrationDataStore.data.first()
        if (prefs[KEY_SCHEDULED_WORKOUT_ID] != scheduledWorkoutId) return null
        return WorkoutCelebration(
            scheduledWorkoutId = scheduledWorkoutId,
            gifUrl = prefs[KEY_GIF_URL],
        )
    }
}
