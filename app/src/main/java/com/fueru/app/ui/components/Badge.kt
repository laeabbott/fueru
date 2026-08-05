package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fueru.app.R
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius

/** Direct port of components/core/Badge.jsx. Stage is clamped to the 8-stage fire scale. */
@Composable
fun FueruBadge(
    text: String,
    stage: Int,
    modifier: Modifier = Modifier,
) {
    val clamped = stage.coerceIn(1, 8)
    val color = FueruColors.FireStops[clamped - 1]
    val light = clamped >= 5
    val contentColor = if (light) FueruColors.TextOnFire else FueruColors.TextPrimary
    val shape = RoundedCornerShape(Radius.full)

    Row(
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fire_fill),
            contentDescription = null,
            tint = contentColor,
        )
        Text(text = text, color = contentColor, style = FueruType.caption, fontWeight = FontWeight.Bold)
    }
}
