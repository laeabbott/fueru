package com.fueru.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.fueru.app.FueruApplication
import com.fueru.app.data.GuidedSessionDefaultStore
import com.fueru.app.data.entity.GuidedSession
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.data.seed.FUWARI_PRACTICE_NAME
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun formatCountdown(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * fuwari round — the Home page's one-tap "begin session" button, deliberately bypassing the
 * Resistance Flow's name-it/defuse/body-check/commit/ignite friction steps that every other
 * practice goes through (per direct user direction: this should feel as immediate as "start
 * workout"/"log food," not another guided flow to sit through first). Still writes exactly the
 * same completion data a full guided-session flow would (PracticeLogEntry + GuidedSession), just
 * skipped straight there — no ResistanceSession row, since that table's tag/attribution fields are
 * non-null and specific to the steps this path deliberately skips.
 *
 * Immersive (this route isn't in NavGraph's tabRoutes, so the bottom nav is already hidden) with
 * the same BackHandler + confirm-exit dialog pattern ActiveWorkoutSession/ActionStep use.
 */
@Composable
fun FuwariQuickStartScreen(onDone: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    var durationMinutes by remember { mutableIntStateOf(45) }
    var practiceId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        durationMinutes = GuidedSessionDefaultStore.getMinutes(application)
        practiceId = database.practiceDao().getByName(FUWARI_PRACTICE_NAME)?.id
    }

    var remaining by remember(durationMinutes) { mutableIntStateOf(durationMinutes * 60) }
    var finished by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    fun commitAndFinish() {
        val id = practiceId ?: return
        finished = true
        scope.launch {
            database.practiceLogEntryDao().upsert(
                PracticeLogEntry(practiceId = id, date = LocalDate.now().toString(), status = "done"),
            )
            database.guidedSessionDao().insert(
                GuidedSession(
                    practiceId = id,
                    timestamp = System.currentTimeMillis(),
                    sessionType = "meditation",
                    durationMinutes = durationMinutes,
                ),
            )
            onDone()
        }
    }

    LaunchedEffect(practiceId) {
        if (practiceId == null) return@LaunchedEffect
        while (remaining > 0 && !finished) {
            delay(1000)
            remaining -= 1
        }
        if (!finished && remaining <= 0) commitAndFinish()
    }

    BackHandler(enabled = !finished) { showExitConfirm = true }
    if (showExitConfirm) {
        FuwariExitConfirmDialog(
            onKeepGoing = { showExitConfirm = false },
            onEnd = { showExitConfirm = false; onDone() },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.space5),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "fuwari", style = FueruType.wordmarkMd.copy(brush = FueruGradients.fireLogo))
        Text(
            text = formatCountdown(remaining),
            color = FueruColors.Fire4,
            style = FueruType.statLg,
            modifier = Modifier.padding(vertical = Spacing.space5),
        )
        FueruButton(text = "I'm done", variant = FueruButtonVariant.Secondary, onClick = ::commitAndFinish)
    }
}

@Composable
private fun FuwariExitConfirmDialog(onKeepGoing: () -> Unit, onEnd: () -> Unit) {
    Dialog(onDismissRequest = onKeepGoing) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "end this practice?", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(
                    text = "Your progress up to this point won't be saved.",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
                FueruButton(text = "Keep going", onClick = onKeepGoing, modifier = Modifier.fillMaxWidth())
                FueruButton(text = "End practice", variant = FueruButtonVariant.Ghost, onClick = onEnd, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
