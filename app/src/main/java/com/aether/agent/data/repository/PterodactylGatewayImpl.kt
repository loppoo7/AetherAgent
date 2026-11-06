package com.aether.agent.data.repository

import com.aether.agent.data.api.readableErrors
import com.aether.agent.domain.model.PteroFile
import com.aether.agent.domain.model.PteroServer
import com.aether.agent.domain.repository.PterodactylGateway
import com.aether.agent.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Client API Pterodactyl (HTTPS + API key). Endpoint mengikuti dokumentasi Client API. */
@Singleton
class PterodactylGatewayImpl @Inject constructor(
    private val http: HttpClient,
    private val settings: SettingsRepository,
) : PterodactylGateway {

    private suspend fun base(): Pair<String, String> {
        val s = settings.observeSettings().first()
        require(s.panelUrl.isNotBlank() && s.pteroApiKey.isNotBlank()) {
            "URL panel dan API key Pterodactyl belum diisi di Pengaturan."
        }
        return s.panelUrl.trim().trimEnd('/') to s.pteroApiKey.trim()
    }

    private fun HttpRequestBuilder.auth(key: String) {
        header(HttpHeaders.Authorization, "Bearer $key")
        header(HttpHeaders.Accept, "application/json")
    }

    private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private suspend fun requestServers(url: String, key: String): List<PteroServer> {
        val o: JsonObject = http.get("$url/api/client") { auth(key) }.body()
        return o["data"]?.jsonArray.orEmpty().mapNotNull { element ->
            val attributes = element.jsonObject["attributes"]?.jsonObject ?: return@mapNotNull null
            val identifier = attributes.text("identifier")
            if (identifier.isBlank()) return@mapNotNull null
            PteroServer(
                id = identifier,
                name = attributes.text("name").ifBlank { identifier },
                suspended = attributes["is_suspended"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }
    }

    override suspend fun testConnection(panelUrl: String, apiKey: String): List<PteroServer> = readableErrors {
        val url = panelUrl.trim().trimEnd('/')
        val key = apiKey.trim()
        require(url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) {
            "URL panel harus diawali http:// atau https://."
        }
        require(key.isNotBlank()) { "Client API Key belum diisi." }
        requestServers(url, key)
    }

    override suspend fun listServers(): List<PteroServer> = readableErrors {
        val (url, key) = base()
        requestServers(url, key)
    }

    override suspend fun serverState(serverId: String): String = readableErrors {
        val (url, key) = base()
        val o: JsonObject = http.get("$url/api/client/servers/$serverId/resources") { auth(key) }.body()
        o["attributes"]?.jsonObject?.text("current_state").orEmpty()
    }

    override suspend fun listFiles(serverId: String, dir: String): List<PteroFile> = readableErrors {
        val (url, key) = base()
        val o: JsonObject = http.get("$url/api/client/servers/$serverId/files/list") {
            auth(key)
            parameter("directory", dir.ifBlank { "/" })
        }.body()
        o["data"]?.jsonArray.orEmpty().map { e ->
            val a = e.jsonObject["attributes"]!!.jsonObject
            PteroFile(
                name = a.text("name"),
                isDir = a["is_file"]?.jsonPrimitive?.booleanOrNull == false,
                size = a["size"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }.sortedWith(compareByDescending<PteroFile> { it.isDir }.thenBy { it.name.lowercase() })
    }

    override suspend fun readFile(serverId: String, path: String): String = readableErrors {
        val (url, key) = base()
        http.get("$url/api/client/servers/$serverId/files/contents") {
            auth(key)
            parameter("file", path)
        }.bodyAsText()
    }

    override suspend fun writeFile(serverId: String, path: String, content: String) {
        readableErrors {
            val (url, key) = base()
            http.post("$url/api/client/servers/$serverId/files/write") {
                auth(key)
                parameter("file", path)
                contentType(ContentType.Text.Plain)
                setBody(content)
            }
        }
    }

    override suspend fun deletePaths(serverId: String, dir: String, names: List<String>) {
        readableErrors {
            val (url, key) = base()
            http.post("$url/api/client/servers/$serverId/files/delete") {
                auth(key)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("root", dir.ifBlank { "/" })
                        putJsonArray("files") { names.forEach { add(JsonPrimitive(it)) } }
                    }.toString(),
                )
            }
        }
    }

    override suspend fun createFolder(serverId: String, dir: String, name: String) {
        readableErrors {
            val (url, key) = base()
            http.post("$url/api/client/servers/$serverId/files/create-folder") {
                auth(key)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("root", dir.ifBlank { "/" }); put("name", name) }.toString())
            }
        }
    }

    override suspend fun rename(serverId: String, dir: String, from: String, to: String) {
        readableErrors {
            val (url, key) = base()
            http.put("$url/api/client/servers/$serverId/files/rename") {
                auth(key)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("root", dir.ifBlank { "/" })
                        putJsonArray("files") {
                            add(buildJsonObject { put("from", from); put("to", to) })
                        }
                    }.toString(),
                )
            }
        }
    }

    override suspend fun power(serverId: String, signal: String) {
        readableErrors {
            val (url, key) = base()
            http.post("$url/api/client/servers/$serverId/power") {
                auth(key)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("signal", signal) }.toString())
            }
        }
    }
}
