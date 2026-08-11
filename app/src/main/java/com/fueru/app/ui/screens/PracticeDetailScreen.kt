package com.fueru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.fueru.app.FueruApplication
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.BusyBlock
import com.fueru.app.data.DateUtils
import com.fueru.app.data.PracticeStartStore
import com.fueru.app.data.ScheduleConflict
import com.fueru.app.data.allBusyBlocksForDay
import com.fueru.app.data.celebration.GiphyApi
import com.fueru.app.data.findScheduleConflicts
import com.fueru.app.data.mondayOfThisWeek
import com.fueru.app.data.remainingSlotsIfTargetMet
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.data.entity.PracticeScheduledSlot
import com.fueru.app.escalation.EscalationScheduler
import com.fueru.app.ui.components.FueruBusyBlocksSummary
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruSwitch
import com.fueru.app.ui.components.FueruTimePickerDialog
import com.fueru.app.ui.components.FueruWeekdayChip
import com.fueru.app.ui.components.isoWeekdayLabels
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

/**
 * Schedule management (§5), stakes, session style, and the entry point into Resistance Flow (§6)
 * for one practice — "I'm resisting this" is the headline action per the brief's "primary path, not
 * a fallback" framing, shown only once today's alarm has actually fired and the practice hasn't
 * already been started/logged/vacationed (resistance-flow gating round — this used to always show).
 * The Done/Partial/Skip/Miss row stays as a direct-logging fallback (today only, always available
 * regardless of the gate above) for when the full flow isn't the point — already did it before
 * opening the app, backdating within today, etc. Progress-consolidation round: this screen no
 * longer shows score/heatmap/history at all — that's the Progress tab's job now, for every practice,
 * not just workouts (see ProgressScreen.kt's PracticeProgressCard).
 */
