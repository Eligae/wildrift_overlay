package com.eligae.wildrift.overlay.api

data class NormalizedHero(
    val heroId: String,
    val cnName: String,
    val alias: String,
    val krName: String?,
    val avatar: String,
    val winRate: Double,
    val pickRate: Double,
    val banRate: Double,
    val strength: Double,
    val tierLevel: Int,
) {
    val displayName: String get() = krName ?: cnName
}

data class TierLaneResponse(
    val fetchedAt: Long,
    val lane: String,
    val champions: List<NormalizedHero>,
)

data class TierAllResponse(
    val fetchedAt: Long,
    val lanes: Map<String, List<NormalizedHero>>,
)

data class LanesResponse(
    val lanes: List<String>,
)
