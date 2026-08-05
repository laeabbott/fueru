package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import com.fueru.app.ui.theme.fireGlow
import com.fueru.app.ui.theme.fueruCardShadow

/** Direct port of components/core/Card.jsx. */
@Composable
fun FueruCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    glow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.lg)
    Box(
        modifier = modifier
            .then(
                when {
                    glow -> Modifier.fireGlow(radius = Radius.lg)
                    elevated -> Modifier.fueruCardShadow(radius = Radius.lg)
                    else -> Modifier
                },
            )
            .clip(shape)
            .background(FueruColors.SurfaceCard)
            .border(1.dp, FueruColors.BorderSubtle, shape)
            .padding(Spacing.space5),
    ) {
        content()
    }
}
