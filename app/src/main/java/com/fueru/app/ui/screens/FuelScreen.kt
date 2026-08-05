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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.fueru.app.BuildConfig
import com.fueru.app.FueruApplication
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.DateUtils
import com.fueru.app.data.NutritionSnapshot
import com.fueru.app.data.food.UsdaFoodApi
import com.fueru.app.data.loadNutritionSnapshot
import com.fueru.app.data.entity.CustomFood
import com.fueru.app.data.entity.CustomFoodIngredient
import com.fueru.app.data.entity.DailyNutritionLog
import com.fueru.app.data.entity.FoodLogEntry
import com.fueru.app.ui.components.FueruButton
import com.fueru.app.ui.components.FueruButtonVariant
import com.fueru.app.ui.components.FueruCard
import com.fueru.app.ui.components.FueruMacroSummaryRow
import com.fueru.app.ui.components.FueruNutritionRow
import com.fueru.app.ui.components.FueruTextField
import com.fueru.app.ui.components.FueruTypewriterText
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruGradients
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius
import com.fueru.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A different one of these every time someone lands on the Fuel tab — same nerdy/gamer-adjacent, slightly cringy voice as the Workout tab's rotating line. */
private val fuelGreetings = listOf(
    "mana regen requires actual food, unfortunately.",
    "feed the machine. the machine is you.",
    "protein: the real main character.",
    "snacks are just tactical resource management.",
    "no potions in real life. eat something.",
    "you can't respawn on an empty stomach.",
    "carbs are not the final boss.",
    "your body runs on fuel, not vibes.",
)

/**
 * A food that can be logged or used as a recipe ingredient, regardless of where it came from — a
 * USDA search result, a manually-entered custom food, or a saved "combine foods" recipe (which is
 * itself just a [CustomFood] row once built). Unifying these means the rest of the logging/recipe
 * UI never needs to branch on source.
 */
private data class FoodPick(
    val name: String,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val kcalPer100g: Float,
    val fdcId: Int?,
    val customFoodId: Long?,
)

/** Searches USDA and the user's own custom/recipe foods together — custom foods are what make a saved recipe ("chicken tacos") reusable without rebuilding it every time. */
private suspend fun searchAllFoods(database: AppDatabase, query: String): List<FoodPick> {
    val usdaResults = UsdaFoodApi.search(query).map {
        FoodPick(
            name = it.name,
            proteinPer100g = it.proteinPer100g,
            carbsPer100g = it.carbsPer100g,
            fatPer100g = it.fatPer100g,
            kcalPer100g = it.kcalPer100g,
            fdcId = it.fdcId,
            customFoodId = null,
        )
    }
    val customResults = database.customFoodDao().search(query).map {
        FoodPick(
            name = "${it.name} (yours)",
            proteinPer100g = it.proteinPer100g,
            carbsPer100g = it.carbsPer100g,
            fatPer100g = it.fatPer100g,
            kcalPer100g = it.kcalPer100g.toFloat(),
            fdcId = null,
            customFoodId = it.id,
        )
    }
    return customResults + usdaResults
}

/**
 * Food tracking (spec Section 7) — only reachable when the user opted into it during onboarding.
 * Macros mode logs real foods via USDA FoodData Central (search by name, pick a serving size, the
 * app works out protein/carbs/fat/kcal from USDA's per-100g data) instead of manually nudging a
 * percentage of target — replaced per the user's own "ideal" framing of this feature. Meal-balance
 * mode is untouched: it's deliberately numberless portion counting, unrelated to precise macros.
 */
