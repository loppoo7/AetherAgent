package com.aether.agent.domain.repository

import com.aether.agent.domain.model.PteroFile

/** Akses file server lewat SFTP. Path relatif terhadap folder server (dimulai "/"). */
interface SftpGateway {
    /** Baca fingerprint host tanpa mengirim username/password. Host baru akan meminta persetujuan pengguna. */
    suspend fun inspectHostKey(host: String, port: Int, knownHostKey: String): String

    /** Login dan pastikan direktori server dapat diakses setelah fingerprint dipercaya. */
    suspend fun testConnection(
        serverId: String,
        host: String,
        port: Int,
        username: String,
        password: String,
        knownHostKey: String,
    )

    suspend fun listFiles(serverId: String, dir: String): List<PteroFile>
    suspend fun readFile(serverId: String, path: String): String
    suspend fun writeFile(serverId: String, path: String, content: String)
    suspend fun deletePaths(serverId: String, dir: String, names: List<String>)
    suspend fun createFolder(serverId: String, dir: String, name: String)
    suspend fun rename(serverId: String, dir: String, from: String, to: String)
}
