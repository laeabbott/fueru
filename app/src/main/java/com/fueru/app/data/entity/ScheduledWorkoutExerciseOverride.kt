package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A per-scheduled-workout exercise substitution that actually persists — unlike the program's own
 * [PrescribedSet.exerciseId], which is shared across every occurrence of that program day and must
 * never be mutated by a one-off swap. One row per (scheduledWorkoutId, prescribedSetId) pair; the
 * unique index lets an upsert (OnConflictStrategy.REPLACE) cleanly replace a prior substitution for
 * the same slot instead of accumulating duplicates.
 */
@Entity(
    tableName = "scheduled_workout_exercise_override",
    foreignKeys = [
        ForeignKey(
            entity = ScheduledWorkout::class,
            parentColumns = ["id"],
            childColumns = ["scheduledWorkoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PrescribedSet::class,
            parentColumns = ["id"],
            childColumns = ["prescribedSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["substituteExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("scheduledWorkoutId"),
        Index("prescribedSetId"),
        Index(value = ["scheduledWorkoutId", "prescribedSetId"], unique = true),
    ],
)
data class ScheduledWorkoutExerciseOverride(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledWorkoutId: Long,
    val prescribedSetId: Long,
    val substituteExerciseId: String,
)
