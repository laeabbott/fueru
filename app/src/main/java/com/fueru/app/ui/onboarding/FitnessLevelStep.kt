package com.fueru.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

private val tierLabels = listOf(
    "could be mistaken for a skeleton",
    "noodle arms, big heart",
    "average human, unracked potential",
    "clearly been to the gym once or twice",
    "swol like yo mama",
)

@Composable
fun FitnessLevelStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "how strong are we right now?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "No wrong answers — this just helps us guess a sensible starting weight so your first " +
                "session doesn't start with an ego check.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            tierLabels.forEachIndexed { index, label ->
                val tier = index + 1
                val selected = state.strengthLevel == tier
                val interactionSource = remember { MutableInteractionSource() }
                FueruCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {
                            state.strengthLevel = tier
                        },
                    glow = selected,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                        FitnessFigureIcon(tier = tier, modifier = Modifier.size(48.dp))
                        Text(
                            text = label,
                            color = if (selected) FueruColors.Fire4 else FueruColors.TextPrimary,
                            style = FueruType.body,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        FueruButton(
            text = "Next",
            onClick = onNext,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}

/** A simple bulk-scaling stick figure, tinted along the fire scale — cooler/thinner to hotter/bulkier. */
@Composable
private fun FitnessFigureIcon(tier: Int, modifier: Modifier = Modifier) {
    val color = fireColorForTier(tier)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bulk = tier / 5f

        val headRadius = h * 0.13f
        drawCircle(color = color, radius = headRadius, center = Offset(w / 2f, h * 0.18f))

        val bodyWidth = w * (0.28f + bulk * 0.34f)
        val bodyHeight = h * 0.5f
        val bodyTop = h * 0.32f
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2f - bodyWidth / 2f, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyWidth * 0.3f, bodyWidth * 0.3f),
        )

        val armWidth = w * (0.08f + bulk * 0.10f)
        val armHeight = bodyHeight * 0.75f
        val armGap = bodyWidth / 2f + armWidth * 0.15f
        val armTop = bodyTop + bodyHeight * 0.05f
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2f - armGap - armWidth, armTop),
            size = Size(armWidth, armHeight),
            cornerRadius = CornerRadius(armWidth * 0.4f, armWidth * 0.4f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2f + armGap, armTop),
            size = Size(armWidth, armHeight),
            cornerRadius = CornerRadius(armWidth * 0.4f, armWidth * 0.4f),
        )

        val legWidth = w * (0.10f + bulk * 0.06f)
        val legHeight = h * 0.16f
        val legTop = bodyTop + bodyHeight - h * 0.02f
        val legGap = bodyWidth * 0.12f
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2f - legGap - legWidth, legTop),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(legWidth * 0.3f, legWidth * 0.3f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2f + legGap, legTop),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(legWidth * 0.3f, legWidth * 0.3f),
        )
    }
}

private fun fireColorForTier(tier: Int): Color {
    val index = when (tier.coerceIn(1, 5)) {
        1 -> 0
        2 -> 2
        3 -> 4
        4 -> 6
        else -> 7
    }
    return FueruColors.FireStops[index]
}
