package com.aether.agent.domain.repository

import com.aether.agent.domain.model.PteroFile
import com.aether.agent.domain.model.PteroServer

interface PterodactylGateway {
    suspend fun listServers(): List<PteroServer>
    suspend fun serverState(serverId: String): String
    suspend fun listFiles(serverId: String, dir: String): List<PteroFile>
    suspend fun readFile(serverId: String, path: String): String
    suspend fun writeFile(serverId: String, path: String, content: String)
    suspend fun deletePaths(serverId: String, dir: String, names: List<String>)
    suspend fun createFolder(serverId: String, dir: String, name: String)
    suspend fun rename(serverId: String, dir: String, from: String, to: String)
    /** signal: start, stop, restart, atau kill. */
    suspend fun power(serverId: String, signal: String)
}
