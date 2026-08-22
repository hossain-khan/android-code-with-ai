package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.ConversationConfig

class FakeInferenceEngine : InferenceEngine {
    val createdConversations = mutableListOf<FakeInferenceConversation>()
    var closed = false

    override fun createConversation(config: ConversationConfig): FakeInferenceConversation {
        val conversation = FakeInferenceConversation(config)
        createdConversations.add(conversation)
        return conversation
    }

    override fun close() {
        closed = true
    }
}
