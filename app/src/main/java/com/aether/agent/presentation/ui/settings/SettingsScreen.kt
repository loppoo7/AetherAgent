package com.aether.agent.presentation.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.Provider
import com.aether.agent.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val current by viewModel.settings.collectAsState()
    var draft by remember(current) { mutableStateOf(current) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)

        SectionTitle("Model AI")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.model,
            onValueChange = { draft = draft.copy(model = it) },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Endpoint: ${draft.provider.baseUrl}", style = MaterialTheme.typography.labelSmall)

        SectionTitle("GitHub")
        OutlinedTextField(
            value = draft.githubToken,
            onValueChange = { draft = draft.copy(githubToken = it) },
            label = { Text("Personal access token (repo, workflow)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.githubOwner,
            onValueChange = { draft = draft.copy(githubOwner = it) },
            label = { Text("Owner (username/org)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.githubRepo,
            onValueChange = { draft = draft.copy(githubRepo = it) },
            label = { Text("Nama repo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.githubBranch,
            onValueChange = { draft = draft.copy(githubBranch = it) },
            label = { Text("Branch kerja (disarankan branch khusus, bukan main)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle("Agen")
        OutlinedTextField(
            value = draft.systemPrompt,
            onValueChange = { draft = draft.copy(systemPrompt = it) },
            label = { Text("System prompt") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Konfirmasi sebelum menulis", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Tampilkan diff dan minta persetujuan sebelum commit atau PR",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = draft.confirmWrites,
                onCheckedChange = { draft = draft.copy(confirmWrites = it) },
            )
        }

        Button(onClick = { viewModel.save(draft, onBack) }, modifier = Modifier.fillMaxWidth()) {
            Text("Simpan")
        }
        TextButton(onClick = onBack) { Text("Kembali") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
