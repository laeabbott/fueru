package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.ProgramDay
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDayDao {

    @Query("SELECT * FROM program_day WHERE phase = :phase ORDER BY id")
    fun observeByPhase(phase: String): Flow<List<ProgramDay>>

    @Query("SELECT * FROM program_day ORDER BY id")
    fun observeAll(): Flow<List<ProgramDay>>

    @Query("SELECT COUNT(*) FROM program_day")
    suspend fun count(): Int

    @Query("SELECT * FROM program_day WHERE id = :id")
    suspend fun getById(id: Long): ProgramDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<ProgramDay>): List<Long>
}
