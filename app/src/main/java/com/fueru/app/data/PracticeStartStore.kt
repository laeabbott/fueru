package com.fueru.app.data

import android.content.Context

private const val PREFS_NAME = "fueru_practice_start"

/**
 * Scheduling & escalation alignment pass, §D — "I've started" marks a practice as underway for
 * today without writing a [com.fueru.app.data.entity.PracticeLogEntry]. Deliberately plain
 * SharedPreferences, not a new log-entry status: PracticeScoring's targetFor() would silently treat
 * any unrecognized status as a miss, and "started but not yet finished" shouldn't be scored either
 * way until it actually resolves (via a real done/partial/skip/miss). Same "throwaway day-scoped UI
 * state" reasoning as ResistanceFlowPrefs/WorkoutSessionStore.
 */
object PracticeStartStore {

    fun markStarted(context: Context, practiceId: Long, date: String) {
        prefs(context).edit().putBoolean(key(practiceId, date), true).apply()
    }

    fun isStarted(context: Context, practiceId: Long, date: String): Boolean =
        prefs(context).getBoolean(key(practiceId, date), false)

    private fun key(practiceId: Long, date: String) = "${practiceId}_$date"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
