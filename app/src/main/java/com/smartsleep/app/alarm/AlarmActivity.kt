package com.smartsleep.app.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartsleep.app.R
import com.smartsleep.app.ui.rating.RatingRoute
import com.smartsleep.app.ui.theme.SmartSleepTheme

class AlarmActivity : ComponentActivity() {

    private var sessionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowOnLockScreenFlags()
        sessionId = intent.getLongExtra(AlarmScheduler.EXTRA_SESSION_ID, -1L)

        // Block plain back-button dismissal: an alarm shouldn't be silently swiped away.
        onBackPressedDispatcher.addCallback(this) { /* no-op */ }

        setContent {
            var showingRating by remember { mutableStateOf(false) }

            SmartSleepTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showingRating) {
                        RatingRoute(
                            sessionId = sessionId,
                            onDone = { finish() }
                        )
                    } else {
                        AlarmScreen(
                            onSnooze = {
                                startService(
                                    Intent(this@AlarmActivity, AlarmService::class.java)
                                        .apply { action = AlarmService.ACTION_SNOOZE }
                                )
                                finish()
                            },
                            onDismiss = {
                                startService(
                                    Intent(this@AlarmActivity, AlarmService::class.java)
                                        .apply { action = AlarmService.ACTION_DISMISS }
                                )
                                showingRating = true
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setShowOnLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
private fun AlarmScreen(onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Alarm,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.alarm_wake_up),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.alarm_dismiss))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.alarm_snooze))
        }
    }
}
