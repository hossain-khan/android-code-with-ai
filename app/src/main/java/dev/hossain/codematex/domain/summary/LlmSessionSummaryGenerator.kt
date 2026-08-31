package dev.hossain.codematex.domain.summary

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import timber.log.Timber
import javax.inject.Inject

@SingleIn(AppScope::class)
class LlmSessionSummaryGenerator
    @Inject
    constructor(
        private val llmEngine: LlmEngine,
    ) : SessionSummaryGenerator {
        override suspend fun generateSummary(messages: List<ChatMessage>): String {
            val startTime = System.currentTimeMillis()
            val conversationText =
                messages
                    .joinToString("\n") { msg ->
                        when (msg) {
                            is ChatMessage.User -> "User: ${msg.content}"
                            is ChatMessage.Agent -> "AI: ${msg.content.take(MAX_AGENT_CONTENT_LENGTH)}"
                            else -> ""
                        }
                    }.take(MAX_CONVERSATION_LENGTH)

            if (conversationText.isBlank()) {
                Timber.d("LlmSessionSummaryGenerator: Empty or non-text messages list, returning fallback summary")
                return "Empty session"
            }

            Timber.d(
                "LlmSessionSummaryGenerator: Starting on-device LLM summary inference for %d messages (transcript length=%d)",
                messages.size,
                conversationText.length,
            )

            var summary = ""
            try {
                llmEngine.runInferenceIsolated(
                    input = "Summarize this coding learning session in 1-2 sentences: $conversationText",
                ) { token, done ->
                    if (!done) {
                        summary += token
                    }
                }
                val durationMs = System.currentTimeMillis() - startTime
                Timber.d(
                    "LlmSessionSummaryGenerator: LLM summary generated in %d ms: '%s'",
                    durationMs,
                    summary.trim(),
                )
            } catch (e: Exception) {
                val durationMs = System.currentTimeMillis() - startTime
                Timber.w(
                    e,
                    "LlmSessionSummaryGenerator: LLM summary generation failed after %d ms, using fallback",
                    durationMs,
                )
            }

            return summary.ifBlank { "Coding session about ${messages.size} messages" }
        }

        private companion object {
            private const val MAX_AGENT_CONTENT_LENGTH = 200
            private const val MAX_CONVERSATION_LENGTH = 1000
        }
    }
