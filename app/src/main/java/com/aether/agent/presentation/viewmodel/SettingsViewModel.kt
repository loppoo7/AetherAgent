package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.repository.PterodactylGateway
import com.aether.agent.domain.repository.SettingsRepository
import com.aether.agent.domain.repository.SftpGateway
import com.aether.agent.domain.usecase.ConnectGithubUseCase
import com.aether.agent.domain.usecase.GithubLoginState
import com.aether.agent.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ClientApiCheckState {
    data object Idle : ClientApiCheckState
    data object Checking : ClientApiCheckState
    data class Success(val message: String) : ClientApiCheckState
    data class Failure(val message: String) : ClientApiCheckState
}

sealed interface SftpCheckState {
    data object Idle : SftpCheckState
    data object Checking : SftpCheckState
    data class AwaitingTrust(
        val fingerprint: String,
        val serverId: String,
        val serverName: String,
    ) : SftpCheckState
    data class Connected(val fingerprint: String, val serverName: String) : SftpCheckState
    data class Failure(val message: String) : SftpCheckState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val connectGithubUseCase: ConnectGithubUseCase,
    private val pterodactyl: PterodactylGateway,
    private val sftp: SftpGateway,
) : ViewModel() {

    val settings: StateFlow<AgentSettings> =
        repository.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentSettings())

    private val _login = MutableStateFlow<GithubLoginState?>(null)
    val login: StateFlow<GithubLoginState?> = _login.asStateFlow()

    private val _clientApiCheck = MutableStateFlow<ClientApiCheckState>(ClientApiCheckState.Idle)
    val clientApiCheck: StateFlow<ClientApiCheckState> = _clientApiCheck.asStateFlow()

    private val _sftpCheck = MutableStateFlow<SftpCheckState>(SftpCheckState.Idle)
    val sftpCheck: StateFlow<SftpCheckState> = _sftpCheck.asStateFlow()

    private var loginJob: Job? = null

    fun connectGithub() {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            try {
                connectGithubUseCase().collect { _login.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _login.value = GithubLoginState.Failed(
                    e.message ?: "Koneksi GitHub gagal. Periksa jaringan dan pengaturan OAuth Device Flow.",
                )
            }
        }
    }

    fun cancelConnect() {
        loginJob?.cancel()
        _login.value = null
    }

    fun disconnectGithub() {
        loginJob?.cancel()
        _login.value = null
        viewModelScope.launch {
            val current = settings.value
            updateSettings(current.copy(githubToken = "", githubLogin = "", githubAvatarUrl = ""))
        }
    }

    fun testClientApi(panelUrl: String, apiKey: String) {
        _clientApiCheck.value = ClientApiCheckState.Checking
        viewModelScope.launch {
            try {
                val servers = pterodactyl.testConnection(panelUrl, apiKey)
                _clientApiCheck.value = ClientApiCheckState.Success(
                    if (servers.isEmpty()) "Client API terhubung, tetapi akun belum memiliki server."
                    else "Client API terhubung · ${servers.size} server ditemukan.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _clientApiCheck.value = ClientApiCheckState.Failure(
                    e.message ?: "Tes Client API gagal. Periksa URL panel dan API key.",
                )
            }
        }
    }

    /** Tahap pertama: cek API, temukan server untuk format username.idserver, lalu baca fingerprint. */
    fun connectSftp(value: AgentSettings) {
        _sftpCheck.value = SftpCheckState.Checking
        viewModelScope.launch {
            try {
                require(value.sftpHost.isNotBlank()) { "Host SFTP belum diisi." }
                require(value.sftpUser.isNotBlank() && value.sftpPassword.isNotBlank()) {
                    "Username dan password SFTP belum diisi."
                }
                val servers = pterodactyl.testConnection(value.panelUrl, value.pteroApiKey)
                val target = servers.firstOrNull { !it.suspended } ?: servers.firstOrNull()
                    ?: throw IllegalStateException("Tidak ada server yang bisa dipakai untuk menguji login SFTP.")
                val fingerprint = sftp.inspectHostKey(
                    host = value.sftpHost,
                    port = value.sftpPort,
                    knownHostKey = value.sftpHostKey,
                )
                if (value.sftpHostKey.isBlank()) {
                    _sftpCheck.value = SftpCheckState.AwaitingTrust(
                        fingerprint = fingerprint,
                        serverId = target.id,
                        serverName = target.name,
                    )
                } else {
                    sftp.testConnection(
                        serverId = target.id,
                        host = value.sftpHost,
                        port = value.sftpPort,
                        username = value.sftpUser,
                        password = value.sftpPassword,
                        knownHostKey = value.sftpHostKey,
                    )
                    _sftpCheck.value = SftpCheckState.Connected(fingerprint, target.name)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _sftpCheck.value = SftpCheckState.Failure(
                    e.message ?: "Koneksi SFTP gagal. Periksa host, port, dan kredensial.",
                )
            }
        }
    }

    /** Tahap kedua: hanya setelah pengguna menekan tombol persetujuan fingerprint. */
    fun trustAndConnect(value: AgentSettings) {
        val pending = _sftpCheck.value as? SftpCheckState.AwaitingTrust ?: return
        _sftpCheck.value = SftpCheckState.Checking
        viewModelScope.launch {
            try {
                val fingerprint = sftp.inspectHostKey(
                    host = value.sftpHost,
                    port = value.sftpPort,
                    knownHostKey = pending.fingerprint,
                )
                sftp.testConnection(
                    serverId = pending.serverId,
                    host = value.sftpHost,
                    port = value.sftpPort,
                    username = value.sftpUser,
                    password = value.sftpPassword,
                    knownHostKey = pending.fingerprint,
                )
                _sftpCheck.value = SftpCheckState.Connected(fingerprint, pending.serverName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _sftpCheck.value = SftpCheckState.Failure(
                    e.message ?: "Verifikasi fingerprint atau login SFTP gagal.",
                )
            }
        }
    }

    fun resetSftpCheck() {
        _sftpCheck.value = SftpCheckState.Idle
    }

    fun save(value: AgentSettings, onDone: () -> Unit) {
        viewModelScope.launch {
            updateSettings(value)
            onDone()
        }
    }
}
