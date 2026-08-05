package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One weekday+time a practice is expected — project brief §5. Modeled directly on
 * `RecurringScheduleEntry` (same dayOfWeek/timeOfDay convention) rather than inventing a new
 * shape. A practice can have zero, one, or several of these; the whole set for a practice is
 * always replaced wholesale via `PracticeScheduledSlotDao.replaceForPractice`, never patched
 * incrementally, matching how the edit UI presents it (pick which days, save).
 */
@Entity(
    tableName = "practice_scheduled_slot",
    foreignKeys = [
        ForeignKey(
            entity = Practice::class,
            parentColumns = ["id"],
            childColumns = ["practiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("practiceId")],
)
data class PracticeScheduledSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long,
    /** ISO day-of-week: 1 = Monday .. 7 = Sunday (matches RecurringScheduleEntry / java.time.DayOfWeek.value). */
    val dayOfWeek: Int,
    /** Minutes since midnight (0-1439), or null if no specific time was set. */
    val timeOfDay: Int? = null,
)
