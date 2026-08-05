package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "program_day")
data class ProgramDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "0-6", "6-12", or "12-24" */
    val phase: String,
    /** "Day 1", "Day 2", etc — see spec Section 8 */
    val dayLabel: String,
)
