package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.CoroutineScope

/**
 * Retained state holder for the chat screen.
 *
 * Owns the message list, loading flags, model selection, inference orchestration,
 * and system-stats monitoring so that [ChatPresenter] can focus on wiring state to
 * the Circuit UI layer.
 */
interface ChatStateHolder {
    val messages: List<ChatMessage>
    val currentSessionId: String?
    val isGenerating: Boolean
    val isPreparing: Boolean
    val persona: TutorPersona
    val errorMessage: String?
    val initTrigger: Int
    val throughputInfo: String?
    val systemStatsInfo: String?
    val availableModels: List<AiModel>
    val activeModel: AiModel?
    val activeBackend: String?

    /**
     * Attaches the coroutine scope supplied by the presenter. The scope is tied to
     * the composable lifecycle, so any work launched from the holder is cancelled
     * when the presenter leaves the composition.
     */
    fun attachScope(scope: CoroutineScope)

    /**
     * Loads the initially selected model and observes the list of available models.
     */
    fun loadAvailableModels()

    /**
     * Instantly loads prior messages from disk when the screen was opened with an
     * existing session ID.
     */
    fun loadSessionMessages()

    /**
     * Initializes the on-device model for the active model and persona.
     */
    suspend fun initializeModel()

    /**
     * Polls system stats while inference is active.
     */
    suspend fun monitorSystemStats()

    /**
     * Sends [text] to the LLM and streams the response into [messages].
     */
    fun sendMessage(text: String)

    /**
     * Stops an ongoing generation and marks the last agent message as no longer streaming.
     */
    fun stopGeneration()

    /**
     * Clears the message history and resets the LLM conversation.
     */
    fun resetSession()

    /**
     * Switches the tutor persona and resets the LLM conversation.
     */
    fun selectPersona(persona: TutorPersona)

    /**
     * Retries model initialization after a failure.
     */
    fun retry()

    /**
     * Placeholder for copying a message to the clipboard.
     */
    fun copyMessage(content: String)
}
