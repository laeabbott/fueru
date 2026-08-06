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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fueru.app.R
import com.fueru.app.data.IcsCalendarStore
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

@Composable
fun CalendarPermissionStep(state: OnboardingState, onNext: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state.calendarPermissionRequested = true
        onNext()
    }
    // Onboarding .ics round — an alternative for anyone whose calendar isn't a synced Android
    // account at all (the user's own example: no Google account, so READ_CALENDAR would return
    // nothing useful) — same IcsCalendarStore.save() This Week's own "import a calendar" link
    // already uses, just reachable here too instead of only after onboarding.
    val icsPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            IcsCalendarStore.save(context, uri)
            state.calendarPermissionRequested = true
            onNext()
        }
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
            text = "Upload a calendar file (.ics) instead",
            variant = FueruButtonVariant.Secondary,
            onClick = { icsPickerLauncher.launch(arrayOf("text/calendar", "application/octet-stream", "*/*")) },
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
