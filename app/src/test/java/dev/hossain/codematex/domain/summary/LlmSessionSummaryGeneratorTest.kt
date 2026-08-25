package dev.hossain.codematex.domain.summary

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.runtime.FakeLlmEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmSessionSummaryGeneratorTest {
    private val fakeEngine = FakeLlmEngine()
    private val generator = LlmSessionSummaryGenerator(fakeEngine)

    @Test
    fun `given empty messages - returns empty session`() =
        runTest {
            val summary = generator.generateSummary(emptyList())

            assertEquals("Empty session", summary)
            assertNull(fakeEngine.lastInput)
        }

    @Test
    fun `given blank conversation text - returns empty session`() =
        runTest {
            val summary = generator.generateSummary(listOf(ChatMessage.System(" ")))

            assertEquals("Empty session", summary)
        }

    @Test
    fun `given user and agent messages - joins conversation and runs inference`() =
        runTest {
            fakeEngine.responseTokens = listOf("A ", "Kotlin ", "summary", "")
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            val summary = generator.generateSummary(messages)

            assertEquals("A Kotlin summary", summary)
            assertTrue(fakeEngine.lastInput?.contains("Summarize this coding learning session") == true)
            assertTrue(fakeEngine.lastInput?.contains("User: Explain Kotlin coroutines") == true)
            assertTrue(fakeEngine.lastInput?.contains("AI: Coroutines are lightweight threads.") == true)
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
            assertEquals(200, agentContentLength)
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

            assertEquals("Coding session about 2 messages", summary)
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

            assertEquals("Coding session about 2 messages", summary)
        }
}
