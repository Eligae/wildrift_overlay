package com.eligae.wildrift.overlay.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class TierCache(context: Context) {

    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(TierAllResponse::class.java)

    fun save(cohort: String, resp: TierAllResponse) {
        prefs.edit().putString(keyFor(cohort), adapter.toJson(resp)).apply()
    }

    fun load(cohort: String): TierAllResponse? {
        val json = prefs.getString(keyFor(cohort), null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    private fun keyFor(cohort: String) = "tier_$cohort"

    companion object {
        private const val NAME = "tier_cache"
    }
}
