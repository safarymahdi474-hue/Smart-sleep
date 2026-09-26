package com.smartsleep.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smartsleep.app.ui.navigation.SmartSleepNavGraph
import com.smartsleep.app.ui.theme.SmartSleepTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result not critical to app function */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val app = application as SmartSleepApplication

        setContent {
            var isDark by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                app.repository.settingsFlow.collect { settings ->
                    isDark = settings.themeMode != com.smartsleep.app.domain.model.ThemeMode.LIGHT
                }
            }

            SmartSleepTheme(darkTheme = isDark) {
                SmartSleepNavGraph(repository = app.repository)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
