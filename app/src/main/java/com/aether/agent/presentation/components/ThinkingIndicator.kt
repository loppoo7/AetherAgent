package com.aether.agent.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import kotlinx.coroutines.delay

private val DefaultStatuses = listOf("Thinking...", "Reading repository...", "Writing code...")

/**
 * Tiga titik berdenyut bergantian + teks status yang berganti halus.
 * Jika [status] diisi (misal judul aksi yang sedang berjalan), teks itu yang ditampilkan.
 */
@Composable
fun ThinkingIndicator(
    modifier: Modifier = Modifier,
    status: String? = null,
    statuses: List<String> = DefaultStatuses,
) {
    val palette = LocalAetherPalette.current
    val transition = rememberInfiniteTransition(label = "thinking")

    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(status, statuses) {
        if (status == null) {
            while (true) {
                delay(2_200)
                index = (index + 1) % statuses.size
            }
        }
    }
    val text = status ?: statuses[index]

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val scale by transition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, delayMillis = i * 160, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.35f + 0.65f * scale
                    }
                    .background(Terracotta, CircleShape),
            )
            if (i < 2) Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
            label = "status",
        ) { current ->
            Text(current, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
    }
}
