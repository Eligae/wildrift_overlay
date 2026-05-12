package com.eligae.wildrift.overlay.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ChampionsCache(context: Context) {

    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(ChampionsResponse::class.java)

    fun save(resp: ChampionsResponse) {
        prefs.edit().putString(KEY, adapter.toJson(resp)).apply()
    }

    fun load(): ChampionsResponse? {
        val json = prefs.getString(KEY, null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    /** 한국명으로 avatar URL 조회. 캐시 미스 또는 매칭 실패 시 null. */
    fun avatarFor(krName: String): String? {
        return load()?.champions?.firstOrNull { it.krName == krName }?.avatar
    }

    companion object {
        private const val NAME = "champions_cache"
        private const val KEY = "champions"
    }
}
