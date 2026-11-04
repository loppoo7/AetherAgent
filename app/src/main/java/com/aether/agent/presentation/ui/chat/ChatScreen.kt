package com.aether.agent.presentation.ui.chat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.presentation.components.ChatInput
import com.aether.agent.presentation.components.MessageBubble
import com.aether.agent.presentation.viewmodel.ChatViewModel
@Composable fun ChatScreen(onSettings:()->Unit,viewModel:ChatViewModel=hiltViewModel()){
 val messages by viewModel.messages.collectAsState();val loading by viewModel.loading.collectAsState();val error by viewModel.error.collectAsState()
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("AetherAgent",style=MaterialTheme.typography.headlineSmall);TextButton(onClick=onSettings){Text("Pengaturan")}}
  LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){if(messages.isEmpty())item{Text("Mulai percakapan dengan AetherAgent.")};items(messages,key={it.id}){MessageBubble(it)};if(loading)item{CircularProgressIndicator()} }
  error?.let{Text("Error: $it",color=MaterialTheme.colorScheme.error)};ChatInput(enabled=!loading,onSend=viewModel::send)
 }
}
