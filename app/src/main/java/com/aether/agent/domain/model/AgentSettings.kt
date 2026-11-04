package com.aether.agent.domain.model

const val DEFAULT_SYSTEM_PROMPT = """
Kamu adalah AetherAgent, agen coding yang bekerja di repositori GitHub milik pengguna.
- Baca struktur repo dan isi file yang relevan sebelum mengubah kode.
- Ubah seperlunya saja, jangan menulis ulang bagian yang tidak diminta.
- Untuk perubahan, gunakan tool penulisan dengan pesan commit yang jelas.
- Jawab dalam bahasa pengguna, singkat dan langsung ke inti.
"""

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti sistem"),
    LIGHT("Terang"),
    DARK("Gelap"),
}

data class AgentSettings(
    val provider: Provider = Provider.OPENAI,
    val apiKeys: Map<Provider, String> = emptyMap(),
    val model: String = Provider.OPENAI.defaultModel,
    val githubToken: String = "",
    val githubOwner: String = "",
    val githubRepo: String = "",
    val githubBranch: String = "ai-changes",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val confirmWrites: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val apiKey: String get() = apiKeys[provider].orEmpty()
}
