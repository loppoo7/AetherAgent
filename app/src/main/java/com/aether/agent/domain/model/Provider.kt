package com.aether.agent.domain.model

enum class ApiStyle { OPENAI_COMPAT, ANTHROPIC }

enum class Provider(
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val style: ApiStyle,
    /** Pilihan cepat di Pengaturan. Kosong berarti hanya model default. */
    val models: List<String> = emptyList(),
) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o", ApiStyle.OPENAI_COMPAT),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-3-5-sonnet-latest", ApiStyle.ANTHROPIC),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", ApiStyle.OPENAI_COMPAT),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", ApiStyle.OPENAI_COMPAT),
    VIKEY(
        "Vikey",
        "https://api.vikey.ai/v1",
        "deepseek/deepseek-v4.1-flash",
        ApiStyle.OPENAI_COMPAT,
        listOf(
            "deepseek/deepseek-v4.1-flash",
            "deepseek/deepseek-v4-pro",
            "anthropic/claude-sonnet-5.5",
            "gemini/gemini-3.8-flash",
            "openai/gpt-6.1-sol",
            "moonshotai/kimi-k3.1-pro",
        ),
    );

    companion object {
        fun fromName(name: String): Provider = entries.firstOrNull { it.name == name } ?: OPENAI
    }
}
