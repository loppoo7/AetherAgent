package com.aether.agent.domain.usecase

import com.aether.agent.domain.agent.AgentEvent
import com.aether.agent.domain.agent.AgentRunner
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
    operator fun invoke(sessionId: String, userText: String): Flow<AgentEvent> = flow {
        val cfg = settings.observeSettings().first()
        val history = chat.recentHistory(sessionId)
        chat.save(sessionId, "user", userText)
        runner.run(cfg, history, userText).collect { event ->
            if (event is AgentEvent.Answer) chat.save(sessionId, "assistant", event.text)
            emit(event)
        }
    }
}
