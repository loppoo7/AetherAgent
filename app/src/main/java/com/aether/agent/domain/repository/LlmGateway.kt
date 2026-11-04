package com.aether.agent.domain.repository

import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.model.LlmReply
import com.aether.agent.domain.model.ToolSpec

interface LlmGateway {
    suspend fun complete(
        settings: AgentSettings,
        system: String,
        messages: List<LlmMessage>,
        tools: List<ToolSpec>,
    ): LlmReply
}
