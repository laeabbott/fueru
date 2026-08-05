package com.fueru.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fueru.app.FueruApplication
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import kotlinx.coroutines.delay

private const val SPLASH_MIN_DURATION_MS = 1500L

/**
 * The real startDestination: decides Onboarding vs Home based on whether a UserProfile already
 * exists, so returning users skip straight past onboarding. A one-shot suspend read (not a Flow)
 * so there's no ambiguity between "still loading" and "loaded, no profile". Held on-screen for a
 * fixed minimum duration so it reads as a deliberate loading moment rather than a flash, even
 * though the profile lookup itself is normally much faster than that. Same two-stop gradient as
 * the primary-button fill (`fireCta`), not the full 8-stop fire scale — a whole-screen background
 * doesn't need every stop, and this keeps it consistent with the rest of the app's "hot" accents.
 */
@Composable
fun SplashScreen(onHasProfile: () -> Unit, onNoProfile: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    LaunchedEffect(Unit) {
        val profile = application.database.userProfileDao().get()
        delay(SPLASH_MIN_DURATION_MS)
        if (profile != null) onHasProfile() else onNoProfile()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FueruGradients.fireCta),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "fueru", style = FueruType.wordmarkLg, color = Color.Black)
    }
}
