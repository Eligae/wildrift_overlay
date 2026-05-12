package com.eligae.wildrift.overlay.history

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * 전적 영구 저장 — SharedPreferences JSON. PoC 단순화로 한 줄 List 직렬화.
 * 30판 LRU. v2에 Room 또는 외부 export.
 */
class MatchHistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("match_history", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, MatchRecord::class.java)
    private val adapter = moshi.adapter<List<MatchRecord>>(listType)

    fun add(record: MatchRecord) {
        val merged = (loadAll().filterNot { it.id == record.id } + record)
            .sortedByDescending { it.endedAtMs }
            .take(MAX_RECORDS)
        save(merged)
    }

    fun update(id: Long, mutate: (MatchRecord) -> MatchRecord) {
        save(loadAll().map { if (it.id == id) mutate(it) else it })
    }

    fun loadAll(): List<MatchRecord> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun find(id: Long): MatchRecord? = loadAll().firstOrNull { it.id == id }

    fun delete(id: Long) {
        save(loadAll().filterNot { it.id == id })
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun save(list: List<MatchRecord>) {
        prefs.edit().putString(KEY, adapter.toJson(list)).apply()
    }

    companion object {
        private const val KEY = "matches"
        private const val MAX_RECORDS = 30
    }
}
