package com.fueru.app.data

import com.fueru.app.data.entity.PracticeLogEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

/**
 * Pure scoring functions — project brief §4.3. No DB access, so these are trivially unit-testable;
 * the score is never stored (same "derive from history on every read" principle ProgressionAlgorithm
 * already uses in this codebase), computed fresh from a practice's full PracticeLogEntry history
 * every time it's displayed.
 */
object PracticeScoring {
    const val DEFAULT_HALF_LIFE_DAYS = 14

    /**
     * 0-100 decay-weighted density score. Walks non-skip entries in date order; for each one the
     * score is pulled toward a target (1.0 done / 0.5 partial / 0.0 miss) by a decay factor based
     * on elapsed days since the previous non-skip entry. Skip days are excluded from that elapsed-
     * day count entirely — not just left unscored, genuinely erased from the decay clock, so a
     * skip truly costs nothing ("score carries over unchanged" per spec). Never floors to zero or
     * resets discontinuously — it's asymptotic by construction, exponential smoothing toward
     * whatever the most recent entries are pulling it toward.
     */
    fun currentScore(entries: List<PracticeLogEntry>, halfLifeDays: Int = DEFAULT_HALF_LIFE_DAYS): Float {
        val sorted = entries.sortedBy { it.date }
        var score = 0f
        var lastScoredDate: LocalDate? = null
        var pendingSkipDays = 0

        for (entry in sorted) {
            val date = LocalDate.parse(entry.date)
            if (entry.status == "skip") {
                pendingSkipDays += 1
                continue
            }
            val target = targetFor(entry.status)
            val previous = lastScoredDate
            score = if (previous == null) {
                // Nothing to decay from yet — the first-ever log is the score.
                target
            } else {
                val calendarGap = ChronoUnit.DAYS.between(previous, date)
                val effectiveGap = (calendarGap - pendingSkipDays).coerceAtLeast(0)
                val decayRatio = 0.5.pow(effectiveGap.toDouble() / halfLifeDays).toFloat()
                target + (score - target) * decayRatio
            }
            lastScoredDate = date
            pendingSkipDays = 0
        }
        return (score * 100f).coerceIn(0f, 100f)
    }

    /**
     * Simple literal completion rate (not decayed) over the trailing [windowDays] ending
     * [asOfDate] inclusive — the smaller secondary number spec §4.4 wants shown alongside the
     * headline decay score. Skip days are excluded from the denominator too — they were never
     * "due" in a way that should count for or against the window.
     */
    fun windowCompletionRate(entries: List<PracticeLogEntry>, windowDays: Int, asOfDate: String): Float {
        val end = LocalDate.parse(asOfDate)
        val start = end.minusDays((windowDays - 1).toLong())
        val inWindow = entries.filter {
            val date = LocalDate.parse(it.date)
            !date.isBefore(start) && !date.isAfter(end) && it.status != "skip"
        }
        if (inWindow.isEmpty()) return 0f
        val total = inWindow.sumOf { targetFor(it.status).toDouble() }
        return (total / inWindow.size * 100).toFloat().coerceIn(0f, 100f)
    }

    private fun targetFor(status: String): Float = when (status) {
        "done" -> 1f
        "partial" -> 0.5f
        else -> 0f // "miss"
    }
}