@Composable
fun FuelScreen() {
    val application = LocalContext.current.applicationContext as FueruApplication
    val database = application.database

    val userProfile by database.userProfileDao().observe().collectAsState(initial = null)
    val profile = userProfile ?: return

    if (!profile.foodTrackingEnabled) {
        PlaceholderScreen(
            title = "fuel",
            subtitle = "Food tracking is off — turn it on in onboarding to use this tab.",
        )
        return
    }

    val scope = rememberCoroutineScope()
    val today = remember { DateUtils.todayEpochMillis() }
    var refresh by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<NutritionSnapshot?>(null) }
    LaunchedEffect(refresh) {
        snapshot = loadNutritionSnapshot(database, profile)
    }
    val loggedFoodsToday by database.foodLogEntryDao().observeForDate(today).collectAsState(initial = emptyList())
    var showAddFood by remember { mutableStateOf(false) }
    var showAddCustomFood by remember { mutableStateOf(false) }
    var showCombineFoods by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space5),
    ) {
        Text(text = "fuel", style = FueruType.wordmarkMd.copy(brush = FueruGradients.fireLogo))
        val greeting = remember { fuelGreetings.random() }
        FueruTypewriterText(text = greeting, color = FueruColors.TextSecondary, style = FueruType.headline)

        val current = snapshot
        if (current == null) {
            Text(
                text = "Add a body weight in onboarding to see your targets.",
                color = FueruColors.TextMuted,
                style = FueruType.body,
            )
            return@Column
        }

        fun update(mutate: (DailyNutritionLog) -> DailyNutritionLog) {
            scope.launch {
                database.dailyNutritionLogDao().upsert(mutate(current.log))
                refresh++
            }
        }

        if (current.mode == "mealBalance") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = "today", color = FueruColors.TextPrimary, style = FueruType.title)
                FueruCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        FueruNutritionRow("Protein portions", current.log.proteinPortions, 4f, step = 1f) { delta ->
                            update { it.copy(proteinPortions = (it.proteinPortions + delta).coerceAtLeast(0f)) }
                        }
                        FueruNutritionRow("Carb portions", current.log.carbPortions, 4f, step = 1f) { delta ->
                            update { it.copy(carbPortions = (it.carbPortions + delta).coerceAtLeast(0f)) }
                        }
                        FueruNutritionRow("Fat portions", current.log.fatPortions, 2f, step = 1f) { delta ->
                            update { it.copy(fatPortions = (it.fatPortions + delta).coerceAtLeast(0f)) }
                        }
                        FueruNutritionRow("Fruit/veg portions", current.log.fruitVegPortions, 4f, step = 1f) { delta ->
                            update { it.copy(fruitVegPortions = (it.fruitVegPortions + delta).coerceAtLeast(0f)) }
                        }
                        Text(
                            text = "target ${current.targets.tdeeKcal} kcal",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                }
            }
        } else {
            val loggedProtein = loggedFoodsToday.sumOf { it.proteinG.toDouble() }.toFloat()
            val loggedCarbs = loggedFoodsToday.sumOf { it.carbsG.toDouble() }.toFloat()
            val loggedFat = loggedFoodsToday.sumOf { it.fatG.toDouble() }.toFloat()
            val loggedKcal = loggedFoodsToday.sumOf { it.kcal }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(text = "today", color = FueruColors.TextPrimary, style = FueruType.title)
                FueruCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        FueruMacroSummaryRow("Protein", loggedProtein, current.targets.proteinG.toFloat())
                        FueruMacroSummaryRow("Carbs", loggedCarbs, current.targets.carbG.toFloat())
                        FueruMacroSummaryRow("Fat", loggedFat, current.targets.fatG.toFloat())
                        Text(
                            text = "$loggedKcal / ${current.targets.tdeeKcal} kcal logged",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                        )
                    }
                }

                if (loggedFoodsToday.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        loggedFoodsToday.forEach { entry ->
                            LoggedFoodRow(entry = entry, onDelete = {
                                scope.launch { database.foodLogEntryDao().delete(entry) }
                            })
                        }
                    }
                }

                FueruButton(text = "+ Add food", onClick = { showAddFood = true }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), modifier = Modifier.fillMaxWidth()) {
                    FueruButton(
                        text = "+ Custom food",
                        variant = FueruButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { showAddCustomFood = true },
                    )
                    FueruButton(
                        text = "+ Combine foods",
                        variant = FueruButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { showCombineFoods = true },
                    )
                }
            }
        }

        Text(
            text = "Targets are based on your onboarding weight, height, and activity level — update those in a future settings pass if they change.",
            color = FueruColors.TextMuted,
            style = FueruType.caption,
        )
    }

    if (showAddFood) {
        AddFoodDialog(
            database = database,
            date = today,
            onLogged = { entry ->
                scope.launch { database.foodLogEntryDao().insert(entry) }
                showAddFood = false
            },
            onDismiss = { showAddFood = false },
        )
    }

    if (showAddCustomFood) {
        AddCustomFoodDialog(
            database = database,
            onSaved = { showAddCustomFood = false },
            onDismiss = { showAddCustomFood = false },
        )
    }

    if (showCombineFoods) {
        CombineFoodsDialog(
            database = database,
            onSaved = { showCombineFoods = false },
            onDismiss = { showCombineFoods = false },
        )
    }
}

