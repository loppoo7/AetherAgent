package com.aether.agent.domain.usecase

import com.aether.agent.domain.agent.ToolScope

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

import com.aether.agent.domain.agent.AgentEvent
import com.aether.agent.domain.agent.AgentRunner
import com.aether.agent.domain.model.AttachedFileContext
import com.aether.agent.domain.repository.ChatRepository
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class RunAgentUseCase @Inject constructor(
    private val runner: AgentRunner,
    private val chat: ChatRepository,
    private val settings: SettingsRepository,
) {
    operator fun invoke(
        sessionId: String,
        userText: String,
        roleInstruction: String? = null,
        context: List<AttachedFileContext> = emptyList(),
        scope: ToolScope = ToolScope.ALL,
    ): Flow<AgentEvent> = flow {
        val cfg = settings.observeSettings().first()
        val history = chat.recentHistory(sessionId)
        chat.save(sessionId, "user", userText)
        val system = if (roleInstruction.isNullOrBlank()) cfg.systemPrompt
            else cfg.systemPrompt + "\n\nPERAN AI AKTIF — ikuti instruksi peran berikut:\n" + roleInstruction
        runner.run(cfg, history, userText, system, context, scope)
            .catch { e ->
                if (e !is CancellationException) chat.save(sessionId, "assistant", "Gagal: ${e.message ?: "error"}")
                throw e
            }
            .collect { event ->
            if (event is AgentEvent.Answer) chat.save(sessionId, "assistant", event.text)
            emit(event)
        }
    }
}
