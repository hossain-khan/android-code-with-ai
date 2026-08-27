package dev.hossain.codematex.domain.summary

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.runtime.FakeLlmEngine
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LlmSessionSummaryGeneratorTest {
    private val fakeEngine = FakeLlmEngine()
    private val generator = LlmSessionSummaryGenerator(fakeEngine)

    @Test
    fun `given empty messages - returns empty session`() =
        runTest {
            val summary = generator.generateSummary(emptyList())

            assertThat(summary).isEqualTo("Empty session")
            assertThat(fakeEngine.lastInput).isNull()
        }

    @Test
    fun `given blank conversation text - returns empty session`() =
        runTest {
            val summary = generator.generateSummary(listOf(ChatMessage.System(" ")))

            assertThat(summary).isEqualTo("Empty session")
        }

    @Test
    fun `given user and agent messages - joins conversation and runs isolated inference`() =
        runTest {
            fakeEngine.responseTokens = listOf("A ", "Kotlin ", "summary", "")
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary).isEqualTo("A Kotlin summary")
            assertThat(fakeEngine.isolatedInferenceCalls).isEqualTo(1)
            assertThat(fakeEngine.lastInput).contains("Summarize this coding learning session")
            assertThat(fakeEngine.lastInput).contains("User: Explain Kotlin coroutines")
            assertThat(fakeEngine.lastInput).contains("AI: Coroutines are lightweight threads.")
        }

    @Test
    fun `given long agent message - truncates agent content`() =
        runTest {
            fakeEngine.responseTokens = listOf("Summary", "")
            val longAgentMessage = "a".repeat(500)
            val messages =
                listOf(
                    ChatMessage.User("Question"),
                    ChatMessage.Agent(longAgentMessage),
                )

            generator.generateSummary(messages)

            val agentContentLength =
                fakeEngine.lastInput
                    ?.substringAfter("AI: ")
                    ?.takeWhile { it == 'a' }
                    ?.length ?: 0
            assertThat(agentContentLength).isEqualTo(200)
        }

    @Test
    fun `given inference throws - returns fallback summary`() =
        runTest {
            fakeEngine.shouldThrow = RuntimeException("Inference failed")
            val messages =
                listOf(
                    ChatMessage.User("Question"),
                    ChatMessage.Agent("Answer"),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary).isEqualTo("Coding session about 2 messages")
        }

    @Test
    fun `given inference returns blank - returns fallback summary`() =
        runTest {
            fakeEngine.responseTokens = listOf("")
            val messages =
                listOf(
                    ChatMessage.User("Question"),
                    ChatMessage.Agent("Answer"),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary).isEqualTo("Coding session about 2 messages")
        }
}
