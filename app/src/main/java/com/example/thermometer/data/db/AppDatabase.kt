package com.example.thermometer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.thermometer.data.db.dao.SensorDataDao
import com.example.thermometer.data.db.dao.SensorDeviceDao
import com.example.thermometer.data.db.entity.SensorDataEntity
import com.example.thermometer.data.db.entity.SensorDeviceEntity

@Database(
    entities = [
        SensorDeviceEntity::class,
        SensorDataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sensorDeviceDao(): SensorDeviceDao
    abstract fun sensorDataDao(): SensorDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thermometer_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
