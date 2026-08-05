package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fueru.app.data.entity.Practice
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    @Query("SELECT * FROM practice ORDER BY name")
    fun observeAll(): Flow<List<Practice>>

    /** Reactive single-row read — PracticeDetailScreen uses this (not getById) so it picks up changes made elsewhere in the same still-alive composable, e.g. Resistance Flow's Summary screen flipping shortFlowEnabled via the §6.3 fade offer, without needing a fresh navigation to re-trigger a one-shot fetch. */
    @Query("SELECT * FROM practice WHERE id = :id")
    fun observeById(id: Long): Flow<Practice?>

    @Query("SELECT * FROM practice WHERE id = :id")
    suspend fun getById(id: Long): Practice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(practice: Practice): Long

    @Update
    suspend fun update(practice: Practice)
}
