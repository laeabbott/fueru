package com.fueru.app.data

import com.fueru.app.data.entity.PracticeLogEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function unit tests for [PracticeScoring] — no Android dependency, no DB, no emulator.
 * These exist specifically to remove the "verify by hand, on-device, every time this file is
 * touched" tax this project has paid repeatedly (see HANDOFF.md's architecture-review round) —
 * every assertion here mirrors a behavior [PracticeScoring.currentScore]'s own doc comment
 * explicitly promises, most importantly the skip-day decay-clock exclusion.
 */
class PracticeScoringTest {

    private fun entry(date: String, status: String) = PracticeLogEntry(practiceId = 1L, date = date, status = status)

    private fun datesFrom(start: LocalDate, count: Int): List<LocalDate> =
        (0 until count).map { start.plusDays(it.toLong()) }

    @Test
    fun `no entries scores zero`() {
        assertEquals(0f, PracticeScoring.currentScore(emptyList()), 0.01f)
    }

    @Test
    fun `first-ever log jumps straight to its target, not a decayed value`() {
        assertEquals(100f, PracticeScoring.currentScore(listOf(entry("2026-08-01", "done"))), 0.01f)
        assertEquals(50f, PracticeScoring.currentScore(listOf(entry("2026-08-01", "partial"))), 0.01f)
        assertEquals(0f, PracticeScoring.currentScore(listOf(entry("2026-08-01", "miss"))), 0.01f)
    }

    @Test
    fun `a lone skip entry never scores anything -- excluded from the decay clock entirely`() {
        assertEquals(0f, PracticeScoring.currentScore(listOf(entry("2026-08-01", "skip"))), 0.01f)
    }

    @Test
    fun `score stays within 0 to 100 across a long mixed history`() {
        val start = LocalDate.parse("2026-01-01")
        val statuses = listOf("done", "done", "miss", "partial", "skip", "miss", "done", "skip", "skip", "partial")
        val entries = datesFrom(start, statuses.size).zip(statuses) { d, s -> entry(d.toString(), s) }
        val score = PracticeScoring.currentScore(entries)
        assertTrue("score $score out of [0,100] range", score in 0f..100f)
    }

    @Test
    fun `many consecutive skip days genuinely do not move the decay clock -- not just left unscored`() {
        // day 1: done (score -> 100). Days 2-11: ten straight skip days. Day 12: miss.
        // Skip-exclusion means the miss at day 12 should decay as if it landed exactly one day
        // after the done, not eleven days after -- the ten skip days cost nothing.
        val start = LocalDate.parse("2026-08-01")
        val entriesWithSkips = buildList {
            add(entry(start.toString(), "done"))
            for (i in 1..10) add(entry(start.plusDays(i.toLong()).toString(), "skip"))
            add(entry(start.plusDays(11).toString(), "miss"))
        }
        val scoreWithTenSkipDays = PracticeScoring.currentScore(entriesWithSkips)

        // Same shape, but the miss really is only one calendar day after the done -- no skips at all.
        val entriesNoSkips = listOf(
            entry(start.toString(), "done"),
            entry(start.plusDays(1).toString(), "miss"),
        )
        val scoreWithOneRealDayGap = PracticeScoring.currentScore(entriesNoSkips)

        assertEquals(
            "ten skip days should decay identically to a single real day gap, not accumulate as elapsed time",
            scoreWithOneRealDayGap,
            scoreWithTenSkipDays,
            0.01f,
        )

        // Regression guard: if skip days were wrongly counted as real elapsed time, the eleven-day
        // gap would produce a much lower score than the one-day-gap result -- assert we're nowhere
        // near that wrong answer.
        val wrongGapDecayRatio = Math.pow(0.5, 11.0 / PracticeScoring.DEFAULT_HALF_LIFE_DAYS).toFloat()
        val wrongScore = wrongGapDecayRatio * 100f
        assertTrue(
            "score $scoreWithTenSkipDays should be far above the wrong 11-day-gap value $wrongScore",
            scoreWithTenSkipDays - wrongScore > 5f,
        )
    }

    @Test
    fun `a partial entry pulls score toward 50, not fully toward done or miss`() {
        val score = PracticeScoring.currentScore(listOf(entry("2026-08-01", "partial")))
        assertEquals(50f, score, 0.01f)
    }

    @Test
    fun `score never floors discontinuously to zero after a single miss following a high streak`() {
        val start = LocalDate.parse("2026-08-01")
        val entries = listOf(
            entry(start.toString(), "done"),
            entry(start.plusDays(1).toString(), "done"),
            entry(start.plusDays(2).toString(), "done"),
            entry(start.plusDays(3).toString(), "miss"),
        )
        val score = PracticeScoring.currentScore(entries)
        assertTrue("a single miss right after a streak shouldn't crater the score to near zero, was $score", score > 40f)
    }

    // -- windowCompletionRate --------------------------------------------------------------------

    @Test
    fun `window completion rate is zero with no entries in range`() {
        assertEquals(0f, PracticeScoring.windowCompletionRate(emptyList(), windowDays = 7, asOfDate = "2026-08-11"), 0.01f)
    }

    @Test
    fun `window completion rate averages targets only for entries inside the window`() {
        val entries = listOf(
            entry("2026-08-11", "done"), // in a 7-day window ending 2026-08-11
            entry("2026-08-10", "miss"), // in window
            entry("2026-07-01", "done"), // well outside the window, must be ignored
        )
        // (1.0 + 0.0) / 2 * 100 = 50
        assertEquals(50f, PracticeScoring.windowCompletionRate(entries, windowDays = 7, asOfDate = "2026-08-11"), 0.01f)
    }

    @Test
    fun `window completion rate excludes skip days from the denominator`() {
        val entries = listOf(
            entry("2026-08-11", "done"),
            entry("2026-08-10", "skip"),
        )
        // Only the "done" entry counts -- skip is excluded from both numerator and denominator, so
        // this should be 100, not 50 (which is what you'd get if skip silently counted as a miss).
        assertEquals(100f, PracticeScoring.windowCompletionRate(entries, windowDays = 7, asOfDate = "2026-08-11"), 0.01f)
    }
}
