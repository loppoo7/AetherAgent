package com.aether.agent.di
import com.aether.agent.data.api.GithubApiService
import com.aether.agent.data.api.LlmApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json
@Module @InstallIn(SingletonComponent::class) object NetworkModule {
 @Provides @Singleton fun provideJson()=Json{ignoreUnknownKeys=true;isLenient=true}
 @Provides @Singleton fun provideHttpClient(json:Json)=HttpClient(Android){install(ContentNegotiation){json(json)};expectSuccess=true}
 @Provides @Singleton fun provideLlmApi(client:HttpClient)=LlmApiService(client)
 @Provides @Singleton fun provideGithubApi(client:HttpClient)=GithubApiService(client)
}
