package com.fueru.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "fueru", color = FueruColors.Fire4, style = FueruType.wordmarkLg)
        Text(
            text = "Gotta fuel to fueru.",
            color = FueruColors.TextSecondary,
            style = FueruType.bodyLg,
        )
        Text(
            text = "Real talk: we picked this name because \"fueru\" (増える, Japanese for \"to increase\") " +
                "sounds almost exactly like \"fuel\" — and the whole team is way too delighted by that " +
                "for it to be a coincidence. So: you fuel, you fueru. Everything here is about building " +
                "and increasing — no burning, no cutting, no undo.",
            color = FueruColors.TextPrimary,
            style = FueruType.body,
        )
        Text(
            text = "One honest note before we start: fueru walks you through a general beginner strength " +
                "program. If you're new to exercise, managing a health condition, or already working with " +
                "a trainer or doctor, it's worth checking this program fits your situation.",
            color = FueruColors.TextMuted,
            style = FueruType.caption,
        )
        FueruButton(
            text = "Let's go",
            onClick = onNext,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
