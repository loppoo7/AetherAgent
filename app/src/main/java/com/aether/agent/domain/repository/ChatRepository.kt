package com.aether.agent.domain.repository

import com.aether.agent.domain.model.ChatSession
import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(sessionId: String): Flow<List<Message>>
    fun observeSessions(): Flow<List<ChatSession>>
    suspend fun createSession(title: String): String
    suspend fun ensureSession(id: String, title: String)
    suspend fun renameSession(id: String, title: String)
    suspend fun deleteSession(id: String)
    suspend fun save(sessionId: String, role: String, content: String)
    suspend fun recentHistory(sessionId: String, limit: Int = 30): List<LlmMessage>
    suspend fun clear(sessionId: String)
}