@Composable
fun PracticeDetailScreen(practiceId: Long, onBack: () -> Unit, onStartResistanceFlow: (startAtIgnite: Boolean) -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val practice by database.practiceDao().observeById(practiceId).collectAsState(initial = null)
    val current = practice ?: return

    // Progress-consolidation round — this screen used to also read/derive score/window/heatmap
    // data, all now shown only on the Progress tab (ProgressScreen.kt's PracticeProgressCard).
    // entries is still needed here, just for todayStatus (gates the manual-log buttons' highlight
    // and, new this round, the resistance-flow entry card below).
    val entries by database.practiceLogEntryDao()
        .observeForPractice(practiceId)
        .collectAsState(initial = emptyList<PracticeLogEntry>())

    val today = remember { LocalDate.now().toString() }
    val todayStatus = remember(entries) { entries.find { it.date == today }?.status }

    val slots by database.practiceScheduledSlotDao()
        .observeForPractice(practiceId)
        .collectAsState(initial = emptyList<PracticeScheduledSlot>())
    var showScheduleDialog by remember { mutableStateOf(false) }

    val charityCount by database.charityDao().observeAll().collectAsState(initial = emptyList())

    // §E — offer to skip a remaining scheduled day this week once the weekly target's already met.
    var weeklyTargetOfferSlots by remember(practiceId) { mutableStateOf<List<PracticeScheduledSlot>>(emptyList()) }

    // Resistance-flow gating round — "I'm resisting this" only shows once today's alarm has
    // actually fired (same condition PracticeScheduler.computeTodaysPracticePlan's isOverdue
    // already uses) and the practice hasn't been started/logged/vacationed — not a general "start
    // anytime" button. The manual Done/Partial/Skip/Miss row below is untouched by this gate.
    var hasStarted by remember(practiceId) { mutableStateOf(false) }
    LaunchedEffect(practiceId, today) { hasStarted = PracticeStartStore.isStarted(application, practiceId, today) }
    val todayDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
    val nowMinutes = remember { LocalTime.now().let { it.hour * 60 + it.minute } }
    val todaySlot = slots.firstOrNull { it.dayOfWeek == todayDayOfWeek }
    val alarmHasFired = todaySlot?.timeOfDay != null && nowMinutes >= todaySlot.timeOfDay
    val onVacation = current.vacationUntilDate
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let { !it.isBefore(LocalDate.now()) }
        ?: false

    fun logToday(status: String) {
        scope.launch {
            database.practiceLogEntryDao().upsert(PracticeLogEntry(practiceId = practiceId, date = today, status = status))
            weeklyTargetOfferSlots = if (status == "done" || status == "partial") {
                remainingSlotsIfTargetMet(database, practiceId)
            } else {
                emptyList()
            }
        }
    }

    // §D — "I've started": a direct quick-start, not tied to reaching Ignite in Resistance Flow.
    // Doesn't log the practice as done (starting isn't finishing) — just holds off today's
    // remaining escalation and marks the practice as underway so reopening the app later today
    // doesn't re-arm alarms for it.
    var justStarted by remember(practiceId) { mutableStateOf(false) }
    var startedGifUrl by remember(practiceId) { mutableStateOf<String?>(null) }
    fun markStartedNow() {
        scope.launch {
            EscalationScheduler.cancelForPractice(application, practiceId)
            PracticeStartStore.markStarted(application, practiceId, today)
            startedGifUrl = GiphyApi.randomCelebrationGifUrl(milestone = false)
            justStarted = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = current.name, color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "target: ${current.targetFrequencyCount}x ${if (current.targetFrequencyType == "per_week") "a week" else "a month"}",
            color = FueruColors.TextMuted,
            style = FueruType.caption,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "schedule", color = FueruColors.TextSecondary, style = FueruType.title)
            Text(
                text = formatScheduleSummary(slots),
                color = FueruColors.TextMuted,
                style = FueruType.body,
            )
            FueruButton(text = "Edit schedule", variant = FueruButtonVariant.Secondary, onClick = { showScheduleDialog = true })
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "stakes", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    if (charityCount.isEmpty()) {
                        Text(
                            text = "Add a charity in Settings before turning this on.",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    } else {
                        FueruSwitch(
                            checked = current.stickCharityEnabled,
                            onCheckedChange = { checked ->
                                scope.launch { database.practiceDao().update(current.copy(stickCharityEnabled = checked)) }
                            },
                            label = "Charity pledge if this goes unmarked long enough",
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            // Progress-consolidation round — "recent sessions" used to also list here; that's
            // progress, so it moved to the Progress tab's PracticeProgressCard along with
            // everything else. This section is config-only now.
            Text(text = "session style", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                FueruSwitch(
                    checked = current.guidedSessionEnabled,
                    onCheckedChange = { checked ->
                        scope.launch { database.practiceDao().update(current.copy(guidedSessionEnabled = checked)) }
                    },
                    label = "Guided session (pick a type + duration each time)",
                )
            }
        }

        if (weeklyTargetOfferSlots.isNotEmpty()) {
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "Already hit this week's target for ${current.name}. Want to skip one of the remaining days?",
                        color = FueruColors.TextPrimary,
                        style = FueruType.body,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        weeklyTargetOfferSlots.forEach { slot ->
                            FueruButton(
                                text = "Skip ${dayNames[slot.dayOfWeek] ?: "?"}",
                                variant = FueruButtonVariant.Secondary,
                                onClick = {
                                    scope.launch {
                                        val slotDate = mondayOfThisWeek().plusDays((slot.dayOfWeek - 1).toLong())
                                        database.practiceLogEntryDao().upsert(
                                            PracticeLogEntry(practiceId = practiceId, date = slotDate.toString(), status = "skip"),
                                        )
                                        weeklyTargetOfferSlots = weeklyTargetOfferSlots - slot
                                    }
                                },
                            )
                        }
                    }
                    Text(
                        text = "not now",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                        modifier = Modifier.clickable { weeklyTargetOfferSlots = emptyList() },
                    )
                }
            }
        }

        // Resistance-flow gating round — this whole card only appears once today's alarm has
        // actually fired for this practice (matches PracticeScheduler's own isOverdue condition)
        // and it hasn't already been started/logged/vacationed — not a general-purpose "start
        // anytime" entry point. justStarted overrides the gate so the celebration state (below)
        // stays visible for the rest of this screen visit once tapped, same as before this round.
        val showResistanceEntry = justStarted || (alarmHasFired && !hasStarted && todayStatus == null && !onVacation)
        if (showResistanceEntry) {
            if (justStarted) {
                FueruCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        if (startedGifUrl != null) {
                            AsyncImage(
                                model = startedGifUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().size(140.dp).clip(RoundedCornerShape(Radius.lg)),
                            )
                        }
                        Text(
                            text = "nice — didn't need the ramp-up today",
                            color = FueruColors.TextPrimary,
                            style = FueruType.body,
                        )
                    }
                }
            } else {
                FueruCard(modifier = Modifier.fillMaxWidth(), glow = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        Text(
                            text = "I know, I don't want to either. That's fine — this is what the flow is for.",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                        FueruButton(
                            text = "I'm resisting this",
                            onClick = { onStartResistanceFlow(current.shortFlowEnabled) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FueruButton(
                            text = current.actionVerb?.let { "$it!" } ?: "I've started",
                            variant = FueruButtonVariant.Secondary,
                            onClick = { markStartedNow() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(
                text = "or log it directly" + (todayStatus?.let { " · today logged as $it" } ?: ""),
                color = FueruColors.TextMuted,
                style = FueruType.caption,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), modifier = Modifier.fillMaxWidth()) {
                FueruButton(
                    text = "Done",
                    variant = if (todayStatus == "done") FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { logToday("done") },
                )
                FueruButton(
                    text = "Partial",
                    variant = if (todayStatus == "partial") FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { logToday("partial") },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), modifier = Modifier.fillMaxWidth()) {
                FueruButton(
                    text = "Skip",
                    variant = if (todayStatus == "skip") FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { logToday("skip") },
                )
                FueruButton(
                    text = "Miss",
                    variant = if (todayStatus == "miss") FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { logToday("miss") },
                )
            }
        }

        FueruButton(text = "Back", variant = FueruButtonVariant.Ghost, onClick = onBack)
    }

    if (showScheduleDialog) {
        EditScheduleDialog(
            database = database,
            practiceId = practiceId,
            currentSlots = slots,
            onSave = { newSlots ->
                scope.launch {
                    database.practiceScheduledSlotDao().replaceForPractice(
                        practiceId,
                        newSlots.map { (dow, minutes) -> PracticeScheduledSlot(practiceId = practiceId, dayOfWeek = dow, timeOfDay = minutes) },
                    )
                }
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false },
        )
    }
}

private val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

/** "no schedule set" / "Mon, Wed, Fri" / "Mon, Wed, Fri · 7:00 AM" (only shows a time if every slot shares the same one — the common case, since v1's editor only ever writes one shared time across all selected days). */
private fun formatScheduleSummary(slots: List<PracticeScheduledSlot>): String {
    if (slots.isEmpty()) return "no schedule set"
    val days = slots.sortedBy { it.dayOfWeek }.joinToString(", ") { dayNames[it.dayOfWeek] ?: "?" }
    val times = slots.mapNotNull { it.timeOfDay }.distinct()
    return if (times.size == 1) "$days · ${DateUtils.formatMinutesSinceMidnight(times[0])}" else days
}

/**
 * Multi-select weekday picker + one shared time — project brief §5's scheduledSlots, simplified
 * for v1 (see the Phase 2 plan's "one simplification" note: real per-day times aren't built yet).
 * Reuses FueruWeekdayChip/isoWeekdayLabels and FueruTimePickerDialog exactly as
 * ui/onboarding/ScheduleStep.kt does for the equivalent workout-day picker.
 */
@Composable
private fun EditScheduleDialog(
    database: AppDatabase,
    practiceId: Long,
    currentSlots: List<PracticeScheduledSlot>,
    onSave: (List<Pair<Int, Int?>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDays by remember { mutableStateOf(currentSlots.map { it.dayOfWeek }.toSet()) }
    var timeMinutes by remember { mutableStateOf(currentSlots.firstOrNull { it.timeOfDay != null }?.timeOfDay) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Scheduling & escalation alignment pass, §A — a 20min buffer between different practices'
    // scheduled times, so back-to-back days actually leave room to transition. Re-checked whenever
    // the picked days/time change.
    var conflicts by remember { mutableStateOf<List<ScheduleConflict>>(emptyList()) }
    LaunchedEffect(selectedDays, timeMinutes) {
        conflicts = findScheduleConflicts(database, practiceId, selectedDays, timeMinutes)
    }

    // §B — calendar/.ics busy-block awareness, same mechanism This Week's time picker already uses
    // (allBusyBlocksForDay), adapted for a recurring weekly slot: shown per selected day, computed
    // against that day's *next upcoming occurrence* rather than one specific date.
    val context = LocalContext.current
    var busyBlocksByDay by remember { mutableStateOf<Map<Int, List<BusyBlock>>>(emptyMap()) }
    LaunchedEffect(selectedDays) {
        val weekStart = DateUtils.startOfWeek(System.currentTimeMillis())
        val todayStart = DateUtils.todayEpochMillis()
        busyBlocksByDay = selectedDays.associateWith { dow ->
            // A day-of-week already past this week means its *next* occurrence is next week's,
            // not a stale date in the past.
            val candidate = DateUtils.dateForDayOfWeek(weekStart, dow)
            val effective = if (candidate < todayStart) candidate + 7 * 86_400_000L else candidate
            allBusyBlocksForDay(context, effective)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "edit schedule", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(text = "which days", color = FueruColors.TextSecondary, style = FueruType.caption)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    isoWeekdayLabels.forEach { (dow, label) ->
                        FueruWeekdayChip(
                            label = label,
                            selected = dow in selectedDays,
                            onClick = { selectedDays = if (dow in selectedDays) selectedDays - dow else selectedDays + dow },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeMinutes?.let { "time · ${DateUtils.formatMinutesSinceMidnight(it)}" } ?: "time · none set",
                        color = FueruColors.TextSecondary,
                        style = FueruType.caption,
                    )
                    Text(
                        text = "change",
                        color = FueruColors.Fire4,
                        style = FueruType.caption,
                        modifier = Modifier.padding(start = Spacing.space2).clickable { showTimePicker = true },
                    )
                }
                if (conflicts.isNotEmpty()) {
                    Text(
                        text = conflicts.joinToString("\n") {
                            "too close to ${it.otherPracticeName} on ${it.dayLabel} — needs 20 min either side"
                        },
                        color = FueruColors.SignalDanger,
                        style = FueruType.caption,
                    )
                }
                FueruButton(
                    text = "Save schedule",
                    enabled = conflicts.isEmpty(),
                    onClick = { onSave(selectedDays.map { it to timeMinutes }) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showTimePicker) {
        val initial = timeMinutes?.let { (it / 60) to (it % 60) } ?: (7 to 0)
        FueruTimePickerDialog(
            initialHour = initial.first,
            initialMinute = initial.second,
            onConfirm = { hour, minute ->
                timeMinutes = DateUtils.minutesSinceMidnight(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            extraContent = {
                selectedDays.sorted().forEach { dow ->
                    val blocks = busyBlocksByDay[dow].orEmpty()
                    if (blocks.isNotEmpty()) {
                        Text(text = dayNames[dow] ?: "", color = FueruColors.TextMuted, style = FueruType.caption)
                        FueruBusyBlocksSummary(blocks)
                    }
                }
            },
        )
    }
}
