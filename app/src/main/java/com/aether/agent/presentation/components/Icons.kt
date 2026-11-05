package com.aether.agent.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ikon digambar dengan Canvas, bukan karakter Unicode, supaya tidak bergantung pada glyph font
 * (karakter seperti ☰ ⋯ ⚙ ↑ sering tampil sebagai kotak atau emoji di font tertentu).
 */
enum class AetherIcon {
    Menu, More, Plus, ArrowUp, Stop, Check, Close, Minus,
    ChevronDown, ChevronRight, Back, Copy, Settings, Sparkle, Eye, EyeOff,
}

@Composable
fun AIcon(
    icon: AetherIcon,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 22.dp,
    strokeWidth: Dp = 1.8.dp,
) {
    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        drawAetherIcon(icon, tint, scale, strokeWidth.toPx())
    }
}

private fun DrawScope.drawAetherIcon(icon: AetherIcon, tint: Color, s: Float, sw: Float) {
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun p(x: Float, y: Float) = Offset(x * s, y * s)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        drawLine(color = tint, start = p(x1, y1), end = p(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
    }

    fun poly(vararg pts: Float) {
        val path = Path()
        for (i in pts.indices step 2) {
            if (i == 0) path.moveTo(pts[i] * s, pts[i + 1] * s) else path.lineTo(pts[i] * s, pts[i + 1] * s)
        }
        drawPath(path = path, color = tint, style = stroke)
    }

    when (icon) {
        AetherIcon.Menu -> {
            line(4f, 8f, 20f, 8f)
            line(4f, 16f, 13f, 16f)
        }
        AetherIcon.More -> {
            drawCircle(color = tint, radius = 1.7f * s, center = p(5f, 12f))
            drawCircle(color = tint, radius = 1.7f * s, center = p(12f, 12f))
            drawCircle(color = tint, radius = 1.7f * s, center = p(19f, 12f))
        }
        AetherIcon.Plus -> {
            line(12f, 5f, 12f, 19f)
            line(5f, 12f, 19f, 12f)
        }
        AetherIcon.ArrowUp -> {
            line(12f, 19f, 12f, 5f)
            poly(6f, 11f, 12f, 5f, 18f, 11f)
        }
        AetherIcon.Stop -> {
            drawRoundRect(
                color = tint,
                topLeft = p(6f, 6f),
                size = Size(12f * s, 12f * s),
                cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
            )
        }
        AetherIcon.Check -> poly(5f, 12.5f, 10f, 17.5f, 19f, 7f)
        AetherIcon.Close -> {
            line(6f, 6f, 18f, 18f)
            line(18f, 6f, 6f, 18f)
        }
        AetherIcon.Minus -> line(6f, 12f, 18f, 12f)
        AetherIcon.ChevronDown -> poly(6f, 9f, 12f, 15f, 18f, 9f)
        AetherIcon.ChevronRight -> poly(9f, 6f, 15f, 12f, 9f, 18f)
        AetherIcon.Back -> {
            line(19f, 12f, 5f, 12f)
            poly(11f, 6f, 5f, 12f, 11f, 18f)
        }
        AetherIcon.Copy -> {
            poly(9f, 15f, 4f, 15f, 4f, 4f, 15f, 4f, 15f, 9f)
            drawRoundRect(
                color = tint,
                topLeft = p(9f, 9f),
                size = Size(11f * s, 11f * s),
                cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
                style = stroke,
            )
        }
        AetherIcon.Settings -> {
            line(4f, 7f, 20f, 7f)
            line(4f, 12f, 20f, 12f)
            line(4f, 17f, 20f, 17f)
            drawCircle(color = tint, radius = 2.4f * s, center = p(15f, 7f))
            drawCircle(color = tint, radius = 2.4f * s, center = p(8f, 12f))
            drawCircle(color = tint, radius = 2.4f * s, center = p(14f, 17f))
        }
        AetherIcon.Eye, AetherIcon.EyeOff -> {
            val path = Path()
            path.moveTo(2f * s, 12f * s)
            path.cubicTo(7f * s, 5f * s, 17f * s, 5f * s, 22f * s, 12f * s)
            path.cubicTo(17f * s, 19f * s, 7f * s, 19f * s, 2f * s, 12f * s)
            drawPath(path = path, color = tint, style = stroke)
            drawCircle(color = tint, radius = 3f * s, center = p(12f, 12f), style = stroke)
            if (icon == AetherIcon.EyeOff) line(4f, 20f, 20f, 4f)
        }
        AetherIcon.Sparkle -> {
            // Bintang empat sudut milik AetherAgent (bukan logo Anthropic).
            val path = Path()
            for (i in 0 until 8) {
                val angle = Math.toRadians((-90 + i * 45).toDouble())
                val r = if (i % 2 == 0) 10f else 4.2f
                val x = (12f + r * cos(angle).toFloat()) * s
                val y = (12f + r * sin(angle).toFloat()) * s
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = tint)
        }
    }
}

/** Tombol ikon bundar dengan area sentuh 44dp. */
@Composable
fun IconAction(
    icon: AetherIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier
            .size(boxSize)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AIcon(icon, tint = if (enabled) tint else tint.copy(alpha = 0.4f), size = iconSize)
    }
}

/** Titik status kecil (hijau = terhubung). */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(color))
}
