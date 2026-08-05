package com.fueru.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun NameStep(state: OnboardingState, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "what should we call you?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "This is what shows up on Home instead of a generic \"hey there\" — make it yours.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruTextField(
            value = state.displayName,
            onValueChange = { state.displayName = it },
            placeholder = "e.g. Kenji",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        FueruButton(
            text = "Next",
            onClick = onNext,
            enabled = state.displayName.isNotBlank(),
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
