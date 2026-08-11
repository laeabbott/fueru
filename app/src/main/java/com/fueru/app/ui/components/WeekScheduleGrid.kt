package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fueru.app.data.BusyBlock
import com.fueru.app.data.DateUtils
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle

/** One already-scheduled workout for the grid to render, paired with its day label for display. */
data class GridScheduledBlock(val scheduledWorkout: ScheduledWorkout, val dayLabel: String)

private const val START_HOUR = 6
private const val END_HOUR = 23 // exclusive — grid covers 6 AM through 11 PM
private val HOUR_HEIGHT: Dp = 48.dp
private val QUARTER_HEIGHT: Dp = HOUR_HEIGHT / 4
private val GUTTER_WIDTH: Dp = 40.dp

private fun offsetForMinutes(minutesSinceMidnight: Int): Dp {
    val clamped = minutesSinceMidnight.coerceIn(START_HOUR * 60, END_HOUR * 60)
    return HOUR_HEIGHT * ((clamped - START_HOUR * 60) / 60f)
}

/**
 * Calendar-redesign round — replaces the old weekday-chip "still need a date" picker with an
 * hour-by-hour weekly grid (7 day columns × quarter-hour cells, 6 AM-11 PM): imported/device
 * calendar events and already-placed exercise days render as real blocks at their actual time,
 * tapping an empty quarter-hour cell on a today-or-later day stages that slot for the practice
 * currently being scheduled ([pendingDayLabel]), and "Next" commits it — advancing to the next
 * unscheduled day is the caller's job (recomputed reactively from [scheduledThisWeek] growing, not
 * tracked here).
 *
 * Days already past this week still render their busy blocks (full-week visibility, per spec) but
 * aren't tappable for new placement — this is the mechanism behind "schedule only what fits":
 * once every remaining day already holds a placement, nothing more can be staged, and whatever's
 * left over just stays unscheduled, same as this app's existing pattern for anything not gotten to.
 */
