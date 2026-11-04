package com.aether.agent.presentation.ui.chat

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.presentation.components.AIcon
import com.aether.agent.presentation.components.AetherIcon
import com.aether.agent.presentation.components.AgentActionCard
import com.aether.agent.presentation.components.AppDrawer
import com.aether.agent.presentation.components.ApprovalDialog
import com.aether.agent.presentation.components.ChatBubble
import com.aether.agent.presentation.components.IconAction
import com.aether.agent.presentation.components.ThinkingIndicator
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.CardState
import com.aether.agent.presentation.viewmodel.ChatViewModel
import java.time.LocalTime
import kotlinx.coroutines.launch

private data class Suggestion(val label: String, val prompt: String)

private val suggestions = listOf(
    Suggestion("Baca struktur repo", "Baca struktur repo lalu jelaskan arsitekturnya secara singkat."),
    Suggestion("10 commit terakhir", "Tampilkan 10 commit terakhir dan ringkas perubahannya."),
    Suggestion("Issue yang terbuka", "Lihat issue yang masih terbuka dan kelompokkan berdasarkan prioritas."),
)

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> "Selamat pagi"
        hour < 15 -> "Selamat siang"
        hour < 18 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

@Composable
fun ChatScreen(onSettings: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val sessions by viewModel.sessionList.collectAsState()
    val activeId by viewModel.activeSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val running by viewModel.running.collectAsState()
    val error by viewModel.error.collectAsState()
    val approval by viewModel.approval.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }

    val status = cards.lastOrNull { it.state == CardState.RUNNING }?.title
    val title = sessions.firstOrNull { it.id == activeId }?.title ?: "AetherAgent"
    val subtitle = if (settings.githubOwner.isNotBlank() && settings.githubRepo.isNotBlank()) {
        "${settings.githubOwner}/${settings.githubRepo} · ${settings.githubBranch}"
    } else {
        "Repo belum diatur"
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    LaunchedEffect(messages.size, cards.size, running) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                sessions = sessions,
                activeId = activeId,
                running = running,
                modelLabel = "${settings.provider.label} · ${settings.model}",
                githubLogin = settings.githubLogin,
                onNewChat = {
                    viewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onSelect = { id ->
                    viewModel.selectSession(id)
                    scope.launch { drawerState.close() }
                },
                onRename = viewModel::renameSession,
                onDelete = viewModel::deleteSession,
                onSettings = {
                    scope.launch { drawerState.close() }
                    onSettings()
                },
            )
        },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(
                title = title,
                subtitle = subtitle,
                onMenu = { scope.launch { drawerState.open() } },
                onClear = viewModel::clear,
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (messages.isEmpty() && cards.isEmpty() && !running) {
                    item { EmptyState(onPick = { input = it }) }
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
    }

    approval?.let { pending ->
        ApprovalDialog(pending = pending, onResolve = viewModel::resolveApproval)
    }
}

@Composable
private fun TopBar(title: String, subtitle: String, onMenu: () -> Unit, onClear: () -> Unit) {
    val palette = LocalAetherPalette.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(AetherIcon.Menu, "Buka menu", onMenu)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconAction(AetherIcon.More, "Opsi chat", { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Kosongkan chat ini") },
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
private fun EmptyState(onPick: (String) -> Unit) {
    val palette = LocalAetherPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AIcon(AetherIcon.Sparkle, tint = Terracotta, size = 44.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            greeting(),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp, lineHeight = 38.sp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Mau mengerjakan apa?",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { s ->
                Surface(
                    color = palette.cardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, palette.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        s.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(s.prompt) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(28.dp),
        color = palette.surface,
        border = BorderStroke(0.5.dp, palette.border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Tulis instruksi...", color = palette.muted) },
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
                IconAction(AetherIcon.Plus, "Lampirkan", onAttach, tint = palette.muted)
                if (running) {
                    CircleButton(icon = AetherIcon.Stop, description = "Hentikan", enabled = true, onClick = onStop)
                } else {
                    CircleButton(icon = AetherIcon.ArrowUp, description = "Kirim", enabled = text.isNotBlank(), onClick = onSend)
                }
            }
        }
    }
}

@Composable
private fun CircleButton(icon: AetherIcon, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (enabled) Terracotta else Terracotta.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AIcon(icon, tint = Color.White, size = 20.dp, strokeWidth = 2.2.dp)
    }
}
