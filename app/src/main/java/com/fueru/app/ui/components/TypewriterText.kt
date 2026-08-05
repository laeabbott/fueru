package com.fueru.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

private const val TYPEWRITER_MS_PER_CHAR = 35L

/** Reveals [text] one character at a time for a little life on load, instead of appearing all at once. Restarts whenever [text] itself changes. */
@Composable
fun FueruTypewriterText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    var visibleChars by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        visibleChars = 0
        for (i in 1..text.length) {
            visibleChars = i
            delay(TYPEWRITER_MS_PER_CHAR)
        }
    }
    Text(text = text.take(visibleChars), color = color, style = style, modifier = modifier)
}
