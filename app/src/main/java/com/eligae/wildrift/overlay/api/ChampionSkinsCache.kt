package com.eligae.wildrift.overlay.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * 스킨 매핑 로컬 캐시. 챔피언 단위로 저장 — manifest 비교 후 변경된 항목만 갱신 가능.
 * - perChamp: alias → ChampionSkinEntry (이 자체로 sha 포함)
 * - globalSha: 마지막으로 본 manifest의 글로벌 sha (full sync 판단용)
 *
 * 파서가 쓰는 skin name → krName 평탄화는 [flatten] 으로 동적 계산.
 */
class ChampionSkinsCache(context: Context) {

    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(
        Map::class.java, String::class.java, ChampionSkinEntry::class.java,
    )
    private val mapAdapter = moshi.adapter<Map<String, ChampionSkinEntry>>(mapType)

    fun loadPerChamp(): Map<String, ChampionSkinEntry> {
        val json = prefs.getString(KEY_PER_CHAMP, null) ?: return emptyMap()
        return try { mapAdapter.fromJson(json) ?: emptyMap() } catch (_: Throwable) { emptyMap() }
    }

    fun savePerChamp(entries: Map<String, ChampionSkinEntry>) {
        prefs.edit().putString(KEY_PER_CHAMP, mapAdapter.toJson(entries)).apply()
    }

    var globalSha: String?
        get() = prefs.getString(KEY_GLOBAL_SHA, null)
        set(value) { prefs.edit().putString(KEY_GLOBAL_SHA, value).apply() }

    /** 파서 소비용 평탄화: skin name → krName. */
    fun flatten(): Map<String, String> {
        val out = HashMap<String, String>()
        for (entry in loadPerChamp().values) {
            for (s in entry.skins) out[s] = entry.krName
        }
        return out
    }

    companion object {
        private const val NAME = "champion_skins_cache_v2"
        private const val KEY_PER_CHAMP = "per_champ"
        private const val KEY_GLOBAL_SHA = "global_sha"
    }
}
