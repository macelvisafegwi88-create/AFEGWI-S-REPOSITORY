package com.example.ui.theme

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
    primary = TelegramBlue,
    onPrimary = TextPrimary,
    primaryContainer = TelegramBubbleOut,
    onPrimaryContainer = TextPrimary,
    secondary = SessionCyan,
    onSecondary = TelegramDarkBg,
    secondaryContainer = TelegramSurfaceVariant,
    onSecondaryContainer = SessionCyan,
    tertiary = WhatsAppGreen,
    onTertiary = TextPrimary,
    background = TelegramDarkBg,
    onBackground = TextPrimary,
    surface = TelegramSurface,
    onSurface = TextPrimary,
    surfaceVariant = TelegramSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TelegramDivider,
    error = AccentRed
)

private val LightColorScheme = DarkColorScheme // SessionGram is fundamentally designed around high-contrast privacy dark UI

@Composable
fun SessionGramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = TelegramDarkBg.toArgb()
                window.navigationBarColor = TelegramDarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
