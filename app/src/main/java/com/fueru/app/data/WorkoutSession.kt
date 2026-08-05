package com.fueru.app.data

import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.entity.PrescribedSet
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.data.entity.ScheduledWorkoutExerciseOverride

/** One exercise slot in an active session. [exercise] can change mid-session via substitution. */
data class WorkoutSlot(
    val prescribedSet: PrescribedSet,
    val exercise: Exercise,
)

data class WorkoutSessionPlan(
    val scheduledWorkout: ScheduledWorkout,
    val dayLabel: String,
    val slots: List<WorkoutSlot>,
)

/**
 * Builds the ordered exercise plan for a scheduled workout's program day, joining in each Exercise
 * row — applying any persisted per-slot substitution from ScheduledWorkoutExerciseOverride first,
 * falling back to the program's own prescribed exercise otherwise.
 */
suspend fun loadWorkoutSessionPlan(database: AppDatabase, scheduledWorkout: ScheduledWorkout): WorkoutSessionPlan? {
    val programDay = database.programDayDao().getById(scheduledWorkout.programDayId) ?: return null
    val prescribedSets = database.prescribedSetDao().getForProgramDay(programDay.id)
    val overridesBySlot = database.scheduledWorkoutExerciseOverrideDao()
        .getForScheduledWorkout(scheduledWorkout.id)
        .associateBy { it.prescribedSetId }
    val slots = prescribedSets.mapNotNull { set ->
        val exerciseId = overridesBySlot[set.id]?.substituteExerciseId ?: set.exerciseId
        database.exerciseDao().getById(exerciseId)?.let { WorkoutSlot(set, it) }
    }
    return WorkoutSessionPlan(scheduledWorkout, programDay.dayLabel, slots)
}

/**
 * Alternative exercises sharing the same primary muscle, for the "suggest a different exercise"
 * flow — sorted (not filtered) so exercises matching [equipmentPreference] come first, since
 * strictly filtering could leave zero options for some muscle groups. Capped at 30 so the picker
 * stays a reasonable length now that the catalog can be much larger than the original lean set.
 */
suspend fun loadSubstitutes(database: AppDatabase, exercise: Exercise, equipmentPreference: String?): List<Exercise> =
    database.exerciseDao().getByPrimaryMuscle(exercise.primaryMuscle, exercise.id)
        .sortedByDescending { EquipmentPreference.matches(it.equipment, equipmentPreference) }
        .take(30)

/** Persists a substitution for one slot of one scheduled workout — survives leaving/resuming the session and shows up for future previews of the same day. */
suspend fun saveExerciseOverride(database: AppDatabase, scheduledWorkoutId: Long, prescribedSetId: Long, substituteExerciseId: String) {
    database.scheduledWorkoutExerciseOverrideDao().upsert(
        ScheduledWorkoutExerciseOverride(
            scheduledWorkoutId = scheduledWorkoutId,
            prescribedSetId = prescribedSetId,
            substituteExerciseId = substituteExerciseId,
        ),
    )
}

private val noWeightEquipment = setOf("body only", "other", "foam roll", "bands", "exercise ball")

/** Equipment with no meaningful working weight (see StartingWeightSeed) — reps-only logging for these. */
fun exerciseHasWeight(equipment: String): Boolean = equipment !in noWeightEquipment
