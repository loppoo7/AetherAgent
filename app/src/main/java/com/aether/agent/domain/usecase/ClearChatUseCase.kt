package com.aether.agent.domain.usecase

import com.aether.agent.domain.repository.ChatRepository
import javax.inject.Inject

class ClearChatUseCase @Inject constructor(private val chat: ChatRepository) {
    suspend operator fun invoke(sessionId: String) = chat.clear(sessionId)
}
