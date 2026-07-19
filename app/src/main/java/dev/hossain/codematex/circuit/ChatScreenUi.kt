package dev.hossain.codematex.circuit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatMessage
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = ChatScreen::class, scope = AppScope::class)
@Composable
fun ChatScreenContent(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ChatScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is ChatScreen.State.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    IconButton(onClick = { state.eventSink(ChatScreen.Event.Retry) }) {
                        Text("Retry")
                    }
                }
            }
        }

        is ChatScreen.State.Active -> {
            ChatLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatLayout(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .safeDrawingPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            reverseLayout = true,
        ) {
            items(state.messages.reversed(), key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onCopy = {
                        state.eventSink(ChatScreen.Event.CopyMessage(it))
                    },
                )
            }
        }

        Surface(tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                if (state.isPreparing) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ask about ${state.topic.displayName}...") },
                    trailingIcon = {
                        if (state.isGenerating) {
                            IconButton(onClick = { state.eventSink(ChatScreen.Event.StopGeneration) }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                            }
                        } else {
                            IconButton(
                                enabled = inputText.isNotBlank(),
                                onClick = {
                                    state.eventSink(ChatScreen.Event.SendMessage(inputText))
                                    inputText = ""
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Default.Send, contentDescription = "Send")
                            }
                        }
                    },
                    maxLines = 4,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onCopy: (String) -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        tonalElevation =
            when (message) {
                is ChatMessage.User -> 0.dp
                is ChatMessage.Agent -> 1.dp
                is ChatMessage.Error -> 0.dp
                is ChatMessage.System -> 0.dp
            },
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                val messageContent =
                    when (message) {
                        is ChatMessage.User -> message.content
                        is ChatMessage.Agent -> message.content.ifEmpty { "..." }
                        is ChatMessage.Error -> "Error: ${message.message}"
                        is ChatMessage.System -> message.info
                    }

                val messageColor =
                    when (message) {
                        is ChatMessage.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                when (message) {
                    is ChatMessage.Agent -> {
                        RichText {
                            Markdown(content = messageContent)
                        }
                    }

                    else -> {
                        Text(
                            text = messageContent,
                            modifier = Modifier.weight(1f),
                            color = messageColor,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val content =
                            when (message) {
                                is ChatMessage.User -> message.content
                                is ChatMessage.Agent -> message.content
                                is ChatMessage.Error -> message.message
                                is ChatMessage.System -> message.info
                            }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Chat message", content))
                    },
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy message")
                }
            }
        }
    }
}
