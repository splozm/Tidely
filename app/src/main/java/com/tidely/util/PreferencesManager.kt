package com.tidely.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var selectedStationId: String?
        get() = prefs.getString(KEY_STATION_ID, null)
        set(value) = prefs.edit().putString(KEY_STATION_ID, value).apply()

    var selectedStationName: String?
        get() = prefs.getString(KEY_STATION_NAME, null)
        set(value) = prefs.edit().putString(KEY_STATION_NAME, value).apply()

    var useGps: Boolean
        get() = prefs.getBoolean(KEY_USE_GPS, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_GPS, value).apply()

    var lastLatitude: Double
        get() = prefs.getFloat(KEY_LAST_LAT, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_LAT, value.toFloat()).apply()

    var lastLongitude: Double
        get() = prefs.getFloat(KEY_LAST_LON, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_LON, value.toFloat()).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    fun hasSelectedStation(): Boolean = !selectedStationId.isNullOrEmpty()

    fun clearStation() {
        prefs.edit()
            .remove(KEY_STATION_ID)
            .remove(KEY_STATION_NAME)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "tidely_prefs"
        private const val KEY_STATION_ID = "station_id"
        private const val KEY_STATION_NAME = "station_name"
        private const val KEY_USE_GPS = "use_gps"
        private const val KEY_LAST_LAT = "last_latitude"
        private const val KEY_LAST_LON = "last_longitude"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
