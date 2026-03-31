package com.example.mithermoble.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.mithermoble.data.ble.BleManager
import com.example.mithermoble.data.db.SensorDataDao
import com.example.mithermoble.domain.repository.SensorRepository
import com.example.mithermoble.data.repository.SensorRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(bluetoothManager: BluetoothManager): BluetoothAdapter? {
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun provideBleManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter?
    ): BleManager {
        return BleManager(context, bluetoothAdapter)
    }

    @Provides
    @Singleton
    fun provideSensorRepository(
        bleManager: BleManager,
        sensorDataDao: SensorDataDao
    ): SensorRepository {
        return SensorRepositoryImpl(bleManager, sensorDataDao)
    }
}
