package com.aether.agent.domain.repository

import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(sessionId: String): Flow<List<Message>>
    suspend fun save(sessionId: String, role: String, content: String)
    suspend fun recentHistory(sessionId: String, limit: Int = 30): List<LlmMessage>
    suspend fun clear(sessionId: String)
}
