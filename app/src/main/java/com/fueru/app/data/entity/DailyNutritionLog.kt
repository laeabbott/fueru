package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One upserted row per calendar day, tracking a running tally toward that day's targets — not an
 * itemized food diary. [date] is a midnight-aligned day bucket. Macro-mode fields (gram totals)
 * and meal-balance-mode fields (portion counts) both live here; only the fields matching the
 * user's UserProfile.foodTrackingMode are ever shown or incremented.
 */
@Entity(tableName = "daily_nutrition_log", indices = [Index("date", unique = true)])
data class DailyNutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val proteinPortions: Float = 0f,
    val carbPortions: Float = 0f,
    val fatPortions: Float = 0f,
    val fruitVegPortions: Float = 0f,
)
