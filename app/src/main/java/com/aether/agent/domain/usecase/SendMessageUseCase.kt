package com.aether.agent.domain.usecase
import com.aether.agent.domain.repository.ChatRepository
import javax.inject.Inject
class SendMessageUseCase @Inject constructor(private val repository:ChatRepository){ suspend operator fun invoke(sessionId:String,content:String)=repository.sendMessage(sessionId,content) }
