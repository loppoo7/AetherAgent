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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
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
import com.aether.agent.presentation.components.AetherMark
import com.aether.agent.presentation.components.AgentActionCard
import com.aether.agent.presentation.components.AppDrawer
import com.aether.agent.presentation.components.ApprovalDialog
import com.aether.agent.presentation.components.ChatBubble
import com.aether.agent.presentation.components.ChatModelRoleSelector
import com.aether.agent.presentation.components.MultiFileContextPickerDialog
import com.aether.agent.presentation.components.IconAction
import com.aether.agent.presentation.components.RepoPickerDialog
import com.aether.agent.presentation.components.ThinkingIndicator
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.CardState
import com.aether.agent.presentation.viewmodel.ChatViewModel
import com.aether.agent.domain.model.AiRole
import com.aether.agent.domain.model.AttachedFileContext
import java.time.LocalTime
import kotlinx.coroutines.launch

private data class Suggestion(val category: String, val label: String, val prompt: String)

private val suggestionPool: List<List<Suggestion>> = listOf(
    listOf(
        Suggestion("Pelajari", "Baca struktur repo", "Baca struktur repo lalu jelaskan arsitekturnya secara singkat."),
        Suggestion("Pelajari", "Ringkas commit terbaru", "Tampilkan 10 commit terakhir dan ringkas perubahannya."),
        Suggestion("Pelajari", "Cek issue terbuka", "Lihat issue yang masih terbuka dan kelompokkan berdasarkan prioritas."),
        Suggestion("Pelajari", "Jelaskan alur utama", "Cari file utama (entry point) di repo ini lalu jelaskan alur kerjanya."),
        Suggestion("Pelajari", "Daftar dependensi", "Baca file build dan daftar dependensi utama beserta fungsinya."),
    ),
    listOf(
        Suggestion("Perbaiki", "Cari potensi bug", "Baca file kode utama dan cari potensi bug atau penanganan error yang kurang. Jangan ubah apa pun dulu."),
        Suggestion("Perbaiki", "Periksa keamanan", "Periksa repo untuk kebiasaan tidak aman, misalnya key atau token yang tertulis di kode. Laporkan saja, jangan ubah."),
        Suggestion("Perbaiki", "Rapikan penanganan error", "Cari bagian yang tidak menangani error dengan baik dan usulkan perbaikannya."),
        Suggestion("Perbaiki", "Cari kode tidak terpakai", "Cari file, fungsi, atau import yang kelihatannya tidak terpakai lalu daftarkan."),
    ),
    listOf(
        Suggestion("Bangun", "Usulkan fitur kecil", "Usulkan satu fitur kecil yang masuk akal untuk proyek ini lalu jelaskan langkah mengerjakannya."),
        Suggestion("Bangun", "Buat branch baru", "Buat branch baru bernama fitur-baru dari main."),
        Suggestion("Bangun", "Siapkan pull request", "Buat pull request dari branch kerja ke main dengan deskripsi singkat."),
        Suggestion("Bangun", "Tambah pengujian", "Usulkan pengujian unit yang paling penting untuk ditambahkan di repo ini."),
    ),
    listOf(
        Suggestion("Dokumentasi", "Perbarui README", "Baca README dan kode, lalu usulkan pembaruan README agar sesuai kondisi terbaru."),
        Suggestion("Dokumentasi", "Tulis changelog", "Buat ringkasan changelog dari commit terakhir."),
        Suggestion("Dokumentasi", "Cari praktik terbaik", "Cari di web praktik terbaik untuk menstrukturkan proyek seperti ini, lalu rangkum."),
        Suggestion("Dokumentasi", "Jelaskan untuk pemula", "Jelaskan proyek ini untuk pengembang baru dalam beberapa paragraf pendek."),
    ),
)

