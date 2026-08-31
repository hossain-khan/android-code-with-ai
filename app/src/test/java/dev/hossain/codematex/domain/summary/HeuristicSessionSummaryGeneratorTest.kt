package dev.hossain.codematex.domain.summary

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ChatMessage
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HeuristicSessionSummaryGeneratorTest {
    private val generator = HeuristicSessionSummaryGenerator()

    @Test
    fun `given empty messages - returns empty session fallback`() =
        runTest {
            val summary = generator.generateSummary(emptyList())

            assertThat(summary).isEqualTo("Empty session")
        }

    @Test
    fun `given only agent or system messages - returns empty session fallback`() =
        runTest {
            val messages =
                listOf(
                    ChatMessage.System("You are a helpful tutor."),
                    ChatMessage.Agent("Hello, how can I help?"),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary).isEqualTo("Empty session")
        }

    @Test
    fun `given single user question - returns full prompt if within length limit`() =
        runTest {
            val messages =
                listOf(
                    ChatMessage.User("How do Kotlin coroutines work?"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary).isEqualTo("How do Kotlin coroutines work?")
        }

    @Test
    fun `given single long user question - truncates with ellipsis`() =
        runTest {
            val longQuestion =
                "Explain the detailed differences between StateFlow and SharedFlow with cold streams " +
                    "and hot streams in Android Jetpack Compose architectures."
            val messages =
                listOf(
                    ChatMessage.User(longQuestion),
                    ChatMessage.Agent("StateFlow is a state-holder observable..."),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary.length).isAtMost(120)
            assertThat(summary).endsWith("...")
        }

    @Test
    fun `given multi-turn conversation - returns discussion summary with turn count`() =
        runTest {
            val messages =
                listOf(
                    ChatMessage.User("What is Jetpack Compose?"),
                    ChatMessage.Agent("Compose is Android's modern toolkit for building native UI."),
                    ChatMessage.User("How does recomposition work?"),
                    ChatMessage.Agent("Recomposition is the process of calling composables again when inputs change."),
                    ChatMessage.User("Give me a code snippet"),
                    ChatMessage.Agent("```kotlin\n@Composable fun Greeting() {}\n```"),
                )

            val summary = generator.generateSummary(messages)

            assertThat(summary).isEqualTo("Discussion on \"What is Jetpack Compose?\" (3 questions)")
        }
}
