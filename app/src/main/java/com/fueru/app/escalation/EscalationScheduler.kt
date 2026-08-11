package com.fueru.app.escalation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.AppLogger
import com.fueru.app.data.PracticeStartStore
import com.fueru.app.data.processDailyVacations
import java.time.LocalDate
import java.time.ZoneId

const val EXTRA_PRACTICE_ID = "practiceId"
const val EXTRA_PRACTICE_NAME = "practiceName"
const val EXTRA_STAGE = "stage"

const val STAGE_NUDGE = 0
const val STAGE_PERSIST = 1
const val STAGE_LOCK = 2
const val STAGE_WARNING = 3
const val STAGE_CONSEQUENCE = 4
/** Scheduling & escalation alignment pass, §C — a heads-up fired *before* the slot's own time, not instead of Stage 0. Kept a single digit like every other stage so requestCodeFor's `practiceId * 10 + stage` scheme still holds with no collision risk. */
const val STAGE_UPCOMING = 5

/** How far ahead of the scheduled time the §C heads-up fires. */
private const val UPCOMING_LEAD_MINUTES = 15L
private const val STAGE_1_OFFSET_MINUTES = 15L
private const val STAGE_2_OFFSET_MINUTES = 30L
// §7.2's own copy is internally inconsistent ("Stage 3 fires in 10 minutes" — but Stage 3 is what's
// already firing at +45min) — read as "the consequence fires 10 minutes after the warning," so
// Stage 4 = +55min. See the Phase 5 plan's judgment call #2.
private const val STAGE_3_OFFSET_MINUTES = 45L
private const val STAGE_4_OFFSET_MINUTES = 55L

/** BOOT_COMPLETED rescheduling round — when the self-re-arming daily reschedule alarm fires each day. A few minutes past midnight, not exactly on it, as a small DST/day-boundary buffer. */
private const val DAILY_RESCHEDULE_HOUR = 0
private const val DAILY_RESCHEDULE_MINUTE = 5
private const val DAILY_RESCHEDULE_REQUEST_CODE = 999_999

/**
 * Project brief §7.2, Stages 0-2 — exact-alarm scheduling only, no notification/UI logic here (see
 * EscalationReceiver for what actually happens when a stage fires). No Room table for escalation
 * state: firing time is computed fresh from PracticeScheduledSlot, and "already handled" is just
 * "does a PracticeLogEntry exist for today" — checked again at fire time in EscalationReceiver,
 * since a slot can get logged in the gap between scheduling and firing.
 *
 * Scheduling used to be app-open only (FueruApplication.onCreate) — a day the app was never opened
 * got zero alarms scheduled. Closed by the BOOT_COMPLETED rescheduling round: [scheduleDailyRescheduleAlarm]
 * arms a self-re-arming exact alarm (DailyRescheduleReceiver) that calls this function once daily
 * regardless of whether the app is ever opened, and BootCompletedReceiver re-arms both after a
 * reboot or app update (which otherwise clear every AlarmManager alarm outright).
 */
object EscalationScheduler {

