package com.fueru.app.data

import kotlin.math.roundToInt

/** Pure Mifflin-St Jeor TDEE + macro-split math, per spec Section 6.1. No I/O. */
object TdeeCalculator {

    private const val KG_TO_LB = 2.2046226f

    fun bmr(weightKg: Float, heightCm: Float, age: Int, bmrFormulaVariant: String): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * age
        return if (bmrFormulaVariant == "A") base + 5f else base - 161f
    }

    fun activityMultiplier(activityLevel: String): Float = when (activityLevel) {
        "sedentary" -> 1.2f
        "light" -> 1.375f
        "moderate" -> 1.55f
        "active" -> 1.725f
        "veryActive" -> 1.9f
        else -> 1.2f
    }

    fun tdee(weightKg: Float, heightCm: Float, age: Int, bmrFormulaVariant: String, activityLevel: String): Float =
        bmr(weightKg, heightCm, age, bmrFormulaVariant) * activityMultiplier(activityLevel)

    data class MacroTargets(
        val tdeeKcal: Int,
        val proteinG: Int,
        val fatG: Int,
        val carbG: Int,
    )

    /**
     * [proteinGPerLb] is adjustable in the UI within a 0.7-1.1 g/lb range (spec 6.1); defaults to
     * the 0.9 g/lb used in the source program's own worked example. All grams rounded to nearest 5.
     */
    fun macroTargets(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        bmrFormulaVariant: String,
        activityLevel: String,
        proteinGPerLb: Float = 0.9f,
    ): MacroTargets {
        val tdeeKcal = tdee(weightKg, heightCm, age, bmrFormulaVariant, activityLevel)
        val weightLb = weightKg * KG_TO_LB
        val proteinG = proteinGPerLb * weightLb
        val proteinKcal = proteinG * 4f
        val fatKcal = 0.25f * tdeeKcal
        val fatG = fatKcal / 9f
        val carbKcal = tdeeKcal - proteinKcal - fatKcal
        val carbG = carbKcal / 4f
        return MacroTargets(
            tdeeKcal = tdeeKcal.roundToInt(),
            proteinG = roundTo5(proteinG),
            fatG = roundTo5(fatG),
            carbG = roundTo5(carbG),
        )
    }

    private fun roundTo5(value: Float): Int = (Math.round(value / 5f) * 5)
}
