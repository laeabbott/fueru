package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fueru.app.data.entity.CustomFood
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFoodDao {

    @Query("SELECT * FROM custom_food ORDER BY name")
    fun observeAll(): Flow<List<CustomFood>>

    /** Simple case-insensitive name filter — dataset is user-created and small, no need for a real search index. */
    @Query("SELECT * FROM custom_food WHERE name LIKE '%' || :query || '%' ORDER BY name")
    suspend fun search(query: String): List<CustomFood>

    @Query("SELECT * FROM custom_food WHERE id = :id")
    suspend fun getById(id: Long): CustomFood?

    @Insert
    suspend fun insert(food: CustomFood): Long

    /** Custom-food-editing round. */
    @Update
    suspend fun update(food: CustomFood)
}
