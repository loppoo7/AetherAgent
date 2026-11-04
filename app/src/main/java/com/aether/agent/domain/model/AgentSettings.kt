package com.aether.agent.domain.model
data class AgentSettings(val baseUrl:String="https://api.openai.com/v1",val apiKey:String="",val model:String="gpt-4o-mini",val githubToken:String="")
