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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.fueru.app.FueruApplication
import com.fueru.app.R
import com.fueru.app.data.CompletedSession
import com.fueru.app.data.ExerciseProgress
import com.fueru.app.data.PracticeScoring
import com.fueru.app.data.ProgressOverview
import com.fueru.app.data.WeightUnit
import com.fueru.app.data.WeightUnitStore
import com.fueru.app.data.convertToDisplay
import com.fueru.app.data.formatWeightValue
import com.fueru.app.data.loadProgressOverview
import com.fueru.app.data.entity.GuidedSession
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruPracticeHeatmap
import com.fueru.app.ui.components.FueruStatChip
import com.fueru.app.ui.components.FueruTypewriterText
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A different one of these every time someone lands on the Progress tab — same nerdy/gamer-adjacent, slightly cringy voice as Workout/Fuel's rotating lines, themed on consistency over time rather than any single session. */
private val progressGreetings = listOf(
    "consistency: the only stat that actually compounds.",
    "small numbers today, big numbers eventually.",
    "the grind arc never really ends. that's the fun part.",
    "you vs. you, rematch #47.",
    "slow xp is still xp.",
    "every session is a save point.",
    "plot twist: showing up is the strategy.",
    "streaks are just consistency wearing a cape.",
)

/**
 * Overview stats + history (spec Section 5.4), plus every practice's own progress (progress-
 * consolidation round — score/heatmap used to live on each practice's own detail screen and again,
 * smaller, on the Practices list; both now point here instead, so this tab is the one place any
 * kind of backward-looking progress shows up, for workouts and every practice alike). Per-exercise
 * charts and phase-progress visuals are a further follow-up — this pass surfaces the SetLog/
 * ScheduledWorkout history the Workout screen now generates, since there was nothing to show here
 * before that existed.
 */
@Composable
fun ProgressScreen() {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val unit = remember { WeightUnitStore.get(application) }

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    if (userProfile == null) return

    var overview by remember { mutableStateOf<ProgressOverview?>(null) }
    LaunchedEffect(Unit) {
        overview = loadProgressOverview(database)
    }
    val practices by database.practiceDao().observeAll().collectAsState(initial = emptyList())
    val current = overview ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "progress", style = FueruType.wordmarkMd.copy(brush = FueruGradients.fireLogo))
        val greeting = remember { progressGreetings.random() }
        FueruTypewriterText(text = greeting, color = FueruColors.TextSecondary, style = FueruType.headline)

        if (current.completedSessions.isEmpty() && current.exerciseProgress.isEmpty() && practices.isEmpty()) {
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "finish your first workout to start seeing history here.",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
            }
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                FueruStatChip(
                    icon = painterResource(R.drawable.ic_barbell_fill),
                    value = current.completedSessions.size.toString(),
                    label = "sessions",
                )
                FueruStatChip(
                    icon = painterResource(R.drawable.ic_chart_line_up_fill),
                    value = current.exerciseProgress.size.toString(),
                    label = "exercises tracked",
                )
            }
        }

        if (current.completedSessions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text(text = "recent sessions", color = FueruColors.TextSecondary, style = FueruType.title)
                current.completedSessions.take(10).forEach { session ->
                    SessionRow(session)
                }
            }
        }

        if (current.exerciseProgress.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text(text = "per exercise", color = FueruColors.TextSecondary, style = FueruType.title)
                current.exerciseProgress.forEach { progress ->
                    ExerciseProgressRow(progress, unit)
                }
            }
        }

        if (practices.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text(text = "practices", color = FueruColors.TextSecondary, style = FueruType.title)
                practices.forEach { practice ->
                    PracticeProgressCard(practice = practice)
                }
            }
        }
    }
}

// ---- Practice progress (progress-consolidation round) ------------------------------------------

/**
 * One practice's full progress picture — score, 7-/30-day windows, heatmap, and recent guided
 * sessions if applicable. Same content that used to live on PracticeDetailScreen's own "progress"
 * section (and, smaller, on the Practices list) — extracted here since that's the only place any
 * practice's progress should show now.
 */
@Composable
private fun PracticeProgressCard(practice: Practice) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database

    val entries by database.practiceLogEntryDao()
        .observeForPractice(practice.id)
        .collectAsState(initial = emptyList<PracticeLogEntry>())
    val today = remember { LocalDate.now().toString() }
    val score = remember(entries, practice.halfLifeDays) { PracticeScoring.currentScore(entries, practice.halfLifeDays) }
    val window7 = remember(entries) { PracticeScoring.windowCompletionRate(entries, 7, today) }
    val window30 = remember(entries) { PracticeScoring.windowCompletionRate(entries, 30, today) }

    val recentGuidedSessions by database.guidedSessionDao()
        .observeRecentForPractice(practice.id, 5)
        .collectAsState(initial = emptyList<GuidedSession>())

    FueruCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Text(text = practice.name, color = FueruColors.TextPrimary, style = FueruType.bodyLg)
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
            FueruPracticeHeatmap(entries = entries, weeksShown = 12)
            if (practice.guidedSessionEnabled && recentGuidedSessions.isNotEmpty()) {
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

private val sessionDateFormatter = DateTimeFormatter.ofPattern("MMM d")

private fun formatSessionDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(sessionDateFormatter)

@Composable
private fun SessionRow(session: CompletedSession) {
    FueruCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = session.dayLabel, color = FueruColors.TextPrimary, style = FueruType.body)
            Text(
                text = session.scheduledWorkout.completedDate?.let { formatDate(it) } ?: "",
                color = FueruColors.TextMuted,
                style = FueruType.caption,
            )
        }
    }
}

@Composable
private fun ExerciseProgressRow(progress: ExerciseProgress, unit: WeightUnit) {
    val log = progress.lastSetLog
    FueruCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = progress.exercise.name, color = FueruColors.TextPrimary, style = FueruType.body)
            Column {
                Text(
                    text = buildString {
                        if (log.actualWeight != null) {
                            append(formatWeightValue(convertToDisplay(log.actualWeight, unit)))
                            append(unit.label)
                            append(" x ")
                        }
                        append("${log.actualReps ?: 0} reps")
                    },
                    color = FueruColors.TextMuted,
                    style = FueruType.caption,
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
