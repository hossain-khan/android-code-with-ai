package dev.hossain.codematex.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = SessionDetailScreen::class, scope = AppScope::class)
@Composable
fun SessionDetailScreenContent(
    state: SessionDetailScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SessionDetailScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is SessionDetailScreen.State.Success -> {
            SessionDetailLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailLayout(
    state: SessionDetailScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Session Detail") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(SessionDetailScreen.Event.Back) }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(SessionDetailScreen.Event.DeleteSession) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            SessionInfoCard(state.session, modifier = Modifier.padding(16.dp))

            if (state.messages.isNotEmpty()) {
                Text(
                    text = "Conversation",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.messages) { message ->
                        SessionMessageBubble(message)
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { state.eventSink(SessionDetailScreen.Event.ResumeSession) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Resume Session")
                }

                OutlinedButton(
                    onClick = { state.eventSink(SessionDetailScreen.Event.DeleteSession) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Session")
                }
            }
        }
    }
}

@Composable
private fun SessionInfoCard(
    session: ChatSession,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(session.title, style = MaterialTheme.typography.titleLarge)
        Text(session.summary, style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Topic: ${session.topic.displayName}", style = MaterialTheme.typography.labelMedium)
            Text("Messages: ${session.messageCount}", style = MaterialTheme.typography.labelMedium)
            Text("Model: ${session.modelUsed}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SessionMessageBubble(message: ChatMessage) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
                text =
                    when (message) {
                        is ChatMessage.User -> "You"
                        is ChatMessage.Agent -> "AI"
                        is ChatMessage.Error -> "Error"
                        is ChatMessage.System -> "System"
                    },
                style = MaterialTheme.typography.labelSmall,
                color =
                    when (message) {
                        is ChatMessage.User -> MaterialTheme.colorScheme.primary
                        is ChatMessage.Agent -> MaterialTheme.colorScheme.secondary
                        is ChatMessage.Error -> MaterialTheme.colorScheme.error
                        is ChatMessage.System -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                text =
                    when (message) {
                        is ChatMessage.User -> message.content
                        is ChatMessage.Agent -> message.content
                        is ChatMessage.Error -> message.message
                        is ChatMessage.System -> message.info
                    },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
