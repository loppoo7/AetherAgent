package com.aether.agent.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Terracotta = Color(0xFFD97757)

/** Warna tambahan yang tidak ada di ColorScheme Material. */
@Immutable
data class AetherPalette(
    val surface: Color,
    val userBubble: Color,
    val border: Color,
    val muted: Color,
    val codeBg: Color,
    val codeHeader: Color,
    val codeText: Color,
    val cardBg: Color,
)

val LightPalette = AetherPalette(
    surface = Color(0xFFFFFDF9),
    userBubble = Color(0xFFECE6DA),
    border = Color(0xFFE2DACB),
    muted = Color(0xFF8A8178),
    codeBg = Color(0xFF1E1E22),
    codeHeader = Color(0xFF2A2A30),
    codeText = Color(0xFFE4E2EA),
    cardBg = Color(0xFFFFFDF9),
)

val DarkPalette = AetherPalette(
    surface = Color(0xFF26262C),
    userBubble = Color(0xFF2E2E35),
    border = Color(0xFF3A3A42),
    muted = Color(0xFFA9A7B0),
    codeBg = Color(0xFF16161A),
    codeHeader = Color(0xFF24242B),
    codeText = Color(0xFFE4E2EA),
    cardBg = Color(0xFF26262C),
)

val LocalAetherPalette = staticCompositionLocalOf { LightPalette }
