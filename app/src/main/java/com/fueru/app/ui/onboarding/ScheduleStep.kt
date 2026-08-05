package com.fueru.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.fueru.app.FueruApplication
import com.fueru.app.data.DateUtils
import com.fueru.app.data.entity.ProgramDay
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruSwitch
import com.fueru.app.ui.components.FueruTimePickerDialog
import com.fueru.app.ui.components.FueruWeekdayChip
import com.fueru.app.ui.components.isoWeekdayLabels
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun ScheduleStep(state: OnboardingState, onNext: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val programDays by application.database.programDayDao()
        .observeByPhase("0-6")
        .collectAsState(initial = emptyList())

    var pendingPick by remember { mutableStateOf<Pair<ProgramDay, Int>?>(null) }

    val allAssigned = programDays.isNotEmpty() && programDays.all { state.recurringAssignments.containsKey(it.id) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "same schedule every week?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "Pin each workout day to a weekday and time once, and fueru fills in every week for you. " +
                "Prefer to plan week by week instead? Leave this off — you'll set it up fresh in This Week.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruCard(modifier = Modifier.fillMaxWidth()) {
            FueruSwitch(
                checked = state.useRecurringSchedule,
                onCheckedChange = { state.useRecurringSchedule = it },
                label = "Same days every week",
            )
        }
        if (state.useRecurringSchedule) {
            programDays.forEach { day ->
                val assignment = state.recurringAssignments[day.id]
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = day.dayLabel, color = FueruColors.TextSecondary, style = FueruType.caption)
                        if (assignment?.timeOfDay != null) {
                            Text(
                                text = "· ${DateUtils.formatMinutesSinceMidnight(assignment.timeOfDay)}",
                                color = FueruColors.Fire4,
                                style = FueruType.caption,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        isoWeekdayLabels.forEach { (dow, label) ->
                            val selected = assignment?.dayOfWeek == dow
                            FueruWeekdayChip(label = label, selected = selected) {
                                pendingPick = day to dow
                            }
                        }
                    }
                }
            }
        }
        FueruButton(
            text = "Next",
            onClick = onNext,
            enabled = !state.useRecurringSchedule || allAssigned,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }

    pendingPick?.let { (day, dow) ->
        val existingMinutes = state.recurringAssignments[day.id]?.timeOfDay
        val initial = existingMinutes?.let { (it / 60) to (it % 60) } ?: (7 to 0)
        FueruTimePickerDialog(
            initialHour = initial.first,
            initialMinute = initial.second,
            onConfirm = { hour, minute ->
                val minutes = DateUtils.minutesSinceMidnight(hour, minute)
                state.recurringAssignments = state.recurringAssignments + (day.id to RecurringAssignment(dow, minutes))
                pendingPick = null
            },
            onDismiss = { pendingPick = null },
        )
    }
}
