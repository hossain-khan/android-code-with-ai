package dev.hossain.codematex.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import kotlinx.coroutines.launch

/**
 * Renders the scrollable feed of chat messages using a reversed [LazyColumn].
 *
 * ### Auto-Scroll & User Intervention Business Logic:
 * 1. **Live Token Streaming**: While the LLM is generating (`state.isGenerating`), the list auto-scrolls
 *    to index 0 (the bottom of the feed) on each incoming token chunk.
 * 2. **User Manual Intervention**: Uses [collectIsDraggedAsState] on [listState]'s interaction source to
 *    reliably differentiate physical user touch/drag gestures from programmatic [LazyListState.scrollToItem] calls.
 * 3. **Viewport Freezing on Scroll-Up**: If the user actively touches/drags the screen to scroll up away from
 *    the bottom (`!isAtBottom`), auto-scrolling is immediately halted (`userScrolledUp = true`) so the user can
 *    read conversation history undisturbed by streaming token updates.
 * 4. **Auto-Scroll Resumption**: Auto-scrolling is automatically restored when:
 *    - The user scrolls or flings back to the bottom (`isAtBottom == true`).
 *    - The user taps the floating "Jump to Bottom ↓" / "New Response ↓" pill.
 *    - A new message is submitted / turn begins (detected via `state.messages.size` change).
 */
@Composable
internal fun ChatMessageList(
    state: ChatScreen.State.Active,
    listState: LazyListState,
    visualInfo: TopicVisualInfo,
    modifier: Modifier = Modifier,
    onCopyMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    // Track whether the user has explicitly scrolled up to view message history
    var userScrolledUp by remember { mutableStateOf(false) }

    // Track active user touch/drag gesture on the list (programmatic scrollToItem does not set isDragged)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    // Check if the viewport is currently anchored at the bottom (index 0 in reverseLayout)
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 60
        }
    }

    // Detect user manual scroll gestures:
    // 1. If the user actively drags away from the bottom, disable streaming auto-scrolling immediately.
    // 2. When the list settles or the user drags back to the bottom, re-enable auto-scrolling.
    LaunchedEffect(isDragged, isAtBottom) {
        if (isDragged) {
            if (!isAtBottom) {
                userScrolledUp = true
            }
        } else if (isAtBottom) {
            userScrolledUp = false
        }
    }

    // Auto-scroll to bottom whenever a new turn starts (user sent message or new session loaded)
    val messageCount = state.messages.size
    LaunchedEffect(messageCount) {
        userScrolledUp = false
        listState.scrollToItem(0)
    }

    // Auto-scroll during live token streaming as long as the user has not intervened by scrolling up
    val lastMessageContentLength = (state.messages.lastOrNull() as? ChatMessage.Agent)?.content?.length ?: 0
    LaunchedEffect(lastMessageContentLength, state.isGenerating) {
        if (state.isGenerating && !userScrolledUp) {
            listState.scrollToItem(0)
        }
    }

    // Floating pill visibility condition: show when user is scrolled away from bottom
    val showJumpToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isGenerating) {
                item {
                    GeneratingIndicator(visualInfo.accentColor)
                }
            }
            items(state.messages.reversed(), key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    visualAccent = visualInfo.accentColor,
                    onCopy = onCopyMessage,
                )
            }
        }

        // Floating "Jump to Bottom ↓" pill
        AnimatedVisibility(
            visible = showJumpToBottom,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier =
                    Modifier.clickable {
                        userScrolledUp = false
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(18.dp),
                        tint = if (state.isGenerating) visualInfo.accentColor else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (state.isGenerating) "New Response ↓" else "Jump to Bottom ↓",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: ChatMessage,
    visualAccent: Color,
    onCopy: (String) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (message is ChatMessage.User) {
        val bubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("User message", message.content))
                                onCopy(message.content)
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                }
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
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                                Icons.Default.AutoAwesome,
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
                        }

                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = {
                                val content =
                                    when (message) {
                                        is ChatMessage.Agent -> message.content
                                        is ChatMessage.Error -> message.message
                                        is ChatMessage.System -> message.info
                                        is ChatMessage.User -> message.content
                                    }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Chat message", content))
                            },
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }

                    val messageContent =
                        when (message) {
                            is ChatMessage.Agent -> message.content.ifEmpty { "..." }
                            is ChatMessage.Error -> "Error: ${message.message}"
                            is ChatMessage.System -> message.info
                            is ChatMessage.User -> message.content
                        }

                    MarkdownMessage(
                        content = messageContent,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun MessageBubblePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MessageBubble(
                    message = ChatMessage.User("How do I filter a list in Kotlin?"),
                    visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    onCopy = {},
                )
                MessageBubble(
                    message =
                        ChatMessage.Agent(
                            """You can use the `filter` function:
                            |```kotlin
                            |val numbers = listOf(1, 2, 3, 4, 5)
                            |val evens = numbers.filter { it % 2 == 0 }
                            |println(evens) // [2, 4]
                            |```
                            """.trimMargin(),
                        ),
                    visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    onCopy = {},
                )
            }
        }
    }
}