@Composable
private fun LoggedFoodRow(entry: FoodLogEntry, onDelete: () -> Unit) {
    FueruCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = entry.foodName, color = FueruColors.TextPrimary, style = FueruType.body)
                Text(
                    text = "${entry.servingGrams.roundToInt()}g · ${entry.kcal} kcal",
                    color = FueruColors.TextMuted,
                    style = FueruType.caption,
                )
            }
            Text(
                text = "remove",
                color = FueruColors.Fire4,
                style = FueruType.caption,
                modifier = Modifier.clickable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun AddFoodDialog(database: AppDatabase, date: Long, onLogged: (FoodLogEntry) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val apiKeyMissing = remember { BuildConfig.USDA_API_KEY.isBlank() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodPick>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<FoodPick?>(null) }
    var servingGramsText by remember(selected) { mutableStateOf("100") }

    fun runSearch() {
        if (query.isBlank()) return
        searching = true
        scope.launch {
            results = searchAllFoods(database, query)
            searching = false
            searched = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.space5)) {
                val current = selected
                if (current == null) {
                    Text(text = "add a food", color = FueruColors.TextPrimary, style = FueruType.title)
                    if (apiKeyMissing) {
                        Text(
                            text = "No USDA API key configured — database search won't return anything, but your own custom foods still will.",
                            color = FueruColors.SignalDanger,
                            style = FueruType.caption,
                            modifier = Modifier.padding(top = Spacing.space2),
                        )
                    }
                    Text(
                        text = "Search the USDA food database and your own custom foods — generic ingredients work best (\"chicken breast\", not a brand name).",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                        modifier = Modifier.padding(top = Spacing.space2, bottom = Spacing.space3),
                    )
                    FueruTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "e.g. chicken breast",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FueruButton(
                        text = if (searching) "Searching…" else "Search",
                        enabled = !searching && query.isNotBlank(),
                        onClick = { runSearch() },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                    )
                    if (searched && !searching && results.isEmpty()) {
                        Text(
                            text = "No results with full nutrition data — try a plainer search term.",
                            color = FueruColors.TextMuted,
                            style = FueruType.caption,
                            modifier = Modifier.padding(top = Spacing.space3),
                        )
                    }
                    if (results.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = Spacing.space3)) {
                            results.forEach { result ->
                                Text(
                                    text = "${result.name} · ${result.kcalPer100g.roundToInt()} kcal/100g",
                                    color = FueruColors.TextPrimary,
                                    style = FueruType.body,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selected = result }
                                        .padding(vertical = Spacing.space2),
                                )
                            }
                        }
                    }
                    FueruButton(
                        text = "Cancel",
                        variant = FueruButtonVariant.Ghost,
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = Spacing.space2),
                    )
                } else {
                    Text(text = current.name, color = FueruColors.TextPrimary, style = FueruType.title)
                    FueruTextField(
                        value = servingGramsText,
                        onValueChange = { servingGramsText = it },
                        label = "Serving size (grams)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                    )
                    val grams = servingGramsText.toFloatOrNull() ?: 0f
                    val factor = grams / 100f
                    Text(
                        text = "${(current.kcalPer100g * factor).roundToInt()} kcal · " +
                            "${(current.proteinPer100g * factor).roundToInt()}g protein · " +
                            "${(current.carbsPer100g * factor).roundToInt()}g carbs · " +
                            "${(current.fatPer100g * factor).roundToInt()}g fat",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                        modifier = Modifier.padding(top = Spacing.space2),
                    )
                    FueruButton(
                        text = "Log it",
                        enabled = grams > 0f,
                        onClick = {
                            onLogged(
                                FoodLogEntry(
                                    date = date,
                                    fdcId = current.fdcId,
                                    customFoodId = current.customFoodId,
                                    foodName = current.name,
                                    servingGrams = grams,
                                    proteinG = current.proteinPer100g * factor,
                                    carbsG = current.carbsPer100g * factor,
                                    fatG = current.fatPer100g * factor,
                                    kcal = (current.kcalPer100g * factor).roundToInt(),
                                    timestamp = System.currentTimeMillis(),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.space4),
                    )
                    FueruButton(
                        text = "Back to search",
                        variant = FueruButtonVariant.Ghost,
                        onClick = { selected = null },
                        modifier = Modifier.padding(top = Spacing.space2),
                    )
                }
            }
        }
    }
}

