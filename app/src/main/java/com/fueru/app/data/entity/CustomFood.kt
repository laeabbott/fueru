package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created food, alongside USDA results as a loggable source. Two ways to end up here:
 * entered directly (manual per-100g macros) or built from [CustomFoodIngredient] rows via the
 * "combine foods" recipe flow — both are just a plain [CustomFood] row once saved, with no
 * distinction at the logging layer, since a recipe's whole point is to become an ordinary reusable
 * food ("chicken tacos") rather than something you rebuild every time.
 */
@Entity(tableName = "custom_food")
data class CustomFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val kcalPer100g: Int,
    val createdAt: Long,
)
