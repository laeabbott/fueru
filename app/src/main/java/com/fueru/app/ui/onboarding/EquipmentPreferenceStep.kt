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
import com.fueru.app.data.EquipmentPreference
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun EquipmentPreferenceStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "got a favorite kind of equipment?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "When we suggest a different exercise mid-workout, we'll lean toward this — not a hard rule, " +
                "just a nudge.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            EquipmentPreference.options.forEach { (key, label) ->
                val selected = state.equipmentPreference == key
                val interactionSource = remember { MutableInteractionSource() }
                FueruCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {
                            state.equipmentPreference = key
                        },
                    glow = selected,
                ) {
                    Text(
                        text = label,
                        color = if (selected) FueruColors.Fire4 else FueruColors.TextPrimary,
                        style = FueruType.body,
                    )
                }
            }
            val noPreferenceSelected = state.equipmentPreference == null
            val interactionSource = remember { MutableInteractionSource() }
            FueruCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null) {
                        state.equipmentPreference = null
                    },
                glow = noPreferenceSelected,
            ) {
                Text(
                    text = "No preference",
                    color = if (noPreferenceSelected) FueruColors.Fire4 else FueruColors.TextPrimary,
                    style = FueruType.body,
                )
            }
        }
        FueruButton(
            text = "Next",
            onClick = onNext,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
