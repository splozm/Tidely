package com.tidely

import android.app.Application
import com.tidely.data.api.RetrofitClient
import com.tidely.data.db.TideDatabase
import com.tidely.data.repository.TideRepository

class TidelyApplication : Application() {

    lateinit var tideRepository: TideRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        val database = TideDatabase.getInstance(this)

        // Initialize repository
        tideRepository = TideRepository(
            apiService = RetrofitClient.apiService,
            stationDao = database.stationDao(),
            tidalEventDao = database.tidalEventDao()
        )
    }
}
