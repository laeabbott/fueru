package com.fueru.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.fueru.app.FueruApplication
import com.fueru.app.R
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.DateUtils
import com.fueru.app.data.WeightUnit
import com.fueru.app.data.WeightUnitStore
import com.fueru.app.data.WorkoutCelebration
import com.fueru.app.data.WorkoutCelebrationStore
import com.fueru.app.data.WorkoutSessionPlan
import com.fueru.app.data.WorkoutSlot
import com.fueru.app.data.celebration.GiphyApi
import com.fueru.app.data.convertToDisplay
import com.fueru.app.data.convertToKg
import com.fueru.app.data.exerciseHasWeight
import com.fueru.app.data.findTodayOrNextWorkout
import com.fueru.app.data.formatWeightValue
import com.fueru.app.data.loadSubstitutes
import com.fueru.app.data.loadWorkoutSessionPlan
import com.fueru.app.data.saveExerciseOverride
import com.fueru.app.data.suggestProgression
import com.fueru.app.data.WorkoutSessionProgress
import com.fueru.app.data.WorkoutSessionStore
import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.data.entity.SetLog
import com.fueru.app.data.entity.UserProfile
import com.fueru.app.notifications.NotificationHelper
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruRepsScrollPicker
import com.fueru.app.ui.components.FueruSubstituteDialog
import com.fueru.app.ui.components.FueruTag
import com.fueru.app.ui.components.FueruTagVariant
import com.fueru.app.ui.components.FueruTypewriterText
import com.fueru.app.ui.components.FueruWeekdayChip
import com.fueru.app.ui.components.FueruWeightScrollPicker
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Active-session Workout screen (spec Section 5.3). Only "today" is actionable here — tapping the
 * Workout tab on a day with nothing scheduled shows an empty state pointing at This Week, since
 * starting a future day's workout early isn't a supported flow.
 *
 * Leaving mid-session (back button / bottom nav tap) doesn't lose your place: session position
 * (which exercise, which set) is persisted via WorkoutSessionStore and restored on the next "Start"
 * / "Continue workout" tap for the same ScheduledWorkout, so it won't re-log sets you already did.
 * It does *not* remember mid-session exercise substitutions — see WorkoutSessionStore's doc comment.
 */
@Composable
fun WorkoutScreen(onOpenThisWeek: () -> Unit, onSessionActiveChange: (Boolean) -> Unit = {}, onBack: () -> Unit = {}) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var todayWorkout by remember { mutableStateOf<ScheduledWorkout?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(refreshTrigger) {
        todayWorkout = database.scheduledWorkoutDao().getForDate(DateUtils.todayEpochMillis())
        loaded = true
    }

    var activeSession by remember { mutableStateOf<WorkoutSessionPlan?>(null) }

    // Immersive-mode round — lets NavGraph hide the bottom tab bar for the duration of an active
    // session, same as it's already hidden for every step of Resistance Flow (a different route
    // entirely, not something this screen controls). Reported on every recomposition where the
    // active/inactive state actually flips, not just once, since activeSession can go back to null
    // (onFinished) without this composable leaving the WORKOUT route at all.
    LaunchedEffect(activeSession != null) { onSessionActiveChange(activeSession != null) }

    when {
        !loaded -> Unit
        activeSession != null -> {
            ActiveWorkoutSession(
                database = database,
                profile = profile,
                plan = activeSession!!,
                onFinished = {
                    activeSession = null
                    // Update eagerly so the "done for today" state shows immediately, rather than
                    // waiting a frame for the refreshTrigger-driven DB re-read to land.
                    todayWorkout = todayWorkout?.copy(status = "completed", completedDate = System.currentTimeMillis())
                    refreshTrigger++
                },
                onExitRequested = onBack,
            )
        }
        todayWorkout?.status == "planned" -> {
            WorkoutPreview(
                database = database,
                scheduledWorkout = todayWorkout!!,
                onStart = { plan -> activeSession = plan },
            )
        }
        todayWorkout?.status == "completed" -> WorkoutDoneForToday(database = database, scheduledWorkout = todayWorkout!!)
        else -> WorkoutEmptyState(onOpenThisWeek = onOpenThisWeek)
    }
}

