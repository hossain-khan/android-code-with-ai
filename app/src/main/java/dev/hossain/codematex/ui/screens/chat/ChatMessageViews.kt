package dev.hossain.codematex.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.ChatMessageBubble
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
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
                ChatMessageBubble(
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

@ThemePreviews
@Composable
private fun ChatMessageListPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                ChatMessageList(
                    state =
                        ChatScreen.State.Active(
                            messages =
                                listOf(
                                    ChatMessage.User("How do I filter a list in Kotlin?"),
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
                                ),
                            isGenerating = false,
                            isPreparing = false,
                            modelName = "gemma-2b-it",
                            activeBackend = "GPU",
                            modelSize = "2.5 GB",
                            modelMemory = "3.2 GB",
                            configInfo = "Temp: 0.7",
                            throughputInfo = "12.5 t/s",
                            topic = CodingTopic.KOTLIN,
                            eventSink = {},
                        ),
                    listState = rememberLazyListState(),
                    visualInfo = CodingTopic.KOTLIN.visualInfo,
                    onCopyMessage = {},
                )
            }
        }
    }
}
