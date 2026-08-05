package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius

enum class FueruTagVariant { Neutral, Fire, Danger }

/** Direct port of components/core/Tag.jsx. Danger added for the Scheduler's "overdue" tag — same dim, non-alarming SignalDanger treatment the practice heatmap already uses for "miss" cells, not the solid SignalDanger Button.jsx's Danger variant uses (that's reserved for destructive confirms). */
@Composable
fun FueruTag(
    text: String,
    modifier: Modifier = Modifier,
    variant: FueruTagVariant = FueruTagVariant.Neutral,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val (background, borderColor, contentColor) = when (variant) {
        FueruTagVariant.Neutral -> Triple(FueruColors.SurfaceRaised, FueruColors.BorderSubtle, FueruColors.TextSecondary)
        FueruTagVariant.Fire -> Triple(FueruColors.Fire4.copy(alpha = 0.14f), FueruColors.Fire4.copy(alpha = 0.3f), FueruColors.Fire4)
        FueruTagVariant.Danger -> Triple(FueruColors.SignalDanger.copy(alpha = 0.14f), FueruColors.SignalDanger.copy(alpha = 0.3f), FueruColors.SignalDanger)
    }
    Text(
        text = text,
        color = contentColor,
        style = FueruType.caption,
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
