package dev.hossain.codematex.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeveloperProfileTest {
    @Test
    fun `formatPromptDirectives returns empty string when disabled`() {
        val profile =
            DeveloperProfile(
                enabled = false,
                experienceLevel = DeveloperExperienceLevel.SENIOR,
                primaryStack = "Kotlin, Compose",
                customDirectives = "Direct and concise",
            )
        assertThat(profile.formatPromptDirectives()).isEmpty()
    }

    @Test
    fun `formatPromptDirectives formats all fields when enabled`() {
        val profile =
            DeveloperProfile(
                enabled = true,
                experienceLevel = DeveloperExperienceLevel.STAFF_LEAD,
                primaryStack = "Go, Distributed Systems",
                customDirectives = "Focus on concurrency and memory safety.",
            )
        val formatted = profile.formatPromptDirectives()
        assertThat(formatted).contains("=== USER DEVELOPER PROFILE ===")
        assertThat(
            formatted,
        ).contains(
            "- Experience Level: Staff / Principal (Focuses on high-level system design, edge cases, scalability, and deep trade-offs.)",
        )
        assertThat(formatted).contains("- Primary Tech Stack: Go, Distributed Systems")
        assertThat(formatted).contains("- Custom Directives & Preferences: Focus on concurrency and memory safety.")
    }

    @Test
    fun `formatPromptDirectives omits blank stack and directives`() {
        val profile =
            DeveloperProfile(
                enabled = true,
                experienceLevel = DeveloperExperienceLevel.BEGINNER,
                primaryStack = "   ",
                customDirectives = "",
            )
        val formatted = profile.formatPromptDirectives()
        assertThat(formatted).contains("- Experience Level: Beginner")
        assertThat(formatted).doesNotContain("Primary Tech Stack")
        assertThat(formatted).doesNotContain("Custom Directives")
    }

    @Test
    fun `getDisplaySummary returns Disabled when not enabled`() {
        val profile = DeveloperProfile(enabled = false)
        assertThat(profile.getDisplaySummary()).isEqualTo("Disabled")
    }

    @Test
    fun `getDisplaySummary returns level and first stack token when enabled`() {
        val profile =
            DeveloperProfile(
                enabled = true,
                experienceLevel = DeveloperExperienceLevel.SENIOR,
                primaryStack = "Kotlin, Jetpack Compose, Coroutines",
            )
        assertThat(profile.getDisplaySummary()).isEqualTo("Senior • Kotlin")
    }

    @Test
    fun `getDisplaySummary returns only level when stack is empty`() {
        val profile =
            DeveloperProfile(
                enabled = true,
                experienceLevel = DeveloperExperienceLevel.INTERMEDIATE,
                primaryStack = "",
            )
        assertThat(profile.getDisplaySummary()).isEqualTo("Intermediate")
    }

    @Test
    fun `developer profile presets have valid configurations`() {
        DeveloperProfilePreset.entries.forEach { preset ->
            assertThat(preset.title).isNotEmpty()
            assertThat(preset.primaryStack).isNotEmpty()
            assertThat(preset.customDirectives).isNotEmpty()
            assertThat(preset.experienceLevel).isNotNull()
        }
    }
}
