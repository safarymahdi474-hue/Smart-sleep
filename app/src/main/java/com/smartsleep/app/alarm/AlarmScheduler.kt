package com.smartsleep.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Wraps AlarmManager.setAlarmClock(), which is the API Android reserves for genuine alarm-clock
 * apps: it is exempt from Doze/App Standby deferral and does NOT require the user to grant the
 * special "Alarms & reminders" (SCHEDULE_EXACT_ALARM) permission the way setExactAndAllowWhileIdle
 * does on Android 12+. That makes it the right tool here instead of a generic exact alarm.
 *
 * The alarm always fires via [AlarmReceiver], which starts the foreground [AlarmService] to
 * actually ring/vibrate — required because a BroadcastReceiver alone cannot reliably keep audio
 * playing.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val REQUEST_CODE_WAKE_ALARM = 1001
        const val REQUEST_CODE_SMART_WAKE_MONITOR_START = 1002
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    /** Schedules the main wake alarm. Shows [showIntent] as the "alarm clock" info in system UI. */
    fun scheduleWakeAlarm(sessionId: Long, triggerAtMillis: Long, isSnooze: Boolean = false) {
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val firePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WAKE_ALARM,
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_WAKE_ALARM,
            showIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(info, firePendingIntent)
    }

    fun cancelWakeAlarm() {
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WAKE_ALARM,
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /** Schedules the (optional, best-effort) Smart Wake monitor to start a bit before the window. */
    fun scheduleSmartWakeMonitorStart(sessionId: Long, startAtMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_START_SMART_WAKE_MONITOR
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SMART_WAKE_MONITOR_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAtMillis, pendingIntent)
    }

    fun cancelSmartWakeMonitorStart() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_START_SMART_WAKE_MONITOR
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SMART_WAKE_MONITOR_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
