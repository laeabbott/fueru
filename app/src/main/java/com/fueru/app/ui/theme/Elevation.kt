package com.fueru.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ambient dark elevation for a raised card — port of --shadow-card / --shadow-raised.
 * Use for "this is above the surface" lift.
 */
fun Modifier.fueruCardShadow(elevation: Dp = 12.dp, radius: Dp = Radius.lg): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(radius),
    ambientColor = Color.Black.copy(alpha = 0.4f),
    spotColor = Color.Black.copy(alpha = 0.4f),
)

/**
 * Colored glow — port of --glow-fire-sm / --glow-fire-lg. Reserve for the single "hot" element
 * per screen (a PR card, an active toggle, the fire meter); glow means "this is the exciting
 * thing here", never generic decoration.
 */
fun Modifier.fireGlow(
    color: Color = FueruColors.Fire4,
    elevation: Dp = 20.dp,
    radius: Dp = Radius.lg,
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(radius),
    ambientColor = color.copy(alpha = 0.45f),
    spotColor = color.copy(alpha = 0.45f),
)
