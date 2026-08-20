package dev.hossain.codematex.circuit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun HomeLayout(
    state: HomeScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Code with AI") },
                scrollBehavior = scrollBehavior,
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
        if (isExpanded) {
            // Adaptive 2-Column Multi-Pane Layout for Tablets / Foldables / Landscape
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Left Column: Topics Grid
                Column(
                    modifier =
                        Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Choose a topic", style = MaterialTheme.typography.titleLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.topics.forEach { topic ->
                            TopicChip(topic) {
                                state.eventSink(HomeScreen.Event.TopicSelected(topic))
                            }
                        }
                    }
                }

                // Right Column: Recent Sessions
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Recent sessions", style = MaterialTheme.typography.titleLarge)
                    if (state.recentSessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No recent sessions yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.recentSessions) { session ->
                                SessionCard(session) {
                                    state.eventSink(HomeScreen.Event.SessionClicked(session.id))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Compact Single Column Layout
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
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleSmall)
            Text(
                session.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    session.topic.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${session.messageCount} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
