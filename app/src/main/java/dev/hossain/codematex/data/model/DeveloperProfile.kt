package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Developer experience level for tailoring AI tutor explanation depth.
 */
@Serializable
enum class DeveloperExperienceLevel(
    val displayName: String,
    val description: String,
) {
    BEGINNER(
        displayName = "Beginner",
        description = "Learning language fundamentals, core syntax, and basic concepts.",
    ),
    INTERMEDIATE(
        displayName = "Intermediate",
        description = "Comfortable with standard libraries, common APIs, and application architecture.",
    ),
    SENIOR(
        displayName = "Senior",
        description = "Experienced in production systems, clean architecture, and performance tuning.",
    ),
    STAFF_LEAD(
        displayName = "Staff / Principal",
        description = "Focuses on high-level system design, edge cases, scalability, and deep trade-offs.",
    ),
}

/**
 * One-tap pre-crafted starter presets for instant profile configuration.
 */
@Serializable
enum class DeveloperProfilePreset(
    val title: String,
    val experienceLevel: DeveloperExperienceLevel,
    val primaryStack: String,
    val customDirectives: String,
) {
    ANDROID_KOTLIN(
        title = "📱 Android & Kotlin",
        experienceLevel = DeveloperExperienceLevel.SENIOR,
        primaryStack = "Kotlin, Jetpack Compose, Coroutines, Flow, Android Architecture",
        customDirectives =
            "Provide idiomatic Kotlin solutions using modern Jetpack Compose. " +
                "Skip basic syntax; focus on clean architecture, state hoisting, and lifecycle safety.",
    ),
    FULL_STACK_WEB(
        title = "🌐 Full-Stack & TypeScript",
        experienceLevel = DeveloperExperienceLevel.INTERMEDIATE,
        primaryStack = "TypeScript, React, Node.js, Next.js, REST & GraphQL",
        customDirectives =
            "Provide clean TypeScript examples with explicit types. " +
                "Compare new concepts to modern web and frontend design patterns.",
    ),
    SYSTEMS_BACKEND(
        title = "⚙️ Systems & Backend",
        experienceLevel = DeveloperExperienceLevel.SENIOR,
        primaryStack = "Go, Rust, Distributed Systems, Concurrency, PostgreSQL",
        customDirectives =
            "Focus on memory safety, concurrency models, error handling, and performance trade-offs. " +
                "Keep explanations concise, technical, and code-first.",
    ),
    CS_INTERVIEW(
        title = "🎓 CS Student & Algorithms",
        experienceLevel = DeveloperExperienceLevel.INTERMEDIATE,
        primaryStack = "Data Structures, Algorithms, Python, Java, Problem Solving",
        customDirectives =
            "Analyze time and space complexity (Big-O). " +
                "Highlight edge cases, recursive invariants, and walk through step-by-step dry runs.",
    ),
}

/**
 * Persistent user developer profile and context customization for AI tutoring.
 */
@Immutable
@Serializable
data class DeveloperProfile(
    val enabled: Boolean = false,
    val experienceLevel: DeveloperExperienceLevel = DeveloperExperienceLevel.INTERMEDIATE,
    val primaryStack: String = "",
    val customDirectives: String = "",
) {
    /**
     * Formats this developer profile into a structured prompt block to be injected into LLM system prompts.
     * Returns an empty string if [enabled] is false or if no meaningful fields are configured.
     */
    fun formatPromptDirectives(): String {
        if (!enabled) return ""

        val lines = mutableListOf<String>()
        lines.add("=== USER DEVELOPER PROFILE ===")
        lines.add("- Experience Level: ${experienceLevel.displayName} (${experienceLevel.description})")

        if (primaryStack.isNotBlank()) {
            lines.add("- Primary Tech Stack: ${primaryStack.trim()}")
        }

        if (customDirectives.isNotBlank()) {
            lines.add("- Custom Directives & Preferences: ${customDirectives.trim()}")
        }

        return lines.joinToString(separator = "\n")
    }

    /**
     * Returns a compact summary string for display in settings row subtitles.
     */
    fun getDisplaySummary(): String {
        if (!enabled) return "Disabled"
        val stackSnippet =
            if (primaryStack.isNotBlank()) {
                " • " +
                    primaryStack
                        .split(",")
                        .firstOrNull()
                        ?.trim()
                        .orEmpty()
            } else {
                ""
            }
        return "${experienceLevel.displayName}$stackSnippet"
    }
}
