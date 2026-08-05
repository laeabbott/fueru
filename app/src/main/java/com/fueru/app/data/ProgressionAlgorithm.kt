package com.fueru.app.data

import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.entity.PrescribedSet
import com.fueru.app.data.entity.UserProfile
import com.fueru.app.data.seed.StartingWeightSeed

/**
 * Auto-regulated weight/rep-target progression, based on guidelines the user supplied directly
 * (2026-07-15, from their own "maximization" reference doc) rather than the original app spec —
 * see HANDOFF.md for the fuller design discussion. Per exercise, per session:
 *
 * - Any set falling short of its target ("shortfallReason" logged) → hold everything steady.
 *   This algorithm only ever holds or advances, **never auto-deloads** — the source doc's own
 *   answer to a stuck plateau is adding a set / cutting rest / adjusting tempo, which is a
 *   deliberately-deferred, human-suggested lever (see WorkoutScreen), not automated here.
 * - No shortfall, but the hardest set logged was RPE 8+ (within ~3 reps of failure, using the
 *   user-confirmed RPE scale: 10=failure, 9=1 left, 8=2 left) → hold steady. Still a good
 *   session, just not evidence there's more in the tank yet.
 * - No shortfall and the hardest set was RPE ≤7 (or RPE wasn't logged at all — defaults to
 *   advancing rather than stalling users who skip RPE, since "no shortfall" alone is already a
 *   decent signal) → advance:
 *   - Below the program's rep ceiling: rep target +1.
 *   - At the ceiling and just cleared it: reset to the program's rep floor, weight goes up.
 *
 * Derived entirely from SetLog history (via SetLogDao.getAllForExercise) — no separate mutable
 * progress table, so there's no second source of truth to drift out of sync with the log itself.
 */
data class ProgressionSuggestion(
    val targetReps: Int,
    val suggestedWeightKg: Float?,
    val weightFromHistory: Boolean,
    val justBumpedWeight: Boolean,
)

/** RPE at or above this counts as "within ~3 reps of failure" per the user-confirmed scale. */
private const val RPE_NEAR_FAILURE = 8

suspend fun suggestProgression(
    database: AppDatabase,
    exercise: Exercise,
    prescribedSet: PrescribedSet,
    profile: UserProfile,
    unit: WeightUnit,
    excludeScheduledWorkoutId: Long,
): ProgressionSuggestion {
    val hasWeight = exerciseHasWeight(exercise.equipment)
    // Excludes the workout currently in progress — without this, sets already logged earlier in
    // *today's* session would be mistaken for "last session" and the suggestion would climb every
    // single set instead of holding steady until the next real session (the bug the user reported).
    val history = database.setLogDao().getAllForExercise(exercise.id)
        .filter { it.scheduledWorkoutId != excludeScheduledWorkoutId }
    val lastSessionId = history.firstOrNull()?.scheduledWorkoutId

    if (lastSessionId == null) {
        val starting = if (hasWeight) StartingWeightSeed.startingWeightKg(exercise.equipment, profile.strengthLevel) else null
        return ProgressionSuggestion(
            targetReps = prescribedSet.repsMin,
            suggestedWeightKg = starting,
            weightFromHistory = false,
            justBumpedWeight = false,
        )
    }

    val lastSession = history.filter { it.scheduledWorkoutId == lastSessionId }
    val lastTargetReps = lastSession.first().prescribedReps
    val lastWeightKg = lastSession.first().actualWeight
    val hadShortfall = lastSession.any { it.shortfallReason != null }
    val maxRpe = lastSession.mapNotNull { it.rpe }.maxOrNull()
    val readyToAdvance = !hadShortfall && (maxRpe == null || maxRpe < RPE_NEAR_FAILURE)

    if (!readyToAdvance) {
        return ProgressionSuggestion(lastTargetReps, lastWeightKg, weightFromHistory = true, justBumpedWeight = false)
    }
    if (lastTargetReps < prescribedSet.repsMax) {
        return ProgressionSuggestion(lastTargetReps + 1, lastWeightKg, weightFromHistory = true, justBumpedWeight = false)
    }
    if (!hasWeight || lastWeightKg == null) {
        // Nothing to bump (bodyweight/band work, or no weight ever logged) — hold at the ceiling.
        return ProgressionSuggestion(lastTargetReps, lastWeightKg, weightFromHistory = true, justBumpedWeight = false)
    }
    return ProgressionSuggestion(
        targetReps = prescribedSet.repsMin,
        suggestedWeightKg = bumpWeight(lastWeightKg, exercise.equipment, unit),
        weightFromHistory = true,
        justBumpedWeight = true,
    )
}

/**
 * Bumps by a clean, loadable increment **in the user's own unit**, then converts back to kg for
 * storage — a literal lb->kg conversion of a "+5lb" bump (≈+2.27kg) wouldn't correspond to a real
 * plate jump on either kind of equipment. Smaller increment for dumbbell/cable (finer-grained
 * equipment), larger for barbell/machine — mirrors StartingWeightSeed's own equipment tiers.
 */
private fun bumpWeight(currentKg: Float, equipment: String, unit: WeightUnit): Float {
    val currentInUnit = convertToDisplay(currentKg, unit)
    val fineGrained = equipment == "dumbbell" || equipment == "cable"
    val increment = when (unit) {
        WeightUnit.LB -> if (fineGrained) 2.5f else 5f
        WeightUnit.KG -> if (fineGrained) 1f else 2.5f
    }
    return convertToKg(currentInUnit + increment, unit)
}
