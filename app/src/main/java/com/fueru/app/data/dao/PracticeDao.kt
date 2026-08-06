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

    /** One-shot read — used by the vacation-processing daily pass (PracticeVacation.kt), which isn't inside a composable and doesn't want a live Flow subscription. */
    @Query("SELECT * FROM practice")
    suspend fun getAll(): List<Practice>

    /** Reactive single-row read — PracticeDetailScreen uses this (not getById) so it picks up changes made elsewhere in the same still-alive composable, e.g. Resistance Flow's Summary screen flipping shortFlowEnabled via the §6.3 fade offer, without needing a fresh navigation to re-trigger a one-shot fetch. */
    @Query("SELECT * FROM practice WHERE id = :id")
    fun observeById(id: Long): Flow<Practice?>

    @Query("SELECT * FROM practice WHERE id = :id")
    suspend fun getById(id: Long): Practice?

    /** Used to find the foundational seeded "fuwari" practice (both for the idempotent seed check and by FuwariQuickStartScreen) — practice names aren't otherwise unique/indexed, but there's only ever one row actually looked up by name in this app. */
    @Query("SELECT * FROM practice WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Practice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(practice: Practice): Long

    @Update
    suspend fun update(practice: Practice)
}
