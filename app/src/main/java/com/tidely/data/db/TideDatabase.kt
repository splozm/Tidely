package com.tidely.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tidely.data.model.Station
import com.tidely.data.model.TidalEvent

@Database(
    entities = [Station::class, TidalEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TideDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao
    abstract fun tidalEventDao(): TidalEventDao

    companion object {
        @Volatile
        private var INSTANCE: TideDatabase? = null

        fun getInstance(context: Context): TideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TideDatabase::class.java,
                    "tide_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
