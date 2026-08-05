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
import com.fueru.app.data.entity.Charity
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Manage the Stage 3/4 charity list — project brief §7.3. Two sentiments, not one list: "glad" is
 * a charity the user would actually be glad to support (the sting is "this miss cost me something
 * real, even something good"), "resent" is the classic aversive pledge-device charity. At least one
 * charity has to exist before a practice can turn the charity stake on (see PracticeDetailScreen).
 */
@Composable
fun CharitiesScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database
    val scope = rememberCoroutineScope()

    val charities by database.charityDao().observeAll().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    val glad = charities.filter { it.sentiment == "glad" }
    val resent = charities.filter { it.sentiment == "resent" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "charities", color = FueruColors.TextPrimary, style = FueruType.headline)
        Text(
            text = "First miss in a week draws from \"glad to support\"; a repeat miss the same week draws from \"resent.\"",
            color = FueruColors.TextMuted,
            style = FueruType.caption,
        )

        CharityListSection(title = "glad to support", charities = glad, onDelete = { scope.launch { database.charityDao().delete(it) } })
        CharityListSection(title = "resent", charities = resent, onDelete = { scope.launch { database.charityDao().delete(it) } })

        FueruButton(text = "+ Add charity", onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth())
        FueruButton(text = "Back", variant = FueruButtonVariant.Ghost, onClick = onBack)
    }

    if (showAddDialog) {
        AddCharityDialog(
            onAdd = { name, url, sentiment ->
                scope.launch { database.charityDao().insert(Charity(name = name, url = url, sentiment = sentiment)) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun CharityListSection(title: String, charities: List<Charity>, onDelete: (Charity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(text = title, color = FueruColors.TextSecondary, style = FueruType.title)
        if (charities.isEmpty()) {
            Text(text = "none yet", color = FueruColors.TextMuted, style = FueruType.caption)
        } else {
            charities.forEach { charity ->
                FueruCard(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(text = charity.name, color = FueruColors.TextPrimary, style = FueruType.body)
                            Text(text = charity.url, color = FueruColors.TextMuted, style = FueruType.caption)
                        }
                        Text(
                            text = "remove",
                            color = FueruColors.SignalDanger,
                            style = FueruType.caption,
                            modifier = Modifier.clickable { onDelete(charity) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCharityDialog(onAdd: (name: String, url: String, sentiment: String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var sentiment by remember { mutableStateOf("glad") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(
                modifier = Modifier.padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3),
            ) {
                Text(text = "new charity", color = FueruColors.TextPrimary, style = FueruType.title)
                FueruTextField(value = name, onValueChange = { name = it }, label = "Name", placeholder = "e.g. a local food bank")
                FueruTextField(value = url, onValueChange = { url = it }, label = "Donation link", placeholder = "https://...")
                Text(text = "which list", color = FueruColors.TextSecondary, style = FueruType.caption)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    FueruButton(
                        text = "glad to support",
                        variant = if (sentiment == "glad") FueruButtonVariant.Secondary else FueruButtonVariant.Ghost,
                        onClick = { sentiment = "glad" },
                    )
                    FueruButton(
                        text = "resent",
                        variant = if (sentiment == "resent") FueruButtonVariant.Secondary else FueruButtonVariant.Ghost,
                        onClick = { sentiment = "resent" },
                    )
                }
                FueruButton(
                    text = "Add charity",
                    enabled = name.isNotBlank() && url.isNotBlank(),
                    onClick = { onAdd(name.trim(), url.trim(), sentiment) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
