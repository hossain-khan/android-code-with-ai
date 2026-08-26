package dev.hossain.codematex.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import kotlinx.coroutines.launch

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

    // Check if the viewport is currently anchored at the bottom (index 0 in reverseLayout)
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 60
        }
    }

    // Detect user manual scroll gesture: if scrolling away from bottom, mark userScrolledUp
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledUp = !isAtBottom
        }
    }

    // Auto-scroll on new message added (user sent message or new session)
    val messageCount = state.messages.size
    LaunchedEffect(messageCount) {
        userScrolledUp = false
        listState.scrollToItem(0)
    }

    // Smart auto-scroll during token streaming (token count increases) as long as user hasn't scrolled up
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

@Composable
internal fun MessageBubble(
    message: ChatMessage,
    visualAccent: Color,
    onCopy: (String) -> Unit,
) {
    val context = LocalContext.current

    if (message is ChatMessage.User) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 48.dp),
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
