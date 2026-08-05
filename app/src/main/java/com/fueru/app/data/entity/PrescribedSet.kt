package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prescribed_set",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDay::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programDayId"), Index("exerciseId")],
)
data class PrescribedSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDayId: Long,
    val exerciseId: String,
    val orderInDay: Int,
    val sets: Int,
    val repsMin: Int,
    /** Equal to repsMin when the program specifies a fixed rep count rather than a range. */
    val repsMax: Int,
    /** e.g. "2-1-2" */
    val tempo: String,
    val comment: String?,
    /** Exercises sharing this tag are a superset (red highlight in the source doc). */
    val supersetGroup: String?,
    /** Final set is a drop set (yellow highlight in the source doc). */
    val isDropSetFinal: Boolean,
    /** Tension-focus / slow tempo (blue highlight in the source doc). */
    val isTensionFocus: Boolean,
)
