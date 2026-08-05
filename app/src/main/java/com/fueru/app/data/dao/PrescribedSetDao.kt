package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.PrescribedSet
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescribedSetDao {

    @Query("SELECT * FROM prescribed_set WHERE programDayId = :programDayId ORDER BY orderInDay")
    fun observeForProgramDay(programDayId: Long): Flow<List<PrescribedSet>>

    @Query("SELECT * FROM prescribed_set WHERE programDayId = :programDayId ORDER BY orderInDay")
    suspend fun getForProgramDay(programDayId: Long): List<PrescribedSet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<PrescribedSet>)
}
