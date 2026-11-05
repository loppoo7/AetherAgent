package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.repository.SettingsRepository
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val connectGithubUseCase: ConnectGithubUseCase,
) : ViewModel() {

    val settings: StateFlow<AgentSettings> =
        repository.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentSettings())

    private val _login = MutableStateFlow<GithubLoginState?>(null)
    val login: StateFlow<GithubLoginState?> = _login.asStateFlow()

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

    fun save(value: AgentSettings, onDone: () -> Unit) {
        viewModelScope.launch {
            updateSettings(value)
            onDone()
        }
    }
}
