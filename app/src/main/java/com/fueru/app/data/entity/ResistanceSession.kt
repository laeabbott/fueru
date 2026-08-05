package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One trip through the Resistance Flow (project brief §6.2) for a practice. Written once, at
 * Summary time — there's no partial/abandoned row for a session that never got past Commit,
 * since there's no real content to attribute yet (see the Phase 3 plan's judgment call #6).
 */
@Entity(
    tableName = "resistance_session",
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
data class ResistanceSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long,
    val timestamp: Long,
    /** The Name It selection, or the free-text "other" entry. */
    val tag: String,
    /** Name It + Commit always count; Defuse/Body Check count only if not skipped — see PracticeScoring-adjacent doc in ResistanceFlowScreen.kt for the exact rule. Ranges 2-4. */
    val stepsUsed: Int,
    val completed: Boolean,
    val attribution: String,
)
