package com.aether.agent.domain.repository

import com.aether.agent.domain.model.PteroFile

/** Akses file server lewat SFTP. Path relatif terhadap folder server (dimulai "/"). */
interface SftpGateway {
    /** Langkah 1: baca fingerprint host. Tidak mengirim kredensial. */
    suspend fun fetchFingerprint(host: String, port: Int): String

    /** Langkah 3: login dengan fingerprint yang sudah disetujui, lalu baca folder server. */
    suspend fun checkLogin(
        host: String,
        port: Int,
        username: String,
        password: String,
        serverId: String,
        pinnedFingerprint: String,
    ): String

    suspend fun listFiles(serverId: String, dir: String): List<PteroFile>
    suspend fun readFile(serverId: String, path: String): String
    suspend fun writeFile(serverId: String, path: String, content: String)
    suspend fun deletePaths(serverId: String, dir: String, names: List<String>)
    suspend fun createFolder(serverId: String, dir: String, name: String)
    suspend fun rename(serverId: String, dir: String, from: String, to: String)
}
