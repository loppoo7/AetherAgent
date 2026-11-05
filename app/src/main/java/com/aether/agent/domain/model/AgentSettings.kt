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

/** Gaya jawaban dipetakan ke temperature model. */
enum class AnswerStyle(val label: String, val temperature: Double, val hint: String) {
    RENDAH("Rendah", 0.1, "Konsisten dan fokus, cocok untuk edit kode dan commit"),
    SEDANG("Sedang", 0.3, "Seimbang antara teliti dan luwes"),
    TINGGI("Tinggi", 0.7, "Lebih bervariasi, cocok untuk brainstorming ide"),
}

data class AgentSettings(
    val provider: Provider = Provider.OPENAI,
    val apiKeys: Map<Provider, String> = emptyMap(),
    val model: String = Provider.OPENAI.defaultModel,
    val answerStyle: AnswerStyle = AnswerStyle.SEDANG,
    val githubToken: String = "",
    val githubLogin: String = "",
    val githubAvatarUrl: String = "",
    val githubOwner: String = "",
    val githubRepo: String = "",
    /** Multi-repository targets encoded as owner/name|branch. */
    val githubTargets: Set<String> = emptySet(),
    val githubBranch: String = "ai-changes",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val confirmWrites: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val apiKey: String get() = apiKeys[provider].orEmpty()
}
