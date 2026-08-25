package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.TopicPromptProvider
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona

/**
 * In-memory fake of [dev.hossain.codematex.data.TopicPromptProvider] for unit tests.
 */
class FakeTopicPromptProvider(
    private val prompt: String = "Default system prompt",
) : TopicPromptProvider {
    val requestedTopics = mutableListOf<CodingTopic>()
    val requestedPersonas = mutableListOf<TutorPersona>()

    override fun buildSystemPrompt(
        topic: CodingTopic,
        persona: TutorPersona,
    ): String {
        requestedTopics += topic
        requestedPersonas += persona
        return "[$topic][$persona] $prompt"
    }
}
