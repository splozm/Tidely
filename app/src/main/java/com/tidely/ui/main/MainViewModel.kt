package com.tidely.ui.main

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidely.data.model.Station
import com.tidely.data.model.TidalEvent
import com.tidely.data.model.TideState
import com.tidely.data.model.TideType
import com.tidely.data.repository.TideRepository
import com.tidely.util.LocationHelper
import com.tidely.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(
    private val repository: TideRepository,
    private val preferencesManager: PreferencesManager,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _selectedStation = MutableLiveData<Station?>()
    val selectedStation: LiveData<Station?> = _selectedStation

    private val _tidalEvents = MutableLiveData<List<TidalEvent>>()
    val tidalEvents: LiveData<List<TidalEvent>> = _tidalEvents

    private val _tideState = MutableLiveData<TideState>()
    val tideState: LiveData<TideState> = _tideState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadCurrentStation()
    }

    fun loadCurrentStation() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Check if stations are cached
                val stationCount = repository.getStationCount()
                if (stationCount == 0) {
                    // First launch - fetch stations
                    val result = repository.fetchAndCacheStations()
                    if (result.isFailure) {
                        _error.value = "Failed to fetch stations: ${result.exceptionOrNull()?.message}"
                        _isLoading.value = false
                        return@launch
                    }
                }

                // Load selected station or find nearest
                val stationId = preferencesManager.selectedStationId
                val station = if (stationId != null) {
                    repository.getStationById(stationId)
                } else if (preferencesManager.useGps) {
                    findNearestStation()
                } else {
                    null
                }

                if (station != null) {
                    setStation(station)
                    loadTidalEvents(station.id)
                } else {
                    _error.value = "No station selected. Please select a station from the Stations tab."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load station"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun findNearestStation(): Station? {
        val location = locationHelper.getCurrentLocation()
            ?: locationHelper.getLastKnownLocation()
            ?: return null

        val stations = repository.getAllStations().first()
        return locationHelper.findNearestStation(location, stations)
    }

    fun setStation(station: Station) {
        _selectedStation.value = station
        preferencesManager.selectedStationId = station.id
        preferencesManager.selectedStationName = station.name

        viewModelScope.launch {
            loadTidalEvents(station.id)
        }
    }

    private suspend fun loadTidalEvents(stationId: String) {
        _isLoading.value = true
        _error.value = null

        try {
            // Check if we need to refresh data
            if (repository.needsRefresh(stationId)) {
                repository.fetchAndCacheTidalEvents(stationId)
            }

            // Load events from cache
            val events = repository.getNextTidalEvents(stationId, limit = 10)
            _tidalEvents.value = events

            // Calculate current tide state
            updateTideState(events)
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to load tide data"
        } finally {
            _isLoading.value = false
        }
    }

    private fun updateTideState(events: List<TidalEvent>) {
        if (events.size < 2) return

        val now = Date()
        val nextEvent = events.firstOrNull { it.dateTime.after(now) } ?: return
        val previousEvent = events.lastOrNull { it.dateTime.before(now) }

        if (previousEvent == null) {
            _tideState.value = if (nextEvent.eventType == TideType.HIGH) {
                TideState.RISING
            } else {
                TideState.FALLING
            }
            return
        }

        _tideState.value = when {
            previousEvent.eventType == TideType.LOW && nextEvent.eventType == TideType.HIGH -> TideState.RISING
            previousEvent.eventType == TideType.HIGH && nextEvent.eventType == TideType.LOW -> TideState.FALLING
            else -> TideState.RISING
        }
    }

    fun refreshData() {
        val stationId = _selectedStation.value?.id ?: return
        viewModelScope.launch {
            loadTidalEvents(stationId)
        }
    }

    fun requestLocationAndFindStation(location: Location) {
        viewModelScope.launch {
            try {
                val stations = repository.getAllStations().first()
                val nearest = locationHelper.findNearestStation(location, stations)
                if (nearest != null) {
                    setStation(nearest)
                }
            } catch (e: Exception) {
                _error.value = "Failed to find nearest station"
            }
        }
    }
}
