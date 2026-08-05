package com.fueru.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Day-bucket helpers shared by scheduling and nutrition logging. Weeks are Monday-start. */
object DateUtils {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun todayEpochMillis(): Long = startOfDay(System.currentTimeMillis())

    private fun epochMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun startOfDay(millis: Long): Long =
        epochMillisToLocalDate(millis).atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfWeek(millis: Long): Long {
        val date = epochMillisToLocalDate(millis)
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** [dayOfWeek] is ISO: 1 = Monday .. 7 = Sunday. */
    fun dateForDayOfWeek(weekStart: Long, dayOfWeek: Int): Long {
        val monday = epochMillisToLocalDate(weekStart)
        return monday.plusDays((dayOfWeek - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun minutesSinceMidnight(hour: Int, minute: Int): Int = hour * 60 + minute

    /** Combines a day-bucket timestamp (from [startOfDay]/[dateForDayOfWeek]) with a time-of-day. */
    fun combineDateAndMinutes(dayStartMillis: Long, minutesSinceMidnight: Int): Long =
        dayStartMillis + minutesSinceMidnight * 60_000L

    fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)

    /** Formats a bare time-of-day (no date attached yet) — e.g. a recurring schedule's chosen time. */
    fun formatMinutesSinceMidnight(minutes: Int): String =
        LocalTime.of(minutes / 60, minutes % 60).format(timeFormatter)
}
