package com.fueru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.fueru.app.FueruApplication
import com.fueru.app.R
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.DateUtils
import com.fueru.app.data.NutritionSnapshot
import com.fueru.app.data.TodayPracticeSlot
import com.fueru.app.data.computeTodaysPracticePlan
import com.fueru.app.data.findTodayOrNextWorkout
import com.fueru.app.data.loadNutritionSnapshot
import com.fueru.app.data.entity.DailyNutritionLog
import com.fueru.app.data.entity.UserProfile
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruMacroSummaryRow
import com.fueru.app.ui.components.FueruNutritionRow
import com.fueru.app.ui.components.FueruTag
import com.fueru.app.ui.components.FueruTagVariant
import com.fueru.app.ui.components.FueruTypewriterText
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A handful of phrasings so the greeting isn't the exact same text on every visit. */
private val greetingTemplates = listOf(
    "hey, %s",
    "welcome back, %s",
    "good to see you, %s",
    "let's go, %s",
    "ready, %s?",
    "hey there, %s",
    "back at it, %s",
)

@Composable
fun HomeScreen(
    onOpenThisWeek: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPractices: () -> Unit,
    onOpenPractice: (Long) -> Unit,
    onOpenFuel: () -> Unit,
    onStartFuwari: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    var workoutRefresh by remember { mutableIntStateOf(0) }
    var workoutInfo by remember { mutableStateOf<HomeWorkoutInfo?>(null) }
    var workoutLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(workoutRefresh) {
        workoutInfo = loadHomeWorkoutInfo(database)
        workoutLoaded = true
    }

    // Recomputed each time Home enters composition (e.g. returning from logging a practice), same
    // "cheap DB read on entry" pattern the workout card already uses via workoutRefresh.
    var todaysPractices by remember { mutableStateOf<List<TodayPracticeSlot>>(emptyList()) }
    LaunchedEffect(workoutRefresh) {
        todaysPractices = computeTodaysPracticePlan(database)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.space5, vertical = Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "welcome back",
                color = FueruColors.TextMuted,
                style = FueruType.overline,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                // Practices lives here (not the bottom nav) until the modules-building phase
                // decides which tabs become module entry points — see the pivot plan.
                Text(
                    text = "practices",
                    color = FueruColors.TextMuted,
                    style = FueruType.caption,
                    modifier = Modifier.clickable { onOpenPractices() },
                )
                Text(
                    text = "settings",
                    color = FueruColors.TextMuted,
                    style = FueruType.caption,
                    modifier = Modifier.clickable { onOpenSettings() },
                )
            }
        }
        // Re-rolled each time this composable enters composition (leaving the tab and coming back
        // counts), not on every recomposition — remember without a changing key holds it steady
        // while the user is actually looking at the screen.
        val greeting = remember(profile.displayName) { greetingTemplates.random().format(profile.displayName) }
        FueruTypewriterText(text = greeting, color = FueruColors.TextPrimary, style = FueruType.headline)

        WorkoutCard(
            info = workoutInfo,
            loaded = workoutLoaded,
            onOpenThisWeek = onOpenThisWeek,
            onStartWorkout = onStartWorkout,
        )

        if (todaysPractices.isNotEmpty()) {
            TodaysPracticesCard(slots = todaysPractices, onOpenPractice = onOpenPractice)
        }

        if (profile.foodTrackingEnabled) {
            FoodChecklistCard(database = database, profile = profile, onOpenFuel = onOpenFuel)
        }

        FuwariCard(onStartFuwari = onStartFuwari)
    }
}

// ---- Fuwari quick-start card --------------------------------------------------------------------

