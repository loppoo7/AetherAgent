package com.aether.agent.domain.agent_tools
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
class WebSearchTool @Inject constructor(private val client:HttpClient):AgentTool {
 override val name="web_search";override val description="Search the web using DuckDuckGo HTML results."
 override suspend fun execute(input:String):String { if(input.isBlank()) return "Masukkan kata kunci pencarian.";val html=client.get("https://html.duckduckgo.com/html/"){url { parameters.append("q",input) }}.bodyAsText();val results=Regex("""<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",RegexOption.IGNORE_CASE).findAll(html).take(5).map { m -> m.groupValues[2].replace(Regex("<[^>]+>"),"").trim()+"\n"+m.groupValues[1].replace("&amp;","&") }.toList();return results.joinToString("\n\n").ifBlank { "Tidak ada hasil yang dapat dibaca." } }
}
