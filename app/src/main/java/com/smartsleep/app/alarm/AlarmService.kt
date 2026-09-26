package com.smartsleep.app.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.smartsleep.app.R
import com.smartsleep.app.SmartSleepApplication
import com.smartsleep.app.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that actually rings the alarm. A BroadcastReceiver can't reliably hold a
 * MediaPlayer/Vibrator alive, so [AlarmReceiver] hands off to this service as soon as the alarm
 * fires.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_RING = "com.smartsleep.app.action.RING"
        const val ACTION_SNOOZE = "com.smartsleep.app.action.SNOOZE"
        const val ACTION_DISMISS = "com.smartsleep.app.action.DISMISS"
        const val NOTIFICATION_ID = 42
        var currentSessionId: Long = -1L
            private set
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                currentSessionId = intent.getLongExtra(AlarmScheduler.EXTRA_SESSION_ID, -1L)
                startRinging()
            }
            ACTION_SNOOZE -> handleSnooze()
            ACTION_DISMISS -> handleDismiss()
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun startRinging() {
        val app = application as SmartSleepApplication
        scope.launch {
            val settings = app.repository.settingsFlow.first()
            app.repository.markSessionWoken(currentSessionId)

            startForeground(NOTIFICATION_ID, buildNotification(settings))
            playSound(settings)
            if (settings.vibrationEnabled) startVibration()

            // Also try to bring up the full-screen alarm UI directly (in addition to the
            // notification's full-screen intent, which some launchers/OEMs suppress).
            val activityIntent = Intent(this@AlarmService, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmScheduler.EXTRA_SESSION_ID, currentSessionId)
            }
            startActivity(activityIntent)
        }
    }

    private fun buildNotification(settings: AppSettings): android.app.Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_SESSION_ID, currentSessionId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS }
        val dismissPendingIntent = PendingIntent.getService(
            this, 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, SmartSleepApplication.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(getString(R.string.alarm_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.alarm_dismiss), dismissPendingIntent)

        if (settings.snoozeEnabled) {
            val snoozeIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_SNOOZE }
            val snoozePendingIntent = PendingIntent.getService(
                this, 2, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, getString(R.string.alarm_snooze), snoozePendingIntent)
        }
        return builder.build()
    }

    private fun playSound(settings: AppSettings) {
        try {
            val uri: Uri = settings.alarmSoundUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getValidRingtoneUri(this)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val target = (maxVol * (settings.volumePercent / 100f)).toInt().coerceIn(1, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
                start()
            }
        } catch (e: Exception) {
            // If sound playback fails for any reason we still rely on vibration + full-screen UI.
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun handleSnooze() {
        val app = application as SmartSleepApplication
        val sessionId = currentSessionId
        stopRingingResources()
        scope.launch {
            val settings = app.repository.settingsFlow.first()
            val triggerAt = System.currentTimeMillis() + settings.snoozeDurationMinutes * 60_000L
            AlarmScheduler(this@AlarmService).scheduleWakeAlarm(sessionId, triggerAt, isSnooze = true)
        }
        stopForegroundCompat()
        stopSelf()
    }

    private fun handleDismiss() {
        stopRingingResources()
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopRingingResources() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopRingingResources()
        super.onDestroy()
    }
}
