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

/** ISO Monday of the current week, as a plain [LocalDate] — shared by §E's target-met check and its own "write a future skip" callback. */
fun mondayOfThisWeek(): LocalDate = LocalDate.now().let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }

/**
 * Scheduling & escalation alignment pass, §E — if a per-week practice's target is already met from
 * this week's done/partial entries, returns whichever of this week's scheduled slots are still
 * *ahead* (later day-of-week than today) and unlogged, so the caller can offer to skip one. Empty
 * when the target isn't met yet, every remaining slot this week is already logged, or the practice
 * targets "per_month" — a monthly target doesn't map cleanly onto "which day this week to drop."
 */
suspend fun remainingSlotsIfTargetMet(database: AppDatabase, practiceId: Long): List<PracticeScheduledSlot> {
    val practice = database.practiceDao().getById(practiceId) ?: return emptyList()
    if (practice.targetFrequencyType != "per_week") return emptyList()

    val today = LocalDate.now()
    val todayDow = today.dayOfWeek.value
    val mondayThisWeek = mondayOfThisWeek()

    val entriesThisWeek = database.practiceLogEntryDao().getForPracticeSince(practiceId, mondayThisWeek.toString())
    val completedCount = entriesThisWeek.count { it.status == "done" || it.status == "partial" }
    if (completedCount < practice.targetFrequencyCount) return emptyList()

    val loggedDates = entriesThisWeek.map { it.date }.toSet()
    return database.practiceScheduledSlotDao().getAll()
        .filter { it.practiceId == practiceId && it.dayOfWeek > todayDow }
        .filter { slot -> mondayThisWeek.plusDays((slot.dayOfWeek - 1).toLong()).toString() !in loggedDates }
}

suspend fun computeTodaysPracticePlan(database: AppDatabase): List<TodayPracticeSlot> {
    val today = LocalDate.now()
    val todayIso = today.toString()
    val todayDayOfWeek = today.dayOfWeek.value
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }

    val todaysSlots = database.practiceScheduledSlotDao().getAll().filter { it.dayOfWeek == todayDayOfWeek }

    return todaysSlots.mapNotNull { slot ->
        val practice = database.practiceDao().getById(slot.practiceId) ?: return@mapNotNull null
        // Vacation-practices round — not prompted at all while vacationed, not just de-emphasized.
        val onVacation = practice.vacationUntilDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { !it.isBefore(today) }
            ?: false
        if (onVacation) return@mapNotNull null
        val entry = database.practiceLogEntryDao().getForPracticeAndDate(practice.id, todayIso)
        val isOverdue = entry == null && slot.timeOfDay != null && nowMinutes >= slot.timeOfDay
        TodayPracticeSlot(practice = practice, slot = slot, loggedStatus = entry?.status, isOverdue = isOverdue)
    }
}

/** Home's "no practices today" fallback — the next scheduled slot, however many days out. [dayOffset] is 1 for tomorrow, up to 7 for the same weekday next week. */
data class NextPracticeSlot(val practice: Practice, val slot: PracticeScheduledSlot, val dayOffset: Int)

/**
 * Only called once [computeTodaysPracticePlan] has already come back empty, so today's own
 * dayOfWeek is never a candidate here — starts the search at tomorrow and walks a full week
 * forward, same vacation exclusion as [computeTodaysPracticePlan] (checked against the slot's own
 * future date, not today's), so a practice that's vacationed through its next occurrence gets
 * skipped in favor of whichever comes after it.
 */
suspend fun findNextScheduledPractice(database: AppDatabase): NextPracticeSlot? {
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek.value
    val allSlots = database.practiceScheduledSlotDao().getAll()
    for (offset in 1..7) {
        val dayOfWeek = (todayDayOfWeek - 1 + offset) % 7 + 1
        val futureDate = today.plusDays(offset.toLong())
        val candidates = allSlots.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.timeOfDay ?: Int.MAX_VALUE }
        for (slot in candidates) {
            val practice = database.practiceDao().getById(slot.practiceId) ?: continue
            val onVacation = practice.vacationUntilDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { !it.isBefore(futureDate) }
                ?: false
            if (!onVacation) return NextPracticeSlot(practice, slot, offset)
        }
    }
    return null
}
