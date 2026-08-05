package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.RecurringScheduleEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringScheduleDao {

    @Query("SELECT * FROM recurring_schedule_entry ORDER BY dayOfWeek")
    fun observeAll(): Flow<List<RecurringScheduleEntry>>

    @Query("SELECT * FROM recurring_schedule_entry ORDER BY dayOfWeek")
    suspend fun getAll(): List<RecurringScheduleEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RecurringScheduleEntry>)

    @Query("DELETE FROM recurring_schedule_entry")
    suspend fun deleteAll()
}
