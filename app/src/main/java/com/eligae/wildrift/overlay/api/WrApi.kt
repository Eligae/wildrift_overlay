package com.eligae.wildrift.overlay.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WrApi {
    @GET("v1/lanes")
    suspend fun getLanes(): LanesResponse

    @GET("v1/tier")
    suspend fun getTierAll(): TierAllResponse

    @GET("v1/tier")
    suspend fun getTierLane(@Query("lane") lane: String): TierLaneResponse

    @GET("v1/champions")
    suspend fun getChampions(): ChampionsResponse

    @GET("v1/composition/synergy")
    suspend fun getSynergy(@Query("team") team: String): SynergyResponse

    @GET("v1/composition/counter")
    suspend fun getCounter(@Query("enemy") enemy: String): CounterResponse
}
