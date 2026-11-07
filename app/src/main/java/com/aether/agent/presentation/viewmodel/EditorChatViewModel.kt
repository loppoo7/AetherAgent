package com.aether.agent.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.agent.domain.agent.AgentEvent
import com.aether.agent.domain.model.AttachedFileContext
import com.aether.agent.domain.model.EDITOR_SESSION_PREFIX
import com.aether.agent.domain.model.Message
import com.aether.agent.domain.repository.ChatRepository
import com.aether.agent.domain.usecase.RunAgentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Chat AI yang tampil di bawah editor. Riwayatnya terpisah per file dan tidak muncul di Chat utama. */
@HiltViewModel
class EditorChatViewModel @Inject constructor(
    private val runAgent: RunAgentUseCase,
    private val chat: ChatRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = checkNotNull(savedState.get<String>("server"))
    val path: String = checkNotNull(savedState.get<String>("path"))
    private val sessionId: String = EDITOR_SESSION_PREFIX + serverId + ":" + path

    val messages: StateFlow<List<Message>> = chat.observeMessages(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _activity = MutableStateFlow<String?>(null)
    val activity: StateFlow<String?> = _activity.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _approval = MutableStateFlow<PendingApproval?>(null)
    val approval: StateFlow<PendingApproval?> = _approval.asStateFlow()

    private var job: Job? = null
    private val thinking = ThinkingTracker()
    val thinkingSteps = thinking.steps

    /** [fileText] adalah isi editor saat ini, termasuk perubahan yang belum disimpan. */
    fun send(text: String, fileText: String) {
        val input = text.trim()
        if (input.isEmpty() || _running.value) return
        _running.value = true
        _error.value = null
        thinking.reset()
        job = viewModelScope.launch {
            try {
                chat.ensureSession(sessionId, "Editor: " + path.substringAfterLast('/'))
                val context = listOf(AttachedFileContext("editor", path, fileText))
                runAgent(sessionId, input, null, context).collect { event ->
                    when (event) {
                        is AgentEvent.ActionStarted -> {
                            thinking.actionStarted(event.callId, event.title)
                            _activity.value = event.title
                        }
                        is AgentEvent.ActionFinished -> {
                            thinking.actionFinished(event.callId, event.output)
                            _activity.value = null
                        }
                        is AgentEvent.Approval ->
                            _approval.value = PendingApproval(event.title, event.diff, event.decision)
                        is AgentEvent.Status -> {
                            thinking.status(event.text)
                            _activity.value = event.text
                        }
                        is AgentEvent.Answer -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan."
            } finally {
                _running.value = false
                thinking.finish()
                _activity.value = null
                _approval.value = null
            }
        }
    }

    fun resolveApproval(approved: Boolean) {
        val pending = _approval.value ?: return
        _approval.value = null
        pending.decision.complete(approved)
    }

    fun stop() {
        _approval.value?.decision?.complete(false)
        job?.cancel()
    }

    fun clear() {
        if (_running.value) return
        viewModelScope.launch { chat.clear(sessionId) }
    }
}
