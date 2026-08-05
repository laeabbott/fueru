package com.fueru.app.data

/**
 * Body-type weight estimator, spec Section 7.3. BMI is used only internally to seed a starting
 * TDEE number — it's never shown to the user or used to comment on their body.
 */
enum class BodyType(val label: String, private val bmiMidpoint: Float) {
    LEANER("Leaner / smaller frame", 20f),
    AVERAGE("Average build", 23.5f),
    BROADER("Broader or more muscular frame", 27f),
    LARGER("Larger frame", 31f),
    ;

    fun estimateWeightKg(heightCm: Float): Float {
        val heightM = heightCm / 100f
        return bmiMidpoint * heightM * heightM
    }
}
