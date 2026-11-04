package com.aether.agent.data.local.datastore
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
private val Context.settingsStore by preferencesDataStore(name="agent_settings")
@Singleton class SettingsDataStore @Inject constructor(@ApplicationContext private val context:Context){
 private object Keys { val baseUrl=stringPreferencesKey("base_url"); val apiKey=stringPreferencesKey("api_key"); val model=stringPreferencesKey("model"); val githubToken=stringPreferencesKey("github_token") }
 val settings:Flow<Map<String,String>> = context.settingsStore.data.map { p -> mapOf("baseUrl" to (p[Keys.baseUrl] ?: "https://api.openai.com/v1"),"apiKey" to (p[Keys.apiKey] ?: ""),"model" to (p[Keys.model] ?: "gpt-4o-mini"),"githubToken" to (p[Keys.githubToken] ?: "")) }
 suspend fun update(baseUrl:String,apiKey:String,model:String,githubToken:String){ context.settingsStore.edit { p -> p[Keys.baseUrl]=baseUrl.trim();p[Keys.apiKey]=apiKey;p[Keys.model]=model.trim();p[Keys.githubToken]=githubToken } }
}
