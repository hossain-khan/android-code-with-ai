package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
sealed class ChatMessage {
    abstract val id: String

    data class User(
        val content: String,
        override val id: String = UUID.randomUUID().toString(),
    ) : ChatMessage()

    data class Agent(
        val content: String,
        val isStreaming: Boolean = false,
        override val id: String = UUID.randomUUID().toString(),
    ) : ChatMessage()

    data class Error(
        val message: String,
        override val id: String = UUID.randomUUID().toString(),
    ) : ChatMessage()

    data class System(
        val info: String,
        override val id: String = UUID.randomUUID().toString(),
    ) : ChatMessage()
}
