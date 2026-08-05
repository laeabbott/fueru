package com.fueru.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fueru.app.FueruApplication
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.ResistanceFlowPrefs
import com.fueru.app.data.entity.GuidedSession
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.data.entity.ResistanceSession
import com.fueru.app.ui.components.FueruBreathingAnimation
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruIgniteHoldButton
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class FlowStep { NAME_IT, DEFUSE, BODY_CHECK, COMMIT, IGNITE, ACTION, WRAP, ATTRIBUTE, SUMMARY }

private val nameItTags = listOf("tired", "bored", "scared it'll go badly", "don't see the point", "body doesn't want to", "other")
private val attributionOptions = listOf(
    "I just started",
    "the small step made it doable",
    "the timer made it feel finite",
    "I reminded myself why",
    "I let myself stop if I needed to",
    "honestly, no idea — but I did it",
)
private val timerChoicesSeconds = listOf(60, 120, 300)

// ---- Guided session (module round 1, "fuwari") --------------------------------------------------
private val guidedDurationPresetMinutes = listOf(5, 20, 45, 60, 90)
private const val GUIDED_DEFAULT_SECONDS = 20 * 60
private val guidedDefaultTypes = listOf("meditation", "dharma study")

private data class SessionData(
    val tag: String = "",
    val defuseSkipped: Boolean = false,
    val bodyCheckSkipped: Boolean = false,
    val microAction: String = "",
    val timerSeconds: Int = 120,
    val attribution: String = "",
)

private fun stepsUsedFor(session: SessionData): Int =
    2 + (if (!session.defuseSkipped) 1 else 0) + (if (!session.bodyCheckSkipped) 1 else 0)

