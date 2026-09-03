package dev.hossain.codematex.data.model

/**
 * Coding topics with stable persistence IDs. The [stableId] is stored in the database instead of
 * the enum name so that renaming an enum entry does not break existing sessions.
 */
enum class CodingTopic(
    val displayName: String,
    val stableId: String,
) {
    SYSTEM_DESIGN("System Design", "system-design"),
    WEB("Web Development", "web"),
    KOTLIN("Kotlin", "kotlin"),
    PYTHON("Python", "python"),
    JAVASCRIPT("JavaScript", "javascript"),
    TYPESCRIPT("TypeScript", "typescript"),
    RUST("Rust", "rust"),
    GO("Go", "go"),
    SWIFT("Swift", "swift"),
    ALGORITHMS("Algorithms & Data Structures", "algorithms"),
    ANDROID("Android Development", "android"),
    UNKNOWN("Unknown", "unknown"),
    ;

    companion object {
        /**
         * Returns the topic for the given [stableId], or [UNKNOWN] if the id does not match any
         * known topic. This prevents corrupt or legacy values from crashing the sessions list.
         */
        fun fromStableId(stableId: String): CodingTopic = entries.find { it.stableId == stableId } ?: UNKNOWN

        /**
         * Topics that can be selected by the user. [UNKNOWN] is excluded because it only exists as
         * a recoverable fallback for corrupt or legacy persisted data.
         */
        val selectableEntries: List<CodingTopic> = entries.filter { it != UNKNOWN }
    }
}
