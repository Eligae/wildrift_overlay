package com.eligae.wildrift.overlay.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WrApi {
    @GET("v1/lanes")
    suspend fun getLanes(): LanesResponse

    @GET("v1/tier")
    suspend fun getTierAll(@Query("cohort") cohort: String): TierAllResponse

    @GET("v1/tier")
    suspend fun getTierLane(
        @Query("lane") lane: String,
        @Query("cohort") cohort: String,
    ): TierLaneResponse

    @GET("v1/tier/all")
    suspend fun getTierAllCohorts(): TierAllCohortsResponse

    @GET("v1/news/latest")
    suspend fun getLatestNews(): NewsResponse

    @GET("v1/champions")
    suspend fun getChampions(): ChampionsResponse

    @GET("v1/champion-skins")
    suspend fun getChampionSkins(): ChampionSkinsResponse

    @GET("v1/champion-skins/version")
    suspend fun getChampionSkinsVersion(): ChampionSkinsVersionResponse

    @GET("v1/champion-skins/manifest")
    suspend fun getChampionSkinsManifest(): ChampionSkinsManifestResponse

    @GET("v1/champion-skins/c/{alias}")
    suspend fun getChampionSkinEntry(@retrofit2.http.Path("alias") alias: String): ChampionSkinEntry

    @GET("v1/composition/synergy")
    suspend fun getSynergy(
        @Query("team") team: String,
        @Query("lane") lane: String? = null,
    ): SynergyResponse

    @GET("v1/composition/counter")
    suspend fun getCounter(@Query("enemy") enemy: String): CounterResponse
}
