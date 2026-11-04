package com.aether.agent.domain.usecase

import com.aether.agent.domain.model.ChatSession
import com.aether.agent.domain.model.DEFAULT_SESSION_TITLE
import com.aether.agent.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatSessionsUseCase @Inject constructor(private val chat: ChatRepository) {
    fun observe(): Flow<List<ChatSession>> = chat.observeSessions()

    suspend fun create(): String = chat.createSession(DEFAULT_SESSION_TITLE)

    suspend fun rename(id: String, title: String) {
        chat.renameSession(id, title.trim().ifBlank { DEFAULT_SESSION_TITLE })
    }

    suspend fun delete(id: String) = chat.deleteSession(id)
}
