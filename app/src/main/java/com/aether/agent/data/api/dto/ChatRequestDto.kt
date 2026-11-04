package com.aether.agent.data.api.dto

import kotlinx.serialization.Serializable

@Serializable data class ChatRequestDto(val model: String, val messages: List<ChatMessageDto>, val temperature: Double = 0.7)
@Serializable data class ChatMessageDto(val role: String, val content: String)
