package com.fueru.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

/**
 * Shared shell for screens not yet built in this pass. Real screen UIs are a follow-up phase
 * (see Section 10 build order) — this only proves the nav graph, theme, and bottom nav wiring.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.space5),
        contentAlignment = if (content == null) Alignment.Center else Alignment.TopStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Text(text = title, color = FueruColors.TextPrimary, style = FueruType.headline)
            Text(text = subtitle, color = FueruColors.TextMuted, style = FueruType.body)
            content?.invoke()
        }
    }
}
