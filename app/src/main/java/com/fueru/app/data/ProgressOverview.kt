package com.fueru.app.data

import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.data.entity.SetLog

data class CompletedSession(
    val scheduledWorkout: ScheduledWorkout,
    val dayLabel: String,
)

data class ExerciseProgress(
    val exercise: Exercise,
    val lastSetLog: SetLog,
)

data class ProgressOverview(
    val completedSessions: List<CompletedSession>,
    val exerciseProgress: List<ExerciseProgress>,
)

/** Everything the Progress screen needs — empty lists until the Workout screen has logged something. */
suspend fun loadProgressOverview(database: AppDatabase): ProgressOverview {
    val completed = database.scheduledWorkoutDao().getAllCompleted().mapNotNull { workout ->
        val dayLabel = database.programDayDao().getById(workout.programDayId)?.dayLabel ?: return@mapNotNull null
        CompletedSession(workout, dayLabel)
    }

    val exerciseProgress = database.setLogDao().getDistinctExerciseIds().mapNotNull { exerciseId ->
        val lastLog = database.setLogDao().getLastForExercise(exerciseId) ?: return@mapNotNull null
        val exercise = database.exerciseDao().getById(exerciseId) ?: return@mapNotNull null
        ExerciseProgress(exercise, lastLog)
    }.sortedByDescending { it.lastSetLog.timestamp }

    return ProgressOverview(completed, exerciseProgress)
}
