package com.aether.agent.di

import com.aether.agent.domain.agent_tools.AgentTool
import com.aether.agent.domain.agent_tools.CreateBranchTool
import com.aether.agent.domain.agent_tools.CreatePullRequestTool
import com.aether.agent.domain.agent_tools.DeleteFileTool
import com.aether.agent.domain.agent_tools.ListCommitsTool
import com.aether.agent.domain.agent_tools.ListFilesTool
import com.aether.agent.domain.agent_tools.ListIssuesTool
import com.aether.agent.domain.agent_tools.ReadFileTool
import com.aether.agent.domain.agent_tools.WebSearchTool
import com.aether.agent.domain.agent_tools.WriteFileTool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ToolModule {
    @Binds @IntoSet abstract fun listFiles(tool: ListFilesTool): AgentTool
    @Binds @IntoSet abstract fun readFile(tool: ReadFileTool): AgentTool
    @Binds @IntoSet abstract fun writeFile(tool: WriteFileTool): AgentTool
    @Binds @IntoSet abstract fun deleteFile(tool: DeleteFileTool): AgentTool
    @Binds @IntoSet abstract fun createBranch(tool: CreateBranchTool): AgentTool
    @Binds @IntoSet abstract fun createPullRequest(tool: CreatePullRequestTool): AgentTool
    @Binds @IntoSet abstract fun listCommits(tool: ListCommitsTool): AgentTool
    @Binds @IntoSet abstract fun listIssues(tool: ListIssuesTool): AgentTool
    @Binds @IntoSet abstract fun webSearch(tool: WebSearchTool): AgentTool
    @Binds @IntoSet abstract fun serverList(tool: com.aether.agent.domain.agent_tools.PteroListFilesTool): AgentTool
    @Binds @IntoSet abstract fun serverRead(tool: com.aether.agent.domain.agent_tools.PteroReadFileTool): AgentTool
    @Binds @IntoSet abstract fun serverWrite(tool: com.aether.agent.domain.agent_tools.PteroWriteFileTool): AgentTool
    @Binds @IntoSet abstract fun serverDelete(tool: com.aether.agent.domain.agent_tools.PteroDeleteTool): AgentTool
    @Binds @IntoSet abstract fun serverPower(tool: com.aether.agent.domain.agent_tools.PteroPowerTool): AgentTool
}
