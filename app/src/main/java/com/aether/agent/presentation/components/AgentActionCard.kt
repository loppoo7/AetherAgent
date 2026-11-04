package com.aether.agent.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.aether.agent.presentation.ui.theme.AetherMono
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.AgentCard
import com.aether.agent.presentation.viewmodel.CardState

/** Kartu aksi GitHub: garis tipis, ketuk untuk membuka detail output. */
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
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = hasDetail) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
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
                    AIcon(
                        icon = AetherIcon.ChevronDown,
                        tint = palette.muted,
                        size = 18.dp,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f),
                    )
                }
            }
            AnimatedVisibility(visible = expanded && hasDetail) {
                Text(
                    card.output.take(2000),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = AetherMono,
                    color = palette.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StateIcon(state: CardState) {
    val palette = LocalAetherPalette.current
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        when (state) {
            CardState.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Terracotta,
                strokeWidth = 2.dp,
            )
            CardState.OK -> AIcon(AetherIcon.Check, tint = Terracotta, size = 18.dp, strokeWidth = 2.2.dp)
            CardState.FAILED -> AIcon(AetherIcon.Close, tint = MaterialTheme.colorScheme.error, size = 16.dp, strokeWidth = 2.2.dp)
            CardState.REJECTED -> AIcon(AetherIcon.Minus, tint = palette.muted, size = 16.dp, strokeWidth = 2.2.dp)
        }
    }
}
