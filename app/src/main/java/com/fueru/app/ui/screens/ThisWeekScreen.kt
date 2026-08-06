package com.fueru.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fueru.app.FueruApplication
import com.fueru.app.data.BusyBlock
import com.fueru.app.data.DateUtils
import com.fueru.app.data.IcsCalendarStore
import com.fueru.app.data.IgnoredEventStore
import com.fueru.app.data.allBusyBlocksForWeek
import com.fueru.app.data.autoFillRecurringWeek
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruWeekScheduleGrid
import com.fueru.app.ui.components.GridScheduledBlock
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ThisWeekScreen(onBack: () -> Unit, onViewExercises: (Long) -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    val weekStart = remember { DateUtils.startOfWeek(DateUtils.todayEpochMillis()) }
    var autoFillTrigger by remember { mutableIntStateOf(0) }

    var icsImported by remember { mutableStateOf(IcsCalendarStore.savedUri(application) != null) }
    val icsPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            IcsCalendarStore.save(application, uri)
            icsImported = true
        }
    }

    LaunchedEffect(profile.useRecurringSchedule, autoFillTrigger) {
        if (profile.useRecurringSchedule) {
            autoFillRecurringWeek(database, weekStart)
        }
    }

    val scheduledThisWeek by database.scheduledWorkoutDao()
        .observeForWeek(weekStart)
        .collectAsState(initial = emptyList())
    val programDays by database.programDayDao()
        .observeByPhase(profile.currentPhase)
        .collectAsState(initial = emptyList())

    val programDayById = remember(programDays) { programDays.associateBy { it.id } }
    val unscheduledDays = programDays.filter { day -> scheduledThisWeek.none { it.programDayId == day.id } }

    // Calendar-redesign round — one combined 7-day fetch for the grid, re-run whenever the ICS
    // import state or the ignored-events set could have changed (icsImported flip, or
    // busyRefreshTrigger after an "ignore").
    var busyRefreshTrigger by remember { mutableIntStateOf(0) }
    var busyBlocks by remember { mutableStateOf<List<BusyBlock>>(emptyList()) }
    LaunchedEffect(icsImported, busyRefreshTrigger) {
        busyBlocks = allBusyBlocksForWeek(application, weekStart)
    }

    fun scheduleDay(dayOfWeek: Int, minutesSinceMidnight: Int) {
        val day = unscheduledDays.firstOrNull() ?: return
        val date = DateUtils.dateForDayOfWeek(weekStart, dayOfWeek)
        scope.launch {
            database.scheduledWorkoutDao().insert(
                ScheduledWorkout(
                    weekStartDate = weekStart,
                    programDayId = day.id,
                    scheduledDate = date,
                    scheduledTime = DateUtils.combineDateAndMinutes(date, minutesSinceMidnight),
                    status = "planned",
                    completedDate = null,
                ),
            )
            autoFillTrigger++
        }
    }

    fun unschedule(workout: ScheduledWorkout) {
        scope.launch { database.scheduledWorkoutDao().delete(workout) }
    }

    val scheduledDates = remember(scheduledThisWeek) { scheduledThisWeek.map { it.scheduledDate }.toSet() }
    val remainingPlaceableDayCount = remember(scheduledDates, weekStart) {
        (1..7).count { dow ->
            val date = DateUtils.dateForDayOfWeek(weekStart, dow)
            date >= DateUtils.todayEpochMillis() && date !in scheduledDates
        }
    }
    val overflowCount = (unscheduledDays.size - remainingPlaceableDayCount).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "what are we doing this week?", color = FueruColors.TextPrimary, style = FueruType.headline)

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Text(
                text = if (icsImported) "calendar imported" else "import a calendar (.ics)",
                color = if (icsImported) FueruColors.TextMuted else FueruColors.Fire4,
                style = FueruType.caption,
                modifier = Modifier.clickable(enabled = !icsImported) {
                    icsPickerLauncher.launch(arrayOf("text/calendar", "application/octet-stream", "*/*"))
                },
            )
            if (icsImported) {
                Text(
                    text = "remove",
                    color = FueruColors.Fire4,
                    style = FueruType.caption,
                    modifier = Modifier.clickable {
                        IcsCalendarStore.clear(application)
                        icsImported = false
                    },
                )
            }
        }

        if (profile.useRecurringSchedule) {
            Text(
                text = "Your fixed schedule fills this in automatically — change a day below if this week needs to be different.",
                color = FueruColors.TextMuted,
                style = FueruType.body,
            )
        }

        if (scheduledThisWeek.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                scheduledThisWeek.sortedBy { it.scheduledDate }.forEach { scheduled ->
                    val day = programDayById[scheduled.programDayId]
                    ScheduledWorkoutCard(
                        dayLabel = day?.dayLabel ?: "Workout",
                        scheduled = scheduled,
                        onViewExercises = { onViewExercises(scheduled.id) },
                    )
                }
            }
        }

        when {
            programDays.isEmpty() -> {
                // The seeded program (Section 8) has no rows for this phase — either the DB hasn't
                // finished seeding yet, or the seed callback never ran. Not a normal empty state.
                FueruCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Text(
                            text = "no program days found",
                            color = FueruColors.TextPrimary,
                            style = FueruType.bodyLg,
                        )
                        Text(
                            text = "Phase \"${profile.currentPhase}\" has zero seeded workout days — that shouldn't " +
                                "happen. Check the Database Inspector's program_day table, or reinstall so the " +
                                "seed data reloads.",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                }
            }
            unscheduledDays.isEmpty() -> {
                FueruCard(modifier = Modifier.fillMaxWidth(), glow = true) {
                    Text(
                        text = "you're fully booked this week — nice.",
                        color = FueruColors.Fire4,
                        style = FueruType.bodyLg,
                    )
                }
            }
            else -> {
                FueruWeekScheduleGrid(
                    weekStart = weekStart,
                    busyBlocks = busyBlocks,
                    scheduledThisWeek = scheduledThisWeek.map { sw ->
                        GridScheduledBlock(sw, programDayById[sw.programDayId]?.dayLabel ?: "Workout")
                    },
                    pendingDayLabel = unscheduledDays.firstOrNull()?.dayLabel,
                    onIgnoreEvent = { block ->
                        IgnoredEventStore.ignore(application, block.id)
                        busyRefreshTrigger++
                    },
                    onUnschedule = ::unschedule,
                    onCommit = ::scheduleDay,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (overflowCount > 0) {
                    Text(
                        text = "no more room this week for $overflowCount more day${if (overflowCount == 1) "" else "s"} — pick ${if (overflowCount == 1) "it" else "them"} up next week.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                }
            }
        }

        FueruButton(text = "Back to Home", onClick = onBack, variant = FueruButtonVariant.Ghost)
    }
}

@Composable
private fun ScheduledWorkoutCard(
    dayLabel: String,
    scheduled: ScheduledWorkout,
    onViewExercises: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FueruCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = dayLabel, color = FueruColors.TextPrimary, style = FueruType.body)
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatShortDate(scheduled.scheduledDate), color = FueruColors.TextMuted, style = FueruType.caption)
                    Text(
                        text = scheduled.scheduledTime?.let { DateUtils.formatTime(it) } ?: "no time set",
                        color = if (scheduled.scheduledTime != null) FueruColors.Fire4 else FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                }
            }
            Text(
                text = "view / swap exercises",
                color = FueruColors.Fire4,
                style = FueruType.caption,
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onViewExercises,
                ),
            )
        }
    }
}

private fun formatShortDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
