package com.aether.agent.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.AgentCard
import com.aether.agent.presentation.viewmodel.CardState

/** Kartu aksi GitHub: transparan dengan garis tipis, bisa dibuka untuk melihat detail. */
@Composable
fun AgentActionCard(card: AgentCard) {
    val palette = LocalAetherPalette.current
    var expanded by remember { mutableStateOf(false) }
    val hasDetail = card.state != CardState.RUNNING && card.output.isNotBlank()

    Surface(
        color = palette.cardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StateIcon(card.state)
                Text(
                    card.title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (hasDetail) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Tutup" else "Detail", color = palette.muted)
                    }
                }
            }
            AnimatedVisibility(visible = expanded && hasDetail) {
                Text(
                    card.output.take(2000),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = palette.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StateIcon(state: CardState) {
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        when (state) {
            CardState.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Terracotta,
                strokeWidth = 2.dp,
            )
            CardState.OK -> Text("✓", color = Terracotta, fontWeight = FontWeight.Bold)
            CardState.FAILED -> Text("✕", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            CardState.REJECTED -> Text("–", color = LocalAetherPalette.current.muted, fontWeight = FontWeight.Bold)
        }
    }
}
