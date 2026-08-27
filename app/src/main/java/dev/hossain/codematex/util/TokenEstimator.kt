package dev.hossain.codematex.util

import dev.hossain.codematex.data.model.ChatMessage
import kotlin.math.ceil

/**
 * Utility for estimating Large Language Model token counts from text and conversation history.
 *
 * Standard BPE and Byte-level Byte-Pair Encoding tokenizers (e.g. Gemma, Qwen, Tiktoken, Llama)
 * typically average approximately 3.7 to 4.0 characters per token for mixed English and source code.
 */
object TokenEstimator {
    /**
     * Average number of characters per token for English and source code.
     */
    const val CHARACTERS_PER_TOKEN = 3.7f

    /**
     * Estimates the token count for a raw string.
     */
    fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 0
        return ceil(text.length / CHARACTERS_PER_TOKEN).toInt()
    }

    /**
     * Estimates the total token footprint of a multi-turn conversation including
     * optional [systemInstruction] and all user/agent [messages].
     */
    fun estimateConversationTokens(
        systemInstruction: String?,
        messages: List<ChatMessage>,
    ): Int {
        var totalChars = systemInstruction?.length ?: 0
        for (msg in messages) {
            when (msg) {
                is ChatMessage.User -> {
                    totalChars += msg.content.length
                }

                is ChatMessage.Agent -> {
                    totalChars += msg.content.length
                }

                else -> {
                    // ChatMessage.System or other message types if any
                }
            }
        }
        if (totalChars <= 0) return 0
        return ceil(totalChars / CHARACTERS_PER_TOKEN).toInt()
    }
}
