package com.eligae.wildrift.overlay.api

/**
 * 클라이언트 측 캐시 TTL. 12시간 안이면 서버 호출 생략 → Railway 비용 절감.
 * 사용자가 강제 새로고침하면 우회.
 */
object CachePolicy {
    const val TTL_MS: Long = 12L * 60 * 60 * 1000

    fun isFresh(fetchedAt: Long, now: Long = System.currentTimeMillis()): Boolean {
        return (now - fetchedAt) < TTL_MS
    }
}
