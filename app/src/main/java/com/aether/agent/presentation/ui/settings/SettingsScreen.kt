package com.aether.agent.presentation.ui.settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.presentation.viewmodel.SettingsViewModel
@Composable fun SettingsScreen(onBack:()->Unit,viewModel:SettingsViewModel=hiltViewModel()){
 val current by viewModel.settings.collectAsState();var baseUrl by remember(current.baseUrl){mutableStateOf(current.baseUrl)};var apiKey by remember(current.apiKey){mutableStateOf(current.apiKey)};var model by remember(current.model){mutableStateOf(current.model)};var githubToken by remember(current.githubToken){mutableStateOf(current.githubToken)}
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Pengaturan koneksi",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(baseUrl,{baseUrl=it},Modifier.fillMaxWidth(),label={Text("Base URL")},singleLine=true);OutlinedTextField(apiKey,{apiKey=it},Modifier.fillMaxWidth(),label={Text("API key LLM")},singleLine=true);OutlinedTextField(model,{model=it},Modifier.fillMaxWidth(),label={Text("Model")},singleLine=true);OutlinedTextField(githubToken,{githubToken=it},Modifier.fillMaxWidth(),label={Text("GitHub token (opsional)")},singleLine=true);Button(onClick={viewModel.save(AgentSettings(baseUrl,apiKey,model,githubToken));onBack()},modifier=Modifier.fillMaxWidth()){Text("Simpan")};TextButton(onClick=onBack){Text("Kembali")} }
}
