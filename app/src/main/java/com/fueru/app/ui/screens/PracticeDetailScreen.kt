package com.fueru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import com.fueru.app.FueruApplication
import com.fueru.app.R
import com.fueru.app.data.DateUtils
import com.fueru.app.data.PracticeScoring
import com.fueru.app.data.entity.GuidedSession
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.data.entity.PracticeScheduledSlot
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruPracticeHeatmap
import com.fueru.app.ui.components.FueruStatChip
import com.fueru.app.ui.components.FueruSwitch
import com.fueru.app.ui.components.FueruTimePickerDialog
import com.fueru.app.ui.components.FueruWeekdayChip
import com.fueru.app.ui.components.isoWeekdayLabels
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Heatmap + decay score for one practice, plus schedule management (§5) and the entry point into
 * Resistance Flow (§6) — "I'm resisting this" is the headline action per the brief's "primary path,
 * not a fallback" framing. The Done/Partial/Skip/Miss row stays as a direct-logging fallback (today
 * only) for when the full flow isn't the point — already did it before opening the app, backdating
 * within today, etc. §4.4's display rules: decay score is the headline number, 7-day/30-day windows
 * are secondary, no streak counter anywhere.
 */
@Composable
fun PracticeDetailScreen(practiceId: Long, onBack: () -> Unit, onStartResistanceFlow: (startAtIgnite: Boolean) -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val practice by database.practiceDao().observeById(practiceId).collectAsState(initial = null)
    val current = practice ?: return

    val entries by database.practiceLogEntryDao()
        .observeForPractice(practiceId)
        .collectAsState(initial = emptyList<PracticeLogEntry>())

    val today = remember { LocalDate.now().toString() }
    val score = remember(entries, current.halfLifeDays) { PracticeScoring.currentScore(entries, current.halfLifeDays) }
    val window7 = remember(entries) { PracticeScoring.windowCompletionRate(entries, 7, today) }
    val window30 = remember(entries) { PracticeScoring.windowCompletionRate(entries, 30, today) }
    val todayStatus = remember(entries) { entries.find { it.date == today }?.status }

    val slots by database.practiceScheduledSlotDao()
        .observeForPractice(practiceId)
        .collectAsState(initial = emptyList<PracticeScheduledSlot>())
    var showScheduleDialog by remember { mutableStateOf(false) }

    val charityCount by database.charityDao().observeAll().collectAsState(initial = emptyList())

    val recentGuidedSessions by database.guidedSessionDao()
        .observeRecentForPractice(practiceId, 10)
        .collectAsState(initial = emptyList<GuidedSession>())

    fun logToday(status: String) {
        scope.launch {
            database.practiceLogEntryDao().upsert(PracticeLogEntry(practiceId = practiceId, date = today, status = status))
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
            Text(text = "session style", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    FueruSwitch(
                        checked = current.guidedSessionEnabled,
                        onCheckedChange = { checked ->
                            scope.launch { database.practiceDao().update(current.copy(guidedSessionEnabled = checked)) }
                        },
                        label = "Guided session (pick a type + duration each time)",
                    )
                    if (current.guidedSessionEnabled && recentGuidedSessions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                            Text(text = "recent sessions", color = FueruColors.TextMuted, style = FueruType.caption)
                            recentGuidedSessions.forEach { session ->
                                Text(
                                    text = "${formatSessionDate(session.timestamp)} · ${session.sessionType} · ${session.durationMinutes} min",
                                    color = FueruColors.TextSecondary,
                                    style = FueruType.caption,
                                )
                            }
                        }
                    }
                }
            }
        }

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
            }
        }

        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = score.toInt().toString(), color = FueruColors.Fire4, style = FueruType.statLg)
                Text(text = "consistency score", color = FueruColors.TextMuted, style = FueruType.caption)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    FueruStatChip(
                        icon = painterResource(R.drawable.ic_chart_line_up_fill),
                        value = "${window7.toInt()}%",
                        label = "7-day",
                    )
                    FueruStatChip(
                        icon = painterResource(R.drawable.ic_chart_line_up_fill),
                        value = "${window30.toInt()}%",
                        label = "30-day",
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "history", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruPracticeHeatmap(entries = entries, weeksShown = 12)
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

private val sessionDateFormatter = DateTimeFormatter.ofPattern("MMM d")

/** "Aug 5" — used by the guided-session "recent sessions" list, module round 1 ("fuwari"). */
private fun formatSessionDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(sessionDateFormatter)

/** "no schedule set" / "Mon, Wed, Fri" / "Mon, Wed, Fri · 7:00 AM" (only shows a time if every slot shares the same one — the common case, since v1's editor only ever writes one shared time across all selected days). */
private fun formatScheduleSummary(slots: List<PracticeScheduledSlot>): String {
    if (slots.isEmpty()) return "no schedule set"
    val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
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
    currentSlots: List<PracticeScheduledSlot>,
    onSave: (List<Pair<Int, Int?>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDays by remember { mutableStateOf(currentSlots.map { it.dayOfWeek }.toSet()) }
    var timeMinutes by remember { mutableStateOf(currentSlots.firstOrNull { it.timeOfDay != null }?.timeOfDay) }
    var showTimePicker by remember { mutableStateOf(false) }

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
                FueruButton(
                    text = "Save schedule",
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
        )
    }
}
