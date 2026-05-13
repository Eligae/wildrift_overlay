package com.eligae.wildrift.overlay.update

import android.content.Context
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.NewsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 공식 와일드리프트 게임 업데이트 페이지의 최신 1건.
 * 하루 1회만 fetch — 이미 오늘 날짜로 가져온 캐시가 있으면 그대로 사용.
 * TierActivity refresh 버튼이 [invalidate]로 캐시를 비울 수 있다.
 */
object NewsCheck {

    private const val PREFS = "news_check"
    private const val K_LAST_SEEN_URL = "last_seen_url"
    private const val K_CACHED_NEWS = "cached_news_json"
    private const val K_CACHED_DATE = "cached_date"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(NewsResponse::class.java)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    /** 새 패치노트가 있으면 반환, 이미 본 글이거나 실패 시 null. */
    suspend fun checkLatest(context: Context): NewsResponse? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = dateFmt.format(Date())
        val news = loadCached(prefs, today) ?: fetchAndCache(prefs, today) ?: return null
        val lastSeen = prefs.getString(K_LAST_SEEN_URL, null)
        if (news.url == lastSeen) return null
        return news
    }

    fun markSeen(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(K_LAST_SEEN_URL, url).apply()
    }

    /** 캐시 무효화 — 다음 [checkLatest]에서 서버 재호출. */
    fun invalidate(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(K_CACHED_NEWS).remove(K_CACHED_DATE).apply()
    }

    private fun loadCached(prefs: android.content.SharedPreferences, today: String): NewsResponse? {
        if (prefs.getString(K_CACHED_DATE, null) != today) return null
        val json = prefs.getString(K_CACHED_NEWS, null) ?: return null
        return try { adapter.fromJson(json) } catch (_: Throwable) { null }
    }

    private suspend fun fetchAndCache(
        prefs: android.content.SharedPreferences,
        today: String,
    ): NewsResponse? {
        val news = try {
            ApiClient.api.getLatestNews()
        } catch (_: Exception) {
            return null
        }
        prefs.edit()
            .putString(K_CACHED_NEWS, adapter.toJson(news))
            .putString(K_CACHED_DATE, today)
            .apply()
        return news
    }
}
