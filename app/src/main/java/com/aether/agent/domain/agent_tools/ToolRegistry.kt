package com.aether.agent.domain.agent_tools

import com.aether.agent.domain.model.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(tools: Set<@JvmSuppressWildcards AgentTool>) {
    private val byName = tools.associateBy { it.name }

    fun specs(): List<ToolSpec> = byName.values.map { ToolSpec(it.name, it.description, it.parameters) }

    fun get(name: String): AgentTool? = byName[name]
}
