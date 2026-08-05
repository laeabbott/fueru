package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fueru.app.data.formatWeightValue
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PICKER_ITEM_HEIGHT: Dp = 36.dp
private const val VISIBLE_ROWS = 3
private const val SIDE_PADDING_ROWS = VISIBLE_ROWS / 2

/**
 * Scroll-to-select wheel — deliberately built on plain `LazyColumn` + `LazyListState` rather than
 * Compose Foundation's snap-fling APIs (`rememberSnapFlingBehavior`/`SnapPosition`), since that
 * API's exact signature has shifted across Compose versions and there's no way to verify it
 * compiles without an Android SDK on this machine. Snapping is done manually instead: once the
 * user's drag/fling settles (`isScrollInProgress` goes false), it animates to whichever row is
 * closest to center and reports that value — using only long-stable `LazyListState` primitives.
 * Tapping a row jumps straight to it.
 */
@Composable
fun FueruScrollPicker(
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    recommendedIndex: Int? = null,
) {
    if (values.isEmpty()) return
    val clampedSelected = selectedIndex.coerceIn(0, values.lastIndex)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { PICKER_ITEM_HEIGHT.toPx() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = SIDE_PADDING_ROWS + clampedSelected)
    val scope = rememberCoroutineScope()

    fun listIndexFor(valueIndex: Int) = SIDE_PADDING_ROWS + valueIndex

    // External changes (e.g. a freshly-suggested value when a new exercise loads) move the wheel
    // too, but only when the user isn't actively dragging it.
    LaunchedEffect(clampedSelected) {
        if (!listState.isScrollInProgress) {
            listState.scrollToItem(listIndexFor(clampedSelected))
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstListIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val centeredListIndex = if (offset > itemHeightPx / 2) firstListIndex + 1 else firstListIndex
            val newValueIndex = (centeredListIndex - SIDE_PADDING_ROWS).coerceIn(0, values.lastIndex)
            listState.animateScrollToItem(listIndexFor(newValueIndex))
            if (newValueIndex != clampedSelected) onSelectedIndexChange(newValueIndex)
        }
    }

    Box(
        modifier = modifier.height(PICKER_ITEM_HEIGHT * VISIBLE_ROWS),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PICKER_ITEM_HEIGHT)
                .background(FueruColors.SurfaceRaised, RoundedCornerShape(Radius.sm)),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(SIDE_PADDING_ROWS) { Box(Modifier.height(PICKER_ITEM_HEIGHT)) }
            itemsIndexed(values) { index, label ->
                val isSelected = index == clampedSelected
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PICKER_ITEM_HEIGHT)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            scope.launch { listState.animateScrollToItem(listIndexFor(index)) }
                            onSelectedIndexChange(index)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = when {
                            isSelected -> FueruColors.TextPrimary
                            index == recommendedIndex -> FueruColors.Fire4
                            else -> FueruColors.TextMuted
                        },
                        style = if (isSelected) FueruType.bodyLg else FueruType.body,
                    )
                }
            }
            items(SIDE_PADDING_ROWS) { Box(Modifier.height(PICKER_ITEM_HEIGHT)) }
        }
    }
}

/**
 * Whole-number scroll picker for reps. [recommendedReps], when given, marks the algorithm's
 * original suggestion in orange so it stays visible even after the user scrolls to a different
 * value — distinct from the highlighted (bright) currently-selected row.
 */
@Composable
fun FueruRepsScrollPicker(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
    recommendedReps: Int? = null,
) {
    val values = remember(range) { range.map { it.toString() } }
    val selectedIndex = (reps - range.first).coerceIn(0, values.lastIndex)
    val recommendedIndex = recommendedReps?.let { (it - range.first).coerceIn(0, values.lastIndex) }
    FueruScrollPicker(
        values = values,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { onRepsChange(range.first + it) },
        modifier = modifier,
        recommendedIndex = recommendedIndex,
    )
}

/**
 * Scroll picker for a weight value already expressed **in the display unit** (kg or lb — the
 * caller converts to/from kg for storage, same as the old text field did). Steps in halves of
 * that unit regardless of which one is active — fine-grained enough for either, and simpler than
 * picking a different step per unit.
 */
@Composable
fun FueruWeightScrollPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxValue: Float = 500f,
    step: Float = 0.5f,
    recommendedValue: Float? = null,
) {
    val steps = remember(maxValue, step) { (maxValue / step).roundToInt() + 1 }
    val values = remember(steps, step) { (0 until steps).map { formatWeightValue(it * step) } }
    val selectedIndex = (value / step).roundToInt().coerceIn(0, values.lastIndex)
    val recommendedIndex = recommendedValue?.let { (it / step).roundToInt().coerceIn(0, values.lastIndex) }
    FueruScrollPicker(
        values = values,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { onValueChange(it * step) },
        modifier = modifier,
        recommendedIndex = recommendedIndex,
    )
}
