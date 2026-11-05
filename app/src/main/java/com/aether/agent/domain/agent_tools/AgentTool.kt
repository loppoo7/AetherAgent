package com.aether.agent.domain.agent_tools

import kotlinx.serialization.json.JsonObject

interface AgentTool {
    val name: String
    val description: String
    /** JSON Schema parameter (object). */
    val parameters: JsonObject
    /** Aksi yang mengubah repo harus disetujui pengguna jika confirmWrites aktif. */
    val requiresApproval: Boolean get() = false
    /** Tool file server Pterodactyl, diatur oleh mode Agen AI, bukan peran. */
    val isServerTool: Boolean get() = false

    /** Label singkat untuk action card, contoh: "Menulis app/Main.kt". */
    fun describe(args: JsonObject): String
    /** Diff yang ditampilkan di dialog persetujuan. */
    suspend fun preview(args: JsonObject): String = ""
    suspend fun execute(args: JsonObject): String
}
