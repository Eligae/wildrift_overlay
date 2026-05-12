package com.eligae.wildrift.overlay.history

import com.eligae.wildrift.overlay.model.MatchResult

/**
 * 한 게임의 결과 + 매핑.
 * id = endedAtMs (epoch ms) — 종료 감지 시점이 유일 식별자.
 * userVerified = true면 dataset 가치 있는 정답으로 간주.
 */
data class MatchRecord(
    val id: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val result: MatchResult,
    val enemies: List<String>,
    val allies: List<String>,
    val userVerified: Boolean = false,
)
