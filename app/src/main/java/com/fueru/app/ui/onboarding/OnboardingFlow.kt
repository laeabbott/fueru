package com.fueru.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fueru.app.FueruApplication
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.DateUtils
import com.fueru.app.data.WeightUnitStore
import com.fueru.app.data.autoFillRecurringWeek
import com.fueru.app.data.celebration.GiphyApi
import com.fueru.app.data.entity.RecurringScheduleEntry
import com.fueru.app.data.entity.UserProfile
import kotlinx.coroutines.launch

private enum class OnboardingStep {
    WELCOME, NAME, BASIC_INFO, BODY_WEIGHT, FITNESS_LEVEL, EQUIPMENT_PREFERENCE, BMR_FORMULA, ACTIVITY_LEVEL, GOAL,
    FOOD_TRACKING, CALENDAR_PERMISSION, NOTIFICATION_PERMISSION, SCHEDULE, PROGRAM_START, CELEBRATION,
}

private val steps = OnboardingStep.entries

@Composable
fun OnboardingFlow(onDone: () -> Unit) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val scope = rememberCoroutineScope()
    val state = remember { OnboardingState() }
    var stepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[stepIndex]

    // Prefetched here, at the very start of onboarding, rather than when CelebrationStep (the
    // last step) actually mounts — by the time someone works through every step, the network
    // round-trip to Giphy is long done, so the reward screen shows a gif instantly.
    var celebrationGifUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { celebrationGifUrl = GiphyApi.randomCelebrationGifUrl(milestone = false) }

    fun goNext() {
        if (currentStep == OnboardingStep.PROGRAM_START) {
            scope.launch {
                WeightUnitStore.save(application, state.weightUnit)
                persistOnboarding(application.database, state)
            }
        }
        if (stepIndex < steps.lastIndex) stepIndex++ else onDone()
    }

    fun goBack() {
        if (stepIndex > 0) stepIndex--
    }

    // Celebration is the reward screen, not part of the step-progress dots.
    if (currentStep == OnboardingStep.CELEBRATION) {
        CelebrationStep(displayName = state.displayName, gifUrl = celebrationGifUrl, onDone = ::goNext)
        return
    }

    OnboardingScaffold(
        stepIndex = stepIndex,
        totalSteps = steps.size - 1,
        onBack = if (stepIndex > 0) ::goBack else null,
    ) {
        when (currentStep) {
            OnboardingStep.WELCOME -> WelcomeStep(onNext = ::goNext)
            OnboardingStep.NAME -> NameStep(state = state, onNext = ::goNext)
            OnboardingStep.BASIC_INFO -> BasicInfoStep(state = state, onNext = ::goNext)
            OnboardingStep.BODY_WEIGHT -> BodyWeightStep(state = state, onNext = ::goNext)
            OnboardingStep.FITNESS_LEVEL -> FitnessLevelStep(state = state, onNext = ::goNext)
            OnboardingStep.EQUIPMENT_PREFERENCE -> EquipmentPreferenceStep(state = state, onNext = ::goNext)
            OnboardingStep.BMR_FORMULA -> BmrFormulaStep(state = state, onNext = ::goNext)
            OnboardingStep.ACTIVITY_LEVEL -> ActivityLevelStep(state = state, onNext = ::goNext)
            OnboardingStep.GOAL -> GoalStep(state = state, onNext = ::goNext)
            OnboardingStep.FOOD_TRACKING -> FoodTrackingStep(state = state, onNext = ::goNext)
            OnboardingStep.CALENDAR_PERMISSION -> CalendarPermissionStep(state = state, onNext = ::goNext)
            OnboardingStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep(state = state, onNext = ::goNext)
            OnboardingStep.SCHEDULE -> ScheduleStep(state = state, onNext = ::goNext)
            OnboardingStep.PROGRAM_START -> ProgramStartStep(state = state, onNext = ::goNext)
            OnboardingStep.CELEBRATION -> Unit // handled above
        }
    }
}

private suspend fun persistOnboarding(database: AppDatabase, state: OnboardingState) {
    database.userProfileDao().upsert(
        UserProfile(
            displayName = state.displayName.ifBlank { "friend" },
            heightCm = state.heightCm,
            bodyWeightKg = state.bodyWeightKg,
            weightIsEstimated = state.weightIsEstimated,
            age = state.age,
            bmrFormulaVariant = state.bmrFormulaVariant,
            activityLevel = state.activityLevel,
            goal = state.goal,
            strengthLevel = state.strengthLevel,
            foodTrackingEnabled = state.foodTrackingEnabled,
            foodTrackingMode = state.foodTrackingMode,
            equipmentPreference = state.equipmentPreference,
            useRecurringSchedule = state.useRecurringSchedule,
            programStartDate = state.programStartDate,
            currentPhase = "0-6",
        ),
    )

    if (state.useRecurringSchedule && state.recurringAssignments.isNotEmpty()) {
        database.recurringScheduleDao().insertAll(
            state.recurringAssignments.map { (programDayId, assignment) ->
                RecurringScheduleEntry(
                    programDayId = programDayId,
                    dayOfWeek = assignment.dayOfWeek,
                    timeOfDay = assignment.timeOfDay,
                )
            },
        )
        autoFillRecurringWeek(database, DateUtils.startOfWeek(DateUtils.todayEpochMillis()))
    }
}
