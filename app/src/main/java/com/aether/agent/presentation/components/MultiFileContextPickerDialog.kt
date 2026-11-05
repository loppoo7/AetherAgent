package com.aether.agent.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.AttachedFileContext
import com.aether.agent.presentation.viewmodel.ContextPickerViewModel
import kotlinx.coroutines.launch

@Composable
fun MultiFileContextPickerDialog(
    targets: Set<String>,
    onDismiss: () -> Unit,
    onAttach: (List<AttachedFileContext>) -> Unit,
    viewModel: ContextPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(targets) { viewModel.load(targets) }
    val filtered = state.files.filter {
        state.query.isBlank() || it.path.contains(state.query, true) || it.repository.contains(state.query, true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lampirkan konteks file") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pilih hingga 20 file dari repository yang dipilih.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Cari file atau repository") },
                )
                when {
                    state.loading || state.reading -> Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(if (state.reading) "Membaca file..." else "Memuat pohon repository...")
                    }
                    state.error != null -> Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    filtered.isEmpty() -> Text("Tidak ada file yang cocok.")
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.id }) { file ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = file.id in state.selected,
                                    onCheckedChange = { viewModel.toggle(file.id) },
                                )
                                Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
                                    Text(file.path, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        file.repository,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Text("${state.selected.size}/20 file dipilih", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            Button(
                enabled = state.selected.isNotEmpty() && !state.loading && !state.reading,
                onClick = {
                    scope.launch {
                        runCatching { viewModel.readSelected() }
                            .onSuccess { onAttach(it); onDismiss() }
                    }
                },
            ) { Text("Lampirkan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
