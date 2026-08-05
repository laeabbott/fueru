package com.fueru.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fueru.app.R
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing

/**
 * Lands the "let's get you fueru-ing" beat with a real Giphy reaction (spec 6.4) above the
 * checkmark, rather than the checkmark standing in alone. Falls back to just the checkmark —
 * silently, no error text — if the gif never loads (no key, offline, no match for the tag), same
 * "fails quiet" pattern as the exercise-catalog images and food search elsewhere in this project.
 * [gifUrl] is prefetched by OnboardingFlow as soon as onboarding starts, not fetched here — by the
 * time the user reaches this, the last step, the network round-trip is long since done.
 */
@Composable
fun CelebrationStep(displayName: String, gifUrl: String?, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (gifUrl != null) {
            AsyncImage(
                model = gifUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(Radius.lg)),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle_fill),
                contentDescription = null,
                tint = FueruColors.Fire4,
                modifier = Modifier.size(64.dp),
            )
        }
        Text(
            text = "let's get you fueru-ing",
            color = FueruColors.Fire4,
            style = FueruType.displayMd,
            modifier = Modifier.padding(top = Spacing.space5),
        )
        Text(
            text = "Day 1, $displayName. Nothing undone. Everything fueru.",
            color = FueruColors.TextSecondary,
            style = FueruType.bodyLg,
            modifier = Modifier.padding(top = Spacing.space3, bottom = Spacing.space7),
        )
        FueruButton(text = "Take me to Home", onClick = onDone)
    }
}
