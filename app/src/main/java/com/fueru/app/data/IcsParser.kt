package com.fueru.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Minimal hand-rolled parser for the subset of RFC 5545 (iCalendar) needed to pull VEVENT busy
 * blocks out of an exported .ics file — Proton Calendar is the motivating case, since it doesn't
 * sync into Android's CalendarContract provider the way Google/Samsung calendars do. Not a
 * general-purpose ICS library: no RRULE recurrence expansion, no TEXT-value escape decoding
 * (`\,` `\;` `\n`), just enough to answer "is this day busy, and with what."
 */
object IcsParser {

    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val utcFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    private val floatingFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val tzidPattern = Regex("TZID=([^;:]+)")

    fun parse(icsText: String): List<BusyBlock> {
        val events = mutableListOf<BusyBlock>()
        var inEvent = false
        var summary: String? = null
        var uid: String? = null
        var start: Long? = null
        var end: Long? = null

        unfold(icsText).lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line == "BEGIN:VEVENT" -> {
                    inEvent = true
                    summary = null
                    uid = null
                    start = null
                    end = null
                }
                line == "END:VEVENT" -> {
                    val s = start
                    val e = end
                    if (inEvent && s != null && e != null) {
                        val title = summary ?: "Busy"
                        // UID is a standard RFC 5545 property, virtually always present — falls back
                        // to a content hash only for a malformed file missing it, so "ignore this
                        // event" (calendar-redesign round) still has a stable-enough id to persist
                        // against for a given file's contents.
                        val id = "ics:" + (uid ?: "${title.hashCode()}:$s:$e")
                        events.add(BusyBlock(id = id, title = title, startMillis = s, endMillis = e))
                    }
                    inEvent = false
                }
                !inEvent -> Unit
                line.startsWith("SUMMARY") -> summary = line.substringAfter(":", "").ifBlank { null }
                line.startsWith("UID") -> uid = line.substringAfter(":", "").ifBlank { null }
                line.startsWith("DTSTART") -> start = parseDateTimeProperty(line)
                line.startsWith("DTEND") -> end = parseDateTimeProperty(line)
            }
        }
        return events
    }

    /** Unfolds RFC 5545 line-folding: a continuation line starts with a single space or tab. */
    private fun unfold(text: String): String {
        val builder = StringBuilder()
        text.replace("\r\n", "\n").replace("\r", "\n").lines().forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && builder.isNotEmpty()) {
                builder.append(line.drop(1))
            } else {
                if (builder.isNotEmpty()) builder.append("\n")
                builder.append(line)
            }
        }
        return builder.toString()
    }

    /**
     * [line] is a full "DTSTART[;PARAMS]:value" (or DTEND) property line. Returns epoch millis in
     * the device's default zone, or null if unparseable — one bad date drops that one event rather
     * than failing the whole import.
     */
    private fun parseDateTimeProperty(line: String): Long? {
        val value = line.substringAfter(":", "")
        if (value.isBlank()) return null
        val propertyHead = line.substringBefore(":")
        val tzid = tzidPattern.find(propertyHead)?.groupValues?.get(1)

        return try {
            when {
                !value.contains("T") ->
                    LocalDate.parse(value, dateOnlyFormatter)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                value.endsWith("Z") ->
                    LocalDateTime.parse(value, utcFormatter)
                        .atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                else -> {
                    val zone = tzid?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                    LocalDateTime.parse(value, floatingFormatter)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                }
            }
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
