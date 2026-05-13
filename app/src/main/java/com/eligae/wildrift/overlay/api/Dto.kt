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
    val cohort: String,
    val lanes: Map<String, List<NormalizedHero>>,
)

data class TierAllCohortsResponse(
    val fetchedAt: Long,
    val cohorts: Map<String, Map<String, List<NormalizedHero>>>,
)

data class NewsResponse(
    val title: String,
    val url: String,
    val publishedAt: String,
    val fetchedAt: Long,
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

data class ChampionSkinsResponse(
    val fetchedAt: String,
    val sha256: String,
    val skins: Map<String, String>, // skin name → champion krName
)

data class ChampionSkinsVersionResponse(
    val sha256: String,
    val fetchedAt: String,
)

data class ChampionSkinsManifestResponse(
    val fetchedAt: String,
    val sha256: String,
    val champions: Map<String, String>, // alias → champion-level sha256
)

data class ChampionSkinEntry(
    val alias: String,
    val krName: String,
    val skins: List<String>,
    val sha256: String,
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
