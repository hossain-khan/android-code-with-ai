package dev.hossain.codematex.circuit

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SessionHistoryLayout(
    state: SessionHistoryScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        topBar = {
            TopAppBar(
                title = { Text("Session History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(SessionHistoryScreen.Event.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            // Display horizontal filter chips if there are 2 or more unique topic categories
            if (state.availableTopics.size >= 2) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedTopic == null,
                            onClick = {
                                state.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(null))
                            },
                            label = {
                                Text("All (${state.allSessions.size})")
                            },
                            leadingIcon =
                                if (state.selectedTopic == null) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    }
                                } else {
                                    null
                                },
                        )
                    }

                    items(state.availableTopics) { topic ->
                        val visualInfo = topic.visualInfo
                        val count = state.allSessions.count { it.topic == topic }
                        val isSelected = state.selectedTopic == topic

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                state.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(topic))
                            },
                            label = {
                                Text("${topic.displayName} ($count)")
                            },
                            leadingIcon = {
                                Surface(
                                    shape = CircleShape,
                                    color =
                                        if (isSelected) {
                                            visualInfo.accentColor.copy(alpha = 0.3f)
                                        } else {
                                            visualInfo.accentColor.copy(alpha = 0.15f)
                                        },
                                ) {
                                    Text(
                                        text = visualInfo.iconGlyph,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = visualInfo.accentColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            if (state.sessions.isEmpty()) {
                EmptySessionsView(
                    selectedTopic = state.selectedTopic,
                    onClearFilter = {
                        state.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(null))
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                LazyVerticalGrid(
                    columns = if (isExpanded) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.sessions, key = { it.id }) { session ->
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
}

@Composable
private fun EmptySessionsView(
    selectedTopic: dev.hossain.codematex.data.model.CodingTopic?,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualInfo = selectedTopic?.visualInfo
    val accentColor = visualInfo?.accentColor ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (visualInfo != null) {
                        Text(
                            text = visualInfo.iconGlyph,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Text(
                text =
                    if (selectedTopic != null) {
                        "No ${selectedTopic.displayName} Sessions"
                    } else {
                        "No Chat Sessions Yet"
                    },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text =
                    if (selectedTopic != null) {
                        "You haven't recorded any conversations in ${selectedTopic.displayName} yet."
                    } else {
                        "Your offline AI tutoring sessions will be automatically saved and cataloged here."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f),
            )

            if (selectedTopic != null) {
                FilledTonalButton(
                    onClick = onClearFilter,
                    modifier = Modifier.padding(top = 4.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                ) {
                    Text("Show All Topics", style = MaterialTheme.typography.labelMedium)
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
    val visualInfo = session.topic.visualInfo
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.25f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = visualInfo.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = visualInfo.iconGlyph,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = visualInfo.accentColor,
                        )
                    }

                    Text(
                        session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete session",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Text(
                session.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    session.topic.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = visualInfo.accentColor,
                    fontWeight = FontWeight.SemiBold,
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

// ==========================================
// Previews
// ==========================================

private val sampleHistorySessions =
    listOf(
        ChatSession(
            id = "1",
            title = "Kotlin Flow Lifecycle in Compose",
            summary = "collectAsStateWithLifecycle best practices on Android.",
            topic = CodingTopic.KOTLIN,
            messageCount = 8,
            lastActiveAt = 0L,
            modelUsed = "Gemma 4-E2B IT",
        ),
        ChatSession(
            id = "2",
            title = "Python Asyncio Event Loop",
            summary = "Understanding asyncio tasks and gather concurrency.",
            topic = CodingTopic.PYTHON,
            messageCount = 5,
            lastActiveAt = 0L,
            modelUsed = "Gemma 4-E2B IT",
        ),
        ChatSession(
            id = "3",
            title = "Rust Ownership & Borrow Checker",
            summary = "Deep dive into mutable references and lifetimes.",
            topic = CodingTopic.RUST,
            messageCount = 12,
            lastActiveAt = 0L,
            modelUsed = "Gemma 4-E2B IT",
        ),
    )

@DevicePreviews
@Composable
private fun SessionHistoryScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        SessionHistoryLayout(
            state =
                SessionHistoryScreen.State.Success(
                    allSessions = sampleHistorySessions,
                    sessions = sampleHistorySessions,
                    selectedTopic = null,
                    availableTopics = listOf(CodingTopic.KOTLIN, CodingTopic.PYTHON, CodingTopic.RUST),
                    eventSink = {},
                ),
        )
    }
}

@ThemePreviews
@Composable
private fun EmptySessionsViewPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            EmptySessionsView(
                selectedTopic = CodingTopic.KOTLIN,
                onClearFilter = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SessionHistoryCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        SessionCard(
            session = sampleHistorySessions.first(),
            onClick = {},
            onDelete = {},
        )
    }
}
