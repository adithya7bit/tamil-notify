package com.tamilnotify.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = Slate900,
    primaryContainer = Orange600,
    onPrimaryContainer = Slate100,
    secondary = Emerald400,
    onSecondary = Slate900,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    error = Rose500
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = Slate100,
    primaryContainer = Orange400,
    onPrimaryContainer = Slate900,
    secondary = Emerald500,
    onSecondary = Slate100,
    background = Slate100,
    onBackground = Slate900,
    surface = Slate200,
    onSurface = Slate900,
    surfaceVariant = Slate400,
    onSurfaceVariant = Slate900,
    error = Rose500
)

@Composable
fun TamilNotifyTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
