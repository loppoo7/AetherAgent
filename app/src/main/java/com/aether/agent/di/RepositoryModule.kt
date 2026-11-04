package com.aether.agent.di
import com.aether.agent.data.repository.ChatRepositoryImpl
import com.aether.agent.data.repository.SettingsRepositoryImpl
import com.aether.agent.domain.repository.ChatRepository
import com.aether.agent.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class) abstract class RepositoryModule {
 @Binds @Singleton abstract fun bindChatRepository(impl:ChatRepositoryImpl):ChatRepository
 @Binds @Singleton abstract fun bindSettingsRepository(impl:SettingsRepositoryImpl):SettingsRepository
}
