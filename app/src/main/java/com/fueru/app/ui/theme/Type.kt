package com.fueru.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fueru.app.R

/**
 * Body/UI + display family — Rethink Sans, upright weights 400-800.
 */
val RethinkSans = FontFamily(
    Font(R.font.rethink_sans_400, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.rethink_sans_500, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.rethink_sans_600, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.rethink_sans_700, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.rethink_sans_800, FontWeight.ExtraBold, FontStyle.Normal),
)

/**
 * Rethink Sans italic — used for the rare hero "display" moments ("arc 1: begins", "level up.").
 */
val RethinkSansItalic = FontFamily(
    Font(R.font.rethink_sans_italic_400, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.rethink_sans_italic_800, FontWeight.ExtraBold, FontStyle.Italic),
)

/**
 * Stat numbers only — streaks, PRs, macros.
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_400, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.jetbrains_mono_600, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.jetbrains_mono_700, FontWeight.Bold, FontStyle.Normal),
)

/**
 * Wordmark face — originally reserved for the literal word "fueru" only, since extended (per
 * explicit user direction) to a handful of other short, lowercase page-header labels ("fuel",
 * "progress") that want the same branded treatment. Still never capitalized, still never used for
 * body copy. Supplied under a personal-use-only license (see design system readme's Caveats) —
 * fine for this build, needs a commercial license or a shippable substitute before any production
 * release.
 */
val Valty = FontFamily(
    Font(R.font.valty_bold_italic, FontWeight.Normal, FontStyle.Normal),
)

private val overlineTracking = 0.08.em

/**
 * Extra type slots the default Material3 Typography has no room for (wordmark, stat numbers,
 * overline tracking) — direct port of tokens/typography.css. Use alongside MaterialTheme.typography.
 */
object FueruType {
    val displayXl = TextStyle(fontFamily = RethinkSansItalic, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 64.sp, lineHeight = 65.sp)
    val displayLg = TextStyle(fontFamily = RethinkSansItalic, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 44.sp, lineHeight = 46.sp)
    val displayMd = TextStyle(fontFamily = RethinkSansItalic, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 32.sp, lineHeight = 35.sp)

    val wordmarkLg = TextStyle(fontFamily = Valty, fontWeight = FontWeight.Normal, fontSize = 64.sp, lineHeight = 64.sp)
    val wordmarkMd = TextStyle(fontFamily = Valty, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 40.sp)
    val wordmarkSm = TextStyle(fontFamily = Valty, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 28.sp)

    val headline = TextStyle(fontFamily = RethinkSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 29.sp)
    val title = TextStyle(fontFamily = RethinkSans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp)
    val bodyLg = TextStyle(fontFamily = RethinkSans, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 25.sp)
    val body = TextStyle(fontFamily = RethinkSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp)
    val caption = TextStyle(fontFamily = RethinkSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
    val overline = TextStyle(fontFamily = RethinkSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = overlineTracking)

    val statLg = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 40.sp)
    val statMd = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 22.sp)
    val statSm = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 14.sp)
}

/** Standard Material3 Typography slots, mapped onto FueruType for components (Button, TextField, ...) that read MaterialTheme.typography directly. */
val FueruMaterialTypography = Typography(
    headlineMedium = FueruType.headline,
    titleMedium = FueruType.title,
    bodyLarge = FueruType.bodyLg,
    bodyMedium = FueruType.body,
    labelLarge = FueruType.body,
    labelMedium = FueruType.caption,
    labelSmall = FueruType.overline,
)
