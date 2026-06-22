package com.tidely.data.api.response

import com.google.gson.annotations.SerializedName

data class TidalEventsResponse(
    val events: List<TidalEventDto>
)

data class TidalEventDto(
    @SerializedName("EventType")
    val eventType: String, // "HighWater" or "LowWater"
    @SerializedName("DateTime")
    val dateTime: String, // ISO 8601 format
    @SerializedName("Height")
    val height: Double,
    @SerializedName("IsApproximateTime")
    val isApproximateTime: Boolean = false,
    @SerializedName("IsApproximateHeight")
    val isApproximateHeight: Boolean = false
)
