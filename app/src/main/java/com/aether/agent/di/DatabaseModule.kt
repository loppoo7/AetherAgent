package com.aether.agent.di
import android.content.Context
import androidx.room.Room
import com.aether.agent.data.local.db.AppDatabase
import com.aether.agent.data.local.db.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class) object DatabaseModule {
 @Provides @Singleton fun provideDatabase(@ApplicationContext context:Context)=Room.databaseBuilder(context,AppDatabase::class.java,"aether.db").build()
 @Provides fun provideChatDao(database:AppDatabase):ChatDao=database.chatDao()
}
