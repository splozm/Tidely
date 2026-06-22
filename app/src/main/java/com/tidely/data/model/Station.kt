package com.tidely.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class Station(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val continuousHeightsAvailable: Boolean = false
)
