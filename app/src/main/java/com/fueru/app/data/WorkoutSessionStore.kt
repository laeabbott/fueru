package com.fueru.app.data

import android.content.Context

private const val PREFS_NAME = "fueru_workout_session"
private const val KEY_SCHEDULED_WORKOUT_ID = "scheduled_workout_id"
private const val KEY_SLOT_INDEX = "slot_index"
private const val KEY_SET_NUMBER = "set_number"

/**
 * Persists in-progress Workout session position so leaving mid-session (back button, bottom nav
 * tap, process death) and coming back resumes where you left off instead of restarting at set 1
 * and risking duplicate SetLog rows for exercises already finished. Plain SharedPreferences, not a
 * Room table — this is transient UI state that should vanish the moment a session finishes or a
 * new one starts for a different day, not something that belongs in the durable data model (and a
 * Room table would mean a schema version bump, which under this project's current
 * fallbackToDestructiveMigration setup wipes all local dev data — see IcsCalendarStore for the
 * same reasoning applied to the .ics import feature).
 *
 * Known gap: does not remember mid-session exercise substitutions — resuming rebuilds the session
 * from the program's original exercise list, so a substitution chosen for a not-yet-logged exercise
 * is lost if you leave before logging its first set. Already-logged sets are unaffected either way,
 * since SetLog rows capture whichever exercise id was actually used at the time.
 */
data class WorkoutSessionProgress(val scheduledWorkoutId: Long, val slotIndex: Int, val setNumber: Int)

object WorkoutSessionStore {

    fun save(context: Context, progress: WorkoutSessionProgress) {
        prefs(context).edit()
            .putLong(KEY_SCHEDULED_WORKOUT_ID, progress.scheduledWorkoutId)
            .putInt(KEY_SLOT_INDEX, progress.slotIndex)
            .putInt(KEY_SET_NUMBER, progress.setNumber)
            .apply()
    }

    /** Only returns a result if it matches [scheduledWorkoutId] — a saved position for a different (stale, e.g. yesterday's) workout is discarded rather than resumed. */
    fun resumeFor(context: Context, scheduledWorkoutId: Long): WorkoutSessionProgress? {
        val p = prefs(context)
        val savedId = p.getLong(KEY_SCHEDULED_WORKOUT_ID, -1L)
        if (savedId != scheduledWorkoutId) return null
        return WorkoutSessionProgress(
            scheduledWorkoutId = savedId,
            slotIndex = p.getInt(KEY_SLOT_INDEX, 0),
            setNumber = p.getInt(KEY_SET_NUMBER, 1),
        )
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
