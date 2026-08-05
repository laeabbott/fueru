package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise ORDER BY name")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeById(id: String): Flow<Exercise?>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: String): Exercise?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    /** Used by the workout screen's "suggest a different exercise" substitution flow. */
    @Query("SELECT * FROM exercise WHERE primaryMuscle = :primaryMuscle AND id != :excludeId")
    suspend fun getByPrimaryMuscle(primaryMuscle: String, excludeId: String): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)
}
