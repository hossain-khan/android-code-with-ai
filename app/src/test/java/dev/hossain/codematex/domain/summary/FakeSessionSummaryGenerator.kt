package dev.hossain.codematex.domain.summary

import dev.hossain.codematex.data.model.ChatMessage

class FakeSessionSummaryGenerator : SessionSummaryGenerator {
    var summaryToReturn: String = ""
    var shouldThrow: Exception? = null
    var generateSummaryCalls = 0
        private set

    override suspend fun generateSummary(messages: List<ChatMessage>): String {
        generateSummaryCalls++
        if (shouldThrow != null) throw shouldThrow!!
        return summaryToReturn
    }
}
