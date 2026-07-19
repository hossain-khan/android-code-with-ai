package dev.hossain.codematex.circuit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = SessionHistoryScreen::class, scope = AppScope::class)
@Composable
fun SessionHistoryScreenContent(
    state: SessionHistoryScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SessionHistoryScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is SessionHistoryScreen.State.Success -> {
            SessionHistoryLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionHistoryLayout(
    state: SessionHistoryScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Session History") })
        },
    ) { innerPadding ->
        if (state.sessions.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No sessions yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.sessions) { session ->
                    SessionCard(
                        session = session,
                        onClick = {
                            state.eventSink(SessionHistoryScreen.Event.OpenSession(session.id))
                        },
                        onDelete = {
                            state.eventSink(SessionHistoryScreen.Event.DeleteSession(session.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(session.title, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete session")
                }
            }
            Text(session.summary, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(session.topic.displayName, style = MaterialTheme.typography.labelSmall)
                Text("${session.messageCount} messages", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
