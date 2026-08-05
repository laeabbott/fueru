package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fueru.app.data.entity.ResistanceSession

@Dao
interface ResistanceSessionDao {

    @Insert
    suspend fun insert(session: ResistanceSession): Long

    /** Most recent sessions first — feeds the Summary sparkline and the §6.3 fade-offer check (last 3, all stepsUsed <= 3). */
    @Query("SELECT * FROM resistance_session WHERE practiceId = :practiceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentForPractice(practiceId: Long, limit: Int): List<ResistanceSession>
}
