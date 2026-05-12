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

data class ChampionEntry(
    val heroId: String,
    val krName: String,
    val avatar: String?,
)

data class ChampionsResponse(
    val champions: List<ChampionEntry>,
    val fetchedAt: Long? = null,
)

data class SynergySuggestion(
    val heroId: String,
    val krName: String?,
    val avatar: String?,
    val reasons: List<String>,
)

data class SynergyResponse(
    val team: List<String>,
    val suggestions: List<SynergySuggestion>,
)

data class CounterRef(
    val heroId: String,
    val krName: String?,
    val avatar: String?,
)

data class CounterResponse(
    val enemyHeroId: String,
    val enemyKrName: String?,
    val enemyAvatar: String?,
    val counters: List<CounterRef>,
    val note: String?,
)
