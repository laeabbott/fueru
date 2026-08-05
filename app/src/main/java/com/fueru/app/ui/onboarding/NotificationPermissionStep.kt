package com.fueru.app.ui.onboarding

import android.Manifest
import android.os.Build
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
fun NotificationPermissionStep(state: OnboardingState, onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state.notificationPermissionRequested = true
        onNext()
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = null,
            tint = FueruColors.Fire4,
            modifier = Modifier.size(40.dp),
        )
        Text(text = "okay if we nudge you sometimes?", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "A weekly planning nudge, and the occasional workout reminder. No guilt-trip pings if you " +
                "miss a day — that's not what these are for.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )
        FueruButton(
            text = "Allow notifications",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    state.notificationPermissionRequested = true
                    onNext()
                }
            },
            modifier = Modifier.padding(top = Spacing.space2),
        )
        FueruButton(
            text = "Skip for now",
            onClick = {
                state.notificationPermissionRequested = true
                onNext()
            },
            variant = FueruButtonVariant.Ghost,
        )
    }
}
