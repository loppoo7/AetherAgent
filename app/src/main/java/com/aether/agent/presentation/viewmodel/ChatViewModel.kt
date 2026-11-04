package com.aether.agent.presentation.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.Message
import com.aether.agent.domain.usecase.ObserveChatUseCase
import com.aether.agent.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
@HiltViewModel class ChatViewModel @Inject constructor(private val observeChat:ObserveChatUseCase,private val sendMessage:SendMessageUseCase):ViewModel(){
 private val sessionId=UUID.randomUUID().toString();private val _messages=MutableStateFlow<List<Message>>(emptyList());val messages=_messages.asStateFlow();private val _loading=MutableStateFlow(false);val loading=_loading.asStateFlow();private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
 init { viewModelScope.launch { observeChat(sessionId).collect { _messages.value=it } } }
 fun send(content:String){ if(content.isBlank()||_loading.value)return;viewModelScope.launch { _loading.value=true;_error.value=null;try{sendMessage(sessionId,content)}catch(e:Exception){_error.value=e.message ?: "Gagal mengirim pesan."}finally{_loading.value=false} } }
}
