package com.aether.agent.domain.usecase
import com.aether.agent.domain.repository.ChatRepository
import javax.inject.Inject
class ObserveChatUseCase @Inject constructor(private val repository:ChatRepository){ operator fun invoke(sessionId:String)=repository.observeMessages(sessionId) }
