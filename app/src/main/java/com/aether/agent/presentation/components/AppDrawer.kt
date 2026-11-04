package com.aether.agent.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.aether.agent.domain.model.ChatSession
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Drawer kiri: header, Chat Baru, riwayat per waktu, dan footer pengaturan + status GitHub. */
@Composable
fun AppDrawer(
    sessions: List<ChatSession>,
    activeId: String,
    running: Boolean,
    modelLabel: String,
    githubLogin: String,
    onNewChat: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    ModalDrawerSheet(
        modifier = Modifier.width(304.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Terracotta),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AetherAgent", style = MaterialTheme.typography.headlineSmall)
                    Text(modelLabel, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Button(
                onClick = onNewChat,
                enabled = !running,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("+  Chat Baru", style = MaterialTheme.typography.labelLarge)
            }

            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
            ) {
                groupSessions(sessions).forEach { (label, group) ->
                    item(key = "header-$label") {
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted,
                            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp),
                        )
                    }
                    items(group, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            selected = session.id == activeId,
                            enabled = !running,
                            onSelect = { onSelect(session.id) },
                            onRename = { title -> onRename(session.id, title) },
                            onDelete = { onDelete(session.id) },
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.border)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSettings) {
                    Text("⚙  Pengaturan", style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                if (githubLogin.isNotBlank()) "● Terhubung sebagai @$githubLogin" else "○ GitHub belum terhubung",
                style = MaterialTheme.typography.bodySmall,
                color = if (githubLogin.isNotBlank()) Color(0xFF22C55E) else palette.muted,
                modifier = Modifier.padding(start = 20.dp, bottom = 20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: ChatSession,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var draft by remember(session.title) { mutableStateOf(session.title) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .combinedClickable(enabled = enabled, onClick = onSelect, onLongClick = { menuOpen = true })
                .padding(start = 14.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                session.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { menuOpen = true }, enabled = enabled) {
                Text("⋯", color = palette.muted)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Ubah nama") },
                onClick = {
                    menuOpen = false
                    renaming = true
                },
            )
            DropdownMenuItem(
                text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    deleting = true
                },
            )
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Ubah nama chat") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    renaming = false
                    onRename(draft)
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Batal") } },
        )
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Hapus chat ini?") },
            text = { Text("Riwayat percakapan ini akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    deleting = false
                    onDelete()
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Batal") } },
        )
    }
}

/** Kelompokkan sesi: Hari ini, Kemarin, 7 hari terakhir, lalu Lebih lama. */
internal fun groupSessions(
    sessions: List<ChatSession>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<Pair<String, List<ChatSession>>> {
    val today = LocalDate.now(zone)
    val groups = linkedMapOf(
        "Hari ini" to mutableListOf<ChatSession>(),
        "Kemarin" to mutableListOf(),
        "7 hari terakhir" to mutableListOf(),
        "Lebih lama" to mutableListOf(),
    )
    sessions.sortedByDescending { it.updatedAt }.forEach { session ->
        val date = Instant.ofEpochMilli(session.updatedAt).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(date, today)
        val key = when {
            days <= 0 -> "Hari ini"
            days == 1L -> "Kemarin"
            days <= 7 -> "7 hari terakhir"
            else -> "Lebih lama"
        }
        groups.getValue(key).add(session)
    }
    return groups.filterValues { it.isNotEmpty() }.map { it.key to it.value }
}
