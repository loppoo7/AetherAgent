package com.aether.agent.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aether.agent.presentation.viewmodel.AgentCard
import com.aether.agent.presentation.viewmodel.CardState

/** Kartu log aksi agen (baca/tulis/commit). Ketuk untuk melihat detail output. */
@Composable
fun AgentActionCard(card: AgentCard) {
    var expanded by remember { mutableStateOf(false) }
    val (icon, label) = when (card.state) {
        CardState.RUNNING -> "⏳" to "Berjalan"
        CardState.OK -> "✅" to "Selesai"
        CardState.FAILED -> "❌" to "Gagal"
        CardState.REJECTED -> "⛔" to "Ditolak"
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    card.title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
            if (expanded && card.output.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    card.output.take(2000),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
