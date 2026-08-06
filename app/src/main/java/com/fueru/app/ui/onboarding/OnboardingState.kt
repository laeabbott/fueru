package com.fueru.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fueru.app.data.BodyType
import com.fueru.app.data.DateUtils
import com.fueru.app.data.WeightUnit

/** Accumulates every onboarding answer in-memory; persisted in one shot on the Program Start step. */
class OnboardingState {
    var displayName by mutableStateOf("")
    var heightCm by mutableStateOf(170f)
    var age by mutableStateOf(25)

    var bodyWeightKg by mutableStateOf<Float?>(null)
    var weightIsEstimated by mutableStateOf(false)
    var selectedBodyType by mutableStateOf<BodyType?>(null)
    /** Display/input unit only — bodyWeightKg above is always the canonical kg value. Defaults to lb (see WeightUnitStore). */
    var weightUnit by mutableStateOf(WeightUnit.LB)

    /** 1 ("could be mistaken for a skeleton") .. 5 ("swol like yo mama"). */
    var strengthLevel by mutableStateOf(3)

    /** "bodyweight" / "freeWeight" / "machines", or null for no preference — see data/EquipmentPreference.kt. */
    var equipmentPreference by mutableStateOf<String?>(null)

    /** "A" or "B", see spec 7.2. */
    var bmrFormulaVariant by mutableStateOf("A")

    /** sedentary / light / moderate / active / veryActive */
    var activityLevel by mutableStateOf("sedentary")

    /** "buildMuscle" or "maintain" — feeds TdeeCalculator's surplus decision. */
    var goal by mutableStateOf("maintain")

    var foodTrackingEnabled by mutableStateOf(false)
    /** "macros" or "mealBalance" */
    var foodTrackingMode by mutableStateOf<String?>(null)

    var calendarPermissionRequested by mutableStateOf(false)
    var notificationPermissionRequested by mutableStateOf(false)

    var useRecurringSchedule by mutableStateOf(false)
    /** programDayId -> weekday + optional time */
    var recurringAssignments by mutableStateOf<Map<Long, RecurringAssignment>>(emptyMap())

    var programStartDate by mutableStateOf(DateUtils.todayEpochMillis())
}

/** [dayOfWeek] is ISO (1=Mon..7=Sun); [timeOfDay] is minutes since midnight, or null if unset. */
data class RecurringAssignment(val dayOfWeek: Int, val timeOfDay: Int? = null)
