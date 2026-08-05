package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.ScheduledWorkoutExerciseOverride

@Dao
interface ScheduledWorkoutExerciseOverrideDao {

    @Query("SELECT * FROM scheduled_workout_exercise_override WHERE scheduledWorkoutId = :scheduledWorkoutId")
    suspend fun getForScheduledWorkout(scheduledWorkoutId: Long): List<ScheduledWorkoutExerciseOverride>

    /** Replaces any existing override for the same (scheduledWorkoutId, prescribedSetId) pair, via the unique index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: ScheduledWorkoutExerciseOverride)
}
