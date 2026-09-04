package dev.hossain.codematex.data.model

/**
 * Teaching personas that adapt the on-device LLM system prompt instructions.
 */
enum class TutorPersona(
    val id: String,
    val displayName: String,
    val shortName: String,
    val iconGlyph: String,
    val tagline: String,
    val description: String,
) {
    SENIOR_ENGINEER(
        id = "senior_engineer",
        displayName = "Senior Engineer",
        shortName = "Senior",
        iconGlyph = "⚡",
        tagline = "Code-first, minimal fluff & production patterns",
        description =
            "Direct, concise, production-ready code with emphasis on performance, concurrency, and architecture trade-offs.",
    ),
    BEGINNER_FRIENDLY(
        id = "beginner_friendly",
        displayName = "Beginner Tutor",
        shortName = "Beginner",
        iconGlyph = "🎓",
        tagline = "Intuitive analogies & step-by-step breakdowns",
        description =
            "Supportive, analogy-driven explanations with commented code snippets breaking down foundational syntax step-by-step.",
    ),
    INTERVIEW_COACH(
        id = "interview_coach",
        displayName = "Interview Coach",
        shortName = "Interview",
        iconGlyph = "💼",
        tagline = "Time/Space complexity & algorithmic trade-offs",
        description =
            "Deep dives into Big-O complexity, optimal algorithmic alternatives, edge cases, and common interviewer follow-ups.",
    ),
}
