package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AppThemeMode

private fun getAmoledColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
    onPrimary = Color.Black,
    primaryContainer = accentColor.copy(alpha = 0.2f),
    onPrimaryContainer = accentColor,
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.Black,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = ObsidianDark,
    onSurface = TextPrimary,
    surfaceVariant = GlassCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderTop,
    outlineVariant = GlassBorderBottom
)

@Composable
fun LinguaGlassTheme(
    themeMode: AppThemeMode = AppThemeMode.AMOLED_BLACK,
    accentPalette: AccentPalette = AccentPalette.CYAN,
    content: @Composable () -> Unit
) {
    val accentColor = Color(accentPalette.hex)
    val colorScheme = when (themeMode) {
        AppThemeMode.AMOLED_BLACK -> getAmoledColorScheme(accentColor).copy(
            background = AmoledBlack,
            surface = ObsidianDark
        )
        AppThemeMode.DARK_GLASS -> getAmoledColorScheme(accentColor).copy(
            background = Color(0xFF090B10),
            surface = Color(0xFF0E121C)
        )
        AppThemeMode.OBSIDIAN -> getAmoledColorScheme(accentColor).copy(
            background = Color(0xFF050508),
            surface = Color(0xFF0A0C14)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
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
