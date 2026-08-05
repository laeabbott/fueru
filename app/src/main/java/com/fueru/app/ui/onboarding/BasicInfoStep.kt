package com.fueru.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun BasicInfoStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "the basics", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "Height and age feed your TDEE estimate later — purely functional, never judged.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruTextField(
            label = "Height (cm)",
            value = if (state.heightCm == 0f) "" else state.heightCm.toInt().toString(),
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }
                state.heightCm = digits.toFloatOrNull() ?: 0f
            },
            placeholder = "e.g. 170",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        FueruTextField(
            label = "Age",
            value = if (state.age == 0) "" else state.age.toString(),
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }
                state.age = digits.toIntOrNull() ?: 0
            },
            placeholder = "e.g. 28",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        FueruButton(
            text = "Next",
            onClick = onNext,
            enabled = state.heightCm > 0f && state.age > 0,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
