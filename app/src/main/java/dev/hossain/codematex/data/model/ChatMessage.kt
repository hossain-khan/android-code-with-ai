package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class ChatMessage {
    data class User(val content: String) : ChatMessage()
    data class Agent(val content: String, val isStreaming: Boolean = false) : ChatMessage()
    data class Error(val message: String) : ChatMessage()
    data class System(val info: String) : ChatMessage()
}
