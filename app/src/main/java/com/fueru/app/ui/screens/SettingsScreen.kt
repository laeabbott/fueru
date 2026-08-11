package com.fueru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.navigation.FueruRoutes
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Spacing

private data class SettingsCategory(val label: String, val subtitle: String, val route: String, val danger: Boolean = false)

private val categories = listOf(
    SettingsCategory("Profile & Units", "Display name, kg/lb", FueruRoutes.SETTINGS_PROFILE),
    SettingsCategory("Notifications & Diagnostics", "Exact-alarm permission, export logs", FueruRoutes.SETTINGS_NOTIFICATIONS),
    SettingsCategory("Food Tracking", "Turn tracking on or off", FueruRoutes.SETTINGS_FOOD),
    SettingsCategory("Calendar", "Imported calendar data", FueruRoutes.SETTINGS_CALENDAR),
    SettingsCategory("Practices", "Bottom-nav tabs, vacation, session length", FueruRoutes.SETTINGS_PRACTICES),
    SettingsCategory("Stakes & Charities", "Stage 3/4 pledge consequences", FueruRoutes.SETTINGS_STAKES),
    SettingsCategory("About & Updates", "Version, check for updates", FueruRoutes.SETTINGS_ABOUT),
    SettingsCategory("Danger Zone", "Wipe all data", FueruRoutes.SETTINGS_DANGER, danger = true),
)

/**
 * Settings-categorization round — this used to be one long scrolling page; now it's a menu, since
 * enough sections have accumulated across every follow-up round that a flat list stopped being
 * scannable. Each row below is its own small screen (SettingsCategoryScreens.kt), same content as
 * before just split up, plus what's new this round (purge calendar data, vacation, default
 * meditation length — all folded into their natural category rather than added as more rows here).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCategory: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "settings", color = FueruColors.TextPrimary, style = FueruType.headline)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            categories.forEach { category ->
                FueruCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategory(category.route) },
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = category.label,
                                color = if (category.danger) FueruColors.SignalDanger else FueruColors.TextPrimary,
                                style = FueruType.bodyLg,
                            )
                            Text(text = category.subtitle, color = FueruColors.TextMuted, style = FueruType.caption)
                        }
                    }
                }
            }
        }

        FueruButton(text = "Back", onClick = onBack, variant = FueruButtonVariant.Ghost)
    }
}
