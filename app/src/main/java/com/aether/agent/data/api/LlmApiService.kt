package com.aether.agent.data.api

import com.aether.agent.data.api.dto.ChatRequestDto
import com.aether.agent.data.api.dto.ChatResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class LlmApiService @Inject constructor(private val client: HttpClient) {
    suspend fun complete(baseUrl: String, apiKey: String, request: ChatRequestDto): ChatResponseDto =
        client.post(baseUrl.trimEnd('/') + "/chat/completions") {
            contentType(ContentType.Application.Json)
            if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            setBody(request)
        }.body()
}