@Composable
fun FueruWeekScheduleGrid(
    weekStart: Long,
    busyBlocks: List<BusyBlock>,
    scheduledThisWeek: List<GridScheduledBlock>,
    pendingDayLabel: String?,
    onIgnoreEvent: (BusyBlock) -> Unit,
    onUnschedule: (ScheduledWorkout) -> Unit,
    onCommit: (dayOfWeek: Int, minutesSinceMidnight: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var staged by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(pendingDayLabel) { staged = null }
    var ignoreTarget by remember { mutableStateOf<BusyBlock?>(null) }

    // Lint round -- reads through LocalLocale (observable, recomposes if the user changes the
    // system locale) instead of Locale.getDefault() (a one-time, non-observable read).
    val locale = LocalLocale.current.platformLocale
    val today = DateUtils.todayEpochMillis()
    val scheduledByDay = remember(scheduledThisWeek) {
        scheduledThisWeek.associateBy { block ->
            Instant.ofEpochMilli(block.scheduledWorkout.scheduledDate).atZone(ZoneId.systemDefault()).dayOfWeek.value
        }
    }
    val busyByDay = remember(busyBlocks, weekStart) {
        (1..7).associateWith { dow ->
            val dayStart = DateUtils.dateForDayOfWeek(weekStart, dow)
            val dayEnd = dayStart + 86_400_000L
            busyBlocks.filter { it.startMillis < dayEnd && it.endMillis > dayStart }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        if (pendingDayLabel != null) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "scheduling: $pendingDayLabel", color = FueruColors.TextSecondary, style = FueruType.title)
                FueruButton(
                    text = "Next",
                    enabled = staged != null,
                    onClick = { staged?.let { (dow, minutes) -> onCommit(dow, minutes) } },
                )
            }
        }

        // Day-of-week header — fixed, doesn't scroll with the hour grid below.
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(GUTTER_WIDTH))
            (1..7).forEach { dow ->
                val dayStart = DateUtils.dateForDayOfWeek(weekStart, dow)
                val isToday = dayStart == today
                val date = Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).toLocalDate()
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        color = if (isToday) FueruColors.Fire4 else FueruColors.TextMuted,
                        style = FueruType.overline,
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = if (isToday) FueruColors.Fire4 else FueruColors.TextSecondary,
                        style = FueruType.caption,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            HourGutter()
            (1..7).forEach { dow ->
                val dayStart = DateUtils.dateForDayOfWeek(weekStart, dow)
                val scheduledBlock = scheduledByDay[dow]
                val isPlaceable = pendingDayLabel != null && dayStart >= today && scheduledBlock == null
                DayColumn(
                    dayOfWeek = dow,
                    dayStart = dayStart,
                    busyBlocksToday = busyByDay[dow].orEmpty(),
                    scheduledToday = scheduledBlock,
                    staged = staged?.takeIf { it.first == dow }?.second,
                    isPlaceable = isPlaceable,
                    onTapQuarter = { minutes -> staged = dow to minutes },
                    onIgnoreEvent = { ignoreTarget = it },
                    onUnschedule = onUnschedule,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    ignoreTarget?.let { block ->
        IgnoreEventDialog(
            title = block.title,
            onConfirm = { onIgnoreEvent(block); ignoreTarget = null },
            onDismiss = { ignoreTarget = null },
        )
    }
}

@Composable
private fun HourGutter() {
    Column(modifier = Modifier.width(GUTTER_WIDTH)) {
        for (hour in START_HOUR until END_HOUR) {
            Box(modifier = Modifier.height(HOUR_HEIGHT), contentAlignment = Alignment.TopStart) {
                Text(
                    text = formatHourLabel(hour),
                    color = FueruColors.TextMuted,
                    style = FueruType.overline,
                )
            }
        }
    }
}

private fun formatHourLabel(hour: Int): String = when {
    hour == 0 -> "12A"
    hour < 12 -> "${hour}A"
    hour == 12 -> "12P"
    else -> "${hour - 12}P"
}

@Composable
private fun DayColumn(
    dayOfWeek: Int,
    dayStart: Long,
    busyBlocksToday: List<BusyBlock>,
    scheduledToday: GridScheduledBlock?,
    staged: Int?,
    isPlaceable: Boolean,
    onTapQuarter: (minutesSinceMidnight: Int) -> Unit,
    onIgnoreEvent: (BusyBlock) -> Unit,
    onUnschedule: (ScheduledWorkout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalHeight = HOUR_HEIGHT * (END_HOUR - START_HOUR)
    Box(modifier = modifier.height(totalHeight)) {
        // Empty tappable quarter-hour cells, background layer.
        Column(modifier = Modifier.fillMaxWidth()) {
            for (hour in START_HOUR until END_HOUR) {
                for (quarter in 0 until 4) {
                    val minutes = hour * 60 + quarter * 15
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(QUARTER_HEIGHT)
                            .then(
                                if (isPlaceable) {
                                    Modifier.clickable { onTapQuarter(minutes) }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }

        busyBlocksToday.forEach { block ->
            val startMinutes = ((block.startMillis - dayStart) / 60_000L).toInt()
            val endMinutes = ((block.endMillis - dayStart) / 60_000L).toInt()
            TimeBlock(
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                label = block.title,
                color = FueruColors.Ink600,
                textColor = FueruColors.TextSecondary,
                onClick = { onIgnoreEvent(block) },
            )
        }

        scheduledToday?.let { block ->
            val minutes = block.scheduledWorkout.scheduledTime?.let {
                ((it - dayStart) / 60_000L).toInt()
            } ?: (START_HOUR * 60)
            TimeBlock(
                startMinutes = minutes,
                endMinutes = minutes + 60,
                label = block.dayLabel,
                color = FueruColors.Fire4.copy(alpha = 0.3f),
                textColor = FueruColors.Fire4,
                onClick = { onUnschedule(block.scheduledWorkout) },
            )
        }

        if (staged != null) {
            TimeBlock(
                startMinutes = staged,
                endMinutes = staged + 60,
                label = "placing here",
                color = FueruColors.SurfaceRaised,
                textColor = FueruColors.Fire4,
                onClick = null,
            )
        }
    }
}

@Composable
private fun TimeBlock(
    startMinutes: Int,
    endMinutes: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)?,
) {
    if (endMinutes <= START_HOUR * 60 || startMinutes >= END_HOUR * 60) return
    val top = offsetForMinutes(startMinutes)
    val bottom = offsetForMinutes(endMinutes)
    val height = (bottom - top).let { if (it < QUARTER_HEIGHT) QUARTER_HEIGHT else it }
    Box(
        modifier = Modifier
            .padding(top = top, start = 1.dp, end = 1.dp)
            .height(height)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(2.dp),
    ) {
        Text(text = label, color = textColor, style = FueruType.overline, maxLines = 2)
    }
}

@Composable
private fun IgnoreEventDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "ignore \"$title\"?", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(
                    text = "It won't show here again.",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
                FueruButton(text = "Ignore", variant = FueruButtonVariant.Secondary, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
