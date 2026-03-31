package com.example.thermometer.di

import android.content.Context
import com.example.thermometer.data.db.AppDatabase
import com.example.thermometer.data.db.dao.SensorDataDao
import com.example.thermometer.data.db.dao.SensorDeviceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideSensorDeviceDao(database: AppDatabase): SensorDeviceDao =
        database.sensorDeviceDao()

    @Provides
    fun provideSensorDataDao(database: AppDatabase): SensorDataDao =
        database.sensorDataDao()
}
