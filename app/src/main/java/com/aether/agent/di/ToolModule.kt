package com.aether.agent.di
import com.aether.agent.domain.agent_tools.AgentTool
import com.aether.agent.domain.agent_tools.GithubTool
import com.aether.agent.domain.agent_tools.WebSearchTool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
@Module @InstallIn(SingletonComponent::class) abstract class ToolModule {
 @Binds @IntoSet abstract fun bindGithubTool(tool:GithubTool):AgentTool
 @Binds @IntoSet abstract fun bindWebSearchTool(tool:WebSearchTool):AgentTool
}
