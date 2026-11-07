package com.aether.agent.data.repository

import com.aether.agent.data.local.db.ChatDao
import com.aether.agent.data.local.db.ChatSessionEntity
import com.aether.agent.data.local.db.MessageEntity
import com.aether.agent.data.mapper.toDomain
import com.aether.agent.domain.model.ChatSession
import com.aether.agent.domain.model.DEFAULT_SESSION_TITLE
import com.aether.agent.domain.model.EDITOR_SESSION_PREFIX
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

    override fun observeSessions(): Flow<List<ChatSession>> =
        dao.observeSessions().map { rows ->
            rows.filterNot { it.id.startsWith(EDITOR_SESSION_PREFIX) }
                .map { ChatSession(it.id, it.title, it.updatedAt) }
        }

    override suspend fun createSession(title: String): String {
        val id = UUID.randomUUID().toString()
        dao.upsertSession(ChatSessionEntity(id, title, System.currentTimeMillis()))
        return id
    }

    override suspend fun ensureSession(id: String, title: String) {
        if (dao.getSession(id) == null) dao.upsertSession(ChatSessionEntity(id, title, System.currentTimeMillis()))
    }

    override suspend fun renameSession(id: String, title: String) = dao.renameSession(id, title)

    override suspend fun deleteSession(id: String) {
        dao.clearMessages(id)
        dao.deleteSession(id)
    }

    override suspend fun save(sessionId: String, role: String, content: String) {
        val now = System.currentTimeMillis()
        dao.insert(MessageEntity(UUID.randomUUID().toString(), sessionId, role, content, now))
        // Pesan user pertama dipakai sebagai judul chat yang masih bernama default.
        val session = dao.getSession(sessionId)
        if (role == "user" && session != null && session.title == DEFAULT_SESSION_TITLE) {
            dao.renameSession(sessionId, content.lineSequence().first().take(40))
        }
        dao.touchSession(sessionId, now)
    }

    override suspend fun recentHistory(sessionId: String, limit: Int): List<LlmMessage> =
        dao.latest(sessionId, limit).reversed().map { row ->
            if (row.role == "user") LlmMessage.User(row.content) else LlmMessage.Assistant(row.content)
        }

    override suspend fun clear(sessionId: String) = dao.clearMessages(sessionId)
}
