package dev.hossain.codematex.ui.screens.home.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.animation.SessionCardSharedKey
import dev.hossain.codematex.ui.animation.SessionTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.codematex.util.formatRelativeTime
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun RecentSessionsSubUi(
    state: RecentSessionsSubState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.recentSessions.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { state.eventSink(RecentSessionsUiEvent.ViewAllSessions) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("View all", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (state.recentSessions.isEmpty()) {
            EmptySessionsCard(modifier = Modifier.fillMaxWidth())
        } else if (state.isExpanded) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.recentSessions) { session ->
                    SessionCard(session) {
                        state.eventSink(RecentSessionsUiEvent.SessionClicked(session.topic, session.id))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.recentSessions.forEach { session ->
                    SessionCard(session) {
                        state.eventSink(RecentSessionsUiEvent.SessionClicked(session.topic, session.id))
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: ChatSession,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val visualInfo = session.topic.visualInfo
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .sharedBoundsNav(SessionCardSharedKey(session.id))
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.12f))
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Topic accent vertical strip
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(visualInfo.accentColor),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedBoundsNav(SessionTitleSharedKey(session.id)),
                )
                Text(
                    session.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        session.topic.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualInfo.accentColor,
                        fontWeight = FontWeight.Medium,
                    )
                    val relativeTime = formatRelativeTime(session.lastActiveAt)
                    val metadataText =
                        if (relativeTime.isNotEmpty()) {
                            "${session.messageCount} messages • $relativeTime"
                        } else {
                            "${session.messageCount} messages"
                        }
                    Text(
                        metadataText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun EmptySessionsCard(modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Start Your First Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Select a topic above to ask questions and learn concepts with your private on-device AI tutor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class RecentSessionsSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is RecentSessionsSubScreen -> {
                SubUi<RecentSessionsSubState> { state, modifier ->
                    RecentSessionsSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun RecentSessionsSubUiPopulatedPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            RecentSessionsSubUi(
                state =
                    RecentSessionsSubState(
                        recentSessions =
                            listOf(
                                ChatSession(
                                    id = "s-1",
                                    topic = CodingTopic.KOTLIN,
                                    title = "Kotlin Coroutines Scopes",
                                    summary = "Discussion on viewModelScope vs lifecycleScope",
                                    messageCount = 6,
                                    lastActiveAt = System.currentTimeMillis() - 1800_000,
                                    modelUsed = "Gemma 2B",
                                ),
                                ChatSession(
                                    id = "s-2",
                                    topic = CodingTopic.PYTHON,
                                    title = "List Comprehensions",
                                    summary = "Nested loops and conditional filtering syntax",
                                    messageCount = 4,
                                    lastActiveAt = System.currentTimeMillis() - 43200_000,
                                    modelUsed = "Gemma 2B",
                                ),
                            ),
                        isExpanded = false,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun RecentSessionsSubUiEmptyPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            RecentSessionsSubUi(
                state =
                    RecentSessionsSubState(
                        recentSessions = emptyList(),
                        isExpanded = false,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
