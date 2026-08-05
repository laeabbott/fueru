package com.fueru.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fueru.app.R
import com.fueru.app.ui.components.FueruIconButton
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.Spacing

/** Shared chrome for every onboarding step: progress dots + back button. Each step owns its own CTA(s). */
@Composable
fun OnboardingScaffold(
    stepIndex: Int,
    totalSteps: Int,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FueruColors.SurfaceApp)
            .padding(Spacing.space5),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FueruIconButton(
                    icon = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    onClick = onBack,
                )
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalSteps) { i ->
                    val active = i <= stepIndex
                    Spacer(
                        modifier = Modifier
                            .size(if (i == stepIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) FueruColors.Fire4 else FueruColors.Ink700),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.height(Spacing.space6))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}
