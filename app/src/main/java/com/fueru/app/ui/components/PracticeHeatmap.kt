package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.ui.theme.FueruColors
import java.time.LocalDate

private const val WEEKS_SHOWN = 12
private val CELL_SIZE = 12.dp
private val CELL_GAP = 3.dp
private val CELL_RADIUS = 3.dp

/**
 * GitHub-style day grid for one practice's recent history — project brief §4.4's "primary visual."
 * Columns run oldest (left) to newest (right, today), 7 cells each. Not strictly Monday-aligned
 * across columns — each column is just 7 sequential days — a cosmetic simplification acceptable
 * for this first pass; easy to tighten to real calendar-week alignment later if it matters.
 *
 * Colors reuse the existing fueru palette rather than introducing new tokens: done = Fire4 solid,
 * partial = Fire4 dim, miss = SignalDanger dim (visibly distinct from partial, still muted rather
 * than alarming — spec's "trend framing, never loss framing" for misses), skip = flat neutral
 * (visibly distinct from a miss), no entry = the plain track color.
 */
@Composable
fun FueruPracticeHeatmap(
    entries: List<PracticeLogEntry>,
    modifier: Modifier = Modifier,
    weeksShown: Int = WEEKS_SHOWN,
    today: LocalDate = LocalDate.now(),
) {
    val byDate = remember(entries) { entries.associateBy { it.date } }
    val totalDays = weeksShown * 7
    val startDate = today.minusDays((totalDays - 1).toLong())

    Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP), modifier = modifier) {
        for (week in 0 until weeksShown) {
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                for (dayInWeek in 0 until 7) {
                    val date = startDate.plusDays((week * 7 + dayInWeek).toLong())
                    val status = if (date.isAfter(today)) null else byDate[date.toString()]?.status
                    HeatmapCell(color = colorForStatus(status))
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(color: Color) {
    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .clip(RoundedCornerShape(CELL_RADIUS))
            .background(color),
    )
}

private fun colorForStatus(status: String?): Color = when (status) {
    "done" -> FueruColors.Fire4
    "partial" -> FueruColors.Fire4.copy(alpha = 0.4f)
    "miss" -> FueruColors.SignalDanger.copy(alpha = 0.5f)
    "skip" -> FueruColors.Ink600
    else -> FueruColors.SurfaceRaised
}
