if (deviation > MOVEMENT_THRESHOLD) {
            triggerCount++
            if (triggerCount >= REQUIRED_TRIGGER_SAMPLES) {
                triggerWakeNow()
            }
        } else {
            triggerCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun triggerWakeNow() {
        sensorManager?.unregisterListener(this)
        // Fire the real alarm immediately by re-scheduling it a couple seconds out, reusing the
        // exact same path as the normal cycle-based alarm so ringing/notification behavior is
        // identical. markSessionWoken() happens inside AlarmService once it actually rings.
        AlarmScheduler(this).scheduleWakeAlarm(sessionId, System.currentTimeMillis() + 1500)
        stopSelf()
    }

    private fun buildNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, SmartSleepApplication.SMART_WAKE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.settings_smart_wake))
            .setContentText(getString(R.string.smart_wake_monitoring_notification))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }
}
