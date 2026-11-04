package com.aether.agent.domain.usecase
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
class UpdateSettingsUseCase @Inject constructor(private val repository:SettingsRepository){ suspend operator fun invoke(settings:AgentSettings)=repository.update(settings) }
