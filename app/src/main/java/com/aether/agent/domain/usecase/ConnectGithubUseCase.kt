package com.aether.agent.domain.usecase

import com.aether.agent.domain.model.GITHUB_CLIENT_ID
import com.aether.agent.domain.model.PollResult
import com.aether.agent.domain.repository.GithubAuthGateway
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

sealed interface GithubLoginState {
    data object Requesting : GithubLoginState
    data class WaitingForUser(val userCode: String, val verificationUri: String) : GithubLoginState
    data class Connected(val login: String) : GithubLoginState
    data class Failed(val message: String) : GithubLoginState
}

/** Device flow: tampilkan kode, lalu polling sampai pengguna menyetujui di browser. */
class ConnectGithubUseCase @Inject constructor(
    private val auth: GithubAuthGateway,
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<GithubLoginState> = flow {
        emit(GithubLoginState.Requesting)
        val code = auth.requestDeviceCode(GITHUB_CLIENT_ID)
        emit(GithubLoginState.WaitingForUser(code.userCode, code.verificationUri))

        var interval = maxOf(5L, code.intervalSeconds.toLong())
        val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000)
            when (val result = auth.pollToken(GITHUB_CLIENT_ID, code.deviceCode)) {
                is PollResult.Token -> {
                    val user = auth.fetchUser(result.accessToken)
                    val current = settings.observeSettings().first()
                    settings.update(
                        current.copy(
                            githubToken = result.accessToken,
                            githubLogin = user.login,
                            githubAvatarUrl = user.avatarUrl,
                        ),
                    )
                    emit(GithubLoginState.Connected(user.login))
                    return@flow
                }
                PollResult.Pending -> Unit
                PollResult.SlowDown -> interval += 5
                is PollResult.Failed -> {
                    emit(GithubLoginState.Failed(result.reason))
                    return@flow
                }
            }
        }
        emit(GithubLoginState.Failed("Kode sudah kedaluwarsa. Coba hubungkan lagi."))
    }
}
