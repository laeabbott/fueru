package com.fueru.app.data

private const val BUFFER_MINUTES = 20
private val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

data class ScheduleConflict(val dayOfWeek: Int, val otherPracticeName: String) {
    val dayLabel: String get() = dayNames[dayOfWeek] ?: "?"
}

/**
 * Scheduling & escalation alignment pass, §A — back-to-back practices need a real transition
 * buffer (getting home, switching gears), so any two *different* practices' scheduled times on the
 * same day must be at least [BUFFER_MINUTES] apart. Checks a practice's proposed schedule (selected
 * days sharing one time, matching EditScheduleDialog's v1 "one shared time" simplification) against
 * every other practice's existing slots. Only slots with a set time can conflict — an untimed slot
 * has nothing to compare against.
 */
suspend fun findScheduleConflicts(
    database: AppDatabase,
    excludePracticeId: Long,
    selectedDays: Set<Int>,
    timeMinutes: Int?,
): List<ScheduleConflict> {
    if (timeMinutes == null || selectedDays.isEmpty()) return emptyList()
    val otherSlots = database.practiceScheduledSlotDao().getAll()
        .filter { it.practiceId != excludePracticeId && it.timeOfDay != null && it.dayOfWeek in selectedDays }

    val conflicts = mutableListOf<ScheduleConflict>()
    for (slot in otherSlots) {
        val diffMinutes = kotlin.math.abs(slot.timeOfDay!! - timeMinutes)
        if (diffMinutes < BUFFER_MINUTES) {
            val practice = database.practiceDao().getById(slot.practiceId) ?: continue
            conflicts.add(ScheduleConflict(slot.dayOfWeek, practice.name))
        }
    }
    return conflicts
}
