package com.tidely.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "tidal_events",
    primaryKeys = ["stationId", "dateTime"]  // Composite key prevents duplicates
)
data class TidalEvent(
    val stationId: String,
    val eventType: TideType,
    val dateTime: Date,
    val height: Double
)

enum class TideType {
    HIGH,
    LOW
}

enum class TideState {
    RISING,
    FALLING
}
