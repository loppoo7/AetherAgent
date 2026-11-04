package com.aether.agent.domain.agent_tools
import javax.inject.Inject
import javax.inject.Singleton
@Singleton class ToolRegistry @Inject constructor(tools:Set<@JvmSuppressWildcards AgentTool>){ private val registered=tools.associateBy { it.name }; fun all()=registered.values.toList(); suspend fun execute(name:String,input:String)=registered[name]?.execute(input) ?: "Tool '$name' tidak ditemukan." }
