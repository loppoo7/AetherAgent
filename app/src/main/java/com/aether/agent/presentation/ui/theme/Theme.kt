package com.aether.agent.presentation.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aether.agent.domain.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4DDD3),
    onPrimaryContainer = Color(0xFF4A2A1C),
    secondary = Color(0xFF71717A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4DDD3),
    onSecondaryContainer = Color(0xFF4A2A1C),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1F1D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1D1B),
    surfaceVariant = Color(0xFFF0EDE6),
    onSurfaceVariant = Color(0xFF52525B),
    surfaceTint = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFAF7),
    surfaceContainer = Color(0xFFF7F4EF),
    surfaceContainerHigh = Color(0xFFF0EDE6),
    surfaceContainerHighest = Color(0xFFEAE6DE),
    outline = Color(0xFFD4D4D8),
    outlineVariant = Color(0xFFE4E4E7),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A3129),
    onPrimaryContainer = Color(0xFFF4DDD3),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF18181B),
    secondaryContainer = Color(0xFF4A3129),
    onSecondaryContainer = Color(0xFFF4DDD3),
    background = Color(0xFF18181B),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF27272A),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF303034),
    onSurfaceVariant = Color(0xFFD4D4D8),
    surfaceTint = Color(0xFF27272A),
    surfaceContainerLowest = Color(0xFF141416),
    surfaceContainerLow = Color(0xFF1F1F22),
    surfaceContainer = Color(0xFF27272A),
    surfaceContainerHigh = Color(0xFF2F2F33),
    surfaceContainerHighest = Color(0xFF38383D),
    outline = Color(0xFF52525B),
    outlineVariant = Color(0xFF3F3F46),
    error = Color(0xFFF2B8B5),
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AetherTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val palette = if (dark) DarkPalette else LightPalette

    // Ikon status bar (jam, sinyal, baterai) mengikuti tema aplikasi, bukan tema sistem.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
            if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        }
    }

    CompositionLocalProvider(LocalAetherPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = AetherTypography) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = scheme.background,
                contentColor = scheme.onBackground,
            ) { content() }
        }
    }
}
