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

/**
 * Stable persistence IDs for [ChatMessage] subtypes. The [stableId] is stored in the database
 * instead of the sealed-class name or any other internal label so that message type mapping is
 * resilient to renames.
 */
enum class ChatMessageKind(
    val stableId: String,
) {
    USER("user"),
    AGENT("agent"),
    ERROR("error"),
    SYSTEM("system"),
    UNKNOWN("unknown"),
    ;

    companion object {
        /**
         * Returns the kind for the given [stableId], or [UNKNOWN] if the id does not match any
         * known kind.
         */
        fun fromStableId(stableId: String): ChatMessageKind = entries.find { it.stableId == stableId } ?: UNKNOWN
    }
}
