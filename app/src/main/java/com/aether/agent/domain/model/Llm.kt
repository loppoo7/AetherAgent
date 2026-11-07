package com.aether.agent.domain.model

import kotlinx.serialization.json.JsonObject

data class ToolSpec(val name: String, val description: String, val parameters: JsonObject)

/** [thoughtSignature] wajib dikirim balik ke Gemini pada pesan berikutnya, kalau tidak API menolak dengan HTTP 400. */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject,
    val thoughtSignature: String? = null,
)

sealed interface LlmMessage {
    data class User(val text: String) : LlmMessage
    data class Assistant(val text: String, val toolCalls: List<ToolCall> = emptyList()) : LlmMessage
    data class ToolResult(val callId: String, val name: String, val output: String) : LlmMessage
}

data class LlmReply(val text: String, val toolCalls: List<ToolCall>)

/** Kunci penanda: argumen tool dari model tidak valid JSON. Nilainya berisi teks mentah. */
const val ARGS_PARSE_ERROR_KEY = "__args_parse_error"
