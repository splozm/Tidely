package com.tidely.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tidely.data.model.Station
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {

    @Query("SELECT * FROM stations")
    fun getAllStations(): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    suspend fun getStationById(stationId: String): Station?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<Station>)

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun getStationCount(): Int

    @Query("DELETE FROM stations")
    suspend fun deleteAllStations()
}
