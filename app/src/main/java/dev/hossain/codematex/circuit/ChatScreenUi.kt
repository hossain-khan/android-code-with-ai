package dev.hossain.codematex.circuit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatMessage
import dev.zacsweers.metro.AppScope

@CircuitInject(screen = ChatScreen::class, scope = AppScope::class)
@Composable
fun ChatScreenContent(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ChatScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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

@Composable
private fun ChatLayout(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            reverseLayout = true,
        ) {
            items(state.messages.reversed(), key = { it.hashCode() }) { message ->
                MessageBubble(message)
            }
        }

        Surface(tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                if (state.isPreparing) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
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
private fun MessageBubble(message: ChatMessage) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        tonalElevation = when (message) {
            is ChatMessage.User -> 0.dp
            is ChatMessage.Agent -> 1.dp
            is ChatMessage.Error -> 0.dp
            is ChatMessage.System -> 0.dp
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = when (message) {
                is ChatMessage.User -> message.content
                is ChatMessage.Agent -> message.content.ifEmpty { "..." }
                is ChatMessage.Error -> "Error: ${message.message}"
                is ChatMessage.System -> message.info
            },
            modifier = Modifier.padding(12.dp),
            color = when (message) {
                is ChatMessage.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
