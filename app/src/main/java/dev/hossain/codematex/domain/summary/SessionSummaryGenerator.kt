package dev.hossain.codematex.domain.summary

import dev.hossain.codematex.data.model.ChatMessage

/**
 * Generates a short summary for a chat session.
 *
 * This abstraction decouples repositories from the LLM runtime. Implementations
 * may use on-device inference, cloud APIs, or heuristic summarization.
 */
interface SessionSummaryGenerator {
    /**
     * Returns a 1-2 sentence summary of [messages], or a fallback string if
     * generation fails or [messages] is empty.
     */
    suspend fun generateSummary(messages: List<ChatMessage>): String
}
