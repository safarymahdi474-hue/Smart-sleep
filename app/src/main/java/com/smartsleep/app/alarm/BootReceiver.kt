package com.smartsleep.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsleep.app.SmartSleepApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms the alarm after: device reboot, the app being updated (MY_PACKAGE_REPLACED), or the
 * system clock / timezone changing under an in-progress sleep session (both are explicitly
 * called out in the spec as cases to handle correctly).
 *
 * Uses goAsync() because BroadcastReceiver.onReceive must return quickly, but rescheduling needs
 * a coroutine hop to read Room/DataStore.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as SmartSleepApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.rescheduleActiveSessionIfNeeded()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
