package com.fueru.app.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [imageAssetPaths] holds paths into assets/exercises/<id>/ — free-exercise-db ships two static
 * JPGs per exercise (start/end position), not an animated GIF, despite the original spec calling
 * this field gifAssetPath. The workout screen (follow-up phase) cross-fades between the two.
 *
 * [Immutable]: the `List<String>` fields are the reason the Compose compiler can't infer this
 * class stable on its own (plain `List` isn't compiler-known-stable) -- this promise is safe
 * because every `Exercise` instance is always freshly constructed (Room query results, seed data)
 * and never mutated in place after that. Without it, `Exercise` cascades unstable into `WorkoutSlot`
 * -> `WorkoutSessionPlan` and `ExerciseProgress` -> `ProgressOverview`, forcing their parent
 * composables (ActiveWorkoutSession, ProgressScreen) to recompose more than necessary. Confirmed
 * via the Compose compiler's own metrics report (`./gradlew compileDebugKotlin --rerun-tasks`,
 * see build/compose_compiler/reports/ -- reportsDestination/metricsDestination are configured in
 * app/build.gradle.kts).
 */
@Immutable
@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val imageAssetPaths: List<String>,
    val instructions: String,
)
