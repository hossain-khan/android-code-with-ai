package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.MessageCallback

class FakeInferenceConversation(
    val config: ConversationConfig? = null,
) : InferenceConversation {
    data class SentMessage(
        val input: String,
        val callback: MessageCallback,
    )

    val sentMessages = mutableListOf<SentMessage>()
    var cancelled = false
    var closed = false

    override fun sendMessageAsync(
        input: String,
        callback: MessageCallback,
    ) {
        sentMessages.add(SentMessage(input, callback))
    }

    override fun cancelProcess() {
        cancelled = true
    }

    override fun close() {
        closed = true
    }
}
