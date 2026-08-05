package com.fueru.app.ui.theme

import androidx.compose.ui.graphics.Brush

/** Direct port of the gradient tokens in tokens/colors.css. */
object FueruGradients {

    /** --gradient-fire-full — the full 8-stop fire scale, left to right. */
    val fireFull = Brush.horizontalGradient(FueruColors.FireStops)

    /** --gradient-fire-cta — used on primary buttons and the one "hot" glow element per screen. */
    val fireCta = Brush.horizontalGradient(listOf(FueruColors.Fire3, FueruColors.Fire4))

    /** --gradient-fire-logo — wordmark fill only (yellow -> orange -> red). */
    val fireLogo = Brush.horizontalGradient(
        listOf(
            androidx.compose.ui.graphics.Color(0xFFF2C94C),
            androidx.compose.ui.graphics.Color(0xFFF2791C),
            androidx.compose.ui.graphics.Color(0xFFD1481E),
        ),
    )
}
