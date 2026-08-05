package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.DailyNutritionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNutritionLogDao {

    @Query("SELECT * FROM daily_nutrition_log WHERE date = :date LIMIT 1")
    fun observeForDate(date: Long): Flow<DailyNutritionLog?>

    @Query("SELECT * FROM daily_nutrition_log WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: Long): DailyNutritionLog?

    /** Upserts by [DailyNutritionLog.date]'s unique index — replaces any existing row for that day. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyNutritionLog)
}
