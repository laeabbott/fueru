package com.fueru.app.data.seed

/**
 * Starting-weight suggestions by equipment type and self-assessed strength tier (1-5, from the
 * onboarding fitness-level step). Static reference data, not a DB table — the same shape as
 * ExerciseSeed/ProgramSeed. Bodyweight ("body only") and "other" (bands, etc.) equipment have no
 * meaningful starting weight and return null.
 *
 * These numbers only seed a first guess for whoever builds the real set-logging UI (a later
 * phase, per the onboarding plan) — they're not applied anywhere yet in this pass.
 */
object StartingWeightSeed {

    private val kgByEquipmentAndTier: Map<String, Map<Int, Float>> = mapOf(
        "barbell" to mapOf(1 to 20f, 2 to 30f, 3 to 40f, 4 to 60f, 5 to 80f),
        "dumbbell" to mapOf(1 to 4f, 2 to 6f, 3 to 9f, 4 to 14f, 5 to 20f),
        "machine" to mapOf(1 to 10f, 2 to 20f, 3 to 30f, 4 to 45f, 5 to 60f),
        "cable" to mapOf(1 to 5f, 2 to 10f, 3 to 15f, 4 to 25f, 5 to 35f),
    )

    fun startingWeightKg(equipment: String, strengthLevel: Int): Float? {
        val tier = strengthLevel.coerceIn(1, 5)
        return kgByEquipmentAndTier[equipment]?.get(tier)
    }
}
