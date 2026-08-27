package dev.hossain.codematex.util

import dev.hossain.codematex.data.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenEstimatorTest {
    @Test
    fun `estimateTokens returns 0 for blank or empty string`() {
        assertEquals(0, TokenEstimator.estimateTokens(""))
        assertEquals(0, TokenEstimator.estimateTokens("   "))
    }

    @Test
    fun `estimateTokens estimates correct tokens using character ratio`() {
        // 37 characters / 3.7 = 10 tokens
        val text37Chars = "1234567890123456789012345678901234567"
        assertEquals(10, TokenEstimator.estimateTokens(text37Chars))
    }

    @Test
    fun `estimateConversationTokens returns 0 for empty messages and null system prompt`() {
        assertEquals(0, TokenEstimator.estimateConversationTokens(null, emptyList()))
        assertEquals(0, TokenEstimator.estimateConversationTokens("", emptyList()))
    }

    @Test
    fun `estimateConversationTokens accumulates system prompt and message lengths`() {
        val systemPrompt = "You are a helpful coding tutor." // 31 chars
        val messages =
            listOf(
                ChatMessage.User(content = "How to sort a list in Kotlin?"), // 30 chars
                ChatMessage.Agent(content = "Use `list.sorted()` to return a sorted list."), // 44 chars
            )
        // Total chars = 31 + 30 + 44 = 105 chars
        // 105 / 3.7 = 28.37 -> ceil = 29
        val estimated = TokenEstimator.estimateConversationTokens(systemPrompt, messages)
        assertEquals(29, estimated)
    }
}
