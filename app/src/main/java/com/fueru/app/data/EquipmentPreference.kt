package com.fueru.app.data

/**
 * Coarse equipment preference asked at onboarding — biases (not filters) the exercise-substitution
 * suggestion list, since strictly filtering could leave a muscle group with zero substitute options.
 */
object EquipmentPreference {
    const val BODYWEIGHT = "bodyweight"
    const val FREE_WEIGHT = "freeWeight"
    const val MACHINES = "machines"
    // null = no preference

    val options = listOf(
        BODYWEIGHT to "Bodyweight",
        FREE_WEIGHT to "Free weight",
        MACHINES to "Machines",
    )

    fun matches(equipment: String, preference: String?): Boolean = when (preference) {
        BODYWEIGHT -> equipment == "body only" || equipment == "other"
        FREE_WEIGHT -> equipment == "barbell" || equipment == "dumbbell" || equipment == "e-z curl bar"
        MACHINES -> equipment == "machine" || equipment == "cable"
        else -> true
    }
}
