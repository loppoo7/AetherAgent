package com.aether.agent.data.repository
import com.aether.agent.data.api.LlmApiService
import com.aether.agent.data.api.dto.ChatMessageDto
import com.aether.agent.data.api.dto.ChatRequestDto
import com.aether.agent.data.local.db.ChatDao
import com.aether.agent.data.local.db.MessageEntity
import com.aether.agent.data.mapper.toDomain
import com.aether.agent.domain.model.Message
import com.aether.agent.domain.repository.ChatRepository
import com.aether.agent.domain.repository.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
@Singleton class ChatRepositoryImpl @Inject constructor(private val dao:ChatDao,private val llm:LlmApiService,private val settings:SettingsRepository):ChatRepository {
 override fun observeMessages(sessionId:String):Flow<List<Message>> = dao.observeMessages(sessionId).map { rows -> rows.map { it.toDomain() } }
 override suspend fun sendMessage(sessionId:String,content:String):Message {
  require(content.isNotBlank()) { "Pesan tidak boleh kosong." }; val cfg=settings.observeSettings().first()
  require(cfg.baseUrl.isNotBlank() && cfg.model.isNotBlank()) { "Base URL dan model wajib diatur." }
  val history=dao.observeMessages(sessionId).first()
  val user=MessageEntity(UUID.randomUUID().toString(),sessionId,"user",content.trim(),System.currentTimeMillis());dao.insert(user)
  val prompt=history.map { ChatMessageDto(it.role,it.content) } + ChatMessageDto("user",content.trim())
  val answer=llm.complete(cfg.baseUrl,cfg.apiKey,ChatRequestDto(cfg.model,prompt)).choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() } ?: "Model tidak mengembalikan jawaban."
  val assistant=MessageEntity(UUID.randomUUID().toString(),sessionId,"assistant",answer,System.currentTimeMillis());dao.insert(assistant);return assistant.toDomain()
 }
}
