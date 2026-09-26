package com.smartsleep.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import com.smartsleep.app.data.repository.SleepRepository

class SmartSleepApplication : Application() {

    val repository: SleepRepository by lazy { SleepRepository(this) }

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val SMART_WAKE_CHANNEL_ID = "smart_wake_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.alarm_channel_desc)
            setSound(null, null) // AlarmService plays audio itself via MediaPlayer for full control
            enableVibration(false) // vibration handled manually so it can be toggled at runtime
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val smartWakeChannel = NotificationChannel(
            SMART_WAKE_CHANNEL_ID,
            getString(R.string.smart_wake_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.smart_wake_channel_desc)
        }

        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(smartWakeChannel)
    }
}
