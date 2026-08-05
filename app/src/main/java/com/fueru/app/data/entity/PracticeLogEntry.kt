package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One day's outcome for one practice — spec §4.2. Unlike SetLog (many rows per day), this is at
 * most one row per (practiceId, date): the unique index below plus REPLACE-on-conflict inserts
 * mean re-logging a day cleanly overwrites rather than duplicating.
 */
@Entity(
    tableName = "practice_log_entry",
    foreignKeys = [
        ForeignKey(
            entity = Practice::class,
            parentColumns = ["id"],
            childColumns = ["practiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("practiceId"), Index(value = ["practiceId", "date"], unique = true)],
)
data class PracticeLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long,
    /** "YYYY-MM-DD" — matches spec §4.2's date format exactly, not an epoch-millis day-bucket like ScheduledWorkout, since this is never combined with a time-of-day. */
    val date: String,
    /**
     * "done" / "partial" / "skip" / "miss" — see PracticeScoring for exactly how each feeds the
     * decay score: skip is excluded from the decay clock entirely, partial nudges toward a 0.5
     * target, miss toward 0, done toward 1.
     */
    val status: String,
)
