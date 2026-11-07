package com.aether.agent.domain.agent

import kotlinx.coroutines.CompletableDeferred

enum class ActionStatus { OK, FAILED, REJECTED }

sealed interface AgentEvent {
    data class ActionStarted(val callId: String, val title: String) : AgentEvent
    data class ActionFinished(
        val callId: String,
        val title: String,
        val output: String,
        val status: ActionStatus,
    ) : AgentEvent
    data class Approval(
        val title: String,
        val diff: String,
        val decision: CompletableDeferred<Boolean>,
    ) : AgentEvent
    data class Answer(val text: String) : AgentEvent
    /** Teks status singkat selama model berpikir atau menyiapkan tool. */
    data class Status(val text: String) : AgentEvent
}
