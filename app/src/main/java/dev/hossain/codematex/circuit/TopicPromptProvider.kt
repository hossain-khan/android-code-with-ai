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
               |Explain concepts clearly with examples. Use markdown for code blocks.
               |Keep explanations concise but thorough.
            """.trimMargin()
    }
