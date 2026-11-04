package com.aether.agent.presentation.components

import com.aether.agent.presentation.ui.theme.AetherMono

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.agent.presentation.viewmodel.PendingApproval

/** Dialog konfirmasi sebelum agen menulis atau commit. Menampilkan diff baris. */
@Composable
fun ApprovalDialog(pending: PendingApproval, onResolve: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        title = { Text("Setujui perubahan?") },
        text = {
            Column {
                Text(pending.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                ) {
                    if (pending.diff.isBlank()) {
                        Text("(tidak ada perubahan teks)", fontSize = 12.sp)
                    } else {
                        pending.diff.lines().forEach { line ->
                            Text(
                                line,
                                fontFamily = AetherMono,
                                fontSize = 12.sp,
                                color = when {
                                    line.startsWith("+") -> Color(0xFF22C55E)
                                    line.startsWith("-") -> Color(0xFFEF4444)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onResolve(true) }) { Text("Setujui") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Tolak") } },
    )
}
