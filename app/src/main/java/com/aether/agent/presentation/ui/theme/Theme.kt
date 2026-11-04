package com.aether.agent.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aether.agent.domain.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4DDD3),
    onPrimaryContainer = Color(0xFF4A2A1C),
    secondary = Color(0xFF8A8178),
    onSecondary = Color.White,
    background = Color(0xFFF9F6F0),
    onBackground = Color(0xFF1F1E1D),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF1F1E1D),
    surfaceVariant = Color(0xFFECE6DA),
    onSurfaceVariant = Color(0xFF5C544B),
    outline = Color(0xFFD9D0C2),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A3129),
    onPrimaryContainer = Color(0xFFF4DDD3),
    secondary = Color(0xFFA9A7B0),
    onSecondary = Color(0xFF1E1E22),
    background = Color(0xFF1E1E22),
    onBackground = Color(0xFFECEAE4),
    surface = Color(0xFF26262C),
    onSurface = Color(0xFFECEAE4),
    surfaceVariant = Color(0xFF2E2E35),
    onSurfaceVariant = Color(0xFFC9C6CF),
    outline = Color(0xFF3A3A42),
    error = Color(0xFFF2B8B5),
)

@Composable
fun AetherTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalAetherPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = AetherTypography) {
            // Surface root memberi warna latar dan warna teks default sesuai tema.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = scheme.background,
                contentColor = scheme.onBackground,
            ) { content() }
        }
    }
}
