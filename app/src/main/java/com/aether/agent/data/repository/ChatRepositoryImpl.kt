package com.aether.agent.data.repository

import com.aether.agent.data.local.db.ChatDao
import com.aether.agent.data.local.db.MessageEntity
import com.aether.agent.data.mapper.toDomain
import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.model.Message
import com.aether.agent.domain.repository.ChatRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ChatRepositoryImpl @Inject constructor(private val dao: ChatDao) : ChatRepository {

    override fun observeMessages(sessionId: String): Flow<List<Message>> =
        dao.observeMessages(sessionId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(sessionId: String, role: String, content: String) {
        dao.insert(MessageEntity(UUID.randomUUID().toString(), sessionId, role, content, System.currentTimeMillis()))
    }

    override suspend fun recentHistory(sessionId: String, limit: Int): List<LlmMessage> =
        dao.latest(sessionId, limit).reversed().map { row ->
            if (row.role == "user") LlmMessage.User(row.content) else LlmMessage.Assistant(row.content)
        }

    override suspend fun clear(sessionId: String) = dao.clearSession(sessionId)
}
