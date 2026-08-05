package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_log",
    foreignKeys = [
        ForeignKey(
            entity = ScheduledWorkout::class,
            parentColumns = ["id"],
            childColumns = ["scheduledWorkoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scheduledWorkoutId"), Index("exerciseId")],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledWorkoutId: Long,
    val exerciseId: String,
    val setNumber: Int,
    val prescribedWeight: Float?,
    val prescribedReps: Int,
    val actualWeight: Float?,
    val actualReps: Int?,
    /** null, "couldnt", or "choseNotTo" — only asked when actual < prescribed. */
    val shortfallReason: String?,
    /** Optional quick-select 6-10. */
    val rpe: Int?,
    val timestamp: Long,
)
