package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A program day the user has pinned to a fixed weekday (e.g. "Day 1 is always Monday"), set once
 * during onboarding or from This Week. Only consulted when UserProfile.useRecurringSchedule is true.
 */
@Entity(
    tableName = "recurring_schedule_entry",
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
data class RecurringScheduleEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDayId: Long,
    /** ISO day-of-week: 1 = Monday .. 7 = Sunday (matches java.time.DayOfWeek.value). */
    val dayOfWeek: Int,
    /** Minutes since midnight (0-1439), or null if no specific time was set. */
    val timeOfDay: Int? = null,
)
