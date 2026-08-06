package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.PracticeLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeLogEntryDao {

    /** Feeds the heatmap + decay score for one practice's detail screen — full history, oldest first, PracticeScoring walks it in that order. */
    @Query("SELECT * FROM practice_log_entry WHERE practiceId = :practiceId ORDER BY date")
    fun observeForPractice(practiceId: Long): Flow<List<PracticeLogEntry>>

    @Query("SELECT * FROM practice_log_entry WHERE practiceId = :practiceId AND date = :date LIMIT 1")
    suspend fun getForPracticeAndDate(practiceId: Long, date: String): PracticeLogEntry?

    /** Every entry from [sinceDateIso] (inclusive) onward — feeds the weekly-target-met-early check, scheduling & escalation alignment pass §E. Plain string comparison works since dates are always "YYYY-MM-DD". */
    @Query("SELECT * FROM practice_log_entry WHERE practiceId = :practiceId AND date >= :sinceDateIso")
    suspend fun getForPracticeSince(practiceId: Long, sinceDateIso: String): List<PracticeLogEntry>

    /** REPLACE relies on the (practiceId, date) unique index — re-logging a day overwrites cleanly instead of duplicating. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PracticeLogEntry)
}
