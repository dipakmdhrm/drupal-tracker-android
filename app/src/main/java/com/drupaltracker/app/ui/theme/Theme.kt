package com.drupaltracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DrupalBlue = Color(0xFF0678BE)
private val DrupalDarkBlue = Color(0xFF004C80)
private val DrupalLightBlue = Color(0xFF5AADEB)

private val LightColorScheme = lightColorScheme(
    primary = DrupalBlue,
    onPrimary = Color.White,
    primaryContainer = DrupalLightBlue,
    onPrimaryContainer = DrupalDarkBlue,
    secondary = DrupalDarkBlue,
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DrupalLightBlue,
    onPrimary = DrupalDarkBlue,
    primaryContainer = DrupalDarkBlue,
    onPrimaryContainer = DrupalLightBlue,
    secondary = DrupalBlue,
    onSecondary = Color.White
)

@Composable
fun DrupalTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
