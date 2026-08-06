package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fueru.app.data.entity.ScheduledWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledWorkoutDao {

    @Query("SELECT * FROM scheduled_workout WHERE weekStartDate = :weekStartDate ORDER BY scheduledDate")
    fun observeForWeek(weekStartDate: Long): Flow<List<ScheduledWorkout>>

    @Query("SELECT * FROM scheduled_workout WHERE scheduledDate = :date LIMIT 1")
    suspend fun getForDate(date: Long): ScheduledWorkout?

    @Query("SELECT * FROM scheduled_workout WHERE scheduledDate > :afterDate AND status = 'planned' ORDER BY scheduledDate LIMIT 1")
    suspend fun getNextPlannedAfter(afterDate: Long): ScheduledWorkout?

    @Query("SELECT * FROM scheduled_workout WHERE id = :id")
    suspend fun getById(id: Long): ScheduledWorkout?

    /** Feeds the Progress screen's recent-sessions list. */
    @Query("SELECT * FROM scheduled_workout WHERE status = 'completed' ORDER BY completedDate DESC")
    suspend fun getAllCompleted(): List<ScheduledWorkout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: ScheduledWorkout): Long

    @Update
    suspend fun update(workout: ScheduledWorkout)

    /** Calendar-redesign round — tapping an already-placed block on the weekly grid un-schedules it, returning that ProgramDay to the unscheduled pool. */
    @Delete
    suspend fun delete(workout: ScheduledWorkout)
}
