package com.aether.agent.presentation.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.presentation.components.AgentActionCard
import com.aether.agent.presentation.components.ApprovalDialog
import com.aether.agent.presentation.components.ChatInput
import com.aether.agent.presentation.components.MessageBubble
import com.aether.agent.presentation.viewmodel.ChatViewModel

@Composable
fun ChatScreen(onSettings: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val running by viewModel.running.collectAsState()
    val error by viewModel.error.collectAsState()
    val approval by viewModel.approval.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, cards.size, running) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AetherAgent", style = MaterialTheme.typography.headlineSmall)
            Row {
                TextButton(onClick = viewModel::clear) { Text("Hapus") }
                TextButton(onClick = onSettings) { Text("Pengaturan") }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty() && cards.isEmpty() && !running) {
                item {
                    Text(
                        "Mulai percakapan. Coba: \"baca struktur repo lalu jelaskan arsitekturnya\".",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(messages, key = { it.id }) { MessageBubble(it) }
            items(cards, key = { it.id }) { AgentActionCard(it) }
            if (running) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        if (running) TextButton(onClick = viewModel::stop) { Text("Hentikan") }
        ChatInput(enabled = !running, onSend = viewModel::send)
    }

    approval?.let { pending ->
        ApprovalDialog(pending = pending, onResolve = viewModel::resolveApproval)
    }
}
