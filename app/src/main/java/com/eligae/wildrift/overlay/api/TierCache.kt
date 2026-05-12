package com.eligae.wildrift.overlay.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class TierCache(context: Context) {

    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(TierAllResponse::class.java)

    fun save(resp: TierAllResponse) {
        prefs.edit().putString(KEY_TIER, adapter.toJson(resp)).apply()
    }

    fun load(): TierAllResponse? {
        val json = prefs.getString(KEY_TIER, null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        private const val NAME = "tier_cache"
        private const val KEY_TIER = "tier_all"
    }
}
