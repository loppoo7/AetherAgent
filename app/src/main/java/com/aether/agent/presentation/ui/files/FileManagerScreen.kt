package com.aether.agent.presentation.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.PteroFile
import com.aether.agent.presentation.components.AIcon
import com.aether.agent.presentation.components.AetherIcon
import com.aether.agent.presentation.components.IconAction
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.FileManagerViewModel

private enum class Prompt { NEW_FOLDER, RENAME, DELETE }

@Composable
fun FileManagerScreen(
    onBack: () -> Unit,
    onEdit: (serverId: String, path: String) -> Unit,
    viewModel: FileManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAetherPalette.current
    var prompt by remember { mutableStateOf<Prompt?>(null) }
    var target by remember { mutableStateOf<PteroFile?>(null) }
    var input by remember { mutableStateOf("") }

    BackHandler { if (!viewModel.up()) onBack() }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(AetherIcon.Back, "Kembali", { if (!viewModel.up()) onBack() })
            Column(Modifier.weight(1f)) {
                Text(
                    viewModel.serverName.ifBlank { viewModel.serverId },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(state.dir, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconAction(AetherIcon.Plus, "Folder baru", { input = ""; prompt = Prompt.NEW_FOLDER })
            IconAction(AetherIcon.Refresh, "Muat ulang", { viewModel.open(state.dir) })
        }

        state.notice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.padding(vertical = 4.dp))
        }

        when {
            state.loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Terracotta)
            }
            state.error != null -> Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            state.files.isEmpty() -> Text("Folder ini kosong.", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.files, key = { it.name }) { file ->
                    FileRow(
                        file = file,
                        onOpen = {
                            if (file.isDir) viewModel.open(viewModel.pathOf(file.name)) else onEdit(viewModel.serverId, viewModel.pathOf(file.name))
                        },
                        onRename = {
                            target = file
                            input = file.name
                            prompt = Prompt.RENAME
                        },
                        onDelete = {
                            target = file
                            prompt = Prompt.DELETE
                        },
                    )
                }
            }
        }
    }

    when (prompt) {
        Prompt.NEW_FOLDER -> TextPrompt(
            title = "Folder baru",
            value = input,
            onValue = { input = it },
            confirm = "Buat",
            onDismiss = { prompt = null },
            onConfirm = {
                viewModel.createFolder(input)
                prompt = null
            },
        )
        Prompt.RENAME -> TextPrompt(
            title = "Ganti nama",
            value = input,
            onValue = { input = it },
            confirm = "Simpan",
            onDismiss = { prompt = null },
            onConfirm = {
                target?.let { viewModel.rename(it.name, input) }
                prompt = null
            },
        )
        Prompt.DELETE -> AlertDialog(
            onDismissRequest = { prompt = null },
            title = { Text("Hapus ${target?.name.orEmpty()}?") },
            text = {
                Text(
                    if (target?.isDir == true) {
                        "Folder dan seluruh isinya akan dihapus permanen dari server."
                    } else {
                        "File ini akan dihapus permanen dari server."
                    },
                )
            },
            confirmButton = {
                Button(onClick = {
                    target?.let { viewModel.delete(listOf(it.name)) }
                    prompt = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { prompt = null }) { Text("Batal") } },
        )
        null -> Unit
    }
}

@Composable
private fun FileRow(file: PteroFile, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalAetherPalette.current
    var menu by remember { mutableStateOf(false) }
    Surface(
        color = palette.cardBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AIcon(
                if (file.isDir) AetherIcon.Folder else AetherIcon.FileDoc,
                tint = if (file.isDir) Terracotta else palette.muted,
                size = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!file.isDir) {
                    Text(formatSize(file.size), style = MaterialTheme.typography.labelSmall, color = palette.muted, fontFamily = FontFamily.Monospace)
                }
            }
            Box {
                IconAction(AetherIcon.More, "Opsi", { menu = true }, tint = palette.muted, boxSize = 40.dp, iconSize = 20.dp)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Ganti nama") },
                        onClick = {
                            menu = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TextPrompt(
    title: String,
    value: String,
    onValue: (String) -> Unit,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = value.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}
