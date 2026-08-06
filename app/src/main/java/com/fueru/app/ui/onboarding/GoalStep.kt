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
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

private data class GoalOption(val key: String, val title: String, val detail: String)

private val goalOptions = listOf(
    GoalOption("buildMuscle", "Build muscle", "A modest calorie surplus on top of your maintenance number."),
    GoalOption("maintain", "Maintain", "Eat at your maintenance number — no surplus, no deficit."),
)

/** Follow-up round — onboarding's macro math never asked this, it just always targeted maintenance. Feeds TdeeCalculator's surplus decision (spec Section 6.1's own macro split, extended). */
@Composable
fun GoalStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "what's the goal?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "This shapes your daily calorie target — you can always change your mind later.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        goalOptions.forEach { option ->
            val selected = state.goal == option.key
            val interactionSource = remember { MutableInteractionSource() }
            FueruCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null) {
                        state.goal = option.key
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
        FueruButton(
            text = "Next",
            onClick = onNext,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
