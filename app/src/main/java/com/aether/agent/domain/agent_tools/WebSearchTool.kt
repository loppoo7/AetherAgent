package com.aether.agent.domain.agent_tools

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

class WebSearchTool @Inject constructor(private val client: HttpClient) : AgentTool {
    override val name = "web_search"
    override val description = "Cari di web (DuckDuckGo HTML) dan kembalikan judul serta tautan hasil teratas."
    override val parameters = objectSchema(mapOf("query" to "Kata kunci pencarian"), listOf("query"))
    override fun describe(args: JsonObject) = "Mencari: ${args.str("query")}"

    override suspend fun execute(args: JsonObject): String {
        val query = args.str("query")
        if (query.isBlank()) return "Masukkan kata kunci pencarian."
        val html = client.get("https://html.duckduckgo.com/html/") {
            url { parameters.append("q", query) }
        }.bodyAsText()
        val regex = Regex(
            """<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
            RegexOption.IGNORE_CASE,
        )
        return regex.findAll(html).take(5).joinToString("\n\n") { m ->
            m.groupValues[2].replace(Regex("<[^>]+>"), "").trim() + "\n" + m.groupValues[1].replace("&amp;", "&")
        }.ifBlank { "Tidak ada hasil yang dapat dibaca." }
    }
}
