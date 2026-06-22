package com.tidely.data.api.response

import com.google.gson.annotations.SerializedName

/**
 * GeoJSON FeatureCollection response from the ADMIRALTY API
 */
data class StationResponse(
    val type: String,
    val features: List<StationFeature>
)

data class StationFeature(
    val type: String,
    val id: String,
    val geometry: Geometry,
    val properties: StationProperties
)

data class Geometry(
    val type: String,
    val coordinates: List<Double> // [longitude, latitude]
)

data class StationProperties(
    @SerializedName("Id")
    val id: String,
    @SerializedName("Name")
    val name: String,
    @SerializedName("Country")
    val country: String,
    @SerializedName("ContinuousHeightsAvailable")
    val continuousHeightsAvailable: Boolean = false,
    @SerializedName("Footnote")
    val footnote: String? = null
)
