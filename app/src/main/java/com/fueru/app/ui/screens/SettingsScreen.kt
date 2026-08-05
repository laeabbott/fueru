package com.fueru.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.fueru.app.FueruApplication
import com.fueru.app.data.IcsCalendarStore
import com.fueru.app.data.WeightUnit
import com.fueru.app.data.WeightUnitStore
import com.fueru.app.data.WorkoutSessionStore
import com.fueru.app.data.seed.ensureSeeded
import com.fueru.app.escalation.EscalationPermissions
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruSwitch
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Minimal settings screen — kg/lb, display name, food-tracking on/off, and a full data wipe.
 * Everything here was previously either onboarding-only (name, food tracking — both said
 * "switchable anytime in Settings" before this screen existed) or had nowhere to live at all
 * (unit preference, wipe).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onDataWiped: () -> Unit, onOpenCharities: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    var unit by remember { mutableStateOf(WeightUnitStore.get(application)) }
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "settings", color = FueruColors.TextPrimary, style = FueruType.headline)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "units", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    WeightUnit.entries.forEach { option ->
                        FueruButton(
                            text = option.label,
                            variant = if (unit == option) FueruButtonVariant.Secondary else FueruButtonVariant.Ghost,
                            onClick = {
                                unit = option
                                WeightUnitStore.save(application, option)
                            },
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "display name", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    FueruTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Display name",
                        placeholder = "your name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FueruButton(
                        text = "Save name",
                        enabled = displayName.isNotBlank() && displayName != profile.displayName,
                        onClick = {
                            scope.launch {
                                database.userProfileDao().update(profile.copy(displayName = displayName))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "food tracking", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                FueruSwitch(
                    checked = profile.foodTrackingEnabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            database.userProfileDao().update(
                                profile.copy(
                                    foodTrackingEnabled = checked,
                                    foodTrackingMode = if (checked) (profile.foodTrackingMode ?: "macros") else null,
                                ),
                            )
                        }
                    },
                    label = "Track what I eat",
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "escalation alerts", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "If a scheduled practice goes unmarked, fueru nudges you, then locks the " +
                            "screen until you actually start the resistance flow. Needs permission to " +
                            "schedule exact alarms.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    // Not re-checked on resume (e.g. after returning from the system settings screen) —
                    // reopen Settings to see this flip once granted. Flagged as a known gap, not fixed
                    // this pass.
                    var exactAlarmsGranted by remember { mutableStateOf(EscalationPermissions.canScheduleExactAlarms(application)) }
                    if (exactAlarmsGranted) {
                        Text(text = "exact alarms allowed", color = FueruColors.Fire4, style = FueruType.caption)
                    } else {
                        FueruButton(
                            text = "Allow exact alarms",
                            onClick = {
                                EscalationPermissions.requestExactAlarmPermission(application)
                                exactAlarmsGranted = EscalationPermissions.canScheduleExactAlarms(application)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "stakes", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "Charities used for Stage 3/4 consequences (§7.3) — a practice can be " +
                            "configured to pledge a donation if it goes unmarked long enough.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    FueruButton(
                        text = "Manage charities",
                        variant = FueruButtonVariant.Secondary,
                        onClick = onOpenCharities,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "danger zone", color = FueruColors.SignalDanger, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(text = "Wipe all data", color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                    Text(
                        text = "Deletes your profile, schedule, and logged workouts — everything. Can't be undone.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    FueruButton(
                        text = "Wipe all data",
                        variant = FueruButtonVariant.Danger,
                        onClick = { showWipeConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        FueruButton(text = "Back", onClick = onBack, variant = FueruButtonVariant.Ghost)
    }

    if (showWipeConfirm) {
        WipeConfirmDialog(
            onConfirm = {
                showWipeConfirm = false
                scope.launch {
                    withContext(Dispatchers.IO) { database.clearAllTables() }
                    ensureSeeded(application, database)
                    WeightUnitStore.clear(application)
                    IcsCalendarStore.clear(application)
                    WorkoutSessionStore.clear(application)
                    onDataWiped()
                }
            },
            onDismiss = { showWipeConfirm = false },
        )
    }
}

@Composable
private fun WipeConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "wipe everything?", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(
                    text = "This deletes your profile, schedule, and workout history, and can't be undone. " +
                        "You'll go through onboarding again.",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
                FueruButton(text = "Wipe all data", variant = FueruButtonVariant.Danger, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
