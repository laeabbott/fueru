package com.fueru.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.fireGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TICK_MS = 16L

/**
 * Press-and-hold (~2.5s) ignition button — project brief §6.1's "deliberate embodied threshold."
 * Highest-risk new interaction code in this phase, same caliber as this project's hand-rolled
 * scroll-snap picker (ui/components/ScrollPicker.kt): no existing hold-gesture precedent here,
 * needs real finger input to confirm the feel, not just a compile check.
 *
 * Fires [onIgnite] the moment the hold completes — doesn't wait for finger-up (matches how a
 * physical "hold to confirm" control behaves) — via a coroutine launched from `onPress` that
 * animates progress 0f->1f over [holdDurationMs] and calls onIgnite() itself on completion. If the
 * finger lifts before that job finishes, the job is cancelled and progress resets to 0 — so a
 * partial hold is a full no-op, not a partial ignite.
 */
@Composable
fun FueruIgniteHoldButton(
    onIgnite: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Int = 2500,
    buttonSize: Dp = 180.dp,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var progress by remember { mutableFloatStateOf(0f) }
    var pressing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(buttonSize)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressing = true
                        val job = scope.launch {
                            val steps = (holdDurationMs / TICK_MS).toInt().coerceAtLeast(1)
                            for (i in 1..steps) {
                                delay(TICK_MS)
                                progress = (i.toFloat() / steps).coerceIn(0f, 1f)
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIgnite()
                        }
                        tryAwaitRelease()
                        pressing = false
                        if (progress < 1f) {
                            job.cancel()
                            progress = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val strokeWidth = 8.dp.toPx()
            val ringSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val ringTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = FueruColors.SurfaceRaised,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = ringSize,
                topLeft = ringTopLeft,
            )
            if (progress > 0f) {
                drawArc(
                    color = FueruColors.Fire4,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = ringSize,
                    topLeft = ringTopLeft,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(buttonSize - 40.dp)
                // radius = half the box size so the RoundedCornerShape shadow fireGlow builds
                // renders as a circle instead of a rounded square peeking out from behind the
                // CircleShape clip below — same fix as FueruBreathingAnimation, same bug caught
                // on the same on-device pass.
                .fireGlow(elevation = if (pressing) 28.dp else 16.dp, radius = (buttonSize - 40.dp) / 2)
                .clip(CircleShape)
                .background(FueruGradients.fireCta),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (pressing) "hold" else "press & hold",
                color = FueruColors.TextOnFire,
                style = FueruType.body,
            )
        }
    }
}
