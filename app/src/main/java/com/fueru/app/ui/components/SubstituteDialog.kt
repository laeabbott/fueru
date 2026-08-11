package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import com.fueru.app.data.entity.Exercise
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing

/**
 * Shared "swap for a different exercise" picker — used by both the live Workout session and the
 * upcoming-workout preview/edit screen.
 *
 * The "no equipment right now" filter is a purely client-side re-filter of the already-fetched
 * [options] list — no new DB call. Real scenario this closes: your normal equipment is broken or
 * unavailable, and "Suggest a different exercise" (the one-tap auto-pick, unaffected by this
 * toggle) sorts by your saved equipment preference — meaning it'll happily hand you another piece
 * of equipment you may not have access to either. This toggle narrows the full list down to
 * `equipment == "body only"` options instead, so the fast path actually surfaces something usable
 * standing at a broken machine.
 */
@Composable
fun FueruSubstituteDialog(options: List<Exercise>, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var noEquipmentOnly by remember { mutableStateOf(false) }
    val bodyweightOptions = remember(options) { options.filter { it.equipment == "body only" } }
    val displayedOptions = if (noEquipmentOnly) bodyweightOptions else options

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(modifier = Modifier.padding(Spacing.space5)) {
                Text(text = "swap for a different exercise", color = FueruColors.TextPrimary, style = FueruType.title)
                if (options.isNotEmpty()) {
                    EquipmentFilterChip(
                        selected = noEquipmentOnly,
                        onClick = { noEquipmentOnly = !noEquipmentOnly },
                        modifier = Modifier.padding(top = Spacing.space3),
                    )
                }
                if (displayedOptions.isEmpty()) {
                    Text(
                        text = if (noEquipmentOnly) {
                            "No bodyweight alternatives for this muscle group — turn the filter off to see the rest."
                        } else {
                            "No alternatives found for this muscle group."
                        },
                        color = FueruColors.TextMuted,
                        style = FueruType.body,
                        modifier = Modifier.padding(top = Spacing.space3),
                    )
                } else {
                    Column(modifier = Modifier.padding(top = Spacing.space3)) {
                        displayedOptions.forEach { exercise ->
                            Text(
                                text = exercise.name,
                                color = FueruColors.TextPrimary,
                                style = FueruType.body,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(exercise) }
                                    .padding(vertical = Spacing.space3),
                            )
                        }
                    }
                }
                FueruButton(
                    text = "Keep current",
                    variant = FueruButtonVariant.Ghost,
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Spacing.space2),
                )
            }
        }
    }
}

/** Small selectable filter pill — same selected/unselected treatment as [FueruWeekdayChip] and [FueruTag]'s Fire variant, just clickable. */
@Composable
private fun EquipmentFilterChip(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.sm)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) FueruColors.Fire4.copy(alpha = 0.14f) else FueruColors.SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.space3, vertical = Spacing.space2),
    ) {
        Text(
            text = if (selected) "✓ no equipment right now" else "no equipment right now",
            color = if (selected) FueruColors.Fire4 else FueruColors.TextSecondary,
            style = FueruType.caption,
        )
    }
}
