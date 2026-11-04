package com.aether.agent.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

class GithubApiService @Inject constructor(private val client: HttpClient) {
    suspend fun searchRepositories(query: String, token: String = ""): JsonObject =
        client.get("https://api.github.com/search/repositories") {
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            if (token.isNotBlank()) header("Authorization", "Bearer $token")
            url { parameters.append("q", query) }
        }.body()
}
