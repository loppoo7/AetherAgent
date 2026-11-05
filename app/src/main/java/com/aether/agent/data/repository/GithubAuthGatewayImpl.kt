package com.aether.agent.data.repository

import com.aether.agent.data.api.readableErrors
import com.aether.agent.domain.model.DeviceCode
import com.aether.agent.domain.model.GithubUser
import com.aether.agent.domain.model.PollResult
import com.aether.agent.domain.repository.GithubAuthGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.Parameters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class GithubAuthGatewayImpl @Inject constructor(private val http: HttpClient) : GithubAuthGateway {

    private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    override suspend fun requestDeviceCode(clientId: String): DeviceCode = readableErrors {
        val o: JsonObject = http.post("https://github.com/login/device/code") {
            header(HttpHeaders.Accept, "application/json")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("scope", "repo workflow")
            }))
        }.body()
        check(o.text("device_code").isNotBlank()) {
            "GitHub menolak permintaan: ${o.text("error_description").ifBlank { o.text("error") }}"
        }
        DeviceCode(
            deviceCode = o.text("device_code"),
            userCode = o.text("user_code"),
            verificationUri = o.text("verification_uri").ifBlank { "https://github.com/login/device" },
            intervalSeconds = o["interval"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 5,
            expiresInSeconds = o["expires_in"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 900,
        )
    }

    override suspend fun pollToken(clientId: String, deviceCode: String): PollResult = readableErrors {
        val o: JsonObject = http.post("https://github.com/login/oauth/access_token") {
            header(HttpHeaders.Accept, "application/json")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("device_code", deviceCode)
                append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            }))
        }.body()
        val token = o.text("access_token")
        if (token.isNotBlank()) return@readableErrors PollResult.Token(token)
        when (val error = o.text("error")) {
            "authorization_pending" -> PollResult.Pending
            "slow_down" -> PollResult.SlowDown
            "" -> PollResult.Failed("Respons tidak dikenal dari GitHub.")
            else -> PollResult.Failed(o.text("error_description").ifBlank { error })
        }
    }

    override suspend fun fetchUser(token: String): GithubUser = readableErrors {
        val o: JsonObject = http.get("https://api.github.com/user") {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("User-Agent", "AetherAgent-Android")
            header("X-GitHub-Api-Version", "2022-11-28")
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
        GithubUser(login = o.text("login"), avatarUrl = o.text("avatar_url"))
    }
}
