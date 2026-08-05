package com.fueru.app.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruSwitch
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

private data class ModeOption(val key: String, val title: String, val detail: String)

private val modeOptions = listOf(
    ModeOption("macros", "Macro numbers", "Daily protein/fat/carb targets with a running total you log against."),
    ModeOption("mealBalance", "Meal balance", "Portion-based, no numbers at all — fist-size guides per meal."),
)

@Composable
fun FoodTrackingStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "want fueru to track fuel too?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "Fully optional, off by default, and switchable anytime in Settings. Both modes below are equally valid.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruCard(modifier = Modifier.fillMaxWidth()) {
            FueruSwitch(
                checked = state.foodTrackingEnabled,
                onCheckedChange = { checked ->
                    state.foodTrackingEnabled = checked
                    if (checked && state.foodTrackingMode == null) {
                        state.foodTrackingMode = "macros"
                    }
                    if (!checked) {
                        state.foodTrackingMode = null
                    }
                },
                label = "Track what I eat",
            )
        }
        if (state.foodTrackingEnabled) {
            modeOptions.forEach { option ->
                val selected = state.foodTrackingMode == option.key
                val interactionSource = remember { MutableInteractionSource() }
                FueruCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {
                            state.foodTrackingMode = option.key
                        },
                    glow = selected,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Text(
                            text = option.title,
                            color = if (selected) FueruColors.Fire4 else FueruColors.TextPrimary,
                            style = FueruType.title,
                        )
                        Text(text = option.detail, color = FueruColors.TextSecondary, style = FueruType.caption)
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
