package com.aether.agent.domain.repository
import com.aether.agent.domain.model.Message
import kotlinx.coroutines.flow.Flow
interface ChatRepository { fun observeMessages(sessionId:String):Flow<List<Message>>; suspend fun sendMessage(sessionId:String,content:String):Message }
