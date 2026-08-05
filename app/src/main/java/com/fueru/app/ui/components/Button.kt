package com.fueru.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.fireGlow

enum class FueruButtonVariant { Primary, Secondary, Ghost, Danger }
enum class FueruButtonSize { Md, Lg }

/** Direct port of components/core/Button.jsx. */
@Composable
fun FueruButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FueruButtonVariant = FueruButtonVariant.Primary,
    size: FueruButtonSize = FueruButtonSize.Md,
    iconLeft: Painter? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val padding = when (size) {
        FueruButtonSize.Md -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        FueruButtonSize.Lg -> PaddingValues(horizontal = 26.dp, vertical = 16.dp)
    }

    val background: Brush = when (variant) {
        FueruButtonVariant.Primary -> FueruGradients.fireCta
        FueruButtonVariant.Secondary -> SolidColor(FueruColors.SurfaceRaised)
        FueruButtonVariant.Ghost -> SolidColor(Color.Transparent)
        FueruButtonVariant.Danger -> SolidColor(FueruColors.SignalDanger)
    }

    val contentColor by animateColorAsState(
        targetValue = when (variant) {
            FueruButtonVariant.Primary -> FueruColors.TextOnFire
            FueruButtonVariant.Secondary -> FueruColors.TextPrimary
            FueruButtonVariant.Ghost -> FueruColors.TextSecondary
            FueruButtonVariant.Danger -> Color.White
        },
        label = "buttonContentColor",
    )

    val rowModifier = modifier
        .clip(RoundedCornerShape(Radius.md))
        .then(if (variant == FueruButtonVariant.Primary) Modifier.fireGlow(elevation = 8.dp) else Modifier)
        .background(background)
        .then(
            if (variant == FueruButtonVariant.Secondary) {
                Modifier.border(1.dp, FueruColors.BorderSubtle, RoundedCornerShape(Radius.md))
            } else {
                Modifier
            },
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
        .padding(padding)
        .alpha(if (!enabled) 0.45f else if (pressed) 0.97f else 1f)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (iconLeft != null) {
                Icon(painter = iconLeft, contentDescription = null)
            }
            Text(text = text, style = FueruType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
}
