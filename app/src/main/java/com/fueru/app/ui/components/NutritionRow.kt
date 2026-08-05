package com.fueru.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.fueru.app.R
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import androidx.compose.material3.Text

/** One "left to log" row with quick +/- controls — shared by Home's checklist card and the Fuel tab. */
@Composable
fun FueruNutritionRow(label: String, logged: Float, target: Float, step: Float, onDelta: (Float) -> Unit) {
    val remaining = (target - logged).coerceAtLeast(0f)
    val done = remaining <= 0f && target > 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = FueruColors.TextPrimary, style = FueruType.body)
            Text(
                text = if (done) "done for today" else "${remaining.toInt()} left of ${target.toInt()}",
                color = if (done) FueruColors.Fire4 else FueruColors.TextMuted,
                style = FueruType.caption,
            )
        }
        FueruIconButton(
            icon = painterResource(R.drawable.ic_minus_circle),
            contentDescription = "Log less $label",
            size = FueruIconButtonSize.Sm,
            onClick = { onDelta(-step) },
        )
        FueruIconButton(
            icon = painterResource(R.drawable.ic_plus_circle),
            contentDescription = "Log more $label",
            size = FueruIconButtonSize.Sm,
            onClick = { onDelta(step) },
        )
    }
}
