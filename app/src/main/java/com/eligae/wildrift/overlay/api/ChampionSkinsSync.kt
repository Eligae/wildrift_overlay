package com.eligae.wildrift.overlay.api

import android.content.Context
import android.util.Log

/**
 * 스킨 매핑 manifest 기반 incremental 동기화.
 *   1. /v1/champion-skins/version (~80B) — 글로벌 sha 비교, 같으면 skip
 *   2. 로컬 캐시가 비어 있으면 — manifest로 전체 챔피언 슬롯 채움 (모두 변경 = full sync)
 *   3. 로컬 캐시가 있으면 — manifest와 챔피언별 sha 비교, 변경된 alias만 /c/<alias> 호출
 *   4. 글로벌 sha 저장
 *
 * 데이터 낭비 최소화 — 첫 로드 후엔 변경된 챔피언 수 × ~수백B만 발생.
 */
object ChampionSkinsSync {

    private const val TAG = "ChampionSkinsSync"

    suspend fun syncIfChanged(context: Context) {
        val cache = ChampionSkinsCache(context.applicationContext)
        val version = try {
            ApiClient.api.getChampionSkinsVersion()
        } catch (e: Exception) {
            Log.w(TAG, "version check failed: ${e.message}")
            return
        }
        if (cache.globalSha == version.sha256 && cache.loadPerChamp().isNotEmpty()) {
            Log.d(TAG, "globalSha match — local cache up-to-date")
            return
        }
        val manifest = try {
            ApiClient.api.getChampionSkinsManifest()
        } catch (e: Exception) {
            Log.w(TAG, "manifest fetch failed: ${e.message}")
            return
        }
        val local = cache.loadPerChamp().toMutableMap()
        val changedAliases = manifest.champions.entries.filter { (alias, sha) ->
            local[alias]?.sha256 != sha
        }.map { it.key }
        // 서버에서 사라진 alias는 로컬에서도 제거
        val removed = local.keys.filter { it !in manifest.champions.keys }
        for (alias in removed) local.remove(alias)

        Log.d(TAG, "sync: ${changedAliases.size} changed, ${removed.size} removed (of ${manifest.champions.size} total)")

        var failures = 0
        for (alias in changedAliases) {
            try {
                val entry = ApiClient.api.getChampionSkinEntry(alias)
                local[alias] = entry
            } catch (e: Exception) {
                failures++
                Log.w(TAG, "fetch $alias failed: ${e.message}")
            }
        }
        cache.savePerChamp(local)
        // 일부 실패 시 globalSha를 덮어쓰지 않고 다음 동기화에서 재시도.
        if (failures == 0) cache.globalSha = manifest.sha256
        Log.d(TAG, "sync done. local entries=${local.size}, failures=$failures")
    }
}