// ---- Brand header --------------------------------------------------------------------------------

/**
 * A different one of these every time someone lands on the Workout tab's "at rest" states (empty,
 * pre-session preview, or done-for-today) — deliberately not shown during ActiveWorkoutSession
 * itself, since that screen is already tight on vertical space and this is decoration, not
 * function. Voice: nerdy/gamer/anime-adjacent, a little cringy on purpose — matches the "arc
 * complete," "level up." language already used elsewhere in this app.
 */
private val workoutGreetings = listOf(
    "protagonist arc: loading.",
    "main character energy, incoming.",
    "xp gains only go up from here.",
    "plot armor doesn't build itself. lift.",
    "respect points: earned, not given.",
    "no skip button on leg day, sorry.",
    "this is the training montage part.",
    "insert dramatic power-up music here.",
)

@Composable
private fun WorkoutBrandHeader() {
    val greeting = remember { workoutGreetings.random() }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Text(text = "fueru", style = FueruType.wordmarkMd.copy(brush = FueruGradients.fireLogo))
        FueruTypewriterText(text = greeting, color = FueruColors.TextSecondary, style = FueruType.headline)
    }
}

// ---- Empty / done states ------------------------------------------------------------------------

@Composable
private fun WorkoutEmptyState(onOpenThisWeek: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        WorkoutBrandHeader()
        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = "nothing on deck today", color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                Text(
                    text = "Plan This Week to line up your next session.",
                    color = FueruColors.TextMuted,
                    style = FueruType.caption,
                )
                FueruButton(text = "Plan this week", onClick = onOpenThisWeek)
            }
        }
    }
}

/**
 * Reloads today's already-finished session from durable state (not from anything held in
 * [ActiveWorkoutSession], which no longer exists once the user's navigated away and back) so the
 * celebration — gif, "what you did," next-up preview — stays viewable for the rest of the day, not
 * just in the moment right after finishing. [WorkoutCelebrationStore] carries the one bit that
 * can't be recomputed (which gif got picked, so a later visit shows the same one, not a re-roll);
 * everything else is a fresh, cheap DB read.
 */
@Composable
private fun WorkoutDoneForToday(database: AppDatabase, scheduledWorkout: ScheduledWorkout) {
    val context = LocalContext.current
    val unit = remember { WeightUnitStore.get(context) }

    var loaded by remember(scheduledWorkout.id) { mutableStateOf(false) }
    var celebration by remember(scheduledWorkout.id) { mutableStateOf<WorkoutCelebration?>(null) }
    var dayLabel by remember(scheduledWorkout.id) { mutableStateOf("today's arc") }
    var totalSets by remember(scheduledWorkout.id) { mutableIntStateOf(0) }
    var accomplishment by remember(scheduledWorkout.id) { mutableStateOf<List<AccomplishedExercise>>(emptyList()) }
    var nextWorkoutPreview by remember(scheduledWorkout.id) { mutableStateOf<NextWorkoutPreview?>(null) }

    LaunchedEffect(scheduledWorkout.id) {
        celebration = WorkoutCelebrationStore.get(context, scheduledWorkout.id)
        val plan = loadWorkoutSessionPlan(database, scheduledWorkout)
        val setLogs = database.setLogDao().getForScheduledWorkout(scheduledWorkout.id)
        totalSets = setLogs.size
        if (plan != null) {
            dayLabel = plan.dayLabel
            accomplishment = summarizeAccomplishment(setLogs, plan.slots, unit)
        }
        nextWorkoutPreview = loadNextWorkoutPreview(database)
        loaded = true
    }

    if (!loaded) return

    WorkoutCelebrationCard(
        dayLabel = dayLabel,
        totalSets = totalSets,
        gifUrl = celebration?.gifUrl,
        accomplishment = accomplishment,
        nextWorkout = nextWorkoutPreview,
        doneButton = null,
    )
}

// ---- Preview (pre-session) ----------------------------------------------------------------------

