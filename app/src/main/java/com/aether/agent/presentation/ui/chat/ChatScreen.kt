package com.aether.agent.presentation.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.presentation.components.AgentActionCard
import com.aether.agent.presentation.components.ApprovalDialog
import com.aether.agent.presentation.components.ChatBubble
import com.aether.agent.presentation.components.ThinkingIndicator
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.CardState
import com.aether.agent.presentation.viewmodel.ChatViewModel

@Composable
fun ChatScreen(onSettings: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val running by viewModel.running.collectAsState()
    val error by viewModel.error.collectAsState()
    val approval by viewModel.approval.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }

    // Judul aksi yang sedang berjalan dipakai sebagai status "berpikir".
    val status = cards.lastOrNull { it.state == CardState.RUNNING }?.title

    LaunchedEffect(messages.size, cards.size, running) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TopBar(
            title = if (settings.githubOwner.isNotBlank() && settings.githubRepo.isNotBlank()) {
                "${settings.githubOwner}/${settings.githubRepo}"
            } else {
                "AetherAgent"
            },
            subtitle = "${settings.githubBranch} · ${settings.model}",
            onSettings = onSettings,
            onClear = viewModel::clear,
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (messages.isEmpty() && cards.isEmpty() && !running) {
                item { EmptyState() }
            }
            items(messages, key = { it.id }) { ChatBubble(it) }
            items(cards, key = { it.id }) { AgentActionCard(it) }
            if (running) item { ThinkingIndicator(status = status) }
            error?.let { message ->
                item {
                    Text(
                        "Error: $message",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        InputBar(
            text = input,
            onTextChange = { input = it },
            running = running,
            onSend = {
                viewModel.send(input)
                input = ""
            },
            onStop = viewModel::stop,
            onAttach = { /* Lampiran file/repo belum diimplementasikan. */ },
        )
    }

    approval?.let { pending ->
        ApprovalDialog(pending = pending, onResolve = viewModel::resolveApproval)
    }
}

@Composable
private fun TopBar(title: String, subtitle: String, onSettings: () -> Unit, onClear: () -> Unit) {
    val palette = LocalAetherPalette.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Text("⋯", style = MaterialTheme.typography.titleLarge, color = palette.muted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Pengaturan") },
                    onClick = {
                        menuOpen = false
                        onSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Hapus riwayat") },
                    onClick = {
                        menuOpen = false
                        onClear()
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val palette = LocalAetherPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Terracotta),
            contentAlignment = Alignment.Center,
        ) {
            Text("A", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Apa yang mau dikerjakan hari ini?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tanyakan struktur repo, minta perubahan kode, atau buat pull request.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    running: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(0.5.dp, palette.border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Tulis instruksi…", color = palette.muted) },
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onAttach) {
                    Text("+", style = MaterialTheme.typography.titleLarge, color = palette.muted)
                }
                if (running) {
                    CircleButton(label = "■", enabled = true, color = palette.muted, onClick = onStop)
                } else {
                    CircleButton(label = "↑", enabled = text.isNotBlank(), color = Terracotta, onClick = onSend)
                }
            }
        }
    }
}

@Composable
private fun CircleButton(label: String, enabled: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) color else color.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}
