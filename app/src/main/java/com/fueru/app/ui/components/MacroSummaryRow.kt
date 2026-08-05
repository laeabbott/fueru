package com.fueru.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import kotlin.math.roundToInt

/** Read-only "Ng / Ng" macro line — shared by Home's checklist card and the Fuel tab's macros mode. */
@Composable
fun FueruMacroSummaryRow(label: String, logged: Float, target: Float) {
    val met = target > 0f && logged >= target
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = FueruColors.TextPrimary, style = FueruType.body)
        Text(
            text = "${logged.roundToInt()} / ${target.roundToInt()}g",
            color = if (met) FueruColors.Fire4 else FueruColors.TextMuted,
            style = FueruType.caption,
        )
    }
}
