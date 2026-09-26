package com.smartsleep.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles the "Snooze" action tapped from the alarm notification (when the full-screen
 * AlarmActivity isn't the one handling it directly).
 */
class SnoozeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
        }
        context.startService(serviceIntent)
    }
}
