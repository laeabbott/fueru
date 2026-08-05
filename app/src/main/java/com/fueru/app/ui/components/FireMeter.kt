package com.fueru.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.fireGlow

private val stageNames = listOf("ember", "spark", "kindling", "blaze", "inferno", "supernova", "white-hot", "plasma")

/**
 * Direct port of components/data/FireMeter.jsx — the app's core progression visual metaphor.
 * [stage] is 1-8 (see [stageNames]); [progress] is 0-100 progress within the current stage.
 */
@Composable
fun FireMeter(
    stage: Int,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedStage = stage.coerceIn(1, 8)
    val clampedProgress = progress.coerceIn(0f, 100f)
    val targetFraction = ((clampedStage - 1) / 7f) + (clampedProgress / 100f) * (1f / 7f)
    val fraction by animateFloatAsState(targetValue = targetFraction, animationSpec = tween(400), label = "fireMeterFraction")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stageNames[clampedStage - 1].uppercase(),
                color = FueruColors.Fire4,
                style = FueruType.overline,
            )
            Text(
                text = "stage $clampedStage/8",
                color = FueruColors.TextMuted,
                style = FueruType.caption,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(Radius.full))
                .background(FueruColors.Ink800),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .align(Alignment.CenterStart)
                    .fireGlow(elevation = 6.dp, radius = Radius.full)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(FueruGradients.fireFull),
            )
        }
    }
}
