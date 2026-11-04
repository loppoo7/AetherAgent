package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.repository.SettingsRepository
import com.aether.agent.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    val settings: StateFlow<AgentSettings> =
        repository.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentSettings())

    fun save(value: AgentSettings, onDone: () -> Unit) {
        viewModelScope.launch {
            updateSettings(value)
            onDone()
        }
    }
}
