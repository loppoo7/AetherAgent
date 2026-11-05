package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.model.AttachedFileContext
import com.aether.agent.domain.repository.GithubGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContextFileOption(val repositoryTarget: String, val path: String) {
    val id: String get() = "$repositoryTarget::$path"
    val repository: String get() = repositoryTarget.substringBefore('|')
}
data class ContextPickerState(
    val loading: Boolean = false,
    val reading: Boolean = false,
    val files: List<ContextFileOption> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val error: String? = null,
)

@HiltViewModel
class ContextPickerViewModel @Inject constructor(private val github: GithubGateway) : ViewModel() {
    private val _state = MutableStateFlow(ContextPickerState())
    val state = _state.asStateFlow()

    fun load(targets: Set<String>) {
        if (targets.isEmpty()) {
            _state.value = ContextPickerState(error = "Pilih repository GitHub terlebih dahulu.")
            return
        }
        _state.value = ContextPickerState(loading = true)
        viewModelScope.launch {
            try {
                val rows = targets.sorted().flatMap { target ->
                    github.listFiles(target).map { ContextFileOption(target, it) }
                }
                _state.value = _state.value.copy(loading = false, files = rows)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Gagal memuat file repository.")
            }
        }
    }

    fun setQuery(value: String) { _state.value = _state.value.copy(query = value) }
    fun toggle(id: String) {
        val current = _state.value.selected
        _state.value = _state.value.copy(
            selected = if (id in current) current - id else if (current.size < 20) current + id else current,
        )
    }

    suspend fun readSelected(): List<AttachedFileContext> {
        val snapshot = _state.value
        require(snapshot.selected.isNotEmpty()) { "Pilih minimal satu file." }
        _state.value = snapshot.copy(reading = true, error = null)
        return try {
            snapshot.files.filter { it.id in snapshot.selected }.take(20).map { file ->
                val content = github.readFile(file.repositoryTarget, file.path)
                    ?: error("File ${file.path} tidak ditemukan di ${file.repository}.")
                AttachedFileContext(file.repository, file.path, content.take(12_000))
            }.also { _state.value = _state.value.copy(reading = false) }
        } catch (e: Exception) {
            _state.value = _state.value.copy(reading = false, error = e.message ?: "Gagal membaca file.")
            throw e
        }
    }
}
