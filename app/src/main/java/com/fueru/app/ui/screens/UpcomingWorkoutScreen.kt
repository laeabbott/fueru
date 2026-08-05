package com.fueru.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fueru.app.FueruApplication
import com.fueru.app.data.WorkoutSessionPlan
import com.fueru.app.data.WorkoutSlot
import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.loadSubstitutes
import com.fueru.app.data.loadWorkoutSessionPlan
import com.fueru.app.data.saveExerciseOverride
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruSubstituteDialog
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Preview and edit an upcoming (or today's, before starting) scheduled workout's exercise list —
 * unlike the live Workout session's one-exercise-at-a-time substitution, this lets you browse the
 * whole day and swap ahead of time. Substitutions persist via ScheduledWorkoutExerciseOverride, so
 * a swap made here shows up automatically when the workout is actually started later (Workout
 * screen's loadWorkoutSessionPlan applies the same overrides).
 */
@Composable
fun UpcomingWorkoutScreen(scheduledWorkoutId: Long, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var plan by remember { mutableStateOf<WorkoutSessionPlan?>(null) }
    var swapTargetSlot by remember { mutableStateOf<WorkoutSlot?>(null) }
    var substituteOptions by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    LaunchedEffect(scheduledWorkoutId, refreshTrigger) {
        val workout = database.scheduledWorkoutDao().getById(scheduledWorkoutId)
        plan = workout?.let { loadWorkoutSessionPlan(database, it) }
    }

    val current = plan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = current?.dayLabel ?: "workout", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "Tap \"swap\" to change an exercise for this day only — the program itself is unchanged.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )

        if (current != null) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                current.slots.forEach { slot ->
                    FueruCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(text = slot.exercise.name, color = FueruColors.TextPrimary, style = FueruType.body)
                                Text(
                                    text = "${slot.prescribedSet.sets} x ${formatUpcomingRepRange(slot.prescribedSet.repsMin, slot.prescribedSet.repsMax)}",
                                    color = FueruColors.TextMuted,
                                    style = FueruType.caption,
                                )
                            }
                            FueruButton(
                                text = "Swap exercise",
                                variant = FueruButtonVariant.Ghost,
                                onClick = {
                                    swapTargetSlot = slot
                                    scope.launch {
                                        substituteOptions = loadSubstitutes(database, slot.exercise, profile.equipmentPreference)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        FueruButton(text = "Back", onClick = onBack, variant = FueruButtonVariant.Ghost)
    }

    val target = swapTargetSlot
    if (target != null) {
        FueruSubstituteDialog(
            options = substituteOptions,
            onPick = { picked ->
                swapTargetSlot = null
                scope.launch {
                    saveExerciseOverride(database, scheduledWorkoutId, target.prescribedSet.id, picked.id)
                    refreshTrigger++
                }
            },
            onDismiss = { swapTargetSlot = null },
        )
    }
}

private fun formatUpcomingRepRange(min: Int, max: Int): String = if (min == max) "$max reps" else "$min-$max reps"
