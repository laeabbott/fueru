package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.SetLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {

    @Query("SELECT * FROM set_log WHERE scheduledWorkoutId = :scheduledWorkoutId ORDER BY setNumber")
    fun observeForScheduledWorkout(scheduledWorkoutId: Long): Flow<List<SetLog>>

    /** One-shot read for the "what we accomplished" completion summary — ordered by insertion so exercises group by when they were actually logged. */
    @Query("SELECT * FROM set_log WHERE scheduledWorkoutId = :scheduledWorkoutId ORDER BY id")
    suspend fun getForScheduledWorkout(scheduledWorkoutId: Long): List<SetLog>

    /** Most recent logged set for an exercise. */
    @Query("SELECT * FROM set_log WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastForExercise(exerciseId: String): SetLog?

    /** Every logged set for an exercise, most recent first — the progression algorithm filters this down to the most recent session's rows (those sharing the first row's scheduledWorkoutId). */
    @Query("SELECT * FROM set_log WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    suspend fun getAllForExercise(exerciseId: String): List<SetLog>

    /** Every exercise the user has ever logged a set for — feeds the Progress screen's per-exercise list. */
    @Query("SELECT DISTINCT exerciseId FROM set_log")
    suspend fun getDistinctExerciseIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setLog: SetLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(setLogs: List<SetLog>)
}
