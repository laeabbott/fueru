package com.fueru.app.data

import com.fueru.app.data.entity.ScheduledWorkout

data class TodayOrNextWorkout(
    val scheduledWorkout: ScheduledWorkout,
    val isToday: Boolean,
)

/** Today's scheduled workout if one exists, else the nearest future one, else null. */
suspend fun findTodayOrNextWorkout(database: AppDatabase): TodayOrNextWorkout? {
    val today = DateUtils.todayEpochMillis()
    val todays = database.scheduledWorkoutDao().getForDate(today)
    if (todays != null && todays.status == "planned") {
        return TodayOrNextWorkout(todays, isToday = true)
    }
    val next = database.scheduledWorkoutDao().getNextPlannedAfter(today) ?: return null
    return TodayOrNextWorkout(next, isToday = false)
}

/**
 * Auto-fills [weekStart]'s ScheduledWorkout rows from RecurringScheduleEntry, skipping any date
 * that already has a scheduled workout. No-op if the user has no recurring entries set up.
 */
suspend fun autoFillRecurringWeek(database: AppDatabase, weekStart: Long) {
    val entries = database.recurringScheduleDao().getAll()
    if (entries.isEmpty()) return

    entries.forEach { entry ->
        val date = DateUtils.dateForDayOfWeek(weekStart, entry.dayOfWeek)
        val existing = database.scheduledWorkoutDao().getForDate(date)
        if (existing == null) {
            val time = entry.timeOfDay?.let { DateUtils.combineDateAndMinutes(date, it) }
            database.scheduledWorkoutDao().insert(
                ScheduledWorkout(
                    weekStartDate = weekStart,
                    programDayId = entry.programDayId,
                    scheduledDate = date,
                    scheduledTime = time,
                    status = "planned",
                    completedDate = null,
                ),
            )
        }
    }
}
