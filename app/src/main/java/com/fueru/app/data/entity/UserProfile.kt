package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton row (id is always 0) — this is a single-user, local-first app. */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    /** What Home greets the user by — replaces the placeholder "nakama". */
    val displayName: String,
    val heightCm: Float,
    val bodyWeightKg: Float?,
    val weightIsEstimated: Boolean,
    val age: Int,
    /** "A" (Mifflin-St Jeor +5 constant) or "B" (-161 constant) — user-selected, see spec Section 7.2. */
    val bmrFormulaVariant: String,
    /** sedentary / light / moderate / active / veryActive */
    val activityLevel: String,
    /** "buildMuscle" or "maintain" — feeds TdeeCalculator's surplus decision. */
    val goal: String,
    /** 1 ("could be mistaken for a skeleton") to 5 ("swol like yo mama") — feeds StartingWeightSeed. */
    val strengthLevel: Int,
    val foodTrackingEnabled: Boolean,
    /** "macros" or "mealBalance" — null when food tracking is off. */
    val foodTrackingMode: String?,
    /** "bodyweight" / "freeWeight" / "machines", or null for no preference — see data/EquipmentPreference.kt. Biases (doesn't strictly filter) exercise-substitution suggestions. */
    val equipmentPreference: String?,
    /** If true, ScheduledWorkouts are auto-filled each week from RecurringScheduleEntry. */
    val useRecurringSchedule: Boolean,
    val programStartDate: Long,
    /** "0-6", "6-12", or "12-24" */
    val currentPhase: String,
)
