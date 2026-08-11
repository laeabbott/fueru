package com.fueru.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.weightUnitDataStore by preferencesDataStore(name = "fueru_weight_unit")
private val KEY_UNIT = stringPreferencesKey("unit")

/**
 * Persists the kg/lb display preference. Preferences DataStore, not a UserProfile column — this
 * is a UI/input setting, not workout data, so it doesn't need a Room schema bump.
 *
 * Defaults to LB: most English-speaking users think in pounds, and — per the user directly — even
 * gyms outside English-speaking countries (their example: Japan) commonly have lb-denominated
 * plates, so lb is the more broadly useful default here.
 */
object WeightUnitStore {

    suspend fun get(context: Context): WeightUnit {
        val stored = context.weightUnitDataStore.data.first()[KEY_UNIT]
        return WeightUnit.entries.find { it.name == stored } ?: WeightUnit.LB
    }

    suspend fun save(context: Context, unit: WeightUnit) {
        context.weightUnitDataStore.edit { it[KEY_UNIT] = unit.name }
    }

    suspend fun clear(context: Context) {
        context.weightUnitDataStore.edit { it.remove(KEY_UNIT) }
    }
}
