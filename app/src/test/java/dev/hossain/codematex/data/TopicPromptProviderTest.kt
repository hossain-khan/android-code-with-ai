package dev.hossain.codematex.data

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DeveloperExperienceLevel
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.TutorPersona
import org.junit.Test

/**
 * Unit tests for [DefaultTopicPromptProvider].
 */
class TopicPromptProviderTest {
    private val provider = DefaultTopicPromptProvider()

    @Test
    fun `buildSystemPrompt includes topic display name for senior engineer persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.KOTLIN, TutorPersona.SENIOR_ENGINEER)

        assertThat(prompt).contains("Kotlin")
        assertThat(prompt).contains("senior principal engineer")
        assertThat(prompt).contains("concise")
    }

    @Test
    fun `buildSystemPrompt adapts to beginner friendly persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.ANDROID, TutorPersona.BEGINNER_FRIENDLY)

        assertThat(prompt).contains("Android")
        assertThat(prompt).contains("beginner-friendly")
        assertThat(prompt).contains("analogies")
    }

    @Test
    fun `buildSystemPrompt adapts to interview coach persona`() {
        val prompt = provider.buildSystemPrompt(CodingTopic.GO, TutorPersona.INTERVIEW_COACH)

        assertThat(prompt).contains("Go")
        assertThat(prompt).contains("interview coach")
        assertThat(prompt).contains("Big-O")
    }

    @Test
    fun `buildSystemPrompt differs by topic`() {
        val kotlinPrompt = provider.buildSystemPrompt(CodingTopic.KOTLIN)
        val pythonPrompt = provider.buildSystemPrompt(CodingTopic.PYTHON)

        assertThat(kotlinPrompt).contains("Kotlin")
        assertThat(pythonPrompt).contains("Python")
        assertThat(pythonPrompt).isEqualTo(kotlinPrompt.replace("Kotlin", "Python"))
    }

    @Test
    fun `buildSystemPrompt includes anti-thinking critical rule across all personas`() {
        for (persona in TutorPersona.entries) {
            val prompt = provider.buildSystemPrompt(CodingTopic.KOTLIN, persona)
            assertThat(prompt).contains("CRITICAL RULE:")
            assertThat(prompt).contains("Do NOT generate or output any internal thoughts")
            assertThat(prompt).contains("Output ONLY the final direct response")
        }
    }

    @Test
    fun `given enabled developer profile then buildSystemPrompt includes formatted profile directives`() {
        val profile =
            DeveloperProfile(
                enabled = true,
                experienceLevel = DeveloperExperienceLevel.STAFF_LEAD,
                primaryStack = "Kotlin, Rust, Compose Multiplatform",
                customDirectives = "Skip beginner fluff. Focus on concurrency and zero-cost abstractions.",
            )

        val prompt = provider.buildSystemPrompt(CodingTopic.KOTLIN, TutorPersona.SENIOR_ENGINEER, profile)

        assertThat(prompt).contains("=== USER DEVELOPER PROFILE ===")
        assertThat(prompt).contains("Experience Level: Staff / Principal")
        assertThat(prompt).contains("Primary Tech Stack: Kotlin, Rust, Compose Multiplatform")
        assertThat(prompt).contains("Custom Directives & Preferences: Skip beginner fluff.")
    }

    @Test
    fun `given disabled developer profile then buildSystemPrompt omits profile block`() {
        val profile =
            DeveloperProfile(
                enabled = false,
                experienceLevel = DeveloperExperienceLevel.STAFF_LEAD,
                primaryStack = "Kotlin, Rust",
                customDirectives = "Skip beginner fluff.",
            )

        val prompt = provider.buildSystemPrompt(CodingTopic.KOTLIN, TutorPersona.SENIOR_ENGINEER, profile)

        assertThat(prompt).doesNotContain("=== USER DEVELOPER PROFILE ===")
    }
}
