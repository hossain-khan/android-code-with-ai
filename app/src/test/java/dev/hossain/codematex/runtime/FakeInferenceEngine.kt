package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.ConversationConfig

class FakeInferenceEngine : InferenceEngine {
    val createdConversations = mutableListOf<FakeInferenceConversation>()
    var initialized = false
    var closed = false
    var conversationToThrow: Throwable? = null
    var onInitialize: (() -> Unit)? = null
    var onCreateConversation: (() -> Unit)? = null

    override fun initialize() {
        initialized = true
        onInitialize?.invoke()
    }

    override fun createConversation(config: ConversationConfig): FakeInferenceConversation {
        onCreateConversation?.invoke()
        conversationToThrow?.let { throw it }
        val conversation = FakeInferenceConversation(config)
        createdConversations.add(conversation)
        return conversation
    }

    override fun close() {
        closed = true
    }
}
