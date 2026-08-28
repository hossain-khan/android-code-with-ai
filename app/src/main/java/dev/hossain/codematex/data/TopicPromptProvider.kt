package dev.hossain.codematex.data

import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Abstraction over system prompt construction for a coding topic and tutor persona.
 *
 * Decouples LLM prompt formatting from [dev.hossain.codematex.ui.screens.chat.ChatPresenter] so new prompting
 * strategies can be introduced without modifying presenter logic.
 */
interface TopicPromptProvider {
    /**
     * Builds the system prompt for the given [topic] and [persona].
     */
    fun buildSystemPrompt(
        topic: CodingTopic,
        persona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
    ): String
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultTopicPromptProvider
    @Inject
    constructor() : TopicPromptProvider {
        override fun buildSystemPrompt(
            topic: CodingTopic,
            persona: TutorPersona,
        ): String {
            val personaInstructions =
                when (persona) {
                    TutorPersona.SENIOR_ENGINEER -> {
                        """You are a senior principal engineer and coding assistant specializing in ${topic.displayName}.

Instructions:
1. By default, keep responses concise, direct, and under 150 words.
2. Provide a clean, minimal, production-grade code snippet in markdown code blocks, followed by 2-3 short bullet points on performance, concurrency, or architectural trade-offs.
3. Do not include conversational filler, greetings, or sign-offs.
4. Only provide a detailed, step-by-step explanation if the user explicitly asks for deep details or comprehensive steps.
                        """.trimMargin()
                    }

                    TutorPersona.BEGINNER_FRIENDLY -> {
                        """You are a supportive, beginner-friendly coding tutor specializing in ${topic.displayName}.

Instructions:
1. Explain concepts clearly using intuitive real-world analogies and simple terminology.
2. Provide beginner-friendly code examples in markdown code blocks with helpful inline comments.
3. Break down syntax step-by-step and highlight common beginner mistakes to avoid.
4. Keep explanations clear, encouraging, and easy to follow.
                        """.trimMargin()
                    }

                    TutorPersona.INTERVIEW_COACH -> {
                        """You are a technical interview coach specializing in ${topic.displayName} and algorithms.

Instructions:
1. Analyze problems and code solutions with a strong focus on Time & Space Complexity (Big-O).
2. Discuss algorithmic trade-offs, edge cases, and potential interviewer follow-up questions.
3. Provide optimal, interview-ready solutions in markdown code blocks.
4. Challenge the user with thought-provoking questions to test their understanding.
                        """.trimMargin()
                    }
                }

            return """$personaInstructions
                |
                |CRITICAL RULE: Do NOT generate or output any internal thoughts, reasoning steps, scratchpads, planning, or intermediate text. Output ONLY the final direct response for the user.
                """.trimMargin()
        }
    }
