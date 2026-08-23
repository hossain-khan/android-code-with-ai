package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Abstraction over system prompt construction for a coding topic and tutor persona.
 *
 * Decouples LLM prompt formatting from [ChatPresenter] so new prompting
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

    /**
     * Builds an interactive "Quiz Me" challenge prompt for [topic].
     */
    fun buildQuizPrompt(topic: CodingTopic): String

    /**
     * Builds an interactive "Find the Bug" puzzle prompt for [topic].
     */
    fun buildBugFinderPrompt(topic: CodingTopic): String

    /**
     * Builds an interactive "Optimize" prompt for [topic].
     */
    fun buildOptimizerPrompt(topic: CodingTopic): String
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultTopicPromptProvider
    @Inject
    constructor() : TopicPromptProvider {
        override fun buildSystemPrompt(
            topic: CodingTopic,
            persona: TutorPersona,
        ): String =
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

                TutorPersona.CODE_REVIEWER -> {
                    """You are a meticulous senior code reviewer specializing in ${topic.displayName}.

Instructions:
1. Review code and questions focusing on idiomatic style, potential bugs, edge cases, and memory/coroutine safety.
2. Point out anti-patterns or code smells and suggest concrete, improved code in markdown code blocks.
3. Highlight testability, security, and clean code practices in concise bullet points.
4. Be constructive, direct, and actionable.
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

        override fun buildQuizPrompt(topic: CodingTopic): String =
            "Generate a quick 1-question multiple-choice technical quiz about ${topic.displayName} with 4 options (A, B, C, D) to test my understanding. Do not reveal the answer yet."

        override fun buildBugFinderPrompt(topic: CodingTopic): String =
            "Show a short ${topic.displayName} code snippet containing a subtle bug or anti-pattern. Ask me to find and explain the bug without revealing the answer immediately."

        override fun buildOptimizerPrompt(topic: CodingTopic): String =
            "What are the top performance and memory optimization techniques in ${topic.displayName}? Provide 1 concise code snippet and key bullet points."
    }
