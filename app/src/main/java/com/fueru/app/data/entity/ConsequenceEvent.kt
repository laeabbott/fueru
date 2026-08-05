package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One Stage 4 firing — project brief §7.2/§7.4. Charity name/url are snapshotted at fire time (not
 * re-read from the Charity table later), same "snapshot, don't re-derive" reasoning FoodLogEntry
 * already uses in this codebase — a charity edited or deleted later shouldn't change what a past
 * pledge said. Also the system of record for the weekly glad→resent escalation rule (count of rows
 * this week for a practice) and for whether a firing is still waiting on connectivity.
 */
@Entity(
    tableName = "consequence_event",
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
data class ConsequenceEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long,
    val timestamp: Long,
    val charityId: Long,
    val charityName: String,
    val charityUrl: String,
    /** True once the user has tapped "I did this" on the pledge screen — manual, since there's no payment API to verify a donation actually happened (§7.4's own "pledge, not verify" scope). */
    val completed: Boolean = false,
    /** True if this fired while offline and is waiting for connectivity (§8.3) — the pledge screen isn't shown until this flips back to false. */
    val queued: Boolean = false,
)
