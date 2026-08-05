package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_workout",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDay::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programDayId")],
)
data class ScheduledWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartDate: Long,
    val programDayId: Long,
    val scheduledDate: Long,
    val scheduledTime: Long?,
    /** "planned" / "completed" / "skipped" — skipped is NEVER shown negatively in the UI. */
    val status: String,
    val completedDate: Long?,
)
