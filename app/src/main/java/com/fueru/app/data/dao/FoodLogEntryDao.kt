package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.fueru.app.data.entity.FoodLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogEntryDao {

    @Query("SELECT * FROM food_log_entry WHERE date = :date ORDER BY timestamp DESC")
    fun observeForDate(date: Long): Flow<List<FoodLogEntry>>

    @Insert
    suspend fun insert(entry: FoodLogEntry): Long

    @Delete
    suspend fun delete(entry: FoodLogEntry)
}
