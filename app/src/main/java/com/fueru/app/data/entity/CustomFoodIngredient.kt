package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One ingredient's contribution to a [CustomFood] built via "combine foods" — kept only for
 * display ("what's in this recipe"), not re-read to recompute macros later. The parent
 * [CustomFood]'s own per-100g fields are the authoritative, already-computed values; deleting or
 * changing an ingredient here doesn't retroactively change a recipe already saved.
 */
@Entity(
    tableName = "custom_food_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = CustomFood::class,
            parentColumns = ["id"],
            childColumns = ["customFoodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("customFoodId")],
)
data class CustomFoodIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customFoodId: Long,
    val name: String,
    val grams: Float,
)
