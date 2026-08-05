package com.fueru.app.data

import com.fueru.app.data.entity.DailyNutritionLog
import com.fueru.app.data.entity.UserProfile

/** Shared by Home's checklist card and the Fuel tab — both show the same day's running tally. */
data class NutritionSnapshot(
    val log: DailyNutritionLog,
    val targets: TdeeCalculator.MacroTargets,
    val mode: String,
)

/** Null when the user hasn't entered a body weight yet — TDEE math needs it. */
suspend fun loadNutritionSnapshot(database: AppDatabase, profile: UserProfile): NutritionSnapshot? {
    val weightKg = profile.bodyWeightKg ?: return null
    val today = DateUtils.todayEpochMillis()
    val log = database.dailyNutritionLogDao().getForDate(today) ?: DailyNutritionLog(date = today)
    val targets = TdeeCalculator.macroTargets(
        weightKg = weightKg,
        heightCm = profile.heightCm,
        age = profile.age,
        bmrFormulaVariant = profile.bmrFormulaVariant,
        activityLevel = profile.activityLevel,
    )
    return NutritionSnapshot(log = log, targets = targets, mode = profile.foodTrackingMode ?: "macros")
}
