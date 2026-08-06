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
import com.fueru.app.data.allBusyBlocksForDay
import com.fueru.app.data.autoFillRecurringWeek
import com.fueru.app.data.entity.ProgramDay
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.ui.components.FueruBusyBlocksSummary
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruTimePickerDialog
import com.fueru.app.ui.components.FueruWeekdayChip
import com.fueru.app.ui.components.isoWeekdayLabels
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What the time picker dialog is currently being shown for. */
private sealed class TimePickTarget {
    data class NewAssignment(val day: ProgramDay, val dayOfWeek: Int) : TimePickTarget()
    data class EditExisting(val scheduledWorkout: ScheduledWorkout) : TimePickTarget()
}

@Composable
fun ThisWeekScreen(onBack: () -> Unit, onViewExercises: (Long) -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    val weekStart = remember { DateUtils.startOfWeek(DateUtils.todayEpochMillis()) }
    var autoFillTrigger by remember { mutableIntStateOf(0) }
    var pickTarget by remember { mutableStateOf<TimePickTarget?>(null) }

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
                        onClick = { pickTarget = TimePickTarget.EditExisting(scheduled) },
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
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(text = "still need a date", color = FueruColors.TextSecondary, style = FueruType.title)
                    unscheduledDays.forEach { day ->
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                            Text(text = day.dayLabel, color = FueruColors.TextPrimary, style = FueruType.body)
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                                isoWeekdayLabels.forEach { (dow, label) ->
                                    FueruWeekdayChip(label = label, selected = false) {
                                        pickTarget = TimePickTarget.NewAssignment(day, dow)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FueruButton(text = "Back to Home", onClick = onBack, variant = FueruButtonVariant.Ghost)
    }

    pickTarget?.let { target ->
        val pickDate = when (target) {
            is TimePickTarget.NewAssignment -> DateUtils.dateForDayOfWeek(weekStart, target.dayOfWeek)
            is TimePickTarget.EditExisting -> target.scheduledWorkout.scheduledDate
        }
        var busyBlocks by remember(pickDate) { mutableStateOf<List<BusyBlock>>(emptyList()) }
        LaunchedEffect(pickDate) {
            busyBlocks = allBusyBlocksForDay(application, pickDate)
        }

        val existingTime = (target as? TimePickTarget.EditExisting)?.scheduledWorkout?.scheduledTime
        val initial = existingTime?.let {
            val local = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            local.hour to local.minute
        } ?: (9 to 0)

        FueruTimePickerDialog(
            initialHour = initial.first,
            initialMinute = initial.second,
            extraContent = { FueruBusyBlocksSummary(busyBlocks) },
            onConfirm = { hour, minute ->
                val minutes = DateUtils.minutesSinceMidnight(hour, minute)
                scope.launch {
                    when (target) {
                        is TimePickTarget.NewAssignment -> {
                            val date = DateUtils.dateForDayOfWeek(weekStart, target.dayOfWeek)
                            database.scheduledWorkoutDao().insert(
                                ScheduledWorkout(
                                    weekStartDate = weekStart,
                                    programDayId = target.day.id,
                                    scheduledDate = date,
                                    scheduledTime = DateUtils.combineDateAndMinutes(date, minutes),
                                    status = "planned",
                                    completedDate = null,
                                ),
                            )
                        }
                        is TimePickTarget.EditExisting -> {
                            val sw = target.scheduledWorkout
                            database.scheduledWorkoutDao().update(
                                sw.copy(scheduledTime = DateUtils.combineDateAndMinutes(sw.scheduledDate, minutes)),
                            )
                        }
                    }
                    autoFillTrigger++
                }
                pickTarget = null
            },
            onDismiss = { pickTarget = null },
        )
    }
}

@Composable
private fun ScheduledWorkoutCard(
    dayLabel: String,
    scheduled: ScheduledWorkout,
    onClick: () -> Unit,
    onViewExercises: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FueruCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = dayLabel, color = FueruColors.TextPrimary, style = FueruType.body)
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatShortDate(scheduled.scheduledDate), color = FueruColors.TextMuted, style = FueruType.caption)
                    Text(
                        text = scheduled.scheduledTime?.let { DateUtils.formatTime(it) } ?: "tap to set a time",
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
                    interactionSource = remember { MutableInteractionSource() },
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
