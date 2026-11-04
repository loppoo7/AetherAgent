package com.aether.agent.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val Connected = Color(0xFF22C55E)

/** Drawer kiri: nama app, Chat Baru, riwayat per waktu, lalu footer pengaturan dan status GitHub. */
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
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 4.dp)) {
                Text("AetherAgent", style = MaterialTheme.typography.headlineSmall)
                Text(
                    modelLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = onNewChat,
                enabled = !running,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                AIcon(AetherIcon.Plus, size = 18.dp, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Chat Baru", style = MaterialTheme.typography.labelLarge)
            }

            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
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
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AIcon(AetherIcon.Settings, size = 20.dp)
                Spacer(Modifier.width(14.dp))
                Text("Pengaturan", style = MaterialTheme.typography.labelLarge)
            }
            Row(
                Modifier.padding(start = 22.dp, end = 22.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(if (githubLogin.isNotBlank()) Connected else palette.border)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (githubLogin.isNotBlank()) "Terhubung sebagai @$githubLogin" else "GitHub belum terhubung",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                .padding(start = 14.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                session.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
            IconAction(
                icon = AetherIcon.More,
                description = "Opsi chat",
                onClick = { menuOpen = true },
                tint = palette.muted,
                enabled = enabled,
                boxSize = 40.dp,
                iconSize = 20.dp,
            )
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
