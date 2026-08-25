package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.MessageCallback

/**
 * Wrapper around the native LiteRT-LM [Engine].
 *
 * This interface exists purely to make the runtime testable: the real implementation
 * delegates to the native object, while fakes can simulate engine behavior without
 * loading JNI libraries.
 */
interface InferenceEngine {
    /**
     * Initializes the engine after creation. Must be called before creating conversations.
     */
    fun initialize()

    /**
     * Creates a new conversation with the given [config].
     */
    fun createConversation(config: ConversationConfig): InferenceConversation

    /**
     * Releases native resources held by the engine.
     */
    fun close()
}

/**
 * Wrapper around the native LiteRT-LM [com.google.ai.edge.litertlm.Conversation].
 */
interface InferenceConversation {
    /**
     * Sends [input] to the model and streams tokens through [callback].
     */
    fun sendMessageAsync(
        input: String,
        callback: MessageCallback,
    )

    /**
     * Cancels an in-flight inference request.
     */
    fun cancelProcess()

    /**
     * Releases native resources held by the conversation.
     */
    fun close()
}

/**
 * Default implementation that forwards calls to the native LiteRT-LM [Engine].
 */
class DefaultInferenceEngine(
    private val engine: Engine,
) : InferenceEngine {
    override fun initialize() {
        engine.initialize()
    }

    override fun createConversation(config: ConversationConfig): InferenceConversation =
        DefaultInferenceConversation(engine.createConversation(config))

    override fun close() {
        engine.close()
    }
}

/**
 * Default implementation that forwards calls to the native LiteRT-LM [com.google.ai.edge.litertlm.Conversation].
 */
class DefaultInferenceConversation(
    private val conversation: com.google.ai.edge.litertlm.Conversation,
) : InferenceConversation {
    override fun sendMessageAsync(
        input: String,
        callback: MessageCallback,
    ) {
        conversation.sendMessageAsync(input, callback)
    }

    override fun cancelProcess() {
        conversation.cancelProcess()
    }

    override fun close() {
        conversation.close()
    }
}
