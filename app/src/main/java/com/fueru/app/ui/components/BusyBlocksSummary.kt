package com.fueru.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fueru.app.data.BusyBlock
import com.fueru.app.data.DateUtils
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

/**
 * Extracted from ThisWeekScreen.kt's original private copy — second real usage (the scheduling &
 * escalation alignment pass wires it into Practice's Edit Schedule dialog too), matching this
 * project's established extract-on-second-use convention (NutritionRow, MacroSummaryRow,
 * TypewriterText). Silent when empty — whether that's a genuinely free day or READ_CALENDAR isn't
 * granted, there's nothing useful to say.
 */
@Composable
fun FueruBusyBlocksSummary(blocks: List<BusyBlock>) {
    if (blocks.isEmpty()) return
    Column(modifier = Modifier.padding(bottom = Spacing.space3)) {
        Text(text = "busy that day", color = FueruColors.TextMuted, style = FueruType.overline)
        blocks.take(5).forEach { block ->
            Text(
                text = "${DateUtils.formatTime(block.startMillis)}–${DateUtils.formatTime(block.endMillis)} ${block.title}",
                color = FueruColors.TextSecondary,
                style = FueruType.caption,
            )
        }
    }
}
