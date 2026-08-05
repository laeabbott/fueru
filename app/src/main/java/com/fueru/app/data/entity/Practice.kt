package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring practice (exercise, meditation, flashcards, Japanese, diet, ...) tracked by the
 * pivot's Core Engine — see the project brief §4.1. `scheduledSlots` lives in the separate
 * PracticeScheduledSlot table (Phase 2, matches this codebase's RecurringScheduleEntry pattern).
 */
@Entity(tableName = "practice")
data class Practice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** "per_week" or "per_month" — never defaults to daily, per spec §4.5. */
    val targetFrequencyType: String,
    val targetFrequencyCount: Int,
    /** Per-practice override of the decay half-life used by PracticeScoring; ships at the spec's default (14) everywhere for now — no tuning UI yet. */
    val halfLifeDays: Int = 14,
    /** Pre-filled "smallest version" for Resistance Flow's Commit step. */
    val microActionDefault: String? = null,
    val createdDate: Long,
    /** §6.3's fade-unlock: once true, this practice's Resistance Flow jumps straight from Landing to Ignite. Opt-in only, set from the Summary screen's explicit offer — never flipped silently. */
    val shortFlowEnabled: Boolean = false,
    /** §7.3 Stage 3/4 stick config — when true, this practice's escalation goes past Stage 2 into the charity-pledge consequence. False (the default) means Stage 2 is the ceiling, matching "alarm-only, no external stakes." */
    val stickCharityEnabled: Boolean = false,
    /** When true, Resistance Flow's Commit step swaps its generic micro-action+short-timer UI for a session-type + real-duration picker (module round 1, "fuwari") — see GuidedSession for what gets logged. Generic flag, not tied to any one practice, so any future practice needing a real timed session can opt in. */
    val guidedSessionEnabled: Boolean = false,
    /** Whether this practice gets its own bottom-nav tab (routes straight to its detail screen), toggled from Settings' "practice tabs" section — independent of guidedSessionEnabled. */
    val showAsTab: Boolean = false,
)
