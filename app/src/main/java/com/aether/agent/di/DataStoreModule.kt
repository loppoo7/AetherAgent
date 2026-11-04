package com.aether.agent.di
import com.aether.agent.data.local.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class) object DataStoreModule { @Provides @Singleton fun provideSettingsDataStore(store:SettingsDataStore)=store }
