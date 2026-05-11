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
}
