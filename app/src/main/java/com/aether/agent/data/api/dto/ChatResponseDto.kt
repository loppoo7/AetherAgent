package com.aether.agent.data.api.dto

import kotlinx.serialization.Serializable

@Serializable data class ChatResponseDto(val choices: List<ChoiceDto> = emptyList()) {
    @Serializable data class ChoiceDto(val message: ChatMessageDto)
}
