package com.aether.agent.data.repository

import com.aether.agent.data.local.datastore.SettingsDataStore
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val store: SettingsDataStore) : SettingsRepository {
    override fun observeSettings(): Flow<AgentSettings> = store.settings
    override suspend fun update(settings: AgentSettings) = store.update(settings)
}
