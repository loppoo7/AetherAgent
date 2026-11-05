package com.aether.agent.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.aether.agent.domain.model.AiRole

@Composable
fun ChatModelRoleSelector(
    providerLabel: String,
    model: String,
    modelChoices: List<String>,
    role: AiRole,
    onModelSelected: (String) -> Unit,
    onRoleSelected: (AiRole) -> Unit,
) {
    var modelExpanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            TextButton(onClick = { modelExpanded = true }) {
                Text("$providerLabel · $model", maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
            DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                modelChoices.distinct().forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice) },
                        onClick = { modelExpanded = false; onModelSelected(choice) },
                    )
                }
            }
        }
        Box {
            TextButton(onClick = { roleExpanded = true }) {
                Text(role.label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
            DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                AiRole.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label) },
                        onClick = { roleExpanded = false; onRoleSelected(item) },
                    )
                }
            }
        }
    }
}
