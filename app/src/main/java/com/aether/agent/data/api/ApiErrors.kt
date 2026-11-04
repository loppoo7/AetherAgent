package com.aether.agent.data.api

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText

/** Ubah error HTTP menjadi pesan yang bisa dibaca pengguna, lengkap dengan potongan body respons. */
suspend fun <T> readableErrors(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ResponseException) {
        val detail = runCatching { e.response.bodyAsText() }.getOrDefault("").take(300)
        throw IllegalStateException("HTTP ${e.response.status.value}: $detail", e)
    }
