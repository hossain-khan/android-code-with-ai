package dev.hossain.codematex.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatSession
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(screen = SessionDetailScreen::class, scope = AppScope::class)
@Composable
fun SessionDetailScreenContent(
    state: SessionDetailScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SessionDetailScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                    .padding(16.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SessionInfoCard(state.session)

            Spacer(modifier = Modifier.weight(1f))

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

@Composable
private fun SessionInfoCard(session: ChatSession) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
