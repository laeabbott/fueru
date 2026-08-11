package com.fueru.app.data

import android.content.Context
import com.fueru.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** [versionCode]/[versionName] mirror the matching GitHub Release; [downloadUrl] is the release's .apk asset. */
data class UpdateInfo(val versionCode: Int, val versionName: String, val downloadUrl: String)

/**
 * In-app-update round — checks the (now-public) repo's GitHub Releases for a build newer than this
 * install. Plain HttpURLConnection + org.json, matching GiphyApi.kt's established convention for a
 * single-endpoint client rather than pulling in Retrofit/OkHttp for one call. No auth needed —
 * `/releases/latest` and release asset downloads are both unauthenticated for a public repo.
 *
 * Release tags are `v<versionCode>` (an integer, stamped by CI as `github.run_number` — see
 * .github/workflows/build.yml's release job), so "is this newer" is a plain integer comparison
 * against [com.fueru.app.BuildConfig.VERSION_CODE], no semver parsing needed.
 */
object AppUpdateChecker {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/laeabbott/fueru/releases/latest"

    /** Null on any failure (network, parse, no .apk asset) or if already up to date — never crashes, matches GiphyApi's own "silently absent" convention. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val body = try {
            get(URL(LATEST_RELEASE_URL))
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        try {
            val json = JSONObject(body)
            val versionCode = json.optString("tag_name").removePrefix("v").toIntOrNull() ?: return@withContext null
            if (versionCode <= BuildConfig.VERSION_CODE) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (asset.optString("name").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url").ifBlank { null }
                    break
                }
            }
            downloadUrl ?: return@withContext null

            UpdateInfo(
                versionCode = versionCode,
                versionName = json.optString("name").ifBlank { json.optString("tag_name") },
                downloadUrl = downloadUrl,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Streams [url] into a fresh file in [context]'s cache dir, reporting 0f-1f progress as it
     * goes (indeterminate — passes null — if the server doesn't send a Content-Length). Null on
     * any failure, same "never crash on a network hiccup" convention as [checkForUpdate].
     */
    suspend fun downloadApk(context: Context, url: String, onProgress: (Float?) -> Unit): File? = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "fueru-update.apk")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                connect()
            }
            if (connection.responseCode !in 200..299) return@withContext null

            val total = connection.contentLength
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0
                    var read = input.read(buffer)
                    while (read >= 0) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(if (total > 0) downloaded / total.toFloat() else null)
                        read = input.read(buffer)
                    }
                }
            }
            outFile
        } catch (e: Exception) {
            outFile.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun get(url: URL): String? {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
