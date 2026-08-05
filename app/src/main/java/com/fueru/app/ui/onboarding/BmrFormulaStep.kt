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

private data class FormulaOption(val variant: String, val title: String, val detail: String)

private val formulaOptions = listOf(
    FormulaOption("A", "Calculation set A", "Uses the +5 constant in the standard formula."),
    FormulaOption("B", "Calculation set B", "Uses the -161 constant instead — worth picking this " +
        "set if you're on HRT and want your numbers calculated based on the sex you're transitioning to."),
)

@Composable
fun BmrFormulaStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "which set of formulas fits you best?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "These feed the standard Mifflin-St Jeor calorie math — pick whichever set matches your body right now.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        formulaOptions.forEach { option ->
            val selected = state.bmrFormulaVariant == option.variant
            val interactionSource = remember { MutableInteractionSource() }
            FueruCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null) {
                        state.bmrFormulaVariant = option.variant
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
