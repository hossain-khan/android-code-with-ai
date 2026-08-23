package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.CodingTopic

/**
 * In-memory fake of [TopicPromptProvider] for unit tests.
 */
class FakeTopicPromptProvider(
    private val prompt: String = "Default system prompt",
) : TopicPromptProvider {
    val requestedTopics = mutableListOf<CodingTopic>()

    override fun buildSystemPrompt(topic: CodingTopic): String {
        requestedTopics += topic
        return "[$topic] $prompt"
    }
}
