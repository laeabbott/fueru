package com.fueru.app.data

import android.content.Context

private const val PREFS_NAME = "fueru_weight_unit"
private const val KEY_UNIT = "unit"

/**
 * Persists the kg/lb display preference. Plain SharedPreferences, not a UserProfile column — this
 * is a UI/input setting, not workout data, so it doesn't need a Room schema bump (which, under this
 * project's current fallbackToDestructiveMigration setup, would wipe local dev data — same
 * reasoning as IcsCalendarStore and WorkoutSessionStore).
 *
 * Defaults to LB: most English-speaking users think in pounds, and — per the user directly — even
 * gyms outside English-speaking countries (their example: Japan) commonly have lb-denominated
 * plates, so lb is the more broadly useful default here.
 */
object WeightUnitStore {

    fun get(context: Context): WeightUnit {
        val stored = prefs(context).getString(KEY_UNIT, null)
        return WeightUnit.entries.find { it.name == stored } ?: WeightUnit.LB
    }

    fun save(context: Context, unit: WeightUnit) {
        prefs(context).edit().putString(KEY_UNIT, unit.name).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_UNIT).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
