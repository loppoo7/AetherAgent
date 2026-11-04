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
    surface = Color(0xFFFFFFFF),
    userBubble = Color(0xFFF0EDE6),
    border = Color(0xFFE4E4E7),
    muted = Color(0xFF71717A),
    codeBg = Color(0xFF1E1E22),
    codeHeader = Color(0xFF2A2A30),
    codeText = Color(0xFFE4E2EA),
    cardBg = Color(0xFFFFFFFF),
)

val DarkPalette = AetherPalette(
    surface = Color(0xFF27272A),
    userBubble = Color(0xFF303034),
    border = Color(0xFF3F3F46),
    muted = Color(0xFFA1A1AA),
    codeBg = Color(0xFF101012),
    codeHeader = Color(0xFF202024),
    codeText = Color(0xFFE4E4E7),
    cardBg = Color(0xFF27272A),
)

val LocalAetherPalette = staticCompositionLocalOf { LightPalette }
