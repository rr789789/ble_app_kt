package com.example.mithermoble.data.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mithermoble.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BLE前台服务
 * 保持BLE连接在后台持续运行
 */
@AndroidEntryPoint
class BleService : Service() {

    companion object {
        private const val CHANNEL_ID = "ble_service_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_CONNECT = "com.example.mithermoble.CONNECT"
        const val ACTION_DISCONNECT = "com.example.mithermoble.DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    @Inject
    lateinit var bleManager: BleManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID, createNotification("连接设备中..."))
            }
            ACTION_DISCONNECT -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持蓝牙连接"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("小米温湿度")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
