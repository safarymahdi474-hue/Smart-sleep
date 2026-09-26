package com.smartsleep.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatClock(epochMillis: Long, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return SimpleDateFormat(pattern, locale).format(Date(epochMillis))
    }

    fun formatDate(epochMillis: Long, locale: Locale = Locale.getDefault()): String {
        return SimpleDateFormat("yyyy/MM/dd", locale).format(Date(epochMillis))
    }

    /** e.g. "3h 12m" remaining, clamped to zero. */
    fun formatRemaining(targetEpochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = (targetEpochMillis - nowMillis).coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun formatDurationMinutes(startMillis: Long, endMillis: Long): String {
        val diff = (endMillis - startMillis).coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return "${hours}h ${minutes}m"
    }
}