/** Manual entry for a food not worth searching USDA for — a homemade dish, a packaged item with its own label, etc. Once saved it's just a [CustomFood] row, searchable/loggable from "+ Add food" like anything else. */
@Composable
private fun AddCustomFoodDialog(database: AppDatabase, onSaved: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var kcalText by remember { mutableStateOf("") }

    val protein = proteinText.toFloatOrNull()
    val carbs = carbsText.toFloatOrNull()
    val fat = fatText.toFloatOrNull()
    val kcal = kcalText.toFloatOrNull()
    val canSave = name.isNotBlank() && protein != null && carbs != null && fat != null && kcal != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.space5)) {
                Text(text = "add a custom food", color = FueruColors.TextPrimary, style = FueruType.title)
                Text(
                    text = "Enter macros per 100g — same basis USDA uses, so it mixes cleanly with searched foods.",
                    color = FueruColors.TextMuted,
                    style = FueruType.caption,
                    modifier = Modifier.padding(top = Spacing.space2, bottom = Spacing.space3),
                )
                FueruTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    placeholder = "e.g. mom's lasagna",
                    modifier = Modifier.fillMaxWidth(),
                )
                FueruTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = "Protein per 100g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                )
                FueruTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it },
                    label = "Carbs per 100g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                )
                FueruTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = "Fat per 100g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                )
                FueruTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it },
                    label = "Calories per 100g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                )
                FueruButton(
                    text = "Save",
                    enabled = canSave,
                    onClick = {
                        scope.launch {
                            database.customFoodDao().insert(
                                CustomFood(
                                    name = name,
                                    proteinPer100g = protein ?: 0f,
                                    carbsPer100g = carbs ?: 0f,
                                    fatPer100g = fat ?: 0f,
                                    kcalPer100g = (kcal ?: 0f).roundToInt(),
                                    createdAt = System.currentTimeMillis(),
                                ),
                            )
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space4),
                )
                FueruButton(
                    text = "Cancel",
                    variant = FueruButtonVariant.Ghost,
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Spacing.space2),
                )
            }
        }
    }
}

/**
 * Combines multiple foods (each searched the same way as logging one directly) into a single
 * reusable [CustomFood] — "chicken, lettuce, taco seasoning, beans" become "chicken tacos" once.
 * Per-100g macros are the weighted sum of every ingredient's contribution divided by the recipe's
 * total weight, the standard way a nutrition label for a homemade dish is worked out.
 */
