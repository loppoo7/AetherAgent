package com.aether.agent.domain.model

const val DEFAULT_SESSION_TITLE = "Chat baru"

data class ChatSession(val id: String, val title: String, val updatedAt: Long)
