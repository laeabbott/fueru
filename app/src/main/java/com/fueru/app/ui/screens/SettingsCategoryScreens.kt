package com.fueru.app.ui.screens

import android.content.Intent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fueru.app.BuildConfig
import com.fueru.app.FueruApplication
import com.fueru.app.data.AppLogger
import com.fueru.app.data.AppUpdateChecker
import com.fueru.app.data.AppUpdatePermissions
import com.fueru.app.data.GuidedSessionDefaultStore
import com.fueru.app.data.IcsCalendarStore
import com.fueru.app.data.IgnoredEventStore
import com.fueru.app.data.UpdateInfo
import com.fueru.app.data.WeightUnit
import com.fueru.app.data.WeightUnitStore
import com.fueru.app.data.WorkoutSessionStore
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.seed.ensureSeeded
import com.fueru.app.escalation.EscalationPermissions
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruDatePickerDialog
import com.fueru.app.ui.components.FueruSwitch
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateLabelFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
private fun SettingsSubScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = title, color = FueruColors.TextPrimary, style = FueruType.headline)
        content()
        FueruButton(text = "Back", onClick = onBack, variant = FueruButtonVariant.Ghost)
    }
}

// ---- Profile & Units --------------------------------------------------------------------------

@Composable
fun SettingsProfileScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()
    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    var unit by remember { mutableStateOf(WeightUnit.LB) }
    LaunchedEffect(Unit) { unit = WeightUnitStore.get(application) }
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }

    SettingsSubScaffold(title = "profile & units", onBack = onBack) {
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
                                scope.launch { WeightUnitStore.save(application, option) }
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
    }
}

// ---- Notifications & Alerts --------------------------------------------------------------------

@Composable
fun SettingsNotificationsScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    var exportMessage by remember { mutableStateOf<String?>(null) }

    SettingsSubScaffold(title = "notifications & diagnostics", onBack = onBack) {
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
                    // reopen this screen to see this flip once granted. Flagged as a known gap, not
                    // fixed this pass.
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
            Text(text = "logs", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "A running log of scheduling, notifications, and crashes — export it and send it over if something's gone wrong and you want it looked at.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    FueruButton(
                        text = "Export logs",
                        variant = FueruButtonVariant.Secondary,
                        onClick = {
                            val shared = AppLogger.shareLogFile(application)
                            exportMessage = if (shared) null else "Nothing logged yet."
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    exportMessage?.let {
                        Text(text = it, color = FueruColors.TextMuted, style = FueruType.caption)
                    }
                }
            }
        }
    }
}

// ---- Food Tracking ------------------------------------------------------------------------------

@Composable
fun SettingsFoodScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()
    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    SettingsSubScaffold(title = "food tracking", onBack = onBack) {
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
}

// ---- Calendar -----------------------------------------------------------------------------------

