package com.aether.agent.di

import com.aether.agent.data.api.llm.LlmGatewayImpl
import com.aether.agent.data.repository.ChatRepositoryImpl
import com.aether.agent.data.repository.GithubAuthGatewayImpl
import com.aether.agent.data.repository.GithubGatewayImpl
import com.aether.agent.data.repository.SettingsRepositoryImpl
import com.aether.agent.domain.repository.ChatRepository
import com.aether.agent.domain.repository.GithubAuthGateway
import com.aether.agent.domain.repository.GithubGateway
import com.aether.agent.domain.repository.LlmGateway
import com.aether.agent.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindGithubGateway(impl: GithubGatewayImpl): GithubGateway
    @Binds @Singleton abstract fun bindLlmGateway(impl: LlmGatewayImpl): LlmGateway
    @Binds @Singleton abstract fun bindGithubAuthGateway(impl: GithubAuthGatewayImpl): GithubAuthGateway
    @Binds @Singleton abstract fun bindPterodactylGateway(impl: com.aether.agent.data.repository.PterodactylGatewayImpl): com.aether.agent.domain.repository.PterodactylGateway
    @Binds @Singleton abstract fun bindSftpGateway(impl: com.aether.agent.data.remote.SftpGatewayImpl): com.aether.agent.domain.repository.SftpGateway
}
