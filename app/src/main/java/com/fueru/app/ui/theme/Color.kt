package com.fueru.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Direct port of tokens/colors.css from the fueru design system.
 * Dark-mode only — there is no light palette.
 */
object FueruColors {
    // neutrals — warm dark greys, never pure black/white
    val Ink950 = Color(0xFF111113)
    val Ink900 = Color(0xFF17171A)
    val Ink800 = Color(0xFF1E1E22)
    val Ink700 = Color(0xFF2A2A2F)
    val Ink600 = Color(0xFF3A3A41)
    val Ink500 = Color(0xFF54545D)
    val Ink400 = Color(0xFF78787F)
    val Ink300 = Color(0xFF9D9DA3)
    val Ink200 = Color(0xFFC4C4C8)
    val Ink100 = Color(0xFFE6E6E8)
    val Ink50 = Color(0xFFF7F7F8)

    // fire scale — 8 stages, ember (low heat) to plasma (electric blue, hottest)
    val Fire1 = Color(0xFF7A0C02) // ember
    val Fire2 = Color(0xFFF52500) // spark
    val Fire3 = Color(0xFFFF4D00) // kindling
    val Fire4 = Color(0xFFFF7F11) // blaze
    val Fire5 = Color(0xFFFFB100) // inferno
    val Fire6 = Color(0xFFFFE600) // supernova
    val Fire7 = Color(0xFFF5FAFF) // white-hot
    val Fire8 = Color(0xFF00E5FF) // plasma

    val FireStops = listOf(Fire1, Fire2, Fire3, Fire4, Fire5, Fire6, Fire7, Fire8)

    // rare accent for destructive confirms only — never used for "burning calories" framing
    val SignalDanger = Color(0xFFE0473B)
    val SignalInfo = Color(0xFF4C9DE0)

    // semantic surfaces
    val SurfaceApp = Ink950
    val SurfaceCard = Ink900
    val SurfaceRaised = Ink800
    val SurfaceSunken = Color(0xFF0B0B0D)
    val SurfaceOverlay = Color(0xE0111113) // rgba(17,17,19,.88)

    val BorderSubtle = Ink700
    val BorderStrong = Ink600

    val TextPrimary = Ink50
    val TextSecondary = Ink300
    val TextMuted = Ink500
    val TextOnFire = Color(0xFF1A0F06) // dark text on light fire-5/6 fills

    val AccentFireMid = Fire4
}
