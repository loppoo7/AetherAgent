package com.aether.agent.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.DEFAULT_SYSTEM_PROMPT
import com.aether.agent.domain.model.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "agent_settings")

/** Preferences DataStore. Catatan: DataStore tidak mengenkripsi nilai; lihat README. */
@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val provider = stringPreferencesKey("provider")
        val model = stringPreferencesKey("model")
        val githubToken = stringPreferencesKey("github_token")
        val githubOwner = stringPreferencesKey("github_owner")
        val githubRepo = stringPreferencesKey("github_repo")
        val githubBranch = stringPreferencesKey("github_branch")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val confirmWrites = booleanPreferencesKey("confirm_writes")
        fun apiKey(p: Provider) = stringPreferencesKey("api_key_${p.name}")
    }

    val settings: Flow<AgentSettings> = context.settingsStore.data.map { p ->
        val provider = Provider.fromName(p[Keys.provider].orEmpty())
        AgentSettings(
            provider = provider,
            apiKeys = Provider.entries.associateWith { p[Keys.apiKey(it)].orEmpty() },
            model = p[Keys.model].takeUnless { it.isNullOrBlank() } ?: provider.defaultModel,
            githubToken = p[Keys.githubToken].orEmpty(),
            githubOwner = p[Keys.githubOwner].orEmpty(),
            githubRepo = p[Keys.githubRepo].orEmpty(),
            githubBranch = p[Keys.githubBranch].takeUnless { it.isNullOrBlank() } ?: "ai-changes",
            systemPrompt = p[Keys.systemPrompt] ?: DEFAULT_SYSTEM_PROMPT,
            confirmWrites = p[Keys.confirmWrites] ?: true,
        )
    }

    suspend fun update(s: AgentSettings) {
        context.settingsStore.edit { p ->
            p[Keys.provider] = s.provider.name
            p[Keys.model] = s.model.trim()
            p[Keys.githubToken] = s.githubToken.trim()
            p[Keys.githubOwner] = s.githubOwner.trim()
            p[Keys.githubRepo] = s.githubRepo.trim()
            p[Keys.githubBranch] = s.githubBranch.trim().ifBlank { "ai-changes" }
            p[Keys.systemPrompt] = s.systemPrompt
            p[Keys.confirmWrites] = s.confirmWrites
            Provider.entries.forEach { prov -> p[Keys.apiKey(prov)] = s.apiKeys[prov].orEmpty().trim() }
        }
    }
}
