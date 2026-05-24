package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatSession(
    val id: String,
    val topic: CodingTopic,
    val title: String,
    val summary: String,
    val messageCount: Int,
    val lastActiveAt: Long,
    val modelUsed: String,
)
