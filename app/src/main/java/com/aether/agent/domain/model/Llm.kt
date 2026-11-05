package com.aether.agent.domain.model

import kotlinx.serialization.json.JsonObject

data class ToolSpec(val name: String, val description: String, val parameters: JsonObject)

data class ToolCall(val id: String, val name: String, val arguments: JsonObject)

sealed interface LlmMessage {
    data class User(val text: String) : LlmMessage
    data class Assistant(val text: String, val toolCalls: List<ToolCall> = emptyList()) : LlmMessage
    data class ToolResult(val callId: String, val name: String, val output: String) : LlmMessage
}

data class LlmReply(val text: String, val toolCalls: List<ToolCall>)
