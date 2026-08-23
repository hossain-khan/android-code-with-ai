package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.CodingTopic
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Abstraction over system prompt construction for a coding topic.
 *
 * Decouples LLM prompt formatting from [ChatPresenter] so new prompting
 * strategies can be introduced without modifying presenter logic.
 */
interface TopicPromptProvider {
    /**
     * Builds the system prompt for the given [topic].
     */
    fun buildSystemPrompt(topic: CodingTopic): String
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultTopicPromptProvider
    @Inject
    constructor() : TopicPromptProvider {
        override fun buildSystemPrompt(topic: CodingTopic): String =
            """You are a coding tutor specializing in ${topic.displayName}.
               |
               |Instructions:
               |1. By default, keep responses concise, direct, and under 150 words.
               |2. Provide a clean, minimal code snippet in markdown code blocks, followed by 2-3 short bullet points.
               |3. Do not include conversational filler, greetings, or sign-offs.
               |4. Only provide a detailed, step-by-step explanation if the user explicitly asks for deep details or comprehensive steps.
            """.trimMargin()
    }
