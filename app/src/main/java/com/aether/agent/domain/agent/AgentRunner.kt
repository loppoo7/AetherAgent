package com.aether.agent.domain.agent

import com.aether.agent.domain.agent_tools.ToolRegistry
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.repository.LlmGateway
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Loop agen: kirim ke LLM, jalankan tool yang diminta, ulangi sampai model menjawab tanpa tool. */
class AgentRunner @Inject constructor(
    private val llm: LlmGateway,
    private val tools: ToolRegistry,
) {
    fun run(settings: AgentSettings, history: List<LlmMessage>, userText: String): Flow<AgentEvent> = flow {
        val messages = (history + LlmMessage.User(userText)).toMutableList()
        val specs = tools.specs()

        repeat(MAX_STEPS) {
            val reply = llm.complete(settings, settings.systemPrompt, messages, specs)
            messages += LlmMessage.Assistant(reply.text, reply.toolCalls)

            if (reply.toolCalls.isEmpty()) {
                emit(AgentEvent.Answer(reply.text.ifBlank { "Model tidak memberi jawaban." }))
                return@flow
            }

            for (call in reply.toolCalls) {
                val tool = tools.get(call.name)
                if (tool == null) {
                    messages += LlmMessage.ToolResult(call.id, call.name, "Tool tidak dikenal: ${call.name}")
                    continue
                }
                val title = tool.describe(call.arguments)
                emit(AgentEvent.ActionStarted(call.id, title))

                if (tool.requiresApproval && settings.confirmWrites) {
                    val diff = runCatching { tool.preview(call.arguments) }.getOrDefault("")
                    val decision = CompletableDeferred<Boolean>()
                    emit(AgentEvent.Approval(title, diff, decision))
                    if (!decision.await()) {
                        emit(AgentEvent.ActionFinished(call.id, title, "Ditolak pengguna.", ActionStatus.REJECTED))
                        messages += LlmMessage.ToolResult(
                            call.id, call.name,
                            "Pengguna menolak aksi ini. Jangan ulangi tanpa instruksi baru.",
                        )
                        continue
                    }
                }

                val output = runCatching { tool.execute(call.arguments) }
                    .getOrElse { "Error: ${it.message ?: it::class.simpleName}" }
                val status = if (output.startsWith("Error:")) ActionStatus.FAILED else ActionStatus.OK
                emit(AgentEvent.ActionFinished(call.id, title, output, status))
                messages += LlmMessage.ToolResult(call.id, call.name, output)
            }
        }
        emit(AgentEvent.Answer("Batas langkah agen tercapai. Coba persempit instruksinya."))
    }

    companion object {
        const val MAX_STEPS = 8
    }
}
