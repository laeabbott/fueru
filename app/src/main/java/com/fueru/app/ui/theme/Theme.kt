package com.fueru.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * fueru is dark-mode only by design (see design system readme: "never pure black or white").
 * There is deliberately no light ColorScheme to switch to.
 */
private val FueruColorScheme = darkColorScheme(
    primary = FueruColors.Fire4,
    onPrimary = FueruColors.TextOnFire,
    secondary = FueruColors.Fire4,
    onSecondary = FueruColors.TextOnFire,
    background = FueruColors.SurfaceApp,
    onBackground = FueruColors.TextPrimary,
    surface = FueruColors.SurfaceCard,
    onSurface = FueruColors.TextPrimary,
    surfaceVariant = FueruColors.SurfaceRaised,
    onSurfaceVariant = FueruColors.TextSecondary,
    error = FueruColors.SignalDanger,
    onError = FueruColors.Ink50,
    outline = FueruColors.BorderSubtle,
    outlineVariant = FueruColors.BorderStrong,
)

@Composable
fun FueruTheme(content: @Composable () -> Unit) {
    // isSystemInDarkTheme() is intentionally not consulted — fueru has no light theme to fall back to.
    val colorScheme = FueruColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FueruMaterialTypography,
        shapes = FueruShapes,
        content = content,
    )
}
