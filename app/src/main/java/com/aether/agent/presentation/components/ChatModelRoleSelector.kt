package com.aether.agent.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.agent.domain.model.AiRole
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta

/**
 * Compact model/role chips in the chat composer, matching the inline selector pattern.
 * Role selection can be disabled independently; model selection remains available.
 */
@Composable
fun ChatModelRoleSelector(
    providerLabel: String,
    model: String,
    modelChoices: List<String>,
    role: AiRole,
    roleSelectorEnabled: Boolean,
    onModelSelected: (String) -> Unit,
    onRoleSelected: (AiRole) -> Unit,
    onRoleSelectorEnabledChange: (Boolean) -> Unit,
) {
    val palette = LocalAetherPalette.current
    var modelExpanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Surface(
                color = palette.userBubble,
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.clickable { modelExpanded = true },
            ) {
                Text(
                    "$providerLabel · $model",
                    modifier = Modifier,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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

        if (roleSelectorEnabled) {
            Box {
                Surface(
                    color = Terracotta.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.clickable { roleExpanded = true },
                ) {
                    Text(
                        role.label,
                        modifier = Modifier,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        color = Terracotta,
                    )
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

        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        Text("Role", style = MaterialTheme.typography.labelSmall, color = palette.muted)
        Switch(
            checked = roleSelectorEnabled,
            onCheckedChange = onRoleSelectorEnabledChange,
        )
    }
}
