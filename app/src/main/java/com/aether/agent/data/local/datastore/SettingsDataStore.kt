package com.aether.agent.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aether.agent.data.security.SecretBox
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.AnswerStyle
import com.aether.agent.domain.model.DEFAULT_SYSTEM_PROMPT
import com.aether.agent.domain.model.Provider
import com.aether.agent.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "agent_settings")

/** API key dan token dienkripsi dengan Keystore sebelum disimpan. Nilai lama yang tidak terenkripsi akan kosong. */
@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val provider = stringPreferencesKey("provider")
        val model = stringPreferencesKey("model")
        val answerStyle = stringPreferencesKey("answer_style")
        val githubToken = stringPreferencesKey("github_token")
        val githubLogin = stringPreferencesKey("github_login")
        val githubAvatar = stringPreferencesKey("github_avatar")
        val githubOwner = stringPreferencesKey("github_owner")
        val githubRepo = stringPreferencesKey("github_repo")
        val githubBranch = stringPreferencesKey("github_branch")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val confirmWrites = booleanPreferencesKey("confirm_writes")
        val themeMode = stringPreferencesKey("theme_mode")
        fun apiKey(p: Provider) = stringPreferencesKey("api_key_${p.name}")
    }

    val settings: Flow<AgentSettings> = context.settingsStore.data.map { p ->
        val provider = Provider.fromName(p[Keys.provider].orEmpty())
        AgentSettings(
            provider = provider,
            apiKeys = Provider.entries.associateWith { SecretBox.decrypt(p[Keys.apiKey(it)].orEmpty()) },
            model = p[Keys.model].takeUnless { it.isNullOrBlank() } ?: provider.defaultModel,
            answerStyle = AnswerStyle.entries.firstOrNull { it.name == p[Keys.answerStyle] } ?: AnswerStyle.SEDANG,
            githubToken = SecretBox.decrypt(p[Keys.githubToken].orEmpty()),
            githubLogin = p[Keys.githubLogin].orEmpty(),
            githubAvatarUrl = p[Keys.githubAvatar].orEmpty(),
            githubOwner = p[Keys.githubOwner].orEmpty(),
            githubRepo = p[Keys.githubRepo].orEmpty(),
            githubBranch = p[Keys.githubBranch].takeUnless { it.isNullOrBlank() } ?: "ai-changes",
            systemPrompt = p[Keys.systemPrompt] ?: DEFAULT_SYSTEM_PROMPT,
            confirmWrites = p[Keys.confirmWrites] ?: true,
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.themeMode] } ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun update(s: AgentSettings) {
        context.settingsStore.edit { p ->
            p[Keys.provider] = s.provider.name
            p[Keys.model] = s.model.trim()
            p[Keys.answerStyle] = s.answerStyle.name
            p[Keys.githubToken] = SecretBox.encrypt(s.githubToken.trim())
            p[Keys.githubLogin] = s.githubLogin
            p[Keys.githubAvatar] = s.githubAvatarUrl
            p[Keys.githubOwner] = s.githubOwner.trim()
            p[Keys.githubRepo] = s.githubRepo.trim()
            p[Keys.githubBranch] = s.githubBranch.trim().ifBlank { "ai-changes" }
            p[Keys.systemPrompt] = s.systemPrompt
            p[Keys.confirmWrites] = s.confirmWrites
            p[Keys.themeMode] = s.themeMode.name
            Provider.entries.forEach { prov ->
                p[Keys.apiKey(prov)] = SecretBox.encrypt(s.apiKeys[prov].orEmpty().trim())
            }
        }
    }
}
