package com.fueru.app.data.seed

import android.content.Context
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.entity.Exercise
import org.json.JSONArray

private const val ASSET_FILE = "exercise_catalog.json"
private const val IMAGE_BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

/**
 * Expands the exercise table beyond the lean, hand-picked, locally-bundled-image subset in
 * ExerciseSeed.kt (29 exercises) with the rest of free-exercise-db's ~873 exercises — the exact
 * same public-domain source, just not worth shipping ~800 exercises' JPGs in the APK. Images for
 * these load on demand over the network (same GitHub repo, via Coil — which caches to disk after
 * first view, so it's not a re-download every time) instead of being bundled; see
 * ExerciseFormImages in WorkoutScreen.kt, which picks a local-asset vs. network URL per path.
 *
 * Only inserts exercises not already present locally — the hand-picked 29 keep their bundled
 * local image paths untouched, this never overwrites them.
 */
suspend fun ensureExerciseCatalogSeeded(context: Context, database: AppDatabase) {
    if (database.exerciseDao().count() >= 800) return

    val existingIds = ExerciseSeed.all.map { it.id }.toSet()
    val json = context.assets.open(ASSET_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
    val array = JSONArray(json)

    val exercises = buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            if (id in existingIds) continue

            val primaryMusclesArray = obj.optJSONArray("primaryMuscles")
            val primaryMuscle = if (primaryMusclesArray != null && primaryMusclesArray.length() > 0) {
                primaryMusclesArray.getString(0)
            } else {
                "other"
            }

            val secondaryMusclesArray = obj.optJSONArray("secondaryMuscles")
            val secondaryMuscles = buildList {
                if (secondaryMusclesArray != null) {
                    for (j in 0 until secondaryMusclesArray.length()) add(secondaryMusclesArray.getString(j))
                }
            }

            val instructionsArray = obj.optJSONArray("instructions")
            val instructions = if (instructionsArray != null && instructionsArray.length() > 0) {
                (0 until instructionsArray.length()).joinToString("\n") { j -> "${j + 1}. ${instructionsArray.getString(j)}" }
            } else {
                "No instructions available for this exercise."
            }

            val imagesArray = obj.optJSONArray("images")
            val imagePaths = buildList {
                if (imagesArray != null) {
                    for (j in 0 until imagesArray.length()) add(IMAGE_BASE_URL + imagesArray.getString(j))
                }
            }

            add(
                Exercise(
                    id = id,
                    name = obj.getString("name"),
                    primaryMuscle = primaryMuscle,
                    secondaryMuscles = secondaryMuscles,
                    equipment = obj.optString("equipment", "other"),
                    imageAssetPaths = imagePaths,
                    instructions = instructions,
                ),
            )
        }
    }

    database.exerciseDao().insertAll(exercises)
}
