package com.aether.agent.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LogoOrange = Color(0xFFED8D56)

/**
 * Logo AetherAgent tanpa latar: huruf A dari garis dengan tiga titik di ujung dan titik oranye di palang.
 * Warna garis mengikuti tema (gelap di mode terang, terang di mode gelap).
 * Koordinat mengikuti gambar logo asli, diskalakan ke kotak 700 satuan.
 */
@Composable
fun AetherMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    ink: Color = MaterialTheme.colorScheme.onBackground,
    accent: Color = LogoOrange,
    glow: Boolean = true,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 700f

        fun p(x: Float, y: Float) = Offset((x - 628f + 350f) * unit, (y - 598f + 350f) * unit)

        val top = p(628f, 371f)
        val left = p(357f, 826f)
        val right = p(899f, 826f)
        val dot = p(630f, 635f)

        drawLine(color = ink, start = top, end = left, strokeWidth = 70f * unit, cap = StrokeCap.Round)
        drawLine(color = ink, start = top, end = right, strokeWidth = 70f * unit, cap = StrokeCap.Round)
        drawLine(color = ink, start = p(471f, 635f), end = p(785f, 635f), strokeWidth = 56f * unit, cap = StrokeCap.Round)
        drawCircle(color = ink, radius = 65f * unit, center = top)
        drawCircle(color = ink, radius = 65f * unit, center = left)
        drawCircle(color = ink, radius = 65f * unit, center = right)

        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.5f), Color.Transparent),
                    center = dot,
                    radius = 150f * unit,
                ),
                radius = 150f * unit,
                center = dot,
            )
        }
        drawCircle(color = accent, radius = 61f * unit, center = dot)
    }
}
