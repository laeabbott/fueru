package com.fueru.app.data

import android.content.Context
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.notifications.NotificationHelper
import java.time.LocalDate

/**
 * Vacation-practices round — a practice with [com.fueru.app.data.entity.Practice.vacationUntilDate]
 * set is neither prompted on Home nor escalation-alarmed while vacationed (see
 * [computeTodaysPracticePlan] and [EscalationScheduler.scheduleTodaysEscalations] for those two
 * checks). This is the one place the vacation *date* itself gets processed daily: writing a "skip"
 * PracticeLogEntry for each vacation day (so PracticeScoring's existing skip-exclusion protects the
 * score with zero changes to that file), firing the "resumes tomorrow" notification on the last
 * vacation day, and clearing the field once it's passed. Called from inside
 * [EscalationScheduler.scheduleTodaysEscalations] — that function's own three call sites
 * (FueruApplication.onCreate, DailyRescheduleReceiver, BootCompletedReceiver) already give this the
 * "runs once a day regardless of whether the app opens" guarantee it needs, without adding new call
 * sites of its own.
 */
suspend fun processDailyVacations(context: Context, database: AppDatabase) {
    val today = LocalDate.now()
    val todayIso = today.toString()
    val practices = database.practiceDao().getAll()
    for (practice in practices) {
        val vacationUntil = practice.vacationUntilDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: continue
        when {
            vacationUntil.isBefore(today) -> {
                database.practiceDao().update(practice.copy(vacationUntilDate = null))
                AppLogger.log(context, "Vacation", "cleared expired vacation for '${practice.name}'")
            }
            vacationUntil.isEqual(today) -> {
                NotificationHelper.notifyVacationEnding(context, practice.id, practice.name)
                writeVacationSkip(database, practice.id, todayIso)
                AppLogger.log(context, "Vacation", "'${practice.name}' resumes tomorrow — notified, wrote today's skip")
            }
            else -> {
                // Still mid-vacation (vacationUntil is after today).
                writeVacationSkip(database, practice.id, todayIso)
                AppLogger.log(context, "Vacation", "'${practice.name}' still vacationed through $vacationUntil — wrote today's skip")
            }
        }
    }
}

private suspend fun writeVacationSkip(database: AppDatabase, practiceId: Long, todayIso: String) {
    val existing = database.practiceLogEntryDao().getForPracticeAndDate(practiceId, todayIso)
    if (existing == null) {
        database.practiceLogEntryDao().upsert(PracticeLogEntry(practiceId = practiceId, date = todayIso, status = "skip"))
    }
}
