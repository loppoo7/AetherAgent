package com.aether.agent.presentation.viewmodel

import com.aether.agent.domain.repository.SftpGateway
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.AgentMode
import com.aether.agent.domain.model.PteroFile
import com.aether.agent.domain.model.PteroServer
import com.aether.agent.domain.repository.PterodactylGateway
import com.aether.agent.domain.repository.SettingsRepository
import com.aether.agent.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Daftar server + kontrol power + mode Agen AI. */
@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val ptero: PterodactylGateway,
    private val settingsRepo: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val servers: List<PteroServer> = emptyList(),
        val error: String? = null,
        val notice: String? = null,
        val busyId: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val mode: StateFlow<AgentMode> = settingsRepo.observeSettings()
        .map { it.serverAgentMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AgentMode.OFF)

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = false, servers = ptero.listServers())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal memuat server.")
            }
        }
    }

    fun power(serverId: String, signal: String) {
        _state.value = _state.value.copy(busyId = serverId, notice = null)
        viewModelScope.launch {
            try {
                ptero.power(serverId, signal)
                _state.value = _state.value.copy(busyId = null, notice = "Sinyal $signal dikirim")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(busyId = null, notice = "Gagal: ${e.message}")
            }
        }
    }

    fun setMode(m: AgentMode) {
        viewModelScope.launch {
            val current = settingsRepo.observeSettings().first()
            updateSettings(current.copy(serverAgentMode = m))
        }
    }
}

/** Pengelola file satu server. Argumen navigasi: server, name. */
@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val sftp: SftpGateway,
    savedState: SavedStateHandle,
) : ViewModel() {

    val serverId: String = checkNotNull(savedState.get<String>("server"))
    val serverName: String = savedState.get<String>("name").orEmpty()

    data class UiState(
        val dir: String = "/",
        val loading: Boolean = true,
        val files: List<PteroFile> = emptyList(),
        val error: String? = null,
        val notice: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        open("/")
    }

    fun open(dir: String) {
        _state.value = _state.value.copy(dir = dir, loading = true, error = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = false, files = sftp.listFiles(serverId, dir))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal memuat folder.")
            }
        }
    }

    /** Naik satu folder. Mengembalikan false jika sudah di root. */
    fun up(): Boolean {
        val dir = _state.value.dir
        if (dir == "/") return false
        val parent = dir.trimEnd('/').substringBeforeLast('/', "")
        open(if (parent.isEmpty()) "/" else parent)
        return true
    }

    fun pathOf(name: String): String = join(_state.value.dir, name)

    fun createFolder(name: String) = runAction("Folder dibuat") {
        sftp.createFolder(serverId, _state.value.dir, name.trim())
        open(_state.value.dir)
    }

    fun rename(from: String, to: String) = runAction("Nama diubah") {
        sftp.rename(serverId, _state.value.dir, from, to.trim())
        open(_state.value.dir)
    }

    fun delete(names: List<String>) = runAction("Dihapus") {
        sftp.deletePaths(serverId, _state.value.dir, names)
        open(_state.value.dir)
    }

    fun clearNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    private fun runAction(done: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _state.value = _state.value.copy(notice = done)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(notice = "Gagal: ${e.message}")
            }
        }
    }

    companion object {
        fun join(dir: String, name: String): String = if (dir == "/") "/$name" else "$dir/$name"
    }
}

/** Editor satu file. Argumen navigasi: server, path. */
@HiltViewModel
class FileEditorViewModel @Inject constructor(
    private val ptero: PterodactylGateway,
    private val sftp: SftpGateway,
    savedState: SavedStateHandle,
) : ViewModel() {

    val serverId: String = checkNotNull(savedState.get<String>("server"))
    val path: String = checkNotNull(savedState.get<String>("path"))

    data class UiState(
        val loading: Boolean = true,
        val original: String = "",
        val text: String = "",
        val error: String? = null,
        val saving: Boolean = false,
        val notice: String? = null,
    ) {
        val dirty: Boolean get() = text != original
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val content = sftp.readFile(serverId, path)
                if (content.length > MAX_CHARS) {
                    throw IllegalStateException("File terlalu besar untuk diedit di HP (di atas 1 MB).")
                }
                _state.value = UiState(loading = false, original = content, text = content)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal membuka file.")
            }
        }
    }

    fun onTextChange(text: String) {
        _state.value = _state.value.copy(text = text)
    }

    /** Simpan. Jika [restart] true, server di-restart setelah file tersimpan. */
    fun save(restart: Boolean) {
        if (_state.value.saving) return
        _state.value = _state.value.copy(saving = true, notice = null)
        viewModelScope.launch {
            try {
                val text = _state.value.text
                sftp.writeFile(serverId, path, text)
                if (restart) ptero.power(serverId, "restart")
                _state.value = _state.value.copy(
                    original = text,
                    saving = false,
                    notice = if (restart) "Tersimpan dan server di-restart" else "Tersimpan",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, notice = "Gagal menyimpan: ${e.message}")
            }
        }
    }

    companion object {
        const val MAX_CHARS = 1_000_000
    }
}
