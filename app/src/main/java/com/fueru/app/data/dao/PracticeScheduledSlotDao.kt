package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fueru.app.data.entity.PracticeScheduledSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeScheduledSlotDao {

    @Query("SELECT * FROM practice_scheduled_slot WHERE practiceId = :practiceId ORDER BY dayOfWeek")
    fun observeForPractice(practiceId: Long): Flow<List<PracticeScheduledSlot>>

    /** Every slot, across every practice — feeds PracticeScheduler.computeTodaysPracticePlan, which filters down to today's dayOfWeek itself. */
    @Query("SELECT * FROM practice_scheduled_slot")
    suspend fun getAll(): List<PracticeScheduledSlot>

    /** Reactive twin of [getAll] — This Week's grid renders every practice's slots live alongside workouts. */
    @Query("SELECT * FROM practice_scheduled_slot")
    fun observeAll(): Flow<List<PracticeScheduledSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<PracticeScheduledSlot>)

    @Query("DELETE FROM practice_scheduled_slot WHERE practiceId = :practiceId")
    suspend fun deleteForPractice(practiceId: Long)

    /** This Week's grid un-schedules one slot at a time (tapping a single placed block), unlike Edit Schedule's wholesale [replaceForPractice]. */
    @Query("DELETE FROM practice_scheduled_slot WHERE id = :slotId")
    suspend fun deleteById(slotId: Long)

    /** Replaces a practice's whole slot set atomically — "Save schedule" always means "this is the full new set," never an incremental patch. */
    @Transaction
    suspend fun replaceForPractice(practiceId: Long, slots: List<PracticeScheduledSlot>) {
        deleteForPractice(practiceId)
        if (slots.isNotEmpty()) insertAll(slots)
    }
}