private fun formatCountdown(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * The guided "I know I should do this and I don't want to" flow — project brief §6. No prototype
 * was available to build against this round (checked the repo and the fueru Design System project;
 * the user's call was to build from §6's own step table and mechanism notes directly) — see the
 * Phase 3 plan's "judgment calls" section for every interpretive choice made here, especially:
 * Landing is the entry CTA on PracticeDetailScreen, not its own screen inside this flow.
 *
 * No Room writes happen until Attribute completes and Summary is reached (judgment call #6) —
 * everything before that is transient [SessionData] state. [startAtIgnite] is the §6.3 fade-unlock
 * path: skips straight to Ignite using the practice's remembered micro-action/timer.
 */
@Composable
fun ResistanceFlowScreen(
    practiceId: Long,
    startAtIgnite: Boolean,
    onIgnited: (() -> Unit)? = null,
    onDone: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val context = LocalContext.current
    val database = application.database
    val scope = rememberCoroutineScope()

    var practice by remember(practiceId) { mutableStateOf<Practice?>(null) }
    LaunchedEffect(practiceId) { practice = database.practiceDao().getById(practiceId) }
    val current = practice ?: return

    var step by remember {
        mutableStateOf(if (startAtIgnite) FlowStep.IGNITE else FlowStep.NAME_IT)
    }
    var session by remember(practiceId) {
        mutableStateOf(
            SessionData(
                // For a guided-session practice, microAction/timerSeconds are semantically repurposed
                // as "session type" and "session duration" — same fields, same ResistanceFlowPrefs
                // persistence, same §6.3 fade-unlock interaction, different UI to set them (see
                // GuidedSessionCommitStep). No fallback micro-action text for guided practices — the
                // type picker starts blank until the user actually picks one.
                microAction = if (current.guidedSessionEnabled) {
                    ResistanceFlowPrefs.getMicroAction(context, practiceId, null)
                } else {
                    ResistanceFlowPrefs.getMicroAction(context, practiceId, current.microActionDefault)
                },
                timerSeconds = ResistanceFlowPrefs.getTimerSeconds(
                    context,
                    practiceId,
                    default = if (current.guidedSessionEnabled) GUIDED_DEFAULT_SECONDS else 120,
                ),
            ),
        )
    }
    fun finishAndLog() {
        // step only moves to SUMMARY once both writes actually complete — SummaryStep reads the
        // last 3 sessions (sparkline + §6.3 fade check) on mount, so if it mounted before this
        // session's own insert landed, it'd read stale data and the fade offer could misfire a
        // session late. scope.launch on its own doesn't guarantee that ordering; doing the step
        // transition inside the same coroutine, after both suspend calls, does.
        scope.launch {
            val today = LocalDate.now().toString()
            database.practiceLogEntryDao().upsert(PracticeLogEntry(practiceId = practiceId, date = today, status = "done"))
            database.resistanceSessionDao().insert(
                ResistanceSession(
                    practiceId = practiceId,
                    timestamp = System.currentTimeMillis(),
                    tag = session.tag,
                    stepsUsed = stepsUsedFor(session),
                    completed = true,
                    attribution = session.attribution,
                ),
            )
            // Guided-session practices (module round 1, "fuwari") get their own log row too — type +
            // real duration, the "extra fields" ResistanceSession deliberately doesn't carry since it
            // stays generic across every practice. microAction/timerSeconds are the repurposed fields
            // set by GuidedSessionCommitStep.
            if (current.guidedSessionEnabled && session.microAction.isNotBlank()) {
                database.guidedSessionDao().insert(
                    GuidedSession(
                        practiceId = practiceId,
                        timestamp = System.currentTimeMillis(),
                        sessionType = session.microAction,
                        durationMinutes = session.timerSeconds / 60,
                    ),
                )
            }
            step = FlowStep.SUMMARY
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(Spacing.space5)) {
        when (step) {
            FlowStep.NAME_IT -> NameItStep(
                selected = session.tag,
                onPick = { session = session.copy(tag = it) },
                onNext = { step = FlowStep.DEFUSE },
            )
            FlowStep.DEFUSE -> DefuseStep(
                tag = session.tag,
                onNext = { step = FlowStep.BODY_CHECK },
                onSkip = { session = session.copy(defuseSkipped = true); step = FlowStep.BODY_CHECK },
            )
            FlowStep.BODY_CHECK -> BodyCheckStep(
                onDone = { step = FlowStep.COMMIT },
                onSkip = { session = session.copy(bodyCheckSkipped = true); step = FlowStep.COMMIT },
            )
            FlowStep.COMMIT -> if (current.guidedSessionEnabled) {
                GuidedSessionCommitStep(
                    database = database,
                    practiceId = practiceId,
                    sessionType = session.microAction,
                    durationSeconds = session.timerSeconds,
                    onTypeChange = { session = session.copy(microAction = it) },
                    onDurationChange = { session = session.copy(timerSeconds = it) },
                    onNext = {
                        ResistanceFlowPrefs.save(context, practiceId, session.microAction, session.timerSeconds)
                        step = FlowStep.IGNITE
                    },
                )
            } else {
                CommitStep(
                    microAction = session.microAction,
                    timerSeconds = session.timerSeconds,
                    onMicroActionChange = { session = session.copy(microAction = it) },
                    onTimerChange = { session = session.copy(timerSeconds = it) },
                    onNext = {
                        ResistanceFlowPrefs.save(context, practiceId, session.microAction, session.timerSeconds)
                        step = FlowStep.IGNITE
                    },
                )
            }
            FlowStep.IGNITE -> IgniteStep(
                practiceName = current.name,
                onIgnite = {
                    // Fired once, the moment the hold completes — this is the §7.2 Stage 2 lock's
                    // dismissal signal (EscalationLockActivity), independent of whether this
                    // particular entry came from a lock screen at all.
                    onIgnited?.invoke()
                    step = FlowStep.ACTION
                },
            )
            FlowStep.ACTION -> ActionStep(
                microAction = session.microAction.ifBlank { current.name },
                durationSeconds = session.timerSeconds,
                onDone = { step = FlowStep.WRAP },
            )
            FlowStep.WRAP -> WrapStep(
                onKeepGoing = { step = FlowStep.ACTION },
                onEnough = { step = FlowStep.ATTRIBUTE },
            )
            FlowStep.ATTRIBUTE -> AttributeStep(
                selected = session.attribution,
                onPick = { session = session.copy(attribution = it) },
                onNext = { finishAndLog() },
            )
            FlowStep.SUMMARY -> SummaryStep(
                database = database,
                practice = current,
                stepsUsed = stepsUsedFor(session),
                onDone = onDone,
            )
        }
    }
}

// ---- Shared step chrome --------------------------------------------------------------------------

@Composable
private fun StepScaffold(
    title: String,
    onSkip: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = title, color = FueruColors.TextPrimary, style = FueruType.headline)
        content()
        if (onSkip != null) {
            Text(
                text = "skip this",
                color = FueruColors.TextMuted,
                style = FueruType.caption,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }
    }
}

// ---- Name It --------------------------------------------------------------------------------------

@Composable
private fun NameItStep(selected: String, onPick: (String) -> Unit, onNext: () -> Unit) {
    var otherText by remember { mutableStateOf("") }
    val isOther = selected.isNotBlank() && selected !in nameItTags.dropLast(1)

    StepScaffold(title = "I'm resisting this") {
        Text(text = "what's going on?", color = FueruColors.TextSecondary, style = FueruType.body)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            nameItTags.dropLast(1).forEach { tag ->
                FueruButton(
                    text = tag,
                    variant = if (selected == tag) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    onClick = { onPick(tag) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FueruButton(
                text = "other",
                variant = if (isOther) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                onClick = { onPick(otherText.ifBlank { "something else" }) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isOther) {
                FueruTextField(
                    value = otherText,
                    onValueChange = { otherText = it; onPick(it.ifBlank { "something else" }) },
                    placeholder = "say a bit more",
                )
            }
        }
        FueruButton(text = "Next", enabled = selected.isNotBlank(), onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

// ---- Defuse ---------------------------------------------------------------------------------------

@Composable
private fun DefuseStep(tag: String, onNext: () -> Unit, onSkip: () -> Unit) {
    StepScaffold(title = "naming it", onSkip = onSkip) {
        // Deliberately "I'm having the thought: '___'" rather than the brief's literal
        // "I'm having the thought that I'm ___" — that template reads fine for adjective-style
        // tags ("tired") but breaks grammatically for the brief's own clause-style tags ("don't
        // see the point," "body doesn't want to") and for arbitrary free-text "other" input. This
        // phrasing keeps the same ACT defusion structure (naming the thought as a thought, not a
        // fact) without depending on the tag's grammar.
        Text(
            text = "I'm having the thought: \"$tag.\"",
            color = FueruColors.TextPrimary,
            style = FueruType.displayMd,
        )
        Text(
            text = "That's a thought passing through — not an instruction.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruButton(text = "Next", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

// ---- Body Check -------------------------------------------------------------------------------------

private const val BODY_CHECK_SECONDS = 40

@Composable
private fun BodyCheckStep(onDone: () -> Unit, onSkip: () -> Unit) {
    var remaining by remember { mutableIntStateOf(BODY_CHECK_SECONDS) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        onDone()
    }
    StepScaffold(title = "it's a wave", onSkip = onSkip) {
        Text(
            text = "It crests, then passes. You don't have to act on it — just notice it for a moment.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space6), contentAlignment = Alignment.Center) {
            FueruBreathingAnimation()
        }
        Text(
            text = formatCountdown(remaining),
            color = FueruColors.Fire8,
            style = FueruType.statMd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---- Commit ---------------------------------------------------------------------------------------

@Composable
private fun CommitStep(
    microAction: String,
    timerSeconds: Int,
    onMicroActionChange: (String) -> Unit,
    onTimerChange: (Int) -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(title = "smallest version") {
        Text(
            text = "What's the tiniest version of this you could do right now?",
            color = FueruColors.TextSecondary,
            style = FueruType.body,
        )
        FueruTextField(value = microAction, onValueChange = onMicroActionChange, placeholder = "e.g. just sit down for it")
        Text(text = "for how long", color = FueruColors.TextSecondary, style = FueruType.caption)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            timerChoicesSeconds.forEach { seconds ->
                FueruButton(
                    text = if (seconds < 60) "${seconds}s" else "${seconds / 60}min",
                    variant = if (timerSeconds == seconds) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    onClick = { onTimerChange(seconds) },
                )
            }
        }
        FueruButton(text = "Next", enabled = microAction.isNotBlank(), onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

// ---- Guided session Commit (module round 1, "fuwari") ---------------------------------------------

/**
 * Replaces [CommitStep] for practices with `guidedSessionEnabled` — a real session type + duration
 * picker instead of the generic micro-action + short-timer UI. Mirrors [NameItStep]'s exact
 * "quick-picks + other reveals a text field" pattern for session type (source: real usage history via
 * [GuidedSessionDao], not a fixed enum — deliberately flexible, e.g. meditation one day, text study
 * another). Duration is five presets (5/20/45/60/90min) or a custom minute entry.
 */
@Composable
private fun GuidedSessionCommitStep(
    database: AppDatabase,
    practiceId: Long,
    sessionType: String,
    durationSeconds: Int,
    onTypeChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onNext: () -> Unit,
) {
    var recentTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(practiceId) { recentTypes = database.guidedSessionDao().getRecentTypesForPractice(practiceId) }
    val quickTypes = remember(recentTypes) { (recentTypes + guidedDefaultTypes).distinct() }

    var customTypeText by remember { mutableStateOf("") }
    val isOtherType = sessionType.isNotBlank() && sessionType !in quickTypes

    var showCustomDuration by remember { mutableStateOf((durationSeconds / 60) !in guidedDurationPresetMinutes) }
    var customDurationText by remember { mutableStateOf((durationSeconds / 60).toString()) }

    StepScaffold(title = "this session") {
        Text(text = "what kind of session", color = FueruColors.TextSecondary, style = FueruType.body)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            quickTypes.forEach { type ->
                FueruButton(
                    text = type,
                    variant = if (sessionType == type) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    onClick = { onTypeChange(type) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FueruButton(
                text = "other",
                variant = if (isOtherType) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                onClick = { onTypeChange(customTypeText.ifBlank { "a session" }) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isOtherType) {
                FueruTextField(
                    value = customTypeText,
                    onValueChange = { customTypeText = it; onTypeChange(it.ifBlank { "a session" }) },
                    placeholder = "say what kind",
                )
            }
        }
        Text(text = "for how long", color = FueruColors.TextSecondary, style = FueruType.caption)
        // Five presets don't fit one Row at this width without squeezing the last button unreadably
        // narrow (caught live on-device) — two rows instead, matching how every other multi-choice
        // row in this file tops out at 3 across.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                guidedDurationPresetMinutes.take(3).forEach { minutes ->
                    FueruButton(
                        text = "${minutes}min",
                        variant = if (!showCustomDuration && durationSeconds == minutes * 60) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                        onClick = { showCustomDuration = false; onDurationChange(minutes * 60) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                guidedDurationPresetMinutes.drop(3).forEach { minutes ->
                    FueruButton(
                        text = "${minutes}min",
                        variant = if (!showCustomDuration && durationSeconds == minutes * 60) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                        onClick = { showCustomDuration = false; onDurationChange(minutes * 60) },
                    )
                }
            }
        }
        FueruButton(
            text = "custom",
            variant = if (showCustomDuration) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
            onClick = { showCustomDuration = true },
        )
        if (showCustomDuration) {
            FueruTextField(
                value = customDurationText,
                onValueChange = { text ->
                    customDurationText = text
                    text.toIntOrNull()?.takeIf { it > 0 }?.let { onDurationChange(it * 60) }
                },
                placeholder = "minutes",
            )
        }
        FueruButton(text = "Next", enabled = sessionType.isNotBlank(), onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

// ---- Ignite ---------------------------------------------------------------------------------------

@Composable
private fun IgniteStep(practiceName: String, onIgnite: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = practiceName, color = FueruColors.TextMuted, style = FueruType.overline)
        Box(modifier = Modifier.padding(vertical = Spacing.space6)) {
            FueruIgniteHoldButton(onIgnite = onIgnite)
        }
        Text(text = "cross the line whenever you're ready", color = FueruColors.TextMuted, style = FueruType.caption)
    }
}

// ---- Action ---------------------------------------------------------------------------------------

@Composable
private fun ActionStep(microAction: String, durationSeconds: Int, onDone: () -> Unit) {
    // Owns its own countdown state, same self-contained "one LaunchedEffect(Unit) with an internal
    // while loop" pattern BodyCheckStep uses — this step gets a genuinely fresh composition every
    // time it's entered (from Ignite, or from Wrap's "keep going"), since it's fully unmounted
    // while WRAP/IGNITE are showing, so a plain remember(Unit) here is not stale across re-entries.
    var remaining by remember { mutableIntStateOf(durationSeconds) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        onDone()
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = microAction, color = FueruColors.TextPrimary, style = FueruType.title)
        Text(
            text = formatCountdown(remaining),
            color = FueruColors.Fire4,
            style = FueruType.statLg,
            modifier = Modifier.padding(vertical = Spacing.space5),
        )
        FueruButton(text = "I'm done", variant = FueruButtonVariant.Secondary, onClick = onDone)
    }
}

// ---- Wrap -----------------------------------------------------------------------------------------

@Composable
private fun WrapStep(onKeepGoing: () -> Unit, onEnough: () -> Unit) {
    StepScaffold(title = "time's up") {
        Text(text = "Keep going, or call it here — either one is a full success.", color = FueruColors.TextMuted, style = FueruType.body)
        FueruButton(text = "Keep going", variant = FueruButtonVariant.Secondary, onClick = onKeepGoing, modifier = Modifier.fillMaxWidth())
        FueruButton(text = "That's enough for now", onClick = onEnough, modifier = Modifier.fillMaxWidth())
    }
}

// ---- Attribute --------------------------------------------------------------------------------------

@Composable
private fun AttributeStep(selected: String, onPick: (String) -> Unit, onNext: () -> Unit) {
    StepScaffold(title = "what got you through") {
        Text(text = "Not us — you. What was it?", color = FueruColors.TextMuted, style = FueruType.body)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            attributionOptions.forEach { option ->
                FueruButton(
                    text = option,
                    variant = if (selected == option) FueruButtonVariant.Primary else FueruButtonVariant.Secondary,
                    onClick = { onPick(option) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        FueruButton(text = "Done", enabled = selected.isNotBlank(), onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

// ---- Summary --------------------------------------------------------------------------------------

@Composable
private fun SummaryStep(database: AppDatabase, practice: Practice, stepsUsed: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recentSteps by remember { mutableStateOf<List<Int>>(emptyList()) }
    var offerFade by remember { mutableStateOf(false) }
    var fadeAccepted by remember { mutableStateOf(false) }

    LaunchedEffect(practice.id) {
        val recent = database.resistanceSessionDao().getRecentForPractice(practice.id, 3)
        recentSteps = recent.map { it.stepsUsed }.reversed()
        offerFade = !practice.shortFlowEnabled && recent.size == 3 && recent.all { it.stepsUsed <= 3 }
    }

    StepScaffold(title = "that's it") {
        Text(text = "$stepsUsed steps this time.", color = FueruColors.TextMuted, style = FueruType.body)
        if (recentSteps.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.Bottom) {
                recentSteps.forEach { count ->
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = (count * 12).dp)
                            .background(FueruColors.Fire4),
                    )
                }
            }
        }
        if (offerFade && !fadeAccepted) {
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "You've needed 3 steps or fewer, three times running. That's you, not the app. " +
                            "You can jump straight to the ignition hold from now on if that's true.",
                        color = FueruColors.TextPrimary,
                        style = FueruType.body,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        FueruButton(
                            text = "Yes, skip ahead",
                            onClick = {
                                scope.launch { database.practiceDao().update(practice.copy(shortFlowEnabled = true)) }
                                fadeAccepted = true
                            },
                        )
                        FueruButton(text = "Not yet", variant = FueruButtonVariant.Ghost, onClick = { offerFade = false })
                    }
                }
            }
        }
        FueruButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
