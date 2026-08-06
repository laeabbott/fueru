package com.fueru.app.ui.screens

import androidx.compose.foundation.clickable
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
import com.fueru.app.FueruApplication
import com.fueru.app.data.PracticeScoring
import com.fueru.app.data.TodayPracticeSlot
import com.fueru.app.data.computeTodaysPracticePlan
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruPracticeHeatmap
import com.fueru.app.ui.components.FueruTag
import com.fueru.app.ui.components.FueruTagVariant
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Core Engine build-order step 2 (project brief §11): a bare-bones list + add flow so the Practice/
 * PracticeLogEntry/PracticeScoring machinery has something to actually exercise on-device.
 * Deliberately not a bottom-nav tab yet — reached via a small link from Home, same pattern
 * Settings already uses. Resistance Flow and Escalation are later phases; today's/manual logging
 * happens directly from the detail screen for now. Scheduling (Phase 2) is built — each card shows
 * a due-today/overdue tag via the same computeTodaysPracticePlan Home's card uses.
 */
@Composable
fun PracticesScreen(onOpenPractice: (Long) -> Unit, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val practices by database.practiceDao().observeAll().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    var todaysPlan by remember { mutableStateOf<List<TodayPracticeSlot>>(emptyList()) }
    LaunchedEffect(Unit) { todaysPlan = computeTodaysPracticePlan(database) }
    val todayByPracticeId = remember(todaysPlan) { todaysPlan.associateBy { it.practice.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "practices", style = FueruType.wordmarkMd.copy(brush = FueruGradients.fireLogo))

        if (practices.isEmpty()) {
            FueruCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "nothing tracked yet — add a practice to start seeing its rhythm.",
                    color = FueruColors.TextMuted,
                    style = FueruType.body,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                practices.forEach { practice ->
                    PracticeListCard(
                        practice = practice,
                        today = todayByPracticeId[practice.id],
                        onClick = { onOpenPractice(practice.id) },
                    )
                }
            }
        }

        FueruButton(text = "+ Add practice", onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth())
        FueruButton(text = "Back", variant = FueruButtonVariant.Ghost, onClick = onBack)
    }

    if (showAddDialog) {
        AddPracticeDialog(
            onAdd = { name, frequencyType, frequencyCount, actionVerb ->
                scope.launch {
                    database.practiceDao().insert(
                        Practice(
                            name = name,
                            targetFrequencyType = frequencyType,
                            targetFrequencyCount = frequencyCount,
                            createdDate = System.currentTimeMillis(),
                            actionVerb = actionVerb,
                        ),
                    )
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun PracticeListCard(practice: Practice, today: TodayPracticeSlot?, onClick: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val entries by application.database.practiceLogEntryDao()
        .observeForPractice(practice.id)
        .collectAsState(initial = emptyList<PracticeLogEntry>())
    val score = remember(entries, practice.halfLifeDays) { PracticeScoring.currentScore(entries, practice.halfLifeDays) }

    FueruCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(text = practice.name, color = FueruColors.TextPrimary, style = FueruType.bodyLg)
                    when {
                        today == null -> Unit
                        today.loggedStatus != null -> FueruTag(text = today.loggedStatus)
                        today.isOverdue -> FueruTag(text = "overdue", variant = FueruTagVariant.Danger)
                        else -> FueruTag(text = "due today", variant = FueruTagVariant.Fire)
                    }
                }
                Text(text = score.toInt().toString(), color = FueruColors.Fire4, style = FueruType.statSm)
            }
            FueruPracticeHeatmap(entries = entries, weeksShown = 6)
        }
    }
}

@Composable
private fun AddPracticeDialog(
    onAdd: (name: String, frequencyType: String, frequencyCount: Int, actionVerb: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var frequencyType by remember { mutableStateOf("per_week") }
    var frequencyCountText by remember { mutableStateOf("3") }
    var actionVerbText by remember { mutableStateOf("") }
    val frequencyCount = frequencyCountText.toIntOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "new practice", color = FueruColors.TextPrimary, style = FueruType.title)
                FueruTextField(value = name, onValueChange = { name = it }, label = "Name", placeholder = "e.g. meditation")

                Text(text = "how often", color = FueruColors.TextSecondary, style = FueruType.caption)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    FueruButton(
                        text = "per week",
                        variant = if (frequencyType == "per_week") FueruButtonVariant.Secondary else FueruButtonVariant.Ghost,
                        onClick = { frequencyType = "per_week" },
                    )
                    FueruButton(
                        text = "per month",
                        variant = if (frequencyType == "per_month") FueruButtonVariant.Secondary else FueruButtonVariant.Ghost,
                        onClick = { frequencyType = "per_month" },
                    )
                }
                FueruTextField(
                    value = frequencyCountText,
                    onValueChange = { frequencyCountText = it },
                    label = "Target count",
                    placeholder = "3",
                )
                FueruTextField(
                    value = actionVerbText,
                    onValueChange = { actionVerbText = it },
                    label = "Quick-start verb (optional)",
                    placeholder = "e.g. fueruing",
                )

                FueruButton(
                    text = "Add practice",
                    enabled = name.isNotBlank() && frequencyCount != null && frequencyCount > 0,
                    onClick = { onAdd(name.trim(), frequencyType, frequencyCount!!, actionVerbText.trim().ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
