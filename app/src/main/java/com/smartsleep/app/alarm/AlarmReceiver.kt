package com.smartsleep.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Entry point when AlarmManager fires. Kept minimal on purpose: a BroadcastReceiver has only a
 * few seconds to run, so it just starts the appropriate foreground service which does the real
 * work (ringing, or Smart Wake monitoring).
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE_ALARM = "com.smartsleep.app.action.FIRE_ALARM"
        const val ACTION_START_SMART_WAKE_MONITOR = "com.smartsleep.app.action.START_SMART_WAKE_MONITOR"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(AlarmScheduler.EXTRA_SESSION_ID, -1L)
        when (intent.action) {
            ACTION_FIRE_ALARM -> {
                val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_RING
                    putExtra(AlarmScheduler.EXTRA_SESSION_ID, sessionId)
                    putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            ACTION_START_SMART_WAKE_MONITOR -> {
                val serviceIntent = Intent(context, SmartWakeMonitorService::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_SESSION_ID, sessionId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
