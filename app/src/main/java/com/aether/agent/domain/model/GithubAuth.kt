package com.aether.agent.domain.model

/** Client ID publik OAuth App. Client secret tidak dipakai di device flow dan tidak boleh ditaruh di app. */
const val GITHUB_CLIENT_ID = "Ov23liAknfj7zDqqwahA"

data class DeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int,
)

sealed interface PollResult {
    data class Token(val accessToken: String) : PollResult
    data object Pending : PollResult
    data object SlowDown : PollResult
    data class Failed(val reason: String) : PollResult
}

data class GithubUser(val login: String, val avatarUrl: String)
