package com.aether.agent.domain.agent_tools
import com.aether.agent.data.api.GithubApiService
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
class GithubTool @Inject constructor(private val api:GithubApiService,private val settings:SettingsRepository):AgentTool {
 override val name="github_search";override val description="Search public GitHub repositories by query."
 override suspend fun execute(input:String):String { if(input.isBlank()) return "Masukkan kata kunci pencarian.";val token=settings.observeSettings().first().githubToken;val items=api.searchRepositories(input,token)["items"]?.jsonArray ?: return "Tidak ada hasil.";return items.take(8).joinToString("\n\n"){ val o=it.jsonObject;val n=o["full_name"]?.jsonPrimitive?.content ?: "unknown";val u=o["html_url"]?.jsonPrimitive?.content ?: "";val d=o["description"]?.jsonPrimitive?.contentOrNull ?: ""; "$n — $d\n$u" }.ifBlank { "Tidak ada repository ditemukan." } }
}
