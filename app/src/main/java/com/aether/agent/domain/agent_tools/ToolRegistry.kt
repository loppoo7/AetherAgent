package com.aether.agent.domain.agent_tools

import com.aether.agent.domain.model.AgentMode
import com.aether.agent.domain.model.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(tools: Set<@JvmSuppressWildcards AgentTool>) {
    private val byName = tools.associateBy { it.name }

    fun specs(serverMode: AgentMode = AgentMode.OFF): List<ToolSpec> =
        byName.values
            .filter { !it.isServerTool || serverModeAllows(it, serverMode) }
            .map { ToolSpec(it.name, it.description, it.parameters) }

    fun serverModeAllows(tool: AgentTool, mode: AgentMode): Boolean =
        mode != AgentMode.OFF && (!tool.requiresApproval || mode == AgentMode.ACT)

    fun get(name: String): AgentTool? = byName[name]
}
