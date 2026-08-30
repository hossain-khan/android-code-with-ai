package dev.hossain.codematex.data

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.hossain.codematex.runtime.BackendFailureException
import dev.hossain.codematex.runtime.DEV_STUB_MODEL_PATH
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.runtime.LlmEngine.Backend
import dev.hossain.codematex.runtime.LowMemoryException
import dev.hossain.codematex.system.MemoryHeadroomResult
import dev.hossain.codematex.system.SystemMemoryManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Events emitted by [ChatInferenceOrchestrator.sendMessage].
 */
sealed interface ChatInferenceEvent {
    /**
     * A partial token produced by the LLM.
     */
    data class Token(
        val partialToken: String,
    ) : ChatInferenceEvent

    /**
     * Emitted when the LLM finishes generating the response.
     */
    data object Done : ChatInferenceEvent

    /**
     * Emitted when inference failed on a hardware backend and a fallback attempt is about to start.
     * The presenter should discard any partial output from the failed backend.
     */
    data class BackendFailed(
        val backend: LlmEngine.Backend,
    ) : ChatInferenceEvent
}

/**
 * Abstraction over the LLM inference lifecycle for a chat session.
 *
 * Encapsulates model initialization, history restoration, inference streaming,
 * stop/reset, and retry coordination so that [dev.hossain.codematex.ui.screens.chat.ChatPresenter] can focus on UI
 * state management.
 */
interface ChatInferenceOrchestrator {
    /**
     * Initializes the LLM for [model] and restores history for [sessionId] when
     * provided.
     *
     * @param existingMessages messages already loaded in the UI; used to avoid
     *        reloading from disk when possible.
     * @return [Result.success] containing the messages that should be displayed
     *         (loaded from disk if [existingMessages] was empty) when
     *         initialization and history restoration complete, or
     *         [Result.failure] otherwise.
     */
    suspend fun initialize(
        model: AiModel,
        topic: CodingTopic,
        sessionId: String?,
        existingMessages: List<ChatMessage>,
        persona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
    ): Result<List<ChatMessage>>

    /**
     * Stops an ongoing inference.
     */
    fun stop()

    /**
     * Resets the conversation using the system prompt for [topic] and [persona].
     */
    suspend fun resetConversation(
        topic: CodingTopic,
        persona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
    )

    /**
     * Switches the persona by resetting the conversation with the new persona's system prompt
     * and restoring the given [messages].
     */
    suspend fun switchPersona(
        topic: CodingTopic,
        persona: TutorPersona,
        messages: List<ChatMessage>,
    )

    /**
     * Returns the currently active backend, or null if the engine is not
     * initialized.
     */
    fun getActiveBackend(): Backend?

