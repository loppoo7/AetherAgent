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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.GithubRepo
import com.aether.agent.domain.model.toTargetKey
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.RepoPickerViewModel

@Composable
fun RepoPickerDialog(
    selectedRepos: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<GithubRepo>) -> Unit,
    viewModel: RepoPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAetherPalette.current
    var selected by remember(state.repos, selectedRepos) { mutableStateOf(selectedRepos.toSet()) }
    val filtered = remember(state.repos, state.query) {
        val q = state.query.trim()
        if (q.isEmpty()) state.repos else state.repos.filter {
            it.name.contains(q, true) || it.owner.contains(q, true) || (it.description?.contains(q, true) == true)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, palette.border),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).heightIn(max = 650.dp),
        ) {
            Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pilih repository", style = MaterialTheme.typography.titleLarge)
                        Text("${selected.size} repository dipilih", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    }
                    IconAction(AetherIcon.Close, "Tutup", onDismiss)
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Cari repo...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
                when {
                    state.loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Terracotta)
                    }
                    state.error != null -> Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::load) { Text("Coba lagi") }
                    }
                    filtered.isEmpty() -> Text("Tidak ada repo yang cocok.", modifier = Modifier.padding(20.dp), color = palette.muted)
                    else -> LazyColumn(
                        Modifier.weight(1f, fill = false),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered, key = { it.owner + "/" + it.name }) { repo ->
                            val key = repo.toTargetKey()
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(if (key in selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clickable { selected = if (key in selected) selected - key else selected + key }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = key in selected, onCheckedChange = { checked ->
                                    selected = if (checked) selected + key else selected - key
                                })
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(repo.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (repo.isPrivate) RepoTag("Privat")
                                        if (!repo.canPush) RepoTag("Hanya baca")
                                    }
                                    Text(
                                        repo.owner + (repo.description?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        enabled = !state.loading && selected.isNotEmpty() && state.repos.isNotEmpty(),
                        onClick = { onConfirm(state.repos.filter { it.toTargetKey() in selected }) },
                    ) { Text("Gunakan ${selected.size} repo") }
                }
            }
        }
    }
}

@Composable
private fun RepoTag(text: String) {
    val palette = LocalAetherPalette.current
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = palette.muted,
        modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
