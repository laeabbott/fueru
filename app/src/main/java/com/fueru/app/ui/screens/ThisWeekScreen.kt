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
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.PracticeScheduledSlot
import com.fueru.app.data.entity.ProgramDay
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

    var icsImported by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { icsImported = IcsCalendarStore.savedUri(application) != null }
    val icsPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch { IcsCalendarStore.save(application, uri) }
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
    // Practices-on-This-Week round — practices are the second thing this screen schedules,
    // alongside workout program days. Both feed the same grid; see PendingScheduleItem below for
    // how the two get merged into one queue.
    val practices by database.practiceDao().observeAll().collectAsState(initial = emptyList())
    val practiceSlots by database.practiceScheduledSlotDao().observeAll().collectAsState(initial = emptyList())

    val programDayById = remember(programDays) { programDays.associateBy { it.id } }
    val practiceById = remember(practices) { practices.associateBy { it.id } }
    val unscheduledDays = programDays.filter { day -> scheduledThisWeek.none { it.programDayId == day.id } }
    // A practice needs only one placement to be escalation-armed — EscalationScheduler reads the
    // same practice_scheduled_slot table this writes to, so there's no separate "turn on
    // enforcement" step. It drops out of the queue as soon as it has any slot at all, unlike a
    // program day's exactly-one-per-week requirement (further days for an already-slotted practice
    // still go through Edit Schedule on its own detail screen).
    val slottedPracticeIds = remember(practiceSlots) { practiceSlots.map { it.practiceId }.toSet() }
    val unscheduledPractices = practices.filter { it.id !in slottedPracticeIds }
    val pendingQueue = remember(unscheduledDays, unscheduledPractices) {
        unscheduledDays.map { PendingScheduleItem.Workout(it) } + unscheduledPractices.map { PendingScheduleItem.PracticeItem(it) }
    }
    val pendingItem = pendingQueue.firstOrNull()

    // Calendar-redesign round — one combined 7-day fetch for the grid, re-run whenever the ICS
    // import state or the ignored-events set could have changed (icsImported flip, or
    // busyRefreshTrigger after an "ignore").
    var busyRefreshTrigger by remember { mutableIntStateOf(0) }
    var busyBlocks by remember { mutableStateOf<List<BusyBlock>>(emptyList()) }
    LaunchedEffect(icsImported, busyRefreshTrigger) {
        busyBlocks = allBusyBlocksForWeek(application, weekStart)
    }

    fun scheduleDay(dayOfWeek: Int, minutesSinceMidnight: Int) {
        when (val item = pendingItem ?: return) {
            is PendingScheduleItem.Workout -> {
                val date = DateUtils.dateForDayOfWeek(weekStart, dayOfWeek)
                scope.launch {
                    database.scheduledWorkoutDao().insert(
                        ScheduledWorkout(
                            weekStartDate = weekStart,
                            programDayId = item.day.id,
                            scheduledDate = date,
                            scheduledTime = DateUtils.combineDateAndMinutes(date, minutesSinceMidnight),
                            status = "planned",
                            completedDate = null,
                        ),
                    )
                    autoFillTrigger++
                }
            }
            is PendingScheduleItem.PracticeItem -> {
                scope.launch {
                    // insertAll rather than replaceForPractice (which Edit Schedule uses) — this
                    // only ever adds the one new slot being placed here, leaving any others alone.
                    database.practiceScheduledSlotDao().insertAll(
                        listOf(PracticeScheduledSlot(practiceId = item.practice.id, dayOfWeek = dayOfWeek, timeOfDay = minutesSinceMidnight)),
                    )
                }
            }
        }
    }

    fun unschedule(workout: ScheduledWorkout) {
        scope.launch { database.scheduledWorkoutDao().delete(workout) }
    }

    fun unschedulePracticeSlot(slot: PracticeScheduledSlot) {
        scope.launch { database.practiceScheduledSlotDao().deleteById(slot.id) }
    }

    val scheduledDates = remember(scheduledThisWeek) { scheduledThisWeek.map { it.scheduledDate }.toSet() }
    val remainingPlaceableDayCount = remember(scheduledDates, weekStart) {
        (1..7).count { dow ->
            val date = DateUtils.dateForDayOfWeek(weekStart, dow)
            date >= DateUtils.todayEpochMillis() && date !in scheduledDates
        }
    }
    // Workouts only — a program day needs a whole day to itself the way this count assumes, but a
    // practice can share a day with other things, so it has no equivalent "ran out of room" state.
    val overflowCount = (unscheduledDays.size - remainingPlaceableDayCount).coerceAtLeast(0)

    // One combined list of every already-placed block the grid draws — workouts derive their
    // day-of-week/minutes from their absolute scheduledDate/scheduledTime, practice slots already
    // carry theirs natively (dayOfWeek/timeOfDay is exactly this shape, recurring).
    val gridBlocks = remember(scheduledThisWeek, practiceSlots, programDayById, practiceById) {
        val workoutBlocks = scheduledThisWeek.map { sw ->
            GridScheduledBlock(
                id = "workout:${sw.id}",
                dayOfWeek = Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).dayOfWeek.value,
                minutesSinceMidnight = sw.scheduledTime?.let { ((it - sw.scheduledDate) / 60_000L).toInt() },
                label = programDayById[sw.programDayId]?.dayLabel ?: "Workout",
                color = FueruColors.Fire4.copy(alpha = 0.3f),
                textColor = FueruColors.Fire4,
                onUnschedule = { unschedule(sw) },
            )
        }
        val practiceBlocks = practiceSlots.map { slot ->
            GridScheduledBlock(
                id = "practice:${slot.id}",
                dayOfWeek = slot.dayOfWeek,
                minutesSinceMidnight = slot.timeOfDay,
                label = practiceById[slot.practiceId]?.name ?: "Practice",
                color = FueruColors.SignalInfo.copy(alpha = 0.3f),
                textColor = FueruColors.SignalInfo,
                onUnschedule = { unschedulePracticeSlot(slot) },
            )
        }
        workoutBlocks + practiceBlocks
    }

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
                        scope.launch { IcsCalendarStore.clear(application) }
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

        if (programDays.isEmpty()) {
            // The seeded program (Section 8) has no rows for this phase — either the DB hasn't
            // finished seeding yet, or the seed callback never ran. Not a normal empty state, and
            // unrelated to practices, so this still short-circuits the whole screen rather than
            // just the workout half of it.
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
        } else {
            // Practices-on-This-Week round — the grid now always renders (previously it hid
            // entirely once nothing was left to place), because it's also the one place already-
            // placed practice slots are visible for removal, and that shouldn't disappear just
            // because the workout side of the queue is empty.
            if (pendingQueue.isEmpty()) {
                FueruCard(modifier = Modifier.fillMaxWidth(), glow = true) {
                    Text(
                        text = "you're fully booked this week — nice.",
                        color = FueruColors.Fire4,
                        style = FueruType.bodyLg,
                    )
                }
            }
            FueruWeekScheduleGrid(
                weekStart = weekStart,
                busyBlocks = busyBlocks,
                scheduledThisWeek = gridBlocks,
                pendingDayLabel = pendingItem?.label,
                onIgnoreEvent = { block ->
                    scope.launch { IgnoredEventStore.ignore(application, block.id) }
                    busyRefreshTrigger++
                },
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

/**
 * The one thing being placed on the grid at a time — either a workout program day or a practice
 * still needing its first slot. Program days are exhausted from [unscheduledDays] one at a time
 * same as before; practices are appended after, so existing workout-scheduling behavior is
 * unchanged until that queue empties.
 */
private sealed interface PendingScheduleItem {
    val label: String

    data class Workout(val day: ProgramDay) : PendingScheduleItem {
        override val label get() = day.dayLabel
    }

    data class PracticeItem(val practice: Practice) : PendingScheduleItem {
        override val label get() = practice.name
    }
}
