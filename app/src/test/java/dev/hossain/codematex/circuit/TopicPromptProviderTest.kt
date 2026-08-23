package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DefaultTopicPromptProvider].
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
    fun `buildSystemPrompt adapts to code reviewer persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.PYTHON, TutorPersona.CODE_REVIEWER)

        assertTrue(prompt.contains("Python"))
        assertTrue(prompt.contains("code reviewer"))
        assertTrue(prompt.contains("anti-patterns"))
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

    @Test
    fun `buildQuizPrompt contains topic name and quiz instructions`() {
        val prompt = provider.buildQuizPrompt(CodingTopic.KOTLIN)

        assertTrue(prompt.contains("Kotlin"))
        assertTrue(prompt.contains("multiple-choice"))
    }

    @Test
    fun `buildBugFinderPrompt contains topic name and bug finding instructions`() {
        val prompt = provider.buildBugFinderPrompt(CodingTopic.RUST)

        assertTrue(prompt.contains("Rust"))
        assertTrue(prompt.contains("subtle bug"))
    }

    @Test
    fun `buildOptimizerPrompt contains topic name and optimization instructions`() {
        val prompt = provider.buildOptimizerPrompt(CodingTopic.SWIFT)

        assertTrue(prompt.contains("Swift"))
        assertTrue(prompt.contains("optimization"))
    }
}
