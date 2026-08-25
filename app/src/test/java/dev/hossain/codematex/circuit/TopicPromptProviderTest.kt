package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.DefaultTopicPromptProvider
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [dev.hossain.codematex.data.DefaultTopicPromptProvider].
 */
class TopicPromptProviderTest {
    private val provider = DefaultTopicPromptProvider()

    @Test
    fun `buildSystemPrompt includes topic display name for senior engineer persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.KOTLIN, TutorPersona.SENIOR_ENGINEER)

        assertTrue(prompt.contains("Kotlin"))
        assertTrue(prompt.contains("senior principal engineer"))
        assertTrue(prompt.contains("concise"))
    }

    @Test
    fun `buildSystemPrompt adapts to beginner friendly persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.ANDROID, TutorPersona.BEGINNER_FRIENDLY)

        assertTrue(prompt.contains("Android"))
        assertTrue(prompt.contains("beginner-friendly"))
        assertTrue(prompt.contains("analogies"))
    }

    @Test
    fun `buildSystemPrompt adapts to interview coach persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.GO, TutorPersona.INTERVIEW_COACH)

        assertTrue(prompt.contains("Go"))
        assertTrue(prompt.contains("interview coach"))
        assertTrue(prompt.contains("Big-O"))
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
}
