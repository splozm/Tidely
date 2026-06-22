package com.tidely.data.api

import com.tidely.data.api.response.StationResponse
import com.tidely.data.api.response.TidalEventsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AdmiraltyApiService {

    @GET("Stations")
    suspend fun getAllStations(
        @Header("Ocp-Apim-Subscription-Key") apiKey: String
    ): StationResponse

    @GET("Stations/{stationId}/TidalEvents")
    suspend fun getTidalEvents(
        @Path("stationId") stationId: String,
        @Query("duration") durationDays: Int = 7,
        @Header("Ocp-Apim-Subscription-Key") apiKey: String
    ): TidalEventsResponse
}