/** Tiga saran dari tiga kategori berbeda, dipilih acak setiap chat baru. */
private fun pickSuggestions(): List<Suggestion> =
    suggestionPool.shuffled().take(3).map { it.random() }

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
fun ChatScreen(onSettings: () -> Unit, onFileServer: () -> Unit = {}, viewModel: ChatViewModel = hiltViewModel()) {
    val sessions by viewModel.sessionList.collectAsState()
    val activeId by viewModel.activeSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val running by viewModel.running.collectAsState()
    val error by viewModel.error.collectAsState()
    val approval by viewModel.approval.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val attachedContext by viewModel.attachedContext.collectAsState()

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    val pendingInput by viewModel.pendingInput.collectAsState()
    LaunchedEffect(pendingInput) {
        pendingInput?.let {
            input = it
            viewModel.consumeHandoff()
        }
    }
    val picks = remember(activeId) { pickSuggestions() }
    var showRepoPicker by remember { mutableStateOf(false) }
    var showContextPicker by remember { mutableStateOf(false) }
    val repoTargets = settings.githubTargets.ifEmpty {
        if (settings.githubOwner.isNotBlank() && settings.githubRepo.isNotBlank())
            setOf("${settings.githubOwner}/${settings.githubRepo}|${settings.githubBranch}") else emptySet()
    }
    val repoSet = repoTargets.isNotEmpty()

    val status = cards.lastOrNull { it.state == CardState.RUNNING }?.title
    val title = sessions.firstOrNull { it.id == activeId }?.title ?: "AetherAgent"
    val subtitle = if (repoTargets.isNotEmpty()) "${repoTargets.size} repo dipilih" else "Pilih repo"

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
                onFileServer = {
                    scope.launch { drawerState.close() }
                    onFileServer()
                },
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
                onRepo = { showRepoPicker = true },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (messages.isEmpty() && cards.isEmpty() && !running) {
                    item {
                        EmptyState(
                            picks = picks,
                            repoSet = repoSet,
                            onPick = { input = it },
                            onPickRepo = { showRepoPicker = true },
                        )
                    }
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
                onAttach = { showContextPicker = true },
                providerLabel = settings.provider.label,
                model = settings.model,
                modelChoices = (settings.provider.models.ifEmpty {
                    when (settings.provider.name) {
                        "OPENAI" -> listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1", "o3-mini")
                        "ANTHROPIC" -> listOf("claude-3-7-sonnet-latest", "claude-3-5-sonnet-latest", "claude-3-5-haiku-latest")
                        "DEEPSEEK" -> listOf("deepseek-chat", "deepseek-reasoner")
                        else -> listOf(settings.model)
                    }
                } + settings.model).distinct(),
                role = selectedRole,
                roleSelectorEnabled = settings.roleSelectorEnabled,
                onModelSelected = viewModel::selectModel,
                onRoleSelected = viewModel::selectRole,
                onRoleSelectorEnabledChange = viewModel::setRoleSelectorEnabled,
                attachedContext = attachedContext,
                onRemoveContext = viewModel::removeContext,
            )
        }
    }

    approval?.let { pending ->
        ApprovalDialog(pending = pending, onResolve = viewModel::resolveApproval)
    }

    if (showRepoPicker) {
        RepoPickerDialog(
            selectedRepos = repoTargets,
            onDismiss = { showRepoPicker = false },
            onConfirm = { repos ->
                viewModel.selectRepos(repos)
                showRepoPicker = false
            },
        )
    }
    if (showContextPicker) {
        MultiFileContextPickerDialog(
            targets = repoTargets,
            onDismiss = { showContextPicker = false },
            onAttach = viewModel::attachContext,
        )
    }
}

@Composable
private fun TopBar(title: String, subtitle: String, onMenu: () -> Unit, onClear: () -> Unit, onRepo: () -> Unit) {
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
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRepo)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AIcon(AetherIcon.ChevronDown, tint = palette.muted, size = 14.dp)
            }
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
private fun EmptyState(
    picks: List<Suggestion>,
    repoSet: Boolean,
    onPick: (String) -> Unit,
    onPickRepo: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AetherMark(size = 88.dp)
        Spacer(Modifier.height(14.dp))
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
            if (!repoSet) {
                EmptyCard(category = "Mulai", label = "Pilih repo kerja", onClick = onPickRepo)
            } else {
                picks.forEach { s ->
                    EmptyCard(category = s.category, label = s.label, onClick = { onPick(s.prompt) })
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(category: String, label: String, onClick: () -> Unit) {
    val palette = LocalAetherPalette.current
    Surface(
        color = palette.cardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Terracotta)
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            AIcon(AetherIcon.ChevronRight, tint = palette.muted, size = 18.dp)
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
    providerLabel: String,
    model: String,
    modelChoices: List<String>,
    role: AiRole,
    roleSelectorEnabled: Boolean,
    onModelSelected: (String) -> Unit,
    onRoleSelected: (AiRole) -> Unit,
    onRoleSelectorEnabledChange: (Boolean) -> Unit,
    attachedContext: List<AttachedFileContext>,
    onRemoveContext: (String) -> Unit,
) {
    val palette = LocalAetherPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = palette.surface,
        border = BorderStroke(0.5.dp, palette.border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
            ChatModelRoleSelector(
                providerLabel = providerLabel,
                model = model,
                modelChoices = modelChoices,
                role = role,
                roleSelectorEnabled = roleSelectorEnabled,
                onModelSelected = onModelSelected,
                onRoleSelected = onRoleSelected,
                onRoleSelectorEnabledChange = onRoleSelectorEnabledChange,
            )
            if (attachedContext.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    attachedContext.forEach { file ->
                        Surface(color = palette.userBubble, shape = RoundedCornerShape(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(file.path.substringAfterLast('/'), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                Text(" ×", modifier = Modifier.clickable { onRemoveContext(file.id) }, color = palette.muted)
                            }
                        }
                    }
                }
            }
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
