package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One completed guided session — module round 1 ("fuwari"), for practices with
 * [Practice.guidedSessionEnabled]. Kept as its own table rather than extending [ResistanceSession]
 * (which stays fully generic across every practice, guided or not) — matches the project brief §3's
 * own module principle: "Modules are thin UIs that log completions into Core, with their own extra
 * fields as needed." [sessionType] is free text, not an enum — deliberately flexible since a single
 * guided-session practice can cover more than one kind of use (e.g. stillness meditation one day,
 * text study another).
 */
@Entity(tableName = "guided_session")
data class GuidedSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long,
    val timestamp: Long,
    val sessionType: String,
    val durationMinutes: Int,
)
