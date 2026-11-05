package com.aether.agent.data.api.github

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class GithubConfig(val token: String, val owner: String, val repo: String, val branch: String)

@Singleton
class GithubClient @Inject constructor(private val http: HttpClient) {
    private val api = "https://api.github.com"

    private fun HttpRequestBuilder.github(token: String) {
        header(HttpHeaders.Accept, "application/vnd.github+json")
        header("User-Agent", "AetherAgent-Android")
        header("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun repo(c: GithubConfig) = "$api/repos/${c.owner}/${c.repo}"

    private fun encodePath(path: String) = path.trim('/').split('/')
        .joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    suspend fun tree(c: GithubConfig): JsonObject =
        http.get("${repo(c)}/git/trees/${c.branch}?recursive=1") { github(c.token) }.body<JsonObject>()

    /** Mengembalikan null jika file tidak ada (404). */
    suspend fun file(c: GithubConfig, path: String): JsonObject? = try {
        http.get("${repo(c)}/contents/${encodePath(path)}?ref=${c.branch}") { github(c.token) }.body<JsonObject>()
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) null else throw e
    }

    suspend fun putFile(c: GithubConfig, path: String, content: String, message: String, sha: String?): JsonObject =
        http.put("${repo(c)}/contents/${encodePath(path)}") {
            github(c.token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("message", message)
                put("content", Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                put("branch", c.branch)
                if (sha != null) put("sha", sha)
            }.toString())
        }.body<JsonObject>()

    suspend fun deleteFile(c: GithubConfig, path: String, message: String, sha: String) {
        http.delete("${repo(c)}/contents/${encodePath(path)}") {
            github(c.token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("message", message)
                put("sha", sha)
                put("branch", c.branch)
            }.toString())
        }
    }

    suspend fun branchSha(c: GithubConfig, branch: String): String {
        val o: JsonObject = http.get("${repo(c)}/git/ref/heads/$branch") { github(c.token) }.body()
        return o["object"]!!.jsonObject["sha"]!!.jsonPrimitive.content
    }

    suspend fun createBranch(c: GithubConfig, name: String, sha: String) {
        http.post("${repo(c)}/git/refs") {
            github(c.token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("ref", "refs/heads/$name")
                put("sha", sha)
            }.toString())
        }
    }

    suspend fun createPull(c: GithubConfig, title: String, head: String, base: String, body: String): JsonObject =
        http.post("${repo(c)}/pulls") {
            github(c.token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("title", title)
                put("head", head)
                put("base", base)
                put("body", body)
            }.toString())
        }.body<JsonObject>()

    suspend fun userRepos(token: String, page: Int): JsonArray =
        http.get("$api/user/repos?per_page=100&page=$page&sort=updated&affiliation=owner,collaborator,organization_member") {
            github(token)
        }.body<JsonArray>()

    suspend fun commits(c: GithubConfig, limit: Int): JsonArray =
        http.get("${repo(c)}/commits?sha=${c.branch}&per_page=$limit") { github(c.token) }.body<JsonArray>()

    suspend fun issues(c: GithubConfig, limit: Int): JsonArray =
        http.get("${repo(c)}/issues?state=open&per_page=$limit") { github(c.token) }.body<JsonArray>()
}
