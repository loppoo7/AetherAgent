package com.aether.agent.data.api.llm

import io.ktor.client.request.preparePost
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.json.intOrNull

import io.ktor.client.plugins.timeout

import com.aether.agent.domain.model.ARGS_PARSE_ERROR_KEY

import com.aether.agent.data.api.readableErrors
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.ApiStyle
import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.model.LlmReply
import com.aether.agent.domain.model.ToolCall
import com.aether.agent.domain.model.ToolSpec
import com.aether.agent.domain.repository.LlmGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Model reasoning bisa berpikir bermenit-menit. Tanpa streaming, batas ini berlaku untuk seluruh jawaban (20 menit). */
private const val LLM_TIMEOUT_MS = 1_200_000L

/** Satu gateway untuk dua gaya API: OpenAI-compatible (OpenAI, DeepSeek, OpenRouter) dan Anthropic Messages. */
@Singleton
class LlmGatewayImpl @Inject constructor(private val http: HttpClient) : LlmGateway {

    override suspend fun complete(
        settings: AgentSettings,
        system: String,
        messages: List<LlmMessage>,
        tools: List<ToolSpec>,
        onStatus: suspend (String) -> Unit,
    ): LlmReply {
        require(settings.apiKey.isNotBlank()) { "API key ${settings.provider.label} belum diisi di Pengaturan." }
        onStatus("Berpikir…")
        return readableErrors {
            when (settings.provider.style) {
                ApiStyle.OPENAI_COMPAT -> try {
                    openAiCompatible(settings, system, messages, tools, onStatus = onStatus)
                } catch (e: ResponseException) {
                    val detail = runCatching { e.response.bodyAsText() }.getOrDefault("")
                    if (e.response.status.value == 400 && detail.contains("temperature", ignoreCase = true)) {
                        // Sebagian model menolak parameter temperature; ulangi tanpa itu.
                        openAiCompatible(settings, system, messages, tools, useTemperature = false, onStatus = onStatus)
                    } else {
                        throw IllegalStateException("HTTP ${e.response.status.value}: ${detail.take(300)}", e)
                    }
                }
                ApiStyle.ANTHROPIC -> anthropic(settings, system, messages, tools)
            }
        }
    }

