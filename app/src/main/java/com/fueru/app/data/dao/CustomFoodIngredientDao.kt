package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fueru.app.data.entity.CustomFoodIngredient

@Dao
interface CustomFoodIngredientDao {

    @Query("SELECT * FROM custom_food_ingredient WHERE customFoodId = :customFoodId")
    suspend fun getForCustomFood(customFoodId: Long): List<CustomFoodIngredient>

    @Insert
    suspend fun insertAll(ingredients: List<CustomFoodIngredient>)
}
