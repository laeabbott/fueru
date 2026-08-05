package com.fueru.app.ui.onboarding

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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProgramStartStep(state: OnboardingState, onNext: () -> Unit) {
    val formatted = remember(state.programStartDate) {
        Instant.ofEpochMilli(state.programStartDate)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Text(text = "arc 1: begins", color = FueruColors.Fire4, style = FueruType.displayMd)
        Text(
            text = "Your program starts today, at the 0-6 month phase — the foundational stretch where " +
                "everything else builds from.",
            color = FueruColors.TextSecondary,
            style = FueruType.body,
        )
        FueruCard(modifier = Modifier.fillMaxWidth(), glow = true) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text(text = "Start date", color = FueruColors.TextMuted, style = FueruType.caption)
                Text(text = formatted, color = FueruColors.TextPrimary, style = FueruType.title)
            }
        }
        FueruButton(
            text = "Start my arc",
            onClick = onNext,
            modifier = Modifier.padding(top = Spacing.space4),
        )
    }
}
