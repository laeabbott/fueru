package com.fueru.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.fireGlow

/**
 * A slow scale in/out loop — Body Check's "it's a wave, it crests, then passes" (§6.1). Same
 * `rememberInfiniteTransition` primitive `WorkoutScreen.kt`'s exercise-image crossfade already
 * uses, just driving scale instead of alpha. Purely visual — the step's countdown text is a
 * separate concern, handled by the screen that hosts this.
 */
@Composable
fun FueruBreathingAnimation(modifier: Modifier = Modifier, size: Dp = 180.dp, cycleMs: Int = 8000) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingScale",
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            // fireGlow's shadow defaults to a RoundedCornerShape (Radius.lg) — fine for cards, but
            // here it sits behind a circular fill, and a rounded-*square* shadow visibly peeks out
            // from behind a circle. Passing radius = half the button size makes that same
            // RoundedCornerShape render as a circle instead (a corner radius >= half the smaller
            // dimension is indistinguishable from CircleShape) — confirmed on-device, not just in
            // theory, since this was visibly wrong on first run.
            .fireGlow(color = FueruColors.Fire8, elevation = 20.dp, radius = size / 2)
            .clip(CircleShape)
            .background(FueruColors.Fire8.copy(alpha = 0.25f)),
    )
}
