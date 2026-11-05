package com.aether.agent.presentation.ui.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.AgentMode
import com.aether.agent.domain.model.PteroServer
import com.aether.agent.presentation.components.AIcon
import com.aether.agent.presentation.components.AetherIcon
import com.aether.agent.presentation.components.IconAction
import com.aether.agent.presentation.components.StatusDot
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.ServerListViewModel

private val Running = Color(0xFF22C55E)

@Composable
fun ServerListScreen(
    onBack: () -> Unit,
    onOpen: (PteroServer) -> Unit,
    viewModel: ServerListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val palette = LocalAetherPalette.current

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(AetherIcon.Back, "Kembali", onBack)
            Text("File Server", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconAction(AetherIcon.Refresh, "Muat ulang", viewModel::refresh)
        }

        AgentModeCard(mode = mode, onChange = viewModel::setMode)
        Spacer(Modifier.height(12.dp))

        state.notice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.padding(bottom = 6.dp))
        }

        when {
            state.loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Terracotta)
            }
            state.error != null -> Text(
                state.error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            state.servers.isEmpty() -> Text(
                "Tidak ada server. Cek URL panel dan API key di Pengaturan.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        busy = state.busyId == server.id,
                        onOpen = { onOpen(server) },
                        onPower = { signal -> viewModel.power(server.id, signal) },
                    )
                }
            }
        }
    }
}

/** Pilihan Mati, Bantu, Lakukan untuk Agen AI. Ringkas, dengan penjelasan satu baris. */
@Composable
private fun AgentModeCard(mode: AgentMode, onChange: (AgentMode) -> Unit) {
    val palette = LocalAetherPalette.current
    Surface(
        color = palette.cardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Terracotta.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AIcon(AetherIcon.Sparkle, tint = Terracotta, size = 12.dp)
                }
                Spacer(Modifier.width(8.dp))
                Text("Agen AI", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(palette.userBubble)
                    .padding(3.dp),
            ) {
                AgentMode.entries.forEach { m ->
                    val selected = m == mode
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { onChange(m) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            m.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else palette.muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(mode.hint, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
    }
}

@Composable
private fun ServerCard(server: PteroServer, busy: Boolean, onOpen: () -> Unit, onPower: (String) -> Unit) {
    val palette = LocalAetherPalette.current
    var menu by remember { mutableStateOf(false) }
    Surface(
        color = palette.cardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(if (server.suspended) palette.muted else Running)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (server.suspended) "Ditangguhkan · ${server.id}" else server.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Terracotta)
            } else {
                Box {
                    IconAction(AetherIcon.Power, "Power", { menu = true }, tint = palette.muted, boxSize = 40.dp, iconSize = 20.dp)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Mulai") }, onClick = { menu = false; onPower("start") })
                        DropdownMenuItem(text = { Text("Restart") }, onClick = { menu = false; onPower("restart") })
                        DropdownMenuItem(text = { Text("Berhenti") }, onClick = { menu = false; onPower("stop") })
                    }
                }
            }
        }
    }
}
