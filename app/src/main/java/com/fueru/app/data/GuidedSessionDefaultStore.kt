package com.fueru.app.data

import android.content.Context

private const val PREFS_NAME = "fueru_guided_session_default"
private const val KEY_MINUTES = "minutes"
private const val DEFAULT_MINUTES = 45

/**
 * The default guided-session length (minutes) — feeds both the fuwari Home quick-start button and
 * the generic guided-session Commit step's pre-filled duration for any guidedSessionEnabled
 * practice (see ResistanceFlowScreen.kt's GUIDED_DEFAULT_SECONDS), so there's one coherent "default
 * length" concept regardless of entry point. Plain SharedPreferences, same reasoning as
 * WeightUnitStore — a UI/input setting, not data worth a schema bump.
 */
object GuidedSessionDefaultStore {

    fun getMinutes(context: Context): Int = prefs(context).getInt(KEY_MINUTES, DEFAULT_MINUTES)

    fun saveMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_MINUTES, minutes).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
