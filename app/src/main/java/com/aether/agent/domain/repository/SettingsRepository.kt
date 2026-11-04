package com.aether.agent.domain.repository
import com.aether.agent.domain.model.AgentSettings
import kotlinx.coroutines.flow.Flow
interface SettingsRepository { fun observeSettings():Flow<AgentSettings>; suspend fun update(settings:AgentSettings) }
