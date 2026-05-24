package dev.hossain.codematex.runtime

import android.content.Context
import dev.hossain.codematex.data.model.ChatMessage

class LlmEngineImpl(
    @Suppress("UNUSED_PARAMETER") context: Context,
) : LlmEngine {
    override suspend fun initialize(
        modelPath: String,
        backend: LlmEngine.Backend,
        systemInstruction: String?,
    ) {
        // TODO: Implement LiteRT-LM engine initialization
    }

    override suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        // TODO: Implement LiteRT-LM inference with streaming
    }

    override fun stop() {
        // TODO: Implement stop
    }

    override fun resetConversation(systemInstruction: String?) {
        // TODO: Implement reset
    }

    override suspend fun restoreHistory(messages: List<ChatMessage>) {
        // TODO: Implement history restoration
    }

    override fun cleanup() {
        // TODO: Implement cleanup
    }
}
