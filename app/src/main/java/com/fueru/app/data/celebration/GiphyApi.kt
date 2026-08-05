package com.fueru.app.data.celebration

import com.fueru.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Thin client for Giphy's random-gif endpoint (https://developers.giphy.com/) — plain
 * HttpURLConnection + org.json, matching how UsdaFoodApi.kt and the ICS import handle network
 * calls elsewhere in this project (no Retrofit/OkHttp for a single endpoint).
 *
 * Uses `/v1/gifs/random?tag=...` rather than `/v1/gifs/search`, since celebration moments just
 * need *a* fitting gif, not a list to choose from. A live test call confirmed the response shape:
 * on a match, `data` is a JSON object with `images.fixed_height.url` (a bounded-size gif, better
 * fit for a phone screen than the full-resolution `images.original`); when nothing matches the
 * tag, `data` comes back as an *empty array*, not an object — `optJSONObject` returns null for
 * that case for free, so no separate type-check is needed.
 */
/** Regular completions/onboarding — lighter, everyday "nice one" energy. Anime/meme/pop-culture, deliberately steered away from generic sports-highlight gifs. */
private val regularCelebrationTags = listOf(
    "anime celebration",
    "excited anime",
    "spongebob excited",
    "the office success",
    "wholesome anime",
    "anime yes",
    "cute anime happy",
)

/** Streak-milestone completions — bigger-deal, hype energy. */
private val milestoneCelebrationTags = listOf(
    "anime hype",
    "dragon ball power up",
    "naruto hype",
    "one piece hype",
    "anime power up",
    "epic anime",
    "shonen hype",
)

object GiphyApi {
    private const val BASE_URL = "https://api.giphy.com/v1/gifs/random"

    /** Picks a random tag from the anime/meme/pop-culture pool matching [milestone] — a different gif each time, not the same fixed tag every call. */
    suspend fun randomCelebrationGifUrl(milestone: Boolean): String? =
        randomGifUrl((if (milestone) milestoneCelebrationTags else regularCelebrationTags).random())

    /** Silently returns null on a missing key, network failure, or no match — never crashes. */
    suspend fun randomGifUrl(tag: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GIPHY_API_KEY
        if (apiKey.isBlank()) return@withContext null

        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val url = URL("$BASE_URL?api_key=$apiKey&tag=$encodedTag&rating=g")

        val body = try {
            get(url)
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        try {
            JSONObject(body).optJSONObject("data")
                ?.optJSONObject("images")
                ?.optJSONObject("fixed_height")
                ?.optString("url")
                ?.ifBlank { null }
        } catch (e: Exception) {
            null
        }
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
