package com.aether.agent.domain.repository

interface GithubGateway {
    suspend fun listFiles(): List<String>
    suspend fun readFile(path: String): String?
    suspend fun writeFile(path: String, content: String, message: String): String
    suspend fun deleteFile(path: String, message: String): String
    suspend fun createBranch(name: String, from: String): String
    suspend fun createPullRequest(title: String, head: String, base: String, body: String): String
    suspend fun listCommits(limit: Int): List<String>
    suspend fun listIssues(limit: Int): List<String>
}
