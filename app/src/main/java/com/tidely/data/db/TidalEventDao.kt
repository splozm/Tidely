package com.tidely.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tidely.data.model.TidalEvent
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TidalEventDao {

    @Query("SELECT * FROM tidal_events WHERE stationId = :stationId AND dateTime >= :fromDate ORDER BY dateTime ASC")
    fun getTidalEvents(stationId: String, fromDate: Date): Flow<List<TidalEvent>>

    @Query("SELECT * FROM tidal_events WHERE stationId = :stationId AND dateTime >= :fromDate ORDER BY dateTime ASC LIMIT :limit")
    suspend fun getNextTidalEvents(stationId: String, fromDate: Date, limit: Int): List<TidalEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTidalEvents(events: List<TidalEvent>)

    @Query("DELETE FROM tidal_events WHERE stationId = :stationId AND dateTime < :beforeDate")
    suspend fun deleteOldEvents(stationId: String, beforeDate: Date)

    @Query("DELETE FROM tidal_events WHERE stationId = :stationId")
    suspend fun deleteEventsForStation(stationId: String)

    @Query("SELECT MAX(dateTime) FROM tidal_events WHERE stationId = :stationId")
    suspend fun getLatestEventDate(stationId: String): Date?
}
