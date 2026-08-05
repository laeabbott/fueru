package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fueru.app.data.entity.ConsequenceEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsequenceEventDao {

    @Insert
    suspend fun insert(event: ConsequenceEvent): Long

    @Update
    suspend fun update(event: ConsequenceEvent)

    /** Feeds §7.3's weekly glad->resent escalation rule — count of firings for this practice since [weekStartMillis] (Monday-start, see DateUtils.startOfWeek). */
    @Query("SELECT * FROM consequence_event WHERE practiceId = :practiceId AND timestamp >= :weekStartMillis ORDER BY timestamp")
    suspend fun getForPracticeSinceWeekStart(practiceId: Long, weekStartMillis: Long): List<ConsequenceEvent>

    @Query("SELECT * FROM consequence_event WHERE id = :id")
    suspend fun getById(id: Long): ConsequenceEvent?

    /** Still-queued firings (§8.3) — ConsequenceRetryWorker resolves these once connectivity returns. */
    @Query("SELECT * FROM consequence_event WHERE queued = 1")
    suspend fun getQueued(): List<ConsequenceEvent>

    @Query("SELECT * FROM consequence_event WHERE practiceId = :practiceId ORDER BY timestamp DESC LIMIT 1")
    fun observeMostRecentForPractice(practiceId: Long): Flow<ConsequenceEvent?>
}
