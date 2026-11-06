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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    private val repository: SettingsRepository,
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
    private var draftSaveJob: Job? = null

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

    fun testClientApi(value: AgentSettings) {
        if (_clientApiCheck.value is ClientApiCheckState.Checking) return
        _clientApiCheck.value = ClientApiCheckState.Checking
        draftSaveJob?.cancel()
        viewModelScope.launch {
            try {
                updateSettings(value)
                val servers = withTimeout(25_000) {
                    withContext(Dispatchers.IO) { pterodactyl.testConnection(value.panelUrl, value.pteroApiKey) }
                }
                _clientApiCheck.value = ClientApiCheckState.Success(
                    if (servers.isEmpty()) "Client API terhubung, tetapi akun belum memiliki server."
                    else "Client API terhubung · ${servers.size} server ditemukan.",
                )
            } catch (e: TimeoutCancellationException) {
                _clientApiCheck.value = ClientApiCheckState.Failure(
                    "Tes Client API melewati batas 25 detik. Periksa jaringan dan URL panel.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: LinkageError) {
                _clientApiCheck.value = ClientApiCheckState.Failure(
                    "Komponen keamanan Android gagal dimuat saat membuat koneksi HTTPS. Restart aplikasi setelah memperbarui.",
                )
            } catch (e: Exception) {
                _clientApiCheck.value = ClientApiCheckState.Failure(
                    e.message ?: "Tes Client API gagal. Periksa URL panel dan API key.",
                )
            }
        }
    }

    /** Tahap pertama: cek API, temukan server untuk format username.idserver, lalu baca fingerprint. */
    fun connectSftp(value: AgentSettings) {
        if (_sftpCheck.value is SftpCheckState.Checking) return
        _sftpCheck.value = SftpCheckState.Checking
        draftSaveJob?.cancel()
        viewModelScope.launch {
            try {
                updateSettings(value)
                withTimeout(50_000) {
                    withContext(Dispatchers.IO) {
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
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _sftpCheck.value = SftpCheckState.Failure(
                    "Koneksi SFTP melewati batas 50 detik. Periksa host, port, dan jaringan.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: LinkageError) {
                _sftpCheck.value = SftpCheckState.Failure(
                    "Komponen keamanan Android gagal dimuat saat memulai SFTP. Restart aplikasi setelah memperbarui.",
                )
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
        draftSaveJob?.cancel()
        viewModelScope.launch {
            try {
                updateSettings(value)
                withTimeout(50_000) {
                    withContext(Dispatchers.IO) {
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
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _sftpCheck.value = SftpCheckState.Failure(
                    "Verifikasi/login SFTP melewati batas 50 detik. Periksa host, port, dan jaringan.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: LinkageError) {
                _sftpCheck.value = SftpCheckState.Failure(
                    "Komponen keamanan Android gagal dimuat saat verifikasi fingerprint. Restart aplikasi setelah memperbarui.",
                )
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

    /** Persist the complete editor draft after a short pause while the user types. */
    fun persistDraft(value: AgentSettings) {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            updateSettings(value)
        }
    }

    fun save(value: AgentSettings, onDone: () -> Unit) {
        draftSaveJob?.cancel()
        viewModelScope.launch {
            updateSettings(value)
            onDone()
        }
    }
}
