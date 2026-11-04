package com.aether.agent.data.mapper
import com.aether.agent.data.local.db.MessageEntity
import com.aether.agent.domain.model.Message
fun MessageEntity.toDomain()=Message(id,sessionId,role,content,createdAt)
fun Message.toEntity()=MessageEntity(id,sessionId,role,content,createdAt)