@Composable
private fun CombineFoodsDialog(database: AppDatabase, onSaved: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var recipeName by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<RecipeIngredient>>(emptyList()) }
    var addingIngredient by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(Radius.lg), color = FueruColors.SurfaceCard) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.space5)) {
                if (addingIngredient) {
                    IngredientPicker(
                        database = database,
                        onAdded = { picked ->
                            ingredients = ingredients + picked
                            addingIngredient = false
                        },
                        onCancel = { addingIngredient = false },
                    )
                } else {
                    val totalGrams = ingredients.sumOf { it.grams.toDouble() }.toFloat()
                    Text(text = "combine foods", color = FueruColors.TextPrimary, style = FueruType.title)
                    Text(
                        text = "Add each ingredient by weight — the total becomes one reusable food, e.g. \"chicken tacos.\"",
                        color = FueruColors.TextMuted,
                        style = FueruType.caption,
                        modifier = Modifier.padding(top = Spacing.space2, bottom = Spacing.space3),
                    )
                    FueruTextField(
                        value = recipeName,
                        onValueChange = { recipeName = it },
                        label = "Recipe name",
                        placeholder = "e.g. chicken tacos",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (ingredients.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = Spacing.space3)) {
                            ingredients.forEach { ingredient ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space1),
                                ) {
                                    Text(text = "${ingredient.pick.name} · ${ingredient.grams.roundToInt()}g", color = FueruColors.TextPrimary, style = FueruType.body)
                                    Text(
                                        text = "remove",
                                        color = FueruColors.Fire4,
                                        style = FueruType.caption,
                                        modifier = Modifier.clickable { ingredients = ingredients - ingredient },
                                    )
                                }
                            }
                            Text(
                                text = "total: ${totalGrams.roundToInt()}g",
                                color = FueruColors.TextMuted,
                                style = FueruType.caption,
                                modifier = Modifier.padding(top = Spacing.space1),
                            )
                        }
                    }
                    FueruButton(
                        text = "+ Add ingredient",
                        variant = FueruButtonVariant.Secondary,
                        onClick = { addingIngredient = true },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                    )
                    FueruButton(
                        text = "Save recipe",
                        enabled = recipeName.isNotBlank() && ingredients.isNotEmpty() && totalGrams > 0f,
                        onClick = {
                            scope.launch {
                                val proteinTotal = ingredients.sumOf { (it.pick.proteinPer100g * it.grams / 100f).toDouble() }.toFloat()
                                val carbsTotal = ingredients.sumOf { (it.pick.carbsPer100g * it.grams / 100f).toDouble() }.toFloat()
                                val fatTotal = ingredients.sumOf { (it.pick.fatPer100g * it.grams / 100f).toDouble() }.toFloat()
                                val kcalTotal = ingredients.sumOf { (it.pick.kcalPer100g * it.grams / 100f).toDouble() }.toFloat()
                                val customFoodId = database.customFoodDao().insert(
                                    CustomFood(
                                        name = recipeName,
                                        proteinPer100g = proteinTotal / totalGrams * 100f,
                                        carbsPer100g = carbsTotal / totalGrams * 100f,
                                        fatPer100g = fatTotal / totalGrams * 100f,
                                        kcalPer100g = (kcalTotal / totalGrams * 100f).roundToInt(),
                                        createdAt = System.currentTimeMillis(),
                                    ),
                                )
                                database.customFoodIngredientDao().insertAll(
                                    ingredients.map { CustomFoodIngredient(customFoodId = customFoodId, name = it.pick.name, grams = it.grams) },
                                )
                                onSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
                    )
                    FueruButton(
                        text = "Cancel",
                        variant = FueruButtonVariant.Ghost,
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = Spacing.space2),
                    )
                }
            }
        }
    }
}

private data class RecipeIngredient(val pick: FoodPick, val grams: Float)

/** Search-then-weigh step nested inside [CombineFoodsDialog] — same search behavior as logging a food directly, but the terminal action adds it to the in-progress recipe instead of logging it. */
@Composable
private fun IngredientPicker(database: AppDatabase, onAdded: (RecipeIngredient) -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodPick>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<FoodPick?>(null) }
    var gramsText by remember(picked) { mutableStateOf("100") }

    val current = picked
    if (current == null) {
        Text(text = "add an ingredient", color = FueruColors.TextPrimary, style = FueruType.title)
        FueruTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "e.g. chicken breast",
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
        )
        FueruButton(
            text = if (searching) "Searching…" else "Search",
            enabled = !searching && query.isNotBlank(),
            onClick = {
                searching = true
                scope.launch {
                    results = searchAllFoods(database, query)
                    searching = false
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
        )
        if (results.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = Spacing.space3)) {
                results.forEach { result ->
                    Text(
                        text = "${result.name} · ${result.kcalPer100g.roundToInt()} kcal/100g",
                        color = FueruColors.TextPrimary,
                        style = FueruType.body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { picked = result }
                            .padding(vertical = Spacing.space2),
                    )
                }
            }
        }
        FueruButton(text = "Cancel", variant = FueruButtonVariant.Ghost, onClick = onCancel, modifier = Modifier.padding(top = Spacing.space2))
    } else {
        Text(text = current.name, color = FueruColors.TextPrimary, style = FueruType.title)
        FueruTextField(
            value = gramsText,
            onValueChange = { gramsText = it },
            label = "Amount (grams)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
        )
        val grams = gramsText.toFloatOrNull() ?: 0f
        FueruButton(
            text = "Add to recipe",
            enabled = grams > 0f,
            onClick = { onAdded(RecipeIngredient(current, grams)) },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.space3),
        )
        FueruButton(
            text = "Back to search",
            variant = FueruButtonVariant.Ghost,
            onClick = { picked = null },
            modifier = Modifier.padding(top = Spacing.space2),
        )
    }
}
