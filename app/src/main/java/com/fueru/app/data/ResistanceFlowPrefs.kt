package com.fueru.app.data

import android.content.Context

private const val PREFS_NAME = "fueru_resistance_flow"
private const val DEFAULT_TIMER_SECONDS = 120

/**
 * Remembers each practice's last Commit choice (micro-action text + timer duration) so short-flow
 * mode (§6.3 — jump straight to Ignite once shortFlowEnabled) still has something sensible to run
 * with, since that mode skips the Commit step entirely. Plain SharedPreferences keyed by
 * practiceId, not a Room table — same "throwaway UI-preference state" reasoning as WeightUnitStore.
 */
object ResistanceFlowPrefs {

    fun getMicroAction(context: Context, practiceId: Long, fallback: String?): String =
        prefs(context).getString(microActionKey(practiceId), null) ?: fallback ?: ""

    /** [default] lets a guided-session practice (module round 1, "fuwari") default to a real session length (e.g. 20min) on first use, instead of the generic 2min micro-action timer default. */
    fun getTimerSeconds(context: Context, practiceId: Long, default: Int = DEFAULT_TIMER_SECONDS): Int =
        prefs(context).getInt(timerKey(practiceId), default)

    fun save(context: Context, practiceId: Long, microAction: String, timerSeconds: Int) {
        prefs(context).edit()
            .putString(microActionKey(practiceId), microAction)
            .putInt(timerKey(practiceId), timerSeconds)
            .apply()
    }

    private fun microActionKey(practiceId: Long) = "microAction_$practiceId"
    private fun timerKey(practiceId: Long) = "timerSeconds_$practiceId"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
