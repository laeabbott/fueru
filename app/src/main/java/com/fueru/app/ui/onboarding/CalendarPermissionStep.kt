package com.fueru.app.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fueru.app.R
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun CalendarPermissionStep(state: OnboardingState, onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state.calendarPermissionRequested = true
        onNext()
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = FueruColors.Fire4,
            modifier = Modifier.size(40.dp),
        )
        Text(text = "want workout time suggestions?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "If you let fueru peek at your calendar, This Week can suggest open windows for your " +
                "workouts. Totally skippable — planning still works fine without it.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruButton(
            text = "Allow calendar access",
            onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) },
            modifier = Modifier.padding(top = Spacing.space2),
        )
        FueruButton(
            text = "Skip for now",
            onClick = {
                state.calendarPermissionRequested = true
                onNext()
            },
            variant = FueruButtonVariant.Ghost,
        )
    }
}
