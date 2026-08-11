package com.fueru.app.data

import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.entity.PrescribedSet
import com.fueru.app.data.entity.SetLog
import com.fueru.app.data.entity.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [computeProgression] — the pure decision logic extracted from [suggestProgression]
 * specifically so it's testable without a database (see that function's doc comment and the
 * HANDOFF.md architecture-review round). [suggestProgression] itself (the suspend, DB-fetching
 * wrapper) is intentionally not covered here — it's a thin fetch-then-delegate, nothing to unit
 * test there without a real or fake Room instance.
 */
class ProgressionAlgorithmTest {

    private fun barbellExercise(equipment: String = "barbell") = Exercise(
        id = "test_exercise",
        name = "Test Exercise",
        primaryMuscle = "chest",
        secondaryMuscles = emptyList(),
        equipment = equipment,
        imageAssetPaths = emptyList(),
        instructions = "",
    )

    private fun prescribedSet(repsMin: Int = 8, repsMax: Int = 12) = PrescribedSet(
        programDayId = 1L,
        exerciseId = "test_exercise",
        orderInDay = 1,
        sets = 3,
        repsMin = repsMin,
        repsMax = repsMax,
        tempo = "2-1-2",
        comment = null,
        supersetGroup = null,
        isDropSetFinal = false,
        isTensionFocus = false,
    )

    private fun profile(strengthLevel: Int = 3) = UserProfile(
        displayName = "Tester",
        heightCm = 170f,
        bodyWeightKg = 70f,
        weightIsEstimated = false,
        age = 30,
        bmrFormulaVariant = "A",
        activityLevel = "moderate",
        goal = "maintain",
        strengthLevel = strengthLevel,
        foodTrackingEnabled = false,
        foodTrackingMode = null,
        equipmentPreference = null,
        useRecurringSchedule = false,
        programStartDate = 0L,
        currentPhase = "0-6",
    )

    private fun setLog(
        scheduledWorkoutId: Long,
        prescribedReps: Int,
        actualWeight: Float?,
        shortfallReason: String? = null,
        rpe: Int? = null,
        timestamp: Long = 0L,
    ) = SetLog(
        scheduledWorkoutId = scheduledWorkoutId,
        exerciseId = "test_exercise",
        setNumber = 1,
        prescribedWeight = actualWeight,
        prescribedReps = prescribedReps,
        actualWeight = actualWeight,
        actualReps = prescribedReps,
        shortfallReason = shortfallReason,
        rpe = rpe,
        timestamp = timestamp,
    )

    @Test
    fun `no history on a weighted exercise starts from StartingWeightSeed, not from history`() {
        val suggestion = computeProgression(
            history = emptyList(),
            exercise = barbellExercise(),
            prescribedSet = prescribedSet(repsMin = 8),
            profile = profile(),
            unit = WeightUnit.KG,
        )
        assertEquals(8, suggestion.targetReps)
        assertEquals(false, suggestion.weightFromHistory)
        assertEquals(false, suggestion.justBumpedWeight)
        assertTrue("expected a real starting weight for a weighted exercise", suggestion.suggestedWeightKg != null && suggestion.suggestedWeightKg > 0f)
    }

    @Test
    fun `no history on a bodyweight exercise suggests no weight at all`() {
        val suggestion = computeProgression(
            history = emptyList(),
            exercise = barbellExercise(equipment = "body only"),
            prescribedSet = prescribedSet(),
            profile = profile(),
            unit = WeightUnit.KG,
        )
        assertNull(suggestion.suggestedWeightKg)
    }

    @Test
    fun `a shortfall in the last session holds everything steady -- never auto-deloads`() {
        val history = listOf(
            setLog(scheduledWorkoutId = 1L, prescribedReps = 10, actualWeight = 60f, shortfallReason = "couldnt"),
        )
        val suggestion = computeProgression(history, barbellExercise(), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(10, suggestion.targetReps)
        assertEquals(60f, suggestion.suggestedWeightKg)
        assertEquals(false, suggestion.justBumpedWeight)
    }

    @Test
    fun `RPE 8 or above with no shortfall still holds steady -- not evidence there's more in the tank`() {
        val history = listOf(
            setLog(scheduledWorkoutId = 1L, prescribedReps = 10, actualWeight = 60f, rpe = 8),
        )
        val suggestion = computeProgression(history, barbellExercise(), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(10, suggestion.targetReps)
        assertEquals(false, suggestion.justBumpedWeight)
    }

    @Test
    fun `no shortfall, RPE under 8, below the rep ceiling -- climbs one rep and holds weight`() {
        val history = listOf(
            setLog(scheduledWorkoutId = 1L, prescribedReps = 10, actualWeight = 60f, rpe = 7),
        )
        val suggestion = computeProgression(history, barbellExercise(), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(11, suggestion.targetReps)
        assertEquals(60f, suggestion.suggestedWeightKg)
        assertEquals(false, suggestion.justBumpedWeight)
    }

    @Test
    fun `no shortfall and RPE not logged at all still advances -- doesn't stall users who skip RPE`() {
        val history = listOf(
            setLog(scheduledWorkoutId = 1L, prescribedReps = 10, actualWeight = 60f, rpe = null),
        )
        val suggestion = computeProgression(history, barbellExercise(), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(11, suggestion.targetReps)
    }

    @Test
    fun `clearing the rep ceiling resets to the floor and bumps the weight`() {
        val history = listOf(
            setLog(scheduledWorkoutId = 1L, prescribedReps = 12, actualWeight = 60f, rpe = 6),
        )
        val suggestion = computeProgression(history, barbellExercise(), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(8, suggestion.targetReps)
        assertEquals(true, suggestion.justBumpedWeight)
        // Non-fine-grained equipment (barbell), KG unit -> +2.5kg per bumpWeight's own increment table.
        assertEquals(62.5f, suggestion.suggestedWeightKg!!, 0.01f)
    }

    @Test
    fun `clearing the ceiling on bodyweight work holds at the ceiling -- nothing to bump`() {
        val history = listOf(
            setLog(scheduledWorkoutId = 1L, prescribedReps = 12, actualWeight = null, rpe = 6),
        )
        val suggestion = computeProgression(history, barbellExercise(equipment = "body only"), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(12, suggestion.targetReps)
        assertEquals(false, suggestion.justBumpedWeight)
        assertNull(suggestion.suggestedWeightKg)
    }

    @Test
    fun `only the most recent session's rows count -- history is keyed by the first row's scheduledWorkoutId`() {
        // Most-recent-first, matching SetLogDao.getAllForExercise's own ORDER BY timestamp DESC.
        // The newest session (id 2) had a shortfall; an older session (id 1) didn't -- only the
        // newest session's outcome should matter.
        val history = listOf(
            setLog(scheduledWorkoutId = 2L, prescribedReps = 10, actualWeight = 60f, shortfallReason = "couldnt", timestamp = 200L),
            setLog(scheduledWorkoutId = 1L, prescribedReps = 10, actualWeight = 55f, timestamp = 100L),
        )
        val suggestion = computeProgression(history, barbellExercise(), prescribedSet(repsMin = 8, repsMax = 12), profile(), WeightUnit.KG)
        assertEquals(60f, suggestion.suggestedWeightKg) // from session 2, not session 1's 55f
        assertEquals(false, suggestion.justBumpedWeight) // session 2 had a shortfall -> hold steady
    }
}
