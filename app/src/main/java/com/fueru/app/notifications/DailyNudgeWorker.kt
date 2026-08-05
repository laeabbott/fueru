package com.fueru.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fueru.app.FueruApplication
import com.fueru.app.data.DateUtils
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * Runs roughly once a day (see FueruApplication's periodic enqueue — WorkManager doesn't guarantee
 * exact time-of-day for periodic work, so this can drift to whenever in the day it happens to fire
 * rather than always being a tidy "morning" nudge; acceptable for a first pass). Posts at most one
 * of the two notifications promised in onboarding, and only when there's something to say — never
 * a "you missed a day" guilt trip.
 */
class DailyNudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as FueruApplication
        val database = application.database
        val profile = database.userProfileDao().get() ?: return Result.success()

        NotificationHelper.ensureChannel(applicationContext)

        val today = DateUtils.todayEpochMillis()

        val todaysWorkout = database.scheduledWorkoutDao().getForDate(today)
        if (todaysWorkout != null && todaysWorkout.status == "planned") {
            val programDay = database.programDayDao().getById(todaysWorkout.programDayId)
            if (programDay != null) {
                val exerciseCount = database.prescribedSetDao().getForProgramDay(todaysWorkout.programDayId).size
                NotificationHelper.notifyTodaysWorkout(applicationContext, programDay.dayLabel, exerciseCount)
                return Result.success()
            }
        }

        val isSunday = Instant.ofEpochMilli(today).atZone(ZoneId.systemDefault()).dayOfWeek == DayOfWeek.SUNDAY
        if (isSunday && !profile.useRecurringSchedule) {
            val nextWeekStart = DateUtils.startOfWeek(today + 7 * 86_400_000L)
            val nextWeekScheduled = database.scheduledWorkoutDao().observeForWeek(nextWeekStart).first()
            if (nextWeekScheduled.isEmpty()) {
                NotificationHelper.notifyPlanNextWeek(applicationContext)
            }
        }

        return Result.success()
    }
}
