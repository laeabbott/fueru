package com.fueru.app.data

import android.content.Context

private const val PREFS_NAME = "fueru_ignored_events"
private const val KEY_IDS = "ignored_ids"

/**
 * Calendar-redesign round — persists which imported/device calendar events the user has dismissed
 * ("ignore" on the weekly scheduling grid) by [BusyBlock.id], so a dismissed event stays gone
 * across re-parses of the same .ics file or re-queries of the device calendar, not just for one
 * screen visit. Same plain-SharedPreferences shape as IcsCalendarStore — this is display-filtering
 * state, not data worth a schema bump.
 */
object IgnoredEventStore {

    fun getIgnoredIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_IDS, emptySet()) ?: emptySet()

    fun ignore(context: Context, id: String) {
        val current = getIgnoredIds(context)
        prefs(context).edit().putStringSet(KEY_IDS, current + id).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_IDS).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