@Composable
private fun WorkoutPreview(
    database: AppDatabase,
    scheduledWorkout: ScheduledWorkout,
    onStart: (WorkoutSessionPlan) -> Unit,
) {
    var plan by remember(scheduledWorkout.id) { mutableStateOf<WorkoutSessionPlan?>(null) }
    LaunchedEffect(scheduledWorkout.id) {
        plan = loadWorkoutSessionPlan(database, scheduledWorkout)
    }
    val current = plan ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        WorkoutBrandHeader()
        Text(text = current.dayLabel, color = FueruColors.TextPrimary, style = FueruType.title)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            current.slots.forEach { slot ->
                FueruCard(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = slot.exercise.name, color = FueruColors.TextPrimary, style = FueruType.body)
                        Text(
                            text = "${slot.prescribedSet.sets} x ${formatRepRange(slot.prescribedSet.repsMin, slot.prescribedSet.repsMax)}",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                }
            }
        }

        FueruButton(text = "Start workout", onClick = { onStart(current) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun formatRepRange(min: Int, max: Int): String = if (min == max) "$max reps" else "$min-$max reps"

// ---- Active session ------------------------------------------------------------------------------

/** Inactivity-check round — how long with zero touch activity before "still there?" fires. */
private const val INACTIVITY_THRESHOLD_MS = 5 * 60 * 1000L

@Composable
private fun ActiveWorkoutSession(
    database: AppDatabase,
    profile: UserProfile,
    plan: WorkoutSessionPlan,
    onFinished: () -> Unit,
    onExitRequested: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val unit = remember { WeightUnitStore.get(context) }
    var showExitConfirm by remember { mutableStateOf(false) }

    val resumed = remember(plan.scheduledWorkout.id) { WorkoutSessionStore.resumeFor(context, plan.scheduledWorkout.id) }
    val initialSlotIndex = (resumed?.slotIndex ?: 0).coerceIn(0, plan.slots.size - 1)
    val initialSetNumber = (resumed?.setNumber ?: 1).coerceIn(1, plan.slots[initialSlotIndex].prescribedSet.sets)

    var slots by remember(plan.scheduledWorkout.id) { mutableStateOf(plan.slots) }
    var slotIndex by remember(plan.scheduledWorkout.id) { mutableIntStateOf(initialSlotIndex) }
    var setNumber by remember(plan.scheduledWorkout.id) { mutableIntStateOf(initialSetNumber) }
    var totalLogged by remember(plan.scheduledWorkout.id) { mutableIntStateOf(0) }
    var finished by remember(plan.scheduledWorkout.id) { mutableStateOf(false) }
    var celebrationGifUrl by remember(plan.scheduledWorkout.id) { mutableStateOf<String?>(null) }
    var nextWorkoutPreview by remember(plan.scheduledWorkout.id) { mutableStateOf<NextWorkoutPreview?>(null) }
    var sessionSetLogs by remember(plan.scheduledWorkout.id) { mutableStateOf<List<SetLog>>(emptyList()) }

    // Prefetched the moment the session starts, not at completion — a real workout takes minutes,
    // so by the time the last set is logged the network round-trip to Giphy is long done, and the
    // completion screen shows a gif instantly instead of visibly waiting on it. Two separate
    // LaunchedEffects so the regular and milestone fetches run concurrently, not one after another.
    var prefetchedRegularGif by remember(plan.scheduledWorkout.id) { mutableStateOf<String?>(null) }
    var prefetchedMilestoneGif by remember(plan.scheduledWorkout.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(plan.scheduledWorkout.id) { prefetchedRegularGif = GiphyApi.randomCelebrationGifUrl(milestone = false) }
    LaunchedEffect(plan.scheduledWorkout.id) { prefetchedMilestoneGif = GiphyApi.randomCelebrationGifUrl(milestone = true) }

    LaunchedEffect(finished) {
        if (finished) {
            nextWorkoutPreview = loadNextWorkoutPreview(database)
            sessionSetLogs = database.setLogDao().getForScheduledWorkout(plan.scheduledWorkout.id)
        }
    }

    // Immersive mode round — once the session's actually finished, the completion card has its own
    // "Done" button and there's no more in-progress state to protect, so back behaves normally
    // again. Progress up to any point is already durable via WorkoutSessionStore either way — this
    // is purely about not losing your place in the flow to an accidental back press/gesture.
    BackHandler(enabled = !finished) { showExitConfirm = true }
    if (showExitConfirm) {
        ExitConfirmDialog(
            onKeepGoing = { showExitConfirm = false },
            onEnd = { showExitConfirm = false; onExitRequested() },
        )
    }

    // Inactivity-check round — "if the user does nothing for 5 minutes, check they're still
    // engaged." lastInteraction is stamped by a passive touch watcher on the root Column below
    // (every pointer event, not consumed — the existing picker/button gestures are untouched).
    // Polls rather than a single delayed alarm so it naturally re-arms on real activity without
    // cancel/reschedule bookkeeping; only fires once per idle stretch (won't fire again until
    // either real activity resets the clock, or another full 5 minutes passes since the last fire).
    var lastInteraction by remember(plan.scheduledWorkout.id) { mutableStateOf(System.currentTimeMillis()) }
    var lastInactivityNotification by remember(plan.scheduledWorkout.id) { mutableStateOf<Long?>(null) }
    LaunchedEffect(plan.scheduledWorkout.id, finished) {
        if (finished) return@LaunchedEffect
        while (true) {
            delay(30_000)
            val now = System.currentTimeMillis()
            val idleFor = now - lastInteraction
            val sinceLastNotification = lastInactivityNotification?.let { now - it }
            if (idleFor >= INACTIVITY_THRESHOLD_MS && (sinceLastNotification == null || sinceLastNotification >= INACTIVITY_THRESHOLD_MS)) {
                NotificationHelper.notifyWorkoutInactivity(context)
                lastInactivityNotification = now
            }
        }
    }

    if (finished) {
        WorkoutCompleteCard(
            dayLabel = plan.dayLabel,
            totalSets = totalLogged,
            gifUrl = celebrationGifUrl,
            nextWorkout = nextWorkoutPreview,
            accomplishment = summarizeAccomplishment(sessionSetLogs, slots, unit),
            onDone = onFinished,
        )
        return
    }

    val slot = slots[slotIndex]
    val hasWeight = exerciseHasWeight(slot.exercise.equipment)

    // Computed once per exercise (not per set) and held constant across every set of that
    // exercise — the suggestion should only move between real sessions (per the progression
    // doc's week/every-other-week cadence), never mid-session from set to set.
    var targetReps by remember(slotIndex) { mutableIntStateOf(slot.prescribedSet.repsMin) }
    var suggestedWeightKg by remember(slotIndex) { mutableStateOf<Float?>(null) }
    var suggestionFromHistory by remember(slotIndex) { mutableStateOf(false) }
    var justBumpedWeight by remember(slotIndex) { mutableStateOf(false) }
    var showInstructions by remember(slotIndex) { mutableStateOf(false) }
    var showSubstitutePicker by remember { mutableStateOf(false) }
    var substituteOptions by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    // Values, not strings — a scroll picker always has a definite selection, so there's no
    // "invalid text" state to track the way the old free-text fields needed.
    var weightValue by remember(slotIndex, setNumber) {
        mutableStateOf(if (hasWeight) suggestedWeightKg?.let { convertToDisplay(it, unit) } ?: 0f else 0f)
    }
    var repsValue by remember(slotIndex, setNumber) { mutableIntStateOf(targetReps) }
    var rpe by remember(slotIndex, setNumber) { mutableStateOf<Int?>(null) }
    var showShortfallDialog by remember(slotIndex, setNumber) { mutableStateOf(false) }

    LaunchedEffect(slot.exercise.id) {
        val result = suggestProgression(database, slot.exercise, slot.prescribedSet, profile, unit, plan.scheduledWorkout.id)
        targetReps = result.targetReps
        repsValue = result.targetReps
        suggestionFromHistory = result.weightFromHistory
        justBumpedWeight = result.justBumpedWeight
        suggestedWeightKg = result.suggestedWeightKg
        if (hasWeight) {
            weightValue = result.suggestedWeightKg?.let { convertToDisplay(it, unit) } ?: 0f
        }
    }

    fun applySubstitution(picked: Exercise) {
        slots = slots.toMutableList().also { it[slotIndex] = it[slotIndex].copy(exercise = picked) }
        scope.launch {
            saveExerciseOverride(database, plan.scheduledWorkout.id, slot.prescribedSet.id, picked.id)
        }
    }

    fun commitSetLog(reason: String?) {
        val reps = repsValue
        val weight = if (hasWeight) convertToKg(weightValue, unit) else null
        scope.launch {
            database.setLogDao().insert(
                SetLog(
                    scheduledWorkoutId = plan.scheduledWorkout.id,
                    exerciseId = slot.exercise.id,
                    setNumber = setNumber,
                    prescribedWeight = suggestedWeightKg,
                    prescribedReps = targetReps,
                    actualWeight = weight,
                    actualReps = reps,
                    shortfallReason = reason,
                    rpe = rpe,
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }
        totalLogged += 1
        showShortfallDialog = false
        if (setNumber < slot.prescribedSet.sets) {
            setNumber += 1
        } else if (slotIndex < slots.size - 1) {
            slotIndex += 1
            setNumber = 1
        } else {
            scope.launch {
                database.scheduledWorkoutDao().update(
                    plan.scheduledWorkout.copy(status = "completed", completedDate = System.currentTimeMillis()),
                )
                // No points/streak ledger anymore — "milestone" is just every 5th completed
                // workout ever, derived on the spot, purely to pick a livelier Giphy tag pool.
                // Nothing reads or displays this number; it only ever selects which prefetch to use.
                val isMilestone = database.scheduledWorkoutDao().getAllCompleted().size % 5 == 0
                // Uses whichever prefetch already landed — only falls back to a fresh (slower)
                // fetch if the session was too short for the prefetch to finish in time. This is
                // also what gets persisted, so a later visit shows this exact gif, not a re-roll.
                val gifUrl = (if (isMilestone) prefetchedMilestoneGif else prefetchedRegularGif)
                    ?: GiphyApi.randomCelebrationGifUrl(isMilestone)
                celebrationGifUrl = gifUrl
                WorkoutCelebrationStore.save(context, WorkoutCelebration(plan.scheduledWorkout.id, gifUrl))
            }
            finished = true
        }
        if (finished) {
            WorkoutSessionStore.clear(context)
        } else {
            WorkoutSessionStore.save(context, WorkoutSessionProgress(plan.scheduledWorkout.id, slotIndex, setNumber))
        }
    }

    fun onLogTapped() {
        if (repsValue < targetReps) {
            showShortfallDialog = true
        } else {
            commitSetLog(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Inactivity-check round — observes every touch at the Initial pass (before any child
            // gesture handler sees it) purely to stamp lastInteraction; never consumes, so existing
            // picker/button/tap handling elsewhere in this screen is completely unaffected.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteraction = System.currentTimeMillis()
                    }
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = plan.dayLabel.uppercase(), color = FueruColors.TextMuted, style = FueruType.overline)
            Text(
                text = "exercise ${slotIndex + 1} of ${slots.size} · set $setNumber of ${slot.prescribedSet.sets}",
                color = FueruColors.TextSecondary,
                style = FueruType.caption,
            )
        }

        ExerciseFormImages(
            paths = slot.exercise.imageAssetPaths,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(Radius.lg)),
        )

        Text(text = slot.exercise.name, color = FueruColors.TextPrimary, style = FueruType.headline)

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
            FueruTag(text = "tempo ${slot.prescribedSet.tempo}")
            Text(
                text = if (showInstructions) "hide how-to" else "how to",
                color = FueruColors.Fire4,
                style = FueruType.caption,
                modifier = Modifier.clickable { showInstructions = !showInstructions },
            )
            if (slot.prescribedSet.supersetGroup != null) {
                FueruTag(text = "superset ${slot.prescribedSet.supersetGroup}", variant = FueruTagVariant.Fire)
            }
            if (slot.prescribedSet.isTensionFocus) {
                FueruTag(text = "tension focus", variant = FueruTagVariant.Fire)
            }
            if (slot.prescribedSet.isDropSetFinal && setNumber == slot.prescribedSet.sets) {
                FueruTag(text = "drop set", variant = FueruTagVariant.Fire)
            }
        }

        if (slot.prescribedSet.comment != null) {
            Text(text = slot.prescribedSet.comment, color = FueruColors.TextMuted, style = FueruType.caption)
        }

        if (showInstructions) {
            Text(text = slot.exercise.instructions, color = FueruColors.TextMuted, style = FueruType.caption)
        }

        if (setNumber == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), modifier = Modifier.fillMaxWidth()) {
                FueruButton(
                    text = "Suggest a different exercise",
                    variant = FueruButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            val suggestions = loadSubstitutes(database, slot.exercise, profile.equipmentPreference)
                            val autoPick = suggestions.firstOrNull()
                            if (autoPick != null) {
                                applySubstitution(autoPick)
                            } else {
                                // Nothing shares this muscle group — fall back to the full picker,
                                // which shows its own "no alternatives found" message.
                                substituteOptions = suggestions
                                showSubstitutePicker = true
                            }
                        }
                    },
                )
                FueruButton(
                    text = "Pick a specific exercise",
                    variant = FueruButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            substituteOptions = loadSubstitutes(database, slot.exercise, profile.equipmentPreference)
                            showSubstitutePicker = true
                        }
                    },
                )
            }
        }

        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                if (hasWeight) {
                    val targetWeightDisplay = suggestedWeightKg?.let { formatWeightValue(convertToDisplay(it, unit)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Weight (${unit.label})" + (targetWeightDisplay?.let { " · target $it" } ?: ""),
                                color = if (targetWeightDisplay != null) FueruColors.Fire4 else FueruColors.TextSecondary,
                                style = FueruType.caption,
                            )
                            FueruWeightScrollPicker(
                                value = weightValue,
                                onValueChange = { weightValue = it },
                                recommendedValue = suggestedWeightKg?.let { convertToDisplay(it, unit) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reps · target $targetReps",
                                color = FueruColors.Fire4,
                                style = FueruType.caption,
                            )
                            FueruRepsScrollPicker(
                                reps = repsValue,
                                onRepsChange = { repsValue = it },
                                recommendedReps = targetReps,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (suggestedWeightKg != null) {
                        Text(
                            text = when {
                                justBumpedWeight -> "nice — weight went up from last time"
                                suggestionFromHistory -> "based on your last session"
                                else -> "starting estimate for your level"
                            },
                            color = if (justBumpedWeight) FueruColors.Fire4 else FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                } else {
                    Text(text = "Reps · target $targetReps", color = FueruColors.Fire4, style = FueruType.caption)
                    FueruRepsScrollPicker(
                        reps = repsValue,
                        onRepsChange = { repsValue = it },
                        recommendedReps = targetReps,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(text = "RPE (optional)", color = FueruColors.TextSecondary, style = FueruType.caption)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        (6..10).forEach { value ->
                            FueruWeekdayChip(
                                label = value.toString(),
                                selected = rpe == value,
                                onClick = { rpe = if (rpe == value) null else value },
                            )
                        }
                    }
                }

                FueruButton(text = "Log set", onClick = ::onLogTapped, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSubstitutePicker) {
        FueruSubstituteDialog(
            options = substituteOptions,
            onPick = { picked ->
                applySubstitution(picked)
                showSubstitutePicker = false
            },
            onDismiss = { showSubstitutePicker = false },
        )
    }

    if (showShortfallDialog) {
        ShortfallDialog(
            targetReps = targetReps,
            actualReps = repsValue,
            weightDisplay = if (hasWeight) "${formatWeightValue(weightValue)} ${unit.label}" else null,
            onCouldnt = { commitSetLog("couldnt") },
            onChoseNotTo = { commitSetLog("choseNotTo") },
            onDismiss = { showShortfallDialog = false },
        )
    }
}

/** Bundled exercises reference a local asset path; the wider on-demand catalog stores a full https URL — Coil handles both, this just picks the right model string. */
private fun exerciseImageModel(path: String): String = if (path.startsWith("http")) path else "file:///android_asset/$path"

@Composable
private fun ExerciseFormImages(paths: List<String>, modifier: Modifier = Modifier) {
    if (paths.isEmpty()) {
        Box(modifier = modifier.background(FueruColors.SurfaceRaised))
        return
    }
    if (paths.size == 1) {
        AsyncImage(
            model = exerciseImageModel(paths[0]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "exerciseCrossfade")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "exerciseCrossfadeAlpha",
    )
    Box(modifier = modifier) {
        AsyncImage(
            model = exerciseImageModel(paths[0]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(1f - t),
        )
        AsyncImage(
            model = exerciseImageModel(paths[1]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(t),
        )
    }
}

// ---- Dialogs ---------------------------------------------------------------------------------

/**
 * Immersive mode round — intercepts an in-app back press/gesture during an active session so it
 * can't accidentally abandon the workout. Only guards system back, not Home/app-switching/
 * notification taps, which never route through BackHandler at all.
 */
@Composable
private fun ExitConfirmDialog(onKeepGoing: () -> Unit, onEnd: () -> Unit) {
    Dialog(onDismissRequest = onKeepGoing) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "end this workout?", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(
                    text = "Your progress is saved — you can pick up right where you left off later.",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
                FueruButton(text = "Keep going", onClick = onKeepGoing, modifier = Modifier.fillMaxWidth())
                FueruButton(text = "End workout", variant = FueruButtonVariant.Ghost, onClick = onEnd, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ShortfallDialog(
    targetReps: Int,
    actualReps: Int,
    weightDisplay: String?,
    onCouldnt: () -> Unit,
    onChoseNotTo: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "fewer reps than the target", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(
                    text = "Target: $targetReps reps" + (weightDisplay?.let { " at $it" } ?: "") + "\nYou logged: $actualReps reps.",
                    color = FueruColors.TextSecondary,
                    style = FueruType.body,
                )
                Text(
                    text = "Couldn't get them, or chose not to push for them?",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
                FueruButton(text = "Couldn't get them", variant = FueruButtonVariant.Secondary, onClick = onCouldnt, modifier = Modifier.fillMaxWidth())
                FueruButton(text = "Chose not to", variant = FueruButtonVariant.Secondary, onClick = onChoseNotTo, modifier = Modifier.fillMaxWidth())
                FueruButton(text = "Let me edit", variant = FueruButtonVariant.Ghost, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private data class AccomplishedExercise(
    val name: String,
    val setCount: Int,
    val topWeightDisplay: String?,
    val topReps: Int,
)

/**
 * Summarizes the session's actual logged sets for the completion screen — one row per exercise,
 * in the order it was performed, "top set" being whichever logged set had the heaviest weight (or,
 * for reps-only exercises, the most reps). Reads from [slots] (the session's final, possibly
 * substituted, exercise list) rather than the program's original plan, so a mid-session swap shows
 * under the exercise actually done.
 */
private fun summarizeAccomplishment(setLogs: List<SetLog>, slots: List<WorkoutSlot>, unit: WeightUnit): List<AccomplishedExercise> {
    val setsByExercise = setLogs.groupBy { it.exerciseId }
    return slots.mapNotNull { slot ->
        val sets = setsByExercise[slot.exercise.id]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val topSet = sets.maxWithOrNull(compareBy({ it.actualWeight ?: -1f }, { it.actualReps ?: 0 }))
        AccomplishedExercise(
            name = slot.exercise.name,
            setCount = sets.size,
            topWeightDisplay = topSet?.actualWeight?.let { "${formatWeightValue(convertToDisplay(it, unit))} ${unit.label}" },
            topReps = topSet?.actualReps ?: 0,
        )
    }
}

private data class NextWorkoutPreview(
    val dayLabel: String,
    val scheduledDate: Long,
    val scheduledTime: Long?,
    val exerciseCount: Int,
)

private suspend fun loadNextWorkoutPreview(database: AppDatabase): NextWorkoutPreview? {
    // findTodayOrNextWorkout falls through to "next planned" once today's row is "completed",
    // which it already is by the time this runs — so this always resolves to the true next one.
    val result = findTodayOrNextWorkout(database) ?: return null
    val programDay = database.programDayDao().getById(result.scheduledWorkout.programDayId) ?: return null
    val exerciseCount = database.prescribedSetDao().getForProgramDay(programDay.id).size
    return NextWorkoutPreview(
        dayLabel = programDay.dayLabel,
        scheduledDate = result.scheduledWorkout.scheduledDate,
        scheduledTime = result.scheduledWorkout.scheduledTime,
        exerciseCount = exerciseCount,
    )
}

private fun formatNextWorkoutDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d"))

@Composable
private fun WorkoutCompleteCard(
    dayLabel: String,
    totalSets: Int,
    gifUrl: String?,
    nextWorkout: NextWorkoutPreview?,
    accomplishment: List<AccomplishedExercise>,
    onDone: () -> Unit,
) {
    WorkoutCelebrationCard(
        dayLabel = dayLabel,
        totalSets = totalSets,
        gifUrl = gifUrl,
        accomplishment = accomplishment,
        nextWorkout = nextWorkout,
        doneButton = { FueruButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth()) },
    )
}

/**
 * Shared rendering for "a workout just finished" (right after the last set, [doneButton] set) and
 * "here's today's already-finished workout" (returning to the tab later, [doneButton] null — there's
 * nothing left to dismiss, this is just the day's standing state). Both call sites resolve [gifUrl]
 * themselves — the live session from in-memory state, a later revisit from
 * [WorkoutCelebrationStore] — so this composable only ever renders already-resolved data.
 */
@Composable
private fun WorkoutCelebrationCard(
    dayLabel: String,
    totalSets: Int,
    gifUrl: String?,
    accomplishment: List<AccomplishedExercise>,
    nextWorkout: NextWorkoutPreview?,
    doneButton: (@Composable () -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxSize().padding(Spacing.space5), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4),
        ) {
            WorkoutBrandHeader()
            FueruCard(modifier = Modifier.fillMaxWidth(), glow = true) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    // Falls back to the plain checkmark — silently, no error text — if the gif
                    // never arrived (no key configured, offline, no match for the tag) or this
                    // completion predates the celebration-store feature entirely.
                    if (gifUrl != null) {
                        AsyncImage(
                            model = gifUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(Radius.md)),
                        )
                    } else {
                        Icon(painter = painterResource(R.drawable.ic_check_circle_fill), contentDescription = null, tint = FueruColors.Fire4)
                    }
                    Text(text = "$dayLabel done", color = FueruColors.TextPrimary, style = FueruType.headline)
                    Text(text = "$totalSets sets logged. Nice work.", color = FueruColors.TextMuted, style = FueruType.body)
                }
            }
            if (accomplishment.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(text = "what you did", color = FueruColors.TextMuted, style = FueruType.overline)
                    FueruCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                            accomplishment.forEach { exercise ->
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = exercise.name, color = FueruColors.TextPrimary, style = FueruType.body)
                                    Text(
                                        text = "${exercise.setCount} sets · top set " +
                                            (exercise.topWeightDisplay?.let { "$it × ${exercise.topReps}" } ?: "${exercise.topReps} reps"),
                                        color = FueruColors.TextMuted,
                                        style = FueruType.caption,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (nextWorkout != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(text = "next up", color = FueruColors.TextMuted, style = FueruType.overline)
                    FueruCard(modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = nextWorkout.dayLabel, color = FueruColors.TextPrimary, style = FueruType.body)
                            Text(
                                text = formatNextWorkoutDate(nextWorkout.scheduledDate) +
                                    (nextWorkout.scheduledTime?.let { " · ${DateUtils.formatTime(it)}" } ?: "") +
                                    " · ${nextWorkout.exerciseCount} exercises",
                                color = FueruColors.TextMuted,
                                style = FueruType.caption,
                            )
                        }
                    }
                }
            }
            doneButton?.invoke()
        }
    }
}
