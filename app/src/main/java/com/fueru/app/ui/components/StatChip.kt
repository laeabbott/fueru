package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius

/** Direct port of components/data/StatChip.jsx. */
@Composable
fun FueruStatChip(
    icon: Painter,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.md)
    Row(
        modifier = modifier
            .clip(shape)
            .background(FueruColors.SurfaceCard)
            .border(1.dp, FueruColors.BorderSubtle, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painter = icon, contentDescription = null, tint = FueruColors.Fire4)
        Column {
            Text(text = value, color = FueruColors.TextPrimary, style = FueruType.statSm)
            Text(text = label, color = FueruColors.TextMuted, style = FueruType.caption.copy(fontSize = 10.sp))
        }
    }
}
