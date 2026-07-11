package com.smartlighting.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SmartLightingColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = Cyan,
    tertiary = Green,
    background = DarkBlue,
    surface = Surface,
    onPrimary = White,
    onSecondary = DarkBlue,
    onTertiary = White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Red,
)

@Composable
fun SmartLightingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartLightingColorScheme,
        typography = AppTypography,
        content = content
    )
}
