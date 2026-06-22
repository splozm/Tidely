package com.tidely.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tidely.data.model.Station
import com.tidely.data.repository.TideRepository
import com.tidely.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StationPickerViewModel(
    private val repository: TideRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _allStations = MutableLiveData<List<Station>>()
    private val _filteredStations = MutableLiveData<List<Station>>()
    val filteredStations: LiveData<List<Station>> = _filteredStations

    private val _selectedStationId = MutableLiveData<String?>()
    val selectedStationId: LiveData<String?> = _selectedStationId

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _selectedStationId.value = preferencesManager.selectedStationId
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stations = repository.getAllStations().first()
                _allStations.value = stations
                _filteredStations.value = stations
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchStations(query: String) {
        val allStations = _allStations.value ?: return

        if (query.isBlank()) {
            _filteredStations.value = allStations
            return
        }

        val filtered = allStations.filter { station ->
            station.name.contains(query, ignoreCase = true) ||
                    station.country.contains(query, ignoreCase = true)
        }
        _filteredStations.value = filtered
    }

    fun selectStation(station: Station) {
        preferencesManager.selectedStationId = station.id
        preferencesManager.selectedStationName = station.name
        _selectedStationId.value = station.id
    }
}

class StationPickerViewModelFactory(
    private val repository: TideRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationPickerViewModel::class.java)) {
            return StationPickerViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
