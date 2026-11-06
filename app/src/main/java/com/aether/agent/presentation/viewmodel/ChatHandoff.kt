package com.aether.agent.presentation.viewmodel

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Teks yang dikirim dari layar lain (misalnya editor) untuk mengisi kotak input Chat. */
@Singleton
class ChatHandoff @Inject constructor() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(text: String) {
        _pending.value = text
    }

    fun consume() {
        _pending.value = null
    }
}