    private suspend fun openAiCompatible(
        s: AgentSettings,
        system: String,
        messages: List<LlmMessage>,
        tools: List<ToolSpec>,
        useTemperature: Boolean = true,
        onStatus: suspend (String) -> Unit = {},
    ): LlmReply {
        val body = buildJsonObject {
            put("model", s.model)
            if (useTemperature) put("temperature", s.answerStyle.temperature)
            put("stream", true)
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                messages.forEach { add(it.toOpenAi()) }
            }
            if (tools.isNotEmpty()) {
                putJsonArray("tools") {
                    tools.forEach { t ->
                        add(buildJsonObject {
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", t.name)
                                put("description", t.description)
                                put("parameters", t.parameters)
                            }
                        })
                    }
                }
            }
        }
        val calls = mutableMapOf<Int, PartialCall>()
        val text = StringBuilder()
        var statusSent = false
        http.preparePost(s.provider.baseUrl.trimEnd('/') + "/chat/completions") {
            timeout { requestTimeoutMillis = LLM_TIMEOUT_MS; socketTimeoutMillis = LLM_TIMEOUT_MS }
            header(HttpHeaders.Authorization, "Bearer ${s.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val chunk = runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: continue
                val delta = chunk["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject ?: continue

                delta["content"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { piece ->
                    if (!statusSent) {
                        statusSent = true
                        onStatus("Menulis jawaban…")
                    }
                    text.append(piece)
                }
                delta["tool_calls"]?.jsonArray?.forEach { tc ->
                    val o = tc.jsonObject
                    val index = o["index"]?.jsonPrimitive?.intOrNull ?: 0
                    val partial = calls.getOrPut(index) { PartialCall() }
                    o["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { partial.id = it }
                    val fn = o["function"]?.jsonObject
                    fn?.get("name")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() && it != partial.name }?.let { name ->
                        partial.name = name
                        onStatus("Menyiapkan tool: $name")
                    }
                    fn?.get("arguments")?.jsonPrimitive?.contentOrNull?.let { partial.args.append(it) }
                    o["extra_content"]?.jsonObject?.get("google")?.jsonObject?.get("thought_signature")
                        ?.jsonPrimitive?.contentOrNull?.let { partial.signature = it }
                }
            }
        }
        return LlmReply(text.toString(), calls.toSortedMap().map { (index, p) -> p.toToolCall(index) })
    }

    private suspend fun anthropic(
        s: AgentSettings,
        system: String,
        messages: List<LlmMessage>,
        tools: List<ToolSpec>,
    ): LlmReply {
        val body = buildJsonObject {
            put("model", s.model)
            put("max_tokens", 4096)
            put("temperature", minOf(s.answerStyle.temperature, 1.0))
            put("system", system)
            putJsonArray("messages") { anthropicMessages(messages).forEach { add(it) } }
            if (tools.isNotEmpty()) {
                putJsonArray("tools") {
                    tools.forEach { t ->
                        add(buildJsonObject {
                            put("name", t.name)
                            put("description", t.description)
                            put("input_schema", t.parameters)
                        })
                    }
                }
            }
        }
        val res = http.post(s.provider.baseUrl.trimEnd('/') + "/messages") {
            timeout { requestTimeoutMillis = LLM_TIMEOUT_MS; socketTimeoutMillis = LLM_TIMEOUT_MS }
            header("x-api-key", s.apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.body<JsonObject>()

        val blocks = res["content"]?.jsonArray ?: error("Respons tidak valid: $res")
        val text = blocks.filter { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }
            .joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }
        val calls = blocks.filter { it.jsonObject["type"]?.jsonPrimitive?.content == "tool_use" }.map { b ->
            val o = b.jsonObject
            ToolCall(
                id = (o["id"] ?: error("ID tool call kosong.")).jsonPrimitive.content,
                name = (o["name"] ?: error("Nama tool_use kosong.")).jsonPrimitive.content,
                arguments = o["input"]?.jsonObject ?: JsonObject(emptyMap()),
            )
        }
        return LlmReply(text, calls)
    }

    private fun LlmMessage.toOpenAi(): JsonObject = when (val m = this) {
        is LlmMessage.User -> buildJsonObject {
            put("role", "user")
            put("content", m.text)
        }
        is LlmMessage.Assistant -> buildJsonObject {
            put("role", "assistant")
            put("content", m.text)
            if (m.toolCalls.isNotEmpty()) {
                putJsonArray("tool_calls") {
                    m.toolCalls.forEach { c ->
                        add(buildJsonObject {
                            put("id", c.id)
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", c.name)
                                put("arguments", c.arguments[ARGS_PARSE_ERROR_KEY]?.jsonPrimitive?.content ?: c.arguments.toString())
                            }
                            c.thoughtSignature?.let { sig ->
                                putJsonObject("extra_content") {
                                    putJsonObject("google") { put("thought_signature", sig) }
                                }
                            }
                        })
                    }
                }
            }
        }
        is LlmMessage.ToolResult -> buildJsonObject {
            put("role", "tool")
            put("tool_call_id", m.callId)
            put("content", m.output)
        }
    }

    /** Anthropic mewajibkan role bergantian; hasil tool digabung ke pesan user yang sama. */
    private fun anthropicMessages(messages: List<LlmMessage>): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun push(role: String, blocks: List<JsonObject>) {
            val last = out.lastOrNull()
            if (last != null && last["role"]?.jsonPrimitive?.content == role) {
                val merged: List<JsonElement> = last["content"]?.jsonArray.orEmpty() + blocks
                out[out.lastIndex] = message(role, merged)
            } else {
                out += message(role, blocks)
            }
        }
        for (m in messages) {
            when (m) {
                is LlmMessage.User -> push("user", listOf(textBlock(m.text)))
                is LlmMessage.Assistant -> {
                    val blocks = buildList<JsonObject> {
                        if (m.text.isNotBlank()) add(textBlock(m.text))
                        m.toolCalls.forEach { c ->
                            add(buildJsonObject {
                                put("type", "tool_use")
                                put("id", c.id)
                                put("name", c.name)
                                put("input", c.arguments)
                            })
                        }
                    }
                    push("assistant", blocks.ifEmpty { listOf(textBlock("(kosong)")) })
                }
                is LlmMessage.ToolResult -> push("user", listOf(buildJsonObject {
                    put("type", "tool_result")
                    put("tool_use_id", m.callId)
                    put("content", m.output)
                }))
            }
        }
        return out
    }

    private fun message(role: String, content: List<JsonElement>): JsonObject = buildJsonObject {
        put("role", role)
        put("content", JsonArray(content))
    }

    private fun textBlock(text: String): JsonObject = buildJsonObject {
        put("type", "text")
        put("text", text)
    }

    private fun PartialCall.toToolCall(index: Int): ToolCall = ToolCall(
        id = id.ifBlank { "call_$index" },
        name = name,
        arguments = parseArgs(args.toString()),
        thoughtSignature = signature,
    )

    private fun parseArgs(raw: String?): JsonObject =
        runCatching { Json.parseToJsonElement(raw?.takeIf { it.isNotBlank() } ?: "{}").jsonObject }
            .getOrElse { buildJsonObject { put(ARGS_PARSE_ERROR_KEY, raw.orEmpty()) } }
}

/** Potongan pemanggilan tool yang sedang dirangkai dari stream. */
private class PartialCall {
    var id: String = ""
    var name: String = ""
    val args = StringBuilder()
    var signature: String? = null
}
