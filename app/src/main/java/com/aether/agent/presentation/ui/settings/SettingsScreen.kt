package com.aether.agent.presentation.ui.settings

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.aether.agent.presentation.ui.theme.AetherMono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.AnswerStyle
import com.aether.agent.domain.model.Provider
import com.aether.agent.domain.model.ThemeMode
import com.aether.agent.domain.model.toTargetKey
import com.aether.agent.domain.usecase.GithubLoginState
import com.aether.agent.presentation.components.AetherIcon
import com.aether.agent.presentation.components.IconAction
import com.aether.agent.presentation.components.RepoPickerDialog
import com.aether.agent.presentation.components.StatusDot
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.ClientApiCheckState
import com.aether.agent.presentation.viewmodel.SftpCheckState
import com.aether.agent.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val current by viewModel.settings.collectAsState()
    val login by viewModel.login.collectAsState()
    val clientApiCheck by viewModel.clientApiCheck.collectAsState()
    val sftpCheck by viewModel.sftpCheck.collectAsState()
    var draft by remember(current) { mutableStateOf(current) }
    var showKey by remember { mutableStateOf(false) }
    var showPteroKey by remember { mutableStateOf(false) }
    var showSftpPass by remember { mutableStateOf(false) }
    var showRepoPicker by remember { mutableStateOf(false) }
    val palette = LocalAetherPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(AetherIcon.Back, "Kembali", onBack)
            Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)
        }

        Section("AI & Model") {
            Text("Provider", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Provider.entries.forEach { p ->
                    FilterChip(
                        selected = draft.provider == p,
                        onClick = { draft = draft.copy(provider = p, model = p.defaultModel) },
                        label = { Text(p.label) },
                    )
                }
            }
            OutlinedTextField(
                value = draft.apiKey,
                onValueChange = { v -> draft = draft.copy(apiKeys = draft.apiKeys + (draft.provider to v)) },
                label = { Text("API key ${draft.provider.label}") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconAction(
                        icon = if (showKey) AetherIcon.EyeOff else AetherIcon.Eye,
                        description = if (showKey) "Sembunyikan key" else "Tampilkan key",
                        onClick = { showKey = !showKey },
                        tint = palette.muted,
                        boxSize = 40.dp,
                        iconSize = 20.dp,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.model,
                onValueChange = { draft = draft.copy(model = it) },
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (draft.provider.models.size > 1) {
                Text("Pilih cepat", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.provider.models.forEach { m ->
                        FilterChip(
                            selected = draft.model == m,
                            onClick = { draft = draft.copy(model = m) },
                            label = { Text(m.substringAfter('/')) },
                        )
                    }
                }
            }
            Text("Gaya jawaban", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnswerStyle.entries.forEach { style ->
                    FilterChip(
                        selected = draft.answerStyle == style,
                        onClick = { draft = draft.copy(answerStyle = style) },
                        label = { Text(style.label) },
                    )
                }
            }
            Text(draft.answerStyle.hint, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            OutlinedTextField(
                value = draft.systemPrompt,
                onValueChange = { draft = draft.copy(systemPrompt = it) },
                label = { Text("System prompt") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Section("GitHub") {
            GithubConnection(
                state = login,
                connectedLogin = current.githubLogin,
                avatarUrl = current.githubAvatarUrl,
                onConnect = viewModel::connectGithub,
                onCancel = viewModel::cancelConnect,
                onDisconnect = viewModel::disconnectGithub,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Repository target", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                    val targets = draft.githubTargets.ifEmpty {
                        if (draft.githubOwner.isNotBlank() && draft.githubRepo.isNotBlank())
                            setOf("${draft.githubOwner}/${draft.githubRepo}|${draft.githubBranch}") else emptySet()
                    }
                    Text(if (targets.isNotEmpty()) "${targets.size} repository dipilih" else "Belum dipilih", style = MaterialTheme.typography.bodyLarge)
                    targets.sorted().forEach { target ->
                        Text(target.substringBefore('|'), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
                Button(onClick = { showRepoPicker = true }) { Text("Pilih repo") }
            }
            OutlinedTextField(
                value = draft.githubBranch,
                onValueChange = { branch ->
                    draft = draft.copy(
                        githubBranch = branch,
                        githubTargets = draft.githubTargets.map { target ->
                            target.substringBefore('|') + "|" + branch
                        }.toSet(),
                    )
                },
                label = { Text("Branch kerja untuk semua repo (harus sudah ada)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ToggleRow(
                title = "Konfirmasi diff sebelum commit",
                subtitle = "Tampilkan perubahan dan minta persetujuan sebelum menulis ke repo",
                checked = draft.confirmWrites,
                onChange = { draft = draft.copy(confirmWrites = it) },
            )
        }

        Section("Tampilan") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(
                        selected = draft.themeMode == m,
                        onClick = { draft = draft.copy(themeMode = m) },
                        label = { Text(m.label) },
                    )
                }
            }
        }

        Section("Server") {
            Text("File lewat SFTP", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            OutlinedTextField(
                value = draft.sftpHost,
                onValueChange = { draft = draft.copy(sftpHost = it.trim()) },
                label = { Text("Host SFTP (alamat node)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (draft.sftpPort == 0) "" else draft.sftpPort.toString(),
                    onValueChange = { v -> draft = draft.copy(sftpPort = v.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.4f),
                )
                OutlinedTextField(
                    value = draft.sftpUser,
                    onValueChange = { draft = draft.copy(sftpUser = it.trim()) },
                    label = { Text("Username panel") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f),
                )
            }
            OutlinedTextField(
                value = draft.sftpPassword,
                onValueChange = { draft = draft.copy(sftpPassword = it) },
                label = { Text("Password SFTP (password akun panel)") },
                singleLine = true,
                visualTransformation = if (showSftpPass) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconAction(
                        icon = if (showSftpPass) AetherIcon.EyeOff else AetherIcon.Eye,
                        description = if (showSftpPass) "Sembunyikan password" else "Tampilkan password",
                        onClick = { showSftpPass = !showSftpPass },
                        tint = palette.muted,
                        boxSize = 40.dp,
                        iconSize = 20.dp,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.connectSftp(draft) },
                enabled = sftpCheck !is SftpCheckState.Checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (sftpCheck is SftpCheckState.Checking) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Connect SFTP")
                }
            }
            when (val check = sftpCheck) {
                SftpCheckState.Idle -> Unit
                SftpCheckState.Checking -> Text(
                    "Memeriksa host dan koneksi SFTP…",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                is SftpCheckState.AwaitingTrust -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Fingerprint perangkat/host ditemukan. Cocokkan fingerprint ini dengan penyedia server sebelum menyetujuinya:",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                    Text(
                        check.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = AetherMono,
                    )
                    Button(
                        onClick = {
                            val updated = draft.copy(sftpHostKey = check.fingerprint)
                            draft = updated
                            viewModel.trustAndConnect(updated)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Percayai fingerprint & Connect")
                    }
                }
                is SftpCheckState.Connected -> Text(
                    "Terhubung ke ${check.serverName}. Fingerprint sudah diverifikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16A34A),
                )
                is SftpCheckState.Failure -> Text(
                    check.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Fingerprint server", style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (draft.sftpHostKey.isBlank()) "Belum diverifikasi. Tekan Connect SFTP untuk membaca fingerprint." else draft.sftpHostKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        maxLines = 2,
                    )
                }
                TextButton(onClick = {
                    draft = draft.copy(sftpHostKey = "")
                    viewModel.resetSftpCheck()
                }) { Text("Lupakan") }
            }
            Text(
                "Login SFTP memakai username.idserver secara otomatis, jadi satu akun panel berlaku untuk semua server.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            Text("Restart server (Client API panel)", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            OutlinedTextField(
                value = draft.panelUrl,
                onValueChange = { draft = draft.copy(panelUrl = it) },
                label = { Text("URL panel (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.pteroApiKey,
                onValueChange = { draft = draft.copy(pteroApiKey = it) },
                label = { Text("API key Client") },
                singleLine = true,
                visualTransformation = if (showPteroKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconAction(
                        icon = if (showPteroKey) AetherIcon.EyeOff else AetherIcon.Eye,
                        description = if (showPteroKey) "Sembunyikan key" else "Tampilkan key",
                        onClick = { showPteroKey = !showPteroKey },
                        tint = palette.muted,
                        boxSize = 40.dp,
                        iconSize = 20.dp,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Gunakan Client API Key dari menu Account API (umumnya diawali ptlc_), bukan Application API Key.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            Button(
                onClick = { viewModel.testClientApi(draft.panelUrl, draft.pteroApiKey) },
                enabled = clientApiCheck !is ClientApiCheckState.Checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (clientApiCheck is ClientApiCheckState.Checking) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Test Client API")
                }
            }
            when (val check = clientApiCheck) {
                ClientApiCheckState.Idle -> Unit
                ClientApiCheckState.Checking -> Text(
                    "Memeriksa URL panel dan API key…",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                is ClientApiCheckState.Success -> Text(
                    check.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16A34A),
                )
                is ClientApiCheckState.Failure -> Text(
                    check.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Section("Aplikasi") {
            Text("AetherAgent · versi 1.0.0", style = MaterialTheme.typography.bodyMedium)
            Text("Dibangun dengan Jetpack Compose. Lisensi open source.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }

        Button(
            onClick = { viewModel.save(draft, onBack) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Simpan")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showRepoPicker) {
        RepoPickerDialog(
            selectedRepos = draft.githubTargets.ifEmpty {
                if (draft.githubOwner.isNotBlank() && draft.githubRepo.isNotBlank())
                    setOf("${draft.githubOwner}/${draft.githubRepo}|${draft.githubBranch}") else emptySet()
            },
            onDismiss = { showRepoPicker = false },
            onConfirm = { repos ->
                val first = repos.firstOrNull()
                draft = draft.copy(
                    githubOwner = first?.owner.orEmpty(),
                    githubRepo = first?.name.orEmpty(),
                    githubBranch = first?.defaultBranch ?: draft.githubBranch,
                    githubTargets = repos.map { it.toTargetKey() }.toSet(),
                )
                showRepoPicker = false
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalAetherPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = palette.muted, modifier = Modifier.padding(start = 4.dp))
        Surface(
            color = palette.cardBg,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.5.dp, palette.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalAetherPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun GithubConnection(
    state: GithubLoginState?,
    connectedLogin: String,
    avatarUrl: String,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current

    when {
        state is GithubLoginState.WaitingForUser -> {
            Text("Buka github.com/login/device lalu masukkan kode ini:", style = MaterialTheme.typography.bodyMedium)
            Text(
                state.userCode,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = AetherMono,
                color = Terracotta,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.userBubble)
                    .clickable { clipboard.setText(AnnotatedString(state.userCode)) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Text("Ketuk kode untuk menyalin.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { uriHandler.openUri(state.verificationUri) }) { Text("Buka GitHub") }
                TextButton(onClick = onCancel) { Text("Batal") }
            }
        }
        state is GithubLoginState.Requesting -> Text("Menyiapkan kode…", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        connectedLogin.isNotBlank() -> Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto profil GitHub",
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            } else {
                Text("@", modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.userBubble).padding(12.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("@$connectedLogin", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(Color(0xFF22C55E))
                    Spacer(Modifier.width(6.dp))
                    Text("Terhubung", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
            }
            TextButton(onClick = onDisconnect) { Text("Putuskan", color = MaterialTheme.colorScheme.error) }
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hubungkan akun GitHub untuk membaca dan menulis repo.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onConnect) { Text("Hubungkan") }
        }
    }
    if (state is GithubLoginState.Failed) {
        Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
