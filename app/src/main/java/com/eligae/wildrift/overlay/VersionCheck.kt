package com.eligae.wildrift.overlay

import android.content.Context
import com.eligae.wildrift.overlay.api.GitHubRelease
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * GitHub Release API 폴링. 6시간 캐시 (rate limit 회피).
 * 새 버전 발견 시 [Result] 반환 — MainActivity 배너에 표시.
 * 사용자가 "닫기" 누른 버전은 dismiss 저장 → 재안내 안 함.
 */
object VersionCheck {

    private const val REPO = "Eligae/wildrift_overlay"
    private const val URL = "https://api.github.com/repos/$REPO/releases/latest"
    private const val PAGES_URL = "https://eligae.github.io/wildrift_overlay/"

    private const val PREFS = "version_check"
    private const val K_FETCHED_AT = "fetched_at"
    private const val K_TAG = "tag"
    private const val K_URL = "url"
    private const val K_DISMISSED = "dismissed"
    private const val TTL_MS = 6L * 60 * 60 * 1000

    data class Result(
        val newTag: String,
        val pageUrl: String,
        val releaseUrl: String,
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GitHubRelease::class.java)
    private val client = OkHttpClient()

    suspend fun check(context: Context): Result? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val cachedTag: String?
        val cachedUrl: String?
        if (now - prefs.getLong(K_FETCHED_AT, 0) < TTL_MS) {
            cachedTag = prefs.getString(K_TAG, null)
            cachedUrl = prefs.getString(K_URL, null)
        } else {
            try {
                val req = Request.Builder().url(URL).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body?.string() ?: return@withContext null
                    val r = adapter.fromJson(body) ?: return@withContext null
                    prefs.edit()
                        .putLong(K_FETCHED_AT, now)
                        .putString(K_TAG, r.tag_name)
                        .putString(K_URL, r.html_url)
                        .apply()
                    cachedTag = r.tag_name
                    cachedUrl = r.html_url
                }
            } catch (_: Exception) {
                return@withContext null
            }
        }

        val tag = cachedTag ?: return@withContext null
        val url = cachedUrl ?: return@withContext null
        val current = "v" + BuildConfig.VERSION_NAME
        if (tag == current) return@withContext null
        if (tag == prefs.getString(K_DISMISSED, null)) return@withContext null
        Result(tag, PAGES_URL, url)
    }

    fun dismiss(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(K_DISMISSED, tag).apply()
    }
}
