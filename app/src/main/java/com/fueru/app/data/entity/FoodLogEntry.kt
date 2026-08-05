package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged food item ("macros" mode only — mealBalance mode stays portion-based and doesn't use
 * this table). Macros are snapshotted at log time (computed from the source's per-100g values times
 * [servingGrams] / 100), not re-derived from [fdcId]/[customFoodId] later — so history doesn't
 * shift if the source data changes later, and logging history still displays if USDA is
 * unreachable. Exactly one of [fdcId] (a USDA food) / [customFoodId] (a user-created or
 * combined-recipe food) is set, kept only for traceability — nothing currently reads either back.
 */
@Entity(tableName = "food_log_entry", indices = [Index("date")])
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Midnight-aligned day bucket, same convention as DailyNutritionLog.date. */
    val date: Long,
    val fdcId: Int?,
    val customFoodId: Long? = null,
    val foodName: String,
    val servingGrams: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val kcal: Int,
    val timestamp: Long,
)
