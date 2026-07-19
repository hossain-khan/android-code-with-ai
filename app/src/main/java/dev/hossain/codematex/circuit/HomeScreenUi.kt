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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
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
import dev.hossain.codematex.data.model.CodingTopic
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomeScreenContent(
    state: HomeScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is HomeScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is HomeScreen.State.Success -> {
            HomeLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeLayout(
    state: HomeScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Code with AI") },
                actions = {
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.ManageModels) }) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Manage Models")
                    }
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.ViewAllSessions) }) {
                        Icon(Icons.Default.History, contentDescription = "Session History")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Choose a topic", style = MaterialTheme.typography.titleMedium)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.topics) { topic ->
                        TopicChip(topic) {
                            state.eventSink(HomeScreen.Event.TopicSelected(topic))
                        }
                    }
                }
            }

            if (state.recentSessions.isNotEmpty()) {
                item {
                    Text("Recent sessions", style = MaterialTheme.typography.titleMedium)
                }

                items(state.recentSessions) { session ->
                    SessionCard(session) {
                        state.eventSink(HomeScreen.Event.SessionClicked(session.id))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicChip(
    topic: CodingTopic,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(topic.displayName) },
    )
}

@Composable
private fun SessionCard(
    session: ChatSession,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleSmall)
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
