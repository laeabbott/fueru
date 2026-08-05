package com.fueru.app.data

import android.content.Context

/** Snapshot of a finished session's celebration — just which gif got picked. Used to be bundled with a points/streak result before that system was removed; gifs stayed because they're not the gamification the pivot was about, just a nice moment. */
data class WorkoutCelebration(
    val scheduledWorkoutId: Long,
    val gifUrl: String?,
)

/**
 * Persists [WorkoutCelebration] so leaving the Workout tab and coming back later the same day
 * still shows the same gif instead of nothing (the old static "done for today" state) or a
 * freshly re-rolled one. Plain SharedPreferences, not a Room table — this is throwaway,
 * one-day-lifetime UI state, not worth a schema bump, same reasoning as WorkoutSessionStore.
 */
object WorkoutCelebrationStore {
    private const val PREFS_NAME = "workout_celebration"
    private const val KEY_SCHEDULED_WORKOUT_ID = "scheduledWorkoutId"
    private const val KEY_GIF_URL = "gifUrl"

    fun save(context: Context, celebration: WorkoutCelebration) {
        prefs(context).edit()
            .putLong(KEY_SCHEDULED_WORKOUT_ID, celebration.scheduledWorkoutId)
            .putString(KEY_GIF_URL, celebration.gifUrl)
            .apply()
    }

    /** Only returns a match for [scheduledWorkoutId] — a stale save from an earlier completed day shouldn't leak into today's view. */
    fun get(context: Context, scheduledWorkoutId: Long): WorkoutCelebration? {
        val prefs = prefs(context)
        if (prefs.getLong(KEY_SCHEDULED_WORKOUT_ID, -1L) != scheduledWorkoutId) return null
        return WorkoutCelebration(
            scheduledWorkoutId = scheduledWorkoutId,
            gifUrl = prefs.getString(KEY_GIF_URL, null),
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
