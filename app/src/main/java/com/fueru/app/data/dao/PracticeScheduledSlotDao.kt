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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<PracticeScheduledSlot>)

    @Query("DELETE FROM practice_scheduled_slot WHERE practiceId = :practiceId")
    suspend fun deleteForPractice(practiceId: Long)

    /** Replaces a practice's whole slot set atomically — "Save schedule" always means "this is the full new set," never an incremental patch. */
    @Transaction
    suspend fun replaceForPractice(practiceId: Long, slots: List<PracticeScheduledSlot>) {
        deleteForPractice(practiceId)
        if (slots.isNotEmpty()) insertAll(slots)
    }
}
