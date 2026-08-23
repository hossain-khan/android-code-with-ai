package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.CodingTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DefaultTopicPromptProvider].
 */
class TopicPromptProviderTest {
    private val provider = DefaultTopicPromptProvider()

    @Test
    fun `buildSystemPrompt includes topic display name`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.KOTLIN)

        assertTrue(prompt.contains("Kotlin"))
        assertTrue(prompt.contains("coding tutor"))
    }

    @Test
    fun `buildSystemPrompt uses markdown instructions`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.ANDROID)

        assertTrue(prompt.contains("markdown"))
        assertTrue(prompt.contains("code blocks"))
    }

    @Test
    fun `buildSystemPrompt differs by topic`() {
        val kotlinPrompt = provider.buildSystemPrompt(CodingTopic.KOTLIN)
        val pythonPrompt = provider.buildSystemPrompt(CodingTopic.PYTHON)

        assertTrue(kotlinPrompt.contains("Kotlin"))
        assertTrue(pythonPrompt.contains("Python"))
        assertEquals(
            kotlinPrompt.replace("Kotlin", "Python"),
            pythonPrompt,
        )
    }

    @Test
    fun `buildSystemPrompt includes conciseness and brevity instructions`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.GO)

        assertTrue(prompt.contains("concise"))
        assertTrue(prompt.contains("under 150 words"))
    }
}
