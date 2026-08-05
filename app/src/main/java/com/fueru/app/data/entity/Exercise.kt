package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [imageAssetPaths] holds paths into assets/exercises/<id>/ — free-exercise-db ships two static
 * JPGs per exercise (start/end position), not an animated GIF, despite the original spec calling
 * this field gifAssetPath. The workout screen (follow-up phase) cross-fades between the two.
 */
@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val imageAssetPaths: List<String>,
    val instructions: String,
)