/** Settings-categorization round — new "purge calendar data" action, scoped to just calendar state (unlike Danger Zone's full wipe). */
@Composable
fun SettingsCalendarScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val scope = rememberCoroutineScope()
    var showPurgeConfirm by remember { mutableStateOf(false) }

    SettingsSubScaffold(title = "calendar", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "imported calendar data", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "Clears your imported .ics file and any calendar events you've dismissed on " +
                            "This Week's scheduling grid — they'll show up again next time you import.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    FueruButton(
                        text = "Purge calendar data",
                        variant = FueruButtonVariant.Danger,
                        onClick = { showPurgeConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showPurgeConfirm) {
        Dialog(onDismissRequest = { showPurgeConfirm = false }) {
            Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
                Column(
                    modifier = Modifier.padding(Spacing.space5),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space3),
                ) {
                    Text(text = "purge calendar data?", color = FueruColors.TextPrimary, style = FueruType.title)
                    Text(
                        text = "Removes your imported .ics file and forgets any events you've dismissed. Doesn't touch your scheduled workouts.",
                        color = FueruColors.TextMuted,
                        style = FueruType.body,
                    )
                    FueruButton(
                        text = "Purge calendar data",
                        variant = FueruButtonVariant.Danger,
                        onClick = {
                            scope.launch {
                                IcsCalendarStore.clear(application)
                                IgnoredEventStore.clear(application)
                            }
                            showPurgeConfirm = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = { showPurgeConfirm = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ---- Practices ------------------------------------------------------------------------------------

/** Settings-categorization round — bottom-nav practice tabs (existing) plus vacation and default meditation length (both new this round). */
@Composable
fun SettingsPracticesScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()
    val practices by database.practiceDao().observeAll().collectAsState(initial = emptyList())
    var vacationTarget by remember { mutableStateOf<Practice?>(null) }
    var defaultMinutes by remember { mutableStateOf(45) }
    LaunchedEffect(Unit) { defaultMinutes = GuidedSessionDefaultStore.getMinutes(application) }

    SettingsSubScaffold(title = "practices", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "default session length", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(
                        text = "How long fuwari's quick-start timer runs, and every guided session's pre-filled duration.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        FueruButton(
                            text = "−5",
                            variant = FueruButtonVariant.Secondary,
                            enabled = defaultMinutes > 5,
                            onClick = {
                                defaultMinutes = (defaultMinutes - 5).coerceAtLeast(5)
                                scope.launch { GuidedSessionDefaultStore.saveMinutes(application, defaultMinutes) }
                            },
                        )
                        Text(
                            text = "$defaultMinutes min",
                            color = FueruColors.TextPrimary,
                            style = FueruType.bodyLg,
                            modifier = Modifier.padding(horizontal = Spacing.space3),
                        )
                        FueruButton(
                            text = "+5",
                            variant = FueruButtonVariant.Secondary,
                            onClick = {
                                defaultMinutes += 5
                                scope.launch { GuidedSessionDefaultStore.saveMinutes(application, defaultMinutes) }
                            },
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "practice tabs", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "Choose which practices get their own tab at the bottom, instead of being reached through the Practices list.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    if (practices.isEmpty()) {
                        Text(text = "no practices yet", color = FueruColors.TextMuted, style = FueruType.caption)
                    } else {
                        practices.forEach { practice ->
                            FueruSwitch(
                                checked = practice.showAsTab,
                                onCheckedChange = { checked ->
                                    scope.launch { database.practiceDao().update(practice.copy(showAsTab = checked)) }
                                },
                                label = practice.name,
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(text = "vacation", color = FueruColors.TextSecondary, style = FueruType.title)
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Text(
                        text = "While vacationed, a practice isn't prompted on Home and no escalation alarms fire — you'll get a notification the day before it resumes.",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    if (practices.isEmpty()) {
                        Text(text = "no practices yet", color = FueruColors.TextMuted, style = FueruType.caption)
                    } else {
                        practices.forEach { practice ->
                            val vacationUntil = practice.vacationUntilDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space1)) {
                                Column {
                                    Text(text = practice.name, color = FueruColors.TextPrimary, style = FueruType.body)
                                    if (vacationUntil != null) {
                                        Text(
                                            text = "back on ${vacationUntil.plusDays(1).format(dateLabelFormatter)}",
                                            color = FueruColors.Fire4,
                                            style = FueruType.caption,
                                        )
                                    }
                                }
                                if (vacationUntil != null) {
                                    FueruButton(
                                        text = "End vacation now",
                                        variant = FueruButtonVariant.Ghost,
                                        onClick = {
                                            scope.launch { database.practiceDao().update(practice.copy(vacationUntilDate = null)) }
                                        },
                                    )
                                } else {
                                    FueruButton(
                                        text = "Vacation…",
                                        variant = FueruButtonVariant.Secondary,
                                        onClick = { vacationTarget = practice },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    vacationTarget?.let { practice ->
        FueruDatePickerDialog(
            title = "vacation until",
            initialDate = LocalDate.now().plusDays(1),
            onConfirm = { date ->
                scope.launch { database.practiceDao().update(practice.copy(vacationUntilDate = date.toString())) }
                vacationTarget = null
            },
            onDismiss = { vacationTarget = null },
        )
    }
}

// ---- Stakes & Charities -------------------------------------------------------------------------

@Composable
fun SettingsStakesScreen(onBack: () -> Unit, onOpenCharities: () -> Unit) {
    SettingsSubScaffold(title = "stakes & charities", onBack = onBack) {
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
}

// ---- Danger Zone ---------------------------------------------------------------------------------

@Composable
fun SettingsDangerScreen(onBack: () -> Unit, onDataWiped: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()
    var showWipeConfirm by remember { mutableStateOf(false) }

    SettingsSubScaffold(title = "danger zone", onBack = onBack) {
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

    if (showWipeConfirm) {
        Dialog(onDismissRequest = { showWipeConfirm = false }) {
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
                    FueruButton(
                        text = "Wipe all data",
                        variant = FueruButtonVariant.Danger,
                        onClick = {
                            showWipeConfirm = false
                            scope.launch {
                                withContext(Dispatchers.IO) { database.clearAllTables() }
                                ensureSeeded(application, database)
                                WeightUnitStore.clear(application)
                                IcsCalendarStore.clear(application)
                                IgnoredEventStore.clear(application)
                                WorkoutSessionStore.clear(application)
                                onDataWiped()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = { showWipeConfirm = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ---- About & Updates ------------------------------------------------------------------------

/**
 * In-app-update round — self-serve "check for updates" against the (now-public) repo's GitHub
 * Releases, replacing the old "I build it, send you a file, you install it" chat-transfer flow.
 * No Play Store, so at least one Android system "install this?" confirmation is unavoidable — this
 * gets it down to: Check for updates -> Download & Install -> (first time only) grant a permission
 * -> Android's own install prompt.
 */
private sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data object CheckFailed : UpdateUiState()
    data class Available(val info: UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateInfo, val progress: Float?) : UpdateUiState()
    data object DownloadFailed : UpdateUiState()
}

@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    // Unlike SettingsNotificationsScreen's exact-alarm permission (a known, accepted gap — see its
    // own comment), this one *is* re-checked on resume: coming straight back from the system "allow
    // installs" screen and still seeing "grant permission" would directly undercut the whole point
    // of this round (a smooth, no-friction self-update path).
    var hasInstallPermission by remember { mutableStateOf(AppUpdatePermissions.canRequestPackageInstalls(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasInstallPermission = AppUpdatePermissions.canRequestPackageInstalls(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun startDownload(info: UpdateInfo) {
        state = UpdateUiState.Downloading(info, null)
        scope.launch {
            val file = AppUpdateChecker.downloadApk(context, info.downloadUrl) { progress ->
                state = UpdateUiState.Downloading(info, progress)
            }
            if (file == null) {
                state = UpdateUiState.DownloadFailed
                return@launch
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
            // Back to Available (not a "done" state) -- Android's own install screen is what's in
            // front of the user now; if they back out of it without confirming, the button here is
            // still there to try again rather than looking stuck.
            state = UpdateUiState.Available(info)
        }
    }

    SettingsSubScaffold(title = "about & updates", onBack = onBack) {
        FueruCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = "fueru", color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                Text(text = "Version ${BuildConfig.VERSION_NAME}", color = FueruColors.TextMuted, style = FueruType.caption)

                when (val s = state) {
                    is UpdateUiState.Idle -> FueruButton(
                        text = "Check for updates",
                        onClick = {
                            state = UpdateUiState.Checking
                            scope.launch {
                                state = try {
                                    AppUpdateChecker.checkForUpdate()?.let { UpdateUiState.Available(it) } ?: UpdateUiState.UpToDate
                                } catch (e: Exception) {
                                    UpdateUiState.CheckFailed
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is UpdateUiState.Checking -> Text(text = "checking...", color = FueruColors.TextMuted, style = FueruType.caption)
                    is UpdateUiState.UpToDate -> Text(text = "you're up to date.", color = FueruColors.TextMuted, style = FueruType.caption)
                    is UpdateUiState.CheckFailed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Text(text = "couldn't check for updates — check your connection.", color = FueruColors.SignalDanger, style = FueruType.caption)
                        FueruButton(
                            text = "Try again",
                            variant = FueruButtonVariant.Secondary,
                            onClick = { state = UpdateUiState.Idle },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is UpdateUiState.Available -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Text(text = "${s.info.versionName} is available.", color = FueruColors.Fire4, style = FueruType.caption)
                        if (hasInstallPermission) {
                            FueruButton(text = "Download & Install", onClick = { startDownload(s.info) }, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(
                                text = "Needs one-time permission to install updates from outside the Play Store.",
                                color = FueruColors.TextMuted,
                                style = FueruType.caption,
                            )
                            FueruButton(
                                text = "Grant install permission",
                                variant = FueruButtonVariant.Secondary,
                                onClick = { AppUpdatePermissions.requestInstallPermission(context) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    is UpdateUiState.Downloading -> Text(
                        text = s.progress?.let { "downloading... ${(it * 100).toInt()}%" } ?: "downloading...",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                    )
                    is UpdateUiState.DownloadFailed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Text(text = "download failed — check your connection and try again.", color = FueruColors.SignalDanger, style = FueruType.caption)
                        FueruButton(
                            text = "Try again",
                            variant = FueruButtonVariant.Secondary,
                            onClick = { state = UpdateUiState.Idle },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
