package com.fueru.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A themed wrapper around Material3's DatePicker — vacation-practices round, same shape as
 * FueruTimePickerDialog's TimePicker wrapper. Picks a single calendar date (no time component);
 * callers convert to/from an ISO "yyyy-MM-dd" string themselves, matching how the rest of this
 * codebase stores dates (PracticeLogEntry.date, Practice.vacationUntilDate).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FueruDatePickerDialog(
    title: String = "pick a date",
    initialDate: LocalDate = LocalDate.now(),
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

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
                    text = title,
                    color = FueruColors.TextPrimary,
                    style = FueruType.title,
                    modifier = Modifier.padding(bottom = Spacing.space4),
                )
                DatePicker(state = state, showModeToggle = false)
                Row(
                    modifier = Modifier.padding(top = Spacing.space3),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
                ) {
                    FueruButton(text = "Cancel", onClick = onDismiss, variant = FueruButtonVariant.Ghost)
                    FueruButton(
                        text = "Set date",
                        onClick = {
                            val millis = state.selectedDateMillis ?: return@FueruButton
                            onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        },
                    )
                }
            }
        }
    }
}
