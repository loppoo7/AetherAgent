package com.aether.agent.presentation.viewmodel

import com.aether.agent.presentation.components.ThinkingStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Menyusun langkah berpikir dari event agen. Semua teks berasal dari kejadian nyata. */
class ThinkingTracker {
    private val _steps = MutableStateFlow<List<ThinkingStep>>(emptyList())
    val steps: StateFlow<List<ThinkingStep>> = _steps.asStateFlow()
    private var counter = 0

    fun reset() {
        _steps.value = emptyList()
        counter = 0
    }

    /** Status dari model, misalnya "Berpikir…" atau "Menyiapkan tool: x". */
    fun status(label: String) {
        closeOpen()
        counter++
        _steps.value = _steps.value + ThinkingStep(id = "status_$counter", label = label)
    }

    /** Tool mulai dijalankan. Id-nya memakai callId supaya bisa ditandai selesai nanti. */
    fun actionStarted(callId: String, title: String) {
        closeOpen()
        _steps.value = _steps.value + ThinkingStep(id = callId, label = title)
    }

    /** Tool selesai. Detail diambil dari baris pertama hasilnya. */
    fun actionFinished(callId: String, output: String) {
        val detail = output.lineSequence().firstOrNull()?.trim()?.take(80)?.takeIf { it.isNotBlank() }
        _steps.value = _steps.value.map {
            if (it.id == callId) it.copy(done = true, detail = detail) else it
        }
    }

    fun finish() {
        _steps.value = _steps.value.map { it.copy(done = true) }
    }

    private fun closeOpen() {
        _steps.value = _steps.value.map { if (it.done) it else it.copy(done = true) }
    }
}