    /**
     * Sends [input] to the LLM and returns a [Flow] of inference events.
     *
     * The flow emits [ChatInferenceEvent.Token] for each partial token,
     * [ChatInferenceEvent.BackendFailed] when a hardware backend fails and a
     * fallback attempt is starting, and [ChatInferenceEvent.Done] once generation
     * completes. Errors terminate the flow with an exception.
     */
    suspend fun sendMessage(input: String): Flow<ChatInferenceEvent>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultChatInferenceOrchestrator
    @Inject
    constructor(
        private val llmEngine: LlmEngine,
        private val sessionRepository: ChatSessionRepository,
        private val configStore: ModelConfigStore,
        private val topicPromptProvider: TopicPromptProvider,
        private val systemMemoryManager: SystemMemoryManager,
        private val userPreferencesStore: UserPreferencesStore,
    ) : ChatInferenceOrchestrator {
        override suspend fun initialize(
            model: AiModel,
            topic: CodingTopic,
            sessionId: String?,
            existingMessages: List<ChatMessage>,
            persona: TutorPersona,
        ): Result<List<ChatMessage>> =
            try {
                // Pre-flight memory headroom check:
                // If model is a real downloaded model (not a stub), verify the device has sufficient available RAM.
                // On-device LLMs allocate 1.5GB–3.5GB of physical RAM across native C++ and GPU buffers.
                // Failing to validate available RAM before loading can trigger kernel Low Memory Killer (LMK) kills.
                // See: https://developer.android.com/topic/performance/memory/manage-app-memory#CheckMemory
                if (model.localPath != null && model.localPath != DEV_STUB_MODEL_PATH) {
                    val headroomResult = systemMemoryManager.checkMemoryHeadroom()
                    if (headroomResult is MemoryHeadroomResult.Constrained) {
                        Timber.w(
                            "ChatInferenceOrchestrator [PRE_FLIGHT_OOM_GUARD]: Blocking model load due to memory pressure " +
                                "(available=%d bytes, required=%d bytes)",
                            headroomResult.availMemBytes,
                            headroomResult.requiredBytes,
                        )
                        throw LowMemoryException(
                            availMemBytes = headroomResult.availMemBytes,
                            requiredBytes = headroomResult.requiredBytes,
                        )
                    }
                }

                Timber.d(
                    "ChatInferenceOrchestrator [INIT_START]: Initializing model=${model.name}, path=${model.localPath}, persona=${persona.name}",
                )
                val modelConfig = configStore.getConfig(model.id)
                val devProfile = userPreferencesStore.getDeveloperProfile()
                llmEngine.initialize(
                    modelPath = model.localPath ?: "",
                    backend = model.preferredBackend,
                    systemInstruction = topicPromptProvider.buildSystemPrompt(topic, persona, devProfile),
                    config = modelConfig,
                )
                Timber.d("ChatInferenceOrchestrator: Model initialized successfully")

                val messagesToDisplay =
                    if (sessionId != null) {
                        val sessionMessages =
                            existingMessages.ifEmpty {
                                sessionRepository.getMessages(sessionId)
                            }
                        llmEngine.restoreHistory(sessionMessages)
                        sessionMessages
                    } else {
                        existingMessages
                    }

                Result.success(messagesToDisplay)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ChatInferenceOrchestrator: Model initialization failed")
                Result.failure(e)
            }

        override fun stop() {
            Timber.d("ChatInferenceOrchestrator: Stopping LLM engine")
            llmEngine.stop()
        }

        override suspend fun resetConversation(
            topic: CodingTopic,
            persona: TutorPersona,
        ) {
            Timber.d("ChatInferenceOrchestrator: Resetting conversation with persona=${persona.name}")
            val devProfile = userPreferencesStore.getDeveloperProfile()
            llmEngine.resetConversation(
                topicPromptProvider.buildSystemPrompt(topic, persona, devProfile),
                configStore.config,
            )
        }

        override suspend fun switchPersona(
            topic: CodingTopic,
            persona: TutorPersona,
            messages: List<ChatMessage>,
        ) {
            Timber.d("ChatInferenceOrchestrator: Switching persona to ${persona.name} and restoring ${messages.size} messages")
            val devProfile = userPreferencesStore.getDeveloperProfile()
            llmEngine.resetConversation(
                topicPromptProvider.buildSystemPrompt(topic, persona, devProfile),
                configStore.config,
            )
            if (messages.isNotEmpty()) {
                llmEngine.restoreHistory(messages)
            }
        }

        override fun getActiveBackend(): Backend? = llmEngine.getActiveBackend()

        override suspend fun sendMessage(input: String): Flow<ChatInferenceEvent> =
            callbackFlow {
                Timber.d("ChatInferenceOrchestrator: Starting inference. Input: '${input.take(100)}' (length: ${input.length})")

                suspend fun runAndEmit(input: String) {
                    llmEngine.runInference(input) { partialToken, done ->
                        if (partialToken.isNotEmpty()) {
                            trySend(ChatInferenceEvent.Token(partialToken))
                        }
                        if (done) {
                            trySend(ChatInferenceEvent.Done)
                            close()
                        }
                    }
                }

                val failedBackends = mutableSetOf<LlmEngine.Backend>()
                while (true) {
                    try {
                        runAndEmit(input)
                        break
                    } catch (e: BackendFailureException) {
                        if (e.failedBackend == LlmEngine.Backend.CPU || !failedBackends.add(e.failedBackend)) {
                            // CPU and repeated backend failures are terminal.
                            throw e
                        }
                        Timber.w(e, "ChatInferenceOrchestrator: Backend ${e.failedBackend} failed, signaling retry boundary")
                        send(ChatInferenceEvent.BackendFailed(e.failedBackend))
                    }
                }

                awaitClose {
                    // No explicit cleanup needed; the engine handles cancellation
                    // via stop() when the presenter leaves the composition.
                }
            }
    }
