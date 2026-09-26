package com.smartsleep.app.alarm

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smartsleep.app.R
import com.smartsleep.app.SmartSleepApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Best-effort, optional "Smart Wake" monitor. Important: this is NOT a sleep-stage classifier.
 * It only watches for a burst of device movement (accelerometer variance) once the user's chosen
 * wake window has opened, and — if it sees one — treats that as a reasonable guess that the user
 * is already stirring, and triggers the alarm right then instead of waiting for the originally
 * calculated cycle-based time. If the device has no usable accelerometer, or nothing is detected,
 * it simply lets the normal cycle-based alarm (already scheduled by AlarmScheduler) fire as
 * planned — it never blocks or delays that fallback.
 */
class SmartWakeMonitorService : Service(), SensorEventListener {

    companion object {
        private const val NOTIFICATION_ID = 43
        private const val MOVEMENT_THRESHOLD = 1.6f // m/s^2 deviation from gravity baseline
        private const val REQUIRED_TRIGGER_SAMPLES = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var triggerCount = 0
    private var sessionId: Long = -1L
    private var windowEndMillis: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sessionId = intent?.getLongExtra(AlarmScheduler.EXTRA_SESSION_ID, -1L) ?: -1L
        if (sessionId < 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val app = application as SmartSleepApplication
        scope.launch {
            val session = app.repository.let {
                // Repository doesn't expose a direct single-session getter publicly beyond flows;
                // reuse activeSessionFlow's first emission which reflects this session while active.
                it.activeSessionFlow
            }
            val active = kotlinx.coroutines.flow.first(session)
            windowEndMillis = active?.wakeWindowEndEpochMillis ?: (System.currentTimeMillis() + 60 * 60_000L)

            if (accelerometer == null) {
                // No usable sensor on this device: honestly fall back, do nothing further and
                // let the already-scheduled cycle-based alarm handle waking the user.
                stopSelf()
                return@launch
            }
            sensorManager?.registerListener(this@SmartWakeMonitorService, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

            // Safety timeout: stop monitoring once we reach the wake window end, since the main
            // alarm will fire there regardless.
            val delayMs = (windowEndMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            kotlinx.coroutines.delay(delayMs)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val (x, y, z) = event.values
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val deviation = kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH)

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
