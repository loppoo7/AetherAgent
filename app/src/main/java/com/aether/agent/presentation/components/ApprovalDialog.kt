package com.aether.agent.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.agent.presentation.viewmodel.PendingApproval

@Composable
fun ApprovalDialog(pending: PendingApproval, onResolve: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        title = { Text("Review perubahan") },
        text = {
            Column {
                Text(pending.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                DiffViewer(pending.diff, Modifier.fillMaxWidth().height(300.dp))
                Spacer(Modifier.height(6.dp))
                Text("Perubahan hanya dikirim setelah kamu mengonfirmasi.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onResolve(true) }) { Text("Commit & Push") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Request Revision") } },
    )
}
