package com.aether.agent.data.api

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Ubah respons HTTP menjadi pesan yang bisa dipahami, bukan menampilkan JSON mentah ke UI. */
suspend fun <T> readableErrors(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ResponseException) {
        val body = runCatching { e.response.bodyAsText() }.getOrDefault("")
        val details = runCatching {
            Json.parseToJsonElement(body).jsonObject["errors"]?.jsonArray
                ?.mapNotNull { item ->
                    item.jsonObject["detail"]?.jsonPrimitive?.contentOrNull
                        ?: item.jsonObject["title"]?.jsonPrimitive?.contentOrNull
                }
                ?.distinct()
                ?.joinToString("; ")
        }.getOrNull().orEmpty()

        val message = when (e.response.status.value) {
            401 -> "Client API Key ditolak (HTTP 401). Pastikan key berasal dari menu Account API di panel dan masih aktif."
            403 -> "Akses Client API ditolak (HTTP 403). Periksa izin akun atau server pada panel."
            404 -> "Endpoint panel tidak ditemukan (HTTP 404). Periksa URL panel dan dukungan Client API."
            429 -> "Terlalu banyak permintaan ke panel (HTTP 429). Coba lagi beberapa saat."
            in 500..599 -> "Panel sedang mengalami gangguan (HTTP ${e.response.status.value}). Coba lagi nanti."
            else -> details.ifBlank { "Permintaan ke panel gagal (HTTP ${e.response.status.value})." }
        }
        throw IllegalStateException(message, e)
    }
