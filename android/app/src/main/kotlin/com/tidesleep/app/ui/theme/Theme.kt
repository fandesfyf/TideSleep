package com.tidesleep.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TideColorScheme = darkColorScheme(
    primary = TidePrimary,
    onPrimary = TideOnBackground,
    secondary = TidePrimaryVariant,
    background = TideBackground,
    onBackground = TideOnBackground,
    surface = TideSurface,
    onSurface = TideOnBackground,
    surfaceVariant = TideSurfaceVariant,
    onSurfaceVariant = TideOnSurfaceMuted,
    error = TideError,
)

@Composable
fun TideSleepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TideColorScheme,
        typography = TideTypography,
        content = content,
    )
}
