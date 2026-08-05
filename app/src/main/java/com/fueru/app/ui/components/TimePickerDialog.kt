package com.fueru.app.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing

/** A themed wrapper around Material3's TimePicker for scheduling a workout time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FueruTimePickerDialog(
    initialHour: Int = 9,
    initialMinute: Int = 0,
    extraContent: (@Composable () -> Unit)? = null,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Radius.lg),
            color = FueruColors.SurfaceCard,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "pick a time",
                    color = FueruColors.TextPrimary,
                    style = FueruType.title,
                    modifier = Modifier.padding(bottom = Spacing.space4),
                )
                extraContent?.invoke()
                TimePicker(state = state)
                Row(
                    modifier = Modifier.padding(top = Spacing.space5),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
                ) {
                    FueruButton(text = "Cancel", onClick = onDismiss, variant = FueruButtonVariant.Ghost)
                    FueruButton(text = "Set time", onClick = { onConfirm(state.hour, state.minute) })
                }
            }
        }
    }
}