    suspend fun scheduleTodaysEscalations(context: Context, database: AppDatabase) {
        // Vacation-practices round — piggybacks on this function's existing "runs once a day
        // regardless of app-open state" guarantee (all three call sites — app launch, the daily
        // reschedule alarm, boot/update — already land here) rather than adding new call sites.
        processDailyVacations(context, database)

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        if (!EscalationPermissions.canScheduleExactAlarms(context)) {
            AppLogger.log(context, "Escalation", "scheduleTodaysEscalations skipped — exact-alarm permission not granted")
            return
        }

        val today = LocalDate.now()
        val todayIso = today.toString()
        val todayDayOfWeek = today.dayOfWeek.value
        val slots = database.practiceScheduledSlotDao().getAll()
            .filter { it.dayOfWeek == todayDayOfWeek && it.timeOfDay != null }
        AppLogger.log(context, "Escalation", "scheduleTodaysEscalations: ${slots.size} slot(s) due today")

        for (slot in slots) {
            val alreadyLogged = database.practiceLogEntryDao().getForPracticeAndDate(slot.practiceId, todayIso) != null
            // §D — "I've started" holds off the rest of today's escalation for this practice even
            // though nothing's logged yet; reopening the app later today shouldn't re-arm alarms
            // for something already marked underway.
            val alreadyStarted = PracticeStartStore.isStarted(context, slot.practiceId, todayIso)
            if (alreadyLogged || alreadyStarted) {
                cancelForPractice(context, slot.practiceId)
                continue
            }
            val practice = database.practiceDao().getById(slot.practiceId) ?: continue
            // Vacation-practices round — no alarms while vacationed.
            val onVacation = practice.vacationUntilDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { !it.isBefore(today) }
                ?: false
            if (onVacation) {
                cancelForPractice(context, practice.id)
                continue
            }
            val baseMillis = todayAtMinutes(slot.timeOfDay!!)
            scheduleStage(context, alarmManager, practice.id, practice.name, STAGE_UPCOMING, baseMillis - UPCOMING_LEAD_MINUTES * 60_000L)
            scheduleStage(context, alarmManager, practice.id, practice.name, STAGE_NUDGE, baseMillis)
            scheduleStage(context, alarmManager, practice.id, practice.name, STAGE_PERSIST, baseMillis + STAGE_1_OFFSET_MINUTES * 60_000L)
            scheduleStage(context, alarmManager, practice.id, practice.name, STAGE_LOCK, baseMillis + STAGE_2_OFFSET_MINUTES * 60_000L)
            // Stage 2 is always the floor — Stage 3/4 only get scheduled at all when this practice
            // actually has a stick consequence configured, matching §7.2's "this stage requires no
            // external dependency and is likely doing most of the enforcement work" framing for
            // Stage 2, versus Stage 3/4 being opt-in on top of it.
            if (practice.stickCharityEnabled) {
                scheduleStage(context, alarmManager, practice.id, practice.name, STAGE_WARNING, baseMillis + STAGE_3_OFFSET_MINUTES * 60_000L)
                scheduleStage(context, alarmManager, practice.id, practice.name, STAGE_CONSEQUENCE, baseMillis + STAGE_4_OFFSET_MINUTES * 60_000L)
            }
        }
    }

    /**
     * BOOT_COMPLETED rescheduling round — sets one exact alarm for the next occurrence of
     * [DAILY_RESCHEDULE_HOUR]:[DAILY_RESCHEDULE_MINUTE] (today's if still ahead, otherwise
     * tomorrow's), targeting [DailyRescheduleReceiver]. That receiver calls
     * [scheduleTodaysEscalations] and then calls this function again for the *next* day — a
     * self-re-arming one-shot alarm, since exact RTC_WAKEUP alarms aren't natively repeating.
     */
    fun scheduleDailyRescheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        if (!EscalationPermissions.canScheduleExactAlarms(context)) return

        val zone = ZoneId.systemDefault()
        var trigger = LocalDate.now().atStartOfDay(zone)
            .plusHours(DAILY_RESCHEDULE_HOUR.toLong())
            .plusMinutes(DAILY_RESCHEDULE_MINUTE.toLong())
            .toInstant().toEpochMilli()
        if (trigger <= System.currentTimeMillis()) trigger += 24 * 60 * 60_000L

        val intent = Intent(context, DailyRescheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
    }

    fun cancelForPractice(context: Context, practiceId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        listOf(STAGE_UPCOMING, STAGE_NUDGE, STAGE_PERSIST, STAGE_LOCK, STAGE_WARNING, STAGE_CONSEQUENCE).forEach { stage ->
            alarmManager.cancel(pendingIntentFor(context, practiceId, "", stage))
        }
    }

    private fun todayAtMinutes(minutesSinceMidnight: Int): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now().atStartOfDay(zone).plusMinutes(minutesSinceMidnight.toLong()).toInstant().toEpochMilli()
    }

    /** Doesn't schedule a stage whose trigger time has already passed today — opening the app late shouldn't immediately fire a backlog of stale stage 0/1 alerts; whichever stages are still ahead still get scheduled normally. */
    private fun scheduleStage(context: Context, alarmManager: AlarmManager, practiceId: Long, practiceName: String, stage: Int, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val pendingIntent = pendingIntentFor(context, practiceId, practiceName, stage)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun pendingIntentFor(context: Context, practiceId: Long, practiceName: String, stage: Int): PendingIntent {
        val intent = Intent(context, EscalationReceiver::class.java).apply {
            putExtra(EXTRA_PRACTICE_ID, practiceId)
            putExtra(EXTRA_PRACTICE_NAME, practiceName)
            putExtra(EXTRA_STAGE, stage)
        }
        // minSdk 26 is already above the API 23 floor FLAG_IMMUTABLE needs, so no version guard.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCodeFor(practiceId, stage), intent, flags)
    }

    private fun requestCodeFor(practiceId: Long, stage: Int): Int = (practiceId * 10 + stage).toInt()
}
