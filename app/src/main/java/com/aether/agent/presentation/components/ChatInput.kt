package com.aether.agent.presentation.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
@Composable fun ChatInput(enabled:Boolean,onSend:(String)->Unit){ var text by remember{mutableStateOf("")};Row(Modifier.fillMaxWidth()){OutlinedTextField(value=text,onValueChange={text=it},modifier=Modifier.weight(1f),enabled=enabled,placeholder={Text("Tulis pesan...")},maxLines=4);Button(onClick={val v=text.trim();if(v.isNotEmpty()){onSend(v);text=""}},enabled=enabled&&text.isNotBlank()){Text("Kirim")} } }
