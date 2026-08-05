package com.fueru.app.data.food

import com.fueru.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Per-100g macros, straight from USDA — [UsdaFoodApi] scales these by serving size at log time. */
data class UsdaFoodResult(
    val fdcId: Int,
    val name: String,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val kcalPer100g: Float,
)

private const val NUTRIENT_ID_PROTEIN = 1003
private const val NUTRIENT_ID_FAT = 1004
private const val NUTRIENT_ID_CARBS = 1005
private const val NUTRIENT_ID_ENERGY = 1008

/**
 * Thin client for USDA FoodData Central (https://fdc.nal.usda.gov/) — plain HttpURLConnection +
 * org.json rather than adding Retrofit/OkHttp for a single endpoint, matching how the ICS import
 * and exercise-catalog seeding handle parsing elsewhere in this project.
 *
 * The search endpoint conveniently already returns each food's full nutrient breakdown, so there's
 * no second "get details" call needed — one request per search. Nutrients are matched by USDA's
 * standard, stable nutrientId (1003/1004/1005/1008) rather than by name, since name strings can
 * vary slightly between records ("Energy" appears twice per food — once in kcal, once in kJ — the
 * unit is what disambiguates them, not the name).
 */
object UsdaFoodApi {
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1"

    /**
     * Searches USDA's Foundation + SR Legacy datasets (both are generic, per-100g reference foods —
     * not branded/packaged products, which is exactly the "ingredient" framing this feature wants).
     * Silently returns an empty list on any network/parse failure, and drops any result missing a
     * usable calorie value (a handful of Foundation entries only carry micronutrient data, no
     * macros) rather than showing a food that can't actually be logged.
     */
    suspend fun search(query: String): List<UsdaFoodResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.USDA_API_KEY
        if (apiKey.isBlank() || query.isBlank()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL(
            "$BASE_URL/foods/search?query=$encodedQuery&pageSize=20" +
                "&dataType=Foundation,SR%20Legacy&api_key=$apiKey",
        )

        val body = try {
            get(url)
        } catch (e: Exception) {
            null
        } ?: return@withContext emptyList()

        try {
            val foods = JSONObject(body).optJSONArray("foods") ?: return@withContext emptyList()
            (0 until foods.length())
                .mapNotNull { i -> parseFood(foods.getJSONObject(i)) }
                .filter { it.kcalPer100g > 0f }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFood(food: JSONObject): UsdaFoodResult? {
        val fdcId = food.optInt("fdcId", -1)
        if (fdcId == -1) return null
        val name = food.optString("description", "").ifBlank { return null }

        val nutrients = food.optJSONArray("foodNutrients") ?: JSONArray()
        var protein = 0f
        var carbs = 0f
        var fat = 0f
        var kcal = 0f
        for (i in 0 until nutrients.length()) {
            val nutrient = nutrients.getJSONObject(i)
            val value = nutrient.optDouble("value", 0.0).toFloat()
            when (nutrient.optInt("nutrientId", -1)) {
                NUTRIENT_ID_PROTEIN -> protein = value
                NUTRIENT_ID_FAT -> fat = value
                NUTRIENT_ID_CARBS -> carbs = value
                NUTRIENT_ID_ENERGY -> if (nutrient.optString("unitName").equals("KCAL", ignoreCase = true)) kcal = value
            }
        }

        return UsdaFoodResult(
            fdcId = fdcId,
            name = name,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            kcalPer100g = kcal,
        )
    }

    private fun get(url: URL): String? {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
