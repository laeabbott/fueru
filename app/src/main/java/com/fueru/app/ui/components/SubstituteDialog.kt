package com.fueru.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.fueru.app.data.entity.Exercise
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing

/** Shared "swap for a different exercise" picker — used by both the live Workout session and the upcoming-workout preview/edit screen. */
@Composable
fun FueruSubstituteDialog(options: List<Exercise>, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(modifier = Modifier.padding(Spacing.space5)) {
                Text(text = "swap for a different exercise", color = FueruColors.TextPrimary, style = FueruType.title)
                if (options.isEmpty()) {
                    Text(
                        text = "No alternatives found for this muscle group.",
                        color = FueruColors.TextMuted,
                        style = FueruType.body,
                        modifier = Modifier.padding(top = Spacing.space3),
                    )
                } else {
                    Column(modifier = Modifier.padding(top = Spacing.space3)) {
                        options.forEach { exercise ->
                            Text(
                                text = exercise.name,
                                color = FueruColors.TextPrimary,
                                style = FueruType.body,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(exercise) }
                                    .padding(vertical = Spacing.space3),
                            )
                        }
                    }
                }
                FueruButton(
                    text = "Keep current",
                    variant = FueruButtonVariant.Ghost,
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Spacing.space2),
                )
            }
        }
    }
}
