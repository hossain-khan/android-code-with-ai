package dev.hossain.codematex.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme

/**
 * Unified chat message bubble composable supporting user, agent, error, and system messages.
 *
 * Used across both active inference chat sessions and historical session inspection.
 * Provides consistent styling, Markdown rendering, syntax-highlighted code blocks,
 * tutor persona branding, and clipboard copying with haptic feedback.
 *
 * @param message The [ChatMessage] instance to render.
 * @param visualAccent The topic visual accent color used for tutor branding icons and badges.
 * @param modifier The modifier to apply to the message container.
 * @param onCopy Optional callback triggered when message text is copied to clipboard.
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    visualAccent: Color,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null,
) {
    when (message) {
        is ChatMessage.User -> {
            UserMessageBubble(
                message = message,
                modifier = modifier,
                onCopy = onCopy,
            )
        }

        is ChatMessage.Agent -> {
            AgentMessageBubble(
                message = message,
                visualAccent = visualAccent,
                modifier = modifier,
                onCopy = onCopy,
            )
        }

        is ChatMessage.Error -> {
            ErrorMessageBubble(
                message = message,
                modifier = modifier,
                onCopy = onCopy,
            )
        }

        is ChatMessage.System -> {
            SystemMessageBubble(
                message = message,
                modifier = modifier,
            )
        }
    }
}

/**
 * Renders user message text in a primary-container colored pill aligned to the right.
 * Long-pressing the bubble triggers haptic feedback and copies the message text to the clipboard.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserMessageBubble(
    message: ChatMessage.User,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val bubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape = bubbleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier =
                Modifier
                    .padding(start = 48.dp)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            copyMessageToClipboard(
                                context = context,
                                haptic = haptic,
                                label = "User message",
                                content = message.content,
                                onCopy = onCopy,
                            )
                        },
                    ),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * Renders agent/tutor responses aligned to the left in a surface-container card.
 * Features tutor persona glyph and header, an explicit copy action button,
 * and markdown-formatted content with code syntax highlighting.
 */
@Composable
fun AgentMessageBubble(
    message: ChatMessage.Agent,
    visualAccent: Color,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = visualAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "CodeMateX",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = visualAccent,
                        )
                        if (message.isStreaming) {
                            Text(
                                text = "• Generating…",
                                style = MaterialTheme.typography.labelSmall,
                                color = visualAccent.copy(alpha = 0.7f),
                            )
                        }
                    }

                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = {
                            copyMessageToClipboard(
                                context = context,
                                haptic = haptic,
                                label = "Chat message",
                                content = message.content,
                                onCopy = onCopy,
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                MarkdownMessage(
                    content = message.content.ifEmpty { "..." },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Renders error messages with error-container styling and a warning icon.
 */
@Composable
fun ErrorMessageBubble(
    message: ChatMessage.Error,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = {
                        copyMessageToClipboard(
                            context = context,
                            haptic = haptic,
                            label = "Error message",
                            content = message.message,
                            onCopy = onCopy,
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy error message",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * Renders subtle system notices as centered pills.
 */
@Composable
fun SystemMessageBubble(
    message: ChatMessage.System,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        ) {
            Text(
                text = message.info,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * Internal helper to copy message content to the clipboard, trigger haptic feedback,
 * invoke the [onCopy] callback if provided, and show a toast on Android < 13.
 */
private fun copyMessageToClipboard(
    context: Context,
    haptic: HapticFeedback?,
    label: String,
    content: String,
    onCopy: ((String) -> Unit)?,
) {
    haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, content))
    onCopy?.invoke(content)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
    }
}

@ThemePreviews
@Composable
private fun ChatMessageBubblePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChatMessageBubble(
                        message = ChatMessage.User("How do I filter a list in Kotlin?"),
                        visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    )
                    ChatMessageBubble(
                        message =
                            ChatMessage.Agent(
                                content =
                                    """You can use the `filter` function:
                                    |```kotlin
                                    |val numbers = listOf(1, 2, 3, 4, 5)
                                    |val evens = numbers.filter { it % 2 == 0 }
                                    |println(evens) // [2, 4]
                                    |```
                                    """.trimMargin(),
                            ),
                        visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    )
                    ChatMessageBubble(
                        message =
                            ChatMessage.Agent(
                                content = "Let me look that up for you...",
                                isStreaming = true,
                            ),
                        visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    )
                    ChatMessageBubble(
                        message = ChatMessage.Error("Failed to allocate KV cache: Out of memory."),
                        visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    )
                    ChatMessageBubble(
                        message = ChatMessage.System("Session restored from local database"),
                        visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    )
                }
            }
        }
    }
}
