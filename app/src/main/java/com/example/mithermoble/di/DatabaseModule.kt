package com.example.mithermoble.di

import android.content.Context
import com.example.mithermoble.data.db.AppDatabase
import com.example.mithermoble.data.db.SensorDataDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideSensorDataDao(database: AppDatabase): SensorDataDao {
        return database.sensorDataDao()
    }
}
