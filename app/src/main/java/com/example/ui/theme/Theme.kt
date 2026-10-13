package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = TechDark,
    primaryContainer = JarvisSurfaceElevated,
    onPrimaryContainer = ArcCyan,
    secondary = ArcBlue,
    onSecondary = TechDark,
    secondaryContainer = JarvisSurface,
    onSecondaryContainer = TechWhite,
    tertiary = HologramTeal,
    onTertiary = TechDark,
    background = JarvisBackground,
    onBackground = TechWhite,
    surface = JarvisSurface,
    onSurface = TechWhite,
    surfaceVariant = JarvisSurfaceElevated,
    onSurfaceVariant = TechMuted,
    outline = JarvisCardBorder,
    error = AlertRed,
    onError = TechWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // JARVIS is permanently high-tech dark HUD
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
