package dev.hossain.codematex.util

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ChatMessage
import org.junit.Test

class TokenEstimatorTest {
    @Test
    fun `estimateTokens returns 0 for blank or empty string`() {
        assertThat(TokenEstimator.estimateTokens("")).isEqualTo(0)
        assertThat(TokenEstimator.estimateTokens("   ")).isEqualTo(0)
    }

    @Test
    fun `estimateTokens estimates correct tokens using character ratio`() {
        // 37 characters / 3.7 = 10 tokens
        val text37Chars = "1234567890123456789012345678901234567"
        assertThat(TokenEstimator.estimateTokens(text37Chars)).isEqualTo(10)
    }

    @Test
    fun `estimateConversationTokens returns 0 for empty messages and null system prompt`() {
        assertThat(TokenEstimator.estimateConversationTokens(null, emptyList())).isEqualTo(0)
        assertThat(TokenEstimator.estimateConversationTokens("", emptyList())).isEqualTo(0)
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
        assertThat(estimated).isEqualTo(29)
    }
}
