package com.aether.agent.presentation.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
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
import com.aether.agent.presentation.ui.theme.AetherMono
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.ClientApiCheckState
import com.aether.agent.presentation.viewmodel.SettingsViewModel
import com.aether.agent.presentation.viewmodel.SftpCheckState

private val SuccessGreen = Color(0xFF16A34A)

private enum class SettingsTab(val label: String) {
    PROVIDER("Provider AI"),
    UMUM("Umum"),
    GITHUB("GitHub"),
    PANEL("Panel"),
    SFTP("SFTP"),
}

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val current by viewModel.settings.collectAsState()
    val login by viewModel.login.collectAsState()
    val clientApiCheck by viewModel.clientApiCheck.collectAsState()
    val sftpCheck by viewModel.sftpCheck.collectAsState()
    var draft by remember(current) { mutableStateOf(current) }
    var tab by rememberSaveable { mutableStateOf(SettingsTab.PROVIDER) }

    /** Perubahan yang langsung disimpan (dipakai untuk bagian Panel dan SFTP). */
    fun persistDraft(updated: AgentSettings) {
        draft = updated
        viewModel.persistDraft(updated)
    }

    /** Mengganti host atau port membuat fingerprint lama tidak berlaku lagi. */
    fun sftpChanged(updated: AgentSettings) {
        val endpointChanged = updated.sftpHost != draft.sftpHost || updated.sftpPort != draft.sftpPort
        if (endpointChanged) viewModel.resetSftpCheck()
        persistDraft(if (endpointChanged) updated.copy(sftpHostKey = "") else updated)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(AetherIcon.Back, "Kembali", onBack)
            Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)
        }

        ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp) {
            SettingsTab.entries.forEach { t ->
                Tab(
                    selected = tab == t,
                    onClick = { tab = t },
                    text = { Text(t.label) },
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when (tab) {
                SettingsTab.PROVIDER -> ProviderSection(
                    draft = draft,
                    onChange = { draft = it },
                )
                SettingsTab.UMUM -> GeneralSection(
                    draft = draft,
                    onChange = { draft = it },
                )
                SettingsTab.GITHUB -> GithubSection(
                    draft = draft,
                    current = current,
                    login = login,
                    onChange = { draft = it },
                    onConnect = viewModel::connectGithub,
                    onCancel = viewModel::cancelConnect,
                    onDisconnect = viewModel::disconnectGithub,
                )
                SettingsTab.PANEL -> PanelSection(
                    draft = draft,
                    check = clientApiCheck,
                    onChange = ::persistDraft,
                    onTest = { viewModel.testClientApi(draft) },
                )
                SettingsTab.SFTP -> SftpSection(
                    draft = draft,
                    check = sftpCheck,
                    onChange = ::sftpChanged,
                    onConnect = { viewModel.connectSftp(draft) },
                    onTrust = { fingerprint ->
                        val updated = draft.copy(sftpHostKey = fingerprint)
                        persistDraft(updated)
                        viewModel.trustAndConnect(updated)
                    },
                    onForget = {
                        persistDraft(draft.copy(sftpHostKey = ""))
                        viewModel.resetSftpCheck()
                    },
                )
            }

            SettingsSection("Tentang aplikasi") {
                Text("AetherAgent · versi 1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Dibangun dengan Jetpack Compose. Lisensi open source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAetherPalette.current.muted,
                )
            }

            Button(
                onClick = { viewModel.save(draft, onBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simpan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}


/* ---------------------------------------------------------------------------------------------
 * Bagian: Provider AI
 * ------------------------------------------------------------------------------------------- */

@Composable
private fun ProviderSection(draft: AgentSettings, onChange: (AgentSettings) -> Unit) {
    val palette = LocalAetherPalette.current
    var showKey by remember { mutableStateOf(false) }

    SettingsSection(
        title = "Provider AI",
        description = "Penyedia model yang dipakai agen untuk membalas chat.",
    ) {
        SubLabel("Provider")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Provider.entries.forEach { p ->
                FilterChip(
                    selected = draft.provider == p,
                    onClick = { onChange(draft.copy(provider = p, model = p.defaultModel)) },
                    label = { Text(p.label) },
                )
            }
        }
        OutlinedTextField(
            value = draft.apiKey,
            onValueChange = { v -> onChange(draft.copy(apiKeys = draft.apiKeys + (draft.provider to v))) },
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
            onValueChange = { onChange(draft.copy(model = it)) },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.provider.models.size > 1) {
            SubLabel("Pilih cepat")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.provider.models.forEach { m ->
                    FilterChip(
                        selected = draft.model == m,
                        onClick = { onChange(draft.copy(model = m)) },
                        label = { Text(m.substringAfter('/')) },
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Bagian: GitHub
 * ------------------------------------------------------------------------------------------- */

@Composable
private fun GithubSection(
    draft: AgentSettings,
    current: AgentSettings,
    login: GithubLoginState?,
    onChange: (AgentSettings) -> Unit,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    var showRepoPicker by remember { mutableStateOf(false) }

    SettingsSection(
        title = "GitHub",
        description = "Akun dan repository tempat agen membaca serta menulis kode.",
    ) {
        GithubConnection(
            state = login,
            connectedLogin = current.githubLogin,
            avatarUrl = current.githubAvatarUrl,
            onConnect = onConnect,
            onCancel = onCancel,
            onDisconnect = onDisconnect,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SubLabel("Repository target")
                val targets = draft.githubTargets.ifEmpty {
                    if (draft.githubOwner.isNotBlank() && draft.githubRepo.isNotBlank())
                        setOf("${draft.githubOwner}/${draft.githubRepo}|${draft.githubBranch}") else emptySet()
                }
                Text(
                    if (targets.isNotEmpty()) "${targets.size} repository dipilih" else "Belum dipilih",
                    style = MaterialTheme.typography.bodyLarge,
                )
                targets.sorted().forEach { target ->
                    Text(
                        target.substringBefore('|'),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }
            Button(onClick = { showRepoPicker = true }) { Text("Pilih repo") }
        }
        OutlinedTextField(
            value = draft.githubBranch,
            onValueChange = { branch ->
                onChange(
                    draft.copy(
                        githubBranch = branch,
                        githubTargets = draft.githubTargets.map { target ->
                            target.substringBefore('|') + "|" + branch
                        }.toSet(),
                    ),
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
            onChange = { onChange(draft.copy(confirmWrites = it)) },
        )
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
                onChange(
                    draft.copy(
                        githubOwner = first?.owner.orEmpty(),
                        githubRepo = first?.name.orEmpty(),
                        githubBranch = first?.defaultBranch ?: draft.githubBranch,
                        githubTargets = repos.map { it.toTargetKey() }.toSet(),
                    ),
                )
                showRepoPicker = false
            },
        )
    }
}

/* ---------------------------------------------------------------------------------------------
 * Bagian: Umum
 * ------------------------------------------------------------------------------------------- */

@Composable
private fun GeneralSection(draft: AgentSettings, onChange: (AgentSettings) -> Unit) {
    SettingsSection(
        title = "Umum",
        description = "Tampilan dan perilaku jawaban agen.",
    ) {
        SubLabel("Tampilan")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(
                    selected = draft.themeMode == m,
                    onClick = { onChange(draft.copy(themeMode = m)) },
                    label = { Text(m.label) },
                )
            }
        }

        SubLabel("Gaya jawaban")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnswerStyle.entries.forEach { style ->
                FilterChip(
                    selected = draft.answerStyle == style,
                    onClick = { onChange(draft.copy(answerStyle = style)) },
                    label = { Text(style.label) },
                )
            }
        }
        Text(
            draft.answerStyle.hint,
            style = MaterialTheme.typography.bodySmall,
            color = LocalAetherPalette.current.muted,
        )

        OutlinedTextField(
            value = draft.systemPrompt,
            onValueChange = { onChange(draft.copy(systemPrompt = it)) },
            label = { Text("System prompt") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/* ---------------------------------------------------------------------------------------------
 * Bagian: Panel Pterodactyl (Client API)
 * ------------------------------------------------------------------------------------------- */

@Composable
private fun PanelSection(
    draft: AgentSettings,
    check: ClientApiCheckState,
    onChange: (AgentSettings) -> Unit,
    onTest: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    var showKey by remember { mutableStateOf(false) }

    SettingsSection(
        title = "Panel Pterodactyl",
        description = "Dipakai untuk memuat daftar server dan mengirim sinyal power. " +
            "URL ini adalah alamat web panel tempat kamu login, bukan alamat node SFTP. Contoh: https://panel.domain.com",
    ) {
        OutlinedTextField(
            value = draft.panelUrl,
            onValueChange = { onChange(draft.copy(panelUrl = it)) },
            label = { Text("URL web panel (bukan URL node)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.pteroApiKey,
            onValueChange = { onChange(draft.copy(pteroApiKey = it)) },
            label = { Text("API key Client") },
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
        Text(
            "Gunakan Client API Key dari menu Account API (umumnya diawali ptlc_), bukan Application API Key.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
        Button(
            onClick = onTest,
            enabled = check !is ClientApiCheckState.Checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (check is ClientApiCheckState.Checking) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Test Client API")
            }
        }
        when (check) {
            ClientApiCheckState.Idle -> Unit
            ClientApiCheckState.Checking -> Text(
                "Memeriksa URL panel dan API key…",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            is ClientApiCheckState.Success -> Text(
                check.message,
                style = MaterialTheme.typography.bodySmall,
                color = SuccessGreen,
            )
            is ClientApiCheckState.Failure -> Text(
                check.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Bagian: SFTP
 * ------------------------------------------------------------------------------------------- */

@Composable
private fun SftpSection(
    draft: AgentSettings,
    check: SftpCheckState,
    onChange: (AgentSettings) -> Unit,
    onConnect: () -> Unit,
    onTrust: (String) -> Unit,
    onForget: () -> Unit,
) {
    val palette = LocalAetherPalette.current
    var showPass by remember { mutableStateOf(false) }
    val trusted = draft.sftpHostKey.isNotBlank()

    SettingsSection(
        title = "SFTP",
        description = "Akses file server lewat SFTP. Jalankan tiga langkah berurutan.",
    ) {
        StatusLine(
            text = if (trusted) "Fingerprint tersimpan, host sudah dipercaya" else "Belum terverifikasi",
            color = if (trusted) SuccessGreen else palette.muted,
        )

        SubLabel("1 · Alamat dan login")
        OutlinedTextField(
            value = draft.sftpHost,
            onValueChange = { onChange(draft.copy(sftpHost = it.trim())) },
            label = { Text("Host node SFTP") },
            supportingText = {
                Text("Alamat NODE, bukan web panel. Boleh hostname atau https://node.domain.com")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = if (draft.sftpPort == 0) "" else draft.sftpPort.toString(),
                onValueChange = { v -> onChange(draft.copy(sftpPort = v.filter { it.isDigit() }.toIntOrNull() ?: 0)) },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.4f),
            )
            OutlinedTextField(
                value = draft.sftpUser,
                onValueChange = { onChange(draft.copy(sftpUser = it.trim())) },
                label = { Text("Username panel") },
                singleLine = true,
                modifier = Modifier.weight(0.6f),
            )
        }
        OutlinedTextField(
            value = draft.sftpServerId,
            onValueChange = { onChange(draft.copy(sftpServerId = it.trim())) },
            label = { Text("ID server (8 karakter)") },
            supportingText = { Text("Opsional. Dipakai untuk tes login dan akses file.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.sftpPassword,
            onValueChange = { onChange(draft.copy(sftpPassword = it)) },
            label = { Text("Password akun panel") },
            singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconAction(
                    icon = if (showPass) AetherIcon.EyeOff else AetherIcon.Eye,
                    description = if (showPass) "Sembunyikan password" else "Tampilkan password",
                    onClick = { showPass = !showPass },
                    tint = palette.muted,
                    boxSize = 40.dp,
                    iconSize = 20.dp,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        SubLabel("2 · Baca fingerprint host")
        Button(
            onClick = onConnect,
            enabled = check !is SftpCheckState.Checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (check is SftpCheckState.Checking) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Connect SFTP")
            }
        }
        when (check) {
            SftpCheckState.Idle -> Unit
            SftpCheckState.Checking -> Text(
                "Memeriksa host dan koneksi SFTP…",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            is SftpCheckState.AwaitingTrust -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Cocokkan fingerprint ini dengan penyedia server sebelum menyetujuinya:",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                Text(check.fingerprint, style = MaterialTheme.typography.bodySmall, fontFamily = AetherMono)
                Button(
                    onClick = { onTrust(check.fingerprint) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Percayai fingerprint & tes login")
                }
            }
            is SftpCheckState.Connected -> Text(
                check.serverName,
                style = MaterialTheme.typography.bodySmall,
                color = SuccessGreen,
            )
            is SftpCheckState.Failure -> Text(
                check.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        SubLabel("3 · Fingerprint tersimpan")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (trusted) draft.sftpHostKey else "Belum ada. Selesaikan langkah 2 untuk menyimpannya.",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (trusted) AetherMono else null,
                color = palette.muted,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onForget, enabled = trusted) { Text("Lupakan") }
        }
        Text(
            "Mengganti host atau port otomatis menghapus fingerprint lama. " +
                "Login memakai username.idserver secara otomatis, jadi satu akun panel berlaku untuk semua server.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
    }
}

/* ---------------------------------------------------------------------------------------------
 * Komponen pendukung
 * ------------------------------------------------------------------------------------------- */

/** Kartu satu bagian. Judul ditampilkan di atas kartu, deskripsi singkat di dalam kartu. */
@Composable
private fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalAetherPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            color = palette.cardBg,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.5.dp, palette.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                content()
            }
        }
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = LocalAetherPalette.current.muted)
}

@Composable
private fun StatusLine(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
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
        state is GithubLoginState.Requesting -> Text(
            "Menyiapkan kode…",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )
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
                    StatusDot(SuccessGreen)
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
