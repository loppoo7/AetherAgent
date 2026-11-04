package com.aether.agent.domain.model

enum class ApiStyle { OPENAI_COMPAT, ANTHROPIC }

enum class Provider(
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val style: ApiStyle,
) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o", ApiStyle.OPENAI_COMPAT),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-3-5-sonnet-latest", ApiStyle.ANTHROPIC),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", ApiStyle.OPENAI_COMPAT),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", ApiStyle.OPENAI_COMPAT);

    companion object {
        fun fromName(name: String): Provider = entries.firstOrNull { it.name == name } ?: OPENAI
    }
}
