package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fueru.app.data.entity.GuidedSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GuidedSessionDao {

    @Insert
    suspend fun insert(session: GuidedSession): Long

    /**
     * Reactive, not one-shot — [PracticeDetailScreen]'s composition survives underneath
     * ResistanceFlowScreen when the user navigates into a session and back (same shape as the
     * Phase 3 "stale practice data" bug fixed via PracticeDao.observeById; this sidesteps it from
     * the start instead of reintroducing it).
     */
    @Query("SELECT * FROM guided_session WHERE practiceId = :practiceId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentForPractice(practiceId: Long, limit: Int): Flow<List<GuidedSession>>

    /** Distinct, most-recently-used first — feeds the Commit step's session-type quick-pick chips. One-shot is fine here since ResistanceFlowScreen is a fresh composable instance every time it's navigated to. */
    @Query("SELECT DISTINCT sessionType FROM guided_session WHERE practiceId = :practiceId ORDER BY timestamp DESC LIMIT 8")
    suspend fun getRecentTypesForPractice(practiceId: Long): List<String>
}
