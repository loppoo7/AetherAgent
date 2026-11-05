package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.agent.ActionStatus
import com.aether.agent.domain.agent.AgentEvent
import com.aether.agent.domain.model.AiRole
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.AttachedFileContext
import com.aether.agent.domain.model.ChatSession
import com.aether.agent.domain.model.GithubRepo
import com.aether.agent.domain.model.Message
import com.aether.agent.domain.model.toTargetKey
import com.aether.agent.domain.repository.SettingsRepository
import com.aether.agent.domain.usecase.ChatSessionsUseCase
import com.aether.agent.domain.usecase.ClearChatUseCase
import com.aether.agent.domain.usecase.ObserveChatUseCase
import com.aether.agent.domain.usecase.RunAgentUseCase
import com.aether.agent.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CardState { RUNNING, OK, FAILED, REJECTED }
data class AgentCard(val id: String, val title: String, val state: CardState, val output: String = "")
data class PendingApproval(val title: String, val diff: String, val decision: CompletableDeferred<Boolean>)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val observeChat: ObserveChatUseCase,
    private val runAgent: RunAgentUseCase,
    private val clearChat: ClearChatUseCase,
    private val sessions: ChatSessionsUseCase,
    private val settingsRepository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {
    val sessionList: StateFlow<List<ChatSession>> =
        sessions.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<AgentSettings> =
        settingsRepository.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentSettings())

    private val _activeId = MutableStateFlow("")
    val activeSessionId: StateFlow<String> = _activeId.asStateFlow()
    val messages: StateFlow<List<Message>> = _activeId.flatMapLatest { id ->
        if (id.isEmpty()) flowOf(emptyList<Message>()) else observeChat(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cards = MutableStateFlow<List<AgentCard>>(emptyList())
    val cards: StateFlow<List<AgentCard>> = _cards.asStateFlow()
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()
    private val _approval = MutableStateFlow<PendingApproval?>(null)
    val approval: StateFlow<PendingApproval?> = _approval.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedRole = MutableStateFlow(AiRole.FEATURE_BUILDER)
    val selectedRole: StateFlow<AiRole> = _selectedRole.asStateFlow()
    private val _attachedContext = MutableStateFlow<List<AttachedFileContext>>(emptyList())
    val attachedContext: StateFlow<List<AttachedFileContext>> = _attachedContext.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            _activeId.value = sessions.observe().first().firstOrNull()?.id ?: sessions.create()
        }
    }

    fun selectRole(role: AiRole) { _selectedRole.value = role }

    fun setRoleSelectorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.observeSettings().first()
            updateSettings(current.copy(roleSelectorEnabled = enabled))
        }
    }

    fun selectModel(model: String) {
        if (model.isBlank()) return
        viewModelScope.launch {
            val current = settingsRepository.observeSettings().first()
            updateSettings(current.copy(model = model.trim()))
        }
    }

    fun attachContext(files: List<AttachedFileContext>) {
        _attachedContext.value = (_attachedContext.value + files).distinctBy { it.id }.take(20)
    }
    fun removeContext(fileId: String) { _attachedContext.value = _attachedContext.value.filterNot { it.id == fileId } }

    fun send(text: String) {
        val id = _activeId.value
        if (text.isBlank() || _running.value || id.isEmpty()) return
        val contextSnapshot = _attachedContext.value
        val roleInstruction = if (settings.value.roleSelectorEnabled) _selectedRole.value.instruction else ""
        _running.value = true
        _error.value = null
        _cards.value = emptyList()
        _attachedContext.value = emptyList()
        job = viewModelScope.launch {
            try {
                runAgent(id, text.trim(), roleInstruction, contextSnapshot).collect { event -> onEvent(event) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan."
            } finally {
                _running.value = false
                _approval.value = null
            }
        }
    }

    private fun onEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.ActionStarted ->
                _cards.value = _cards.value + AgentCard(event.callId, event.title, CardState.RUNNING)
            is AgentEvent.ActionFinished -> {
                val state = when (event.status) {
                    ActionStatus.OK -> CardState.OK
                    ActionStatus.FAILED -> CardState.FAILED
                    ActionStatus.REJECTED -> CardState.REJECTED
                }
                _cards.value = _cards.value.map { if (it.id == event.callId) it.copy(state = state, output = event.output) else it }
            }
            is AgentEvent.Approval -> _approval.value = PendingApproval(event.title, event.diff, event.decision)
            is AgentEvent.Answer -> Unit
        }
    }

    fun resolveApproval(approved: Boolean) {
        val pending = _approval.value ?: return
        _approval.value = null
        pending.decision.complete(approved)
    }
    fun stop() { _approval.value?.decision?.complete(false); job?.cancel() }

    fun selectRepos(repos: List<GithubRepo>) {
        viewModelScope.launch {
            val current = settingsRepository.observeSettings().first()
            val selected = repos.distinctBy { it.owner + "/" + it.name }
            val first = selected.firstOrNull()
            updateSettings(current.copy(
                githubOwner = first?.owner.orEmpty(),
                githubRepo = first?.name.orEmpty(),
                githubBranch = first?.defaultBranch ?: current.githubBranch,
                githubTargets = selected.map { it.toTargetKey() }.toSet(),
            ))
        }
    }
    fun selectRepo(repo: GithubRepo) = selectRepos(listOf(repo))

    fun newChat() {
        if (_running.value) return
        viewModelScope.launch { selectSession(sessions.create()) }
    }
    fun selectSession(id: String) {
        if (_running.value) return
        _activeId.value = id
        _cards.value = emptyList()
        _error.value = null
        _attachedContext.value = emptyList()
    }
    fun renameSession(id: String, title: String) { viewModelScope.launch { sessions.rename(id, title) } }
    fun deleteSession(id: String) {
        if (_running.value && id == _activeId.value) return
        viewModelScope.launch {
            sessions.delete(id)
            if (_activeId.value == id) {
                _activeId.value = sessions.observe().first().firstOrNull()?.id ?: sessions.create()
                _cards.value = emptyList()
            }
        }
    }
    fun clear() { viewModelScope.launch { clearChat(_activeId.value) } }
}
