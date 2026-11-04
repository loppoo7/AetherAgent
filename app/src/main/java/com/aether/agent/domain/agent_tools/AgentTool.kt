package com.aether.agent.domain.agent_tools
interface AgentTool { val name:String; val description:String; suspend fun execute(input:String):String }
