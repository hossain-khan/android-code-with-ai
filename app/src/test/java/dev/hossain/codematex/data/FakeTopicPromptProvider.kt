package dev.hossain.codematex.data

import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.TutorPersona

/**
 * In-memory fake of [dev.hossain.codematex.data.TopicPromptProvider] for unit tests.
 */
class FakeTopicPromptProvider(
    private val prompt: String = "Default system prompt",
) : TopicPromptProvider {
    val requestedTopics = mutableListOf<CodingTopic>()
    val requestedPersonas = mutableListOf<TutorPersona>()
    val requestedProfiles = mutableListOf<DeveloperProfile?>()

    override fun buildSystemPrompt(
        topic: CodingTopic,
        persona: TutorPersona,
        developerProfile: DeveloperProfile?,
    ): String {
        requestedTopics += topic
        requestedPersonas += persona
        requestedProfiles += developerProfile
        val profileTag = developerProfile?.formatPromptDirectives()?.let { "[$it]" }.orEmpty()
        return "[$topic][$persona]$profileTag $prompt"
    }
}
