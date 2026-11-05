package com.aether.agent.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.GithubRepo
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.RepoPickerViewModel

/** Dialog pilih repo: daftar repo akun GitHub yang terhubung, dengan pencarian. */
@Composable
fun RepoPickerDialog(
    selectedOwner: String,
    selectedRepo: String,
    onDismiss: () -> Unit,
    onPick: (GithubRepo) -> Unit,
    viewModel: RepoPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAetherPalette.current
    val filtered = remember(state.repos, state.query) {
        val q = state.query.trim()
        if (q.isEmpty()) {
            state.repos
        } else {
            state.repos.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.owner.contains(q, ignoreCase = true) ||
                    (it.description?.contains(q, ignoreCase = true) == true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .heightIn(max = 620.dp),
        ) {
            Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Pilih repo kerja",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconAction(AetherIcon.Close, "Tutup", onDismiss)
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Cari repo...", color = palette.muted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
                when {
                    state.loading -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Terracotta)
                    }
                    state.error != null -> Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::load) { Text("Coba lagi") }
                    }
                    filtered.isEmpty() -> Text(
                        "Tidak ada repo yang cocok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                    else -> LazyColumn(
                        Modifier.weight(1f, fill = false),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered, key = { it.owner + "/" + it.name }) { repo ->
                            RepoRow(
                                repo = repo,
                                selected = repo.owner.equals(selectedOwner, ignoreCase = true) &&
                                    repo.name.equals(selectedRepo, ignoreCase = true),
                                onClick = { onPick(repo) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoRow(repo: GithubRepo, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAetherPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (repo.isPrivate) RepoTag("Privat")
                if (!repo.canPush) RepoTag("Hanya baca")
            }
            val desc = repo.description?.takeIf { it.isNotBlank() }
            Text(
                if (desc != null) "${repo.owner} · $desc" else repo.owner,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) AIcon(AetherIcon.Check, tint = Terracotta, size = 20.dp, strokeWidth = 2.2.dp)
    }
}

@Composable
private fun RepoTag(text: String) {
    val palette = LocalAetherPalette.current
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = palette.muted,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
