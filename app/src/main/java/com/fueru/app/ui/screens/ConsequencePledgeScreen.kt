package com.fueru.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fueru.app.FueruApplication
import com.fueru.app.data.entity.ConsequenceEvent
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Stage 4's pledge screen — project brief §7.4 v1 "pledge mode": logs the pledge prominently and
 * surfaces a direct link to the charity's own donation page, one manual tap to complete. No
 * payment API, so "I did this" is a manual confirm, not a verified one — same scope the brief's
 * own phased-approach recommendation describes for v1.
 */
@Composable
fun ConsequencePledgeScreen(eventId: Long, onDone: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var event by remember(eventId) { mutableStateOf<ConsequenceEvent?>(null) }
    LaunchedEffect(eventId) { event = database.consequenceEventDao().getById(eventId) }
    val current = event ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "the pledge", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "The miss went unmarked long enough that this stake fired. It's real, same as the rest of this.",
            color = FueruColors.TextMuted,
            style = FueruType.body,
        )

        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = current.charityName, color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                Text(text = current.charityUrl, color = FueruColors.TextMuted, style = FueruType.caption)
                FueruButton(
                    text = "Donate now",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(current.charityUrl))) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (current.completed) {
            Text(text = "marked done", color = FueruColors.Fire4, style = FueruType.body)
        } else {
            FueruButton(
                text = "I did this",
                variant = FueruButtonVariant.Secondary,
                onClick = {
                    scope.launch { database.consequenceEventDao().update(current.copy(completed = true)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FueruButton(text = "Done", variant = FueruButtonVariant.Ghost, onClick = onDone)
    }
}
