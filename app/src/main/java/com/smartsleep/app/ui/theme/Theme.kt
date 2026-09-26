package com.smartsleep.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NightColorScheme = darkColorScheme(
    primary = MoonBlue,
    secondary = SoftLavender,
    tertiary = WarnAmber,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onBackground = OnNight,
    onSurface = OnNight,
    onSurfaceVariant = OnNightMuted,
    onPrimary = Color(0xFF0B1220)
)

private val DayColorScheme = lightColorScheme(
    primary = DayPrimary,
    background = DayBackground,
    surface = DaySurface,
    onBackground = OnDay,
    onSurface = OnDay
)

@Composable
fun SmartSleepTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) NightColorScheme else DayColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartSleepTypography,
        content = content
    )
}
