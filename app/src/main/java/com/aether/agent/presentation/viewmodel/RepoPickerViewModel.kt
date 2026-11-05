package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.GithubRepo
import com.aether.agent.domain.repository.GithubGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RepoPickerState(
    val loading: Boolean = true,
    val repos: List<GithubRepo> = emptyList(),
    val error: String? = null,
    val query: String = "",
)

@HiltViewModel
class RepoPickerViewModel @Inject constructor(private val github: GithubGateway) : ViewModel() {

    private val _state = MutableStateFlow(RepoPickerState())
    val state: StateFlow<RepoPickerState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val repos = github.listRepos()
                _state.value = _state.value.copy(loading = false, repos = repos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal memuat daftar repo.")
            }
        }
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }
}
