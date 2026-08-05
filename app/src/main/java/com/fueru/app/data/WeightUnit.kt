package com.fueru.app.data

import java.util.Locale

/**
 * kg is the canonical storage unit everywhere in the data model (UserProfile.bodyWeightKg,
 * SetLog.actualWeight/prescribedWeight, StartingWeightSeed, TdeeCalculator, ...) — this enum is
 * purely a display/input preference, converted at the UI boundary. See WeightUnitStore for where
 * the user's choice is persisted.
 */
enum class WeightUnit(val label: String) {
    KG("kg"),
    LB("lb"),
}

private const val KG_PER_LB = 0.45359237f

fun kgToLb(kg: Float): Float = kg / KG_PER_LB
fun lbToKg(lb: Float): Float = lb * KG_PER_LB

/** Converts a canonical kg value to [unit] for display. */
fun convertToDisplay(kg: Float, unit: WeightUnit): Float = if (unit == WeightUnit.LB) kgToLb(kg) else kg

/** Converts a value already expressed in [unit] (e.g. user input) to kg for storage. */
fun convertToKg(value: Float, unit: WeightUnit): Float = if (unit == WeightUnit.LB) lbToKg(value) else value

/** Whole numbers show with no decimal, otherwise one decimal place — used for both kg and lb display. */
fun formatWeightValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
