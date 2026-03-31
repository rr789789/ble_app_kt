package com.example.thermometer.di

import android.content.Context
import com.example.thermometer.data.repository.BleRepositoryImpl
import com.example.thermometer.data.repository.DataRepositoryImpl
import com.example.thermometer.domain.repository.BleRepository
import com.example.thermometer.domain.repository.DataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindBleRepository(impl: BleRepositoryImpl): BleRepository

    @Binds
    @Singleton
    abstract fun bindDataRepository(impl: DataRepositoryImpl): DataRepository

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(@ApplicationContext context: Context) =
            context.getSharedPreferences("thermometer_prefs", Context.MODE_PRIVATE)
    }
}
