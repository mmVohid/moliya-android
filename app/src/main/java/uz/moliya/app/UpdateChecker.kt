package uz.moliya.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateResult {
    data object Latest : UpdateResult
    data class Available(val url: String) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/** GitHub nashrini faqat foydalanuvchi tugmani bosganda tekshiradi. Hech qanday shaxsiy ma'lumot yuborilmaydi. */
object UpdateChecker {
    private const val API = "https://api.github.com/repos/mmVohid/moliya-android/releases/tags/latest"

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(API).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val code = conn.responseCode
            if (code != 200) {
                UpdateResult.Failed("Server javobi: $code")
            } else {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val sha = Regex("sha:([0-9a-f]{7,40})").find(json.optString("body"))?.groupValues?.get(1)
                val assets = json.optJSONArray("assets")
                val url = if (assets != null && assets.length() > 0) {
                    assets.getJSONObject(0).optString("browser_download_url")
                } else {
                    json.optString("html_url")
                }
                when {
                    sha == null -> UpdateResult.Failed("Versiya ma'lumoti topilmadi")
                    sha == BuildConfig.GIT_SHA -> UpdateResult.Latest
                    else -> UpdateResult.Available(url)
                }
            }
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: "Internetga ulanib bo'lmadi")
        }
    }
}
