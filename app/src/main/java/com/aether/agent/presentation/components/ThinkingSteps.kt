package com.aether.agent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import kotlinx.coroutines.delay

/** Satu langkah berpikir. Teksnya diambil dari kejadian nyata (status model atau nama tool), bukan dibuat AI. */
data class ThinkingStep(
    val id: String,
    val label: String,
    val detail: String? = null,
    val done: Boolean = false,
)

private val SuccessGreen = Color(0xFF16A34A)

/** Daftar langkah seperti panel Summary: hijau selesai, terracotta sedang berjalan, dengan waktu berjalan. */
@Composable
fun ThinkingSteps(
    steps: List<ThinkingStep>,
    running: Boolean,
    modifier: Modifier = Modifier,
    onStop: (() -> Unit)? = null,
) {
    val palette = LocalAetherPalette.current
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        elapsed = 0
        val start = System.currentTimeMillis()
        while (true) {
            elapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
            delay(500)
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.cardBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (running) "Berpikir" else "Selesai", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Text("$elapsed dtk", style = MaterialTheme.typography.labelSmall, color = palette.muted)
            Spacer(Modifier.weight(1f))
            if (onStop != null && running) {
                TextButton(onClick = onStop) { Text("Stop") }
            }
        }

        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            steps.forEachIndexed { i, step ->
                val isLast = i == steps.lastIndex
                val dotColor = when {
                    step.done -> SuccessGreen
                    running && isLast -> Terracotta
                    else -> palette.muted
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(
                        modifier = Modifier.width(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                        if (!isLast) Box(Modifier.width(1.dp).height(18.dp).background(palette.border))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Text(
                            step.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (step.done) palette.muted else MaterialTheme.colorScheme.onSurface,
                        )
                        step.detail?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                        }
                    }
                }
            }
        }
    }
}
