package com.tidely.data.repository

import com.tidely.BuildConfig
import com.tidely.data.api.AdmiraltyApiService
import com.tidely.data.api.response.TidalEventDto
import com.tidely.data.db.StationDao
import com.tidely.data.db.TidalEventDao
import com.tidely.data.model.Station
import com.tidely.data.model.TidalEvent
import com.tidely.data.model.TideType
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TideRepository(
    private val apiService: AdmiraltyApiService,
    private val stationDao: StationDao,
    private val tidalEventDao: TidalEventDao
) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK).apply {
        timeZone = TimeZone.getTimeZone("Europe/London")
    }

    // Station operations
    fun getAllStations(): Flow<List<Station>> = stationDao.getAllStations()

    suspend fun getStationById(stationId: String): Station? = stationDao.getStationById(stationId)

    suspend fun fetchAndCacheStations(): Result<Unit> = runCatching {
        val response = apiService.getAllStations(BuildConfig.ADMIRALTY_API_KEY)
        val stations = response.features.map { feature ->
            Station(
                id = feature.properties.id,  // ID is in properties, not at feature level
                name = feature.properties.name,
                country = feature.properties.country,
                latitude = feature.geometry.coordinates[1],
                longitude = feature.geometry.coordinates[0],
                continuousHeightsAvailable = feature.properties.continuousHeightsAvailable
            )
        }
        stationDao.insertStations(stations)
    }

    suspend fun getStationCount(): Int = stationDao.getStationCount()

    // Tidal event operations
    fun getTidalEvents(stationId: String): Flow<List<TidalEvent>> {
        return tidalEventDao.getTidalEvents(stationId, Date())
    }

    suspend fun getNextTidalEvents(stationId: String, limit: Int = 4): List<TidalEvent> {
        return tidalEventDao.getNextTidalEvents(stationId, Date(), limit)
    }

    suspend fun fetchAndCacheTidalEvents(stationId: String): Result<Unit> = runCatching {
        val response = apiService.getTidalEvents(
            stationId = stationId,
            durationDays = 7,
            apiKey = BuildConfig.ADMIRALTY_API_KEY
        )

        val events = response.events.map { dto ->
            dto.toTidalEvent(stationId)
        }

        // Delete old events before inserting new ones
        val twoDaysAgo = Date(System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000))
        tidalEventDao.deleteOldEvents(stationId, twoDaysAgo)

        tidalEventDao.insertTidalEvents(events)
    }

    suspend fun getLatestEventDate(stationId: String): Date? {
        return tidalEventDao.getLatestEventDate(stationId)
    }

    suspend fun needsRefresh(stationId: String): Boolean {
        val latestDate = getLatestEventDate(stationId) ?: return true
        val oneDayFromNow = Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))
        return latestDate.before(oneDayFromNow)
    }

    private fun TidalEventDto.toTidalEvent(stationId: String): TidalEvent {
        return TidalEvent(
            stationId = stationId,
            eventType = when (eventType) {
                "HighWater" -> TideType.HIGH
                "LowWater" -> TideType.LOW
                else -> TideType.HIGH
            },
            dateTime = isoDateFormat.parse(dateTime) ?: Date(),
            height = height
        )
    }
}
