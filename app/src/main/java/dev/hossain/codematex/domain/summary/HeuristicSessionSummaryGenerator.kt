package dev.hossain.codematex.domain.summary

import dev.hossain.codematex.data.model.ChatMessage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import timber.log.Timber
import javax.inject.Inject

/**
 * Fast, deterministic, zero-resource heuristic summary generator.
 *
 * Extracts context from the user's initial question and subsequent conversation turns
 * without invoking the on-device Large Language Model. This eliminates JNI transitions,
 * engine mutex lock contention, and background inference thrashing during active chat.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class HeuristicSessionSummaryGenerator
    @Inject
    constructor() : SessionSummaryGenerator {
        override suspend fun generateSummary(messages: List<ChatMessage>): String {
            val userMessages = messages.filterIsInstance<ChatMessage.User>()
            if (userMessages.isEmpty()) {
                Timber.d("HeuristicSessionSummaryGenerator: Empty messages list, returning fallback summary")
                return "Empty session"
            }

            val firstQuestion = userMessages.first().content.trim()
            val questionCount = userMessages.size

            val summary =
                when {
                    questionCount == 1 -> {
                        if (firstQuestion.length <= MAX_SUMMARY_LENGTH) {
                            firstQuestion
                        } else {
                            "${firstQuestion.take(MAX_SUMMARY_LENGTH - 3).trimEnd()}..."
                        }
                    }

                    else -> {
                        val basePrompt =
                            if (firstQuestion.length <= TRUNCATED_PROMPT_LENGTH) {
                                firstQuestion
                            } else {
                                "${firstQuestion.take(TRUNCATED_PROMPT_LENGTH - 3).trimEnd()}..."
                            }
                        "Discussion on \"$basePrompt\" ($questionCount questions)"
                    }
                }

            Timber.d(
                "HeuristicSessionSummaryGenerator: Generated summary for %d messages (%d user turns): '%s'",
                messages.size,
                questionCount,
                summary,
            )
            return summary
        }

        private companion object {
            private const val MAX_SUMMARY_LENGTH = 120
            private const val TRUNCATED_PROMPT_LENGTH = 60
        }
    }
