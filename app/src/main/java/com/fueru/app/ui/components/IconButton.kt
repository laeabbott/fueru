package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.Radius

enum class FueruIconButtonSize(val box: Dp, val icon: Dp) {
    Sm(32.dp, 16.dp),
    Md(44.dp, 20.dp),
    Lg(56.dp, 26.dp),
}

/** Direct port of components/core/IconButton.jsx. */
@Composable
fun FueruIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: FueruIconButtonSize = FueruIconButtonSize.Md,
    active: Boolean = false,
) {
    val shape = RoundedCornerShape(Radius.md)
    val background = if (active) FueruColors.Fire4.copy(alpha = 0.12f) else FueruColors.SurfaceCard
    val borderColor = if (active) FueruColors.Fire4 else FueruColors.BorderSubtle
    val tint = if (active) FueruColors.Fire4 else FueruColors.TextSecondary

    Box(
        modifier = modifier
            .size(size.box)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size.icon),
        )
    }
}
