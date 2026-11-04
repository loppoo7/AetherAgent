package com.aether.agent.domain.repository

import com.aether.agent.domain.model.DeviceCode
import com.aether.agent.domain.model.GithubUser
import com.aether.agent.domain.model.PollResult

interface GithubAuthGateway {
    suspend fun requestDeviceCode(clientId: String): DeviceCode
    suspend fun pollToken(clientId: String, deviceCode: String): PollResult
    suspend fun fetchUser(token: String): GithubUser
}
