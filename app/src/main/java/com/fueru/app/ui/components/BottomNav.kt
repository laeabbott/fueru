package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors

data class FueruBottomNavItem(
    val label: String,
    val iconRegular: Int,
    val iconFill: Int,
    val route: String,
)

/** Direct port of components/navigation/BottomNav.jsx. */
@Composable
fun FueruBottomNav(
    items: List<FueruBottomNavItem>,
    activeRoute: String?,
    onSelect: (FueruBottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hairline = FueruColors.BorderSubtle
    Row(
        modifier = modifier
            .background(FueruColors.SurfaceCard)
            .drawBehind {
                drawLine(
                    color = hairline,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        items.forEach { item ->
            val isActive = item.route == activeRoute
            val tint = if (isActive) FueruColors.Fire4 else FueruColors.TextMuted
            val iconRes: Int = if (isActive) item.iconFill else item.iconRegular
            val icon: Painter = painterResource(iconRes)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(item) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Label kept as contentDescription only — icons alone, no text underneath.
                Icon(painter = icon, contentDescription = item.label, tint = tint)
            }
        }
    }
}
