package com.fueru.app.data

import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.PracticeScheduledSlot
import java.time.LocalDate
import java.time.LocalTime

/**
 * "Today's plan" for practices — project brief §5. Due/missed *detection* only: no notification,
 * no alarm, no enforcement action. That's the Escalation Engine, a later phase — this just answers
 * "what's due today, and has it slipped past its time unlogged."
 */
data class TodayPracticeSlot(
    val practice: Practice,
    val slot: PracticeScheduledSlot,
    /** Today's PracticeLogEntry.status, or null if nothing's been logged yet today. */
    val loggedStatus: String?,
    /** True only when the slot has a time, that time has passed, and nothing's logged yet. */
    val isOverdue: Boolean,
)

suspend fun computeTodaysPracticePlan(database: AppDatabase): List<TodayPracticeSlot> {
    val today = LocalDate.now()
    val todayIso = today.toString()
    val todayDayOfWeek = today.dayOfWeek.value
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }

    val todaysSlots = database.practiceScheduledSlotDao().getAll().filter { it.dayOfWeek == todayDayOfWeek }

    return todaysSlots.mapNotNull { slot ->
        val practice = database.practiceDao().getById(slot.practiceId) ?: return@mapNotNull null
        val entry = database.practiceLogEntryDao().getForPracticeAndDate(practice.id, todayIso)
        val isOverdue = entry == null && slot.timeOfDay != null && nowMinutes >= slot.timeOfDay
        TodayPracticeSlot(practice = practice, slot = slot, loggedStatus = entry?.status, isOverdue = isOverdue)
    }
}
