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
class DefaultInferenceEngine internal constructor(
    private val initAction: () -> Unit,
    private val createConversationAction: (ConversationConfig) -> InferenceConversation,
    private val closeAction: () -> Unit,
) : InferenceEngine {
    private val stateLock = Any()
    private var isClosed = false

    constructor(engine: Engine) : this(
        initAction = { engine.initialize() },
        createConversationAction = { config -> DefaultInferenceConversation(engine.createConversation(config)) },
        closeAction = { engine.close() },
    )

    override fun initialize() {
        synchronized(stateLock) {
            check(!isClosed) { "Cannot initialize a closed engine." }
            initAction()
        }
    }

    override fun createConversation(config: ConversationConfig): InferenceConversation {
        synchronized(stateLock) {
            check(!isClosed) { "Cannot create conversation from a closed engine." }
            return createConversationAction(config)
        }
    }

    override fun close() {
        synchronized(stateLock) {
            if (isClosed) return
            isClosed = true
            closeAction()
        }
    }
}

/**
 * Default implementation that forwards calls to the native LiteRT-LM [com.google.ai.edge.litertlm.Conversation].
 */
class DefaultInferenceConversation internal constructor(
    private val sendAction: (String, MessageCallback) -> Unit,
    private val cancelAction: () -> Unit,
    private val closeAction: () -> Unit,
) : InferenceConversation {
    private val stateLock = Any()
    private var isClosed = false

    constructor(conversation: com.google.ai.edge.litertlm.Conversation) : this(
        sendAction = { input, callback -> conversation.sendMessageAsync(input, callback) },
        cancelAction = { conversation.cancelProcess() },
        closeAction = { conversation.close() },
    )

    override fun sendMessageAsync(
        input: String,
        callback: MessageCallback,
    ) {
        synchronized(stateLock) {
            check(!isClosed) { "Cannot send message on a closed conversation." }
            sendAction(input, callback)
        }
    }

    override fun cancelProcess() {
        synchronized(stateLock) {
            if (isClosed) return
            cancelAction()
        }
    }

    override fun close() {
        synchronized(stateLock) {
            if (isClosed) return
            isClosed = true
            closeAction()
        }
    }
}
