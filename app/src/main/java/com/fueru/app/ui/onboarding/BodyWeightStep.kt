package com.fueru.app.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fueru.app.data.BodyType
import com.fueru.app.data.WeightUnit
import com.fueru.app.data.convertToDisplay
import com.fueru.app.data.convertToKg
import com.fueru.app.data.formatWeightValue
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun BodyWeightStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "how much do you weigh?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "Just a starting estimate for your fuel targets — you can update it anytime in Settings.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            WeightUnit.entries.forEach { option ->
                FueruButton(
                    text = option.label,
                    variant = if (state.weightUnit == option) FueruButtonVariant.Secondary else FueruButtonVariant.Ghost,
                    onClick = { state.weightUnit = option },
                )
            }
        }

        if (state.selectedBodyType == null) {
            var weightText by remember(state.weightUnit) {
                mutableStateOf(state.bodyWeightKg?.let { formatWeightValue(convertToDisplay(it, state.weightUnit)) } ?: "")
            }
            FueruTextField(
                label = "Weight (${state.weightUnit.label})",
                value = weightText,
                onValueChange = { raw ->
                    weightText = raw
                    state.bodyWeightKg = raw.toFloatOrNull()?.let { convertToKg(it, state.weightUnit) }
                    state.weightIsEstimated = false
                },
                placeholder = if (state.weightUnit == WeightUnit.LB) "e.g. 150" else "e.g. 68",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Text(
                text = "Not sure? Pick the closest fit instead:",
                color = FueruColors.TextMuted,
                style = FueruType.caption,
                modifier = Modifier.padding(top = Spacing.space2),
            )
            BodyType.entries.forEach { type ->
                val interactionSource = remember { MutableInteractionSource() }
                FueruCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {
                            state.selectedBodyType = type
                            state.bodyWeightKg = type.estimateWeightKg(state.heightCm)
                            state.weightIsEstimated = true
                        },
                ) {
                    Text(text = type.label, color = FueruColors.TextPrimary, style = FueruType.body)
                }
            }
        } else {
            FueruCard(modifier = Modifier.fillMaxWidth(), glow = true) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(text = state.selectedBodyType!!.label, color = FueruColors.Fire4, style = FueruType.title)
                    Text(
                        text = "Estimated starting weight: " +
                            (state.bodyWeightKg?.let { formatWeightValue(convertToDisplay(it, state.weightUnit)) } ?: "0") +
                            " ${state.weightUnit.label}",
                        color = FueruColors.TextSecondary,
                        style = FueruType.body,
                    )
                }
            }
            FueruButton(
                text = "Enter my exact weight instead",
                onClick = {
                    state.selectedBodyType = null
                    state.bodyWeightKg = null
                    state.weightIsEstimated = false
                },
                variant = FueruButtonVariant.Ghost,
            )
        }

        FueruButton(
            text = "Next",
            onClick = onNext,
            enabled = state.bodyWeightKg != null,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