/** fuwari round — a foundational card, always shown (fuwari is seeded on first launch, see ensureFuwariSeeded), same one-tap-direct-action language as the Workout/Fuel cards above it. */
@Composable
private fun FuwariCard(onStartFuwari: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Text(text = "fuwari", style = FueruType.wordmarkSm.copy(brush = FueruGradients.fireLogo))
        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = "a quiet minute, whenever you need one", color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                FueruButton(text = "Begin session", onClick = onStartFuwari, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ---- Workout card -----------------------------------------------------------------------------

private data class HomeWorkoutInfo(
    val isToday: Boolean,
    val scheduledDate: Long,
    val scheduledTime: Long?,
    val dayLabel: String,
    val exerciseCount: Int,
)

private suspend fun loadHomeWorkoutInfo(database: AppDatabase): HomeWorkoutInfo? {
    val result = findTodayOrNextWorkout(database) ?: return null
    val programDay = database.programDayDao().getById(result.scheduledWorkout.programDayId) ?: return null
    val exerciseCount = database.prescribedSetDao().getForProgramDay(programDay.id).size
    return HomeWorkoutInfo(
        isToday = result.isToday,
        scheduledDate = result.scheduledWorkout.scheduledDate,
        scheduledTime = result.scheduledWorkout.scheduledTime,
        dayLabel = programDay.dayLabel,
        exerciseCount = exerciseCount,
    )
}

@Composable
private fun WorkoutCard(info: HomeWorkoutInfo?, loaded: Boolean, onOpenThisWeek: () -> Unit, onStartWorkout: () -> Unit) {
    Text(text = "fueru", style = FueruType.wordmarkSm.copy(brush = FueruGradients.fireLogo))
    when {
        !loaded -> Unit
        info == null -> {
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "nothing on the calendar yet",
                        color = FueruColors.TextPrimary,
                        style = FueruType.bodyLg,
                    )
                    Text(
                        text = "Plan This Week to get your next workout lined up.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    FueruButton(text = "Plan this week", onClick = onOpenThisWeek)
                }
            }
        }
        info.isToday -> {
            val interactionSource = remember { MutableInteractionSource() }
            FueruCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onStartWorkout),
                glow = true,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barbell_fill),
                        contentDescription = null,
                        tint = FueruColors.Fire4,
                        modifier = Modifier,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = info.dayLabel, color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                        Text(
                            text = "${info.exerciseCount} exercises · " +
                                (info.scheduledTime?.let { DateUtils.formatTime(it) } ?: "today"),
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                    FueruTag(text = "start", variant = FueruTagVariant.Fire)
                }
            }
        }
        else -> {
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barbell),
                        contentDescription = null,
                        tint = FueruColors.TextMuted,
                        modifier = Modifier,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "next up: ${info.dayLabel}", color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                        Text(
                            text = formatDate(info.scheduledDate) +
                                (info.scheduledTime?.let { " · ${DateUtils.formatTime(it)}" } ?: "") +
                                " · ${info.exerciseCount} exercises",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

// ---- Today's practices card --------------------------------------------------------------------

/**
 * Everything due today per PracticeScheduler.computeTodaysPracticePlan — project brief §5's
 * "today's plan." No inline log buttons here on purpose: a day can have several due practices at
 * once (expected, not an edge case, per the brief), so this stays a tap-through list rather than
 * cramming four log buttons per row; PracticeDetailScreen already has those.
 */
@Composable
private fun TodaysPracticesCard(slots: List<TodayPracticeSlot>, onOpenPractice: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(text = "today's practices", color = FueruColors.TextSecondary, style = FueruType.overline)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            slots.forEach { today ->
                FueruCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPractice(today.practice.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        Text(text = today.practice.name, color = FueruColors.TextPrimary, style = FueruType.body, modifier = Modifier.weight(1f))
                        when {
                            today.loggedStatus != null -> FueruTag(text = today.loggedStatus)
                            today.isOverdue -> FueruTag(text = "overdue", variant = FueruTagVariant.Danger)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

// ---- Food checklist card -----------------------------------------------------------------------

@Composable
private fun FoodChecklistCard(database: AppDatabase, profile: UserProfile, onOpenFuel: () -> Unit) {
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<NutritionSnapshot?>(null) }
    LaunchedEffect(refresh) {
        snapshot = loadNutritionSnapshot(database, profile)
    }
    val current = snapshot ?: return

    fun update(mutate: (DailyNutritionLog) -> DailyNutritionLog) {
        scope.launch {
            database.dailyNutritionLogDao().upsert(mutate(current.log))
            refresh++
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Text(text = "fuel", style = FueruType.wordmarkSm.copy(brush = FueruGradients.fireLogo))
        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                if (current.mode == "mealBalance") {
                    FueruNutritionRow("Protein portions", current.log.proteinPortions, 4f, step = 1f) { delta ->
                        update { it.copy(proteinPortions = (it.proteinPortions + delta).coerceAtLeast(0f)) }
                    }
                    FueruNutritionRow("Carb portions", current.log.carbPortions, 4f, step = 1f) { delta ->
                        update { it.copy(carbPortions = (it.carbPortions + delta).coerceAtLeast(0f)) }
                    }
                    FueruNutritionRow("Fruit/veg portions", current.log.fruitVegPortions, 4f, step = 1f) { delta ->
                        update { it.copy(fruitVegPortions = (it.fruitVegPortions + delta).coerceAtLeast(0f)) }
                    }
                } else {
                    // Macros mode logs real foods now (Fuel tab, USDA-backed) — this card is just a
                    // read-only glance at today's running total, not another place to edit it.
                    // Protein/carbs/fat lead here, not the kcal total — hitting all three macros
                    // matters more day-to-day than the calorie count, so kcal is a small footnote.
                    val today = remember { DateUtils.todayEpochMillis() }
                    val loggedFoodsToday by database.foodLogEntryDao()
                        .observeForDate(today)
                        .collectAsState(initial = emptyList())
                    val loggedProtein = loggedFoodsToday.sumOf { it.proteinG.toDouble() }.toFloat()
                    val loggedCarbs = loggedFoodsToday.sumOf { it.carbsG.toDouble() }.toFloat()
                    val loggedFat = loggedFoodsToday.sumOf { it.fatG.toDouble() }.toFloat()
                    val loggedKcal = loggedFoodsToday.sumOf { it.kcal }

                    FueruMacroSummaryRow("Protein", loggedProtein, current.targets.proteinG.toFloat())
                    FueruMacroSummaryRow("Carbs", loggedCarbs, current.targets.carbG.toFloat())
                    FueruMacroSummaryRow("Fat", loggedFat, current.targets.fatG.toFloat())
                    Text(
                        text = "$loggedKcal / ${current.targets.tdeeKcal} kcal · " +
                            "${loggedFoodsToday.size} food${if (loggedFoodsToday.size == 1) "" else "s"} logged",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    FueruButton(text = "Log food", onClick = onOpenFuel, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
